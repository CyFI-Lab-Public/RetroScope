/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.phone;

import android.app.Notification;
import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.ContactsContract.Contacts;
import android.util.Log;

import com.android.internal.telephony.CallerInfo;
import com.android.internal.telephony.Connection;

import java.io.IOException;
import java.io.InputStream;

/**
 * Helper class for loading contacts photo asynchronously.
 */
public class ContactsAsyncHelper {

    private static final boolean DBG = false;
    private static final String LOG_TAG = "ContactsAsyncHelper";

    /**
     * Interface for a WorkerHandler result return.
     */
    public interface OnImageLoadCompleteListener {
        /**
         * Called when the image load is complete.
         *
         * @param token Integer passed in {@link ContactsAsyncHelper#startObtainPhotoAsync(int,
         * Context, Uri, OnImageLoadCompleteListener, Object)}.
         * @param photo Drawable object obtained by the async load.
         * @param photoIcon Bitmap object obtained by the async load.
         * @param cookie Object passed in {@link ContactsAsyncHelper#startObtainPhotoAsync(int,
         * Context, Uri, OnImageLoadCompleteListener, Object)}. Can be null iff. the original
         * cookie is null.
         */
        public void onImageLoadComplete(int token, Drawable photo, Bitmap photoIcon,
                Object cookie);
    }

    // constants
    private static final int EVENT_LOAD_IMAGE = 1;

    private final Handler mResultHandler = new Handler() {
        /** Called when loading is done. */
        @Override
        public void handleMessage(Message msg) {
            WorkerArgs args = (WorkerArgs) msg.obj;
            switch (msg.arg1) {
                case EVENT_LOAD_IMAGE:
                    if (args.listener != null) {
                        if (DBG) {
                            Log.d(LOG_TAG, "Notifying listener: " + args.listener.toString() +
                                    " image: " + args.uri + " completed");
                        }
                        args.listener.onImageLoadComplete(msg.what, args.photo, args.photoIcon,
                                args.cookie);
                    }
                    break;
                default:
            }
        }
    };

    /** Handler run on a worker thread to load photo asynchronously. */
    private static Handler sThreadHandler;

    /** For forcing the system to call its constructor */
    @SuppressWarnings("unused")
    private static ContactsAsyncHelper sInstance;

    static {
        sInstance = new ContactsAsyncHelper();
    }

    private static final class WorkerArgs {
        public Context context;
        public Uri uri;
        public Drawable photo;
        public Bitmap photoIcon;
        public Object cookie;
        public OnImageLoadCompleteListener listener;
    }

    /**
     * public inner class to help out the ContactsAsyncHelper callers
     * with tracking the state of the CallerInfo Queries and image
     * loading.
     *
     * Logic contained herein is used to remove the race conditions
     * that exist as the CallerInfo queries run and mix with the image
     * loads, which then mix with the Phone state changes.
     */
    public static class ImageTracker {

        // Image display states
        public static final int DISPLAY_UNDEFINED = 0;
        public static final int DISPLAY_IMAGE = -1;
        public static final int DISPLAY_DEFAULT = -2;

        // State of the image on the imageview.
        private CallerInfo mCurrentCallerInfo;
        private int displayMode;

        public ImageTracker() {
            mCurrentCallerInfo = null;
            displayMode = DISPLAY_UNDEFINED;
        }

        /**
         * Used to see if the requested call / connection has a
         * different caller attached to it than the one we currently
         * have in the CallCard.
         */
        public boolean isDifferentImageRequest(CallerInfo ci) {
            // note, since the connections are around for the lifetime of the
            // call, and the CallerInfo-related items as well, we can
            // definitely use a simple != comparison.
            return (mCurrentCallerInfo != ci);
        }

        public boolean isDifferentImageRequest(Connection connection) {
            // if the connection does not exist, see if the
            // mCurrentCallerInfo is also null to match.
            if (connection == null) {
                if (DBG) Log.d(LOG_TAG, "isDifferentImageRequest: connection is null");
                return (mCurrentCallerInfo != null);
            }
            Object o = connection.getUserData();

            // if the call does NOT have a callerInfo attached
            // then it is ok to query.
            boolean runQuery = true;
            if (o instanceof CallerInfo) {
                runQuery = isDifferentImageRequest((CallerInfo) o);
            }
            return runQuery;
        }

        /**
         * Simple setter for the CallerInfo object.
         */
        public void setPhotoRequest(CallerInfo ci) {
            mCurrentCallerInfo = ci;
        }

        /**
         * Convenience method used to retrieve the URI
         * representing the Photo file recorded in the attached
         * CallerInfo Object.
         */
        public Uri getPhotoUri() {
            if (mCurrentCallerInfo != null) {
                return ContentUris.withAppendedId(Contacts.CONTENT_URI,
                        mCurrentCallerInfo.person_id);
            }
            return null;
        }

        /**
         * Simple setter for the Photo state.
         */
        public void setPhotoState(int state) {
            displayMode = state;
        }

        /**
         * Simple getter for the Photo state.
         */
        public int getPhotoState() {
            return displayMode;
        }
    }

    /**
     * Thread worker class that handles the task of opening the stream and loading
     * the images.
     */
    private class WorkerHandler extends Handler {
        public WorkerHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            WorkerArgs args = (WorkerArgs) msg.obj;

