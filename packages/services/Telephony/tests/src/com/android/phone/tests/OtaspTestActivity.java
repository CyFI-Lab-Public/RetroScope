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

package com.android.phone.tests;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.phone.OtaUtils;

/**
 * Test activity that mimics the PERFORM_CDMA_PROVISIONING behavior of
 * SetupWizard, useful for testing "non-interactive" OTASP.
 * @see OtaUtils.startNonInteractiveOtasp
 *
 */
public class OtaspTestActivity extends Activity implements View.OnClickListener {
    private static final String LOG_TAG = "OtaspTestActivity";

    // Request code used with startActivityForResult()
    private static final int PERFORM_CDMA_PROVISIONING_REQUEST_CODE = 1;

    // UI elements
    private TextView mLabel;
    private ProgressBar mProgressBar;
    private TextView mResult;
    private Button mButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent intent = getIntent();
        Log.i(LOG_TAG, "##### onCreate: intent = " + intent);
        Bundle extras = intent.getExtras();
        if (extras != null) {
            Log.i(LOG_TAG, "      - has extras: size = " + extras.size()); // forces an unparcel()
            Log.i(LOG_TAG, "      - extras = " + extras);
        }

        // Construct our basic UI:
        super.onCreate(savedInstanceState);
        setContentView(R.layout.otasp_test_activity);

        mLabel = (TextView) findViewById(R.id.label1);
        mLabel.setText("OTA Test Activity");

        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        mResult = (TextView) findViewById(R.id.result1);

        mButton = (Button) findViewById(R.id.button1);
        mButton.setText("Make test call");
        mButton.setOnClickListener(this);


        // We can be launched either:
        //
        // (1) Directly from the launcher, in which case the current intent
        //     will simply be an ACTION_MAIN intent
        //
        // (2) Via the PendingIntent that we sent along (when we originally
        //     fired off the ACTION_PERFORM_CDMA_PROVISIONING intent) that
        //     allows the phone app to send us back a result code.
        //     We can identify this case by the presence of the
        //     EXTRA_OTASP_RESULT_CODE extra.

        if (intent.hasExtra(OtaUtils.EXTRA_OTASP_RESULT_CODE)) {
            // Got a result from the OTASP call!
            Log.i(LOG_TAG, "==> onCreate: got a result from the OTASP call!");

            int resultCode = intent.getIntExtra(OtaUtils.EXTRA_OTASP_RESULT_CODE,
                                                OtaUtils.OTASP_UNKNOWN);
            Log.i(LOG_TAG, "    - resultCode = " + resultCode);

            String resultString;
            switch (resultCode) {
                case OtaUtils.OTASP_USER_SKIPPED:
                    resultString = "User skipped!";
                    break;
                case OtaUtils.OTASP_SUCCESS:
                    resultString = "Success!";
                    break;
                case OtaUtils.OTASP_FAILURE:
                    resultString = "FAILURE";
                    break;
                default:
                    resultString = "Unexpected code: " + resultCode;
                    break;
            }
            Log.i(LOG_TAG, "    - result: " + resultString);
            mResult.setText(resultString);
            mResult.setVisibility(View.VISIBLE);
            mProgressBar.setVisibility(View.INVISIBLE);

        } else {
            // We must have gotten here directly from the launcher.
            // Leave the UI in its initial state.
            Log.i(LOG_TAG, "==> onCreate: entered from the launcher.");
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.i(LOG_TAG, "onNewIntent: intent=" + intent);
        Bundle extras = intent.getExtras();
        if (extras != null) Log.i(LOG_TAG, "      - intent extras = " + extras);

        // This method isn't actually used since this test activity is not
        // launched in "singleTop" mode.

        // Activities that *are* launched in singleTop mode, like the SetupWizard,
        // would have to handle the various PendingIntents here just like
        // we do above in onCreate().
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(LOG_TAG, "onActivityResult: request " + requestCode
              + " result " + resultCode + " data " + data);

        // Note we receive this call immediately before onResume(), when
        // we get re-started after launching the PERFORM_CDMA_PROVISIONING
        // intent.

        if (requestCode == PERFORM_CDMA_PROVISIONING_REQUEST_CODE) {
            // The InCallScreenShowActivation activity can set the following
            // result codes:
            //
            //   RESULT_INTERACTIVE_OTASP_STARTED
            //   RESULT_NONINTERACTIVE_OTASP_STARTED
            //   RESULT_NONINTERACTIVE_OTASP_FAILED
            //
            // but note that in practice we won't ever *get* the
            // RESULT_INTERACTIVE_OTASP_STARTED result code, since the
            // "interactive" OTASP sequence never actually finish()es;
            // it ends by directly launching the Home activity.
            //
            // However, in non-interactive OTASP, the
            // InCallScreenShowActivation activity will set one of the
            // RESULT_NONINTERACTIVE_* codes and immediately
            // finish(), so we *will* see that result here.
            //
            // Also, resultCode will be RESULT_CANCELED (= 0) if the
            // InCallScreenShowActivation activity didn't return any
            // result, or crashed.

            switch (resultCode) {
                case OtaUtils.RESULT_INTERACTIVE_OTASP_STARTED:
                    Log.i(LOG_TAG, "==> onActivityResult: INTERACTIVE_OTASP_STARTED");
                    break;
                case OtaUtils.RESULT_NONINTERACTIVE_OTASP_STARTED:
                    Log.i(LOG_TAG, "==> onActivityResult: NONINTERACTIVE_OTASP_STARTED");
                    break;
                case OtaUtils.RESULT_NONINTERACTIVE_OTASP_FAILED:
                    Log.w(LOG_TAG, "==> onActivityResult: NONINTERACTIVE_OTASP_FAILED");
                    // This means we couldn't even *initiate* an outgoing call
                    // to start the OTASP process.  Not sure what could cause this.
                    // TODO: Update UI to indicate the error.
                    break;
                case RESULT_CANCELED:
                    Log.i(LOG_TAG, "==> onActivityResult: CANCELED");
                    break;
                default:
                    Log.i(LOG_TAG, "==> onActivityResult: unknown result: " + resultCode);
                    break;
            }
        }
    }

