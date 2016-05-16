/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.dreams.bummer;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

public class BummerView extends TextView {
    public static final int START = 1;
    public static final int MOVE = 2;

    private int mDelay = 10000; // ms
    private int mAnimTime = 2000; // ms
    private boolean mAnimate = true;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message m) {
            boolean animate = false;
            switch (m.what) {
                case MOVE:
                    animate = mAnimate;
                    // fall through
                case START:
                    final View parent = (View) BummerView.this.getParent();
                    if (parent == null)
                        return;

                    final float framew = parent.getMeasuredWidth();
                    final float frameh = parent.getMeasuredHeight();
                    final float textw = getMeasuredWidth();
                    final float texth = getMeasuredHeight();

                    final float newx = (float) (Math.random() * (framew - textw));
                    final float newy = (float) (Math.random() * (frameh - texth));
                    if (animate) {
                        animate().x(newx)
                                .y(newy)
                                .setDuration(mAnimTime)
                                .start();
                    } else {
                        setX(newx);
                        setY(newy);
                    }
                    setVisibility(View.VISIBLE);

                    removeMessages(MOVE);
                    sendEmptyMessageDelayed(MOVE, mDelay);
                    break;
            }
        }
    };

    public BummerView(Context context) {
        super(context);
    }

    public BummerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BummerView(Context context, AttributeSet attrs, int flags) {
        super(context, attrs, flags);
    }

    public void setAnimationParams(boolean animate, int delay, int animTime) {
        mAnimate = animate;
        mDelay = delay;
        mAnimTime = animTime;
    }

    @Override
    public void onAttachedToWindow() {
        final View parent = (View) this.getParent();
        parent.addOnLayoutChangeListener(new OnLayoutChangeListener() {
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                    int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (v == parent && right != oldRight) {
                    mHandler.removeMessages(MOVE);
                    mHandler.sendEmptyMessage(START);
                }
            }
        });

        mHandler.sendEmptyMessage(START);
    }

}
