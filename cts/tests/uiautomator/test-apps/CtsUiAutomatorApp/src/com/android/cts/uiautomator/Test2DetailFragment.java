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
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class Test2DetailFragment extends Fragment {
    public static final String ARG_ITEM_ID = "item_id";
    private Button mButton1, mButton2, mButton3, mDynaButton;

    public Test2DetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.test2_detail_activity, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.dialog_title_result);
        builder.setMessage(item.getTitle());
        builder.setPositiveButton(R.string.OK, null);
        AlertDialog diag = builder.create();
        diag.show();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        View rootView = inflater.inflate(R.layout.test2_detail_fragment, container, false);

        mButton1 = (Button) rootView.findViewById(R.id.test2button1);
        mButton2 = (Button) rootView.findViewById(R.id.test2button2);
        mButton3 = (Button) rootView.findViewById(R.id.test2button3);
        mDynaButton = (Button) rootView.findViewById(R.id.test2dynaButton);

        mButton1.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.dialog_title_result);
                builder.setPositiveButton(R.string.OK, null);
                builder.setMessage(R.string.button1);
                AlertDialog diag = builder.create();
                diag.show();
            }
        });

        mButton1.setOnLongClickListener(new Button.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.dialog_title_result);
                builder.setPositiveButton(R.string.OK, null);
                builder.setMessage(R.string.button1long);
                AlertDialog diag = builder.create();
                diag.show();
                return true;
            }
        });

        mButton2.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.dialog_title_result);
                builder.setPositiveButton(R.string.OK, null);
                builder.setMessage(R.string.button2);
                AlertDialog diag = builder.create();
                diag.show();
            }
        });

        mButton2.setOnLongClickListener(new Button.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.dialog_title_result);
                builder.setPositiveButton(R.string.OK, null);
                builder.setMessage(R.string.button2long);
                AlertDialog diag = builder.create();
                diag.show();
                return true;
            }
        });

        mButton3.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.dialog_title_result);
                builder.setPositiveButton(R.string.OK, null);
                builder.setMessage(R.string.button3);
                AlertDialog diag = builder.create();
                diag.show();
            }
        });

        mButton3.setOnLongClickListener(new Button.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.dialog_title_result);
                builder.setPositiveButton(R.string.OK, null);
                builder.setMessage(R.string.button3long);
                AlertDialog diag = builder.create();
                diag.show();
                return true;
            }
        });

        mDynaButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDynaButton.setText(R.string.buttonAfter);
                mDynaButton.setContentDescription(getString(R.string.buttonAfter));
            }
        });

        return rootView;
    }
}
