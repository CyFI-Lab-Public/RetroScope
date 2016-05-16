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
import android.util.Log;

import com.android.gallery3d.filtershow.cache.BitmapCache;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import com.android.gallery3d.filtershow.imageshow.GeometryMathUtils;

import java.util.ArrayList;
import java.util.Vector;

public class CacheProcessing {
    private static final String LOGTAG = "CacheProcessing";
    private static final boolean DEBUG = false;
    private static final boolean NO_CACHING = false;
    private Vector<CacheStep> mSteps = new Vector<CacheStep>();

    static class CacheStep {
        ArrayList<FilterRepresentation> representations;
        Bitmap cache;

        public CacheStep() {
            representations = new ArrayList<FilterRepresentation>();
        }

        public void add(FilterRepresentation representation) {
            representations.add(representation);
        }

        public boolean canMergeWith(FilterRepresentation representation) {
            for (FilterRepresentation rep : representations) {
                if (!rep.canMergeWith(representation)) {
                    return false;
                }
            }
            return true;
        }

        public boolean equals(CacheStep step) {
            if (representations.size() != step.representations.size()) {
                return false;
            }
            for (int i = 0; i < representations.size(); i++) {
                FilterRepresentation r1 = representations.get(i);
                FilterRepresentation r2 = step.representations.get(i);
                if (!r1.equals(r2)) {
                    return false;
                }
            }
            return true;
        }

        public static Vector<CacheStep> buildSteps(Vector<FilterRepresentation> filters) {
            Vector<CacheStep> steps = new Vector<CacheStep>();
            CacheStep step = new CacheStep();
            for (int i = 0; i < filters.size(); i++) {
                FilterRepresentation representation = filters.elementAt(i);
                if (step.canMergeWith(representation)) {
                    step.add(representation.copy());
                } else {
                    steps.add(step);
                    step = new CacheStep();
                    step.add(representation.copy());
                }
            }
            steps.add(step);
            return steps;
        }

        public String getName() {
            if (representations.size() > 0) {
                return representations.get(0).getName();
            }
            return "EMPTY";
        }

        public Bitmap apply(FilterEnvironment environment, Bitmap cacheBitmap) {
            boolean onlyGeometry = true;
            Bitmap source = cacheBitmap;
            for (FilterRepresentation representation : representations) {
                if (representation.getFilterType() != FilterRepresentation.TYPE_GEOMETRY) {
                    onlyGeometry = false;
                    break;
                }
            }
            if (onlyGeometry) {
                ArrayList<FilterRepresentation> geometry = new ArrayList<FilterRepresentation>();
                for (FilterRepresentation representation : representations) {
                    geometry.add(representation);
                }
                if (DEBUG) {
                    Log.v(LOGTAG, "Apply geometry to bitmap " + cacheBitmap);
                }
                cacheBitmap = GeometryMathUtils.applyGeometryRepresentations(geometry, cacheBitmap);
            } else {
                for (FilterRepresentation representation : representations) {
                    if (DEBUG) {
                        Log.v(LOGTAG, "Apply " + representation.getSerializationName()
                                + " to bitmap " + cacheBitmap);
                    }
                    cacheBitmap = environment.applyRepresentation(representation, cacheBitmap);
                }
            }
            if (cacheBitmap != source) {
                environment.cache(source);
            }
            if (DEBUG) {
                Log.v(LOGTAG, "Apply returns bitmap " + cacheBitmap);
            }
            return cacheBitmap;
        }
    }

