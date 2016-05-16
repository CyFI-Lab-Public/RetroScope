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
package com.android.cts.tradefed.testtype;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

import junit.framework.TestCase;

/**
 * Unit tests for {@link TestPackageDef}.
 */
public class TestPackageDefTest extends TestCase {

    /**
     * Regression test for {@link TestPackageDef#generateDigest(File, String)} that ensures expected
     * digest is generated for fixed data.
     */
    public void testGenerateDigest() {
        TestPackageDef def = new TestPackageDef() {
          @Override
          InputStream getFileStream(File dir, String fileName) {
              return new ByteArrayInputStream("test data for digest".getBytes());
          }
        };
        String digest = def.generateDigest(new File("unused"), "alsounused");
        assertNotNull(digest);
        assertEquals("58c222b5f5f81b4b58891ec59924b9b2f530452e", digest);

    }

}
