/*
 * Copyright (C) 2013 The Android Open Source Project
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

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.deviceadmin.cts.CtsDeviceAdminBrokenReceiver;
import android.deviceadmin.cts.CtsDeviceAdminBrokenReceiver2;
import android.deviceadmin.cts.CtsDeviceAdminBrokenReceiver3;
import android.deviceadmin.cts.CtsDeviceAdminBrokenReceiver4;
import android.deviceadmin.cts.CtsDeviceAdminBrokenReceiver5;
import android.deviceadmin.cts.CtsDeviceAdminDeactivatedReceiver;
import android.deviceadmin.cts.CtsDeviceAdminActivationTestActivity;
import android.deviceadmin.cts.CtsDeviceAdminActivationTestActivity.OnActivityResultListener;
import android.os.SystemClock;
import android.test.ActivityInstrumentationTestCase2;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

/**
 * Tests for the standard way of activating a Device Admin: by starting system UI via an
 * {@link Intent} with {@link DevicePolicyManager#ACTION_ADD_DEVICE_ADMIN}. The test requires that
 * the {@code CtsDeviceAdmin.apk} be installed.
 */
public class DeviceAdminActivationTest
    extends ActivityInstrumentationTestCase2<CtsDeviceAdminActivationTestActivity> {

    // IMPLEMENTATION NOTE: Because Device Admin activation requires the use of
    // Activity.startActivityForResult, this test creates an empty Activity which then invokes
    // startActivityForResult.

    private static final int REQUEST_CODE_ACTIVATE_ADMIN = 1;

    /**
     * Maximum duration of time (milliseconds) after which the effects of programmatic actions in
     * this test should have affected the UI.
     */
    private static final int UI_EFFECT_TIMEOUT_MILLIS = 5000;

    @Mock private OnActivityResultListener mMockOnActivityResultListener;

    public DeviceAdminActivationTest() {
        super(CtsDeviceAdminActivationTestActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);
        getActivity().setOnActivityResultListener(mMockOnActivityResultListener);
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            finishActivateDeviceAdminActivity();
        } finally {
            super.tearDown();
        }
    }

    public void testActivateGoodReceiverDisplaysActivationUi() throws Exception {
        assertDeviceAdminDeactivated(CtsDeviceAdminDeactivatedReceiver.class);
        startAddDeviceAdminActivityForResult(CtsDeviceAdminDeactivatedReceiver.class);
        assertWithTimeoutOnActivityResultNotInvoked();
        // The UI is up and running. Assert that dismissing the UI returns the corresponding result
        // to the test activity.
        finishActivateDeviceAdminActivity();
        assertWithTimeoutOnActivityResultInvokedWithResultCode(Activity.RESULT_CANCELED);
        assertDeviceAdminDeactivated(CtsDeviceAdminDeactivatedReceiver.class);
    }

    public void testActivateBrokenReceiverFails() throws Exception {
        assertDeviceAdminDeactivated(CtsDeviceAdminBrokenReceiver.class);
        startAddDeviceAdminActivityForResult(CtsDeviceAdminBrokenReceiver.class);
        assertWithTimeoutOnActivityResultInvokedWithResultCode(Activity.RESULT_CANCELED);
        assertDeviceAdminDeactivated(CtsDeviceAdminBrokenReceiver.class);
    }

    public void testActivateBrokenReceiver2Fails() throws Exception {
        assertDeviceAdminDeactivated(CtsDeviceAdminBrokenReceiver2.class);
        startAddDeviceAdminActivityForResult(CtsDeviceAdminBrokenReceiver2.class);
        assertWithTimeoutOnActivityResultInvokedWithResultCode(Activity.RESULT_CANCELED);
        assertDeviceAdminDeactivated(CtsDeviceAdminBrokenReceiver2.class);
    }

    public void testActivateBrokenReceiver3Fails() throws Exception {
        assertDeviceAdminDeactivated(CtsDeviceAdminBrokenReceiver3.class);
        startAddDeviceAdminActivityForResult(CtsDeviceAdminBrokenReceiver3.class);
        assertWithTimeoutOnActivityResultInvokedWithResultCode(Activity.RESULT_CANCELED);
        assertDeviceAdminDeactivated(CtsDeviceAdminBrokenReceiver3.class);
    }

    public void testActivateBrokenReceiver4Fails() throws Exception {
        assertDeviceAdminDeactivated(CtsDeviceAdminBrokenReceiver4.class);
        startAddDeviceAdminActivityForResult(CtsDeviceAdminBrokenReceiver4.class);
        assertWithTimeoutOnActivityResultInvokedWithResultCode(Activity.RESULT_CANCELED);
        assertDeviceAdminDeactivated(CtsDeviceAdminBrokenReceiver4.class);
    }

    public void testActivateBrokenReceiver5Fails() throws Exception {
        assertDeviceAdminDeactivated(CtsDeviceAdminBrokenReceiver5.class);
        startAddDeviceAdminActivityForResult(CtsDeviceAdminBrokenReceiver5.class);
        assertWithTimeoutOnActivityResultInvokedWithResultCode(Activity.RESULT_CANCELED);
        assertDeviceAdminDeactivated(CtsDeviceAdminBrokenReceiver5.class);
    }

    private void startAddDeviceAdminActivityForResult(Class<?> receiverClass) {
        getActivity().startActivityForResult(
                getAddDeviceAdminIntent(receiverClass),
                REQUEST_CODE_ACTIVATE_ADMIN);
    }

    private Intent getAddDeviceAdminIntent(Class<?> receiverClass) {
        return new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            .putExtra(
                    DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                    new ComponentName(
                            getInstrumentation().getTargetContext(),
                            receiverClass));
    }

    private void assertWithTimeoutOnActivityResultNotInvoked() {
        SystemClock.sleep(UI_EFFECT_TIMEOUT_MILLIS);
        Mockito.verify(mMockOnActivityResultListener, Mockito.never())
                .onActivityResult(
                        Mockito.eq(REQUEST_CODE_ACTIVATE_ADMIN),
                        Mockito.anyInt(),
                        Mockito.any(Intent.class));
    }

    private void assertWithTimeoutOnActivityResultInvokedWithResultCode(int expectedResultCode) {
        ArgumentCaptor<Integer> resultCodeCaptor = ArgumentCaptor.forClass(int.class);
        Mockito.verify(mMockOnActivityResultListener, Mockito.timeout(UI_EFFECT_TIMEOUT_MILLIS))
                .onActivityResult(
                        Mockito.eq(REQUEST_CODE_ACTIVATE_ADMIN),
                        resultCodeCaptor.capture(),
                        Mockito.any(Intent.class));
        assertEquals(expectedResultCode, (int) resultCodeCaptor.getValue());
    }

    private void finishActivateDeviceAdminActivity() {
        getActivity().finishActivity(REQUEST_CODE_ACTIVATE_ADMIN);
    }

    private void assertDeviceAdminDeactivated(Class<?> receiverClass) {
        DevicePolicyManager devicePolicyManager =
                (DevicePolicyManager) getActivity().getSystemService(
                        Context.DEVICE_POLICY_SERVICE);
        assertFalse(devicePolicyManager.isAdminActive(
                new ComponentName(getInstrumentation().getTargetContext(), receiverClass)));
    }
}
