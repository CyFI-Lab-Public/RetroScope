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

import android.graphics.Point;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class JPEGInputStream extends FilterInputStream {
    private long JNIPointer = 0; // Used by JNI code. Don't touch.

    private boolean mValidConfig = false;
    private boolean mConfigChanged = false;
    private int mFormat = -1;
    private byte[] mTmpBuffer = new byte[1];
    private int mWidth = 0;
    private int mHeight = 0;

    public JPEGInputStream(InputStream in) {
        super(in);
    }

    public JPEGInputStream(InputStream in, int format) {
        super(in);
        setConfig(format);
    }

    public boolean setConfig(int format) {
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
        mFormat = format;
        mValidConfig = true;
        mConfigChanged = true;
        return true;
    }

    public Point getDimensions() throws IOException {
        if (mValidConfig) {
            applyConfigChange();
            return new Point(mWidth, mHeight);
        }
        return null;
    }

    @Override
    public int available() {
        return 0; // TODO
    }

    @Override
    public void close() throws IOException {
        cleanup();
        super.close();
    }

    @Override
    public synchronized void mark(int readlimit) {
        // Do nothing
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public int read() throws IOException {
        read(mTmpBuffer, 0, 1);
        return 0xFF & mTmpBuffer[0];
    }

    @Override
    public int read(byte[] buffer) throws IOException {
        return read(buffer, 0, buffer.length);
    }

    @Override
    public int read(byte[] buffer, int offset, int count) throws IOException {
        if (offset < 0 || count < 0 || (offset + count) > buffer.length) {
            throw new ArrayIndexOutOfBoundsException(String.format(
                    " buffer length %d, offset %d, length %d",
                    buffer.length, offset, count));
        }
        if (!mValidConfig) {
            return 0;
        }
        applyConfigChange();
        int flag = JpegConfig.J_ERROR_FATAL;
        try {
            flag = readDecodedBytes(buffer, offset, count);
        } finally {
            if (flag < 0) {
                cleanup();
            }
        }
        if (flag < 0) {
            switch (flag) {
                case JpegConfig.J_DONE:
                    return -1; // Returns -1 after reading EOS.
                default:
                    throw new IOException("Error reading jpeg stream");
            }
        }
        return flag;
    }

    @Override
    public synchronized void reset() throws IOException {
        throw new IOException("Reset not supported.");
    }

    @Override
    public long skip(long byteCount) throws IOException {
        if (byteCount <= 0) {
            return 0;
        }
        // Shorten skip to a reasonable amount
        int flag = skipDecodedBytes((int) (0x7FFFFFFF & byteCount));
        if (flag < 0) {
            switch (flag) {
                case JpegConfig.J_DONE:
                    return 0; // Returns 0 after reading EOS.
                default:
                    throw new IOException("Error skipping jpeg stream");
            }
        }
        return flag;
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            cleanup();
        } finally {
            super.finalize();
        }
    }

    private void applyConfigChange() throws IOException {
        if (mConfigChanged) {
            cleanup();
            Point dimens = new Point(0, 0);
            int flag = setup(dimens, in, mFormat);
            switch(flag) {
                case JpegConfig.J_SUCCESS:
                    break; // allow setup to continue
                case JpegConfig.J_ERROR_BAD_ARGS:
                    throw new IllegalArgumentException("Bad arguments to read");
                default:
                    throw new IOException("Error to reading jpeg headers.");
            }
            mWidth = dimens.x;
            mHeight = dimens.y;
            mConfigChanged = false;
        }
    }

    native private int setup(Point dimens, InputStream in, int format);

    native private void cleanup();

    native private int readDecodedBytes( byte[] inBuffer, int offset, int inCount);

    native private int skipDecodedBytes(int bytes);

    static {
        System.loadLibrary("jni_jpegstream");
    }
}
