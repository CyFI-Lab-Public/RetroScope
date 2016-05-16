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

package com.android.mail.ui;

import android.content.ContentResolver;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.DisplayMetrics;

import com.android.ex.photo.util.Exif;
import com.android.ex.photo.util.ImageUtils;

import com.android.mail.providers.Attachment;
import com.android.mail.utils.LogTag;
import com.android.mail.utils.LogUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * Performs the load of a thumbnail bitmap in a background
 * {@link AsyncTask}. Available for use with any view that implements
 * the {@link AttachmentBitmapHolder} interface.
 */
public class ThumbnailLoadTask extends AsyncTask<Uri, Void, Bitmap> {
    private static final String LOG_TAG = LogTag.getLogTag();

    private final AttachmentBitmapHolder mHolder;
    private final int mWidth;
    private final int mHeight;

    public static void setupThumbnailPreview(
            ThumbnailLoadTask task, final AttachmentBitmapHolder holder,
            final Attachment attachment, final Attachment prevAttachment) {
        final int width = holder.getThumbnailWidth();
        final int height = holder.getThumbnailHeight();
        if (attachment == null || width == 0 || height == 0
                || !ImageUtils.isImageMimeType(attachment.getContentType())) {
            holder.setThumbnailToDefault();
            return;
        }

        final Uri thumbnailUri = attachment.thumbnailUri;
        final Uri contentUri = attachment.contentUri;
        final Uri uri = (prevAttachment == null) ? null : prevAttachment.getIdentifierUri();
        final Uri prevUri = (prevAttachment == null) ? null : prevAttachment.getIdentifierUri();
        // begin loading a thumbnail if this is an image and either the thumbnail or the original
        // content is ready (and different from any existing image)
        if ((thumbnailUri != null || contentUri != null)
                && (holder.bitmapSetToDefault() ||
                prevUri == null || !uri.equals(prevUri))) {
            // cancel/dispose any existing task and start a new one
            if (task != null) {
                task.cancel(true);
            }

            task = new ThumbnailLoadTask(
                    holder, width, height);
            task.execute(thumbnailUri, contentUri);
        } else if (thumbnailUri == null && contentUri == null) {
            // not an image, or no thumbnail exists. fall back to default.
            // async image load must separately ensure the default appears upon load failure.
            holder.setThumbnailToDefault();
        }
    }

    public ThumbnailLoadTask(AttachmentBitmapHolder holder, int width, int height) {
        mHolder = holder;
        mWidth = width;
        mHeight = height;
    }

    @Override
    protected Bitmap doInBackground(Uri... params) {
        Bitmap result = loadBitmap(params[0]);
        if (result == null) {
            result = loadBitmap(params[1]);
        }

        return result;
    }

    private Bitmap loadBitmap(final Uri thumbnailUri) {
        if (thumbnailUri == null) {
            LogUtils.e(LOG_TAG, "Attempting to load bitmap for null uri");
            return null;
        }

        final int orientation = getOrientation(thumbnailUri);

        AssetFileDescriptor fd = null;
        try {
            fd = mHolder.getResolver().openAssetFileDescriptor(thumbnailUri, "r");
            if (isCancelled() || fd == null) {
                return null;
            }

            final BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            opts.inDensity = DisplayMetrics.DENSITY_LOW;

            BitmapFactory.decodeFileDescriptor(fd.getFileDescriptor(), null, opts);
            if (isCancelled() || opts.outWidth == -1 || opts.outHeight == -1) {
                return null;
            }

            opts.inJustDecodeBounds = false;
            // Shrink both X and Y (but do not over-shrink)
            // and pick the least affected dimension to ensure the thumbnail is fillable
            // (i.e. ScaleType.CENTER_CROP)
            final int wDivider = Math.max(opts.outWidth / mWidth, 1);
            final int hDivider = Math.max(opts.outHeight / mHeight, 1);
            opts.inSampleSize = Math.min(wDivider, hDivider);

            LogUtils.d(LOG_TAG, "in background, src w/h=%d/%d dst w/h=%d/%d, divider=%d",
                    opts.outWidth, opts.outHeight, mWidth, mHeight, opts.inSampleSize);

            final Bitmap originalBitmap = BitmapFactory.decodeFileDescriptor(
                    fd.getFileDescriptor(), null, opts);
            if (originalBitmap != null && orientation != 0) {
                final Matrix matrix = new Matrix();
                matrix.postRotate(orientation);
                return Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.getWidth(),
                        originalBitmap.getHeight(), matrix, true);
            }
            return originalBitmap;
        } catch (Throwable t) {
            LogUtils.i(LOG_TAG, "Unable to decode thumbnail %s: %s %s", thumbnailUri,
                    t.getClass(), t.getMessage());
        } finally {
            if (fd != null) {
                try {
                    fd.close();
                } catch (IOException e) {
                    LogUtils.e(LOG_TAG, e, "");
                }
            }
        }

        return null;
    }

    private int getOrientation(final Uri thumbnailUri) {
        if (thumbnailUri == null) {
            return 0;
        }

        InputStream in = null;
        try {
            final ContentResolver resolver = mHolder.getResolver();
            in = resolver.openInputStream(thumbnailUri);
            return Exif.getOrientation(in, -1);
        } catch (Throwable t) {
            LogUtils.i(LOG_TAG, "Unable to get orientation of thumbnail %s: %s %s", thumbnailUri,
                    t.getClass(), t.getMessage());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    LogUtils.e(LOG_TAG, e, "error attemtping to close input stream");
                }
            }
        }

        return 0;
    }

    @Override
    protected void onPostExecute(Bitmap result) {
        if (result == null) {
            LogUtils.d(LOG_TAG, "back in UI thread, decode failed or file does not exist");
            mHolder.thumbnailLoadFailed();
            return;
        }

        LogUtils.d(LOG_TAG, "back in UI thread, decode success, w/h=%d/%d", result.getWidth(),
                result.getHeight());
        mHolder.setThumbnail(result);
    }

}
