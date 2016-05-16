/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.wallpaper.holospiral;

import com.android.wallpaper.holospiral.ScriptC_holo_spiral;
import com.android.wallpaper.holospiral.ScriptField_VertexColor_s;
import com.android.wallpaper.holospiral.ScriptField_VertexShaderConstants_s;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.Float3;
import android.renderscript.Float4;
import android.renderscript.Mesh;
import android.renderscript.Mesh.Primitive;
import android.renderscript.Program;
import android.renderscript.ProgramFragment;
import android.renderscript.ProgramStore;
import android.renderscript.ProgramVertex;
import android.renderscript.RenderScriptGL;
import android.renderscript.Sampler;

public class HoloSpiralRS {
    private static final String LOG_TAG = "HoloSpiralRS";
    private static final float MAX_POINT_SIZE = 75.0f;
    private static final float NEAR_PLANE = 1.0f;
    private static final float FAR_PLANE = 55.0f;

    private static final int NUM_INNER_POINTS = 100;
    private static final float INNER_SPIRAL_DEPTH = 50.0f;
    private static final float INNER_RADIUS = 5.0f;
    private static final float INNER_SEPARATION_DEG = 23.0f;

    private static final int NUM_OUTER_POINTS = 50;
    private static final float OUTER_SPIRAL_DEPTH = 30.0f;
    private static final float OUTER_RADIUS = 10.0f;
    private static final float OUTER_SEPARATION_DEG = 23.0f;

    /* Colors */
    private static final int POINTS_COLOR_BLUE = Color.argb(179, 0, 0, 255);
    private static final int POINTS_COLOR_GREEN = Color.argb(210, 166, 51, 255);
    private static final int POINTS_COLOR_AQUA = Color.argb(220, 38, 120, 148);
    private static final int BG_COLOR_BLACK = Color.argb(255, 26, 26, 83);
    private static final int BG_COLOR_BLUE = Color.argb(255, 8, 0, 26);

    private ScriptC_holo_spiral mScript;
    private RenderScriptGL mRS = null;
    private Resources mResources = null;

    public HoloSpiralRS(RenderScriptGL renderer, Resources resources) {
        init(renderer, resources);
    }

    public void init(RenderScriptGL renderer, Resources resources) {
        mRS = renderer;
        mResources = resources;
        createScript();
    }

    public void setOffset(float xOffset, float yOffset, int xPixels, int yPixels) {
        mScript.set_gXOffset(xOffset);
    }

    public Bundle onCommand(String action, int x, int y, int z, Bundle extras,
            boolean resultRequested) {
        return null;
    }

    public void start() {
        mRS.bindRootScript(mScript);
    }

    public void stop() {
        mRS.bindRootScript(null);
    }

    public void resize(int width, int height) {
        mScript.invoke_resize(width, height);
    }

    private void createScript() {
        mScript = new ScriptC_holo_spiral(mRS, mResources, R.raw.holo_spiral);
        mScript.set_gNearPlane(NEAR_PLANE);
        mScript.set_gFarPlane(FAR_PLANE);

        createVertexPrograms();
        createFragmentPrograms();
        createStorePrograms();

        createPointGeometry();
        createBackgroundMesh();
        createTextures();
    }

    private void createVertexPrograms() {
        ScriptField_VertexShaderConstants_s vertexShaderConstants =
                new ScriptField_VertexShaderConstants_s(mRS, 1);
        mScript.bind_gVSConstants(vertexShaderConstants);
        vertexShaderConstants.set_maxPointSize(0, MAX_POINT_SIZE, false);
        vertexShaderConstants.copyAll();

        ProgramVertex.Builder backgroundBuilder = new ProgramVertex.Builder(mRS);
        backgroundBuilder.setShader(mResources, R.raw.vertex_background);
        backgroundBuilder.addInput(ScriptField_VertexColor_s.createElement(mRS));
        ProgramVertex programVertexBackground = backgroundBuilder.create();
        mScript.set_gPVBackground(programVertexBackground);

        ProgramVertex.Builder geometryBuilder = new ProgramVertex.Builder(mRS);
        geometryBuilder.setShader(mResources, R.raw.vertex_geometry);
        geometryBuilder.addConstant(vertexShaderConstants.getAllocation().getType());
        geometryBuilder.addInput(ScriptField_VertexColor_s.createElement(mRS));
        ProgramVertex programVertexGeometry = geometryBuilder.create();
        programVertexGeometry.bindConstants(vertexShaderConstants.getAllocation(), 0);
        mScript.set_gPVGeometry(programVertexGeometry);
    }

    private void createFragmentPrograms() {
        ProgramFragment.Builder backgroundBuilder = new ProgramFragment.Builder(mRS);
        backgroundBuilder.setShader(mResources, R.raw.fragment_background);
        ProgramFragment programFragmentBackground = backgroundBuilder.create();
        mScript.set_gPFBackground(programFragmentBackground);

        ProgramFragment.Builder geometryBuilder = new ProgramFragment.Builder(mRS);
        geometryBuilder.setShader(mResources, R.raw.fragment_geometry);
        geometryBuilder.addTexture(Program.TextureType.TEXTURE_2D);
        ProgramFragment programFragmentGeometry = geometryBuilder.create();
        programFragmentGeometry.bindSampler(Sampler.CLAMP_LINEAR(mRS), 0);
        mScript.set_gPFGeometry(programFragmentGeometry);
    }

