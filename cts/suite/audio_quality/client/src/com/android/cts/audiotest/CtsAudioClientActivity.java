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

package com.android.cts.audiotest;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;


public class CtsAudioClientActivity extends Activity {
    private AudioProtocol mProtocol = null;
    int mVolumeMusic;
    int mVolumeVoice;
    @Override
    protected void onPause() {
        try {
            mProtocol.stop();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mProtocol = null;
        setVolume(AudioManager.STREAM_MUSIC, mVolumeMusic);
        setVolume(AudioManager.STREAM_VOICE_CALL, mVolumeVoice);
        super.onPause();
    }

    @Override
    protected void onResume() {
        // set volume to max
        mVolumeMusic = setVolume(AudioManager.STREAM_MUSIC, -1);
        mVolumeVoice = setVolume(AudioManager.STREAM_VOICE_CALL, -1);
        mProtocol = new AudioProtocol();
        mProtocol.start();
        super.onResume();
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        KeyguardManager keyguardManager =
            (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        keyguardManager.newKeyguardLock("cts-audio").disableKeyguard();
    }

    /**
     * set volume to desired level
     * @param level target level, if -1, set to max
     * @return the original volume level
     */
    int setVolume(int stream, int level) {
        AudioManager mgr = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mgr.setStreamMute(stream, false);
        int original = mgr.getStreamVolume(AudioManager.STREAM_MUSIC);
        int targetLevel = level;
        if (level == -1) {
            targetLevel = mgr.getStreamMaxVolume(stream);
        }
        mgr.setStreamVolume(stream, targetLevel, 0);
        return original;
    }
}