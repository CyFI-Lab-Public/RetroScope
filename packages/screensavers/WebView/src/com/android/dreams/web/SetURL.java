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

import android.util.Log;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

public class SetURL extends Activity {
    @Override
    public void onCreate(Bundle stuff) {
        super.onCreate(stuff);

        final Intent intent = getIntent();

        final String action = intent.getAction();
        String url = intent.getStringExtra(Intent.EXTRA_TEXT);

        if (url == null) {
            finish();
        } else if (Intent.ACTION_SEND.equals(action)) {
            set(url);
            finish();
        }
    }

    protected void set(String url) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final Editor editor = prefs.edit();
        editor.putString("url", url);
        editor.putBoolean("interactive", false);
        editor.commit();

        Toast.makeText(this, "WebView dream URL set to: " + url, Toast.LENGTH_SHORT).show();
    }
}
