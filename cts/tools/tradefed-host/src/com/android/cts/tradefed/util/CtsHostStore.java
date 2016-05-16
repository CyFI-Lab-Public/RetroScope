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
package com.android.cts.tradefed.util;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class for storing Cts Results.
 * This is necessary for host tests where test metrics cannot be passed.
 */
public class CtsHostStore {

    // needs concurrent verion as there can be multiple client accessing this.
    // But there is no additional protection for the same key as that should not happen.
    private static final ConcurrentHashMap<String, String> mMap =
            new ConcurrentHashMap<String, String>();

    /**
     * Stores CTS result. Existing result with the same key will be replaced.
     * Note that key is generated in the form of device_serial#class#method name.
     * So there should be no concurrent test for the same (serial, class, method).
     * @param deviceSerial
     * @param classMethodName
     * @param result CTS result string
     */
    public static void storeCtsResult(String deviceSerial, String classMethodName, String result) {
        mMap.put(generateTestKey(deviceSerial, classMethodName), result);
    }

    /**
     * retrieves a CTS result for the given condition and remove it from the internal
     * storage. If there is no result for the given condition, it will return null.
     */
    public static String removeCtsResult(String deviceSerial, String classMethodName) {
        return mMap.remove(generateTestKey(deviceSerial, classMethodName));
    }

    /**
     * return test key in the form of device_serial#class_name#method_name
     */
    private static String generateTestKey(String deviceSerial, String classMethodName) {
        return String.format("%s#%s", deviceSerial, classMethodName);

    }
}
