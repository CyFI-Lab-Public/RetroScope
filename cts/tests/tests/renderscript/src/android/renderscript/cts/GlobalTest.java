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

public class GlobalTest extends RSBaseCompute {
    /**
     * Test whether we properly clean up after large global arrays are
     * allocated/deallocated.
     */
    public void testLargeGlobal() {
        for (int i = 0; i < 1000; i++) {
            ScriptC_large_global lg = new ScriptC_large_global(mRS);
            lg.destroy();
        }
        checkForErrors();
    }
}
