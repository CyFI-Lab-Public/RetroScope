/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.gallery3d.filtershow.category;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.FilterShowActivity;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import com.android.gallery3d.filtershow.filters.FilterTinyPlanetRepresentation;
import com.android.gallery3d.filtershow.pipeline.ImagePreset;

public class CategoryAdapter extends ArrayAdapter<Action> {

    private static final String LOGTAG = "CategoryAdapter";
    private int mItemHeight;
    private View mContainer;
    private int mItemWidth = ListView.LayoutParams.MATCH_PARENT;
    private int mSelectedPosition;
    int mCategory;
    private int mOrientation;
    private boolean mShowAddButton = false;
    private String mAddButtonText;

    public CategoryAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
        mItemHeight = (int) (context.getResources().getDisplayMetrics().density * 100);
    }

    public CategoryAdapter(Context context) {
        this(context, 0);
    }

    @Override
    public void clear() {
        for (int i = 0; i < getCount(); i++) {
            Action action = getItem(i);
            action.clearBitmap();
        }
        super.clear();
    }

    public void setItemHeight(int height) {
        mItemHeight = height;
    }

    public void setItemWidth(int width) {
        mItemWidth = width;
    }

    @Override
    public void add(Action action) {
        super.add(action);
        action.setAdapter(this);
    }

    public void initializeSelection(int category) {
        mCategory = category;
        mSelectedPosition = -1;
        if (category == MainPanel.LOOKS) {
            mSelectedPosition = 0;
            mAddButtonText = getContext().getString(R.string.filtershow_add_button_looks);
        }
        if (category == MainPanel.BORDERS) {
            mSelectedPosition = 0;
        }
        if (category == MainPanel.VERSIONS) {
            mAddButtonText = getContext().getString(R.string.filtershow_add_button_versions);
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = new CategoryView(getContext());
        }
        CategoryView view = (CategoryView) convertView;
        view.setOrientation(mOrientation);
        Action action = getItem(position);
        view.setAction(action, this);
        int width = mItemWidth;
        int height = mItemHeight;
        if (action.getType() == Action.SPACER) {
            if (mOrientation == CategoryView.HORIZONTAL) {
                width = width / 2;
            } else {
                height = height / 2;
            }
        }
        if (action.getType() == Action.ADD_ACTION
                && mOrientation == CategoryView.VERTICAL) {
            height = height / 2;
        }
        view.setLayoutParams(
                new ListView.LayoutParams(width, height));
        view.setTag(position);
        view.invalidate();
        return view;
    }

    public void setSelected(View v) {
        int old = mSelectedPosition;
        mSelectedPosition = (Integer) v.getTag();
        if (old != -1) {
            invalidateView(old);
        }
        invalidateView(mSelectedPosition);
    }

    public boolean isSelected(View v) {
        return (Integer) v.getTag() == mSelectedPosition;
    }

    private void invalidateView(int position) {
        View child = null;
        if (mContainer instanceof ListView) {
            ListView lv = (ListView) mContainer;
            child = lv.getChildAt(position - lv.getFirstVisiblePosition());
        } else {
            CategoryTrack ct = (CategoryTrack) mContainer;
            child = ct.getChildAt(position);
        }
        if (child != null) {
            child.invalidate();
        }
    }

    public void setContainer(View container) {
        mContainer = container;
    }

    public void imageLoaded() {
        notifyDataSetChanged();
    }

    public FilterRepresentation getTinyPlanet() {
        for (int i = 0; i < getCount(); i++) {
            Action action = getItem(i);
            if (action.getRepresentation() != null
                    && action.getRepresentation()
                    instanceof FilterTinyPlanetRepresentation) {
                return action.getRepresentation();
            }
        }
        return null;
    }

    public void removeTinyPlanet() {
        for (int i = 0; i < getCount(); i++) {
            Action action = getItem(i);
            if (action.getRepresentation() != null
                    && action.getRepresentation()
                    instanceof FilterTinyPlanetRepresentation) {
                super.remove(action);
                return;
            }
        }
    }

    @Override
    public void remove(Action action) {
        if (!(mCategory == MainPanel.VERSIONS
                || mCategory == MainPanel.LOOKS)) {
            return;
        }
        super.remove(action);
        FilterShowActivity activity = (FilterShowActivity) getContext();
        if (mCategory == MainPanel.LOOKS) {
            activity.removeLook(action);
        } else if (mCategory == MainPanel.VERSIONS) {
            activity.removeVersion(action);
        }
    }

    public void setOrientation(int orientation) {
        mOrientation = orientation;
    }

    public void reflectImagePreset(ImagePreset preset) {
        if (preset == null) {
            return;
        }
        int selected = 0; // if nothing found, select "none" (first element)
        FilterRepresentation rep = null;
        if (mCategory == MainPanel.LOOKS) {
            int pos = preset.getPositionForType(FilterRepresentation.TYPE_FX);
            if (pos != -1) {
                rep = preset.getFilterRepresentation(pos);
            }
        } else if (mCategory == MainPanel.BORDERS) {
            int pos = preset.getPositionForType(FilterRepresentation.TYPE_BORDER);
            if (pos != -1) {
                rep = preset.getFilterRepresentation(pos);
            }
        }
        if (rep != null) {
            for (int i = 0; i < getCount(); i++) {
                FilterRepresentation itemRep = getItem(i).getRepresentation();
                if (itemRep == null) {
                    continue;
                }
                if (rep.getName().equalsIgnoreCase(
                        itemRep.getName())) {
                    selected = i;
                    break;
                }
            }
        }
        if (mSelectedPosition != selected) {
            mSelectedPosition = selected;
            this.notifyDataSetChanged();
        }
    }

    public boolean showAddButton() {
        return mShowAddButton;
    }

    public void setShowAddButton(boolean showAddButton) {
        mShowAddButton = showAddButton;
    }

    public String getAddButtonText() {
        return mAddButtonText;
    }
}
