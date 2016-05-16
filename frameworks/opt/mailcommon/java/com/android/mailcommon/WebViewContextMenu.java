/*
 * Copyright (C) 2011 Google Inc.
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

package com.android.mailcommon;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.provider.ContactsContract;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.webkit.WebView;
import android.widget.TextView;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;

/**
 * <p>Handles display and behavior of the context menu for known actionable content in WebViews.
 * Requires an Activity to bind to for Context resolution and to start other activites.</p>
 * <br>
 * <p>Activity/Fragment clients must forward the 'onContextItemSelected' method.</p>
 * <br>
 * Dependencies:
 * <ul>
 * <li>res/menu/webview_context_menu.xml</li>
 * <li>res/values/webview_context_menu_strings.xml</li>
 * </ul>
 */
public abstract class WebViewContextMenu implements OnCreateContextMenuListener,
        MenuItem.OnMenuItemClickListener {

    private Activity mActivity;

    protected static enum MenuType {
        OPEN_MENU,
        COPY_LINK_MENU,
        SHARE_LINK_MENU,
        DIAL_MENU,
        SMS_MENU,
        ADD_CONTACT_MENU,
        COPY_PHONE_MENU,
        EMAIL_CONTACT_MENU,
        COPY_MAIL_MENU,
        MAP_MENU,
        COPY_GEO_MENU,
    }

    protected static enum MenuGroupType {
        PHONE_GROUP,
        EMAIL_GROUP,
        GEO_GROUP,
        ANCHOR_GROUP,
    }

    public WebViewContextMenu(Activity host) {
        this.mActivity = host;
    }

    // For our copy menu items.
    private class Copy implements MenuItem.OnMenuItemClickListener {
        private final CharSequence mText;

        public Copy(CharSequence text) {
            mText = text;
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            copy(mText);
            return true;
        }
    }

    // For our share menu items.
    private class Share implements MenuItem.OnMenuItemClickListener {
        private final String mUri;

        public Share(String text) {
            mUri = text;
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            shareLink(mUri);
            return true;
        }
    }

    private boolean showShareLinkMenuItem() {
        PackageManager pm = mActivity.getPackageManager();
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        ResolveInfo ri = pm.resolveActivity(send, PackageManager.MATCH_DEFAULT_ONLY);
        return ri != null;
    }

    private void shareLink(String url) {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, url);

        try {
            mActivity.startActivity(Intent.createChooser(send, mActivity.getText(
                    getChooserTitleStringResIdForMenuType(MenuType.SHARE_LINK_MENU))));
        } catch(android.content.ActivityNotFoundException ex) {
            // if no app handles it, do nothing
        }
    }

    private void copy(CharSequence text) {
        ClipboardManager clipboard =
                (ClipboardManager) mActivity.getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(null, text));
    }

    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo info) {
        // FIXME: This is copied over almost directly from BrowserActivity.
        // Would like to find a way to combine the two (Bug 1251210).

        WebView webview = (WebView) v;
        WebView.HitTestResult result = webview.getHitTestResult();
        if (result == null) {
            return;
        }

        int type = result.getType();
        switch (type) {
            case WebView.HitTestResult.UNKNOWN_TYPE:
            case WebView.HitTestResult.EDIT_TEXT_TYPE:
                return;
            default:
                break;
        }

        // Note, http://b/issue?id=1106666 is requesting that
        // an inflated menu can be used again. This is not available
        // yet, so inflate each time (yuk!)
        MenuInflater inflater = mActivity.getMenuInflater();
        // Also, we are copying the menu file from browser until
        // 1251210 is fixed.
        inflater.inflate(getMenuResourceId(), menu);

        // Initially make set the menu item handler this WebViewContextMenu, which will default to
        // calling the non-abstract subclass's implementation.
        for (int i = 0; i < menu.size(); i++) {
            final MenuItem menuItem = menu.getItem(i);
            menuItem.setOnMenuItemClickListener(this);
        }


        // Show the correct menu group
        String extra = result.getExtra();
        menu.setGroupVisible(getMenuGroupResId(MenuGroupType.PHONE_GROUP),
                type == WebView.HitTestResult.PHONE_TYPE);
        menu.setGroupVisible(getMenuGroupResId(MenuGroupType.EMAIL_GROUP),
                type == WebView.HitTestResult.EMAIL_TYPE);
        menu.setGroupVisible(getMenuGroupResId(MenuGroupType.GEO_GROUP),
                type == WebView.HitTestResult.GEO_TYPE);
        menu.setGroupVisible(getMenuGroupResId(MenuGroupType.ANCHOR_GROUP),
                type == WebView.HitTestResult.SRC_ANCHOR_TYPE
                || type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE);

        // Setup custom handling depending on the type
        switch (type) {
            case WebView.HitTestResult.PHONE_TYPE:
                String decodedPhoneExtra;
                try {
                    decodedPhoneExtra = URLDecoder.decode(extra, Charset.defaultCharset().name());
                }
                catch (UnsupportedEncodingException ignore) {
                    // Should never happen; default charset is UTF-8
                    decodedPhoneExtra = extra;
                }

                menu.setHeaderTitle(decodedPhoneExtra);
                // Dial
                final MenuItem dialMenuItem =
                        menu.findItem(getMenuResIdForMenuType(MenuType.DIAL_MENU));
                // remove the on click listener
                dialMenuItem.setOnMenuItemClickListener(null);
                dialMenuItem.setIntent(new Intent(Intent.ACTION_VIEW,
                        Uri.parse(WebView.SCHEME_TEL + extra)));

                // Send SMS
                final MenuItem sendSmsMenuItem =
                        menu.findItem(getMenuResIdForMenuType(MenuType.SMS_MENU));
                // remove the on click listener
                sendSmsMenuItem.setOnMenuItemClickListener(null);
                sendSmsMenuItem.setIntent(new Intent(Intent.ACTION_SENDTO,
                        Uri.parse("smsto:" + extra)));

                // Add to contacts
                final Intent addIntent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
                addIntent.setType(ContactsContract.Contacts.CONTENT_ITEM_TYPE);

                addIntent.putExtra(ContactsContract.Intents.Insert.PHONE, decodedPhoneExtra);
                final MenuItem addToContactsMenuItem =
                        menu.findItem(getMenuResIdForMenuType(MenuType.ADD_CONTACT_MENU));
                // remove the on click listener
                addToContactsMenuItem.setOnMenuItemClickListener(null);
                addToContactsMenuItem.setIntent(addIntent);

                // Copy
                menu.findItem(getMenuResIdForMenuType(MenuType.COPY_PHONE_MENU)).
                        setOnMenuItemClickListener(new Copy(extra));
                break;

            case WebView.HitTestResult.EMAIL_TYPE:
                menu.setHeaderTitle(extra);
                menu.findItem(getMenuResIdForMenuType(MenuType.EMAIL_CONTACT_MENU)).setIntent(
                        new Intent(Intent.ACTION_VIEW, Uri
                                .parse(WebView.SCHEME_MAILTO + extra)));
                menu.findItem(getMenuResIdForMenuType(MenuType.COPY_MAIL_MENU)).
                        setOnMenuItemClickListener(new Copy(extra));
                break;

            case WebView.HitTestResult.GEO_TYPE:
                menu.setHeaderTitle(extra);
                String geoExtra = "";
                try {
                    geoExtra = URLEncoder.encode(extra, Charset.defaultCharset().name());
                }
                catch (UnsupportedEncodingException ignore) {
                    // Should never happen; default charset is UTF-8
                }
                final MenuItem viewMapMenuItem =
                        menu.findItem(getMenuResIdForMenuType(MenuType.MAP_MENU));
                // remove the on click listener
                viewMapMenuItem.setOnMenuItemClickListener(null);
                viewMapMenuItem.setIntent(new Intent(Intent.ACTION_VIEW,
                        Uri.parse(WebView.SCHEME_GEO + geoExtra)));
                menu.findItem(getMenuResIdForMenuType(MenuType.COPY_GEO_MENU)).
                        setOnMenuItemClickListener(new Copy(extra));
                break;

            case WebView.HitTestResult.SRC_ANCHOR_TYPE:
            case WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE:
                // FIXME: Make this look like the normal menu header
                // We cannot use the normal menu header because we need to
                // edit the ContextMenu after it has been created.
                final TextView titleView = (TextView) LayoutInflater.from(mActivity)
                        .inflate(getTitleViewLayoutResId(MenuType.SHARE_LINK_MENU), null);
                menu.setHeaderView(titleView);

                menu.findItem(getMenuResIdForMenuType(MenuType.SHARE_LINK_MENU)).setVisible(
                        showShareLinkMenuItem());

                // The documentation for WebView indicates that if the HitTestResult is
                // SRC_ANCHOR_TYPE or the url would be specified in the extra.  We don't need to
                // call requestFocusNodeHref().  If we wanted to handle UNKNOWN HitTestResults, we
                // would.  With this knowledge, we can just set the title
                titleView.setText(extra);

                menu.findItem(getMenuResIdForMenuType(MenuType.COPY_LINK_MENU)).
                        setOnMenuItemClickListener(new Copy(extra));

                final MenuItem openLinkMenuItem =
                        menu.findItem(getMenuResIdForMenuType(MenuType.OPEN_MENU));
                // remove the on click listener
                openLinkMenuItem.setOnMenuItemClickListener(null);
                openLinkMenuItem.setIntent(new Intent(Intent.ACTION_VIEW, Uri.parse(extra)));

                menu.findItem(getMenuResIdForMenuType(MenuType.SHARE_LINK_MENU)).
                        setOnMenuItemClickListener(new Share(extra));
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return onMenuItemSelected(item);
    }

    /**
     * Returns the menu type from the given resource id
     * @param menuResId resource id of the menu
     * @return MenuType for the specified menu resource id
     */
    abstract protected MenuType getMenuTypeFromResId(int menuResId);

    /**
     * Returns the menu resource id for the specified menu type
     * @param menuType type of the specified menu
     * @return menu resource id
     */
    abstract protected int getMenuResIdForMenuType(MenuType menuType);

    /**
     * Returns the resource id of the string to be used when showing a chooser for a menu
     * @param menuType type of the specified menu
     * @return string resource id
     */
    abstract protected int getChooserTitleStringResIdForMenuType(MenuType menuType);

    /**
     * Returns the resource id of the layout to be used for the title of the specified menu
     * @param menuType type of the specified menu
     * @return layout resource id
     */
    abstract protected int getTitleViewLayoutResId(MenuType menuType);

    /**
     * Returns the menu group resource id for the specified menu group type.
     * @param menuGroupType menu group type
     * @return menu group resource id
     */
    abstract protected int getMenuGroupResId(MenuGroupType menuGroupType);

    /**
     * Returns the resource id for the web view context menu
     */
    abstract protected int getMenuResourceId();


    /**
     * Called when a menu item is not handled by the context menu.
     */
    abstract protected boolean onMenuItemSelected(MenuItem menuItem);
}
