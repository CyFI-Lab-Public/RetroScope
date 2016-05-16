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

import com.google.common.base.Preconditions;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import com.android.incallui.ContactInfoCache.ContactCacheEntry;
import com.android.incallui.ContactInfoCache.ContactInfoCacheCallback;
import com.android.incallui.InCallApp.NotificationBroadcastReceiver;
import com.android.incallui.InCallPresenter.InCallState;
import com.android.services.telephony.common.Call;

/**
 * This class adds Notifications to the status bar for the in-call experience.
 */
public class StatusBarNotifier implements InCallPresenter.InCallStateListener {
    // notification types
    private static final int IN_CALL_NOTIFICATION = 1;

    private static final long IN_CALL_TIMEOUT = 1000L;

    private interface NotificationTimer {
        enum State {
            SCHEDULED,
            FIRED,
            CLEAR;
        }
        State getState();
        void schedule();
        void clear();
    }

    private NotificationTimer mNotificationTimer = new NotificationTimer() {
        private final Handler mHandler = new Handler(new Handler.Callback() {
            public boolean handleMessage(Message m) {
                fire();
                return true;
            }
        });
        private State mState = State.CLEAR;
        public State getState() { return mState; }
        public void schedule() {
            if (mState == State.CLEAR) {
                Log.d(this, "updateInCallNotification: timer scheduled");
                mHandler.sendEmptyMessageDelayed(0, IN_CALL_TIMEOUT);
                mState = State.SCHEDULED;
            }
        }
        public void clear() {
            Log.d(this, "updateInCallNotification: timer cleared");
            mHandler.removeMessages(0);
            mState = State.CLEAR;
        }
        private void fire() {
            Log.d(this, "updateInCallNotification: timer fired");
            mState = State.FIRED;
            updateNotification(
                    InCallPresenter.getInstance().getInCallState(),
                    InCallPresenter.getInstance().getCallList());
        }
    };

    private final Context mContext;
    private final ContactInfoCache mContactInfoCache;
    private final NotificationManager mNotificationManager;
    private boolean mIsShowingNotification = false;
    private int mCallState = Call.State.INVALID;
    private int mSavedIcon = 0;
    private int mSavedContent = 0;
    private Bitmap mSavedLargeIcon;
    private String mSavedContentTitle;

