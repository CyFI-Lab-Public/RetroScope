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

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaCodecInfo.CodecCapabilities;
import android.media.MediaCodecInfo.CodecProfileLevel;
import android.opengl.GLES20;
import android.test.AndroidTestCase;
import android.util.Log;
import android.view.Surface;

import java.nio.ByteBuffer;


/**
 * General MediaCodec tests.
 *
 * In particular, check various API edge cases.
 */
public class MediaCodecTest extends AndroidTestCase {
    private static final String TAG = "MediaCodecTest";
    private static final boolean VERBOSE = false;           // lots of logging

    // parameters for the video encoder
    private static final String MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding
    private static final int BIT_RATE = 2000000;            // 2Mbps
    private static final int FRAME_RATE = 15;               // 15fps
    private static final int IFRAME_INTERVAL = 10;          // 10 seconds between I-frames
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    // parameters for the audio encoder
    private static final String MIME_TYPE_AUDIO = "audio/mp4a-latm";
    private static final int AUDIO_SAMPLE_RATE = 44100;
    private static final int AUDIO_AAC_PROFILE = 2; /* OMX_AUDIO_AACObjectLC */
    private static final int AUDIO_CHANNEL_COUNT = 2; // mono
    private static final int AUDIO_BIT_RATE = 128000;

    private static final int TIMEOUT_USEC = 100000;
    private static final int TIMEOUT_USEC_SHORT = 100;

    private boolean mVideoEncoderHadError = false;
    private boolean mAudioEncoderHadError = false;
    private volatile boolean mVideoEncodingOngoing = false;

    /**
     * Tests:
     * <br> calling createInputSurface() before configure() throws exception
     * <br> calling createInputSurface() after start() throws exception
     * <br> calling createInputSurface() with a non-Surface color format throws exception
     */
    public void testCreateInputSurfaceErrors() {
        MediaFormat format = createMediaFormat();
        MediaCodec encoder = null;
        Surface surface = null;

        // Replace color format with something that isn't COLOR_FormatSurface.
        MediaCodecInfo codecInfo = selectCodec(MIME_TYPE);
        int colorFormat = findNonSurfaceColorFormat(codecInfo, MIME_TYPE);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat);

