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

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.os.Handler;

/**
 * MinTimeProgressDialog implements a ProgressDialog that waits MIN_DELAY ms to
 * be dismissed before showing. Once visible, the dialog will be visible for at
 * least MIN_SHOW_TIME to avoid "flashes" in the UI when an event could take a
 * largely variable time to complete (from none, to a user perceivable amount)
 *
 * @author mindyp
 */
public class MinTimeProgressDialog extends ProgressDialog implements OnShowListener {
    private static int sMinShowTime;

    private static int sMinDelay;

    private int mMinShowTime = -1;

    private long mStartTime = -1;

    private boolean mDismissed = false;

    private final Handler mHandler = new Handler();

    private final Runnable mDelayedDismiss = new Runnable() {
        @Override
        public void run() {
            MinTimeProgressDialog.super.dismiss();
        }
    };

    private final Runnable mDelayedShow = new Runnable() {
        @Override
        public void run() {
            if (!mDismissed) {
                MinTimeProgressDialog.super.show();
            }
        }
    };

    public MinTimeProgressDialog(Context context) {
        super(context, R.style.MinTimeProgressDialogStyle);
        sMinShowTime = context.getResources()
            .getInteger(R.integer.batch_progress_display_time);
        sMinDelay = context.getResources()
            .getInteger(R.integer.batch_progress_wait_time);
        mMinShowTime = sMinShowTime
                + context.getResources().getInteger(R.integer.dialog_animationDefaultDur);
    }

    @Override
    public void dismiss() {
        mDismissed = true;
        long diff = System.currentTimeMillis() - mStartTime;
        if (diff >= mMinShowTime || mStartTime == -1) {
            // This covers the case where the dialog was not shown
            // at all yet OR enough time of the dialog showing
            // has passed. If it wasn't shown at all yet, then it is
            // just never shown.
            super.dismiss();
        } else {
            mHandler.postDelayed(mDelayedDismiss, mMinShowTime - diff);
        }
    }

    /**
     * Dismiss the dialog, immediately if necessary.
     *
     * @param force If true, dismiss the dialog right away.
     */
    public void dismiss(boolean force) {
        if (force) {
            mDismissed = true;
            super.dismiss();
        } else {
            dismiss();
        }
    }

    @Override
    public void show() {
        mDismissed = false;
        mHandler.postDelayed(mDelayedShow, sMinDelay);
    }

    @Override
    public void onShow(DialogInterface dialog) {
        // When the dialog is actually shown, start the timer.
        mStartTime = System.currentTimeMillis();
    }

    /**
     * Show a MinTimeProgressDialog.
     */
    public static MinTimeProgressDialog show(Context context, CharSequence title,
            CharSequence message, boolean indeterminate, boolean cancelable,
            OnCancelListener cancelListener) {

        MinTimeProgressDialog dialog = new MinTimeProgressDialog(context);
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setIndeterminate(indeterminate);
        dialog.setCancelable(cancelable);
        dialog.setOnCancelListener(cancelListener);
        dialog.setOnShowListener(dialog);
        dialog.show();

        return dialog;
    }
}
