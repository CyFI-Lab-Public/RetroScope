/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.htmlviewer;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.webkit.CookieSyncManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Wraps a WebView widget within an Activity. When launched, it uses the 
 * URI from the intent as the URL to load into the WebView. 
 * It supports all URLs schemes that a standard WebView supports, as well as
 * loading the top level markup using the file scheme.
 * The WebView default settings are used with the exception of normal layout 
 * is set.
 * This activity shows a loading progress bar in the window title and sets
 * the window title to the title of the content.
 *
 */
public class HTMLViewerActivity extends Activity {
    
    /*
     * The WebView that is placed in this Activity
     */
    private WebView mWebView;
    
    /*
     * As the file content is loaded completely into RAM first, set 
     * a limitation on the file size so we don't use too much RAM. If someone
     * wants to load content that is larger than this, then a content
     * provider should be used.
     */
    static final int MAXFILESIZE = 8096;
    
    static final String LOGTAG = "HTMLViewerActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Call createInstance() explicitly. createInstance() is called in 
        // BrowserFrame by WebView. As it is called in WebCore thread, it can 
        // happen after onResume() is called. To use getInstance() in onResume,
        // createInstance() needs to be called first.
        CookieSyncManager.createInstance(this);

        requestWindowFeature(Window.FEATURE_PROGRESS);
        
        mWebView = new WebView(this);
        setContentView(mWebView);
        
        // Setup callback support for title and progress bar
        mWebView.setWebChromeClient( new WebChrome() );
        
        // Configure the webview
        WebSettings s = mWebView.getSettings();
        s.setUseWideViewPort(true);
        s.setSupportZoom(true);
        s.setBuiltInZoomControls(true);
        s.setSavePassword(false);
        s.setSaveFormData(false);
        s.setBlockNetworkLoads(true);
        
        // Javascript is purposely disabled, so that nothing can be 
        // automatically run.
        s.setJavaScriptEnabled(false);
        
        // Restore a webview if we are meant to restore
        if (savedInstanceState != null) {
            mWebView.restoreState(savedInstanceState);
        } else {
            // Check the intent for the content to view
            Intent intent = getIntent();
            if (intent.getData() != null) {
                Uri uri = intent.getData();
                String contentUri = "file".equals(uri.getScheme())
                        ? FileContentProvider.BASE_URI + uri.getEncodedPath()
                        : uri.toString();
                String intentType = intent.getType();
                if (intentType != null) {
                    contentUri += "?" + intentType;
                }
                mWebView.loadUrl(contentUri);
            }
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        CookieSyncManager.getInstance().startSync(); 
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // the default implementation requires each view to have an id. As the
        // browser handles the state itself and it doesn't use id for the views,
        // don't call the default implementation. Otherwise it will trigger the
        // warning like this, "couldn't save which view has focus because the 
        // focused view XXX has no id".
        mWebView.saveState(outState);
    }

    @Override
    protected void onStop() {
        super.onStop();
        
        CookieSyncManager.getInstance().stopSync(); 
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mWebView.destroy();
    }
    
    class WebChrome extends WebChromeClient {
        
        @Override
        public void onReceivedTitle(WebView view, String title) {
            HTMLViewerActivity.this.setTitle(title);
        }
        
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            getWindow().setFeatureInt(
                    Window.FEATURE_PROGRESS, newProgress*100);
            if (newProgress == 100) {
                CookieSyncManager.getInstance().sync();
            }
        }
    }
    
}
