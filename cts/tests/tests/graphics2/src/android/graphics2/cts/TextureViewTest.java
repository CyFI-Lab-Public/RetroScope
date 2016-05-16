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

package android.graphics2.cts;

import android.graphics2.cts.TextureViewCameraActivity;
import android.hardware.Camera;
import android.test.ActivityInstrumentationTestCase2;


public class TextureViewTest extends ActivityInstrumentationTestCase2<TextureViewCameraActivity> {
    private static final long WAIT_TIMEOUT_IN_SECS = 10;
    private TextureViewCameraActivity mActivity;
    public TextureViewTest() {
        super(TextureViewCameraActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        if (Camera.getNumberOfCameras() < 1) {
            return;
        }
        mActivity = getActivity();
    }

    public void testTextureViewActivity() throws InterruptedException {
        if (Camera.getNumberOfCameras() < 1) {
            return;
        }
        assertTrue(mActivity.waitForCompletion(WAIT_TIMEOUT_IN_SECS));
    }

}

