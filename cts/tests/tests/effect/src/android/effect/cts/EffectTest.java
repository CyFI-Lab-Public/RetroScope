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

package android.effect.cts;

import android.graphics.Bitmap;
import android.media.effect.Effect;
import android.media.effect.EffectContext;
import android.media.effect.EffectFactory;

import junit.framework.TestCase;

/**
 * Test case to assert that the EffectFactory is correctly instantiating the effects that should
 * be available on all devices, and that they execute as expected.
 */
public class EffectTest extends TestCase {

    /** The GL environment that our tests will run in */
    private GLEnv mEnv;

    /** This is the effect context we will run the tests in */
    private EffectContext mEffectContext;

    /** This array contains the list of effects we expect to be available and fuctioning */
    private static String[] mExpectedEffects;

    @Override
    protected void setUp() throws Exception {
        mEnv = new GLEnv();
        mEnv.makeCurrent();
        mEffectContext = EffectContext.createWithCurrentGlContext();
    }

    @Override
    protected void tearDown() throws Exception {
        if (mEffectContext != null) {
            mEffectContext.release();
        }
        if (mEnv != null) {
            mEnv.tearDown();
        }
    }

    /** Assert an effect context can be created and attached to GL context. */
    public void test1_createContext() {
        assertNotNull("EffectContext attachment", mEffectContext);
    }

    /** Test that a factory can be retrieved from an EffectContext */
    public void test2_getFactory() {
        EffectFactory factory = getEffectContext().getFactory();
        assertNotNull("EffectFactory retrieval", factory);
    }

    /** Assert the built-in effects are available */
    public void test3_availableEffects() {
        EffectFactory factory = getEffectContext().getFactory();
        for (String effectName : getExpectedEffectList()) {
            assertTrue(
                "Effect '" + effectName + "' supported",
                factory.isEffectSupported(effectName));
        }
    }

    /** Assert that bogus effects are unavailable */
    public void test4_effectCreate() {
        EffectFactory factory = getEffectContext().getFactory();
        assertFalse("Empty effect name unsupported", factory.isEffectSupported(""));
        assertFalse("Bogus effect name unsupported", factory.isEffectSupported("!BOGUS!"));
        //assertFalse("Non-effect name unsupported", factory.isEffectSupported("java.lang.String"));
    }

    /** Assert that we can instantiate an effect */
    public void test5_effectCreate() {
        EffectFactory factory = getEffectContext().getFactory();
        for (String effectName : getExpectedEffectList()) {
            Effect effect = factory.createEffect(effectName);
            assertNotNull("Effect '" + effectName + "' instantiation", effect);
            effect.release();
        }
    }

    /** Assert that we can apply an effect */
    public void test6_effectApply() {
        EffectFactory factory = getEffectContext().getFactory();
        Effect effect = factory.createEffect(EffectFactory.EFFECT_SEPIA);
        Bitmap bitmap = createTestBitmap();
        int inputTexture = mEnv.bitmapToTexture(bitmap);
        int outputTexture = mEnv.generateTexture();
        try {
            effect.apply(inputTexture, bitmap.getWidth(), bitmap.getHeight(), outputTexture);
        } catch(RuntimeException e) {
            fail("Applying EFFECT_SEPIA failed: '" + e.getMessage() + "'");
        }
        mEnv.releaseTexture(inputTexture);
        mEnv.releaseTexture(outputTexture);
    }

    private EffectContext getEffectContext() {
        assertNotNull("EffectContext attachment", mEffectContext);
        return mEffectContext;
    }

    private Bitmap createTestBitmap() {
        Bitmap testBitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888);
        testBitmap.setPixel(0, 0, 0xFF000000);
        testBitmap.setPixel(0, 1, 0xFF0000FF);
        testBitmap.setPixel(1, 0, 0xFF00FF00);
        testBitmap.setPixel(1, 1, 0xFFFF0000);
        return testBitmap;
    }

    private String[] getExpectedEffectList() {
        if (mExpectedEffects == null) {
            mExpectedEffects = new String[] {
                EffectFactory.EFFECT_BRIGHTNESS,
                EffectFactory.EFFECT_CONTRAST,
                EffectFactory.EFFECT_FISHEYE,
                EffectFactory.EFFECT_BACKDROPPER,
                EffectFactory.EFFECT_AUTOFIX,
                EffectFactory.EFFECT_BLACKWHITE,
                EffectFactory.EFFECT_CROP,
                EffectFactory.EFFECT_CROSSPROCESS,
                EffectFactory.EFFECT_DOCUMENTARY,
                EffectFactory.EFFECT_BITMAPOVERLAY,
                EffectFactory.EFFECT_DUOTONE,
                EffectFactory.EFFECT_FILLLIGHT,
                EffectFactory.EFFECT_FLIP,
                EffectFactory.EFFECT_GRAIN,
                EffectFactory.EFFECT_GRAYSCALE,
                EffectFactory.EFFECT_LOMOISH,
                EffectFactory.EFFECT_NEGATIVE,
                EffectFactory.EFFECT_POSTERIZE,
                EffectFactory.EFFECT_REDEYE,
                EffectFactory.EFFECT_ROTATE,
                EffectFactory.EFFECT_SATURATE,
                EffectFactory.EFFECT_SEPIA,
                EffectFactory.EFFECT_SHARPEN,
                EffectFactory.EFFECT_STRAIGHTEN,
                EffectFactory.EFFECT_TEMPERATURE,
                EffectFactory.EFFECT_TINT,
                EffectFactory.EFFECT_VIGNETTE
            };
        }
        return mExpectedEffects;
    }
}
