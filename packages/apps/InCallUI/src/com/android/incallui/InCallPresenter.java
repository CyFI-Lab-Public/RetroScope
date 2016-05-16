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

package com.android.incallui;

import com.android.incallui.service.PhoneNumberService;
import com.google.android.collect.Sets;
import com.google.common.base.Preconditions;

import android.content.Context;
import android.content.Intent;

import com.android.services.telephony.common.Call;
import com.android.services.telephony.common.Call.Capabilities;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Set;

/**
 * Takes updates from the CallList and notifies the InCallActivity (UI)
 * of the changes.
 * Responsible for starting the activity for a new call and finishing the activity when all calls
 * are disconnected.
 * Creates and manages the in-call state and provides a listener pattern for the presenters
 * that want to listen in on the in-call state changes.
 * TODO: This class has become more of a state machine at this point.  Consider renaming.
 */
public class InCallPresenter implements CallList.Listener {

    private static InCallPresenter sInCallPresenter;

    private final Set<InCallStateListener> mListeners = Sets.newHashSet();
    private final ArrayList<IncomingCallListener> mIncomingCallListeners = Lists.newArrayList();

    private AudioModeProvider mAudioModeProvider;
    private StatusBarNotifier mStatusBarNotifier;
    private ContactInfoCache mContactInfoCache;
    private Context mContext;
    private CallList mCallList;
    private InCallActivity mInCallActivity;
    private InCallState mInCallState = InCallState.NO_CALLS;
    private ProximitySensor mProximitySensor;
    private boolean mServiceConnected = false;

    /**
     * Is true when the activity has been previously started. Some code needs to know not just if
     * the activity is currently up, but if it had been previously shown in foreground for this
     * in-call session (e.g., StatusBarNotifier). This gets reset when the session ends in the
     * tear-down method.
     */
    private boolean mIsActivityPreviouslyStarted = false;

    public static synchronized InCallPresenter getInstance() {
        if (sInCallPresenter == null) {
            sInCallPresenter = new InCallPresenter();
        }
        return sInCallPresenter;
    }

    public InCallState getInCallState() {
        return mInCallState;
    }

    public CallList getCallList() {
        return mCallList;
    }

    public void setUp(Context context, CallList callList, AudioModeProvider audioModeProvider) {
        if (mServiceConnected) {
            Log.i(this, "New service connection replacing existing one.");
            // retain the current resources, no need to create new ones.
            Preconditions.checkState(context == mContext);
            Preconditions.checkState(callList == mCallList);
            Preconditions.checkState(audioModeProvider == mAudioModeProvider);
            return;
        }

        Preconditions.checkNotNull(context);
        mContext = context;

        mContactInfoCache = ContactInfoCache.getInstance(context);

        mStatusBarNotifier = new StatusBarNotifier(context, mContactInfoCache);
        addListener(mStatusBarNotifier);

        mAudioModeProvider = audioModeProvider;

        mProximitySensor = new ProximitySensor(context, mAudioModeProvider);
        addListener(mProximitySensor);

        mCallList = callList;

        // This only gets called by the service so this is okay.
        mServiceConnected = true;

        // The final thing we do in this set up is add ourselves as a listener to CallList.  This
        // will kick off an update and the whole process can start.
        mCallList.addListener(this);

        Log.d(this, "Finished InCallPresenter.setUp");
    }

    /**
     * Called when the telephony service has disconnected from us.  This will happen when there are
     * no more active calls. However, we may still want to continue showing the UI for
     * certain cases like showing "Call Ended".
     * What we really want is to wait for the activity and the service to both disconnect before we
     * tear things down. This method sets a serviceConnected boolean and calls a secondary method
     * that performs the aforementioned logic.
     */
    public void tearDown() {
        Log.d(this, "tearDown");
        mServiceConnected = false;
        attemptCleanup();
    }

    private void attemptFinishActivity() {
        final boolean doFinish = (mInCallActivity != null && isActivityStarted());
        Log.i(this, "Hide in call UI: " + doFinish);

        if (doFinish) {
            mInCallActivity.finish();
        }
    }

