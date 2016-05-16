package com.android.mail.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.View;
import android.view.ViewPropertyAnimator;

/**
 * A backwards-compatible substitute for {@link ViewPropertyAnimator#withLayer()}.
 *
 */
public class HardwareLayerEnabler extends AnimatorListenerAdapter {

    private final View mTarget;

    public HardwareLayerEnabler(View target) {
        mTarget = target;
    }

    @Override
    public void onAnimationStart(Animator animation) {
        mTarget.setLayerType(View.LAYER_TYPE_HARDWARE, null);
    }

    @Override
    public void onAnimationEnd(Animator animation) {
        mTarget.setLayerType(View.LAYER_TYPE_NONE, null);
    }

}
