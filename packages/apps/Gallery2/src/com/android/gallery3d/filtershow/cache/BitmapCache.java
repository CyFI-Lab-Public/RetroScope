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

package com.android.gallery3d.filtershow.cache;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Log;
import com.android.gallery3d.filtershow.pipeline.Buffer;
import com.android.gallery3d.filtershow.pipeline.CacheProcessing;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;

public class BitmapCache {
    private static final String LOGTAG = "BitmapCache";
    private HashMap<Long, ArrayList<WeakReference<Bitmap>>>
            mBitmapCache = new HashMap<Long, ArrayList<WeakReference<Bitmap>>>();
    private final int mMaxItemsPerKey = 4;

    private static final boolean DEBUG = false;
    private CacheProcessing mCacheProcessing;

    public final static int PREVIEW_CACHE = 1;
    public final static int NEW_LOOK = 2;
    public final static int ICON = 3;
    public final static int FILTERS = 4;
    public final static int GEOMETRY = 5;
    public final static int HIGHRES = 6;
    public final static int UTIL_GEOMETRY = 7;
    public final static int RENDERING_REQUEST = 8;
    public final static int REGION = 9;
    public final static int TINY_PLANET = 10;
    public final static int PREVIEW_CACHE_NO_FILTERS = 11;
    public final static int PREVIEW_CACHE_NO_ROOT = 12;
    public static final int PREVIEW_CACHE_NO_APPLY = 13;
    public final static int TRACKING_COUNT = 14;
    private int[] mTracking = new int[TRACKING_COUNT];

    class BitmapTracking {
        Bitmap bitmap;
        int type;
    }

    private ArrayList<BitmapTracking> mBitmapTracking = new ArrayList<BitmapTracking>();

    private void track(Bitmap bitmap, int type) {
        for (int i = 0; i < mBitmapTracking.size(); i++) {
            BitmapTracking tracking = mBitmapTracking.get(i);
            if (tracking.bitmap == bitmap) {
                Log.e(LOGTAG, "giving a bitmap already given!!!");
            }
        }
        BitmapTracking tracking = new BitmapTracking();
        tracking.bitmap = bitmap;
        tracking.type = type;
        mBitmapTracking.add(tracking);
        mTracking[tracking.type] ++;
    }

    private void untrack(Bitmap bitmap) {
        for (int i = 0; i < mBitmapTracking.size(); i++) {
            BitmapTracking tracking = mBitmapTracking.get(i);
            if (tracking.bitmap == bitmap) {
                mTracking[tracking.type] --;
                mBitmapTracking.remove(i);
                return;
            }
        }
    }

    public String getTrackingName(int i) {
        switch (i) {
            case PREVIEW_CACHE: return "PREVIEW_CACHE";
            case PREVIEW_CACHE_NO_FILTERS: return "PREVIEW_CACHE_NO_FILTERS";
            case PREVIEW_CACHE_NO_ROOT: return "PREVIEW_CACHE_NO_ROOT";
            case PREVIEW_CACHE_NO_APPLY: return "PREVIEW_CACHE_NO_APPLY";
            case NEW_LOOK: return "NEW_LOOK";
            case ICON: return "ICON";
            case FILTERS: return "FILTERS";
            case GEOMETRY: return "GEOMETRY";
            case HIGHRES: return "HIGHRES";
            case UTIL_GEOMETRY: return "UTIL_GEOMETRY";
            case RENDERING_REQUEST: return "RENDERING_REQUEST";
            case REGION: return "REGION";
            case TINY_PLANET: return "TINY_PLANET";
        }
        return "UNKNOWN";
    }

    public void showBitmapCounts() {
        if (!DEBUG) {
            return;
        }
        Log.v(LOGTAG, "\n--- showBitmap --- ");
        for (int i = 0; i < TRACKING_COUNT; i++) {
            if (mTracking[i] != 0) {
                Log.v(LOGTAG, getTrackingName(i) + " => " + mTracking[i]);
            }
        }
    }

    public void setCacheProcessing(CacheProcessing cache) {
        mCacheProcessing = cache;
    }

    public void cache(Buffer buffer) {
        if (buffer == null) {
            return;
        }
        Bitmap bitmap = buffer.getBitmap();
        cache(bitmap);
    }

    public synchronized boolean cache(Bitmap bitmap) {
        if (bitmap == null) {
            return true;
        }
        if (mCacheProcessing != null && mCacheProcessing.contains(bitmap)) {
            Log.e(LOGTAG, "Trying to cache a bitmap still used in the pipeline");
            return false;
        }
        if (DEBUG) {
            untrack(bitmap);
        }
        if (!bitmap.isMutable()) {
            Log.e(LOGTAG, "Trying to cache a non mutable bitmap");
            return true;
        }
        Long key = calcKey(bitmap.getWidth(), bitmap.getHeight());
        ArrayList<WeakReference<Bitmap>> list = mBitmapCache.get(key);
        if (list == null) {
            list = new ArrayList<WeakReference<Bitmap>>();
            mBitmapCache.put(key, list);
        }
        int i = 0;
        while (i < list.size()) {
            if (list.get(i).get() == null) {
                list.remove(i);
            } else {
                i++;
            }
        }
        for (i = 0; i < list.size(); i++) {
            if (list.get(i).get() == null) {
                list.remove(i);
            }
        }
        if (list.size() < mMaxItemsPerKey) {
            for (i = 0; i < list.size(); i++) {
                WeakReference<Bitmap> ref = list.get(i);
                if (ref.get() == bitmap) {
                    return true; // bitmap already in the cache
                }
            }
            list.add(new WeakReference<Bitmap>(bitmap));
        }
        return true;
    }

    public synchronized Bitmap getBitmap(int w, int h, int type) {
        Long key = calcKey(w, h);
        WeakReference<Bitmap> ref = null;
        ArrayList<WeakReference<Bitmap>> list = mBitmapCache.get(key);
        if (list != null && list.size() > 0) {
            ref = list.remove(0);
            if (list.size() == 0) {
                mBitmapCache.remove(key);
            }
        }
        Bitmap bitmap = null;
        if (ref != null) {
            bitmap = ref.get();
        }
        if (bitmap == null
                || bitmap.getWidth() != w
                || bitmap.getHeight() != h) {
            bitmap = Bitmap.createBitmap(
                    w, h, Bitmap.Config.ARGB_8888);
            showBitmapCounts();
        }

        if (DEBUG) {
            track(bitmap, type);
            if (mCacheProcessing != null && mCacheProcessing.contains(bitmap)) {
                Log.e(LOGTAG, "Trying to give a bitmap used in the pipeline");
            }
        }
        return bitmap;
    }

    public synchronized Bitmap getBitmapCopy(Bitmap source, int type) {
        Bitmap bitmap = getBitmap(source.getWidth(), source.getHeight(), type);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(source, 0, 0, null);
        return bitmap;
    }

    private Long calcKey(long w, long h) {
        return (w << 32) | h;
    }
}
