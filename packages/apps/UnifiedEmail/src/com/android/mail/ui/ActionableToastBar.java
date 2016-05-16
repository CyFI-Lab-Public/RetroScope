/**
 * Copyright (c) 2011, Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.mail.ui;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.mail.R;

/**
 * A custom {@link View} that exposes an action to the user.
 */
public class ActionableToastBar extends LinearLayout {
    private boolean mHidden = false;
    private Animator mShowAnimation;
    private Animator mHideAnimation;
    private final Runnable mRunnable;
    private final Handler mFadeOutHandler;

    /** How long toast will last in ms */
    private static final long TOAST_LIFETIME = 15*1000L;

    /** Icon for the description. */
    private ImageView mActionDescriptionIcon;
    /** The clickable view */
    private View mActionButton;
    /** Icon for the action button. */
    private View mActionIcon;
    /** The view that contains the description. */
    private TextView mActionDescriptionView;
    /** The view that contains the text for the action button. */
    private TextView mActionText;
    private ToastBarOperation mOperation;

    public ActionableToastBar(Context context) {
        this(context, null);
    }

    public ActionableToastBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ActionableToastBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mFadeOutHandler = new Handler();
        mRunnable = new Runnable() {
            @Override
            public void run() {
                if(!mHidden) {
                    hide(true, false /* actionClicked */);
                }
            }
        };
        LayoutInflater.from(context).inflate(R.layout.actionable_toast_row, this, true);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mActionDescriptionIcon = (ImageView) findViewById(R.id.description_icon);
        mActionDescriptionView = (TextView) findViewById(R.id.description_text);
        mActionButton = findViewById(R.id.action_button);
        mActionIcon = findViewById(R.id.action_icon);
        mActionText = (TextView) findViewById(R.id.action_text);
    }

    /**
     * Displays the toast bar and makes it visible. Allows the setting of
     * parameters to customize the display.
     * @param listener Performs some action when the action button is clicked.
     *                 If the {@link ToastBarOperation} overrides
     *                 {@link ToastBarOperation#shouldTakeOnActionClickedPrecedence()}
     *                 to return <code>true</code>, the
     *                 {@link ToastBarOperation#onActionClicked(android.content.Context)}
     *                 will override this listener and be called instead.
     * @param descriptionIconResourceId resource ID for the description icon or
     *                                  0 if no icon should be shown
     * @param descriptionText a description text to show in the toast bar
     * @param showActionIcon if true, the action button icon should be shown
     * @param actionTextResource resource ID for the text to show in the action button
     * @param replaceVisibleToast if true, this toast should replace any currently visible toast.
     *                            Otherwise, skip showing this toast.
     * @param op the operation that corresponds to the specific toast being shown
     */
    public void show(final ActionClickedListener listener, int descriptionIconResourceId,
            CharSequence descriptionText, boolean showActionIcon, int actionTextResource,
            boolean replaceVisibleToast, final ToastBarOperation op) {

        if (!mHidden && !replaceVisibleToast) {
            return;
        }
        // Remove any running delayed animations first
        mFadeOutHandler.removeCallbacks(mRunnable);

        mOperation = op;

        mActionButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View widget) {
                if (op.shouldTakeOnActionClickedPrecedence()) {
                    op.onActionClicked(getContext());
                } else {
                    listener.onActionClicked(getContext());
                }
                hide(true /* animate */, true /* actionClicked */);
            }
        });

        // Set description icon.
        if (descriptionIconResourceId == 0) {
            mActionDescriptionIcon.setVisibility(GONE);
        } else {
            mActionDescriptionIcon.setVisibility(VISIBLE);
            mActionDescriptionIcon.setImageResource(descriptionIconResourceId);
        }

        mActionDescriptionView.setText(descriptionText);
        mActionIcon.setVisibility(showActionIcon ? VISIBLE : GONE);
        mActionText.setText(actionTextResource);

        mHidden = false;
        getShowAnimation().start();

        // Set up runnable to execute hide toast once delay is completed
        mFadeOutHandler.postDelayed(mRunnable, TOAST_LIFETIME);
    }

    public ToastBarOperation getOperation() {
        return mOperation;
    }

    /**
     * Hides the view and resets the state.
     */
    public void hide(boolean animate, boolean actionClicked) {
        mHidden = true;
        mFadeOutHandler.removeCallbacks(mRunnable);
        if (getVisibility() == View.VISIBLE) {
            mActionDescriptionView.setText("");
            mActionButton.setOnClickListener(null);
            // Hide view once it's clicked.
            if (animate) {
                getHideAnimation().start();
            } else {
                setAlpha(0);
                setVisibility(View.GONE);
            }

            if (!actionClicked && mOperation != null) {
                mOperation.onToastBarTimeout(getContext());
            }
        }
    }

    private Animator getShowAnimation() {
        if (mShowAnimation == null) {
            mShowAnimation = AnimatorInflater.loadAnimator(getContext(),
                    R.anim.fade_in);
            mShowAnimation.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    setVisibility(View.VISIBLE);
                }
                @Override
                public void onAnimationEnd(Animator animation) {
                }
                @Override
                public void onAnimationCancel(Animator animation) {
                }
                @Override
                public void onAnimationRepeat(Animator animation) {
                }
            });
            mShowAnimation.setTarget(this);
        }
        return mShowAnimation;
    }

    private Animator getHideAnimation() {
        if (mHideAnimation == null) {
            mHideAnimation = AnimatorInflater.loadAnimator(getContext(),
                    R.anim.fade_out);
            mHideAnimation.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                }
                @Override
                public void onAnimationRepeat(Animator animation) {
                }
                @Override
                public void onAnimationEnd(Animator animation) {
                    setVisibility(View.GONE);
                }
                @Override
                public void onAnimationCancel(Animator animation) {
                }
            });
            mHideAnimation.setTarget(this);
        }
        return mHideAnimation;
    }

    public boolean isEventInToastBar(MotionEvent event) {
        if (!isShown()) {
            return false;
        }
        int[] xy = new int[2];
        float x = event.getX();
        float y = event.getY();
        getLocationOnScreen(xy);
        return (x > xy[0] && x < (xy[0] + getWidth()) && y > xy[1] && y < xy[1] + getHeight());
    }

    public boolean isAnimating() {
        return mShowAnimation != null && mShowAnimation.isStarted();
    }

    @Override
    public void onDetachedFromWindow() {
        mFadeOutHandler.removeCallbacks(mRunnable);
        super.onDetachedFromWindow();
    }

    /**
     * Classes that wish to perform some action when the action button is clicked
     * should implement this interface.
     */
    public interface ActionClickedListener {
        public void onActionClicked(Context context);
    }
}
