package org.muckebox.android.db;


import org.muckebox.android.db.MuckeboxContract.AlbumArtistJoin;
import org.muckebox.android.db.MuckeboxContract.AlbumEntry;
import org.muckebox.android.db.MuckeboxContract.ArtistAlbumJoin;
import org.muckebox.android.db.MuckeboxContract.ArtistEntry;
import org.muckebox.android.db.MuckeboxContract.CacheEntry;
import org.muckebox.android.db.MuckeboxContract.DownloadEntry;
import org.muckebox.android.db.MuckeboxContract.DownloadTrackEntry;
import org.muckebox.android.db.MuckeboxContract.TrackDownloadCacheJoin;
import org.muckebox.android.db.MuckeboxContract.TrackEntry;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

public class MuckeboxProvider extends ContentProvider {
	private final static String LOG_TAG = "Provider";

	public final static String AUTHORITY = "org.muckebox.android.provider";
	public final static String SCHEME = "content://";
		
	private static int MASK_GROUP(int mask) {
		return (mask & ~0xffff);
	}
	
	private static final int ARTISTS					= (1 << 16);
	private static final int ARTISTS_ID					= (1 << 16) + 1;
	private static final int ARTISTS_NAME				= (1 << 16) + 2;
	
	private static final int ARTISTS_WITH_ALBUMS		= (2 << 16);
	private static final int ARTISTS_WITH_ALBUMS_NAME	= (2 << 16) + 1;
	
	private static final int ALBUMS						= (3 << 16);
	private static final int ALBUMS_ID					= (3 << 16) + 1;
	private static final int ALBUMS_TITLE				= (3 << 16) + 2;
	private static final int ALBUMS_ARTIST				= (3 << 16) + 3;
	
	private static final int ALBUMS_WITH_ARTIST			= (4 << 16);
	private static final int ALBUMS_WITH_ARTIST_TITLE	= (4 << 16) + 1;
	private static final int ALBUMS_WITH_ARTIST_ARTIST	= (4 << 16) + 2;
	
	private static final int TRACKS						= (5 << 16);
	private static final int TRACKS_ID					= (5 << 16) + 1;
	private static final int TRACKS_ALBUM				= (5 << 16) + 4;
	
	private static final int TRACKS_WITH_DETAILS		= (6 << 16);
	private static final int TRACKS_WITH_DETAILS_ALBUM	= (6 << 16) + 1;
	
	private static final int DOWNLOADS					= (7 << 16);
	private static final int DOWNLOADS_ID				= (7 << 16) + 1;
	private static final int DOWNLOADS_TRACK			= (7 << 16) + 2;
	
	private static final int DOWNLOADS_WITH_DETAILS		= (8 << 16);
	
	private static final int CACHE						= (9 << 16);
	private static final int CACHE_ID					= (9 << 16) + 1;
	private static final int CACHE_TRACK				= (9 << 16) + 2;
	
	public static final Uri URI_ARTISTS						= Uri.parse(SCHEME + AUTHORITY + "/artists");
	public static final Uri URI_ARTISTS_NAME				= Uri.parse(SCHEME + AUTHORITY + "/artists/name");
	
	public static final Uri URI_ARTISTS_WITH_ALBUMS			= Uri.parse(SCHEME + AUTHORITY + "/artists+albums");
	public static final Uri URI_ARTISTS_WITH_ALBUMS_NAME	= Uri.parse(SCHEME + AUTHORITY + "/artists+albums/name");
	
	public static final Uri URI_ALBUMS						= Uri.parse(SCHEME + AUTHORITY + "/albums");
	public static final Uri URI_ALBUMS_ARTIST				= Uri.parse(SCHEME + AUTHORITY + "/albums/artist");
	public static final Uri URI_ALBUMS_TITLE				= Uri.parse(SCHEME + AUTHORITY + "/albums/title");
	
