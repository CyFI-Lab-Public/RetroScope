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

import android.app.ActivityManager;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.Type;
import android.util.Log;
import android.content.Context;

/*
// -target-api 11

#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

rs_allocation a;

void print() {
    rsDebug("unused", rsGetElementAt_int(a, 0, 0));
}
*/

/**
 * Test for memory leaks due to missing object slot information.
 *
 * The code to generate leak.bc is in the previous comment block.
 * Note that you need to modify llvm-rs-cc to skip emitting the
 * .rs.dtor() function, since it will also do the proper cleanup
 * (but not trigger the original bug). Old HC code can trigger this
 * bug, since it may have been compiled without .rs.dtor() support.
 */
public class LeakTest extends RSBaseCompute {
    private static final String TAG = "LeakTest";

    public void testForLeaks() {
        ActivityManager am = (ActivityManager) getContext().getSystemService("activity");
        int mc = am.getLargeMemoryClass() / 32;
        if (mc < 1) {
            mc = 1;
        }
        int x = mc * 1024 * 1024;

        for (int i = 0; i < 100; i++) {
            Log.w(TAG, "Leak test iteration " + i);
            ScriptC_leak leak = new ScriptC_leak(mRS);
            Type t = new Type.Builder(mRS, Element.I32(mRS)).setX(x).create();
            Allocation A = Allocation.createTyped(mRS, t);
            leak.set_a(A);
            A = null;
            //System.gc();
            leak.destroy();
            mRS.finish();
        }
        mRS.finish();
        checkForErrors();
    }
}
