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

package com.android.wallpaper.galaxy;

import android.renderscript.*;
import android.renderscript.Mesh.Primitive;
import static android.renderscript.ProgramStore.DepthFunc.*;
import static android.renderscript.ProgramStore.BlendDstFunc;
import static android.renderscript.ProgramStore.BlendSrcFunc;
import static android.renderscript.Element.*;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.util.TimeZone;

import com.android.wallpaper.R;
import com.android.wallpaper.RenderScriptScene;

class GalaxyRS extends RenderScriptScene {
    private static final int PARTICLES_COUNT = 12000;
    private final BitmapFactory.Options mOptionsARGB = new BitmapFactory.Options();
    private ProgramVertexFixedFunction.Constants mPvOrthoAlloc;
    private ProgramVertexFixedFunction.Constants mPvProjectionAlloc;
    private ScriptField_VpConsts mPvStarAlloc;
    private Mesh mParticlesMesh;
    private ScriptC_galaxy mScript;

    GalaxyRS(int width, int height) {
        super(width, height);

        mOptionsARGB.inScaled = false;
        mOptionsARGB.inPreferredConfig = Bitmap.Config.ARGB_8888;
    }

    @Override
    protected ScriptC createScript() {
        mScript = new ScriptC_galaxy(mRS, mResources, R.raw.galaxy);
        mScript.set_gIsPreview(isPreview() ? 1 : 0);
        if (isPreview()) {
            mScript.set_gXOffset(0.5f);
        }


        createParticlesMesh();
        createProgramVertex();
        createProgramRaster();
        createProgramFragmentStore();
        createProgramFragment();
        loadTextures();

        mScript.setTimeZone(TimeZone.getDefault().getID());
        return mScript;
    }

    private void createParticlesMesh() {
        ScriptField_Particle p = new ScriptField_Particle(mRS, PARTICLES_COUNT);

        final Mesh.AllocationBuilder meshBuilder = new Mesh.AllocationBuilder(mRS);
        meshBuilder.addVertexAllocation(p.getAllocation());
        final int vertexSlot = meshBuilder.getCurrentVertexTypeIndex();
        meshBuilder.addIndexSetType(Primitive.POINT);
        mParticlesMesh = meshBuilder.create();

        mScript.set_gParticlesMesh(mParticlesMesh);
        mScript.bind_Particles(p);
    }

    private Matrix4f getProjectionNormalized(int w, int h) {
        // range -1,1 in the narrow axis at z = 0.
        Matrix4f m1 = new Matrix4f();
        Matrix4f m2 = new Matrix4f();

        if(w > h) {
            float aspect = ((float)w) / h;
            m1.loadFrustum(-aspect,aspect,  -1,1,  1,100);
        } else {
            float aspect = ((float)h) / w;
            m1.loadFrustum(-1,1, -aspect,aspect, 1,100);
        }

        m2.loadRotate(180, 0, 1, 0);
        m1.loadMultiply(m1, m2);

        m2.loadScale(-2, 2, 1);
        m1.loadMultiply(m1, m2);

        m2.loadTranslate(0, 0, 2);
        m1.loadMultiply(m1, m2);
        return m1;
    }

    private void updateProjectionMatrices() {
        Matrix4f proj = new Matrix4f();
        proj.loadOrthoWindow(mWidth, mHeight);
        mPvOrthoAlloc.setProjection(proj);

        Matrix4f projNorm = getProjectionNormalized(mWidth, mHeight);
        ScriptField_VpConsts.Item i = new ScriptField_VpConsts.Item();
        i.Proj = projNorm;
        i.MVP = projNorm;
        mPvStarAlloc.set(i, 0, true);
        mPvProjectionAlloc.setProjection(projNorm);
    }

    @Override
    public void setOffset(float xOffset, float yOffset, int xPixels, int yPixels) {
        mScript.set_gXOffset(xOffset);
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);

