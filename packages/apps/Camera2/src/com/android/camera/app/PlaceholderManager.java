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

package com.android.camera.app;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;

import com.android.camera.ImageTaskManager;
import com.android.camera.Storage;
import com.android.camera.exif.ExifInterface;
import com.android.camera.util.CameraUtil;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;

public class PlaceholderManager implements ImageTaskManager {
    private static final String TAG = "PlaceholderManager";

    public static final String PLACEHOLDER_MIME_TYPE = "application/placeholder-image";
    private final Context mContext;

    final private ArrayList<WeakReference<TaskListener>> mListenerRefs;

    public static class Session {
        String outputTitle;
        Uri outputUri;
        long time;

        Session(String title, Uri uri, long timestamp) {
            outputTitle = title;
            outputUri = uri;
            time = timestamp;
        }
    }

    public PlaceholderManager(Context context) {
        mContext = context;
        mListenerRefs = new ArrayList<WeakReference<TaskListener>>();
    }

    @Override
    public void addTaskListener(TaskListener l) {
        synchronized (mListenerRefs) {
            if (findTaskListener(l) == -1) {
                mListenerRefs.add(new WeakReference<TaskListener>(l));
            }
        }
    }

    @Override
    public void removeTaskListener(TaskListener l) {
        synchronized (mListenerRefs) {
            int i = findTaskListener(l);
            if (i != -1) {
                mListenerRefs.remove(i);
            }
        }
    }

    @Override
    public int getTaskProgress(Uri uri) {
        return 0;
    }

    private int findTaskListener(TaskListener listener) {
        int index = -1;
        for (int i = 0; i < mListenerRefs.size(); i++) {
            TaskListener l = mListenerRefs.get(i).get();
            if (l != null && l == listener) {
                index = i;
                break;
            }
        }
        return index;
    }

    private Iterable<TaskListener> getListeners() {
        return new Iterable<TaskListener>() {
            @Override
            public Iterator<TaskListener> iterator() {
                return new ListenerIterator();
            }
        };
    }

    private class ListenerIterator implements Iterator<TaskListener> {
        private int mIndex = 0;
        private TaskListener mNext = null;

        @Override
        public boolean hasNext() {
            while (mNext == null && mIndex < mListenerRefs.size()) {
                mNext = mListenerRefs.get(mIndex).get();
                if (mNext == null) {
                    mListenerRefs.remove(mIndex);
                }
            }
            return mNext != null;
        }

        @Override
        public TaskListener next() {
            hasNext(); // Populates mNext
            mIndex++;
            TaskListener next = mNext;
            mNext = null;
            return next;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public Session insertPlaceholder(String title, byte[] placeholder, long timestamp) {
        if (title == null || placeholder == null) {
            throw new IllegalArgumentException("Null argument passed to insertPlaceholder");
        }

        // Decode bounds
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(placeholder, 0, placeholder.length, options);
        int width = options.outWidth;
        int height = options.outHeight;

        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Image had bad height/width");
        }

        Uri uri =
                Storage.addImage(mContext.getContentResolver(), title, timestamp, null, 0, null,
                        placeholder, width, height, PLACEHOLDER_MIME_TYPE);

        if (uri == null) {
            return null;
        }

        String filePath = uri.getPath();
        synchronized (mListenerRefs) {
            for (TaskListener l : getListeners()) {
                l.onTaskQueued(filePath, uri);
            }
        }

        return new Session(title, uri, timestamp);
    }

    public void replacePlaceholder(Session session, Location location, int orientation,
            ExifInterface exif, byte[] jpeg, int width, int height, String mimeType) {

        Storage.updateImage(session.outputUri, mContext.getContentResolver(), session.outputTitle,
                session.time, location, orientation, exif, jpeg, width, height, mimeType);

        synchronized (mListenerRefs) {
            for (TaskListener l : getListeners()) {
                l.onTaskDone(session.outputUri.getPath(), session.outputUri);
            }
        }
        CameraUtil.broadcastNewPicture(mContext, session.outputUri);
    }

    public void removePlaceholder(Session session) {
        Storage.deleteImage(mContext.getContentResolver(), session.outputUri);
    }

}
