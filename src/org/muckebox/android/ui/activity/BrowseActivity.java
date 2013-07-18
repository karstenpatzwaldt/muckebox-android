package org.muckebox.android.ui.activity;

import org.muckebox.android.R;
import org.muckebox.android.ui.fragment.BrowseFragment;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuItem;

public class BrowseActivity extends Activity {
    public final static String ACTION_PLAYLIST = "playlist";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_browse);
        
        FragmentTransaction tf = getFragmentManager().beginTransaction();
        tf.add(R.id.fragment_container, new BrowseFragment());
        tf.commit();
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
}
