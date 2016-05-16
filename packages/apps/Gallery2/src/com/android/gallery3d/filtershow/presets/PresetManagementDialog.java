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

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.FilterShowActivity;

public class PresetManagementDialog extends DialogFragment implements View.OnClickListener {
    private UserPresetsAdapter mAdapter;
    private EditText mEditText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.filtershow_presets_management_dialog, container);

        FilterShowActivity activity = (FilterShowActivity) getActivity();
        mAdapter = activity.getUserPresetsAdapter();
        mEditText = (EditText) view.findViewById(R.id.editView);
        view.findViewById(R.id.cancel).setOnClickListener(this);
        view.findViewById(R.id.ok).setOnClickListener(this);
        getDialog().setTitle(getString(R.string.filtershow_save_preset));
        return view;
    }

    @Override
    public void onClick(View v) {
        FilterShowActivity activity = (FilterShowActivity) getActivity();
        switch (v.getId()) {
            case R.id.cancel:
                mAdapter.clearChangedRepresentations();
                mAdapter.clearDeletedRepresentations();
                activity.updateUserPresetsFromAdapter(mAdapter);
                dismiss();
                break;
            case R.id.ok:
                String text = String.valueOf(mEditText.getText());
                activity.saveCurrentImagePreset(text);
                mAdapter.updateCurrent();
                activity.updateUserPresetsFromAdapter(mAdapter);
                dismiss();
                break;
        }
    }
}
