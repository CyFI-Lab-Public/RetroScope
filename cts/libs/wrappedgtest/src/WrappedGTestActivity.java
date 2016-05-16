/*
 * Copyright 2012 The Android Open Source Project
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

package android.test.wrappedgtest;

import android.app.Activity;
import android.app.Instrumentation;
import android.os.Bundle;

public class WrappedGTestActivity extends Activity {

    private WrappedGTestInstrumentation mInstrumentation;

    public void setInstrumentation(WrappedGTestInstrumentation instrumentation) {
        mInstrumentation = instrumentation;
    }

    public int runGTests() {
        return runTests(this);
    }

    public void sendStatus(String output) {
        Bundle outputBundle = new Bundle();
        outputBundle.putString("gtest", output);
        mInstrumentation.sendStatus(1, outputBundle);
    }

    protected static native int runTests(WrappedGTestActivity activity);
}
