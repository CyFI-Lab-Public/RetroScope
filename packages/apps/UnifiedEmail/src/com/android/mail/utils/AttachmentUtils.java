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
package com.android.mail.utils;

import com.google.common.collect.ImmutableMap;

import android.app.DownloadManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.text.TextUtils;

import com.android.mail.R;
import com.android.mail.providers.Attachment;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class AttachmentUtils {
    private static final String LOG_TAG = LogTag.getLogTag();

    private static final int KILO = 1024;
    private static final int MEGA = KILO * KILO;

    /** Any IO reads should be limited to this timeout */
    private static final long READ_TIMEOUT = 3600 * 1000;

    private static final float MIN_CACHE_THRESHOLD = 0.25f;
    private static final int MIN_CACHE_AVAILABLE_SPACE_BYTES = 100 * 1024 * 1024;

    /**
     * Singleton map of MIME->friendly description
     * @see #getMimeTypeDisplayName(Context, String)
     */
    private static Map<String, String> sDisplayNameMap;

    /**
     * @return A string suitable for display in bytes, kilobytes or megabytes
     *         depending on its size.
     */
    public static String convertToHumanReadableSize(Context context, long size) {
        final String count;
        if (size == 0) {
            return "";
        } else if (size < KILO) {
            count = String.valueOf(size);
            return context.getString(R.string.bytes, count);
        } else if (size < MEGA) {
            count = String.valueOf(size / KILO);
            return context.getString(R.string.kilobytes, count);
        } else {
            DecimalFormat onePlace = new DecimalFormat("0.#");
            count = onePlace.format((float) size / (float) MEGA);
            return context.getString(R.string.megabytes, count);
        }
    }

    /**
     * Return a friendly localized file type for this attachment, or the empty string if
     * unknown.
     * @param context a Context to do resource lookup against
     * @return friendly file type or empty string
     */
    public static String getDisplayType(final Context context, final Attachment attachment) {
        if ((attachment.flags & Attachment.FLAG_DUMMY_ATTACHMENT) != 0) {
            // This is a dummy attachment, display blank for type.
            return null;
        }

        // try to get a friendly name for the exact mime type
        // then try to show a friendly name for the mime family
        // finally, give up and just show the file extension
        final String contentType = attachment.getContentType();
        String displayType = getMimeTypeDisplayName(context, contentType);
        int index = !TextUtils.isEmpty(contentType) ? contentType.indexOf('/') : -1;
        if (displayType == null && index > 0) {
            displayType = getMimeTypeDisplayName(context, contentType.substring(0, index));
        }
        if (displayType == null) {
            String extension = Utils.getFileExtension(attachment.getName());
            // show '$EXTENSION File' for unknown file types
            if (extension != null && extension.length() > 1 && extension.indexOf('.') == 0) {
                displayType = context.getString(R.string.attachment_unknown,
                        extension.substring(1).toUpperCase());
            }
        }
        if (displayType == null) {
            // no extension to display, but the map doesn't accept null entries
            displayType = "";
        }
        return displayType;
    }

    /**
     * Returns a user-friendly localized description of either a complete a MIME type or a
     * MIME family.
     * @param context used to look up localized strings
     * @param type complete MIME type or just MIME family
     * @return localized description text, or null if not recognized
     */
    public static synchronized String getMimeTypeDisplayName(final Context context,
            String type) {
        if (sDisplayNameMap == null) {
            String docName = context.getString(R.string.attachment_application_msword);
            String presoName = context.getString(R.string.attachment_application_vnd_ms_powerpoint);
            String sheetName = context.getString(R.string.attachment_application_vnd_ms_excel);

            sDisplayNameMap = new ImmutableMap.Builder<String, String>()
                .put("image", context.getString(R.string.attachment_image))
                .put("audio", context.getString(R.string.attachment_audio))
                .put("video", context.getString(R.string.attachment_video))
                .put("text", context.getString(R.string.attachment_text))
                .put("application/pdf", context.getString(R.string.attachment_application_pdf))

                // Documents
                .put("application/msword", docName)
                .put("application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        docName)

                // Presentations
                .put("application/vnd.ms-powerpoint",
                        presoName)
                .put("application/vnd.openxmlformats-officedocument.presentationml.presentation",
                        presoName)

                // Spreadsheets
                .put("application/vnd.ms-excel", sheetName)
                .put("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        sheetName)

                .build();
        }
        return sDisplayNameMap.get(type);
    }

    /**
     * Cache the file specified by the given attachment.  This will attempt to use any
     * {@link ParcelFileDescriptor} in the Bundle parameter
     * @param context
     * @param attachment  Attachment to be cached
     * @param attachmentFds optional {@link Bundle} containing {@link ParcelFileDescriptor} if the
     *        caller has opened the files
     * @return String file path for the cached attachment
     */
    // TODO(pwestbro): Once the attachment has a field for the cached path, this method should be
    // changed to update the attachment, and return a boolean indicating that the attachment has
    // been cached.
    public static String cacheAttachmentUri(Context context, Attachment attachment,
            Bundle attachmentFds) {
        final File cacheDir = context.getCacheDir();

        final long totalSpace = cacheDir.getTotalSpace();
        if (attachment.size > 0) {
            final long usableSpace = cacheDir.getUsableSpace() - attachment.size;
            if (isLowSpace(totalSpace, usableSpace)) {
                LogUtils.w(LOG_TAG, "Low memory (%d/%d). Can't cache attachment %s",
                        usableSpace, totalSpace, attachment);
                return null;
            }
        }
        InputStream inputStream = null;
        FileOutputStream outputStream = null;
        File file = null;
        try {
            final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-kk:mm:ss");
            file = File.createTempFile(dateFormat.format(new Date()), ".attachment", cacheDir);
            final ParcelFileDescriptor fileDescriptor = attachmentFds != null
                    && attachment.contentUri != null ? (ParcelFileDescriptor) attachmentFds
                    .getParcelable(attachment.contentUri.toString())
                    : null;
            if (fileDescriptor != null) {
                // Get the input stream from the file descriptor
                inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
            } else {
                if (attachment.contentUri == null) {
                    // The contentUri of the attachment is null.  This can happen when sending a
                    // message that has been previously saved, and the attachments had been
                    // uploaded.
                    LogUtils.d(LOG_TAG, "contentUri is null in attachment: %s", attachment);
                    throw new FileNotFoundException("Missing contentUri in attachment");
                }
                // Attempt to open the file
                inputStream = context.getContentResolver().openInputStream(attachment.contentUri);
            }
            outputStream = new FileOutputStream(file);
            final long now = SystemClock.elapsedRealtime();
            final byte[] bytes = new byte[1024];
            while (true) {
                int len = inputStream.read(bytes);
                if (len <= 0) {
                    break;
                }
                outputStream.write(bytes, 0, len);
                if (SystemClock.elapsedRealtime() - now > READ_TIMEOUT) {
                    throw new IOException("Timed out reading attachment data");
                }
            }
            outputStream.flush();
            String cachedFileUri = file.getAbsolutePath();
            LogUtils.d(LOG_TAG, "Cached %s to %s", attachment.contentUri, cachedFileUri);

            final long usableSpace = cacheDir.getUsableSpace();
            if (isLowSpace(totalSpace, usableSpace)) {
                file.delete();
                LogUtils.w(LOG_TAG, "Low memory (%d/%d). Can't cache attachment %s",
                        usableSpace, totalSpace, attachment);
                cachedFileUri = null;
            }

            return cachedFileUri;
        } catch (IOException e) {
            // Catch any exception here to allow for unexpected failures during caching se we don't
            // leave app in inconsistent state as we call this method outside of a transaction for
            // performance reasons.
            LogUtils.e(LOG_TAG, e, "Failed to cache attachment %s", attachment);
            if (file != null) {
                file.delete();
            }
            return null;
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                LogUtils.w(LOG_TAG, e, "Failed to close stream");
            }
        }
    }

    private static boolean isLowSpace(long totalSpace, long usableSpace) {
        // For caching attachments we want to enable caching if there is
        // more than 100MB available, or if 25% of total space is free on devices
        // where the cache partition is < 400MB.
        return usableSpace <
                Math.min(totalSpace * MIN_CACHE_THRESHOLD, MIN_CACHE_AVAILABLE_SPACE_BYTES);
    }

    /**
     * Checks if the attachment can be downloaded with the current network
     * connection.
     *
     * @param attachment the attachment to be checked
     * @return true if the attachment can be downloaded.
     */
    public static boolean canDownloadAttachment(Context context, Attachment attachment) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        if (info == null) {
            return false;
        } else if (info.isConnected()) {
            if (info.getType() != ConnectivityManager.TYPE_MOBILE) {
                // not mobile network
                return true;
            } else {
                // mobile network
                Long maxBytes = DownloadManager.getMaxBytesOverMobile(context);
                return maxBytes == null || attachment == null || attachment.size <= maxBytes;
            }
        } else {
            return false;
        }
    }
}
