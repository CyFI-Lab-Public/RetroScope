/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package android.app.cts;


import android.app.Activity;
import android.app.ActivityGroup;
import android.app.LocalActivityManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.app.cts.CTSResult;

public class LocalActivityManagerTestHelper extends ActivityGroup {

    public static final String ACTION_DISPATCH_RESUME = "dispatchResume";
    public static final String ACTION_START_ACTIVITY = "startActivity";
    public static final String ACTION_DISPATCH_CREATE = "dispatchCreate";
    public static final String ACTION_DISPATCH_STOP = "dispatchStop";
    public static final String ACTION_DISPATCH_PAUSE_TRUE = "dispatchPauseTrue";
    public static final String ACTION_DISPATCH_PAUSE_FALSE = "dispatchPauseFalse";
    public static final String ACTION_SAVE_INSTANCE_STATE = "saveInstanceState";
    public static final String ACTION_DISPATCH_DESTROY = "dispatchDestroy";
    public static final String ACTION_REMOVE_ALL_ACTIVITY = "removeAllActivities";

    private String mCurrentAction;
    private LocalActivityManager mLocalActivityManager;

    private static CTSResult sResult;

    public static void setResult(CTSResult cr) {
        sResult = cr;
    }

    public LocalActivityManagerTestHelper() {
        super();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCurrentAction = getIntent().getAction();
        mLocalActivityManager = getLocalActivityManager();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    protected void onResume() {
        super.onResume();
        if (mCurrentAction.equals(ACTION_DISPATCH_RESUME)) {
            testDispatchResume();
        } else if (mCurrentAction.equals(ACTION_START_ACTIVITY)) {
            testStartActivity();
        } else if (mCurrentAction.equals(ACTION_DISPATCH_CREATE)) {
            testDispatchCreate();
        } else if (mCurrentAction.equals(ACTION_DISPATCH_STOP)) {
            testDispatchStop();
        } else if (mCurrentAction.equals(ACTION_DISPATCH_PAUSE_TRUE)) {
            testDispatchPauseTrue();
        } else if (mCurrentAction.equals(ACTION_DISPATCH_PAUSE_FALSE)) {
            testDispatchPauseFalse();
        } else if (mCurrentAction.equals(ACTION_SAVE_INSTANCE_STATE)) {
            testSaveInstanceState();
        } else if (mCurrentAction.equals(ACTION_DISPATCH_DESTROY)) {
            testDispatchDestroy();
        } else if (mCurrentAction.equals(ACTION_REMOVE_ALL_ACTIVITY)) {
            testRemoveAllActivity();
        }
    }

    private void testRemoveAllActivity() {
        final String id = "id_remove_activity";
        final Intent intent = new Intent(this, LocalActivityManagerStubActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mLocalActivityManager.startActivity(id, intent);

        Activity activity = mLocalActivityManager.getActivity(id);
        if (activity == null) {
            fail();
            return;
        }

        if (!activity.getClass().getName().equals("android.app.cts."
                    + "LocalActivityManagerStubActivity")) {
            fail();
            return;
        }

        mLocalActivityManager.removeAllActivities();
        activity = mLocalActivityManager.getActivity(id);
        if (activity != null) {
            fail();
            return;
        }
        pass();
    }

    private void testDispatchDestroy() {
        final String id1 = "id_dispatch_destroy1";
        final String id2 = "id_dispatch_destroy2";
        final Intent intent = new Intent(this, LocalActivityManagerStubActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mLocalActivityManager.startActivity(id1, intent);

        LocalActivityManagerStubActivity.sIsOnDestroyCalled = false;
        mLocalActivityManager.dispatchDestroy(false);
        if (mLocalActivityManager.getCurrentActivity().isFinishing()){
            fail();
            return;
        }

        if (!LocalActivityManagerStubActivity.sIsOnDestroyCalled) {
            fail();
            return;
        }

        mLocalActivityManager.startActivity(id2, intent);
        LocalActivityManagerStubActivity.sIsOnDestroyCalled = false;
        mLocalActivityManager.dispatchDestroy(true);

        if (!LocalActivityManagerStubActivity.sIsOnDestroyCalled) {
            fail();
            return;
        }

        if (!mLocalActivityManager.getCurrentActivity().isFinishing()){
            fail();
            return;
        }
        pass();
    }

    private void testSaveInstanceState() {
        final String key = "_id1";
        mLocalActivityManager.dispatchCreate(null);
        final Bundle bundle = mLocalActivityManager.saveInstanceState();
        if (bundle != null) {
            fail();
            return;
        }

        final String id = "id_dispatch_pause";
        final Intent intent = new Intent(this, LocalActivityManagerStubActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mLocalActivityManager.startActivity(id, intent);

        final Bundle savedBundle = new Bundle();
        final Bundle bb = new Bundle();
        savedBundle.putBundle(key, bb);

        mLocalActivityManager.dispatchCreate(savedBundle);
        final Bundle returnedBundle = mLocalActivityManager.saveInstanceState();
        if (returnedBundle.getBundle(key) == null ) {
            fail();
            return;
        }
        pass();
    }

    private void testDispatchPauseFalse() {
        final String id = "id_dispatch_pause";
        final Intent intent = new Intent(this, LocalActivityManagerStubActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mLocalActivityManager.startActivity(id, intent);
        LocalActivityManagerStubActivity.sIsOnPauseCalled = false;
        mLocalActivityManager.dispatchPause(false);
        if (!LocalActivityManagerStubActivity.sIsOnPauseCalled) {
            fail();
            return;
        }
        pass();
    }

    private void testDispatchPauseTrue() {
        final String id = "id_dispatch_pause";
        final Intent intent = new Intent(this, LocalActivityManagerStubActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mLocalActivityManager.startActivity(id, intent);
        LocalActivityManagerStubActivity.sIsOnPauseCalled = false;
        mLocalActivityManager.dispatchPause(true);
        if (!LocalActivityManagerStubActivity.sIsOnPauseCalled) {
            fail();
            return;
        }
        pass();
    }

    private void testDispatchStop() {
        final String id = "id_dispatch_stop";
        final Intent intent = new Intent(this, LocalActivityManagerStubActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mLocalActivityManager.startActivity(id, intent);
        if (mLocalActivityManager.getCurrentActivity() == null) {
            fail();
            return;
        }

        LocalActivityManagerStubActivity.sIsOnStopCalled = false;
        mLocalActivityManager.dispatchStop();

        if (!LocalActivityManagerStubActivity.sIsOnStopCalled) {
            fail();
            return;
        }
        pass();
    }

    private void testDispatchCreate() {
        final Bundle EXPECTED = new Bundle();
        final String id = "id";

        final Intent intent = new Intent(this, LocalActivityManagerStubActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mLocalActivityManager.startActivity("_id" + System.currentTimeMillis(), intent);
        final Bundle bundle = mLocalActivityManager.saveInstanceState();
        if (bundle == null) {
            fail();
            return;
        }

        if (bundle.keySet().size() != 1) {
            fail();
            return;
        }

        bundle.putBundle(id, EXPECTED);
        // test null parameter
        mLocalActivityManager.dispatchCreate(null);

        if (mLocalActivityManager.saveInstanceState().keySet().size() != 1) {
            fail();
            return;
        }

        mLocalActivityManager.dispatchCreate(bundle);

        final Bundle b = mLocalActivityManager.saveInstanceState();
        final Bundle bb = b.getBundle(id);
        if (bb != EXPECTED) {
            fail();
            return;
        }
        pass();
    }

    private void testStartActivity() {
        final Intent intent = new Intent(this, LocalActivityManagerStubActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        final String id = "_id_resume_test";
        final Window w = mLocalActivityManager.startActivity(id, intent);
        if (w == null) {
            fail();
            return;
        }

        Activity activity = mLocalActivityManager.getActivity(id);
        if (activity == null) {
            fail();
            return;
        }

        // testing null id
        activity = mLocalActivityManager.getActivity("null id");
        if (activity != null) {
            fail();
            return;
        }

        if (!mLocalActivityManager.getCurrentId().equals(id)) {
            fail();
            return;
        }

        if (mLocalActivityManager.getActivity(id) != mLocalActivityManager
                .getCurrentActivity()) {
            fail();
            return;
        }

        if (mLocalActivityManager.destroyActivity(id, true) == null) {
            fail();
            return;
        }

        if (mLocalActivityManager.startActivity(null, intent) == null) {
            fail();
            return;
        }

        try {
            // test when calling startActivity with both null parameter.
            mLocalActivityManager.startActivity(null, null);
            fail();
            return;
        } catch (NullPointerException e) {
        }
        pass();
    }

    private void testDispatchResume() {
        final Intent intent = new Intent(this, LocalActivityManagerStubActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mLocalActivityManager.startActivity("_id_resume_test", intent);
        mLocalActivityManager.dispatchStop();
        LocalActivityManagerStubActivity.sIsOnResumeCalled = false;
        mLocalActivityManager.dispatchResume();
        if (LocalActivityManagerStubActivity.sIsOnResumeCalled) {
            pass();
        } else {
            fail();
        }
    }

    private void fail() {
        sResult.setResult(CTSResult.RESULT_FAIL);
        finish();
    }

    private void pass() {
        sResult.setResult(CTSResult.RESULT_OK);
        finish();
    }
}
