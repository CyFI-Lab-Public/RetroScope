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
package android.view.cts;

import junit.framework.TestCase;
import android.view.SurfaceHolder.BadSurfaceTypeException;

public class SurfaceHolder_BadSurfaceTypeExceptionTest extends TestCase {
    public void testBadSurfaceTypeException(){
        BadSurfaceTypeException ne = null;
        boolean isThrowed = false;

        try {
            ne = new BadSurfaceTypeException();
            throw ne;
        } catch (BadSurfaceTypeException e) {
            assertSame(ne, e);
            isThrowed = true;
        } finally {
            if (!isThrowed) {
                fail("should throw out InflateException");
            }
        }

        String name = "SurfaceHolder_BadSurfaceTypeExceptionTest";
        isThrowed = false;

        try {
            ne = new BadSurfaceTypeException(name);
            throw ne;
        } catch (BadSurfaceTypeException e) {
            assertSame(ne, e);
            isThrowed = true;
        } finally {
            if (!isThrowed) {
                fail("should throw out InflateException");
            }
        }
    }
}
