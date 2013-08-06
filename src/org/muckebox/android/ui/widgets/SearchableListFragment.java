/*   
 * Copyright 2013 karsten
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

package org.muckebox.android.ui.widgets;

import org.muckebox.android.R;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.SearchView;
import android.widget.SearchView.OnCloseListener;
import android.widget.SearchView.OnQueryTextListener;

public abstract class SearchableListFragment extends RefreshableListFragment
implements OnQueryTextListener, OnCloseListener {
    SearchView mSearchView;
    String mCurFilter;
    
    protected abstract void reload();
    
    protected boolean isSearchable() {
        return true;
    }
    
    protected boolean hasFilter() {
        return mCurFilter != null;
    }
    
    protected String getFilter() {
        return mCurFilter;
    }

    public static class MySearchView extends SearchView {
        public MySearchView(Context context) {
            super(context);
        }

        @Override
        public void onActionViewCollapsed() {
            setQuery("", false);
            super.onActionViewCollapsed();
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MenuItem searchItem = menu.findItem(R.id.album_list_action_search);
        
        if (searchItem == null) {
            inflater.inflate(R.menu.searchable_list, menu);
            searchItem = menu.findItem(R.id.album_list_action_search);
        }
            
        if (! isSearchable())
        {
            searchItem.setVisible(false);
        } else
        {
            mSearchView = new MySearchView(getActivity());
            mSearchView.setOnQueryTextListener(this);
            mSearchView.setOnCloseListener(this);
            mSearchView.setIconifiedByDefault(true);
            
            searchItem.setActionView(mSearchView);
        }
        
        super.onCreateOptionsMenu(menu, inflater);
    }
   
    @Override
    public void onDestroyOptionsMenu()
    {
        if (mSearchView != null)
            mSearchView.setQuery(null, true);

        super.onDestroyOptionsMenu();
    }
  
    @Override
    public boolean onQueryTextChange(String newText) {
        String newFilter = !TextUtils.isEmpty(newText) ? newText : null;

        if (mCurFilter == null && newFilter == null) {
            return true;
        }
        
        if (mCurFilter != null && mCurFilter.equals(newFilter)) {
            return true;
        }
        
        mCurFilter = newFilter;
        
        reload();
        
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return true;
    }

    @Override
    public boolean onClose() {
        if (!TextUtils.isEmpty(mSearchView.getQuery())) {
            mSearchView.setQuery(null, true);
        }

        return true;
    }
}
