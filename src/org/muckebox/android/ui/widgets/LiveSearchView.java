package org.muckebox.android.ui.widgets;

import android.content.Context;
import android.widget.SearchView;


public class LiveSearchView extends SearchView {
    public LiveSearchView(Context context) {
        super(context);
    }

    @Override
    public void onActionViewCollapsed() {
        setQuery("", false);
        super.onActionViewCollapsed();
    }
}