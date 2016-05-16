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

package com.android.gallery3d.data;

import com.android.gallery3d.R;
import com.android.gallery3d.app.GalleryApp;

public class UnlockImage extends ActionImage {
    @SuppressWarnings("unused")
    private static final String TAG = "UnlockImage";

    public UnlockImage(Path path, GalleryApp application) {
        super(path, application, R.drawable.placeholder_locked);
    }

    @Override
    public int getSupportedOperations() {
        return super.getSupportedOperations() | SUPPORT_UNLOCK;
    }
}
