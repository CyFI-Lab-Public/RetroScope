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

package android.permission.cts;

import android.hardware.Camera;
import android.os.Environment;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.MediumTest;

import java.io.FileOutputStream;

/**
 * Tests for camera-related Permissions. Currently, this means
 * android.permission.CAMERA.
 */
public class CameraPermissionTest extends AndroidTestCase {

    private static String PATH_PREFIX = Environment.getExternalStorageDirectory().toString();
    private static String CAMERA_IMAGE_PATH = PATH_PREFIX + "this-should-not-exist.jpg";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    class ShutterCallback implements Camera.ShutterCallback {
        public void onShutter() { }
    }

    class RawPictureCallback implements Camera.PictureCallback {
        public void onPictureTaken(byte [] rawData, Camera camera) { }
    }

    class JpegPictureCallback implements Camera.PictureCallback {
        public void onPictureTaken(byte [] jpegData, Camera camera) {
            if (jpegData == null) {
                // TODO: Is this good (= expected, = correct), or weird, or bad?
                return;
            }

            try {
                FileOutputStream s = new FileOutputStream(CAMERA_IMAGE_PATH);
                s.write(jpegData);
                s.flush();
            }
            catch (SecurityException e) {
                // Sure, NOW they tell us (NOTE: this could be a side effect
                // of the upcoming WRITE_EXTERNAL_STORAGE permission).
            } catch (Exception e) {
                // We didn't really need to save it anyway, did we?
            }

            fail("Successfully captured an image of " + jpegData.length +
                 " bytes, and saved it to " + CAMERA_IMAGE_PATH);
        }
    }

    /**
     * Attempt to take a picture. Requires Permission:
     * {@link android.Manifest.permission#CAMERA}.
     */
    @MediumTest
    public void testCamera() {
        try {
            (Camera.open()).takePicture(new ShutterCallback(),
                                        new RawPictureCallback(),
                                        new JpegPictureCallback());
            fail("Was able to take a picture with the camera with no permission");
        }
        catch (SecurityException e) {
            // expected
        } catch (RuntimeException e) {
            // expected
            // The JNI layer isn't translating the EPERM error status into
            // a SecurityException.
        }
    }

}

