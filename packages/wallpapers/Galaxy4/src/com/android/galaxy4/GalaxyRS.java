package com.android.galaxy4;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.renderscript.Allocation;
import android.renderscript.Matrix4f;
import android.renderscript.Mesh;
import android.renderscript.ProgramFragment;
import android.renderscript.ProgramFragmentFixedFunction;
import android.renderscript.ProgramRaster;
import android.renderscript.ProgramStore;
import android.renderscript.Sampler;
import android.renderscript.ProgramStore.BlendDstFunc;
import android.renderscript.ProgramStore.BlendSrcFunc;
import android.renderscript.ProgramVertex;
import android.renderscript.ProgramVertexFixedFunction;
import android.renderscript.RenderScriptGL;
import android.renderscript.ProgramVertexFixedFunction.Builder;
import android.util.Log;
import android.renderscript.Program;
import static android.renderscript.Sampler.Value.*;

public class GalaxyRS {
    public static final int BG_STAR_COUNT = 11000;
    public static final int SPACE_CLOUDSTAR_COUNT = 25;
    public static final int STATIC_STAR_COUNT = 50;
    private Resources mRes;

    private RenderScriptGL mRS;
    private ScriptC_galaxy mScript;

    private ScriptField_VpConsts mPvConsts;
    private ScriptField_Particle mSpaceClouds;
    private ScriptField_Particle mBgStars;
    private ScriptField_Particle mStaticStars;
    private Mesh mSpaceCloudsMesh;
    private Mesh mBgStarsMesh;
    private Mesh mStaticStarsMesh;

    private int mHeight;
    private int mWidth;
    private boolean mInited = false;
    private int mDensityDPI;

    private final BitmapFactory.Options mOptionsARGB = new BitmapFactory.Options();

    private Allocation mCloudAllocation;
    private Allocation mStaticStarAllocation;
    private Allocation mStaticStar2Allocation;
    private Allocation mBgAllocation;

    public void init(int dpi, RenderScriptGL rs, Resources res, int width, int height) {
        if (!mInited) {
            mDensityDPI = dpi;

            mRS = rs;
            mRes = res;

            mWidth = width;
            mHeight = height;

            mOptionsARGB.inScaled = false;
            mOptionsARGB.inPreferredConfig = Bitmap.Config.ARGB_8888;

            mSpaceClouds = new ScriptField_Particle(mRS, SPACE_CLOUDSTAR_COUNT);
            Mesh.AllocationBuilder smb = new Mesh.AllocationBuilder(mRS);
            smb.addVertexAllocation(mSpaceClouds.getAllocation());
            smb.addIndexSetType(Mesh.Primitive.POINT);
            mSpaceCloudsMesh = smb.create();

            mBgStars = new ScriptField_Particle(mRS, BG_STAR_COUNT);
            Mesh.AllocationBuilder smb2 = new Mesh.AllocationBuilder(mRS);
            smb2.addVertexAllocation(mBgStars.getAllocation());
            smb2.addIndexSetType(Mesh.Primitive.POINT);
            mBgStarsMesh = smb2.create();

            mStaticStars = new ScriptField_Particle(mRS, STATIC_STAR_COUNT);
            Mesh.AllocationBuilder smb3 = new Mesh.AllocationBuilder(mRS);
            smb3.addVertexAllocation(mStaticStars.getAllocation());
            smb3.addIndexSetType(Mesh.Primitive.POINT);
            mStaticStarsMesh = smb3.create();

            mScript = new ScriptC_galaxy(mRS, mRes, R.raw.galaxy);
            mScript.set_spaceCloudsMesh(mSpaceCloudsMesh);
            mScript.bind_spaceClouds(mSpaceClouds);
            mScript.set_bgStarsMesh(mBgStarsMesh);
            mScript.bind_bgStars(mBgStars);
            mScript.set_staticStarsMesh(mStaticStarsMesh);
            mScript.bind_staticStars(mStaticStars);

            mPvConsts = new ScriptField_VpConsts(mRS, 1);

            createProgramVertex();
            createProgramRaster();
            createProgramFragmentStore();
            createProgramFragment();

            loadTextures();

            mScript.set_densityDPI(mDensityDPI);
            mRS.bindRootScript(mScript);
            mScript.invoke_positionParticles();
            mInited = true;
        }
    }

    private Allocation loadTexture(int id) {
        final Allocation allocation = Allocation.createFromBitmapResource(mRS, mRes, id,
                                           Allocation.MipmapControl.MIPMAP_NONE,
                                           Allocation.USAGE_GRAPHICS_TEXTURE);
        return allocation;
    }

    private void loadTextures() {
        mStaticStarAllocation = loadTexture(R.drawable.staticstar);
        mStaticStar2Allocation = loadTexture(R.drawable.staticstar2);
        mCloudAllocation = loadTexture(R.drawable.cloud);
        mBgAllocation = loadTexture(R.drawable.bg);
        mScript.set_textureSpaceCloud(mCloudAllocation);
        mScript.set_textureStaticStar(mStaticStarAllocation);
        mScript.set_textureStaticStar2(mStaticStar2Allocation);
        mScript.set_textureBg(mBgAllocation);
    }

    private Matrix4f getProjectionNormalized(int w, int h) {
        Matrix4f m1 = new Matrix4f();
        Matrix4f m2 = new Matrix4f();

        if (w > h) {
            float aspect = ((float) w) / h;
            m1.loadFrustum(-aspect, aspect, -1, 1, 1, 100);
        } else {
            float aspect = ((float) h) / w;
            m1.loadFrustum(-1, 1, -aspect, aspect, 1, 100);
        }

        m2.loadRotate(180, 0, 1, 0);
        m1.loadMultiply(m1, m2);
        m2.loadScale(-1, 1, 1);
        m1.loadMultiply(m1, m2);
        m2.loadTranslate(0, 0, 1);
        m1.loadMultiply(m1, m2);
        return m1;
    }

