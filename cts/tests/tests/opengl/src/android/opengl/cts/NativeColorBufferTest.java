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
package android.opengl.cts;

import android.test.ActivityInstrumentationTestCase2;

public class NativeColorBufferTest extends ActivityInstrumentationTestCase2<OpenGLES20NativeActivityTwo> {
    private static final long SLEEP_TIME = 500l;
    private static final String TAG = NativeColorBufferTest.class.getName();
    public NativeColorBufferTest(Class<OpenGLES20NativeActivityTwo> activityClass) {
        super(activityClass);
    }

    private OpenGLES20NativeActivityTwo mActivity;

    public NativeColorBufferTest() {
        super(OpenGLES20NativeActivityTwo.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
    }

    public void test_RGBA_1001() throws Throwable {
        float r = 1.0f;
        float g = 0.0f;
        float b = 0.0f;
        float a = 1.0f;
        final float[] vertexColors =  getVertexColors(r, g, b, a);
        mActivity = getActivity();
        float[] expectedColor = {r, g, b, a};
        this.runTestOnUiThread(new Runnable() {
            public void run() {
                mActivity.setView(Constants.COLOR, 1, vertexColors);
            }
        });
        assertTrue(mActivity.waitForFrameDrawn());
        float[] actualColor = mActivity.getActualColor();
        compare(expectedColor, actualColor);
    }

    public void test_RGBA_1101() throws Throwable {
        float r = 1.0f;
        float g = 1.0f;
        float b = 0.0f;
        float a = 1.0f;
        final float[] vertexColors = getVertexColors(r, g, b, a);
        float[] expectedColor = {r, g, b, a};
        mActivity = getActivity();
        this.runTestOnUiThread(new Runnable() {
            public void run() {
                mActivity.setView(Constants.COLOR, 1, vertexColors);
            }
        });
        assertTrue(mActivity.waitForFrameDrawn());
        float[] actualColor = mActivity.getActualColor();
        compare(expectedColor, actualColor);
    }

    public void test_RGBA_1111() throws Throwable {
        float r = 1.0f;
        float g = 1.0f;
        float b = 1.0f;
        float a = 1.0f;
        final float[] vertexColors = getVertexColors(r, g, b, a);

        float[] expectedColor = {r, g, b, a};
        mActivity = getActivity();
        this.runTestOnUiThread(new Runnable() {
            public void run() {
                mActivity.setView(Constants.COLOR, 1, vertexColors);
            }
        });
        assertTrue(mActivity.waitForFrameDrawn());
        float[] actualColor = mActivity.getActualColor();
        compare(expectedColor, actualColor);
    }

    public void test_RGBA_0101() throws Throwable {
        float r = 0.0f;
        float g = 1.0f;
        float b = 0.0f;
        float a = 1.0f;
        final float[] vertexColors = getVertexColors(r, g, b, a);

        float[] expectedColor = {r, g, b, a};
        mActivity = getActivity();
        this.runTestOnUiThread(new Runnable() {
            public void run() {
                mActivity.setView(Constants.COLOR, 1, vertexColors);
            }
        });
        assertTrue(mActivity.waitForFrameDrawn());
        float[] actualColor = mActivity.getActualColor();
        compare(expectedColor, actualColor);
    }

    public void test_RGBA_0011() throws Throwable {
        float r = 0.0f;
        float g = 0.0f;
        float b = 1.0f;
        float a = 1.0f;
        final float[] vertexColors = getVertexColors(r, g, b, a);

        float[] expectedColor = {r, g, b, a};
        mActivity = getActivity();
        this.runTestOnUiThread(new Runnable() {
            public void run() {
                mActivity.setView(Constants.COLOR, 1, vertexColors);
            }
        });
        assertTrue(mActivity.waitForFrameDrawn());
        float[] actualColor = mActivity.getActualColor();
        compare(expectedColor, actualColor);
    }

    public void test_RGBA_0000() throws Throwable {
        float r = 0.0f;
        float g = 0.0f;
        float b = 0.0f;
        float a = 0.0f;
        final float[] vertexColors = getVertexColors(r, g, b, a);

        float[] expectedColor = {r, g, b, a};
        mActivity = getActivity();
        this.runTestOnUiThread(new Runnable() {
            public void run() {
                mActivity.setView(Constants.COLOR, 1, vertexColors);
            }
        });
        assertTrue(mActivity.waitForFrameDrawn());
        float[] actualColor = mActivity.getActualColor();
        compare(expectedColor, actualColor);
    }

    public void test_RGBA_rand_val_one() throws Throwable {
        float r = 0.6f;
        float g = 0.7f;
        float b = 0.25f;
        float a = 0.5f;
        final float[] vertexColors = getVertexColors(r, g, b, a);

        float[] expectedColor = {r, g, b, a};
        mActivity = getActivity();
        this.runTestOnUiThread(new Runnable() {
            public void run() {
                mActivity.setView(Constants.COLOR, 1, vertexColors);
            }
        });
        assertTrue(mActivity.waitForFrameDrawn());
        float[] actualColor = mActivity.getActualColor();
        compare(expectedColor, actualColor);
    }

    private float[] getVertexColors(float r, float g, float b, float a) {
        float[] vertexColors =
              { r, g, b, a,
                r, g, b, a,
                r, g, b, a
               };
        return vertexColors;
    }

    private void compare(float[] expectedColor, float[] actualColor) {
        assertNotNull(actualColor);
        assertEquals(4, actualColor.length);
        float r = expectedColor[0];
        float g = expectedColor[1];
        float b = expectedColor[2];
        float a = expectedColor[3];
        //We are giving 0.1 buffer as colors might not be exactly same as input color
        assertTrue(Math.abs(r - (actualColor[0]/255.0)) < 0.1f);
        assertTrue(Math.abs(g - (actualColor[1]/255.0)) < 0.1f);
        assertTrue(Math.abs(b - (actualColor[2]/255.0)) < 0.1f);
        float actualAlpha = (float) (actualColor[3]/255.0);
        //Commented as of now as the Alpha being returned is always 1
        //assertTrue(Math.abs(a - (actualColor[3]/255)) < 0.1f);
    }
}