    /**
     * Called when the UI begins or ends. Starts the callstate callbacks if the UI just began.
     * Attempts to tear down everything if the UI just ended. See #tearDown for more insight on
     * the tear-down process.
     */
    public void setActivity(InCallActivity inCallActivity) {
        boolean updateListeners = false;
        boolean doAttemptCleanup = false;

        if (inCallActivity != null) {
            if (mInCallActivity == null) {
                updateListeners = true;
                Log.i(this, "UI Initialized");
            } else if (mInCallActivity != inCallActivity) {
                Log.wtf(this, "Setting a second activity before destroying the first.");
            } else {
                // since setActivity is called onStart(), it can be called multiple times.
                // This is fine and ignorable, but we do not want to update the world every time
                // this happens (like going to/from background) so we do not set updateListeners.
            }

            mInCallActivity = inCallActivity;

            // By the time the UI finally comes up, the call may already be disconnected.
            // If that's the case, we may need to show an error dialog.
            if (mCallList != null && mCallList.getDisconnectedCall() != null) {
                maybeShowErrorDialogOnDisconnect(mCallList.getDisconnectedCall());
            }

            // When the UI comes up, we need to first check the in-call state.
            // If we are showing NO_CALLS, that means that a call probably connected and
            // then immediately disconnected before the UI was able to come up.
            // If we dont have any calls, start tearing down the UI instead.
            // NOTE: This code relies on {@link #mInCallActivity} being set so we run it after
            // it has been set.
            if (mInCallState == InCallState.NO_CALLS) {
                Log.i(this, "UI Intialized, but no calls left.  shut down.");
                attemptFinishActivity();
                return;
            }
        } else {
            Log.i(this, "UI Destroyed)");
            updateListeners = true;
            mInCallActivity = null;

            // We attempt cleanup for the destroy case but only after we recalculate the state
            // to see if we need to come back up or stay shut down. This is why we do the cleanup
            // after the call to onCallListChange() instead of directly here.
            doAttemptCleanup = true;
        }

        // Messages can come from the telephony layer while the activity is coming up
        // and while the activity is going down.  So in both cases we need to recalculate what
        // state we should be in after they complete.
        // Examples: (1) A new incoming call could come in and then get disconnected before
        //               the activity is created.
        //           (2) All calls could disconnect and then get a new incoming call before the
        //               activity is destroyed.
        //
        // b/1122139 - We previously had a check for mServiceConnected here as well, but there are
        // cases where we need to recalculate the current state even if the service in not
        // connected.  In particular the case where startOrFinish() is called while the app is
        // already finish()ing. In that case, we skip updating the state with the knowledge that
        // we will check again once the activity has finished. That means we have to recalculate the
        // state here even if the service is disconnected since we may not have finished a state
        // transition while finish()ing.
        if (updateListeners) {
            onCallListChange(mCallList);
        }

        if (doAttemptCleanup) {
            attemptCleanup();
        }
    }

    /**
     * Called when there is a change to the call list.
     * Sets the In-Call state for the entire in-call app based on the information it gets from
     * CallList. Dispatches the in-call state to all listeners. Can trigger the creation or
     * destruction of the UI based on the states that is calculates.
     */
    @Override
    public void onCallListChange(CallList callList) {
        if (callList == null) {
            return;
        }
        InCallState newState = getPotentialStateFromCallList(callList);
        newState = startOrFinishUi(newState);

        // Renable notification shade and soft navigation buttons, if we are no longer in the
        // incoming call screen
        if (!newState.isIncoming()) {
            CallCommandClient.getInstance().setSystemBarNavigationEnabled(true);
        }

        // Set the new state before announcing it to the world
        Log.i(this, "Phone switching state: " + mInCallState + " -> " + newState);
        mInCallState = newState;

        // notify listeners of new state
        for (InCallStateListener listener : mListeners) {
            Log.d(this, "Notify " + listener + " of state " + mInCallState.toString());
            listener.onStateChange(mInCallState, callList);
        }

        if (isActivityStarted()) {
            final boolean hasCall = callList.getActiveOrBackgroundCall() != null ||
                    callList.getOutgoingCall() != null;
            mInCallActivity.dismissKeyguard(hasCall);
        }
    }

