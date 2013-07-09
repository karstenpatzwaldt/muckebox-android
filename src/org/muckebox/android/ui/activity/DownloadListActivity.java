package org.muckebox.android.ui.activity;

import org.muckebox.android.R;
import org.muckebox.android.ui.fragment.DownloadListFragment;

import android.os.Bundle;
import android.app.Activity;
import android.app.FragmentTransaction;

public class DownloadListActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_downloadlist);
        
        FragmentTransaction tf = getFragmentManager().beginTransaction();
        tf.add(R.id.fragment_container, new DownloadListFragment());
        tf.commit();
    }
}
