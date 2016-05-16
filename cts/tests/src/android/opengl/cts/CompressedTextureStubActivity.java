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
package android.opengl.cts;

import com.android.cts.stub.R;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;

public class CompressedTextureStubActivity extends Activity {

    private static final String TAG = "CompressedTextureStubActivity";

    protected Resources mResources;

    private CompressedTextureSurfaceView mCompressedTextureView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        String formatTest = extras.getString("TextureFormat", null);

        Log.i(TAG, "Testing format " + formatTest);

        mResources = getResources();

        CompressedTextureLoader.Texture compressed = null;

        BitmapFactory.Options optionsRGB = new BitmapFactory.Options();
        optionsRGB.inPreferredConfig = Bitmap.Config.RGB_565;
        optionsRGB.inScaled = false;
        Bitmap bitmap = BitmapFactory.decodeResource(mResources, R.raw.basetex, optionsRGB);

        if (formatTest.equals(CompressedTextureLoader.TEXTURE_ETC1)) {
            compressed = CompressedTextureLoader.createFromUncompressedETC1(bitmap);
        } else if (formatTest.equals(CompressedTextureLoader.TEXTURE_S3TC)) {
            compressed = CompressedTextureLoader.loadTextureDXT(mResources, R.raw.ddstex);
        } else if (formatTest.equals(CompressedTextureLoader.TEXTURE_ATC)) {
            compressed = CompressedTextureLoader.loadTextureATC(mResources, 0); //stub for now
        } else if (formatTest.equals(CompressedTextureLoader.TEXTURE_PVRTC)) {
            compressed = CompressedTextureLoader.loadTexturePVRTC(mResources, R.raw.pvrtex);
        }

        mCompressedTextureView = new CompressedTextureSurfaceView(this, bitmap, compressed);
        setContentView(mCompressedTextureView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCompressedTextureView.onResume();
    }

    public boolean getPassed() throws InterruptedException {
        return mCompressedTextureView.getTestPassed();
    }
}
