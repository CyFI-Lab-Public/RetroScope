/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.camera.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class PieMenuButton extends View {
    private boolean mPressed;
    private boolean mReadyToClick = false;
    public PieMenuButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        mPressed = isPressed();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean handled = super.onTouchEvent(event);
        if (MotionEvent.ACTION_UP == event.getAction() && mPressed) {
            // Perform a customized click as soon as the ACTION_UP event
            // is received. The reason for doing this is that Framework
            // delays the performClick() call after ACTION_UP. But we do not
            // want the delay because it affects an important state change
            // for PieRenderer.
            mReadyToClick = true;
            performClick();
        }
        return handled;
    }

    @Override
    public boolean performClick() {
        if (mReadyToClick) {
            // We only respond to our customized click which happens right
            // after ACTION_UP event is received, with no delay.
            mReadyToClick = false;
            return super.performClick();
        }
        return false;
    }
};
