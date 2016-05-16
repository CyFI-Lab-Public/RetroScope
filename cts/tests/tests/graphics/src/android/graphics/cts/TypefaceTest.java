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

package android.graphics.cts;


import android.graphics.Typeface;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.ParcelFileDescriptor;
import android.test.AndroidTestCase;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class TypefaceTest extends AndroidTestCase {

    // generic family name for monospaced fonts
    private static final String MONO = "monospace";
    private static final String DEFAULT = (String)null;
    private static final String INVALID = "invalid-family-name";

    // list of family names to try when attempting to find a typeface with a given style
    private static final String[] FAMILIES =
            { (String) null, "monospace", "serif", "sans-serif", "cursive", "arial", "times" };

    /**
     * Create a typeface of the given style. If the default font does not support the style,
     * a number of generic families are tried.
     * @return The typeface or null, if no typeface with the given style can be found.
     */
    private Typeface createTypeface(int style) {
        for (String family : FAMILIES) {
            Typeface tf = Typeface.create(family, style);
            if (tf.getStyle() == style) {
                return tf;
            }
        }
        return null;
    }


    public void testIsBold() {
        Typeface typeface = createTypeface(Typeface.BOLD);
        if (typeface != null) {
            assertEquals(Typeface.BOLD, typeface.getStyle());
            assertTrue(typeface.isBold());
            assertFalse(typeface.isItalic());
        }

        typeface = createTypeface(Typeface.ITALIC);
        if (typeface != null) {
            assertEquals(Typeface.ITALIC, typeface.getStyle());
            assertFalse(typeface.isBold());
            assertTrue(typeface.isItalic());
        }

        typeface = createTypeface(Typeface.BOLD_ITALIC);
        if (typeface != null) {
            assertEquals(Typeface.BOLD_ITALIC, typeface.getStyle());
            assertTrue(typeface.isBold());
            assertTrue(typeface.isItalic());
        }

        typeface = createTypeface(Typeface.NORMAL);
        if (typeface != null) {
            assertEquals(Typeface.NORMAL, typeface.getStyle());
            assertFalse(typeface.isBold());
            assertFalse(typeface.isItalic());
        }
    }

    public void testCreate() {
        Typeface typeface = Typeface.create(DEFAULT, Typeface.NORMAL);
        assertNotNull(typeface);
        typeface = Typeface.create(MONO, Typeface.BOLD);
        assertNotNull(typeface);
        typeface = Typeface.create(INVALID, Typeface.ITALIC);
        assertNotNull(typeface);

        typeface = Typeface.create(typeface, Typeface.NORMAL);
        assertNotNull(typeface);
        typeface = Typeface.create(typeface, Typeface.BOLD);
        assertNotNull(typeface);
    }

    public void testDefaultFromStyle() {
        Typeface typeface = Typeface.defaultFromStyle(Typeface.NORMAL);
        assertNotNull(typeface);
        typeface = Typeface.defaultFromStyle(Typeface.BOLD);
        assertNotNull(typeface);
        typeface = Typeface.defaultFromStyle(Typeface.ITALIC);
        assertNotNull(typeface);
        typeface = Typeface.defaultFromStyle(Typeface.BOLD_ITALIC);
        assertNotNull(typeface);
    }

    public void testConstants() {
        assertNotNull(Typeface.DEFAULT);
        assertNotNull(Typeface.DEFAULT_BOLD);
        assertNotNull(Typeface.MONOSPACE);
        assertNotNull(Typeface.SANS_SERIF);
        assertNotNull(Typeface.SERIF);
    }

    public void testCreateFromAsset() {
        // input abnormal params.
        try {
            Typeface.createFromAsset(null, null);
            fail("Should throw a NullPointerException.");
        } catch (NullPointerException e) {
            // except here
        }

        Typeface typeface = Typeface.createFromAsset(getContext().getAssets(), "samplefont.ttf");
        assertNotNull(typeface);
    }

    public void testCreateFromFile1() throws IOException {
        // input abnormal params.
        try {
            Typeface.createFromFile((File)null);
            fail("Should throw a NullPointerException.");
        } catch (NullPointerException e) {
            // except here
        }
        File file = new File(obtainPath());
        Typeface typeface = Typeface.createFromFile(file);
        assertNotNull(typeface);
    }

    public void testCreateFromFile2() throws IOException {
        // input abnormal params.
        try {
            Typeface.createFromFile((String)null);
            fail("Should throw a NullPointerException.");
        } catch (NullPointerException e) {
            // except here
        }

        Typeface typeface = Typeface.createFromFile(obtainPath());
        assertNotNull(typeface);
    }

    private String obtainPath() throws IOException {
        File dir = getContext().getFilesDir();
        dir.mkdirs();
        File file = new File(dir, "test.jpg");
        if (!file.createNewFile()) {
            if (!file.exists()) {
                fail("Failed to create new File!");
            }
        }
        InputStream is = getContext().getAssets().open("samplefont.ttf");
        FileOutputStream fOutput = new FileOutputStream(file);
        byte[] dataBuffer = new byte[1024];
        int readLength = 0;
        while ((readLength = is.read(dataBuffer)) != -1) {
            fOutput.write(dataBuffer, 0, readLength);
        }
        is.close();
        fOutput.close();
        return (file.getPath());
    }
}
