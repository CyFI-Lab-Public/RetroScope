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
 * limitations under the License
 */

package com.android.providers.contacts;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;

import java.util.ArrayList;
import java.util.List;

public class MockPackageManager extends android.test.mock.MockPackageManager {
    private final String[] mPackages;
    public MockPackageManager(String... packages) {
        mPackages = packages;
    }

    @Override
    public List<ResolveInfo> queryBroadcastReceivers(Intent intent, int flags) {
        List<ResolveInfo> resolveInfos = new ArrayList<ResolveInfo>();
        for (String pkg : mPackages) {
            resolveInfos.add(createResolveInfo(pkg));
        }
        return resolveInfos;
    }

    private ResolveInfo createResolveInfo(String packageName) {
        ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.packageName = packageName;
        activityInfo.name = "FooClass";
        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.activityInfo = activityInfo;
        return resolveInfo;
    }

    @Override
    public String[] getPackagesForUid(int uid) {
        return new String[] {mPackages[0]};
    }

    @Override
    public int checkPermission(String permName, String pkgName) {
        return PERMISSION_GRANTED;
    }
}
