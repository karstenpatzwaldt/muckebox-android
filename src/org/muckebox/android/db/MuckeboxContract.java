package org.muckebox.android.db;

import android.provider.BaseColumns;

public final class MuckeboxContract {
	public MuckeboxContract() { }
	
	public static abstract class ArtistEntry implements BaseColumns {
		public static final String TABLE_NAME = "artists";
		public static final String COLUMN_NAME_REMOTE_ID = "remote_id";
		public static final String COLUMN_NAME_NAME = "name";
		
    	public static final String[] PROJECTION = {
    		_ID,
    		COLUMN_NAME_REMOTE_ID,
    		COLUMN_NAME_NAME
    	};
    	
    	public final static String SORT_ORDER = COLUMN_NAME_NAME + " ASC ";
	}
	
	public static abstract class AlbumEntry implements BaseColumns {
		public static final String TABLE_NAME = "albums";
		public static final String COLUMN_NAME_REMOTE_ID = "remote_id";
		public static final String COLUMN_NAME_REMOTE_ARTIST_ID = "remote_artist_id";
		public static final String COLUMN_NAME_TITLE = "title";
		
		public static final String[] PROJECTION = {
			_ID,
			COLUMN_NAME_REMOTE_ID,
			COLUMN_NAME_REMOTE_ARTIST_ID,
			COLUMN_NAME_TITLE
		};
		
		public static final String SORT_ORDER = COLUMN_NAME_TITLE + " ASC ";
	}
}
