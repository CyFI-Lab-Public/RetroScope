package com.android.cts.verifier.nfc.hce;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.android.cts.verifier.R;
import com.android.cts.verifier.nfc.NfcDialogs;

public class DualNonPaymentEmulatorActivity extends BaseEmulatorActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pass_fail_text);
        setPassFailButtonClickListeners();
        getPassButton().setEnabled(false);
        setupServices(this, TransportService2.COMPONENT, AccessService.COMPONENT);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    void onServicesSetup(boolean result) {
        NfcDialogs.createHceTapReaderDialog(this, null).show();
    }

    public static Intent buildReaderIntent(Context context) {
        Intent readerIntent = new Intent(context, SimpleReaderActivity.class);
        // Combine command/response APDU arrays
        String[] commandSequences = new String[TransportService2.APDU_COMMAND_SEQUENCE.length +
                                               AccessService.APDU_COMMAND_SEQUENCE.length];
        System.arraycopy(TransportService2.APDU_COMMAND_SEQUENCE, 0, commandSequences, 0,
                TransportService2.APDU_COMMAND_SEQUENCE.length);
        System.arraycopy(AccessService.APDU_COMMAND_SEQUENCE, 0, commandSequences,
                TransportService2.APDU_COMMAND_SEQUENCE.length,
                AccessService.APDU_COMMAND_SEQUENCE.length);

        String[] responseSequences = new String[TransportService2.APDU_RESPOND_SEQUENCE.length +
                                               AccessService.APDU_RESPOND_SEQUENCE.length];
        System.arraycopy(TransportService2.APDU_RESPOND_SEQUENCE, 0, responseSequences, 0,
                TransportService2.APDU_RESPOND_SEQUENCE.length);
        System.arraycopy(AccessService.APDU_RESPOND_SEQUENCE, 0, responseSequences,
                TransportService2.APDU_RESPOND_SEQUENCE.length,
                AccessService.APDU_RESPOND_SEQUENCE.length);

        readerIntent.putExtra(SimpleReaderActivity.EXTRA_APDUS, commandSequences);
        readerIntent.putExtra(SimpleReaderActivity.EXTRA_RESPONSES, responseSequences);
        readerIntent.putExtra(SimpleReaderActivity.EXTRA_LABEL,
                context.getString(R.string.nfc_hce_dual_non_payment_reader));
        return readerIntent;
    }

    @Override
    void onApduSequenceComplete(ComponentName component, long duration) {
        if (component.equals(TransportService2.COMPONENT)) {
            getPassButton().setEnabled(true);
        }
    }
}