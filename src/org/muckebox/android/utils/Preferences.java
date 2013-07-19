package org.muckebox.android.utils;

import org.muckebox.android.Muckebox;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Preferences {
	static SharedPreferences mSharedPref = null;
	
	public static SharedPreferences getPreferences() {
		if (mSharedPref == null)
			mSharedPref = PreferenceManager.getDefaultSharedPreferences(Muckebox.getAppContext());

		return mSharedPref;
	}
	
	public static boolean getTranscodingEnabled() {
		return getPreferences().getBoolean("transcoding_enable", true);
	}
	
	public static String getTranscodingType() {
		return getPreferences().getString("transcoding_type", "ogg");
	}
	
	public static String getTranscodingQuality() {
		return getPreferences().getString("transcoding_quality", "high");
	}
	
	public static boolean isCachingEnabled() {
		return getPreferences().getBoolean("caching_enable", true);
	}
	
	public static int getCurrentPlaylistId() {
	    return getPreferences().getInt("current_playlist_id", 0);
	}
	
	public static void setCurrentPlaylistItem() {
	}
	
	public static boolean getCachingEnabled() {
	    return getPreferences().getBoolean("caching_enable", true);
	}
	
	public static int getCacheSize() {
	    return Integer.parseInt(getPreferences().getString("cache_size", "500")) * (1024 * 1024);
	}
}