    private void createStorePrograms() {
        ProgramStore.Builder builder = new ProgramStore.Builder(mRS);
        builder.setBlendFunc(ProgramStore.BlendSrcFunc.SRC_ALPHA,
                ProgramStore.BlendDstFunc.ONE_MINUS_SRC_ALPHA);
        mScript.set_gPSGeometry(builder.create());
        builder.setBlendFunc(ProgramStore.BlendSrcFunc.ONE, ProgramStore.BlendDstFunc.ZERO);
        builder.setDitherEnabled(true);
        mScript.set_gPSBackground(builder.create());
    }

    private void createPointGeometry() {
        ScriptField_VertexColor_s innerPoints =
                new ScriptField_VertexColor_s(mRS, NUM_INNER_POINTS);
        generateSpiral(innerPoints, INNER_SPIRAL_DEPTH, INNER_RADIUS, INNER_SEPARATION_DEG,
                POINTS_COLOR_BLUE, POINTS_COLOR_GREEN);

        Mesh.AllocationBuilder innerPointBuilder = new Mesh.AllocationBuilder(mRS);
        innerPointBuilder.addIndexSetType(Primitive.POINT);
        innerPointBuilder.addVertexAllocation(innerPoints.getAllocation());
        mScript.set_gInnerGeometry(innerPointBuilder.create());

        ScriptField_VertexColor_s outerPoints =
                new ScriptField_VertexColor_s(mRS, NUM_OUTER_POINTS);
        generateSpiral(outerPoints, OUTER_SPIRAL_DEPTH, OUTER_RADIUS, OUTER_SEPARATION_DEG,
                POINTS_COLOR_AQUA, POINTS_COLOR_AQUA);

        Mesh.AllocationBuilder outerPointBuilder = new Mesh.AllocationBuilder(mRS);
        outerPointBuilder.addIndexSetType(Primitive.POINT);
        outerPointBuilder.addVertexAllocation(outerPoints.getAllocation());
        mScript.set_gOuterGeometry(outerPointBuilder.create());
    }

    private void createTextures() {
        Bitmap bmp = BitmapFactory.decodeResource(
                mResources, R.drawable.points_red_green, null);
        Allocation pointTexture = Allocation.createFromBitmap(mRS, bmp,
                                           Allocation.MipmapControl.MIPMAP_NONE,
                                           Allocation.USAGE_GRAPHICS_TEXTURE);
        mScript.set_gPointTexture(pointTexture);
    }

    private void createBackgroundMesh() {
        ScriptField_VertexColor_s fullQuad = new ScriptField_VertexColor_s(mRS, 4);

        Float3 topLeft = new Float3(-1.0f, 1.0f, 0.0f);
        Float3 bottomLeft = new Float3(-1.0f, -1.0f, 0.0f);
        Float3 topRight = new Float3(1.0f, 1.0f, 0.0f);
        Float3 bottomRight = new Float3(1.0f, -1.0f, 0.0f);

        fullQuad.set_position(0, topLeft, false);
        fullQuad.set_color(0, convertColor(BG_COLOR_BLUE), false);

        fullQuad.set_position(1, bottomLeft, false);
        fullQuad.set_color(1, convertColor(BG_COLOR_BLACK), false);

        fullQuad.set_position(2, topRight, false);
        fullQuad.set_color(2, convertColor(BG_COLOR_BLUE), false);

        fullQuad.set_position(3, bottomRight, false);
        fullQuad.set_color(3, convertColor(BG_COLOR_BLACK), false);

        fullQuad.copyAll();

        Mesh.AllocationBuilder backgroundBuilder = new Mesh.AllocationBuilder(mRS);
        backgroundBuilder.addIndexSetType(Primitive.TRIANGLE_STRIP);
        backgroundBuilder.addVertexAllocation(fullQuad.getAllocation());
        mScript.set_gBackgroundMesh(backgroundBuilder.create());
    }

    private void generateSpiral(ScriptField_VertexColor_s points, float depth, float radius,
            float separationDegrees, int primaryColor, int secondaryColor) {

        float separationRads = (separationDegrees / 360.0f) * 2 * (float) Math.PI;
        int size = points.getAllocation().getType().getX();

        float halfDepth = depth / 2.0f;
        float radians = 0.0f;

        Float4 primary = convertColor(primaryColor);
        Float4 secondary = convertColor(secondaryColor);

        for (int i = 0; i < size; i++) {
            float percentage = (float) i / (float) size;
            Float3 position = new Float3(radius * (float) Math.cos(radians),
                    radius * (float) Math.sin(radians), (percentage * depth) - halfDepth);

            float r = (float) Math.sin(radians / 2.0f);

            Float4 color = new Float4();
            color.x = primary.x + ((secondary.x - primary.x) * r);
            color.y = primary.y + ((secondary.y - primary.y) * r);
            color.z = primary.z + ((secondary.z - primary.z) * r);
            color.w = primary.w + ((secondary.w - primary.w) * r);

            points.set_position(i, position, false);
            points.set_color(i, color, false);

            radians += separationRads;
            int multiplier = (int) (radians / (2.0f * (float) Math.PI));
            radians -= multiplier * 2.0f * (float) Math.PI;
        }

        points.copyAll();
    }

    private static Float4 convertColor(int color) {
        float red = Color.red(color) / 255.0f;
        float green = Color.green(color) / 255.0f;
        float blue = Color.blue(color) / 255.0f;
        float alpha = Color.alpha(color) / 255.0f;
        return new Float4(red, green, blue, alpha);
    }
}
