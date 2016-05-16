package com.android.dreamtheater;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Canvas;

import android.widget.ViewFlipper;
import android.widget.ImageView;
import android.view.View;

import android.support.v13.dreams.BasicDream;

public class Demos {
    public static class Demo1 extends BasicDream {
    }

    public static class Demo2 extends BasicDream {
        @Override
        public void onStart() {
            super.onStart();
            setContentView(R.layout.slideshow);
        }

        @Override
        public void onAttachedToWindow() {
            final ViewFlipper flipper = (ViewFlipper) findViewById(R.id.faster_than_lightning);
            flipper.setSystemUiVisibility(View.STATUS_BAR_HIDDEN);
            flipper.startFlipping();
        }
    }
}
