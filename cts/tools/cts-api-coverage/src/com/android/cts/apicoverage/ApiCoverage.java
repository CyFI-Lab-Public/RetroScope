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

package com.android.cts.apicoverage;

import java.lang.String;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Representation of the entire API containing packages. */
class ApiCoverage {

    private final Map<String, ApiPackage> mPackages = new HashMap<String, ApiPackage>();

    public void addPackage(ApiPackage pkg) {
        mPackages.put(pkg.getName(), pkg);
    }

    public ApiPackage getPackage(String name) {
        return mPackages.get(name);
    }

    public Collection<ApiPackage> getPackages() {
        return Collections.unmodifiableCollection(mPackages.values());
    }

    public void removeEmptyAbstractClasses() {
        for (Map.Entry<String, ApiPackage> entry : mPackages.entrySet()) {
            ApiPackage pkg = entry.getValue();
            pkg.removeEmptyAbstractClasses();
        }
    }
}
