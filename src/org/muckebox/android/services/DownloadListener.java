package org.muckebox.android.services;

import java.nio.ByteBuffer;

public interface DownloadListener {
	void onDownloadStarted(long trackId, String mimeType);
	
	void onDataReceived(long trackId, ByteBuffer buffer);
	
	void onDownloadCanceled(long trackId);
	
	void onDownloadStopped(long trackId);
}
