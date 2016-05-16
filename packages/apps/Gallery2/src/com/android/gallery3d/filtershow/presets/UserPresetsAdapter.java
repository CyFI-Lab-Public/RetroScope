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

package com.android.gallery3d.filtershow.presets;

import android.content.Context;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.category.Action;
import com.android.gallery3d.filtershow.category.CategoryView;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import com.android.gallery3d.filtershow.filters.FilterUserPresetRepresentation;

import java.util.ArrayList;

public class UserPresetsAdapter extends ArrayAdapter<Action>
        implements View.OnClickListener, View.OnFocusChangeListener {
    private static final String LOGTAG = "UserPresetsAdapter";
    private LayoutInflater mInflater;
    private int mIconSize = 160;
    private ArrayList<FilterUserPresetRepresentation> mDeletedRepresentations =
            new ArrayList<FilterUserPresetRepresentation>();
    private ArrayList<FilterUserPresetRepresentation> mChangedRepresentations =
            new ArrayList<FilterUserPresetRepresentation>();
    private EditText mCurrentEditText;

    public UserPresetsAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
        mInflater = LayoutInflater.from(context);
        mIconSize = context.getResources().getDimensionPixelSize(R.dimen.category_panel_icon_size);
    }

    public UserPresetsAdapter(Context context) {
        this(context, 0);
    }

    @Override
    public void add(Action action) {
        super.add(action);
        action.setAdapter(this);
    }

    private void deletePreset(Action action) {
        FilterRepresentation rep = action.getRepresentation();
        if (rep instanceof FilterUserPresetRepresentation) {
            mDeletedRepresentations.add((FilterUserPresetRepresentation) rep);
        }
        remove(action);
        notifyDataSetChanged();
    }

    private void changePreset(Action action) {
        FilterRepresentation rep = action.getRepresentation();
        rep.setName(action.getName());
        if (rep instanceof FilterUserPresetRepresentation) {
            mChangedRepresentations.add((FilterUserPresetRepresentation) rep);
        }
    }

    public void updateCurrent() {
        if (mCurrentEditText != null) {
            updateActionFromEditText(mCurrentEditText);
        }
    }

    static class UserPresetViewHolder {
        ImageView imageView;
        EditText editText;
        ImageButton deleteButton;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        UserPresetViewHolder viewHolder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.filtershow_presets_management_row, null);
            viewHolder = new UserPresetViewHolder();
            viewHolder.imageView = (ImageView) convertView.findViewById(R.id.imageView);
            viewHolder.editText = (EditText) convertView.findViewById(R.id.editView);
            viewHolder.deleteButton = (ImageButton) convertView.findViewById(R.id.deleteUserPreset);
            viewHolder.editText.setOnClickListener(this);
            viewHolder.editText.setOnFocusChangeListener(this);
            viewHolder.deleteButton.setOnClickListener(this);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (UserPresetViewHolder) convertView.getTag();
        }
        Action action = getItem(position);
        viewHolder.imageView.setImageBitmap(action.getImage());
        if (action.getImage() == null) {
            // queue image rendering for this action
            action.setImageFrame(new Rect(0, 0, mIconSize, mIconSize), CategoryView.VERTICAL);
        }
        viewHolder.deleteButton.setTag(action);
        viewHolder.editText.setTag(action);
        viewHolder.editText.setHint(action.getName());

        return convertView;
    }

    public ArrayList<FilterUserPresetRepresentation> getDeletedRepresentations() {
        return mDeletedRepresentations;
    }

    public void clearDeletedRepresentations() {
        mDeletedRepresentations.clear();
    }

    public ArrayList<FilterUserPresetRepresentation> getChangedRepresentations() {
        return mChangedRepresentations;
    }

    public void clearChangedRepresentations() {
        mChangedRepresentations.clear();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.editView:
                v.requestFocus();
                break;
            case R.id.deleteUserPreset:
                Action action = (Action) v.getTag();
                deletePreset(action);
                break;
        }
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (v.getId() != R.id.editView) {
            return;
        }
        EditText editText = (EditText) v;
        if (!hasFocus) {
            updateActionFromEditText(editText);
        } else {
            mCurrentEditText = editText;
        }
    }

    private void updateActionFromEditText(EditText editText) {
        Action action = (Action) editText.getTag();
        String newName = editText.getText().toString();
        if (newName.length() > 0) {
            action.setName(editText.getText().toString());
            changePreset(action);
        }
    }
}
