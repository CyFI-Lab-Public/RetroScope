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

package android.graphics.cts;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.ParcelFileDescriptor;
import android.test.InstrumentationTestCase;
import android.util.Log;

import com.android.cts.stub.R;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class BitmapRegionDecoderTest extends InstrumentationTestCase {
    private static final String TAG = "BitmapRegionDecoderTest";
    private ArrayList<File> mFilesCreated = new ArrayList<File>(
            NAMES_TEMP_FILES.length);

    private Resources mRes;

    // The test images, including baseline JPEGs and progressive JPEGs, a PNG,
    // a WEBP, a GIF and a BMP.
    private static int[] RES_IDS = new int[] {
            R.drawable.baseline_jpeg, R.drawable.progressive_jpeg,
            R.drawable.baseline_restart_jpeg,
            R.drawable.progressive_restart_jpeg,
            R.drawable.png_test, R.drawable.webp_test,
            R.drawable.gif_test, R.drawable.bmp_test
    };
    private static String[] NAMES_TEMP_FILES = new String[] {
        "baseline_temp.jpg", "progressive_temp.jpg", "baseline_restart_temp.jpg",
        "progressive_restart_temp.jpg", "png_temp.png", "webp_temp.webp",
        "gif_temp.gif", "bmp_temp.bmp"
    };

    // The width and height of the above image.
    // -1 denotes that the image format is not supported by BitmapRegionDecoder
    private static int WIDTHS[] = new int[] {
            1280, 1280, 1280, 1280, 640, 640, -1, -1};
    private static int HEIGHTS[] = new int[] {960, 960, 960, 960, 480, 480, -1, -1};

    // The number of test images, format of which is supported by BitmapRegionDecoder
    private static int NUM_TEST_IMAGES = 6;

    private static int TILE_SIZE = 256;

    // Configurations for BitmapFactory.Options
    private static Config[] COLOR_CONFIGS = new Config[] {Config.ARGB_8888,
            Config.RGB_565};
    private static int[] SAMPLESIZES = new int[] {1, 4};

    private int[] mExpectedColors = new int [TILE_SIZE * TILE_SIZE];
    private int[] mActualColors = new int [TILE_SIZE * TILE_SIZE];

    // We allow a certain degree of discrepancy between the tile-based decoding
    // result and the regular decoding result, because the two decoders may have
    // different implementations. The allowable discrepancy is set to a mean
    // square error of 3 * (1 * 1) among the RGB values.
    private int mMseMargin = 3 * (1 * 1);

    // MSE margin for WebP Region-Decoding for 'Config.RGB_565' is little bigger.
    private int mMseMarginWebPConfigRgb565 = 5;


    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mRes = getInstrumentation().getTargetContext().getResources();
    }

    @Override
    protected void tearDown() throws Exception {
        for (File file : mFilesCreated) {
            file.delete();
        }
        super.tearDown();
    }

    public void testNewInstanceInputStream() throws IOException {
        for (int i = 0; i < RES_IDS.length; ++i) {
            InputStream is = obtainInputStream(RES_IDS[i]);
            try {
                BitmapRegionDecoder decoder =
                        BitmapRegionDecoder.newInstance(is, false);
                assertEquals(WIDTHS[i], decoder.getWidth());
                assertEquals(HEIGHTS[i], decoder.getHeight());
            } catch (IOException e) {
                assertEquals(WIDTHS[i], -1);
                assertEquals(HEIGHTS[i], -1);
            } finally {
                if (is != null) {
                    is.close();
                }
            }
        }
    }

    public void testNewInstanceByteArray() throws IOException {
        for (int i = 0; i < RES_IDS.length; ++i) {
            byte[] imageData = obtainByteArray(RES_IDS[i]);
            try {
                BitmapRegionDecoder decoder = BitmapRegionDecoder
                        .newInstance(imageData, 0, imageData.length, false);
                assertEquals(WIDTHS[i], decoder.getWidth());
                assertEquals(HEIGHTS[i], decoder.getHeight());
            } catch (IOException e) {
                assertEquals(WIDTHS[i], -1);
                assertEquals(HEIGHTS[i], -1);
            }
        }
    }

    public void testNewInstanceStringAndFileDescriptor() throws IOException {
        for (int i = 0; i < RES_IDS.length; ++i) {
            String filepath = obtainPath(i);
            ParcelFileDescriptor pfd = obtainParcelDescriptor(filepath);
            FileDescriptor fd = pfd.getFileDescriptor();
            try {
                BitmapRegionDecoder decoder1 =
                        BitmapRegionDecoder.newInstance(filepath, false);
                assertEquals(WIDTHS[i], decoder1.getWidth());
                assertEquals(HEIGHTS[i], decoder1.getHeight());

                BitmapRegionDecoder decoder2 =
                        BitmapRegionDecoder.newInstance(fd, false);
                assertEquals(WIDTHS[i], decoder2.getWidth());
                assertEquals(HEIGHTS[i], decoder2.getHeight());
            } catch (IOException e) {
                assertEquals(WIDTHS[i], -1);
                assertEquals(HEIGHTS[i], -1);
            }
        }
    }

    public void testDecodeRegionInputStream() throws IOException {
        Options opts = new BitmapFactory.Options();
        for (int i = 0; i < NUM_TEST_IMAGES; ++i) {
            for (int j = 0; j < SAMPLESIZES.length; ++j) {
                for (int k = 0; k < COLOR_CONFIGS.length; ++k) {
                    opts.inSampleSize = SAMPLESIZES[j];
                    opts.inPreferredConfig = COLOR_CONFIGS[k];

                    InputStream is1 = obtainInputStream(RES_IDS[i]);
                    BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(is1, false);
                    InputStream is2 = obtainInputStream(RES_IDS[i]);
                    Bitmap wholeImage = BitmapFactory.decodeStream(is2, null, opts);

                    if (RES_IDS[i] == R.drawable.webp_test && COLOR_CONFIGS[k] == Config.RGB_565) {
                        compareRegionByRegion(decoder, opts, mMseMarginWebPConfigRgb565,
                                              wholeImage);
                    } else {
                        compareRegionByRegion(decoder, opts, mMseMargin, wholeImage);
                    }
                    wholeImage.recycle();
                }
            }
        }
    }

    public void testDecodeRegionInputStreamInBitmap() throws IOException {
        Options opts = new BitmapFactory.Options();
        for (int i = 0; i < NUM_TEST_IMAGES; ++i) {
            for (int j = 0; j < SAMPLESIZES.length; ++j) {
                for (int k = 0; k < COLOR_CONFIGS.length; ++k) {
                    opts.inSampleSize = SAMPLESIZES[j];
                    opts.inPreferredConfig = COLOR_CONFIGS[k];
                    opts.inBitmap = null;

                    InputStream is1 = obtainInputStream(RES_IDS[i]);
                    BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(is1, false);
                    InputStream is2 = obtainInputStream(RES_IDS[i]);
                    Bitmap wholeImage = BitmapFactory.decodeStream(is2, null, opts);

                    // setting inBitmap enables several checks within compareRegionByRegion
                    opts.inBitmap = Bitmap.createBitmap(
                            wholeImage.getWidth(), wholeImage.getHeight(), opts.inPreferredConfig);

                    if (RES_IDS[i] == R.drawable.webp_test && COLOR_CONFIGS[k] == Config.RGB_565) {
                        compareRegionByRegion(decoder, opts, mMseMarginWebPConfigRgb565,
                                              wholeImage);
                    } else {
                        compareRegionByRegion(decoder, opts, mMseMargin, wholeImage);
                    }
                    wholeImage.recycle();
                }
            }
        }
    }

    public void testDecodeRegionByteArray() throws IOException {
        Options opts = new BitmapFactory.Options();
        for (int i = 0; i < NUM_TEST_IMAGES; ++i) {
            for (int j = 0; j < SAMPLESIZES.length; ++j) {
                for (int k = 0; k < COLOR_CONFIGS.length; ++k) {
                    opts.inSampleSize = SAMPLESIZES[j];
                    opts.inPreferredConfig = COLOR_CONFIGS[k];

                    byte[] imageData = obtainByteArray(RES_IDS[i]);
                    BitmapRegionDecoder decoder = BitmapRegionDecoder
                            .newInstance(imageData, 0, imageData.length, false);
                    Bitmap wholeImage = BitmapFactory.decodeByteArray(imageData,
                            0, imageData.length, opts);

                    if (RES_IDS[i] == R.drawable.webp_test && COLOR_CONFIGS[k] == Config.RGB_565) {
                        compareRegionByRegion(decoder, opts, mMseMarginWebPConfigRgb565,
                                              wholeImage);
                    } else {
                        compareRegionByRegion(decoder, opts, mMseMargin, wholeImage);
                    }
                    wholeImage.recycle();
                }
            }
        }
    }

    public void testDecodeRegionStringAndFileDescriptor() throws IOException {
        Options opts = new BitmapFactory.Options();
        for (int i = 0; i < NUM_TEST_IMAGES; ++i) {
            String filepath = obtainPath(i);
            for (int j = 0; j < SAMPLESIZES.length; ++j) {
                for (int k = 0; k < COLOR_CONFIGS.length; ++k) {
                    opts.inSampleSize = SAMPLESIZES[j];
                    opts.inPreferredConfig = COLOR_CONFIGS[k];

                    BitmapRegionDecoder decoder =
                        BitmapRegionDecoder.newInstance(filepath, false);
                    Bitmap wholeImage = BitmapFactory.decodeFile(filepath, opts);
                    if (RES_IDS[i] == R.drawable.webp_test && COLOR_CONFIGS[k] == Config.RGB_565) {
                        compareRegionByRegion(decoder, opts, mMseMarginWebPConfigRgb565,
                                              wholeImage);
                    } else {
                        compareRegionByRegion(decoder, opts, mMseMargin, wholeImage);
                    }

                    ParcelFileDescriptor pfd1 = obtainParcelDescriptor(filepath);
                    FileDescriptor fd1 = pfd1.getFileDescriptor();
                    decoder = BitmapRegionDecoder.newInstance(fd1, false);
                    ParcelFileDescriptor pfd2 = obtainParcelDescriptor(filepath);
                    FileDescriptor fd2 = pfd2.getFileDescriptor();
                    if (RES_IDS[i] == R.drawable.webp_test && COLOR_CONFIGS[k] == Config.RGB_565) {
                        compareRegionByRegion(decoder, opts, mMseMarginWebPConfigRgb565,
                                              wholeImage);
                    } else {
                        compareRegionByRegion(decoder, opts, mMseMargin, wholeImage);
                    }
                    wholeImage.recycle();
                }
            }
        }
    }

    public void testRecycle() throws IOException {
        InputStream is = obtainInputStream(RES_IDS[0]);
        BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(is, false);
        decoder.recycle();
        assertTrue(decoder.isRecycled());
        try {
            decoder.getWidth();
            fail("Should throw an exception!");
        } catch (Exception e) {
        }

        try {
            decoder.getHeight();
            fail("Should throw an exception!");
        } catch (Exception e) {
        }

        Rect rect = new Rect(0, 0, WIDTHS[0], HEIGHTS[0]);
        BitmapFactory.Options opts = new BitmapFactory.Options();
        try {
            decoder.decodeRegion(rect, opts);
            fail("Should throw an exception!");
        } catch (Exception e) {
        }
    }

    private void compareRegionByRegion(BitmapRegionDecoder decoder,
            Options opts, int mseMargin, Bitmap wholeImage) {
        int width = decoder.getWidth();
        int height = decoder.getHeight();
        Rect rect = new Rect(0, 0, width, height);
        int numCols = (width + TILE_SIZE - 1) / TILE_SIZE;
        int numRows = (height + TILE_SIZE - 1) / TILE_SIZE;
        Bitmap actual;
        Bitmap expected;

        for (int i = 0; i < numCols; ++i) {
            for (int j = 0; j < numRows; ++j) {
                Rect rect1 = new Rect(i * TILE_SIZE, j * TILE_SIZE,
                        (i + 1) * TILE_SIZE, (j + 1) * TILE_SIZE);
                rect1.intersect(rect);
                actual = decoder.decodeRegion(rect1, opts);
                int left = rect1.left / opts.inSampleSize;
                int top = rect1.top / opts.inSampleSize;
                if (opts.inBitmap != null) {
                    // bitmap reuse path - ensure reuse worked
                    assertSame(opts.inBitmap, actual);
                    int currentWidth = rect1.width() / opts.inSampleSize;
                    int currentHeight = rect1.height() / opts.inSampleSize;
                    Rect actualRect = new Rect(0, 0, currentWidth, currentHeight);
                    // crop 'actual' to the size to be tested (and avoid recycling inBitmap)
                    actual = cropBitmap(actual, actualRect);
                }
                Rect expectedRect = new Rect(left, top, left + actual.getWidth(),
                        top + actual.getHeight());
                expected = cropBitmap(wholeImage, expectedRect);
                compareBitmaps(expected, actual, mseMargin, true);
                actual.recycle();
                expected.recycle();
            }
        }
    }

    private Bitmap cropBitmap(Bitmap wholeImage, Rect rect) {
        Bitmap cropped = Bitmap.createBitmap(rect.width(), rect.height(),
                wholeImage.getConfig());
        Canvas canvas = new Canvas(cropped);
        Rect dst = new Rect(0, 0, rect.width(), rect.height());
        canvas.drawBitmap(wholeImage, rect, dst, null);
        return cropped;
    }

    private InputStream obtainInputStream(int resId) {
        return mRes.openRawResource(resId);
    }

    private byte[] obtainByteArray(int resId) throws IOException {
        InputStream is = obtainInputStream(resId);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int readLength;
        while ((readLength = is.read(buffer)) != -1) {
            os.write(buffer, 0, readLength);
        }
        byte[] data = os.toByteArray();
        is.close();
        os.close();
        return data;
    }

    private String obtainPath(int idx) throws IOException {
        File dir = getInstrumentation().getTargetContext().getFilesDir();
        dir.mkdirs();
        File file = new File(dir, NAMES_TEMP_FILES[idx]);
        InputStream is = obtainInputStream(RES_IDS[idx]);
        FileOutputStream fOutput = new FileOutputStream(file);
        mFilesCreated.add(file);
        byte[] dataBuffer = new byte[1024];
        int readLength = 0;
        while ((readLength = is.read(dataBuffer)) != -1) {
            fOutput.write(dataBuffer, 0, readLength);
        }
        is.close();
        fOutput.close();
        return (file.getPath());
    }

    private ParcelFileDescriptor obtainParcelDescriptor(String path)
            throws IOException {
        File file = new File(path);
        return(ParcelFileDescriptor.open(file,
                ParcelFileDescriptor.MODE_READ_ONLY));
    }


    // Compare expected to actual to see if their diff is less then mseMargin.
    // lessThanMargin is to indicate whether we expect the diff to be
    // "less than" or "no less than".
    private void compareBitmaps(Bitmap expected, Bitmap actual,
            int mseMargin, boolean lessThanMargin) {
        assertEquals("mismatching widths", expected.getWidth(),
                actual.getWidth());
        assertEquals("mismatching heights", expected.getHeight(),
                actual.getHeight());

        double mse = 0;
        int width = expected.getWidth();
        int height = expected.getHeight();
        int[] expectedColors;
        int[] actualColors;
        if (width == TILE_SIZE && height == TILE_SIZE) {
            expectedColors = mExpectedColors;
            actualColors = mActualColors;
        } else {
            expectedColors = new int [width * height];
            actualColors = new int [width * height];
        }

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

    private double distance(int exp, int actual) {
        int r = Color.red(actual) - Color.red(exp);
        int g = Color.green(actual) - Color.green(exp);
        int b = Color.blue(actual) - Color.blue(exp);
        return r * r + g * g + b * b;
    }
}
