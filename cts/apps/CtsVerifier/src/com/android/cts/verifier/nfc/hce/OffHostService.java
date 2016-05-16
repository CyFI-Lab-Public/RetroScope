package com.android.cts.verifier.nfc.hce;

import android.content.ComponentName;

public class OffHostService {
    public static final ComponentName COMPONENT =
            new ComponentName("com.android.cts.verifier",
                    OffHostService.class.getName());

    public static final String[] APDU_COMMAND_SEQUENCE = {
        HceUtils.buildSelectApdu("A000000151000000"),
        "80CA9F7F00",
        HceUtils.buildSelectApdu("A000000003000000"),
        "80CA9F7F00"
    };

    public static final String[] APDU_RESPOND_SEQUENCE = {
        "*",
        "*",
        "*",
        "*"
    };
}
