package com.android.cts.verifier.nfc.hce;

import android.content.ComponentName;
import android.content.pm.PackageManager;

public final class HceUtils {
    public static final String ACTION_APDU_SEQUENCE_COMPLETE =
            "com.android.cts.verifier.nfc.hce.ACTION_APDU_SEQUENCE_COMPLETE";
    public static final String ACTION_APDU_SEQUENCE_ERROR =
            "com.android.cts.verifier.nfc.hce.ACTION_APDU_SEQUENCE_ERROR";

    public static final String EXTRA_COMPONENT = "component";
    public static final String EXTRA_DURATION = "duration";

    public static final String PPSE_AID = "325041592E5359532E4444463031";
    public static final String MC_AID = "A0000000041010";

    public static final String TRANSPORT_AID = "F001020304";
    public static final String ACCESS_AID = "F005060708";

    public static void enableComponent(PackageManager pm, ComponentName component) {
        pm.setComponentEnabledSetting(
                component,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }

    public static void disableComponent(PackageManager pm, ComponentName component) {
        pm.setComponentEnabledSetting(
                component,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

    public static String getHexBytes(String header, byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        if (header != null) {
            sb.append(header + ": ");
        }
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString();
    }

    public static byte[] hexStringToBytes(String s) {
        if (s == null || s.length() == 0) return null;
        int len = s.length();
        if (len % 2 != 0) {
            s = '0' + s;
            len++;
        }
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                                 + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    public static final String buildSelectApdu(String aid) {
        StringBuilder sb = new StringBuilder();
        sb.append("00A40400");
        sb.append(String.format("%02X", aid.length() / 2));
        sb.append(aid);
        return sb.toString();
    }
}
