/*
 * Copyright (C) 2011 The Android Open Source Project
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

package android.tests.getinfo;

/**
 * Constants for device info attributes to be sent as instrumentation keys.
 * <p/>
 * These values should correspond to attributes defined in cts_result.xsd.
 */
public interface DeviceInfoConstants {

    public static final String OPEN_GL_COMPRESSED_TEXTURE_FORMATS =
            "openGlCompressedTextureFormats";
    public static final String SYS_LIBRARIES = "systemlibraries";
    public static final String PARTITIONS = "partitions";
    public static final String OPEN_GL_ES_VERSION = "openGlEsVersion";
    public static final String GRAPHICS_VENDOR = "graphicsVendor";
    public static final String GRAPHICS_RENDERER = "graphicsRenderer";
    public static final String PROCESSES = "processes";
    public static final String FEATURES = "features";
    public static final String PHONE_NUMBER = "subscriberId";
    public static final String LOCALES = "locales";
    public static final String IMSI = "imsi";
    public static final String IMEI = "imei";
    public static final String NETWORK = "network";
    public static final String KEYPAD = "keypad";
    public static final String NAVIGATION = "navigation";
    public static final String TOUCH_SCREEN = "touch";
    public static final String SCREEN_Y_DENSITY = "Ydpi";
    public static final String SCREEN_X_DENSITY = "Xdpi";
    public static final String SCREEN_SIZE = "screen_size";
    public static final String SCREEN_DENSITY_BUCKET = "screen_density_bucket";
    public static final String SCREEN_DENSITY = "screen_density";
    public static final String RESOLUTION = "resolution";
    public static final String VERSION_SDK = "androidPlatformVersion";
    public static final String VERSION_RELEASE = "buildVersion";
    public static final String BUILD_ABI = "build_abi";
    public static final String BUILD_ABI2 = "build_abi2";
    public static final String BUILD_FINGERPRINT = "build_fingerprint";
    public static final String BUILD_TYPE = "build_type";
    public static final String BUILD_MODEL = "build_model";
    public static final String BUILD_BRAND = "build_brand";
    public static final String BUILD_MANUFACTURER = "build_manufacturer";
    public static final String BUILD_BOARD = "build_board";
    public static final String BUILD_DEVICE = "build_device";
    public static final String PRODUCT_NAME = "buildName";
    public static final String BUILD_ID = "buildID";
    public static final String BUILD_VERSION = "buildVersion";
    public static final String BUILD_TAGS = "build_tags";
    public static final String SERIAL_NUMBER = "deviceID";
    public static final String STORAGE_DEVICES = "storage_devices";
    public static final String MULTI_USER = "multi_user";
}
