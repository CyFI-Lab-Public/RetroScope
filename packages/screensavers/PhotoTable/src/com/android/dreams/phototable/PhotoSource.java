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
package com.android.dreams.phototable;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

/**
 * Picks a random image from a source of photos.
 */
public abstract class PhotoSource {
    private static final String TAG = "PhotoTable.PhotoSource";
    private static final boolean DEBUG = false;

    // This should be large enough for BitmapFactory to decode the header so
    // that we can mark and reset the input stream to avoid duplicate network i/o
    private static final int BUFFER_SIZE = 32 * 1024;

    public class ImageData {
        public String id;
        public String url;
        public int orientation;

        protected String albumId;
        protected Cursor cursor;
        protected int position;
        protected Uri uri;

        InputStream getStream(int longSide) {
            return PhotoSource.this.getStream(this, longSide);
        }
        ImageData naturalNext() {
            return PhotoSource.this.naturalNext(this);
        }
        ImageData naturalPrevious() {
            return PhotoSource.this.naturalPrevious(this);
        }
        public void donePaging() {
            PhotoSource.this.donePaging(this);
        }
    }

    public class AlbumData {
        public String id;
        public String title;
        public String thumbnailUrl;
        public String account;
        public long updated;

        public String getType() {
            String type = PhotoSource.this.getClass().getName();
            log(TAG, "type is " + type);
            return type;
        }
    }

    private final LinkedList<ImageData> mImageQueue;
    private final int mMaxQueueSize;
    private final float mMaxCropRatio;
    private final int mBadImageSkipLimit;
    private final PhotoSource mFallbackSource;
    private final HashMap<Bitmap, ImageData> mImageMap;

    protected final Context mContext;
    protected final Resources mResources;
    protected final Random mRNG;
    protected final AlbumSettings mSettings;
    protected final ContentResolver mResolver;

    protected String mSourceName;

    public PhotoSource(Context context, SharedPreferences settings) {
        this(context, settings, new StockSource(context, settings));
    }

    public PhotoSource(Context context, SharedPreferences settings, PhotoSource fallbackSource) {
        mSourceName = TAG;
        mContext = context;
        mSettings = AlbumSettings.getAlbumSettings(settings);
        mResolver = mContext.getContentResolver();
        mResources = context.getResources();
        mImageQueue = new LinkedList<ImageData>();
        mMaxQueueSize = mResources.getInteger(R.integer.image_queue_size);
        mMaxCropRatio = mResources.getInteger(R.integer.max_crop_ratio) / 1000000f;
        mBadImageSkipLimit = mResources.getInteger(R.integer.bad_image_skip_limit);
        mImageMap = new HashMap<Bitmap, ImageData>();
        mRNG = new Random();
        mFallbackSource = fallbackSource;
    }

    protected void fillQueue() {
        log(TAG, "filling queue");
        mImageQueue.addAll(findImages(mMaxQueueSize - mImageQueue.size()));
        Collections.shuffle(mImageQueue);
        log(TAG, "queue contains: " + mImageQueue.size() + " items.");
    }

    public Bitmap next(BitmapFactory.Options options, int longSide, int shortSide) {
        log(TAG, "decoding a picasa resource to " +  longSide + ", " + shortSide);
        Bitmap image = null;
        ImageData imageData = null;
        int tries = 0;

        while (image == null && tries < mBadImageSkipLimit) {
            synchronized(mImageQueue) {
                if (mImageQueue.isEmpty()) {
                    fillQueue();
                }
                imageData = mImageQueue.poll();
            }
            if (imageData != null) {
                image = load(imageData, options, longSide, shortSide);
                mImageMap.put(image, imageData);
                imageData = null;
            }

            tries++;
        }

        if (image == null && mFallbackSource != null) {
            image = load((ImageData) mFallbackSource.findImages(1).toArray()[0],
                    options, longSide, shortSide);
        }

        return image;
    }

