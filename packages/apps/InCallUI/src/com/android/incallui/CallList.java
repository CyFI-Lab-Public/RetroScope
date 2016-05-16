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
 * limitations under the License
 */

package com.android.incallui;

import com.google.android.collect.Lists;
import com.google.android.collect.Maps;
import com.google.android.collect.Sets;
import com.google.common.base.Preconditions;

import android.os.Handler;
import android.os.Message;

import com.android.services.telephony.common.Call;
import com.android.services.telephony.common.Call.DisconnectCause;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Maintains the list of active calls received from CallHandlerService and notifies interested
 * classes of changes to the call list as they are received from the telephony stack.
 * Primary lister of changes to this class is InCallPresenter.
 */
public class CallList {

    private static final int DISCONNECTED_CALL_SHORT_TIMEOUT_MS = 200;
    private static final int DISCONNECTED_CALL_MEDIUM_TIMEOUT_MS = 2000;
    private static final int DISCONNECTED_CALL_LONG_TIMEOUT_MS = 5000;

    private static final int EVENT_DISCONNECTED_TIMEOUT = 1;

    private static CallList sInstance = new CallList();

    private final HashMap<Integer, Call> mCallMap = Maps.newHashMap();
    private final HashMap<Integer, ArrayList<String>> mCallTextReponsesMap =
            Maps.newHashMap();
    private final Set<Listener> mListeners = Sets.newArraySet();
    private final HashMap<Integer, List<CallUpdateListener>> mCallUpdateListenerMap = Maps
            .newHashMap();


    /**
     * Static singleton accessor method.
     */
    public static CallList getInstance() {
        return sInstance;
    }

    /**
     * Private constructor.  Instance should only be acquired through getInstance().
     */
    private CallList() {
    }

    /**
     * Called when a single call has changed.
     */
    public void onUpdate(Call call) {
        Log.d(this, "onUpdate - ", call);

        updateCallInMap(call);
        notifyListenersOfChange();
    }

    /**
     * Called when a single call disconnects.
     */
    public void onDisconnect(Call call) {
        Log.d(this, "onDisconnect: ", call);

        boolean updated = updateCallInMap(call);

        if (updated) {
            // notify those listening for changes on this specific change
            notifyCallUpdateListeners(call);

            // notify those listening for all disconnects
            notifyListenersOfDisconnect(call);
        }
    }

    /**
     * Called when a single call has changed.
     */
    public void onIncoming(Call call, List<String> textMessages) {
        Log.d(this, "onIncoming - " + call);

        updateCallInMap(call);
        updateCallTextMap(call, textMessages);

        for (Listener listener : mListeners) {
            listener.onIncomingCall(call);
        }
    }

    /**
     * Called when multiple calls have changed.
     */
    public void onUpdate(List<Call> callsToUpdate) {
        Log.d(this, "onUpdate(...)");

        Preconditions.checkNotNull(callsToUpdate);
        for (Call call : callsToUpdate) {
            Log.d(this, "\t" + call);

            updateCallInMap(call);
            updateCallTextMap(call, null);

            notifyCallUpdateListeners(call);
        }

        notifyListenersOfChange();
    }

    public void notifyCallUpdateListeners(Call call) {
        final List<CallUpdateListener> listeners = mCallUpdateListenerMap.get(call.getCallId());
        if (listeners != null) {
            for (CallUpdateListener listener : listeners) {
                listener.onCallStateChanged(call);
            }
        }
    }

    /**
     * Add a call update listener for a call id.
     *
     * @param callId The call id to get updates for.
     * @param listener The listener to add.
     */
    public void addCallUpdateListener(int callId, CallUpdateListener listener) {
        List<CallUpdateListener> listeners = mCallUpdateListenerMap.get(callId);
        if (listeners == null) {
            listeners = Lists.newArrayList();
            mCallUpdateListenerMap.put(callId, listeners);
        }
        listeners.add(listener);
    }

