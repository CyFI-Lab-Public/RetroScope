/*
 * Copyright (C) 2012 The Android Open Source Project
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

package android.mediastress.cts;

import android.media.CamcorderProfile;
import android.util.Log;

import junit.framework.Assert;

public class VideoPlayerCapability {
    private static final String TAG = "VideoPlayCapability";

    static boolean formatSupported(int quality, int videoCodec, int audioCodec) {
        if (!CamcorderProfile.hasProfile(quality)) {
            Log.i(TAG, "quality " + quality + " not supported");
            return false;
        }
        CamcorderProfile profile = CamcorderProfile.get(quality);
        Assert.assertNotNull(profile);
        if ((profile.videoCodec == videoCodec) && (profile.audioCodec == audioCodec)) {
            Log.i(TAG, "quality " + quality + " video codec " + videoCodec + " audio codec " +
                    audioCodec + " supproted");
            return true;
        }
        return false;
    }

}
