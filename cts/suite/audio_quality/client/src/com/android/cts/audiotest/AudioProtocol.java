/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.cts.audiotest;

import android.app.Activity;
import  android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.Thread;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;


public class AudioProtocol implements AudioTrack.OnPlaybackPositionUpdateListener {
    private static final String TAG = "AudioProtocol";
    private static final int PORT_NUMBER = 15001;

    private Thread mThread = new Thread(new ProtocolServer());
    private boolean mExitRequested = false;

    private static final int PROTOCOL_HEADER_SIZE = 8; // id + payload length
    private static final int MAX_NON_DATA_PAYLOAD_SIZE = 20;
    private static final int PROTOCOL_SIMPLE_REPLY_SIZE = 12;
    private static final int PROTOCOL_OK = 0;
    private static final int PROTOCOL_ERROR_WRONG_PARAM = 1;
    private static final int PROTOCOL_ERROR_GENERIC = 2;

    private static final int CMD_DOWNLOAD        = 0x12340001;
    private static final int CMD_START_PLAYBACK  = 0x12340002;
    private static final int CMD_STOP_PLAYBACK   = 0x12340003;
    private static final int CMD_START_RECORDING = 0x12340004;
    private static final int CMD_STOP_RECORDING  = 0x12340005;
    private static final int CMD_GET_DEVICE_INFO = 0x12340006;

    private ByteBuffer mHeaderBuffer = ByteBuffer.allocate(PROTOCOL_HEADER_SIZE);
    private ByteBuffer mDataBuffer = ByteBuffer.allocate(MAX_NON_DATA_PAYLOAD_SIZE);
    private ByteBuffer mReplyBuffer = ByteBuffer.allocate(PROTOCOL_SIMPLE_REPLY_SIZE);

    // all socket access (accept / read) set this timeout to check exit periodically.
    private static final int SOCKET_ACCESS_TIMEOUT = 2000;
    private Socket mClient = null;
    private InputStream mInput = null;
    private OutputStream mOutput = null;
    // lock to use to write to socket, I/O streams, and also change socket (create, destroy)
    private ReentrantLock mClientLock = new ReentrantLock();

    private AudioRecord mRecord = null;
    private LoopThread mRecordThread = null;
    private AudioTrack mPlayback = null;
    private LoopThread mPlaybackThread = null;
    // store recording length
    private int mRecordingLength = 0;

    // map for playback data
    private HashMap<Integer, ByteBuffer> mDataMap = new HashMap<Integer, ByteBuffer>();

    public boolean start() {
        Log.d(TAG, "start");
        mExitRequested = false;
        mThread.start();
        //Log.d(TAG, "started");
        return true;
    }

    public void stop() throws InterruptedException {
        Log.d(TAG, "stop");
        mExitRequested = true;
        try {
            mClientLock.lock();
            if (mClient != null) {
                // wake up from socket read
                mClient.shutdownInput();
            }
        }catch (IOException e) {
                // ignore
        } finally {
            mClientLock.unlock();
        }
        mThread.interrupt(); // this does not bail out from socket in android
        mThread.join();
        reset();
        Log.d(TAG, "stopped");
    }

    @Override
    public void onMarkerReached(AudioTrack track) {
        Log.d(TAG, "playback completed");
        track.stop();
        track.flush();
        track.release();
        mPlaybackThread.quitLoop();
        mPlaybackThread = null;
        try {
            sendSimpleReplyHeader(CMD_START_PLAYBACK, PROTOCOL_OK);
        } catch (IOException e) {
            // maybe socket already closed. don't do anything
            Log.e(TAG, "ignore exception", e);
        }
    }

    @Override
    public void onPeriodicNotification(AudioTrack arg0) {
        Log.d(TAG, "track periodic notification");
        // TODO Auto-generated method stub
    }

