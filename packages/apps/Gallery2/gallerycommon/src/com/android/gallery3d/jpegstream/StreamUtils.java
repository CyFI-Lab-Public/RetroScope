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

import java.nio.ByteOrder;

public class StreamUtils {

    private StreamUtils() {
    }

    /**
     * Copies the input byte array into the output int array with the given
     * endianness. If input is not a multiple of 4, ignores the last 1-3 bytes
     * and returns true.
     */
    public static boolean byteToIntArray(int[] output, byte[] input, ByteOrder endianness) {
        int length = input.length - (input.length % 4);
        if (output.length * 4 < length) {
            throw new ArrayIndexOutOfBoundsException("Output array is too short to hold input");
        }
        if (endianness == ByteOrder.BIG_ENDIAN) {
            for (int i = 0, j = 0; i < output.length; i++, j += 4) {
                output[i] = ((input[j] & 0xFF) << 24) | ((input[j + 1] & 0xFF) << 16)
                        | ((input[j + 2] & 0xFF) << 8) | ((input[j + 3] & 0xFF));
            }
        } else {
            for (int i = 0, j = 0; i < output.length; i++, j += 4) {
                output[i] = ((input[j + 3] & 0xFF) << 24) | ((input[j + 2] & 0xFF) << 16)
                        | ((input[j + 1] & 0xFF) << 8) | ((input[j] & 0xFF));
            }
        }
        return input.length % 4 != 0;
    }

    public static int[] byteToIntArray(byte[] input, ByteOrder endianness) {
        int[] output = new int[input.length / 4];
        byteToIntArray(output, input, endianness);
        return output;
    }

    /**
     * Uses native endianness.
     */
    public static int[] byteToIntArray(byte[] input) {
        return byteToIntArray(input, ByteOrder.nativeOrder());
    }

    /**
     * Returns the number of bytes in a pixel for a given format defined in
     * JpegConfig.
     */
    public static int pixelSize(int format) {
        switch (format) {
            case JpegConfig.FORMAT_ABGR:
            case JpegConfig.FORMAT_RGBA:
                return 4;
            case JpegConfig.FORMAT_RGB:
                return 3;
            case JpegConfig.FORMAT_GRAYSCALE:
                return 1;
            default:
                return -1;
        }
    }
}
