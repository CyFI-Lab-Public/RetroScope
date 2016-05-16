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

import java.util.Comparator;

interface HasCoverage {
    float getCoveragePercentage();
    String getName();
}

class CoverageComparator implements Comparator<HasCoverage> {
    public int compare(HasCoverage entity, HasCoverage otherEntity) {
        int diff = Math.round(entity.getCoveragePercentage())
                - Math.round(otherEntity.getCoveragePercentage());
        return diff != 0 ? diff : entity.getName().compareTo(otherEntity.getName());
    }
}