    /**
     * Called when there is a new incoming call.
     *
     * @param call
     */
    @Override
    public void onIncomingCall(Call call) {
        InCallState newState = startOrFinishUi(InCallState.INCOMING);

        Log.i(this, "Phone switching state: " + mInCallState + " -> " + newState);
        mInCallState = newState;

        // Disable notification shade and soft navigation buttons
        if (newState.isIncoming()) {
            CallCommandClient.getInstance().setSystemBarNavigationEnabled(false);
        }

        for (IncomingCallListener listener : mIncomingCallListeners) {
            listener.onIncomingCall(mInCallState, call);
        }
    }

    /**
     * Called when a call becomes disconnected. Called everytime an existing call
     * changes from being connected (incoming/outgoing/active) to disconnected.
     */
    @Override
    public void onDisconnect(Call call) {
        hideDialpadForDisconnect();
        maybeShowErrorDialogOnDisconnect(call);

        // We need to do the run the same code as onCallListChange.
        onCallListChange(CallList.getInstance());

        if (isActivityStarted()) {
            mInCallActivity.dismissKeyguard(false);
        }
    }

    /**
     * Given the call list, return the state in which the in-call screen should be.
     */
    public static InCallState getPotentialStateFromCallList(CallList callList) {

        InCallState newState = InCallState.NO_CALLS;

        if (callList == null) {
            return newState;
        }
        if (callList.getIncomingCall() != null) {
            newState = InCallState.INCOMING;
        } else if (callList.getOutgoingCall() != null) {
            newState = InCallState.OUTGOING;
        } else if (callList.getActiveCall() != null ||
                callList.getBackgroundCall() != null ||
                callList.getDisconnectedCall() != null ||
                callList.getDisconnectingCall() != null) {
            newState = InCallState.INCALL;
        }

        return newState;
    }

    public void addIncomingCallListener(IncomingCallListener listener) {
        Preconditions.checkNotNull(listener);
        mIncomingCallListeners.add(listener);
    }

    public void removeIncomingCallListener(IncomingCallListener listener) {
        Preconditions.checkNotNull(listener);
        mIncomingCallListeners.remove(listener);
    }

    public void addListener(InCallStateListener listener) {
        Preconditions.checkNotNull(listener);
        mListeners.add(listener);
    }

    public void removeListener(InCallStateListener listener) {
        Preconditions.checkNotNull(listener);
        mListeners.remove(listener);
    }

    public AudioModeProvider getAudioModeProvider() {
        return mAudioModeProvider;
    }

    public ContactInfoCache getContactInfoCache() {
        return mContactInfoCache;
    }

    public ProximitySensor getProximitySensor() {
        return mProximitySensor;
    }

    /**
     * Hangs up any active or outgoing calls.
     */
    public void hangUpOngoingCall(Context context) {
        // By the time we receive this intent, we could be shut down and call list
        // could be null.  Bail in those cases.
        if (mCallList == null) {
            if (mStatusBarNotifier == null) {
                // The In Call UI has crashed but the notification still stayed up. We should not
                // come to this stage.
                StatusBarNotifier.clearInCallNotification(context);
            }
            return;
        }

        Call call = mCallList.getOutgoingCall();
        if (call == null) {
            call = mCallList.getActiveOrBackgroundCall();
        }

        if (call != null) {
            CallCommandClient.getInstance().disconnectCall(call.getCallId());
        }
    }

    /**
     * Returns true if the incall app is the foreground application.
     */
    public boolean isShowingInCallUi() {
        return (isActivityStarted() && mInCallActivity.isForegroundActivity());
    }

