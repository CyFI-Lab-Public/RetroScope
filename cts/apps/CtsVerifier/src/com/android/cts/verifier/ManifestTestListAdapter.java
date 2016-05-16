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

package com.android.cts.verifier;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * {@link TestListAdapter} that populates the {@link TestListActivity}'s {@link ListView} by
 * reading data from the CTS Verifier's AndroidManifest.xml.
 * <p>
 * Making a new test activity to appear in the list requires the following steps:
 *
 * <ol>
 *     <li>REQUIRED: Add an activity to the AndroidManifest.xml with an intent filter with a
 *         main action and the MANUAL_TEST category.
 *         <pre>
 *             <intent-filter>
 *                <action android:name="android.intent.action.MAIN" />
 *                <category android:name="android.cts.intent.category.MANUAL_TEST" />
 *             </intent-filter>
 *         </pre>
 *     </li>
 *     <li>OPTIONAL: Add a meta data attribute to indicate what category of tests the activity
 *         should belong to. If you don't add this attribute, your test will show up in the
 *         "Other" tests category.
 *         <pre>
 *             <meta-data android:name="test_category" android:value="@string/test_category_security" />
 *         </pre>
 *     </li>
 *     <li>OPTIONAL: Add a meta data attribute to indicate whether this test has a parent test.
 *         <pre>
 *             <meta-data android:name="test_parent" android:value="com.android.cts.verifier.bluetooth.BluetoothTestActivity" />
 *         </pre>
 *     </li>
 *     <li>OPTIONAL: Add a meta data attribute to indicate what features are required to run the
 *         test. If the device does not have all of the required features then it will not appear
 *         in the test list. Use a colon (:) to specify multiple required features.
 *         <pre>
 *             <meta-data android:name="test_required_features" android:value="android.hardware.sensor.accelerometer" />
 *         </pre>
 *     </li>
 *
 * </ol>
 */
public class ManifestTestListAdapter extends TestListAdapter {

    private static final String TEST_CATEGORY_META_DATA = "test_category";

    private static final String TEST_PARENT_META_DATA = "test_parent";

    private static final String TEST_REQUIRED_FEATURES_META_DATA = "test_required_features";

    private Context mContext;

    private String mTestParent;

    public ManifestTestListAdapter(Context context, String testParent) {
        super(context);
        mContext = context;
        mTestParent = testParent;
    }

    @Override
    protected List<TestListItem> getRows() {

        /*
         * 1. Get all the tests belonging to the test parent.
         * 2. Get all the tests keyed by their category.
         * 3. Flatten the tests and categories into one giant list for the list view.
         */

        List<ResolveInfo> infos = getResolveInfosForParent();
        Map<String, List<TestListItem>> testsByCategory = getTestsByCategory(infos);

        List<String> testCategories = new ArrayList<String>(testsByCategory.keySet());
        Collections.sort(testCategories);

        List<TestListItem> allRows = new ArrayList<TestListItem>();
        for (String testCategory : testCategories) {
            List<TestListItem> tests = filterTests(testsByCategory.get(testCategory));
            if (!tests.isEmpty()) {
                allRows.add(TestListItem.newCategory(testCategory));
                Collections.sort(tests, new Comparator<TestListItem>() {
                    @Override
                    public int compare(TestListItem item, TestListItem otherItem) {
                        return item.title.compareTo(otherItem.title);
                    }
                });
                allRows.addAll(tests);
            }
        }
        return allRows;
    }

    List<ResolveInfo> getResolveInfosForParent() {
        Intent mainIntent = new Intent(Intent.ACTION_MAIN);
        mainIntent.addCategory(CATEGORY_MANUAL_TEST);

        PackageManager packageManager = mContext.getPackageManager();
        List<ResolveInfo> list = packageManager.queryIntentActivities(mainIntent,
                PackageManager.GET_ACTIVITIES | PackageManager.GET_META_DATA);
        int size = list.size();

        List<ResolveInfo> matchingList = new ArrayList<ResolveInfo>();
        for (int i = 0; i < size; i++) {
            ResolveInfo info = list.get(i);
            String parent = getTestParent(info.activityInfo.metaData);
            if ((mTestParent == null && parent == null)
                    || (mTestParent != null && mTestParent.equals(parent))) {
                matchingList.add(info);
            }
        }
        return matchingList;
    }

    Map<String, List<TestListItem>> getTestsByCategory(List<ResolveInfo> list) {
        Map<String, List<TestListItem>> testsByCategory =
                new HashMap<String, List<TestListItem>>();

        int size = list.size();
        for (int i = 0; i < size; i++) {
            ResolveInfo info = list.get(i);
            String title = getTitle(mContext, info.activityInfo);
            String testName = info.activityInfo.name;
            Intent intent = getActivityIntent(info.activityInfo);
            String[] requiredFeatures = getRequiredFeatures(info.activityInfo.metaData);
            TestListItem item = TestListItem.newTest(title, testName, intent, requiredFeatures);

            String testCategory = getTestCategory(mContext, info.activityInfo.metaData);
            addTestToCategory(testsByCategory, testCategory, item);
        }

        return testsByCategory;
    }

    static String getTestCategory(Context context, Bundle metaData) {
        String testCategory = null;
        if (metaData != null) {
            testCategory = metaData.getString(TEST_CATEGORY_META_DATA);
        }
        if (testCategory != null) {
            return testCategory;
        } else {
            return context.getString(R.string.test_category_other);
        }
    }

    static String getTestParent(Bundle metaData) {
        return metaData != null ? metaData.getString(TEST_PARENT_META_DATA) : null;
    }

    static String[] getRequiredFeatures(Bundle metaData) {
        if (metaData == null) {
            return null;
        } else {
            String value = metaData.getString(TEST_REQUIRED_FEATURES_META_DATA);
            if (value == null) {
                return null;
            } else {
                return value.split(":");
            }
        }
    }

    static String getTitle(Context context, ActivityInfo activityInfo) {
        if (activityInfo.labelRes != 0) {
            return context.getString(activityInfo.labelRes);
        } else {
            return activityInfo.name;
        }
    }

    static Intent getActivityIntent(ActivityInfo activityInfo) {
        Intent intent = new Intent();
        intent.setClassName(activityInfo.packageName, activityInfo.name);
        return intent;
    }

    static void addTestToCategory(Map<String, List<TestListItem>> testsByCategory,
            String testCategory, TestListItem item) {
        List<TestListItem> tests;
        if (testsByCategory.containsKey(testCategory)) {
            tests = testsByCategory.get(testCategory);
        } else {
            tests = new ArrayList<TestListItem>();
        }
        testsByCategory.put(testCategory, tests);
        tests.add(item);
    }

    List<TestListItem> filterTests(List<TestListItem> tests) {
        List<TestListItem> filteredTests = new ArrayList<TestListItem>(tests);
        PackageManager packageManager = mContext.getPackageManager();
        Iterator<TestListItem> iterator = filteredTests.iterator();
        while (iterator.hasNext()) {
            TestListItem item = iterator.next();
            String[] requiredFeatures = item.requiredFeatures;
            if (requiredFeatures != null) {
                for (int i = 0; i < requiredFeatures.length; i++) {
                    if (!packageManager.hasSystemFeature(requiredFeatures[i])) {
                        iterator.remove();
                        break;
                    }
                }
            }
        }
        return filteredTests;
    }
}
