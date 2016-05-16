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

package com.android.mail.ui;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.provider.Browser;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.android.mail.providers.Account;
import com.android.mail.providers.UIProvider;
import com.android.mail.utils.LogTag;
import com.android.mail.utils.LogUtils;
import com.android.mail.utils.Utils;

import java.util.List;

/**
 * Base implementation of a web view client for the conversation views.
 * Handles proxying the view intent so that additional information can
 * be sent with the intent when links are clicked.
 */
public class AbstractConversationWebViewClient extends WebViewClient {
    private static final String LOG_TAG = LogTag.getLogTag();

    private Account mAccount;
    private Activity mActivity;

    public AbstractConversationWebViewClient(Account account) {
        mAccount = account;
    }

    public void setAccount(Account account) {
        mAccount = account;
    }

    public void setActivity(Activity activity) {
        mActivity = activity;
    }

    public Activity getActivity() {
        return mActivity;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (mActivity == null) {
            return false;
        }

        final Uri uri = Uri.parse(url);
        if (Utils.divertMailtoUri(mActivity, uri, mAccount)) {
            return true;
        }

        final Intent intent;
        if (mAccount != null && !Utils.isEmpty(mAccount.viewIntentProxyUri)) {
            intent = generateProxyIntent(uri);
        } else {
            intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.putExtra(Browser.EXTRA_APPLICATION_ID, mActivity.getPackageName());
        }

        boolean result = false;
        try {
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET
                    | Intent.FLAG_ACTIVITY_NO_ANIMATION);
            mActivity.startActivity(intent);
            result = true;
        } catch (ActivityNotFoundException ex) {
            // If no application can handle the URL, assume that the
            // caller can handle it.
        }

        return result;
    }

    private Intent generateProxyIntent(Uri uri) {
        final Intent intent = new Intent(Intent.ACTION_VIEW, mAccount.viewIntentProxyUri);
        intent.putExtra(UIProvider.ViewProxyExtras.EXTRA_ORIGINAL_URI, uri);
        intent.putExtra(UIProvider.ViewProxyExtras.EXTRA_ACCOUNT, mAccount);

        PackageManager manager = null;
        // We need to catch the exception to make CanvasConversationHeaderView
        // test pass.  Bug: http://b/issue?id=3470653.
        try {
            manager = mActivity.getPackageManager();
        } catch (UnsupportedOperationException e) {
            LogUtils.e(LOG_TAG, e, "Error getting package manager");
        }

        if (manager != null) {
            // Try and resolve the intent, to find an activity from this package
            final List<ResolveInfo> resolvedActivities = manager.queryIntentActivities(
                    intent, PackageManager.MATCH_DEFAULT_ONLY);

            final String packageName = mActivity.getPackageName();

            // Now try and find one that came from this package, if one is not found, the UI
            // provider must have specified an intent that is to be handled by a different apk.
            // In that case, the class name will not be set on the intent, so the default
            // intent resolution will be used.
            for (ResolveInfo resolveInfo: resolvedActivities) {
                final ActivityInfo activityInfo = resolveInfo.activityInfo;
                if (packageName.equals(activityInfo.packageName)) {
                    intent.setClassName(activityInfo.packageName, activityInfo.name);
                    break;
                }
            }
        }

        return intent;
    }
}
