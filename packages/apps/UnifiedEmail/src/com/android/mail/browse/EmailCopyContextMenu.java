/*
 * Copyright (C) 2013 Google Inc.
 * Licensed to The Android Open Source Project.
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

package com.android.mail.browse;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.webkit.WebView;

import com.android.mail.R;

/**
 * <p>
 * Handles display and behavior for long clicking on expanded messages' headers.
 * Requires a context to use for inflation and clipboard copying.
 * </p>
 * <br>
 * Dependencies:
 * <ul>
 * <li>res/menu/email_copy_context_menu.xml</li>
 * </ul>
 */
public class EmailCopyContextMenu implements OnCreateContextMenuListener{

    // IDs for displaying in the menu
    private static final int SEND_EMAIL_ITEM = R.id.mail_context_menu_id;
    private static final int COPY_CONTACT_ITEM = R.id.copy_mail_context_menu_id;

    // Reference to context for layout inflation & copying the email
    private final Context mContext;
    private CharSequence mAddress;

    public EmailCopyContextMenu(Context context) {
        mContext = context;
    }

    /**
     * Copy class for handling click event on the "Copy" button and storing
     * copied text.
     */
    private class Copy implements MenuItem.OnMenuItemClickListener {
        private final CharSequence mText;

        public Copy(CharSequence text) {
            mText = text;
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            copy(mText);
            //Handled & consumed event
            return true;
        }
    }

    /**
     * Copy the input text sequence to the system clipboard.
     * @param text CharSequence to be copied.
     */
    private void copy(CharSequence text) {
        ClipboardManager clipboard =
                (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(null, text));
    }

    public void setAddress(CharSequence address) {
        this.mAddress = address;
    }

    /**
     * Creates context menu via MenuInflater and populates with items defined
     * in res/menu/email_copy_context_menu.xml
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo info) {
        if(!TextUtils.isEmpty(mAddress)) {
            MenuInflater inflater = new MenuInflater(mContext);
            inflater.inflate(getMenuResourceId(), menu);

            // Create menu and bind listener/intent
            menu.setHeaderTitle(mAddress);
            menu.findItem(SEND_EMAIL_ITEM).setIntent(new Intent(Intent.ACTION_VIEW,
                    Uri.parse(WebView.SCHEME_MAILTO + mAddress)));
            menu.findItem(COPY_CONTACT_ITEM).setOnMenuItemClickListener(
                    new Copy(mAddress));
        }
    }

    // Location of Context Menu layout
    private static int getMenuResourceId() {
        return R.menu.email_copy_context_menu;
    }
}
