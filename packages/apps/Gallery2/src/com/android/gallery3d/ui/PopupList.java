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

package com.android.gallery3d.ui;

import android.content.Context;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.android.gallery3d.R;

import java.util.ArrayList;

public class PopupList {

    public static interface OnPopupItemClickListener {
        public boolean onPopupItemClick(int itemId);
    }

    public static class Item {
        public final int id;
        public String title;

        public Item(int id, String title) {
            this.id = id;
            this.title = title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }

    private final Context mContext;
    private final View mAnchorView;
    private final ArrayList<Item> mItems = new ArrayList<Item>();
    private PopupWindow mPopupWindow;
    private ListView mContentList;
    private OnPopupItemClickListener mOnPopupItemClickListener;
    private int mPopupOffsetX;
    private int mPopupOffsetY;
    private int mPopupWidth;
    private int mPopupHeight;

    public PopupList(Context context, View anchorView) {
        mContext = context;
        mAnchorView = anchorView;
    }

    public void setOnPopupItemClickListener(OnPopupItemClickListener listener) {
        mOnPopupItemClickListener = listener;
    }

    public void addItem(int id, String title) {
        mItems.add(new Item(id, title));
    }

    public void clearItems() {
        mItems.clear();
    }

    private final PopupWindow.OnDismissListener mOnDismissListener =
            new PopupWindow.OnDismissListener() {
        @SuppressWarnings("deprecation")
        @Override
        public void onDismiss() {
            if (mPopupWindow == null) return;
            mPopupWindow = null;
            ViewTreeObserver observer = mAnchorView.getViewTreeObserver();
            if (observer.isAlive()) {
                // We used the deprecated function for backward compatibility
                // The new "removeOnGlobalLayoutListener" is introduced in API level 16
                observer.removeGlobalOnLayoutListener(mOnGLobalLayoutListener);
            }
        }
    };

    private final OnItemClickListener mOnItemClickListener =
            new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (mPopupWindow == null) return;
            mPopupWindow.dismiss();
            if (mOnPopupItemClickListener != null) {
                mOnPopupItemClickListener.onPopupItemClick((int) id);
            }
        }
    };

    private final OnGlobalLayoutListener mOnGLobalLayoutListener =
            new OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            if (mPopupWindow == null) return;
            updatePopupLayoutParams();
            // Need to update the position of the popup window
            mPopupWindow.update(mAnchorView,
                    mPopupOffsetX, mPopupOffsetY, mPopupWidth, mPopupHeight);
        }
    };

    public void show() {
        if (mPopupWindow != null) return;
        mAnchorView.getViewTreeObserver()
                .addOnGlobalLayoutListener(mOnGLobalLayoutListener);
        mPopupWindow = createPopupWindow();
        updatePopupLayoutParams();
        mPopupWindow.setWidth(mPopupWidth);
        mPopupWindow.setHeight(mPopupHeight);
        mPopupWindow.showAsDropDown(mAnchorView, mPopupOffsetX, mPopupOffsetY);
    }

    private void updatePopupLayoutParams() {
        ListView content = mContentList;
        PopupWindow popup = mPopupWindow;

        Rect p = new Rect();
        popup.getBackground().getPadding(p);

        int maxHeight = mPopupWindow.getMaxAvailableHeight(mAnchorView) - p.top - p.bottom;
        mContentList.measure(
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST));
        mPopupWidth = content.getMeasuredWidth() + p.top + p.bottom;
        mPopupHeight = Math.min(maxHeight, content.getMeasuredHeight() + p.left + p.right);
        mPopupOffsetX = -p.left;
        mPopupOffsetY = -p.top;
    }

    private PopupWindow createPopupWindow() {
        PopupWindow popup = new PopupWindow(mContext);
        popup.setOnDismissListener(mOnDismissListener);

        popup.setBackgroundDrawable(mContext.getResources().getDrawable(
                R.drawable.menu_dropdown_panel_holo_dark));

        mContentList = new ListView(mContext, null,
                android.R.attr.dropDownListViewStyle);
        mContentList.setAdapter(new ItemDataAdapter());
        mContentList.setOnItemClickListener(mOnItemClickListener);
        popup.setContentView(mContentList);
        popup.setFocusable(true);
        popup.setOutsideTouchable(true);

        return popup;
    }

    public Item findItem(int id) {
        for (Item item : mItems) {
            if (item.id == id) return item;
        }
        return null;
    }

    private class ItemDataAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return mItems.size();
        }

        @Override
        public Object getItem(int position) {
            return mItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return mItems.get(position).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext)
                        .inflate(R.layout.popup_list_item, null);
            }
            TextView text = (TextView) convertView.findViewById(android.R.id.text1);
            text.setText(mItems.get(position).title);
            return convertView;
        }
    }
}
