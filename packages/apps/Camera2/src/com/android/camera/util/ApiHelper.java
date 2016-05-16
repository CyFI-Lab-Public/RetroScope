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

package com.android.camera.util;

import android.os.Build;

import java.lang.reflect.Field;

public class ApiHelper {
    public static final boolean AT_LEAST_16 = Build.VERSION.SDK_INT >= 16;

    public static final boolean HAS_APP_GALLERY =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1;

    public static final boolean HAS_ANNOUNCE_FOR_ACCESSIBILITY =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    public static final boolean HAS_AUTO_FOCUS_MOVE_CALLBACK =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    public static final boolean HAS_MEDIA_ACTION_SOUND =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    public static final boolean HAS_MEDIA_COLUMNS_WIDTH_AND_HEIGHT =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    public static final boolean HAS_SET_BEAM_PUSH_URIS =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    public static final boolean HAS_SURFACE_TEXTURE_RECORDING =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;

    public static final boolean HAS_CAMERA_HDR_PLUS = isKitKatOrHigher();
    public static final boolean HAS_CAMERA_HDR =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1;
    public static final boolean HAS_DISPLAY_LISTENER =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1;

    public static final boolean HAS_ORIENTATION_LOCK =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2;
    public static final boolean HAS_ROTATION_ANIMATION =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2;

    public static final boolean HAS_HIDEYBARS = isKitKatOrHigher();

    public static int getIntFieldIfExists(Class<?> klass, String fieldName,
            Class<?> obj, int defaultVal) {
        try {
            Field f = klass.getDeclaredField(fieldName);
            return f.getInt(obj);
        } catch (Exception e) {
            return defaultVal;
        }
    }

    public static boolean isKitKatOrHigher() {
        // TODO: Remove CODENAME check as soon as VERSION_CODES.KITKAT is final.
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
                || "KeyLimePie".equals(Build.VERSION.CODENAME);
    }
}
