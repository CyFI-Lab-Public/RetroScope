package com.android.noisefield;

import android.app.Activity;
import android.os.Bundle;

public class NoiseField extends Activity {

    private NoiseFieldView mView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mView = new NoiseFieldView(this);
        setContentView(mView);
    }
}