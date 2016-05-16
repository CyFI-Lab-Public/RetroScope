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

package com.android.mail.browse;

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
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.webkit.WebView;

import com.android.mail.R;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;

/**
 * <p>Handles display and behavior of the context menu for known actionable content in WebViews.
 * Requires an Activity to bind to for Context resolution and to start other activites.</p>
 * <br>
 * Dependencies:
 * <ul>
 * <li>res/menu/webview_context_menu.xml</li>
 * </ul>
 */
public class WebViewContextMenu implements OnCreateContextMenuListener,
        MenuItem.OnMenuItemClickListener {

    private final boolean mSupportsDial;
    private final boolean mSupportsSms;

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
        mActivity = host;

        // Query the package manager to see if the device
        // has an app that supports ACTION_DIAL or ACTION_SENDTO
        // with the appropriate uri schemes.
        final PackageManager pm = mActivity.getPackageManager();
        mSupportsDial = !pm.queryIntentActivities(
                new Intent(Intent.ACTION_DIAL, Uri.parse(WebView.SCHEME_TEL)),
                PackageManager.MATCH_DEFAULT_ONLY).isEmpty();
        mSupportsSms = !pm.queryIntentActivities(
                new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")),
                PackageManager.MATCH_DEFAULT_ONLY).isEmpty();
        ;
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

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo info) {
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

                if (mSupportsDial) {
                    // remove the on click listener
                    dialMenuItem.setOnMenuItemClickListener(null);
                    dialMenuItem.setIntent(new Intent(Intent.ACTION_DIAL,
                            Uri.parse(WebView.SCHEME_TEL + extra)));
                } else {
                    dialMenuItem.setVisible(false);
                }

                // Send SMS
                final MenuItem sendSmsMenuItem =
                        menu.findItem(getMenuResIdForMenuType(MenuType.SMS_MENU));
                if (mSupportsSms) {
                    // remove the on click listener
                    sendSmsMenuItem.setOnMenuItemClickListener(null);
                    sendSmsMenuItem.setIntent(new Intent(Intent.ACTION_SENDTO,
                            Uri.parse("smsto:" + extra)));
                } else {
                    sendSmsMenuItem.setVisible(false);
                }

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
                menu.findItem(getMenuResIdForMenuType(MenuType.SHARE_LINK_MENU)).setVisible(
                        showShareLinkMenuItem());

                // The documentation for WebView indicates that if the HitTestResult is
                // SRC_ANCHOR_TYPE or the url would be specified in the extra.  We don't need to
                // call requestFocusNodeHref().  If we wanted to handle UNKNOWN HitTestResults, we
                // would.  With this knowledge, we can just set the title
                menu.setHeaderTitle(extra);

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
    protected MenuType getMenuTypeFromResId(final int menuResId) {
        if (menuResId == R.id.open_context_menu_id) {
            return MenuType.OPEN_MENU;
        } else if (menuResId == R.id.copy_link_context_menu_id) {
            return MenuType.COPY_LINK_MENU;
        } else if (menuResId == R.id.share_link_context_menu_id) {
            return MenuType.SHARE_LINK_MENU;
        } else if (menuResId == R.id.dial_context_menu_id) {
            return MenuType.DIAL_MENU;
        } else if (menuResId == R.id.sms_context_menu_id) {
            return MenuType.SMS_MENU;
        } else if (menuResId == R.id.add_contact_context_menu_id) {
            return MenuType.ADD_CONTACT_MENU;
        } else if (menuResId == R.id.copy_phone_context_menu_id) {
            return MenuType.COPY_PHONE_MENU;
        } else if (menuResId == R.id.email_context_menu_id) {
            return MenuType.EMAIL_CONTACT_MENU;
        } else if (menuResId == R.id.copy_mail_context_menu_id) {
            return MenuType.COPY_MAIL_MENU;
        } else if (menuResId == R.id.map_context_menu_id) {
            return MenuType.MAP_MENU;
        } else if (menuResId == R.id.copy_geo_context_menu_id) {
            return MenuType.COPY_GEO_MENU;
        } else {
            throw new IllegalStateException("Unexpected resource id");
        }
    }

    /**
     * Returns the menu resource id for the specified menu type
     * @param menuType type of the specified menu
     * @return menu resource id
     */
    protected int getMenuResIdForMenuType(MenuType menuType) {
        switch(menuType) {
            case OPEN_MENU:
                return R.id.open_context_menu_id;
            case COPY_LINK_MENU:
                return R.id.copy_link_context_menu_id;
            case SHARE_LINK_MENU:
                return R.id.share_link_context_menu_id;
            case DIAL_MENU:
                return R.id.dial_context_menu_id;
            case SMS_MENU:
                return R.id.sms_context_menu_id;
            case ADD_CONTACT_MENU:
                return R.id.add_contact_context_menu_id;
            case COPY_PHONE_MENU:
                return R.id.copy_phone_context_menu_id;
            case EMAIL_CONTACT_MENU:
                return R.id.email_context_menu_id;
            case COPY_MAIL_MENU:
                return R.id.copy_mail_context_menu_id;
            case MAP_MENU:
                return R.id.map_context_menu_id;
            case COPY_GEO_MENU:
                return R.id.copy_geo_context_menu_id;
            default:
                throw new IllegalStateException("Unexpected MenuType");
        }
    }

    /**
     * Returns the resource id of the string to be used when showing a chooser for a menu
     * @param menuType type of the specified menu
     * @return string resource id
     */
    protected int getChooserTitleStringResIdForMenuType(MenuType menuType) {
        switch(menuType) {
            case SHARE_LINK_MENU:
                return R.string.choosertitle_sharevia;
            default:
                throw new IllegalStateException("Unexpected MenuType");
        }
    }

    /**
     * Returns the menu group resource id for the specified menu group type.
     * @param menuGroupType menu group type
     * @return menu group resource id
     */
    protected int getMenuGroupResId(MenuGroupType menuGroupType) {
        switch (menuGroupType) {
            case PHONE_GROUP:
                return R.id.PHONE_MENU;
            case EMAIL_GROUP:
                return R.id.EMAIL_MENU;
            case GEO_GROUP:
                return R.id.GEO_MENU;
            case ANCHOR_GROUP:
                return R.id.ANCHOR_MENU;
            default:
                throw new IllegalStateException("Unexpected MenuGroupType");
        }
    }

    /**
     * Returns the resource id for the web view context menu
     */
    protected int getMenuResourceId() {
        return R.menu.webview_context_menu;
    }


    /**
     * Called when a menu item is not handled by the context menu.
     */
    protected boolean onMenuItemSelected(MenuItem menuItem) {
        return mActivity.onOptionsItemSelected(menuItem);
    }
}
