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

package com.android.phone;

/**
 * App-wide constants and enums for the phone app.
 *
 * Any constants that need to be shared between two or more classes within
 * the com.android.phone package should be defined here.  (Constants that
 * are private to only one class can go in that class's .java file.)
 */
public class Constants {

    /**
     * Complete list of error / diagnostic indications we might possibly
     * need to present to the user.
     *
     * This enum is basically a high-level list of the kinds of failures
     * or "exceptional conditions" that can occur when making a phone
     * call.  When an error occurs, the CallController stashes away one of
     * these codes in the InCallUiState.pendingCallStatusCode flag and
     * launches the InCallScreen; the InCallScreen will then display some
     * kind of message to the user (usually an error dialog) explaining
     * what happened.
     *
     * The enum values here cover all possible result status / error
     * conditions that can happen when attempting to place an outgoing
     * call (see CallController.placeCall() and placeCallInternal()), as
     * well as some other conditions (like CDMA_CALL_LOST and EXITED_ECM)
     * that don't technically result from the placeCall() sequence but
     * still need to be communicated to the user.
     */
    public enum CallStatusCode {
        /**
         * No error or exceptional condition occurred.
         * The InCallScreen does not need to display any kind of alert to the user.
         */
        SUCCESS,

        /**
         * Radio is explictly powered off, presumably because the
         * device is in airplane mode.
         */
        POWER_OFF,

        /**
         * Only emergency numbers are allowed, but we tried to dial
         * a non-emergency number.
         */
        EMERGENCY_ONLY,

        /**
         * No network connection.
         */
        OUT_OF_SERVICE,

        /**
         * The supplied CALL Intent didn't contain a valid phone number.
         */
        NO_PHONE_NUMBER_SUPPLIED,

        /**
         * Our initial phone number was actually an MMI sequence.
         */
        DIALED_MMI,

        /**
         * We couldn't successfully place the call due to an
         * unknown failure in the telephony layer.
         */
        CALL_FAILED,

        /**
         * We tried to call a voicemail: URI but the device has no
         * voicemail number configured.
         *
         * When InCallUiState.pendingCallStatusCode is set to this
         * value, the InCallScreen will bring up a UI explaining what
         * happened, and allowing the user to go into Settings to fix the
         * problem.
         */
        VOICEMAIL_NUMBER_MISSING,

        /**
         * This status indicates that InCallScreen should display the
         * CDMA-specific "call lost" dialog.  (If an outgoing call fails,
         * and the CDMA "auto-retry" feature is enabled, *and* the retried
         * call fails too, we display this specific dialog.)
         *
         * TODO: this is currently unused, since the "call lost" dialog
         * needs to be triggered by a *disconnect* event, rather than when
         * the InCallScreen first comes to the foreground.  For now we use
         * the needToShowCallLostDialog field for this (see below.)
         */
        CDMA_CALL_LOST,

        /**
         * This status indicates that the call was placed successfully,
         * but additionally, the InCallScreen needs to display the
         * "Exiting ECM" dialog.
         *
         * (Details: "Emergency callback mode" is a CDMA-specific concept
         * where the phone disallows data connections over the cell
         * network for some period of time after you make an emergency
         * call.  If the phone is in ECM and you dial a non-emergency
         * number, that automatically *cancels* ECM, but we additionally
         * need to warn the user that ECM has been canceled (see bug
         * 4207607.))
         */
        EXITED_ECM
    }

    //
    // URI schemes
    //

    public static final String SCHEME_SIP = "sip";
    public static final String SCHEME_SMS = "sms";
    public static final String SCHEME_SMSTO = "smsto";
    public static final String SCHEME_TEL = "tel";
    public static final String SCHEME_VOICEMAIL = "voicemail";

    //
    // TODO: Move all the various EXTRA_* and intent action constants here too.
    // (Currently they're all over the place: InCallScreen,
    // OutgoingCallBroadcaster, OtaUtils, etc.)
    //

    // Dtmf tone type setting value for CDMA phone
    public static final int DTMF_TONE_TYPE_NORMAL = 0;
    public static final int DTMF_TONE_TYPE_LONG   = 1;
}
