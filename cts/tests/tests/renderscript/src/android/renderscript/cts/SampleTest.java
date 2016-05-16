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

package android.renderscript.cts;

import com.android.cts.stub.R;

import android.graphics.Bitmap;
import android.renderscript.Allocation;
import android.renderscript.Allocation.MipmapControl;
import android.renderscript.Element;
import android.renderscript.RSIllegalArgumentException;
import android.renderscript.Sampler;
import android.renderscript.Type;
import android.renderscript.Type.CubemapFace;

public class SampleTest extends RSBaseCompute {

    ScriptC_sample mScript;

    Allocation mAlloc_RGBA_1D;
    Allocation mAlloc_RGBA_2D;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        Element format = Element.RGBA_8888(mRS);
        Type.Builder b = new Type.Builder(mRS, format);
        b.setMipmaps(true);
        mAlloc_RGBA_1D = Allocation.createTyped(mRS, b.setX(8).create(),
                                                Allocation.MipmapControl.MIPMAP_FULL,
                                                Allocation.USAGE_SCRIPT);
        mAlloc_RGBA_2D = Allocation.createTyped(mRS, b.setX(8).setY(8).create(),
                                                Allocation.MipmapControl.MIPMAP_FULL,
                                                Allocation.USAGE_SCRIPT);

        mScript = new ScriptC_sample(mRS, mRes, R.raw.sample);
        mScript.bind_gAllocPtr(mAlloc_RGBA_1D);
        mScript.invoke_init_RGBA(mAlloc_RGBA_1D);
        mScript.bind_gAllocPtr(mAlloc_RGBA_2D);
        mScript.invoke_init_RGBA(mAlloc_RGBA_2D);

        mScript.set_gNearest(Sampler.CLAMP_NEAREST(mRS));
        mScript.set_gLinear(Sampler.CLAMP_LINEAR(mRS));

        Sampler.Builder sb = new Sampler.Builder(mRS);
        sb.setMinification(Sampler.Value.LINEAR_MIP_NEAREST);
        mScript.set_gMipNearest(sb.create());

        mScript.set_gMipLinear(Sampler.CLAMP_LINEAR_MIP_LINEAR(mRS));
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testNearest() {
        mScript.invoke_test_RGBA(mAlloc_RGBA_1D, mAlloc_RGBA_2D);
        mRS.finish();
        checkForErrors();
        waitForMessage();
    }
}


