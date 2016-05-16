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

package com.android.gallery3d.filtershow.filters;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.imageshow.MasterImage;
import com.android.gallery3d.filtershow.pipeline.FilterEnvironment;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.Script.LaunchOptions;
import android.support.v8.renderscript.Type;
import android.util.Log;

public class ImageFilterVignette extends ImageFilterRS {
    private static final String LOGTAG = "ImageFilterVignette";
    private Bitmap mOverlayBitmap;
    private ScriptC_vignette mScript;
    FilterVignetteRepresentation mParameters;
    public static final int MODE_VIGNETTE = FilterVignetteRepresentation.MODE_VIGNETTE;
    public static final int MODE_EXPOSURE = FilterVignetteRepresentation.MODE_EXPOSURE;
    public static final int MODE_SATURATION = FilterVignetteRepresentation.MODE_SATURATION;
    public static final int MODE_CONTRAST = FilterVignetteRepresentation.MODE_CONTRAST;
    public static final int MODE_FALLOFF = FilterVignetteRepresentation.MODE_FALLOFF;

    public ImageFilterVignette() {
        mName = "Vignette";
    }

    @Override
    public FilterRepresentation getDefaultRepresentation() {
        FilterVignetteRepresentation representation = new FilterVignetteRepresentation();
        return representation;
    }

    @Override
    public void useRepresentation(FilterRepresentation representation) {
        mParameters = (FilterVignetteRepresentation) representation;
    }

    native protected void nativeApplyFilter(
            Bitmap bitmap, int w, int h, int cx, int cy, float radx, float rady,
            float strength, float finalValue);

    private float calcRadius(float cx, float cy, int w, int h) {
        float d = cx;
        if (d < (w - cx)) {
            d = w - cx;
        }
        if (d < cy) {
            d = cy;
        }
        if (d < (h - cy)) {
            d = h - cy;
        }
        return d * d * 2.0f;
    }

    @Override
    protected void createFilter(Resources res, float scaleFactor, int quality) {
        RenderScript rsCtx = getRenderScriptContext();

        mScript = new ScriptC_vignette(rsCtx, res, R.raw.vignette);
    }

    @Override
    protected void runFilter() {

        int w = getInPixelsAllocation().getType().getX();
        int h = getInPixelsAllocation().getType().getY();

        float cx = w / 2;
        float cy = h / 2;
        float r = calcRadius(cx, cy, w, h);
        float rx = r;
        float ry = r;

        float[]c = new float[2];
        if (mParameters.isCenterSet()) {
            Matrix m = getOriginalToScreenMatrix(w, h);
            Rect bounds = MasterImage.getImage().getOriginalBounds();
            c[0] = bounds.right * mParameters.getCenterX();
            c[1] = bounds.bottom * mParameters.getCenterY();
            m.mapPoints(c);
            cx = c[0];
            cy = c[1];
            c[0] = bounds.right * mParameters.getRadiusX();
            c[1] = bounds.bottom * mParameters.getRadiusY();
            m.mapVectors(c);
            rx = c[0];
            ry = c[1];
        }

        mScript.set_inputWidth(w);
        mScript.set_inputHeight(h);
        int v = mParameters.getValue(MODE_VIGNETTE);
        mScript.set_finalSubtract((v < 0) ? v : 0);
        mScript.set_finalBright((v > 0) ? -v : 0);
        mScript.set_finalSaturation(mParameters.getValue(MODE_SATURATION));
        mScript.set_finalContrast(mParameters.getValue(MODE_CONTRAST));
        mScript.set_centerx(cx);
        mScript.set_centery(cy);
        mScript.set_radiusx(rx);
        mScript.set_radiusy(ry);
        mScript.set_strength(mParameters.getValue(MODE_FALLOFF)/10.f);
        mScript.invoke_setupVignetteParams();
        mScript.forEach_vignette(getInPixelsAllocation(), getOutPixelsAllocation());
    }

    @Override
    public Bitmap apply(Bitmap bitmap, float scaleFactor, int quality) {
        if (SIMPLE_ICONS && FilterEnvironment.QUALITY_ICON == quality) {
            if (mOverlayBitmap == null) {
                Resources res = getEnvironment().getPipeline().getResources();
                mOverlayBitmap = IconUtilities.getFXBitmap(res,
                        R.drawable.filtershow_icon_vignette);
            }

            Canvas c = new Canvas(bitmap);
            int dim = Math.max(bitmap.getWidth(), bitmap.getHeight());
            Rect r = new Rect(0, 0, dim, dim);
            c.drawBitmap(mOverlayBitmap, null, r, null);
            return bitmap;
        }
        Bitmap ret = super.apply(bitmap, scaleFactor, quality);
        return bitmap;
    }


    @Override
    protected void resetAllocations() {

    }

    @Override
    public void resetScripts() {

    }

    @Override
    protected void bindScriptValues() {
        int width = getInPixelsAllocation().getType().getX();
        int height = getInPixelsAllocation().getType().getY();
        mScript.set_inputWidth(width);
        mScript.set_inputHeight(height);
    }
}
