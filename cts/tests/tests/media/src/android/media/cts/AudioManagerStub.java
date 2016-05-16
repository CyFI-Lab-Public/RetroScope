/*
 * Copyright (C) 2009 The Android Open Source Project
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

package android.media.cts;

import com.android.cts.media.R;

import android.app.Activity;
import android.app.cts.CTSResult;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.AudioSystem;
import android.media.MediaPlayer;
import android.os.Bundle;

public class AudioManagerStub extends Activity {
    private final int MP3_TO_PLAY = R.raw.testmp3;
    private MediaPlayer mMediaPlayer;
    private AudioManager mAudioManager;
    private static CTSResult mCTSResult;

    public static void setCTSResult(CTSResult cr) {
        mCTSResult = cr;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mMediaPlayer = MediaPlayer.create(this, MP3_TO_PLAY);
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setLooping(false);
        mMediaPlayer.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            for (int i = 0; i < AudioSystem.getNumStreamTypes(); i++) {
                mAudioManager.setStreamMute(i, false);
                mAudioManager.setStreamSolo(i, false);
            }
        } catch (Exception e) {
            mCTSResult.setResult(CTSResult.RESULT_FAIL);
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            for (int i = 0; i < AudioSystem.getNumStreamTypes(); i++) {
                mAudioManager.setStreamMute(i, true);
                mAudioManager.setStreamSolo(i, true);
            }
        } catch (Exception e) {
            mCTSResult.setResult(CTSResult.RESULT_FAIL);
            finish();
            return;
        }

        Intent intent = new Intent();
        intent.setClass(this, AudioManagerStubHelper.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mMediaPlayer.stop();
        mMediaPlayer.release();
        mCTSResult.setResult(CTSResult.RESULT_OK);
        finish();
    }
}
