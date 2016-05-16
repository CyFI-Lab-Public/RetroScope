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

package com.android.mail.photomanager;

import android.content.ComponentCallbacks2;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;
import android.util.LruCache;

import com.android.mail.ui.ImageCanvas;
import com.android.mail.utils.LogUtils;
import com.android.mail.utils.Utils;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Asynchronously loads photos and maintains a cache of photos
 */
public abstract class PhotoManager implements ComponentCallbacks2, Callback {
    /**
     * Get the default image provider that draws while the photo is being
     * loaded.
     */
    protected abstract DefaultImageProvider getDefaultImageProvider();

    /**
     * Generate a hashcode unique to each request.
     */
    protected abstract int getHash(PhotoIdentifier id, ImageCanvas view);

    /**
     * Return a specific implementation of PhotoLoaderThread.
     */
    protected abstract PhotoLoaderThread getLoaderThread(ContentResolver contentResolver);

    /**
     * Subclasses can implement this method to alert callbacks that images finished loading.
     * @param request The original request made.
     * @param success True if we successfully loaded the image from cache. False if we fell back
     *                to the default image.
     */
    protected void onImageDrawn(final Request request, final boolean success) {
        // Subclasses can choose to do something about this
    }

    /**
     * Subclasses can implement this method to alert callbacks that images started loading.
     * @param request The original request made.
     */
    protected void onImageLoadStarted(final Request request) {
        // Subclasses can choose to do something about this
    }

    /**
     * Subclasses can implement this method to determine whether a previously loaded bitmap can
     * be reused for a new canvas size.
     * @param prevWidth The width of the previously loaded bitmap.
     * @param prevHeight The height of the previously loaded bitmap.
     * @param newWidth The width of the canvas this request is drawing on.
     * @param newHeight The height of the canvas this request is drawing on.
     * @return
     */
    protected boolean isSizeCompatible(int prevWidth, int prevHeight, int newWidth, int newHeight) {
        return true;
    }

    protected final Context getContext() {
        return mContext;
    }

    static final String TAG = "PhotoManager";
    static final boolean DEBUG = false; // Don't submit with true
    static final boolean DEBUG_SIZES = false; // Don't submit with true

    private static final String LOADER_THREAD_NAME = "PhotoLoader";

    /**
     * Type of message sent by the UI thread to itself to indicate that some photos
     * need to be loaded.
     */
    private static final int MESSAGE_REQUEST_LOADING = 1;

    /**
     * Type of message sent by the loader thread to indicate that some photos have
     * been loaded.
     */
    private static final int MESSAGE_PHOTOS_LOADED = 2;

    /**
     * Type of message sent by the loader thread to indicate that
     */
    private static final int MESSAGE_PHOTO_LOADING = 3;

    public interface DefaultImageProvider {
        /**
         * Applies the default avatar to the DividedImageView. Extent is an
         * indicator for the size (width or height). If darkTheme is set, the
         * avatar is one that looks better on dark background
         * @param id
         */
        public void applyDefaultImage(PhotoIdentifier id, ImageCanvas view, int extent);
    }

    /**
     * Maintains the state of a particular photo.
     */
    protected static class BitmapHolder {
        byte[] bytes;
        int width;
        int height;

        volatile boolean fresh;

        public BitmapHolder(byte[] bytes, int width, int height) {
            this.bytes = bytes;
            this.width = width;
            this.height = height;
            this.fresh = true;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("{");
            sb.append(super.toString());
            sb.append(" bytes=");
            sb.append(bytes);
            sb.append(" size=");
            sb.append(bytes == null ? 0 : bytes.length);
            sb.append(" width=");
            sb.append(width);
            sb.append(" height=");
            sb.append(height);
            sb.append(" fresh=");
            sb.append(fresh);
            sb.append("}");
            return sb.toString();
        }
    }

