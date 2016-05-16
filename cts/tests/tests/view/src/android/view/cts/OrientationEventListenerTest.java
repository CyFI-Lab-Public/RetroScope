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

package android.view.cts;


import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.test.AndroidTestCase;
import android.view.OrientationEventListener;

/**
 * Test {@link OrientationEventListener}.
 */
public class OrientationEventListenerTest extends AndroidTestCase {
    public void testConstructor() {
        new MockOrientationEventListener(mContext);

        new MockOrientationEventListener(mContext, SensorManager.SENSOR_DELAY_UI);
    }

    public void testEnableAndDisable() {
        MockOrientationEventListener listener = new MockOrientationEventListener(mContext);
        listener.enable();
        listener.disable();
    }

    public void testCanDetectOrientation() {
        SensorManager sm = (SensorManager)mContext.getSystemService(Context.SENSOR_SERVICE);
        // Orientation can only be detected if there is an accelerometer
        boolean hasSensor = (sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null);
        
        MockOrientationEventListener listener = new MockOrientationEventListener(mContext);
        assertEquals(hasSensor, listener.canDetectOrientation());
    }

    private static class MockOrientationEventListener extends OrientationEventListener {
        public MockOrientationEventListener(Context context) {
            super(context);
        }

        public MockOrientationEventListener(Context context, int rate) {
            super(context, rate);
        }

        @Override
        public void onOrientationChanged(int orientation) {
        }
    }
}
