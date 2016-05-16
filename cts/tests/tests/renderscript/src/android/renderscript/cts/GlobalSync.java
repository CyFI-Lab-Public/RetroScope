/*
 * Copyright (C) 2013 The Android Open Source Project
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
import android.renderscript.RenderScript;
import android.renderscript.Type;

public class GlobalSync extends RSBaseCompute {
    Allocation AFailed;
    int [] Failed;
    Allocation AIn;

    protected void setupGlobalSync(RenderScript mRS, ScriptC_global_sync gs, int v) {
        Type.Builder typeBuilder = new Type.Builder(mRS, Element.I32(mRS));
        Type t = typeBuilder.setX(1).create();

        AFailed = Allocation.createTyped(mRS, t);
        Failed = new int [1];
        Failed[0] = 0;
        AFailed.copyFrom(Failed);
        gs.set_aFailed(AFailed);

        AIn = Allocation.createTyped(mRS, t);
        int [] In = new int [1];
        In[0] = v;
        AIn.copyFrom(In);
    }

    /**
     * Test whether we are properly synchronizing extern global data
     * when going from kernel to invokable.
     */
    public void testKIGlobalSync() {
        ScriptC_global_sync gs = new ScriptC_global_sync(mRS);

        int v = 7;
        setupGlobalSync(mRS, gs, v);
        gs.forEach_write_global(AIn);
        gs.invoke_test_read_global(v);

        AFailed.copyTo(Failed);
        if (Failed[0] != 0) {
            FoundError = true;
        }

        gs.destroy();
        checkForErrors();
    }

    /**
     * Test whether we are properly synchronizing static global data
     * when going from invokable to kernel.
     */
    public void testKIStaticGlobalSync() {
        ScriptC_global_sync gs = new ScriptC_global_sync(mRS);

        int v = 9;
        setupGlobalSync(mRS, gs, v);
        gs.forEach_write_static_global(AIn);
        gs.invoke_test_read_static_global(v);

        AFailed.copyTo(Failed);
        if (Failed[0] != 0) {
            FoundError = true;
        }

        gs.destroy();
        checkForErrors();
    }

    /**
     * Test whether we are properly synchronizing extern global data
     * when going from invokable to kernel.
     */
    public void testIKGlobalSync() {
        ScriptC_global_sync gs = new ScriptC_global_sync(mRS);

        int v = 7;
        setupGlobalSync(mRS, gs, v);
        gs.invoke_test_write_global(v);
        gs.forEach_read_global(AIn, AFailed);

        AFailed.copyTo(Failed);
        if (Failed[0] != 0) {
            FoundError = true;
        }

        gs.destroy();
        checkForErrors();
    }

    /**
     * Test whether we are properly synchronizing static global data
     * when going from kernel to invokable.
     */
    public void testIKStaticGlobalSync() {
        ScriptC_global_sync gs = new ScriptC_global_sync(mRS);

        int v = 9;
        setupGlobalSync(mRS, gs, v);
        gs.invoke_test_write_static_global(v);
        gs.forEach_read_static_global(AIn, AFailed);

        AFailed.copyTo(Failed);
        if (Failed[0] != 0) {
            FoundError = true;
        }

        gs.destroy();
        checkForErrors();
    }
}
