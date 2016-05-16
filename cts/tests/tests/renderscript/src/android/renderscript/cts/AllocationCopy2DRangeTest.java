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

package android.renderscript.cts;

import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.Type;
import java.util.Random;
import com.android.cts.stub.R;

public class AllocationCopy2DRangeTest extends RSBaseCompute {
    private Allocation mInAllocation;
    private Allocation mOutAllocation;

    public void testAllocationCopy2DRange() {
        ScriptC_allocationCopy2DRange mScript =
                new ScriptC_allocationCopy2DRange(mRS, mRes, R.raw.allocationcopy2drange);
        Random random = new Random(0x172d8ab9);
        int width = random.nextInt(512);
        int height = random.nextInt(512);

        int[] inArray = new int[width * height];
        int[] outArray = new int[width * height];

        for (int i = 0; i < width * height; i++) {
            inArray[i] = random.nextInt();
        }

        // Create 2D data.
        Type.Builder typeBuilder = new Type.Builder(mRS, Element.I32(mRS));
        typeBuilder.setX(width).setY(height);
        mInAllocation = Allocation.createTyped(mRS, typeBuilder.create());
        mOutAllocation = Allocation.createTyped(mRS, mInAllocation.getType());
        mInAllocation.copy2DRangeFrom(0, 0, width, height, inArray);

        mScript.set_height(height);
        mScript.set_width(width);
        mScript.set_mIn(mInAllocation);
        mScript.set_mOut(mOutAllocation);

        mScript.invoke_testAllocationCopy2DRange();
        mOutAllocation.copyTo(outArray);
        assertTrue("testAllocationCopy2DRange failed, output array does not match input",
                compareTwoArrays(inArray, outArray, width*height));
    }

    private boolean compareTwoArrays(int[] src, int[] dest, int size) {
        boolean result = true;
        for (int i = 0; i < size; i++) {
            if (src[i] != dest[i]) {
                result = false;
                break;
            }
        }
        return result;
    }
}
