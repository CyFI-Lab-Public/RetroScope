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

package android.media.cts;

import com.android.cts.media.R;


import android.content.ComponentName;
import android.content.Context;
import android.cts.util.PollingCheck;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.IBinder;
import android.provider.cts.FileCopyHelper;
import android.test.AndroidTestCase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MediaScannerConnectionTest extends AndroidTestCase {

    private static final String MEDIA_TYPE = "audio/mpeg";
    private File mMediaFile;
    private static final int TIME_OUT = 10000;
    private MockMediaScannerConnection mMediaScannerConnection;
    private MockMediaScannerConnectionClient mMediaScannerConnectionClient;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // prepare the media file.

        FileCopyHelper copier = new FileCopyHelper(mContext);
        String fileName = "test" + System.currentTimeMillis();
        copier.copy(R.raw.testmp3, fileName);

        File dir = getContext().getFilesDir();
        mMediaFile = new File(dir, fileName);
        assertTrue(mMediaFile.exists());
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if (mMediaFile != null) {
            mMediaFile.delete();
        }
        if (mMediaScannerConnection != null) {
            mMediaScannerConnection.disconnect();
            mMediaScannerConnection = null;
        }
    }

    public void testMediaScannerConnection() throws InterruptedException {
        mMediaScannerConnectionClient = new MockMediaScannerConnectionClient();
        mMediaScannerConnection = new MockMediaScannerConnection(getContext(),
                                    mMediaScannerConnectionClient);

        assertFalse(mMediaScannerConnection.isConnected());

        // test connect and disconnect.
        mMediaScannerConnection.connect();
        checkConnectionState(true);

        assertTrue(mMediaScannerConnection.mIsOnServiceConnectedCalled);
        mMediaScannerConnection.disconnect();

        checkConnectionState(false);

        // FIXME: onServiceDisconnected is not called.
        assertFalse(mMediaScannerConnection.mIsOnServiceDisconnectedCalled);
        mMediaScannerConnection.connect();

        checkConnectionState(true);

        mMediaScannerConnection.scanFile(mMediaFile.getAbsolutePath(), MEDIA_TYPE);

        checkMediaScannerConnection();

        assertEquals(mMediaFile.getAbsolutePath(), mMediaScannerConnectionClient.mediaPath);
        assertNotNull(mMediaScannerConnectionClient.mediaUri);
    }

    private void checkMediaScannerConnection() {
        new PollingCheck(TIME_OUT) {
            protected boolean check() {
                return mMediaScannerConnectionClient.isOnMediaScannerConnectedCalled;
            }
        }.run();
        new PollingCheck(TIME_OUT) {
            protected boolean check() {
                return mMediaScannerConnectionClient.mediaPath != null;
            }
        }.run();
    }

    private void checkConnectionState(final boolean expected) {
        new PollingCheck(TIME_OUT) {
            protected boolean check() {
                return mMediaScannerConnection.isConnected() == expected;
            }
        }.run();
    }

    class MockMediaScannerConnection extends MediaScannerConnection {

        public boolean mIsOnServiceConnectedCalled;
        public boolean mIsOnServiceDisconnectedCalled;
        public MockMediaScannerConnection(Context context, MediaScannerConnectionClient client) {
            super(context, client);
        }

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            super.onServiceConnected(className, service);
            mIsOnServiceConnectedCalled = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            super.onServiceDisconnected(className);
            mIsOnServiceDisconnectedCalled = true;
            // this is not called.
        }
    }

    class MockMediaScannerConnectionClient implements MediaScannerConnectionClient {

        public boolean isOnMediaScannerConnectedCalled;
        public String mediaPath;
        public Uri mediaUri;
        public void onMediaScannerConnected() {
            isOnMediaScannerConnectedCalled = true;
        }

        public void onScanCompleted(String path, Uri uri) {
            mediaPath = path;
            if (uri != null) {
                mediaUri = uri;
            }
        }

    }

}
