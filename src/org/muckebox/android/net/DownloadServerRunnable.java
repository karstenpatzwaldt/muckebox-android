package org.muckebox.android.net;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;

import android.util.Log;

public class DownloadServerRunnable implements Runnable {
    private static final String LOG_TAG = "DownloadServer";
    private static final int PORT = 2342;
    
    private String mMimeType;
    
    private boolean mFinished = false;
    private boolean mReady = false;
    
    private Queue<ByteBuffer> mQueue;
    
    public static String getUrl() {
        return "http://localhost:" + PORT + "/";
    }
    
    public DownloadServerRunnable(String mimeType) {
        mMimeType = mimeType;
        
        mQueue = new LinkedList<ByteBuffer>();
    }

    @Override
    public void run() {
        try {
            ServerSocket serverSocket = null;
            Socket socket = null;
            OutputStream os = null;
            
            Log.d(LOG_TAG, "Starting server on " + PORT);
            
            try {
                serverSocket = new ServerSocket(PORT);
                
                mReady = true;
                
                socket = serverSocket.accept();
                os = socket.getOutputStream();
                
                Log.d(LOG_TAG, "Got connection!");
            
                os.write(new String("HTTP/1.1 200 OK\n").getBytes());
                os.write(new String("Content-Type: " + mMimeType + "\n").getBytes());
                os.write(new String("Connection: close\n").getBytes());
                os.write(new String("\n").getBytes());
                
                while (true)
                {
                    while (mQueue.isEmpty() && ! mFinished) {
                        synchronized (mQueue) {
                            mQueue.wait();
                        }
                    }
                    
                    synchronized (mQueue) {
                        if (! mQueue.isEmpty()) {
                            ByteBuffer buf = mQueue.remove();
                            
                            os.write(buf.array(), 0, buf.position());
                        }
                    }
                    
                    if (mFinished)
                        break;
                }
            } finally {
                if (os != null)
                    os.close();
                
                if (socket != null)
                    socket.close();
                
                if (serverSocket != null)
                    serverSocket.close();
            }
            
            Log.d(LOG_TAG, "Finished");
        } catch (InterruptedException e) {
            // should be ok?
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void feed(ByteBuffer buf) {
        synchronized (mQueue) {
            mQueue.add(buf);
            mQueue.notify();
        }
    }
    
    public void done() {
        mFinished = true;
    }
    
    public boolean isReady() {
        return mReady;
    }
    
}
