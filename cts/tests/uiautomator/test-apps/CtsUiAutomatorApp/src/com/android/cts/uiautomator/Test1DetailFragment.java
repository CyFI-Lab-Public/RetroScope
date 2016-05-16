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

package com.android.cts.uiautomator;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

public class Test1DetailFragment extends Fragment {

    public static final String ARG_ITEM_ID = "item_id";
    private Button mSubmitButton;
    private EditText mEditText;
    TestItems.TestItem mItem;

    public Test1DetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItem = TestItems.getTest(getArguments().getString(ARG_ITEM_ID));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        View rootView = inflater.inflate(R.layout.test1_detail_fragment, container, false);
        if (mItem != null) {
            ((EditText) rootView.findViewById(R.id.test1TextField)).setText(mItem.mName);

            mSubmitButton = (Button) rootView.findViewById(R.id.test1SubmitButton);
            mEditText = (EditText) rootView.findViewById(R.id.test1TextField);
            mEditText.setText("");
            mSubmitButton.setOnClickListener(new Button.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final String savedInput = mEditText.getText().toString();
                    // clear so we won't be confused by the input text in
                    // validation
                    mEditText.setText("");
                    // close soft keyboard
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(
                            Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
                    // display the submitted text
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle(R.string.item1_dialog_title);
                    builder.setPositiveButton(R.string.OK, null);
                    builder.setMessage(savedInput);
                    AlertDialog diag = builder.create();
                    diag.show();
                }
            });
        }
        return rootView;
    }
}
