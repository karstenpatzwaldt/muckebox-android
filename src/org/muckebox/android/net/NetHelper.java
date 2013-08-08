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

package org.muckebox.android.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONException;
import org.muckebox.android.utils.Preferences;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.util.Log;

public class NetHelper {
	private static final String LOG_TAG = "NetHelper";
	
	private static final int TIMEOUT = 30 * 1000;
	
	public static JSONArray callApi(String query, String id, String[] keys, String[] values) throws IOException, JSONException {
		String str_url = getApiUrl(query, id, keys, values);
		
		Log.i(LOG_TAG, "Connecting to " + str_url);
		
		HttpURLConnection conn = getDefaultConnection(new URL(str_url));
		
		try
		{
			String response = getResponseAsString(conn);
			
			return new JSONArray(response);
		} finally {
			conn.disconnect();
		}
	}
	
	public static JSONArray callApi(String query, String id) throws IOException, JSONException
	{
		return callApi(query, id, null, null);
	}
	
	public static JSONArray callApi(String query) throws IOException, JSONException
	{
		return callApi(query, null, null, null);
	}
	
	@SuppressLint("DefaultLocale")
	public static String getApiUrl(String query, String extra,
			String[] keys, String[] values) throws IOException {
		Uri.Builder builder = Uri.parse(
				String.format("http%s://%s:%d",
				    Preferences.getSSLEnabled() ? "s" : "",
				    Preferences.getServerAddress(),
					Preferences.getServerPort())).buildUpon();
		
		builder.path(String.format("/api/%s", query));
		
		if (extra != null)
			builder.appendPath(extra);
		
		if ((keys != null) && (values != null) && (keys.length > 0))
		{
			if (keys.length != values.length)
				throw new IOException();
			
			for (int i = 0; i < keys.length; ++i)
			{
				builder.appendQueryParameter(keys[i], values[i]);
			}
		}
		
		return builder.build().toString();
	}
	
	public static HttpURLConnection getDefaultConnection(URL url) throws IOException {
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		
		conn.setReadTimeout(TIMEOUT);
		conn.setConnectTimeout(TIMEOUT);
		conn.setRequestMethod("GET");
		conn.setDoInput(true);
		conn.connect();
		
		return conn;
	}
	
	public static String getResponseAsString(HttpURLConnection conn) throws IOException {
		int response = conn.getResponseCode();
        Log.d(LOG_TAG, "HTTP Response " + response);
        
        if (response != 200) {
        	throw new IOException();
        }
        
        BufferedReader reader = new BufferedReader(
        		new InputStreamReader(conn.getInputStream(), "UTF-8"));
        StringBuilder str = new StringBuilder();
        char[] charBuf = new char[1024];
        
        int bytes_read;
        
        while ((bytes_read = reader.read(charBuf)) != -1)
        	str.append(charBuf, 0, bytes_read);
        
        return str.toString();
	}
}
