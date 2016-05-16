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
 * Class with a bunch of static methods that get called from native
 * code. See {@code macroized_tests.c} in {@code libjnitest} for more
 * details.
 */
public class InstanceFromNative {
    /** convenient instance */
    public static final InstanceFromNative theOne = new InstanceFromNative();
    
    /**
     * Constructs an instance.
     */
    public InstanceFromNative() {
        // This space intentionally left blank.
    }

    public void nop() {
        // This space intentionally left blank.
    }

    public boolean returnBoolean() {
        return true;
    }
    
    public byte returnByte() {
        return (byte) 14;
    }
    
    public short returnShort() {
        return (short) -608;
    }
    
    public char returnChar() {
        return (char) 9000;
    }
    
    public int returnInt() {
        return 4004004;
    }
    
    public long returnLong() {
        return -80080080087L;
    }
    
    public float returnFloat() {
        return 2.5e22f;
    }
    
    public double returnDouble() {
        return 7.503e100;
    }
    
    public String returnString() {
        return "muffins";
    }

    public boolean takeOneOfEach(double v0, float v1, long v2, int v3,
            char v4, short v5, byte v6, boolean v7, String v8) {
        return (v0 == 0.0) && (v1 == 1.0f) && (v2 == 2L) && (v3 == 3)
            && (v4 == 4) && (v5 == 5) && (v6 == 6) && v7 &&
            v8.equals("biscuits");
    }

    public boolean takeCoolHandLuke(
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
            int v50) {
        return (v1 == 1) && (v2 == 2) && (v3 == 3) &&
            (v4 == 4) && (v5 == 5) && (v6 == 6) && (v7 == 7) &&
            (v8 == 8) && (v9 == 9) &&
            (v10 == 10) && (v11 == 11) && (v12 == 12) && (v13 == 13) &&
            (v14 == 14) && (v15 == 15) && (v16 == 16) && (v17 == 17) &&
            (v18 == 18) && (v19 == 19) &&
            (v20 == 20) && (v21 == 21) && (v22 == 22) && (v23 == 23) &&
            (v24 == 24) && (v25 == 25) && (v26 == 26) && (v27 == 27) &&
            (v28 == 28) && (v29 == 29) &&
            (v30 == 30) && (v31 == 31) && (v32 == 32) && (v33 == 33) &&
            (v34 == 34) && (v35 == 35) && (v36 == 36) && (v37 == 37) &&
            (v38 == 38) && (v39 == 39) &&
            (v40 == 40) && (v41 == 41) && (v42 == 42) && (v43 == 43) &&
            (v44 == 44) && (v45 == 45) && (v46 == 46) && (v47 == 47) &&
            (v48 == 48) && (v49 == 49) &&
            (v50 == 50);
    }
}
