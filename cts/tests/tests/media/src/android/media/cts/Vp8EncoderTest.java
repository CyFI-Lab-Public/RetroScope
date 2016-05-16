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

import android.content.Context;
import android.content.res.Resources;
import android.media.MediaCodec;
import android.media.MediaCodecInfo.CodecCapabilities;
import android.media.MediaFormat;
import android.os.Bundle;
import android.test.AndroidTestCase;
import android.util.Log;

import com.android.cts.media.R;

import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Basic verification test for vp8 encoder.
 *
 * A raw yv12 stream is encoded and written to an IVF
 * file, which is later decoded by vp8 decoder to verify
 * frames are at least decodable.
 */
public class Vp8EncoderTest extends AndroidTestCase {

    private static final String TAG = "VP8EncoderTest";
    private static final String VP8_MIME = "video/x-vnd.on2.vp8";
    private static final String VPX_DECODER_NAME = "OMX.google.vp8.decoder";
    private static final String VPX_ENCODER_NAME = "OMX.google.vp8.encoder";
    private static final String BASIC_IVF = "video_176x144_vp8_basic.ivf";
    private static final long DEFAULT_TIMEOUT_US = 5000;

    private Resources mResources;
    private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
    private ByteBuffer[] mInputBuffers;
    private ByteBuffer[] mOutputBuffers;

    @Override
    public void setContext(Context context) {
        super.setContext(context);
        mResources = mContext.getResources();
    }

    /**
     * A basic test for VP8 encoder.
     *
     * Encodes a raw stream with default configuration options,
     * and then decodes it to verify the bitstream.
     */
    public void testBasic() throws Exception {
        encode(BASIC_IVF,
               R.raw.video_176x144_yv12,
               176,  // width
               144,  // height
               30);  // framerate
        decode(BASIC_IVF);
    }

    /**
     * Check if MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME is honored.
     *
     * At frame 15, request a sync frame. If one does not occur by EOF the
     * encoder fails. The test does not verify the output stream.
     */
    public void testSyncFrame() throws Exception {
        encodeSyncFrame(R.raw.video_176x144_yv12,
                        176, // width
                        144, // height
                        30); // framerate
    }

    /**
     * Check if MediaCodec.PARAMETER_KEY_VIDEO_BITRATE is honored.
     *
     * Run the sample multiple times. Request periodic changes to the
     * bitrate and ensure the encoder responds.
     */
    public void testVariableBitrate() throws Exception {
        encodeVariableBitrate(R.raw.video_176x144_yv12,
                              176, // width
                              144, // height
                              30); // framerate
    }

    /**
     * A basic check if an encoded stream is decodable.
     *
     * The most basic confirmation we can get about a frame
     * being properly encoded is trying to decode it.
     * (Especially in realtime mode encode output is non-
     * deterministic, therefore a more thorough check like
     * md5 sum comparison wouldn't work.)
     *
     * Indeed, MediaCodec will raise an IllegalStateException
     * whenever vp8 decoder fails to decode a frame, and
     * this test uses that fact to verify the bitstream.
     *
     * @param filename  The name of the IVF file containing encoded bitsream.
     */
    private void decode(String filename) throws Exception {
        IvfReader ivf = null;
        try {
            ivf = new IvfReader(filename);
            int frameWidth = ivf.getWidth();
            int frameHeight = ivf.getHeight();
            int frameCount = ivf.getFrameCount();

            assertTrue(frameWidth > 0);
            assertTrue(frameHeight > 0);
            assertTrue(frameCount > 0);

            MediaFormat format = MediaFormat.createVideoFormat(VP8_MIME,
                                                               ivf.getWidth(),
                                                               ivf.getHeight());

            Log.d(TAG, "Creating decoder");
            MediaCodec decoder = MediaCodec.createByCodecName(VPX_DECODER_NAME);
            decoder.configure(format,
                              null,  // surface
                              null,  // crypto
                              0);  // flags
            decoder.start();

            mInputBuffers = decoder.getInputBuffers();
            mOutputBuffers = decoder.getOutputBuffers();

            // decode loop
            int frameIndex = 0;
            boolean sawOutputEOS = false;
            boolean sawInputEOS = false;

            while (!sawOutputEOS) {
                if (!sawInputEOS) {
                    int inputBufIndex = decoder.dequeueInputBuffer(DEFAULT_TIMEOUT_US);
                    if (inputBufIndex >= 0) {
                        byte[] frame = ivf.readFrame(frameIndex);

                        if (frameIndex == frameCount - 1) {
                            sawInputEOS = true;
                        }

                        mInputBuffers[inputBufIndex].clear();
                        mInputBuffers[inputBufIndex].put(frame);
                        mInputBuffers[inputBufIndex].rewind();

                        Log.d(TAG, "Decoding frame at index " + frameIndex);
                        try {
                            decoder.queueInputBuffer(
                                    inputBufIndex,
                                    0,  // offset
                                    frame.length,
                                    frameIndex,
                                    sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
                        } catch (IllegalStateException ise) {
                            //That is all what is passed from MediaCodec in case of
                            //decode failure.
                            fail("Failed to decode frame at index " + frameIndex);
                        }
                        frameIndex++;
                    }
                }

                int result = decoder.dequeueOutputBuffer(mBufferInfo, DEFAULT_TIMEOUT_US);
                if (result >= 0) {
                    int outputBufIndex = result;
                    if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        sawOutputEOS = true;
                    }
                    decoder.releaseOutputBuffer(outputBufIndex, false);
                } else if (result == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    mOutputBuffers = decoder.getOutputBuffers();
                }
            }
            decoder.stop();
            decoder.release();
        } finally {
            if (ivf != null) {
                ivf.close();
            }
        }
    }

