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

package android.util.cts;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import android.content.Context;
import android.test.AndroidTestCase;
import android.util.PrintWriterPrinter;

public class PrintWriterPrinterTest extends AndroidTestCase {
    private File mFile;
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        File dbDir = getContext().getDir("tests", Context.MODE_PRIVATE);
        mFile = new File(dbDir,"print.log");
        if (!mFile.exists())
            mFile.createNewFile();
    }

    public void testConstructor() {

        PrintWriterPrinter printWriterPrinter = null;

        try {
            PrintWriter pw = new PrintWriter(mFile);
            printWriterPrinter = new PrintWriterPrinter(pw);
        } catch (FileNotFoundException e) {
            fail("shouldn't throw exception");
        }
    }

    public void testPrintln() {
        PrintWriterPrinter printWriterPrinter = null;
        String mMessage = "testMessage";
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(mFile);
            printWriterPrinter = new PrintWriterPrinter(pw);
        } catch (FileNotFoundException e) {
            fail("shouldn't throw exception");
        }
        printWriterPrinter.println(mMessage);
        pw.flush();
        pw.close();
        String mLine = "";
        try {
            InputStream is = new FileInputStream(mFile);
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is));
            mLine = reader.readLine();
        } catch (Exception e) {
        }
        assertEquals(mMessage, mLine);
    }

    @Override
    protected void tearDown() throws Exception {
        if (mFile.exists())
            mFile.delete();
    }

}

