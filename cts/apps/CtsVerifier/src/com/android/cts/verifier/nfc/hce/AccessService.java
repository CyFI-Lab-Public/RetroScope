package com.android.cts.verifier.nfc.hce;

import android.content.ComponentName;

public class AccessService extends HceService {
    static final ComponentName COMPONENT =
            new ComponentName("com.android.cts.verifier",
            AccessService.class.getName());

    public static final String[] APDU_COMMAND_SEQUENCE = {
        HceUtils.buildSelectApdu(HceUtils.ACCESS_AID),
        "80CA01F000"
    };

    public static final String[] APDU_RESPOND_SEQUENCE = {
        "123456789000",
        "1481148114819000"
    };

    public AccessService() {
        initialize(APDU_COMMAND_SEQUENCE, APDU_RESPOND_SEQUENCE);
    }

    @Override
    public ComponentName getComponent() {
        return COMPONENT;
    }
}
