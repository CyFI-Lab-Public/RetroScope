/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.dreams.web;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Handler;
import android.provider.Settings;
import android.service.dreams.DreamService;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.TextView;

public class Screensaver extends DreamService {
    private class LinkLauncher extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return true;
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        setContentView(R.layout.main);

        setFullscreen(true);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final String url = prefs.getString("url", "file:///android_asset/default.html");

        final boolean interactive = prefs.getBoolean("interactive", false);
        Log.v("WebViewDream", String.format("loading %s in %s mode",
                url, interactive ? "interactive" : "noninteractive"));
        setInteractive(interactive);

        WebView webview = (WebView) findViewById(R.id.webview);
        webview.setWebViewClient(new LinkLauncher());

        WebSettings webSettings = webview.getSettings();
        webSettings.setJavaScriptEnabled(true);
        
        webview.loadUrl(url);
    }
}
