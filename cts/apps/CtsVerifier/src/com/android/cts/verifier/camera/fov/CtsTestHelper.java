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

package com.android.cts.verifier.camera.fov;

import android.app.Activity;
import android.content.Intent;

import java.util.List;

class CtsTestHelper {

    private static final String REPORTED_FOV_EXTRA = "lightcycle.reported_fov";
    private static final String MEASURED_FOV_EXTRA = "lightcycle.measured_fov";

    public static void storeCtsTestResult(Activity activity, float reportedFOV, float measuredFOV) {
        Intent it = new Intent();
        it.putExtra(REPORTED_FOV_EXTRA, reportedFOV);
        it.putExtra(MEASURED_FOV_EXTRA, measuredFOV);
        activity.setResult(Activity.RESULT_OK, it);
    }

    public static float getMeasuredFOV(Intent intent) {
        return intent.getFloatExtra(MEASURED_FOV_EXTRA, -1f);
    }

    public static float getReportedFOV(Intent intent) {
        return intent.getFloatExtra(REPORTED_FOV_EXTRA, -1f);
    }

    public static boolean isResultPassed(float reportedFOV, float measuredFOV) {
        if (Math.abs(reportedFOV - measuredFOV) < 2f) return true;
        return false;
    }

    public static String getTestDetails(List<SelectableResolution> resolutions) {
        String details = "PhotoSphere FOV test result:\n";
        for (int i = 0; i < resolutions.size(); i++) {
            SelectableResolution res = resolutions.get(i);
            details += "Camera:" + res.cameraId + ", Resolution:" + res.width + 'x' + res.height
                    + ", Measured FOV = " + res.measuredFOV + '\n';

        }

        return details;
    }
}