    public StatusBarNotifier(Context context, ContactInfoCache contactInfoCache) {
        Preconditions.checkNotNull(context);

        mContext = context;
        mContactInfoCache = contactInfoCache;
        mNotificationManager =
                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    /**
     * Creates notifications according to the state we receive from {@link InCallPresenter}.
     */
    @Override
    public void onStateChange(InCallState state, CallList callList) {
        Log.d(this, "onStateChange");

        updateNotification(state, callList);
    }

    /**
     * Updates the phone app's status bar notification based on the
     * current telephony state, or cancels the notification if the phone
     * is totally idle.
     *
     * This method will never actually launch the incoming-call UI.
     * (Use updateNotificationAndLaunchIncomingCallUi() for that.)
     */
    public void updateNotification(InCallState state, CallList callList) {
        Log.d(this, "updateNotification");
        // allowFullScreenIntent=false means *don't* allow the incoming
        // call UI to be launched.
        updateInCallNotification(false, state, callList);
    }

    /**
     * Updates the phone app's status bar notification *and* launches the
     * incoming call UI in response to a new incoming call.
     *
     * This is just like updateInCallNotification(), with one exception:
     * If an incoming call is ringing (or call-waiting), the notification
     * will also include a "fullScreenIntent" that will cause the
     * InCallScreen to be launched immediately, unless the current
     * foreground activity is marked as "immersive".
     *
     * (This is the mechanism that actually brings up the incoming call UI
     * when we receive a "new ringing connection" event from the telephony
     * layer.)
     *
     * Watch out: this method should ONLY be called directly from the code
     * path in CallNotifier that handles the "new ringing connection"
     * event from the telephony layer.  All other places that update the
     * in-call notification (like for phone state changes) should call
     * updateInCallNotification() instead.  (This ensures that we don't
     * end up launching the InCallScreen multiple times for a single
     * incoming call, which could cause slow responsiveness and/or visible
     * glitches.)
     *
     * Also note that this method is safe to call even if the phone isn't
     * actually ringing (or, more likely, if an incoming call *was*
     * ringing briefly but then disconnected).  In that case, we'll simply
     * update or cancel the in-call notification based on the current
     * phone state.
     *
     * @see #updateInCallNotification(boolean,InCallState,CallList)
     */
    public void updateNotificationAndLaunchIncomingCallUi(InCallState state, CallList callList) {
        // Set allowFullScreenIntent=true to indicate that we *should*
        // launch the incoming call UI if necessary.
        updateInCallNotification(true, state, callList);
    }

    /**
     * Take down the in-call notification.
     * @see #updateInCallNotification(boolean,InCallState,CallList)
     */
    private void cancelInCall() {
        Log.d(this, "cancelInCall()...");
        mNotificationManager.cancel(IN_CALL_NOTIFICATION);
        mIsShowingNotification = false;
    }

    /* package */ static void clearInCallNotification(Context backupContext) {
        Log.i(StatusBarNotifier.class.getSimpleName(),
                "Something terrible happened. Clear all InCall notifications");

        NotificationManager notificationManager =
                (NotificationManager) backupContext.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(IN_CALL_NOTIFICATION);
    }

    /**
     * Helper method for updateInCallNotification() and
     * updateNotificationAndLaunchIncomingCallUi(): Update the phone app's
     * status bar notification based on the current telephony state, or
     * cancels the notification if the phone is totally idle.
     *
     * @param allowFullScreenIntent If true, *and* an incoming call is
     *   ringing, the notification will include a "fullScreenIntent"
     *   pointing at the InCallActivity (which will cause the InCallActivity
     *   to be launched.)
     *   Watch out: This should be set to true *only* when directly
     *   handling a new incoming call for the first time.
     */
    private void updateInCallNotification(final boolean allowFullScreenIntent,
            final InCallState state, CallList callList) {
        Log.d(this, "updateInCallNotification(allowFullScreenIntent = "
                + allowFullScreenIntent + ")...");

        Call call = getCallToShow(callList);

        // Whether we have an outgoing call but the incall UI has yet to show up.
        // Since we don't normally show a notification while the incall screen is
        // in the foreground, if we show the outgoing notification before the activity
        // comes up the user will see it flash on and off on an outgoing call. We therefore
        // do not show the notification for outgoing calls before the activity has started.
        boolean isOutgoingWithoutIncallUi =
                state == InCallState.OUTGOING &&
                !InCallPresenter.getInstance().isActivityPreviouslyStarted();

        // Whether to show a notification immediately.
        boolean showNotificationNow =

                // We can still be in the INCALL state when a call is disconnected (in order to show
                // the "Call ended" screen. So check that we have an active connection too.
                (call != null) &&

                // We show a notification iff there is an active call.
                state.isConnectingOrConnected() &&

                // If the UI is already showing, then for most cases we do not want to show
                // a notification since that would be redundant, unless it is an incoming call,
                // in which case the notification is actually an important alert.
                (!InCallPresenter.getInstance().isShowingInCallUi() || state.isIncoming()) &&

                // If we have an outgoing call with no UI but the timer has fired, we show
                // a notification anyway.
                (!isOutgoingWithoutIncallUi ||
                        mNotificationTimer.getState() == NotificationTimer.State.FIRED);

        if (showNotificationNow) {
            showNotification(call, allowFullScreenIntent);
        } else {
            cancelInCall();
            if (isOutgoingWithoutIncallUi &&
                    mNotificationTimer.getState() == NotificationTimer.State.CLEAR) {
                mNotificationTimer.schedule();
            }
        }

        // If we see a UI, or we are done with calls for now, reset to ground state.
        if (InCallPresenter.getInstance().isShowingInCallUi() || call == null) {
            mNotificationTimer.clear();
        }
    }

    private void showNotification(final Call call, final boolean allowFullScreenIntent) {
        final boolean isIncoming = (call.getState() == Call.State.INCOMING ||
                call.getState() == Call.State.CALL_WAITING);

        // we make a call to the contact info cache to query for supplemental data to what the
        // call provides.  This includes the contact name and photo.
        // This callback will always get called immediately and synchronously with whatever data
        // it has available, and may make a subsequent call later (same thread) if it had to
        // call into the contacts provider for more data.
        mContactInfoCache.findInfo(call.getIdentification(), isIncoming,
                new ContactInfoCacheCallback() {
                    private boolean mAllowFullScreenIntent = allowFullScreenIntent;

                    @Override
                    public void onContactInfoComplete(int callId, ContactCacheEntry entry) {
                        Call call = CallList.getInstance().getCall(callId);
                        if (call != null) {
                            buildAndSendNotification(call, entry, mAllowFullScreenIntent);
                        }

                        // Full screen intents are what bring up the in call screen. We only want
                        // to do this the first time we are called back.
                        mAllowFullScreenIntent = false;
                    }

                    @Override
                    public void onImageLoadComplete(int callId, ContactCacheEntry entry) {
                        Call call = CallList.getInstance().getCall(callId);
                        if (call != null) {
                            buildAndSendNotification(call, entry, mAllowFullScreenIntent);
                        }
                    } });
    }

    /**
     * Sets up the main Ui for the notification
     */
    private void buildAndSendNotification(Call originalCall, ContactCacheEntry contactInfo,
            boolean allowFullScreenIntent) {

        // This can get called to update an existing notification after contact information has come
        // back. However, it can happen much later. Before we continue, we need to make sure that
        // the call being passed in is still the one we want to show in the notification.
        final Call call = getCallToShow(CallList.getInstance());
        if (call == null || call.getCallId() != originalCall.getCallId()) {
            return;
        }

        final int state = call.getState();
        final boolean isConference = call.isConferenceCall();
        final int iconResId = getIconToDisplay(call);
        final Bitmap largeIcon = getLargeIconToDisplay(contactInfo, isConference);
        final int contentResId = getContentString(call);
        final String contentTitle = getContentTitle(contactInfo, isConference);

        // If we checked and found that nothing is different, dont issue another notification.
        if (!checkForChangeAndSaveData(iconResId, contentResId, largeIcon, contentTitle, state,
                allowFullScreenIntent)) {
            return;
        }

        /*
         * Nothing more to check...build and send it.
         */
        final Notification.Builder builder = getNotificationBuilder();

        // Set up the main intent to send the user to the in-call screen
        final PendingIntent inCallPendingIntent = createLaunchPendingIntent();
        builder.setContentIntent(inCallPendingIntent);

        // Set the intent as a full screen intent as well if requested
        if (allowFullScreenIntent) {
            configureFullScreenIntent(builder, inCallPendingIntent, call);
        }

        // set the content
        builder.setContentText(mContext.getString(contentResId));
        builder.setSmallIcon(iconResId);
        builder.setContentTitle(contentTitle);
        builder.setLargeIcon(largeIcon);

        if (state == Call.State.ACTIVE) {
            builder.setUsesChronometer(true);
            builder.setWhen(call.getConnectTime());
        } else {
            builder.setUsesChronometer(false);
        }

        // Add hang up option for any active calls (active | onhold), outgoing calls (dialing).
        if (state == Call.State.ACTIVE ||
                state == Call.State.ONHOLD ||
                Call.State.isDialing(state)) {
            addHangupAction(builder);
        }

        /*
         * Fire off the notification
         */
        Notification notification = builder.build();
        Log.d(this, "Notifying IN_CALL_NOTIFICATION: " + notification);
        mNotificationManager.notify(IN_CALL_NOTIFICATION, notification);
        mIsShowingNotification = true;
    }

    /**
     * Checks the new notification data and compares it against any notification that we
     * are already displaying. If the data is exactly the same, we return false so that
     * we do not issue a new notification for the exact same data.
     */
    private boolean checkForChangeAndSaveData(int icon, int content, Bitmap largeIcon,
            String contentTitle, int state, boolean showFullScreenIntent) {

        // The two are different:
        // if new title is not null, it should be different from saved version OR
        // if new title is null, the saved version should not be null
        final boolean contentTitleChanged =
                (contentTitle != null && !contentTitle.equals(mSavedContentTitle)) ||
                (contentTitle == null && mSavedContentTitle != null);

        // any change means we are definitely updating
        boolean retval = (mSavedIcon != icon) || (mSavedContent != content) ||
                (mCallState != state) || (mSavedLargeIcon != largeIcon) ||
                contentTitleChanged;

        // A full screen intent means that we have been asked to interrupt an activity,
        // so we definitely want to show it.
        if (showFullScreenIntent) {
            Log.d(this, "Forcing full screen intent");
            retval = true;
        }

        // If we aren't showing a notification right now, definitely start showing one.
        if (!mIsShowingNotification) {
            Log.d(this, "Showing notification for first time.");
            retval = true;
        }

        mSavedIcon = icon;
        mSavedContent = content;
        mCallState = state;
        mSavedLargeIcon = largeIcon;
        mSavedContentTitle = contentTitle;

        if (retval) {
            Log.d(this, "Data changed.  Showing notification");
        }

        return retval;
    }

    /**
     * Returns the main string to use in the notification.
     */
    private String getContentTitle(ContactCacheEntry contactInfo, boolean isConference) {
        if (isConference) {
            return mContext.getResources().getString(R.string.card_title_conf_call);
        }
        if (TextUtils.isEmpty(contactInfo.name)) {
            return contactInfo.number;
        }

        return contactInfo.name;
    }

    /**
     * Gets a large icon from the contact info object to display in the notification.
     */
    private Bitmap getLargeIconToDisplay(ContactCacheEntry contactInfo, boolean isConference) {
        Bitmap largeIcon = null;
        if (isConference) {
            largeIcon = BitmapFactory.decodeResource(mContext.getResources(),
                    R.drawable.picture_conference);
        }
        if (contactInfo.photo != null && (contactInfo.photo instanceof BitmapDrawable)) {
            largeIcon = ((BitmapDrawable) contactInfo.photo).getBitmap();
        }

        if (largeIcon != null) {
            final int height = (int) mContext.getResources().getDimension(
                    android.R.dimen.notification_large_icon_height);
            final int width = (int) mContext.getResources().getDimension(
                    android.R.dimen.notification_large_icon_width);
            largeIcon = Bitmap.createScaledBitmap(largeIcon, width, height, false);
        }

        return largeIcon;
    }

    /**
     * Returns the appropriate icon res Id to display based on the call for which
     * we want to display information.
     */
    private int getIconToDisplay(Call call) {
        // Even if both lines are in use, we only show a single item in
        // the expanded Notifications UI.  It's labeled "Ongoing call"
        // (or "On hold" if there's only one call, and it's on hold.)
        // Also, we don't have room to display caller-id info from two
        // different calls.  So if both lines are in use, display info
        // from the foreground call.  And if there's a ringing call,
        // display that regardless of the state of the other calls.
        if (call.getState() == Call.State.ONHOLD) {
            return R.drawable.stat_sys_phone_call_on_hold;
        }
        return R.drawable.stat_sys_phone_call;
    }

    /**
     * Returns the message to use with the notification.
     */
    private int getContentString(Call call) {
        int resId = R.string.notification_ongoing_call;

        if (call.getState() == Call.State.INCOMING || call.getState() == Call.State.CALL_WAITING) {
            resId = R.string.notification_incoming_call;

        } else if (call.getState() == Call.State.ONHOLD) {
            resId = R.string.notification_on_hold;

        } else if (Call.State.isDialing(call.getState())) {
            resId = R.string.notification_dialing;
        }

        return resId;
    }

    /**
     * Gets the most relevant call to display in the notification.
     */
    private Call getCallToShow(CallList callList) {
        if (callList == null) {
            return null;
        }
        Call call = callList.getIncomingCall();
        if (call == null) {
            call = callList.getOutgoingCall();
        }
        if (call == null) {
            call = callList.getActiveOrBackgroundCall();
        }
        return call;
    }

    private void addHangupAction(Notification.Builder builder) {
        Log.i(this, "Will show \"hang-up\" action in the ongoing active call Notification");

        // TODO: use better asset.
        builder.addAction(R.drawable.stat_sys_phone_call_end,
                mContext.getText(R.string.notification_action_end_call),
                createHangUpOngoingCallPendingIntent(mContext));
    }

    /**
     * Adds fullscreen intent to the builder.
     */
    private void configureFullScreenIntent(Notification.Builder builder, PendingIntent intent,
            Call call) {
        // Ok, we actually want to launch the incoming call
        // UI at this point (in addition to simply posting a notification
        // to the status bar).  Setting fullScreenIntent will cause
        // the InCallScreen to be launched immediately *unless* the
        // current foreground activity is marked as "immersive".
        Log.d(this, "- Setting fullScreenIntent: " + intent);
        builder.setFullScreenIntent(intent, true);

        // Ugly hack alert:
        //
        // The NotificationManager has the (undocumented) behavior
        // that it will *ignore* the fullScreenIntent field if you
        // post a new Notification that matches the ID of one that's
        // already active.  Unfortunately this is exactly what happens
        // when you get an incoming call-waiting call:  the
        // "ongoing call" notification is already visible, so the
        // InCallScreen won't get launched in this case!
        // (The result: if you bail out of the in-call UI while on a
        // call and then get a call-waiting call, the incoming call UI
        // won't come up automatically.)
        //
        // The workaround is to just notice this exact case (this is a
        // call-waiting call *and* the InCallScreen is not in the
        // foreground) and manually cancel the in-call notification
        // before (re)posting it.
        //
        // TODO: there should be a cleaner way of avoiding this
        // problem (see discussion in bug 3184149.)

        // If a call is onhold during an incoming call, the call actually comes in as
        // INCOMING.  For that case *and* traditional call-waiting, we want to
        // cancel the notification.
        boolean isCallWaiting = (call.getState() == Call.State.CALL_WAITING ||
                (call.getState() == Call.State.INCOMING &&
                        CallList.getInstance().getBackgroundCall() != null));

        if (isCallWaiting) {
            Log.i(this, "updateInCallNotification: call-waiting! force relaunch...");
            // Cancel the IN_CALL_NOTIFICATION immediately before
            // (re)posting it; this seems to force the
            // NotificationManager to launch the fullScreenIntent.
            mNotificationManager.cancel(IN_CALL_NOTIFICATION);
        }
    }

    private Notification.Builder getNotificationBuilder() {
        final Notification.Builder builder = new Notification.Builder(mContext);
        builder.setOngoing(true);

        // Make the notification prioritized over the other normal notifications.
        builder.setPriority(Notification.PRIORITY_HIGH);

        return builder;
    }
    private PendingIntent createLaunchPendingIntent() {

        final Intent intent = InCallPresenter.getInstance().getInCallIntent(/*showdialpad=*/false);

        // PendingIntent that can be used to launch the InCallActivity.  The
        // system fires off this intent if the user pulls down the windowshade
        // and clicks the notification's expanded view.  It's also used to
        // launch the InCallActivity immediately when when there's an incoming
        // call (see the "fullScreenIntent" field below).
        PendingIntent inCallPendingIntent = PendingIntent.getActivity(mContext, 0, intent, 0);

        return inCallPendingIntent;
    }

    /**
     * Returns PendingIntent for hanging up ongoing phone call. This will typically be used from
     * Notification context.
     */
    private static PendingIntent createHangUpOngoingCallPendingIntent(Context context) {
        final Intent intent = new Intent(InCallApp.ACTION_HANG_UP_ONGOING_CALL, null,
                context, NotificationBroadcastReceiver.class);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }
}
