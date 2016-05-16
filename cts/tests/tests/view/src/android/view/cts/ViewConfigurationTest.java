/*
 * Copyright (C) 2008 The Android Open Source Project
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

package android.view.cts;

import android.content.Context;
import android.test.InstrumentationTestCase;
import android.view.ViewConfiguration;

/**
 * Test {@link ViewConfiguration}.
 */
public class ViewConfigurationTest extends InstrumentationTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @SuppressWarnings("deprecation")
    public void testStaticValues() {
        ViewConfiguration.getScrollBarSize();
        ViewConfiguration.getFadingEdgeLength();
        ViewConfiguration.getPressedStateDuration();
        ViewConfiguration.getLongPressTimeout();
        ViewConfiguration.getTapTimeout();
        ViewConfiguration.getJumpTapTimeout();
        ViewConfiguration.getEdgeSlop();
        ViewConfiguration.getTouchSlop();
        ViewConfiguration.getWindowTouchSlop();
        ViewConfiguration.getMinimumFlingVelocity();
        ViewConfiguration.getMaximumDrawingCacheSize();
        ViewConfiguration.getZoomControlsTimeout();
        ViewConfiguration.getGlobalActionKeyTimeout();
        ViewConfiguration.getScrollFriction();
        ViewConfiguration.getDoubleTapTimeout();
    }

    @SuppressWarnings("deprecation")
    public void testConstructor() {
        new ViewConfiguration();
    }

    public void testInstanceValues() {
        ViewConfiguration vc = ViewConfiguration.get(getInstrumentation().getTargetContext());
        assertNotNull(vc);
        vc.getScaledDoubleTapSlop();
        vc.getScaledEdgeSlop();
        vc.getScaledFadingEdgeLength();
        vc.getScaledMaximumDrawingCacheSize();
        vc.getScaledMinimumFlingVelocity();
        vc.getScaledScrollBarSize();
        vc.getScaledTouchSlop();
        vc.getScaledWindowTouchSlop();
    }
}
