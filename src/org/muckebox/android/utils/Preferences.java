/*   
 * Copyright 2013 Karsten Patzwaldt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
	
	public static String getServerAddress() {
	    return getPreferences().getString("server_address", "");
	}
	
	public static int getServerPort() {
	    return Integer.parseInt(getPreferences().getString("server_port", "2342"));
	}
	
	public static String getServerPassword() {
	    return getPreferences().getString("server_password", "");
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
	
	public static boolean getSSLEnabled() {
	    return getPreferences().getBoolean("ssl_enabled", true);
	}
}
