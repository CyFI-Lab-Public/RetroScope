/*
 * Copyright (C) 2013 The Android Open Source Project
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

package android.media.cts;

import android.app.Presentation;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.opengl.GLES20;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.test.AndroidTestCase;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;


/**
 * Tests connecting a virtual display to the input of a MediaCodec encoder.
 * <p>
 * Other test cases exercise these independently in more depth.  The goal here is to make sure
 * that virtual displays and MediaCodec can be used together.
 * <p>
 * We can't control frame-by-frame what appears on the virtual display, because we're
 * just throwing a Presentation and a View at it.  Further, it's possible that frames
 * will be dropped if they arrive faster than they are consumed, so any given frame
 * may not appear at all.  We can't wait for a series of actions to complete by watching
 * the output, because the frames are going directly to the encoder, and the encoder may
 * collect a number of frames before producing output.
 * <p>
 * The test puts up a series of colored screens, expecting to see all of them, and in order.
 * Any black screens that appear before or after are ignored.
 */
public class EncodeVirtualDisplayTest extends AndroidTestCase {
    private static final String TAG = "EncodeVirtualTest";
    private static final boolean VERBOSE = false;           // lots of logging
    private static final boolean DEBUG_SAVE_FILE = false;   // save copy of encoded movie
    private static final String DEBUG_FILE_NAME_BASE = "/sdcard/test.";

    // Virtual display characteristics.  Scaled down from full display size because not all
    // devices can encode at the resolution of their own display.
    private static final String NAME = TAG;
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final int DENSITY = DisplayMetrics.DENSITY_HIGH;
    private static final int UI_TIMEOUT_MS = 2000;
    private static final int UI_RENDER_PAUSE_MS = 200;

    // Encoder parameters.  We use the same width/height as the virtual display.
    private static final String MIME_TYPE = "video/avc";
    private static final int FRAME_RATE = 15;               // 15fps
    private static final int IFRAME_INTERVAL = 10;          // 10 seconds between I-frames
    private static final int BIT_RATE = 6000000;            // 6Mbps

    // Colors to test (RGB).  These must convert cleanly to and from BT.601 YUV.
    private static final int TEST_COLORS[] = {
        makeColor(10, 100, 200),        // YCbCr 89,186,82
        makeColor(100, 200, 10),        // YCbCr 144,60,98
        makeColor(200, 10, 100),        // YCbCr 203,10,103
        makeColor(10, 200, 100),        // YCbCr 130,113,52
        makeColor(100, 10, 200),        // YCbCr 67,199,154
        makeColor(200, 100, 10),        // YCbCr 119,74,179
    };

    private final ByteBuffer mPixelBuf = ByteBuffer.allocateDirect(4);
    private Handler mUiHandler;                             // Handler on main Looper
    private DisplayManager mDisplayManager;
    volatile boolean mInputDone;

    /* TEST_COLORS static initialization; need ARGB for ColorDrawable */
    private static int makeColor(int red, int green, int blue) {
        return 0xff << 24 | (red & 0xff) << 16 | (green & 0xff) << 8 | (blue & 0xff);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mUiHandler = new Handler(Looper.getMainLooper());
        mDisplayManager = (DisplayManager)mContext.getSystemService(Context.DISPLAY_SERVICE);
    }

    /**
     * Basic test.
     *
     * @throws Exception
     */
    public void testEncodeVirtualDisplay() throws Throwable {
        EncodeVirtualWrapper.runTest(this);
    }

    /**
     * Wraps encodeVirtualTest, running it in a new thread.  Required because of the way
     * SurfaceTexture.OnFrameAvailableListener works when the current thread has a Looper
     * configured.
     */
    private static class EncodeVirtualWrapper implements Runnable {
        private Throwable mThrowable;
        private EncodeVirtualDisplayTest mTest;

        private EncodeVirtualWrapper(EncodeVirtualDisplayTest test) {
            mTest = test;
        }

        @Override
        public void run() {
            try {
                mTest.encodeVirtualDisplayTest();
            } catch (Throwable th) {
                mThrowable = th;
            }
        }

        /** Entry point. */
        public static void runTest(EncodeVirtualDisplayTest obj) throws Throwable {
            EncodeVirtualWrapper wrapper = new EncodeVirtualWrapper(obj);
            Thread th = new Thread(wrapper, "codec test");
            th.start();
            th.join();
            if (wrapper.mThrowable != null) {
                throw wrapper.mThrowable;
            }
        }
    }

