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
 * Test that uses an Activity with hardware acceleration enabled.
 */
public class HardwareAccelerationTest
        extends BaseAccelerationTest<HardwareAcceleratedActivity> {

    public HardwareAccelerationTest() {
        super(HardwareAcceleratedActivity.class);
    }

    public void testIsHardwareAccelerated() {
        // Hardware acceleration should be available on devices with GL ES 2 or higher...
        if (getGlEsVersion(mActivity) >= 2) {
            // Both of the views are attached to a hardware accelerated window
            assertTrue(mHardwareView.isHardwareAccelerated());
            assertTrue(mSoftwareView.isHardwareAccelerated());
            assertTrue(mManualHardwareView.isHardwareAccelerated());
            assertTrue(mManualSoftwareView.isHardwareAccelerated());

            assertTrue(mHardwareView.isCanvasHardwareAccelerated());
            assertFalse(mSoftwareView.isCanvasHardwareAccelerated());
            assertTrue(mManualHardwareView.isCanvasHardwareAccelerated());
            assertFalse(mManualSoftwareView.isCanvasHardwareAccelerated());
        } else {
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
}
