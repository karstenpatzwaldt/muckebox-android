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

package org.muckebox.android.utils;

import java.util.HashSet;
import java.util.Set;

import org.muckebox.android.Muckebox;
import org.muckebox.android.db.MuckeboxProvider;
import org.muckebox.android.db.MuckeboxContract.CacheEntry;
import org.muckebox.android.db.MuckeboxContract.CachePlaylistEntry;
import org.muckebox.android.services.DownloadService;

import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;
import android.util.Log;

public class CacheCleaner extends ContentObserver {
    private final static String LOG_TAG = "CacheCleaner";
    
    public CacheCleaner(Handler handler) {
        super(handler);
    }

    @Override
    public void onChange(boolean selfChange) {
        Log.d(LOG_TAG, "Cache changed, checking");

        if (isCacheTooBig()) {
            Set<String> dbFiles = removeOldestEntryAndListFiles();
            Set<String> localFiles = getAllLocalFiles();

            localFiles.removeAll(dbFiles);

            for (String fileName: localFiles) {
                Log.w(LOG_TAG, "Found orphaned file " + fileName + ", removing");
                
                Muckebox.getAppContext().deleteFile(fileName);
            }
        }
    }
    
    private boolean isCacheTooBig() {
        return getCurrentCacheSize() > Preferences.getCacheSize();
    }
    
    private int getCurrentCacheSize() {
        Cursor c = Muckebox.getAppContext().getContentResolver().query(
            MuckeboxProvider.URI_CACHE_SIZE, null, null, null, null);
        
        try {
            if (c.moveToFirst()) {
                int ret = c.getInt(c.getColumnIndex(CacheEntry.ALIAS_SIZE));
                
                Log.d(LOG_TAG, "Current cache size is " + ret);
                
                return ret;
            } else {
                Log.e(LOG_TAG, "Could not get size");
                return 0;
            }
        } finally {
            c.close();
        }
    }
    
    private Set<String> removeOldestEntryAndListFiles() {
        Cursor c = Muckebox.getAppContext().getContentResolver().query(
            MuckeboxProvider.URI_CACHE, null, null, null, null);
        
        try {
            int playingIndex = c.getColumnIndex(CachePlaylistEntry.ALIAS_PLAYING);
            int pinnedIndex = c.getColumnIndex(CacheEntry.ALIAS_PINNED);
            int trackIdIndex = c.getColumnIndex(CacheEntry.ALIAS_TRACK_ID);
            int fileNameIndex = c.getColumnIndex(CacheEntry.ALIAS_FILENAME);
            boolean removedFile = false;
            Set<String> ret = new HashSet<String>();
            
            c.moveToPosition(-1);
            
            while (c.moveToNext()) {
                if (c.getInt(playingIndex) == 0 &&
                    c.getInt(pinnedIndex) == 0 &&
                    ! removedFile)
                {
                    int trackId = c.getInt(trackIdIndex);
                    
                    DownloadService.discardTrack(Muckebox.getAppContext(), trackId);
                    
                    Log.d(LOG_TAG, "Removing track " + trackId);
                    
                    removedFile = true;
                }
                
                ret.add(c.getString(fileNameIndex));
            }
            
            return ret;
        } finally {
            c.close();
        }
    }
    
    private Set<String> getAllLocalFiles() {
        Set<String> ret = new HashSet<String>();
        String[] fileList = Muckebox.getAppContext().fileList();
        
        for (int i = 0; i < fileList.length; ++i)
            ret.add(fileList[i]);
        
        return ret;
    }
}
