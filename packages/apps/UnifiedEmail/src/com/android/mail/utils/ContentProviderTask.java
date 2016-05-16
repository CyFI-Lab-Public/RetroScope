/*
 * Copyright (C) 2012 Google Inc.
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

package com.android.mail.utils;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.AsyncTask;

import com.google.common.collect.Lists;

import java.util.ArrayList;

/**
 * Simple utility class to make an asynchronous {@link ContentProvider} request expressed as
 * a list of {@link ContentProviderOperation}s. As with all {@link AsyncTask}s, subclasses should
 * override {@link #onPostExecute(Object)} to handle success/failure.
 *
 * @see InsertTask
 * @see UpdateTask
 * @see DeleteTask
 *
 */
public class ContentProviderTask extends AsyncTask<Void, Void, ContentProviderTask.Result> {

    private ContentResolver mResolver;
    private String mAuthority;
    private ArrayList<ContentProviderOperation> mOps;

    private static final String LOG_TAG = LogTag.getLogTag();

    public static class Result {
        public final Exception exception;
        public final ContentProviderResult[] results;

        /**
         * Create a new result.
         * @param exception
         * @param results
         */
        private Result(Exception exception, ContentProviderResult[] results) {
            this.exception = exception;
            this.results = results;
        }

        /**
         * Create a new success result.
         * @param success
         * @return
         */
        private static Result newSuccess(ContentProviderResult[] success) {
            return new Result(null, success);
        }

        /**
         * Create a new failure result.
         * @param failure
         */
        private static Result newFailure(Exception failure) {
            return new Result(failure, null);
        }
    }

    @Override
    protected Result doInBackground(Void... params) {
        Result result;
            try {
                result = Result.newSuccess(mResolver.applyBatch(mAuthority, mOps));
            } catch (Exception e) {
                LogUtils.w(LOG_TAG, e, "exception executing ContentProviderOperationsTask");
                result = Result.newFailure(e);
            }
        return result;
    }

    public void run(ContentResolver resolver, String authority,
            ArrayList<ContentProviderOperation> ops) {
        mResolver = resolver;
        mAuthority = authority;
        mOps = ops;
        executeOnExecutor(THREAD_POOL_EXECUTOR, (Void) null);
    }

    public static class InsertTask extends ContentProviderTask {

        public void run(ContentResolver resolver, Uri uri, ContentValues values) {
            final ContentProviderOperation op = ContentProviderOperation
                    .newInsert(uri)
                    .withValues(values)
                    .build();
            super.run(resolver, uri.getAuthority(), Lists.newArrayList(op));
        }

    }

    public static class UpdateTask extends ContentProviderTask {

        public void run(ContentResolver resolver, Uri uri, ContentValues values,
                String selection, String[] selectionArgs) {
            final ContentProviderOperation op = ContentProviderOperation
                    .newUpdate(uri)
                    .withValues(values)
                    .withSelection(selection, selectionArgs)
                    .build();
            super.run(resolver, uri.getAuthority(), Lists.newArrayList(op));
        }

    }

    public static class DeleteTask extends ContentProviderTask {

        public void run(ContentResolver resolver, Uri uri, String selection,
                String[] selectionArgs) {
            final ContentProviderOperation op = ContentProviderOperation
                    .newDelete(uri)
                    .withSelection(selection, selectionArgs)
                    .build();
            super.run(resolver, uri.getAuthority(), Lists.newArrayList(op));
        }

    }

}
