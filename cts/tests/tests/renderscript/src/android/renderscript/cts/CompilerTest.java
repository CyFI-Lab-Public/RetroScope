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

import android.renderscript.RSRuntimeException;
import android.util.Log;

import com.android.cts.stub.R;

/**
 * Test whether the driver properly handles compile-time issues.
 */
public class CompilerTest extends RSBaseCompute {
    /**
     * missing_link.rs contains symbols that can't be resolved at runtime,
     * which should trigger an exception.
     */
    public void testMissingLink() {
        try {
            ScriptC_missing_link t = new ScriptC_missing_link(mRS,
                                                              mRes,
                                                              R.raw.missing_link);
            fail("should throw RSRuntimeException");
        } catch (RSRuntimeException e) {
        }
    }
}
