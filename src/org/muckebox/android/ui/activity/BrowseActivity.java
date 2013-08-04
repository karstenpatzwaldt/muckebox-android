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
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class BrowseActivity extends Activity
    implements AlbumListFragment.OnAlbumSelectedListener {
    public final static String ACTION_PLAYLIST = "playlist";
    public final static String ACTION_DOWNLOADS = "downloads";
    
    private final static String LOG_TAG = "BrowseActivity";
    
    private ListView mBrowseList;
    private ListView mOtherList;
    private DrawerLayout mDrawer;
    
    private ActionBarDrawerToggle mDrawerToggle;
    
    private static class DrawerEntry {
        public DrawerEntry(int newIconId, int newTextId) {
            iconId = newIconId;
            textId = newTextId;
        }
        int iconId;
        int textId;
    }
    
    private static final DrawerEntry DRAWER_BROWSE_ENTRIES[] = new DrawerEntry[] {
        new DrawerEntry(R.drawable.social_group, R.string.title_artists),
        new DrawerEntry(R.drawable.collections_collection, R.string.title_albums)
    };
    
    private static final DrawerEntry DRAWER_OTHER_ENTRIES[] = new DrawerEntry[] {
        new DrawerEntry(R.drawable.av_download, R.string.title_activity_downloads),
        new DrawerEntry(R.drawable.action_settings, R.string.title_activity_settings)
    };
    
    private class DrawerListAdapter extends ArrayAdapter<DrawerEntry> {
        DrawerEntry[] mEntries;
        Activity mActivity;
        
        public DrawerListAdapter(Activity activity, DrawerEntry[] entries) {
            super(activity, 0, entries);
            
            mEntries = entries;
            mActivity = activity;
        }
        
        @Override
        public View getView(int position, View view, ViewGroup parent) {
            if (view == null) {
                LayoutInflater inflater = mActivity.getLayoutInflater();
                view = inflater.inflate(R.layout.list_row_drawer, null);
            }
            
            TextView textView = (TextView) view.findViewById(R.id.drawer_list_row_title);
            ImageView iconView = (ImageView) view.findViewById(R.id.drawer_list_row_icon);
            
            textView.setText(mEntries[position].textId);
            iconView.setImageResource(mEntries[position].iconId);
            
            return view;
        }
    }
    
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
        } else {
            if (savedInstanceState == null)
            {
                FragmentTransaction tf = getFragmentManager().beginTransaction();
                tf.add(R.id.fragment_container, new ArtistListFragment());
                tf.commit();
            }
        }

        mBrowseList = (ListView) findViewById(R.id.drawer_browse_list);
        mBrowseList.setAdapter(new DrawerListAdapter(this, DRAWER_BROWSE_ENTRIES));
        
        mBrowseList.setOnItemClickListener(new ListView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Fragment target = null;

                switch (DRAWER_BROWSE_ENTRIES[position].textId) {
                case R.string.title_artists:
                    target = new ArtistListFragment();
                    break;
                case R.string.title_albums:
                    target = new AlbumListFragment();
                    break;
                }
                
                switchFragment(target);
                mDrawer.closeDrawers();
            }
        });
        
        mOtherList = (ListView) findViewById(R.id.drawer_other_list);
        mOtherList.setAdapter(new DrawerListAdapter(this, DRAWER_OTHER_ENTRIES));
        
        mOtherList.setOnItemClickListener(new ListView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Fragment target = null;

                switch (DRAWER_OTHER_ENTRIES[position].textId) {
                case R.string.title_activity_downloads:
                    target = new DownloadListFragment();
                    break;
                case R.string.title_activity_settings:
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
        tf.addToBackStack(null);
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