            switch (msg.arg1) {
                case EVENT_LOAD_IMAGE:
                    InputStream inputStream = null;
                    try {
                        try {
                            inputStream = Contacts.openContactPhotoInputStream(
                                    args.context.getContentResolver(), args.uri, true);
                        } catch (Exception e) {
                            Log.e(LOG_TAG, "Error opening photo input stream", e);
                        }

                        if (inputStream != null) {
                            args.photo = Drawable.createFromStream(inputStream,
                                    args.uri.toString());

                            // This assumes Drawable coming from contact database is usually
                            // BitmapDrawable and thus we can have (down)scaled version of it.
                            args.photoIcon = getPhotoIconWhenAppropriate(args.context, args.photo);

                            if (DBG) {
                                Log.d(LOG_TAG, "Loading image: " + msg.arg1 +
                                        " token: " + msg.what + " image URI: " + args.uri);
                            }
                        } else {
                            args.photo = null;
                            args.photoIcon = null;
                            if (DBG) {
                                Log.d(LOG_TAG, "Problem with image: " + msg.arg1 +
                                        " token: " + msg.what + " image URI: " + args.uri +
                                        ", using default image.");
                            }
                        }
                    } finally {
                        if (inputStream != null) {
                            try {
                                inputStream.close();
                            } catch (IOException e) {
                                Log.e(LOG_TAG, "Unable to close input stream.", e);
                            }
                        }
                    }
                    break;
                default:
            }

            // send the reply to the enclosing class.
            Message reply = ContactsAsyncHelper.this.mResultHandler.obtainMessage(msg.what);
            reply.arg1 = msg.arg1;
            reply.obj = msg.obj;
            reply.sendToTarget();
        }

        /**
         * Returns a Bitmap object suitable for {@link Notification}'s large icon. This might
         * return null when the given Drawable isn't BitmapDrawable, or if the system fails to
         * create a scaled Bitmap for the Drawable.
         */
        private Bitmap getPhotoIconWhenAppropriate(Context context, Drawable photo) {
            if (!(photo instanceof BitmapDrawable)) {
                return null;
            }
            int iconSize = context.getResources()
                    .getDimensionPixelSize(R.dimen.notification_icon_size);
            Bitmap orgBitmap = ((BitmapDrawable) photo).getBitmap();
            int orgWidth = orgBitmap.getWidth();
            int orgHeight = orgBitmap.getHeight();
            int longerEdge = orgWidth > orgHeight ? orgWidth : orgHeight;
            // We want downscaled one only when the original icon is too big.
            if (longerEdge > iconSize) {
                float ratio = ((float) longerEdge) / iconSize;
                int newWidth = (int) (orgWidth / ratio);
                int newHeight = (int) (orgHeight / ratio);
                // If the longer edge is much longer than the shorter edge, the latter may
                // become 0 which will cause a crash.
                if (newWidth <= 0 || newHeight <= 0) {
                    Log.w(LOG_TAG, "Photo icon's width or height become 0.");
                    return null;
                }

                // It is sure ratio >= 1.0f in any case and thus the newly created Bitmap
                // should be smaller than the original.
                return Bitmap.createScaledBitmap(orgBitmap, newWidth, newHeight, true);
            } else {
                return orgBitmap;
            }
        }
    }

    /**
     * Private constructor for static class
     */
    private ContactsAsyncHelper() {
        HandlerThread thread = new HandlerThread("ContactsAsyncWorker");
        thread.start();
        sThreadHandler = new WorkerHandler(thread.getLooper());
    }

    /**
     * Starts an asynchronous image load. After finishing the load,
     * {@link OnImageLoadCompleteListener#onImageLoadComplete(int, Drawable, Bitmap, Object)}
     * will be called.
     *
     * @param token Arbitrary integer which will be returned as the first argument of
     * {@link OnImageLoadCompleteListener#onImageLoadComplete(int, Drawable, Bitmap, Object)}
     * @param context Context object used to do the time-consuming operation.
     * @param personUri Uri to be used to fetch the photo
     * @param listener Callback object which will be used when the asynchronous load is done.
     * Can be null, which means only the asynchronous load is done while there's no way to
     * obtain the loaded photos.
     * @param cookie Arbitrary object the caller wants to remember, which will become the
     * fourth argument of {@link OnImageLoadCompleteListener#onImageLoadComplete(int, Drawable,
     * Bitmap, Object)}. Can be null, at which the callback will also has null for the argument.
     */
    public static final void startObtainPhotoAsync(int token, Context context, Uri personUri,
            OnImageLoadCompleteListener listener, Object cookie) {
        // in case the source caller info is null, the URI will be null as well.
        // just update using the placeholder image in this case.
        if (personUri == null) {
            Log.wtf(LOG_TAG, "Uri is missing");
            return;
        }

        // Added additional Cookie field in the callee to handle arguments
        // sent to the callback function.

        // setup arguments
        WorkerArgs args = new WorkerArgs();
        args.cookie = cookie;
        args.context = context;
        args.uri = personUri;
        args.listener = listener;

        // setup message arguments
        Message msg = sThreadHandler.obtainMessage(token);
        msg.arg1 = EVENT_LOAD_IMAGE;
        msg.obj = args;

        if (DBG) Log.d(LOG_TAG, "Begin loading image: " + args.uri +
                ", displaying default image for now.");

        // notify the thread to begin working
        sThreadHandler.sendMessage(msg);
    }


}
