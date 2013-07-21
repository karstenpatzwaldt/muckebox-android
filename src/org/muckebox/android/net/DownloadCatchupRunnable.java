/*   
 * Copyright 2013 Karsten Patzwaldt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.muckebox.android.net;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.muckebox.android.Muckebox;
import org.muckebox.android.utils.BufferUtils;

import android.os.Handler;
import android.util.Log;

public class DownloadCatchupRunnable implements Runnable {
    private final static String LOG_TAG = "DownloadCatchupRunnable";
    private final static long BUFFER_SIZE = 32 * 1024;
    
    public static final int MESSAGE_DATA_RECEIVED   = 10;
    public static final int MESSAGE_DATA_COMPLETE   = 11;
    public static final int MESSAGE_ERROR           = 12;
    
    private String mFilename;
    private int mTrackId;
    private Handler mHandler;
    private long mBytesToRead;
    
    public DownloadCatchupRunnable(String filename, long bytesToRead, int trackId, Handler handler) {
        mFilename = filename;
        mTrackId = trackId;
        mHandler = handler;
        mBytesToRead = bytesToRead;
    }

    @Override
    public void run() {
        try {
            FileInputStream input = Muckebox.getAppContext().openFileInput(mFilename);
            boolean eofSeen = false;
            
            while (mBytesToRead > 0 && ! eofSeen) {
                ByteBuffer buf = ByteBuffer.allocate((int) Math.min(mBytesToRead, BUFFER_SIZE));
                
                eofSeen = BufferUtils.readIntoBuffer(input, buf) || Thread.interrupted();
                
                if (mHandler != null)
                    mHandler.sendMessage(mHandler.obtainMessage(
                        MESSAGE_DATA_RECEIVED, mTrackId, 0, buf));
                
                mBytesToRead -= buf.position();
            }
            
            if (mBytesToRead > 0)
                Log.e(LOG_TAG, "Could not read all bytes (" + mBytesToRead + " left)");
            
            if (mHandler != null)
                mHandler.sendMessage(mHandler.obtainMessage(
                    MESSAGE_DATA_COMPLETE, mTrackId, 0));
        } catch (IOException e) {
            Log.e(LOG_TAG, "failed to catch up!");
            
            e.printStackTrace();
            
            if (mHandler != null)
                mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_ERROR, mTrackId));
        }
    }

}
