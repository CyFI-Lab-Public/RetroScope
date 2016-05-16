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

import com.android.cts.stub.R;

import android.renderscript.RenderScript;
import android.test.AndroidTestCase;

public class RenderScriptTest extends AndroidTestCase {

    /**
     * Simple test for Renderscript that executes a passthrough function
     * from cts/tests/src/android/renderscript/cts/passthrough.rs.
     */
    public void testRenderScript() {
        RenderScript mRS = RenderScript.create(getContext());
        ScriptC_passthrough t = new ScriptC_passthrough(mRS,
            getContext().getResources(), R.raw.passthrough);
        t.invoke_passthrough(5);
        mRS.destroy();
    }

    /**
     * Excercise all API calls in the basic RenderScript class.
     */
    public void testAPI() {
        try {
            RenderScript mRS = RenderScript.create(null);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
        }

        RenderScript mRS = RenderScript.create(getContext());
        mRS.contextDump();
        mRS.finish();
        assertEquals(mRS.getApplicationContext(),
                     getContext().getApplicationContext());
        RenderScript.RSErrorHandler mEH = mRS.getErrorHandler();
        RenderScript.RSMessageHandler mMH = mRS.getMessageHandler();
        mRS.setErrorHandler(mEH);
        mRS.setMessageHandler(mMH);
        mRS.setPriority(RenderScript.Priority.LOW);
        mRS.setPriority(RenderScript.Priority.NORMAL);
        mRS.destroy();
    }

    /**
     * Verify Priority enum properties.
     */
    public void testPriority() {
        assertEquals(RenderScript.Priority.LOW,
            RenderScript.Priority.valueOf("LOW"));
        assertEquals(RenderScript.Priority.NORMAL,
            RenderScript.Priority.valueOf("NORMAL"));
        assertEquals(2, RenderScript.Priority.values().length);
    }

    /**
     * Create a base RSMessageHandler object and run() it.
     * Note that most developers will subclass RSMessageHandler and use
     * their own non-empty implementation.
     */
    public void testRSMessageHandler() {
        RenderScript.RSMessageHandler mMH = new RenderScript.RSMessageHandler();
        mMH.run();
    }

    /**
     * Create a base RSErrorHandler object and run() it.
     * Note that most developers will subclass RSErrorHandler and use
     * their own non-empty implementation.
     */
    public void testRSErrorHandler() {
        RenderScript.RSErrorHandler mEH = new RenderScript.RSErrorHandler();
        mEH.run();
    }
}