    /**
     * Read given amount of data to the buffer
     * @param in
     * @param buffer
     * @param len length to read
     * @return true if header read successfully, false if exit requested
     * @throws IOException
     * @throws ExitRequest
     */
    private void read(InputStream in, ByteBuffer buffer, int len) throws IOException, ExitRequest {
        buffer.clear();
        int totalRead = 0;
        while (totalRead < len) {
            int readNow = in.read(buffer.array(), totalRead, len - totalRead);
            if (readNow < 0) { // end-of-stream, error
                Log.e(TAG, "read returned " + readNow);
                throw new IOException();
            }
            totalRead += readNow;
            if(mExitRequested) {
                throw new ExitRequest();
            }
        }
    }

    private class ProtocolError  extends Exception {
        public ProtocolError(String message) {
            super(message);
        }
    }

    private class ExitRequest extends Exception {
        public ExitRequest() {
            super();
        }
    }

    private void assertProtocol(boolean cond, String message) throws ProtocolError {
        if (!cond) {
            throw new ProtocolError(message);
        }
    }

    private void reset() {
        // lock only when it is not already locked by this thread
        if (mClientLock.getHoldCount() == 0) {
            mClientLock.lock();
        }
        if (mClient != null) {
            try {
                mClient.close();
            } catch (IOException e) {
                // ignore
            }
            mClient = null;
        }
        mInput = null;
        mOutput = null;
        while (mClientLock.getHoldCount() > 0) {
            mClientLock.unlock();
        }
        if (mRecord != null) {
            if (mRecord.getState() != AudioRecord.STATE_UNINITIALIZED) {
                mRecord.stop();
            }
            mRecord.release();
            mRecord = null;
        }
        if (mRecordThread != null) {
            mRecordThread.quitLoop();
            mRecordThread = null;
        }
        if (mPlayback != null) {
            if (mPlayback.getState() != AudioTrack.STATE_UNINITIALIZED) {
                mPlayback.stop();
                mPlayback.flush();
            }
            mPlayback.release();
            mPlayback = null;
        }
        if (mPlaybackThread != null) {
            mPlaybackThread.quitLoop();
            mPlaybackThread = null;
        }
        mDataMap.clear();
    }

    private void handleDownload(int len) throws IOException, ExitRequest {
        read(mInput, mDataBuffer, 4); // only for id
        Integer id  = new Integer(mDataBuffer.getInt(0));
        int dataLength = len - 4;
        ByteBuffer data = ByteBuffer.allocate(dataLength);
        read(mInput, data, dataLength);
        mDataMap.put(id, data);
        Log.d(TAG, "downloaded data id " + id + " len " + dataLength);
        sendSimpleReplyHeader(CMD_DOWNLOAD, PROTOCOL_OK);
    }

