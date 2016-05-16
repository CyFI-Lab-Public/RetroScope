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
package com.android.providers.contacts;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.res.Resources;
import android.os.Binder;
import android.test.mock.MockPackageManager;

import com.google.android.collect.Lists;

import java.util.HashMap;
import java.util.List;

/**
 * Mock {@link PackageManager} that knows about a specific set of packages
 * to help test security models. Because {@link Binder#getCallingUid()}
 * can't be mocked, you'll have to find your mock-UID manually using your
 * {@link Context#getPackageName()}.
 */
public class ContactsMockPackageManager extends MockPackageManager {
    private final HashMap<Integer, String> mForward = new HashMap<Integer, String>();
    private final HashMap<String, Integer> mReverse = new HashMap<String, Integer>();
    private List<PackageInfo> mPackages;

    public ContactsMockPackageManager() {
    }

    /**
     * Add a UID-to-package mapping, which is then stored internally.
     */
    public void addPackage(int packageUid, String packageName) {
        mForward.put(packageUid, packageName);
        mReverse.put(packageName, packageUid);
    }

    @Override
    public String getNameForUid(int uid) {
        return "name-for-uid";
    }

    @Override
    public String[] getPackagesForUid(int uid) {
        if (mPackages != null) {
            return new String[] { mPackages.get(0).packageName };
        } else {
            return new String[] { ContactsActor.sCallingPackage };
        }
    }

    @Override
    public ApplicationInfo getApplicationInfo(String packageName, int flags) {
        ApplicationInfo info = new ApplicationInfo();
        Integer uid = mReverse.get(packageName);
        info.uid = (uid != null) ? uid : -1;
        return info;
    }

    public void setInstalledPackages(List<PackageInfo> packages) {
        this.mPackages = packages;
    }

    @Override
    public List<PackageInfo> getInstalledPackages(int flags) {
        return mPackages;
    }

    @Override
    public PackageInfo getPackageInfo(String packageName, int flags) throws NameNotFoundException {
        for (PackageInfo info : mPackages) {
            if (info.packageName.equals(packageName)) {
                return info;
            }
        }
        throw new NameNotFoundException();
    }

    @Override
    public Resources getResourcesForApplication(String appPackageName) {
        return new ContactsMockResources();
    }

    @Override
    public List<ProviderInfo> queryContentProviders(String processName, int uid, int flags) {
        final List<ProviderInfo> ret = Lists.newArrayList();
        if (mPackages == null) return ret;
        for (PackageInfo packageInfo : mPackages) {
            if (packageInfo.providers == null) continue;
            for (ProviderInfo providerInfo : packageInfo.providers) {
                ret.add(providerInfo);
            }
        }
        return ret;
    }
}
