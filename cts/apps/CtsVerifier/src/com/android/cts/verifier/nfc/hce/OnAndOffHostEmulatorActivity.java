package com.android.cts.verifier.nfc.hce;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.android.cts.verifier.R;
import com.android.cts.verifier.nfc.NfcDialogs;

@TargetApi(19)
public class OnAndOffHostEmulatorActivity extends BaseEmulatorActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pass_fail_text);
        setPassFailButtonClickListeners();
        getPassButton().setEnabled(false);
        setupServices(this, OffHostService.COMPONENT, AccessService.COMPONENT);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    void onServicesSetup(boolean result) {
        NfcDialogs.createHceTapReaderDialog(this, getString(R.string.nfc_hce_on_and_offhost_emulator_help)).show();
    }

    public static Intent buildReaderIntent(Context context) {
        // Combine command/response APDU arrays
        String[] commandSequences = new String[OffHostService.APDU_COMMAND_SEQUENCE.length +
                                               AccessService.APDU_COMMAND_SEQUENCE.length];
        System.arraycopy(OffHostService.APDU_COMMAND_SEQUENCE, 0, commandSequences, 0,
                OffHostService.APDU_COMMAND_SEQUENCE.length);
        System.arraycopy(AccessService.APDU_COMMAND_SEQUENCE, 0, commandSequences,
                OffHostService.APDU_COMMAND_SEQUENCE.length,
                AccessService.APDU_COMMAND_SEQUENCE.length);

        String[] responseSequences = new String[OffHostService.APDU_RESPOND_SEQUENCE.length +
                                               AccessService.APDU_RESPOND_SEQUENCE.length];
        System.arraycopy(OffHostService.APDU_RESPOND_SEQUENCE, 0, responseSequences, 0,
                OffHostService.APDU_RESPOND_SEQUENCE.length);
        System.arraycopy(AccessService.APDU_RESPOND_SEQUENCE, 0, responseSequences,
                OffHostService.APDU_RESPOND_SEQUENCE.length,
                AccessService.APDU_RESPOND_SEQUENCE.length);

        Intent readerIntent = new Intent(context, SimpleReaderActivity.class);
        readerIntent.putExtra(SimpleReaderActivity.EXTRA_APDUS,
                commandSequences);
        readerIntent.putExtra(SimpleReaderActivity.EXTRA_RESPONSES,
                responseSequences);
        readerIntent.putExtra(SimpleReaderActivity.EXTRA_LABEL,
                context.getString(R.string.nfc_hce_on_and_offhost_service_reader));
        return readerIntent;
    }

    @Override
    void onApduSequenceComplete(ComponentName component, long duration) {
        if (component.equals(AccessService.COMPONENT)) {
            getPassButton().setEnabled(true);
        }
    }
}
