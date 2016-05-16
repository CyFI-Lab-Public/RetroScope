/*
 * Copyright (C) 2009 The Android Open Source Project
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
package android.os.cts;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import android.content.Context;
import android.os.Debug;
import android.test.AndroidTestCase;
import dalvik.system.VMDebug;

public class DebugTest extends AndroidTestCase {
    private static final Logger Log = Logger.getLogger(DebugTest.class.getName());

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        Debug.stopAllocCounting();
        Debug.resetAllCounts();
    }

    public void testPrintLoadedClasses() {
        Debug.printLoadedClasses(Debug.SHOW_FULL_DETAIL);
        Debug.printLoadedClasses(Debug.SHOW_CLASSLOADER);
        Debug.printLoadedClasses(Debug.SHOW_INITIALIZED);
    }

    public void testStartMethodTracing() throws InterruptedException {
        final long debugTime = 3000;
        final String traceName = getFileName();

        final int bufSize = 1024 * 1024 * 2;
        final int debug_flag = VMDebug.TRACE_COUNT_ALLOCS;

        Debug.startMethodTracing();
        Thread.sleep(debugTime);
        Debug.stopMethodTracing();

        Debug.startMethodTracing(traceName);
        Thread.sleep(debugTime);
        Debug.stopMethodTracing();

        Debug.startMethodTracing(traceName, bufSize);
        Thread.sleep(debugTime);
        Debug.stopMethodTracing();

        Debug.startMethodTracing(traceName, bufSize, debug_flag);
        Thread.sleep(debugTime);
        Debug.stopMethodTracing();
    }

    private String getFileName() {
        File dir = getContext().getFilesDir();
        File file = new File(dir, "debug.trace");
        return file.getAbsolutePath();
    }

    public void testStartNativeTracing() {
        Debug.startNativeTracing();

        Debug.stopNativeTracing();
    }

    public void testThreadCpuTimeNanos() throws Exception {
        if (Debug.threadCpuTimeNanos() == -1) {
            // Indicates the system does not support this operation, so we can't test it.
            Log.log(Level.WARNING, "Skipping testThreadCpuTimeNanos() on unsupported system");
            return;
        }

        TestThread t = new TestThread(new Runnable() {
                @Override
                public void run() {
                    long startDebugTime = Debug.threadCpuTimeNanos();

                    // Do some work for a second to increment CPU time
                    long startSystemTime = System.currentTimeMillis();
                    while (System.currentTimeMillis() - startSystemTime < 1000) {
                        Math.random();
                    }

                    // Verify that threadCpuTimeNanos reports that some work was done.
                    // We can't do more than this because the specification for this API call makes
                    // clear that this is only an estimate.
                    assertTrue(Debug.threadCpuTimeNanos() > startDebugTime);
                }
            });
        t.start();
        t.join();
    }

    public void testWaitingForDebugger() {
        assertFalse(Debug.waitingForDebugger());
    }

    public void testGetAndReset() throws IOException {
        final String dumpFile = getFileName();
        Debug.startAllocCounting();

        final int MIN_GLOBAL_ALLOC_COUNT = 100;
        final int ARRAY_SIZE = 100;
        final int MIN_GLOBAL_ALLOC_SIZE = MIN_GLOBAL_ALLOC_COUNT * ARRAY_SIZE;
        for(int i = 0; i < MIN_GLOBAL_ALLOC_COUNT; i++){
            // for test alloc huge memory
            int[] test = new int[ARRAY_SIZE];
        }

        assertTrue(Debug.getGlobalAllocCount() >= MIN_GLOBAL_ALLOC_COUNT);
        assertTrue(Debug.getGlobalAllocSize() >= MIN_GLOBAL_ALLOC_SIZE);
        assertTrue(Debug.getGlobalFreedCount() >= 0);
        assertTrue(Debug.getGlobalFreedSize() >= 0);
        assertTrue(Debug.getNativeHeapSize() >= 0);
        assertTrue(Debug.getGlobalExternalAllocCount() >= 0);
        assertTrue(Debug.getGlobalExternalAllocSize() >= 0);
        assertTrue(Debug.getGlobalExternalFreedCount() >= 0);
        assertTrue(Debug.getGlobalExternalFreedSize() >= 0);
        assertTrue(Debug.getLoadedClassCount() >= 0);
        assertTrue(Debug.getNativeHeapAllocatedSize() >= 0);
        assertTrue(Debug.getNativeHeapFreeSize() >= 0);
        assertTrue(Debug.getNativeHeapSize() >= 0);
        assertTrue(Debug.getThreadAllocCount() >= 0);
        assertTrue(Debug.getThreadAllocSize() >= 0);
        assertTrue(Debug.getThreadExternalAllocCount() >=0);
        assertTrue(Debug.getThreadExternalAllocSize() >= 0);
        assertTrue(Debug.getThreadGcInvocationCount() >= 0);
        assertTrue(Debug.getBinderDeathObjectCount() >= 0);
        assertTrue(Debug.getBinderLocalObjectCount() >= 0);
        assertTrue(Debug.getBinderProxyObjectCount() >= 0);
        Debug.getBinderReceivedTransactions();
        Debug.getBinderSentTransactions();

        Debug.stopAllocCounting();

        Debug.MemoryInfo memoryInfo = new Debug.MemoryInfo();
        Debug.getMemoryInfo(memoryInfo);

        Debug.resetGlobalAllocCount();
        assertEquals(0, Debug.getGlobalAllocCount());

        Debug.resetGlobalAllocSize();
        assertEquals(0, Debug.getGlobalAllocSize());

        Debug.resetGlobalExternalAllocCount();
        assertEquals(0, Debug.getGlobalExternalAllocCount());

        Debug.resetGlobalExternalAllocSize();
        assertEquals(0, Debug.getGlobalExternalAllocSize());

        Debug.resetGlobalExternalFreedCount();
        assertEquals(0, Debug.getGlobalExternalFreedCount());

        Debug.resetGlobalExternalFreedSize();
        assertEquals(0, Debug.getGlobalExternalFreedSize());

        Debug.resetGlobalFreedCount();
        assertEquals(0, Debug.getGlobalFreedCount());

        Debug.resetGlobalFreedSize();
        assertEquals(0, Debug.getGlobalFreedSize());

        Debug.resetGlobalGcInvocationCount();
        assertEquals(0, Debug.getGlobalGcInvocationCount());

        Debug.resetThreadAllocCount();
        assertEquals(0, Debug.getThreadAllocCount());

        Debug.resetThreadAllocSize();
        assertEquals(0, Debug.getThreadAllocSize());

        Debug.resetThreadExternalAllocCount();
        assertEquals(0, Debug.getThreadExternalAllocCount());

        Debug.resetThreadExternalAllocSize();
        assertEquals(0, Debug.getThreadExternalAllocSize());

        Debug.resetThreadGcInvocationCount();
        assertEquals(0, Debug.getThreadGcInvocationCount());

        Debug.resetAllCounts();
        Debug.dumpHprofData(dumpFile);
    }

    public void testDumpService() throws Exception {
        File file = getContext().getFileStreamPath("dump.out");
        file.delete();
        assertFalse(file.exists());

        FileOutputStream out = getContext().openFileOutput("dump.out", Context.MODE_PRIVATE);
        assertFalse(Debug.dumpService("xyzzy -- not a valid service name", out.getFD(), null));
        out.close();

        // File was opened, but nothing was written
        assertTrue(file.exists());
        assertEquals(0, file.length());

        out = getContext().openFileOutput("dump.out", Context.MODE_PRIVATE);
        assertTrue(Debug.dumpService(Context.POWER_SERVICE, out.getFD(), null));
        out.close();

        // Don't require any specific content, just that something was written
        assertTrue(file.exists());
        assertTrue(file.length() > 0);
    }
}