        try {
            encoder = MediaCodec.createByCodecName(codecInfo.getName());
            try {
                surface = encoder.createInputSurface();
                fail("createInputSurface should not work pre-configure");
            } catch (IllegalStateException ise) {
                // good
            }
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

            try {
                surface = encoder.createInputSurface();
                fail("createInputSurface should require COLOR_FormatSurface");
            } catch (IllegalStateException ise) {
                // good
            }

            encoder.start();

            try {
                surface = encoder.createInputSurface();
                fail("createInputSurface should not work post-start");
            } catch (IllegalStateException ise) {
                // good
            }
        } finally {
            if (encoder != null) {
                encoder.stop();
                encoder.release();
            }
        }
        assertNull(surface);
    }


    /**
     * Tests:
     * <br> signaling end-of-stream before any data is sent works
     * <br> signaling EOS twice throws exception
     * <br> submitting a frame after EOS throws exception [TODO]
     */
    public void testSignalSurfaceEOS() {
        MediaFormat format = createMediaFormat();
        MediaCodec encoder = null;
        InputSurface inputSurface = null;

        try {
            encoder = MediaCodec.createEncoderByType(MIME_TYPE);
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            inputSurface = new InputSurface(encoder.createInputSurface());
            inputSurface.makeCurrent();
            encoder.start();

            // send an immediate EOS
            encoder.signalEndOfInputStream();

            try {
                encoder.signalEndOfInputStream();
                fail("should not be able to signal EOS twice");
            } catch (IllegalStateException ise) {
                // good
            }

            // submit a frame post-EOS
            GLES20.glClearColor(0.0f, 0.5f, 0.0f, 1.0f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            try {
                inputSurface.swapBuffers();
                if (false) {    // TODO
                    fail("should not be able to submit frame after EOS");
                }
            } catch (Exception ex) {
                // good
            }
        } finally {
            if (encoder != null) {
                encoder.stop();
                encoder.release();
            }
            if (inputSurface != null) {
                inputSurface.release();
            }
        }
    }

    /**
     * Tests:
     * <br> dequeueInputBuffer() fails when encoder configured with an input Surface
     */
    public void testDequeueSurface() {
        MediaFormat format = createMediaFormat();
        MediaCodec encoder = null;
        Surface surface = null;

        try {
            encoder = MediaCodec.createEncoderByType(MIME_TYPE);
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            surface = encoder.createInputSurface();
            encoder.start();

            try {
                encoder.dequeueInputBuffer(-1);
                fail("dequeueInputBuffer should fail on encoder with input surface");
            } catch (IllegalStateException ise) {
                // good
            }

        } finally {
            if (encoder != null) {
                encoder.stop();
                encoder.release();
            }
            if (surface != null) {
                surface.release();
            }
        }
    }

    /**
     * Tests:
     * <br> configure() encoder with Surface, re-configure() without Surface works
     * <br> sending EOS with signalEndOfInputStream on non-Surface encoder fails
     */
    public void testReconfigureWithoutSurface() {
        MediaFormat format = createMediaFormat();
        MediaCodec encoder = null;
        Surface surface = null;

        try {
            encoder = MediaCodec.createEncoderByType(MIME_TYPE);
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            surface = encoder.createInputSurface();
            encoder.start();

            encoder.getOutputBuffers();

            // re-configure, this time without an input surface
            if (VERBOSE) Log.d(TAG, "reconfiguring");
            encoder.stop();
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            encoder.start();
            if (VERBOSE) Log.d(TAG, "reconfigured");

            encoder.getOutputBuffers();
            encoder.dequeueInputBuffer(-1);

            try {
                encoder.signalEndOfInputStream();
                fail("signalEndOfInputStream only works on surface input");
            } catch (IllegalStateException ise) {
                // good
            }
        } finally {
            if (encoder != null) {
                encoder.stop();
                encoder.release();
            }
            if (surface != null) {
                surface.release();
            }
        }
    }

    public void testConcurrentAudioVideoEncodings() throws InterruptedException {
        final int VIDEO_NUM_SWAPS = 100;
        // audio only checks this and stop
        mVideoEncodingOngoing = true;
        final CodecInfo info = getAvcSupportedFormatInfo();
        long start = System.currentTimeMillis();
        Thread videoEncodingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                runVideoEncoding(VIDEO_NUM_SWAPS, info);
            }
        });
        Thread audioEncodingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                runAudioEncoding();
            }
        });
        videoEncodingThread.start();
        audioEncodingThread.start();
        videoEncodingThread.join();
        mVideoEncodingOngoing = false;
        audioEncodingThread.join();
        assertFalse("Video encoding error. Chekc logcat", mVideoEncoderHadError);
        assertFalse("Audio encoding error. Chekc logcat", mAudioEncoderHadError);
        long end = System.currentTimeMillis();
        Log.w(TAG, "Concurrent AV encoding took " + (end - start) + " ms for " + VIDEO_NUM_SWAPS +
                " video frames");
    }

    private static class CodecInfo {
        public int mMaxW;
        public int mMaxH;
        public int mFps;
        public int mBitRate;
    };

    private static CodecInfo getAvcSupportedFormatInfo() {
        MediaCodecInfo mediaCodecInfo = selectCodec(MIME_TYPE);
        CodecCapabilities cap = mediaCodecInfo.getCapabilitiesForType(MIME_TYPE);
        if (cap == null) { // not supported
            return null;
        }
        CodecInfo info = new CodecInfo();
        int highestLevel = 0;
        for (CodecProfileLevel lvl : cap.profileLevels) {
            if (lvl.level > highestLevel) {
                highestLevel = lvl.level;
            }
        }
        int maxW = 0;
        int maxH = 0;
        int bitRate = 0;
        int fps = 0; // frame rate for the max resolution
        switch(highestLevel) {
            // Do not support Level 1 to 2.
            case CodecProfileLevel.AVCLevel1:
            case CodecProfileLevel.AVCLevel11:
            case CodecProfileLevel.AVCLevel12:
            case CodecProfileLevel.AVCLevel13:
            case CodecProfileLevel.AVCLevel1b:
            case CodecProfileLevel.AVCLevel2:
                return null;
            case CodecProfileLevel.AVCLevel21:
                maxW = 352;
                maxH = 576;
                bitRate = 4000000;
                fps = 25;
                break;
            case CodecProfileLevel.AVCLevel22:
                maxW = 720;
                maxH = 480;
                bitRate = 4000000;
                fps = 15;
                break;
            case CodecProfileLevel.AVCLevel3:
                maxW = 720;
                maxH = 480;
                bitRate = 10000000;
                fps = 30;
                break;
            case CodecProfileLevel.AVCLevel31:
                maxW = 1280;
                maxH = 720;
                bitRate = 14000000;
                fps = 30;
                break;
            case CodecProfileLevel.AVCLevel32:
                maxW = 1280;
                maxH = 720;
                bitRate = 20000000;
                fps = 60;
                break;
            case CodecProfileLevel.AVCLevel4: // only try up to 1080p
            default:
                maxW = 1920;
                maxH = 1080;
                bitRate = 20000000;
                fps = 30;
                break;
        }
        info.mMaxW = maxW;
        info.mMaxH = maxH;
        info.mFps = fps;
        info.mBitRate = bitRate;
        Log.i(TAG, "AVC Level 0x" + Integer.toHexString(highestLevel) + " bit rate " + bitRate +
                " fps " + info.mFps + " w " + maxW + " h " + maxH);

        return info;
    }

    private void runVideoEncoding(int numSwap, CodecInfo info) {
        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, info.mMaxW, info.mMaxH);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, info.mBitRate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, info.mFps);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
        MediaCodec encoder = null;
        InputSurface inputSurface = null;
        mVideoEncoderHadError = false;
        try {
            encoder = MediaCodec.createEncoderByType(MIME_TYPE);
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            inputSurface = new InputSurface(encoder.createInputSurface());
            inputSurface.makeCurrent();
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            encoder.start();
            for (int i = 0; i < numSwap; i++) {
                GLES20.glClearColor(0.0f, 0.5f, 0.0f, 1.0f);
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
                inputSurface.swapBuffers();
                // dequeue buffers until not available
                int index = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
                while (index >= 0) {
                    encoder.releaseOutputBuffer(index, false);
                    // just throw away output
                    // allow shorter wait for 2nd round to move on quickly.
                    index = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC_SHORT);
                }
            }
            encoder.signalEndOfInputStream();
        } catch (Throwable e) {
            Log.w(TAG, "runVideoEncoding got error: " + e);
            mVideoEncoderHadError = true;
        } finally {
            if (encoder != null) {
                encoder.stop();
                encoder.release();
            }
            if (inputSurface != null) {
                inputSurface.release();
            }
        }
    }

    private void runAudioEncoding() {
        MediaFormat format = MediaFormat.createAudioFormat(MIME_TYPE_AUDIO, AUDIO_SAMPLE_RATE,
                AUDIO_CHANNEL_COUNT);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, AUDIO_AAC_PROFILE);
        format.setInteger(MediaFormat.KEY_BIT_RATE, AUDIO_BIT_RATE);
        MediaCodec encoder = null;
        mAudioEncoderHadError = false;
        try {
            encoder = MediaCodec.createEncoderByType(MIME_TYPE_AUDIO);
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            encoder.start();
            ByteBuffer[] inputBuffers = encoder.getInputBuffers();
            ByteBuffer source = ByteBuffer.allocate(inputBuffers[0].capacity());
            for (int i = 0; i < source.capacity()/2; i++) {
                source.putShort((short)i);
            }
            source.rewind();
            int currentInputBufferIndex = 0;
            long encodingLatencySum = 0;
            int totalEncoded = 0;
            int numRepeat = 0;
            while (mVideoEncodingOngoing) {
                numRepeat++;
                int inputIndex = encoder.dequeueInputBuffer(TIMEOUT_USEC);
                while (inputIndex == -1) {
                    inputIndex = encoder.dequeueInputBuffer(TIMEOUT_USEC);
                }
                ByteBuffer inputBuffer = inputBuffers[inputIndex];
                inputBuffer.rewind();
                inputBuffer.put(source);
                long start = System.currentTimeMillis();
                totalEncoded += inputBuffers[inputIndex].limit();
                encoder.queueInputBuffer(inputIndex, 0, inputBuffer.limit(), 0, 0);
                source.rewind();
                int index = encoder.dequeueOutputBuffer(info, TIMEOUT_USEC);
                long end = System.currentTimeMillis();
                encodingLatencySum += (end - start);
                while (index >= 0) {
                    encoder.releaseOutputBuffer(index, false);
                    // just throw away output
                    // allow shorter wait for 2nd round to move on quickly.
                    index = encoder.dequeueOutputBuffer(info, TIMEOUT_USEC_SHORT);
                }
            }
            Log.w(TAG, "Audio encoding average latency " + encodingLatencySum / numRepeat +
                    " ms for average write size " + totalEncoded / numRepeat +
                    " total latency " + encodingLatencySum + " ms for total bytes " + totalEncoded);
        } catch (Throwable e) {
            Log.w(TAG, "runAudioEncoding got error: " + e);
            mAudioEncoderHadError = true;
        } finally {
            if (encoder != null) {
                encoder.stop();
                encoder.release();
            }
        }
    }

    /**
     * Creates a MediaFormat with the basic set of values.
     */
    private static MediaFormat createMediaFormat() {
        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, WIDTH, HEIGHT);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
        return format;
    }

    /**
     * Returns the first codec capable of encoding the specified MIME type, or null if no
     * match was found.
     */
    private static MediaCodecInfo selectCodec(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

            if (!codecInfo.isEncoder()) {
                continue;
            }

            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }

    /**
     * Returns a color format that is supported by the codec and isn't COLOR_FormatSurface.  Throws
     * an exception if none found.
     */
    private static int findNonSurfaceColorFormat(MediaCodecInfo codecInfo, String mimeType) {
        MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(mimeType);
        for (int i = 0; i < capabilities.colorFormats.length; i++) {
            int colorFormat = capabilities.colorFormats[i];
            if (colorFormat != MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface) {
                return colorFormat;
            }
        }
        fail("couldn't find a good color format for " + codecInfo.getName() + " / " + MIME_TYPE);
        return 0;   // not reached
    }
}
