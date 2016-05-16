/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.providers.downloads;

import android.provider.Downloads;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.LargeTest;

/**
 * This test exercises methods in the {@Helpers} utility class.
 */
@LargeTest
public class HelpersTest extends AndroidTestCase {

    public HelpersTest() {
    }

    public void testGetFullPath() throws Exception {
      String hint = "file:///com.android.providers.downloads/test";

      // Test that we never change requested filename.
      String fileName = Helpers.getFullPath(
          hint,
          "video/mp4", // MIME type corresponding to file extension .mp4
          Downloads.Impl.DESTINATION_FILE_URI,
          null);
      assertEquals(hint, fileName);
    }

}
