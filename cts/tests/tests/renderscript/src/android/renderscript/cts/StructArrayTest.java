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
import com.android.cts.stub.R;

public class StructArrayTest extends RSBaseCompute {
    /**
     * Test for that array struct elements are properly set
     */
    public void testStructArrays() {
        mRS.setErrorHandler(mRsError);
        ScriptC_struct_array pad = new ScriptC_struct_array(mRS,
                                                            mRes,
                                                            R.raw.struct_array);
        ScriptField_ArrayMe S = new ScriptField_ArrayMe(mRS, 1);
        int[] values = {0, 1, 2, 3, 4};
        S.set_i(0, values, true);

        pad.bind_s(S);
        pad.invoke_verify();
        waitForMessage();
        checkForErrors();
    }
}