        updateProjectionMatrices();
    }

    private void loadTextures() {
        mScript.set_gTSpace(loadTexture(R.drawable.space));
        mScript.set_gTLight1(loadTexture(R.drawable.light1));
        mScript.set_gTFlares(loadTextureARGB(R.drawable.flares));
    }

    private Allocation loadTexture(int id) {
        final Allocation allocation = Allocation.createFromBitmapResource(mRS, mResources, id,
                                           Allocation.MipmapControl.MIPMAP_NONE,
                                           Allocation.USAGE_GRAPHICS_TEXTURE);
        return allocation;
    }

    // TODO: Fix Allocation.createFromBitmapResource() to do this when RGBA_8888 is specified
    private Allocation loadTextureARGB(int id) {
        Bitmap b = BitmapFactory.decodeResource(mResources, id, mOptionsARGB);
        final Allocation allocation = Allocation.createFromBitmap(mRS, b,
                                           Allocation.MipmapControl.MIPMAP_NONE,
                                           Allocation.USAGE_GRAPHICS_TEXTURE);
        return allocation;
    }

    private void createProgramFragment() {
        ProgramFragmentFixedFunction.Builder builder = new ProgramFragmentFixedFunction.Builder(mRS);
        builder.setTexture(ProgramFragmentFixedFunction.Builder.EnvMode.REPLACE,
                           ProgramFragmentFixedFunction.Builder.Format.RGB, 0);
        ProgramFragment pfb = builder.create();
        pfb.bindSampler(Sampler.WRAP_NEAREST(mRS), 0);
        mScript.set_gPFBackground(pfb);

        builder = new ProgramFragmentFixedFunction.Builder(mRS);
        builder.setPointSpriteTexCoordinateReplacement(true);
        builder.setTexture(ProgramFragmentFixedFunction.Builder.EnvMode.MODULATE,
                           ProgramFragmentFixedFunction.Builder.Format.RGBA, 0);
        builder.setVaryingColor(true);
        ProgramFragment pfs = builder.create();
        pfs.bindSampler(Sampler.WRAP_LINEAR(mRS), 0);
        mScript.set_gPFStars(pfs);
    }

    private void createProgramFragmentStore() {
        ProgramStore.Builder builder = new ProgramStore.Builder(mRS);
        builder.setBlendFunc(BlendSrcFunc.ONE, BlendDstFunc.ZERO);
        mRS.bindProgramStore(builder.create());

        builder.setBlendFunc(BlendSrcFunc.SRC_ALPHA, BlendDstFunc.ONE);
        mScript.set_gPSLights(builder.create());
    }

    private void createProgramVertex() {
        mPvOrthoAlloc = new ProgramVertexFixedFunction.Constants(mRS);

        ProgramVertexFixedFunction.Builder builder = new ProgramVertexFixedFunction.Builder(mRS);
        ProgramVertex pvbo = builder.create();
        ((ProgramVertexFixedFunction)pvbo).bindConstants(mPvOrthoAlloc);
        mRS.bindProgramVertex(pvbo);

        mPvStarAlloc = new ScriptField_VpConsts(mRS, 1);
        mScript.bind_vpConstants(mPvStarAlloc);
        mPvProjectionAlloc = new ProgramVertexFixedFunction.Constants(mRS);
        updateProjectionMatrices();

        builder = new ProgramVertexFixedFunction.Builder(mRS);
        ProgramVertex pvbp = builder.create();
        ((ProgramVertexFixedFunction)pvbp).bindConstants(mPvProjectionAlloc);
        mScript.set_gPVBkProj(pvbp);

        ProgramVertex.Builder sb = new ProgramVertex.Builder(mRS);
        String t =  "varying vec4 varColor;\n" +
                    "varying vec2 varTex0;\n" +
                    "void main() {\n" +
                    "  float dist = ATTRIB_position.y;\n" +
                    "  float angle = ATTRIB_position.x;\n" +
                    "  float x = dist * sin(angle);\n" +
                    "  float y = dist * cos(angle) * 0.892;\n" +
                    "  float p = dist * 5.5;\n" +
                    "  float s = cos(p);\n" +
                    "  float t = sin(p);\n" +
                    "  vec4 pos;\n" +
                    "  pos.x = t * x + s * y;\n" +
                    "  pos.y = s * x - t * y;\n" +
                    "  pos.z = ATTRIB_position.z;\n" +
                    "  pos.w = 1.0;\n" +
                    "  gl_Position = UNI_MVP * pos;\n" +
                    "  gl_PointSize = ATTRIB_color.a * 10.0;\n" +
                    "  varColor.rgb = ATTRIB_color.rgb;\n" +
                    "  varColor.a = 1.0;\n" +
                    "}\n";
        sb.setShader(t);
        sb.addInput(mParticlesMesh.getVertexAllocation(0).getType().getElement());
        sb.addConstant(mPvStarAlloc.getType());
        ProgramVertex pvs = sb.create();
        pvs.bindConstants(mPvStarAlloc.getAllocation(), 0);
        mScript.set_gPVStars(pvs);
    }

    private void createProgramRaster() {
        ProgramRaster.Builder b = new ProgramRaster.Builder(mRS);
        b.setPointSpriteEnabled(true);
        ProgramRaster pr = b.create();
        mRS.bindProgramRaster(pr);
    }

}