    @Override
    protected void onResume() {
        Log.i(LOG_TAG, "onResume()...");
        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.i(LOG_TAG, "onPause()...");
        super.onPause();
    }

    // View.OnClickListener implementation
    @Override
    public void onClick(View view) {
        int id = view.getId();
        Log.i(LOG_TAG, "onClick(View " + view + ", id " + id + ")...");

        switch (id) {
            case R.id.button1:
                Log.i(LOG_TAG, "onClick: button1...");
                makeTestCall();
                break;
            default:
                Log.w(LOG_TAG, "onClick: unexpected View: " + view);
                break;
        }
    }

    private void makeTestCall() {
        Log.i(LOG_TAG, "##### makeTestCall()...");

        mProgressBar.setVisibility(View.VISIBLE);
        mResult.setVisibility(View.INVISIBLE);

        try {
            Intent performProvisioningIntent =
                    new Intent(OtaUtils.ACTION_PERFORM_CDMA_PROVISIONING);

            // Set the magic extra to force "non-interactive mode" for the
            // OTASP call.
            performProvisioningIntent.putExtra(OtaUtils.EXTRA_OVERRIDE_INTERACTIVE_MODE, false);

            // Pass a PendingIntent along with the
            // ACTION_PERFORM_CDMA_PROVISIONING intent, which allows
            // results to be sent back to us.
            Intent resultIntent = new Intent(this, this.getClass());
            PendingIntent pendingResultIntent =
                    PendingIntent.getActivity(this, 0,
                                              resultIntent, 0);
            performProvisioningIntent.putExtra(OtaUtils.EXTRA_OTASP_RESULT_CODE_PENDING_INTENT,
                                               pendingResultIntent);

            Log.i(LOG_TAG, "- Firing off PERFORM_CDMA_PROVISIONING intent: "
                  + performProvisioningIntent);
            Bundle extras = performProvisioningIntent.getExtras();
            if (extras != null) Log.i(LOG_TAG, "      - intent extras = " + extras);

            // Originally, we would simply call
            //     startActivity(performProvisioningIntent);
            // to launch the InCallScreenShowActivation activity and *not* expect
            // a result.  (Note that calling the plain startActivity()
            // method *guarantees* that your onActivityResult() method
            // will NOT be called at all.)

            // Now, we ask for a result:
            startActivityForResult(performProvisioningIntent,
                                   PERFORM_CDMA_PROVISIONING_REQUEST_CODE);

            // On a non-voice-capable device, the InCallScreenShowActivation activity
            // will kick off the OTASP call and immediately return, passing
            // the code RESULT_STARTED_NONINTERACTIVE_OTASP to our
            // onActivityResult method.

        } catch (ActivityNotFoundException e) {
            Log.w(LOG_TAG, "Couldn't show activiation UI; ActivityNotFoundException: " + e);
        }
    }
}
