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

package com.android.cts.appaccessdata;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import android.test.AndroidTestCase;

/**
 * Test that another app's private data cannot be accessed, while its public data can.
 *
 * Assumes that {@link APP_WITH_DATA_PKG} has already created the private and public data.
 */
public class AccessPrivateDataTest extends AndroidTestCase {

    /**
     * The Android package name of the application that owns the data
     */
    private static final String APP_WITH_DATA_PKG = "com.android.cts.appwithdata";

    /**
     * Name of private file to access. This must match the name of the file created by
     * {@link APP_WITH_DATA_PKG}.
     */
    private static final String PRIVATE_FILE_NAME = "private_file.txt";
    /**
     * Name of public file to access. This must match the name of the file created by
     * {@link APP_WITH_DATA_PKG}.
     */
    private static final String PUBLIC_FILE_NAME = "public_file.txt";

    /**
     * Tests that another app's private data cannot be accessed. It includes file
     * and detailed traffic stats.
     * @throws IOException
     */
    public void testAccessPrivateData() throws IOException {
        try {
            // construct the absolute file path to the app's private file
            String privateFilePath = String.format("/data/data/%s/%s", APP_WITH_DATA_PKG,
                    PRIVATE_FILE_NAME);
            FileInputStream inputStream = new FileInputStream(privateFilePath);
            inputStream.read();
            inputStream.close();
            fail("Was able to access another app's private data");
        } catch (FileNotFoundException e) {
            // expected
        } catch (SecurityException e) {
            // also valid
        }
        accessPrivateTrafficStats();
    }

    /**
     * Tests that another app's public file can be accessed
     * @throws IOException
     */
    public void testAccessPublicData() throws IOException {
        try {
            getOtherAppUid();
        } catch (FileNotFoundException e) {
            fail("Was not able to access another app's public file: " + e);
        } catch (SecurityException e) {
            fail("Was not able to access another app's public file: " + e);
        }
    }

    private int getOtherAppUid() throws IOException, FileNotFoundException, SecurityException {
        // construct the absolute file path to the other app's public file
        String publicFilePath = String.format("/data/data/%s/files/%s", APP_WITH_DATA_PKG,
                PUBLIC_FILE_NAME);
        DataInputStream inputStream = new DataInputStream(new FileInputStream(publicFilePath));
        int otherAppUid = (int)inputStream.readInt();
        inputStream.close();
        return otherAppUid;
    }

    private void accessPrivateTrafficStats() throws IOException {
        int otherAppUid = -1;
        try {
            otherAppUid = getOtherAppUid();
        } catch (FileNotFoundException e) {
            fail("Was not able to access another app's public file: " + e);
        } catch (SecurityException e) {
            fail("Was not able to access another app's public file: " + e);
        }

        boolean foundOtherStats = false;
        try {
            BufferedReader qtaguidReader = new BufferedReader(new FileReader("/proc/net/xt_qtaguid/stats"));
            String line;
            while ((line = qtaguidReader.readLine()) != null) {
                String tokens[] = line.split(" ");
                if (tokens.length > 3 && tokens[3].equals(String.valueOf(otherAppUid))) {
                    foundOtherStats = true;
                    if (!tokens[2].equals("0x0")) {
                        fail("Other apps detailed traffic stats leaked");
                    }
                }
            }
            qtaguidReader.close();
        } catch (FileNotFoundException e) {
            fail("Was not able to access qtaguid/stats: " + e);
        }
        assertTrue("Was expecting to find other apps' traffic stats", foundOtherStats);
    }
}
