/*   
 * Copyright 2013 karsten
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

package org.muckebox.android.ui.fragment;

import org.muckebox.android.R;
import org.muckebox.android.ui.utils.NavigationListener;

import android.app.Activity;
import android.app.Fragment;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class DrawerFragment extends Fragment {
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
        new DrawerEntry(R.drawable.collections_collection, R.string.title_albums),
        new DrawerEntry(R.drawable.device_access_time, R.string.title_latest),
        new DrawerEntry(R.drawable.av_play, R.string.title_now_playing)
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_drawer, container, false);
        
        final NavigationListener listener = (NavigationListener) getActivity();
        
        mBrowseList = (ListView) view.findViewById(R.id.drawer_browse_list);
        mBrowseList.setAdapter(new DrawerListAdapter(getActivity(), DRAWER_BROWSE_ENTRIES));
        
        mBrowseList.setOnItemClickListener(new ListView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (DRAWER_BROWSE_ENTRIES[position].textId) {
                case R.string.title_artists:
                    listener.onAllArtistsSelected();
                    break;
                case R.string.title_albums:
                    listener.onAllAlbumsSelected();
                    break;
                case R.string.title_latest:
                    listener.onRecentAlbumsSelected();
                    break;
                case R.string.title_now_playing:
                    listener.onNowPlayingSelected();
                    break;
                }
                
                mDrawer.closeDrawers();
            }
        });
        
        mOtherList = (ListView) view.findViewById(R.id.drawer_other_list);
        mOtherList.setAdapter(new DrawerListAdapter(getActivity(), DRAWER_OTHER_ENTRIES));
        
        mOtherList.setOnItemClickListener(new ListView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (DRAWER_OTHER_ENTRIES[position].textId) {
                case R.string.title_activity_downloads:
                    listener.onDownloadsSelected();
                    break;
                case R.string.title_activity_settings:
                    listener.onSettingsSelected();
                    break;
                }
                
                mDrawer.closeDrawers();
            }
        });
        
        setHasOptionsMenu(true);

        return view;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        
        mDrawer = (DrawerLayout) getActivity().findViewById(R.id.drawer_layout);
        mDrawer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // ignore
            }
        });
        
        mDrawerToggle = new ActionBarDrawerToggle(
            getActivity(), mDrawer, R.drawable.ic_drawer,
            R.string.drawer_open, R.string.drawer_close);
        
        getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
        getActivity().getActionBar().setHomeButtonEnabled(true);
        
        mDrawerToggle.syncState();
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return mDrawerToggle.onOptionsItemSelected(item);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        
        mDrawerToggle.onConfigurationChanged(newConfig);
    }
    
}
