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

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.TelephonyCapabilities;

/**
 * Invisible activity that handles the com.android.phone.PERFORM_CDMA_PROVISIONING intent.
 * This activity is protected by the android.permission.PERFORM_CDMA_PROVISIONING permission.
 *
 * We handle the PERFORM_CDMA_PROVISIONING action by launching an OTASP
 * call via one of the OtaUtils helper methods: startInteractiveOtasp() on
 * regular phones, or startNonInteractiveOtasp() on data-only devices.
 *
 * TODO: The class name InCallScreenShowActivation is misleading, since
 * this activity is totally unrelated to the InCallScreen (which
 * implements the in-call UI.)  Let's eventually rename this to something
 * like CdmaProvisioningLauncher or CdmaProvisioningHandler...
 */
public class InCallScreenShowActivation extends Activity {
    private static final String LOG_TAG = "InCallScreenShowActivation";
    private static final boolean DBG =
            (PhoneGlobals.DBG_LEVEL >= 1) && (SystemProperties.getInt("ro.debuggable", 0) == 1);

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Intent intent = getIntent();
        if (DBG) Log.d(LOG_TAG, "onCreate: intent = " + intent);
        Bundle extras = intent.getExtras();
        if (DBG && (extras != null)) {
            Log.d(LOG_TAG, "      - has extras: size = " + extras.size()); // forces an unparcel()
            Log.d(LOG_TAG, "      - extras = " + extras);
        }

        PhoneGlobals app = PhoneGlobals.getInstance();
        Phone phone = app.getPhone();
        if (!TelephonyCapabilities.supportsOtasp(phone)) {
            Log.w(LOG_TAG, "CDMA Provisioning not supported on this device");
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        if (intent.getAction().equals(OtaUtils.ACTION_PERFORM_CDMA_PROVISIONING)) {

            boolean usesHfa = getResources().getBoolean(R.bool.config_use_hfa_for_provisioning);
            if (usesHfa) {
                Log.d(LOG_TAG, "Starting Hfa from ACTION_PERFORM_CDMA_PROVISIONING");
                startHfa();
                finish();
                return;
            }

            // On voice-capable devices, we perform CDMA provisioning in
            // "interactive" mode by directly launching the InCallScreen.
            // boolean interactiveMode = PhoneGlobals.sVoiceCapable;
            // TODO: Renable interactive mode for device provisioning.
            boolean interactiveMode = false;
            Log.d(LOG_TAG, "ACTION_PERFORM_CDMA_PROVISIONING (interactiveMode = "
                  + interactiveMode + ")...");

            // Testing: this intent extra allows test apps manually
            // enable/disable "interactive mode", regardless of whether
            // the current device is voice-capable.  This is allowed only
            // in userdebug or eng builds.
            if (intent.hasExtra(OtaUtils.EXTRA_OVERRIDE_INTERACTIVE_MODE)
                    && (SystemProperties.getInt("ro.debuggable", 0) == 1)) {
                interactiveMode =
                        intent.getBooleanExtra(OtaUtils.EXTRA_OVERRIDE_INTERACTIVE_MODE, false);
                Log.d(LOG_TAG, "===> MANUALLY OVERRIDING interactiveMode to " + interactiveMode);
            }

            // We allow the caller to pass a PendingIntent (as the
            // EXTRA_NONINTERACTIVE_OTASP_RESULT_PENDING_INTENT extra)
            // which we'll later use to notify them when the OTASP call
            // fails or succeeds.
            //
            // Stash that away here, and we'll fire it off later in
            // OtaUtils.sendOtaspResult().
            app.cdmaOtaScreenState.otaspResultCodePendingIntent =
                        (PendingIntent) intent.getParcelableExtra(
                                OtaUtils.EXTRA_OTASP_RESULT_CODE_PENDING_INTENT);

            if (interactiveMode) {
                // On voice-capable devices, launch an OTASP call and arrange
                // for the in-call UI to come up.  (The InCallScreen will
                // notice that an OTASP call is active, and display the
                // special OTASP UI instead of the usual in-call controls.)

                if (DBG) Log.d(LOG_TAG, "==> Starting interactive CDMA provisioning...");
                OtaUtils.startInteractiveOtasp(this);

                // The result we set here is actually irrelevant, since the
                // InCallScreen's "interactive" OTASP sequence never actually
                // finish()es; it ends by directly launching the Home
                // activity.  So our caller won't actually ever get an
                // onActivityResult() call in this case.
                setResult(OtaUtils.RESULT_INTERACTIVE_OTASP_STARTED);
            } else {
                // On data-only devices, manually launch the OTASP call
                // *without* displaying any UI.  (Our caller, presumably
                // SetupWizardActivity, is responsible for displaying some
                // sort of progress UI.)

                if (DBG) Log.d(LOG_TAG, "==> Starting non-interactive CDMA provisioning...");
                int callStatus = OtaUtils.startNonInteractiveOtasp(this);

                if (callStatus == PhoneUtils.CALL_STATUS_DIALED) {
                    if (DBG) Log.d(LOG_TAG, "  ==> successful result from startNonInteractiveOtasp(): "
                          + callStatus);
                    setResult(OtaUtils.RESULT_NONINTERACTIVE_OTASP_STARTED);
                } else {
                    Log.w(LOG_TAG, "Failure code from startNonInteractiveOtasp(): " + callStatus);
                    setResult(OtaUtils.RESULT_NONINTERACTIVE_OTASP_FAILED);
                }
            }
        } else {
            Log.e(LOG_TAG, "Unexpected intent action: " + intent);
            setResult(RESULT_CANCELED);
        }

        finish();
    }

