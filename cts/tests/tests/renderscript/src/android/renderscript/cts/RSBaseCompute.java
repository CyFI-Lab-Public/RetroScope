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

import android.renderscript.RenderScript;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RSRuntimeException;
import android.util.Log;

/**
 * Base RenderScript test class. This class provides a message handler and a
 * convenient way to wait for compute scripts to complete their execution.
 */
class RSBaseCompute extends RSBase {
    RenderScript mRS;

    static final int TEST_F32 = 0;
    static final int TEST_F32_2 = 1;
    static final int TEST_F32_3 = 2;
    static final int TEST_F32_4 = 3;
    static final int TEST_RELAXED_F32 = 4;
    static final int TEST_RELAXED_F32_2 = 5;
    static final int TEST_RELAXED_F32_3 = 6;
    static final int TEST_RELAXED_F32_4 = 7;
    protected int INPUTSIZE = 512;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mRS = RenderScript.create(mCtx);
        mRS.setMessageHandler(mRsMessage);
    }

    @Override
    protected void tearDown() throws Exception {
        if (mRS != null) {
            mRS.destroy();
            mRS = null;
        }
        super.tearDown();
    }

    public void checkArray(float[] ref, float[] out, int height, int refStride,
             int outStride, float ulpCount) {
        int minStride = refStride > outStride ? outStride : refStride;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < minStride; j++) {
                int refIdx = i * refStride + j;
                int outIdx = i * outStride + j;
                float ulp = Math.ulp(ref[refIdx]) * ulpCount;
                assertEquals("Incorrect value @ idx = " + i + " |",
                        ref[refIdx],
                        out[outIdx],
                        ulp);
            }
        }
    }

    public void checkArray(int[] ref, int[] out, int height, int refStride,
             int outStride) {
        int minStride = refStride > outStride ? outStride : refStride;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < minStride; j++) {
                int refIdx = i * refStride + j;
                int outIdx = i * outStride + j;
                assertEquals("Incorrect value @ idx = " + i + " |",
                        ref[refIdx],
                        out[outIdx]);
            }
        }
    }

    private void baseTestHelper(int testid, Element inElement, Element outElement, long seed, int fact,
                                int offset, int rStride, int rSkip, int refStride, int outStride,
                                int inStride, int skip, int ulp) {
        float[] inArray = makeInArray(INPUTSIZE * inStride);
        fillRandom(seed, fact, offset, inArray, rStride, rSkip);
        float[] refArray = getRefArray(inArray, INPUTSIZE, inStride, skip);

        Allocation mAllocationIn = setInAlloc(inElement);
        fillInAlloc(mAllocationIn, inArray);

        Allocation mAllocationOut = setOutAlloc(outElement);
        try {
            RSUtils.forEach(this, testid, mAllocationIn, mAllocationOut);
        } catch (RSRuntimeException e) {
            Log.e("RenderscriptCTS", "Caught RSRuntimeException: " +
                  e.getMessage());
        }
        float[] outArray = makeOutArray(INPUTSIZE * outStride);
        mAllocationOut.copyTo(outArray);
        checkArray(refArray, outArray, INPUTSIZE, refStride, outStride, ulp);
    }

    public void baseTest(int testid, long seed, int refStride, int outStride, int inStride, int skip, int ulp) {
        baseTestHelper(testid, null, null, seed, 1, 0, 1, 0, refStride, outStride, inStride, skip, ulp);
    }

    public void doF32(long seed, int ulp) {
        baseTestHelper(TEST_F32, Element.F32(mRS), Element.F32(mRS), seed, 1, 0, 1, 0, 1, 1, 1, 0, ulp);
    }

    public void doF32_2(long seed, int ulp) {
        baseTestHelper(TEST_F32_2, Element.F32_2(mRS), Element.F32_2(mRS), seed, 1, 0, 1, 0, 2, 2, 2, 0, ulp);
    }

    public void doF32_3(long seed, int ulp) {
        baseTestHelper(TEST_F32_3, Element.F32_3(mRS), Element.F32_3(mRS), seed, 1, 0, 4, 1, 3, 4, 4, 1, ulp);
    }

    public void doF32_4(long seed, int ulp) {
        baseTestHelper(TEST_F32_4, Element.F32_4(mRS), Element.F32_4(mRS), seed, 1, 0, 1, 0, 4, 4, 4, 0, ulp);
    }

    public void doF32_relaxed(long seed, int ulp) {
        baseTestHelper(TEST_RELAXED_F32, Element.F32(mRS), Element.F32(mRS), seed, 1, 0, 1, 0, 1, 1, 1, 0, ulp);
    }

    public void doF32_2_relaxed(long seed, int ulp) {
        baseTestHelper(TEST_RELAXED_F32_2, Element.F32_2(mRS), Element.F32_2(mRS), seed, 1, 0, 1, 0, 2, 2, 2, 0, ulp);
    }

    public void doF32_3_relaxed(long seed, int ulp) {
        baseTestHelper(TEST_RELAXED_F32_3, Element.F32_3(mRS), Element.F32_3(mRS), seed, 1, 0, 4, 1, 3, 4, 4, 1, ulp);
    }

    public void doF32_4_relaxed(long seed, int ulp) {
        baseTestHelper(TEST_RELAXED_F32_4, Element.F32_4(mRS), Element.F32_4(mRS), seed, 1, 0, 1, 0, 4, 4, 4, 0, ulp);
    }


    public void forEach(int testId, Allocation mIn, Allocation mOut) throws RSRuntimeException {
        // Intentionally empty... subclass will likely define only one, but not both
    }

    public void forEach(int testId, Allocation mIn) throws RSRuntimeException {
        // Intentionally empty... subclass will likely define only one, but not both
    }

    //These are default actions for these functions, specific tests overload them
    protected float[] getRefArray(float[] inArray, int size, int stride, int skip) {
        return null;
    }

    protected Allocation setInAlloc(Element e) {
        return Allocation.createSized(mRS, e, INPUTSIZE);
    }

    protected Allocation setOutAlloc(Element e) {
        return Allocation.createSized(mRS, e, INPUTSIZE);
    }

    protected float[] makeInArray(int size) {
        return new float[size];
    }

    protected float[] makeOutArray(int size) {
        return new float[size];
    }

    protected void fillRandom(long seed, int fact, int offset, float[] inArray, int rStride, int rSkip) {
        RSUtils.genRandom(seed, fact, offset, inArray, rStride, rSkip);
    }

    protected void fillInAlloc(Allocation mIn, float[] inArray) {
        mIn.copy1DRangeFromUnchecked(0, INPUTSIZE, inArray);
    }
}
