/*
 * Copyright (C) 2013 Google Inc.
 * Licensed to The Android Open Source Project.
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
package com.android.mail.preferences;

import com.google.common.collect.Sets;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Set;

/**
* A POJO for shared preferences to be used for backing up and restoring.
*/
public class SimpleBackupSharedPreference implements BackupSharedPreference {
    private String mKey;
    private Object mValue;

    private static final String KEY = "key";
    private static final String VALUE = "value";

    public SimpleBackupSharedPreference(final String key, final Object value) {
        mKey = key;
        mValue = value;
    }

    @Override
    public String getKey() {
        return mKey;
    }

    @Override
    public Object getValue() {
        return mValue;
    }

    public void setValue(Object value) {
        mValue = value;
    }

    @Override
    public JSONObject toJson() throws JSONException {
        final JSONObject json = new JSONObject();
        json.put(KEY, mKey);
        if (mValue instanceof Set) {
            final Set<?> set = (Set<?>) mValue;
            final JSONArray array = new JSONArray();
            for (final Object o : set) {
                array.put(o);
            }
            json.put(VALUE, array);
        } else {
            json.put(VALUE, mValue);
        }
        return json;
    }

    public static BackupSharedPreference fromJson(final JSONObject json) throws JSONException {
        Object value = json.get(VALUE);
        if (value instanceof JSONArray) {
            final Set<Object> set = Sets.newHashSet();
            final JSONArray array = (JSONArray) value;
            for (int i = 0, len = array.length(); i < len; i++) {
                  set.add(array.get(i));
            }
            value = set;
        }
        return new SimpleBackupSharedPreference(json.getString(KEY), value);
    }

    @Override
    public String toString() {
        return "BackupSharedPreference{" + "mKey='" + mKey + '\'' + ", mValue=" + mValue + '}';
    }
}
