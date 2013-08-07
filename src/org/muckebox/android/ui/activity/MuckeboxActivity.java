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

package org.muckebox.android.ui.activity;

import org.muckebox.android.R;
import org.muckebox.android.ui.fragment.ArtistListFragment;
import org.muckebox.android.ui.fragment.DownloadListFragment;
import org.muckebox.android.ui.fragment.SettingsFragment;
import org.muckebox.android.ui.fragment.TrackListFragment;
import org.muckebox.android.ui.fragment.AlbumListFragment;
import org.muckebox.android.ui.utils.NavigationListener;
import org.muckebox.android.utils.Preferences;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.util.Log;

public class MuckeboxActivity extends Activity
    implements NavigationListener {
    public final static String ACTION_PLAYLIST = "playlist";
    public final static String ACTION_DOWNLOADS = "downloads";
    
    private final static String LOG_TAG = "BrowseActivity";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_browse);
        
        Intent intent = getIntent();
        
        if (ACTION_DOWNLOADS.equals(intent.getAction()))
        {
            FragmentTransaction tf = getFragmentManager().beginTransaction();
            tf.replace(R.id.fragment_container, new DownloadListFragment());
            tf.commit();
        } else if (ACTION_PLAYLIST.equals(intent.getAction())) {
            Fragment fragment = TrackListFragment.newInstanceFromPlaylist(
                Preferences.getCurrentPlaylistId());
            
            FragmentTransaction tf = getFragmentManager().beginTransaction();
            tf.replace(R.id.fragment_container, fragment);
            tf.commit();
        } else {
            if (savedInstanceState == null)
            {
                FragmentTransaction tf = getFragmentManager().beginTransaction();
                tf.add(R.id.fragment_container, new ArtistListFragment());
                tf.commit();
            }
        }
    }
    
    private void switchFragment(Fragment newFragment) {
        FragmentTransaction tf = getFragmentManager().beginTransaction();
        
        tf.replace(R.id.fragment_container, newFragment);
        tf.addToBackStack(null);
        
        tf.commit();
    }

    @Override
    public void onAllAlbumsSelected() {
        switchFragment(new AlbumListFragment());
    }

    @Override
    public void onAllArtistsSelected() {
        switchFragment(new ArtistListFragment());
    }

    @Override
    public void onRecentAlbumsSelected() {
        switchFragment(AlbumListFragment.newInstanceLatest());
    }

    @Override
    public void onAlbumSelected(long id, String title) {
        Log.d(LOG_TAG, "Opening track list for album " + id + "(" + title + ")");

        FragmentManager fm = getFragmentManager();
        TrackListFragment tracklist;
        
        tracklist = (TrackListFragment) fm.findFragmentById(R.id.tracklist);
            
        if (tracklist == null) {
            tracklist = TrackListFragment.newInstanceFromAlbum(id, title);
            FragmentTransaction t = fm.beginTransaction();
     
            t.replace(R.id.fragment_container, tracklist);
            t.addToBackStack(null);
            t.commit();
        } else {
            tracklist.reInit(id, title);
        }
    }

    @Override
    public void onNowPlayingSelected() {
        switchFragment(TrackListFragment.newInstanceFromPlaylist(
            Preferences.getCurrentPlaylistId()));
    }

    @Override
    public void onDownloadsSelected() {
        switchFragment(new DownloadListFragment());
    }
    
    @Override
    public void onSettingsSelected() {
        switchFragment(new SettingsFragment());
    }
}
