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


import android.app.KeyguardManager;
import android.content.Context;
import android.test.ActivityInstrumentationTestCase2;

public class KeyguardManagerTest
        extends ActivityInstrumentationTestCase2<KeyguardManagerActivity> {

    private static final String TAG = "KeyguardManagerTest";

    public KeyguardManagerTest() {
        super("com.android.cts.stub", KeyguardManagerActivity.class);
    }

    public void testNewKeyguardLock() {
        final Context c = getInstrumentation().getContext();
        final KeyguardManager keyguardManager = (KeyguardManager) c.getSystemService(
                Context.KEYGUARD_SERVICE);
        final KeyguardManager.KeyguardLock keyLock = keyguardManager.newKeyguardLock(TAG);
        assertNotNull(keyLock);
    }

    public void testInKeyguardRestrictedInputMode() {
    }

    public void testExitKeyguardSecurely() {
    }
}
