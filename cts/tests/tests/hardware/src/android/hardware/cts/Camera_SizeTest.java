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

package android.hardware.cts;


import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.test.suitebuilder.annotation.LargeTest;

import junit.framework.TestCase;

@LargeTest
public class Camera_SizeTest extends TestCase {

    private final int HEIGHT1 = 320;
    private final int WIDTH1 = 240;
    private final int HEIGHT2 = 480;
    private final int WIDTH2 = 320;
    private final int HEIGHT3 = 640;
    private final int WIDTH3 = 480;

    public void testConstructor() {
        if (Camera.getNumberOfCameras() < 1) {
            return;
        }

        Camera camera = Camera.open(0);
        Parameters parameters = camera.getParameters();

        checkSize(parameters, WIDTH1, HEIGHT1);
        checkSize(parameters, WIDTH2, HEIGHT2);
        checkSize(parameters, WIDTH3, HEIGHT3);

        camera.release();
    }

    private void checkSize(Parameters parameters, int width, int height) {
        parameters.setPictureSize(width, height);
        assertEquals(width, parameters.getPictureSize().width);
        assertEquals(height, parameters.getPictureSize().height);
    }
}

