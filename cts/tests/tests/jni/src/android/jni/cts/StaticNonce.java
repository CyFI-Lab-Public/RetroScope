/*
 * Copyright (C) 2009 The Android Open Source Project
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

package android.jni.cts;

/**
 * Class with a bunch of native static methods. These methods are called by
 * the various tests in {@link JniStaticTest}.
 */
public class StaticNonce {
    static {
        if (!JniTestCase.isCpuAbiNone()) {
            System.loadLibrary("jnitest");
        }
    }

    /**
     * Construct an instance.
     */
    public StaticNonce() {
        // This space intentionally left blank.
    }

    // See JniStaticTest for the expected behavior of these methods.

    public static native void nop();

    public static native boolean returnBoolean();
    public static native byte returnByte();
    public static native short returnShort();
    public static native char returnChar();
    public static native int returnInt();
    public static native long returnLong();
    public static native float returnFloat();
    public static native double returnDouble();
    public static native Object returnNull();
    public static native String returnString();
    public static native short[] returnShortArray();
    public static native String[] returnStringArray();
    public static native Class returnThisClass();
    public static native StaticNonce returnInstance();

    public static native boolean takeBoolean(boolean v);
    public static native boolean takeByte(byte v);
    public static native boolean takeShort(short v);
    public static native boolean takeChar(char v);
    public static native boolean takeInt(int v);
    public static native boolean takeLong(long v);
    public static native boolean takeFloat(float v);
    public static native boolean takeDouble(double v);
    public static native boolean takeNull(Object v);
    public static native boolean takeString(String v);
    public static native boolean takeThisClass(Class v);
    public static native boolean takeIntLong(int v1, long v2);
    public static native boolean takeLongInt(long v1, int v2);
    public static native boolean takeOneOfEach(boolean v0, byte v1, short v2,
            char v3, int v4, long v5, String v6, float v7, double v8,
            int[] v9);
    public static native boolean takeCoolHandLuke(
            int v1, int v2, int v3, int v4,
            int v5, int v6, int v7, int v8, int v9,
            int v10, int v11, int v12, int v13, int v14,
            int v15, int v16, int v17, int v18, int v19,
            int v20, int v21, int v22, int v23, int v24,
            int v25, int v26, int v27, int v28, int v29,
            int v30, int v31, int v32, int v33, int v34,
            int v35, int v36, int v37, int v38, int v39,
            int v40, int v41, int v42, int v43, int v44,
            int v45, int v46, int v47, int v48, int v49,
            int v50);
}
