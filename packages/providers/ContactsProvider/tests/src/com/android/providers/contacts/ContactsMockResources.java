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

package com.android.providers.contacts;

import android.test.mock.MockResources;

import com.google.android.collect.Maps;

import java.util.Map;

final class ContactsMockResources extends MockResources {
    private Map<Integer, String> mPackages = Maps.newHashMap();
    private Map<Integer, String> mTypes = Maps.newHashMap();
    private Map<Integer, String> mEntries = Maps.newHashMap();

    public void addResource(int resId, String packageName, String typeName, String entryName) {
        mPackages.put(resId, packageName);
        mTypes.put(resId, typeName);
        mEntries.put(resId, entryName);
    }

    @Override
    public String getResourceName(int resId) throws NotFoundException {
        if (!mPackages.containsKey(resId)) {
            throw new NotFoundException("Resource " + resId + " not found");
        }
        return mPackages.get(resId) + ":" + mTypes.get(resId) + "/" + mEntries.get(resId);
    }

    @Override
    public String getResourcePackageName(int resId) throws NotFoundException {
        if (!mPackages.containsKey(resId)) {
            throw new NotFoundException("Resource " + resId + " not found");
        }
        return mPackages.get(resId);
    }

    @Override
    public String getResourceTypeName(int resId) throws NotFoundException {
        if (!mPackages.containsKey(resId)) {
            throw new NotFoundException("Resource " + resId + " not found");
        }
        return mTypes.get(resId);
    }

    @Override
    public String getResourceEntryName(int resId) throws NotFoundException {
        if (!mPackages.containsKey(resId)) {
            throw new NotFoundException("Resource " + resId + " not found");
        }
        return mEntries.get(resId);
    }
}