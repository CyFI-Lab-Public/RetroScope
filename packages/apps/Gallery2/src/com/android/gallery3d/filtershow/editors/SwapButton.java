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

package com.android.gallery3d.filtershow.editors;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.Button;

public class SwapButton extends Button implements GestureDetector.OnGestureListener {

    public static int ANIM_DURATION = 200;

    public interface SwapButtonListener {
        public void swapLeft(MenuItem item);
        public void swapRight(MenuItem item);
    }

    private GestureDetector mDetector;
    private SwapButtonListener mListener;
    private Menu mMenu;
    private int mCurrentMenuIndex;

    public SwapButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        mDetector = new GestureDetector(context, this);
    }

    public SwapButtonListener getListener() {
        return mListener;
    }

    public void setListener(SwapButtonListener listener) {
        mListener = listener;
    }

    public boolean onTouchEvent(MotionEvent me) {
        if (!mDetector.onTouchEvent(me)) {
            return super.onTouchEvent(me);
        }
        return true;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        callOnClick();
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (mMenu == null) {
            return false;
        }
        if (e1.getX() - e2.getX() > 0) {
            // right to left
            mCurrentMenuIndex++;
            if (mCurrentMenuIndex == mMenu.size()) {
                mCurrentMenuIndex = 0;
            }
            if (mListener != null) {
                mListener.swapRight(mMenu.getItem(mCurrentMenuIndex));
            }
        } else {
            // left to right
            mCurrentMenuIndex--;
            if (mCurrentMenuIndex < 0) {
                mCurrentMenuIndex = mMenu.size() - 1;
            }
            if (mListener != null) {
                mListener.swapLeft(mMenu.getItem(mCurrentMenuIndex));
            }
        }
        return true;
    }
}