    /**
     * Returns true of the activity has been created and is running.
     * Returns true as long as activity is not destroyed or finishing.  This ensures that we return
     * true even if the activity is paused (not in foreground).
     */
    public boolean isActivityStarted() {
        return (mInCallActivity != null &&
                !mInCallActivity.isDestroyed() &&
                !mInCallActivity.isFinishing());
    }

    public boolean isActivityPreviouslyStarted() {
        return mIsActivityPreviouslyStarted;
    }

    /**
     * Called when the activity goes in/out of the foreground.
     */
    public void onUiShowing(boolean showing) {
        // We need to update the notification bar when we leave the UI because that
        // could trigger it to show again.
        if (mStatusBarNotifier != null) {
            mStatusBarNotifier.updateNotification(mInCallState, mCallList);
        }

        if (mProximitySensor != null) {
            mProximitySensor.onInCallShowing(showing);
        }

        if (showing) {
            mIsActivityPreviouslyStarted = true;
        }
    }

    /**
     * Brings the app into the foreground if possible.
     */
    public void bringToForeground(boolean showDialpad) {
        // Before we bring the incall UI to the foreground, we check to see if:
        // 1. We've already started the activity once for this session
        // 2. If it exists, the activity is not already in the foreground
        // 3. We are in a state where we want to show the incall ui
        if (mIsActivityPreviouslyStarted && !isShowingInCallUi() &&
                mInCallState != InCallState.NO_CALLS) {
            showInCall(showDialpad);
        }
    }

    public void onPostDialCharWait(int callId, String chars) {
        mInCallActivity.showPostCharWaitDialog(callId, chars);
    }

    /**
     * Handles the green CALL key while in-call.
     * @return true if we consumed the event.
     */
    public boolean handleCallKey() {
        Log.v(this, "handleCallKey");

        // The green CALL button means either "Answer", "Unhold", or
        // "Swap calls", or can be a no-op, depending on the current state
        // of the Phone.

        /**
         * INCOMING CALL
         */
        final CallList calls = CallList.getInstance();
        final Call incomingCall = calls.getIncomingCall();
        Log.v(this, "incomingCall: " + incomingCall);

        // (1) Attempt to answer a call
        if (incomingCall != null) {
            CallCommandClient.getInstance().answerCall(incomingCall.getCallId());
            return true;
        }

        /**
         * ACTIVE CALL
         */
        final Call activeCall = calls.getActiveCall();
        if (activeCall != null) {
            // TODO: This logic is repeated from CallButtonPresenter.java. We should
            // consolidate this logic.
            final boolean isGeneric = activeCall.can(Capabilities.GENERIC_CONFERENCE);
            final boolean canMerge = activeCall.can(Capabilities.MERGE_CALLS);
            final boolean canSwap = activeCall.can(Capabilities.SWAP_CALLS);

            Log.v(this, "activeCall: " + activeCall + ", isGeneric: " + isGeneric + ", canMerge: " +
                    canMerge + ", canSwap: " + canSwap);

            // (2) Attempt actions on Generic conference calls
            if (activeCall.isConferenceCall() && isGeneric) {
                if (canMerge) {
                    CallCommandClient.getInstance().merge();
                    return true;
                } else if (canSwap) {
                    CallCommandClient.getInstance().swap();
                    return true;
                }
            }

            // (3) Swap calls
            if (canSwap) {
                CallCommandClient.getInstance().swap();
                return true;
            }
        }

        /**
         * BACKGROUND CALL
         */
        final Call heldCall = calls.getBackgroundCall();
        if (heldCall != null) {
            // We have a hold call so presumeable it will always support HOLD...but
            // there is no harm in double checking.
            final boolean canHold = heldCall.can(Capabilities.HOLD);

            Log.v(this, "heldCall: " + heldCall + ", canHold: " + canHold);

            // (4) unhold call
            if (heldCall.getState() == Call.State.ONHOLD && canHold) {
                CallCommandClient.getInstance().hold(heldCall.getCallId(), false);
                return true;
            }
        }

        // Always consume hard keys
        return true;
    }

