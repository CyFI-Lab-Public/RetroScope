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

import android.os.Bundle;
import android.test.ActivityInstrumentationTestCase2;

/**
 */
public class CompressedTextureTest extends ActivityInstrumentationTestCase2<CompressedTextureStubActivity> {

    public CompressedTextureTest() {
        super("com.android.cts.stub", CompressedTextureStubActivity.class);
    }

    private void launchTest(String format) throws Exception {
        Bundle extras = new Bundle();
        extras.putString("TextureFormat", format);
        CompressedTextureStubActivity activity = launchActivity("com.android.cts.stub",
                CompressedTextureStubActivity.class, extras);
        activity.finish();
        assertTrue(activity.getPassed());
    }

    public void testTextureUncompressed() throws Exception {
        launchTest(CompressedTextureLoader.TEXTURE_UNCOMPRESSED);
    }

    public void testTextureETC1() throws Exception {
        launchTest(CompressedTextureLoader.TEXTURE_ETC1);
    }

    public void testTexturePVRTC() throws Exception {
        launchTest(CompressedTextureLoader.TEXTURE_PVRTC);
    }

    public void testTextureS3TC() throws Exception {
        launchTest(CompressedTextureLoader.TEXTURE_S3TC);
    }

    /*public void testTextureATC() throws Exception {
        launchTest(CompressedTextureLoader.TEXTURE_ATC);
    }*/
}
