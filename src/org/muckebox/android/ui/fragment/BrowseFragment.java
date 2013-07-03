package org.muckebox.android.ui.fragment;

import java.util.Locale;

import org.muckebox.android.R;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class BrowseFragment extends Fragment {
    SectionsPagerAdapter mSectionsPagerAdapter;
    ViewPager mViewPager;

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View ret = inflater.inflate(R.layout.fragment_browse, container, false);
       
        mSectionsPagerAdapter = new SectionsPagerAdapter(getChildFragmentManager());

        mViewPager = (ViewPager) ret.findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        
        return ret;
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
        	if (isAdded())
        	{
	            Locale l = Locale.getDefault();
	            switch (position) {
	                case 0:
	                    return getString(R.string.title_section1).toUpperCase(l);
	                case 1:
	                    return getString(R.string.title_section2).toUpperCase(l);
	            }
        	}
        	
            return null;
        }
    }
}
