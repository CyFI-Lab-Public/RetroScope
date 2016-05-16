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
import android.renderscript.RSRuntimeException;
import com.android.cts.stub.R;
import java.util.Random;

public class GetAllocationTest extends RSBaseCompute {

    public void testGetAllocation () {
        Random random = new Random(0x827492ab);
        int[] tempArray = new int[INPUTSIZE];
        for (int i = 0; i < INPUTSIZE; ++i) {
            tempArray[i] = random.nextInt();
        }
        ScriptC_get_allocation ms =
                new ScriptC_get_allocation(mRS, mRes, R.raw.get_allocation);

        Allocation mTemp = Allocation.createSized(mRS, Element.I32(mRS), INPUTSIZE);
        mTemp.copyFrom(tempArray);
        Allocation mOut = Allocation.createTyped(mRS, mTemp.getType());

        ms.bind_pointer(mTemp);
        ms.set_script(ms);
        ms.set_alloc_out(mOut);
        ms.invoke_start();

        int [] out = new int[INPUTSIZE];
        mOut.copyTo(out);
        for (int i = 0; i < tempArray.length; i++) {
            assertEquals("Incorrect value @ idx = " + i + " | ",
            tempArray[i],
            out[i]);
        }
    }

}
