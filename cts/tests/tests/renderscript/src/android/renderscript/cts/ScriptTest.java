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

import android.content.res.Resources;

import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.FieldPacker;
import android.renderscript.RenderScript;
import android.renderscript.Script;
import android.renderscript.ScriptC;
import android.renderscript.Type;

import android.renderscript.cts.ScriptC_negate;

import com.android.cts.stub.R;

public class ScriptTest extends RSBaseCompute {

    public void testScript() {
        Script S = new ScriptC_primitives(mRS, mRes, R.raw.primitives);

        S.setTimeZone("America/New_York");

        S.setVar(4, 9.0f);  // floatTest
        S.setVar(5, 9.0);  // doubleTest
        S.setVar(6, 7);  // charTest
        S.setVar(7, 300);  // shortTest
        S.setVar(8, 20000);  // intTest
        S.setVar(9, 20000000000l);  // longTest
        S.setVar(11, true);  // boolTest

        FieldPacker fp = new FieldPacker(4);
        fp.addI32(3);
        S.setVar(12, fp);  // structTest

        Type.Builder tb = new Type.Builder(mRS, Element.I32(mRS));
        Allocation a = Allocation.createTyped(mRS, tb.create());
        S.setVar(13, a);  // allocationTest

        S.bindAllocation(a, 14);  // intPtrTest
    }
}
