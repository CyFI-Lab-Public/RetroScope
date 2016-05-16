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

import android.content.AsyncTaskLoader;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.android.emailcommon.TempDirectory;
import com.android.emailcommon.internet.MimeMessage;
import com.android.emailcommon.mail.MessagingException;
import com.android.mail.utils.LogTag;
import com.android.mail.utils.LogUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Loader that builds a ConversationMessage from an EML file Uri.
 */
public class EmlMessageLoader extends AsyncTaskLoader<ConversationMessage> {
    private static final String LOG_TAG = LogTag.getLogTag();

    private Uri mEmlFileUri;
    private ConversationMessage mMessage;

    public EmlMessageLoader(Context context, Uri emlFileUri) {
        super(context);
        mEmlFileUri = emlFileUri;
    }

    @Override
    public ConversationMessage loadInBackground() {
        final Context context = getContext();
        TempDirectory.setTempDirectory(context);
        final ContentResolver resolver = context.getContentResolver();
        final InputStream stream;
        try {
            stream = resolver.openInputStream(mEmlFileUri);
        } catch (FileNotFoundException e) {
            LogUtils.e(LOG_TAG, e, "Could not find eml file at uri: %s", mEmlFileUri);
            return null;
        }

        final MimeMessage mimeMessage;
        final ConversationMessage convMessage;
        try {
            mimeMessage = new MimeMessage(stream);
            convMessage = new ConversationMessage(context, mimeMessage, mEmlFileUri);
        } catch (IOException e) {
            LogUtils.e(LOG_TAG, e, "Could not read eml file");
            return null;
        } catch (MessagingException e) {
            LogUtils.e(LOG_TAG, e, "Error in parsing eml file");
            return null;
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
                return null;
            }

            // delete temp files created during parsing
            final File[] cacheFiles = TempDirectory.getTempDirectory().listFiles();
            for (final File file : cacheFiles) {
                if (file.getName().startsWith("body")) {
                    file.delete();
                }
            }

        }

        return convMessage;
    }

    /**
     * Called when there is new data to deliver to the client.  The
     * super class will take care of delivering it; the implementation
     * here just adds a little more logic.
     */
    @Override
    public void deliverResult(ConversationMessage result) {
        if (isReset()) {
            // An async query came in while the loader is stopped.  We
            // don't need the result.
            if (result != null) {
                onReleaseResources(result);
            }
            return;
        }
        ConversationMessage oldMessage = mMessage;
        mMessage = result;

        if (isStarted()) {
            // If the Loader is currently started, we can immediately
            // deliver its results.
            super.deliverResult(result);
        }

        // At this point we can release the resources associated with
        // 'oldMessage' if needed; now that the new result is delivered we
        // know that it is no longer in use.
        if (oldMessage != null && oldMessage != mMessage) {
            onReleaseResources(oldMessage);
        }
    }

    /**
     * Handles a request to start the Loader.
     */
    @Override
    protected void onStartLoading() {
        if (mMessage != null) {
            // If we currently have a result available, deliver it immediately.
            deliverResult(mMessage);
        }

        if (takeContentChanged() || mMessage == null) {
            // If the data has changed since the last time it was loaded
            // or is not currently available, start a load.
            forceLoad();
        }
    }

    /**
     * Handles a request to stop the Loader.
     */
    @Override protected void onStopLoading() {
        // Attempt to cancel the current load task if possible.
        cancelLoad();
    }

    /**
     * Handles a request to cancel a load.
     */
    @Override
    public void onCanceled(ConversationMessage result) {
        super.onCanceled(result);

        // At this point we can release the resources associated with
        // the message, if needed.
        if (result != null) {
            onReleaseResources(result);
        }
    }

    /**
     * Handles a request to completely reset the Loader.
     */
    @Override
    protected void onReset() {
        super.onReset();

        // Ensure the loader is stopped
        onStopLoading();

        // At this point we can release the resources associated with
        // the message, if needed.
        if (mMessage != null) {
            onReleaseResources(mMessage);
            mMessage = null;
        }
    }


    /**
     * Helper function to take care of releasing resources associated
     * with an actively loaded data set.
     */
    protected void onReleaseResources(ConversationMessage message) {
        // if this eml message had attachments, start a service to clean up the cache files
        if (message.attachmentListUri != null) {
            final Intent intent = new Intent(Intent.ACTION_DELETE);
            intent.setClass(getContext(), EmlTempFileDeletionService.class);
            intent.setData(message.attachmentListUri);

            getContext().startService(intent);
        }
    }
}
