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

package android.os.storage.cts;

import com.android.cts.stub.R;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.os.Environment;
import android.os.cts.FileUtils;
import android.os.storage.OnObbStateChangeListener;
import android.os.storage.StorageManager;
import android.test.AndroidTestCase;
import android.test.ComparisonFailure;
import android.util.Log;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StorageManagerTest extends AndroidTestCase {

    private static final String TAG = StorageManager.class.getSimpleName();

    private static final long MAX_WAIT_TIME = 25*1000;
    private static final long WAIT_TIME_INCR = 5*1000;

    private static final String OBB_MOUNT_PREFIX = "/mnt/obb/";

    private StorageManager mStorageManager;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mStorageManager = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
    }

    public void testMountAndUnmountObbNormal() {
        for (File target : getTargetFiles()) {
            target = new File(target, "test1.obb");
            Log.d(TAG, "Testing path " + target);
            doMountAndUnmountObbNormal(target);
        }
    }

    private void doMountAndUnmountObbNormal(File outFile) {
        final String canonPath = mountObb(R.raw.test1, outFile, OnObbStateChangeListener.MOUNTED);

        mountObb(R.raw.test1, outFile, OnObbStateChangeListener.ERROR_ALREADY_MOUNTED);

        final String mountPath = checkMountedPath(canonPath);
        final File mountDir = new File(mountPath);

        assertTrue("OBB mounted path should be a directory", mountDir.isDirectory());

        unmountObb(outFile, OnObbStateChangeListener.UNMOUNTED);
    }

    public void testAttemptMountNonObb() {
        for (File target : getTargetFiles()) {
            target = new File(target, "test1_nosig.obb");
            Log.d(TAG, "Testing path " + target);
            doAttemptMountNonObb(target);
        }
    }

    private void doAttemptMountNonObb(File outFile) {
        mountObb(R.raw.test1_nosig, outFile, OnObbStateChangeListener.ERROR_INTERNAL);

        assertFalse("OBB should not be mounted",
                mStorageManager.isObbMounted(outFile.getPath()));

        assertNull("OBB's mounted path should be null",
                mStorageManager.getMountedObbPath(outFile.getPath()));
    }

    public void testAttemptMountObbWrongPackage() {
        for (File target : getTargetFiles()) {
            target = new File(target, "test1_wrongpackage.obb");
            Log.d(TAG, "Testing path " + target);
            doAttemptMountObbWrongPackage(target);
        }
    }

    private void doAttemptMountObbWrongPackage(File outFile) {
        mountObb(R.raw.test1_wrongpackage, outFile,
                OnObbStateChangeListener.ERROR_PERMISSION_DENIED);

        assertFalse("OBB should not be mounted",
                mStorageManager.isObbMounted(outFile.getPath()));

        assertNull("OBB's mounted path should be null",
                mStorageManager.getMountedObbPath(outFile.getPath()));
    }

    public void testMountAndUnmountTwoObbs() {
        for (File target : getTargetFiles()) {
            Log.d(TAG, "Testing target " + target);
            final File test1 = new File(target, "test1.obb");
            final File test2 = new File(target, "test2.obb");
            doMountAndUnmountTwoObbs(test1, test2);
        }
    }

    private void doMountAndUnmountTwoObbs(File file1, File file2) {
        ObbObserver oo1 = mountObbWithoutWait(R.raw.test1, file1);
        ObbObserver oo2 = mountObbWithoutWait(R.raw.test1, file2);

        Log.d(TAG, "Waiting for OBB #1 to complete mount");
        waitForObbActionCompletion(file1, oo1, OnObbStateChangeListener.MOUNTED);
        Log.d(TAG, "Waiting for OBB #2 to complete mount");
        waitForObbActionCompletion(file2, oo2, OnObbStateChangeListener.MOUNTED);

        final String mountPath1 = checkMountedPath(oo1.getPath());
        final File mountDir1 = new File(mountPath1);
        assertTrue("OBB mounted path should be a directory", mountDir1.isDirectory());

        final String mountPath2 = checkMountedPath(oo2.getPath());
        final File mountDir2 = new File(mountPath2);
        assertTrue("OBB mounted path should be a directory", mountDir2.isDirectory());

        unmountObb(file1, OnObbStateChangeListener.UNMOUNTED);
        unmountObb(file2, OnObbStateChangeListener.UNMOUNTED);
    }

    private static void assertStartsWith(String message, String prefix, String actual) {
        if (!actual.startsWith(prefix)) {
            throw new ComparisonFailure(message, prefix, actual);
        }
    }

    private static class ObbObserver extends OnObbStateChangeListener {
        private String path;

        public int state = -1;
        boolean done = false;

        @Override
        public void onObbStateChange(String path, int state) {
            Log.d(TAG, "Received message.  path=" + path + ", state=" + state);
            synchronized (this) {
                this.path = path;
                this.state = state;
                done = true;
                notifyAll();
            }
        }

        public String getPath() {
            assertTrue("Expected ObbObserver to have received a state change.", done);
            return path;
        }

        public int getState() {
            assertTrue("Expected ObbObserver to have received a state change.", done);
            return state;
        }

        public boolean isDone() {
            return done;
        }

        public boolean waitForCompletion() {
            long waitTime = 0;
            synchronized (this) {
                while (!isDone() && waitTime < MAX_WAIT_TIME) {
                    try {
                        wait(WAIT_TIME_INCR);
                        waitTime += WAIT_TIME_INCR;
                    } catch (InterruptedException e) {
                        Log.i(TAG, "Interrupted during sleep", e);
                    }
                }
            }

            return isDone();
        }
    }

    private List<File> getTargetFiles() {
        final List<File> targets = new ArrayList<File>();
        targets.add(mContext.getFilesDir());
        for (File dir : mContext.getObbDirs()) {
            assertNotNull("Valid media must be inserted during CTS", dir);
            assertEquals("Valid media must be inserted during CTS", Environment.MEDIA_MOUNTED,
                    Environment.getStorageState(dir));
            targets.add(dir);
        }
        return targets;
    }

    private void copyRawToFile(int rawResId, File outFile) {
        Resources res = mContext.getResources();
        InputStream is = null;
        try {
            is = res.openRawResource(rawResId);
        } catch (NotFoundException e) {
            fail("Failed to load resource with id: " + rawResId);
        }
        FileUtils.setPermissions(outFile.getPath(), FileUtils.S_IRWXU | FileUtils.S_IRWXG
                | FileUtils.S_IRWXO);
        assertTrue(FileUtils.copyToFile(is, outFile));
        FileUtils.setPermissions(outFile.getPath(), FileUtils.S_IRWXU | FileUtils.S_IRWXG
                | FileUtils.S_IRWXO);
    }

    private String mountObb(final int resource, final File file, int expectedState) {
        copyRawToFile(resource, file);

        final ObbObserver observer = new ObbObserver();
        assertTrue("mountObb call on " + file.getPath() + " should succeed",
                mStorageManager.mountObb(file.getPath(), null, observer));

        assertTrue("Mount should have completed",
                observer.waitForCompletion());

        if (expectedState == OnObbStateChangeListener.MOUNTED) {
            assertTrue("OBB should be mounted", mStorageManager.isObbMounted(observer.getPath()));
        }

        assertEquals(expectedState, observer.getState());

        return observer.getPath();
    }

    private ObbObserver mountObbWithoutWait(final int resource, final File file) {
        copyRawToFile(resource, file);

        final ObbObserver observer = new ObbObserver();
        assertTrue("mountObb call on " + file.getPath() + " should succeed",
                mStorageManager.mountObb(file.getPath(), null, observer));

        return observer;
    }

    private void waitForObbActionCompletion(final File file, final ObbObserver observer,
            int expectedState) {
        assertTrue("Mount should have completed", observer.waitForCompletion());

        assertTrue("OBB should be mounted", mStorageManager.isObbMounted(observer.getPath()));

        assertEquals(expectedState, observer.getState());
    }

    private String checkMountedPath(final String path) {
        final String mountPath = mStorageManager.getMountedObbPath(path);
        assertStartsWith("Path should be in " + OBB_MOUNT_PREFIX,
                OBB_MOUNT_PREFIX,
                mountPath);
        return mountPath;
    }

    private void unmountObb(final File file, int expectedState) {
        final ObbObserver observer = new ObbObserver();

        assertTrue("unmountObb call on test1.obb should succeed",
                mStorageManager.unmountObb(file.getPath(), false, observer));

        assertTrue("Unmount should have completed",
                observer.waitForCompletion());

        assertEquals(expectedState, observer.getState());

        if (expectedState == OnObbStateChangeListener.UNMOUNTED) {
            assertFalse("OBB should not be mounted", mStorageManager.isObbMounted(file.getPath()));
        }
    }
}
