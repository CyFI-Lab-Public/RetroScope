/*
 * Copyright (C) 2008 The Android Open Source Project
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

package android.app.cts;

import android.app.Activity;
import android.os.Bundle;

public class LocalActivityManagerStubActivity extends Activity{

    public static boolean sIsOnResumeCalled;
    public static boolean sIsOnStopCalled;
    public static boolean sIsOnPauseCalled;
    public static boolean sIsOnDestroyCalled;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
    }

    @Override
    public void onResume() {
        super.onResume();
        sIsOnResumeCalled = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        sIsOnStopCalled = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        sIsOnPauseCalled = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sIsOnDestroyCalled = true;
    }
}