	public static final Uri URI_ALBUMS_WITH_ARTIST			= Uri.parse(SCHEME + AUTHORITY + "/albums+artist");
	public static final Uri URI_ALBUMS_WITH_ARTIST_TITLE	= Uri.parse(SCHEME + AUTHORITY + "/albums+artist/name");
	public static final Uri URI_ALBUMS_WITH_ARTIST_ARTIST	= Uri.parse(SCHEME + AUTHORITY + "/albums+artist/artist");
	
	public static final Uri URI_TRACKS						= Uri.parse(SCHEME + AUTHORITY + "/tracks");
	public static final Uri URI_TRACKS_ALBUM				= Uri.parse(SCHEME + AUTHORITY + "/tracks/album");
	
	public static final Uri URI_TRACKS_WITH_DETAILS			= Uri.parse(SCHEME + AUTHORITY + "/tracks+details");
	public static final Uri URI_TRACKS_WITH_DETAILS_ALBUM	= Uri.parse(SCHEME + AUTHORITY + "/tracks+details/album");
	
	public static final Uri URI_DOWNLOADS					= Uri.parse(SCHEME + AUTHORITY + "/downloads");
	public static final Uri URI_DOWNLOADS_TRACK				= Uri.parse(SCHEME + AUTHORITY + "/downloads/track");
	
	public static final Uri URI_DOWNLOADS_WITHDETAILS		= Uri.parse(SCHEME + AUTHORITY + "/downloads+details");
	
	public static final Uri URI_CACHE						= Uri.parse(SCHEME + AUTHORITY + "/cache");
	public static final Uri URI_CACHE_TRACK					= Uri.parse(SCHEME + AUTHORITY + "/cache/track");
	
	private static final UriMatcher mMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	
	static {
		mMatcher.addURI(AUTHORITY, "artists", 					ARTISTS);
		mMatcher.addURI(AUTHORITY, "artists/#", 				ARTISTS_ID);
		mMatcher.addURI(AUTHORITY, "artists/name/*", 			ARTISTS_NAME);
		
		mMatcher.addURI(AUTHORITY, "artists+albums",			ARTISTS_WITH_ALBUMS);
		mMatcher.addURI(AUTHORITY, "artists+albums/name/*",		ARTISTS_WITH_ALBUMS_NAME);
			
		mMatcher.addURI(AUTHORITY, "albums", 					ALBUMS);
		mMatcher.addURI(AUTHORITY, "albums/#", 					ALBUMS_ID);
		mMatcher.addURI(AUTHORITY, "albums/title/*", 			ALBUMS_TITLE);
		mMatcher.addURI(AUTHORITY, "albums/artist/#", 			ALBUMS_ARTIST);
		
		mMatcher.addURI(AUTHORITY, "albums+artist",				ALBUMS_WITH_ARTIST);
		mMatcher.addURI(AUTHORITY, "albums+artist/name/*",		ALBUMS_WITH_ARTIST_TITLE);
		mMatcher.addURI(AUTHORITY, "albums+artist/artist/#",	ALBUMS_WITH_ARTIST_ARTIST);
		
		mMatcher.addURI(AUTHORITY, "tracks", 					TRACKS);
		mMatcher.addURI(AUTHORITY, "tracks/#", 					TRACKS_ID);
		mMatcher.addURI(AUTHORITY, "tracks/album/#",			TRACKS_ALBUM);
		
		mMatcher.addURI(AUTHORITY, "tracks+details", 			TRACKS_WITH_DETAILS);
		mMatcher.addURI(AUTHORITY, "tracks+details/album/#",	TRACKS_WITH_DETAILS_ALBUM);
		
		mMatcher.addURI(AUTHORITY, "downloads", 				DOWNLOADS);
		mMatcher.addURI(AUTHORITY, "downloads/#", 				DOWNLOADS_ID);
		mMatcher.addURI(AUTHORITY, "downloads/track/#",			DOWNLOADS_TRACK);
		
		mMatcher.addURI(AUTHORITY, "downloads+details",			DOWNLOADS_WITH_DETAILS);
		
		mMatcher.addURI(AUTHORITY, "cache", 					CACHE);
		mMatcher.addURI(AUTHORITY, "cache/#", 					CACHE_ID);
		mMatcher.addURI(AUTHORITY, "cache/track/#",				CACHE_TRACK);
	}
	
