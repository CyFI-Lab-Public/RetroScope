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

import android.renderscript.RenderScript;
import android.renderscript.ScriptC;

import com.android.cts.stub.R;

public class ScriptCTest extends RSBaseCompute {

    class ScriptCHelper extends ScriptC {
        public ScriptCHelper(int id, RenderScript rs) {
            super(id, rs);
        }

        public ScriptCHelper(RenderScript rs,
                             Resources resources,
                             int resourceID) {
            super(rs, resources, resourceID);
        }
    }

    public void testScriptC() {
        // Test basic constructor
        ScriptCHelper h = new ScriptCHelper(0, mRS);

        // Test actual constructor
        h = new ScriptCHelper(mRS, mRes, R.raw.negate);
    }
}
