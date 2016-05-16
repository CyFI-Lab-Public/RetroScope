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

/**
 * Test that uses an Activity with hardware acceleration explicitly disabled
 * and makes sure that all views are rendered using software acceleration.
 */
public class SoftwareAccelerationTest
        extends BaseAccelerationTest<SoftwareAcceleratedActivity> {

    public SoftwareAccelerationTest() {
        super(SoftwareAcceleratedActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
    }

    public void testIsHardwareAccelerated() {
        // Both of the views are not attached to a hardware accelerated window
        assertFalse(mHardwareView.isHardwareAccelerated());
        assertFalse(mSoftwareView.isHardwareAccelerated());
        assertFalse(mManualHardwareView.isHardwareAccelerated());
        assertFalse(mManualSoftwareView.isHardwareAccelerated());

        assertFalse(mHardwareView.isCanvasHardwareAccelerated());
        assertFalse(mSoftwareView.isCanvasHardwareAccelerated());
        assertFalse(mManualHardwareView.isCanvasHardwareAccelerated());
        assertFalse(mManualSoftwareView.isCanvasHardwareAccelerated());
    }
}
