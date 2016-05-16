package com.android.camera.util;

import android.graphics.Matrix;
import android.view.MotionEvent;

public final class MotionEventHelper {
    private MotionEventHelper() {}

    public static MotionEvent transformEvent(MotionEvent e, Matrix m) {
        // We try to use the new transform method if possible because it uses
        // less memory.
        return transformEventNew(e, m);
    }

    private static MotionEvent transformEventNew(MotionEvent e, Matrix m) {
        MotionEvent newEvent = MotionEvent.obtain(e);
        newEvent.transform(m);
        return newEvent;
    }
}
