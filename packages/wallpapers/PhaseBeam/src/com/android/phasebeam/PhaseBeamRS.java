package com.android.phasebeam;

import static android.renderscript.Sampler.Value.NEAREST;
import static android.renderscript.Sampler.Value.WRAP;

import android.content.res.Resources;
import android.renderscript.Allocation;
import android.renderscript.Matrix4f;
import android.renderscript.Mesh;
import android.renderscript.Program;
import android.renderscript.ProgramFragment;
import android.renderscript.ProgramFragmentFixedFunction;
import android.renderscript.ProgramRaster;
import android.renderscript.ProgramStore;
import android.renderscript.ProgramVertex;
import android.renderscript.ProgramVertexFixedFunction;
import android.renderscript.RenderScriptGL;
import android.renderscript.Sampler;
import android.renderscript.ProgramStore.BlendDstFunc;
import android.renderscript.ProgramStore.BlendSrcFunc;
import android.renderscript.Mesh.Primitive;
import android.graphics.Color;
import android.renderscript.Float3;
import android.renderscript.Float4;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import android.util.Log;

public class PhaseBeamRS {
    public static String LOG_TAG = "PhaseBeam";
    public static final int DOT_COUNT = 28;
    private Resources mRes;
    private RenderScriptGL mRS;
    private ScriptC_phasebeam mScript;
    int mHeight;
    int mWidth;

    private ScriptField_VpConsts mPvConsts;
    private Allocation mDotAllocation;
    private Allocation mBeamAllocation;

    private ScriptField_Particle mDotParticles;
    private Mesh mDotMesh;

    private ScriptField_Particle mBeamParticles;
    private Mesh mBeamMesh;

    private ScriptField_VertexColor_s mVertexColors;

    private int mDensityDPI;

    boolean mInited = false;

    public void init(int dpi, RenderScriptGL rs, Resources res, int width, int height) {
        if (!mInited) {
            mDensityDPI = dpi;

            mRS = rs;
            mRes = res;

            mWidth = width;
            mHeight = height;

            mDotParticles = new ScriptField_Particle(mRS, DOT_COUNT);
            Mesh.AllocationBuilder smb2 = new Mesh.AllocationBuilder(mRS);
            smb2.addVertexAllocation(mDotParticles.getAllocation());
            smb2.addIndexSetType(Mesh.Primitive.POINT);
            mDotMesh = smb2.create();

            mBeamParticles = new ScriptField_Particle(mRS, DOT_COUNT);
            Mesh.AllocationBuilder smb3 = new Mesh.AllocationBuilder(mRS);
            smb3.addVertexAllocation(mBeamParticles.getAllocation());
            smb3.addIndexSetType(Mesh.Primitive.POINT);
            mBeamMesh = smb3.create();

            mScript = new ScriptC_phasebeam(mRS, mRes, R.raw.phasebeam);
            mScript.set_dotMesh(mDotMesh);
            mScript.set_beamMesh(mBeamMesh);
            mScript.bind_dotParticles(mDotParticles);
            mScript.bind_beamParticles(mBeamParticles);

            mPvConsts = new ScriptField_VpConsts(mRS, 1);

            createProgramVertex();
            createProgramRaster();
            createProgramFragmentStore();
            createProgramFragment();
            createBackgroundMesh();
            loadTextures();

            mScript.set_densityDPI(mDensityDPI);

            mRS.bindRootScript(mScript);

            mScript.invoke_positionParticles();
            mInited = true;
        }
    }

    private Matrix4f getProjectionNormalized(int w, int h) {
        // range -1,1 in the narrow axis at z = 0.
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

    private void updateProjectionMatrices() {
        Matrix4f projNorm = getProjectionNormalized(mWidth, mHeight);
        ScriptField_VpConsts.Item i = new ScriptField_VpConsts.Item();
        i.MVP = projNorm;
        i.scaleSize = mDensityDPI / 240.0f;
        mPvConsts.set(i, 0, true);
    }

    private void createBackgroundMesh() {
        // The composition and colors of the background mesh were plotted on paper and photoshop
        // first then translated to the csv file in raw. Points and colors are not random.
        ArrayList meshData = new ArrayList();
        InputStream inputStream = mRes.openRawResource(R.raw.bgmesh);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                meshData.add(line);
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Unable to load background mesh from csv file.");
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Unable to close background mesh csv file.");
            }
        }

