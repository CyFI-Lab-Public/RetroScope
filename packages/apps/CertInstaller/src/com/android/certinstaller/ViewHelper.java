/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.certinstaller;

import android.view.View;
import android.widget.TextView;

/**
 * A helper class for handling text views in the dialogs.
 */
class ViewHelper {
    private View mView;
    private boolean mHasEmptyError;

    void setView(View view) {
        mView = view;
    }

    void showError(int msgId) {
        TextView v = (TextView) mView.findViewById(R.id.error);
        v.setText(msgId);
        if (v != null) v.setVisibility(View.VISIBLE);
    }

    String getText(int viewId) {
        return ((TextView) mView.findViewById(viewId)).getText().toString();
    }

    void setText(int viewId, String text) {
        if (text == null) return;
        TextView v = (TextView) mView.findViewById(viewId);
        if (v != null) v.setText(text);
    }

    void setText(int viewId, int textId) {
        TextView v = (TextView) mView.findViewById(viewId);
        if (v != null) v.setText(textId);
    }

    void setHasEmptyError(boolean hasEmptyError) {
        mHasEmptyError = hasEmptyError;
    }

    boolean getHasEmptyError() {
        return mHasEmptyError;
    }
}
