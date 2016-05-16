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

package android.textureview.cts;

import android.test.ActivityInstrumentationTestCase2;

public class TextureViewTest extends
        ActivityInstrumentationTestCase2<TextureViewTestActivity> {

    public TextureViewTest() {
        super(TextureViewTestActivity.class);
    }

    public void testTextureViewStress48Hz() {
        TextureViewTestActivity.mFrames = 600;
        TextureViewTestActivity.mDelayMs = 1000/48;
        if (!getActivity().waitForCompletion())
            fail("Did not complete 48Hz test.");
    }

    public void testTextureViewStress60Hz() {
        TextureViewTestActivity.mFrames = 600;
        TextureViewTestActivity.mDelayMs = 1000/60;
        if (!getActivity().waitForCompletion())
            fail("Did not complete 60Hz test.");
    }

    public void testTextureViewStress70Hz()  {
        TextureViewTestActivity.mFrames = 600;
        TextureViewTestActivity.mDelayMs = 1000/70;
        if (!getActivity().waitForCompletion())
            fail("Did not complete 70Hz test.");
    }

    public void testTextureViewStress200Hz() {
        TextureViewTestActivity.mFrames = 600;
        TextureViewTestActivity.mDelayMs = 1000/200;
        if (!getActivity().waitForCompletion())
            fail("Did not complete 200Hz test.");
    }

}
