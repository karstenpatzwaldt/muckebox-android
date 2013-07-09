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
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class MuckeboxProvider extends ContentProvider {
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
	public static final String ALBUM_ARTIST_BASE = ALBUMS + "/artist=";
	
	public static final String TRACKS = SCHEME + AUTHORITY + "/tracks";
	public static final Uri URI_TRACKS = Uri.parse(TRACKS);
	public static final String TRACK_ID_BASE = TRACKS + "/id=";
	public static final String TRACK_TITLE_BASE = TRACKS + "/title=";
	public static final String TRACK_ALBUM_BASE = TRACKS + "/album=";
	public static final String TRACK_ARTIST_BASE = TRACKS + "/artist=";
	
	public static final String DOWNLOADS = SCHEME + AUTHORITY + "/downloads";
	public static final Uri URI_DOWNLOADS = Uri.parse(DOWNLOADS);
	public static final String DOWNLOAD_ID_BASE = DOWNLOADS + "/id=";
	
	public static final String CACHE = SCHEME + AUTHORITY + "/cache";
	public static final Uri URI_CACHE = Uri.parse(CACHE);
	public static final String CACHE_ID_BASE = CACHE + "/id=";
	
	public static final String DOWNLOADDETAILS = SCHEME + AUTHORITY + "/downloaddetails";
	public static final Uri URI_DOWNLOADDETAILS = Uri.parse(DOWNLOADDETAILS);
	
	private static MuckeboxDbHelper mDbHelper = null;
	
	public MuckeboxProvider() {
	}
	
	private static MuckeboxDbHelper getDbHelper(final Context context) {
		if (mDbHelper == null)
			mDbHelper = new MuckeboxDbHelper(context);
		
		return mDbHelper;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		SQLiteDatabase db = getDbHelper(getContext()).getWritableDatabase();
		ContentResolver resolver = getContext().getContentResolver();
		int ret;
		
		synchronized(this) {
			if (URI_DOWNLOADS.equals(uri))
			{
				ret = db.delete(DownloadEntry.TABLE_NAME, selection, selectionArgs);
				
				resolver.notifyChange(URI_DOWNLOADS, null);
				resolver.notifyChange(URI_TRACKS, null);
			} else if (uri.toString().startsWith(DOWNLOAD_ID_BASE))
			{
				final long id = Long.parseLong(uri.toString().substring(DOWNLOAD_ID_BASE.length()));
				String whereClause = DownloadEntry.FULL_ID + " = " + Long.toString(id);
				
				if (! TextUtils.isEmpty(selection))
				{
					whereClause += " AND " + selection;
				}
				
				ret = db.delete(DownloadEntry.TABLE_NAME, whereClause, selectionArgs);
				
				resolver.notifyChange(URI_DOWNLOADS, null);
				resolver.notifyChange(URI_TRACKS, null);
			} else if (URI_CACHE.equals(uri))
			{
				ret = db.delete(CacheEntry.TABLE_NAME, selection, selectionArgs);
				
				resolver.notifyChange(URI_CACHE, null);
			} else if (uri.toString().startsWith(CACHE_ID_BASE))
			{
				final long id = Long.parseLong(uri.toString().substring(CACHE_ID_BASE.length()));
				String whereClause = CacheEntry.FULL_ID + " = " + Long.toString(id);
				
				if (! TextUtils.isEmpty(selection))
				{
					whereClause += " AND " + selection;
				}
				
				ret = db.delete(CacheEntry.TABLE_NAME, whereClause, selectionArgs);
				
				resolver.notifyChange(URI_CACHE, null);
				resolver.notifyChange(URI_TRACKS, null);
			} else {
				throw new UnsupportedOperationException("Not yet implemented");
			}
		}
		
		return ret;
	}

	@Override
	public String getType(Uri uri) {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		synchronized(this) {
			SQLiteDatabase db = getDbHelper(getContext()).getWritableDatabase();
			ContentResolver resolver = getContext().getContentResolver();
			Uri ret;
	
			if (URI_DOWNLOADS.equals(uri))
			{
				long id = db.insert(DownloadEntry.TABLE_NAME, null, values);
				
				resolver.notifyChange(URI_DOWNLOADS, null);
				resolver.notifyChange(URI_TRACKS, null);
				
				ret = Uri.parse(DOWNLOAD_ID_BASE + Long.toString(id));
			} else if (URI_CACHE.equals(uri))
			{
				long id = db.insert(CacheEntry.TABLE_NAME, null, values);
				
				resolver.notifyChange(URI_CACHE, null);
				resolver.notifyChange(URI_TRACKS, null);
				
				ret = Uri.parse(CACHE_ID_BASE + Long.toString(id));
			} else 
			{
				throw new UnsupportedOperationException("Unknown URI");
			}
			
			return ret;
		}
	}

	@Override
	public boolean onCreate() {
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SQLiteDatabase db = getDbHelper(getContext()).getReadableDatabase();
		ContentResolver resolver = getContext().getContentResolver();
		Cursor result = null;
		
		synchronized(this) {
			if (URI_ARTISTS.equals(uri))
			{
				Log.d(LOG_TAG, "Query all artists");
				
				result = db.query(
						ArtistAlbumJoin.TABLE_NAME, ArtistAlbumJoin.PROJECTION,
						null, null, ArtistAlbumJoin.GROUP_BY, null,
						ArtistAlbumJoin.SORT_ORDER, null);
				
				result.setNotificationUri(resolver, URI_ARTISTS);
			} else if (uri.toString().startsWith(ARTIST_ID_BASE)) {
				final long id = Long.parseLong(uri.toString().substring(ARTIST_ID_BASE.length()));
				
				Log.d(LOG_TAG, "Query artist id = " + id);
				
				result = db.query(
						ArtistAlbumJoin.TABLE_NAME, ArtistAlbumJoin.PROJECTION,
						ArtistEntry.FULL_ID + " IS ?",
						new String[] { String.valueOf(id) }, ArtistAlbumJoin.GROUP_BY, null,
						ArtistAlbumJoin.SORT_ORDER, null);
				
				result.setNotificationUri(resolver, URI_ARTISTS);
			} else if (uri.toString().startsWith(ARTIST_NAME_BASE)) {
				String name = uri.toString().substring(ARTIST_NAME_BASE.length());
				
				Log.d(LOG_TAG, "Query artist name = " + name);
				
				result = db.query(
						ArtistAlbumJoin.TABLE_NAME, ArtistAlbumJoin.PROJECTION,
						"LOWER(" + ArtistEntry.FULL_NAME + ") LIKE LOWER(?)",
						new String[] { "%" + name + "%" }, ArtistAlbumJoin.GROUP_BY, null,
						ArtistAlbumJoin.SORT_ORDER, null);
				
				result.setNotificationUri(resolver, URI_ARTISTS);
			} else if (URI_ALBUMS.equals(uri))
			{
				Log.d(LOG_TAG, "Query all albums");
				
				result = db.query(
						AlbumArtistJoin.TABLE_NAME, AlbumArtistJoin.PROJECTION,
						null, null, null, null, AlbumArtistJoin.SORT_ORDER, null);
				
				result.setNotificationUri(resolver, URI_ALBUMS);
			} else if (uri.toString().startsWith(ALBUM_ID_BASE)) {
				final long id = Long.parseLong(uri.toString().substring(ALBUM_ID_BASE.length()));
				
				Log.d(LOG_TAG, "Query album id = " + id);
				
				result = db.query(
						AlbumArtistJoin.TABLE_NAME, AlbumArtistJoin.PROJECTION,
						AlbumEntry.FULL_ID + " IS ?",
						new String[] { String.valueOf(id) }, null, null,
						AlbumArtistJoin.SORT_ORDER, null);
				
				result.setNotificationUri(resolver, URI_ALBUMS);
			} else if (uri.toString().startsWith(ALBUM_TITLE_BASE)) {
				String name = uri.toString().substring(ALBUM_TITLE_BASE.length());
				
				Log.d(LOG_TAG, "Query album name = " + name);
	
				result = db.query(
						AlbumArtistJoin.TABLE_NAME, AlbumArtistJoin.PROJECTION,
						"LOWER(" + AlbumEntry.FULL_TITLE + ") LIKE LOWER(?)",
						new String[] { "%" + name + "%" }, null, null,
						AlbumArtistJoin.SORT_ORDER, null);
				
				result.setNotificationUri(resolver, URI_ALBUMS);
			} else if (uri.toString().startsWith(ALBUM_ARTIST_BASE))
			{
				final long id = Long.parseLong(uri.toString().substring(ALBUM_ARTIST_BASE.length()));
				
				Log.d(LOG_TAG, "Query album artist = " + id);
				
				result = db.query(
						AlbumArtistJoin.TABLE_NAME, AlbumArtistJoin.PROJECTION,
						AlbumEntry.FULL_ARTIST_ID + " IS ?",
						new String[] { String.valueOf(id) }, null, null,
						AlbumArtistJoin.SORT_ORDER, null);
				
				result.setNotificationUri(resolver, URI_ALBUMS);
			} else if (URI_TRACKS.equals(uri)) {
				Log.d(LOG_TAG, "Query all tracks");
				
				result = db.query(
						TrackDownloadCacheJoin.TABLE_NAME,
						TrackDownloadCacheJoin.PROJECTION,
						null, null, null, null, 
						TrackDownloadCacheJoin.SORT_ORDER, null);
				
				result.setNotificationUri(resolver, URI_TRACKS);
			} else if (uri.toString().startsWith(TRACK_ID_BASE)) {
				final long id = Long.parseLong(uri.toString().substring(TRACK_ID_BASE.length()));
				
				Log.d(LOG_TAG, "Query track id = " + id);
				
				result = db.query(
						TrackDownloadCacheJoin.TABLE_NAME,
						TrackDownloadCacheJoin.PROJECTION,
						TrackEntry.FULL_ID + " IS ?",
						new String[] { String.valueOf(id) }, null, null,
						TrackDownloadCacheJoin.SORT_ORDER, null);
				
				result.setNotificationUri(resolver, URI_TRACKS);
			} else if (uri.toString().startsWith(TRACK_TITLE_BASE)) {
				String name = uri.toString().substring(TRACK_TITLE_BASE.length());
				
				Log.d(LOG_TAG, "Query track name = " + name);
	
				result = db.query(
						TrackDownloadCacheJoin.TABLE_NAME,
						TrackDownloadCacheJoin.PROJECTION,
						"LOWER(" + TrackEntry.FULL_TITLE + ") LIKE LOWER(?)",
						new String[] { "%" + name + "%" }, null, null,
						TrackDownloadCacheJoin.SORT_ORDER, null);
				
				result.setNotificationUri(resolver, URI_TRACKS);
			} else if (uri.toString().startsWith(TRACK_ALBUM_BASE)) {
				String album_id = uri.toString().substring(TRACK_ALBUM_BASE.length());
				
				Log.d(LOG_TAG, "Query track album = " + album_id);
				
				result = db.query(
						TrackDownloadCacheJoin.TABLE_NAME,
						TrackDownloadCacheJoin.PROJECTION,
						TrackEntry.FULL_ALBUM_ID + " IS ?",
						new String[] { album_id }, null, null,
						TrackDownloadCacheJoin.SORT_ORDER, null);
				
				result.setNotificationUri(resolver, URI_TRACKS);
			} else if (uri.toString().startsWith(TRACK_ARTIST_BASE)) {
				String artist_id = uri.toString().substring(TRACK_ARTIST_BASE.length());
				
				Log.d(LOG_TAG, "Query track artist = " + artist_id);
				
				result = db.query(
						TrackDownloadCacheJoin.TABLE_NAME,
						TrackDownloadCacheJoin.PROJECTION,
						TrackEntry.FULL_ARTIST_ID + " IS ?",
						new String[] { artist_id }, null, null,
						TrackDownloadCacheJoin.SORT_ORDER, null);
				
				result.setNotificationUri(resolver, URI_TRACKS);
			} else if (URI_DOWNLOADS.equals(uri))
			{
				Log.d(LOG_TAG, "Query all downloads");
				
				result = db.query(
						DownloadEntry.TABLE_NAME,
						(projection == null) ? DownloadEntry.PROJECTION : projection,
						selection, selectionArgs, null, null,
						(sortOrder == null) ? DownloadEntry.SORT_ORDER : sortOrder,
						null);
				
				result.setNotificationUri(resolver, URI_DOWNLOADS);
			} else if (uri.toString().startsWith(DOWNLOAD_ID_BASE)) {
				String download_id = uri.toString().substring(DOWNLOAD_ID_BASE.length());
				
				Log.d(LOG_TAG, "Query download id = " + download_id);
				
				result = db.query(
						DownloadEntry.TABLE_NAME, DownloadEntry.PROJECTION,
						DownloadEntry.FULL_ID + " IS ?",
						new String[] { download_id }, null, null,
						DownloadEntry.SORT_ORDER, null);
				
				result.setNotificationUri(resolver, URI_DOWNLOADS);
			} else if (URI_CACHE.equals(uri))
			{
				result = db.query(CacheEntry.TABLE_NAME,
						(projection == null) ? CacheEntry.PROJECTION : projection,
						selection, selectionArgs, null, null,
						(sortOrder == null) ? CacheEntry.SORT_ORDER : sortOrder, null);
				
				result.setNotificationUri(resolver, URI_CACHE);
			} else if (uri.toString().startsWith(CACHE_ID_BASE)) {
				String cache_id = uri.toString().substring(CACHE_ID_BASE.length());
				
				result = db.query(CacheEntry.TABLE_NAME,
						(projection == null) ? CacheEntry.PROJECTION : projection,
						CacheEntry.FULL_ID + " IS ?",
						new String[] { cache_id }, null, null,
						CacheEntry.SORT_ORDER, null);
				
				result.setNotificationUri(resolver, URI_CACHE);
			} else if (URI_DOWNLOADDETAILS.equals(uri))
			{
				result = db.query(DownloadTrackEntry.TABLE_NAME, 
						(projection == null) ? DownloadTrackEntry.PROJECTION : projection,
						selection, selectionArgs, null, null,
						(sortOrder == null) ? DownloadEntry.SORT_ORDER : sortOrder, null);
				
				result.setNotificationUri(resolver, URI_DOWNLOADS);
			} else {
		        throw new UnsupportedOperationException("Unknown URI");
		    }
		}
		
		return result;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		synchronized(this) {
			SQLiteDatabase db = getDbHelper(getContext()).getWritableDatabase();
			ContentResolver resolver = getContext().getContentResolver();
			int ret;
			
			if (uri.toString().startsWith(DOWNLOAD_ID_BASE))
			{
				String download_id = uri.toString().substring(DOWNLOAD_ID_BASE.length());
				String whereString = DownloadEntry.FULL_ID + " IS " + download_id;
				
				if (! TextUtils.isEmpty(selection))
				{
					whereString += " AND (" + selection + ")";
				}
				
				Log.d(LOG_TAG, "Updating download " + download_id);
				
				ret = db.update(DownloadEntry.TABLE_NAME, values, whereString, selectionArgs);
				
				resolver.notifyChange(URI_DOWNLOADS, null);
				resolver.notifyChange(URI_TRACKS, null);
			} else if (URI_CACHE.equals(uri))
			{
				ret = db.update(CacheEntry.TABLE_NAME, values, selection, selectionArgs);
				
				resolver.notifyChange(URI_CACHE, null);
				resolver.notifyChange(URI_TRACKS, null);
			} else
			{
				throw new UnsupportedOperationException("Not yet implemented");
			}
			
			return ret;
		}
	}
}
