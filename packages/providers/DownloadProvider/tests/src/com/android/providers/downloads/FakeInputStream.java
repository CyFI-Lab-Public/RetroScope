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

package com.android.providers.downloads;

import java.io.InputStream;
import java.util.Arrays;

/**
 * Provides fake data for large transfers.
 */
public class FakeInputStream extends InputStream {
    private long mRemaining;

    public FakeInputStream(long length) {
        mRemaining = length;
    }

    @Override
    public int read() {
        final int value;
        if (mRemaining > 0) {
            mRemaining--;
            return 0;
        } else {
            return -1;
        }
    }

    @Override
    public int read(byte[] buffer, int offset, int length) {
        Arrays.checkOffsetAndCount(buffer.length, offset, length);

        if (length > mRemaining) {
            length = (int) mRemaining;
        }
        mRemaining -= length;

        if (length == 0) {
            return -1;
        } else {
            return length;
        }
    }
}
