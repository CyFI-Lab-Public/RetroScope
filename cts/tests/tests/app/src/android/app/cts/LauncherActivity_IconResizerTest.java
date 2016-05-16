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

package android.app.cts;

import com.android.cts.stub.R;


import android.app.LauncherActivity;
import android.app.LauncherActivity.IconResizer;
import android.graphics.drawable.Drawable;
import android.test.ActivityInstrumentationTestCase2;

public class LauncherActivity_IconResizerTest extends
        ActivityInstrumentationTestCase2<LauncherActivityStub> {

    private static final String PACKAGE = "com.android.cts.stub";
    private LauncherActivityStub mActivity;

    public LauncherActivity_IconResizerTest() {
        super(PACKAGE, LauncherActivityStub.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
    }

    public void testIconResizer() throws Throwable {
        final IconResizer ir = mActivity.new IconResizer();
        final Drawable d = mActivity.getResources().getDrawable(R.drawable.pass);
        assertNotNull(d);

        runTestOnUiThread(new Runnable() {
            public void run() {
                Drawable thumbNail = ir.createIconThumbnail(d);
                assertNotNull(thumbNail);
                // The size of the thumbnail is defined by inner R resource file
                // whose details are not open.
                assertTrue(thumbNail.getIntrinsicHeight() > 0);
                assertTrue(thumbNail.getIntrinsicWidth() > 0);
            }
        });
        getInstrumentation().waitForIdleSync();
    }
}
