/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.cellbroadcastreceiver.tests;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import java.util.Random;

/**
 * Activity to send test cell broadcast messages from GUI.
 */
public class SendTestBroadcastActivity extends Activity {
    private static final String TAG = "SendTestBroadcastActivity";

    /** Whether to delay before sending test message. */
    private boolean mDelayBeforeSending;

    /** Delay time before sending test message (when box is checked). */
    private static final int DELAY_BEFORE_SENDING_MSEC = 5000;

    private final Handler mDelayHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            // call the onClick() method again, passing null View.
            // The callback will ignore mDelayBeforeSending when the View is null.
            OnClickListener pendingButtonClick = (OnClickListener) msg.obj;
            pendingButtonClick.onClick(null);
        }
    };

    /**
     * Increment the message ID field and return the previous value.
     * @return the current value of the message ID text field
     */
    private int getMessageId() {
        EditText messageIdField = (EditText) findViewById(R.id.message_id);
        int messageId = 0;
        try {
            messageId = Integer.parseInt(messageIdField.getText().toString());
        } catch (NumberFormatException ignored) {
            Log.e(TAG, "Invalid message ID");
        }
        int newMessageId = (messageId + 1) % 65536;
        if (newMessageId == 0) {
            newMessageId = 1;
        }
        messageIdField.setText(String.valueOf(newMessageId));
        return messageId;
    }

    /**
     * Return the value of the category field.
     * @return the current value of the category text field
     */
    private int getCategory() {
        EditText categoryField = (EditText) findViewById(R.id.category_id);
        return Integer.parseInt(categoryField.getText().toString());
    }

    /**
     * Initialization of the Activity after it is first created.  Must at least
     * call {@link android.app.Activity#setContentView(int)} to
     * describe what is to be displayed in the screen.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.test_buttons);

        /* Set message ID to a random value from 1-65535. */
        EditText messageIdField = (EditText) findViewById(R.id.message_id);
        messageIdField.setText(String.valueOf(new Random().nextInt(65535) + 1));

        /* When category ID is non-zero, use it for the GSM/UMTS message identifier. */
        EditText categoryIdField = (EditText) findViewById(R.id.category_id);
        categoryIdField.setText("0");

        /* Send an ETWS normal broadcast message to app. */
        Button etwsNormalTypeButton = (Button) findViewById(R.id.button_etws_normal_type);
        etwsNormalTypeButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (mDelayBeforeSending && v != null) {
                    Message msg = mDelayHandler.obtainMessage(0, this);
                    mDelayHandler.sendMessageDelayed(msg, DELAY_BEFORE_SENDING_MSEC);
                } else {
                    SendTestMessages.testSendEtwsMessageNormal(SendTestBroadcastActivity.this,
                            getMessageId());
                }
            }
        });

        /* Send an ETWS cancel broadcast message to app. */
        Button etwsCancelTypeButton = (Button) findViewById(R.id.button_etws_cancel_type);
        etwsCancelTypeButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (mDelayBeforeSending && v != null) {
                    Message msg = mDelayHandler.obtainMessage(0, this);
                    mDelayHandler.sendMessageDelayed(msg, DELAY_BEFORE_SENDING_MSEC);
                } else {
                    SendTestMessages.testSendEtwsMessageCancel(SendTestBroadcastActivity.this,
                            getMessageId());
                }
            }
        });

        /* Send an ETWS test broadcast message to app. */
        Button etwsTestTypeButton = (Button) findViewById(R.id.button_etws_test_type);
        etwsTestTypeButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (mDelayBeforeSending && v != null) {
                    Message msg = mDelayHandler.obtainMessage(0, this);
                    mDelayHandler.sendMessageDelayed(msg, DELAY_BEFORE_SENDING_MSEC);
                } else {
                    SendTestMessages.testSendEtwsMessageTest(SendTestBroadcastActivity.this,
                            getMessageId());
                }
            }
        });

        /* Send a CMAS presidential alert to app. */
        Button cmasPresAlertButton = (Button) findViewById(R.id.button_cmas_pres_alert);
        cmasPresAlertButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (mDelayBeforeSending && v != null) {
                    Message msg = mDelayHandler.obtainMessage(0, this);
                    mDelayHandler.sendMessageDelayed(msg, DELAY_BEFORE_SENDING_MSEC);
                } else {
                    SendCdmaCmasMessages.testSendCmasPresAlert(SendTestBroadcastActivity.this,
                            getMessageId());
                }
            }
        });

        /* Send a CMAS extreme alert to app. */
        Button cmasExtremeAlertButton = (Button) findViewById(R.id.button_cmas_extreme_alert);
        cmasExtremeAlertButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (mDelayBeforeSending && v != null) {
                    Message msg = mDelayHandler.obtainMessage(0, this);
                    mDelayHandler.sendMessageDelayed(msg, DELAY_BEFORE_SENDING_MSEC);
                } else {
                    SendCdmaCmasMessages.testSendCmasExtremeAlert(SendTestBroadcastActivity.this,
                            getMessageId());
                }
            }
        });

        /* Send a CMAS severe alert to app. */
        Button cmasSevereAlertButton = (Button) findViewById(R.id.button_cmas_severe_alert);
        cmasSevereAlertButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (mDelayBeforeSending && v != null) {
                    Message msg = mDelayHandler.obtainMessage(0, this);
                    mDelayHandler.sendMessageDelayed(msg, DELAY_BEFORE_SENDING_MSEC);
                } else {
                    SendCdmaCmasMessages.testSendCmasSevereAlert(SendTestBroadcastActivity.this,
                            getMessageId());
                }
            }
        });

        /* Send a CMAS AMBER alert to app. */
        Button cmasAmberAlertButton = (Button) findViewById(R.id.button_cmas_amber_alert);
        cmasAmberAlertButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (mDelayBeforeSending && v != null) {
                    Message msg = mDelayHandler.obtainMessage(0, this);
                    mDelayHandler.sendMessageDelayed(msg, DELAY_BEFORE_SENDING_MSEC);
                } else {
                    SendCdmaCmasMessages.testSendCmasAmberAlert(SendTestBroadcastActivity.this,
                            getMessageId());
                }
            }
        });

        /* Send a CMAS monthly test alert to app. */
        Button cmasMonthlyTestButton = (Button) findViewById(R.id.button_cmas_monthly_test);
        cmasMonthlyTestButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (mDelayBeforeSending && v != null) {
                    Message msg = mDelayHandler.obtainMessage(0, this);
                    mDelayHandler.sendMessageDelayed(msg, DELAY_BEFORE_SENDING_MSEC);
                } else {
                    SendCdmaCmasMessages.testSendCmasMonthlyTest(SendTestBroadcastActivity.this,
                            getMessageId());
                }
            }
        });

        /* Send a GSM 7-bit broadcast message to app. */
        Button gsm7bitTypeButton = (Button) findViewById(R.id.button_gsm_7bit_type);
        gsm7bitTypeButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (mDelayBeforeSending && v != null) {
                    Message msg = mDelayHandler.obtainMessage(0, this);
                    mDelayHandler.sendMessageDelayed(msg, DELAY_BEFORE_SENDING_MSEC);
                } else {
                    SendTestMessages.testSendMessage7bit(SendTestBroadcastActivity.this,
                            getMessageId(), getCategory());
                }
            }
        });

        /* Send a UMTS 7-bit broadcast message to app. */
        Button gsm7bitUmtsTypeButton = (Button) findViewById(R.id.button_gsm_7bit_umts_type);
        gsm7bitUmtsTypeButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (mDelayBeforeSending && v != null) {
                    Message msg = mDelayHandler.obtainMessage(0, this);
                    mDelayHandler.sendMessageDelayed(msg, DELAY_BEFORE_SENDING_MSEC);
                } else {
                    SendTestMessages.testSendMessage7bitUmts(SendTestBroadcastActivity.this,
                            getMessageId(), getCategory());
                }
            }
        });

        /* Send a GSM 7-bit no padding broadcast message to app. */
        Button gsm7bitNoPaddingButton = (Button) findViewById(R.id.button_gsm_7bit_nopadding_type);
        gsm7bitNoPaddingButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (mDelayBeforeSending && v != null) {
                    Message msg = mDelayHandler.obtainMessage(0, this);
                    mDelayHandler.sendMessageDelayed(msg, DELAY_BEFORE_SENDING_MSEC);
                } else {
                    SendTestMessages.testSendMessage7bitNoPadding(SendTestBroadcastActivity.this,
                            getMessageId(), getCategory());
                }
            }
        });

        /* Send a UMTS 7-bit no padding broadcast message to app. */
        Button gsm7bitNoPaddingUmtsTypeButton =
                (Button) findViewById(R.id.button_gsm_7bit_nopadding_umts_type);
        gsm7bitNoPaddingUmtsTypeButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (mDelayBeforeSending && v != null) {
                    Message msg = mDelayHandler.obtainMessage(0, this);
                    mDelayHandler.sendMessageDelayed(msg, DELAY_BEFORE_SENDING_MSEC);
                } else {
                    SendTestMessages.testSendMessage7bitNoPaddingUmts(
                            SendTestBroadcastActivity.this, getMessageId(), getCategory());
                }
            }
        });

        /* Send a UMTS 7-bit multi-page broadcast message to app. */
        Button gsm7bitMultipageButton =
                (Button) findViewById(R.id.button_gsm_7bit_multipage_type);
        gsm7bitMultipageButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (mDelayBeforeSending && v != null) {
                    Message msg = mDelayHandler.obtainMessage(0, this);
                    mDelayHandler.sendMessageDelayed(msg, DELAY_BEFORE_SENDING_MSEC);
                } else {
                    SendTestMessages.testSendMessage7bitMultipageGsm(SendTestBroadcastActivity.this,
                            getMessageId(), getCategory());
                }
            }
        });

        /* Send a UMTS 7-bit multi-page broadcast message to app. */
        Button gsm7bitMultipageUmtsButton =
                (Button) findViewById(R.id.button_gsm_7bit_multipage_umts_type);
        gsm7bitMultipageUmtsButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (mDelayBeforeSending && v != null) {
                    Message msg = mDelayHandler.obtainMessage(0, this);
                    mDelayHandler.sendMessageDelayed(msg, DELAY_BEFORE_SENDING_MSEC);
                } else {
                    SendTestMessages.testSendMessage7bitMultipageUmts(
                            SendTestBroadcastActivity.this, getMessageId(), getCategory());
                }
            }
        });

        /* Send a GSM 7-bit broadcast message with language to app. */
        Button gsm7bitWithLanguageButton =
                (Button) findViewById(R.id.button_gsm_7bit_with_language_type);
        gsm7bitWithLanguageButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (mDelayBeforeSending && v != null) {
                    Message msg = mDelayHandler.obtainMessage(0, this);
                    mDelayHandler.sendMessageDelayed(msg, DELAY_BEFORE_SENDING_MSEC);
                } else {
                    SendTestMessages.testSendMessage7bitWithLanguage(SendTestBroadcastActivity.this,
                            getMessageId(), getCategory());
                }
            }
        });

        /* Send a GSM 7-bit broadcast message with language to app. */
        Button gsm7bitWithLanguageInBodyButton =
                (Button) findViewById(R.id.button_gsm_7bit_with_language_body_gsm_type);
        gsm7bitWithLanguageInBodyButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (mDelayBeforeSending && v != null) {
                    Message msg = mDelayHandler.obtainMessage(0, this);
                    mDelayHandler.sendMessageDelayed(msg, DELAY_BEFORE_SENDING_MSEC);
                } else {
                    SendTestMessages.testSendMessage7bitWithLanguageInBody(
                            SendTestBroadcastActivity.this, getMessageId(), getCategory());
                }
            }
        });

        /* Send a UMTS 7-bit broadcast message with language to app. */
        Button gsm7bitWithLanguageUmtsButton =
                (Button) findViewById(R.id.button_gsm_7bit_with_language_body_umts_type);
        gsm7bitWithLanguageUmtsButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (mDelayBeforeSending && v != null) {
                    Message msg = mDelayHandler.obtainMessage(0, this);
                    mDelayHandler.sendMessageDelayed(msg, DELAY_BEFORE_SENDING_MSEC);
                } else {
                    SendTestMessages.testSendMessage7bitWithLanguageInBodyUmts(
                            SendTestBroadcastActivity.this, getMessageId(), getCategory());
                }
            }
        });

        /* Send a GSM UCS-2 broadcast message to app. */
        Button gsmUcs2TypeButton = (Button) findViewById(R.id.button_gsm_ucs2_type);
        gsmUcs2TypeButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (mDelayBeforeSending && v != null) {
                    Message msg = mDelayHandler.obtainMessage(0, this);
                    mDelayHandler.sendMessageDelayed(msg, DELAY_BEFORE_SENDING_MSEC);
                } else {
                    SendTestMessages.testSendMessageUcs2(SendTestBroadcastActivity.this,
                            getMessageId(), getCategory());
                }
            }
        });

        /* Send a UMTS UCS-2 broadcast message to app. */
        Button gsmUcs2UmtsTypeButton = (Button) findViewById(R.id.button_gsm_ucs2_umts_type);
        gsmUcs2UmtsTypeButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (mDelayBeforeSending && v != null) {
                    Message msg = mDelayHandler.obtainMessage(0, this);
                    mDelayHandler.sendMessageDelayed(msg, DELAY_BEFORE_SENDING_MSEC);
                } else {
                    SendTestMessages.testSendMessageUcs2Umts(SendTestBroadcastActivity.this,
                            getMessageId(), getCategory());
                }
            }
        });

        /* Send a UMTS UCS-2 multipage broadcast message to app. */
        Button gsmUcs2MultipageUmtsTypeButton =
                (Button) findViewById(R.id.button_gsm_ucs2_multipage_umts_type);
        gsmUcs2MultipageUmtsTypeButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (mDelayBeforeSending && v != null) {
                    Message msg = mDelayHandler.obtainMessage(0, this);
                    mDelayHandler.sendMessageDelayed(msg, DELAY_BEFORE_SENDING_MSEC);
                } else {
                    SendTestMessages.testSendMessageUcs2MultipageUmts(
                            SendTestBroadcastActivity.this, getMessageId(), getCategory());
                }
            }
        });

        /* Send a GSM UCS-2 broadcast message with language to app. */
        Button gsmUcs2WithLanguageTypeButton =
                (Button) findViewById(R.id.button_gsm_ucs2_with_language_type);
        gsmUcs2WithLanguageTypeButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (mDelayBeforeSending && v != null) {
                    Message msg = mDelayHandler.obtainMessage(0, this);
                    mDelayHandler.sendMessageDelayed(msg, DELAY_BEFORE_SENDING_MSEC);
                } else {
                    SendTestMessages.testSendMessageUcs2WithLanguageInBody(
                            SendTestBroadcastActivity.this, getMessageId(), getCategory());
                }
            }
        });

        /* Send a UMTS UCS-2 broadcast message with language to app. */
        Button gsmUcs2WithLanguageUmtsTypeButton =
                (Button) findViewById(R.id.button_gsm_ucs2_with_language_umts_type);
        gsmUcs2WithLanguageUmtsTypeButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (mDelayBeforeSending && v != null) {
                    Message msg = mDelayHandler.obtainMessage(0, this);
                    mDelayHandler.sendMessageDelayed(msg, DELAY_BEFORE_SENDING_MSEC);
                } else {
                    SendTestMessages.testSendMessageUcs2WithLanguageUmts(
                            SendTestBroadcastActivity.this, getMessageId(), getCategory());
                }
            }
        });

        /* Update boolean to delay before sending when box is checked. */
        final CheckBox delayCheckbox = (CheckBox) findViewById(R.id.button_delay_broadcast);
        delayCheckbox.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mDelayBeforeSending = delayCheckbox.isChecked();
            }
        });
    }
}
