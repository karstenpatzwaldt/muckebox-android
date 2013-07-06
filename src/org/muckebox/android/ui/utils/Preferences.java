package org.muckebox.android.ui.utils;

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
}
