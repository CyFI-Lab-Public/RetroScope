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

package com.android.gallery3d.filtershow.state;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.FilterShowActivity;
import com.android.gallery3d.filtershow.category.SwipableView;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import com.android.gallery3d.filtershow.imageshow.MasterImage;
import com.android.gallery3d.filtershow.pipeline.ImagePreset;

public class StateView extends View implements SwipableView {

    private static final String LOGTAG = "StateView";
    private Path mPath = new Path();
    private Paint mPaint = new Paint();

    public static int DEFAULT = 0;
    public static int BEGIN = 1;
    public static int END = 2;

    public static int UP = 1;
    public static int DOWN = 2;
    public static int LEFT = 3;
    public static int RIGHT = 4;

    private int mType = DEFAULT;
    private float mAlpha = 1.0f;
    private String mText = "Default";
    private float mTextSize = 32;
    private static int sMargin = 16;
    private static int sArrowHeight = 16;
    private static int sArrowWidth = 8;
    private float mStartTouchX = 0;
    private float mStartTouchY = 0;
    private float mDeleteSlope = 20;

    private int mOrientation = LinearLayout.VERTICAL;
    private int mDirection = DOWN;
    private boolean mDuplicateButton;
    private State mState;

    private int mEndsBackgroundColor;
    private int mEndsTextColor;
    private int mBackgroundColor;
    private int mTextColor;
    private int mSelectedBackgroundColor;
    private int mSelectedTextColor;
    private Rect mTextBounds = new Rect();

    public StateView(Context context) {
        this(context, DEFAULT);
    }

    public StateView(Context context, int type) {
        super(context);
        mType = type;
        Resources res = getResources();
        mEndsBackgroundColor = res.getColor(R.color.filtershow_stateview_end_background);
        mEndsTextColor = res.getColor(R.color.filtershow_stateview_end_text);
        mBackgroundColor = res.getColor(R.color.filtershow_stateview_background);
        mTextColor = res.getColor(R.color.filtershow_stateview_text);
        mSelectedBackgroundColor = res.getColor(R.color.filtershow_stateview_selected_background);
        mSelectedTextColor = res.getColor(R.color.filtershow_stateview_selected_text);
        mTextSize = res.getDimensionPixelSize(R.dimen.state_panel_text_size);
    }

    public String getText() {
        return mText;
    }

    public void setText(String text) {
        mText = text;
        invalidate();
    }

    public void setType(int type) {
        mType = type;
        invalidate();
    }

    @Override
    public void setSelected(boolean value) {
        super.setSelected(value);
        if (!value) {
            mDuplicateButton = false;
        }
        invalidate();
    }

    public void drawText(Canvas canvas) {
        if (mText == null) {
            return;
        }
        mPaint.reset();
        if (isSelected()) {
            mPaint.setColor(mSelectedTextColor);
        } else {
            mPaint.setColor(mTextColor);
        }
        if (mType == BEGIN) {
            mPaint.setColor(mEndsTextColor);
        }
        mPaint.setTypeface(Typeface.DEFAULT_BOLD);
        mPaint.setAntiAlias(true);
        mPaint.setTextSize(mTextSize);
        mPaint.getTextBounds(mText, 0, mText.length(), mTextBounds);
        int x = (canvas.getWidth() - mTextBounds.width()) / 2;
        int y = mTextBounds.height() + (canvas.getHeight() - mTextBounds.height()) / 2;
        canvas.drawText(mText, x, y, mPaint);
    }

    public void onDraw(Canvas canvas) {
        canvas.drawARGB(0, 0, 0, 0);
        mPaint.reset();
        mPath.reset();

        float w = canvas.getWidth();
        float h = canvas.getHeight();
        float r = sArrowHeight;
        float d = sArrowWidth;

        if (mOrientation == LinearLayout.HORIZONTAL) {
            drawHorizontalPath(w, h, r, d);
        } else {
            if (mDirection == DOWN) {
                drawVerticalDownPath(w, h, r, d);
            } else {
                drawVerticalPath(w, h, r, d);
            }
        }

        if (mType == DEFAULT || mType == END) {
            if (mDuplicateButton) {
                mPaint.setARGB(255, 200, 0, 0);
            } else if (isSelected()) {
                mPaint.setColor(mSelectedBackgroundColor);
            } else {
                mPaint.setColor(mBackgroundColor);
            }
        } else {
            mPaint.setColor(mEndsBackgroundColor);
        }
        canvas.drawPath(mPath, mPaint);
        drawText(canvas);
    }

