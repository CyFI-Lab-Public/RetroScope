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

package com.android.mail.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.view.DragEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;

import com.android.mail.R;
import com.android.mail.providers.Account;
import com.android.mail.providers.Folder;
import com.android.mail.providers.FolderWatcher;
import com.android.mail.utils.LogTag;
import com.android.mail.utils.LogUtils;
import com.android.mail.utils.Observable;
import com.android.mail.utils.Utils;
import com.android.mail.utils.VeiledAddressMatcher;
import com.android.mail.widget.WidgetProvider;

import java.util.ArrayList;

/**
 * This activity displays the list of available folders for the current account.
 */
public class FolderSelectionActivity extends Activity implements OnClickListener,
        DialogInterface.OnClickListener, FolderChangeListener, ControllableActivity,
        FolderSelector {
    public static final String EXTRA_ACCOUNT_SHORTCUT = "account-shortcut";

    private static final String LOG_TAG = LogTag.getLogTag();

    private static final int CONFIGURE = 0;

    private static final int VIEW = 1;

    private Account mAccount;
    private Folder mSelectedFolder;
    private boolean mConfigureShortcut;
    protected boolean mConfigureWidget;
    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private int mMode = -1;
    /** Empty placeholder for communicating to the consumer of the drawer observer. */
    private final DataSetObservable mDrawerObservers = new Observable("Drawer");

    private final AccountController mAccountController = new AccountController() {
        @Override
        public void registerAccountObserver(DataSetObserver observer) {
            // Do nothing
        }

        @Override
        public void unregisterAccountObserver(DataSetObserver observer) {
            // Do nothing
        }

        @Override
        public Account getAccount() {
            return mAccount;
        }

        @Override
        public void registerAllAccountObserver(DataSetObserver observer) {
            // Do nothing
        }

        @Override
        public void unregisterAllAccountObserver(DataSetObserver observer) {
            // Do nothing
        }

        @Override
        public Account[] getAllAccounts() {
            return new Account[]{mAccount};
        }

        @Override
        public VeiledAddressMatcher getVeiledAddressMatcher() {
            return null;
        }

        @Override
        public void changeAccount(Account account) {
            // Never gets called, so do nothing here.
            LogUtils.wtf(LOG_TAG,
                    "FolderSelectionActivity.changeAccount() called when NOT expected.");
        }

        @Override
        public void switchToDefaultInboxOrChangeAccount(Account account) {
            // Never gets called, so do nothing here.
            LogUtils.wtf(LOG_TAG,"FolderSelectionActivity.switchToDefaultInboxOrChangeAccount() " +
                    "called when NOT expected.");
        }

        @Override
        public void registerDrawerClosedObserver(final DataSetObserver observer) {
            mDrawerObservers.registerObserver(observer);
        }

        @Override
        public void unregisterDrawerClosedObserver(final DataSetObserver observer) {
            mDrawerObservers.unregisterObserver(observer);
        }

        /**
         * Since there is no drawer to wait for, notifyChanged to the observers.
         */
        @Override
        public void closeDrawer(final boolean hasNewFolderOrAccount,
                Account account, Folder folder) {
            mDrawerObservers.notifyChanged();
        }

        @Override
        public void setFolderWatcher(FolderWatcher watcher) {
            // Unsupported.
        }

        @Override
        public boolean isDrawerPullEnabled() {
            // Unsupported
            return false;
        }

        @Override
        public int getFolderListViewChoiceMode() {
            return ListView.CHOICE_MODE_NONE;
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.folders_activity);

        final Intent intent = getIntent();
        final String action = intent.getAction();
        mConfigureShortcut = Intent.ACTION_CREATE_SHORTCUT.equals(action);
        mConfigureWidget = AppWidgetManager.ACTION_APPWIDGET_CONFIGURE.equals(action);
        if (!mConfigureShortcut && !mConfigureWidget) {
            LogUtils.wtf(LOG_TAG, "unexpected intent: %s", intent);
        }
        if (mConfigureShortcut || mConfigureWidget) {
            ActionBar actionBar = getActionBar();
            if (actionBar != null) {
                actionBar.setIcon(R.mipmap.ic_launcher_shortcut_folder);
            }
            mMode = CONFIGURE;
        } else {
            mMode = VIEW;
        }

        if (mConfigureWidget) {
            mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
            if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
                LogUtils.wtf(LOG_TAG, "invalid widgetId");
            }
        }

        mAccount = intent.getParcelableExtra(EXTRA_ACCOUNT_SHORTCUT);
        final Button firstButton = (Button) findViewById(R.id.first_button);
        firstButton.setVisibility(View.VISIBLE);
        // TODO(mindyp) disable the manage folders buttons until we have a manage folders screen.
        if (mMode == VIEW) {
            firstButton.setEnabled(false);
        }
        firstButton.setOnClickListener(this);
        createFolderListFragment(FolderListFragment.ofTopLevelTree(mAccount.folderListUri,
                getExcludedFolderTypes()));
    }

    /**
     * Create a Fragment showing this folder and its children.
     */
    private void createFolderListFragment(Fragment fragment) {
        final FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.content_pane, fragment);
        fragmentTransaction.commitAllowingStateLoss();
    }

    /**
     * Gets an {@link ArrayList} of canonical names of any folders to exclude from displaying.
     * By default, this list is empty.
     *
     * @return An {@link ArrayList} of folder canonical names
     */
    protected ArrayList<Integer> getExcludedFolderTypes() {
        return new ArrayList<Integer>();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // TODO: (mindyp) Make sure we're operating on the same account as
        // before. If the user switched accounts, switch back.
    }

    @Override
    public void onClick(View v) {
        final int id = v.getId();
        if (id == R.id.first_button) {
            if (mMode == CONFIGURE) {
                doCancel();
            } else {
                // TODO (mindyp): open manage folders screen.
            }
        }
    }

    private void doCancel() {
        setResult(RESULT_CANCELED);
        finish();
    }

    /**
     * Create a widget for the specified account and folder
     */
    protected void createWidget(int id, Account account, Folder selectedFolder) {
        WidgetProvider.updateWidget(this, id, account, selectedFolder.type,
                selectedFolder.folderUri.fullUri, selectedFolder.conversationListUri,
                selectedFolder.name);
        final Intent result = new Intent();
        result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
        setResult(RESULT_OK, result);
        finish();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            // The only dialog that is
            createWidget(mAppWidgetId, mAccount, mSelectedFolder);
        } else {
            doCancel();
        }
    }

    @Override
    public void onFolderChanged(Folder folder, final boolean force) {
        if (!folder.equals(mSelectedFolder)) {
            mSelectedFolder = folder;
            Intent resultIntent = new Intent();

            if (mConfigureShortcut) {
                /*
                 * Create the shortcut Intent based on it with the additional
                 * information that we have in this activity: name of the
                 * account, calculate the human readable name of the folder and
                 * use it as the shortcut name, etc...
                 */
                final Intent clickIntent = Utils.createViewFolderIntent(this,
                        mSelectedFolder.folderUri.fullUri, mAccount);
                resultIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, clickIntent);
                resultIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                        Intent.ShortcutIconResource.fromContext(this,
                                R.mipmap.ic_launcher_shortcut_folder));
                /**
                 * Note: Email1 created shortcuts using R.mipmap#ic_launcher_email
                 * so don't delete that resource until we have an upgrade/migration solution
                 */

                final CharSequence humanFolderName = mSelectedFolder.name;

                resultIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, humanFolderName);

                // Now ask the user what name they want for this shortcut. Pass
                // the
                // shortcut intent that we just created, the user can modify the
                // folder in
                // ShortcutNameActivity.
                final Intent shortcutNameIntent = new Intent(this, ShortcutNameActivity.class);
                shortcutNameIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY
                        | Intent.FLAG_ACTIVITY_FORWARD_RESULT);
                shortcutNameIntent.putExtra(ShortcutNameActivity.EXTRA_FOLDER_CLICK_INTENT,
                        resultIntent);
                shortcutNameIntent.putExtra(ShortcutNameActivity.EXTRA_SHORTCUT_NAME,
                        humanFolderName);

                startActivity(shortcutNameIntent);
                finish();
            } else if (mConfigureWidget) {
                createWidget(mAppWidgetId, mAccount, mSelectedFolder);
            }
        }
    }

    @Override
    public String getHelpContext() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Context getActivityContext() {
        return this;
    }

    @Override
    public ViewMode getViewMode() {
        return null;
    }

    @Override
    public ConversationListCallbacks getListHandler() {
        return null;
    }

    @Override
    public FolderChangeListener getFolderChangeListener() {
        return this;
    }

    @Override
    public ConversationSelectionSet getSelectedSet() {
        return null;
    }

    private Folder mNavigatedFolder;
    @Override
    public void onFolderSelected(Folder folder) {
        if (folder.hasChildren && !folder.equals(mNavigatedFolder)) {
            mNavigatedFolder = folder;
            // Replace this fragment with a new FolderListFragment
            // showing this folder's children if we are not already looking
            // at the child view for this folder.
            createFolderListFragment(FolderListFragment.ofTree(folder));
            return;
        }
        onFolderChanged(folder, false /* force */);
    }

    @Override
    public FolderSelector getFolderSelector() {
        return this;
    }

    @Override
    public boolean supportsDrag(DragEvent event, Folder folder) {
        return false;
    }

    @Override
    public void handleDrop(DragEvent event, Folder folder) {
        // Do nothing.
    }

    @Override
    public void onUndoAvailable(ToastBarOperation undoOp) {
        // Do nothing.
    }

    @Override
    public Folder getHierarchyFolder() {
        return null;
    }

    @Override
    public ConversationUpdater getConversationUpdater() {
        return null;
    }

    @Override
    public ErrorListener getErrorListener() {
        return null;
    }

    @Override
    public void setPendingToastOperation(ToastBarOperation op) {
        // Do nothing.
    }

    @Override
    public ToastBarOperation getPendingToastOperation() {
        return null;
    }

    @Override
    public FolderController getFolderController() {
        return null;
    }

    @Override
    public void onAnimationEnd(AnimatedAdapter animatedAdapter) {
    }

    @Override
    public AccountController getAccountController() {
        return mAccountController;
    }

    @Override
    public void onFooterViewErrorActionClick(Folder folder, int errorStatus) {
        // Unsupported
    }

    @Override
    public void onFooterViewLoadMoreClick(Folder folder) {
        // Unsupported
    }

    @Override
    public void startDragMode() {
        // Unsupported
    }

    @Override
    public void stopDragMode() {
        // Unsupported
    }

    @Override
    public RecentFolderController getRecentFolderController() {
        // Unsupported
        return null;
    }

    @Override
    public UpOrBackController getUpOrBackController() {
        // Unsupported
        return null;
    }

    @Override
    public boolean isAccessibilityEnabled() {
        // Unsupported
        return true;
    }

    @Override
    public ConversationListHelper getConversationListHelper() {
        // Unsupported
        return null;
    }

    @Override
    public FragmentLauncher getFragmentLauncher() {
        // Unsupported
        return null;
    }
}