    // todo:ath caches should be member vars
    /**
     * An LRU cache for bitmap holders. The cache contains bytes for photos just
     * as they come from the database. Each holder has a soft reference to the
     * actual bitmap. The keys are decided by the implementation.
     */
    private static final LruCache<Object, BitmapHolder> sBitmapHolderCache;

    /**
     * Level 2 LRU cache for bitmaps. This is a smaller cache that holds
     * the most recently used bitmaps to save time on decoding
     * them from bytes (the bytes are stored in {@link #sBitmapHolderCache}.
     * The keys are decided by the implementation.
     */
    private static final LruCache<BitmapIdentifier, Bitmap> sBitmapCache;

    /** Cache size for {@link #sBitmapHolderCache} for devices with "large" RAM. */
    private static final int HOLDER_CACHE_SIZE = 2000000;

    /** Cache size for {@link #sBitmapCache} for devices with "large" RAM. */
    private static final int BITMAP_CACHE_SIZE = 1024 * 1024 * 8; // 8MB

    /** For debug: How many times we had to reload cached photo for a stale entry */
    private static final AtomicInteger sStaleCacheOverwrite = new AtomicInteger();

    /** For debug: How many times we had to reload cached photo for a fresh entry.  Should be 0. */
    private static final AtomicInteger sFreshCacheOverwrite = new AtomicInteger();

    static {
        final float cacheSizeAdjustment =
                (MemoryUtils.getTotalMemorySize() >= MemoryUtils.LARGE_RAM_THRESHOLD) ?
                        1.0f : 0.5f;
        final int holderCacheSize = (int) (cacheSizeAdjustment * HOLDER_CACHE_SIZE);
        sBitmapHolderCache = new LruCache<Object, BitmapHolder>(holderCacheSize) {
            @Override protected int sizeOf(Object key, BitmapHolder value) {
                return value.bytes != null ? value.bytes.length : 0;
            }

            @Override protected void entryRemoved(
                    boolean evicted, Object key, BitmapHolder oldValue, BitmapHolder newValue) {
                if (DEBUG) dumpStats();
            }
        };
        final int bitmapCacheSize = (int) (cacheSizeAdjustment * BITMAP_CACHE_SIZE);
        sBitmapCache = new LruCache<BitmapIdentifier, Bitmap>(bitmapCacheSize) {
            @Override protected int sizeOf(BitmapIdentifier key, Bitmap value) {
                return value.getByteCount();
            }

            @Override protected void entryRemoved(
                    boolean evicted, BitmapIdentifier key, Bitmap oldValue, Bitmap newValue) {
                if (DEBUG) dumpStats();
            }
        };
        LogUtils.i(TAG, "Cache adj: " + cacheSizeAdjustment);
        if (DEBUG) {
            LogUtils.d(TAG, "Cache size: " + btk(sBitmapHolderCache.maxSize())
                    + " + " + btk(sBitmapCache.maxSize()));
        }
    }

    /**
     * A map from ImageCanvas hashcode to the corresponding photo ID or uri,
     * encapsulated in a request. The request may swapped out before the photo
     * loading request is started.
     */
    private final Map<Integer, Request> mPendingRequests = Collections.synchronizedMap(
            new HashMap<Integer, Request>());

    /**
     * Handler for messages sent to the UI thread.
     */
    private final Handler mMainThreadHandler = new Handler(this);

    /**
     * Thread responsible for loading photos from the database. Created upon
     * the first request.
     */
    private PhotoLoaderThread mLoaderThread;

    /**
     * A gate to make sure we only send one instance of MESSAGE_PHOTOS_NEEDED at a time.
     */
    private boolean mLoadingRequested;

    /**
     * Flag indicating if the image loading is paused.
     */
    private boolean mPaused;

    private final Context mContext;

    public PhotoManager(Context context) {
        mContext = context;
    }

    public void loadThumbnail(PhotoIdentifier id, ImageCanvas view) {
        loadThumbnail(id, view, null);
    }

