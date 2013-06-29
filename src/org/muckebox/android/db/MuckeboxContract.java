package org.muckebox.android.db;

import android.provider.BaseColumns;

public final class MuckeboxContract {
	public MuckeboxContract() { }
	
	public static final String AS = " AS ";
	public static final String ASC = " COLLATE LOCALIZED ASC";
	
	public static abstract class ArtistEntry implements BaseColumns {
		public static final String TABLE_NAME = "artists";
		
		public static final String SHORT_ID = _ID;
		public static final String SHORT_NAME = "name";
		
		public static final String FULL_ID		= TABLE_NAME + "." + SHORT_ID;
		public static final String FULL_NAME	= TABLE_NAME + "." + SHORT_NAME;
		
		public static final String ALIAS_ID		= TABLE_NAME + "_" + SHORT_ID;
		public static final String ALIAS_NAME	= TABLE_NAME + "_" + SHORT_NAME;
		
    	public static final String[] PROJECTION = {
    		FULL_ID,
    		FULL_NAME + AS + ALIAS_NAME
    	};
    	
    	public final static String SORT_ORDER = ALIAS_NAME + ASC;
	}
	
	public static abstract class AlbumEntry implements BaseColumns {
		public static final String TABLE_NAME = "albums";
		
		public static final String SHORT_ID			= _ID;
		public static final String SHORT_COUNT		= _COUNT;
		public static final String SHORT_ARTIST_ID	= "artist_id";
		public static final String SHORT_TITLE 		= "title";
		
		public static final String FULL_ID 			= TABLE_NAME + "." + SHORT_ID;
		public static final String FULL_ARTIST_ID 	= TABLE_NAME + "." + SHORT_ARTIST_ID;
		public static final String FULL_TITLE		= TABLE_NAME + "." + SHORT_TITLE;

		public static final String ALIAS_ID 		= TABLE_NAME + "_" + SHORT_ID;
		public static final String ALIAS_COUNT		= TABLE_NAME + "_" + SHORT_COUNT;
		public static final String ALIAS_ARTIST_ID 	= TABLE_NAME + "_" + SHORT_ARTIST_ID;
		public static final String ALIAS_TITLE		= TABLE_NAME + "_" + SHORT_TITLE;
		
		public static final String[] PROJECTION = {
			FULL_ID,
			FULL_ARTIST_ID + AS + ALIAS_ARTIST_ID,
			FULL_TITLE + AS + ALIAS_TITLE
		};
		
		public static final String SORT_ORDER = ALIAS_TITLE + ASC;
	}
	
	public static abstract class TrackEntry implements BaseColumns {
		public static final String TABLE_NAME	= "tracks";
		
		public static final String ID				= _ID;

		public static final String ALBUM_ID			= "album_id";
		public static final String ARTIST_ID		= "artist_id";
		public static final String TITLE			= "title";
		
		public static final String TRACKNUMBER		= "tracknumber";
		public static final String DISCNUMBER		= "discnumber";
		
		public static final String LABEL			= "label";
		public static final String CATALOGNUMBER	= "catalognumber";
		
		public static final String LENGTH			= "length";
		public static final String DISPLAY_ARTIST	= "display_artist";
		public static final String DATE				= "date";
		
		public static final String COLUMN_NAME_ID				= TABLE_NAME + "." + ID;
		public static final String COLUMN_NAME_ALBUM_ID			= TABLE_NAME + "." + ALBUM_ID;
		public static final String COLUMN_NAME_ARTIST_ID		= TABLE_NAME + "." + ARTIST_ID;
		public static final String COLUMN_NAME_TITLE			= TABLE_NAME + "." + TITLE;
		public static final String COLUMN_NAME_TRACKNUMBER		= TABLE_NAME + "." + TRACKNUMBER;
		public static final String COLUMN_NAME_DISCNUMBER		= TABLE_NAME + "." + DISCNUMBER;
		public static final String COLUMN_NAME_LABEL			= TABLE_NAME + "." + LABEL;
		public static final String COLUMN_NAME_CATALOGNUMBER	= TABLE_NAME + "." + CATALOGNUMBER;
		public static final String COLUMN_NAME_LENGTH			= TABLE_NAME + "." + LENGTH;
		public static final String COLUMN_NAME_DISPLAY_ARTIST	= TABLE_NAME + "." + DISPLAY_ARTIST;
		public static final String COLUMN_NAME_DATE				= TABLE_NAME + "." + DATE;
		
		public static final String[] PROJECTION = {
			COLUMN_NAME_ID,
			COLUMN_NAME_ALBUM_ID,
			COLUMN_NAME_ARTIST_ID,
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
				"(" + COLUMN_NAME_DISCNUMBER + " * 1000 + " +
				COLUMN_NAME_TRACKNUMBER + ") ASC ";
	}
	
	public static abstract class AlbumArtistJoin implements BaseColumns {
		public static final String TABLE_NAME = AlbumEntry.TABLE_NAME + " LEFT OUTER JOIN " +
				ArtistEntry.TABLE_NAME + " ON (" + AlbumEntry.FULL_ARTIST_ID + " = " +
				ArtistEntry.FULL_ID + ")";
		
		public static final String[] PROJECTION = {
			AlbumEntry.FULL_ID,
			AlbumEntry.FULL_TITLE + AS + AlbumEntry.ALIAS_TITLE,
			
			ArtistEntry.FULL_ID + AS + ArtistEntry.ALIAS_ID,
			ArtistEntry.FULL_NAME + AS + ArtistEntry.ALIAS_NAME
		};
		
		public static final String SORT_ORDER = AlbumEntry.SORT_ORDER;
	}
	
	public static abstract class ArtistAlbumJoin implements BaseColumns {
		public static final String TABLE_NAME = ArtistEntry.TABLE_NAME + " JOIN " +
				AlbumEntry.TABLE_NAME + " ON (" + ArtistEntry.FULL_ID + " = " +
				AlbumEntry.FULL_ARTIST_ID + ")";
		
		public static final String[] PROJECTION = {
			ArtistEntry.FULL_ID,
			ArtistEntry.FULL_NAME + AS + ArtistEntry.ALIAS_NAME,
			"GROUP_CONCAT(" + AlbumEntry.FULL_TITLE + ", ', ') " + AS + AlbumEntry.ALIAS_TITLE
		};
		
		public static final String SORT_ORDER = ArtistEntry.SORT_ORDER;
		public static final String GROUP_BY = ArtistEntry.FULL_ID;
	}
}