        int meshDataSize = meshData.size();
        mVertexColors = new ScriptField_VertexColor_s(mRS, meshDataSize);
        for(int i=0; i<meshDataSize; i++) {
            String line = (String) meshData.get(i);
            String[] values = line.split(",");
            float xPos = new Float(values[0]);
            float yPos = new Float(values[1]);
            float red = new Float(values[2]);
            float green = new Float(values[3]);
            float blue = new Float(values[4]);
            mVertexColors.set_position(i, new Float3(xPos, yPos, 0.0f), false);
            mVertexColors.set_color(i, new Float4(red, green, blue, 1.0f), false);
        }
        mVertexColors.copyAll();

        Mesh.AllocationBuilder backgroundBuilder = new Mesh.AllocationBuilder(mRS);
        backgroundBuilder.addIndexSetType(Primitive.TRIANGLE);
        backgroundBuilder.addVertexAllocation(mVertexColors.getAllocation());
        mScript.set_gBackgroundMesh(backgroundBuilder.create());
        mScript.bind_vertexColors(mVertexColors);
    }

    private Allocation loadTexture(int id) {
        final Allocation allocation = Allocation.createFromBitmapResource(mRS, mRes, id,
                                               Allocation.MipmapControl.MIPMAP_NONE,
                                               Allocation.USAGE_GRAPHICS_TEXTURE);
        return allocation;
    }

    private void loadTextures() {
        mDotAllocation = loadTexture(R.drawable.dot);
        mBeamAllocation = loadTexture(R.drawable.beam);
        mScript.set_textureDot(mDotAllocation);
        mScript.set_textureBeam(mBeamAllocation);
    }

    private void createProgramVertex() {
        ProgramVertex.Builder backgroundBuilder = new ProgramVertex.Builder(mRS);
        backgroundBuilder.setShader(mRes, R.raw.bg_vs);
        backgroundBuilder.addInput(ScriptField_VertexColor_s.createElement(mRS));
        ProgramVertex programVertexBackground = backgroundBuilder.create();
        mScript.set_vertBg(programVertexBackground);

        updateProjectionMatrices();

        ProgramVertex.Builder builder = new ProgramVertex.Builder(mRS);
        builder.setShader(mRes, R.raw.dot_vs);
        builder.addConstant(mPvConsts.getType());
        builder.addInput(mDotMesh.getVertexAllocation(0).getType().getElement());
        ProgramVertex pvs = builder.create();
        pvs.bindConstants(mPvConsts.getAllocation(), 0);
        mRS.bindProgramVertex(pvs);
        mScript.set_vertDots(pvs);

    }

    private void createProgramFragment() {
        ProgramFragment.Builder backgroundBuilder = new ProgramFragment.Builder(mRS);
        backgroundBuilder.setShader(mRes, R.raw.bg_fs);
        ProgramFragment programFragmentBackground = backgroundBuilder.create();
        mScript.set_fragBg(programFragmentBackground);

        ProgramFragment.Builder builder = new ProgramFragment.Builder(mRS);
        builder.setShader(mRes, R.raw.dot_fs);
        builder.addTexture(Program.TextureType.TEXTURE_2D);
        ProgramFragment pf = builder.create();
        pf.bindSampler(Sampler.CLAMP_LINEAR(mRS), 0);
        mScript.set_fragDots(pf);

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

    public void setOffset(float xOffset, float yOffset, int xPixels, int yPixels) {
        mScript.set_xOffset(xOffset);
    }

    public void resize(int w, int h) {

    }

}
