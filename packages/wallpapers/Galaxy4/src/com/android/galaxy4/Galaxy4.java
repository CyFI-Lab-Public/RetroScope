package com.android.galaxy4;

import android.app.Activity;
import android.os.Bundle;

public class Galaxy4 extends Activity {

    private GalaxyView mView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mView = new GalaxyView(this);
        setContentView(mView);
    }
}