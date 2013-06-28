package org.muckebox.android.db;

import org.muckebox.android.db.MuckeboxContract.AlbumEntry;
import org.muckebox.android.db.MuckeboxContract.ArtistEntry;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MuckeboxDbHelper extends SQLiteOpenHelper {
	private static final String TEXT_TYPE = " TEXT ";
	private static final String INT_TYPE = " INTEGER ";
	private static final String PRIMARY_KEY = " PRIMARY KEY ";
	private static final String SEP = ",";
	
	private static final String SQL_CREATE_ARTIST_TABLE = 
			"CREATE TABLE " + ArtistEntry.TABLE_NAME + " (" +
			ArtistEntry._ID + INT_TYPE + PRIMARY_KEY + SEP +
			ArtistEntry.COLUMN_NAME_REMOTE_ID + INT_TYPE + SEP +
			ArtistEntry.COLUMN_NAME_NAME + TEXT_TYPE +
			")";
	private static final String SQL_DROP_ARTIST_TABLE =
			"DROP TABLE IF EXISTS " + ArtistEntry.TABLE_NAME;
	
	private static final String SQL_CREATE_ALBUM_TABLE =
			"CREATE TABLE " + AlbumEntry.TABLE_NAME + " (" +
			AlbumEntry._ID + INT_TYPE + PRIMARY_KEY + SEP +
			AlbumEntry.COLUMN_NAME_REMOTE_ID + INT_TYPE + SEP +
			AlbumEntry.COLUMN_NAME_REMOTE_ARTIST_ID + INT_TYPE + SEP +
			AlbumEntry.COLUMN_NAME_TITLE + TEXT_TYPE +
			")";
	private static final String SQL_DROP_ALBUM_TABLE =
			"DROP TABLE IF EXISTS " + AlbumEntry.TABLE_NAME;
	
	private static final int DB_VERSION = 3;
	private static final String DB_NAME = "muckebox.db";
	
	public MuckeboxDbHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(SQL_CREATE_ARTIST_TABLE);
		db.execSQL(SQL_CREATE_ALBUM_TABLE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL(SQL_DROP_ARTIST_TABLE);
		db.execSQL(SQL_DROP_ALBUM_TABLE);
		
		onCreate(db);
	}
	
	@Override
	public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		onUpgrade(db, oldVersion, newVersion);
	}

}
