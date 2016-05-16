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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.RenderScript;
import android.util.Log;

import com.android.gallery3d.filtershow.cache.BitmapCache;
import com.android.gallery3d.filtershow.cache.ImageLoader;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import com.android.gallery3d.filtershow.filters.FiltersManager;
import com.android.gallery3d.filtershow.imageshow.GeometryMathUtils;
import com.android.gallery3d.filtershow.imageshow.MasterImage;

import java.util.Vector;

public class CachingPipeline implements PipelineInterface {
    private static final String LOGTAG = "CachingPipeline";
    private boolean DEBUG = false;

    private static final Bitmap.Config BITMAP_CONFIG = Bitmap.Config.ARGB_8888;

    private static volatile RenderScript sRS = null;

    private FiltersManager mFiltersManager = null;
    private volatile Bitmap mOriginalBitmap = null;
    private volatile Bitmap mResizedOriginalBitmap = null;

    private FilterEnvironment mEnvironment = new FilterEnvironment();
    private CacheProcessing mCachedProcessing = new CacheProcessing();


    private volatile Allocation mOriginalAllocation = null;
    private volatile Allocation mFiltersOnlyOriginalAllocation =  null;

    protected volatile Allocation mInPixelsAllocation;
    protected volatile Allocation mOutPixelsAllocation;
    private volatile int mWidth = 0;
    private volatile int mHeight = 0;

    private volatile float mPreviewScaleFactor = 1.0f;
    private volatile float mHighResPreviewScaleFactor = 1.0f;
    private volatile String mName = "";

    public CachingPipeline(FiltersManager filtersManager, String name) {
        mFiltersManager = filtersManager;
        mName = name;
    }

    public static synchronized RenderScript getRenderScriptContext() {
        return sRS;
    }

    public static synchronized void createRenderscriptContext(Context context) {
        if (sRS != null) {
            Log.w(LOGTAG, "A prior RS context exists when calling setRenderScriptContext");
            destroyRenderScriptContext();
        }
        sRS = RenderScript.create(context);
    }

    public static synchronized void destroyRenderScriptContext() {
        if (sRS != null) {
            sRS.destroy();
        }
        sRS = null;
    }

    public void stop() {
        mEnvironment.setStop(true);
    }

    public synchronized void reset() {
        synchronized (CachingPipeline.class) {
            if (getRenderScriptContext() == null) {
                return;
            }
            mOriginalBitmap = null; // just a reference to the bitmap in ImageLoader
            if (mResizedOriginalBitmap != null) {
                mResizedOriginalBitmap.recycle();
                mResizedOriginalBitmap = null;
            }
            if (mOriginalAllocation != null) {
                mOriginalAllocation.destroy();
                mOriginalAllocation = null;
            }
            if (mFiltersOnlyOriginalAllocation != null) {
                mFiltersOnlyOriginalAllocation.destroy();
                mFiltersOnlyOriginalAllocation = null;
            }
            mPreviewScaleFactor = 1.0f;
            mHighResPreviewScaleFactor = 1.0f;

            destroyPixelAllocations();
        }
    }

    public Resources getResources() {
        return sRS.getApplicationContext().getResources();
    }

    private synchronized void destroyPixelAllocations() {
        if (DEBUG) {
            Log.v(LOGTAG, "destroyPixelAllocations in " + getName());
        }
        if (mInPixelsAllocation != null) {
            mInPixelsAllocation.destroy();
            mInPixelsAllocation = null;
        }
        if (mOutPixelsAllocation != null) {
            mOutPixelsAllocation.destroy();
            mOutPixelsAllocation = null;
        }
        mWidth = 0;
        mHeight = 0;
    }

    private String getType(RenderingRequest request) {
        if (request.getType() == RenderingRequest.ICON_RENDERING) {
            return "ICON_RENDERING";
        }
        if (request.getType() == RenderingRequest.FILTERS_RENDERING) {
            return "FILTERS_RENDERING";
        }
        if (request.getType() == RenderingRequest.FULL_RENDERING) {
            return "FULL_RENDERING";
        }
        if (request.getType() == RenderingRequest.GEOMETRY_RENDERING) {
            return "GEOMETRY_RENDERING";
        }
        if (request.getType() == RenderingRequest.PARTIAL_RENDERING) {
            return "PARTIAL_RENDERING";
        }
        if (request.getType() == RenderingRequest.HIGHRES_RENDERING) {
            return "HIGHRES_RENDERING";
        }
        return "UNKNOWN TYPE!";
    }

