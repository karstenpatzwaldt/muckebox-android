package org.muckebox.android.services;

public interface PlayerListener {
	void onConnected();
	
	void onNewTrack(PlayerService.TrackInfo trackInfo);
	
	void onStartBuffering();
	
	void onStartPlaying();
	
	void onPlayPaused();
	void onPlayResumed();

	void onStopPlaying();
	
	void onPlayProgress(int secondsElapsed);
}
