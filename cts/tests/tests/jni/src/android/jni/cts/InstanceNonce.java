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
 * Class with a bunch of native instance methods. These methods are called by
 * the various tests in {@link JniInstanceTest}.
 */
public class InstanceNonce {
    static {
        if (!JniTestCase.isCpuAbiNone()) {
            System.loadLibrary("jnitest");
        }
    }

    /**
     * Construct an instance.
     */
    public InstanceNonce() {
        // This space intentionally left blank.
    }

    // See JniInstanceTest for the expected behavior of these methods.

    public native void nop();

    public native boolean returnBoolean();
    public native byte returnByte();
    public native short returnShort();
    public native char returnChar();
    public native int returnInt();
    public native long returnLong();
    public native float returnFloat();
    public native double returnDouble();
    public native Object returnNull();
    public native String returnString();
    public native short[] returnShortArray();
    public native String[] returnStringArray();
    public native InstanceNonce returnThis();

    public native boolean takeBoolean(boolean v);
    public native boolean takeByte(byte v);
    public native boolean takeShort(short v);
    public native boolean takeChar(char v);
    public native boolean takeInt(int v);
    public native boolean takeLong(long v);
    public native boolean takeFloat(float v);
    public native boolean takeDouble(double v);
    public native boolean takeNull(Object v);
    public native boolean takeString(String v);
    public native boolean takeThis(InstanceNonce v);
    public native boolean takeIntLong(int v1, long v2);
    public native boolean takeLongInt(long v1, int v2);
    public native boolean takeOneOfEach(boolean v0, byte v1, short v2,
            char v3, int v4, long v5, String v6, float v7, double v8,
            int[] v9);
    public native boolean takeCoolHandLuke(
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