    public Bitmap process(Bitmap originalBitmap,
                          Vector<FilterRepresentation> filters,
                          FilterEnvironment environment) {

        if (filters.size() == 0) {
            return environment.getBitmapCopy(originalBitmap, BitmapCache.PREVIEW_CACHE_NO_FILTERS);
        }

        environment.getBimapCache().setCacheProcessing(this);

        if (DEBUG) {
            displayFilters(filters);
        }
        Vector<CacheStep> steps = CacheStep.buildSteps(filters);
        // New set of filters, let's clear the cache and rebuild it.
        if (steps.size() != mSteps.size()) {
            mSteps = steps;
        }

        if (DEBUG) {
            displaySteps(mSteps);
        }

        // First, let's find how similar we are in our cache
        // compared to the current list of filters
        int similarUpToIndex = -1;
        boolean similar = true;
        for (int i = 0; i < steps.size(); i++) {
            CacheStep newStep = steps.elementAt(i);
            CacheStep cacheStep = mSteps.elementAt(i);
            if (similar) {
                similar = newStep.equals(cacheStep);
            }
            if (similar) {
                similarUpToIndex = i;
            } else {
                mSteps.remove(i);
                mSteps.insertElementAt(newStep, i);
                environment.cache(cacheStep.cache);
            }
        }
        if (DEBUG) {
            Log.v(LOGTAG, "similar up to index " + similarUpToIndex);
        }

        // Now, let's get the earliest cached result in our pipeline
        Bitmap cacheBitmap = null;
        int findBaseImageIndex = similarUpToIndex;
        if (findBaseImageIndex > -1) {
            while (findBaseImageIndex > 0
                    && mSteps.elementAt(findBaseImageIndex).cache == null) {
                findBaseImageIndex--;
            }
            cacheBitmap = mSteps.elementAt(findBaseImageIndex).cache;
        }

        if (DEBUG) {
            Log.v(LOGTAG, "found baseImageIndex: " + findBaseImageIndex + " max is "
                    + mSteps.size() + " cacheBitmap: " + cacheBitmap);
        }

        if (NO_CACHING) {
            cacheBitmap = environment.getBitmapCopy(originalBitmap,
                    BitmapCache.PREVIEW_CACHE_NO_ROOT);
            for (int i = 0; i < mSteps.size(); i++) {
                CacheStep step = mSteps.elementAt(i);
                Bitmap prev = cacheBitmap;
                cacheBitmap = step.apply(environment, cacheBitmap);
                if (prev != cacheBitmap) {
                    environment.cache(prev);
                }
            }
            return cacheBitmap;
        }

        Bitmap originalCopy = null;
        int lastPositionCached = -1;
        for (int i = findBaseImageIndex; i < mSteps.size(); i++) {
            if (i == -1 || cacheBitmap == null) {
                cacheBitmap = environment.getBitmapCopy(originalBitmap,
                        BitmapCache.PREVIEW_CACHE_NO_ROOT);
                originalCopy = cacheBitmap;
                if (DEBUG) {
                    Log.v(LOGTAG, "i: " + i + " cacheBitmap: " + cacheBitmap + " w: "
                            + cacheBitmap.getWidth() + " h: " + cacheBitmap.getHeight()
                            + " got from original Bitmap: " + originalBitmap + " w: "
                            + originalBitmap.getWidth() + " h: " + originalBitmap.getHeight());
                }
                if (i == -1) {
                    continue;
                }
            }
            CacheStep step = mSteps.elementAt(i);
            if (step.cache == null) {
                if (DEBUG) {
                    Log.v(LOGTAG, "i: " + i + " get new copy for cacheBitmap "
                            + cacheBitmap + " apply...");
                }
                cacheBitmap = environment.getBitmapCopy(cacheBitmap, BitmapCache.PREVIEW_CACHE);
                cacheBitmap = step.apply(environment, cacheBitmap);
                step.cache = cacheBitmap;
                lastPositionCached = i;
            }
        }
        environment.cache(originalCopy);

        if (DEBUG) {
            Log.v(LOGTAG, "now let's cleanup the cache...");
            displayNbBitmapsInCache();
        }

        // Let's see if we can cleanup the cache for unused bitmaps
        for (int i = 0; i < similarUpToIndex; i++) {
            CacheStep currentStep = mSteps.elementAt(i);
            Bitmap bitmap = currentStep.cache;
            currentStep.cache = null;
            environment.cache(bitmap);
        }

        if (DEBUG) {
            Log.v(LOGTAG, "cleanup done...");
            displayNbBitmapsInCache();
        }
        if (lastPositionCached != -1) {
            // The last element will never be reused, remove it from the cache.
            mSteps.elementAt(lastPositionCached).cache = null;
        }
        if (contains(cacheBitmap)) {
            return environment.getBitmapCopy(cacheBitmap, BitmapCache.PREVIEW_CACHE_NO_APPLY);
        }
        return cacheBitmap;
    }

    public boolean contains(Bitmap bitmap) {
        for (int i = 0; i < mSteps.size(); i++) {
            if (mSteps.elementAt(i).cache == bitmap) {
                return true;
            }
        }
        return false;
    }

    private void displayFilters(Vector<FilterRepresentation> filters) {
        Log.v(LOGTAG, "------>>> Filters received");
        for (int i = 0; i < filters.size(); i++) {
            FilterRepresentation filter = filters.elementAt(i);
            Log.v(LOGTAG, "[" + i + "] - " + filter.getName());
        }
        Log.v(LOGTAG, "<<<------");
    }

    private void displaySteps(Vector<CacheStep> filters) {
        Log.v(LOGTAG, "------>>>");
        for (int i = 0; i < filters.size(); i++) {
            CacheStep newStep = filters.elementAt(i);
            CacheStep step = mSteps.elementAt(i);
            boolean similar = step.equals(newStep);
            Log.v(LOGTAG, "[" + i + "] - " + step.getName()
                    + " similar rep ? " + (similar ? "YES" : "NO")
                    + " -- bitmap: " + step.cache);
        }
        Log.v(LOGTAG, "<<<------");
    }

    private void displayNbBitmapsInCache() {
        int nbBitmapsCached = 0;
        for (int i = 0; i < mSteps.size(); i++) {
            CacheStep step = mSteps.elementAt(i);
            if (step.cache != null) {
                nbBitmapsCached++;
            }
        }
        Log.v(LOGTAG, "nb bitmaps in cache: " + nbBitmapsCached + " / " + mSteps.size());
    }

}
