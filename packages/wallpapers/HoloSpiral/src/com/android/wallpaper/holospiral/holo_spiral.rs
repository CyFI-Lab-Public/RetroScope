// Copyright (C) 2010 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

#pragma version(1)

#pragma rs java_package_name(com.android.wallpaper.holospiral)

#include "rs_graphics.rsh"

#pragma rs export_func(resize)

#define FOV 60.0f
#define SPIRAL_ROTATE_SPEED 15.0f
#define INNER_ROTATE_SPEED 1.5f
#define OUTER_ROTATE_SPEED 0.5f

// Vertex Shaders
rs_program_vertex gPVBackground;
rs_program_vertex gPVGeometry;

// Fragment Shaders
rs_program_fragment gPFBackground;
rs_program_fragment gPFGeometry;

// Blending and Depth testing
rs_program_store gPSBackground;
rs_program_store gPSGeometry;

// Meshes
rs_mesh gInnerGeometry;
rs_mesh gOuterGeometry;
rs_mesh gBackgroundMesh;

// Matrices
static rs_matrix4x4 gProjectionMatrix;
static rs_matrix4x4 gTransformedModelView;

// Textures
rs_allocation gPointTexture;

// Misc fields
float gXOffset;
float gNearPlane;
float gFarPlane;

// User defined data types
typedef struct VertexShaderConstants_s {
    rs_matrix4x4 modelViewProj;
    float maxPointSize;
    float farPlane;
} VertexShaderConstants;
VertexShaderConstants* gVSConstants;

typedef struct VertexColor_s {
    float3 position;
    float4 color;
} VertexColor;
VertexColor* VertexColor_s_dummy;

static float lastTime;
static float gInnerRotateAngle;
static float gOuterRotateAngle;
static float gWidth;
static float gHeight;

static float modulo360(float val) {
    static const INVERSE_360 = 1.0f / 360.0f;
    int multiplier = (int)(val * INVERSE_360);
    return val - (multiplier * 360.0f);
}

static void drawBackground() {
    rsgBindProgramVertex(gPVBackground);
    rsgBindProgramFragment(gPFBackground);
    rsgBindProgramStore(gPSBackground);

    rsgDrawMesh(gBackgroundMesh);
}

static void drawGeometry(float dt) {
    rsgBindProgramVertex(gPVGeometry);
    rsgBindProgramFragment(gPFGeometry);
    rsgBindProgramStore(gPSGeometry);

    rsgBindTexture(gPFGeometry, 0, gPointTexture);

    rs_matrix4x4 modelView;
    rs_matrix4x4 rotateModelView;

    rsMatrixLoad(&modelView, &gTransformedModelView);
    rsMatrixRotate(&modelView, gXOffset * -SPIRAL_ROTATE_SPEED, 0.0f, 1.0f, 0.0f);

    rsMatrixLoad(&rotateModelView, &modelView);
    {
        rsMatrixRotate(&rotateModelView, -gOuterRotateAngle, 0.0f, 0.0f, 1.0f);
        rsMatrixLoadMultiply(&gVSConstants->modelViewProj, &gProjectionMatrix, &rotateModelView);

        // Wrap the rotation so we don't go past 360
        gOuterRotateAngle = modulo360(gOuterRotateAngle + (dt * OUTER_ROTATE_SPEED));

        rsgDrawMesh(gOuterGeometry);
    }

    rsMatrixLoad(&rotateModelView, &modelView);
    {
        rsMatrixRotate(&rotateModelView, gInnerRotateAngle, 0.0f, 0.0f, 1.0f);
        rsMatrixLoadMultiply(&gVSConstants->modelViewProj, &gProjectionMatrix, &rotateModelView);

        // Wrap the rotation so we don't go past 360
        gInnerRotateAngle = modulo360(gInnerRotateAngle + (dt * INNER_ROTATE_SPEED));

        rsgDrawMesh(gInnerGeometry);
    }
}

void resize(float width, float height) {
    gWidth = width;
    gHeight = height;
    gVSConstants->farPlane = gFarPlane;
    rsMatrixLoadPerspective(&gProjectionMatrix, FOV, gWidth / gHeight, gNearPlane, gFarPlane);
}

void init() {
    gInnerRotateAngle = 0.0f;
    gOuterRotateAngle = 0.0f;

    gXOffset = 0.0f;
    lastTime = rsUptimeMillis();

    // Pre-calculate some fixed transformations
    rsMatrixLoadIdentity(&gTransformedModelView);
    rsMatrixTranslate(&gTransformedModelView, -3.0f, -5.0f, -18.0f);
    rsMatrixRotate(&gTransformedModelView, 20.0f, 0.0f, 1.0f, 0.0f);
    rsMatrixRotate(&gTransformedModelView, -10.0f, 1.0f, 0.0f, 0.0f);
}

int root(void) {
    float now = rsUptimeMillis();
    float elapsed = (now - lastTime) * 0.001f;
    lastTime = now;

    drawBackground();
    drawGeometry(elapsed);

    // Around 14 FPS
    return 70;
}
