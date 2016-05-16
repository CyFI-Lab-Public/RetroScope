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

package android.graphics.cts;

import com.android.cts.stub.R;


import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory.Options;
import android.os.ParcelFileDescriptor;
import android.test.InstrumentationTestCase;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BitmapFactoryTest extends InstrumentationTestCase {
    private Resources mRes;
    // opt for non-null
    private BitmapFactory.Options mOpt1;
    // opt for null
    private BitmapFactory.Options mOpt2;
    // height and width of start.jpg
    private static final int START_HEIGHT = 31;
    private static final int START_WIDTH = 31;
    private int mDefaultDensity;
    private int mTargetDensity;

    // The test images, including baseline JPEG, a PNG, a GIF, a BMP AND a WEBP.
    private static int[] RES_IDS = new int[] {
            R.drawable.baseline_jpeg, R.drawable.png_test, R.drawable.gif_test,
            R.drawable.bmp_test, R.drawable.webp_test
    };
    private static String[] NAMES_TEMP_FILES = new String[] {
        "baseline_temp.jpg", "png_temp.png", "gif_temp.gif",
        "bmp_temp.bmp", "webp_temp.webp"
    };

    // The width and height of the above image.
    private static int WIDTHS[] = new int[] { 1280, 640, 320, 320, 640 };
    private static int HEIGHTS[] = new int[] { 960, 480, 240, 240, 480 };

    // Configurations for BitmapFactory.Options
    private static Config[] COLOR_CONFIGS = new Config[] {Config.ARGB_8888, Config.RGB_565,
        Config.ARGB_4444};
    private static int[] COLOR_TOLS = new int[] {16, 49, 576};

    private static int[] RAW_COLORS = new int[] {
        // raw data from R.drawable.premul_data
        Color.argb(255, 0, 0, 0),
        Color.argb(128, 255, 0, 0),
        Color.argb(128, 25, 26, 27),
        Color.argb(2, 255, 254, 253),
    };

    private static int[] DEPREMUL_COLORS = new int[] {
        // data from R.drawable.premul_data, after premultiplied store + un-premultiplied load
        Color.argb(255, 0, 0, 0),
        Color.argb(128, 255, 0, 0),
        Color.argb(128, 26, 26, 28),
        Color.argb(2, 255, 255, 255),
    };

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mRes = getInstrumentation().getTargetContext().getResources();
        mDefaultDensity = DisplayMetrics.DENSITY_DEFAULT;
        mTargetDensity = mRes.getDisplayMetrics().densityDpi;

        mOpt1 = new BitmapFactory.Options();
        mOpt1.inScaled = false;
        mOpt2 = new BitmapFactory.Options();
        mOpt2.inScaled = false;
        mOpt2.inJustDecodeBounds = true;
    }

    public void testConstructor() {
        // new the BitmapFactory instance
        new BitmapFactory();
    }

    public void testDecodeResource1() {
        Bitmap b = BitmapFactory.decodeResource(mRes, R.drawable.start,
                mOpt1);
        assertNotNull(b);
        // Test the bitmap size
        assertEquals(START_HEIGHT, b.getHeight());
        assertEquals(START_WIDTH, b.getWidth());
        // Test if no bitmap
        assertNull(BitmapFactory.decodeResource(mRes, R.drawable.start, mOpt2));
    }

    public void testDecodeResource2() {
        Bitmap b = BitmapFactory.decodeResource(mRes, R.drawable.start);
        assertNotNull(b);
        // Test the bitmap size
        assertEquals(START_HEIGHT * mTargetDensity / mDefaultDensity, b.getHeight(), 1.1);
        assertEquals(START_WIDTH * mTargetDensity / mDefaultDensity, b.getWidth(), 1.1);
    }

    public void testDecodeResourceStream() {
        InputStream is = obtainInputStream();
        Rect r = new Rect(1, 1, 1, 1);
        TypedValue value = new TypedValue();
        Bitmap b = BitmapFactory.decodeResourceStream(mRes, value, is, r, mOpt1);
        assertNotNull(b);
        // Test the bitmap size
        assertEquals(START_HEIGHT, b.getHeight());
        assertEquals(START_WIDTH, b.getWidth());
    }

    public void testDecodeByteArray1() {
        byte[] array = obtainArray();
        Bitmap b = BitmapFactory.decodeByteArray(array, 0, array.length, mOpt1);
        assertNotNull(b);
        // Test the bitmap size
        assertEquals(START_HEIGHT, b.getHeight());
        assertEquals(START_WIDTH, b.getWidth());
        // Test if no bitmap
        assertNull(BitmapFactory.decodeByteArray(array, 0, array.length, mOpt2));
    }

    public void testDecodeByteArray2() {
        byte[] array = obtainArray();
        Bitmap b = BitmapFactory.decodeByteArray(array, 0, array.length);
        assertNotNull(b);
        // Test the bitmap size
        assertEquals(START_HEIGHT, b.getHeight());
        assertEquals(START_WIDTH, b.getWidth());
    }

    public void testDecodeStream1() {
        InputStream is = obtainInputStream();
        Rect r = new Rect(1, 1, 1, 1);
        Bitmap b = BitmapFactory.decodeStream(is, r, mOpt1);
        assertNotNull(b);
        // Test the bitmap size
        assertEquals(START_HEIGHT, b.getHeight());
        assertEquals(START_WIDTH, b.getWidth());
        // Test if no bitmap
        assertNull(BitmapFactory.decodeStream(is, r, mOpt2));
    }

    public void testDecodeStream2() {
        InputStream is = obtainInputStream();
        Bitmap b = BitmapFactory.decodeStream(is);
        assertNotNull(b);
        // Test the bitmap size
        assertEquals(START_HEIGHT, b.getHeight());
        assertEquals(START_WIDTH, b.getWidth());
    }

    public void testDecodeStream3() throws IOException {
        for (int i = 0; i < RES_IDS.length; ++i) {
            InputStream is = obtainInputStream(RES_IDS[i]);
            Bitmap b = BitmapFactory.decodeStream(is);
            assertNotNull(b);
            // Test the bitmap size
            assertEquals(WIDTHS[i], b.getWidth());
            assertEquals(HEIGHTS[i], b.getHeight());
        }
    }

    public void testDecodeStream4() throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        for (int k = 0; k < COLOR_CONFIGS.length; ++k) {
            options.inPreferredConfig = COLOR_CONFIGS[k];

            // Decode the PNG & WebP test images. The WebP test image has been encoded from PNG test
            // image and should have same similar (within some error-tolerance) Bitmap data.
            InputStream iStreamPng = obtainInputStream(R.drawable.png_test);
            Bitmap bPng = BitmapFactory.decodeStream(iStreamPng, null, options);
            assertNotNull(bPng);
            assertEquals(bPng.getConfig(), COLOR_CONFIGS[k]);

            InputStream iStreamWebp1 = obtainInputStream(R.drawable.webp_test);
            Bitmap bWebp1 = BitmapFactory.decodeStream(iStreamWebp1, null, options);
            assertNotNull(bWebp1);
            compareBitmaps(bPng, bWebp1, COLOR_TOLS[k], true);

            // Compress the PNG image to WebP format (Quality=90) and decode it back.
            // This will test end-to-end WebP encoding and decoding.
            ByteArrayOutputStream oStreamWebp = new ByteArrayOutputStream();
            assertTrue(bPng.compress(CompressFormat.WEBP, 90, oStreamWebp));
            InputStream iStreamWebp2 = new ByteArrayInputStream(oStreamWebp.toByteArray());
            Bitmap bWebp2 = BitmapFactory.decodeStream(iStreamWebp2, null, options);
            assertNotNull(bWebp2);
            compareBitmaps(bPng, bWebp2, COLOR_TOLS[k], true);
        }
    }

    public void testDecodeFileDescriptor1() throws IOException {
        ParcelFileDescriptor pfd = obtainParcelDescriptor(obtainPath());
        FileDescriptor input = pfd.getFileDescriptor();
        Rect r = new Rect(1, 1, 1, 1);
        Bitmap b = BitmapFactory.decodeFileDescriptor(input, r, mOpt1);
        assertNotNull(b);
        // Test the bitmap size
        assertEquals(START_HEIGHT, b.getHeight());
        assertEquals(START_WIDTH, b.getWidth());
        // Test if no bitmap
        assertNull(BitmapFactory.decodeFileDescriptor(input, r, mOpt2));
    }

    public void testDecodeFileDescriptor2() throws IOException {
        ParcelFileDescriptor pfd = obtainParcelDescriptor(obtainPath());
        FileDescriptor input = pfd.getFileDescriptor();
        Bitmap b = BitmapFactory.decodeFileDescriptor(input);
        assertNotNull(b);
        // Test the bitmap size
        assertEquals(START_HEIGHT, b.getHeight());
        assertEquals(START_WIDTH, b.getWidth());
    }

    public void testDecodeFile1() throws IOException {
        Bitmap b = BitmapFactory.decodeFile(obtainPath(), mOpt1);
        assertNotNull(b);
        // Test the bitmap size
        assertEquals(START_HEIGHT, b.getHeight());
        assertEquals(START_WIDTH, b.getWidth());
        // Test if no bitmap
        assertNull(BitmapFactory.decodeFile(obtainPath(), mOpt2));
    }

    public void testDecodeFile2() throws IOException {
        Bitmap b = BitmapFactory.decodeFile(obtainPath());
        assertNotNull(b);
        // Test the bitmap size
        assertEquals(START_HEIGHT, b.getHeight());
        assertEquals(START_WIDTH, b.getWidth());
    }

    public void testDecodeReuseBasic() throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
        options.inSampleSize = 0; // treated as 1
        options.inScaled = false;
        Bitmap start = BitmapFactory.decodeResource(mRes, R.drawable.start, options);
        int originalSize = start.getByteCount();
        assertEquals(originalSize, start.getAllocationByteCount());

        options.inBitmap = start;
        options.inMutable = false; // will be overridden by non-null inBitmap
        options.inSampleSize = -42; // treated as 1
        Bitmap pass = BitmapFactory.decodeResource(mRes, R.drawable.pass, options);

        assertEquals(originalSize, pass.getByteCount());
        assertEquals(originalSize, pass.getAllocationByteCount());
        assertSame(start, pass);
        assertTrue(pass.isMutable());
    }

    public void testDecodeReuseFormats() throws IOException {
        // reuse should support all image formats
        for (int i = 0; i < RES_IDS.length; ++i) {
            Bitmap reuseBuffer = Bitmap.createBitmap(1000000, 1, Bitmap.Config.ALPHA_8);

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inBitmap = reuseBuffer;
            options.inSampleSize = 4;
            options.inScaled = false;
            Bitmap decoded = BitmapFactory.decodeResource(mRes, RES_IDS[i], options);
            assertSame(reuseBuffer, decoded);
        }
    }

    public void testDecodeReuseFailure() throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
        options.inScaled = false;
        options.inSampleSize = 4;
        Bitmap reduced = BitmapFactory.decodeResource(mRes, R.drawable.robot, options);

        options.inBitmap = reduced;
        options.inSampleSize = 1;
        try {
            Bitmap original = BitmapFactory.decodeResource(mRes, R.drawable.robot, options);
            fail("should throw exception due to lack of space");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testDecodeReuseScaling() throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
        options.inScaled = false;
        Bitmap original = BitmapFactory.decodeResource(mRes, R.drawable.robot, options);
        int originalSize = original.getByteCount();
        assertEquals(originalSize, original.getAllocationByteCount());

        options.inBitmap = original;
        options.inSampleSize = 4;
        Bitmap reduced = BitmapFactory.decodeResource(mRes, R.drawable.robot, options);

        assertSame(original, reduced);
        assertEquals(originalSize, reduced.getAllocationByteCount());
        assertEquals(originalSize, reduced.getByteCount() * 16);
    }

    public void testDecodeReuseDoubleScaling() throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
        options.inScaled = false;
        options.inSampleSize = 1;
        Bitmap original = BitmapFactory.decodeResource(mRes, R.drawable.robot, options);
        int originalSize = original.getByteCount();

        // Verify that inSampleSize and density scaling both work with reuse concurrently
        options.inBitmap = original;
        options.inScaled = true;
        options.inSampleSize = 2;
        options.inDensity = 2;
        options.inTargetDensity = 4;
        Bitmap doubleScaled = BitmapFactory.decodeResource(mRes, R.drawable.robot, options);

        assertSame(original, doubleScaled);
        assertEquals(4, doubleScaled.getDensity());
        assertEquals(originalSize, doubleScaled.getByteCount());
    }

    public void testDecodeReuseEquivalentScaling() throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
        options.inScaled = true;
        options.inDensity = 4;
        options.inTargetDensity = 2;
        Bitmap densityReduced = BitmapFactory.decodeResource(mRes, R.drawable.robot, options);
        assertEquals(2, densityReduced.getDensity());
        options.inBitmap = densityReduced;
        options.inDensity = 0;
        options.inScaled = false;
        options.inSampleSize = 2;
        Bitmap scaleReduced = BitmapFactory.decodeResource(mRes, R.drawable.robot, options);
        // verify that density isn't incorrectly carried over during bitmap reuse
        assertFalse(densityReduced.getDensity() == 2);
        assertFalse(densityReduced.getDensity() == 0);
        assertSame(densityReduced, scaleReduced);
    }

    public void testDecodePremultipliedDefault() throws IOException {
        Bitmap simplePremul = BitmapFactory.decodeResource(mRes, R.drawable.premul_data);
        assertTrue(simplePremul.isPremultiplied());
    }

    public void testDecodePremultipliedData() throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        Bitmap premul = BitmapFactory.decodeResource(mRes, R.drawable.premul_data, options);
        options.inPremultiplied = false;
        Bitmap unpremul = BitmapFactory.decodeResource(mRes, R.drawable.premul_data, options);
        assertEquals(premul.getConfig(), Bitmap.Config.ARGB_8888);
        assertEquals(unpremul.getConfig(), Bitmap.Config.ARGB_8888);
        assertTrue(premul.getHeight() == 1 && unpremul.getHeight() == 1);
        assertTrue(premul.getWidth() == unpremul.getWidth() &&
                   DEPREMUL_COLORS.length == RAW_COLORS.length &&
                   premul.getWidth() == DEPREMUL_COLORS.length);

        // verify pixel data - unpremul should have raw values, premul will have rounding errors
        for (int i = 0; i < premul.getWidth(); i++) {
            assertEquals(premul.getPixel(i, 0), DEPREMUL_COLORS[i]);
            assertEquals(unpremul.getPixel(i, 0), RAW_COLORS[i]);
        }
    }

    public void testDecodeInPurgeableAllocationCount() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;
        options.inJustDecodeBounds = false;
        options.inPurgeable = true;
        options.inInputShareable = false;
        byte[] array = obtainArray();
        Bitmap purgeableBitmap = BitmapFactory.decodeByteArray(array, 0, array.length, options);
        assertFalse(purgeableBitmap.getAllocationByteCount() == 0);
    }

    private byte[] obtainArray() {
        ByteArrayOutputStream stm = new ByteArrayOutputStream();
        Options opt = new BitmapFactory.Options();
        opt.inScaled = false;
        Bitmap bitmap = BitmapFactory.decodeResource(mRes, R.drawable.start, opt);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 0, stm);
        return(stm.toByteArray());
    }

    private InputStream obtainInputStream() {
        return mRes.openRawResource(R.drawable.start);
    }

    private InputStream obtainInputStream(int resId) {
        return mRes.openRawResource(resId);
    }

    private ParcelFileDescriptor obtainParcelDescriptor(String path)
            throws IOException {
        File file = new File(path);
        return(ParcelFileDescriptor.open(file,
                ParcelFileDescriptor.MODE_READ_ONLY));
    }

    private String obtainPath() throws IOException {
        File dir = getInstrumentation().getTargetContext().getFilesDir();
        dir.mkdirs();
        File file = new File(dir, "test.jpg");
        if (!file.createNewFile()) {
            if (!file.exists()) {
                fail("Failed to create new File!");
            }
        }
        InputStream is = obtainInputStream();
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

    // Compare expected to actual to see if their diff is less then mseMargin.
    // lessThanMargin is to indicate whether we expect the mean square error
    // to be "less than" or "no less than".
    private void compareBitmaps(Bitmap expected, Bitmap actual,
            int mseMargin, boolean lessThanMargin) {
        final int width = expected.getWidth();
        final int height = expected.getHeight();

        assertEquals("mismatching widths", width, actual.getWidth());
        assertEquals("mismatching heights", height, actual.getHeight());
        assertEquals("mismatching configs", expected.getConfig(),
                actual.getConfig());

        double mse = 0;
        int[] expectedColors = new int [width * height];
        int[] actualColors = new int [width * height];

        expected.getPixels(expectedColors, 0, width, 0, 0, width, height);
        actual.getPixels(actualColors, 0, width, 0, 0, width, height);

        for (int row = 0; row < height; ++row) {
            for (int col = 0; col < width; ++col) {
                int idx = row * width + col;
                mse += distance(expectedColors[idx], actualColors[idx]);
            }
        }
        mse /= width * height;

        if (lessThanMargin) {
            assertTrue("MSE too large for normal case: " + mse,
                    mse <= mseMargin);
        } else {
            assertFalse("MSE too small for abnormal case: " + mse,
                    mse <= mseMargin);
        }
    }

    private double distance(int expect, int actual) {
        final int r = Color.red(actual) - Color.red(expect);
        final int g = Color.green(actual) - Color.green(expect);
        final int b = Color.blue(actual) - Color.blue(expect);
        return r * r + g * g + b * b;
    }
}