	private static MuckeboxDbHelper mDbHelper;
	
	@Override
	public boolean onCreate() {
		mDbHelper = new MuckeboxDbHelper(getContext());
		return true;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		SQLiteDatabase db = mDbHelper.getWritableDatabase();
		ContentResolver resolver = getContext().getContentResolver();
		int match = mMatcher.match(uri);
		int group = MASK_GROUP(match);
		int ret;
		
		Log.d(LOG_TAG, "Deleting " + uri);
		
		switch (group)
		{
		case DOWNLOADS:
			switch (match) {
			case DOWNLOADS_ID:
				selection = DownloadEntry.FULL_ID + " = ?";
				selectionArgs = new String[] { uri.getLastPathSegment() };
				
				break;
			case DOWNLOADS_TRACK:
				selection = DownloadEntry.FULL_TRACK_ID + " = ?";
				selectionArgs = new String[] { uri.getLastPathSegment() };
				
				break;
			}
			
			ret = db.delete(DownloadEntry.TABLE_NAME, selection, selectionArgs);
			
			resolver.notifyChange(URI_DOWNLOADS, null);
			resolver.notifyChange(URI_TRACKS, null);
			
			break;
			
		case CACHE:
			switch (match) {
			case CACHE_ID:
				selection = CacheEntry.FULL_ID + " = ?";
				selectionArgs = new String[] { uri.getLastPathSegment() };
				
				break;
			}
		
			ret = db.delete(CacheEntry.TABLE_NAME, selection, selectionArgs);
			
			resolver.notifyChange(URI_CACHE, null);
			resolver.notifyChange(URI_TRACKS, null);
			
			break;
			
		default:
			throw new UnsupportedOperationException("Not yet implemented");
		}
		
		return ret;
	}

