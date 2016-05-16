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

package android.acceleration.cts;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.content.pm.FeatureInfo;
import android.test.ActivityInstrumentationTestCase2;
import android.view.View;

abstract class BaseAccelerationTest<B extends BaseAcceleratedActivity>
        extends ActivityInstrumentationTestCase2<B> {

    protected B mActivity;

    /** View with android:layerType="hardware" set */
    protected AcceleratedView mHardwareView;

    /** View with android:layerType="software" set */
    protected AcceleratedView mSoftwareView;

    /** View with setLayerType(HARDWARE) called */
    protected AcceleratedView mManualHardwareView;

    /** View with setLayerType(SOFTWARE) called */
    protected AcceleratedView mManualSoftwareView;

    BaseAccelerationTest(Class<B> clazz) {
        super(clazz);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
        mHardwareView = mActivity.getHardwareAcceleratedView();
        mSoftwareView = mActivity.getSoftwareAcceleratedView();
        mManualHardwareView = mActivity.getManualHardwareAcceleratedView();
        mManualSoftwareView = mActivity.getManualSoftwareAcceleratedView();
    }

    public void testNotAttachedView() {
        // Views that are not attached can't be attached to an accelerated window.
        View view = new View(mActivity);
        assertFalse(view.isHardwareAccelerated());
    }

    protected static int getGlEsVersion(Context context) {
        ActivityManager activityManager =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ConfigurationInfo configInfo = activityManager.getDeviceConfigurationInfo();
        if (configInfo.reqGlEsVersion != ConfigurationInfo.GL_ES_VERSION_UNDEFINED) {
            return getMajorVersion(configInfo.reqGlEsVersion);
        } else {
            return 1; // Lack of property means OpenGL ES version 1
        }
    }

    /** @see FeatureInfo#getGlEsVersion() */
    private static int getMajorVersion(int glEsVersion) {
        return ((glEsVersion & 0xffff0000) >> 16);
    }
}
