/*
 * Copyright (C) 2012 The Android Open Source Project
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

package android.service.dreams.cts;

import android.os.ServiceManager;
import android.service.dreams.DreamService;
import android.service.dreams.IDreamManager;

import junit.framework.TestCase;

public class DreamsFeatureTest extends TestCase {

    public void testDreamManagerExists() {
        IDreamManager service = IDreamManager.Stub.asInterface(
                            ServiceManager.getService(DreamService.DREAM_SERVICE));
        assertTrue("Dream manager service missing", service != null);
    }

}
