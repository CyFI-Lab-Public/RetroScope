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

package android.content.pm.cts;


import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.test.AndroidTestCase;

public class PackageManager_NameNotFoundExceptionTest extends AndroidTestCase {

    public void testNameNotFoundException() {
        PackageManager.NameNotFoundException exception = new PackageManager.NameNotFoundException();
        try {
            throw exception;
        } catch (NameNotFoundException e) {
            assertNull(e.getMessage());
        }

        final String message = "test";
        exception = new PackageManager.NameNotFoundException(message);
        try {
            throw exception;
        } catch (NameNotFoundException e) {
            assertEquals(message, e.getMessage());
        }
    }
}
