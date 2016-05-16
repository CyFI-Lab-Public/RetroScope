/*
 * Copyright (C) 2009 The Android Open Source Project
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

/**
 * Class to internally keep track of Call states to maintain
 * information for Call Waiting and 3Way for CDMA instance of Phone App.
 *
 * Explanation for PhoneApp's Call states and why it is required:
 * IDLE - When no call is going on. This is just required as default state to reset the PhoneApp
 *        call state to when the complete call gets disconnected
 * SINGLE_ACTIVE - When only single call is active.
 *        In normal case(on a single call) this state would be similar for FW's state of ACTIVE
 *        call or phone state of OFFHOOK, but in more complex conditions e.g. when phone is already
 *        in a CONF_CALL state and user rejects a CW, which basically tells the PhoneApp that the
 *        Call is back to a single call, the FW's state still would remain ACTIVE or OFFHOOK and
 *        isGeneric would still be true. At this condition PhoneApp does need to enable the
 *        "Add Call" menu item and disable the "Swap" and "Merge" options
 * THRWAY_ACTIVE - When user initiate an outgoing call when already on a call.
 *        fgCall can have more than one connections from various scenarios (accepting the CW or
 *        making a 3way call) but once we are in this state and one of the parties drops off,
 *        when the user originates another call we need to remember this state to update the menu
 *        items accordingly. FW currently does not differentiate this condition hence PhoneApp
 *        needs to maintain it.
 * CONF_CALL - When the user merges two calls or on accepting the Call waiting call.
 *        This is required cause even though a call might be generic but that does not mean it is
 *        in conference. We can take the same example mention in the SINGLE_ACTIVE state.
 *
 * TODO: Eventually this state information should be maintained by Telephony FW.
 */
   public class CdmaPhoneCallState {

        /**
         * Allowable values for the PhoneCallState.
         *   IDLE - When no call is going on.
         *   SINGLE_ACTIVE - When only single call is active
         *   THRWAY_ACTIVE - When user initiate an outgoing call when already on a call
         *   CONF_CALL - When the user merges two calls or on accepting the Call waiting call
         */
        public enum PhoneCallState {
            IDLE,
            SINGLE_ACTIVE,
            THRWAY_ACTIVE,
            CONF_CALL
        }

        // For storing current and previous PhoneCallState's
        private PhoneCallState mPreviousCallState;
        private PhoneCallState mCurrentCallState;

        // Boolean to track 3Way display state
        private boolean mThreeWayCallOrigStateDialing;

        // Flag to indicate if the "Add Call" menu item in an InCallScreen is OK
        // to be displayed after a Call Waiting call was ignored or timed out
        private boolean mAddCallMenuStateAfterCW;

        /**
         * Initialize PhoneCallState related members - constructor
         */
        public void CdmaPhoneCallStateInit() {
            mCurrentCallState = PhoneCallState.IDLE;
            mPreviousCallState = PhoneCallState.IDLE;
            mThreeWayCallOrigStateDialing = false;
            mAddCallMenuStateAfterCW = true;
        }

        /**
         * Returns the current call state
         */
        public PhoneCallState getCurrentCallState() {
            return mCurrentCallState;
        }

        /**
         * Set current and previous PhoneCallState's
         */
        public void setCurrentCallState(PhoneCallState newState) {
            mPreviousCallState = mCurrentCallState;
            mCurrentCallState = newState;

            //Reset the 3Way display boolean
            mThreeWayCallOrigStateDialing = false;

            //Set mAddCallMenuStateAfterCW to true
            //if the current state is being set to SINGLE_ACTIVE
            //and previous state was IDLE as we could reach the SINGLE_ACTIVE
            //from CW ignore too. For all other cases let the timer or
            //specific calls to setAddCallMenuStateAfterCallWaiting set
            //mAddCallMenuStateAfterCW.
            if ((mCurrentCallState == PhoneCallState.SINGLE_ACTIVE)
                && (mPreviousCallState == PhoneCallState.IDLE)) {
                mAddCallMenuStateAfterCW = true;
            }
        }

        /**
         * Return 3Way display information
         */
        public boolean IsThreeWayCallOrigStateDialing() {
            return mThreeWayCallOrigStateDialing;
        }

        /**
         * Set 3Way display information
         */
        public void setThreeWayCallOrigState(boolean newState) {
            mThreeWayCallOrigStateDialing = newState;
        }

        /**
         * Return information for enabling/disabling "Add Call" menu item
         */
        public boolean getAddCallMenuStateAfterCallWaiting() {
            return mAddCallMenuStateAfterCW;
        }

        /**
         * Set mAddCallMenuStateAfterCW to enabling/disabling "Add Call" menu item
         */
        public void setAddCallMenuStateAfterCallWaiting(boolean newState) {
            mAddCallMenuStateAfterCW = newState;
        }

        /**
         * Return previous PhoneCallState's
         */
        public PhoneCallState getPreviousCallState() {
            return mPreviousCallState;
        }

        /**
         * Reset all PhoneCallState
         */
        public void resetCdmaPhoneCallState() {
            mCurrentCallState = PhoneCallState.IDLE;
            mPreviousCallState = PhoneCallState.IDLE;
            mThreeWayCallOrigStateDialing = false;
            mAddCallMenuStateAfterCW = true;
        }
   }
