/*******************************************************************************
 *      Copyright (C) 2012 Google Inc.
 *      Licensed to The Android Open Source Project.
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 *******************************************************************************/

package com.android.mail.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.view.DragEvent;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityManager;

import com.android.mail.compose.ComposeActivity;
import com.android.mail.providers.Folder;
import com.android.mail.utils.StorageLowState;
import com.android.mail.utils.Utils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * This is the root activity container that holds the left navigation fragment
 * (usually a list of folders), and the main content fragment (either a
 * conversation list or a conversation view).
 */
public class MailActivity extends AbstractMailActivity implements ControllableActivity {
    /**
     * The activity controller to which we delegate most Activity lifecycle events.
     */
    private ActivityController mController;

    private ViewMode mViewMode;

    private ToastBarOperation mPendingToastOp;
    private boolean mAccessibilityEnabled;
    private AccessibilityManager mAccessibilityManager;

    protected ConversationListHelper mConversationListHelper;

    /**
     * The account name currently in use. Used to construct the NFC mailto: message. This needs
     * to be static since the {@link ComposeActivity} needs to statically change the account name
     * and have the NFC message changed accordingly.
     */
    protected static String sAccountName = null;

    /**
     * Create an NFC message (in the NDEF: Nfc Data Exchange Format) to instruct the recepient to
     * send an email to the current account.
     */
    private static class NdefMessageMaker implements NfcAdapter.CreateNdefMessageCallback {
        @Override
        public NdefMessage createNdefMessage(NfcEvent event) {
            if (sAccountName == null) {
                return null;
            }
            return getMailtoNdef(sAccountName);
        }