    public Bitmap load(ImageData data, BitmapFactory.Options options, int longSide, int shortSide) {
        log(TAG, "decoding photo resource to " +  longSide + ", " + shortSide);
        InputStream is = data.getStream(longSide);

        Bitmap image = null;
        try {
            BufferedInputStream bis = new BufferedInputStream(is);
            bis.mark(BUFFER_SIZE);

            options.inJustDecodeBounds = true;
            options.inSampleSize = 1;
            image = BitmapFactory.decodeStream(new BufferedInputStream(bis), null, options);
            int rawLongSide = Math.max(options.outWidth, options.outHeight);
            int rawShortSide = Math.min(options.outWidth, options.outHeight);
            log(TAG, "I see bounds of " +  rawLongSide + ", " + rawShortSide);

            if (rawLongSide != -1 && rawShortSide != -1) {
                float insideRatio = Math.max((float) longSide / (float) rawLongSide,
                                             (float) shortSide / (float) rawShortSide);
                float outsideRatio = Math.max((float) longSide / (float) rawLongSide,
                                              (float) shortSide / (float) rawShortSide);
                float ratio = (outsideRatio / insideRatio < mMaxCropRatio ?
                               outsideRatio : insideRatio);

                while (ratio < 0.5) {
                    options.inSampleSize *= 2;
                    ratio *= 2;
                }

                log(TAG, "decoding with inSampleSize " +  options.inSampleSize);
                try {
                    bis.reset();
                } catch (IOException ioe) {
                    // start over, something went wrong and we read too far into the image.
                    bis.close();
                    is = data.getStream(longSide);
                    bis = new BufferedInputStream(is);
                    log(TAG, "resetting the stream");
                }
                options.inJustDecodeBounds = false;
                image = BitmapFactory.decodeStream(bis, null, options);
                rawLongSide = Math.max(options.outWidth, options.outHeight);
                rawShortSide = Math.max(options.outWidth, options.outHeight);
                if (image != null && rawLongSide != -1 && rawShortSide != -1) {
                    ratio = Math.max((float) longSide / (float) rawLongSide,
                            (float) shortSide / (float) rawShortSide);

                    if (Math.abs(ratio - 1.0f) > 0.001) {
                        log(TAG, "still too big, scaling down by " + ratio);
                        options.outWidth = (int) (ratio * options.outWidth);
                        options.outHeight = (int) (ratio * options.outHeight);

                        image = Bitmap.createScaledBitmap(image,
                                options.outWidth, options.outHeight,
                                true);
                    }

                    if (data.orientation != 0) {
                        log(TAG, "rotated by " + data.orientation + ": fixing");
                        Matrix matrix = new Matrix();
                        matrix.setRotate(data.orientation,
                                (float) Math.floor(image.getWidth() / 2f),
                                (float) Math.floor(image.getHeight() / 2f));
                        image = Bitmap.createBitmap(image, 0, 0,
                                                    options.outWidth, options.outHeight,
                                                    matrix, true);
                        if (data.orientation == 90 || data.orientation == 270) {
                            int tmp = options.outWidth;
                            options.outWidth = options.outHeight;
                            options.outHeight = tmp;
                        }
                    }

                    log(TAG, "returning bitmap " + image.getWidth() + ", " + image.getHeight());
                } else {
                    image = null;
                }
            } else {
                image = null;
            }
            if (image == null) {
                log(TAG, "Stream decoding failed with no error" +
                        (options.mCancel ? " due to cancelation." : "."));
            }
        } catch (OutOfMemoryError ome) {
            log(TAG, "OUT OF MEMORY: " + ome);
            image = null;
        } catch (FileNotFoundException fnf) {
            log(TAG, "file not found: " + fnf);
            image = null;
        } catch (IOException ioe) {
            log(TAG, "i/o exception: " + ioe);
            image = null;
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (Throwable t) {
                log(TAG, "close fail: " + t.toString());
            }
        }

        return image;
    }

    public void setSeed(long seed) {
        mRNG.setSeed(seed);
    }

    protected static void log(String tag, String message) {
        if (DEBUG) {
            Log.i(tag, message);
        }
    }

    protected int pickRandomStart(int total, int max) {
        if (max >= total) {
            return -1;
        } else {
            return (mRNG.nextInt() % (total - max)) - 1;
        }
    }

    public Bitmap naturalNext(Bitmap current, BitmapFactory.Options options,
            int longSide, int shortSide) {
        Bitmap image = null;
        ImageData data = mImageMap.get(current);
        if (data != null) {
          ImageData next = data.naturalNext();
          if (next != null) {
            image = load(next, options, longSide, shortSide);
            mImageMap.put(image, next);
          }
        }
        return image;
    }

    public Bitmap naturalPrevious(Bitmap current, BitmapFactory.Options options,
            int longSide, int shortSide) {
        Bitmap image = null;
        ImageData data = mImageMap.get(current);
        if (current != null) {
          ImageData prev = data.naturalPrevious();
          if (prev != null) {
            image = load(prev, options, longSide, shortSide);
            mImageMap.put(image, prev);
          }
        }
        return image;
    }

    public void donePaging(Bitmap current) {
        ImageData data = mImageMap.get(current);
        if (data != null) {
            data.donePaging();
        }
    }

    public void recycle(Bitmap trash) {
        if (trash != null) {
            mImageMap.remove(trash);
            trash.recycle();
        }
    }

    protected abstract InputStream getStream(ImageData data, int longSide);
    protected abstract Collection<ImageData> findImages(int howMany);
    protected abstract ImageData naturalNext(ImageData current);
    protected abstract ImageData naturalPrevious(ImageData current);
    protected abstract void donePaging(ImageData current);

    public abstract Collection<AlbumData> findAlbums();
}
