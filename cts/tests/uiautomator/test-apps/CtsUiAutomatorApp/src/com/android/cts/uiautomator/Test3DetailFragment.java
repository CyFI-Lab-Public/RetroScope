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
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class Test3DetailFragment extends Fragment {

    public static final String ARG_ITEM_ID = "item_id";
    private TextView mTextClock;
    private Button mSubmitButton;
    private EditText mEditText;
    private long mCurrentTime;
    private final Object sync = new Object();
    private boolean mRunCounter = true;

    public Test3DetailFragment() {
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

    private final Handler mHandler = new Handler();

    final Runnable mClockRunnable = new Runnable() {
        @Override
        public void run() {
            // call the activity method that updates the UI
            updateClockOnUi();
        }
    };

    private void updateClockOnUi() {
        synchronized (sync) {
            mTextClock.setText("" + mCurrentTime);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        View rootView = inflater.inflate(R.layout.test3_detail_fragment, container, false);
        mTextClock = (TextView) rootView.findViewById(R.id.test3ClockTextView);
        mSubmitButton = (Button) rootView.findViewById(R.id.test3SubmitButton);
        mEditText = (EditText) rootView.findViewById(R.id.test3TextField);
        mSubmitButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                // close soft keyboard
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(
                        Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);

                // display the submitted text
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.test3_dialog_title);
                builder.setPositiveButton(R.string.OK, null);
                CharSequence inputText = mEditText.getText();
                if (inputText != null && !inputText.toString().isEmpty()) {
                    long inputTime = Long.parseLong(inputText.toString());
                    builder.setMessage("" + (mCurrentTime - inputTime));
                } else {
                    builder.setMessage("<NO DATA>");
                }
                AlertDialog diag = builder.create();
                diag.show();
                mEditText.setText("");
                mRunCounter = false;
            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (mRunCounter) {
                    synchronized (sync) {
                        mCurrentTime = SystemClock.elapsedRealtime();
                    }
                    mHandler.post(mClockRunnable);
                    SystemClock.sleep(100);
                }
            }
        }).start();

        return rootView;
    }
}
