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

package android.security.cts;

import android.os.IBinder;
import android.test.AndroidTestCase;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

/**
 * Verifies that permissions are enforced on various system services.
 */
public class ServicePermissionsTest extends AndroidTestCase {

    private static final String TAG = "ServicePermissionsTest";

    private File mTempFile;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mTempFile = new File(getContext().getCacheDir(), "CTS_DUMP");
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            mTempFile.delete();
        } finally {
            super.tearDown();
        }
    }

    /**
     * Test that {@link IBinder#dump(java.io.FileDescriptor, String[])} on all
     * registered system services checks if caller holds
     * {@link android.Manifest.permission#DUMP} permission.
     */
    public void testDumpProtected() throws Exception {

        String[] services = null;
        try {
            services = (String[]) Class.forName("android.os.ServiceManager")
                    .getDeclaredMethod("listServices").invoke(null);
        } catch (ClassCastException e) {
        } catch (ClassNotFoundException e) {
        } catch (NoSuchMethodException e) {
        } catch (InvocationTargetException e) {
        } catch (IllegalAccessException e) {
        }

        if ((services == null) || (services.length == 0)) {
            Log.w(TAG, "No registered services, that's odd");
            return;
        }

        for (String service : services) {
            mTempFile.delete();

            IBinder serviceBinder = null;
            try {
                serviceBinder = (IBinder) Class.forName("android.os.ServiceManager")
                        .getDeclaredMethod("getService", String.class).invoke(null, service);
            } catch (ClassCastException e) {
            } catch (ClassNotFoundException e) {
            } catch (NoSuchMethodException e) {
            } catch (InvocationTargetException e) {
            } catch (IllegalAccessException e) {
            }

            if (serviceBinder == null) {
                Log.w(TAG, "Missing service " + service);
                continue;
            }

            Log.d(TAG, "Dumping service " + service);
            final FileOutputStream out = new FileOutputStream(mTempFile);
            try {
                serviceBinder.dump(out.getFD(), new String[0]);
            } catch (SecurityException e) {
                if (e.getMessage().contains("android.permission.DUMP")) {
                    // Service correctly checked for DUMP permission, yay
                } else {
                    // Service is throwing about something else; they're
                    // probably not checking for DUMP.
                    throw e;
                }
            } finally {
                out.close();
            }

            // Verify that dump produced minimal output
            final BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(mTempFile)));
            final ArrayList<String> lines = new ArrayList<String>();
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                    Log.v(TAG, "--> " + line);
                }
            } finally {
                reader.close();
            }

            if (lines.size() > 1) {
                fail("dump() for " + service + " produced several lines of output; this "
                        + "may be leaking sensitive data.  At most, services should emit a "
                        + "single line when the caller doesn't have DUMP permission.");
            }

            if (lines.size() == 1) {
                String message = lines.get(0);
                if (!message.contains("Permission Denial") &&
                        !message.contains("android.permission.DUMP")) {
                    fail("dump() for " + service + " produced a single line which didn't "
                            + "reference a permission; it may be leaking sensitive data.");
                }
            }
        }
    }
}
