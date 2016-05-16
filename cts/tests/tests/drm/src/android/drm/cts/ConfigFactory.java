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

package android.drm.cts;

import android.util.Log;

import android.drm.cts.configs.PassthruConfig;
import android.drm.cts.configs.FwdLockConfig;

public final class ConfigFactory {
    private static final String TAG = "ConfigFactory";

    /**
     * Get configurations of specified plug-in name.
     *
     * @param plugInName Name of DRM plug-in. The name SHOULD be consistent with
     *                   the name defined in plug-in's onGetSupportInfo().
     */
    public static Config getConfig(String plugInName) {
        if (plugInName.equals("Passthru plug-in")) {
            return PassthruConfig.getInstance();
        } else if (plugInName.equals("OMA V1 Forward Lock")) {
            return FwdLockConfig.getInstance();
        } else {
            Log.e(TAG, "Configuration for " + plugInName + " is not registered.");
            return null;
        }
    }
}
