package com.android.cts.verifier.nfc.hce;

import android.content.ComponentName;

public class TransportService1 extends HceService {
    static final ComponentName COMPONENT =
            new ComponentName("com.android.cts.verifier",
            TransportService1.class.getName());

    public static final String[] APDU_COMMAND_SEQUENCE = {
        HceUtils.buildSelectApdu(HceUtils.TRANSPORT_AID),
        "80CA01E000"
    };

    public static final String[] APDU_RESPOND_SEQUENCE = {
        "80CA9000",
        "83947102829000"
    };

    public TransportService1() {
        initialize(APDU_COMMAND_SEQUENCE, APDU_RESPOND_SEQUENCE);
    }

    @Override
    public ComponentName getComponent() {
        return COMPONENT;
    }
}
