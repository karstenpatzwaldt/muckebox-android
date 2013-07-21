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
import org.muckebox.android.ui.fragment.BrowseFragment;
import org.muckebox.android.ui.fragment.TrackListFragment;
import org.muckebox.android.ui.fragment.AlbumListFragment;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class BrowseActivity extends Activity
    implements AlbumListFragment.OnAlbumSelectedListener {
    public final static String ACTION_PLAYLIST = "playlist";
    
    private final static String LOG_TAG = "BrowseActivity";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_browse);
        
        if (savedInstanceState == null) {
            FragmentTransaction tf = getFragmentManager().beginTransaction();
            tf.add(R.id.fragment_container, new BrowseFragment());
            tf.commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.browse, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
	{
    	switch (item.getItemId()) {
    	case R.id.action_settings:
    		Intent i = new Intent(BrowseActivity.this, SettingsActivity.class);
    		startActivity(i);
    		return true;
    	}
    	
    	return false;
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
}
