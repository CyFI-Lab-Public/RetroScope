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

package android.cts.util;

import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.content.Context;
import android.os.StatFs;

public class SystemUtil {
    public static long getFreeDiskSize(Context context) {
        StatFs statFs = new StatFs(context.getFilesDir().getAbsolutePath());
        return (long)statFs.getAvailableBlocks() * statFs.getBlockSize();
    }

    public static long getFreeMemory(Context context) {
        MemoryInfo info = new MemoryInfo();
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(info);
        return info.availMem;
    }

    public static long getTotalMemory(Context context) {
        MemoryInfo info = new MemoryInfo();
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(info);
        return info.totalMem; // TODO totalMem N/A in ICS.
    }
}