    /**
     * A dialog could have prevented in-call screen from being previously finished.
     * This function checks to see if there should be any UI left and if not attempts
     * to tear down the UI.
     */
    public void onDismissDialog() {
        Log.i(this, "Dialog dismissed");
        if (mInCallState == InCallState.NO_CALLS) {
            attemptFinishActivity();
            attemptCleanup();
        }
    }

    /**
     * For some disconnected causes, we show a dialog.  This calls into the activity to show
     * the dialog if appropriate for the call.
     */
    private void maybeShowErrorDialogOnDisconnect(Call call) {
        // For newly disconnected calls, we may want to show a dialog on specific error conditions
        if (isActivityStarted() && call.getState() == Call.State.DISCONNECTED) {
            mInCallActivity.maybeShowErrorDialogOnDisconnect(call.getDisconnectCause());
        }
    }

    /**
     * Hides the dialpad.  Called when a call is disconnected (Requires hiding dialpad).
     */
    private void hideDialpadForDisconnect() {
        if (isActivityStarted()) {
            mInCallActivity.hideDialpadForDisconnect();
        }
    }

    /**
     * When the state of in-call changes, this is the first method to get called. It determines if
     * the UI needs to be started or finished depending on the new state and does it.
     */
    private InCallState startOrFinishUi(InCallState newState) {
        Log.d(this, "startOrFinishUi: " + mInCallState + " -> " + newState);

        // TODO: Consider a proper state machine implementation

        // If the state isn't changing, we have already done any starting/stopping of
        // activities in a previous pass...so lets cut out early
        if (newState == mInCallState) {
            return newState;
        }

        // A new Incoming call means that the user needs to be notified of the the call (since
        // it wasn't them who initiated it).  We do this through full screen notifications and
        // happens indirectly through {@link StatusBarListener}.
        //
        // The process for incoming calls is as follows:
        //
        // 1) CallList          - Announces existence of new INCOMING call
        // 2) InCallPresenter   - Gets announcement and calculates that the new InCallState
        //                      - should be set to INCOMING.
        // 3) InCallPresenter   - This method is called to see if we need to start or finish
        //                        the app given the new state.
        // 4) StatusBarNotifier - Listens to InCallState changes. InCallPresenter calls
        //                        StatusBarNotifier explicitly to issue a FullScreen Notification
        //                        that will either start the InCallActivity or show the user a
        //                        top-level notification dialog if the user is in an immersive app.
        //                        That notification can also start the InCallActivity.
        // 5) InCallActivity    - Main activity starts up and at the end of its onCreate will
        //                        call InCallPresenter::setActivity() to let the presenter
        //                        know that start-up is complete.
        //
        //          [ AND NOW YOU'RE IN THE CALL. voila! ]
        //
        // Our app is started using a fullScreen notification.  We need to do this whenever
        // we get an incoming call.
        final boolean startStartupSequence = (InCallState.INCOMING == newState);

        // A new outgoing call indicates that the user just now dialed a number and when that
        // happens we need to display the screen immediateley.
        //
        // This is different from the incoming call sequence because we do not need to shock the
        // user with a top-level notification.  Just show the call UI normally.
        final boolean showCallUi = (InCallState.OUTGOING == newState);

        // TODO: Can we be suddenly in a call without it having been in the outgoing or incoming
        // state?  I havent seen that but if it can happen, the code below should be enabled.
        // showCallUi |= (InCallState.INCALL && !isActivityStarted());

        // The only time that we have an instance of mInCallActivity and it isn't started is
        // when it is being destroyed.  In that case, lets avoid bringing up another instance of
        // the activity.  When it is finally destroyed, we double check if we should bring it back
        // up so we aren't going to lose anything by avoiding a second startup here.
        boolean activityIsFinishing = mInCallActivity != null && !isActivityStarted();
        if (activityIsFinishing) {
            Log.i(this, "Undo the state change: " + newState + " -> " + mInCallState);
            return mInCallState;
        }

        if (showCallUi) {
            Log.i(this, "Start in call UI");
            showInCall(false);
        } else if (startStartupSequence) {
            Log.i(this, "Start Full Screen in call UI");

            // We're about the bring up the in-call UI for an incoming call. If we still have
            // dialogs up, we need to clear them out before showing incoming screen.
            if (isActivityStarted()) {
                mInCallActivity.dismissPendingDialogs();
            }
            startUi(newState);
        } else if (newState == InCallState.NO_CALLS) {
            // The new state is the no calls state.  Tear everything down.
            attemptFinishActivity();
            attemptCleanup();
        }

        return newState;
    }

