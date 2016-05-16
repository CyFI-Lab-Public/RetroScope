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

package com.android.cts.verifier.usb;

import com.android.cts.verifier.PassFailButtons;
import com.android.cts.verifier.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Test for USB accessories. The test activity interacts with a cts-usb-accessory program that
 * acts as an accessory by exchanging a series of messages.
 */
public class UsbAccessoryTestActivity extends PassFailButtons.Activity {

    private static final String TAG = UsbAccessoryTestActivity.class.getSimpleName();

    private static final int FILE_DESCRIPTOR_PROBLEM_DIALOG_ID = 1;

    private static final String ACTION_USB_PERMISSION =
            "com.android.cts.verifier.usb.USB_PERMISSION";

    private ArrayAdapter<String> mReceivedMessagesAdapter;
    private ArrayAdapter<String> mSentMessagesAdapter;
    private MessageHandler mHandler;

    private UsbManager mUsbManager;
    private PendingIntent mPermissionIntent;
    private boolean mPermissionRequestPending;
    private UsbReceiver mUsbReceiver;

    private ParcelFileDescriptor mFileDescriptor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.usb_main);
        setInfoResources(R.string.usb_accessory_test, R.string.usb_accessory_test_info, -1);
        setPassFailButtonClickListeners();

        // Don't allow a test pass until the accessory and the Android device exchange messages...
        getPassButton().setEnabled(false);

        if (!hasUsbAccessorySupport()) {
            showNoUsbAccessoryDialog();
        }

        mReceivedMessagesAdapter = new ArrayAdapter<String>(this, R.layout.usb_message_row);
        mSentMessagesAdapter = new ArrayAdapter<String>(this, R.layout.usb_message_row);
        mHandler = new MessageHandler();

        mUsbManager = (UsbManager) getSystemService(USB_SERVICE);
        mPermissionIntent = PendingIntent.getBroadcast(this, 0,
                new Intent(ACTION_USB_PERMISSION), 0);

        mUsbReceiver = new UsbReceiver();
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
        registerReceiver(mUsbReceiver, filter);

        setupListViews();
    }

    private boolean hasUsbAccessorySupport() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_USB_ACCESSORY);
    }

    private void showNoUsbAccessoryDialog() {
        new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle(R.string.usb_not_available_title)
            .setMessage(R.string.usb_not_available_message)
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            })
            .show();
    }

    private void setupListViews() {
        ListView sentMessages = (ListView) findViewById(R.id.usb_sent_messages);
        ListView receivedMessages = (ListView) findViewById(R.id.usb_received_messages);

        View emptySentView = findViewById(R.id.usb_empty_sent_messages);
        View emptyReceivedView = findViewById(R.id.usb_empty_received_messages);
        sentMessages.setEmptyView(emptySentView);
        receivedMessages.setEmptyView(emptyReceivedView);

        receivedMessages.setAdapter(mReceivedMessagesAdapter);
        sentMessages.setAdapter(mSentMessagesAdapter);
    }

    class UsbReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_USB_PERMISSION.equals(intent.getAction())
                    || UsbManager.ACTION_USB_ACCESSORY_ATTACHED.equals(intent.getAction())) {
                UsbAccessory accessory = intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    openAccessory(accessory);
                } else {
                    Log.i(TAG, "Permission denied...");
                }
                mPermissionRequestPending = false;
            }
        }
    }

    private void openAccessory(UsbAccessory accessory) {
        mFileDescriptor = mUsbManager.openAccessory(accessory);
        if (mFileDescriptor != null) {
            FileDescriptor fileDescriptor = mFileDescriptor.getFileDescriptor();
            FileInputStream inputStream = new FileInputStream(fileDescriptor);
            FileOutputStream outputStream = new FileOutputStream(fileDescriptor);
            new MessageThread(inputStream, outputStream, mHandler).start();
        } else {
            showDialog(FILE_DESCRIPTOR_PROBLEM_DIALOG_ID);
        }
    }

    static class MessageThread extends Thread {

        private final InputStream mInputStream;

        private final OutputStream mOutputStream;

        private final MessageHandler mHandler;

        private int mNextMessageNumber = 0;

        MessageThread(InputStream inputStream, OutputStream outputStream, MessageHandler handler) {
            this.mInputStream = inputStream;
            this.mOutputStream = outputStream;
            this.mHandler = handler;
        }

        @Override
        public void run() {
            mHandler.sendEmptyMessage(MessageHandler.MESSAGE_THREAD_STARTING);

            try {
                // Wait a bit or else the messages can appear to quick and be confusing...
                Thread.sleep(2000);
                sendMessage();

                // Wait for response and send message acks...
                int numRead = 0;
                byte[] buffer = new byte[16384];
                while (numRead >= 0) {
                    numRead = mInputStream.read(buffer);
                    if (numRead > 0) {
                        handleReceivedMessage(buffer, numRead);
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Exception while reading from input stream", e);
                mHandler.sendEmptyMessage(MessageHandler.MESSAGE_THREAD_EXCEPTION);
            } catch (InterruptedException e) {
                Log.e(TAG, "Exception while reading from input stream", e);
                mHandler.sendEmptyMessage(MessageHandler.MESSAGE_THREAD_EXCEPTION);
            }
            mHandler.sendEmptyMessage(MessageHandler.MESSAGE_THREAD_ENDING);
        }

        private void handleReceivedMessage(byte[] buffer, int numRead) throws IOException {
            // TODO: Check the contents of the message?
            String text = new String(buffer, 0, numRead).trim();
            mHandler.sendReceivedMessage(text);

            // Send back a response..
            if (mNextMessageNumber <= 10) {
                sendMessage();
            } else {
                mHandler.sendEmptyMessage(MessageHandler.TEST_PASSED);
            }
        }

        private void sendMessage() throws IOException {
            String text = "Message from Android device #" + mNextMessageNumber++;
            mOutputStream.write(text.getBytes());
            mHandler.sendSentMessage(text);
        }
    }

    class MessageHandler extends Handler {

        static final int RECEIVED_MESSAGE = 1;

        static final int SENT_MESSAGE = 2;

        static final int MESSAGE_THREAD_STARTING = 3;

        static final int MESSAGE_THREAD_EXCEPTION = 4;

        static final int MESSAGE_THREAD_ENDING = 5;

        static final int TEST_PASSED = 6;

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case RECEIVED_MESSAGE:
                    mReceivedMessagesAdapter.add((String) msg.obj);
                    break;

                case SENT_MESSAGE:
                    mSentMessagesAdapter.add((String) msg.obj);
                    break;

                case MESSAGE_THREAD_STARTING:
                    showToast(R.string.usb_message_thread_started);
                    break;

                case MESSAGE_THREAD_EXCEPTION:
                    showToast(R.string.usb_message_thread_exception);
                    break;

                case MESSAGE_THREAD_ENDING:
                    showToast(R.string.usb_message_thread_ended);
                    break;

                case TEST_PASSED:
                    showToast(R.string.usb_test_passed);
                    getPassButton().setEnabled(true);
                    break;

                default:
                    throw new IllegalArgumentException("Bad message type: " + msg.what);
            }
        }

        private void showToast(int messageId) {
            Toast.makeText(UsbAccessoryTestActivity.this, messageId, Toast.LENGTH_SHORT).show();
        }

        void sendReceivedMessage(String text) {
            Message message = Message.obtain(this, RECEIVED_MESSAGE);
            message.obj = text;
            sendMessage(message);
        }

        void sendSentMessage(String text) {
            Message message = Message.obtain(this, SENT_MESSAGE);
            message.obj = text;
            sendMessage(message);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        UsbAccessory[] accessories = mUsbManager.getAccessoryList();
        UsbAccessory accessory = accessories != null && accessories.length > 0
                ? accessories[0]
                : null;
        if (accessory != null) {
            if (mUsbManager.hasPermission(accessory)) {
                openAccessory(accessory);
            } else {
                if (!mPermissionRequestPending) {
                    mUsbManager.requestPermission(accessory, mPermissionIntent);
                    mPermissionRequestPending = true;
                }
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mFileDescriptor != null) {
            try {
                mFileDescriptor.close();
            } catch (IOException e) {
                Log.e(TAG, "Exception while closing file descriptor", e);
            } finally {
                mFileDescriptor = null;
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setContentView(R.layout.usb_main);
        setupListViews();
    }

    @Override
    public Dialog onCreateDialog(int id, Bundle args) {
        switch (id) {
            case FILE_DESCRIPTOR_PROBLEM_DIALOG_ID:
                return new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(R.string.usb_accessory_test)
                    .setMessage(R.string.usb_file_descriptor_error)
                    .create();

            default:
                return super.onCreateDialog(id, args);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mUsbReceiver);
    }
}
