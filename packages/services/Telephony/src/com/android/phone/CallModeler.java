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

package com.android.phone;

import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.telephony.CallManager;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyCapabilities;
import com.android.internal.telephony.cdma.CdmaCallWaitingNotification;
import com.android.phone.CallGatewayManager.RawGatewayInfo;
import com.android.services.telephony.common.Call;
import com.android.services.telephony.common.Call.Capabilities;
import com.android.services.telephony.common.Call.State;

import com.google.android.collect.Maps;
import com.google.android.collect.Sets;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Creates a Call model from Call state and data received from the telephony
 * layer. The telephony layer maintains 3 conceptual objects: Phone, Call,
 * Connection.
 *
 * Phone represents the radio and there is an implementation per technology
 * type such as GSMPhone, SipPhone, CDMAPhone, etc. Generally, we will only ever
 * deal with one instance of this object for the lifetime of this class.
 *
 * There are 3 Call instances that exist for the lifetime of this class which
 * are created by CallTracker. The three are RingingCall, ForegroundCall, and
 * BackgroundCall.
 *
 * A Connection most closely resembles what the layperson would consider a call.
 * A Connection is created when a user dials and it is "owned" by one of the
 * three Call instances.  Which of the three Calls owns the Connection changes
 * as the Connection goes between ACTIVE, HOLD, RINGING, and other states.
 *
 * This class models a new Call class from Connection objects received from
 * the telephony layer. We use Connection references as identifiers for a call;
 * new reference = new call.
 *
 * TODO: Create a new Call class to replace the simple call Id ints
 * being used currently.
 *
 * The new Call models are parcellable for transfer via the CallHandlerService
 * API.
 */
public class CallModeler extends Handler {

    private static final String TAG = CallModeler.class.getSimpleName();
    private static final boolean DBG =
            (PhoneGlobals.DBG_LEVEL >= 1) && (SystemProperties.getInt("ro.debuggable", 0) == 1);

    private static final int CALL_ID_START_VALUE = 1;

    private final CallStateMonitor mCallStateMonitor;
    private final CallManager mCallManager;
    private final CallGatewayManager mCallGatewayManager;
    private final HashMap<Connection, Call> mCallMap = Maps.newHashMap();
    private final HashMap<Connection, Call> mConfCallMap = Maps.newHashMap();
    private final AtomicInteger mNextCallId = new AtomicInteger(CALL_ID_START_VALUE);
    private final ArrayList<Listener> mListeners = new ArrayList<Listener>();
    private Connection mCdmaIncomingConnection;
    private Connection mCdmaOutgoingConnection;

    public CallModeler(CallStateMonitor callStateMonitor, CallManager callManager,
            CallGatewayManager callGatewayManager) {
        mCallStateMonitor = callStateMonitor;
        mCallManager = callManager;
        mCallGatewayManager = callGatewayManager;

        mCallStateMonitor.addListener(this);
    }

    @Override
    public void handleMessage(Message msg) {
        switch(msg.what) {
            case CallStateMonitor.PHONE_NEW_RINGING_CONNECTION:
                // We let the CallNotifier handle the new ringing connection first. When the custom
                // ringtone and send_to_voicemail settings are retrieved, CallNotifier will directly
                // call CallModeler's onNewRingingConnection.
                break;
            case CallStateMonitor.PHONE_DISCONNECT:
                onDisconnect((Connection) ((AsyncResult) msg.obj).result);
                break;
            case CallStateMonitor.PHONE_UNKNOWN_CONNECTION_APPEARED:
                // fall through
            case CallStateMonitor.PHONE_STATE_CHANGED:
                onPhoneStateChanged((AsyncResult) msg.obj);
                break;
            case CallStateMonitor.PHONE_ON_DIAL_CHARS:
                onPostDialChars((AsyncResult) msg.obj, (char) msg.arg1);
                break;
            default:
                break;
        }
    }

