/*
 * Copyright (C) 2011 The Android Open Source Project
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
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;

public class MediaPlayerSurfaceStubActivity extends Activity {

    private static final String TAG = "MediaPlayerSurfaceStubActivity";

    protected Resources mResources;

    private VideoSurfaceView mVideoView = null;
    private MediaPlayer mMediaPlayer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mResources = getResources();
        mMediaPlayer = new MediaPlayer();

        try {
            AssetFileDescriptor afd = mResources.openRawResourceFd(R.raw.testvideo);
            mMediaPlayer.setDataSource(
                    afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }

        mVideoView = new VideoSurfaceView(this, mMediaPlayer);
        setContentView(mVideoView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mVideoView.onResume();
    }

    public void playVideo() throws Exception {
        mVideoView.startTest();
    }

}
