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

package com.replica.replicaisland;

/** A collection of miscellaneous utility functions. */
public class Utils {
    private static final float EPSILON = 0.0001f;

    public final static boolean close(float a, float b) {
        return close(a, b, EPSILON);
    }

    public final static boolean close(float a, float b, float epsilon) {
        return Math.abs(a - b) < epsilon;
    }

    public final static int sign(float a) {
        if (a >= 0.0f) {
            return 1;
        } else {
            return -1;
        }
    }
    
    public final static int clamp(int value, int min, int max) {
        int result = value;
        if (min == max) {
            if (value != min) {
                result = min;
            }
        } else if (min < max) {
            if (value < min) {
                result = min;
            } else if (value > max) {
                result = max;
            }
        } else {
            result = clamp(value, max, min);
        }
        
        return result;
    }
   
    
    public final static int byteArrayToInt(byte[] b) {
        if (b.length != 4) {
            return 0;
        }

        // Same as DataInputStream's 'readInt' method
        /*int i = (((b[0] & 0xff) << 24) | ((b[1] & 0xff) << 16) | ((b[2] & 0xff) << 8) 
                | (b[3] & 0xff));*/
        
        // little endian
        int i = (((b[3] & 0xff) << 24) | ((b[2] & 0xff) << 16) | ((b[1] & 0xff) << 8) 
                | (b[0] & 0xff));
    
        return i;
    }
    
    public final static float byteArrayToFloat(byte[] b) {
        
        // intBitsToFloat() converts bits as follows:
        /*
        int s = ((i >> 31) == 0) ? 1 : -1;
        int e = ((i >> 23) & 0xff);
        int m = (e == 0) ? (i & 0x7fffff) << 1 : (i & 0x7fffff) | 0x800000;
        */
    
        return Float.intBitsToFloat(byteArrayToInt(b));
    }
    
    public final static float framesToTime(int framesPerSecond, int frameCount) {
        return (1.0f / framesPerSecond) * frameCount;
    }

}
