/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.incallui;

import com.android.services.telephony.common.Call;
import com.android.services.telephony.common.Call.State;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

/**
 * Phone app "in call" screen.
 */
public class InCallActivity extends Activity {

    public static final String SHOW_DIALPAD_EXTRA = "InCallActivity.show_dialpad";

    private static final int INVALID_RES_ID = -1;

    private CallButtonFragment mCallButtonFragment;
    private CallCardFragment mCallCardFragment;
    private AnswerFragment mAnswerFragment;
    private DialpadFragment mDialpadFragment;
    private ConferenceManagerFragment mConferenceManagerFragment;
    private boolean mIsForegroundActivity;
    private AlertDialog mDialog;

    /** Use to pass 'showDialpad' from {@link #onNewIntent} to {@link #onResume} */
    private boolean mShowDialpadRequested;

    @Override
    protected void onCreate(Bundle icicle) {
        Log.d(this, "onCreate()...  this = " + this);

        super.onCreate(icicle);

        // set this flag so this activity will stay in front of the keyguard
        // Have the WindowManager filter out touch events that are "too fat".
        int flags = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES;

        getWindow().addFlags(flags);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // TODO(klp): Do we need to add this back when prox sensor is not available?
        // lp.inputFeatures |= WindowManager.LayoutParams.INPUT_FEATURE_DISABLE_USER_ACTIVITY;

        // Inflate everything in incall_screen.xml and add it to the screen.
        setContentView(R.layout.incall_screen);

        initializeInCall();
        Log.d(this, "onCreate(): exit");
    }

    @Override
    protected void onStart() {
        Log.d(this, "onStart()...");
        super.onStart();

        // setting activity should be last thing in setup process
        InCallPresenter.getInstance().setActivity(this);
    }

    @Override
    protected void onResume() {
        Log.i(this, "onResume()...");
        super.onResume();

        mIsForegroundActivity = true;
        InCallPresenter.getInstance().onUiShowing(true);

        if (mShowDialpadRequested) {
            mCallButtonFragment.displayDialpad(true);
            mShowDialpadRequested = false;
        }
    }

    // onPause is guaranteed to be called when the InCallActivity goes
    // in the background.
    @Override
    protected void onPause() {
        Log.d(this, "onPause()...");
        super.onPause();

        mIsForegroundActivity = false;

        mDialpadFragment.onDialerKeyUp(null);

        InCallPresenter.getInstance().onUiShowing(false);
    }

    @Override
    protected void onStop() {
        Log.d(this, "onStop()...");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(this, "onDestroy()...  this = " + this);

        InCallPresenter.getInstance().setActivity(null);

        super.onDestroy();
    }

    /**
     * Returns true when theActivity is in foreground (between onResume and onPause).
     */
    /* package */ boolean isForegroundActivity() {
        return mIsForegroundActivity;
    }

