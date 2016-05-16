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
 * limitations under the License
 */

package com.android.incallui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.google.common.base.Preconditions;

import java.util.ArrayList;

/**
 *
 */
public class AnswerFragment extends BaseFragment<AnswerPresenter, AnswerPresenter.AnswerUi>
        implements GlowPadWrapper.AnswerListener, AnswerPresenter.AnswerUi {

    /**
     * The popup showing the list of canned responses.
     *
     * This is an AlertDialog containing a ListView showing the possible choices.  This may be null
     * if the InCallScreen hasn't ever called showRespondViaSmsPopup() yet, or if the popup was
     * visible once but then got dismissed.
     */
    private Dialog mCannedResponsePopup = null;

    /**
     * The popup showing a text field for users to type in their custom message.
     */
    private AlertDialog mCustomMessagePopup = null;

    private ArrayAdapter<String> mTextResponsesAdapter = null;

    private GlowPadWrapper mGlowpad;

    public AnswerFragment() {
    }

    @Override
    public AnswerPresenter createPresenter() {
        return new AnswerPresenter();
    }

    @Override
    AnswerPresenter.AnswerUi getUi() {
        return this;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mGlowpad = (GlowPadWrapper) inflater.inflate(R.layout.answer_fragment,
                container, false);

        Log.d(this, "Creating view for answer fragment ", this);
        Log.d(this, "Created from activity", getActivity());
        mGlowpad.setAnswerListener(this);

        return mGlowpad;
    }

    @Override
    public void onDestroyView() {
        Log.d(this, "onDestroyView");
        if (mGlowpad != null) {
            mGlowpad.stopPing();
            mGlowpad = null;
        }
        super.onDestroyView();
    }

    @Override
    public void showAnswerUi(boolean show) {
        getView().setVisibility(show ? View.VISIBLE : View.GONE);

        Log.d(this, "Show answer UI: " + show);
        if (show) {
            mGlowpad.startPing();
        } else {
            mGlowpad.stopPing();
        }
    }

    @Override
    public void showTextButton(boolean show) {
        final int targetResourceId = show
                ? R.array.incoming_call_widget_3way_targets
                : R.array.incoming_call_widget_2way_targets;

        if (targetResourceId != mGlowpad.getTargetResourceId()) {
            if (show) {
                // Answer, Decline, and Respond via SMS.
                mGlowpad.setTargetResources(targetResourceId);
                mGlowpad.setTargetDescriptionsResourceId(
                        R.array.incoming_call_widget_3way_target_descriptions);
                mGlowpad.setDirectionDescriptionsResourceId(
                        R.array.incoming_call_widget_3way_direction_descriptions);
            } else {
                // Answer or Decline.
                mGlowpad.setTargetResources(targetResourceId);
                mGlowpad.setTargetDescriptionsResourceId(
                        R.array.incoming_call_widget_2way_target_descriptions);
                mGlowpad.setDirectionDescriptionsResourceId(
                        R.array.incoming_call_widget_2way_direction_descriptions);
            }

            mGlowpad.reset(false);
        }
    }

    @Override
    public void showMessageDialog() {
        final ListView lv = new ListView(getActivity());

        Preconditions.checkNotNull(mTextResponsesAdapter);
        lv.setAdapter(mTextResponsesAdapter);
        lv.setOnItemClickListener(new RespondViaSmsItemClickListener());

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).setCancelable(
                true).setView(lv);
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                if (mGlowpad != null) {
                    mGlowpad.startPing();
                }
                dismissCannedResponsePopup();
                getPresenter().onDismissDialog();
            }
        });
        mCannedResponsePopup = builder.create();
        mCannedResponsePopup.show();
    }

    private boolean isCannedResponsePopupShowing() {
        if (mCannedResponsePopup != null) {
            return mCannedResponsePopup.isShowing();
        }
        return false;
    }

    private boolean isCustomMessagePopupShowing() {
        if (mCustomMessagePopup != null) {
            return mCustomMessagePopup.isShowing();
        }
        return false;
    }

    /**
     * Dismiss the canned response list popup.
     *
     * This is safe to call even if the popup is already dismissed, and even if you never called
     * showRespondViaSmsPopup() in the first place.
     */
    private void dismissCannedResponsePopup() {
        if (mCannedResponsePopup != null) {
            mCannedResponsePopup.dismiss();  // safe even if already dismissed
            mCannedResponsePopup = null;
        }
    }

    /**
     * Dismiss the custom compose message popup.
     */
    private void dismissCustomMessagePopup() {
       if (mCustomMessagePopup != null) {
           mCustomMessagePopup.dismiss();
           mCustomMessagePopup = null;
       }
    }

    public void dismissPendingDialogues() {
        if (isCannedResponsePopupShowing()) {
            dismissCannedResponsePopup();
        }

        if (isCustomMessagePopupShowing()) {
            dismissCustomMessagePopup();
        }
    }

    public boolean hasPendingDialogs() {
        return !(mCannedResponsePopup == null && mCustomMessagePopup == null);
    }

    /**
     * Shows the custom message entry dialog.
     */
    public void showCustomMessageDialog() {
        // Create an alert dialog containing an EditText
        final EditText et = new EditText(getActivity());
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).setCancelable(
                true).setView(et)
                .setPositiveButton(R.string.custom_message_send,
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // The order is arranged in a way that the popup will be destroyed when the
                        // InCallActivity is about to finish.
                        final String textMessage = et.getText().toString().trim();
                        dismissCustomMessagePopup();
                        getPresenter().rejectCallWithMessage(textMessage);
                    }
                })
                .setNegativeButton(R.string.custom_message_cancel,
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dismissCustomMessagePopup();
                        getPresenter().onDismissDialog();
                    }
                })
                .setTitle(R.string.respond_via_sms_custom_message);
        mCustomMessagePopup = builder.create();

        // Enable/disable the send button based on whether there is a message in the EditText
        et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                final Button sendButton = mCustomMessagePopup.getButton(
                        DialogInterface.BUTTON_POSITIVE);
                sendButton.setEnabled(s != null && s.toString().trim().length() != 0);
            }
        });

        // Keyboard up, show the dialog
        mCustomMessagePopup.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        mCustomMessagePopup.show();

        // Send button starts out disabled
        final Button sendButton = mCustomMessagePopup.getButton(DialogInterface.BUTTON_POSITIVE);
        sendButton.setEnabled(false);
    }

    @Override
    public void configureMessageDialog(ArrayList<String> textResponses) {
        final ArrayList<String> textResponsesForDisplay = new ArrayList<String>(textResponses);

        textResponsesForDisplay.add(getResources().getString(
                R.string.respond_via_sms_custom_message));
        mTextResponsesAdapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_list_item_1, android.R.id.text1, textResponsesForDisplay);
    }

    @Override
    public void onAnswer() {
        getPresenter().onAnswer();
    }

    @Override
    public void onDecline() {
        getPresenter().onDecline();
    }

    @Override
    public void onText() {
        getPresenter().onText();
    }

    /**
     * OnItemClickListener for the "Respond via SMS" popup.
     */
    public class RespondViaSmsItemClickListener implements AdapterView.OnItemClickListener {

        /**
         * Handles the user selecting an item from the popup.
         */
        @Override
        public void onItemClick(AdapterView<?> parent,  // The ListView
                View view,  // The TextView that was clicked
                int position, long id) {
            Log.d(this, "RespondViaSmsItemClickListener.onItemClick(" + position + ")...");
            final String message = (String) parent.getItemAtPosition(position);
            Log.v(this, "- message: '" + message + "'");
            dismissCannedResponsePopup();

            // The "Custom" choice is a special case.
            // (For now, it's guaranteed to be the last item.)
            if (position == (parent.getCount() - 1)) {
                // Show the custom message dialog
                showCustomMessageDialog();
            } else {
                getPresenter().rejectCallWithMessage(message);
            }
        }
    }
}
