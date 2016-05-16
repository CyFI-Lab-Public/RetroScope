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

package android.mediastress.cts;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.android.cts.mediastress.R;

public class MediaFrameworkTest extends Activity implements SurfaceHolder.Callback {
    private static String TAG = "MediaFrameworkTest";
    private static SurfaceView mSurfaceView;

    private PowerManager.WakeLock mWakeLock = null;

    public static SurfaceView getSurfaceView() {
        return mSurfaceView;
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.surface_view);
        mSurfaceView = (SurfaceView)findViewById(R.id.surface_view);
        mSurfaceView.getHolder().addCallback(this);

        //Acquire the full wake lock to keep the device up
        PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "MediaFrameworkTest");
        mWakeLock.setReferenceCounted(false);
    }

    @Override
    protected void onResume() {
        Log.v(TAG, "onResume, acquire wakelock");
        super.onResume();
        mWakeLock.acquire();
    }

    @Override
    protected void onPause() {
        Log.v(TAG, "onPause, release wakelock");
        mWakeLock.release();
        super.onPause();
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        //Can do nothing in here. The test case will fail if the surface destroyed.
        Log.v(TAG, "Test application surface destroyed");
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        //Do nothing in here. Just print out the log
        Log.v(TAG, "Test application surface changed");
    }

    public void surfaceCreated(SurfaceHolder holder) {
        holder.addCallback(this);
        Log.v(TAG, "Test application surface created");
    }

    public void startPlayback(String filename){
      String mimetype = "audio/mpeg";
      Uri path = Uri.parse(filename);
      Intent intent = new Intent(Intent.ACTION_VIEW);
      intent.setDataAndType(path, mimetype);
      startActivity(intent);
    }
}