    private void setupEnvironment(ImagePreset preset, boolean highResPreview) {
        mEnvironment.setPipeline(this);
        mEnvironment.setFiltersManager(mFiltersManager);
        mEnvironment.setBitmapCache(MasterImage.getImage().getBitmapCache());
        if (highResPreview) {
            mEnvironment.setScaleFactor(mHighResPreviewScaleFactor);
        } else {
            mEnvironment.setScaleFactor(mPreviewScaleFactor);
        }
        mEnvironment.setQuality(FilterEnvironment.QUALITY_PREVIEW);
        mEnvironment.setImagePreset(preset);
        mEnvironment.setStop(false);
    }

    public void setOriginal(Bitmap bitmap) {
        mOriginalBitmap = bitmap;
        Log.v(LOGTAG,"setOriginal, size " + bitmap.getWidth() + " x " + bitmap.getHeight());
        ImagePreset preset = MasterImage.getImage().getPreset();
        setupEnvironment(preset, false);
        updateOriginalAllocation(preset);
    }

    private synchronized boolean updateOriginalAllocation(ImagePreset preset) {
        if (preset == null) {
            return false;
        }
        Bitmap originalBitmap = mOriginalBitmap;

        if (originalBitmap == null) {
            return false;
        }

        RenderScript RS = getRenderScriptContext();

        Allocation filtersOnlyOriginalAllocation = mFiltersOnlyOriginalAllocation;
        mFiltersOnlyOriginalAllocation = Allocation.createFromBitmap(RS, originalBitmap,
                Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
        if (filtersOnlyOriginalAllocation != null) {
            filtersOnlyOriginalAllocation.destroy();
        }

        Allocation originalAllocation = mOriginalAllocation;
        mResizedOriginalBitmap = preset.applyGeometry(originalBitmap, mEnvironment);
        mOriginalAllocation = Allocation.createFromBitmap(RS, mResizedOriginalBitmap,
                Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
        if (originalAllocation != null) {
            originalAllocation.destroy();
        }

        return true;
    }

    public void renderHighres(RenderingRequest request) {
        synchronized (CachingPipeline.class) {
            if (getRenderScriptContext() == null) {
                return;
            }
            ImagePreset preset = request.getImagePreset();
            setupEnvironment(preset, false);
            Bitmap bitmap = MasterImage.getImage().getOriginalBitmapHighres();
            if (bitmap == null) {
                return;
            }
            bitmap = mEnvironment.getBitmapCopy(bitmap, BitmapCache.HIGHRES);
            bitmap = preset.applyGeometry(bitmap, mEnvironment);

            mEnvironment.setQuality(FilterEnvironment.QUALITY_PREVIEW);
            Bitmap bmp = preset.apply(bitmap, mEnvironment);
            if (!mEnvironment.needsStop()) {
                request.setBitmap(bmp);
            } else {
                mEnvironment.cache(bmp);
            }
            mFiltersManager.freeFilterResources(preset);
        }
    }

    public void renderGeometry(RenderingRequest request) {
        synchronized (CachingPipeline.class) {
            if (getRenderScriptContext() == null) {
                return;
            }
            ImagePreset preset = request.getImagePreset();
            setupEnvironment(preset, false);
            Bitmap bitmap = MasterImage.getImage().getOriginalBitmapHighres();
            if (bitmap == null) {
                return;
            }
            bitmap = mEnvironment.getBitmapCopy(bitmap, BitmapCache.GEOMETRY);
            bitmap = preset.applyGeometry(bitmap, mEnvironment);
            if (!mEnvironment.needsStop()) {
                request.setBitmap(bitmap);
            } else {
                mEnvironment.cache(bitmap);
            }
            mFiltersManager.freeFilterResources(preset);
        }
    }

    public void renderFilters(RenderingRequest request) {
        synchronized (CachingPipeline.class) {
            if (getRenderScriptContext() == null) {
                return;
            }
            ImagePreset preset = request.getImagePreset();
            setupEnvironment(preset, false);
            Bitmap bitmap = MasterImage.getImage().getOriginalBitmapHighres();
            if (bitmap == null) {
                return;
            }
            bitmap = mEnvironment.getBitmapCopy(bitmap, BitmapCache.FILTERS);
            bitmap = preset.apply(bitmap, mEnvironment);
            if (!mEnvironment.needsStop()) {
                request.setBitmap(bitmap);
            } else {
                mEnvironment.cache(bitmap);
            }
            mFiltersManager.freeFilterResources(preset);
        }
    }

    public synchronized void render(RenderingRequest request) {
        // TODO: cleanup/remove GEOMETRY / FILTERS paths
        synchronized (CachingPipeline.class) {
            if (getRenderScriptContext() == null) {
                return;
            }
            if ((request.getType() != RenderingRequest.PARTIAL_RENDERING
                  && request.getType() != RenderingRequest.ICON_RENDERING
                    && request.getBitmap() == null)
                    || request.getImagePreset() == null) {
                return;
            }

            if (DEBUG) {
                Log.v(LOGTAG, "render image of type " + getType(request));
            }

            Bitmap bitmap = request.getBitmap();
            ImagePreset preset = request.getImagePreset();
            setupEnvironment(preset, true);
            mFiltersManager.freeFilterResources(preset);

            if (request.getType() == RenderingRequest.PARTIAL_RENDERING) {
                MasterImage master = MasterImage.getImage();
                bitmap = ImageLoader.getScaleOneImageForPreset(master.getActivity(),
                        mEnvironment.getBimapCache(),
                        master.getUri(), request.getBounds(),
                        request.getDestination());
                if (bitmap == null) {
                    Log.w(LOGTAG, "could not get bitmap for: " + getType(request));
                    return;
                }
            }

            if (request.getType() == RenderingRequest.FULL_RENDERING
                    || request.getType() == RenderingRequest.GEOMETRY_RENDERING
                    || request.getType() == RenderingRequest.FILTERS_RENDERING) {
                updateOriginalAllocation(preset);
            }

            if (DEBUG && bitmap != null) {
                Log.v(LOGTAG, "after update, req bitmap (" + bitmap.getWidth() + "x" + bitmap.getHeight()
                        + " ? resizeOriginal (" + mResizedOriginalBitmap.getWidth() + "x"
                        + mResizedOriginalBitmap.getHeight());
            }

            if (request.getType() == RenderingRequest.FULL_RENDERING
                    || request.getType() == RenderingRequest.GEOMETRY_RENDERING) {
                mOriginalAllocation.copyTo(bitmap);
            } else if (request.getType() == RenderingRequest.FILTERS_RENDERING) {
                mFiltersOnlyOriginalAllocation.copyTo(bitmap);
            }

            if (request.getType() == RenderingRequest.FULL_RENDERING
                    || request.getType() == RenderingRequest.FILTERS_RENDERING
                    || request.getType() == RenderingRequest.ICON_RENDERING
                    || request.getType() == RenderingRequest.PARTIAL_RENDERING
                    || request.getType() == RenderingRequest.STYLE_ICON_RENDERING) {

                if (request.getType() == RenderingRequest.ICON_RENDERING) {
                    mEnvironment.setQuality(FilterEnvironment.QUALITY_ICON);
                } else {
                    mEnvironment.setQuality(FilterEnvironment.QUALITY_PREVIEW);
                }

                if (request.getType() == RenderingRequest.ICON_RENDERING) {
                    Rect iconBounds = request.getIconBounds();
                    Bitmap source = MasterImage.getImage().getThumbnailBitmap();
                    if (iconBounds.width() > source.getWidth() * 2) {
                        source = MasterImage.getImage().getLargeThumbnailBitmap();
                    }
                    if (iconBounds != null) {
                        bitmap = mEnvironment.getBitmap(iconBounds.width(),
                                iconBounds.height(), BitmapCache.ICON);
                        Canvas canvas = new Canvas(bitmap);
                        Matrix m = new Matrix();
                        float minSize = Math.min(source.getWidth(), source.getHeight());
                        float maxSize = Math.max(iconBounds.width(), iconBounds.height());
                        float scale = maxSize / minSize;
                        m.setScale(scale, scale);
                        float dx = (iconBounds.width() - (source.getWidth() * scale))/2.0f;
                        float dy = (iconBounds.height() - (source.getHeight() * scale))/2.0f;
                        m.postTranslate(dx, dy);
                        canvas.drawBitmap(source, m, new Paint(Paint.FILTER_BITMAP_FLAG));
                    } else {
                        bitmap = mEnvironment.getBitmapCopy(source, BitmapCache.ICON);
                    }
                }
                Bitmap bmp = preset.apply(bitmap, mEnvironment);
                if (!mEnvironment.needsStop()) {
                    request.setBitmap(bmp);
                }
                mFiltersManager.freeFilterResources(preset);
            }
        }
    }

    public synchronized void renderImage(ImagePreset preset, Allocation in, Allocation out) {
        synchronized (CachingPipeline.class) {
            if (getRenderScriptContext() == null) {
                return;
            }
            setupEnvironment(preset, false);
            mFiltersManager.freeFilterResources(preset);
            preset.applyFilters(-1, -1, in, out, mEnvironment);
            boolean copyOut = false;
            if (preset.nbFilters() > 0) {
                copyOut = true;
            }
            preset.applyBorder(in, out, copyOut, mEnvironment);
        }
    }

    public synchronized Bitmap renderFinalImage(Bitmap bitmap, ImagePreset preset) {
        synchronized (CachingPipeline.class) {
            if (getRenderScriptContext() == null) {
                return bitmap;
            }
            setupEnvironment(preset, false);
            mEnvironment.setQuality(FilterEnvironment.QUALITY_FINAL);
            mEnvironment.setScaleFactor(1.0f);
            mFiltersManager.freeFilterResources(preset);
            bitmap = preset.applyGeometry(bitmap, mEnvironment);
            bitmap = preset.apply(bitmap, mEnvironment);
            return bitmap;
        }
    }

    public Bitmap renderGeometryIcon(Bitmap bitmap, ImagePreset preset) {
        return GeometryMathUtils.applyGeometryRepresentations(preset.getGeometryFilters(), bitmap);
    }

    public void compute(SharedBuffer buffer, ImagePreset preset, int type) {
        if (getRenderScriptContext() == null) {
            return;
        }
        setupEnvironment(preset, false);
        Vector<FilterRepresentation> filters = preset.getFilters();
        Bitmap result = mCachedProcessing.process(mOriginalBitmap, filters, mEnvironment);
        buffer.setProducer(result);
        mEnvironment.cache(result);
    }

    public boolean needsRepaint() {
        SharedBuffer buffer = MasterImage.getImage().getPreviewBuffer();
        return buffer.checkRepaintNeeded();
    }

    public void setPreviewScaleFactor(float previewScaleFactor) {
        mPreviewScaleFactor = previewScaleFactor;
    }

    public void setHighResPreviewScaleFactor(float highResPreviewScaleFactor) {
        mHighResPreviewScaleFactor = highResPreviewScaleFactor;
    }

    public synchronized boolean isInitialized() {
        return getRenderScriptContext() != null && mOriginalBitmap != null;
    }

    public boolean prepareRenderscriptAllocations(Bitmap bitmap) {
        RenderScript RS = getRenderScriptContext();
        boolean needsUpdate = false;
        if (mOutPixelsAllocation == null || mInPixelsAllocation == null ||
                bitmap.getWidth() != mWidth || bitmap.getHeight() != mHeight) {
            destroyPixelAllocations();
            Bitmap bitmapBuffer = bitmap;
            if (bitmap.getConfig() == null || bitmap.getConfig() != BITMAP_CONFIG) {
                bitmapBuffer = bitmap.copy(BITMAP_CONFIG, true);
            }
            mOutPixelsAllocation = Allocation.createFromBitmap(RS, bitmapBuffer,
                    Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
            mInPixelsAllocation = Allocation.createTyped(RS,
                    mOutPixelsAllocation.getType());
            needsUpdate = true;
        }
        if (RS != null) {
            mInPixelsAllocation.copyFrom(bitmap);
        }
        if (bitmap.getWidth() != mWidth
                || bitmap.getHeight() != mHeight) {
            mWidth = bitmap.getWidth();
            mHeight = bitmap.getHeight();
            needsUpdate = true;
        }
        if (DEBUG) {
            Log.v(LOGTAG, "prepareRenderscriptAllocations: " + needsUpdate + " in " + getName());
        }
        return needsUpdate;
    }

    public synchronized Allocation getInPixelsAllocation() {
        return mInPixelsAllocation;
    }

    public synchronized Allocation getOutPixelsAllocation() {
        return mOutPixelsAllocation;
    }

    public String getName() {
        return mName;
    }

    public RenderScript getRSContext() {
        return CachingPipeline.getRenderScriptContext();
    }
}
