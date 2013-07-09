package org.muckebox.android.db;

import org.muckebox.android.db.MuckeboxContract.AlbumEntry;
import org.muckebox.android.db.MuckeboxContract.ArtistEntry;
import org.muckebox.android.db.MuckeboxContract.CacheEntry;
import org.muckebox.android.db.MuckeboxContract.DownloadEntry;
import org.muckebox.android.db.MuckeboxContract.TrackEntry;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MuckeboxDbHelper extends SQLiteOpenHelper {
	private static final String TEXT_TYPE = " TEXT ";
	private static final String INT_TYPE = " INTEGER ";
	private static final String PRIMARY_KEY = " PRIMARY KEY ";
	private static final String SEP = ",";
	private static final String DEFAULT_NOW = " DEFAULT CURRENT_TIMESTAMP";
	
	private static final String SQL_CREATE_ARTIST_TABLE = 
			"CREATE TABLE " + ArtistEntry.TABLE_NAME + " (" +
			ArtistEntry.SHORT_ID + INT_TYPE + PRIMARY_KEY + SEP +
			ArtistEntry.SHORT_NAME + TEXT_TYPE +
			")";
	private static final String SQL_DROP_ARTIST_TABLE =
			"DROP TABLE IF EXISTS " + ArtistEntry.TABLE_NAME;
	
	private static final String SQL_CREATE_ALBUM_TABLE =
			"CREATE TABLE " + AlbumEntry.TABLE_NAME + " (" +
			AlbumEntry.SHORT_ID + INT_TYPE + PRIMARY_KEY + SEP +
			AlbumEntry.SHORT_ARTIST_ID + INT_TYPE + SEP +
			AlbumEntry.SHORT_TITLE + TEXT_TYPE +
			")";
	private static final String SQL_DROP_ALBUM_TABLE =
			"DROP TABLE IF EXISTS " + AlbumEntry.TABLE_NAME;
	
	private static final String SQL_CREATE_TRACK_TABLE =
			"CREATE TABLE " + TrackEntry.TABLE_NAME + " (" +
			TrackEntry.SHORT_ID + INT_TYPE + PRIMARY_KEY + SEP +
			TrackEntry.SHORT_ALBUM_ID + INT_TYPE + SEP +
			TrackEntry.SHORT_ARTIST_ID + INT_TYPE + SEP +
			TrackEntry.SHORT_TITLE + TEXT_TYPE + SEP +
			TrackEntry.SHORT_DISCNUMBER + INT_TYPE + SEP +
			TrackEntry.SHORT_TRACKNUMBER + INT_TYPE + SEP +
			TrackEntry.SHORT_LABEL + TEXT_TYPE + SEP +
			TrackEntry.SHORT_CATALOGNUMBER + TEXT_TYPE + SEP +
			TrackEntry.SHORT_LENGTH + INT_TYPE + SEP +
			TrackEntry.SHORT_DISPLAY_ARTIST + TEXT_TYPE + SEP +
			TrackEntry.SHORT_DATE + TEXT_TYPE +
			")";
	private static final String SQL_DROP_TRACK_TABLE =
			"DROP TABLE IF EXISTS " + TrackEntry.TABLE_NAME;
	
	private static final String SQL_CREATE_DOWNLOAD_TABLE =
			"CREATE TABLE " + DownloadEntry.TABLE_NAME + " (" +
			DownloadEntry.SHORT_ID + INT_TYPE + PRIMARY_KEY + SEP +
			
			DownloadEntry.SHORT_TIMESTAMP + INT_TYPE + DEFAULT_NOW + SEP +
			DownloadEntry.SHORT_TRACK_ID + INT_TYPE + SEP +
			
			DownloadEntry.SHORT_TRANSCODE + INT_TYPE + SEP +
			DownloadEntry.SHORT_TRANSCODING_TYPE + TEXT_TYPE + SEP +
			DownloadEntry.SHORT_TRANSCODING_QUALITY + TEXT_TYPE + SEP +
			
			DownloadEntry.SHORT_PIN_RESULT + INT_TYPE + SEP +
			DownloadEntry.SHORT_START_NOW + INT_TYPE + SEP +
			
			DownloadEntry.SHORT_STATUS + INT_TYPE + " DEFAULT '" +
				DownloadEntry.STATUS_VALUE_QUEUED + "'" +
			")";
	private static final String SQL_DROP_DOWNLOAD_TABLE =
			"DROP TABLE IF EXISTS " + DownloadEntry.TABLE_NAME;
	
	private static final String SQL_CREATE_CACHE_TABLE =
			"CREATE TABLE " + CacheEntry.TABLE_NAME + " (" +
			CacheEntry.SHORT_ID + INT_TYPE + PRIMARY_KEY + SEP +
			
			CacheEntry.SHORT_TRACK_ID + INT_TYPE + SEP +
			
			CacheEntry.SHORT_FILENAME + TEXT_TYPE + SEP +
			CacheEntry.SHORT_MIME_TYPE + TEXT_TYPE + SEP +
			CacheEntry.SHORT_SIZE + INT_TYPE + SEP +
			CacheEntry.SHORT_TIMESTAMP + INT_TYPE + DEFAULT_NOW + SEP +
			
			CacheEntry.SHORT_TRANSCODED + INT_TYPE + SEP +
			CacheEntry.SHORT_TRANCODING_TYPE + TEXT_TYPE + SEP +
			CacheEntry.SHORT_TRANSCODING_QUALITY + TEXT_TYPE + SEP +
			
			CacheEntry.SHORT_PINNED + INT_TYPE +
			")";
	private static final String SQL_DROP_CACHE_TABLE =
			"DROP TABLE IF EXISTS " + CacheEntry.TABLE_NAME;
	
	private static final int DB_VERSION = 2;
	private static final String DB_NAME = "muckebox.db";
	
	public MuckeboxDbHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(SQL_CREATE_ARTIST_TABLE);
		db.execSQL(SQL_CREATE_ALBUM_TABLE);
		db.execSQL(SQL_CREATE_TRACK_TABLE);
		db.execSQL(SQL_CREATE_DOWNLOAD_TABLE);
		db.execSQL(SQL_CREATE_CACHE_TABLE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL(SQL_DROP_ARTIST_TABLE);
		db.execSQL(SQL_DROP_ALBUM_TABLE);
		db.execSQL(SQL_DROP_TRACK_TABLE);
		db.execSQL(SQL_DROP_DOWNLOAD_TABLE);
		db.execSQL(SQL_DROP_CACHE_TABLE);
		
		onCreate(db);
	}
	
	@Override
	public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		onUpgrade(db, oldVersion, newVersion);
	}
}
