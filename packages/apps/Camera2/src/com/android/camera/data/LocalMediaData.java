/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.camera.data;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.android.camera.ui.FilmStripView;
import com.android.camera.util.CameraUtil;
import com.android.camera.util.PhotoSphereHelper;
import com.android.camera2.R;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * A base class for all the local media files. The bitmap is loaded in
 * background thread. Subclasses should implement their own background loading
 * thread by sub-classing BitmapLoadTask and overriding doInBackground() to
 * return a bitmap.
 */
public abstract class LocalMediaData implements LocalData {
    protected final long mContentId;
    protected final String mTitle;
    protected final String mMimeType;
    protected final long mDateTakenInSeconds;
    protected final long mDateModifiedInSeconds;
    protected final String mPath;
    // width and height should be adjusted according to orientation.
    protected final int mWidth;
    protected final int mHeight;
    protected final long mSizeInBytes;
    protected final double mLatitude;
    protected final double mLongitude;

    /** The panorama metadata information of this media data. */
    protected PhotoSphereHelper.PanoramaMetadata mPanoramaMetadata;

    /** Used to load photo sphere metadata from image files. */
    protected PanoramaMetadataLoader mPanoramaMetadataLoader = null;

    /**
     * Used for thumbnail loading optimization. True if this data has a
     * corresponding visible view.
     */
    protected Boolean mUsing = false;

    public LocalMediaData (long contentId, String title, String mimeType,
            long dateTakenInSeconds, long dateModifiedInSeconds, String path,
            int width, int height, long sizeInBytes, double latitude,
            double longitude) {
        mContentId = contentId;
        mTitle = new String(title);
        mMimeType = new String(mimeType);
        mDateTakenInSeconds = dateTakenInSeconds;
        mDateModifiedInSeconds = dateModifiedInSeconds;
        mPath = new String(path);
        mWidth = width;
        mHeight = height;
        mSizeInBytes = sizeInBytes;
        mLatitude = latitude;
        mLongitude = longitude;
    }

    @Override
    public long getDateTaken() {
        return mDateTakenInSeconds;
    }

    @Override
    public long getDateModified() {
        return mDateModifiedInSeconds;
    }

    @Override
    public long getContentId() {
        return mContentId;
    }

    @Override
    public String getTitle() {
        return new String(mTitle);
    }

    @Override
    public int getWidth() {
        return mWidth;
    }

    @Override
    public int getHeight() {
        return mHeight;
    }

    @Override
    public int getOrientation() {
        return 0;
    }

    @Override
    public String getPath() {
        return mPath;
    }

    @Override
    public long getSizeInBytes() {
        return mSizeInBytes;
    }

    @Override
    public boolean isUIActionSupported(int action) {
        return false;
    }

    @Override
    public boolean isDataActionSupported(int action) {
        return false;
    }

    @Override
    public boolean delete(Context ctx) {
        File f = new File(mPath);
        return f.delete();
    }

    @Override
    public void viewPhotoSphere(PhotoSphereHelper.PanoramaViewHelper helper) {
        helper.showPanorama(getContentUri());
    }

    @Override
    public void isPhotoSphere(Context context, final PanoramaSupportCallback callback) {
        // If we already have metadata, use it.
        if (mPanoramaMetadata != null) {
            callback.panoramaInfoAvailable(mPanoramaMetadata.mUsePanoramaViewer,
                    mPanoramaMetadata.mIsPanorama360);
        }

        // Otherwise prepare a loader, if we don't have one already.
        if (mPanoramaMetadataLoader == null) {
            mPanoramaMetadataLoader = new PanoramaMetadataLoader(getContentUri());
        }

        // Load the metadata asynchronously.
        mPanoramaMetadataLoader.getPanoramaMetadata(context,
                new PanoramaMetadataLoader.PanoramaMetadataCallback() {
                    @Override
                    public void onPanoramaMetadataLoaded(PhotoSphereHelper.PanoramaMetadata metadata) {
                        // Store the metadata and remove the loader to free up
                        // space.
                        mPanoramaMetadata = metadata;
                        mPanoramaMetadataLoader = null;
                        callback.panoramaInfoAvailable(metadata.mUsePanoramaViewer,
                                metadata.mIsPanorama360);
                    }
                });
    }

