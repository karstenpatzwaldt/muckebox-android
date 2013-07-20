package org.muckebox.android.net;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentProducer;
import org.apache.http.entity.EntityTemplate;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;

import android.util.Log;

public class DownloadServer extends Thread {
    private static final String LOG_TAG = "DownloadServer";
    private static final int PORT = 2342;
    
    private String mMimeType;

    private boolean mReady = false;
    private boolean mStopped = false;
    
    private ServerSocket mServerSocket;
    
    private BlockingQueue<ByteBuffer> mQueue;
    private BasicHttpProcessor mHttpProcessor;
    private BasicHttpContext mHttpContext;
    private HttpService mHttpService;
    private HttpRequestHandlerRegistry mRegistry;
    
    private class FileHandler implements HttpRequestHandler {
        @Override
        public void handle(HttpRequest request, HttpResponse response,
            HttpContext context) throws HttpException, IOException {
            EntityTemplate entity = new EntityTemplate(new ContentProducer() {
                public void writeTo(final OutputStream os) throws IOException {
                    while (true) {
                        try {
                            ByteBuffer buf = mQueue.take();
                            
                            os.write(buf.array(), 0, buf.position());
    
                            if (buf.position() == 0 || mStopped)
                                break;
                        } catch (InterruptedException e) {
                            throw new IOException(e.toString());
                        }
                    }
                }
            });
            
            entity.setContentType(mMimeType);
            entity.setChunked(true);
            
            response.setEntity(entity);
        }
    }
    
    private static void d(String text) {
        Log.d(LOG_TAG, Thread.currentThread().getId() + ": " + text);
    }
    
    public static String getUrl() {
        return "http://localhost:" + PORT + "/";
    }
    
    public DownloadServer(String mimeType) {
        super(LOG_TAG);
        
        mMimeType = mimeType;
        mQueue = new LinkedBlockingQueue<ByteBuffer>();
        
        initHttpServer();
    }

    @Override
    public void run() {
        super.run();
        
        try {
            d("Starting server on " + PORT);
            
            try {
                mServerSocket = new ServerSocket(PORT);
                mServerSocket.setReuseAddress(true);
                
                mReady = true;
                
                while (! mStopped) {
                    Socket socket = null;
                    DefaultHttpServerConnection connection =
                        new DefaultHttpServerConnection();
                    
                    try {
                        d("Waiting for new connection");
                        
                        socket = mServerSocket.accept();
                        
                        d("Got connection");
                        
                        connection.bind(socket, new BasicHttpParams());
                        mHttpService.handleRequest(connection, mHttpContext);
                        
                        d("Request handling finished");
                    } catch (HttpException e) {
                        Log.e(LOG_TAG, "Got HTTP error: " + e);
                    } finally {
                        connection.shutdown();
                        
                        if (socket != null)
                            socket.close();
                        
                        socket = null;
                    }
                }
            } finally {
                if (mServerSocket != null)
                    mServerSocket.close();
            }
            
            d("Server stopped");
        } catch (IOException e) {
            Log.e(LOG_TAG, "IOException, maybe ok");
            e.printStackTrace();
        }
    }
    
    private void initHttpServer() {
        mHttpProcessor = new BasicHttpProcessor();
        mHttpContext = new BasicHttpContext();
        mHttpService = new HttpService(mHttpProcessor,
            new DefaultConnectionReuseStrategy(),
            new DefaultHttpResponseFactory());
        
        mHttpProcessor.addInterceptor(new ResponseContent());
        mHttpProcessor.addInterceptor(new ResponseConnControl());
        
        mRegistry = new HttpRequestHandlerRegistry();
        
        mRegistry.register("*", new FileHandler());
        
        mHttpService.setHandlerResolver(mRegistry);
    }
    
    public void feed(ByteBuffer buf) {
        try {
            mQueue.put(buf);
        } catch (InterruptedException e) {
            Log.e(LOG_TAG, "Interrupted while putting, should not happen");
        }
    }
    
    public void finish() {
        d("Finishing input data stream");
        
        feed(ByteBuffer.allocate(0));
    }
    
    public void quit() {
        d("Stopping server");
        
        mStopped = true;
        mQueue.clear();
        
        finish();
        
        try {
            mServerSocket.close();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Could not close server socket!");
        }
    }
    
    public boolean isReady() {
        return mReady;
    }
    
}
