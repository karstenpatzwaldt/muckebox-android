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
	
	public static abstract class TrackEntry implements BaseColumns {
		public static final String TABLE_NAME = "tracks";
		public static final String COLUMN_NAME_REMOTE_ID = "remote_id";
		public static final String COLUMN_NAME_REMOTE_ALBUM_ID = "remote_album_id";
		public static final String COLUMN_NAME_REMOTE_ARTIST_ID = "remote_artist_id";
		public static final String COLUMN_NAME_TITLE = "title";
		
		public static final String COLUMN_NAME_TRACKNUMBER = "tracknumber";
		public static final String COLUMN_NAME_DISCNUMBER = "discnumber";
		
		public static final String COLUMN_NAME_LABEL = "label";
		public static final String COLUMN_NAME_CATALOGNUMBER = "catalognumber";
		
		public static final String COLUMN_NAME_LENGTH = "length";
		public static final String COLUMN_NAME_DISPLAY_ARTIST = "display_artist";
		public static final String COLUMN_NAME_DATE = "DATE";
		
		public static final String[] PROJECTION = {
			_ID,
			COLUMN_NAME_REMOTE_ID,
			COLUMN_NAME_REMOTE_ALBUM_ID,
			COLUMN_NAME_REMOTE_ARTIST_ID,
			COLUMN_NAME_TITLE,
			
			COLUMN_NAME_TRACKNUMBER,
			COLUMN_NAME_DISCNUMBER,
			
			COLUMN_NAME_LABEL,
			COLUMN_NAME_CATALOGNUMBER,
			
			COLUMN_NAME_LENGTH,
			COLUMN_NAME_DISPLAY_ARTIST,
			COLUMN_NAME_DATE
		};
		
		public static final String SORT_ORDER =
				"(" + COLUMN_NAME_DISCNUMBER + " * 1000 + " + COLUMN_NAME_TRACKNUMBER + ") ASC ";
	}
}