    private void handleStartPlayback(int len) throws ProtocolError, IOException, ExitRequest {
        // this error is too critical, so do not even send reply
        assertProtocol(len == 20, "wrong payload len");
        read(mInput, mDataBuffer, len);
        final Integer id = new Integer(mDataBuffer.getInt(0));
        final int samplingRate = mDataBuffer.getInt(1 * 4);
        final boolean stereo = ((mDataBuffer.getInt(2 * 4) & 0x80000000) != 0);
        final int mode = mDataBuffer.getInt(2 * 4) & 0x7fffffff;
        final int volume = mDataBuffer.getInt(3 * 4);
        final int repeat = mDataBuffer.getInt(4 * 4);
        try {
            final ByteBuffer data = mDataMap.get(id);
            if (data == null) {
                throw new ProtocolError("wrong id");
            }
            if (samplingRate != 44100) {
                throw new ProtocolError("wrong rate");
            }
            //FIXME in MODE_STATIC, setNotificationMarkerPosition does not work with full length
            mPlaybackThread = new LoopThread(new Runnable() {

                @Override
                public void run() {
                    if (mPlayback != null) {
                        mPlayback.release();
                        mPlayback = null;
                    }
                    // STREAM_VOICE_CALL activates different speaker.
                    // use MUSIC mode to activate the louder speaker.
                    int type = AudioManager.STREAM_MUSIC;
                    int bufferSize = AudioTrack.getMinBufferSize(samplingRate,
                            stereo ? AudioFormat.CHANNEL_OUT_STEREO : AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT);
                    bufferSize = bufferSize * 4;
                    if (bufferSize < 256 * 1024) {
                        bufferSize = 256 * 1024;
                    }
                    if (bufferSize > data.capacity()) {
                        bufferSize = data.capacity();
                    }
                    mPlayback = new AudioTrack(type, samplingRate,
                            stereo ? AudioFormat.CHANNEL_OUT_STEREO : AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT, bufferSize,
                            AudioTrack.MODE_STREAM);
                    float minVolume = mPlayback.getMinVolume();
                    float maxVolume = mPlayback.getMaxVolume();
                    float newVolume = (maxVolume - minVolume) * volume / 100 + minVolume;
                    mPlayback.setStereoVolume(newVolume, newVolume);
                    Log.d(TAG, "setting volume " + newVolume + " max " + maxVolume +
                            " min " + minVolume + " received " + volume);
                    int dataWritten = 0;
                    int dataToWrite = (bufferSize < data.capacity())? bufferSize : data.capacity();
                    mPlayback.write(data.array(), 0, dataToWrite);
                    dataWritten = dataToWrite;
                    mPlayback.setPlaybackPositionUpdateListener(AudioProtocol.this);

                    int endMarker = data.capacity()/(stereo ? 4 : 2);
                    int res = mPlayback.setNotificationMarkerPosition(endMarker);
                    Log.d(TAG, "start playback id " + id + " len " + data.capacity() +
                            " set.. res " + res + " stereo? " + stereo + " mode " + mode +
                            " end " + endMarker);
                    mPlayback.play();
                    while (dataWritten < data.capacity()) {
                        int dataLeft = data.capacity() - dataWritten;
                        dataToWrite = (bufferSize < dataLeft)? bufferSize : dataLeft;
                        if (mPlayback == null) { // stopped
                            return;
                        }
                        mPlayback.write(data.array(), dataWritten, dataToWrite);
                        dataWritten += dataToWrite;
                    }
                }
            });
            mPlaybackThread.start();
            // send reply when play is completed
        } catch (ProtocolError e) {
            sendSimpleReplyHeader(CMD_START_PLAYBACK, PROTOCOL_ERROR_WRONG_PARAM);
            Log.e(TAG, "wrong param", e);
        }
    }

    private void handleStopPlayback(int len) throws ProtocolError, IOException {
        Log.d(TAG, "stopPlayback");
        assertProtocol(len == 0, "wrong payload len");
        if (mPlayback != null) {
            Log.d(TAG, "release AudioTrack");
            mPlayback.stop();
            mPlayback.flush();
            mPlayback.release();
            mPlayback = null;
        }
        if (mPlaybackThread != null) {
            mPlaybackThread.quitLoop();
            mPlaybackThread = null;
        }
        sendSimpleReplyHeader(CMD_STOP_PLAYBACK, PROTOCOL_OK);
    }

