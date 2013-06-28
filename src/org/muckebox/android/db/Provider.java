package org.muckebox.android.db;


import org.muckebox.android.db.MuckeboxContract.AlbumEntry;
import org.muckebox.android.db.MuckeboxContract.ArtistEntry;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class Provider extends ContentProvider {
	private final static String LOG_TAG = "Provider";
	
	public final static String AUTHORITY = "org.muckebox.android.provider";
	public final static String SCHEME = "content://";
	
	public static final String ARTISTS = SCHEME + AUTHORITY + "/artists";
	public static final Uri URI_ARTISTS = Uri.parse(ARTISTS);
	public static final String ARTIST_ID_BASE = ARTISTS + "/id=";
	public static final String ARTIST_NAME_BASE = ARTISTS + "/name=";
	
	public static final String ALBUMS = SCHEME + AUTHORITY + "/albums";
	public static final Uri URI_ALBUMS = Uri.parse(ALBUMS);
	public static final String ALBUM_ID_BASE = ALBUMS + "/id=";
	public static final String ALBUM_TITLE_BASE = ALBUMS + "/title=";
	
	private static MuckeboxDbHelper mDbHelper = null;
	
	public Provider() {
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// Implement this to handle requests to delete one or more rows.
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public String getType(Uri uri) {
		// TODO: Implement this to handle requests for the MIME type of the data
		// at the given URI.
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// TODO: Implement this to handle requests to insert a new row.
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public boolean onCreate() {
		return true;
	}
	
	private static MuckeboxDbHelper getDbHelper(final Context context) {
		if (mDbHelper == null)
			mDbHelper = new MuckeboxDbHelper(context);
		
		return mDbHelper;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		Cursor result = null;
		
		if (URI_ARTISTS.equals(uri))
		{
			Log.d(LOG_TAG, "Query all artists");
			
			result = getDbHelper(getContext()).getReadableDatabase().query(
					ArtistEntry.TABLE_NAME, ArtistEntry.PROJECTION,
					null, null, null, null, ArtistEntry.SORT_ORDER, null);
			result.setNotificationUri(getContext().getContentResolver(), URI_ARTISTS);
		} else if (uri.toString().startsWith(ARTIST_ID_BASE)) {
			final long id = Long.parseLong(uri.toString().substring(ARTIST_ID_BASE.length()));
			
			Log.d(LOG_TAG, "Query artist id = " + id);
			
			result = getDbHelper(getContext()).getReadableDatabase().query(
					ArtistEntry.TABLE_NAME, ArtistEntry.PROJECTION,
					ArtistEntry.COLUMN_NAME_REMOTE_ID + " IS ?",
					new String[] { String.valueOf(id) }, null, null,
					ArtistEntry.SORT_ORDER, null);
			
			result.setNotificationUri(getContext().getContentResolver(), URI_ARTISTS);
		} else if (uri.toString().startsWith(ARTIST_NAME_BASE)) {
			String name = uri.toString().substring(ARTIST_NAME_BASE.length());
			
			Log.d(LOG_TAG, "Query artist name = " + name);
			
			result = getDbHelper(getContext()).getReadableDatabase().query(
					ArtistEntry.TABLE_NAME, ArtistEntry.PROJECTION,
					"LOWER(" + ArtistEntry.COLUMN_NAME_NAME + ") LIKE LOWER(?)",
					new String[] { "%" + name + "%" }, null, null,
					ArtistEntry.SORT_ORDER, null);
			
			result.setNotificationUri(getContext().getContentResolver(), URI_ARTISTS);
		} else if (URI_ALBUMS.equals(uri))
		{
			Log.d(LOG_TAG, "Query all albums");
			
			result = getDbHelper(getContext()).getReadableDatabase().query(
					AlbumEntry.TABLE_NAME, AlbumEntry.PROJECTION,
					null, null, null, null, AlbumEntry.SORT_ORDER, null);
			
			result.setNotificationUri(getContext().getContentResolver(), URI_ALBUMS);
		} else if (uri.toString().startsWith(ALBUM_ID_BASE)) {
			final long id = Long.parseLong(uri.toString().substring(ALBUM_ID_BASE.length()));
			
			Log.d(LOG_TAG, "Query album id = " + id);
			
			result = getDbHelper(getContext()).getReadableDatabase().query(
					AlbumEntry.TABLE_NAME, AlbumEntry.PROJECTION,
					AlbumEntry.COLUMN_NAME_REMOTE_ID + " IS ?",
					new String[] { String.valueOf(id) }, null, null,
					AlbumEntry.SORT_ORDER, null);
			
			result.setNotificationUri(getContext().getContentResolver(), URI_ALBUMS);
		} else if (uri.toString().startsWith(ALBUM_TITLE_BASE)) {
			String name = uri.toString().substring(ALBUM_TITLE_BASE.length());
			
			Log.d(LOG_TAG, "Query album name = " + name);

			result = getDbHelper(getContext()).getReadableDatabase().query(
					AlbumEntry.TABLE_NAME, AlbumEntry.PROJECTION,
					"LOWER(" + AlbumEntry.COLUMN_NAME_TITLE + ") LIKE LOWER(?)",
					new String[] { "%" + name + "%" }, null, null,
					AlbumEntry.SORT_ORDER, null);
			
			result.setNotificationUri(getContext().getContentResolver(), URI_ALBUMS);
		} else {
	        throw new UnsupportedOperationException("Unknown URI");
	    }
		
		return result;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		// TODO: Implement this to handle requests to update one or more rows.
		throw new UnsupportedOperationException("Not yet implemented");
	}
}
