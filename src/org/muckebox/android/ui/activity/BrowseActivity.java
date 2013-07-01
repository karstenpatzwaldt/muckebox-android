package org.muckebox.android.ui.activity;

import java.util.Locale;

import org.muckebox.android.R;
import org.muckebox.android.ui.fragment.AlbumListFragment;
import org.muckebox.android.ui.fragment.ArtistListFragment;

import android.content.Intent;
import android.os.Bundle;
import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;

public class BrowseActivity extends FragmentActivity {
    SectionsPagerAdapter mSectionsPagerAdapter;
    ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse);

        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
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
    
    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
        	Fragment ret;
        	
        	switch (position) {
        	case 0:
        		ret = new ArtistListFragment();
        		break;
        	case 1:
        		ret = new AlbumListFragment();
        		break;
        	default:
        		assert(false);
        		ret = null;
        	}
        	
        	return ret;
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_section1).toUpperCase(l);
                case 1:
                    return getString(R.string.title_section2).toUpperCase(l);
            }
            return null;
        }
    }
}
