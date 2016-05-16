/*
 * Copyright (C) 2010 The Android Open Source Project
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

package android.app.cts;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.pm.ActivityInfo;

public class OrientationTestUtils {

    /**
     * Change the activity's orientation to something different and then switch back. This is used
     * to trigger {@link Activity#onConfigurationChanged(android.content.res.Configuration)}.
     *
     * @param activity whose orientation will be changed and restored
     */
    public static void toggleOrientation(Activity activity) {
        toggleOrientationSync(activity, null);
    }

    /**
     * Same as {@link #toggleOrientation(Activity)} except {@link Instrumentation#waitForIdleSync()}
     * is called after each orientation change.
     *
     * @param activity whose orientation will be changed and restored
     * @param instrumentation use for idle syncing
     */
    public static void toggleOrientationSync(final Activity activity,
            final Instrumentation instrumentation) {
        final int originalOrientation = activity.getResources().getConfiguration().orientation;
        final int newOrientation = originalOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        changeOrientation(activity, instrumentation, newOrientation);
        changeOrientation(activity, instrumentation, originalOrientation);
    }

    private static void changeOrientation(final Activity activity,
            Instrumentation instrumentation, final int orientation) {
        activity.setRequestedOrientation(orientation);
        if (instrumentation != null) {
            instrumentation.waitForIdleSync();
        }
    }
}
