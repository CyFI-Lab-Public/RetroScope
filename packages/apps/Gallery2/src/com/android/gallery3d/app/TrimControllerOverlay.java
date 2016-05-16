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

package com.android.gallery3d.app;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.view.MotionEvent;
import android.view.View;

import com.android.gallery3d.common.ApiHelper;

/**
 * The controller for the Trimming Video.
 */
public class TrimControllerOverlay extends CommonControllerOverlay  {

    public TrimControllerOverlay(Context context) {
        super(context);
    }

    @Override
    protected void createTimeBar(Context context) {
        mTimeBar = new TrimTimeBar(context, this);
    }

    private void hidePlayButtonIfPlaying() {
        if (mState == State.PLAYING) {
            mPlayPauseReplayView.setVisibility(View.INVISIBLE);
        }
        if (ApiHelper.HAS_OBJECT_ANIMATION) {
            mPlayPauseReplayView.setAlpha(1f);
        }
    }

    @Override
    public void showPlaying() {
        super.showPlaying();
        if (ApiHelper.HAS_OBJECT_ANIMATION) {
            // Add animation to hide the play button while playing.
            ObjectAnimator anim = ObjectAnimator.ofFloat(mPlayPauseReplayView, "alpha", 1f, 0f);
            anim.setDuration(200);
            anim.start();
            anim.addListener(new AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    hidePlayButtonIfPlaying();
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    hidePlayButtonIfPlaying();
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }
            });
        } else {
            hidePlayButtonIfPlaying();
        }
    }

    @Override
    public void setTimes(int currentTime, int totalTime, int trimStartTime, int trimEndTime) {
        mTimeBar.setTime(currentTime, totalTime, trimStartTime, trimEndTime);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (super.onTouchEvent(event)) {
            return true;
        }

        // The special thing here is that the State.ENDED include both cases of
        // the video completed and current == trimEnd. Both request a replay.
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (mState == State.PLAYING || mState == State.PAUSED) {
                    mListener.onPlayPause();
                } else if (mState == State.ENDED) {
                    if (mCanReplay) {
                        mListener.onReplay();
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                break;
        }
        return true;
    }
}
