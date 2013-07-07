package org.muckebox.android.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONException;
import org.muckebox.android.utils.Preferences;

import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

public class NetHelper {
	private static final String LOG_TAG = "NetHelper";
	
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
	
	public static String getApiUrl(String query, String extra,
			String[] keys, String[] values) throws IOException {
		SharedPreferences prefs = Preferences.getPreferences();
		
		Uri.Builder builder = Uri.parse(
				String.format("http://%s:%s",
						prefs.getString("server_address", ""),
						prefs.getString("server_port", "2342"))).buildUpon();
		
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
		
		conn.setReadTimeout(10000);
		conn.setConnectTimeout(1000);
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
