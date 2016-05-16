package com.android.ex.photo.loaders;

import android.graphics.Bitmap;

public interface PhotoBitmapLoaderInterface {

    public void setPhotoUri(String photoUri);

    public void forceLoad();

    public static class BitmapResult {
        public static final int STATUS_SUCCESS = 0;
        public static final int STATUS_EXCEPTION = 1;

        public Bitmap bitmap;
        public int status;
    }
}
