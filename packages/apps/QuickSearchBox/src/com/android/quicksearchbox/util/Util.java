/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.quicksearchbox.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.util.Log;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * General utilities.
 */
public class Util {

    private static final String TAG = "QSB.Util";

    public static <A> Set<A> setOfFirstN(List<A> list, int n) {
        int end = Math.min(list.size(), n);
        HashSet<A> set = new HashSet<A>(end);
        for (int i = 0; i < end; i++) {
            set.add(list.get(i));
        }
        return set;
    }

    public static Uri getResourceUri(Context packageContext, int res) {
        try {
            Resources resources = packageContext.getResources();
            return getResourceUri(resources, packageContext.getPackageName(), res);
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Resource not found: " + res + " in " + packageContext.getPackageName());
            return null;
        }
    }

    public static Uri getResourceUri(Context context, ApplicationInfo appInfo, int res) {
        try {
            Resources resources = context.getPackageManager().getResourcesForApplication(appInfo);
            return getResourceUri(resources, appInfo.packageName, res);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Resources not found for " + appInfo.packageName);
            return null;
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Resource not found: " + res + " in " + appInfo.packageName);
            return null;
        }
    }

    private static Uri getResourceUri(Resources resources, String appPkg, int res)
            throws Resources.NotFoundException {
        String resPkg = resources.getResourcePackageName(res);
        String type = resources.getResourceTypeName(res);
        String name = resources.getResourceEntryName(res);
        return makeResourceUri(appPkg, resPkg, type, name);
    }

    private static Uri makeResourceUri(String appPkg, String resPkg, String type, String name) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.scheme(ContentResolver.SCHEME_ANDROID_RESOURCE);
        uriBuilder.encodedAuthority(appPkg);
        uriBuilder.appendEncodedPath(type);
        if (!appPkg.equals(resPkg)) {
            uriBuilder.appendEncodedPath(resPkg + ":" + name);
        } else {
            uriBuilder.appendEncodedPath(name);
        }
        return uriBuilder.build();
    }
}