    private void handleStartRecording(int len) throws ProtocolError, IOException, ExitRequest {
        assertProtocol(len == 16, "wrong payload len");
        read(mInput, mDataBuffer, len);
        final int samplingRate = mDataBuffer.getInt(0);
        final boolean stereo = ((mDataBuffer.getInt(1 * 4) & 0x80000000) != 0);
        final int mode = mDataBuffer.getInt(1 * 4) & 0x7fffffff;
        final int volume = mDataBuffer.getInt(2 * 4);
        final int samples = mDataBuffer.getInt(3 * 4);
        try {
            if (samplingRate != 44100) {
                throw new ProtocolError("wrong rate");
            }
            if (stereo) {
                throw new ProtocolError("mono only");
            }
            //TODO volume ?
            mRecordingLength = samples * 2;
            mRecordThread = new LoopThread(new Runnable() {

                @Override
                public void run() {
                    int minBufferSize = AudioRecord.getMinBufferSize(samplingRate,
                            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
                    int type = (mode == 0) ? AudioSource.VOICE_RECOGNITION : AudioSource.DEFAULT;
                    mRecord = new AudioRecord(type, samplingRate,
                            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                            (minBufferSize > mRecordingLength) ? minBufferSize : mRecordingLength);

                    mRecord.startRecording();
                    Log.d(TAG, "recording started " + " samples " + samples + " mode " + mode +
                            " recording state " + mRecord.getRecordingState() + " len " +
                            mRecordingLength);
                    try {
                        boolean recordingOk = true;
                        byte[] data = new byte[mRecordingLength];
                        int totalRead = 0;
                        while (totalRead < mRecordingLength) {
                            int lenRead = mRecord.read(data, 0, (mRecordingLength - totalRead));
                            if (lenRead < 0) {
                                Log.e(TAG, "reading recording failed with error code " + lenRead);
                                recordingOk = false;
                                break;
                            } else if (lenRead == 0) {
                                Log.w(TAG, "zero read");
                            }
                            totalRead += lenRead;
                        }
                        Log.d(TAG, "reading recording completed");
                        sendReplyWithData(
                                CMD_START_RECORDING,
                                recordingOk ? PROTOCOL_OK : PROTOCOL_ERROR_GENERIC,
                                recordingOk ? mRecordingLength : 0,
                                recordingOk ? data : null);
                    } catch (IOException e) {
                        // maybe socket already closed. don't do anything
                        Log.e(TAG, "ignore exception", e);
                    } finally {
                        mRecord.stop();
                        mRecord.release();
                        mRecord = null;
                    }
                }
             });
            mRecordThread.start();
        } catch (ProtocolError e) {
            sendSimpleReplyHeader(CMD_START_RECORDING, PROTOCOL_ERROR_WRONG_PARAM);
            Log.e(TAG, "wrong param", e);
        }
    }

    private void handleStopRecording(int len) throws ProtocolError, IOException {
        Log.d(TAG, "stop recording");
        assertProtocol(len == 0, "wrong payload len");
        if (mRecord != null) {
            mRecord.stop();
            mRecord.release();
            mRecord = null;
        }
        if (mRecordThread != null) {
            mRecordThread.quitLoop();
            mRecordThread = null;
        }
        sendSimpleReplyHeader(CMD_STOP_RECORDING, PROTOCOL_OK);
    }

    private static final String BUILD_INFO_TAG = "build-info";

    private void appendAttrib(StringBuilder builder, String name, String value) {
        builder.append(" " + name + "=\"" + value + "\"");
    }

    private void handleGetDeviceInfo(int len) throws ProtocolError, IOException{
        Log.d(TAG, "getDeviceInfo");
        assertProtocol(len == 0, "wrong payload len");
        StringBuilder builder = new StringBuilder();
        builder.append("<build-info");
        appendAttrib(builder, "board", Build.BOARD);
        appendAttrib(builder, "brand", Build.BRAND);
        appendAttrib(builder, "device", Build.DEVICE);
        appendAttrib(builder, "display", Build.DISPLAY);
        appendAttrib(builder, "fingerprint", Build.FINGERPRINT);
        appendAttrib(builder, "id", Build.ID);
        appendAttrib(builder, "model", Build.MODEL);
        appendAttrib(builder, "product", Build.PRODUCT);
        appendAttrib(builder, "release", Build.VERSION.RELEASE);
        appendAttrib(builder, "sdk", Integer.toString(Build.VERSION.SDK_INT));
        builder.append(" />");
        byte[] data = builder.toString().getBytes();

        sendReplyWithData(CMD_GET_DEVICE_INFO, PROTOCOL_OK, data.length, data);
    }
    /**
     * send reply without payload.
     * This function is thread-safe.
     * @param out
     * @param command
     * @param errorCode
     * @throws IOException
     */
    private void sendSimpleReplyHeader(int command, int errorCode) throws IOException {
        Log.d(TAG, "sending reply cmd " + command + " err " + errorCode);
        sendReplyWithData(command, errorCode, 0, null);
    }

    private void sendReplyWithData(int cmd, int errorCode, int len, byte[] data) throws IOException {
        try {
            mClientLock.lock();
            mReplyBuffer.clear();
            mReplyBuffer.putInt((cmd & 0xffff) | 0x43210000);
            mReplyBuffer.putInt(errorCode);
            mReplyBuffer.putInt(len);

            if (mOutput != null) {
                mOutput.write(mReplyBuffer.array(), 0, PROTOCOL_SIMPLE_REPLY_SIZE);
                if (data != null) {
                    mOutput.write(data, 0, len);
                }
            }
        } catch (IOException e) {
            throw e;
        } finally {
            mClientLock.unlock();
        }
    }
    private class LoopThread extends Thread {
        private Looper mLooper;
        LoopThread(Runnable runnable) {
            super(runnable);
        }
        public void run() {
            Looper.prepare();
            mLooper = Looper.myLooper();
            Log.d(TAG, "run runnable");
            super.run();
            //Log.d(TAG, "loop");
            Looper.loop();
        }
        // should be called outside this thread
        public void quitLoop() {
            mLooper.quit();
            try {
                if (Thread.currentThread() != this) {
                    join();
                }
            } catch (InterruptedException e) {
                // ignore
            }
            Log.d(TAG, "quit thread");
        }
    }

    private class ProtocolServer implements Runnable {

        @Override
        public void run() {
            ServerSocket server = null;

            try { // for catching exception from ServerSocket
                Log.d(TAG, "get new server socket");
                server = new ServerSocket(PORT_NUMBER);
                server.setReuseAddress(true);
                server.setSoTimeout(SOCKET_ACCESS_TIMEOUT);
                while (!mExitRequested) {
                    //TODO check already active recording/playback
                    try { // for catching exception from Socket, will restart upon exception
                        try {
                            mClientLock.lock();
                            //Log.d(TAG, "will accept");
                            mClient = server.accept();
                            mClient.setReuseAddress(true);
                            mInput = mClient.getInputStream();
                            mOutput = mClient.getOutputStream();
                        } catch (SocketTimeoutException e) {
                            // This will happen frequently if client does not connect.
                            // just re-start
                            continue;
                        } finally {
                            mClientLock.unlock();
                        }
                        Log.i(TAG, "new client connected");
                        while (!mExitRequested) {
                            read(mInput, mHeaderBuffer, PROTOCOL_HEADER_SIZE);
                            int command = mHeaderBuffer.getInt();
                            int len = mHeaderBuffer.getInt();
                            Log.i(TAG, "received command " + command);
                            switch(command) {
                            case CMD_DOWNLOAD:
                                handleDownload(len);
                                break;
                            case CMD_START_PLAYBACK:
                                handleStartPlayback(len);
                                break;
                            case CMD_STOP_PLAYBACK:
                                handleStopPlayback(len);
                                break;
                            case CMD_START_RECORDING:
                                handleStartRecording(len);
                                break;
                            case CMD_STOP_RECORDING:
                                handleStopRecording(len);
                                break;
                            case CMD_GET_DEVICE_INFO:
                                handleGetDeviceInfo(len);
                            }
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "restart from exception", e);
                    } catch (ProtocolError e) {
                        Log.e(TAG, "restart from exception",  e);
                    } finally {
                        reset();
                    }
                }
            } catch (ExitRequest e) {
                Log.e(TAG, "exit requested, will exit", e);
            } catch (IOException e) {
                // error in server socket, just exit the thread and let things fail.
                Log.e(TAG, "error while init, will exit", e);
            } finally {
                if (server != null) {
                    try {
                        server.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
                reset();
            }
        }
    }
}
