/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.photos;

import android.app.Activity;
import android.os.Bundle;
import com.android.photos.views.TiledImageView;


public class FullscreenViewer extends Activity {

    private TiledImageView mTextureView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String path = getIntent().getData().toString();
        mTextureView = new TiledImageView(this);
        mTextureView.setTileSource(new BitmapRegionTileSource(this, path, 0, 0), null);
        setContentView(mTextureView);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mTextureView.destroy();
    }

}
