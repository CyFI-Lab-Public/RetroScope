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
import android.graphics.Bitmap;
import android.graphics.Rect;
import com.android.gallery3d.app.Log;
import com.android.gallery3d.filtershow.FilterShowActivity;
import com.android.gallery3d.filtershow.cache.BitmapCache;
import com.android.gallery3d.filtershow.filters.FiltersManager;
import com.android.gallery3d.filtershow.imageshow.MasterImage;

public class RenderingRequest {
    private static final String LOGTAG = "RenderingRequest";
    private boolean mIsDirect = false;
    private Bitmap mBitmap = null;
    private ImagePreset mImagePreset = null;
    private ImagePreset mOriginalImagePreset = null;
    private RenderingRequestCaller mCaller = null;
    private float mScaleFactor = 1.0f;
    private Rect mBounds = null;
    private Rect mDestination = null;
    private Rect mIconBounds = null;
    private int mType = FULL_RENDERING;
    public static final int FULL_RENDERING = 0;
    public static final int FILTERS_RENDERING = 1;
    public static final int GEOMETRY_RENDERING = 2;
    public static final int ICON_RENDERING = 3;
    public static final int PARTIAL_RENDERING = 4;
    public static final int HIGHRES_RENDERING = 5;
    public static final int STYLE_ICON_RENDERING = 6;

    private static final Bitmap.Config mConfig = Bitmap.Config.ARGB_8888;

    public static void post(Context context, Bitmap source, ImagePreset preset,
                            int type, RenderingRequestCaller caller) {
        RenderingRequest.post(context, source, preset, type, caller, null, null);
    }

    public static void post(Context context, Bitmap source, ImagePreset preset, int type,
                            RenderingRequestCaller caller, Rect bounds, Rect destination) {
        if (((type != PARTIAL_RENDERING && type != HIGHRES_RENDERING
                && type != GEOMETRY_RENDERING && type != FILTERS_RENDERING) && source == null)
                || preset == null || caller == null) {
            Log.v(LOGTAG, "something null: source: " + source
                    + " or preset: " + preset + " or caller: " + caller);
            return;
        }
        RenderingRequest request = new RenderingRequest();
        Bitmap bitmap = null;
        if (type == FULL_RENDERING
                || type == ICON_RENDERING
                || type == STYLE_ICON_RENDERING) {
            CachingPipeline pipeline = new CachingPipeline(
                    FiltersManager.getManager(), "Icon");
            bitmap = pipeline.renderGeometryIcon(source, preset);
        } else if (type != PARTIAL_RENDERING && type != HIGHRES_RENDERING
                && type != GEOMETRY_RENDERING && type != FILTERS_RENDERING) {
            bitmap = MasterImage.getImage().getBitmapCache().getBitmap(
                    source.getWidth(), source.getHeight(), BitmapCache.RENDERING_REQUEST);
        }

        request.setBitmap(bitmap);
        ImagePreset passedPreset = new ImagePreset(preset);
        request.setOriginalImagePreset(preset);
        request.setScaleFactor(MasterImage.getImage().getScaleFactor());

        if (type == PARTIAL_RENDERING) {
            request.setBounds(bounds);
            request.setDestination(destination);
            passedPreset.setPartialRendering(true, bounds);
        }

        request.setImagePreset(passedPreset);
        request.setType(type);
        request.setCaller(caller);
        request.post(context);
    }

    public static void postIconRequest(Context context, int w, int h,
                                       ImagePreset preset,
                                       RenderingRequestCaller caller) {
        if (preset == null || caller == null) {
            Log.v(LOGTAG, "something null, preset: "
                    + preset + " or caller: " + caller);
            return;
        }
        RenderingRequest request = new RenderingRequest();
        ImagePreset passedPreset = new ImagePreset(preset);
        request.setOriginalImagePreset(preset);
        request.setScaleFactor(MasterImage.getImage().getScaleFactor());
        request.setImagePreset(passedPreset);
        request.setType(RenderingRequest.ICON_RENDERING);
        request.setCaller(caller);
        request.setIconBounds(new Rect(0, 0, w, h));
        request.post(context);
    }

    public void post(Context context) {
        if (context instanceof FilterShowActivity) {
            FilterShowActivity activity = (FilterShowActivity) context;
            ProcessingService service = activity.getProcessingService();
            service.postRenderingRequest(this);
        }
    }

    public void markAvailable() {
        if (mBitmap == null || mImagePreset == null
                || mCaller == null) {
            return;
        }
        mCaller.available(this);
    }

    public boolean isDirect() {
        return mIsDirect;
    }

    public void setDirect(boolean isDirect) {
        mIsDirect = isDirect;
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        mBitmap = bitmap;
    }

    public ImagePreset getImagePreset() {
        return mImagePreset;
    }

    public void setImagePreset(ImagePreset imagePreset) {
        mImagePreset = imagePreset;
    }

    public int getType() {
        return mType;
    }

    public void setType(int type) {
        mType = type;
    }

    public void setCaller(RenderingRequestCaller caller) {
        mCaller = caller;
    }

    public Rect getBounds() {
        return mBounds;
    }

    public void setBounds(Rect bounds) {
        mBounds = bounds;
    }

    public void setScaleFactor(float scaleFactor) {
        mScaleFactor = scaleFactor;
    }

    public float getScaleFactor() {
        return mScaleFactor;
    }

    public Rect getDestination() {
        return mDestination;
    }

    public void setDestination(Rect destination) {
        mDestination = destination;
    }

    public void setIconBounds(Rect bounds) {
        mIconBounds = bounds;
    }

    public Rect getIconBounds() {
        return mIconBounds;
    }

    public ImagePreset getOriginalImagePreset() {
        return mOriginalImagePreset;
    }

    public void setOriginalImagePreset(ImagePreset originalImagePreset) {
        mOriginalImagePreset = originalImagePreset;
    }
}
