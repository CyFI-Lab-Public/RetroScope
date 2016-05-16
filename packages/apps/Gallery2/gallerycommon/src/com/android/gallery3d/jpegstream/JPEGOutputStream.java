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

package com.android.gallery3d.jpegstream;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
public class JPEGOutputStream extends FilterOutputStream {
    private long JNIPointer = 0; // Used by JNI code. Don't touch.

    private byte[] mTmpBuffer = new byte[1];
    private int mWidth = 0;
    private int mHeight = 0;
    private int mQuality = 0;
    private int mFormat = -1;
    private boolean mValidConfig = false;
    private boolean mConfigChanged = false;

    public JPEGOutputStream(OutputStream out) {
        super(out);
    }

    public JPEGOutputStream(OutputStream out, int width, int height, int quality,
            int format) {
        super(out);
        setConfig(width, height, quality, format);
    }

    public boolean setConfig(int width, int height, int quality, int format) {
        // Clamp quality to range (0, 100]
        quality = Math.max(Math.min(quality, 100), 1);

        // Make sure format is valid
        switch (format) {
            case JpegConfig.FORMAT_GRAYSCALE:
            case JpegConfig.FORMAT_RGB:
            case JpegConfig.FORMAT_ABGR:
            case JpegConfig.FORMAT_RGBA:
                break;
            default:
                return false;
        }

        // If valid, set configuration
        if (width > 0 && height > 0) {
            mWidth = width;
            mHeight = height;
            mFormat = format;
            mQuality = quality;
            mValidConfig = true;
            mConfigChanged = true;
        } else {
            return false;
        }

        return mValidConfig;
    }

    @Override
    public void close() throws IOException {
        cleanup();
        super.close();
    }

    @Override
    public void write(byte[] buffer, int offset, int length) throws IOException {
        if (offset < 0 || length < 0 || (offset + length) > buffer.length) {
            throw new ArrayIndexOutOfBoundsException(String.format(
                    " buffer length %d, offset %d, length %d",
                    buffer.length, offset, length));
        }
        if (!mValidConfig) {
            return;
        }
        if (mConfigChanged) {
            cleanup();
            int flag = setup(out, mWidth, mHeight, mFormat, mQuality);
            switch(flag) {
                case JpegConfig.J_SUCCESS:
                    break; // allow setup to continue
                case JpegConfig.J_ERROR_BAD_ARGS:
                    throw new IllegalArgumentException("Bad arguments to write");
                default:
                    throw new IOException("Error to writing jpeg headers.");
            }
            mConfigChanged = false;
        }
        int returnCode = JpegConfig.J_ERROR_FATAL;
        try {
            returnCode = writeInputBytes(buffer, offset, length);
        } finally {
            if (returnCode < 0) {
                cleanup();
            }
        }
        if (returnCode < 0) {
            throw new IOException("Error writing jpeg stream");
        }
    }

    @Override
    public void write(byte[] buffer) throws IOException {
        write(buffer, 0, buffer.length);
    }

    @Override
    public void write(int oneByte) throws IOException {
        mTmpBuffer[0] = (byte) oneByte;
        write(mTmpBuffer);
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            cleanup();
        } finally {
            super.finalize();
        }
    }

    native private int setup(OutputStream out, int width, int height, int format, int quality);

    native private void cleanup();

    native private int writeInputBytes(byte[] inBuffer, int offset, int inCount);

    static {
        System.loadLibrary("jni_jpegstream");
    }
}