    private void startUi(InCallState inCallState) {
        final Call incomingCall = mCallList.getIncomingCall();
        final boolean isCallWaiting = (incomingCall != null &&
                incomingCall.getState() == Call.State.CALL_WAITING);

        // If the screen is off, we need to make sure it gets turned on for incoming calls.
        // This normally works just fine thanks to FLAG_TURN_SCREEN_ON but that only works
        // when the activity is first created. Therefore, to ensure the screen is turned on
        // for the call waiting case, we finish() the current activity and start a new one.
        // There should be no jank from this since the screen is already off and will remain so
        // until our new activity is up.
        if (mProximitySensor.isScreenReallyOff() && isCallWaiting) {
            if (isActivityStarted()) {
                mInCallActivity.finish();
            }
            mInCallActivity = null;
        }

        mStatusBarNotifier.updateNotificationAndLaunchIncomingCallUi(inCallState, mCallList);
    }

    /**
     * Checks to see if both the UI is gone and the service is disconnected. If so, tear it all
     * down.
     */
    private void attemptCleanup() {
        boolean shouldCleanup = (mInCallActivity == null && !mServiceConnected &&
                mInCallState == InCallState.NO_CALLS);
        Log.i(this, "attemptCleanup? " + shouldCleanup);

        if (shouldCleanup) {
            mIsActivityPreviouslyStarted = false;

            // blow away stale contact info so that we get fresh data on
            // the next set of calls
            if (mContactInfoCache != null) {
                mContactInfoCache.clearCache();
            }
            mContactInfoCache = null;

            if (mProximitySensor != null) {
                removeListener(mProximitySensor);
                mProximitySensor.tearDown();
            }
            mProximitySensor = null;

            mAudioModeProvider = null;

            if (mStatusBarNotifier != null) {
                removeListener(mStatusBarNotifier);
            }
            mStatusBarNotifier = null;

            if (mCallList != null) {
                mCallList.removeListener(this);
            }
            mCallList = null;

            mContext = null;
            mInCallActivity = null;

            mListeners.clear();
            mIncomingCallListeners.clear();

            Log.d(this, "Finished InCallPresenter.CleanUp");
        }
    }

    private void showInCall(boolean showDialpad) {
        mContext.startActivity(getInCallIntent(showDialpad));
    }

    public Intent getInCallIntent(boolean showDialpad) {
        final Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        intent.setClass(mContext, InCallActivity.class);
        if (showDialpad) {
            intent.putExtra(InCallActivity.SHOW_DIALPAD_EXTRA, true);
        }

        return intent;
    }

    /**
     * Private constructor. Must use getInstance() to get this singleton.
     */
    private InCallPresenter() {
    }

    /**
     * All the main states of InCallActivity.
     */
    public enum InCallState {
        // InCall Screen is off and there are no calls
        NO_CALLS,

        // Incoming-call screen is up
        INCOMING,

        // In-call experience is showing
        INCALL,

        // User is dialing out
        OUTGOING;

        public boolean isIncoming() {
            return (this == INCOMING);
        }

        public boolean isConnectingOrConnected() {
            return (this == INCOMING ||
                    this == OUTGOING ||
                    this == INCALL);
        }
    }

    /**
     * Interface implemented by classes that need to know about the InCall State.
     */
    public interface InCallStateListener {
        // TODO: Enhance state to contain the call objects instead of passing CallList
        public void onStateChange(InCallState state, CallList callList);
    }

    public interface IncomingCallListener {
        public void onIncomingCall(InCallState state, Call call);
    }
}
