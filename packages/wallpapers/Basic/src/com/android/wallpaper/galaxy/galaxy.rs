// Copyright (C) 2009 The Android Open Source Project
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

#pragma rs java_package_name(com.android.wallpaper.galaxy)

#include "rs_graphics.rsh"

#pragma stateVertex(parent);
#pragma stateStore(parent);

#define ELLIPSE_RATIO 0.892f
#define PI 3.1415f
#define TWO_PI 6.283f
#define ELLIPSE_TWIST 0.023333333f

static float angle = 50.f;
static int gOldWidth;
static int gOldHeight;
static int gWidth;
static int gHeight;
static float gSpeed[12000];
static int gGalaxyRadius = 300;
static rs_allocation gParticlesBuffer;

float gXOffset;
int gIsPreview;

rs_program_fragment gPFBackground;
rs_program_fragment gPFStars;
rs_program_vertex gPVStars;
rs_program_vertex gPVBkProj;
rs_program_store gPSLights;

rs_allocation gTSpace;
rs_allocation gTFlares;
rs_allocation gTLight1;
rs_mesh gParticlesMesh;

typedef struct __attribute__((packed, aligned(4))) Particle {
    uchar4 color;
    float3 position;
} Particle_t;
Particle_t *Particles;

typedef struct VpConsts {
    rs_matrix4x4 Proj;
    rs_matrix4x4 MVP;
} VpConsts_t;
VpConsts_t *vpConstants;

static float mapf(float minStart, float minStop, float maxStart, float maxStop, float value) {
    return maxStart + (maxStart - maxStop) * ((value - minStart) / (minStop - minStart));
}

/**
 * Helper function to generate the stars.
 */
static float randomGauss() {
    float x1;
    float x2;
    float w = 2.f;

    while (w >= 1.0f) {
        x1 = rsRand(2.0f) - 1.0f;
        x2 = rsRand(2.0f) - 1.0f;
        w = x1 * x1 + x2 * x2;
    }

    w = sqrt(-2.0f * log(w) / w);
    return x1 * w;
}

/**
 * Generates the properties for a given star.
 */
static void createParticle(Particle_t *part, int idx, float scale) {
    float d = fabs(randomGauss()) * gGalaxyRadius * 0.5f + rsRand(64.0f);
    float id = d / gGalaxyRadius;
    float z = randomGauss() * 0.4f * (1.0f - id);
    float p = -d * ELLIPSE_TWIST;

    if (d < gGalaxyRadius * 0.33f) {
        part->color.x = (uchar) (220 + id * 35);
        part->color.y = 220;
        part->color.z = 220;
    } else {
        part->color.x = 180;
        part->color.y = 180;
        part->color.z = (uchar) clamp(140.f + id * 115.f, 140.f, 255.f);
    }
    // Stash point size * 10 in Alpha
    part->color.w = (uchar) (rsRand(1.2f, 2.1f) * 60);

    if (d > gGalaxyRadius * 0.15f) {
        z *= 0.6f * (1.0f - id);
    } else {
        z *= 0.72f;
    }

    // Map to the projection coordinates (viewport.x = -1.0 -> 1.0)
    d = mapf(-4.0f, gGalaxyRadius + 4.0f, 0.0f, scale, d);

    part->position.x = rsRand(TWO_PI);
    part->position.y = d;
    gSpeed[idx] = rsRand(0.0015f, 0.0025f) * (0.5f + (scale / d)) * 0.8f;

    part->position.z = z / 5.0f;
}

/**
 * Initialize all the stars. Called from Java.
 */
void initParticles() {
    if (gIsPreview == 1) {
        angle = 0.0f;
    }

    Particle_t *part = Particles;
    float scale = gGalaxyRadius / (gWidth * 0.5f);
    int count = rsAllocationGetDimX(gParticlesBuffer);
    for (int i = 0; i < count; i ++) {
        createParticle(part, i, scale);
        part++;
    }
}

static void drawSpace() {
    rsgBindProgramFragment(gPFBackground);
    rsgBindTexture(gPFBackground, 0, gTSpace);
    rsgDrawQuadTexCoords(
            0.0f, 0.0f, 0.0f, 0.0f, 1.0f,
            gWidth, 0.0f, 0.0f, 2.0f, 1.0f,
            gWidth, gHeight, 0.0f, 2.0f, 0.0f,
            0.0f, gHeight, 0.0f, 0.0f, 0.0f);
}

static void calcMatrix(rs_matrix4x4 *out, float offset) {
    float a = offset * angle;
    float absoluteAngle = fabs(a);

    rsMatrixLoadTranslate(out, 0.0f, 0.0f, 10.0f - 6.0f * absoluteAngle / 50.0f);
    if (gHeight > gWidth) {
        rsMatrixScale(out, 6.6f, 6.0f, 1.0f);
    } else {
        rsMatrixScale(out, 12.6f, 12.0f, 1.0f);
    }
    rsMatrixRotate(out, absoluteAngle, 1.0f, 0.0f, 0.0f);
    rsMatrixRotate(out, a, 0.0f, 0.4f, 0.1f);
}

static void drawLights(const rs_matrix4x4 *m) {
    rsgBindProgramVertex(gPVBkProj);
    rsgBindProgramFragment(gPFBackground);
    rsgBindTexture(gPFBackground, 0, gTLight1);
    rsgProgramVertexLoadModelMatrix(m);

    float sx = (512.0f / gWidth) * 1.1f;
    float sy = (512.0f / gWidth) * 1.2f;
    rsgDrawQuad(-sx, -sy, 0.0f,
                 sx, -sy, 0.0f,
                 sx,  sy, 0.0f,
                -sx,  sy, 0.0f);
}

static void drawParticles(const rs_matrix4x4 *m) {
    rsMatrixLoad(&vpConstants->MVP, &vpConstants->Proj);
    rsMatrixMultiply(&vpConstants->MVP, m);
    rsgAllocationSyncAll(rsGetAllocation(vpConstants));

    rsgBindProgramVertex(gPVStars);
    rsgBindProgramFragment(gPFStars);
    rsgBindProgramStore(gPSLights);
    rsgBindTexture(gPFStars, 0, gTFlares);

    Particle_t *vtx = Particles;
    int count = rsAllocationGetDimX(gParticlesBuffer);
    for (int i = 0; i < count; i++) {
        vtx->position.x = vtx->position.x + gSpeed[i];
        vtx++;
    }

    rsgDrawMesh(gParticlesMesh);
}

int root() {
    rsgClearColor(0.f, 0.f, 0.f, 1.f);

    gParticlesBuffer = rsGetAllocation(Particles);
    rsgBindProgramFragment(gPFBackground);

    gWidth = rsgGetWidth();
    gHeight = rsgGetHeight();
    if ((gWidth != gOldWidth) || (gHeight != gOldHeight)) {
        initParticles();
        gOldWidth = gWidth;
        gOldHeight = gHeight;
    }

    drawSpace();

    rs_matrix4x4 matrix;
    calcMatrix(&matrix, mix(-0.5f, 0.5f, gXOffset));
    drawParticles(&matrix);
    drawLights(&matrix);

    return 45;
}
