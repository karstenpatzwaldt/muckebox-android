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

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.muckebox.android.utils.Preferences;

public class HttpHelper {
    static int DEFAULT_TIMEOUT = 15;
    
    public static void addCredentials(DefaultHttpClient httpClient) {
        String password = Preferences.getServerPassword();
        
        if (password != null && password.length() > 0) {
            httpClient.getCredentialsProvider().setCredentials(
                new AuthScope(Preferences.getServerAddress(),
                    Preferences.getServerPort(),
                    "muckebox"),
                new UsernamePasswordCredentials("muckebox", password));
        }
    }
    
    public static HttpClient getHttpClient() {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpParams httpParams = httpClient.getParams();
                
        addCredentials(httpClient);

        HttpConnectionParams.setConnectionTimeout(httpParams, DEFAULT_TIMEOUT * 1000);
        HttpConnectionParams.setSoTimeout(httpParams, DEFAULT_TIMEOUT * 1000);
        
        return httpClient;
    }
}
