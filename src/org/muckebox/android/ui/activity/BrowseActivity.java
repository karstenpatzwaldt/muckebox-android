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

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class BrowseActivity extends Activity
    implements AlbumListFragment.OnAlbumSelectedListener {
    public final static String ACTION_PLAYLIST = "playlist";
    public final static String ACTION_DOWNLOADS = "downloads";
    
    private final static String LOG_TAG = "BrowseActivity";
    
    private ListView mBrowseList;
    private ListView mOtherList;
    private DrawerLayout mDrawer;
    
    private ActionBarDrawerToggle mDrawerToggle;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_browse);
        
        Intent intent = getIntent();
        
        if (intent.getAction().equals(ACTION_DOWNLOADS))
        {
            FragmentTransaction tf = getFragmentManager().beginTransaction();
            tf.add(R.id.fragment_container, new DownloadListFragment());
            tf.commit();
        } else {
            if (savedInstanceState == null)
            {
                FragmentTransaction tf = getFragmentManager().beginTransaction();
                tf.add(R.id.fragment_container, new ArtistListFragment());
                tf.commit();
            }
        }

        mBrowseList = (ListView) findViewById(R.id.drawer_browse_list);
        mBrowseList.setAdapter(new ArrayAdapter<String>(this,
            R.layout.list_row_drawer, R.id.drawer_list_row_title,
            getResources().getStringArray(R.array.drawer_browse_entries)));
        
        mBrowseList.setOnItemClickListener(new ListView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Fragment target = null;

                switch (position) {
                case 0:
                    target = new ArtistListFragment();
                    break;
                case 1:
                    target = new AlbumListFragment();
                    break;
                }
                
                switchFragment(target);
                mDrawer.closeDrawers();
            }
        });
        
        mOtherList = (ListView) findViewById(R.id.drawer_other_list);
        mOtherList.setAdapter(new ArrayAdapter<String>(this,
            R.layout.list_row_drawer, R.id.drawer_list_row_title,
            getResources().getStringArray(R.array.drawer_other_entries)));
        
        mOtherList.setOnItemClickListener(new ListView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Fragment target = null;

                switch (position) {
                case 0:
                    target = new DownloadListFragment();
                    break;
                case 1:
                    target = new SettingsFragment();
                    break;
                }
                
                switchFragment(target);
                mDrawer.closeDrawers();
            }
        });
       
        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
        
        mDrawerToggle = new ActionBarDrawerToggle(
            this, mDrawer, R.drawable.ic_drawer,
            R.string.drawer_open, R.string.drawer_close);
        
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
    }
    
    private void switchFragment(Fragment newFragment) {
        FragmentTransaction tf = getFragmentManager().beginTransaction();
        tf.replace(R.id.fragment_container, newFragment);
        tf.commit();
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
	{
        return mDrawerToggle.onOptionsItemSelected(item);
	}
    
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        
        mDrawerToggle.onConfigurationChanged(newConfig);
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
