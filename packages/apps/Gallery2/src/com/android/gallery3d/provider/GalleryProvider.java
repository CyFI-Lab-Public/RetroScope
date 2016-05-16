/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.gallery3d.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore.Images.ImageColumns;
import android.util.Log;

import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.common.AsyncTaskUtil;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.picasasource.PicasaSource;
import com.android.gallery3d.util.GalleryUtils;

import java.io.FileNotFoundException;
import java.io.IOException;

public class GalleryProvider extends ContentProvider {
    private static final String TAG = "GalleryProvider";

    public static final String AUTHORITY = "com.android.gallery3d.provider";
    public static final Uri BASE_URI = Uri.parse("content://" + AUTHORITY);

    public static interface PicasaColumns {
        public static final String USER_ACCOUNT = "user_account";
        public static final String PICASA_ID = "picasa_id";
    }

    private static final String[] SUPPORTED_PICASA_COLUMNS = {
            PicasaColumns.USER_ACCOUNT,
            PicasaColumns.PICASA_ID,
            ImageColumns.DISPLAY_NAME,
            ImageColumns.SIZE,
            ImageColumns.MIME_TYPE,
            ImageColumns.DATE_TAKEN,
            ImageColumns.LATITUDE,
            ImageColumns.LONGITUDE,
            ImageColumns.ORIENTATION};

    private DataManager mDataManager;
    private static Uri sBaseUri;

    public static String getAuthority(Context context) {
        return context.getPackageName() + ".provider";
    }

    public static Uri getUriFor(Context context, Path path) {
        if (sBaseUri == null) {
            sBaseUri = Uri.parse("content://" + context.getPackageName() + ".provider");
        }
        return sBaseUri.buildUpon()
                .appendEncodedPath(path.toString().substring(1)) // ignore the leading '/'
                .build();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    // TODO: consider concurrent access
    @Override
    public String getType(Uri uri) {
        long token = Binder.clearCallingIdentity();
        try {
            Path path = Path.fromString(uri.getPath());
            MediaItem item = (MediaItem) mDataManager.getMediaObject(path);
            return item != null ? item.getMimeType() : null;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean onCreate() {
        GalleryApp app = (GalleryApp) getContext().getApplicationContext();
        mDataManager = app.getDataManager();
        return true;
    }

    // TODO: consider concurrent access
    @Override
    public Cursor query(Uri uri, String[] projection,
            String selection, String[] selectionArgs, String sortOrder) {
        long token = Binder.clearCallingIdentity();
        try {
            Path path = Path.fromString(uri.getPath());
            MediaObject object = mDataManager.getMediaObject(path);
            if (object == null) {
                Log.w(TAG, "cannot find: " + uri);
                return null;
            }
            if (PicasaSource.isPicasaImage(object)) {
                return queryPicasaItem(object,
                        projection, selection, selectionArgs, sortOrder);
            } else {
                    return null;
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private Cursor queryPicasaItem(MediaObject image, String[] projection,
            String selection, String[] selectionArgs, String sortOrder) {
        if (projection == null) projection = SUPPORTED_PICASA_COLUMNS;
        Object[] columnValues = new Object[projection.length];
        double latitude = PicasaSource.getLatitude(image);
        double longitude = PicasaSource.getLongitude(image);
        boolean isValidLatlong = GalleryUtils.isValidLocation(latitude, longitude);

        for (int i = 0, n = projection.length; i < n; ++i) {
            String column = projection[i];
            if (PicasaColumns.USER_ACCOUNT.equals(column)) {
                columnValues[i] = PicasaSource.getUserAccount(getContext(), image);
            } else if (PicasaColumns.PICASA_ID.equals(column)) {
                columnValues[i] = PicasaSource.getPicasaId(image);
            } else if (ImageColumns.DISPLAY_NAME.equals(column)) {
                columnValues[i] = PicasaSource.getImageTitle(image);
            } else if (ImageColumns.SIZE.equals(column)){
                columnValues[i] = PicasaSource.getImageSize(image);
            } else if (ImageColumns.MIME_TYPE.equals(column)) {
                columnValues[i] = PicasaSource.getContentType(image);
            } else if (ImageColumns.DATE_TAKEN.equals(column)) {
                columnValues[i] = PicasaSource.getDateTaken(image);
            } else if (ImageColumns.LATITUDE.equals(column)) {
                columnValues[i] = isValidLatlong ? latitude : null;
            } else if (ImageColumns.LONGITUDE.equals(column)) {
                columnValues[i] = isValidLatlong ? longitude : null;
            } else if (ImageColumns.ORIENTATION.equals(column)) {
                columnValues[i] = PicasaSource.getRotation(image);
            } else {
                Log.w(TAG, "unsupported column: " + column);
            }
        }
        MatrixCursor cursor = new MatrixCursor(projection);
        cursor.addRow(columnValues);
        return cursor;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode)
            throws FileNotFoundException {
        long token = Binder.clearCallingIdentity();
        try {
            if (mode.contains("w")) {
                throw new FileNotFoundException("cannot open file for write");
            }
            Path path = Path.fromString(uri.getPath());
            MediaObject object = mDataManager.getMediaObject(path);
            if (object == null) {
                throw new FileNotFoundException(uri.toString());
            }
            if (PicasaSource.isPicasaImage(object)) {
                return PicasaSource.openFile(getContext(), object, mode);
            } else {
                throw new FileNotFoundException("unspported type: " + object);
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    private static interface PipeDataWriter<T> {
        void writeDataToPipe(ParcelFileDescriptor output, T args);
    }

    // Modified from ContentProvider.openPipeHelper. We are target at API LEVEL 10.
    // But openPipeHelper is available in API LEVEL 11.
    private static <T> ParcelFileDescriptor openPipeHelper(
            final T args, final PipeDataWriter<T> func) throws FileNotFoundException {
        try {
            final ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
            AsyncTask<Object, Object, Object> task = new AsyncTask<Object, Object, Object>() {
                @Override
                protected Object doInBackground(Object... params) {
                    try {
                        func.writeDataToPipe(pipe[1], args);
                        return null;
                    } finally {
                        Utils.closeSilently(pipe[1]);
                    }
                }
            };
            AsyncTaskUtil.executeInParallel(task, (Object[]) null);
            return pipe[0];
        } catch (IOException e) {
            throw new FileNotFoundException("failure making pipe");
        }
    }

}
