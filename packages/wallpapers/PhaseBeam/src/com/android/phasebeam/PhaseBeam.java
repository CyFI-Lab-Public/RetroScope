package com.android.phasebeam;

import android.app.Activity;
import android.os.Bundle;

public class PhaseBeam extends Activity {

    private PhaseBeamView mView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mView = new PhaseBeamView(this);
        setContentView(mView);
    }
}