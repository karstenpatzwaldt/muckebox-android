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

package org.muckebox.android.ui.utils;

public interface NavigationListener {
    public void onAllAlbumsSelected();
    public void onAllArtistsSelected();
    public void onRecentAlbumsSelected();
    
    public void onNowPlayingSelected();
    public void onAlbumSelected(long id, String title);
    
    public void onDownloadsSelected();
    public void onSettingsSelected();
}