	@Override
	public String getType(Uri uri) {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		SQLiteDatabase db = mDbHelper.getWritableDatabase();
		ContentResolver resolver = getContext().getContentResolver();
		int match = mMatcher.match(uri);
		long id;
		Uri ret;

		switch (match) {
		case DOWNLOADS:
			id = db.insert(DownloadEntry.TABLE_NAME, null, values);
			
			resolver.notifyChange(URI_DOWNLOADS, null);
			resolver.notifyChange(URI_TRACKS, null);
			
			ret = URI_DOWNLOADS.buildUpon().appendPath(Long.toString(id)).build();
			
			break;
			
		case CACHE:
			id = db.insert(CacheEntry.TABLE_NAME, null, values);
			
			resolver.notifyChange(URI_CACHE, null);
			resolver.notifyChange(URI_TRACKS, null);
			
			ret = URI_CACHE.buildUpon().appendPath(Long.toString(id)).build();
			
			break;
			
		default:
			throw new UnsupportedOperationException("Unknown URI");
		}
		
		return ret;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SQLiteDatabase db = mDbHelper.getReadableDatabase();
		ContentResolver resolver = getContext().getContentResolver();
		int match = mMatcher.match(uri);
		int group = MASK_GROUP(match);
		Cursor result = null;
		
		Log.d(LOG_TAG, "Query '" + uri + "'");
		
		switch (group) {
		case ARTISTS:
			switch (match) {
			case ARTISTS_ID:
				selection = ArtistEntry.FULL_ID + " = ?";
				selectionArgs = new String[] { uri.getLastPathSegment() };
				
				break;
			case ARTISTS_NAME:
				selection = "LOWER(" + ArtistEntry.FULL_NAME + ") LIKE LOWER(?)";
				selectionArgs = new String[] { "%" + uri.getLastPathSegment() + "%" };
				
				break;
			}
			
			result = db.query(ArtistEntry.TABLE_NAME,
					(projection == null ? ArtistEntry.PROJECTION : projection),
					selection, selectionArgs, null, null,
					(sortOrder == null ? ArtistEntry.SORT_ORDER : sortOrder), null);
			
			result.setNotificationUri(resolver, URI_ARTISTS);
			
			break;
			
		case ARTISTS_WITH_ALBUMS:
			switch (match)
			{
			case ARTISTS_WITH_ALBUMS_NAME:
				selection = "LOWER(" + ArtistEntry.FULL_NAME + ") LIKE (?)";
				selectionArgs = new String[] { "%" + uri.getLastPathSegment() + "%" };
				
				break;
			}
			
			result = db.query(ArtistAlbumJoin.TABLE_NAME,
					(projection == null ? ArtistAlbumJoin.PROJECTION : projection),
					selection, selectionArgs, ArtistAlbumJoin.GROUP_BY, null,
					(sortOrder == null ? ArtistAlbumJoin.SORT_ORDER : sortOrder), null);
			
			result.setNotificationUri(resolver, URI_ARTISTS);
			
			break;
			
		case ALBUMS:
			switch (match) {
			case ALBUMS_ID:
				selection = AlbumEntry.FULL_ID + " = ?";
				selectionArgs = new String[] { uri.getLastPathSegment() };
				
				break;
			case ALBUMS_TITLE:
				selection = "LOWER(" + AlbumEntry.FULL_TITLE + ") LIKE LOWER(?)";
				selectionArgs = new String[] { "%" + uri.getLastPathSegment() + "%" };
				
				break;
			case ALBUMS_ARTIST:
				selection = AlbumEntry.FULL_ARTIST_ID + " = ?";
				selectionArgs = new String[] { uri.getLastPathSegment() };
				
				break;
			}
			
			result = db.query(AlbumEntry.TABLE_NAME,
					(projection == null ? AlbumEntry.PROJECTION : projection),
					selection, selectionArgs, null, null,
					(sortOrder == null ? AlbumEntry.SORT_ORDER : sortOrder), null);
			
			result.setNotificationUri(resolver, URI_ALBUMS);
			
			break;
			
		case ALBUMS_WITH_ARTIST:
			switch (match) {
			case ALBUMS_WITH_ARTIST_TITLE:
				selection = "LOWER(" + AlbumEntry.FULL_TITLE + ") LIKE LOWER(?)";
				selectionArgs = new String[] { "%" + uri.getLastPathSegment() + "%" };
				
				break;
			case ALBUMS_WITH_ARTIST_ARTIST:
				selection = AlbumEntry.FULL_ARTIST_ID + " = ?";
				selectionArgs = new String[] { uri.getLastPathSegment() };
				
				break;
			}
			
			result = db.query(AlbumArtistJoin.TABLE_NAME,
					(projection == null ? AlbumArtistJoin.PROJECTION : projection),
					selection, selectionArgs, null, null,
					(sortOrder == null ? AlbumArtistJoin.SORT_ORDER : sortOrder), null);
			
			result.setNotificationUri(resolver, URI_ALBUMS);
			
			break;
			
		case TRACKS:
			switch (match) {
			case TRACKS_ID:
				selection = TrackEntry.FULL_ID + " = ?";
				selectionArgs = new String[] { uri.getLastPathSegment() };
				
				break;
			case TRACKS_ALBUM:
				selection = TrackEntry.FULL_ALBUM_ID + " = ?";
				selectionArgs = new String[] { uri.getLastPathSegment() };
				
				break;
			}
			
			result = db.query(TrackEntry.TABLE_NAME,
					(projection == null ? TrackEntry.PROJECTION : projection),
					selection, selectionArgs, null, null, 
					(sortOrder == null ? TrackEntry.SORT_ORDER : sortOrder), null);
			
			result.setNotificationUri(resolver, URI_TRACKS);
			
			break;
			
		case TRACKS_WITH_DETAILS:
			switch (match) {
			case TRACKS_WITH_DETAILS_ALBUM:
				selection = TrackEntry.FULL_ALBUM_ID + " = ?";
				selectionArgs = new String[] { uri.getLastPathSegment() };
				
				break;
			}
			
			result = db.query(TrackDownloadCacheJoin.TABLE_NAME,
					(projection == null ? TrackDownloadCacheJoin.PROJECTION : projection),
					selection, selectionArgs, null, null, 
					(sortOrder == null ? TrackDownloadCacheJoin.SORT_ORDER : sortOrder), null);
			
			result.setNotificationUri(resolver, URI_TRACKS);
			
			break;
			
		case DOWNLOADS:
			switch (match) {
			case DOWNLOADS_ID:
				selection = DownloadEntry.FULL_ID + " = ?";
				selectionArgs = new String[] { uri.getLastPathSegment() };
				
				break;
			case DOWNLOADS_TRACK:
				selection = DownloadEntry.FULL_TRACK_ID + " = ?";
				selectionArgs = new String[] { uri.getLastPathSegment() };
				
				break;
			}
			
			result = db.query(DownloadEntry.TABLE_NAME,
					(projection == null) ? DownloadEntry.PROJECTION : projection,
					selection, selectionArgs, null, null,
					(sortOrder == null) ? DownloadEntry.SORT_ORDER : sortOrder, null);
			
			result.setNotificationUri(resolver, URI_DOWNLOADS);
			
			break;
			
		case DOWNLOADS_WITH_DETAILS:
			result = db.query(DownloadTrackEntry.TABLE_NAME, 
					(projection == null) ? DownloadTrackEntry.PROJECTION : projection,
					selection, selectionArgs, null, null,
					(sortOrder == null) ? DownloadEntry.SORT_ORDER : sortOrder, null);
			
			result.setNotificationUri(resolver, URI_DOWNLOADS);
			
			break;
			
		case CACHE:
			switch (match) {
			case CACHE_ID:
				selection = CacheEntry.FULL_ID + " = ?";
				selectionArgs = new String[] { uri.getLastPathSegment() };
				
				break;
			case CACHE_TRACK:
				selection = CacheEntry.FULL_TRACK_ID + " = ?";
				selectionArgs = new String[] { uri.getLastPathSegment() };
				
				break;
			}
			
			result = db.query(CacheEntry.TABLE_NAME,
					(projection == null) ? CacheEntry.PROJECTION : projection,
					selection, selectionArgs, null, null,
					(sortOrder == null) ? CacheEntry.SORT_ORDER : sortOrder, null);
			
			result.setNotificationUri(resolver, URI_CACHE);
			
			break;
			
		default:
			throw new UnsupportedOperationException("Unknown URI");
		}
		
		return result;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		SQLiteDatabase db = mDbHelper.getWritableDatabase();
		ContentResolver resolver = getContext().getContentResolver();
		int match = mMatcher.match(uri);
		int group = MASK_GROUP(match);
		int ret;
		
		switch (group) {
		case DOWNLOADS:
			switch (match) {
			case DOWNLOADS_ID:
				selection = DownloadEntry.FULL_ID + " = ?";
				selectionArgs = new String[] { uri.getLastPathSegment() };
				
				break;
			}
			
			ret = db.update(DownloadEntry.TABLE_NAME, values, selection, selectionArgs);
			
			resolver.notifyChange(URI_DOWNLOADS, null);
			resolver.notifyChange(URI_TRACKS, null);
			
			break;
			
		case CACHE:
			switch (match) {
			case CACHE_ID:
				selection = CacheEntry.FULL_ID + " = ?";
				selectionArgs = new String[] { uri.getLastPathSegment() };
				
				break;
			case CACHE_TRACK:
				selection = CacheEntry.FULL_TRACK_ID + " = ?";
				selectionArgs = new String[] { uri.getLastPathSegment() };
				
				break;
			}
			
			ret = db.update(CacheEntry.TABLE_NAME, values, selection, selectionArgs);

			resolver.notifyChange(URI_CACHE, null);
			resolver.notifyChange(URI_TRACKS, null);
			
			break;
			
		default:
			throw new UnsupportedOperationException("Not yet implemented");
		}
		
		return ret;
	}
}
