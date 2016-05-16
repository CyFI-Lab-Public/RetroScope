/*
 * Copyright (C) 2009 The Android Open Source Project
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

package android.view.cts;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.View.OnTouchListener;
import android.widget.Button;

public class GestureDetectorStubActivity extends Activity {

    public boolean isDown;
    public boolean isScroll;
    public boolean isFling;
    public boolean isSingleTapUp;
    public boolean onShowPress;
    public boolean onLongPress;
    public boolean onDoubleTap;
    public boolean onDoubleTapEvent;
    public boolean onSingleTapConfirmed;

    private GestureDetector mGestureDetector;
    private MockOnGestureListener mOnGestureListener;
    private Handler mHandler;
    private View mView;
    private Button mTop;
    private Button mButton;
    private ViewGroup mViewGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mOnGestureListener = new MockOnGestureListener();
        mHandler = new Handler();

        mGestureDetector = new GestureDetector(this, mOnGestureListener, mHandler);
        mGestureDetector.setOnDoubleTapListener(mOnGestureListener);
        mView = new View(this);
        mButton = new Button(this);
        mTop = new Button(this);
        mView.setOnTouchListener(new MockOnTouchListener());

        mViewGroup = new ViewGroup(this) {
            @Override
            protected void onLayout(boolean changed, int l, int t, int r, int b) {
            }
        };
        mViewGroup.addView(mView);
        mViewGroup.addView(mTop);
        mViewGroup.addView(mButton);
        mViewGroup.setOnTouchListener(new MockOnTouchListener());
        setContentView(mViewGroup);

    }

    public View getView() {
        return mView;
    }

    public ViewGroup getViewGroup() {
        return mViewGroup;
    }

    public GestureDetector getGestureDetector() {
        return mGestureDetector;
    }

    public class MockOnGestureListener extends SimpleOnGestureListener {
        public boolean onDown(MotionEvent e) {
            isDown = true;
            return true;
        }

        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            isFling = true;
            return true;
        }

        public void onLongPress(MotionEvent e) {
            onLongPress = true;
        }

        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            isScroll = true;
            return true;
        }

        public void onShowPress(MotionEvent e) {
            onShowPress = true;
        }

        public boolean onSingleTapUp(MotionEvent e) {
            isSingleTapUp = true;
            return true;
        }

        public boolean onDoubleTap(MotionEvent e) {
            onDoubleTap = true;
            return false;
        }

        public boolean onDoubleTapEvent(MotionEvent e) {
            onDoubleTapEvent = true;
            return false;
        }

        public boolean onSingleTapConfirmed(MotionEvent e) {
            onSingleTapConfirmed = true;
            return false;
        }
    }

    class MockOnTouchListener implements OnTouchListener {

        public boolean onTouch(View v, MotionEvent event) {
            mGestureDetector.onTouchEvent(event);
            return true;
        }
    }

}
