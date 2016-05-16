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
public class ThroughputEmulatorActivity extends BaseEmulatorActivity {
    TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pass_fail_text);
        setPassFailButtonClickListeners();
        getPassButton().setEnabled(false);
        mTextView = (TextView) findViewById(R.id.text);
        setupServices(this, ThroughputService.COMPONENT);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    void onServicesSetup(boolean result) {
        NfcDialogs.createHceTapReaderDialog(this,
                getString(R.string.nfc_hce_throughput_emulator_help)).show();
    }

    public static Intent buildReaderIntent(Context context) {
        Intent readerIntent = new Intent(context, SimpleReaderActivity.class);
        readerIntent.putExtra(SimpleReaderActivity.EXTRA_APDUS,
                ThroughputService.APDU_COMMAND_SEQUENCE);
        readerIntent.putExtra(SimpleReaderActivity.EXTRA_RESPONSES,
                ThroughputService.APDU_RESPOND_SEQUENCE);
        readerIntent.putExtra(SimpleReaderActivity.EXTRA_LABEL,
                context.getString(R.string.nfc_hce_throughput_reader));
        return readerIntent;
    }

    @Override
    void onApduSequenceComplete(ComponentName component, long duration) {
        if (component.equals(ThroughputService.COMPONENT)) {
            long timePerApdu = duration / ThroughputService.APDU_COMMAND_SEQUENCE.length;
            if (duration < 1000) {
                mTextView.setText("PASS. Total duration: " + Long.toString(duration) + " ms " +
                        "( " + Long.toString(timePerApdu) + " ms per APDU roundtrip).");
                getPassButton().setEnabled(true);
            } else {
                mTextView.setText("FAIL. Total duration: " + Long.toString(duration) + " ms " +
                        "(" + Long.toString(timePerApdu) + " ms per APDU roundtrip)." +
                        " Require <= 60ms per APDU roundtrip.");
            }
        }
    }
}
