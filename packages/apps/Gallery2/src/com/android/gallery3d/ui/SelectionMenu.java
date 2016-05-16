/*
 * Copyright (C) 2010 The Android Open Source Project
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
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.android.gallery3d.R;
import com.android.gallery3d.ui.PopupList.OnPopupItemClickListener;

public class SelectionMenu implements OnClickListener {
    @SuppressWarnings("unused")
    private static final String TAG = "SelectionMenu";

    private final Context mContext;
    private final Button mButton;
    private final PopupList mPopupList;

    public SelectionMenu(Context context, Button button, OnPopupItemClickListener listener) {
        mContext = context;
        mButton = button;
        mPopupList = new PopupList(context, mButton);
        mPopupList.addItem(R.id.action_select_all,
                context.getString(R.string.select_all));
        mPopupList.setOnPopupItemClickListener(listener);
        mButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        mPopupList.show();
    }

    public void updateSelectAllMode(boolean inSelectAllMode) {
        PopupList.Item item = mPopupList.findItem(R.id.action_select_all);
        if (item != null) {
            item.setTitle(mContext.getString(
                    inSelectAllMode ? R.string.deselect_all : R.string.select_all));
        }
    }

    public void setTitle(CharSequence title) {
        mButton.setText(title);
    }
}
