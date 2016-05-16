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

package android.admin.cts;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.test.AndroidTestCase;

public class DeviceAdminReceiverTest extends AndroidTestCase {

    private static final String DISABLE_WARNING = "Disable Warning";

    private static final int PASSWORD_CHANGED = 0x1;
    private static final int PASSWORD_FAILED = 0x2;
    private static final int PASSWORD_SUCCEEDED = 0x4;
    private static final int DEVICE_ADMIN_ENABLED = 0x8;
    private static final int DEVICE_ADMIN_DISABLE_REQUESTED = 0x10;
    private static final int DEVICE_ADMIN_DISABLED = 0x20;

    private TestReceiver mReceiver;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mReceiver = new TestReceiver();
    }

    public void testOnReceive() {
        mReceiver.reset();
        mReceiver.onReceive(mContext, new Intent(DeviceAdminReceiver.ACTION_PASSWORD_CHANGED));
        assertTrue(mReceiver.hasFlags(PASSWORD_CHANGED));

        mReceiver.reset();
        mReceiver.onReceive(mContext, new Intent(DeviceAdminReceiver.ACTION_PASSWORD_FAILED));
        assertTrue(mReceiver.hasFlags(PASSWORD_FAILED));

        mReceiver.reset();
        mReceiver.onReceive(mContext, new Intent(DeviceAdminReceiver.ACTION_PASSWORD_SUCCEEDED));
        assertTrue(mReceiver.hasFlags(PASSWORD_SUCCEEDED));

        mReceiver.reset();
        mReceiver.onReceive(mContext, new Intent(DeviceAdminReceiver.ACTION_DEVICE_ADMIN_ENABLED));
        assertTrue(mReceiver.hasFlags(DEVICE_ADMIN_ENABLED));

        mReceiver.reset();
        mReceiver.onReceive(mContext, new Intent(DeviceAdminReceiver.ACTION_DEVICE_ADMIN_DISABLED));
        assertTrue(mReceiver.hasFlags(DEVICE_ADMIN_DISABLED));
    }

    private class TestReceiver extends DeviceAdminReceiver {

        private int mFlags = 0;

        void reset() {
            mFlags = 0;
        }

        boolean hasFlags(int flags) {
            return mFlags == flags;
        }

        @Override
        public void onPasswordChanged(Context context, Intent intent) {
            super.onPasswordChanged(context, intent);
            mFlags |= PASSWORD_CHANGED;
        }

        @Override
        public void onPasswordFailed(Context context, Intent intent) {
            super.onPasswordFailed(context, intent);
            mFlags |= PASSWORD_FAILED;
        }

        @Override
        public void onPasswordSucceeded(Context context, Intent intent) {
            super.onPasswordSucceeded(context, intent);
            mFlags |= PASSWORD_SUCCEEDED;
        }

        @Override
        public void onEnabled(Context context, Intent intent) {
            super.onEnabled(context, intent);
            mFlags |= DEVICE_ADMIN_ENABLED;
        }

        @Override
        public CharSequence onDisableRequested(Context context, Intent intent) {
            mFlags |= DEVICE_ADMIN_DISABLE_REQUESTED;
            return DISABLE_WARNING;
        }

        @Override
        public void onDisabled(Context context, Intent intent) {
            super.onDisabled(context, intent);
            mFlags |= DEVICE_ADMIN_DISABLED;
        }
    }
}
