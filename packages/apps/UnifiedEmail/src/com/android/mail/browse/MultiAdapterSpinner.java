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

import com.android.mail.browse.MergedAdapter.ListSpinnerAdapter;
import com.android.mail.browse.MergedAdapter.LocalAdapterPosition;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListPopupWindow;
import android.widget.ListView;


/**
 * <p>A spinner-like widget that combines data and views from multiple adapters (via MergedAdapter)
 * and forwards certain events to those adapters. This widget also supports clickable but
 * unselectable dropdown items, useful when displaying extra items that should not affect spinner
 * selection state.</p>
 *
 * <p>The framework's Spinner widget can't be extended for this task because it uses a private list
 * adapter (which prevents setting multiple item types) and hides access to its popup, which is
 * useful for clients to know about (like when it's been opened).</p>
 *
 * <p>Clients must provide a set of adapters which the widget will use to load dropdown views,
 * receive callbacks, and load the selected item's view.</p>
 *
 * <p>Apps incorporating this widget must declare a custom attribute: "dropDownWidth" under the
 * "MultiAdapterSpinner" name as a "reference" format (see Gmail's attrs.xml file for an
 * example). This attribute controls the width of the dropdown, similar to the attribute in the
 * framework's Spinner widget.</p>
 *
 */
public class MultiAdapterSpinner extends FrameLayout
    implements AdapterView.OnItemClickListener, View.OnClickListener {

    protected MergedAdapter<FancySpinnerAdapter> mAdapter;
    protected ListPopupWindow mPopup;

    private int mSelectedPosition = -1;
    private Rect mTempRect = new Rect();

    /**
     * A basic adapter with some callbacks added so clients can be involved in spinner behavior.
     */
    public interface FancySpinnerAdapter extends ListSpinnerAdapter {
        /**
         * Whether or not an item at position should become the new selected spinner item and change
         * the spinner item view.
         */
        boolean canSelect(int position);
        /**
         * Handle a click on an enabled item.
         */
        void onClick(int position);
        /**
         * Fired when the popup window is about to be displayed.
         */
        void onShowPopup();
    }

    private static class MergedSpinnerAdapter extends MergedAdapter<FancySpinnerAdapter> {
        /**
         * ListPopupWindow uses getView() but spinners return dropdown views in getDropDownView().
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return super.getDropDownView(position, convertView, parent);
        }
    }

    public MultiAdapterSpinner(Context context) {
        this(context, null);
    }

    public MultiAdapterSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);

        mAdapter = new MergedSpinnerAdapter();
        mPopup = new ListPopupWindow(context, attrs);
        mPopup.setAnchorView(this);
        mPopup.setOnItemClickListener(this);
        mPopup.setModal(true);
        mPopup.setAdapter(mAdapter);
    }

    public void setAdapters(FancySpinnerAdapter... adapters) {
        mAdapter.setAdapters(adapters);
    }

    public void setSelectedItem(final FancySpinnerAdapter adapter, final int position) {
        int globalPosition = 0;
        boolean found = false;

        for (int i = 0, count = mAdapter.getSubAdapterCount(); i < count; i++) {
            ListSpinnerAdapter a = mAdapter.getSubAdapter(i);
            if (a == adapter) {
                globalPosition += position;
                found = true;
                break;
            }
            globalPosition += a.getCount();
        }
        if (found) {
            if (adapter.canSelect(position)) {
                removeAllViews();
                View itemView = adapter.getView(position, null, this);
                itemView.setClickable(true);
                itemView.setOnClickListener(this);
                addView(itemView);

                if (position < adapter.getCount()) {
                    mSelectedPosition = globalPosition;
                }
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (!mPopup.isShowing()) {

            for (int i = 0, count = mAdapter.getSubAdapterCount(); i < count; i++) {
                mAdapter.getSubAdapter(i).onShowPopup();
            }

            final int spinnerPaddingLeft = getPaddingLeft();
            final Drawable background = mPopup.getBackground();
            int bgOffset = 0;
            if (background != null) {
                background.getPadding(mTempRect);
                bgOffset = -mTempRect.left;
            }
            mPopup.setHorizontalOffset(bgOffset + spinnerPaddingLeft);
            mPopup.show();
            mPopup.getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            mPopup.setSelection(mSelectedPosition);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        if (position != mSelectedPosition) {
            final LocalAdapterPosition<FancySpinnerAdapter> result =
                mAdapter.getAdapterOffsetForItem(position);

            if (result.mAdapter.canSelect(result.mLocalPosition)) {
                mSelectedPosition = position;
            } else {
                mPopup.clearListSelection();
            }

            post(new Runnable() {
                @Override
                public void run() {
                    result.mAdapter.onClick(result.mLocalPosition);
                }
            });
        }

        mPopup.dismiss();
    }

}
