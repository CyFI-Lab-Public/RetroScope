/*
 * Copyright (C) 2009 The Android Open Source Project
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

import android.app.AliasActivity;
import android.content.Intent;
import android.test.AndroidTestCase;

public class AliasActivityTest extends AndroidTestCase {

    private static final long SLEEP_TIME = 1000;

    public void testAliasActivity() throws InterruptedException {
        new AliasActivity();

        Intent intent = new Intent();
        intent.setClass(getContext(), AliasActivityStub.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        assertFalse(ChildActivity.isStarted);
        assertFalse(AliasActivityStub.isOnCreateCalled);
        getContext().startActivity(intent);
        Thread.sleep(SLEEP_TIME);
        assertTrue(AliasActivityStub.isOnCreateCalled);
        assertTrue(ChildActivity.isStarted);
        assertTrue(AliasActivityStub.isFinished);
    }

}
