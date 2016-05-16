/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.test.ActivityInstrumentationTestCase2;

public class VideoEditorTest extends ActivityInstrumentationTestCase2<MediaStubActivity> {

    public VideoEditorTest() {
        super(MediaStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        //setup for each test case.
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        //Test case clean up.
        super.tearDown();
    }
}