    private void drawHorizontalPath(float w, float h, float r, float d) {
        if (this.getLayoutDirection() == LAYOUT_DIRECTION_RTL) {
            mPath.moveTo(w, 0);
            if (mType == END) {
                mPath.lineTo(0, 0);
                mPath.lineTo(0, h);
            } else {
                mPath.lineTo(d, 0);
                mPath.lineTo(d, r);
                mPath.lineTo(0, r + d);
                mPath.lineTo(d, r + d + r);
                mPath.lineTo(d, h);
            }
            mPath.lineTo(w, h);
            if (mType != BEGIN) {
                mPath.lineTo(w, r + d + r);
                mPath.lineTo(w - d, r + d);
                mPath.lineTo(w, r);
            }
        } else {
            mPath.moveTo(0, 0);
            if (mType == END) {
                mPath.lineTo(w, 0);
                mPath.lineTo(w, h);
            } else {
                mPath.lineTo(w - d, 0);
                mPath.lineTo(w - d, r);
                mPath.lineTo(w, r + d);
                mPath.lineTo(w - d, r + d + r);
                mPath.lineTo(w - d, h);
            }
            mPath.lineTo(0, h);
            if (mType != BEGIN) {
                mPath.lineTo(0, r + d + r);
                mPath.lineTo(d, r + d);
                mPath.lineTo(0, r);
            }
        }
        mPath.close();
    }

    private void drawVerticalPath(float w, float h, float r, float d) {
        if (mType == BEGIN) {
            mPath.moveTo(0, 0);
            mPath.lineTo(w, 0);
        } else {
            mPath.moveTo(0, d);
            mPath.lineTo(r, d);
            mPath.lineTo(r + d, 0);
            mPath.lineTo(r + d + r, d);
            mPath.lineTo(w, d);
        }
        mPath.lineTo(w, h);
        if (mType != END) {
            mPath.lineTo(r + d + r, h);
            mPath.lineTo(r + d, h - d);
            mPath.lineTo(r, h);
        }
        mPath.lineTo(0, h);
        mPath.close();
    }

    private void drawVerticalDownPath(float w, float h, float r, float d) {
        mPath.moveTo(0, 0);
        if (mType != BEGIN) {
            mPath.lineTo(r, 0);
            mPath.lineTo(r + d, d);
            mPath.lineTo(r + d + r, 0);
        }
        mPath.lineTo(w, 0);

        if (mType != END) {
            mPath.lineTo(w, h - d);

            mPath.lineTo(r + d + r, h - d);
            mPath.lineTo(r + d, h);
            mPath.lineTo(r, h - d);

            mPath.lineTo(0, h - d);
        } else {
            mPath.lineTo(w, h);
            mPath.lineTo(0, h);
        }

        mPath.close();
    }

    public void setBackgroundAlpha(float alpha) {
        if (mType == BEGIN) {
            return;
        }
        mAlpha = alpha;
        setAlpha(alpha);
        invalidate();
    }

    public float getBackgroundAlpha() {
        return mAlpha;
    }

    public void setOrientation(int orientation) {
        mOrientation = orientation;
    }

    public void setDuplicateButton(boolean b) {
        mDuplicateButton = b;
        invalidate();
    }

    public State getState() {
        return mState;
    }

    public void setState(State state) {
        mState = state;
        mText = mState.getText().toUpperCase();
        mType = mState.getType();
        invalidate();
    }

    public void resetPosition() {
        setTranslationX(0);
        setTranslationY(0);
        setBackgroundAlpha(1.0f);
    }

    public boolean isDraggable() {
        return mState.isDraggable();
    }

    @Override
    public void delete() {
        FilterShowActivity activity = (FilterShowActivity) getContext();
        FilterRepresentation representation = getState().getFilterRepresentation();
        activity.removeFilterRepresentation(representation);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean ret = super.onTouchEvent(event);
        FilterShowActivity activity = (FilterShowActivity) getContext();

        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            activity.startTouchAnimation(this, event.getX(), event.getY());
        }
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            mStartTouchY = event.getY();
            mStartTouchX = event.getX();
            if (mType == BEGIN) {
                MasterImage.getImage().setShowsOriginal(true);
            }
        }
        if (event.getActionMasked() == MotionEvent.ACTION_UP
            || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            setTranslationX(0);
            setTranslationY(0);
            MasterImage.getImage().setShowsOriginal(false);
            if (mType != BEGIN && event.getActionMasked() == MotionEvent.ACTION_UP) {
                setSelected(true);
                FilterRepresentation representation = getState().getFilterRepresentation();
                MasterImage image = MasterImage.getImage();
                ImagePreset preset = image != null ? image.getCurrentPreset() : null;
                if (getTranslationY() == 0
                        && image != null && preset != null
                        && representation != image.getCurrentFilterRepresentation()
                        && preset.getRepresentation(representation) != null) {
                    activity.showRepresentation(representation);
                    setSelected(false);
                }
            }
        }
        if (mType != BEGIN && event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            float delta = event.getY() - mStartTouchY;
            if (Math.abs(delta) > mDeleteSlope) {
                activity.setHandlesSwipeForView(this, mStartTouchX, mStartTouchY);
            }
        }
        return true;
    }
}
