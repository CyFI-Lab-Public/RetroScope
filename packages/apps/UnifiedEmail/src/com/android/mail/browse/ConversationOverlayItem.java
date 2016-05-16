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

package com.android.mail.browse;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.CursorAdapter;

import com.android.mail.ui.ConversationViewFragment;
import com.android.mail.utils.LogUtils;

public abstract class ConversationOverlayItem {
    private int mHeight;  // in px
    private int mTop;  // in px
    private boolean mNeedsMeasure;

    public static final String LOG_TAG = ConversationViewFragment.LAYOUT_TAG;

    private int mPosition;

    /**
     * @see Adapter#getItemViewType(int)
     */
    public abstract int getType();
    /**
     * Inflate and perform one-time initialization on a view for later binding.
     */
    public abstract View createView(Context context, LayoutInflater inflater,
            ViewGroup parent);

    /**
     * @see CursorAdapter#bindView(View, Context, android.database.Cursor)
     * @param v a view to bind to
     * @param measureOnly true iff we are binding this view only to measure its height (so items
     * know they can cut certain corners that do not affect a view's height)
     */
    public abstract void bindView(View v, boolean measureOnly);
    /**
     * Returns true if this overlay view is meant to be positioned right on top of the overlay
     * below. This special positioning allows {@link ConversationContainer} to stack overlays
     * together even when zoomed into a conversation, when the overlay spacers spread farther
     * apart.
     */
    public abstract boolean isContiguous();

    /**
     * Returns true if this overlay view is in its expanded state.
     */
    public boolean isExpanded() {
        return true;
    }

    public int getGravity() {
        return Gravity.BOTTOM;
    }

    /**
     * This method's behavior is critical and requires some 'splainin.
     * <p>
     * Subclasses that return a zero-size height to the {@link ConversationContainer} will
     * cause the scrolling/recycling logic there to remove any matching view from the container.
     * The item should switch to returning a non-zero height when its view should re-appear.
     * <p>
     * It's imperative that this method stay in sync with the current height of the HTML spacer
     * that matches this overlay.
     */
    public int getHeight() {
        return mHeight;
    }

    /**
     * Set a new height.
     *
     * @param h a new height
     * @return true if the value changed
     */
    public boolean setHeight(int h) {
        LogUtils.i(LOG_TAG, "IN setHeight=%dpx of overlay item: %s", h, this);
        if (mHeight != h) {
            mHeight = h;
            mNeedsMeasure = true;
            return true;
        }
        return false;
    }

    public int getTop() {
        return mTop;
    }

    public void setTop(int top) {
        mTop = top;
    }

    public boolean isMeasurementValid() {
        return !mNeedsMeasure;
    }

    public void markMeasurementValid() {
        mNeedsMeasure = false;
    }

    public void invalidateMeasurement() {
        mNeedsMeasure = true;
    }

    public boolean canBecomeSnapHeader() {
        return false;
    }

    public boolean canPushSnapHeader() {
        return false;
    }

    public boolean belongsToMessage(ConversationMessage message) {
        return false;
    }

    public void setMessage(ConversationMessage message) {
    }

    /**
     * Given a view that is already bound to this item, force the view to re-render the item's
     * current model data. This is typically called after a data model update, to update the
     * affected view in-place.
     */
    public void onModelUpdated(View v) {
    }

    public void setPosition(int position) {
        mPosition = position;
    }

    public int getPosition() {
        return mPosition;
    }

    /**
     * This is a hack. Now that one view can update the
     * state of another view, we need a mechanism when the
     * view's associated item changes to update the state of the
     * view. Typically, classes that override this class should not
     * override this method.<br><br>
     *
     * This method is used by
     * {@link com.android.mail.browse.ConversationViewAdapter.BorderItem}
     * to update the height of the border based on whether the neighboring messages
     * are collapsed or expanded.<br><br>
     *
     * It is also used by {@link com.android.mail.browse.ConversationViewAdapter.MessageHeaderItem}
     * in the case where the snap header is tapped to collapse the message but the
     * message header is still on screen. Since the message header is still on screen,
     * it does not get bound but will get a rebind.<br><br>
     *
     * The only other way to handle this case would be to call
     * {@link com.android.mail.browse.ConversationViewAdapter#notifyDataSetChanged()}
     * but that makes the entire screen flicker since the entire adapter performs
     * a layout of the every item.
     * @param view the view to be re-bound
     */
    public void rebindView(View view) {
        // DO NOTHING
    }
}
