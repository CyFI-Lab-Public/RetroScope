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

package com.android.mail.providers;

import android.app.DownloadManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;

import com.android.ex.photo.provider.PhotoContract;
import com.android.mail.R;
import com.android.mail.utils.LogTag;
import com.android.mail.utils.LogUtils;
import com.android.mail.utils.MimeType;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * A {@link ContentProvider} for attachments created from eml files.
 * Supports all of the semantics (query/insert/update/delete/openFile)
 * of the regular attachment provider.
 *
 * One major difference is that all attachment info is stored in memory (with the
 * exception of the attachment raw data which is stored in the cache). When
 * the process is killed, all of the attachments disappear if they still
 * exist.
 */
public class EmlAttachmentProvider extends ContentProvider {
    private static final String LOG_TAG = LogTag.getLogTag();

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    private static boolean sUrisAddedToMatcher = false;

    private static final int ATTACHMENT_LIST = 0;
    private static final int ATTACHMENT = 1;

    /**
     * The buffer size used to copy data from cache to sd card.
     */
    private static final int BUFFER_SIZE = 4096;

    /** Any IO reads should be limited to this timeout */
    private static final long READ_TIMEOUT = 3600 * 1000;

    private static Uri BASE_URI;

    private DownloadManager mDownloadManager;

    /**
     * Map that contains a mapping from an attachment list uri to a list of uris.
     */
    private Map<Uri, List<Uri>> mUriListMap;

    /**
     * Map that contains a mapping from an attachment uri to an {@link Attachment} object.
     */
    private Map<Uri, Attachment> mUriAttachmentMap;