    /**
     * Prepares the encoder, decoder, and virtual display.
     */
    private void encodeVirtualDisplayTest() {
        MediaCodec encoder = null;
        MediaCodec decoder = null;
        OutputSurface outputSurface = null;
        VirtualDisplay virtualDisplay = null;

        try {
            // Encoded video resolution matches virtual display.
            MediaFormat encoderFormat = MediaFormat.createVideoFormat(MIME_TYPE, WIDTH, HEIGHT);
            encoderFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            encoderFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
            encoderFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
            encoderFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);

            encoder = MediaCodec.createEncoderByType(MIME_TYPE);
            encoder.configure(encoderFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            Surface inputSurface = encoder.createInputSurface();
            encoder.start();

            // Create a virtual display that will output to our encoder.
            virtualDisplay = mDisplayManager.createVirtualDisplay(NAME,
                    WIDTH, HEIGHT, DENSITY, inputSurface, 0);

            // We also need a decoder to check the output of the encoder.
            decoder = MediaCodec.createDecoderByType(MIME_TYPE);
            MediaFormat decoderFormat = MediaFormat.createVideoFormat(MIME_TYPE, WIDTH, HEIGHT);
            outputSurface = new OutputSurface(WIDTH, HEIGHT);
            decoder.configure(decoderFormat, outputSurface.getSurface(), null, 0);
            decoder.start();

            // Run the color slide show on a separate thread.
            mInputDone = false;
            new ColorSlideShow(virtualDisplay.getDisplay()).start();

            // Record everything we can and check the results.
            doTestEncodeVirtual(encoder, decoder, outputSurface);

        } finally {
            if (VERBOSE) Log.d(TAG, "releasing codecs, surfaces, and virtual display");
            if (virtualDisplay != null) {
                virtualDisplay.release();
            }
            if (outputSurface != null) {
                outputSurface.release();
            }
            if (encoder != null) {
                encoder.stop();
                encoder.release();
            }
            if (decoder != null) {
                decoder.stop();
                decoder.release();
            }
        }
    }

    /**
     * Drives the encoder and decoder.
     */
    private void doTestEncodeVirtual(MediaCodec encoder, MediaCodec decoder,
            OutputSurface outputSurface) {
        final int TIMEOUT_USEC = 10000;
        ByteBuffer[] encoderOutputBuffers = encoder.getOutputBuffers();
        ByteBuffer[] decoderInputBuffers = decoder.getInputBuffers();
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        boolean inputEosSignaled = false;
        int lastIndex = -1;
        int goodFrames = 0;
        int debugFrameCount = 0;

        // Save a copy to disk.  Useful for debugging the test.  Note this is a raw elementary
        // stream, not a .mp4 file, so not all players will know what to do with it.
        FileOutputStream outputStream = null;
        if (DEBUG_SAVE_FILE) {
            String fileName = DEBUG_FILE_NAME_BASE + WIDTH + "x" + HEIGHT + ".mp4";
            try {
                outputStream = new FileOutputStream(fileName);
                Log.d(TAG, "encoded output will be saved as " + fileName);
            } catch (IOException ioe) {
                Log.w(TAG, "Unable to create debug output file " + fileName);
                throw new RuntimeException(ioe);
            }
        }

        // Loop until the output side is done.
        boolean encoderDone = false;
        boolean outputDone = false;
        while (!outputDone) {
            if (VERBOSE) Log.d(TAG, "loop");

            if (!inputEosSignaled && mInputDone) {
                if (VERBOSE) Log.d(TAG, "signaling input EOS");
                encoder.signalEndOfInputStream();
                inputEosSignaled = true;
            }

            boolean decoderOutputAvailable = true;
            boolean encoderOutputAvailable = !encoderDone;
            while (decoderOutputAvailable || encoderOutputAvailable) {
                // Start by draining any pending output from the decoder.  It's important to
                // do this before we try to stuff any more data in.
                int decoderStatus = decoder.dequeueOutputBuffer(info, TIMEOUT_USEC);
                if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // no output available yet
                    if (VERBOSE) Log.d(TAG, "no output from decoder available");
                    decoderOutputAvailable = false;
                } else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    if (VERBOSE) Log.d(TAG, "decoder output buffers changed (but we don't care)");
                } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    // this happens before the first frame is returned
                    MediaFormat decoderOutputFormat = decoder.getOutputFormat();
                    if (VERBOSE) Log.d(TAG, "decoder output format changed: " +
                            decoderOutputFormat);
                } else if (decoderStatus < 0) {
                    fail("unexpected result from deocder.dequeueOutputBuffer: " + decoderStatus);
                } else {  // decoderStatus >= 0
                    if (VERBOSE) Log.d(TAG, "surface decoder given buffer " + decoderStatus +
                            " (size=" + info.size + ")");
                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        if (VERBOSE) Log.d(TAG, "output EOS");
                        outputDone = true;
                    }

                    // The ByteBuffers are null references, but we still get a nonzero size for
                    // the decoded data.
                    boolean doRender = (info.size != 0);

