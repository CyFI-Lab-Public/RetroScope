/*
 * Copyright (C) 2011-2012 The Android Open Source Project
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

import android.renderscript.Float4;
import com.android.cts.stub.R;

public class StructPadTest extends RSBaseCompute {
    /**
     * Test for appropriate alignment/padding of structures.
     */
    public void testStructPadding() {
        mRS.setErrorHandler(mRsError);
        ScriptC_struct_pad pad = new ScriptC_struct_pad(mRS,
                                                        mRes,
                                                        R.raw.struct_pad);
        ScriptField_PadMe S = new ScriptField_PadMe(mRS, 1);
        Float4 F4 = new Float4(1.0f, 2.0f, 3.0f, 4.0f);

        S.set_i(0, 7, true);
        S.set_f4(0, F4, true);
        S.set_j(0, 9, true);

        ScriptField_PadMe.Item I = new ScriptField_PadMe.Item();
        I.i = S.get_i(0);
        I.f4 = S.get_f4(0);
        I.j = S.get_j(0);

        S.set(new ScriptField_PadMe.Item(), 0, true);

        pad.set_s(I);
        pad.invoke_verify();
        waitForMessage();
        checkForErrors();
    }
}