    @Override
    public boolean onCreate() {
        final String authority =
                getContext().getResources().getString(R.string.eml_attachment_provider);
        BASE_URI = new Uri.Builder().scheme("content").authority(authority).build();

        if (!sUrisAddedToMatcher) {
            sUrisAddedToMatcher = true;
            sUriMatcher.addURI(authority, "*/*", ATTACHMENT_LIST);
            sUriMatcher.addURI(authority, "*/*/#", ATTACHMENT);
        }

        mDownloadManager =
                (DownloadManager) getContext().getSystemService(Context.DOWNLOAD_SERVICE);

        mUriListMap = Maps.newHashMap();
        mUriAttachmentMap = Maps.newHashMap();
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        final int match = sUriMatcher.match(uri);
        // ignore other projections
        final MatrixCursor cursor = new MatrixCursor(UIProvider.ATTACHMENT_PROJECTION);

        switch (match) {
            case ATTACHMENT_LIST:
                final List<String> contentTypeQueryParameters =
                        uri.getQueryParameters(PhotoContract.ContentTypeParameters.CONTENT_TYPE);
                uri = uri.buildUpon().clearQuery().build();
                final List<Uri> attachmentUris = mUriListMap.get(uri);
                for (final Uri attachmentUri : attachmentUris) {
                    addRow(cursor, attachmentUri, contentTypeQueryParameters);
                }
                cursor.setNotificationUri(getContext().getContentResolver(), uri);
                break;
            case ATTACHMENT:
                addRow(cursor, mUriAttachmentMap.get(uri));
                cursor.setNotificationUri(
                        getContext().getContentResolver(), getListUriFromAttachmentUri(uri));
                break;
            default:
                break;
        }

        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case ATTACHMENT:
                return mUriAttachmentMap.get(uri).getContentType();
            default:
                return null;
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final Uri listUri = getListUriFromAttachmentUri(uri);

        // add mapping from uri to attachment
        if (mUriAttachmentMap.put(uri, new Attachment(values)) == null) {
            // only add uri to list if the list
            // get list of attachment uris, creating if necessary
            List<Uri> list = mUriListMap.get(listUri);
            if (list == null) {
                list = Lists.newArrayList();
                mUriListMap.put(listUri, list);
            }

            list.add(uri);
        }

        return uri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case ATTACHMENT_LIST:
                // remove from list mapping
                final List<Uri> attachmentUris = mUriListMap.remove(uri);

                // delete each file and remove each element from the mapping
                for (final Uri attachmentUri : attachmentUris) {
                    mUriAttachmentMap.remove(attachmentUri);
                }

                deleteDirectory(getCacheFileDirectory(uri));
                // return rows affected
                return attachmentUris.size();
            default:
                return 0;
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case ATTACHMENT:
                return copyAttachment(uri, values);
            default:
                return 0;
        }
    }

    /**
     * Adds a row to the cursor for the attachment at the specific attachment {@link Uri}
     * if the attachment's mime type matches one of the query parameters.
     *
     * Matching is defined to be starting with one of the query parameters. If no
     * parameters exist, all rows are added.
     */
    private void addRow(MatrixCursor cursor, Uri uri,
            List<String> contentTypeQueryParameters) {
        final Attachment attachment = mUriAttachmentMap.get(uri);

        if (contentTypeQueryParameters != null && !contentTypeQueryParameters.isEmpty()) {
            for (final String type : contentTypeQueryParameters) {
                if (attachment.getContentType().startsWith(type)) {
                    addRow(cursor, attachment);
                    return;
                }
            }
        } else {
            addRow(cursor, attachment);
        }
    }

    /**
     * Adds a new row to the cursor for the specific attachment.
     */
    private static void addRow(MatrixCursor cursor, Attachment attachment) {
        cursor.newRow()
                .add(attachment.getName())                          // displayName
                .add(attachment.size)                               // size
                .add(attachment.uri)                                // uri
                .add(attachment.getContentType())                   // contentType
                .add(attachment.state)                              // state
                .add(attachment.destination)                        // destination
                .add(attachment.downloadedSize)                     // downloadedSize
                .add(attachment.contentUri)                         // contentUri
                .add(attachment.thumbnailUri)                       // thumbnailUri
                .add(attachment.previewIntentUri)                   // previewIntentUri
                .add(attachment.providerData)                       // providerData
                .add(attachment.supportsDownloadAgain() ? 1 : 0);   // supportsDownloadAgain
    }

    /**
     * Copies an attachment at the specified {@link Uri}
     * from cache to the external downloads directory (usually the sd card).
     * @return the number of attachments affected. Should be 1 or 0.
     */
    private int copyAttachment(Uri uri, ContentValues values) {
        final Integer newState = values.getAsInteger(UIProvider.AttachmentColumns.STATE);
        final Integer newDestination =
                values.getAsInteger(UIProvider.AttachmentColumns.DESTINATION);
        if (newState == null && newDestination == null) {
            return 0;
        }

        final int destination = newDestination != null ?
                newDestination.intValue() : UIProvider.AttachmentDestination.CACHE;
        final boolean saveToSd =
                destination == UIProvider.AttachmentDestination.EXTERNAL;

        final Attachment attachment = mUriAttachmentMap.get(uri);

        // 1. check if already saved to sd (via uri save to sd)
        // and return if so (we shouldn't ever be here)

        // if the call was not to save to sd or already saved to sd, just bail out
        if (!saveToSd || attachment.isSavedToExternal()) {
            return 0;
        }


        // 2. copy file
        final String oldFilePath = getFilePath(uri);

        // update the destination before getting the new file path
        // otherwise it will just point to the old location.
        attachment.destination = UIProvider.AttachmentDestination.EXTERNAL;
        final String newFilePath = getFilePath(uri);

        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            try {
                inputStream = new FileInputStream(oldFilePath);
            } catch (FileNotFoundException e) {
                LogUtils.e(LOG_TAG, "File not found for file %s", oldFilePath);
                return 0;
            }
            try {
                outputStream = new FileOutputStream(newFilePath);
            } catch (FileNotFoundException e) {
                LogUtils.e(LOG_TAG, "File not found for file %s", newFilePath);
                return 0;
            }
            try {
                final long now = SystemClock.elapsedRealtime();
                final byte data[] = new byte[BUFFER_SIZE];
                int size = 0;
                while (true) {
                    final int len = inputStream.read(data);
                    if (len != -1) {
                        outputStream.write(data, 0, len);

                        size += len;
                    } else {
                        break;
                    }
                    if (SystemClock.elapsedRealtime() - now > READ_TIMEOUT) {
                        throw new IOException("Timed out copying attachment.");
                    }
                }

                // if the attachment is an APK, change contentUri to be a direct file uri
                if (MimeType.isInstallable(attachment.getContentType())) {
                    attachment.contentUri = Uri.parse("file://" + newFilePath);
                }

                // 3. add file to download manager

                try {
                    // TODO - make a better description
                    final String description = attachment.getName();
                    mDownloadManager.addCompletedDownload(attachment.getName(),
                            description, true, attachment.getContentType(),
                            newFilePath, size, false);
                }
                catch (IllegalArgumentException e) {
                    // Even if we cannot save the download to the downloads app,
                    // (likely due to a bad mimeType), we still want to save it.
                    LogUtils.e(LOG_TAG, e, "Failed to save download to Downloads app.");
                }
                final Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                intent.setData(Uri.parse("file://" + newFilePath));
                getContext().sendBroadcast(intent);

                // 4. delete old file
                new File(oldFilePath).delete();
            } catch (IOException e) {
                // Error writing file, delete partial file
                LogUtils.e(LOG_TAG, e, "Cannot write to file %s", newFilePath);
                new File(newFilePath).delete();
            }
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
            }
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
            }
        }

        // 5. notify that the list of attachments has changed so the UI will update
        getContext().getContentResolver().notifyChange(
                getListUriFromAttachmentUri(uri), null, false);
        return 1;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        final String filePath = getFilePath(uri);

        final int fileMode;

        if ("rwt".equals(mode)) {
            fileMode = ParcelFileDescriptor.MODE_READ_WRITE |
                    ParcelFileDescriptor.MODE_TRUNCATE |
                    ParcelFileDescriptor.MODE_CREATE;
        } else if ("rw".equals(mode)) {
            fileMode = ParcelFileDescriptor.MODE_READ_WRITE | ParcelFileDescriptor.MODE_CREATE;
        } else {
            fileMode = ParcelFileDescriptor.MODE_READ_ONLY;
        }

        return ParcelFileDescriptor.open(new File(filePath), fileMode);
    }

    /**
     * Returns an attachment list uri for an eml file at the given uri
     * with the given message id.
     */
    public static Uri getAttachmentsListUri(Uri emlFileUri, String messageId) {
        return BASE_URI.buildUpon().appendPath(Integer.toString(emlFileUri.hashCode()))
                .appendPath(messageId).build();
    }

    /**
     * Returns an attachment list uri for the specific attachment uri passed.
     */
    public static Uri getListUriFromAttachmentUri(Uri uri) {
        final List<String> segments = uri.getPathSegments();
        return BASE_URI.buildUpon()
                .appendPath(segments.get(0)).appendPath(segments.get(1)).build();
    }

    /**
     * Returns an attachment uri for an attachment from the given eml file uri with
     * the given message id and part id.
     */
    public static Uri getAttachmentUri(Uri emlFileUri, String messageId, String partId) {
        return BASE_URI.buildUpon().appendPath(Integer.toString(emlFileUri.hashCode()))
                .appendPath(messageId).appendPath(partId).build();
    }

    /**
     * Returns the absolute file path for the attachment at the given uri.
     */
    private String getFilePath(Uri uri) {
        final Attachment attachment = mUriAttachmentMap.get(uri);
        final boolean saveToSd =
                attachment.destination == UIProvider.AttachmentDestination.EXTERNAL;
        final String pathStart = (saveToSd) ?
                Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() : getCacheDir();

        // we want the root of the downloads directory if the attachment is
        // saved to external (or we're saving to external)
        final String directoryPath = (saveToSd) ? pathStart : pathStart + uri.getEncodedPath();

        final File directory = new File(directoryPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        return directoryPath + "/" + attachment.getName();
    }

    /**
     * Returns the root directory for the attachments for the specific uri.
     */
    private String getCacheFileDirectory(Uri uri) {
        return getCacheDir() + "/" + Uri.encode(uri.getPathSegments().get(0));
    }

    /**
     * Returns the cache directory for eml attachment files.
     */
    private String getCacheDir() {
        return getContext().getCacheDir().getAbsolutePath().concat("/eml");
    }

    /**
     * Recursively delete the directory at the passed file path.
     */
    private void deleteDirectory(String cacheFileDirectory) {
        recursiveDelete(new File(cacheFileDirectory));
    }

    /**
     * Recursively deletes a file or directory.
     */
    private void recursiveDelete(File file) {
        if (file.isDirectory()) {
            final File[] children = file.listFiles();
            for (final File child : children) {
                recursiveDelete(child);
            }
        }

        file.delete();
    }
}