    private void updateProjectionMatrices(int w, int h) {
        mWidth = w;
        mHeight = h;
        Matrix4f proj = new Matrix4f();
        proj.loadOrthoWindow(mWidth, mHeight);
        Matrix4f projNorm = getProjectionNormalized(mWidth, mHeight);
        ScriptField_VpConsts.Item i = new ScriptField_VpConsts.Item();
        i.MVP = projNorm;
        i.scaleSize = mDensityDPI / 240.0f;
        mPvConsts.set(i, 0, true);
        mScript.invoke_positionParticles();
    }

    public void createProgramVertex() {
        ProgramVertexFixedFunction.Constants mPvOrthoAlloc =
            new ProgramVertexFixedFunction.Constants(mRS);
        Matrix4f proj = new Matrix4f();
        proj.loadOrthoWindow(mWidth, mHeight);
        mPvOrthoAlloc.setProjection(proj);

        ProgramVertexFixedFunction.Builder pvb = new ProgramVertexFixedFunction.Builder(mRS);
        ProgramVertex pv = pvb.create();
        ((ProgramVertexFixedFunction) pv).bindConstants(mPvOrthoAlloc);
        mScript.set_vertBg(pv);
        updateProjectionMatrices(mWidth, mHeight);

        // cloud
        ProgramVertex.Builder builder = new ProgramVertex.Builder(mRS);
        builder.setShader(mRes, R.raw.spacecloud_vs);
        builder.addConstant(mPvConsts.getType());
        builder.addInput(mSpaceCloudsMesh.getVertexAllocation(0).getType().getElement());
        ProgramVertex pvs = builder.create();
        pvs.bindConstants(mPvConsts.getAllocation(), 0);
        mRS.bindProgramVertex(pvs);

        mScript.set_vertSpaceClouds(pvs);

        // bg stars
        builder = new ProgramVertex.Builder(mRS);
        builder.setShader(mRes, R.raw.bgstar_vs);
        builder.addConstant(mPvConsts.getType());
        builder.addInput(mBgStarsMesh.getVertexAllocation(0).getType().getElement());
        pvs = builder.create();
        pvs.bindConstants(mPvConsts.getAllocation(), 0);
        mRS.bindProgramVertex(pvs);
        mScript.set_vertBgStars(pvs);

        // static stars
        builder = new ProgramVertex.Builder(mRS);
        builder.setShader(mRes, R.raw.staticstar_vs);
        builder.addConstant(mPvConsts.getType());
        builder.addInput(mBgStarsMesh.getVertexAllocation(0).getType().getElement());
        pvs = builder.create();
        pvs.bindConstants(mPvConsts.getAllocation(), 0);
        mRS.bindProgramVertex(pvs);
        mScript.set_vertStaticStars(pvs);
    }

    private void createProgramFragment() {
        // bg
        Sampler.Builder samplerBuilder = new Sampler.Builder(mRS);
        samplerBuilder.setMinification(LINEAR);
        samplerBuilder.setMagnification(LINEAR);
        samplerBuilder.setWrapS(WRAP);
        samplerBuilder.setWrapT(WRAP);
        Sampler sn = samplerBuilder.create();
        ProgramFragmentFixedFunction.Builder builderff =
                new ProgramFragmentFixedFunction.Builder(mRS);
        builderff = new ProgramFragmentFixedFunction.Builder(mRS);
        builderff.setTexture(ProgramFragmentFixedFunction.Builder.EnvMode.REPLACE,
                ProgramFragmentFixedFunction.Builder.Format.RGB, 0);
        ProgramFragment pfff = builderff.create();
        mScript.set_fragBg(pfff);
        pfff.bindSampler(sn, 0);

        // cloud
        ProgramFragment.Builder builder = new ProgramFragment.Builder(mRS);

        builder.setShader(mRes, R.raw.spacecloud_fs);
        builder.addTexture(Program.TextureType.TEXTURE_2D);

        ProgramFragment pf = builder.create();
        pf.bindSampler(Sampler.CLAMP_LINEAR(mRS), 0);
        mScript.set_fragSpaceClouds(pf);

        // bg stars
        builder = new ProgramFragment.Builder(mRS);
        builder.setShader(mRes, R.raw.bgstar_fs);
        pf = builder.create();
        mScript.set_fragBgStars(pf);

        // static stars
        builder = new ProgramFragment.Builder(mRS);
        builder.setShader(mRes, R.raw.staticstar_fs);
        builder.addTexture(Program.TextureType.TEXTURE_2D);
        builder.addTexture(Program.TextureType.TEXTURE_2D);
        pf = builder.create();
        mScript.set_fragStaticStars(pf);
    }

    private void createProgramRaster() {
        ProgramRaster.Builder builder = new ProgramRaster.Builder(mRS);
        builder.setPointSpriteEnabled(true);
        ProgramRaster pr = builder.create();
        mRS.bindProgramRaster(pr);
    }

    private void createProgramFragmentStore() {
        ProgramStore.Builder builder = new ProgramStore.Builder(mRS);
        builder.setBlendFunc(BlendSrcFunc.SRC_ALPHA, BlendDstFunc.ONE);
        mRS.bindProgramStore(builder.create());
    }

    public void start() {
        mRS.bindRootScript(mScript);
    }

    public void stop() {
        mRS.bindRootScript(null);
    }

    public void resize(int width, int height) {
        mWidth = width;
        mHeight = height;
        createProgramVertex();
    }
}