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

package com.android.quicksearchbox;

import com.android.quicksearchbox.util.CachedLater;
import com.android.quicksearchbox.util.Consumer;
import com.android.quicksearchbox.util.Now;
import com.android.quicksearchbox.util.NowOrLater;
import com.android.quicksearchbox.util.NowOrLaterWrapper;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.util.WeakHashMap;

/**
 * Icon loader that caches the results of another icon loader.
 *
 */
public class CachingIconLoader implements IconLoader {

    private static final boolean DBG = false;
    private static final String TAG = "QSB.CachingIconLoader";

    private final IconLoader mWrapped;

    private final WeakHashMap<String, Entry> mIconCache;

    /**
     * Creates a new caching icon loader.
     *
     * @param wrapped IconLoader whose results will be cached.
     */
    public CachingIconLoader(IconLoader wrapped) {
        mWrapped = wrapped;
        mIconCache = new WeakHashMap<String, Entry>();
    }

    public NowOrLater<Drawable> getIcon(String drawableId) {
        if (DBG) Log.d(TAG, "getIcon(" + drawableId + ")");
        if (TextUtils.isEmpty(drawableId) || "0".equals(drawableId)) {
            return new Now<Drawable>(null);
        }
        Entry newEntry = null;
        NowOrLater<Drawable.ConstantState> drawableState;
        synchronized (this) {
            drawableState = queryCache(drawableId);
            if (drawableState == null) {
                newEntry = new Entry();
                storeInIconCache(drawableId, newEntry);
            }
        }
        if (drawableState != null) {
            return new NowOrLaterWrapper<Drawable.ConstantState, Drawable>(drawableState){
                @Override
                public Drawable get(Drawable.ConstantState value) {
                    return value == null ? null : value.newDrawable();
                }};
        }
        NowOrLater<Drawable> drawable = mWrapped.getIcon(drawableId);
        newEntry.set(drawable);
        storeInIconCache(drawableId, newEntry);
        return drawable;
    }

    public Uri getIconUri(String drawableId) {
        return mWrapped.getIconUri(drawableId);
    }

    private synchronized NowOrLater<Drawable.ConstantState> queryCache(String drawableId) {
        NowOrLater<Drawable.ConstantState> cached = mIconCache.get(drawableId);
        if (DBG) {
            if (cached != null) Log.d(TAG, "Found icon in cache: " + drawableId);
        }
        return cached;
    }

    private synchronized void storeInIconCache(String resourceUri, Entry drawable) {
        if (drawable != null) {
            mIconCache.put(resourceUri, drawable);
        }
    }

    private static class Entry extends CachedLater<Drawable.ConstantState>
            implements Consumer<Drawable>{
        private NowOrLater<Drawable> mDrawable;
        private boolean mGotDrawable;
        private boolean mCreateRequested;

        public Entry() {
        }

        public synchronized void set(NowOrLater<Drawable> drawable) {
            if (mGotDrawable) throw new IllegalStateException("set() may only be called once.");
            mGotDrawable = true;
            mDrawable = drawable;
            if (mCreateRequested) {
                getLater();
            }
        }

        @Override
        protected synchronized void create() {
            if (!mCreateRequested) {
                mCreateRequested = true;
                if (mGotDrawable) {
                    getLater();
                }
            }
        }

        private void getLater() {
            NowOrLater<Drawable> drawable = mDrawable;
            mDrawable = null;
            drawable.getLater(this);
        }

        public boolean consume(Drawable value) {
            store(value == null ? null : value.getConstantState());
            return true;
        }
    }

}
