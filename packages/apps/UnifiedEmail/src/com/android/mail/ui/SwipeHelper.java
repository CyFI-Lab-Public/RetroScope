/*
 * Copyright (C) 2012 Google Inc.
 * Licensed to The Android Open Source Project.
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

package com.android.mail.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import com.android.mail.R;
import com.android.mail.utils.LogUtils;
import com.android.mail.utils.Utils;

public class SwipeHelper {
    static final String TAG = "com.android.systemui.SwipeHelper";
    private static final boolean DEBUG_INVALIDATE = false;
    private static final boolean CONSTRAIN_SWIPE = true;
    private static final boolean FADE_OUT_DURING_SWIPE = true;
    private static final boolean DISMISS_IF_SWIPED_FAR_ENOUGH = true;
    // Turn on for debugging only during development.
    private static final boolean LOG_SWIPE_DISMISS_VELOCITY = false;

    public static final int X = 0;
    public static final int Y = 1;

    private static DecelerateInterpolator sDecelerateInterpolator =
                                                        new DecelerateInterpolator(1.0f);

    private static int SWIPE_ESCAPE_VELOCITY = -1;
    private static int DEFAULT_ESCAPE_ANIMATION_DURATION;
    private static int MAX_ESCAPE_ANIMATION_DURATION;
    private static int MAX_DISMISS_VELOCITY;
    private static int SNAP_ANIM_LEN;
    private static float MIN_SWIPE;

    public static float ALPHA_FADE_START = 0f; // fraction of thumbnail width
                                                 // where fade starts
    public static float ALPHA_TEXT_FADE_START = 0.4f;
    static final float ALPHA_FADE_END = 0.7f; // fraction of thumbnail width
                                              // beyond which alpha->0
    private static final float FACTOR = 1.2f;

    /* Dead region where swipe cannot be initiated. */
    private final static int DEAD_REGION_FOR_SWIPE = 56;

    private float mPagingTouchSlop;
    private final Callback mCallback;
    private final int mSwipeDirection;
    private final VelocityTracker mVelocityTracker;

    private float mInitialTouchPosX;
    private boolean mDragging;
    private SwipeableItemView mCurrView;
    private View mCurrAnimView;
    private boolean mCanCurrViewBeDimissed;
    private float mDensityScale;
    private float mLastY;
    private float mInitialTouchPosY;
    private LeaveBehindItem mPrevView;

    public SwipeHelper(Context context, int swipeDirection, Callback callback, float densityScale,
            float pagingTouchSlop) {
        mCallback = callback;
        mSwipeDirection = swipeDirection;
        mVelocityTracker = VelocityTracker.obtain();
        mDensityScale = densityScale;
        mPagingTouchSlop = pagingTouchSlop;
        if (SWIPE_ESCAPE_VELOCITY == -1) {
            Resources res = context.getResources();
            SWIPE_ESCAPE_VELOCITY = res.getInteger(R.integer.swipe_escape_velocity);
            DEFAULT_ESCAPE_ANIMATION_DURATION = res.getInteger(R.integer.escape_animation_duration);
            MAX_ESCAPE_ANIMATION_DURATION = res.getInteger(R.integer.max_escape_animation_duration);
            MAX_DISMISS_VELOCITY = res.getInteger(R.integer.max_dismiss_velocity);
            SNAP_ANIM_LEN = res.getInteger(R.integer.snap_animation_duration);
            MIN_SWIPE = res.getDimension(R.dimen.min_swipe);
        }
    }

    public void setDensityScale(float densityScale) {
        mDensityScale = densityScale;
    }

    public void setPagingTouchSlop(float pagingTouchSlop) {
        mPagingTouchSlop = pagingTouchSlop;
    }

    private float getVelocity(VelocityTracker vt) {
        return mSwipeDirection == X ? vt.getXVelocity() :
                vt.getYVelocity();
    }

    private ObjectAnimator createTranslationAnimation(View v, float newPos) {
        ObjectAnimator anim = ObjectAnimator.ofFloat(v,
                mSwipeDirection == X ? "translationX" : "translationY", newPos);
        return anim;
    }

    private ObjectAnimator createDismissAnimation(View v, float newPos, int duration) {
        ObjectAnimator anim = createTranslationAnimation(v, newPos);
        anim.setInterpolator(sDecelerateInterpolator);
        anim.setDuration(duration);
        return anim;
    }

    private float getPerpendicularVelocity(VelocityTracker vt) {
        return mSwipeDirection == X ? vt.getYVelocity() :
                vt.getXVelocity();
    }

    private void setTranslation(View v, float translate) {
        if (mSwipeDirection == X) {
            v.setTranslationX(translate);
        } else {
            v.setTranslationY(translate);
        }
    }

    private float getSize(View v) {
        return mSwipeDirection == X ? v.getMeasuredWidth() :
                v.getMeasuredHeight();
    }

    private float getAlphaForOffset(View view) {
        float viewSize = getSize(view);
        final float fadeSize = ALPHA_FADE_END * viewSize;
        float result = 1.0f;
        float pos = view.getTranslationX();
        if (pos >= viewSize * ALPHA_FADE_START) {
            result = 1.0f - (pos - viewSize * ALPHA_FADE_START) / fadeSize;
        } else if (pos < viewSize * (1.0f - ALPHA_FADE_START)) {
            result = 1.0f + (viewSize * ALPHA_FADE_START + pos) / fadeSize;
        }
        float minAlpha = 0.5f;
        return Math.max(minAlpha, result);
    }

    private float getTextAlphaForOffset(View view) {
        float viewSize = getSize(view);
        final float fadeSize = ALPHA_TEXT_FADE_START * viewSize;
        float result = 1.0f;
        float pos = view.getTranslationX();
        if (pos >= 0) {
            result = 1.0f - pos / fadeSize;
        } else if (pos < 0) {
            result = 1.0f + pos / fadeSize;
        }
        return Math.max(0, result);
    }

    // invalidate the view's own bounds all the way up the view hierarchy
    public static void invalidateGlobalRegion(View view) {
        invalidateGlobalRegion(
            view,
            new RectF(view.getLeft(), view.getTop(), view.getRight(), view.getBottom()));
    }

    // invalidate a rectangle relative to the view's coordinate system all the way up the view
    // hierarchy
    public static void invalidateGlobalRegion(View view, RectF childBounds) {
        //childBounds.offset(view.getTranslationX(), view.getTranslationY());
        if (DEBUG_INVALIDATE)
            LogUtils.v(TAG, "-------------");
        while (view.getParent() != null && view.getParent() instanceof View) {
            view = (View) view.getParent();
            view.getMatrix().mapRect(childBounds);
            view.invalidate((int) Math.floor(childBounds.left),
                            (int) Math.floor(childBounds.top),
                            (int) Math.ceil(childBounds.right),
                            (int) Math.ceil(childBounds.bottom));
            if (DEBUG_INVALIDATE) {
                LogUtils.v(TAG, "INVALIDATE(" + (int) Math.floor(childBounds.left)
                        + "," + (int) Math.floor(childBounds.top)
                        + "," + (int) Math.ceil(childBounds.right)
                        + "," + (int) Math.ceil(childBounds.bottom));
            }
        }
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = ev.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mLastY = ev.getY();
                mDragging = false;
                View view = mCallback.getChildAtPosition(ev);
                if (view instanceof SwipeableItemView) {
                    mCurrView = (SwipeableItemView) view;
                }
                mVelocityTracker.clear();
                if (mCurrView != null) {
                    mCurrAnimView = mCurrView.getSwipeableView().getView();
                    mCanCurrViewBeDimissed = mCallback.canChildBeDismissed(mCurrView);
                    mVelocityTracker.addMovement(ev);
                    mInitialTouchPosX = ev.getX();
                    mInitialTouchPosY = ev.getY();
                }
                mCallback.cancelDismissCounter();
                break;
            case MotionEvent.ACTION_MOVE:
                if (mCurrView != null) {
                    // Check the movement direction.
                    if (mLastY >= 0 && !mDragging) {
                        float currY = ev.getY();
                        float currX = ev.getX();
                        float deltaY = Math.abs(currY - mInitialTouchPosY);
                        float deltaX = Math.abs(currX - mInitialTouchPosX);
                        if (deltaY > mCurrView.getMinAllowScrollDistance()
                                && deltaY > (FACTOR * deltaX)) {
                            mLastY = ev.getY();
                            mCallback.onScroll();
                            return false;
                        }
                    }
                    mVelocityTracker.addMovement(ev);
                    float pos = ev.getX();
                    float delta = pos - mInitialTouchPosX;
                    if (Math.abs(delta) > mPagingTouchSlop) {
                        mCallback.onBeginDrag(mCurrView.getSwipeableView().getView());
                        mPrevView = mCallback.getLastSwipedItem();
                        mDragging = true;
                        mInitialTouchPosX = ev.getX() - mCurrAnimView.getTranslationX();
                        mInitialTouchPosY = ev.getY();
                    }
                }
                mLastY = ev.getY();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mDragging = false;
                mCurrView = null;
                mCurrAnimView = null;
                mLastY = -1;
                break;
        }
        return mDragging;
    }

    /**
     * @param view The view to be dismissed
     * @param velocity The desired pixels/second speed at which the view should
     *            move
     */
    private void dismissChild(final SwipeableItemView view, float velocity) {
        final View animView = mCurrView.getSwipeableView().getView();
        final boolean canAnimViewBeDismissed = mCallback.canChildBeDismissed(view);
        float newPos = determinePos(animView, velocity);
        int duration = determineDuration(animView, newPos, velocity);

        Utils.enableHardwareLayer(animView);
        ObjectAnimator anim = createDismissAnimation(animView, newPos, duration);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mCallback.onChildDismissed(mCurrView);
                animView.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        });
        anim.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (FADE_OUT_DURING_SWIPE && canAnimViewBeDismissed) {
                    animView.setAlpha(getAlphaForOffset(animView));
                }
                invalidateGlobalRegion(animView);
            }
        });
        anim.start();
    }

    private static int determineDuration(View animView, float newPos, float velocity) {
        int duration = MAX_ESCAPE_ANIMATION_DURATION;
        if (velocity != 0) {
            duration = Math
                    .min(duration,
                            (int) (Math.abs(newPos - animView.getTranslationX()) * 1000f / Math
                                    .abs(velocity)));
        } else {
            duration = DEFAULT_ESCAPE_ANIMATION_DURATION;
        }
        return duration;
    }

    private float determinePos(View animView, float velocity) {
        final float newPos;
        if (velocity < 0 || (velocity == 0 && animView.getTranslationX() < 0)
        // if we use the Menu to dismiss an item in landscape, animate up
                || (velocity == 0 && animView.getTranslationX() == 0 && mSwipeDirection == Y)) {
            newPos = -getSize(animView);
        } else {
            newPos = getSize(animView);
        }
        return newPos;
    }

    public void snapChild(final SwipeableItemView view) {
        final View animView = view.getSwipeableView().getView();
        final boolean canAnimViewBeDismissed = mCallback.canChildBeDismissed(view);
        final ObjectAnimator anim = createTranslationAnimation(animView, 0);
        final int duration = SNAP_ANIM_LEN;
        anim.setDuration(duration);
        anim.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (FADE_OUT_DURING_SWIPE && canAnimViewBeDismissed) {
                    animView.setAlpha(getAlphaForOffset(animView));
                }
                invalidateGlobalRegion(animView);
            }
        });
        anim.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }
            @Override
            public void onAnimationEnd(Animator animation) {
                animView.setAlpha(1.0f);
                mCallback.onDragCancelled(mCurrView);
            }
            @Override
            public void onAnimationCancel(Animator animation) {
            }
            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        anim.start();
    }

    public boolean onTouchEvent(MotionEvent ev) {
        if (!mDragging) {
            return false;
        }
        mVelocityTracker.addMovement(ev);
        final int action = ev.getAction();
        switch (action) {
            case MotionEvent.ACTION_OUTSIDE:
            case MotionEvent.ACTION_MOVE:
                if (mCurrView != null) {
                    float deltaX = ev.getX() - mInitialTouchPosX;
                    // If the swipe started in the dead region, ignore it.
                    if (mInitialTouchPosX <= (DEAD_REGION_FOR_SWIPE * mDensityScale)){
                            return true;
                    }
                    // If the user has gone vertical and not gone horizontalish AT
                    // LEAST minBeforeLock, switch to scroll. Otherwise, cancel
                    // the swipe.
                    float minDistance = MIN_SWIPE;
                    if (Math.abs(deltaX) < minDistance) {
                        // Don't start the drag until at least X distance has
                        // occurred.
                        return true;
                    }
                    // don't let items that can't be dismissed be dragged more
                    // than maxScrollDistance
                    if (CONSTRAIN_SWIPE && !mCallback.canChildBeDismissed(mCurrView)) {
                        float size = getSize(mCurrAnimView);
                        float maxScrollDistance = 0.15f * size;
                        if (Math.abs(deltaX) >= size) {
                            deltaX = deltaX > 0 ? maxScrollDistance : -maxScrollDistance;
                        } else {
                            deltaX = maxScrollDistance
                                    * (float) Math.sin((deltaX / size) * (Math.PI / 2));
                        }
                    }
                    setTranslation(mCurrAnimView, deltaX);
                    if (FADE_OUT_DURING_SWIPE && mCanCurrViewBeDimissed) {
                        mCurrAnimView.setAlpha(getAlphaForOffset(mCurrAnimView));
                        if (mPrevView != null) {
                            // Base how much the text of the prev item is faded
                            // on how far the current item has moved.
                            mPrevView.setTextAlpha(getTextAlphaForOffset(mCurrAnimView));
                        }
                    }
                    invalidateGlobalRegion(mCurrView.getSwipeableView().getView());
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mCurrView != null) {
                    float maxVelocity = MAX_DISMISS_VELOCITY * mDensityScale;
                    mVelocityTracker.computeCurrentVelocity(1000 /* px/sec */, maxVelocity);
                    float escapeVelocity = SWIPE_ESCAPE_VELOCITY * mDensityScale;
                    float velocity = getVelocity(mVelocityTracker);
                    float perpendicularVelocity = getPerpendicularVelocity(mVelocityTracker);

                    // Decide whether to dismiss the current view
                    // Tweak constants below as required to prevent erroneous
                    // swipe/dismiss
                    float translation = Math.abs(mCurrAnimView.getTranslationX());
                    float currAnimViewSize = getSize(mCurrAnimView);
                    // Long swipe = translation of .4 * width
                    boolean childSwipedFarEnough = DISMISS_IF_SWIPED_FAR_ENOUGH
                            && translation > 0.4 * currAnimViewSize;
                    // Fast swipe = > escapeVelocity and translation of .1 *
                    // width
                    boolean childSwipedFastEnough = (Math.abs(velocity) > escapeVelocity)
                            && (Math.abs(velocity) > Math.abs(perpendicularVelocity))
                            && (velocity > 0) == (mCurrAnimView.getTranslationX() > 0)
                            && translation > 0.05 * currAnimViewSize;
                    if (LOG_SWIPE_DISMISS_VELOCITY) {
                        LogUtils.v(TAG, "Swipe/Dismiss: " + velocity + "/" + escapeVelocity + "/"
                                + perpendicularVelocity + ", x: " + translation + "/"
                                + currAnimViewSize);
                    }

                    boolean dismissChild = mCallback.canChildBeDismissed(mCurrView)
                            && (childSwipedFastEnough || childSwipedFarEnough);

                    if (dismissChild) {
                        dismissChild(mCurrView, childSwipedFastEnough ? velocity : 0f);
                    } else {
                        snapChild(mCurrView);
                    }
                }
                break;
        }
        return true;
    }

    public interface Callback {
        View getChildAtPosition(MotionEvent ev);

        void cancelDismissCounter();

        void onScroll();

        boolean canChildBeDismissed(SwipeableItemView v);

        void onBeginDrag(View v);

        void onChildDismissed(SwipeableItemView v);

        void onDragCancelled(SwipeableItemView v);

        ConversationSelectionSet getSelectionSet();

        LeaveBehindItem getLastSwipedItem();
    }
}