    /**
     * Load an image
     *
     * @param dimensions    Preferred dimensions
     */
    public void loadThumbnail(final PhotoIdentifier id, final ImageCanvas view,
            final ImageCanvas.Dimensions dimensions) {
        Utils.traceBeginSection("Load thumbnail");
        final DefaultImageProvider defaultProvider = getDefaultImageProvider();
        final Request request = new Request(id, defaultProvider, view, dimensions);
        final int hashCode = request.hashCode();

        if (!id.isValid()) {
            // No photo is needed
            request.applyDefaultImage();
            onImageDrawn(request, false);
            mPendingRequests.remove(hashCode);
        } else if (mPendingRequests.containsKey(hashCode)) {
            LogUtils.d(TAG, "load request dropped for %s", id);
        } else {
            if (DEBUG) LogUtils.v(TAG, "loadPhoto request: %s", id.getKey());
            loadPhoto(hashCode, request);
        }
        Utils.traceEndSection();
    }

    private void loadPhoto(int hashCode, Request request) {
        if (DEBUG) {
            LogUtils.v(TAG, "NEW IMAGE REQUEST key=%s r=%s thread=%s",
                    request.getKey(),
                    request,
                    Thread.currentThread());
        }

        boolean loaded = loadCachedPhoto(request, false);
        if (loaded) {
            if (DEBUG) {
                LogUtils.v(TAG, "image request, cache hit. request queue size=%s",
                        mPendingRequests.size());
            }
        } else {
            if (DEBUG) {
                LogUtils.d(TAG, "image request, cache miss: key=%s", request.getKey());
            }
            mPendingRequests.put(hashCode, request);
            if (!mPaused) {
                // Send a request to start loading photos
                requestLoading();
            }
        }
    }

    /**
     * Remove photo from the supplied image view. This also cancels current pending load request
     * inside this photo manager.
     */
    public void removePhoto(int hashcode) {
        Request r = mPendingRequests.remove(hashcode);
        if (r != null) {
            LogUtils.d(TAG, "removed request %s", r.getKey());
        }
    }

    private void ensureLoaderThread() {
        if (mLoaderThread == null) {
            mLoaderThread = getLoaderThread(mContext.getContentResolver());
            mLoaderThread.start();
        }
    }

    /**
     * Checks if the photo is present in cache.  If so, sets the photo on the view.
     *
     * @param request                   Determines which image to load from cache.
     * @param afterLoaderThreadFinished Pass true if calling after the LoaderThread has run. Pass
     *                                  false if the Loader Thread hasn't made any attempts to
     *                                  load images yet.
     * @return false if the photo needs to be (re)loaded from the provider.
     */
    private boolean loadCachedPhoto(final Request request,
            final boolean afterLoaderThreadFinished) {
        Utils.traceBeginSection("Load cached photo");
        final Bitmap cached = getCachedPhoto(request.bitmapKey);
        if (cached != null) {
            if (DEBUG) {
                LogUtils.v(TAG, "%s, key=%s decodedSize=%s thread=%s",
                        afterLoaderThreadFinished ? "DECODED IMG READ"
                                : "DECODED IMG CACHE HIT",
                        request.getKey(),
                        cached.getByteCount(),
                        Thread.currentThread());
            }
            if (request.getView().getGeneration() == request.viewGeneration) {
                request.getView().drawImage(cached, request.getKey());
                onImageDrawn(request, true);
            }
            Utils.traceEndSection();
            return true;
        }

        // We couldn't load the requested image, so try to load a replacement.
        // This removes the flicker from SIMPLE to BEST transition.
        final Object replacementKey = request.getPhotoIdentifier().getKeyToShowInsteadOfDefault();
        if (replacementKey != null) {
            final BitmapIdentifier replacementBitmapKey = new BitmapIdentifier(replacementKey,
                    request.bitmapKey.w, request.bitmapKey.h);
            final Bitmap cachedReplacement = getCachedPhoto(replacementBitmapKey);
            if (cachedReplacement != null) {
                if (DEBUG) {
                    LogUtils.v(TAG, "%s, key=%s decodedSize=%s thread=%s",
                            afterLoaderThreadFinished ? "DECODED IMG READ"
                                    : "DECODED IMG CACHE HIT",
                            replacementKey,
                            cachedReplacement.getByteCount(),
                            Thread.currentThread());
                }
                if (request.getView().getGeneration() == request.viewGeneration) {
                    request.getView().drawImage(cachedReplacement, request.getKey());
                    onImageDrawn(request, true);
                }
                Utils.traceEndSection();
                return false;
            }
        }

        // We couldn't load any image, so draw a default image
        request.applyDefaultImage();

        final BitmapHolder holder = sBitmapHolderCache.get(request.getKey());
        // Check if we loaded null bytes, which means we meant to not draw anything.
        if (holder != null && holder.bytes == null) {
            onImageDrawn(request, holder.fresh);
            Utils.traceEndSection();
            return holder.fresh;
        }
        Utils.traceEndSection();
        return false;
    }

