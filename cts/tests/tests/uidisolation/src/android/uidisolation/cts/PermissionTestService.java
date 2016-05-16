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

package android.uidisolation.cts;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.webkit.cts.CtsTestServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class PermissionTestService extends Service {
    private static String TAG = PermissionTestService.class.getName();

    static final String FILE_NAME = "test_file";

    // Message receieved from the client.
    static final int MSG_START_TEST = 1;

    // Messages sent to the client.
    static final int MSG_NOTIFY_TEST_SUCCESS = 2;
    static final int MSG_NOTIFY_TEST_FAILURE = 3;

    // The different tests types we run.
    static final int FILE_READ_TEST = 1;
    static final int FILE_WRITE_TEST = 2;
    static final int NETWORK_ACCESS_TEST = 3;

    private Messenger mClient;

    // Whether we expect to have permissions to access files, network...
    boolean mExpectPermissionsAllowed = true;

    class IncomingHandler extends Handler {
        private PermissionTestService mService;

        IncomingHandler(PermissionTestService service) {
            mService = service;
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what != MSG_START_TEST) {
                Log.e(TAG, "PermissionTestService received bad message: " + msg.what);
                super.handleMessage(msg);
                return;
            }
            mService.mClient = msg.replyTo;
            mService.startTests();
        }
    }

    final Messenger mMessenger = new Messenger(new IncomingHandler(this));

    class NetworkTestAsyncTask extends AsyncTask<Void, Void, Boolean> {
        protected Boolean doInBackground(Void... nothing) {
            return testNetworkAccess();
        }

        protected void onPostExecute(Boolean success) {
            testNetworkAccessDone(success);
        }
    }

    public PermissionTestService() {
        this(true);
    }

    protected PermissionTestService(boolean expectPermissionsAllowed) {
        mExpectPermissionsAllowed = expectPermissionsAllowed;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    private void notifyClientOfFailure(int failingTest) {
        try {
            mClient.send(Message.obtain(null, MSG_NOTIFY_TEST_FAILURE, failingTest, 0, null));
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to send message back to client.");
        }
    }

    private void notifyClientOfSuccess() {
        try {
            mClient.send(Message.obtain(null, MSG_NOTIFY_TEST_SUCCESS));
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to send message back to client.");
        }
    }

    private void startTests() {
        if (testFileReadAccess() != mExpectPermissionsAllowed) {
            notifyClientOfFailure(FILE_READ_TEST);
            return;
        }
        if (testFileWriteAccess() != mExpectPermissionsAllowed) {
            notifyClientOfFailure(FILE_WRITE_TEST);
            return;
        }

        // testNetworkAccess is performed asynchronously and calls testNetworkAccessDone.
        new NetworkTestAsyncTask().execute();
    }

    private void testNetworkAccessDone(boolean success) {
        if (success != mExpectPermissionsAllowed) {
            notifyClientOfFailure(NETWORK_ACCESS_TEST);
            return;
        }
        notifyClientOfSuccess();
    }

    private boolean testFileReadAccess() {
        File f = getApplication().getFileStreamPath(FILE_NAME);
        if (!f.exists()) {
            Log.e(TAG, "testFileReadAccess: test file does not exists.");
            return false;
        }
        if (!f.canRead()) {
            Log.e(TAG, "testFileReadAccess: no permission to read test file.");
            return false;
        }

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(f);
            for (int i = 0; i < 10; i++) {
                int value;
                try {
                    value = fis.read();
                } catch (IOException ioe) {
                    Log.e(TAG, "testFileReadAccess: failed to read test file, IOException.");
                    return false;
                }
                if (value == -1) {
                    Log.e(TAG, "testFileReadAccess: failed to read test file.");
                    return false;
                }
                if (value != i) {
                    Log.e(TAG, "testFileReadAccess: wrong data read from test file.");
                    return false;
                }
            }
        } catch (FileNotFoundException fnfe) {
            Log.e(TAG, "testFileReadAccess: failed to read test file, FileNotFoundException.");
            return false;
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ioe) {
                }
            }
        }
    return true;
}

    private boolean testFileWriteAccess() {
        FileOutputStream fos = null;
        try {
            fos = getApplication().openFileOutput("writeable_file", 0);
            byte[] content = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
            fos.write(content, 0, content.length);
        } catch (FileNotFoundException fnfe) {
            Log.e(TAG, "Failed to open writable file.");
            return false;
        } catch (IOException ioe) {
            Log.e(TAG, "Failed to write to writable file.");
            return false;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ioe) {
                }
            }
        }
        return true;
    }

    private boolean testNetworkAccess() {
        CtsTestServer webServer = null;
        try {
            try {
                webServer = new CtsTestServer(getApplication());
            } catch (Exception e) {
                Log.e(TAG, "Failed to create CtsTestServer.");
                return false;
            }
            URL url;
            try {
                url = new URL(webServer.getAssetUrl("hello.html"));
            } catch (MalformedURLException mue) {
                Log.e(TAG, "Test is bad, could not create the URL in "
                        + "PermissionTestService.testNetworkAccess().");
                return false;
            }
            InputStream is = null;
            try {
                is = url.openStream();
                // Attempt to read some bytes.
                for (int i = 0; i < 10; i++) {
                    int value = is.read();
                    if (value == -1) {
                        Log.e(TAG, "Failed to read byte " + i + " from network connection");
                        return false;
                    }
                }
            } catch (IOException ioe) {
                Log.e(TAG, "Failed to read from network connection: " + ioe);
                return false;
            } catch (SecurityException se) {
                Log.e(TAG, "Failed to read from network connection: " + se);
                return false;
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException ioe) {
                    }
                }
            }
            Log.i(TAG, "Successfully accessed network.");
            return true;
        } finally {
            if (webServer != null) {
                webServer.shutdown();
            }
        }
    }
}
