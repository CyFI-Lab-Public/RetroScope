package com.android.nfc.cardemulation;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import com.android.internal.R;

import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;

public class DefaultRemovedActivity extends AlertActivity implements
        DialogInterface.OnClickListener {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_DeviceDefault_Light_Dialog_Alert);
        super.onCreate(savedInstanceState);

        AlertController.AlertParams ap = mAlertParams;

        ap.mMessage = getString(com.android.nfc.R.string.default_pay_app_removed);
        ap.mNegativeButtonText = getString(R.string.no);
        ap.mPositiveButtonText = getString(R.string.yes);
        ap.mPositiveButtonListener = this;
        setupAlert();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        // Launch into Settings
        Intent intent = new Intent(Settings.ACTION_NFC_PAYMENT_SETTINGS);
        startActivity(intent);
    }
}