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

package org.muckebox.android.ui.activity;

import java.io.IOException;

import javax.net.ssl.SSLException;

import org.apache.http.auth.AuthenticationException;
import org.json.JSONException;
import org.json.JSONObject;
import org.muckebox.android.R;
import org.muckebox.android.net.ApiHelper;
import org.muckebox.android.ui.utils.HeightEvaluator;
import org.muckebox.android.ui.widgets.ImageViewRotater;
import org.muckebox.android.utils.Preferences;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.CheckBox;
import android.widget.TextView;

public class WizardActivity extends Activity {
    private static final int SPEC = MeasureSpec.makeMeasureSpec(4096, MeasureSpec.AT_MOST);
    
    MenuItem mDoneItem;
    MenuItem mTestItem;
    
    View mExtraContainer;
    int mExtraContainerHeight;
    
    View mExpandButton;
    
    TextView mServerText;
    TextView mPasswordText;
    TextView mPortText;
    CheckBox mSslCheckbox;
    
    TextView mResultText;
    
    private class TestConnectionTask extends AsyncTask<Void, Void, Integer> {
        @Override
        protected void onPreExecute() {
            mTestItem.setActionView(
                ImageViewRotater.getRotatingImageView(
                        WizardActivity.this, R.layout.action_view_refresh));
        }

        @Override
        protected Integer doInBackground(Void... params) {
            try {
                JSONObject response = ApiHelper.callApiForObject("ping");
                
                if (! response.getBoolean("pong"))
                    throw new JSONException("mountpoint");
            } catch (SSLException e) {
                return R.string.wizard_error_ssl;
            } catch (AuthenticationException e) {
                return R.string.wizard_error_password;
            } catch (IOException e) {
                return R.string.wizard_error_connection;
            } catch (JSONException e) {
                return R.string.wizard_error_mount_point;
            }
            
            return R.string.wizard_success;
        }
        
        @Override
        protected void onPostExecute(Integer result) {
            mResultText.setText(getText(result));
            mTestItem.getActionView().clearAnimation();
            mTestItem.setActionView(null);
            
            if (result == R.string.wizard_success) {
                mDoneItem.setVisible(true);
            } 
        }
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_wizard);
        
        mExtraContainer = findViewById(R.id.wizard_extra_container);
        mExtraContainer.measure(SPEC, SPEC);
        mExtraContainerHeight = mExtraContainer.getMeasuredHeight();
        
        LayoutParams params = mExtraContainer.getLayoutParams();
        params.height = 0;
        mExtraContainer.setLayoutParams(params);
        
        mExpandButton = findViewById(R.id.wizard_expand_button);
        mExpandButton.setOnClickListener(
            new OnClickListener() {
                public void onClick(View v) {
                    AnimatorSet animatorSet = new AnimatorSet();
                    
                    animatorSet.play(ValueAnimator.ofObject(
                        new HeightEvaluator(mExtraContainer), 0, mExtraContainerHeight));
                    animatorSet.play(ValueAnimator.ofObject(
                        new HeightEvaluator(mExpandButton), mExpandButton.getHeight(), 0));
                    
                    animatorSet.start();
                }
            });
        
        mServerText = (TextView) findViewById(R.id.wizard_server_text);
        mPasswordText = (TextView) findViewById(R.id.wizard_server_password);
        mPortText = (TextView) findViewById(R.id.wizard_port_text);
        mSslCheckbox = (CheckBox) findViewById(R.id.wizard_ssl_enabled);
        
        mResultText = (TextView) findViewById(R.id.wizard_result_text);
        
        readFromPreferences();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        
        inflater.inflate(R.menu.wizard, menu);
        
        mDoneItem = menu.findItem(R.id.wizard_action_done);
        mTestItem = menu.findItem(R.id.wizard_action_test);
        
        mDoneItem.setVisible(false);
        
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item == mTestItem) {
            writeToPreferences();
        
            new TestConnectionTask().execute();
            
            return true;
        } else if (item == mDoneItem) {
            Preferences.setWizardCompleted();
            
            Intent intent = new Intent(this, MuckeboxActivity.class);
            startActivity(intent);
            
            return true;
        }
        
        return false;
    }
    
    private void readFromPreferences() {
        mServerText.setText(Preferences.getServerAddress());
        mPasswordText.setText(Preferences.getServerPassword());
        mPortText.setText(Integer.toString(Preferences.getServerPort()));
        mSslCheckbox.setChecked(Preferences.getSSLEnabled());
    }
    
    private void writeToPreferences() {
        Preferences.setServerAddress(mServerText.getText().toString());
        Preferences.setServerPassword(mPasswordText.getText().toString());
        Preferences.setServerPort(Integer.parseInt(mPortText.getText().toString()));
        Preferences.setSSLEnabled(mSslCheckbox.isChecked());
    }
}
