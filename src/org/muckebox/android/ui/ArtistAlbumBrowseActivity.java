package org.muckebox.android.ui;

import java.util.Locale;

import org.muckebox.android.R;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.widget.TextView;

public class ArtistAlbumBrowseActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_albumartist_browse);
		
        if (savedInstanceState == null) {
            // During initial setup, plug in the details fragment.
        	long artist_id = getIntent().getLongExtra("artist_id", 0);
        	String name = getIntent().getStringExtra("artist_name").toUpperCase(Locale.getDefault());
        	
            AlbumListFragment details = AlbumListFragment.newInstanceFromArtist(artist_id);
            TextView header = (TextView) findViewById(R.id.artist_albums_title_strip);
            
            header.setText(name);

            getFragmentManager().beginTransaction().add(R.id.artist_album_list_top, details).commit();
        }
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.artist_album_browse_activiy, menu);
		return true;
	}

}