    /**
     * Takes care of retrieving the Bitmap from both the decoded and holder caches.
     */
    private static Bitmap getCachedPhoto(BitmapIdentifier bitmapKey) {
        Utils.traceBeginSection("Get cached photo");
        final Bitmap cached = sBitmapCache.get(bitmapKey);
        Utils.traceEndSection();
        return cached;
    }

    /**
     * Temporarily stops loading photos from the database.
     */
    public void pause() {
        LogUtils.d(TAG, "%s paused.", getClass().getName());
        mPaused = true;
    }

    /**
     * Resumes loading photos from the database.
     */
    public void resume() {
        LogUtils.d(TAG, "%s resumed.", getClass().getName());
        mPaused = false;
        if (DEBUG) dumpStats();
        if (!mPendingRequests.isEmpty()) {
            requestLoading();
        }
    }

    /**
     * Sends a message to this thread itself to start loading images.  If the current
     * view contains multiple image views, all of those image views will get a chance
     * to request their respective photos before any of those requests are executed.
     * This allows us to load images in bulk.
     */
    private void requestLoading() {
        if (!mLoadingRequested) {
            mLoadingRequested = true;
            mMainThreadHandler.sendEmptyMessage(MESSAGE_REQUEST_LOADING);
        }
    }

    /**
     * Processes requests on the main thread.
     */
    @Override
    public boolean handleMessage(final Message msg) {
        switch (msg.what) {
            case MESSAGE_REQUEST_LOADING: {
                mLoadingRequested = false;
                if (!mPaused) {
                    ensureLoaderThread();
                    mLoaderThread.requestLoading();
                }
                return true;
            }

            case MESSAGE_PHOTOS_LOADED: {
                processLoadedImages();
                if (DEBUG) dumpStats();
                return true;
            }

            case MESSAGE_PHOTO_LOADING: {
                final int hashcode = msg.arg1;
                final Request request = mPendingRequests.get(hashcode);
                onImageLoadStarted(request);
                return true;
            }
        }
        return false;
    }

    /**
     * Goes over pending loading requests and displays loaded photos.  If some of the
     * photos still haven't been loaded, sends another request for image loading.
     */
    private void processLoadedImages() {
        Utils.traceBeginSection("process loaded images");
        final List<Integer> toRemove = Lists.newArrayList();
        for (final Integer hash : mPendingRequests.keySet()) {
            final Request request = mPendingRequests.get(hash);
            final boolean loaded = loadCachedPhoto(request, true);
            // Request can go through multiple attempts if the LoaderThread fails to load any
            // images for it, or if the images it loads are evicted from the cache before we
            // could access them in the main thread.
            if (loaded || request.attempts > 2) {
                toRemove.add(hash);
            }
        }
        for (final Integer key : toRemove) {
            mPendingRequests.remove(key);
        }

        if (!mPaused && !mPendingRequests.isEmpty()) {
            LogUtils.d(TAG, "Finished loading batch. %d still have to be loaded.",
                    mPendingRequests.size());
            requestLoading();
        }
        Utils.traceEndSection();
    }

