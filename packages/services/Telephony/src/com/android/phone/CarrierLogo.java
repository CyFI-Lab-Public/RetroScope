/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.phone;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to look up carrier logo resource IDs.
 */
public class CarrierLogo {
    /** This class is never instantiated. */
    private CarrierLogo() {
    }

    private static Map<String, Integer> sLogoMap = null;

    private static Map<String, Integer> getLogoMap() {
        if (sLogoMap == null) {
            sLogoMap = new HashMap<String, Integer>();

            // TODO: Load up sLogoMap with known carriers, like:
            // sLogoMap.put("CarrierName",
            //    Integer.valueOf(R.drawable.mobile_logo_carriername));

            // TODO: ideally, read the mapping from a config file
            // rather than manually creating it here.
        }

        return sLogoMap;
    }

    public static int getLogo(String name) {
        Integer res = getLogoMap().get(name);
        if (res != null) {
            return res.intValue();
        }

        return -1;
    }
}
