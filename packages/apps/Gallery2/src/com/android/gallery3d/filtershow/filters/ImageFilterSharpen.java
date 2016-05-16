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

import com.android.gallery3d.R;

public class ImageFilterSharpen extends ImageFilterRS {
    private static final String SERIALIZATION_NAME = "SHARPEN";
    private static final String LOGTAG = "ImageFilterSharpen";
    private ScriptC_convolve3x3 mScript;

    private FilterBasicRepresentation mParameters;

    public ImageFilterSharpen() {
        mName = "Sharpen";
    }

    public FilterRepresentation getDefaultRepresentation() {
        FilterRepresentation representation = new FilterBasicRepresentation("Sharpen", 0, 0, 100);
        representation.setSerializationName(SERIALIZATION_NAME);
        representation.setShowParameterValue(true);
        representation.setFilterClass(ImageFilterSharpen.class);
        representation.setTextId(R.string.sharpness);
        representation.setOverlayId(R.drawable.filtershow_button_colors_sharpen);
        representation.setEditorId(R.id.imageShow);
        representation.setSupportsPartialRendering(true);
        return representation;
    }

    public void useRepresentation(FilterRepresentation representation) {
        FilterBasicRepresentation parameters = (FilterBasicRepresentation) representation;
        mParameters = parameters;
    }

    @Override
    protected void resetAllocations() {
        // nothing to do
    }

    @Override
    public void resetScripts() {
        if (mScript != null) {
            mScript.destroy();
            mScript = null;
        }
    }

    @Override
    protected void createFilter(android.content.res.Resources res, float scaleFactor,
            int quality) {
        if (mScript == null) {
            mScript = new ScriptC_convolve3x3(getRenderScriptContext(), res, R.raw.convolve3x3);
        }
    }

    private void computeKernel() {
        float scaleFactor = getEnvironment().getScaleFactor();
        float p1 = mParameters.getValue() * scaleFactor;
        float value = p1 / 100.0f;
        float f[] = new float[9];
        float p = value;
        f[0] = -p;
        f[1] = -p;
        f[2] = -p;
        f[3] = -p;
        f[4] = 8 * p + 1;
        f[5] = -p;
        f[6] = -p;
        f[7] = -p;
        f[8] = -p;
        mScript.set_gCoeffs(f);
    }

    @Override
    protected void bindScriptValues() {
        int w = getInPixelsAllocation().getType().getX();
        int h = getInPixelsAllocation().getType().getY();
        mScript.set_gWidth(w);
        mScript.set_gHeight(h);
    }

    @Override
    protected void runFilter() {
        if (mParameters == null) {
            return;
        }
        computeKernel();
        mScript.set_gIn(getInPixelsAllocation());
        mScript.bind_gPixels(getInPixelsAllocation());
        mScript.forEach_root(getInPixelsAllocation(), getOutPixelsAllocation());
    }

}