    /**
     * Stores the supplied bitmap in cache.
     */
    private static void cacheBitmapHolder(final String cacheKey, final BitmapHolder holder) {
        if (DEBUG) {
            BitmapHolder prev = sBitmapHolderCache.get(cacheKey);
            if (prev != null && prev.bytes != null) {
                LogUtils.d(TAG, "Overwriting cache: key=" + cacheKey
                        + (prev.fresh ? " FRESH" : " stale"));
                if (prev.fresh) {
                    sFreshCacheOverwrite.incrementAndGet();
                } else {
                    sStaleCacheOverwrite.incrementAndGet();
                }
            }
            LogUtils.d(TAG, "Caching data: key=" + cacheKey + ", "
                    + (holder.bytes == null ? "<null>" : btk(holder.bytes.length)));
        }

        sBitmapHolderCache.put(cacheKey, holder);
    }

    protected static void cacheBitmap(final BitmapIdentifier bitmapKey, final Bitmap bitmap) {
        sBitmapCache.put(bitmapKey, bitmap);
    }

    // ComponentCallbacks2
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    }

    // ComponentCallbacks2
    @Override
    public void onLowMemory() {
    }

    // ComponentCallbacks2
    @Override
    public void onTrimMemory(int level) {
        if (DEBUG) LogUtils.d(TAG, "onTrimMemory: " + level);
        if (level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
            // Clear the caches.  Note all pending requests will be removed too.
            clear();
        }
    }

    public void clear() {
        if (DEBUG) LogUtils.d(TAG, "clear");
        mPendingRequests.clear();
        sBitmapHolderCache.evictAll();
        sBitmapCache.evictAll();
    }

    /**
     * Dump cache stats on logcat.
     */
    private static void dumpStats() {
        if (!DEBUG) {
            return;
        }
        int numHolders = 0;
        int rawBytes = 0;
        int bitmapBytes = 0;
        int numBitmaps = 0;
        for (BitmapHolder h : sBitmapHolderCache.snapshot().values()) {
            numHolders++;
            if (h.bytes != null) {
                rawBytes += h.bytes.length;
                numBitmaps++;
            }
        }
        LogUtils.d(TAG,
                "L1: " + btk(rawBytes) + " + " + btk(bitmapBytes) + " = "
                        + btk(rawBytes + bitmapBytes) + ", " + numHolders + " holders, "
                        + numBitmaps + " bitmaps, avg: " + btk(safeDiv(rawBytes, numBitmaps)));
        LogUtils.d(TAG, "L1 Stats: %s, overwrite: fresh=%s stale=%s", sBitmapHolderCache,
                sFreshCacheOverwrite.get(), sStaleCacheOverwrite.get());

        numBitmaps = 0;
        bitmapBytes = 0;
        for (Bitmap b : sBitmapCache.snapshot().values()) {
            numBitmaps++;
            bitmapBytes += b.getByteCount();
        }
        LogUtils.d(TAG, "L2: " + btk(bitmapBytes) + ", " + numBitmaps + " bitmaps" + ", avg: "
                + btk(safeDiv(bitmapBytes, numBitmaps)));
        // We don't get from L2 cache, so L2 stats is meaningless.
    }

    /** Converts bytes to K bytes, rounding up.  Used only for debug log. */
    private static String btk(int bytes) {
        return ((bytes + 1023) / 1024) + "K";
    }

    private static final int safeDiv(int dividend, int divisor) {
        return (divisor  == 0) ? 0 : (dividend / divisor);
    }

    public static abstract class PhotoIdentifier implements Comparable<PhotoIdentifier> {
        /**
         * If this returns false, the PhotoManager will not attempt to load the
         * bitmap. Instead, the default image provider will be used.
         */
        public abstract boolean isValid();

        /**
         * Identifies this request.
         */
        public abstract Object getKey();

        /**
         * Replacement key to try to load from cache instead of drawing the default image. This
         * is useful when we've already loaded a SIMPLE rendition, and are now loading the BEST
         * rendition. We want the BEST image to appear seamlessly on top of the existing SIMPLE
         * image.
         */
        public Object getKeyToShowInsteadOfDefault() {
            return null;
        }
    }

    /**
     * The thread that performs loading of photos from the database.
     */
    protected abstract class PhotoLoaderThread extends HandlerThread implements Callback {

        /**
         * Return photos mapped from {@link Request#getKey()} to the photo for
         * that request.
         */
        protected abstract Map<String, BitmapHolder> loadPhotos(Collection<Request> requests);

        private static final int MESSAGE_LOAD_PHOTOS = 0;

        private final ContentResolver mResolver;

        private Handler mLoaderThreadHandler;

        public PhotoLoaderThread(ContentResolver resolver) {
            super(LOADER_THREAD_NAME, Process.THREAD_PRIORITY_BACKGROUND);
            mResolver = resolver;
        }

        protected ContentResolver getResolver() {
            return mResolver;
        }

        public void ensureHandler() {
            if (mLoaderThreadHandler == null) {
                mLoaderThreadHandler = new Handler(getLooper(), this);
            }
        }

        /**
         * Sends a message to this thread to load requested photos.  Cancels a preloading
         * request, if any: we don't want preloading to impede loading of the photos
         * we need to display now.
         */
        public void requestLoading() {
            ensureHandler();
            mLoaderThreadHandler.sendEmptyMessage(MESSAGE_LOAD_PHOTOS);
        }

        /**
         * Receives the above message, loads photos and then sends a message
         * to the main thread to process them.
         */
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_LOAD_PHOTOS:
                    loadPhotosInBackground();
                    break;
            }
            return true;
        }

        /**
         * Subclasses may specify the maximum number of requests to be given at a time to
         * #loadPhotos(). For batch count N, the UI will be updated with up to N images at a time.
         *
         * @return A positive integer if you would like to limit the number of
         *         items in a single batch.
         */
        protected int getMaxBatchCount() {
            return -1;
        }

        private void loadPhotosInBackground() {
            Utils.traceBeginSection("pre processing");
            final Collection<Request> loadRequests = new HashSet<PhotoManager.Request>();
            final Collection<Request> decodeRequests = new HashSet<PhotoManager.Request>();
            final PriorityQueue<Request> requests;
            synchronized (mPendingRequests) {
                requests = new PriorityQueue<Request>(mPendingRequests.values());
            }

            int batchCount = 0;
            int maxBatchCount = getMaxBatchCount();
            while (!requests.isEmpty()) {
                Request request = requests.poll();
                final BitmapHolder holder = sBitmapHolderCache
                        .get(request.getKey());
                if (holder == null || holder.bytes == null || !holder.fresh || !isSizeCompatible(
                        holder.width, holder.height, request.bitmapKey.w, request.bitmapKey.h)) {
                    loadRequests.add(request);
                    decodeRequests.add(request);
                    batchCount++;

                    final Message msg = Message.obtain();
                    msg.what = MESSAGE_PHOTO_LOADING;
                    msg.arg1 = request.hashCode();
                    mMainThreadHandler.sendMessage(msg);
                } else {
                    // Even if the image load is already done, this particular decode configuration
                    // may not yet have run. Be sure to add it to the queue.
                    if (sBitmapCache.get(request.bitmapKey) == null) {
                        decodeRequests.add(request);
                    }
                }
                request.attempts++;
                if (maxBatchCount > 0 && batchCount >= maxBatchCount) {
                    break;
                }
            }
            Utils.traceEndSection();

            Utils.traceBeginSection("load photos");
            // Ask subclass to do the actual loading
            final Map<String, BitmapHolder> photosMap = loadPhotos(loadRequests);
            Utils.traceEndSection();

            if (DEBUG) {
                LogUtils.d(TAG,
                        "worker thread completed read request batch. inputN=%s outputN=%s",
                        loadRequests.size(),
                        photosMap.size());
            }
            Utils.traceBeginSection("post processing");
            for (String cacheKey : photosMap.keySet()) {
                if (DEBUG) {
                    LogUtils.d(TAG,
                            "worker thread completed read request key=%s byteCount=%s thread=%s",
                            cacheKey,
                            photosMap.get(cacheKey) == null ? 0
                                    : photosMap.get(cacheKey).bytes.length,
                            Thread.currentThread());
                }
                cacheBitmapHolder(cacheKey, photosMap.get(cacheKey));
            }

            for (Request r : decodeRequests) {
                if (sBitmapCache.get(r.bitmapKey) != null) {
                    continue;
                }

                final Object cacheKey = r.getKey();
                final BitmapHolder holder = sBitmapHolderCache.get(cacheKey);
                if (holder == null || holder.bytes == null || !holder.fresh || !isSizeCompatible(
                        holder.width, holder.height, r.bitmapKey.w, r.bitmapKey.h)) {
                    continue;
                }

                final int w = r.bitmapKey.w;
                final int h = r.bitmapKey.h;
                final byte[] src = holder.bytes;

                if (w == 0 || h == 0) {
                    LogUtils.e(TAG, new Error(), "bad dimensions for request=%s w/h=%s/%s",
                            r, w, h);
                }

                final Bitmap decoded = BitmapUtil.decodeByteArrayWithCenterCrop(src, w, h);
                if (DEBUG) {
                    LogUtils.i(TAG,
                            "worker thread completed decode bmpKey=%s decoded=%s holder=%s",
                            r.bitmapKey, decoded, holder);
                }

                if (decoded != null) {
                    cacheBitmap(r.bitmapKey, decoded);
                }
            }
            Utils.traceEndSection();

            mMainThreadHandler.sendEmptyMessage(MESSAGE_PHOTOS_LOADED);
        }

        protected String createInQuery(String value, int itemCount) {
            // Build first query
            StringBuilder query = new StringBuilder().append(value + " IN (");
            appendQuestionMarks(query, itemCount);
            query.append(')');
            return query.toString();
        }

        protected void appendQuestionMarks(StringBuilder query, int itemCount) {
            boolean first = true;
            for (int i = 0; i < itemCount; i++) {
                if (first) {
                    first = false;
                } else {
                    query.append(',');
                }
                query.append('?');
            }
        }
    }

    /**
     * An object to uniquely identify a combination of (Request + decoded size). Multiple requests
     * may require the same src image, but want to decode it into different sizes.
     */
    public static final class BitmapIdentifier {
        public final Object key;
        public final int w;
        public final int h;

        // OK to be static as long as all Requests are created on the same
        // thread
        private static final ImageCanvas.Dimensions sWorkDims = new ImageCanvas.Dimensions();

        public static BitmapIdentifier getBitmapKey(PhotoIdentifier id, ImageCanvas view,
                ImageCanvas.Dimensions dimensions) {
            final int width;
            final int height;
            if (dimensions != null) {
                width = dimensions.width;
                height = dimensions.height;
            } else {
                view.getDesiredDimensions(id.getKey(), sWorkDims);
                width = sWorkDims.width;
                height = sWorkDims.height;
            }
            return new BitmapIdentifier(id.getKey(), width, height);
        }

        public BitmapIdentifier(Object key, int w, int h) {
            this.key = key;
            this.w = w;
            this.h = h;
        }

        @Override
        public int hashCode() {
            int hash = 19;
            hash = 31 * hash + key.hashCode();
            hash = 31 * hash + w;
            hash = 31 * hash + h;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || obj.getClass() != getClass()) {
                return false;
            } else if (obj == this) {
                return true;
            }
            final BitmapIdentifier o = (BitmapIdentifier) obj;
            return Objects.equal(key, o.key) && w == o.w && h == o.h;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("{");
            sb.append(super.toString());
            sb.append(" key=");
            sb.append(key);
            sb.append(" w=");
            sb.append(w);
            sb.append(" h=");
            sb.append(h);
            sb.append("}");
            return sb.toString();
        }
    }

    /**
     * A holder for a contact photo request.
     */
    public final class Request implements Comparable<Request> {
        private final int mRequestedExtent;
        private final DefaultImageProvider mDefaultProvider;
        private final PhotoIdentifier mPhotoIdentifier;
        private final ImageCanvas mView;
        public final BitmapIdentifier bitmapKey;
        public final int viewGeneration;
        public int attempts;

        private Request(final PhotoIdentifier photoIdentifier,
                final DefaultImageProvider defaultProvider, final ImageCanvas view,
                final ImageCanvas.Dimensions dimensions) {
            mPhotoIdentifier = photoIdentifier;
            mRequestedExtent = -1;
            mDefaultProvider = defaultProvider;
            mView = view;
            viewGeneration = view.getGeneration();

            bitmapKey = BitmapIdentifier.getBitmapKey(photoIdentifier, mView, dimensions);
        }

        public ImageCanvas getView() {
            return mView;
        }

        public PhotoIdentifier getPhotoIdentifier() {
            return mPhotoIdentifier;
        }

        /**
         * @see PhotoIdentifier#getKey()
         */
        public Object getKey() {
            return mPhotoIdentifier.getKey();
        }

        @Override
        public int hashCode() {
            return getHash(mPhotoIdentifier, mView);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            final Request that = (Request) obj;
            if (mRequestedExtent != that.mRequestedExtent) return false;
            if (!Objects.equal(mPhotoIdentifier, that.mPhotoIdentifier)) return false;
            if (!Objects.equal(mView, that.mView)) return false;
            // Don't compare equality of mDarkTheme because it is only used in the default contact
            // photo case. When the contact does have a photo, the contact photo is the same
            // regardless of mDarkTheme, so we shouldn't need to put the photo request on the queue
            // twice.
            return true;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("{");
            sb.append(super.toString());
            sb.append(" key=");
            sb.append(getKey());
            sb.append(" id=");
            sb.append(mPhotoIdentifier);
            sb.append(" mView=");
            sb.append(mView);
            sb.append(" mExtent=");
            sb.append(mRequestedExtent);
            sb.append(" bitmapKey=");
            sb.append(bitmapKey);
            sb.append(" viewGeneration=");
            sb.append(viewGeneration);
            sb.append("}");
            return sb.toString();
        }

        public void applyDefaultImage() {
            if (mView.getGeneration() != viewGeneration) {
                // This can legitimately happen when an ImageCanvas is reused and re-purposed to
                // house a new set of images (e.g. by ListView recycling).
                // Ignore this now-stale request.
                if (DEBUG) {
                    LogUtils.d(TAG,
                            "ImageCanvas skipping applyDefaultImage; no longer contains" +
                            " item=%s canvas=%s", getKey(), mView);
                }
            }
            mDefaultProvider.applyDefaultImage(mPhotoIdentifier, mView, mRequestedExtent);
        }

        @Override
        public int compareTo(Request another) {
            // Hold off on loading Requests which have failed before so it don't hold up others
            if (attempts - another.attempts != 0) {
                return attempts - another.attempts;
            }
            return mPhotoIdentifier.compareTo(another.mPhotoIdentifier);
        }
    }
}
