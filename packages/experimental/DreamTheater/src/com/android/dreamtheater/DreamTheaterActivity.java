package com.android.dreamtheater;

import android.app.Activity;
import android.content.Intent;

public class DreamTheaterActivity extends Activity {
    @Override
    public void onStart() {
        super.onStart();

        startActivity(Intent.createChooser(new Intent(Intent.ACTION_MAIN)
                    .addCategory("android.intent.category.DREAM"),
                "Choose a screen saver:"));
        finish();
    }
}