    @Override
    public void onFullScreen(boolean fullScreen) {
        // do nothing.
    }

    @Override
    public boolean canSwipeInFullScreen() {
        return true;
    }

    protected ImageView fillImageView(Context ctx, ImageView v,
            int decodeWidth, int decodeHeight, Drawable placeHolder,
            LocalDataAdapter adapter) {
        v.setScaleType(ImageView.ScaleType.FIT_XY);
        v.setImageDrawable(placeHolder);

        BitmapLoadTask task = getBitmapLoadTask(v, decodeWidth, decodeHeight,
                ctx.getContentResolver(), adapter);
        task.execute();
        return v;
    }

    @Override
    public View getView(Activity activity,
            int decodeWidth, int decodeHeight, Drawable placeHolder,
            LocalDataAdapter adapter) {
        return fillImageView(activity, new ImageView(activity),
                decodeWidth, decodeHeight, placeHolder, adapter);
    }

    @Override
    public void prepare() {
        synchronized (mUsing) {
            mUsing = true;
        }
    }

    @Override
    public void recycle() {
        synchronized (mUsing) {
            mUsing = false;
        }
    }

    @Override
    public double[] getLatLong() {
        if (mLatitude == 0 && mLongitude == 0) {
            return null;
        }
        return new double[] {
                mLatitude, mLongitude
        };
    }

    protected boolean isUsing() {
        synchronized (mUsing) {
            return mUsing;
        }
    }

    @Override
    public String getMimeType() {
        return mMimeType;
    }

    @Override
    public MediaDetails getMediaDetails(Context context) {
        DateFormat dateFormatter = DateFormat.getDateTimeInstance();
        MediaDetails mediaDetails = new MediaDetails();
        mediaDetails.addDetail(MediaDetails.INDEX_TITLE, mTitle);
        mediaDetails.addDetail(MediaDetails.INDEX_WIDTH, mWidth);
        mediaDetails.addDetail(MediaDetails.INDEX_HEIGHT, mHeight);
        mediaDetails.addDetail(MediaDetails.INDEX_PATH, mPath);
        mediaDetails.addDetail(MediaDetails.INDEX_DATETIME,
                dateFormatter.format(new Date(mDateModifiedInSeconds * 1000)));
        if (mSizeInBytes > 0) {
            mediaDetails.addDetail(MediaDetails.INDEX_SIZE, mSizeInBytes);
        }
        if (mLatitude != 0 && mLongitude != 0) {
            String locationString = String.format(Locale.getDefault(), "%f, %f", mLatitude,
                    mLongitude);
            mediaDetails.addDetail(MediaDetails.INDEX_LOCATION, locationString);
        }
        return mediaDetails;
    }

    @Override
    public abstract int getViewType();

    protected abstract BitmapLoadTask getBitmapLoadTask(
            ImageView v, int decodeWidth, int decodeHeight,
            ContentResolver resolver, LocalDataAdapter adapter);

    public static final class PhotoData extends LocalMediaData {
        private static final String TAG = "CAM_PhotoData";

        public static final int COL_ID = 0;
        public static final int COL_TITLE = 1;
        public static final int COL_MIME_TYPE = 2;
        public static final int COL_DATE_TAKEN = 3;
        public static final int COL_DATE_MODIFIED = 4;
        public static final int COL_DATA = 5;
        public static final int COL_ORIENTATION = 6;
        public static final int COL_WIDTH = 7;
        public static final int COL_HEIGHT = 8;
        public static final int COL_SIZE = 9;
        public static final int COL_LATITUDE = 10;
        public static final int COL_LONGITUDE = 11;

