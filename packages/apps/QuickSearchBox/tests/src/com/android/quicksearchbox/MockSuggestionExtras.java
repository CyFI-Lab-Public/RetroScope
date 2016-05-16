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
package com.android.quicksearchbox;

import java.util.Collection;
import java.util.HashMap;

/**
 * Mock suggestion extras backed by a map.
 */
public class MockSuggestionExtras extends AbstractSuggestionExtras {

    HashMap<String, Object> mMap;

    public MockSuggestionExtras() {
        super(null);
        mMap = new HashMap<String, Object>();
    }

    public MockSuggestionExtras put(String name, Object value) {
        mMap.put(name, value);
        return this;
    }

    @Override
    public String doGetExtra(String columnName) {
        Object o = mMap.get(columnName);
        return o == null ? null : o.toString();
    }

    @Override
    public Collection<String> doGetExtraColumnNames() {
        return mMap.keySet();
    }

}