    private boolean hasPendingErrorDialog() {
        return mDialog != null;
    }
    /**
     * Dismisses the in-call screen.
     *
     * We never *really* finish() the InCallActivity, since we don't want to get destroyed and then
     * have to be re-created from scratch for the next call.  Instead, we just move ourselves to the
     * back of the activity stack.
     *
     * This also means that we'll no longer be reachable via the BACK button (since moveTaskToBack()
     * puts us behind the Home app, but the home app doesn't allow the BACK key to move you any
     * farther down in the history stack.)
     *
     * (Since the Phone app itself is never killed, this basically means that we'll keep a single
     * InCallActivity instance around for the entire uptime of the device.  This noticeably improves
     * the UI responsiveness for incoming calls.)
     */
    @Override
    public void finish() {
        Log.i(this, "finish().  Dialog showing: " + (mDialog != null));

        // skip finish if we are still showing a dialog.
        if (!hasPendingErrorDialog() && !mAnswerFragment.hasPendingDialogs()) {
            super.finish();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.d(this, "onNewIntent: intent = " + intent);

        // We're being re-launched with a new Intent.  Since it's possible for a
        // single InCallActivity instance to persist indefinitely (even if we
        // finish() ourselves), this sequence can potentially happen any time
        // the InCallActivity needs to be displayed.

        // Stash away the new intent so that we can get it in the future
        // by calling getIntent().  (Otherwise getIntent() will return the
        // original Intent from when we first got created!)
        setIntent(intent);

        // Activities are always paused before receiving a new intent, so
        // we can count on our onResume() method being called next.

        // Just like in onCreate(), handle the intent.
        internalResolveIntent(intent);
    }

    @Override
    public void onBackPressed() {
        Log.d(this, "onBackPressed()...");

        // BACK is also used to exit out of any "special modes" of the
        // in-call UI:

        if (mDialpadFragment.isVisible()) {
            mCallButtonFragment.displayDialpad(false);  // do the "closing" animation
            return;
        } else if (mConferenceManagerFragment.isVisible()) {
            mConferenceManagerFragment.setVisible(false);
            return;
        }

        // Always disable the Back key while an incoming call is ringing
        final Call call = CallList.getInstance().getIncomingCall();
        if (call != null) {
            Log.d(this, "Consume Back press for an inconing call");
            return;
        }

        // Nothing special to do.  Fall back to the default behavior.
        super.onBackPressed();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // push input to the dialer.
        if ((mDialpadFragment.isVisible()) && (mDialpadFragment.onDialerKeyUp(event))){
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_CALL) {
            // Always consume CALL to be sure the PhoneWindow won't do anything with it
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_CALL:
                boolean handled = InCallPresenter.getInstance().handleCallKey();
                if (!handled) {
                    Log.w(this, "InCallActivity should always handle KEYCODE_CALL in onKeyDown");
                }
                // Always consume CALL to be sure the PhoneWindow won't do anything with it
                return true;

            // Note there's no KeyEvent.KEYCODE_ENDCALL case here.
            // The standard system-wide handling of the ENDCALL key
            // (see PhoneWindowManager's handling of KEYCODE_ENDCALL)
            // already implements exactly what the UI spec wants,
            // namely (1) "hang up" if there's a current active call,
            // or (2) "don't answer" if there's a current ringing call.

            case KeyEvent.KEYCODE_CAMERA:
                // Disable the CAMERA button while in-call since it's too
                // easy to press accidentally.
                return true;

            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_MUTE:
                // Ringer silencing handled by PhoneWindowManager.
                break;

            case KeyEvent.KEYCODE_MUTE:
                // toggle mute
                CallCommandClient.getInstance().mute(!AudioModeProvider.getInstance().getMute());
                return true;

            // Various testing/debugging features, enabled ONLY when VERBOSE == true.
            case KeyEvent.KEYCODE_SLASH:
                if (Log.VERBOSE) {
                    Log.v(this, "----------- InCallActivity View dump --------------");
                    // Dump starting from the top-level view of the entire activity:
                    Window w = this.getWindow();
                    View decorView = w.getDecorView();
                    decorView.debug();
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_EQUALS:
                // TODO: Dump phone state?
                break;
        }

        if (event.getRepeatCount() == 0 && handleDialerKeyDown(keyCode, event)) {
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    private boolean handleDialerKeyDown(int keyCode, KeyEvent event) {
        Log.v(this, "handleDialerKeyDown: keyCode " + keyCode + ", event " + event + "...");

        // As soon as the user starts typing valid dialable keys on the
        // keyboard (presumably to type DTMF tones) we start passing the
        // key events to the DTMFDialer's onDialerKeyDown.
        if (mDialpadFragment.isVisible()) {
            return mDialpadFragment.onDialerKeyDown(event);

            // TODO: If the dialpad isn't currently visible, maybe
            // consider automatically bringing it up right now?
            // (Just to make sure the user sees the digits widget...)
            // But this probably isn't too critical since it's awkward to
            // use the hard keyboard while in-call in the first place,
            // especially now that the in-call UI is portrait-only...
        }

        return false;
    }

    @Override
    public void onConfigurationChanged(Configuration config) {
        InCallPresenter.getInstance().getProximitySensor().onConfigurationChanged(config);
    }

    private void internalResolveIntent(Intent intent) {
        final String action = intent.getAction();

        if (action.equals(intent.ACTION_MAIN)) {
            // This action is the normal way to bring up the in-call UI.
            //
            // But we do check here for one extra that can come along with the
            // ACTION_MAIN intent:

            if (intent.hasExtra(SHOW_DIALPAD_EXTRA)) {
                // SHOW_DIALPAD_EXTRA can be used here to specify whether the DTMF
                // dialpad should be initially visible.  If the extra isn't
                // present at all, we just leave the dialpad in its previous state.

                final boolean showDialpad = intent.getBooleanExtra(SHOW_DIALPAD_EXTRA, false);
                Log.d(this, "- internalResolveIntent: SHOW_DIALPAD_EXTRA: " + showDialpad);

                relaunchedFromDialer(showDialpad);
            }

            return;
        }
    }

    private void relaunchedFromDialer(boolean showDialpad) {
        mShowDialpadRequested = showDialpad;

        if (mShowDialpadRequested) {
            // If there's only one line in use, AND it's on hold, then we're sure the user
            // wants to use the dialpad toward the exact line, so un-hold the holding line.
            final Call call = CallList.getInstance().getActiveOrBackgroundCall();
            if (call != null && call.getState() == State.ONHOLD) {
                CallCommandClient.getInstance().hold(call.getCallId(), false);
            }
        }
    }

    private void initializeInCall() {
        if (mCallButtonFragment == null) {
            mCallButtonFragment = (CallButtonFragment) getFragmentManager()
                    .findFragmentById(R.id.callButtonFragment);
            mCallButtonFragment.getView().setVisibility(View.INVISIBLE);
        }

        if (mCallCardFragment == null) {
            mCallCardFragment = (CallCardFragment) getFragmentManager()
                    .findFragmentById(R.id.callCardFragment);
        }

        if (mAnswerFragment == null) {
            mAnswerFragment = (AnswerFragment) getFragmentManager()
                    .findFragmentById(R.id.answerFragment);
        }

        if (mDialpadFragment == null) {
            mDialpadFragment = (DialpadFragment) getFragmentManager()
                    .findFragmentById(R.id.dialpadFragment);
            mDialpadFragment.getView().setVisibility(View.INVISIBLE);
        }

        if (mConferenceManagerFragment == null) {
            mConferenceManagerFragment = (ConferenceManagerFragment) getFragmentManager()
                    .findFragmentById(R.id.conferenceManagerFragment);
            mConferenceManagerFragment.getView().setVisibility(View.INVISIBLE);
        }
    }

    private void toast(String text) {
        final Toast toast = Toast.makeText(this, text, Toast.LENGTH_SHORT);

        toast.show();
    }

    /**
     * Simulates a user click to hide the dialpad. This will update the UI to show the call card,
     * update the checked state of the dialpad button, and update the proximity sensor state.
     */
    public void hideDialpadForDisconnect() {
        mCallButtonFragment.displayDialpad(false);
    }

    public void dismissKeyguard(boolean dismiss) {
        if (dismiss) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        }
    }

    public void displayDialpad(boolean showDialpad) {
        if (showDialpad) {
            mDialpadFragment.setVisible(true);
            mCallCardFragment.setVisible(false);
        } else {
            mDialpadFragment.setVisible(false);
            mCallCardFragment.setVisible(true);
        }

        InCallPresenter.getInstance().getProximitySensor().onDialpadVisible(showDialpad);
    }

    public boolean isDialpadVisible() {
        return mDialpadFragment.isVisible();
    }

    public void displayManageConferencePanel(boolean showPanel) {
        if (showPanel) {
            mConferenceManagerFragment.setVisible(true);
        }
    }

    public void showPostCharWaitDialog(int callId, String chars) {
        final PostCharDialogFragment fragment = new PostCharDialogFragment(callId,  chars);
        fragment.show(getFragmentManager(), "postCharWait");
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        if (mCallCardFragment != null) {
            mCallCardFragment.dispatchPopulateAccessibilityEvent(event);
        }
        return super.dispatchPopulateAccessibilityEvent(event);
    }

    public void maybeShowErrorDialogOnDisconnect(Call.DisconnectCause cause) {
        Log.d(this, "maybeShowErrorDialogOnDisconnect");

        if (!isFinishing()) {
            final int resId = getResIdForDisconnectCause(cause);
            if (resId != INVALID_RES_ID) {
                showErrorDialog(resId);
            }
        }
    }

    public void dismissPendingDialogs() {
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
        mAnswerFragment.dismissPendingDialogues();
    }

    /**
     * Utility function to bring up a generic "error" dialog.
     */
    private void showErrorDialog(int resId) {
        final CharSequence msg = getResources().getText(resId);
        Log.i(this, "Show Dialog: " + msg);

        dismissPendingDialogs();

        mDialog = new AlertDialog.Builder(this)
            .setMessage(msg)
            .setPositiveButton(R.string.ok, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    onDialogDismissed();
                }})
            .setOnCancelListener(new OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    onDialogDismissed();
                }})
            .create();

        mDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        mDialog.show();
    }

    private int getResIdForDisconnectCause(Call.DisconnectCause cause) {
        int resId = INVALID_RES_ID;

        if (cause == Call.DisconnectCause.CALL_BARRED) {
            resId = R.string.callFailed_cb_enabled;
        } else if (cause == Call.DisconnectCause.FDN_BLOCKED) {
            resId = R.string.callFailed_fdn_only;
        } else if (cause == Call.DisconnectCause.CS_RESTRICTED) {
            resId = R.string.callFailed_dsac_restricted;
        } else if (cause == Call.DisconnectCause.CS_RESTRICTED_EMERGENCY) {
            resId = R.string.callFailed_dsac_restricted_emergency;
        } else if (cause == Call.DisconnectCause.CS_RESTRICTED_NORMAL) {
            resId = R.string.callFailed_dsac_restricted_normal;
        }

        return resId;
    }

    private void onDialogDismissed() {
        mDialog = null;
        InCallPresenter.getInstance().onDismissDialog();
    }
}
