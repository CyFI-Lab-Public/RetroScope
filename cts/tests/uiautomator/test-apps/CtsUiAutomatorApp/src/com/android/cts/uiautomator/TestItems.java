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

package com.android.cts.uiautomator;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestItems {
    private static String LOG_TAG = TestItems.class.getSimpleName();
    private static List<TestItem> ITEMS = new ArrayList<TestItem>();
    private static Map<String, TestItem> ITEM_MAP = new HashMap<String, TestItem>();

    public static class TestItem {
        public String mId;
        public String mName;
        private final Class<Fragment> mClassFragment;
        public String mTestDescription;

        @SuppressWarnings("unchecked")
        public TestItem(String id, String name, Class<?> clsf) {
            mId = id;
            mName = name;
            mClassFragment = (Class<Fragment>) clsf;
        }

        @Override
        public String toString() {
            return mName;
        }
    }

    static {
        addTestItem(new TestItem("1", "Test 1", Test1DetailFragment.class));
        addTestItem(new TestItem("2", "Test 2", Test2DetailFragment.class));
        addTestItem(new TestItem("3", "Test 3", Test3DetailFragment.class));
        addTestItem(new TestItem("4", "Test 4", Test4DetailFragment.class));
        addTestItem(new TestItem("5", "Test 5", Test5DetailFragment.class));
        addTestItem(new TestItem("6", "Test 6", Test6DetailFragment.class));
        addTestItem(new TestItem("7", "Test 7", TestGenericDetailFragment.class));
        addTestItem(new TestItem("8", "Test 8", TestGenericDetailFragment.class));
        addTestItem(new TestItem("9", "Test 9", TestGenericDetailFragment.class));
        addTestItem(new TestItem("10", "Test 10", TestGenericDetailFragment.class));
        addTestItem(new TestItem("11", "Test 11", TestGenericDetailFragment.class));
        addTestItem(new TestItem("12", "Test 12", TestGenericDetailFragment.class));
        addTestItem(new TestItem("13", "Test 13", TestGenericDetailFragment.class));
        addTestItem(new TestItem("14", "Test 14", TestGenericDetailFragment.class));
        addTestItem(new TestItem("15", "Test 15", TestGenericDetailFragment.class));
        addTestItem(new TestItem("16", "Test 16", TestGenericDetailFragment.class));
        addTestItem(new TestItem("17", "Test 17", TestGenericDetailFragment.class));
        addTestItem(new TestItem("18", "Test 18", TestGenericDetailFragment.class));
        addTestItem(new TestItem("19", "Test 19", TestGenericDetailFragment.class));
        addTestItem(new TestItem("20", "Test 20", TestGenericDetailFragment.class));
        addTestItem(new TestItem("21", "Test 21", TestGenericDetailFragment.class));
        addTestItem(new TestItem("22", "Test 22", TestGenericDetailFragment.class));
        addTestItem(new TestItem("23", "Test 23", TestGenericDetailFragment.class));
        addTestItem(new TestItem("24", "Test 24", TestGenericDetailFragment.class));
        addTestItem(new TestItem("25", "Test 25", TestGenericDetailFragment.class));
        addTestItem(new TestItem("26", "Test 26", TestGenericDetailFragment.class));
        addTestItem(new TestItem("27", "Test 27", TestGenericDetailFragment.class));
        addTestItem(new TestItem("28", "Test 28", TestGenericDetailFragment.class));
        addTestItem(new TestItem("29", "Test 29", TestGenericDetailFragment.class));
        addTestItem(new TestItem("30", "Test 30", TestGenericDetailFragment.class));
        addTestItem(new TestItem("31", "Test 31", TestGenericDetailFragment.class));
        addTestItem(new TestItem("32", "Test 32", TestGenericDetailFragment.class));
        addTestItem(new TestItem("33", "Test 33", TestGenericDetailFragment.class));
        addTestItem(new TestItem("34", "Test 34", TestGenericDetailFragment.class));
        addTestItem(new TestItem("35", "Test 35", TestGenericDetailFragment.class));
        addTestItem(new TestItem("36", "Test 36", TestGenericDetailFragment.class));
        addTestItem(new TestItem("37", "Test 37", TestGenericDetailFragment.class));
        addTestItem(new TestItem("38", "Test 38", TestGenericDetailFragment.class));
        addTestItem(new TestItem("39", "Test 39", TestGenericDetailFragment.class));
        addTestItem(new TestItem("40", "Test 40", TestGenericDetailFragment.class));
    }

    private static void addTestItem(TestItem item) {
        ITEMS.add(item);
        ITEM_MAP.put(item.mId, item);
    }

    public static List<TestItem> getTests() {
        return ITEMS;
    }

    public static TestItem getTest(String id) {
        return ITEM_MAP.get(id);
    }

    public static TestItem getTest(int pos) {
        return ITEMS.get(pos);
    }

    public static Fragment getFragment(String id) {
        try {
            Fragment fragment = getTest(id).mClassFragment.newInstance();
            Bundle arguments = new Bundle();
            arguments.putString("item_id", id);
            fragment.setArguments(arguments);
            return fragment;
        } catch (InstantiationException e) {
            Log.e(LOG_TAG, "Exception", e);
            return null;
        } catch (IllegalAccessException e) {
            Log.e(LOG_TAG, "Exception", e);
            return null;
        }
    }
}
