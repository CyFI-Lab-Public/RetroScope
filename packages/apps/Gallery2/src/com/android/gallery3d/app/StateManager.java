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

package com.android.gallery3d.app;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.Menu;
import android.view.MenuItem;

import com.android.gallery3d.anim.StateTransitionAnimation;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.util.UsageStatistics;

import java.util.Stack;

public class StateManager {
    @SuppressWarnings("unused")
    private static final String TAG = "StateManager";
    private boolean mIsResumed = false;

    private static final String KEY_MAIN = "activity-state";
    private static final String KEY_DATA = "data";
    private static final String KEY_STATE = "bundle";
    private static final String KEY_CLASS = "class";

    private AbstractGalleryActivity mActivity;
    private Stack<StateEntry> mStack = new Stack<StateEntry>();
    private ActivityState.ResultEntry mResult;

    public StateManager(AbstractGalleryActivity activity) {
        mActivity = activity;
    }

    public void startState(Class<? extends ActivityState> klass,
            Bundle data) {
        Log.v(TAG, "startState " + klass);
        ActivityState state = null;
        try {
            state = klass.newInstance();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        if (!mStack.isEmpty()) {
            ActivityState top = getTopState();
            top.transitionOnNextPause(top.getClass(), klass,
                    StateTransitionAnimation.Transition.Incoming);
            if (mIsResumed) top.onPause();
        }

        UsageStatistics.onContentViewChanged(
                UsageStatistics.COMPONENT_GALLERY,
                klass.getSimpleName());
        state.initialize(mActivity, data);

        mStack.push(new StateEntry(data, state));
        state.onCreate(data, null);
        if (mIsResumed) state.resume();
    }

    public void startStateForResult(Class<? extends ActivityState> klass,
            int requestCode, Bundle data) {
        Log.v(TAG, "startStateForResult " + klass + ", " + requestCode);
        ActivityState state = null;
        try {
            state = klass.newInstance();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        state.initialize(mActivity, data);
        state.mResult = new ActivityState.ResultEntry();
        state.mResult.requestCode = requestCode;

        if (!mStack.isEmpty()) {
            ActivityState as = getTopState();
            as.transitionOnNextPause(as.getClass(), klass,
                    StateTransitionAnimation.Transition.Incoming);
            as.mReceivedResults = state.mResult;
            if (mIsResumed) as.onPause();
        } else {
            mResult = state.mResult;
        }
        UsageStatistics.onContentViewChanged(UsageStatistics.COMPONENT_GALLERY,
                klass.getSimpleName());
        mStack.push(new StateEntry(data, state));
        state.onCreate(data, null);
        if (mIsResumed) state.resume();
    }

    public boolean createOptionsMenu(Menu menu) {
        if (mStack.isEmpty()) {
            return false;
        } else {
            return getTopState().onCreateActionBar(menu);
        }
    }

    public void onConfigurationChange(Configuration config) {
        for (StateEntry entry : mStack) {
            entry.activityState.onConfigurationChanged(config);
        }
    }

    public void resume() {
        if (mIsResumed) return;
        mIsResumed = true;
        if (!mStack.isEmpty()) getTopState().resume();
    }

    public void pause() {
        if (!mIsResumed) return;
        mIsResumed = false;
        if (!mStack.isEmpty()) getTopState().onPause();
    }

    public void notifyActivityResult(int requestCode, int resultCode, Intent data) {
        getTopState().onStateResult(requestCode, resultCode, data);
    }

    public void clearActivityResult() {
        if (!mStack.isEmpty()) {
            getTopState().clearStateResult();
        }
    }

    public int getStateCount() {
        return mStack.size();
    }

    public boolean itemSelected(MenuItem item) {
        if (!mStack.isEmpty()) {
            if (getTopState().onItemSelected(item)) return true;
            if (item.getItemId() == android.R.id.home) {
                if (mStack.size() > 1) {
                    getTopState().onBackPressed();
                }
                return true;
            }
        }
        return false;
    }

    public void onBackPressed() {
        if (!mStack.isEmpty()) {
            getTopState().onBackPressed();
        }
    }

    void finishState(ActivityState state) {
        finishState(state, true);
    }

    public void clearTasks() {
        // Remove all the states that are on top of the bottom PhotoPage state
        while (mStack.size() > 1) {
            mStack.pop().activityState.onDestroy();
        }
    }

    void finishState(ActivityState state, boolean fireOnPause) {
        // The finish() request could be rejected (only happens under Monkey),
        // If it is rejected, we won't close the last page.
        if (mStack.size() == 1) {
            Activity activity = (Activity) mActivity.getAndroidContext();
            if (mResult != null) {
                activity.setResult(mResult.resultCode, mResult.resultData);
            }
            activity.finish();
            if (!activity.isFinishing()) {
                Log.w(TAG, "finish is rejected, keep the last state");
                return;
            }
            Log.v(TAG, "no more state, finish activity");
        }

        Log.v(TAG, "finishState " + state);
        if (state != mStack.peek().activityState) {
            if (state.isDestroyed()) {
                Log.d(TAG, "The state is already destroyed");
                return;
            } else {
                throw new IllegalArgumentException("The stateview to be finished"
                        + " is not at the top of the stack: " + state + ", "
                        + mStack.peek().activityState);
            }
        }

        // Remove the top state.
        mStack.pop();
        state.mIsFinishing = true;
        ActivityState top = !mStack.isEmpty() ? mStack.peek().activityState : null;
        if (mIsResumed && fireOnPause) {
            if (top != null) {
                state.transitionOnNextPause(state.getClass(), top.getClass(),
                        StateTransitionAnimation.Transition.Outgoing);
            }
            state.onPause();
        }
        mActivity.getGLRoot().setContentPane(null);
        state.onDestroy();

        if (top != null && mIsResumed) top.resume();
        if (top != null) {
            UsageStatistics.onContentViewChanged(UsageStatistics.COMPONENT_GALLERY,
                    top.getClass().getSimpleName());
        }
    }

    public void switchState(ActivityState oldState,
            Class<? extends ActivityState> klass, Bundle data) {
        Log.v(TAG, "switchState " + oldState + ", " + klass);
        if (oldState != mStack.peek().activityState) {
            throw new IllegalArgumentException("The stateview to be finished"
                    + " is not at the top of the stack: " + oldState + ", "
                    + mStack.peek().activityState);
        }
        // Remove the top state.
        mStack.pop();
        if (!data.containsKey(PhotoPage.KEY_APP_BRIDGE)) {
            // Do not do the fade out stuff when we are switching camera modes
            oldState.transitionOnNextPause(oldState.getClass(), klass,
                    StateTransitionAnimation.Transition.Incoming);
        }
        if (mIsResumed) oldState.onPause();
        oldState.onDestroy();

        // Create new state.
        ActivityState state = null;
        try {
            state = klass.newInstance();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        state.initialize(mActivity, data);
        mStack.push(new StateEntry(data, state));
        state.onCreate(data, null);
        if (mIsResumed) state.resume();
        UsageStatistics.onContentViewChanged(UsageStatistics.COMPONENT_GALLERY,
                klass.getSimpleName());
    }

    public void destroy() {
        Log.v(TAG, "destroy");
        while (!mStack.isEmpty()) {
            mStack.pop().activityState.onDestroy();
        }
        mStack.clear();
    }

    @SuppressWarnings("unchecked")
    public void restoreFromState(Bundle inState) {
        Log.v(TAG, "restoreFromState");
        Parcelable list[] = inState.getParcelableArray(KEY_MAIN);
        ActivityState topState = null;
        for (Parcelable parcelable : list) {
            Bundle bundle = (Bundle) parcelable;
            Class<? extends ActivityState> klass =
                    (Class<? extends ActivityState>) bundle.getSerializable(KEY_CLASS);

            Bundle data = bundle.getBundle(KEY_DATA);
            Bundle state = bundle.getBundle(KEY_STATE);

            ActivityState activityState;
            try {
                Log.v(TAG, "restoreFromState " + klass);
                activityState = klass.newInstance();
            } catch (Exception e) {
                throw new AssertionError(e);
            }
            activityState.initialize(mActivity, data);
            activityState.onCreate(data, state);
            mStack.push(new StateEntry(data, activityState));
            topState = activityState;
        }
        if (topState != null) {
            UsageStatistics.onContentViewChanged(UsageStatistics.COMPONENT_GALLERY,
                    topState.getClass().getSimpleName());
        }
    }

    public void saveState(Bundle outState) {
        Log.v(TAG, "saveState");

        Parcelable list[] = new Parcelable[mStack.size()];
        int i = 0;
        for (StateEntry entry : mStack) {
            Bundle bundle = new Bundle();
            bundle.putSerializable(KEY_CLASS, entry.activityState.getClass());
            bundle.putBundle(KEY_DATA, entry.data);
            Bundle state = new Bundle();
            entry.activityState.onSaveState(state);
            bundle.putBundle(KEY_STATE, state);
            Log.v(TAG, "saveState " + entry.activityState.getClass());
            list[i++] = bundle;
        }
        outState.putParcelableArray(KEY_MAIN, list);
    }

    public boolean hasStateClass(Class<? extends ActivityState> klass) {
        for (StateEntry entry : mStack) {
            if (klass.isInstance(entry.activityState)) {
                return true;
            }
        }
        return false;
    }

    public ActivityState getTopState() {
        Utils.assertTrue(!mStack.isEmpty());
        return mStack.peek().activityState;
    }

    private static class StateEntry {
        public Bundle data;
        public ActivityState activityState;

        public StateEntry(Bundle data, ActivityState state) {
            this.data = data;
            this.activityState = state;
        }
    }
}
