package com.android.cts.verifier.nfc.hce;

import android.content.ComponentName;
import android.os.Bundle;

import com.android.cts.verifier.R;
import com.android.cts.verifier.nfc.NfcDialogs;

public class ProtocolParamsEmulatorActivity extends BaseEmulatorActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pass_fail_text);
        setPassFailButtonClickListeners();
        getPassButton().setEnabled(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        NfcDialogs.createHceTapReaderDialog(this,
                getString(R.string.nfc_hce_protocol_params_emulator_help)).show();
    }

    @Override
    void onServicesSetup(boolean result) {
    }

    @Override
    void onApduSequenceComplete(ComponentName component, long duration) {
    }
}
