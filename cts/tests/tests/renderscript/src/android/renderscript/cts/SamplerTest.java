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

package android.renderscript.cts;

import android.renderscript.Sampler;
import android.renderscript.Sampler.Value;

public class SamplerTest extends RSBaseCompute {

    Sampler.Value[] mMinValues;
    Sampler.Value[] mMagValues;
    Sampler.Value[] mWrapValues;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mMinValues = new Sampler.Value[4];
        mMinValues[0] = Sampler.Value.NEAREST;
        mMinValues[1] = Sampler.Value.LINEAR;
        mMinValues[2] = Sampler.Value.LINEAR_MIP_LINEAR;
        mMinValues[3] = Sampler.Value.LINEAR_MIP_NEAREST;

        mMagValues = new Sampler.Value[2];
        mMagValues[0] = Sampler.Value.NEAREST;
        mMagValues[1] = Sampler.Value.LINEAR;

        mWrapValues = new Sampler.Value[2];
        mWrapValues[0] = Sampler.Value.CLAMP;
        mWrapValues[1] = Sampler.Value.WRAP;
    }

    @Override
    protected void tearDown() throws Exception {
        mMinValues = null;
        mMagValues = null;
        mWrapValues = null;
        super.tearDown();
    }

    boolean contains(Sampler.Value[] array, Sampler.Value val) {
        for (int i = 0; i < array.length; i ++) {
            if (array[i] == val) {
                return true;
            }
        }
        return false;
    }

    public void testSamplerBuilder() {
        for (int aniso = 1; aniso <= 4; aniso++) {
            for (Sampler.Value minV : Sampler.Value.values()) {
                for (Sampler.Value magV : Sampler.Value.values()) {
                    for (Sampler.Value wrapSV : Sampler.Value.values()) {
                        for (Sampler.Value wrapTV : Sampler.Value.values()) {
                            Sampler.Builder b = new Sampler.Builder(mRS);
                            b.setAnisotropy(aniso);

                            // Some value combinations are illegal
                            boolean validMin = contains(mMinValues, minV);
                            boolean validMag = contains(mMagValues, magV);
                            boolean validS = contains(mWrapValues, wrapSV);
                            boolean validT = contains(mWrapValues, wrapTV);

                            try {
                                b.setMinification(minV);
                            } catch (IllegalArgumentException e) {
                                assertFalse(validMin);
                            }
                            try {
                                b.setMagnification(magV);
                            } catch (IllegalArgumentException e) {
                                assertFalse(validMag);
                            }
                            try {
                                b.setWrapS(wrapSV);
                            } catch (IllegalArgumentException e) {
                                assertFalse(validS);
                            }
                            try {
                                b.setWrapT(wrapTV);
                            } catch (IllegalArgumentException e) {
                                assertFalse(validT);
                            }

                            if (validMin && validMag && validS && validT) {
                                b.create();
                            }
                        }
                    }
                }
            }
        }
    }


    public void testPrebuiltSamplers() {
        assertTrue(Sampler.CLAMP_LINEAR(mRS) != null);
        assertTrue(Sampler.CLAMP_LINEAR_MIP_LINEAR(mRS) != null);
        assertTrue(Sampler.CLAMP_NEAREST(mRS) != null);
        assertTrue(Sampler.WRAP_LINEAR(mRS) != null);
        assertTrue(Sampler.WRAP_LINEAR_MIP_LINEAR(mRS) != null);
        assertTrue(Sampler.WRAP_NEAREST(mRS) != null);
        assertTrue(Sampler.MIRRORED_REPEAT_NEAREST(mRS) != null);
        assertTrue(Sampler.MIRRORED_REPEAT_LINEAR(mRS) != null);
        assertTrue(Sampler.MIRRORED_REPEAT_LINEAR_MIP_LINEAR(mRS) != null);
    }

    public void testSamplerValue() {
        assertEquals(Value.NEAREST, Value.valueOf("NEAREST"));
        assertEquals(Value.LINEAR, Value.valueOf("LINEAR"));
        assertEquals(Value.LINEAR_MIP_LINEAR, Value.valueOf("LINEAR_MIP_LINEAR"));
        assertEquals(Value.LINEAR_MIP_NEAREST, Value.valueOf("LINEAR_MIP_NEAREST"));
        assertEquals(Value.WRAP, Value.valueOf("WRAP"));
        assertEquals(Value.CLAMP, Value.valueOf("CLAMP"));
        assertEquals(Value.MIRRORED_REPEAT, Value.valueOf("MIRRORED_REPEAT"));

        // Make sure no new enums are added
        assertEquals(7, Value.values().length);
    }
}