                    // As soon as we call releaseOutputBuffer, the buffer will be forwarded
                    // to SurfaceTexture to convert to a texture.  The API doesn't guarantee
                    // that the texture will be available before the call returns, so we
                    // need to wait for the onFrameAvailable callback to fire.  If we don't
                    // wait, we risk dropping frames.
                    outputSurface.makeCurrent();
                    decoder.releaseOutputBuffer(decoderStatus, doRender);
                    if (doRender) {
                        if (VERBOSE) Log.d(TAG, "awaiting frame " + (lastIndex+1));
                        outputSurface.awaitNewImage();
                        outputSurface.drawImage();
                        int foundIndex = checkSurfaceFrame();
                        if (foundIndex == lastIndex + 1) {
                            // found the next one in the series
                            lastIndex = foundIndex;
                            goodFrames++;
                        } else if (foundIndex == lastIndex) {
                            // Sometimes we see the same color two frames in a row.
                            if (VERBOSE) Log.d(TAG, "Got another " + lastIndex);
                        } else if (foundIndex > 0) {
                            // Looks like we missed a color frame.  It's possible something
                            // stalled and we dropped a frame.  Skip forward to see if we
                            // can catch the rest.
                            if (foundIndex < lastIndex) {
                                Log.w(TAG, "Ignoring backward skip from " +
                                    lastIndex + " to " + foundIndex);
                            } else {
                                Log.w(TAG, "Frame skipped, advancing lastIndex from " +
                                        lastIndex + " to " + foundIndex);
                                goodFrames++;
                                lastIndex = foundIndex;
                            }
                        }
                    }
                }
                if (decoderStatus != MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // Continue attempts to drain output.
                    continue;
                }

                // Decoder is drained, check to see if we've got a new buffer of output from
                // the encoder.
                if (!encoderDone) {
                    int encoderStatus = encoder.dequeueOutputBuffer(info, TIMEOUT_USEC);
                    if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        // no output available yet
                        if (VERBOSE) Log.d(TAG, "no output from encoder available");
                        encoderOutputAvailable = false;
                    } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                        // not expected for an encoder
                        encoderOutputBuffers = encoder.getOutputBuffers();
                        if (VERBOSE) Log.d(TAG, "encoder output buffers changed");
                    } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        // received before first buffer
                        MediaFormat newFormat = encoder.getOutputFormat();
                        if (VERBOSE) Log.d(TAG, "encoder output format changed: " + newFormat);
                    } else if (encoderStatus < 0) {
                        fail("unexpected result from encoder.dequeueOutputBuffer: " + encoderStatus);
                    } else { // encoderStatus >= 0
                        ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                        if (encodedData == null) {
                            fail("encoderOutputBuffer " + encoderStatus + " was null");
                        }

                        // It's usually necessary to adjust the ByteBuffer values to match BufferInfo.
                        encodedData.position(info.offset);
                        encodedData.limit(info.offset + info.size);

                        if (outputStream != null) {
                            byte[] data = new byte[info.size];
                            encodedData.get(data);
                            encodedData.position(info.offset);
                            try {
                                outputStream.write(data);
                            } catch (IOException ioe) {
                                Log.w(TAG, "failed writing debug data to file");
                                throw new RuntimeException(ioe);
                            }
                            debugFrameCount++;
                        }

                        // Get a decoder input buffer, blocking until it's available.  We just
                        // drained the decoder output, so we expect there to be a free input
                        // buffer now or in the near future (i.e. this should never deadlock
                        // if the codec is meeting requirements).
                        //
                        // The first buffer of data we get will have the BUFFER_FLAG_CODEC_CONFIG
                        // flag set; the decoder will see this and finish configuring itself.
                        int inputBufIndex = decoder.dequeueInputBuffer(-1);
                        ByteBuffer inputBuf = decoderInputBuffers[inputBufIndex];
                        inputBuf.clear();
                        inputBuf.put(encodedData);
                        decoder.queueInputBuffer(inputBufIndex, 0, info.size,
                                info.presentationTimeUs, info.flags);

                        // If everything from the encoder has been passed to the decoder, we
                        // can stop polling the encoder output.  (This just an optimization.)
                        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            encoderDone = true;
                            encoderOutputAvailable = false;
                        }
                        if (VERBOSE) Log.d(TAG, "passed " + info.size + " bytes to decoder"
                                + (encoderDone ? " (EOS)" : ""));

                        encoder.releaseOutputBuffer(encoderStatus, false);
                    }
                }
            }
        }

        if (outputStream != null) {
            try {
                outputStream.close();
                if (VERBOSE) Log.d(TAG, "Wrote " + debugFrameCount + " frames");
            } catch (IOException ioe) {
                Log.w(TAG, "failed closing debug file");
                throw new RuntimeException(ioe);
            }
        }

        if (goodFrames != TEST_COLORS.length) {
            fail("Found " + goodFrames + " of " + TEST_COLORS.length + " expected frames");
        }
    }

    /**
     * Checks the contents of the current EGL surface to see if it matches expectations.
     * <p>
     * The surface may be black or one of the colors we've drawn.  We have sufficiently little
     * control over the rendering process that we don't know how many (if any) black frames
     * will appear between each color frame.
     * <p>
     * @return the color index, or -2 for black
     * @throw RuntimeException if the color isn't recognized (probably because the RGB<->YUV
     *     conversion introduced too much variance)
     */
    private int checkSurfaceFrame() {
        boolean frameFailed = false;

        // Read a pixel from the center of the surface.  Might want to read from multiple points
        // and average them together.
        int x = WIDTH / 2;
        int y = HEIGHT / 2;
        GLES20.glReadPixels(x, y, 1, 1, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, mPixelBuf);
        int r = mPixelBuf.get(0) & 0xff;
        int g = mPixelBuf.get(1) & 0xff;
        int b = mPixelBuf.get(2) & 0xff;
        if (VERBOSE) Log.d(TAG, "GOT: r=" + r + " g=" + g + " b=" + b);

        if (approxEquals(0, r) && approxEquals(0, g) && approxEquals(0, b)) {
            return -2;
        }

        // Walk through the color list and try to find a match.  These may have gone through
        // RGB<->YCbCr conversions, so don't expect exact matches.
        for (int i = 0; i < TEST_COLORS.length; i++) {
            int testRed = (TEST_COLORS[i] >> 16) & 0xff;
            int testGreen = (TEST_COLORS[i] >> 8) & 0xff;
            int testBlue = TEST_COLORS[i] & 0xff;
            if (approxEquals(testRed, r) && approxEquals(testGreen, g) &&
                    approxEquals(testBlue, b)) {
                if (VERBOSE) Log.d(TAG, "Matched color " + i + ": r=" + r + " g=" + g + " b=" + b);
                return i;
            }
        }

        throw new RuntimeException("No match for color r=" + r + " g=" + g + " b=" + b);
    }

    /**
     * Determines if two color values are approximately equal.
     */
    private static boolean approxEquals(int expected, int actual) {
        final int MAX_DELTA = 4;
        return Math.abs(expected - actual) <= MAX_DELTA;
    }

    /**
     * Creates a series of colorful Presentations on the specified Display.
     */
    private class ColorSlideShow extends Thread {
        private Display mDisplay;

        public ColorSlideShow(Display display) {
            mDisplay = display;
        }

        @Override
        public void run() {
            for (int i = 0; i < TEST_COLORS.length; i++) {
                showPresentation(TEST_COLORS[i]);
            }

            if (VERBOSE) Log.d(TAG, "slide show finished");
            mInputDone = true;
        }

        private void showPresentation(final int color) {
            final TestPresentation[] presentation = new TestPresentation[1];
            try {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Want to create presentation on UI thread so it finds the right Looper
                        // when setting up the Dialog.
                        presentation[0] = new TestPresentation(getContext(), mDisplay, color);
                        if (VERBOSE) Log.d(TAG, "showing color=0x" + Integer.toHexString(color));
                        presentation[0].show();
                    }
                });

                // Give the presentation an opportunity to render.  We don't have a way to
                // monitor the output, so we just sleep for a bit.
                try { Thread.sleep(UI_RENDER_PAUSE_MS); }
                catch (InterruptedException ignore) {}
            } finally {
                if (presentation[0] != null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            presentation[0].dismiss();
                        }
                    });
                }
            }
        }
    }

    /**
     * Executes a runnable on the UI thread, and waits for it to complete.
     */
    private void runOnUiThread(Runnable runnable) {
        Runnable waiter = new Runnable() {
            @Override
            public void run() {
                synchronized (this) {
                    notifyAll();
                }
            }
        };
        synchronized (waiter) {
            mUiHandler.post(runnable);
            mUiHandler.post(waiter);
            try {
                waiter.wait(UI_TIMEOUT_MS);
            } catch (InterruptedException ex) {
            }
        }
    }

    /**
     * Presentation we can show on a virtual display.  The view is set to a single color value.
     */
    private class TestPresentation extends Presentation {
        private final int mColor;

        public TestPresentation(Context context, Display display, int color) {
            super(context, display);
            mColor = color;
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            setTitle("Encode Virtual Test");
            getWindow().setType(WindowManager.LayoutParams.TYPE_PRIVATE_PRESENTATION);

            // Create a solid color image to use as the content of the presentation.
            ImageView view = new ImageView(getContext());
            view.setImageDrawable(new ColorDrawable(mColor));
            view.setLayoutParams(new LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            setContentView(view);
        }
    }
}
