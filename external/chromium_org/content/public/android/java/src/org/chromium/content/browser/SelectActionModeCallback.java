// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.content.browser;

import android.app.Activity;
import android.app.SearchManager;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.Browser;
import android.text.TextUtils;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

import org.chromium.content.R;

/**
 * An ActionMode.Callback for in-page selection. This class handles both the editable and
 * non-editable cases.
 */
public class SelectActionModeCallback implements ActionMode.Callback {
    /**
     * An interface to retrieve information about the current selection, and also to perform
     * actions based on the selection or when the action bar is dismissed.
     */
    public interface ActionHandler {
        /**
         * Perform a select all action.
         * @return true iff the action was successful.
         */
        boolean selectAll();

        /**
         * Perform a copy (to clipboard) action.
         * @return true iff the action was successful.
         */
        boolean copy();

        /**
         * Perform a cut (to clipboard) action.
         * @return true iff the action was successful.
         */
        boolean cut();

        /**
         * Perform a paste action.
         * @return true iff the action was successful.
         */
        boolean paste();

        /**
         * @return true iff the current selection is editable (e.g. text within an input field).
         */
        boolean isSelectionEditable();

        /**
         * @return the currently selected text String.
         */
        String getSelectedText();

        /**
         * Called when the onDestroyActionMode of the SelectActionmodeCallback is called.
         */
        void onDestroyActionMode();
    }

    private Context mContext;
    private ActionHandler mActionHandler;
    private final boolean mIncognito;
    private boolean mEditable;

    protected SelectActionModeCallback(
            Context context, ActionHandler actionHandler, boolean incognito) {
        mContext = context;
        mActionHandler = actionHandler;
        mIncognito = incognito;
    }

    protected Context getContext() {
        return mContext;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.setTitle(null);
        mode.setSubtitle(null);
        mEditable = mActionHandler.isSelectionEditable();
        createActionMenu(mode, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        boolean isEditableNow = mActionHandler.isSelectionEditable();
        if (mEditable != isEditableNow) {
            mEditable = isEditableNow;
            menu.clear();
            createActionMenu(mode, menu);
            return true;
        }
        return false;
    }

    private void createActionMenu(ActionMode mode, Menu menu) {
        mode.getMenuInflater().inflate(R.menu.select_action_menu, menu);
        if (!mEditable || !canPaste()) {
            menu.removeItem(R.id.select_action_menu_paste);
        }

        if (!mEditable) {
            menu.removeItem(R.id.select_action_menu_cut);
        }

        if (mEditable || !isShareHandlerAvailable()) {
            menu.removeItem(R.id.select_action_menu_share);
        }

        if (mEditable || mIncognito || !isWebSearchAvailable()) {
            menu.removeItem(R.id.select_action_menu_web_search);
        }
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        String selection = mActionHandler.getSelectedText();
        int id = item.getItemId();

        if (id == R.id.select_action_menu_select_all) {
            mActionHandler.selectAll();
        } else if (id == R.id.select_action_menu_cut) {
            mActionHandler.cut();
        } else if (id == R.id.select_action_menu_copy) {
            mActionHandler.copy();
            mode.finish();
        } else if (id == R.id.select_action_menu_paste) {
            mActionHandler.paste();
        } else if (id == R.id.select_action_menu_share) {
            if (!TextUtils.isEmpty(selection)) {
                Intent send = new Intent(Intent.ACTION_SEND);
                send.setType("text/plain");
                send.putExtra(Intent.EXTRA_TEXT, selection);
                try {
                    Intent i = Intent.createChooser(send, getContext().getString(
                            R.string.actionbar_share));
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    getContext().startActivity(i);
                } catch (android.content.ActivityNotFoundException ex) {
                    // If no app handles it, do nothing.
                }
            }
            mode.finish();
        } else if (id == R.id.select_action_menu_web_search) {
            if (!TextUtils.isEmpty(selection)) {
                Intent i = new Intent(Intent.ACTION_WEB_SEARCH);
                i.putExtra(SearchManager.EXTRA_NEW_SEARCH, true);
                i.putExtra(SearchManager.QUERY, selection);
                i.putExtra(Browser.EXTRA_APPLICATION_ID, getContext().getPackageName());
                if (!(getContext() instanceof Activity)) {
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }
                try {
                    getContext().startActivity(i);
                } catch (android.content.ActivityNotFoundException ex) {
                    // If no app handles it, do nothing.
                }
            }
            mode.finish();
        } else {
            return false;
        }
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        mActionHandler.onDestroyActionMode();
    }

    private boolean canPaste() {
        ClipboardManager clipMgr = (ClipboardManager)
                getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        return clipMgr.hasPrimaryClip();
    }

    private boolean isShareHandlerAvailable() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        return getContext().getPackageManager()
                .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).size() > 0;
    }

    private boolean isWebSearchAvailable() {
        Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
        intent.putExtra(SearchManager.EXTRA_NEW_SEARCH, true);
        return getContext().getPackageManager()
                .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).size() > 0;
    }
}