    /**
     * Remove a call update listener for a call id.
     *
     * @param callId The call id to remove the listener for.
     * @param listener The listener to remove.
     */
    public void removeCallUpdateListener(int callId, CallUpdateListener listener) {
        List<CallUpdateListener> listeners = mCallUpdateListenerMap.get(callId);
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    public void addListener(Listener listener) {
        Preconditions.checkNotNull(listener);

        mListeners.add(listener);

        // Let the listener know about the active calls immediately.
        listener.onCallListChange(this);
    }

    public void removeListener(Listener listener) {
        Preconditions.checkNotNull(listener);
        mListeners.remove(listener);
    }

    /**
     * TODO: Change so that this function is not needed. Instead of assuming there is an active
     * call, the code should rely on the status of a specific Call and allow the presenters to
     * update the Call object when the active call changes.
     */
    public Call getIncomingOrActive() {
        Call retval = getIncomingCall();
        if (retval == null) {
            retval = getActiveCall();
        }
        return retval;
    }

    public Call getOutgoingCall() {
        Call call = getFirstCallWithState(Call.State.DIALING);
        if (call == null) {
            call = getFirstCallWithState(Call.State.REDIALING);
        }
        return call;
    }

    public Call getActiveCall() {
        return getFirstCallWithState(Call.State.ACTIVE);
    }

    public Call getBackgroundCall() {
        return getFirstCallWithState(Call.State.ONHOLD);
    }

    public Call getDisconnectedCall() {
        return getFirstCallWithState(Call.State.DISCONNECTED);
    }

    public Call getDisconnectingCall() {
        return getFirstCallWithState(Call.State.DISCONNECTING);
    }

    public Call getSecondBackgroundCall() {
        return getCallWithState(Call.State.ONHOLD, 1);
    }

    public Call getActiveOrBackgroundCall() {
        Call call = getActiveCall();
        if (call == null) {
            call = getBackgroundCall();
        }
        return call;
    }

    public Call getIncomingCall() {
        Call call = getFirstCallWithState(Call.State.INCOMING);
        if (call == null) {
            call = getFirstCallWithState(Call.State.CALL_WAITING);
        }

        return call;
    }

    public Call getFirstCall() {
        Call result = getIncomingCall();
        if (result == null) {
            result = getOutgoingCall();
        }
        if (result == null) {
            result = getFirstCallWithState(Call.State.ACTIVE);
        }
        if (result == null) {
            result = getDisconnectingCall();
        }
        if (result == null) {
            result = getDisconnectedCall();
        }
        return result;
    }

    public Call getCall(int callId) {
        return mCallMap.get(callId);
    }

    public boolean existsLiveCall() {
        for (Call call : mCallMap.values()) {
            if (!isCallDead(call)) {
                return true;
            }
        }
        return false;
    }

    public ArrayList<String> getTextResponses(int callId) {
        return mCallTextReponsesMap.get(callId);
    }

    /**
     * Returns first call found in the call map with the specified state.
     */
    public Call getFirstCallWithState(int state) {
        return getCallWithState(state, 0);
    }

    /**
     * Returns the [position]th call found in the call map with the specified state.
     * TODO: Improve this logic to sort by call time.
     */
    public Call getCallWithState(int state, int positionToFind) {
        Call retval = null;
        int position = 0;
        for (Call call : mCallMap.values()) {
            if (call.getState() == state) {
                if (position >= positionToFind) {
                    retval = call;
                    break;
                } else {
                    position++;
                }
            }
        }

        return retval;
    }

    /**
     * This is called when the service disconnects, either expectedly or unexpectedly.
     * For the expected case, it's because we have no calls left.  For the unexpected case,
     * it is likely a crash of phone and we need to clean up our calls manually.  Without phone,
     * there can be no active calls, so this is relatively safe thing to do.
     */
    public void clearOnDisconnect() {
        for (Call call : mCallMap.values()) {
            final int state = call.getState();
            if (state != Call.State.IDLE &&
                    state != Call.State.INVALID &&
                    state != Call.State.DISCONNECTED) {

                call.setState(Call.State.DISCONNECTED);
                call.setDisconnectCause(DisconnectCause.UNKNOWN);
                updateCallInMap(call);
            }
        }
        notifyListenersOfChange();
    }

    /**
     * Sends a generic notification to all listeners that something has changed.
     * It is up to the listeners to call back to determine what changed.
     */
    private void notifyListenersOfChange() {
        for (Listener listener : mListeners) {
            listener.onCallListChange(this);
        }
    }

    private void notifyListenersOfDisconnect(Call call) {
        for (Listener listener : mListeners) {
            listener.onDisconnect(call);
        }
    }

    /**
     * Updates the call entry in the local map.
     * @return false if no call previously existed and no call was added, otherwise true.
     */
    private boolean updateCallInMap(Call call) {
        Preconditions.checkNotNull(call);

        boolean updated = false;

        final Integer id = new Integer(call.getCallId());

        if (call.getState() == Call.State.DISCONNECTED) {
            // update existing (but do not add!!) disconnected calls
            if (mCallMap.containsKey(id)) {

                // For disconnected calls, we want to keep them alive for a few seconds so that the
                // UI has a chance to display anything it needs when a call is disconnected.

                // Set up a timer to destroy the call after X seconds.
                final Message msg = mHandler.obtainMessage(EVENT_DISCONNECTED_TIMEOUT, call);
                mHandler.sendMessageDelayed(msg, getDelayForDisconnect(call));

                mCallMap.put(id, call);
                updated = true;
            }
        } else if (!isCallDead(call)) {
            mCallMap.put(id, call);
            updated = true;
        } else if (mCallMap.containsKey(id)) {
            mCallMap.remove(id);
            updated = true;
        }

        return updated;
    }

    private int getDelayForDisconnect(Call call) {
        Preconditions.checkState(call.getState() == Call.State.DISCONNECTED);


        final Call.DisconnectCause cause = call.getDisconnectCause();
        final int delay;
        switch (cause) {
            case LOCAL:
                delay = DISCONNECTED_CALL_SHORT_TIMEOUT_MS;
                break;
            case NORMAL:
            case UNKNOWN:
                delay = DISCONNECTED_CALL_MEDIUM_TIMEOUT_MS;
                break;
            case INCOMING_REJECTED:
            case INCOMING_MISSED:
                // no delay for missed/rejected incoming calls
                delay = 0;
                break;
            default:
                delay = DISCONNECTED_CALL_LONG_TIMEOUT_MS;
                break;
        }

        return delay;
    }

    private void updateCallTextMap(Call call, List<String> textResponses) {
        Preconditions.checkNotNull(call);

        final Integer id = new Integer(call.getCallId());

        if (!isCallDead(call)) {
            if (textResponses != null) {
                mCallTextReponsesMap.put(id, (ArrayList<String>) textResponses);
            }
        } else if (mCallMap.containsKey(id)) {
            mCallTextReponsesMap.remove(id);
        }
    }

    private boolean isCallDead(Call call) {
        final int state = call.getState();
        return Call.State.IDLE == state || Call.State.INVALID == state;
    }

    /**
     * Sets up a call for deletion and notifies listeners of change.
     */
    private void finishDisconnectedCall(Call call) {
        call.setState(Call.State.IDLE);
        updateCallInMap(call);
        notifyListenersOfChange();
    }

    /**
     * Handles the timeout for destroying disconnected calls.
     */
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_DISCONNECTED_TIMEOUT:
                    Log.d(this, "EVENT_DISCONNECTED_TIMEOUT ", msg.obj);
                    finishDisconnectedCall((Call) msg.obj);
                    break;
                default:
                    Log.wtf(this, "Message not expected: " + msg.what);
                    break;
            }
        }
    };

    /**
     * Listener interface for any class that wants to be notified of changes
     * to the call list.
     */
    public interface Listener {
        /**
         * Called when a new incoming call comes in.
         * This is the only method that gets called for incoming calls. Listeners
         * that want to perform an action on incoming call should respond in this method
         * because {@link #onCallListChange} does not automatically get called for
         * incoming calls.
         */
        public void onIncomingCall(Call call);

        /**
         * Called anytime there are changes to the call list.  The change can be switching call
         * states, updating information, etc. This method will NOT be called for new incoming
         * calls and for calls that switch to disconnected state. Listeners must add actions
         * to those method implementations if they want to deal with those actions.
         */
        public void onCallListChange(CallList callList);

        /**
         * Called when a call switches to the disconnected state.  This is the only method
         * that will get called upon disconnection.
         */
        public void onDisconnect(Call call);
    }

    public interface CallUpdateListener {
        // TODO: refactor and limit arg to be call state.  Caller info is not needed.
        public void onCallStateChanged(Call call);
    }
}
