/*
 * Copyright (C) 2010-2011 The Android Open Source Project
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

package com.android.musicfx;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.audiofx.AudioEffect;
import android.os.Bundle;
import android.util.Log;

import java.util.HashMap;

public class ControlPanelReceiver extends BroadcastReceiver {

    private final static String TAG = "MusicFXControlPanelReceiver";

    @Override
    public void onReceive(final Context context, final Intent intent) {

        Log.v(TAG, "onReceive");

        if ((context == null) || (intent == null)) {
            Log.w(TAG, "Context or intent is null. Do nothing.");
            return;
        }

        final String action = intent.getAction();
        final String packageName = intent.getStringExtra(AudioEffect.EXTRA_PACKAGE_NAME);
        final int audioSession = intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION,
                AudioEffect.ERROR_BAD_VALUE);

        Log.v(TAG, "Action: " + action);
        Log.v(TAG, "Package name: " + packageName);
        Log.v(TAG, "Audio session: " + audioSession);

        // check package name
        if (packageName == null) {
            Log.w(TAG, "Null package name");
            return;
        }

        // check audio session
        if ((audioSession == AudioEffect.ERROR_BAD_VALUE) || (audioSession < 0)) {
            Log.w(TAG, "Invalid or missing audio session " + audioSession);
            return;
        }

        // open audio session
        if (action.equals(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION)) {

            // retrieve the effect enabled state
            final boolean isGlobalEnabled = context.getSharedPreferences(packageName,
                    Context.MODE_PRIVATE).getBoolean(
                    ControlPanelEffect.Key.global_enabled.toString(),
                    ControlPanelEffect.GLOBAL_ENABLED_DEFAULT);

            ControlPanelEffect.openSession(context, packageName, audioSession);
        }

        // close audio session
        if (action.equals(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION)) {

            ControlPanelEffect.closeSession(context, packageName, audioSession);
        }

        // set params
        if (action.equals("AudioEffect.ACTION_SET_PARAM")) {
            final String param = intent.getStringExtra("AudioEffect.EXTRA_PARAM");

            if (param.equals("GLOBAL_ENABLED")) {
                final Boolean value = intent.getBooleanExtra("AudioEffect.EXTRA_VALUE", false);
                ControlPanelEffect.setParameterBoolean(context, packageName, audioSession,
                        ControlPanelEffect.Key.global_enabled, value);
            }
        }

        // get params
        if (action.equals("AudioEffect.ACTION_GET_PARAM")) {
            final String param = intent.getStringExtra("AudioEffect.EXTRA_PARAM");

            if (param.equals("GLOBAL_ENABLED")) {
                final Boolean value = ControlPanelEffect.getParameterBoolean(context, packageName,
                        audioSession, ControlPanelEffect.Key.global_enabled);
                final Bundle extras = new Bundle();
                extras.putBoolean("GLOBAL_ENABLED", value);
                setResultExtras(extras);
            }
        }
    }
}
