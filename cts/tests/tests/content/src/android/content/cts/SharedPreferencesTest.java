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

package android.content.cts;

import com.android.cts.stub.R;

import dalvik.annotation.BrokenTest;

import android.app.QueuedWork;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.test.AndroidTestCase;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Test {@link SharedPreferences}.
 */
public class SharedPreferencesTest extends AndroidTestCase {
    private static final String TAG = "SharedPreferencesTest";

    private Context mContext;
    private ContextWrapper mContextWrapper;

    private File mPrefsFile;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getContext();
        mContextWrapper = new ContextWrapper(mContext);

        SharedPreferences prefs = getPrefs();
        prefs.edit().clear().commit();

        // Duplicated from ContextImpl.java.  Not ideal, but there wasn't a better
        // way to reach into Context{Wrapper,Impl} to ask where this file lives.
        mPrefsFile = new File("/data/data/com.android.cts.stub/shared_prefs",
                              "com.android.cts.stub_preferences.xml");
        mPrefsFile.delete();
    }

    private SharedPreferences getPrefs() {
        return PreferenceManager.getDefaultSharedPreferences(mContext);
    }

    public void testNoFileInitially() {
        assertFalse(mPrefsFile.exists());
    }

    public void testCommitCreatesFiles() {
        SharedPreferences prefs = getPrefs();
        assertFalse(mPrefsFile.exists());
        prefs.edit().putString("foo", "bar").commit();
        assertTrue(mPrefsFile.exists());
    }

    public void testDefaults() {
        SharedPreferences prefs = getPrefs();
        String key = "not-set";
        assertFalse(prefs.contains(key));
        assertEquals(0, prefs.getAll().size());
        assertTrue(prefs.getAll().isEmpty());
        assertEquals(false, prefs.getBoolean(key, false));
        assertEquals(true, prefs.getBoolean(key, true));
        assertEquals(0.5f, prefs.getFloat(key, 0.5f));
        assertEquals(123, prefs.getInt(key, 123));
        assertEquals(999L, prefs.getLong(key, 999L));
        assertEquals("default", prefs.getString(key, "default"));
    }

    private abstract class RedundantWriteTest {
        // Do some initial operation on editor.  No commit needed.
        public abstract void setUp(SharedPreferences.Editor editor);

        // Do some later operation on editor (e.g. a redundant edit).
        // No commit needed.
        public abstract void subsequentEdit(SharedPreferences.Editor editor);

        public boolean expectingMutation() {
            return false;
        }

        // Tests that a redundant edit after an initital setup doesn't
        // result in a duplicate write-out to disk.
        public final void test() throws Exception {
            SharedPreferences prefs = getPrefs();
            SharedPreferences.Editor editor;

            assertFalse(mPrefsFile.exists());
            prefs.edit().commit();
            assertTrue(mPrefsFile.exists());

            editor = prefs.edit();
            setUp(editor);
            editor.commit();
            long modtimeMillis1 = mPrefsFile.lastModified();

            // Wait a second and modify the preferences in a dummy,
            // redundant way.  Wish I could inject a clock or disk mock
            // here, but can't.  Instead relying on checking modtime of
            // file on disk.
            Thread.sleep(1000); // ms

            editor = prefs.edit();
            subsequentEdit(editor);
            editor.commit();

            long modtimeMillis2 = mPrefsFile.lastModified();
            assertEquals(expectingMutation(), modtimeMillis1 != modtimeMillis2);
        }
    };

    public void testRedundantBoolean() throws Exception {
        new RedundantWriteTest() {
            public void setUp(SharedPreferences.Editor editor) {
                editor.putBoolean("foo", true);
            }
            public void subsequentEdit(SharedPreferences.Editor editor) {
                editor.putBoolean("foo", true);
            }
        }.test();
    }

    public void testRedundantString() throws Exception {
        new RedundantWriteTest() {
            public void setUp(SharedPreferences.Editor editor) {
                editor.putString("foo", "bar");
            }
            public void subsequentEdit(SharedPreferences.Editor editor) {
                editor.putString("foo", "bar");
            }
        }.test();
    }

    public void testNonRedundantString() throws Exception {
        new RedundantWriteTest() {
            public void setUp(SharedPreferences.Editor editor) {
                editor.putString("foo", "bar");
            }
            public void subsequentEdit(SharedPreferences.Editor editor) {
                editor.putString("foo", "baz");
            }
            public boolean expectingMutation() {
                return true;
            }
        }.test();
    }

    public void testRedundantClear() throws Exception {
        new RedundantWriteTest() {
            public void setUp(SharedPreferences.Editor editor) {
                editor.clear();
            }
            public void subsequentEdit(SharedPreferences.Editor editor) {
                editor.clear();
            }
        }.test();
    }

    public void testNonRedundantClear() throws Exception {
        new RedundantWriteTest() {
            public void setUp(SharedPreferences.Editor editor) {
                editor.putString("foo", "bar");
            }
            public void subsequentEdit(SharedPreferences.Editor editor) {
                editor.clear();
            }
            public boolean expectingMutation() {
                return true;
            }
        }.test();
    }

    public void testRedundantRemove() throws Exception {
        new RedundantWriteTest() {
            public void setUp(SharedPreferences.Editor editor) {
                editor.putString("foo", "bar");
            }
            public void subsequentEdit(SharedPreferences.Editor editor) {
                editor.remove("not-exist-key");
            }
        }.test();
    }

    public void testRedundantCommitWritesFileIfNotAlreadyExisting() {
        SharedPreferences prefs = getPrefs();
        assertFalse(mPrefsFile.exists());
        prefs.edit().putString("foo", "bar").commit();
        assertTrue(mPrefsFile.exists());

        // Delete the file out from under it.  (not sure why this
        // would happen in practice, but perhaps the app did it for
        // some reason...)
        mPrefsFile.delete();

        // And verify that a redundant edit (which would otherwise not
        // write do disk), still does write to disk if the file isn't
        // there.
        prefs.edit().putString("foo", "bar").commit();
        assertTrue(mPrefsFile.exists());
    }

    public void testTorture() {
        Map<String, String> expectedMap = new HashMap<String, String>();
        Random rand = new Random();

        SharedPreferences prefs = mContext.getSharedPreferences("torture", Context.MODE_PRIVATE);
        prefs.edit().clear().commit();

        for (int i = 0; i < 100; i++) {
            prefs = mContext.getSharedPreferences("torture", Context.MODE_PRIVATE);
            assertEquals(expectedMap, prefs.getAll());

            String key = new Integer(rand.nextInt(25)).toString();
            String value = new Integer(i).toString();
            SharedPreferences.Editor editor = prefs.edit();

            if (rand.nextInt(100) < 85) {
                Log.d(TAG, "Setting " + key + "=" + value);
                editor.putString(key, value);
                expectedMap.put(key, value);
            } else {
                Log.d(TAG, "Removing " + key);
                editor.remove(key);
                expectedMap.remove(key);
            }

            // Use apply on most, but commit some too.
            if (rand.nextInt(100) < 85) {
                Log.d(TAG, "apply.");
                editor.apply();
            } else {
                Log.d(TAG, "commit.");
                editor.commit();
            }

            assertEquals(expectedMap, prefs.getAll());
        }
    }

    // Checks that an in-memory commit doesn't mutate a data structure
    // still being used while writing out to disk.
    public void testTorture2() {
        Random rand = new Random();

        for (int fi = 0; fi < 100; fi++) {
            String prefsName = "torture_" + fi;
            SharedPreferences prefs = mContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
            prefs.edit().clear().commit();
            Map<String, String> expectedMap = new HashMap<String, String>();

            for (int applies = 0; applies < 3; applies++) {
                SharedPreferences.Editor editor = prefs.edit();
                for (int n = 0; n < 1000; n++) {
                    String key = new Integer(rand.nextInt(25)).toString();
                    String value = new Integer(n).toString();
                    editor.putString(key, value);
                    expectedMap.put(key, value);
                }
                editor.apply();
            }
            QueuedWork.waitToFinish();

            String clonePrefsName = prefsName + "_clone";
            File prefsFile = mContext.getSharedPrefsFile(prefsName);
            File prefsFileClone = mContext.getSharedPrefsFile(clonePrefsName);
            prefsFileClone.delete();
            try {
                FileOutputStream fos = new FileOutputStream(prefsFileClone);
                FileInputStream fis = new FileInputStream(prefsFile);
                byte buf[] = new byte[1024];
                int n;
                while ((n = fis.read(buf)) > 0) {
                    fos.write(buf, 0, n);
                }
                fis.close();
                fos.close();
            } catch (IOException e) {
            }

            SharedPreferences clonePrefs = mContext.getSharedPreferences(
                clonePrefsName, Context.MODE_PRIVATE);
            assertEquals(expectedMap, clonePrefs.getAll());

            prefsFile.delete();
            prefsFileClone.delete();
        }
    }

    public void testModeMultiProcess() {
        // Pre-load it.
        mContext.getSharedPreferences("multiprocessTest", 0);

        final StrictMode.ThreadPolicy oldPolicy = StrictMode.getThreadPolicy();
        try {
            StrictMode.ThreadPolicy diskReadDeath =
                    new StrictMode.ThreadPolicy.Builder().detectDiskReads().penaltyDeath().build();
            StrictMode.setThreadPolicy(diskReadDeath);

            // This shouldn't hit disk.  (it was already pre-loaded above)
            mContext.getSharedPreferences("multiprocessTest", 0);

            boolean didRead = false;
            // This SHOULD hit disk.  (multi-process flag is set)
            try {
                mContext.getSharedPreferences("multiprocessTest", Context.MODE_MULTI_PROCESS);
                fail();  // we shouldn't get here.
            } catch (StrictMode.StrictModeViolation e) {
                didRead = true;
            }
            assertTrue(didRead);
        } finally {
            StrictMode.setThreadPolicy(oldPolicy);
        }
    }
}
