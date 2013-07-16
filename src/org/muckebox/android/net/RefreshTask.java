package org.muckebox.android.net;

import org.muckebox.android.Muckebox;

import android.os.AsyncTask;
import android.widget.Toast;

public abstract class RefreshTask<Params> extends AsyncTask<Params, Void, Integer> {
    
    private static boolean mWasRunning = false;
    
    public interface Callbacks {
        void onRefreshStarted();
        
        void onRefreshFinished(boolean success);
    }
    
    public static boolean wasRunning() {
        return mWasRunning;
    }
    
	Callbacks mCallbacks = null;
	
	public RefreshTask<Params> setCallbacks(Callbacks callbacks) {
		mCallbacks = callbacks;
		
		return this;
	}
	
	@Override
	protected void onPreExecute() {
		if (mCallbacks != null)
			mCallbacks.onRefreshStarted();
		
		mWasRunning = true;
	}

	@Override
	protected void onPostExecute(Integer errorCode) {
		if (errorCode != null)
			Toast.makeText(Muckebox.getAppContext(),
					errorCode,
					Toast.LENGTH_SHORT).show();
		
		if (mCallbacks != null)
			mCallbacks.onRefreshFinished(errorCode == null);
	}
}
