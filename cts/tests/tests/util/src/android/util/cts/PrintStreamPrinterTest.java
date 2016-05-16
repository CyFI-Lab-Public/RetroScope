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

package android.util.cts;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import android.test.AndroidTestCase;
import android.util.PrintStreamPrinter;

public class PrintStreamPrinterTest extends AndroidTestCase {
    private File mFile;
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mFile = new File(getContext().getFilesDir(), "PrintStreamPrinter.log");
        if (!mFile.exists()) {
            mFile.createNewFile();
        }
    }

    public void testConstructor() throws FileNotFoundException {
        new PrintStreamPrinter(new PrintStream(mFile));
    }

    public void testPrintln() throws FileNotFoundException, SecurityException, IOException {
        PrintStreamPrinter printStreamPrinter = null;
        final String message = "testMessageOfPrintStreamPrinter";
        InputStream is = null;

        PrintStream ps = new PrintStream(mFile);
        printStreamPrinter = new PrintStreamPrinter(ps);
        printStreamPrinter.println(message);
        ps.flush();
        ps.close();
        String mLine;

        try {
            is = new FileInputStream(mFile);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            mLine = reader.readLine();
            assertEquals(message, mLine);
            reader.close();
        } finally {
            is.close();
        }
    }

    @Override
    protected void tearDown() throws Exception {
        if (mFile.exists()) {
            mFile.delete();
        }
        super.tearDown();
    }
}
