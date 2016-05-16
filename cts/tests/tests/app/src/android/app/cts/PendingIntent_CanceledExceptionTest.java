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

import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.test.AndroidTestCase;

public class PendingIntent_CanceledExceptionTest extends AndroidTestCase {

    public void testConstructor() {
        PendingIntent.CanceledException canceledException = new PendingIntent.CanceledException();
        try {
            throw canceledException;
        } catch (CanceledException e) {
            assertNull(e.getMessage());
        }

        final String message = "test";
        canceledException = new PendingIntent.CanceledException(message);
        try {
            throw canceledException;
        } catch (CanceledException e) {
            assertEquals(message, canceledException.getMessage());
        }

        Exception ex = new Exception();
        canceledException = new PendingIntent.CanceledException(ex);
        try {
            throw canceledException;
        } catch (CanceledException e) {
            assertSame(ex, e.getCause());
        }
    }
}
