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

package org.muckebox.android.db;

import org.muckebox.android.db.MuckeboxContract.PlaylistEntry;
import org.muckebox.android.db.MuckeboxContract.TrackEntry;
import org.muckebox.android.utils.Preferences;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

public class PlaylistHelper {
    public static int getCurrentPlaylistId() {
        return Preferences.getCurrentPlaylistId();
    }
    
    public static void rebuildFromTrackCursor(Context ctx, Uri uri) {
        rebuildFromTrackList(ctx, uri, 0);
    }
    
    public static Uri rebuildFromTrackList(Context ctx, Uri uri, int currentIndex) {
        ContentResolver resolver = ctx.getContentResolver();
        Cursor c = resolver.query(uri, null, null, null, null);
        Uri ret = null;
        
        try {
            int currentPlaylistId = getCurrentPlaylistId();
            Uri currentPlaylistUri = Uri.withAppendedPath(
                MuckeboxProvider.URI_PLAYLIST, Integer.toString(currentPlaylistId));
            int trackIdIndex = c.getColumnIndex(TrackEntry.SHORT_ID);
            int i = -1;
            
            resolver.delete(currentPlaylistUri, null, null);
            
            c.moveToPosition(i);
    
            while (c.moveToNext())
            {
                ++i;
                
                ContentValues values = new ContentValues();
                
                values.put(PlaylistEntry.SHORT_PLAYLIST_ID, currentPlaylistId);
                values.put(PlaylistEntry.SHORT_TRACK_ID, c.getInt(trackIdIndex));
                values.put(PlaylistEntry.SHORT_POSITION, i);
                
                Uri newUri = resolver.insert(currentPlaylistUri, values);
                
                if (i == currentIndex)
                    ret = newUri;
            }
        } finally {
            c.close();
        }
        
        return ret;
    }
    
    public static boolean isLast(Context ctx, Uri entryUri) {
        Uri queryUri = Uri.withAppendedPath(
            MuckeboxProvider.URI_PLAYLIST_AFTER, entryUri.getLastPathSegment());
        Cursor c = ctx.getContentResolver().query(queryUri, null, null, null, null);
        
        try {
            return c.getCount() == 0;
        } finally {
            c.close();
        }
    }
    
    public static int getNextTrackId(Context ctx, Uri entryUri) {
        Uri queryUri = Uri.withAppendedPath(
            MuckeboxProvider.URI_PLAYLIST_AFTER, entryUri.getLastPathSegment());
        Cursor c = ctx.getContentResolver().query(queryUri, null, null, null, null);
        
        if (! c.moveToFirst())
            throw new UnsupportedOperationException("No more tracks in playlist");
            
        try {
            return c.getInt(c.getColumnIndex(PlaylistEntry.ALIAS_TRACK_ID));
        } finally {
            c.close();
        }
    }
    
    public static boolean isFirst(Context ctx, Uri entryUri) {
        Uri queryUri = Uri.withAppendedPath(
            MuckeboxProvider.URI_PLAYLIST_BEFORE, entryUri.getLastPathSegment());
        Cursor c = ctx.getContentResolver().query(queryUri, null, null, null, null);
        
        try {
            return c.getCount() == 0;
        } finally {
            c.close();
        }
    }
    
    public static int getTrackId(Context ctx, Uri entryUri) {
        Cursor c = ctx.getContentResolver().query(
            entryUri, null, null, null, null);
        
        try {
            c.moveToFirst();
            return c.getInt(c.getColumnIndex(PlaylistEntry.ALIAS_TRACK_ID));
        } finally {
            c.close(); 
        }
    }
}
