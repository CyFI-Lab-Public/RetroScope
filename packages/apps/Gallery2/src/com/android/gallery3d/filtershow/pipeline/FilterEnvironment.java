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

package com.android.gallery3d.filtershow.pipeline;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.support.v8.renderscript.Allocation;

import com.android.gallery3d.app.Log;
import com.android.gallery3d.filtershow.cache.BitmapCache;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import com.android.gallery3d.filtershow.filters.FilterUserPresetRepresentation;
import com.android.gallery3d.filtershow.filters.FiltersManagerInterface;
import com.android.gallery3d.filtershow.filters.ImageFilter;

import java.lang.ref.WeakReference;
import java.util.HashMap;

public class FilterEnvironment {
    private static final String LOGTAG = "FilterEnvironment";
    private ImagePreset mImagePreset;
    private float mScaleFactor;
    private int mQuality;
    private FiltersManagerInterface mFiltersManager;
    private PipelineInterface mPipeline;
    private volatile boolean mStop = false;
    private BitmapCache mBitmapCache;

    public static final int QUALITY_ICON = 0;
    public static final int QUALITY_PREVIEW = 1;
    public static final int QUALITY_FINAL = 2;

    public synchronized boolean needsStop() {
        return mStop;
    }

    public synchronized void setStop(boolean stop) {
        this.mStop = stop;
    }

    private HashMap<Integer, Integer>
                    generalParameters = new HashMap<Integer, Integer>();

    public void setBitmapCache(BitmapCache cache) {
        mBitmapCache = cache;
    }

    public void cache(Buffer buffer) {
        mBitmapCache.cache(buffer);
    }

    public void cache(Bitmap bitmap) {
        mBitmapCache.cache(bitmap);
    }

    public Bitmap getBitmap(int w, int h, int type) {
        return mBitmapCache.getBitmap(w, h, type);
    }

    public Bitmap getBitmapCopy(Bitmap source, int type) {
        return mBitmapCache.getBitmapCopy(source, type);
    }

    public void setImagePreset(ImagePreset imagePreset) {
        mImagePreset = imagePreset;
    }

    public ImagePreset getImagePreset() {
        return mImagePreset;
    }

    public void setScaleFactor(float scaleFactor) {
        mScaleFactor = scaleFactor;
    }

    public float getScaleFactor() {
        return mScaleFactor;
    }

    public void setQuality(int quality) {
        mQuality = quality;
    }

    public int getQuality() {
        return mQuality;
    }

    public void setFiltersManager(FiltersManagerInterface filtersManager) {
        mFiltersManager = filtersManager;
    }

    public FiltersManagerInterface getFiltersManager() {
        return mFiltersManager;
    }

    public void applyRepresentation(FilterRepresentation representation,
                                    Allocation in, Allocation out) {
        ImageFilter filter = mFiltersManager.getFilterForRepresentation(representation);
        filter.useRepresentation(representation);
        filter.setEnvironment(this);
        if (filter.supportsAllocationInput()) {
            filter.apply(in, out);
        }
        filter.setGeneralParameters();
        filter.setEnvironment(null);
    }

    public Bitmap applyRepresentation(FilterRepresentation representation, Bitmap bitmap) {
        if (representation instanceof FilterUserPresetRepresentation) {
            // we allow instances of FilterUserPresetRepresentation in a preset only to know if one
            // has been applied (so we can show this in the UI). But as all the filters in them are
            // applied directly they do not themselves need to do any kind of filtering.
            return bitmap;
        }
        ImageFilter filter = mFiltersManager.getFilterForRepresentation(representation);
        if (filter == null){
            Log.e(LOGTAG,"No ImageFilter for "+representation.getSerializationName());
        }
        filter.useRepresentation(representation);
        filter.setEnvironment(this);
        Bitmap ret = filter.apply(bitmap, mScaleFactor, mQuality);
        if (bitmap != ret) {
            cache(bitmap);
        }
        filter.setGeneralParameters();
        filter.setEnvironment(null);
        return ret;
    }

    public PipelineInterface getPipeline() {
        return mPipeline;
    }

    public void setPipeline(PipelineInterface cachingPipeline) {
        mPipeline = cachingPipeline;
    }

    public synchronized void clearGeneralParameters() {
        generalParameters = null;
    }

    public synchronized Integer getGeneralParameter(int id) {
        if (generalParameters == null || !generalParameters.containsKey(id)) {
            return null;
        }
        return generalParameters.get(id);
    }

    public synchronized void setGeneralParameter(int id, int value) {
        if (generalParameters == null) {
            generalParameters = new HashMap<Integer, Integer>();
        }

        generalParameters.put(id, value);
    }

    public BitmapCache getBimapCache() {
        return mBitmapCache;
    }
}
