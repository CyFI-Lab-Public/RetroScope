/*
 * Copyright (C) 2013 Google Inc.
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
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.app.Fragment;
import android.content.res.Resources;
import android.os.Handler;
import android.view.View;

import com.android.mail.R;
import com.android.mail.utils.Utils;

/**
 * <p>Controller to handle showing and hiding progress in the conversation views.</p>
 * <br>
 * <b>NOTE:</b> This class makes several assumptions about the view hierarchy
 * of the conversation view. Use with care.
 */
public class ConversationViewProgressController {
    private static int sMinDelay = -1;
    private static int sMinShowTime = -1;

    private final Handler mHandler;

    private final Fragment mFragment;

    private long mLoadingShownTime = -1;
    private View mProgressView;
    private View mBackgroundView;

    private final Runnable mDelayedShow;

    public ConversationViewProgressController(Fragment fragment, Handler handler) {
        mFragment = fragment;
        mHandler = handler;

        mDelayedShow = new FragmentRunnable("mDelayedShow", mFragment) {
            @Override
            public void go() {
                mLoadingShownTime = System.currentTimeMillis();
                mProgressView.setVisibility(View.VISIBLE);
            }
        };
    }

    public void instantiateProgressIndicators(View rootView) {
        mBackgroundView = rootView.findViewById(R.id.background_view);
        mProgressView = rootView.findViewById(R.id.loading_progress);
    }

    public void showLoadingStatus(boolean isFragmentUserVisible) {
        if (!isFragmentUserVisible) {
            return;
        }
        if (sMinDelay == -1) {
            Resources res = mFragment.getResources();
            sMinDelay = res.getInteger(R.integer.conversationview_show_loading_delay);
            sMinShowTime = res.getInteger(R.integer.conversationview_min_show_loading);
        }
        // If the loading view isn't already showing, show it and remove any
        // pending calls to show the loading screen.
        mBackgroundView.setVisibility(View.VISIBLE);
        mHandler.removeCallbacks(mDelayedShow);
        mHandler.postDelayed(mDelayedShow, sMinDelay);
    }

    protected void dismissLoadingStatus() {
        dismissLoadingStatus(null);
    }

    /**
     * Begin the fade-out animation to hide the Progress overlay, either immediately or after some
     * timeout (to ensure that the progress minimum time elapses).
     *
     * @param doAfter an optional Runnable action to execute after the animation completes
     */
    protected void dismissLoadingStatus(final Runnable doAfter) {
        if (mLoadingShownTime == -1) {
            // The runnable hasn't run yet, so just remove it.
            mHandler.removeCallbacks(mDelayedShow);
            dismiss(doAfter);
            return;
        }
        final long diff = Math.abs(System.currentTimeMillis() - mLoadingShownTime);
        if (diff > sMinShowTime) {
            dismiss(doAfter);
        } else {
            mHandler.postDelayed(new FragmentRunnable("dismissLoadingStatus", mFragment) {
                @Override
                public void go() {
                    dismiss(doAfter);
                }
            }, Math.abs(sMinShowTime - diff));
        }
    }

    private void dismiss(final Runnable doAfter) {
        // Reset loading shown time.
        mLoadingShownTime = -1;
        mProgressView.setVisibility(View.GONE);
        if (mBackgroundView.getVisibility() == View.VISIBLE) {
            animateDismiss(doAfter);
        } else {
            if (doAfter != null) {
                doAfter.run();
            }
        }
    }

    private void animateDismiss(final Runnable doAfter) {
        // the animation can only work (and is only worth doing) if this fragment is added
        // reasons it may not be added: fragment is being destroyed, or in the process of being
        // restored
        if (!mFragment.isAdded()) {
            mBackgroundView.setVisibility(View.GONE);
            return;
        }

        Utils.enableHardwareLayer(mBackgroundView);
        final Animator animator = AnimatorInflater.loadAnimator(
                mFragment.getActivity().getApplicationContext(), R.anim.fade_out);
        animator.setTarget(mBackgroundView);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mBackgroundView.setVisibility(View.GONE);
                mBackgroundView.setLayerType(View.LAYER_TYPE_NONE, null);
                if (doAfter != null) {
                    doAfter.run();
                }
            }
        });
        animator.start();
    }
}
