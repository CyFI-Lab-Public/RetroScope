package com.android.cts.verifier.nfc.hce;

import android.content.ComponentName;

public class PaymentService2 extends HceService {
    static final ComponentName COMPONENT =
            new ComponentName("com.android.cts.verifier",
            PaymentService2.class.getName());

    public static final String[] APDU_COMMAND_SEQUENCE = {
        HceUtils.buildSelectApdu(HceUtils.PPSE_AID),
        HceUtils.buildSelectApdu(HceUtils.MC_AID)
    };

    public static final String[] APDU_RESPOND_SEQUENCE = {
        "12349000",
        "56789000"
    };

    public PaymentService2() {
        initialize(APDU_COMMAND_SEQUENCE, APDU_RESPOND_SEQUENCE);
    }

    @Override
    public ComponentName getComponent() {
        return COMPONENT;
    }
}
