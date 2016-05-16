package com.android.cts.verifier.nfc.hce;

import android.annotation.TargetApi;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.ReaderCallback;
import android.nfc.tech.IsoDep;
import android.nfc.tech.NfcA;
import android.nfc.Tag;
import android.os.Bundle;
import android.widget.TextView;

import com.android.cts.verifier.PassFailButtons;
import com.android.cts.verifier.R;

import java.io.IOException;

@TargetApi(19)
public class ProtocolParamsReaderActivity extends PassFailButtons.Activity implements ReaderCallback {
    public static final String TAG = "ProtocolParamsReaderActivity";

    NfcAdapter mAdapter;

    TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pass_fail_text);
        setPassFailButtonClickListeners();

        setTitle(R.string.nfc_hce_protocol_params_reader);

        mAdapter = NfcAdapter.getDefaultAdapter(this);
        mTextView = (TextView) findViewById(R.id.text);
        mTextView.setTextSize(12.0f);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAdapter.enableReaderMode(this, this, NfcAdapter.FLAG_READER_NFC_A |
                NfcAdapter.FLAG_READER_NFC_BARCODE | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK, null);
    }

    public boolean parseProtocolParameters(StringBuilder sb, byte[] uid,
            short sak, byte[] atqa, byte[] ats) {

        boolean success = true;

        sb.append("UID: " + HceUtils.getHexBytes(null, uid) + "\n\n");
        sb.append("SAK: 0x" + Integer.toHexString(sak & 0xFF) + "\n");

        if ((sak & 0x20) != 0) {
            sb.append("    (OK) ISO-DEP bit (0x20) is set.\n");
        } else {
            success = false;
            sb.append("    (FAIL) ISO-DEP bit (0x20) is NOT set.\n");
        }

        if ((sak & 0x40) != 0) {
            sb.append("    (OK) P2P bit (0x40) is set.\n");
        } else {
            sb.append("    (WARN) P2P bit (0x40) is NOT set.\n");
        }

        sb.append("\n");
        sb.append("ATQA: " + HceUtils.getHexBytes(null, atqa) + "\n");
        sb.append("\n");

        sb.append("ATS: " + HceUtils.getHexBytes(null, ats) + "\n");
        sb.append("    TL: 0x" + Integer.toHexString(ats[0] & 0xFF) + "\n");
        sb.append("    T0: 0x" + Integer.toHexString(ats[1] & 0xFF) + "\n");

        boolean ta_present = false;
        boolean tb_present = false;
        boolean tc_present = false;
        int atsIndex = 1;
        if ((ats[atsIndex] & 0x40) != 0) {
            sb.append("        (OK) T(C) is present (bit 7 is set).\n");
            tc_present = true;
        } else {
            success = false;
            sb.append("        (FAIL) T(C) is not present (bit 7 is NOT set).\n");
        }
        if ((ats[atsIndex] & 0x20) != 0) {
            sb.append("        (OK) T(B) is present (bit 6 is set).\n");
            tb_present = true;
        } else {
            success = false;
            sb.append("        (FAIL) T(B) is not present (bit 6 is NOT set).\n");
        }
        if ((ats[atsIndex] & 0x10) != 0) {
            sb.append("        (OK) T(A) is present (bit 5 is set).\n");
            ta_present = true;
        } else {
            success = false;
            sb.append("        (FAIL) T(A) is not present (bit 5 is NOT set).\n");
        }
        int fsc = ats[atsIndex] & 0x0F;
        if (fsc > 8) {
            success = false;
            sb.append("        (FAIL) FSC " + Integer.toString(fsc) + " is > 8\n");
        } else if (fsc < 2) {
            sb.append("        (FAIL EMVCO) FSC " + Integer.toString(fsc) + " is < 2\n");
        } else {
            sb.append("        (OK) FSC = " + Integer.toString(fsc) + "\n");
        }

        atsIndex++;
        if (ta_present) {
            sb.append("    TA: 0x" + Integer.toHexString(ats[atsIndex] & 0xff) + "\n");
            if ((ats[atsIndex] & 0x80) != 0) {
                sb.append("        (OK) bit 8 set, indicating only same bit rate divisor.\n");
            } else {
                sb.append("        (FAIL EMVCO) bit 8 NOT set, indicating support for assymetric " +
                        "bit rate divisors. EMVCo requires bit 8 set.\n");
            }
            if ((ats[atsIndex] & 0x70) != 0) {
                sb.append("        (FAIL EMVCO) EMVCo requires bits 7 to 5 set to 0.\n");
            } else {
                sb.append("        (OK) bits 7 to 5 indicating only 106 kbit/s L->P supported.\n");
            }
            if ((ats[atsIndex] & 0x7) != 0) {
                sb.append("        (FAIL EMVCO) EMVCo requires bits 3 to 1 set to 0.\n");
            } else {
                sb.append("        (OK) bits 3 to 1 indicating only 106 kbit/s P->L supported.\n");
            }
            atsIndex++;
        }

        if (tb_present) {
            sb.append("    TB: 0x" + Integer.toHexString(ats[3] & 0xFF) + "\n");
            int fwi = (ats[atsIndex] & 0xF0) >> 4;
            if (fwi > 8) {
                success = false;
                sb.append("        (FAIL) FWI=" + Integer.toString(fwi) + ", should be <= 8\n");
            } else if (fwi == 8) {
                sb.append("        (FAIL EMVCO) FWI=" + Integer.toString(fwi) +
                        ", EMVCo requires <= 7\n");
            } else {
                sb.append("        (OK) FWI=" + Integer.toString(fwi) + "\n");
            }
            int sfgi = ats[atsIndex] & 0x0F;
            if (sfgi > 8) {
                success = false;
                sb.append("        (FAIL) SFGI=" + Integer.toString(sfgi) + ", should be <= 8\n");
            } else {
                sb.append("        (OK) SFGI=" + Integer.toString(sfgi) + "\n");
            }
            atsIndex++;
        }

        if (tc_present) {
            sb.append("    TC: 0x" + Integer.toHexString(ats[atsIndex] & 0xFF) + "\n");
            boolean apSupported = (ats[atsIndex] & 0x10) != 0;
            boolean didSupported = (ats[atsIndex] & 0x02) != 0;
            boolean nadSupported = (ats[atsIndex] & 0x01) != 0;
            if (nadSupported) {
                success = false;
                sb.append("        (FAIL) NAD bit is not allowed to be set.\n");
            } else {
                sb.append("        (OK) NAD bit is not set.\n");
            }
            atsIndex++;
            // See if there's any bytes left for general bytes
            if (atsIndex + 1 < ats.length) {
                int bytesToCopy = ats.length - atsIndex;
                byte[] historical_bytes = new byte[bytesToCopy];
                System.arraycopy(ats, atsIndex, historical_bytes, 0, bytesToCopy);
                sb.append("\n(OK) Historical bytes: " +
                        HceUtils.getHexBytes(null, historical_bytes));
            }
        }
        return success;
    }

    @Override
    public void onTagDiscovered(Tag tag) {
        final StringBuilder sb = new StringBuilder();
        IsoDep isoDep = IsoDep.get(tag);
        NfcA nfcA = NfcA.get(tag);
        boolean success = false;
        if (nfcA == null || isoDep == null) {
            return;
        }
        try {
            nfcA.connect();
            byte[] ats = nfcA.transceive(new byte[] { (byte) 0xE0, (byte)0xF0});
            success = parseProtocolParameters(sb, tag.getId(), nfcA.getSak(), nfcA.getAtqa(), ats);
        } catch (IOException e) {
            sb.insert(0, "Test failed. IOException (did you keep the devices in range?)\n\n.");
        } finally {
            if (success) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mTextView.setText(sb.toString());
                        getPassButton().setEnabled(true);
                    }
                });
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mTextView.setText(sb.toString());
                        getPassButton().setEnabled(false);
                    }
                });
            }
            try {
                nfcA.transceive(new byte[] {(byte) 0xC2});
                nfcA.close();
                isoDep.connect();
            } catch (IOException e) {
            }
        }
    }
}
