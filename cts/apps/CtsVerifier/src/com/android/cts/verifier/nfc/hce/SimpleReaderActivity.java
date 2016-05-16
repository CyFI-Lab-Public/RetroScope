package com.android.cts.verifier.nfc.hce;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.ReaderCallback;
import android.nfc.tech.IsoDep;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.android.cts.verifier.PassFailButtons;
import com.android.cts.verifier.R;

import java.io.IOException;
import java.util.Arrays;

@TargetApi(19)
public class SimpleReaderActivity extends PassFailButtons.Activity implements ReaderCallback {
    public static final String TAG = "SimpleReaderActivity";
    public static final String EXTRA_APDUS = "apdus";
    public static final String EXTRA_RESPONSES = "responses";
    public static final String EXTRA_LABEL = "label";

    NfcAdapter mAdapter;
    String[] mApdus;
    String[] mResponses;

    TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pass_fail_text);
        setPassFailButtonClickListeners();
        getPassButton().setEnabled(false);

        String label = getIntent().getStringExtra(EXTRA_LABEL);
        setTitle(label);

        mAdapter = NfcAdapter.getDefaultAdapter(this);
        mTextView = (TextView) findViewById(R.id.text);
        mTextView.setTextSize(12.0f);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAdapter.enableReaderMode(this, this, NfcAdapter.FLAG_READER_NFC_A |
                NfcAdapter.FLAG_READER_NFC_BARCODE | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK, null);
        Intent intent = getIntent();
        mApdus = intent.getStringArrayExtra(EXTRA_APDUS);
        mResponses = intent.getStringArrayExtra(EXTRA_RESPONSES);
    }

    @Override
    public void onTagDiscovered(Tag tag) {
        final StringBuilder sb = new StringBuilder();
        IsoDep isoDep = IsoDep.get(tag);
        if (isoDep == null) {
            // TODO dialog box
            return;
        }

        try {
            isoDep.connect();
            isoDep.setTimeout(5000);
            int count = 0;
            boolean success = true;
            long startTime = System.currentTimeMillis();
            for (String apdu: mApdus) {
                sb.append("Request APDU:\n");
                sb.append(apdu + "\n\n");
                long apduStartTime = System.currentTimeMillis();
                byte[] response = isoDep.transceive(HceUtils.hexStringToBytes(apdu));
                long apduEndTime = System.currentTimeMillis();
                sb.append("Response APDU (in " + Long.toString(apduEndTime - apduStartTime) +
                        " ms):\n");
                sb.append(HceUtils.getHexBytes(null, response));

                sb.append("\n\n\n");
                boolean wildCard = "*".equals(mResponses[count]);
                byte[] expectedResponse = HceUtils.hexStringToBytes(mResponses[count]);
                Log.d(TAG, HceUtils.getHexBytes("APDU response: ", response));
                if (!wildCard && !Arrays.equals(response, expectedResponse)) {
                    Log.d(TAG, "Unexpected APDU response: " + HceUtils.getHexBytes("", response));
                    success = false;
                    break;
                }
                count++;
            }
            if (success) {
                sb.insert(0, "Total APDU exchange time: " +
                        Long.toString(System.currentTimeMillis() - startTime) + " ms.\n\n");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mTextView.setText(sb.toString());
                        getPassButton().setEnabled(true);
                    }
                });
            } else {
                sb.insert(0, "FAIL. Total APDU exchange time: " +
                        Long.toString(System.currentTimeMillis() - startTime) + " ms.\n\n");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mTextView.setText(sb.toString());
                        AlertDialog.Builder builder = new AlertDialog.Builder(SimpleReaderActivity.this);
                        builder.setTitle("Test failed");
                        builder.setMessage("An unexpected response APDU was received, or no APDUs were received at all.");
                        builder.setPositiveButton("OK", null);
                        builder.show();
                    }
                });
            }
        } catch (IOException e) {
            sb.insert(0, "Test failed. IOException (did you keep the devices in range?)\n\n.");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTextView.setText(sb.toString());
                }
            });
        } finally {
        }
    }
}