        static final Uri CONTENT_URI = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        static final String QUERY_ORDER = MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC, "
                + MediaStore.Images.ImageColumns._ID + " DESC";
        /**
         * These values should be kept in sync with column IDs (COL_*) above.
         */
        static final String[] QUERY_PROJECTION = {
                MediaStore.Images.ImageColumns._ID,           // 0, int
                MediaStore.Images.ImageColumns.TITLE,         // 1, string
                MediaStore.Images.ImageColumns.MIME_TYPE,     // 2, string
                MediaStore.Images.ImageColumns.DATE_TAKEN,    // 3, int
                MediaStore.Images.ImageColumns.DATE_MODIFIED, // 4, int
                MediaStore.Images.ImageColumns.DATA,          // 5, string
                MediaStore.Images.ImageColumns.ORIENTATION,   // 6, int, 0, 90, 180, 270
                MediaStore.Images.ImageColumns.WIDTH,         // 7, int
                MediaStore.Images.ImageColumns.HEIGHT,        // 8, int
                MediaStore.Images.ImageColumns.SIZE,          // 9, long
                MediaStore.Images.ImageColumns.LATITUDE,      // 10, double
                MediaStore.Images.ImageColumns.LONGITUDE      // 11, double
        };

        private static final int mSupportedUIActions =
                FilmStripView.ImageData.ACTION_DEMOTE
                        | FilmStripView.ImageData.ACTION_PROMOTE
                        | FilmStripView.ImageData.ACTION_ZOOM;
        private static final int mSupportedDataActions =
                LocalData.ACTION_DELETE;

        /** 32K buffer. */
        private static final byte[] DECODE_TEMP_STORAGE = new byte[32 * 1024];

        /** from MediaStore, can only be 0, 90, 180, 270 */
        private final int mOrientation;

        public PhotoData(long id, String title, String mimeType,
                long dateTakenInSeconds, long dateModifiedInSeconds,
                String path, int orientation, int width, int height,
                long sizeInBytes, double latitude, double longitude) {
            super(id, title, mimeType, dateTakenInSeconds, dateModifiedInSeconds,
                    path, width, height, sizeInBytes, latitude, longitude);
            mOrientation = orientation;
        }

        static PhotoData buildFromCursor(Cursor c) {
            long id = c.getLong(COL_ID);
            String title = c.getString(COL_TITLE);
            String mimeType = c.getString(COL_MIME_TYPE);
            long dateTakenInSeconds = c.getLong(COL_DATE_TAKEN);
            long dateModifiedInSeconds = c.getLong(COL_DATE_MODIFIED);
            String path = c.getString(COL_DATA);
            int orientation = c.getInt(COL_ORIENTATION);
            int width = c.getInt(COL_WIDTH);
            int height = c.getInt(COL_HEIGHT);
            if (width <= 0 || height <= 0) {
                Log.w(TAG, "Zero dimension in ContentResolver for "
                        + path + ":" + width + "x" + height);
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(path, opts);
                if (opts.outWidth > 0 && opts.outHeight > 0) {
                    width = opts.outWidth;
                    height = opts.outHeight;
                } else {
                    Log.w(TAG, "Dimension decode failed for " + path);
                    Bitmap b = BitmapFactory.decodeFile(path);
                    if (b == null) {
                        Log.w(TAG, "PhotoData skipped."
                                + " Decoding " + path + "failed.");
                        return null;
                    }
                    width = b.getWidth();
                    height = b.getHeight();
                    if (width == 0 || height == 0) {
                        Log.w(TAG, "PhotoData skipped. Bitmap size 0 for " + path);
                        return null;
                    }
                }
            }

            long sizeInBytes = c.getLong(COL_SIZE);
            double latitude = c.getDouble(COL_LATITUDE);
            double longitude = c.getDouble(COL_LONGITUDE);
            PhotoData result = new PhotoData(id, title, mimeType, dateTakenInSeconds,
                    dateModifiedInSeconds, path, orientation, width, height,
                    sizeInBytes, latitude, longitude);
            return result;
        }

