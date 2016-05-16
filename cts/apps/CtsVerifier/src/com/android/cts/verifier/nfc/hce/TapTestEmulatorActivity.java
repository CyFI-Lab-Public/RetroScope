package com.android.cts.verifier.nfc.hce;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.android.cts.verifier.R;
import com.android.cts.verifier.nfc.NfcDialogs;

@TargetApi(19)
public class TapTestEmulatorActivity extends BaseEmulatorActivity {
    TextView mTextView;
    int mNumTaps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pass_fail_text);
        setPassFailButtonClickListeners();
        getPassButton().setEnabled(false);
        mTextView = (TextView) findViewById(R.id.text);
        setupServices(this, TransportService1.COMPONENT);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mNumTaps = 0;
        mTextView.setText("Number of successful taps: 0/50.");
    }

    @Override
    void onServicesSetup(boolean result) {
        NfcDialogs.createHceTapReaderDialog(this,
                getString(R.string.nfc_hce_tap_test_emulator_help)).show();
    }

    public static Intent buildReaderIntent(Context context) {
        Intent readerIntent = new Intent(context, SimpleReaderActivity.class);
        readerIntent.putExtra(SimpleReaderActivity.EXTRA_APDUS,
                TransportService1.APDU_COMMAND_SEQUENCE);
        readerIntent.putExtra(SimpleReaderActivity.EXTRA_RESPONSES,
                TransportService1.APDU_RESPOND_SEQUENCE);
        readerIntent.putExtra(SimpleReaderActivity.EXTRA_LABEL,
                context.getString(R.string.nfc_hce_tap_test_reader));
        return readerIntent;
    }

    @Override
    void onApduSequenceComplete(ComponentName component, long duration) {
        if (component.equals(TransportService1.COMPONENT)) {
            mNumTaps++;
            if (mNumTaps <= 50) {
                mTextView.setText("Number of successful taps: " + Integer.toString(mNumTaps) +
                        "/50.");
            }
            if (mNumTaps >= 50) {
                getPassButton().setEnabled(true);
            }
        }
    }
}
