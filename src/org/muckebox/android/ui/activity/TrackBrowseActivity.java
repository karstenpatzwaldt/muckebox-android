package org.muckebox.android.ui.activity;

import org.muckebox.android.R;
import org.muckebox.android.ui.fragment.TrackListFragment;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

public class TrackBrowseActivity extends Activity {
	public final static String ALBUM_ID_ARG = "album_id";
	public final static String TITLE_ARG = "title";
	
	TrackListFragment mDetailsFragment = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_track_browse);
		
        if (savedInstanceState == null) {
            // During initial setup, plug in the details fragment.
        	long artist_id = getIntent().getLongExtra(ALBUM_ID_ARG, 0);
        	String title = getIntent().getStringExtra(TITLE_ARG);
        	
            mDetailsFragment = TrackListFragment.newInstanceFromAlbum(artist_id);
            TextView header = (TextView) findViewById(R.id.track_list_title_strip);
            
            header.setText(title);

            getFragmentManager().beginTransaction().
            	add(R.id.track_list_top, mDetailsFragment).commit();
        }
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.track_browse, menu);
		return true;
	}
	
    public void toggleButtons(View item)
    {
    	mDetailsFragment.toggleButtons(item);
    }
}