    public void addListener(Listener listener) {
        Preconditions.checkNotNull(listener);
        Preconditions.checkNotNull(mListeners);
        if (!mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }

    public List<Call> getFullList() {
        final List<Call> calls =
                Lists.newArrayListWithCapacity(mCallMap.size() + mConfCallMap.size());
        calls.addAll(mCallMap.values());
        calls.addAll(mConfCallMap.values());
        return calls;
    }

    public CallResult getCallWithId(int callId) {
        // max 8 connections, so this should be fast even through we are traversing the entire map.
        for (Entry<Connection, Call> entry : mCallMap.entrySet()) {
            if (entry.getValue().getCallId() == callId) {
                return new CallResult(entry.getValue(), entry.getKey());
            }
        }

        for (Entry<Connection, Call> entry : mConfCallMap.entrySet()) {
            if (entry.getValue().getCallId() == callId) {
                return new CallResult(entry.getValue(), entry.getKey());
            }
        }
        return null;
    }

    public boolean hasLiveCall() {
        return hasLiveCallInternal(mCallMap) ||
            hasLiveCallInternal(mConfCallMap);
    }

    public void onCdmaCallWaiting(CdmaCallWaitingNotification callWaitingInfo) {
        // We dont get the traditional onIncomingCall notification for cdma call waiting,
        // but the Connection does actually exist.  We need to find it in the set of ringing calls
        // and pass it through our normal incoming logic.
        final com.android.internal.telephony.Call teleCall =
            mCallManager.getFirstActiveRingingCall();

        if (teleCall.getState() == com.android.internal.telephony.Call.State.WAITING) {
            Connection connection = teleCall.getLatestConnection();

            if (connection != null) {
                String number = connection.getAddress();
                if (number != null && number.equals(callWaitingInfo.number)) {
                    Call call = onNewRingingConnection(connection);
                    mCdmaIncomingConnection = connection;
                    return;
                }
            }
        }

        Log.e(TAG, "CDMA Call waiting notification without a matching connection.");
    }

    public void onCdmaCallWaitingReject() {
        // Cdma call was rejected...
        if (mCdmaIncomingConnection != null) {
            onDisconnect(mCdmaIncomingConnection);
            mCdmaIncomingConnection = null;
        } else {
            Log.e(TAG, "CDMA Call waiting rejection without an incoming call.");
        }
    }

    /**
     * CDMA Calls have no sense of "dialing" state. For outgoing calls 3way calls we want to
     * mimick this state so that the the UI can notify the user that there is a "dialing"
     * call.
     */
    public void setCdmaOutgoing3WayCall(Connection connection) {
        boolean wasSet = mCdmaOutgoingConnection != null;

        mCdmaOutgoingConnection = connection;

        // If we reset the connection, that mean we can now tell the user that the call is actually
        // part of the conference call and move it out of the dialing state. To do this, issue a
        // new update completely.
        if (wasSet && mCdmaOutgoingConnection == null) {
            onPhoneStateChanged(null);
        }
    }

    private boolean hasLiveCallInternal(HashMap<Connection, Call> map) {
        for (Call call : map.values()) {
            final int state = call.getState();
            if (state == Call.State.ACTIVE ||
                    state == Call.State.CALL_WAITING ||
                    state == Call.State.CONFERENCED ||
                    state == Call.State.DIALING ||
                    state == Call.State.REDIALING ||
                    state == Call.State.INCOMING ||
                    state == Call.State.ONHOLD ||
                    state == Call.State.DISCONNECTING) {
                return true;
            }
        }
        return false;
    }

    public boolean hasOutstandingActiveOrDialingCall() {
        return hasOutstandingActiveOrDialingCallInternal(mCallMap) ||
                hasOutstandingActiveOrDialingCallInternal(mConfCallMap);
    }

    private static boolean hasOutstandingActiveOrDialingCallInternal(
            HashMap<Connection, Call> map) {
        for (Call call : map.values()) {
            final int state = call.getState();
            if (state == Call.State.ACTIVE || Call.State.isDialing(state)) {
                return true;
            }
        }

        return false;
    }


    /**
     * Handles the POST_ON_DIAL_CHARS message from the Phone (see our call to
     * mPhone.setOnPostDialCharacter() above.)
     *
     * TODO: NEED TO TEST THIS SEQUENCE now that we no longer handle "dialable" key events here in
     * the InCallScreen: we do directly to the Dialer UI instead.  Similarly, we may now need to go
     * directly to the Dialer to handle POST_ON_DIAL_CHARS too.
     */
    private void onPostDialChars(AsyncResult r, char ch) {
        final Connection c = (Connection) r.result;

        if (c != null) {
            final Connection.PostDialState state = (Connection.PostDialState) r.userObj;

            switch (state) {
                case WAIT:
                    final Call call = getCallFromMap(mCallMap, c, false);
                    if (call == null) {
                        Log.i(TAG, "Call no longer exists. Skipping onPostDialWait().");
                    } else {
                        for (Listener mListener : mListeners) {
                            mListener.onPostDialAction(state, call.getCallId(),
                                    c.getRemainingPostDialString(), ch);
                        }
                    }
                    break;
                default:
                    // This is primarily to cause the DTMFTonePlayer to play local tones.
                    // Other listeners simply perform no-ops.
                    for (Listener mListener : mListeners) {
                        mListener.onPostDialAction(state, 0, "", ch);
                    }
                    break;
            }
        }
    }

    /* package */ Call onNewRingingConnection(Connection conn) {
        Log.i(TAG, "onNewRingingConnection");
        final Call call = getCallFromMap(mCallMap, conn, true);

        if (call != null) {
            updateCallFromConnection(call, conn, false);

            for (int i = 0; i < mListeners.size(); ++i) {
                mListeners.get(i).onIncoming(call);
            }
        }

        PhoneGlobals.getInstance().updateWakeState();
        return call;
    }

    private void onDisconnect(Connection conn) {
        Log.i(TAG, "onDisconnect");
        final Call call = getCallFromMap(mCallMap, conn, false);

        if (call != null) {
            final boolean wasConferenced = call.getState() == State.CONFERENCED;

            updateCallFromConnection(call, conn, false);

            for (int i = 0; i < mListeners.size(); ++i) {
                mListeners.get(i).onDisconnect(call);
            }

            // If it was a conferenced call, we need to run the entire update
            // to make the proper changes to parent conference calls.
            if (wasConferenced) {
                onPhoneStateChanged(null);
            }

            mCallMap.remove(conn);
        }

        mCallManager.clearDisconnected();
        PhoneGlobals.getInstance().updateWakeState();
    }

    /**
     * Called when the phone state changes.
     */
    private void onPhoneStateChanged(AsyncResult r) {
        Log.i(TAG, "onPhoneStateChanged: ");
        final List<Call> updatedCalls = Lists.newArrayList();
        doUpdate(false, updatedCalls);

        if (updatedCalls.size() > 0) {
            for (int i = 0; i < mListeners.size(); ++i) {
                mListeners.get(i).onUpdate(updatedCalls);
            }
        }

        PhoneGlobals.getInstance().updateWakeState();
    }

    /**
     * Go through the Calls from CallManager and return the list of calls that were updated.
     * Method also finds any orphaned Calls (Connection objects no longer returned by telephony as
     * either ringing, foreground, or background).  For each orphaned call, it sets the call state
     * to IDLE and adds it to the list of calls to update.
     *
     * @param fullUpdate Add all calls to out parameter including those that have no updates.
     * @param out List to populate with Calls that have been updated.
     */
    private void doUpdate(boolean fullUpdate, List<Call> out) {
        final List<com.android.internal.telephony.Call> telephonyCalls = Lists.newArrayList();
        telephonyCalls.addAll(mCallManager.getRingingCalls());
        telephonyCalls.addAll(mCallManager.getForegroundCalls());
        telephonyCalls.addAll(mCallManager.getBackgroundCalls());

        // orphanedConnections starts out including all connections we know about.
        // As we iterate through the connections we get from the telephony layer we
        // prune this Set down to only the connections we have but telephony no longer
        // recognizes.
        final Set<Connection> orphanedConnections = Sets.newHashSet();
        orphanedConnections.addAll(mCallMap.keySet());
        orphanedConnections.addAll(mConfCallMap.keySet());

        // Cycle through all the Connections on all the Calls. Update our Call objects
        // to reflect any new state and send the updated Call objects to the handler service.
        for (com.android.internal.telephony.Call telephonyCall : telephonyCalls) {

            for (Connection connection : telephonyCall.getConnections()) {
                if (DBG) Log.d(TAG, "connection: " + connection + connection.getState());

                if (orphanedConnections.contains(connection)) {
                    orphanedConnections.remove(connection);
                }

                // We only send updates for live calls which are not incoming (ringing).
                // Disconnected and incoming calls are handled by onDisconnect and
                // onNewRingingConnection.
                final boolean shouldUpdate =
                        connection.getState() !=
                                com.android.internal.telephony.Call.State.DISCONNECTED &&
                        connection.getState() !=
                                com.android.internal.telephony.Call.State.IDLE &&
                        !connection.getState().isRinging();

                final boolean isDisconnecting = connection.getState() ==
                                com.android.internal.telephony.Call.State.DISCONNECTING;

                // For disconnecting calls, we still need to send the update to the UI but we do
                // not create a new call if the call did not exist.
                final boolean shouldCreate = shouldUpdate && !isDisconnecting;

                // New connections return a Call with INVALID state, which does not translate to
                // a state in the internal.telephony.Call object.  This ensures that staleness
                // check below fails and we always add the item to the update list if it is new.
                final Call call = getCallFromMap(mCallMap, connection, shouldCreate /* create */);

                if (call == null || !shouldUpdate) {
                    if (DBG) Log.d(TAG, "update skipped");
                    continue;
                }

                boolean changed = updateCallFromConnection(call, connection, false);

                if (fullUpdate || changed) {
                    out.add(call);
                }
            }

            // We do a second loop to address conference call scenarios.  We do this as a separate
            // loop to ensure all child calls are up to date before we start updating the parent
            // conference calls.
            for (Connection connection : telephonyCall.getConnections()) {
                updateForConferenceCalls(connection, out);
            }
        }

        // Iterate through orphaned connections, set them to idle, and remove
        // them from our internal structures.
        for (Connection orphanedConnection : orphanedConnections) {
            if (mCallMap.containsKey(orphanedConnection)) {
                final Call call = mCallMap.get(orphanedConnection);
                call.setState(Call.State.IDLE);
                out.add(call);

                mCallMap.remove(orphanedConnection);
            }

            if (mConfCallMap.containsKey(orphanedConnection)) {
                final Call call = mCallMap.get(orphanedConnection);
                call.setState(Call.State.IDLE);
                out.add(call);

                mConfCallMap.remove(orphanedConnection);
            }
        }
    }

    /**
     * Checks to see if the connection is the first connection in a conference call.
     * If it is a conference call, we will create a new Conference Call object or
     * update the existing conference call object for that connection.
     * If it is not a conference call but a previous associated conference call still exists,
     * we mark it as idle and remove it from the map.
     * In both cases above, we add the Calls to be updated to the UI.
     * @param connection The connection object to check.
     * @param updatedCalls List of 'updated' calls that will be sent to the UI.
     */
    private boolean updateForConferenceCalls(Connection connection, List<Call> updatedCalls) {
        // We consider this connection a conference connection if the call it
        // belongs to is a multiparty call AND it is the first live connection.
        final boolean isConferenceCallConnection = isPartOfLiveConferenceCall(connection) &&
                getEarliestLiveConnection(connection.getCall()) == connection;

        boolean changed = false;

        // If this connection is the main connection for the conference call, then create or update
        // a Call object for that conference call.
        if (isConferenceCallConnection) {
            final Call confCall = getCallFromMap(mConfCallMap, connection, true);
            changed = updateCallFromConnection(confCall, connection, true);

            if (changed) {
                updatedCalls.add(confCall);
            }

            if (DBG) Log.d(TAG, "Updating a conference call: " + confCall);

        // It is possible that through a conference call split, there may be lingering conference
        // calls where this connection was the main connection.  We clean those up here.
        } else {
            final Call oldConfCall = getCallFromMap(mConfCallMap, connection, false);

            // We found a conference call for this connection, which is no longer a conference call.
            // Kill it!
            if (oldConfCall != null) {
                if (DBG) Log.d(TAG, "Cleaning up an old conference call: " + oldConfCall);
                mConfCallMap.remove(connection);
                oldConfCall.setState(State.IDLE);
                changed = true;

                // add to the list of calls to update
                updatedCalls.add(oldConfCall);
            }
        }

        return changed;
    }

    private Connection getEarliestLiveConnection(com.android.internal.telephony.Call call) {
        final List<Connection> connections = call.getConnections();
        final int size = connections.size();
        Connection earliestConn = null;
        long earliestTime = Long.MAX_VALUE;
        for (int i = 0; i < size; i++) {
            final Connection connection = connections.get(i);
            if (!connection.isAlive()) continue;
            final long time = connection.getCreateTime();
            if (time < earliestTime) {
                earliestTime = time;
                earliestConn = connection;
            }
        }
        return earliestConn;
    }

    /**
     * Sets the new call state onto the call and performs some additional logic
     * associated with setting the state.
     */
    private void setNewState(Call call, int newState, Connection connection) {
        Preconditions.checkState(call.getState() != newState);

        // When starting an outgoing call, we need to grab gateway information
        // for the call, if available, and set it.
        final RawGatewayInfo info = mCallGatewayManager.getGatewayInfo(connection);

        if (Call.State.isDialing(newState)) {
            if (!info.isEmpty()) {
                call.setGatewayNumber(info.getFormattedGatewayNumber());
                call.setGatewayPackage(info.packageName);
            }
        } else if (!Call.State.isConnected(newState)) {
            mCallGatewayManager.clearGatewayData(connection);
        }

        call.setState(newState);
    }

    /**
     * Updates the Call properties to match the state of the connection object
     * that it represents.
     * @param call The call object to update.
     * @param connection The connection object from which to update call.
     * @param isForConference There are slight differences in how we populate data for conference
     *     calls. This boolean tells us which method to use.
     */
    private boolean updateCallFromConnection(Call call, Connection connection,
            boolean isForConference) {
        boolean changed = false;

        final int newState = translateStateFromTelephony(connection, isForConference);

        if (call.getState() != newState) {
            setNewState(call, newState, connection);
            changed = true;
        }

        final Call.DisconnectCause newDisconnectCause =
                translateDisconnectCauseFromTelephony(connection.getDisconnectCause());
        if (call.getDisconnectCause() != newDisconnectCause) {
            call.setDisconnectCause(newDisconnectCause);
            changed = true;
        }

        final long oldConnectTime = call.getConnectTime();
        if (oldConnectTime != connection.getConnectTime()) {
            call.setConnectTime(connection.getConnectTime());
            changed = true;
        }

        if (!isForConference) {
            // Number
            final String oldNumber = call.getNumber();
            String newNumber = connection.getAddress();
            RawGatewayInfo info = mCallGatewayManager.getGatewayInfo(connection);
            if (!info.isEmpty()) {
                newNumber = info.trueNumber;
            }
            if (TextUtils.isEmpty(oldNumber) || !oldNumber.equals(newNumber)) {
                call.setNumber(newNumber);
                changed = true;
            }

            // Number presentation
            final int newNumberPresentation = connection.getNumberPresentation();
            if (call.getNumberPresentation() != newNumberPresentation) {
                call.setNumberPresentation(newNumberPresentation);
                changed = true;
            }

            // Name
            final String oldCnapName = call.getCnapName();
            if (TextUtils.isEmpty(oldCnapName) || !oldCnapName.equals(connection.getCnapName())) {
                call.setCnapName(connection.getCnapName());
                changed = true;
            }

            // Name Presentation
            final int newCnapNamePresentation = connection.getCnapNamePresentation();
            if (call.getCnapNamePresentation() != newCnapNamePresentation) {
                call.setCnapNamePresentation(newCnapNamePresentation);
                changed = true;
            }
        } else {

            // update the list of children by:
            // 1) Saving the old set
            // 2) Removing all children
            // 3) Adding the correct children into the Call
            // 4) Comparing the new children set with the old children set
            ImmutableSortedSet<Integer> oldSet = call.getChildCallIds();
            call.removeAllChildren();

            if (connection.getCall() != null) {
                for (Connection childConn : connection.getCall().getConnections()) {
                    final Call childCall = getCallFromMap(mCallMap, childConn, false);
                    if (childCall != null && childConn.isAlive()) {
                        call.addChildId(childCall.getCallId());
                    }
                }
            }
            changed |= !oldSet.equals(call.getChildCallIds());
        }

        /**
         * !!! Uses values from connection and call collected above so this part must be last !!!
         */
        final int newCapabilities = getCapabilitiesFor(connection, call, isForConference);
        if (call.getCapabilities() != newCapabilities) {
            call.setCapabilities(newCapabilities);
            changed = true;
        }

        return changed;
    }

    /**
     * Returns a mask of capabilities for the connection such as merge, hold, etc.
     */
    private int getCapabilitiesFor(Connection connection, Call call, boolean isForConference) {
        final boolean callIsActive = (call.getState() == Call.State.ACTIVE);
        final Phone phone = connection.getCall().getPhone();

        boolean canAddCall = false;
        boolean canMergeCall = false;
        boolean canSwapCall = false;
        boolean canRespondViaText = false;
        boolean canMute = false;

        final boolean supportHold = PhoneUtils.okToSupportHold(mCallManager);
        final boolean canHold = (supportHold ? PhoneUtils.okToHoldCall(mCallManager) : false);
        final boolean genericConf = isForConference &&
                (connection.getCall().getPhone().getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA);

        // only applies to active calls
        if (callIsActive) {
            canMergeCall = PhoneUtils.okToMergeCalls(mCallManager);
            canSwapCall = PhoneUtils.okToSwapCalls(mCallManager);
        }

        canAddCall = PhoneUtils.okToAddCall(mCallManager);

        // "Mute": only enabled when the foreground call is ACTIVE.
        // (It's meaningless while on hold, or while DIALING/ALERTING.)
        // It's also explicitly disabled during emergency calls or if
        // emergency callback mode (ECM) is active.
        boolean isEmergencyCall = false;
        if (connection != null) {
            isEmergencyCall = PhoneNumberUtils.isLocalEmergencyNumber(connection.getAddress(),
                    phone.getContext());
        }
        boolean isECM = PhoneUtils.isPhoneInEcm(phone);
        if (isEmergencyCall || isECM) {  // disable "Mute" item
            canMute = false;
        } else {
            canMute = callIsActive;
        }

        canRespondViaText = RejectWithTextMessageManager.allowRespondViaSmsForCall(call,
                connection);

        // special rules section!
        // CDMA always has Add
        if (phone.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) {
            canAddCall = true;
        }

        int retval = 0x0;
        if (canHold) {
            retval |= Capabilities.HOLD;
        }
        if (supportHold) {
            retval |= Capabilities.SUPPORT_HOLD;
        }
        if (canAddCall) {
            retval |= Capabilities.ADD_CALL;
        }
        if (canMergeCall) {
            retval |= Capabilities.MERGE_CALLS;
        }
        if (canSwapCall) {
            retval |= Capabilities.SWAP_CALLS;
        }
        if (canRespondViaText) {
            retval |= Capabilities.RESPOND_VIA_TEXT;
        }
        if (canMute) {
            retval |= Capabilities.MUTE;
        }
        if (genericConf) {
            retval |= Capabilities.GENERIC_CONFERENCE;
        }

        return retval;
    }

    /**
     * Returns true if the Connection is part of a multiparty call.
     * We do this by checking the isMultiparty() method of the telephony.Call object and also
     * checking to see if more than one of it's children is alive.
     */
    private boolean isPartOfLiveConferenceCall(Connection connection) {
        if (connection.getCall() != null && connection.getCall().isMultiparty()) {
            int count = 0;
            for (Connection currConn : connection.getCall().getConnections()) {

                // Only count connections which are alive and never cound the special
                // "dialing" 3way call for CDMA calls.
                if (currConn.isAlive() && currConn != mCdmaOutgoingConnection) {
                    count++;
                    if (count >= 2) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private int translateStateFromTelephony(Connection connection, boolean isForConference) {

        com.android.internal.telephony.Call.State connState = connection.getState();

        // For the "fake" outgoing CDMA call, we need to always treat it as an outgoing call.
        if (mCdmaOutgoingConnection == connection) {
            connState = com.android.internal.telephony.Call.State.DIALING;
        }

        int retval = State.IDLE;
        switch (connState) {
            case ACTIVE:
                retval = State.ACTIVE;
                break;
            case INCOMING:
                retval = State.INCOMING;
                break;
            case DIALING:
            case ALERTING:
                if (PhoneGlobals.getInstance().notifier.getIsCdmaRedialCall()) {
                    retval = State.REDIALING;
                } else {
                    retval = State.DIALING;
                }
                break;
            case WAITING:
                retval = State.CALL_WAITING;
                break;
            case HOLDING:
                retval = State.ONHOLD;
                break;
            case DISCONNECTING:
                retval = State.DISCONNECTING;
                break;
            case DISCONNECTED:
                retval = State.DISCONNECTED;
            default:
        }

        // If we are dealing with a potential child call (not the parent conference call),
        // the check to see if we have to set the state to CONFERENCED.
        if (!isForConference) {
            // if the connection is part of a multiparty call, and it is live,
            // annotate it with CONFERENCED state instead.
            if (isPartOfLiveConferenceCall(connection) && connection.isAlive()) {
                return State.CONFERENCED;
            }
        }

        return retval;
    }

    private final ImmutableMap<Connection.DisconnectCause, Call.DisconnectCause> CAUSE_MAP =
            ImmutableMap.<Connection.DisconnectCause, Call.DisconnectCause>builder()
                .put(Connection.DisconnectCause.BUSY, Call.DisconnectCause.BUSY)
                .put(Connection.DisconnectCause.CALL_BARRED, Call.DisconnectCause.CALL_BARRED)
                .put(Connection.DisconnectCause.CDMA_ACCESS_BLOCKED,
                        Call.DisconnectCause.CDMA_ACCESS_BLOCKED)
                .put(Connection.DisconnectCause.CDMA_ACCESS_FAILURE,
                        Call.DisconnectCause.CDMA_ACCESS_FAILURE)
                .put(Connection.DisconnectCause.CDMA_DROP, Call.DisconnectCause.CDMA_DROP)
                .put(Connection.DisconnectCause.CDMA_INTERCEPT, Call.DisconnectCause.CDMA_INTERCEPT)
                .put(Connection.DisconnectCause.CDMA_LOCKED_UNTIL_POWER_CYCLE,
                        Call.DisconnectCause.CDMA_LOCKED_UNTIL_POWER_CYCLE)
                .put(Connection.DisconnectCause.CDMA_NOT_EMERGENCY,
                        Call.DisconnectCause.CDMA_NOT_EMERGENCY)
                .put(Connection.DisconnectCause.CDMA_PREEMPTED, Call.DisconnectCause.CDMA_PREEMPTED)
                .put(Connection.DisconnectCause.CDMA_REORDER, Call.DisconnectCause.CDMA_REORDER)
                .put(Connection.DisconnectCause.CDMA_RETRY_ORDER,
                        Call.DisconnectCause.CDMA_RETRY_ORDER)
                .put(Connection.DisconnectCause.CDMA_SO_REJECT, Call.DisconnectCause.CDMA_SO_REJECT)
                .put(Connection.DisconnectCause.CONGESTION, Call.DisconnectCause.CONGESTION)
                .put(Connection.DisconnectCause.CS_RESTRICTED, Call.DisconnectCause.CS_RESTRICTED)
                .put(Connection.DisconnectCause.CS_RESTRICTED_EMERGENCY,
                        Call.DisconnectCause.CS_RESTRICTED_EMERGENCY)
                .put(Connection.DisconnectCause.CS_RESTRICTED_NORMAL,
                        Call.DisconnectCause.CS_RESTRICTED_NORMAL)
                .put(Connection.DisconnectCause.ERROR_UNSPECIFIED,
                        Call.DisconnectCause.ERROR_UNSPECIFIED)
                .put(Connection.DisconnectCause.FDN_BLOCKED, Call.DisconnectCause.FDN_BLOCKED)
                .put(Connection.DisconnectCause.ICC_ERROR, Call.DisconnectCause.ICC_ERROR)
                .put(Connection.DisconnectCause.INCOMING_MISSED,
                        Call.DisconnectCause.INCOMING_MISSED)
                .put(Connection.DisconnectCause.INCOMING_REJECTED,
                        Call.DisconnectCause.INCOMING_REJECTED)
                .put(Connection.DisconnectCause.INVALID_CREDENTIALS,
                        Call.DisconnectCause.INVALID_CREDENTIALS)
                .put(Connection.DisconnectCause.INVALID_NUMBER,
                        Call.DisconnectCause.INVALID_NUMBER)
                .put(Connection.DisconnectCause.LIMIT_EXCEEDED, Call.DisconnectCause.LIMIT_EXCEEDED)
                .put(Connection.DisconnectCause.LOCAL, Call.DisconnectCause.LOCAL)
                .put(Connection.DisconnectCause.LOST_SIGNAL, Call.DisconnectCause.LOST_SIGNAL)
                .put(Connection.DisconnectCause.MMI, Call.DisconnectCause.MMI)
                .put(Connection.DisconnectCause.NORMAL, Call.DisconnectCause.NORMAL)
                .put(Connection.DisconnectCause.NOT_DISCONNECTED,
                        Call.DisconnectCause.NOT_DISCONNECTED)
                .put(Connection.DisconnectCause.NUMBER_UNREACHABLE,
                        Call.DisconnectCause.NUMBER_UNREACHABLE)
                .put(Connection.DisconnectCause.OUT_OF_NETWORK, Call.DisconnectCause.OUT_OF_NETWORK)
                .put(Connection.DisconnectCause.OUT_OF_SERVICE, Call.DisconnectCause.OUT_OF_SERVICE)
                .put(Connection.DisconnectCause.POWER_OFF, Call.DisconnectCause.POWER_OFF)
                .put(Connection.DisconnectCause.SERVER_ERROR, Call.DisconnectCause.SERVER_ERROR)
                .put(Connection.DisconnectCause.SERVER_UNREACHABLE,
                        Call.DisconnectCause.SERVER_UNREACHABLE)
                .put(Connection.DisconnectCause.TIMED_OUT, Call.DisconnectCause.TIMED_OUT)
                .put(Connection.DisconnectCause.UNOBTAINABLE_NUMBER,
                        Call.DisconnectCause.UNOBTAINABLE_NUMBER)
                .build();

    private Call.DisconnectCause translateDisconnectCauseFromTelephony(
            Connection.DisconnectCause causeSource) {

        if (CAUSE_MAP.containsKey(causeSource)) {
            return CAUSE_MAP.get(causeSource);
        }

        return Call.DisconnectCause.UNKNOWN;
    }

    /**
     * Gets an existing callId for a connection, or creates one if none exists.
     * This function does NOT set any of the Connection data onto the Call class.
     * A separate call to updateCallFromConnection must be made for that purpose.
     */
    private Call getCallFromMap(HashMap<Connection, Call> map, Connection conn,
            boolean createIfMissing) {
        Call call = null;

        // Find the call id or create if missing and requested.
        if (conn != null) {
            if (map.containsKey(conn)) {
                call = map.get(conn);
            } else if (createIfMissing) {
                call = createNewCall();
                map.put(conn, call);
            }
        }
        return call;
    }

    /**
     * Creates a brand new connection for the call.
     */
    private Call createNewCall() {
        int callId;
        int newNextCallId;
        do {
            callId = mNextCallId.get();

            // protect against overflow
            newNextCallId = (callId == Integer.MAX_VALUE ?
                    CALL_ID_START_VALUE : callId + 1);

            // Keep looping if the change was not atomic OR the value is already taken.
            // The call to containsValue() is linear, however, most devices support a
            // maximum of 7 connections so it's not expensive.
        } while (!mNextCallId.compareAndSet(callId, newNextCallId));

        return new Call(callId);
    }

    /**
     * Listener interface for changes to Calls.
     */
    public interface Listener {
        void onDisconnect(Call call);
        void onIncoming(Call call);
        void onUpdate(List<Call> calls);
        void onPostDialAction(Connection.PostDialState state, int callId, String remainingChars,
                char c);
    }

    /**
     * Result class for accessing a call by connection.
     */
    public static class CallResult {
        public Call mCall;
        public Call mActionableCall;
        public Connection mConnection;

        private CallResult(Call call, Connection connection) {
            this(call, call, connection);
        }

        private CallResult(Call call, Call actionableCall, Connection connection) {
            mCall = call;
            mActionableCall = actionableCall;
            mConnection = connection;
        }

        public Call getCall() {
            return mCall;
        }

        // The call that should be used for call actions like hanging up.
        public Call getActionableCall() {
            return mActionableCall;
        }

        public Connection getConnection() {
            return mConnection;
        }
    }
}