    /**
     * On devices that provide a phone initialization wizard (such as Google Setup Wizard),
     * the wizard displays it's own activation UI. The Hfa activation started by this class
     * will show a UI or not depending on the status of the setup wizard. If the setup wizard
     * is running, do not show a UI, otherwise show our own UI since setup wizard will not.
     *
     * The method checks two properties:
     * 1. Does the device require a setup wizard (ro.setupwizard.mode == (REQUIRED|OPTIONAL))
     * 2. Is device_provisioned set to non-zero--a property that setup wizard sets at completion.
     * @return true if wizard is running, false otherwise.
     */
    private boolean isWizardRunning(Context context) {
        Intent intent = new Intent("android.intent.action.DEVICE_INITIALIZATION_WIZARD");
        ResolveInfo resolveInfo = context.getPackageManager().resolveActivity(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        boolean provisioned = Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 0) != 0;
        String mode = SystemProperties.get("ro.setupwizard.mode", "REQUIRED");
        boolean runningSetupWizard = "REQUIRED".equals(mode) || "OPTIONAL".equals(mode);
        if (DBG) {
            Log.v(LOG_TAG, "resolvInfo = " + resolveInfo + ", provisioned = " + provisioned
                    + ", runningSetupWizard = " + runningSetupWizard);
        }
        return resolveInfo != null && !provisioned && runningSetupWizard;
    }

    /**
     * Starts the HFA provisioning process by bringing up the HFA Activity.
     */
    private void startHfa() {
        final Intent intent = new Intent();

        final PendingIntent otaResponseIntent = getIntent().getParcelableExtra(
                OtaUtils.EXTRA_OTASP_RESULT_CODE_PENDING_INTENT);

        final boolean showUi = !isWizardRunning(this);

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (otaResponseIntent != null) {
            intent.putExtra(OtaUtils.EXTRA_OTASP_RESULT_CODE_PENDING_INTENT, otaResponseIntent);
        }

        Log.v(LOG_TAG, "Starting hfa activation activity");
        if (showUi) {
            intent.setClassName(this, HfaActivity.class.getName());
            startActivity(intent);
        } else {
            intent.setClassName(this, HfaService.class.getName());
            startService(intent);
        }

        setResult(RESULT_OK);
    }
}