        @Override
        public int getOrientation() {
            return mOrientation;
        }

        @Override
        public String toString() {
            return "Photo:" + ",data=" + mPath + ",mimeType=" + mMimeType
                    + "," + mWidth + "x" + mHeight + ",orientation=" + mOrientation
                    + ",date=" + new Date(mDateTakenInSeconds);
        }

        @Override
        public int getViewType() {
            return VIEW_TYPE_REMOVABLE;
        }

        @Override
        public boolean isUIActionSupported(int action) {
            return ((action & mSupportedUIActions) == action);
        }

        @Override
        public boolean isDataActionSupported(int action) {
            return ((action & mSupportedDataActions) == action);
        }

        @Override
        public boolean delete(Context c) {
            ContentResolver cr = c.getContentResolver();
            cr.delete(CONTENT_URI, MediaStore.Images.ImageColumns._ID + "=" + mContentId, null);
            return super.delete(c);
        }

        @Override
        public Uri getContentUri() {
            Uri baseUri = CONTENT_URI;
            return baseUri.buildUpon().appendPath(String.valueOf(mContentId)).build();
        }

        @Override
        public MediaDetails getMediaDetails(Context context) {
            MediaDetails mediaDetails = super.getMediaDetails(context);
            MediaDetails.extractExifInfo(mediaDetails, mPath);
            mediaDetails.addDetail(MediaDetails.INDEX_ORIENTATION, mOrientation);
            return mediaDetails;
        }

        @Override
        public int getLocalDataType() {
            if (mPanoramaMetadata != null) {
                if (mPanoramaMetadata.mIsPanorama360) {
                    return LOCAL_360_PHOTO_SPHERE;
                } else if (mPanoramaMetadata.mUsePanoramaViewer) {
                    return LOCAL_PHOTO_SPHERE;
                }
            }
            return LOCAL_IMAGE;
        }

        @Override
        public LocalData refresh(ContentResolver resolver) {
            Cursor c = resolver.query(
                    getContentUri(), QUERY_PROJECTION, null, null, null);
            if (c == null || !c.moveToFirst()) {
                return null;
            }
            PhotoData newData = buildFromCursor(c);
            return newData;
        }

        @Override
        public boolean isPhoto() {
            return true;
        }

        @Override
        protected BitmapLoadTask getBitmapLoadTask(
                ImageView v, int decodeWidth, int decodeHeight,
                ContentResolver resolver, LocalDataAdapter adapter) {
            return new PhotoBitmapLoadTask(v, decodeWidth, decodeHeight,
                    resolver, adapter);
        }

        private final class PhotoBitmapLoadTask extends BitmapLoadTask {
            private final int mDecodeWidth;
            private final int mDecodeHeight;
            private final ContentResolver mResolver;
            private final LocalDataAdapter mAdapter;

            private boolean mNeedsRefresh;

            public PhotoBitmapLoadTask(ImageView v, int decodeWidth,
                    int decodeHeight, ContentResolver resolver,
                    LocalDataAdapter adapter) {
                super(v);
                mDecodeWidth = decodeWidth;
                mDecodeHeight = decodeHeight;
                mResolver = resolver;
                mAdapter = adapter;
            }

