package com.android.noisefield;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.renderscript.Allocation;
import android.renderscript.Float3;
import android.renderscript.Float4;
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
import android.renderscript.Mesh.Primitive;
import android.renderscript.ProgramStore.BlendDstFunc;
import android.renderscript.ProgramStore.BlendSrcFunc;
import android.os.Bundle;
import android.app.WallpaperManager;
import android.util.Log;
import android.view.MotionEvent;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

public class NoiseFieldRS {
    public static String LOG_TAG = "NoiseField";

    private Resources mRes;
    private RenderScriptGL mRS;
    private ScriptC_noisefield mScript;
    private int mHeight;
    private int mWidth;
    private boolean mTouchDown;
    private final BitmapFactory.Options mOptionsARGB = new BitmapFactory.Options();

    private ScriptField_VpConsts mPvConsts;
    private Allocation mDotAllocation;
    private ScriptField_VertexColor_s mVertexColors;
    private ScriptField_Particle mDotParticles;
    private Mesh mDotMesh;
    private int mDensityDPI;

    public void init(int dpi, RenderScriptGL rs,
                     Resources res, int width, int height) {
        mDensityDPI = dpi;
        mRS = rs;
        mRes = res;

        mWidth = width;
        mHeight = height;

        mOptionsARGB.inScaled = false;
        mOptionsARGB.inPreferredConfig = Bitmap.Config.ARGB_8888;

        Mesh.AllocationBuilder smb2 = new Mesh.AllocationBuilder(mRS);

        mDotParticles = new ScriptField_Particle(mRS, 83);
        smb2.addVertexAllocation(mDotParticles.getAllocation());

        smb2.addIndexSetType(Mesh.Primitive.POINT);
        mScript = new ScriptC_noisefield(mRS, mRes, R.raw.noisefield);

        mDotMesh = smb2.create();
        mScript.set_dotMesh(mDotMesh);
        mScript.bind_dotParticles(mDotParticles);

        mPvConsts = new ScriptField_VpConsts(mRS, 1);

        createProgramVertex();
        createProgramRaster();
        createProgramFragmentStore();
        createProgramFragment();
        createBackgroundMesh();
        loadTextures();

        mScript.set_densityDPI(mDensityDPI);
        mScript.invoke_positionParticles();
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
            m1.loadFrustum(-0.5f, 1, -aspect, aspect, 1, 100);
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
        i.scaleSize = mDensityDPI/240.0f;
        mPvConsts.set(i, 0, true);
    }

    private void createBackgroundMesh() {
        // The composition and colors of the background mesh were plotted on paper and photoshop
        // first then translated to the csv file in raw. Points and colors are not random.
        ArrayList<String> meshData = new ArrayList<String>();
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
        for (int i=0; i<meshDataSize; i++) {
            String line = (String) meshData.get(i);
            String[] values = line.split(",");
            float xPos = Float.parseFloat(values[0]);
            float yPos = Float.parseFloat(values[1]);
            float red = Float.parseFloat(values[2]);
            float green = Float.parseFloat(values[3]);
            float blue = Float.parseFloat(values[4]);
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
        mScript.set_textureDot(mDotAllocation);
    }

    private void createProgramVertex() {
        ProgramVertex.Builder backgroundBuilder = new ProgramVertex.Builder(mRS);
        backgroundBuilder.setShader(mRes, R.raw.bg_vs);
        backgroundBuilder.addInput(ScriptField_VertexColor_s.createElement(mRS));
        ProgramVertex programVertexBackground = backgroundBuilder.create();
        mScript.set_vertBg(programVertexBackground);

        updateProjectionMatrices();

        ProgramVertex.Builder builder = new ProgramVertex.Builder(mRS);
        builder = new ProgramVertex.Builder(mRS);
        builder.setShader(mRes, R.raw.noisefield_vs);
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
        builder.setShader(mRes, R.raw.noisefield_fs);
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
        builder.setBlendFunc(BlendSrcFunc.SRC_ALPHA, BlendDstFunc.ONE );
        mRS.bindProgramStore(builder.create());
    }

    public void start() {
        mRS.bindRootScript(mScript);
    }

    public void stop() {
        mRS.bindRootScript(null);
    }

    public void resize(int w, int h) {

    }

    public void onTouchEvent(MotionEvent ev) {
        int act = ev.getActionMasked();
        if (act == MotionEvent.ACTION_UP || act == MotionEvent.ACTION_POINTER_UP) {
            if(mTouchDown){
                mTouchDown = false;
                mScript.set_touchDown(mTouchDown);
            }
            return;
        } else if(   act == MotionEvent.ACTION_DOWN
                  || act == MotionEvent.ACTION_MOVE
                  || act == MotionEvent.ACTION_POINTER_DOWN) {
            int pcount = ev.getPointerCount();

            if(!mTouchDown){
                mTouchDown = true;
                mScript.set_touchDown(mTouchDown);
            }
            if(pcount > 0){
                // just send first pointer position
                mScript.invoke_touch(ev.getX(0), ev.getY(0));
            }
        }
    }
}