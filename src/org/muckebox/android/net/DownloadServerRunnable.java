package org.muckebox.android.net;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import android.util.Log;

public class DownloadServerRunnable implements Runnable {
    private static final String LOG_TAG = "DownloadServer";
    private static final int PORT = 2342;
    
    private String mMimeType;

    private boolean mReady = false;
    
    private BlockingQueue<ByteBuffer> mQueue;
    
    private static void d(String text) {
        Log.d(LOG_TAG, Thread.currentThread().getId() + ": " + text);
    }
    
    public static String getUrl() {
        return "http://localhost:" + PORT + "/";
    }
    
    public DownloadServerRunnable(String mimeType) {
        mMimeType = mimeType;
        
        mQueue = new LinkedBlockingQueue<ByteBuffer>();
    }

    @Override
    public void run() {
        try {
            ServerSocket serverSocket = null;
            Socket socket = null;
            OutputStream os = null;
            
            d("Starting server on " + PORT);
            
            try {
                serverSocket = new ServerSocket(PORT);
                
                mReady = true;
                
                socket = serverSocket.accept();
                os = socket.getOutputStream();
                
                d("Got connection!");
            
                os.write(new String("HTTP/1.1 200 OK\r\n").getBytes());
                os.write(new String("Content-Type: " + mMimeType + "\r\n").getBytes());
                os.write(new String("Connection: close\r\n").getBytes());
                os.write(new String("Transfer-Encoding: chunked\r\n").getBytes());
                os.write(new String("\r\n").getBytes());
                
                boolean eosSeen = false;
                
                while (! eosSeen) {
                    ByteBuffer buf = mQueue.take();

                    if (buf.position() == 0)
                    {
                        os.write(new String("0\r\n\r\n").getBytes());
                        
                        eosSeen = true;

                        break;
                    }
                    
                    os.write(String.format("%x\r\n", buf.position()).getBytes());
                    os.write(buf.array(), 0, buf.position());
                    os.write(new String("\r\n").getBytes());
                }
            } finally {
                if (os != null)
                    os.close();
                
                if (socket != null)
                    socket.close();
                
                if (serverSocket != null)
                    serverSocket.close();
            }
            
            d("Finished");
        } catch (InterruptedException e) {
            d("Got interrupted");
        } catch (IOException e) {
            Log.e(LOG_TAG, "IOException, maybe ok");
            e.printStackTrace();
        }
    }
    
    public void feed(ByteBuffer buf) {
        try {
            mQueue.put(buf);
        } catch (InterruptedException e) {
            Log.e(LOG_TAG, "Interrupted while putting, should not happen");
        }
    }
    
    public boolean isReady() {
        return mReady;
    }
    
}
