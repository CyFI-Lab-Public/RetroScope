/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.os.FileObserver;
import android.test.AndroidTestCase;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileObserverTest extends AndroidTestCase {

    private File mTestFile;
    private File mTestDir;
    private File mMoveDestFile;
    private FileOutputStream mOut;
    private static final String PATH = "/PATH";
    private static final String TEST_FILE = "file_observer_test.txt";
    private static final String TEST_DIR = "fileobserver_dir";
    private static final int FILE_DATA = 0x20;
    private static final int UNDEFINED = 0x8000;
    private static final long DELAY_MSECOND = 2000;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        File dir = getContext().getFilesDir();
        mTestFile = new File(dir, TEST_FILE);
        mTestFile.createNewFile();
        mTestDir = new File(dir, TEST_DIR);
        mTestDir.mkdirs();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if (mTestFile != null && mTestFile.exists()) {
            mTestFile.delete();
        }

        if (mMoveDestFile != null && mMoveDestFile.exists()) {
            mMoveDestFile.delete();
        }

        if (mTestDir != null && mTestDir.exists()) {
            mTestDir.delete();
        }

        if (mOut != null) {
            mOut.close();
        }
    }

    public void testConstructor() {
        // new the instance
        new MockFileObserver(PATH);
        // new the instance
        new MockFileObserver(PATH, FileObserver.ACCESS);
    }

    /*
     * Test point
     * 1. Observe a dir, when it's child file have been written and closed,
     * observer should get modify open-child modify-child and closed-write events.
     * 2. While stop observer a dir, observer should't get any event while delete it's child file.
     * 3. Observer a dir, when create delete a child file and delete self,
     * observer should get create-child close-nowrite delete-child delete-self events.
     * 4. Observer a file, the file moved from dir and the file moved to dir, move the file,
     * file observer should get move-self event,
     * moved from dir observer should get moved-from event,
     * moved to dir observer should get moved-to event.
     */
    public void testFileObserver() throws Exception {
        MockFileObserver fileObserver = null;
        int[] expected = null;
        FileEvent[] moveEvents = null;

        fileObserver = new MockFileObserver(mTestFile.getParent());
        try {
            fileObserver.startWatching();
            mOut = new FileOutputStream(mTestFile);

            mOut.write(FILE_DATA); // modify, open, write, modify
            mOut.close(); // close_write

            expected = new int[] {FileObserver.MODIFY, FileObserver.OPEN, FileObserver.MODIFY,
                    FileObserver.CLOSE_WRITE};
            moveEvents = waitForEvent(fileObserver);
            assertEventsEquals(expected, moveEvents);

            fileObserver.stopWatching();

            // action after observer stop watching
            mTestFile.delete(); // delete

            // should not get any event
            expected = new int[] {UNDEFINED};
            moveEvents = waitForEvent(fileObserver);
            assertEventsEquals(expected, moveEvents);
        } finally {
            fileObserver.stopWatching();
            mOut.close();
            mOut = null;
        }

        fileObserver = new MockFileObserver(mTestDir.getPath());
        try {
            fileObserver.startWatching();
            mTestFile = new File(mTestDir, TEST_FILE);
            assertTrue(mTestFile.createNewFile());
            assertTrue(mTestFile.exists());
            mTestFile.delete();
            mTestDir.delete();

            expected = new int[] {FileObserver.CREATE,
                    FileObserver.OPEN, FileObserver.CLOSE_WRITE,
                    FileObserver.DELETE, FileObserver.DELETE_SELF, UNDEFINED};
            moveEvents = waitForEvent(fileObserver);
            assertEventsEquals(expected, moveEvents);
        } finally {
            fileObserver.stopWatching();
        }

        File dir = getContext().getFilesDir();
        mTestFile = new File(dir, TEST_FILE);
        mTestFile.createNewFile();
        mTestDir = new File(dir, TEST_DIR);
        mTestDir.mkdirs();
        mMoveDestFile = new File(mTestDir, TEST_FILE);
        MockFileObserver movedFrom = new MockFileObserver(dir.getPath());
        MockFileObserver movedTo = new MockFileObserver(mTestDir.getPath());
        fileObserver = new MockFileObserver(mTestFile.getPath());
        try {
            movedFrom.startWatching();
            movedTo.startWatching();
            fileObserver.startWatching();
            mTestFile.renameTo(mMoveDestFile);

            expected = new int[] {FileObserver.MOVE_SELF};
            moveEvents = waitForEvent(fileObserver);
            assertEventsEquals(expected, moveEvents);

            expected = new int[] {FileObserver.MOVED_FROM};
            moveEvents = waitForEvent(movedFrom);
            assertEventsEquals(expected, moveEvents);

            expected = new int[] {FileObserver.MOVED_TO};
            moveEvents = waitForEvent(movedTo);
            assertEventsEquals(expected, moveEvents);
        } finally {
            fileObserver.stopWatching();
            movedTo.stopWatching();
            movedFrom.stopWatching();
        }

        // Because Javadoc didn't specify when should a event happened,
        // here ACCESS ATTRIB we found no way to test.
    }

    private void assertEventsEquals(final int[] expected, final FileEvent[] moveEvents) {
        List<Integer> expectedEvents = new ArrayList<Integer>();
        for (int i = 0; i < expected.length; i++) {
            expectedEvents.add(expected[i]);
        }
        List<FileEvent> actualEvents = Arrays.asList(moveEvents);
        String message = "Expected: " + expectedEvents + " Actual: " + actualEvents;
        assertEquals(message, expected.length, moveEvents.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(message, expected[i], moveEvents[i].event);
        }
    }

    private FileEvent[] waitForEvent(MockFileObserver fileObserver)
            throws InterruptedException {
        Thread.sleep(DELAY_MSECOND);
        synchronized (fileObserver) {
            return fileObserver.getEvents();
       }
    }

    private static class FileEvent {
        public int event = UNDEFINED;
        public String path;

        public FileEvent(final int event, final String path) {
            this.event = event;
            this.path = path;
        }

        @Override
        public String toString() {
            return Integer.toString(event);
        }
    }

    /*
     * MockFileObserver
     */
    private static class MockFileObserver extends FileObserver {

        private List<FileEvent> mEvents = new ArrayList<FileEvent>();

        public MockFileObserver(String path) {
            super(path);
        }

        public MockFileObserver(String path, int mask) {
            super(path, mask);
        }

        @Override
        public synchronized void onEvent(int event, String path) {
            mEvents.add(new FileEvent(event, path));
        }

        public synchronized FileEvent[] getEvents() {
            final FileEvent[] events = new FileEvent[mEvents.size()];
            mEvents.toArray(events);
            mEvents.clear();
            return events;
        }
    }
}
