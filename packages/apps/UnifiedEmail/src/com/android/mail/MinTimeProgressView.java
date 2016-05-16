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
package com.android.mail;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ProgressBar;

/**
 * MinTimeProgressView implements a ProgressBar that waits MIN_DELAY ms to be
 * dismissed before showing. Once visible, the progress bar will be visible for
 * at least MIN_SHOW_TIME to avoid "flashes" in the UI when an event could take
 * a largely variable time to complete (from none, to a user perceivable amount)
 *
 * @author mindyp
 */
public class MinTimeProgressView extends ProgressBar {
    private static int sMinShowTime;

    private static int sMinDelay;

    private long mStartTime = -1;

    private final Handler mHandler = new Handler();

    private boolean mDismissed = false;

    public MinTimeProgressView(Context context) {
        this(context, null);
    }

    public MinTimeProgressView(Context context, AttributeSet attrs) {
        super(context, attrs, R.style.MinTimeProgressViewStyle);
        sMinShowTime = context.getResources()
            .getInteger(R.integer.html_conv_progress_display_time);
        sMinDelay = context.getResources()
            .getInteger(R.integer.html_conv_progress_wait_time);
    }

    private final Runnable mDelayedHide = new Runnable() {
        @Override
        public void run() {
            MinTimeProgressView.super.setVisibility(View.GONE);
        }
    };

    private final Runnable mDelayedShow = new Runnable() {
        @Override
        public void run() {
            if (!mDismissed) {
                mStartTime = System.currentTimeMillis();
                MinTimeProgressView.super.setVisibility(View.VISIBLE);
            }
        }
    };

    private void hide() {
        mDismissed = true;
        long diff = System.currentTimeMillis() - mStartTime;
        if (diff >= sMinShowTime || mStartTime == -1) {
            // The progress spinner has been shown long enough
            // OR was not shown yet. If it wasn't shown yet,
            // it will just never be shown.
            MinTimeProgressView.super.setVisibility(View.GONE);
        } else {
            // The progress spinner is shown, but not long enough,
            // so put a delayed message in to hide it when its been
            // shown long enough.
            mHandler.postDelayed(mDelayedHide, sMinShowTime - diff);
        }
    }

    private void show() {
        // Reset the start time.
        mStartTime = -1;
        mHandler.postDelayed(mDelayedShow, sMinDelay);
    }

    @Override
    public void setVisibility(int visibility) {
        // Whenever the visibility gets changed, clear dismissed
        // state.
        mDismissed = false;
        switch (visibility) {
            case View.VISIBLE:
                show();
                break;
            case View.GONE:
                hide();
                break;
            default:
                super.setVisibility(visibility);
        }
    }
}