        /**
         * Returns an NDEF message with a single mailto URI record
         * for the given email address.
         */
        private static NdefMessage getMailtoNdef(String account) {
            byte[] accountBytes;
            try {
                accountBytes = URLEncoder.encode(account, "UTF-8").getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                accountBytes = account.getBytes();
            }
            byte prefix = 0x06; // mailto:
            byte[] recordBytes = new byte[accountBytes.length + 1];
            recordBytes[0] = prefix;
            System.arraycopy(accountBytes, 0, recordBytes, 1, accountBytes.length);
            NdefRecord mailto = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_URI,
                    new byte[0], recordBytes);
            return new NdefMessage(new NdefRecord[] { mailto });
        }
    }

    private final NdefMessageMaker mNdefHandler = new NdefMessageMaker();

    public MailActivity() {
        super();
        mConversationListHelper = new ConversationListHelper();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        mController.onTouchEvent(ev);
        return super.dispatchTouchEvent(ev);
    }

    /**
     * Default implementation returns a null view mode.
     */
    @Override
    public ViewMode getViewMode() {
        return mViewMode;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mController.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        if (!mController.onBackPressed()) {
            super.onBackPressed();
        }
    }

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        mViewMode = new ViewMode();
        final boolean tabletUi = Utils.useTabletUI(this.getResources());
        mController = ControllerFactory.forActivity(this, mViewMode, tabletUi);
        mController.onCreate(savedState);

        mAccessibilityManager =
                (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        mAccessibilityEnabled = mAccessibilityManager.isEnabled();
        final NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter != null) {
            nfcAdapter.setNdefPushMessageCallback(mNdefHandler, this);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        mController.onPostCreate(savedInstanceState);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mController.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        mController.onRestart();
    }

    /**
     * Constructs and sets the default NFC message. This message instructs the receiver to send
     * email to the account provided as the argument. This message is to be shared with
     * "zero-clicks" using NFC. The message will be available as long as the current activity is in
     * the foreground.
     *
     * @param account The email address to send mail to.
     */
    public static void setNfcMessage(String account) {
        sAccountName = account;
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mController.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public Dialog onCreateDialog(int id, Bundle bundle) {
        final Dialog dialog = mController.onCreateDialog(id, bundle);
        return dialog == null ? super.onCreateDialog(id, bundle) : dialog;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return mController.onCreateOptionsMenu(menu) || super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return mController.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return mController.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    @Override
    public void onPause() {
        super.onPause();
        mController.onPause();
    }

    @Override
    public void onPrepareDialog(int id, Dialog dialog, Bundle bundle) {
        super.onPrepareDialog(id, dialog, bundle);
        mController.onPrepareDialog(id, dialog, bundle);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        mController.onPrepareOptionsMenu(menu);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onResume() {
        super.onResume();
        mController.onResume();
        final boolean enabled = mAccessibilityManager.isEnabled();
        if (enabled != mAccessibilityEnabled) {
            onAccessibilityStateChanged(enabled);
        }
        // App has resumed, re-check the top-level storage situation.
        StorageLowState.checkStorageLowMode(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mController.onSaveInstanceState(outState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mController.onStart();
    }

    @Override
    public boolean onSearchRequested() {
        mController.startSearch();
        return true;
    }

    @Override
    public void onStop() {
        super.onStop();
        mController.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mController.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        mController.onWindowFocusChanged(hasFocus);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(super.toString());
        sb.append("{ViewMode=");
        sb.append(mViewMode);
        sb.append(" controller=");
        sb.append(mController);
        sb.append("}");
        return sb.toString();
    }

    @Override
    public ConversationListCallbacks getListHandler() {
        return mController;
    }

    @Override
    public FolderChangeListener getFolderChangeListener() {
        return mController;
    }

    @Override
    public FolderSelector getFolderSelector() {
        return mController;
    }

    @Override
    public FolderController getFolderController() {
        return mController;
    }

    @Override
    public ConversationSelectionSet getSelectedSet() {
        return mController.getSelectedSet();
    }

    @Override
    public boolean supportsDrag(DragEvent event, Folder folder) {
        return mController.supportsDrag(event, folder);
    }

    @Override
    public void handleDrop(DragEvent event, Folder folder) {
        mController.handleDrop(event, folder);
    }

    @Override
    public void onUndoAvailable(ToastBarOperation undoOp) {
        mController.onUndoAvailable(undoOp);
    }

    @Override
    public Folder getHierarchyFolder() {
        return mController.getHierarchyFolder();
    }

    @Override
    public ConversationUpdater getConversationUpdater() {
        return mController;
    }

    @Override
    public ErrorListener getErrorListener() {
        return mController;
    }

    @Override
    public void setPendingToastOperation(ToastBarOperation op) {
        mPendingToastOp = op;
    }

    @Override
    public ToastBarOperation getPendingToastOperation() {
        return mPendingToastOp;
    }

    /**
     * Sole purpose of this method is to stop clicks where we don't want them in
     * the action bar. This can be hooked as a listener to view with
     * android:onClick.
     *
     * @param v
     */
    public void doNothingClickHandler(View v) {
        // Do nothing.
    }

    @Override
    public void onAnimationEnd(AnimatedAdapter animatedAdapter) {
        mController.onAnimationEnd(animatedAdapter);
    }

    @Override
    public AccountController getAccountController() {
        return mController;
    }

    @Override
    public RecentFolderController getRecentFolderController() {
        return mController;
    }

    @Override
    public UpOrBackController getUpOrBackController() {
        return mController;
    }

    @Override
    public void onFooterViewErrorActionClick(Folder folder, int errorStatus) {
        mController.onFooterViewErrorActionClick(folder, errorStatus);
    }

    @Override
    public void onFooterViewLoadMoreClick(Folder folder) {
        mController.onFooterViewLoadMoreClick(folder);
    }

    @Override
    public void startDragMode() {
        mController.startDragMode();
    }

    @Override
    public void stopDragMode() {
        mController.stopDragMode();
    }

    @Override
    public boolean isAccessibilityEnabled() {
        return mAccessibilityEnabled;
    }

    public void onAccessibilityStateChanged(boolean enabled) {
        mAccessibilityEnabled = enabled;
        mController.onAccessibilityStateChanged();
    }

    @Override
    public final ConversationListHelper getConversationListHelper() {
        return mConversationListHelper;
    }

    @Override
    public FragmentLauncher getFragmentLauncher() {
        return mController;
    }
}