    /**
     * A basic vp8 encode loop.
     *
     * MediaCodec will raise an IllegalStateException
     * whenever vp8 encoder fails to encode a frame.
     *
     * In addition to that written IVF file can be tested
     * to be decodable in order to verify the bitstream produced.
     *
     * Color format of input file should be YUV420, and frameWidth,
     * frameHeight should be supplied correctly as raw input file doesn't
     * include any header data.
     *
     * @param outputFilename  The name of the IVF file to write encoded bitsream
     * @param rawInputFd      File descriptor for the raw input file (YUV420)
     * @param frameWidth      Frame width of input file
     * @param frameHeight     Frame height of input file
     * @param frameRate       Frame rate of input file in frames per second
     */
    private void encode(String outputFilename, int rawInputFd,
                       int frameWidth, int frameHeight, int frameRate) throws Exception {
        // Create a media format signifying desired output
        MediaFormat format = MediaFormat.createVideoFormat(VP8_MIME, frameWidth, frameHeight);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 100000);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                          CodecCapabilities.COLOR_FormatYUV420Planar);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 30);

        Log.d(TAG, "Creating encoder");
        MediaCodec encoder;
        encoder = MediaCodec.createByCodecName(VPX_ENCODER_NAME);
        encoder.configure(format,
                          null,  // surface
                          null,  // crypto
                          MediaCodec.CONFIGURE_FLAG_ENCODE);
        encoder.start();

        mInputBuffers = encoder.getInputBuffers();
        mOutputBuffers = encoder.getOutputBuffers();

        InputStream rawStream = null;
        IvfWriter ivf = null;

        try {
            rawStream = mResources.openRawResource(rawInputFd);
            ivf = new IvfWriter(outputFilename, frameWidth, frameHeight);
            // encode loop
            long presentationTimeUs = 0;
            int inputFrameIndex = 0;
            int outputFrameIndex = 0;
            boolean sawInputEOS = false;
            boolean sawOutputEOS = false;

            while (!sawOutputEOS) {
                if (!sawInputEOS) {
                    int inputBufIndex = encoder.dequeueInputBuffer(DEFAULT_TIMEOUT_US);
                    if (inputBufIndex >= 0) {
                        // YUV420 has 3 planes. Y is full size. U and V are each half size (1/4 the
                        // pixels).
                        int frameSize = frameWidth * frameHeight * 3 / 2;

                        byte[] frame = new byte[frameSize];
                        int bytesRead = rawStream.read(frame);

                        if (bytesRead == -1) {
                            sawInputEOS = true;
                            bytesRead = 0;
                        }

                        mInputBuffers[inputBufIndex].clear();
                        mInputBuffers[inputBufIndex].put(frame);
                        mInputBuffers[inputBufIndex].rewind();

                        presentationTimeUs = (inputFrameIndex * 1000000) / frameRate;
                        Log.d(TAG, "Encoding frame at index " + inputFrameIndex);
                        encoder.queueInputBuffer(
                                inputBufIndex,
                                0,  // offset
                                bytesRead,  // size
                                presentationTimeUs,
                                sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);

                        inputFrameIndex++;
                    }
                }

                int result = encoder.dequeueOutputBuffer(mBufferInfo, DEFAULT_TIMEOUT_US);
                if (result >= 0) {
                    int outputBufIndex = result;
                    byte[] buffer = new byte[mBufferInfo.size];
                    mOutputBuffers[outputBufIndex].rewind();
                    mOutputBuffers[outputBufIndex].get(buffer, 0, mBufferInfo.size);

                    if ((outputFrameIndex == 0)
                        && ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_SYNC_FRAME) == 0)) {
                      throw new RuntimeException("First frame is not a sync frame.");

                    }

                    if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        sawOutputEOS = true;
                    } else {
                        ivf.writeFrame(buffer, mBufferInfo.presentationTimeUs);
                    }
                    encoder.releaseOutputBuffer(outputBufIndex,
                                                false);  // render

                    outputFrameIndex++;
                } else if (result == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    mOutputBuffers = encoder.getOutputBuffers();
                }
            }

            encoder.stop();
            encoder.release();
        } finally {
            if (ivf != null) {
                ivf.close();
            }

            if (rawStream != null) {
                rawStream.close();
            }
        }
    }


    /**
     * Request Sync Frames
     *
     * MediaCodec will raise an IllegalStateException
     * whenever vp8 encoder fails to encode a frame.
     *
     * This presumes a file with 28 frames. Under normal circumstances there
     * would only be one sync frame: the first one. This test will request an
     * additional sync frame at 15 and ensure that it occurs by EOF.
     *
     * Color format of input file should be YUV420, and frameWidth,
     * frameHeight should be supplied correctly as raw input file doesn't
     * include any header data.
     *
     * @param rawInputFd      File descriptor for the raw input file (YUV420)
     * @param frameWidth      Frame width of input file
     * @param frameHeight     Frame height of input file
     * @param frameRate       Frame rate of input file in frames per second
     */
    private void encodeSyncFrame(int rawInputFd, int frameWidth,
                                 int frameHeight, int frameRate) throws Exception {
        // Create a media format signifying desired output
        MediaFormat format = MediaFormat.createVideoFormat(VP8_MIME, frameWidth, frameHeight);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 100000);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                          CodecCapabilities.COLOR_FormatYUV420Planar);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 30);

        Log.d(TAG, "Creating encoder");
        MediaCodec encoder;
        encoder = MediaCodec.createByCodecName(VPX_ENCODER_NAME);
        encoder.configure(format,
                          null,  // surface
                          null,  // crypto
                          MediaCodec.CONFIGURE_FLAG_ENCODE);
        encoder.start();

        mInputBuffers = encoder.getInputBuffers();
        mOutputBuffers = encoder.getOutputBuffers();

        InputStream rawStream = null;

        try {
            rawStream = mResources.openRawResource(rawInputFd);
            // encode loop
            long presentationTimeUs = 0;
            int inputFrameIndex = 0;
            boolean sawInputEOS = false;
            boolean sawOutputEOS = false;
            boolean syncFrameRequested = false;
            boolean matchedSyncFrame = false;

            while (!sawOutputEOS) {
                if (!sawInputEOS) {
                    int inputBufIndex = encoder.dequeueInputBuffer(DEFAULT_TIMEOUT_US);
                    if (inputBufIndex >= 0) {
                        int frameSize = frameWidth * frameHeight * 3 / 2;

                        byte[] frame = new byte[frameSize];
                        int bytesRead = rawStream.read(frame);

                        if (bytesRead == -1) {
                            sawInputEOS = true;
                            bytesRead = 0;
                        }

                        mInputBuffers[inputBufIndex].clear();
                        mInputBuffers[inputBufIndex].put(frame);
                        mInputBuffers[inputBufIndex].rewind();

                        if (inputFrameIndex == 15) {
                            Log.d(TAG, "Requesting sync frame at index " + inputFrameIndex);
                            Bundle syncFrame = new Bundle();
                            syncFrame.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0);
                            encoder.setParameters(syncFrame);
                            syncFrameRequested = true;
                        }

                        presentationTimeUs = (inputFrameIndex * 1000000) / frameRate;
                        encoder.queueInputBuffer(
                                inputBufIndex,
                                0,  // offset
                                bytesRead,  // size
                                presentationTimeUs,
                                sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);

                        inputFrameIndex++;
                    }
                }

                int result = encoder.dequeueOutputBuffer(mBufferInfo, DEFAULT_TIMEOUT_US);
                if (result >= 0) {
                    if (syncFrameRequested && ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_SYNC_FRAME) != 0)) {
                        Log.d(TAG, "Found sync frame");
                        matchedSyncFrame = true;
                    }

                    if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        sawOutputEOS = true;
                    }

                    encoder.releaseOutputBuffer(result,
                                                false);  // render

                } else if (result == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    mOutputBuffers = encoder.getOutputBuffers();
                }
            }

            if (!matchedSyncFrame) {
                throw new RuntimeException("Requested sync frame did not occur");
            }

            encoder.stop();
            encoder.release();
        } finally {
            if (rawStream != null) {
                rawStream.close();
            }
        }
    }


    /**
     * Adjust bitrate
     *
     * MediaCodec will raise an IllegalStateException
     * whenever vp8 encoder fails to encode a frame.
     *
     * Encode the file three times: once at the initial bitrate, once at an
     * increased bitrate, and once at a decreased bitrate. Record the frame
     * sizes that are returned and verify a strict ordering.
     *
     * Color format of input file should be YUV420, and frameWidth,
     * frameHeight should be supplied correctly as raw input file doesn't
     * include any header data.
     *
     * @param rawInputFd      File descriptor for the raw input file (YUV420)
     * @param frameWidth      Frame width of input file
     * @param frameHeight     Frame height of input file
     * @param frameRate       Frame rate of input file in frames per second
     */
    private void encodeVariableBitrate(int rawInputFd, int frameWidth,
                                       int frameHeight, int frameRate) throws Exception {
        // Create a media format signifying desired output
        MediaFormat format = MediaFormat.createVideoFormat(VP8_MIME, frameWidth, frameHeight);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 75000);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                          CodecCapabilities.COLOR_FormatYUV420Planar);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 30);

        Log.d(TAG, "Creating encoder");
        MediaCodec encoder;
        encoder = MediaCodec.createByCodecName(VPX_ENCODER_NAME);
        encoder.configure(format,
                          null,  // surface
                          null,  // crypto
                          MediaCodec.CONFIGURE_FLAG_ENCODE);
        encoder.start();

        mInputBuffers = encoder.getInputBuffers();
        mOutputBuffers = encoder.getOutputBuffers();

        InputStream rawStream = null;

        int iteration = 0;
        int[] bits = new int[100];

        try {
            rawStream = mResources.openRawResource(rawInputFd);
            /* Doc says this is not the default:
             * http://developer.android.com/reference/java/io/InputStream.html#markSupported()
             * but it returns true so using .reset() instead of close/open
             */
            if (rawStream.markSupported()) Log.d(TAG, "Stream marking supported");
            rawStream.mark(1000000);

            // encode loop
            long presentationTimeUs = 0;
            int inputFrameIndex = 0;
            int outputFrameIndex = 0;
            boolean sawInputEOS = false;
            boolean sawOutputEOS = false;

            while (!sawOutputEOS) {
                if (!sawInputEOS) {
                    int inputBufIndex = encoder.dequeueInputBuffer(DEFAULT_TIMEOUT_US);
                    if (inputBufIndex >= 0) {
                        int frameSize = frameWidth * frameHeight * 3 / 2;

                        byte[] frame = new byte[frameSize];
                        int bytesRead = rawStream.read(frame);

                        if (bytesRead == -1) {
                            if (iteration < 2) {
                                rawStream.reset();
                                Bundle bitrate = new Bundle();
                                if (iteration == 0) {
                                    bitrate.putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, 150000);
                                    Log.d(TAG, "Setting bitrate to 150000");
                                } else {
                                    bitrate.putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, 25000);
                                    Log.d(TAG, "Setting bitrate to 25000");
                                }
                                encoder.setParameters(bitrate);

                                iteration++;
                                continue;
                            } else {
                                sawInputEOS = true;
                                bytesRead = 0;
                            }
                        }

                        mInputBuffers[inputBufIndex].clear();
                        mInputBuffers[inputBufIndex].put(frame);
                        mInputBuffers[inputBufIndex].rewind();

                        presentationTimeUs = (inputFrameIndex * 1000000) / frameRate;
                        encoder.queueInputBuffer(
                                inputBufIndex,
                                0,  // offset
                                bytesRead,  // size
                                presentationTimeUs,
                                sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);

                        inputFrameIndex++;
                    }
                }

                int result = encoder.dequeueOutputBuffer(mBufferInfo, DEFAULT_TIMEOUT_US);
                if (result >= 0) {

                    bits[outputFrameIndex] = mBufferInfo.size;

                    if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        sawOutputEOS = true;
                    }

                    encoder.releaseOutputBuffer(result,
                                                false);  // render

                    outputFrameIndex++;

                } else if (result == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    mOutputBuffers = encoder.getOutputBuffers();
                }
            }

            // 29 frames per run
            int i;
            int sum = 0;
            int frames = 29;
            for(i = 0; i < frames; i++)
              sum += bits[i];
            int midBitrateAvg = sum / frames;

            sum = 0;
            for(; i < frames * 2; i++)
              sum += bits[i];
            int highBitrateAvg = sum / frames;

            sum = 0;
            for(; i < frames * 3; i++)
              sum += bits[i];
            int lowBitrateAvg = sum / frames;

            // For the given bitrates we expect mid ~= 350, high ~= 575 and low ~= 150
            // bytes per frame
            if ((midBitrateAvg + 100) > highBitrateAvg)
                throw new RuntimeException("Bitrate did not increase when requesting higher bitrate");
            if ((lowBitrateAvg + 100) > midBitrateAvg)
                throw new RuntimeException("Bitrate did not decrease when requesting lower bitrate");


            encoder.stop();
            encoder.release();
        } finally {
            if (rawStream != null) {
                rawStream.close();
            }
        }
    }
}