            @Override
            protected Bitmap doInBackground(Void... v) {
                int sampleSize = 1;
                if (mWidth > mDecodeWidth || mHeight > mDecodeHeight) {
                    int heightRatio = Math.round((float) mHeight / (float) mDecodeHeight);
                    int widthRatio = Math.round((float) mWidth / (float) mDecodeWidth);
                    sampleSize = Math.max(heightRatio, widthRatio);
                }

                // For correctness, we need to double check the size here. The
                // good news is that decoding bounds take much less time than
                // decoding samples like < 1%.
                // TODO: better organize the decoding and sampling by using a
                // image cache.
                int decodedWidth = 0;
                int decodedHeight = 0;
                BitmapFactory.Options justBoundsOpts = new BitmapFactory.Options();
                justBoundsOpts.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(mPath, justBoundsOpts);
                if (justBoundsOpts.outWidth > 0 && justBoundsOpts.outHeight > 0) {
                    decodedWidth = justBoundsOpts.outWidth;
                    decodedHeight = justBoundsOpts.outHeight;
                }

                // If the width and height is valid and not matching the values
                // from MediaStore, then update the MediaStore. This only
                // happened when the MediaStore had been told a wrong data.
                if (decodedWidth > 0 && decodedHeight > 0 &&
                        (decodedWidth != mWidth || decodedHeight != mHeight)) {
                    ContentValues values = new ContentValues();
                    values.put(Images.Media.WIDTH, decodedWidth);
                    values.put(Images.Media.HEIGHT, decodedHeight);
                    mResolver.update(getContentUri(), values, null, null);
                    mNeedsRefresh = true;
                    Log.w(TAG, "Uri " + getContentUri() + " has been updated with" +
                            " correct size!");
                    return null;
                }

                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inSampleSize = sampleSize;
                opts.inTempStorage = DECODE_TEMP_STORAGE;
                if (isCancelled() || !isUsing()) {
                    return null;
                }
                Bitmap b = BitmapFactory.decodeFile(mPath, opts);

                if (mOrientation != 0 && b != null) {
                    if (isCancelled() || !isUsing()) {
                        return null;
                    }
                    Matrix m = new Matrix();
                    m.setRotate(mOrientation);
                    b = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), m, false);
                }
                return b;
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                super.onPostExecute(bitmap);
                if (mNeedsRefresh && mAdapter != null) {
                    mAdapter.refresh(mResolver, getContentUri());
                }
            }
        }

        @Override
        public boolean rotate90Degrees(Context context, LocalDataAdapter adapter,
                int currentDataId, boolean clockwise) {
            RotationTask task = new RotationTask(context, adapter,
                    currentDataId, clockwise);
            task.execute(this);
            return true;
        }
    }

    public static final class VideoData extends LocalMediaData {
        public static final int COL_ID = 0;
        public static final int COL_TITLE = 1;
        public static final int COL_MIME_TYPE = 2;
        public static final int COL_DATE_TAKEN = 3;
        public static final int COL_DATE_MODIFIED = 4;
        public static final int COL_DATA = 5;
        public static final int COL_WIDTH = 6;
        public static final int COL_HEIGHT = 7;
        public static final int COL_RESOLUTION = 8;
        public static final int COL_SIZE = 9;
        public static final int COL_LATITUDE = 10;
        public static final int COL_LONGITUDE = 11;
        public static final int COL_DURATION = 12;

        static final Uri CONTENT_URI = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;

        private static final int mSupportedUIActions =
                FilmStripView.ImageData.ACTION_DEMOTE
                        | FilmStripView.ImageData.ACTION_PROMOTE;
        private static final int mSupportedDataActions =
                LocalData.ACTION_DELETE
                        | LocalData.ACTION_PLAY;

        static final String QUERY_ORDER = MediaStore.Video.VideoColumns.DATE_TAKEN + " DESC, "
                + MediaStore.Video.VideoColumns._ID + " DESC";
        /**
         * These values should be kept in sync with column IDs (COL_*) above.
         */
        static final String[] QUERY_PROJECTION = {
                MediaStore.Video.VideoColumns._ID,           // 0, int
                MediaStore.Video.VideoColumns.TITLE,         // 1, string
                MediaStore.Video.VideoColumns.MIME_TYPE,     // 2, string
                MediaStore.Video.VideoColumns.DATE_TAKEN,    // 3, int
                MediaStore.Video.VideoColumns.DATE_MODIFIED, // 4, int
                MediaStore.Video.VideoColumns.DATA,          // 5, string
                MediaStore.Video.VideoColumns.WIDTH,         // 6, int
                MediaStore.Video.VideoColumns.HEIGHT,        // 7, int
                MediaStore.Video.VideoColumns.RESOLUTION,    // 8 string
                MediaStore.Video.VideoColumns.SIZE,          // 9 long
                MediaStore.Video.VideoColumns.LATITUDE,      // 10 double
                MediaStore.Video.VideoColumns.LONGITUDE,     // 11 double
                MediaStore.Video.VideoColumns.DURATION       // 12 long
        };

        /** The duration in milliseconds. */
        private long mDurationInSeconds;

        public VideoData(long id, String title, String mimeType,
                long dateTakenInSeconds, long dateModifiedInSeconds,
                String path, int width, int height, long sizeInBytes,
                double latitude, double longitude, long durationInSeconds) {
            super(id, title, mimeType, dateTakenInSeconds, dateModifiedInSeconds,
                    path, width, height, sizeInBytes, latitude, longitude);
            mDurationInSeconds = durationInSeconds;
        }

        static VideoData buildFromCursor(Cursor c) {
            long id = c.getLong(COL_ID);
            String title = c.getString(COL_TITLE);
            String mimeType = c.getString(COL_MIME_TYPE);
            long dateTakenInSeconds = c.getLong(COL_DATE_TAKEN);
            long dateModifiedInSeconds = c.getLong(COL_DATE_MODIFIED);
            String path = c.getString(COL_DATA);
            int width = c.getInt(COL_WIDTH);
            int height = c.getInt(COL_HEIGHT);
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            String rotation = null;
            try {
                retriever.setDataSource(path);
            } catch (RuntimeException ex) {
                // setDataSource() can cause RuntimeException beyond
                // IllegalArgumentException. e.g: data contain *.avi file.
                retriever.release();
                Log.e(TAG, "MediaMetadataRetriever.setDataSource() fail:"
                        + ex.getMessage());
                return null;
            }
            rotation = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);

            // Extracts video height/width if available. If unavailable, set to 0.
            if (width == 0 || height == 0) {
                String val = retriever.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
                width = (val == null) ? 0 : Integer.parseInt(val);
                val = retriever.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
                height = (val == null) ? 0 : Integer.parseInt(val);
            }
            retriever.release();
            if (width == 0 || height == 0) {
                // Width or height is still not available.
                Log.e(TAG, "Unable to retrieve dimension of video:" + path);
                return null;
            }
            if (rotation != null
                    && (rotation.equals("90") || rotation.equals("270"))) {
                int b = width;
                width = height;
                height = b;
            }

            long sizeInBytes = c.getLong(COL_SIZE);
            double latitude = c.getDouble(COL_LATITUDE);
            double longitude = c.getDouble(COL_LONGITUDE);
            long durationInSeconds = c.getLong(COL_DURATION) / 1000;
            VideoData d = new VideoData(id, title, mimeType, dateTakenInSeconds,
                    dateModifiedInSeconds, path, width, height, sizeInBytes,
                    latitude, longitude, durationInSeconds);
            return d;
        }

        @Override
        public String toString() {
            return "Video:" + ",data=" + mPath + ",mimeType=" + mMimeType
                    + "," + mWidth + "x" + mHeight + ",date=" + new Date(mDateTakenInSeconds);
        }

        @Override
        public int getViewType() {
            return VIEW_TYPE_REMOVABLE;
        }

        @Override
        public boolean isUIActionSupported(int action) {
            return ((action & mSupportedUIActions) == action);
        }

        @Override
        public boolean isDataActionSupported(int action) {
            return ((action & mSupportedDataActions) == action);
        }

        @Override
        public boolean delete(Context ctx) {
            ContentResolver cr = ctx.getContentResolver();
            cr.delete(CONTENT_URI, MediaStore.Video.VideoColumns._ID + "=" + mContentId, null);
            return super.delete(ctx);
        }

        @Override
        public Uri getContentUri() {
            Uri baseUri = CONTENT_URI;
            return baseUri.buildUpon().appendPath(String.valueOf(mContentId)).build();
        }

        @Override
        public MediaDetails getMediaDetails(Context context) {
            MediaDetails mediaDetails = super.getMediaDetails(context);
            String duration = MediaDetails.formatDuration(context, mDurationInSeconds);
            mediaDetails.addDetail(MediaDetails.INDEX_DURATION, duration);
            return mediaDetails;
        }

        @Override
        public int getLocalDataType() {
            return LOCAL_VIDEO;
        }

        @Override
        public LocalData refresh(ContentResolver resolver) {
            Cursor c = resolver.query(
                    getContentUri(), QUERY_PROJECTION, null, null, null);
            if (c == null || !c.moveToFirst()) {
                return null;
            }
            VideoData newData = buildFromCursor(c);
            return newData;
        }

        @Override
        public View getView(final Activity activity,
                int decodeWidth, int decodeHeight, Drawable placeHolder,
                LocalDataAdapter adapter) {

            // ImageView for the bitmap.
            ImageView iv = new ImageView(activity);
            iv.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER));
            fillImageView(activity, iv, decodeWidth, decodeHeight, placeHolder,
                    adapter);

            // ImageView for the play icon.
            ImageView icon = new ImageView(activity);
            icon.setImageResource(R.drawable.ic_control_play);
            icon.setScaleType(ImageView.ScaleType.CENTER);
            icon.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
            icon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CameraUtil.playVideo(activity, getContentUri(), mTitle);
                }
            });

            FrameLayout f = new FrameLayout(activity);
            f.addView(iv);
            f.addView(icon);
            return f;
        }

        @Override
        public boolean isPhoto() {
            return false;
        }

        @Override
        protected BitmapLoadTask getBitmapLoadTask(
                ImageView v, int decodeWidth, int decodeHeight,
                ContentResolver resolver, LocalDataAdapter adapter) {
            return new VideoBitmapLoadTask(v);
        }

        private final class VideoBitmapLoadTask extends BitmapLoadTask {

            public VideoBitmapLoadTask(ImageView v) {
                super(v);
            }

            @Override
            protected Bitmap doInBackground(Void... v) {
                if (isCancelled() || !isUsing()) {
                    return null;
                }
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                Bitmap bitmap = null;
                try {
                    retriever.setDataSource(mPath);
                    byte[] data = retriever.getEmbeddedPicture();
                    if (!isCancelled() && isUsing()) {
                        if (data != null) {
                            bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                        }
                        if (bitmap == null) {
                            bitmap = retriever.getFrameAtTime();
                        }
                    }
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "MediaMetadataRetriever.setDataSource() fail:"
                            + e.getMessage());
                }
                retriever.release();
                return bitmap;
            }
        }

        @Override
        public boolean rotate90Degrees(Context context, LocalDataAdapter adapter,
                int currentDataId, boolean clockwise) {
            // We don't support rotation for video data.
            Log.e(TAG, "Unexpected call in rotate90Degrees()");
            return false;
        }
    }

    /**
     * An {@link AsyncTask} class that loads the bitmap in the background
     * thread. Sub-classes should implement their own
     * {@code BitmapLoadTask#doInBackground(Void...)}."
     */
    protected abstract class BitmapLoadTask extends AsyncTask<Void, Void, Bitmap> {
        protected ImageView mView;

        protected BitmapLoadTask(ImageView v) {
            mView = v;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (!isUsing()) {
                return;
            }
            if (bitmap == null) {
                Log.e(TAG, "Failed decoding bitmap for file:" + mPath);
                return;
            }
            BitmapDrawable d = new BitmapDrawable(bitmap);
            mView.setScaleType(ImageView.ScaleType.FIT_XY);
            mView.setImageDrawable(d);
        }
    }
}
