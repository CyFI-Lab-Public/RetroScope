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

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.CallLog.Calls;
import android.provider.VoicemailContract;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class for all tests that require interacting with the voicemail content provider.
 */
public class BaseVoicemailProviderTest extends BaseContactsProvider2Test {
    private static final String READ_WRITE_ALL_PERMISSION =
            "com.android.voicemail.permission.READ_WRITE_ALL_VOICEMAIL";
    private static final String ADD_VOICEMAIL_PERMISSION =
            "com.android.voicemail.permission.ADD_VOICEMAIL";

    protected boolean mUseSourceUri = false;
    private File mTestDirectory;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        addProvider(TestVoicemailProvider.class, VoicemailContract.AUTHORITY);
        TestVoicemailProvider.setVvmProviderCallDelegate(createMockProviderCalls());
    }

    @Override
    protected void tearDown() throws Exception {
        removeTestDirectory();
        super.tearDown();
    }

    @Override
    protected Class<? extends ContentProvider> getProviderClass() {
       return TestVoicemailProvider.class;
    }

    @Override
    protected String getAuthority() {
        return VoicemailContract.AUTHORITY;
    }

    protected void setUpForOwnPermission() {
        // Give away full permission, in case it was granted previously.
        mActor.removePermissions(READ_WRITE_ALL_PERMISSION);
        mActor.addPermissions(ADD_VOICEMAIL_PERMISSION);
        mUseSourceUri = true;
    }

    protected void setUpForFullPermission() {
        mActor.addPermissions(ADD_VOICEMAIL_PERMISSION);
        mActor.addPermissions(READ_WRITE_ALL_PERMISSION);
        mUseSourceUri = false;
    }

    protected void setUpForNoPermission() {
        mActor.removePermissions(ADD_VOICEMAIL_PERMISSION);
        mActor.removePermissions(READ_WRITE_ALL_PERMISSION);
        mUseSourceUri = true;
    }

    protected int countFilesInTestDirectory() {
        return findAllFiles(mTestDirectory).size();
    }

    // TODO: Use a mocking framework to mock these calls.
    private VvmProviderCalls createMockProviderCalls() {
        return new VvmProviderCalls() {
            @Override
            public void sendOrderedBroadcast(Intent intent, String receiverPermission) {
                // Do nothing for now.
            }

            @Override
            public File getDir(String name, int mode) {
                return getTestDirectory();
            }
        };
    }

    /** Lazily construct the test directory when required. */
    private synchronized File getTestDirectory() {
        if (mTestDirectory == null) {
            File baseDirectory = getContext().getCacheDir();
            mTestDirectory = new File(baseDirectory, Long.toString(System.currentTimeMillis()));
            assertFalse(mTestDirectory.exists());
            assertTrue(mTestDirectory.mkdirs());
        }
        return mTestDirectory;
    }

    private void removeTestDirectory() {
        if (mTestDirectory != null) {
            recursiveDeleteAll(mTestDirectory);
        }
    }

    private static void recursiveDeleteAll(File input) {
        if (input.isDirectory()) {
            for (File file : input.listFiles()) {
                recursiveDeleteAll(file);
            }
        }
        assertTrue("error deleting " + input.getAbsolutePath(), input.delete());
    }

    private List<File> findAllFiles(File input) {
        if (input == null) {
            return Collections.emptyList();
        }
        if (!input.isDirectory()) {
            return Collections.singletonList(input);
        }
        List<File> results = new ArrayList<File>();
        for (File file : input.listFiles()) {
            results.addAll(findAllFiles(file));
        }
        return results;
    }

    /** The calls that we need to mock out for our VvmProvider, used by TestVoicemailProvider. */
    private interface VvmProviderCalls {
        public void sendOrderedBroadcast(Intent intent, String receiverPermission);
        public File getDir(String name, int mode);
    }

    public static class TestVoicemailProvider extends VoicemailContentProvider {
        private static VvmProviderCalls mDelegate;
        public static synchronized void setVvmProviderCallDelegate(VvmProviderCalls delegate) {
            mDelegate = delegate;
        }

        @Override
        protected ContactsDatabaseHelper getDatabaseHelper(Context context) {
            return ContactsDatabaseHelper.getNewInstanceForTest(context);
        }

        @Override
        protected Context context() {
            return new ContextWrapper(getContext()) {
                @Override
                public File getDir(String name, int mode) {
                    return mDelegate.getDir(name, mode);
                }
                @Override
                public void sendBroadcast(Intent intent, String receiverPermission) {
                    mDelegate.sendOrderedBroadcast(intent, receiverPermission);
                }
                @Override
                public PackageManager getPackageManager() {
                    return new MockPackageManager("com.test.package1", "com.test.package2");
                }
            };
        }

        @Override
        protected String getCallingPackage_() {
            return getContext().getPackageName();
        }

        @Override
        CallLogInsertionHelper createCallLogInsertionHelper(Context context) {
            return new CallLogInsertionHelper() {
                @Override
                public String getGeocodedLocationFor(String number, String countryIso) {
                    return "usa";
                }

                @Override
                public void addComputedValues(ContentValues values) {
                    values.put(Calls.COUNTRY_ISO, "us");
                    values.put(Calls.GEOCODED_LOCATION, "usa");
                }
            };
        }
    }
}
