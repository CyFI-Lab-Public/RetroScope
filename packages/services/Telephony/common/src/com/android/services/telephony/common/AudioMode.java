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

package com.android.services.telephony.common;

/**
 * Container class for audio modes.
 */
public class AudioMode {
    // These can be used as a bit mask
    public static int EARPIECE      = 0x00000001;
    public static int BLUETOOTH     = 0x00000002;
    public static int WIRED_HEADSET = 0x00000004;
    public static int SPEAKER       = 0x00000008;

    public static int WIRED_OR_EARPIECE = EARPIECE | WIRED_HEADSET;
    public static int ALL_MODES = EARPIECE | BLUETOOTH | WIRED_HEADSET | SPEAKER;

    public static String toString(int mode) {
        if ((mode & ~ALL_MODES) != 0x0) {
            return "UNKNOWN";
        }

        StringBuffer buffer = new StringBuffer();
        if ((mode & EARPIECE) == EARPIECE) {
            listAppend(buffer, "EARPIECE");
        }
        if ((mode & BLUETOOTH) == BLUETOOTH) {
            listAppend(buffer, "BLUETOOTH");
        }
        if ((mode & WIRED_HEADSET) == WIRED_HEADSET) {
            listAppend(buffer, "WIRED_HEADSET");
        }
        if ((mode & SPEAKER) == SPEAKER) {
            listAppend(buffer, "SPEAKER");
        }

        return buffer.toString();
    }

    private static void listAppend(StringBuffer buffer, String str) {
        if (buffer.length() > 0) {
            buffer.append(", ");
        }
        buffer.append(str);
    }
}
