package org.muckebox.android.services;

public interface PlayerListener {
	void onConnected();
	
	void onNewTrack(int id, String title, int duration);
	
	void onStartBuffering();
	
	void onStartPlaying();
	
	void onPlayPaused();
	void onPlayResumed();

	void onStopPlaying();
}
