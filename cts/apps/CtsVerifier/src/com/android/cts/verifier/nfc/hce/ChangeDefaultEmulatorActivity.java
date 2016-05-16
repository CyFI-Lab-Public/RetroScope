package com.android.cts.verifier.nfc.hce;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.nfc.cardemulation.CardEmulation;
import android.os.Bundle;

import com.android.cts.verifier.R;
import com.android.cts.verifier.nfc.NfcDialogs;

@TargetApi(19)
public class ChangeDefaultEmulatorActivity extends BaseEmulatorActivity implements OnClickListener {
    final static int STATE_IDLE = 0;
    final static int STATE_SERVICE1_SETTING_UP = 1;
    final static int STATE_SERVICE2_SETTING_UP = 2;
    final static int STATE_DEFAULT_CHANGED = 3;

    boolean mReceiverRegistered = false;
    int mState = STATE_IDLE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pass_fail_text);
        setPassFailButtonClickListeners();
        getPassButton().setEnabled(false);

        mState = STATE_SERVICE2_SETTING_UP;
        setupServices(this, PaymentService2.COMPONENT);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    void onServicesSetup(boolean result) {
        if (mState == STATE_SERVICE2_SETTING_UP) {
            mState = STATE_SERVICE1_SETTING_UP;
            setupServices(this, PaymentService1.COMPONENT, PaymentService2.COMPONENT);
            return;
        }
        // Verify HCE service 2 is the default
        if (!mCardEmulation.isDefaultServiceForCategory(
                PaymentService2.COMPONENT, CardEmulation.CATEGORY_PAYMENT)) {
            // Popup dialog-box, fail test
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Test failed.");
            builder.setMessage("PaymentService2 is not the default service according " +
                    "to CardEmulation.getDefaultServiceForCategory(). Do you have" +
                    "another Payment application installed?");
            builder.setPositiveButton("OK", null);
            builder.show();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Note");
            builder.setMessage(R.string.nfc_hce_change_default_help);
            builder.setPositiveButton("OK", this);
            builder.show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mReceiverRegistered) {
            unregisterReceiver(mReceiver);
        }
    }

    public static Intent buildReaderIntent(Context context) {
        Intent readerIntent = new Intent(context, SimpleReaderActivity.class);
        readerIntent.putExtra(SimpleReaderActivity.EXTRA_APDUS,
                PaymentService1.APDU_COMMAND_SEQUENCE);
        readerIntent.putExtra(SimpleReaderActivity.EXTRA_RESPONSES,
                PaymentService1.APDU_RESPOND_SEQUENCE);
        readerIntent.putExtra(SimpleReaderActivity.EXTRA_LABEL,
                context.getString(R.string.nfc_hce_change_default_reader));
        return readerIntent;
    }

    @Override
    void onApduSequenceComplete(ComponentName component, long duration) {
        if (component.equals(PaymentService1.COMPONENT)) {
            getPassButton().setEnabled(true);
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        Intent changeDefault = new Intent(CardEmulation.ACTION_CHANGE_DEFAULT);
        changeDefault.putExtra(CardEmulation.EXTRA_CATEGORY, CardEmulation.CATEGORY_PAYMENT);
        changeDefault.putExtra(CardEmulation.EXTRA_SERVICE_COMPONENT, PaymentService1.COMPONENT);
        startActivityForResult(changeDefault, 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mState = STATE_DEFAULT_CHANGED;
        NfcDialogs.createHceTapReaderDialog(this, null).show();
    }
}
