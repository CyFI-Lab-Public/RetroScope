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

package com.android.camera.ui;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.FrameLayout.LayoutParams;
import android.widget.LinearLayout;

import com.android.camera.util.CameraUtil;
import com.android.camera.util.GcamHelper;
import com.android.camera.util.PhotoSphereHelper;
import com.android.camera.util.UsageStatistics;
import com.android.camera2.R;

public class ModuleSwitcher extends RotateImageView
        implements OnClickListener, OnTouchListener {

    @SuppressWarnings("unused")
    private static final String TAG = "CAM_Switcher";
    private static final int SWITCHER_POPUP_ANIM_DURATION = 200;

    public static final int PHOTO_MODULE_INDEX = 0;
    public static final int VIDEO_MODULE_INDEX = 1;
    public static final int WIDE_ANGLE_PANO_MODULE_INDEX = 2;
    public static final int LIGHTCYCLE_MODULE_INDEX = 3;
    public static final int GCAM_MODULE_INDEX = 4;

    private static final int[] DRAW_IDS = {
            R.drawable.ic_switch_camera,
            R.drawable.ic_switch_video,
            R.drawable.ic_switch_pan,
            R.drawable.ic_switch_photosphere,
            R.drawable.ic_switch_gcam,
    };

    public interface ModuleSwitchListener {
        public void onModuleSelected(int i);

        public void onShowSwitcherPopup();
    }

    private ModuleSwitchListener mListener;
    private int mCurrentIndex;
    private int[] mModuleIds;
    private int[] mDrawIds;
    private int mItemSize;
    private View mPopup;
    private View mParent;
    private boolean mShowingPopup;
    private boolean mNeedsAnimationSetup;
    private Drawable mIndicator;

    private float mTranslationX = 0;
    private float mTranslationY = 0;

    private AnimatorListener mHideAnimationListener;
    private AnimatorListener mShowAnimationListener;

    public ModuleSwitcher(Context context) {
        super(context);
        init(context);
    }

    public ModuleSwitcher(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        mItemSize = context.getResources().getDimensionPixelSize(R.dimen.switcher_size);
        setOnClickListener(this);
        mIndicator = context.getResources().getDrawable(R.drawable.ic_switcher_menu_indicator);
        initializeDrawables(context);
    }

    public void initializeDrawables(Context context) {
        int numDrawIds = DRAW_IDS.length;

        if (!PhotoSphereHelper.hasLightCycleCapture(context)) {
            --numDrawIds;
        }

        // Always decrement one because of GCam.
        --numDrawIds;

        int[] drawids = new int[numDrawIds];
        int[] moduleids = new int[numDrawIds];
        int ix = 0;
        for (int i = 0; i < DRAW_IDS.length; i++) {
            if (i == LIGHTCYCLE_MODULE_INDEX && !PhotoSphereHelper.hasLightCycleCapture(context)) {
                continue; // not enabled, so don't add to UI
            }
            if (i == GCAM_MODULE_INDEX) {
                continue; // don't add to UI
            }
            moduleids[ix] = i;
            drawids[ix++] = DRAW_IDS[i];
        }
        setIds(moduleids, drawids);
    }

    public void setIds(int[] moduleids, int[] drawids) {
        mDrawIds = drawids;
        mModuleIds = moduleids;
    }

    public void setCurrentIndex(int i) {
        mCurrentIndex = i;
        if (i == GCAM_MODULE_INDEX) {
          setImageResource(R.drawable.ic_switch_camera);
        } else {
          setImageResource(mDrawIds[i]);
        }
    }

    public void setSwitchListener(ModuleSwitchListener l) {
        mListener = l;
    }

    @Override
    public void onClick(View v) {
        showSwitcher();
        mListener.onShowSwitcherPopup();
    }

    private void onModuleSelected(int ix) {
        hidePopup();
        if ((ix != mCurrentIndex) && (mListener != null)) {
            UsageStatistics.onEvent("CameraModeSwitch", null, null);
            UsageStatistics.setPendingTransitionCause(
                    UsageStatistics.TRANSITION_MENU_TAP);
            setCurrentIndex(ix);
            mListener.onModuleSelected(mModuleIds[ix]);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mIndicator.setBounds(getDrawable().getBounds());
        mIndicator.draw(canvas);
    }

    private void initPopup() {
        mParent = LayoutInflater.from(getContext()).inflate(R.layout.switcher_popup,
                (ViewGroup) getParent());
        LinearLayout content = (LinearLayout) mParent.findViewById(R.id.content);
        mPopup = content;
        // Set the gravity of the popup, so that it shows up at the right
        // position
        // on screen
        LayoutParams lp = ((LayoutParams) mPopup.getLayoutParams());
        lp.gravity = ((LayoutParams) mParent.findViewById(R.id.camera_switcher)
                .getLayoutParams()).gravity;
        mPopup.setLayoutParams(lp);

        mPopup.setVisibility(View.INVISIBLE);
        mNeedsAnimationSetup = true;
        for (int i = mDrawIds.length - 1; i >= 0; i--) {
            RotateImageView item = new RotateImageView(getContext());
            item.setImageResource(mDrawIds[i]);
            item.setBackgroundResource(R.drawable.bg_pressed);
            final int index = i;
            item.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (showsPopup()) {
                        onModuleSelected(index);
                    }
                }
            });
            switch (mDrawIds[i]) {
                case R.drawable.ic_switch_camera:
                    item.setContentDescription(getContext().getResources().getString(
                            R.string.accessibility_switch_to_camera));
                    break;
                case R.drawable.ic_switch_video:
                    item.setContentDescription(getContext().getResources().getString(
                            R.string.accessibility_switch_to_video));
                    break;
                case R.drawable.ic_switch_pan:
                    item.setContentDescription(getContext().getResources().getString(
                            R.string.accessibility_switch_to_panorama));
                    break;
                case R.drawable.ic_switch_photosphere:
                    item.setContentDescription(getContext().getResources().getString(
                            R.string.accessibility_switch_to_photo_sphere));
                    break;
                case R.drawable.ic_switch_gcam:
                    item.setContentDescription(getContext().getResources().getString(
                            R.string.accessibility_switch_to_gcam));
                    break;
                default:
                    break;
            }
            content.addView(item, new LinearLayout.LayoutParams(mItemSize, mItemSize));
        }
        mPopup.measure(MeasureSpec.makeMeasureSpec(mParent.getWidth(), MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(mParent.getHeight(), MeasureSpec.AT_MOST));
    }

    public boolean showsPopup() {
        return mShowingPopup;
    }

    public boolean isInsidePopup(MotionEvent evt) {
        if (!showsPopup()) {
            return false;
        }
        int topLeft[] = new int[2];
        mPopup.getLocationOnScreen(topLeft);
        int left = topLeft[0];
        int top = topLeft[1];
        int bottom = top + mPopup.getHeight();
        int right = left + mPopup.getWidth();
        return evt.getX() >= left && evt.getX() < right
                && evt.getY() >= top && evt.getY() < bottom;
    }

    private void hidePopup() {
        mShowingPopup = false;
        setVisibility(View.VISIBLE);
        if (mPopup != null && !animateHidePopup()) {
            mPopup.setVisibility(View.INVISIBLE);
        }
        mParent.setOnTouchListener(null);
    }

    @Override
    public void onConfigurationChanged(Configuration config) {
        if (showsPopup()) {
            ((ViewGroup) mParent).removeView(mPopup);
            mPopup = null;
            initPopup();
            mPopup.setVisibility(View.VISIBLE);
        }
    }

    private void showSwitcher() {
        mShowingPopup = true;
        if (mPopup == null) {
            initPopup();
        }
        layoutPopup();
        mPopup.setVisibility(View.VISIBLE);
        if (!animateShowPopup()) {
            setVisibility(View.INVISIBLE);
        }
        mParent.setOnTouchListener(this);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        closePopup();
        return true;
    }

    public void closePopup() {
        if (showsPopup()) {
            hidePopup();
        }
    }

    @Override
    public void setOrientation(int degree, boolean animate) {
        super.setOrientation(degree, animate);
        ViewGroup content = (ViewGroup) mPopup;
        if (content == null) {
            return;
        }
        for (int i = 0; i < content.getChildCount(); i++) {
            RotateImageView iv = (RotateImageView) content.getChildAt(i);
            iv.setOrientation(degree, animate);
        }
    }

    private void layoutPopup() {
        int orientation = CameraUtil.getDisplayRotation((Activity) getContext());
        int w = mPopup.getMeasuredWidth();
        int h = mPopup.getMeasuredHeight();
        if (orientation == 0) {
            mPopup.layout(getRight() - w, getBottom() - h, getRight(), getBottom());
            mTranslationX = 0;
            mTranslationY = h / 3;
        } else if (orientation == 90) {
            mTranslationX = w / 3;
            mTranslationY = -h / 3;
            mPopup.layout(getRight() - w, getTop(), getRight(), getTop() + h);
        } else if (orientation == 180) {
            mTranslationX = -w / 3;
            mTranslationY = -h / 3;
            mPopup.layout(getLeft(), getTop(), getLeft() + w, getTop() + h);
        } else {
            mTranslationX = -w / 3;
            mTranslationY = h - getHeight();
            mPopup.layout(getLeft(), getBottom() - h, getLeft() + w, getBottom());
        }
    }

    @Override
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (mPopup != null) {
            layoutPopup();
        }
    }

    private void popupAnimationSetup() {
        layoutPopup();
        mPopup.setScaleX(0.3f);
        mPopup.setScaleY(0.3f);
        mPopup.setTranslationX(mTranslationX);
        mPopup.setTranslationY(mTranslationY);
        mNeedsAnimationSetup = false;
    }

    private boolean animateHidePopup() {
        if (mHideAnimationListener == null) {
            mHideAnimationListener = new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    // Verify that we weren't canceled
                    if (!showsPopup() && mPopup != null) {
                        mPopup.setVisibility(View.INVISIBLE);
                        ((ViewGroup) mParent).removeView(mPopup);
                        mPopup = null;
                    }
                }
            };
        }
        mPopup.animate()
                .alpha(0f)
                .scaleX(0.3f).scaleY(0.3f)
                .translationX(mTranslationX)
                .translationY(mTranslationY)
                .setDuration(SWITCHER_POPUP_ANIM_DURATION)
                .setListener(mHideAnimationListener);
        animate().alpha(1f).setDuration(SWITCHER_POPUP_ANIM_DURATION)
                .setListener(null);
        return true;
    }

    private boolean animateShowPopup() {
        if (mNeedsAnimationSetup) {
            popupAnimationSetup();
        }
        if (mShowAnimationListener == null) {
            mShowAnimationListener = new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    // Verify that we weren't canceled
                    if (showsPopup()) {
                        setVisibility(View.INVISIBLE);
                        // request layout to make sure popup is laid out
                        // correctly on ICS
                        mPopup.requestLayout();
                    }
                }
            };
        }
        mPopup.animate()
                .alpha(1f)
                .scaleX(1f).scaleY(1f)
                .translationX(0)
                .translationY(0)
                .setDuration(SWITCHER_POPUP_ANIM_DURATION)
                .setListener(null);
        animate().alpha(0f).setDuration(SWITCHER_POPUP_ANIM_DURATION)
                .setListener(mShowAnimationListener);
        return true;
    }
}
