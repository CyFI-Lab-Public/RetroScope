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

package com.android.gallery3d.util;

import com.android.gallery3d.common.Utils;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;

public class InterruptableOutputStream extends OutputStream {

    private static final int MAX_WRITE_BYTES = 4096;

    private OutputStream mOutputStream;
    private volatile boolean mIsInterrupted = false;

    public InterruptableOutputStream(OutputStream outputStream) {
        mOutputStream = Utils.checkNotNull(outputStream);
    }

    @Override
    public void write(int oneByte) throws IOException {
        if (mIsInterrupted) throw new InterruptedIOException();
        mOutputStream.write(oneByte);
    }

    @Override
    public void write(byte[] buffer, int offset, int count) throws IOException {
        int end = offset + count;
        while (offset < end) {
            if (mIsInterrupted) throw new InterruptedIOException();
            int bytesCount = Math.min(MAX_WRITE_BYTES, end - offset);
            mOutputStream.write(buffer, offset, bytesCount);
            offset += bytesCount;
        }
    }

    @Override
    public void close() throws IOException {
        mOutputStream.close();
    }

    @Override
    public void flush() throws IOException {
        if (mIsInterrupted) throw new InterruptedIOException();
        mOutputStream.flush();
    }

    public void interrupt() {
        mIsInterrupted = true;
    }
}
