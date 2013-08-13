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

import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.muckebox.android.Muckebox;
import org.muckebox.android.R;
import org.muckebox.android.utils.Preferences;

public class MuckeboxHttpClient {
    static int DEFAULT_TIMEOUT = 15;
    
    private DefaultHttpClient mHttpClient;
    private BasicHttpContext mContext;
    
    private class PreemptiveAuth implements HttpRequestInterceptor {
        public void process(final HttpRequest request, final HttpContext context)
            throws HttpException, IOException {

            AuthState authState = (AuthState) context.getAttribute(
                ClientContext.TARGET_AUTH_STATE);

            if (authState.getAuthScheme() == null) {
                AuthScheme authScheme =
                    (AuthScheme) context.getAttribute("preemptive-auth");
                CredentialsProvider credsProvider =
                    (CredentialsProvider) context.getAttribute(ClientContext.CREDS_PROVIDER);
                HttpHost targetHost = (HttpHost) context.getAttribute(
                    ExecutionContext.HTTP_TARGET_HOST);
                
                if (authScheme != null) {
                    Credentials credentials = credsProvider.getCredentials(
                            new AuthScope(targetHost.getHostName(), targetHost.getPort()));
                    
                    if (credentials == null)
                        throw new HttpException("Credentials missing");
                    
                    authState.setAuthScheme(authScheme);
                    authState.setCredentials(credentials);
                }
            }
        }
    }
    
    public MuckeboxHttpClient() {
        mHttpClient = new DefaultHttpClient();
        
        addCredentials();
        configureTimeout();
        setUserAgent();
    }
    
    private void configureTimeout() {
        HttpParams httpParams = mHttpClient.getParams();
        
        HttpConnectionParams.setConnectionTimeout(httpParams, DEFAULT_TIMEOUT * 1000);
        HttpConnectionParams.setSoTimeout(httpParams, DEFAULT_TIMEOUT * 1000);
    }
    
    private void setUserAgent() {
        String userAgent = String.format("%s (%s %s; %s; %s)",
            Muckebox.getAppContext().getString(R.string.user_agent),
            android.os.Build.MANUFACTURER,
            android.os.Build.MODEL,
            android.os.Build.ID,
            android.os.Build.SERIAL);
        
        HttpProtocolParams.setUserAgent(mHttpClient.getParams(), userAgent);
    }
    
    private void addCredentials() {
        String password = Preferences.getServerPassword();
        
        if (password != null && password.length() > 0) {
            mHttpClient.getCredentialsProvider().setCredentials(
                new AuthScope(Preferences.getServerAddress(),
                    Preferences.getServerPort(),
                    "muckebox"),
                new UsernamePasswordCredentials("muckebox", password));
            
            mContext = new BasicHttpContext();
            
            BasicScheme basicAuth = new BasicScheme();
            mContext.setAttribute("preemptive-auth", basicAuth);

            mHttpClient.addRequestInterceptor(new PreemptiveAuth(), 0);
        }
    }
    
    public HttpResponse execute(HttpUriRequest request) throws ClientProtocolException, IOException {
        if (mContext != null)
            return mHttpClient.execute(request, mContext);
        else
            return mHttpClient.execute(request);
    }
    
    public void destroy() {
        mHttpClient.getConnectionManager().shutdown();
    }
}
