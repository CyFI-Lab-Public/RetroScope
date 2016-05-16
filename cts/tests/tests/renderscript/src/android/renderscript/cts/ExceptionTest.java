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

import android.renderscript.RSIllegalArgumentException;
import android.renderscript.RSInvalidStateException;
import android.renderscript.RSRuntimeException;
import android.renderscript.RSDriverException;

import android.test.AndroidTestCase;

public class ExceptionTest extends AndroidTestCase {
    public void testExceptions() {
        try {
            throw new RSIllegalArgumentException("IAE");
        } catch (RSIllegalArgumentException e) {
            assertEquals(e.getMessage(), "IAE");
        }

        try {
            throw new RSInvalidStateException("ISE");
        } catch (RSInvalidStateException e) {
            assertEquals(e.getMessage(), "ISE");
        }

        try {
            throw new RSRuntimeException("RE");
        } catch (RSRuntimeException e) {
            assertEquals(e.getMessage(), "RE");
        }

        try {
            throw new RSDriverException("DE");
        } catch (RSDriverException e) {
            assertEquals(e.getMessage(), "DE");
        }
    }
}
