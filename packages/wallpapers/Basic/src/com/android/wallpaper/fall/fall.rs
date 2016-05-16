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

#pragma rs java_package_name(com.android.wallpaper.fall)

#include "rs_graphics.rsh"

#define LEAVES_TEXTURES_COUNT 8
#define LEAF_SIZE 0.55f
#define LEAVES_COUNT 14

// Things we need to set from the application
float g_glWidth;
float g_glHeight;
float g_meshWidth;
float g_meshHeight;
float g_xOffset;
float g_rotate;

rs_program_vertex g_PVWater;
rs_program_vertex g_PVSky;

rs_program_fragment g_PFSky;
rs_program_store g_PFSLeaf;
rs_program_fragment g_PFBackground;

rs_allocation g_TLeaves;
rs_allocation g_TRiverbed;

rs_mesh g_WaterMesh;

typedef struct Constants {
    float4 Drop01;
    float4 Drop02;
    float4 Drop03;
    float4 Drop04;
    float4 Drop05;
    float4 Drop06;
    float4 Drop07;
    float4 Drop08;
    float4 Drop09;
    float4 Drop10;
    float4 Offset;
    float Rotate;
} Constants_t;

Constants_t *g_Constants;
rs_program_store g_PFSBackground;

//float skyOffsetX;
//float skyOffsetY;
static float g_DT;
static int64_t g_LastTime;

typedef struct Drop {
    float ampS;
    float ampE;
    float spread;
    float x;
    float y;
} Drop_t;
static Drop_t gDrops[10];
static int gMaxDrops;

typedef struct Leaves {
    float x;
    float y;
    float scale;
    float angle;
    float spin;
    float u1;
    float u2;
    float altitude;
    float rippled;
    float deltaX;
    float deltaY;
    int newLeaf;
} Leaves_t;

static Leaves_t gLeavesStore[LEAVES_COUNT];
static Leaves_t* gLeaves[LEAVES_COUNT];
static Leaves_t* gNextLeaves[LEAVES_COUNT];

void initLeaves() {
    Leaves_t *leaf = gLeavesStore;
    // globals haven't been set at this point yet. We need to find the correct
    // function index to call this, we can wait until reflection works
    float width = 2; //g_glWidth;
    float height = 3.333; //g_glHeight;

    int i;
    for (i = 0; i < LEAVES_COUNT; i ++) {
        gLeaves[i] = leaf;
        int sprite = rsRand(LEAVES_TEXTURES_COUNT);
        leaf->x = rsRand(-width, width);
        leaf->y = rsRand(-height * 0.5f, height * 0.5f);
        leaf->scale = rsRand(0.4f, 0.5f);
        leaf->angle = rsRand(0.0f, 360.0f);
        leaf->spin = degrees(rsRand(-0.02f, 0.02f)) * 0.25f;
        leaf->u1 = (float)sprite / (float) LEAVES_TEXTURES_COUNT;
        leaf->u2 = (float)(sprite + 1) / (float) LEAVES_TEXTURES_COUNT;
        leaf->altitude = -1.0f;
        leaf->rippled = 1.0f;
        leaf->deltaX = rsRand(-0.01f, 0.01f);
        leaf->deltaY = -rsRand(0.036f, 0.044f);
        leaf++;
    }
}

void init() {
    int ct;
    gMaxDrops = 10;
    for (ct=0; ct<gMaxDrops; ct++) {
        gDrops[ct].ampS = 0;
        gDrops[ct].ampE = 0;
        gDrops[ct].spread = 1;
    }

    initLeaves();
    g_LastTime = rsUptimeMillis();
    g_DT = 0.1f;
}

static void updateDrop(int ct) {
    gDrops[ct].spread += 30.f * g_DT;
    gDrops[ct].ampE = gDrops[ct].ampS / gDrops[ct].spread;
}

static void drop(int x, int y, float s) {
    int ct;
    int iMin = 0;
    float minAmp = 10000.f;
    for (ct = 0; ct < gMaxDrops; ct++) {
        if (gDrops[ct].ampE < minAmp) {
            iMin = ct;
            minAmp = gDrops[ct].ampE;
        }
    }
    gDrops[iMin].ampS = s;
    gDrops[iMin].spread = 0;
    gDrops[iMin].x = x;
    gDrops[iMin].y = g_meshHeight - y - 1;
    updateDrop(iMin);
}

static void generateRipples() {
    int ct;
    for (ct = 0; ct < gMaxDrops; ct++) {
        Drop_t * d = &gDrops[ct];
        float *v = (float*)&g_Constants->Drop01;
        v += ct*4;
        *(v++) = d->x;
        *(v++) = d->y;
        *(v++) = d->ampE * 0.12f;
        *(v++) = d->spread;
    }
    g_Constants->Offset.x = g_xOffset;

    for (ct = 0; ct < gMaxDrops; ct++) {
        updateDrop(ct);
    }
}

static void genLeafDrop(Leaves_t *leaf, float amp) {
    float nx = (leaf->x + g_glWidth * 0.5f) / g_glWidth;
    float ny = (leaf->y + g_glHeight * 0.5f) / g_glHeight;
    drop(nx * g_meshWidth, g_meshHeight - ny * g_meshHeight, amp);
}

static int drawLeaf(Leaves_t *leaf) {

    float x = leaf->x;
    float y = leaf->y;

    float u1 = leaf->u1;
    float u2 = leaf->u2;

    float a = leaf->altitude;
    float s = leaf->scale;
    float r = leaf->angle;

    float tz = 0.0f;
    if (a > 0.0f) {
        tz = -a;
    }

    rs_matrix4x4 matrix;
    if (a > 0.0f) {

        float alpha = 1.0f;
        if (a >= 0.4f) alpha = 1.0f - (a - 0.4f) / 0.1f;

        rsgProgramFragmentConstantColor(g_PFSky, 0.0f, 0.0f, 0.0f, alpha * 0.15f);

        rsMatrixLoadIdentity(&matrix);
        if (!g_rotate) {
            rsMatrixTranslate(&matrix, x - g_xOffset * 2, y, 0);
        } else {
            rsMatrixTranslate(&matrix, x, y, 0);
            rsMatrixRotate(&matrix, 90.0f, 0.0f, 0.0f, 1.0f);
        }

        float shadowOffet = a * 0.2f;

        rsMatrixScale(&matrix, s, s, 1.0f);
        rsMatrixRotate(&matrix, r, 0.0f, 0.0f, 1.0f);
        rsgProgramVertexLoadModelMatrix(&matrix);

        rsgDrawQuadTexCoords(-LEAF_SIZE, -LEAF_SIZE, 0, u1, 1.0f,
                           LEAF_SIZE, -LEAF_SIZE, 0, u2, 1.0f,
                           LEAF_SIZE,  LEAF_SIZE, 0, u2, 0.0f,
                          -LEAF_SIZE,  LEAF_SIZE, 0, u1, 0.0f);

        rsgProgramFragmentConstantColor(g_PFSky, 1.0f, 1.0f, 1.0f, alpha);
    } else {
        rsgProgramFragmentConstantColor(g_PFSky, 1.0f, 1.0f, 1.0f, 1.0f);
    }

    rsMatrixLoadIdentity(&matrix);
    if (!g_rotate) {
        rsMatrixTranslate(&matrix, x - g_xOffset * 2, y, tz);
    } else {
        rsMatrixTranslate(&matrix, x, y, tz);
        rsMatrixRotate(&matrix, 90.0f, 0.0f, 0.0f, 1.0f);
    }
    rsMatrixScale(&matrix, s, s, 1.0f);
    rsMatrixRotate(&matrix, r, 0.0f, 0.0f, 1.0f);
    rsgProgramVertexLoadModelMatrix(&matrix);

    rsgDrawQuadTexCoords(-LEAF_SIZE, -LEAF_SIZE, 0, u1, 1.0f,
                       LEAF_SIZE, -LEAF_SIZE, 0, u2, 1.0f,
                       LEAF_SIZE,  LEAF_SIZE, 0, u2, 0.0f,
                      -LEAF_SIZE,  LEAF_SIZE, 0, u1, 0.0f);

    float spin = leaf->spin;
    if (a <= 0.0f) {
        float rippled = leaf->rippled;
        if (rippled < 0.0f) {
            genLeafDrop(leaf, 1.5f);
            //drop(((x + g_glWidth * 0.5f) / g_glWidth) * meshWidth,
            //     meshHeight - ((y + g_glHeight * 0.5f) / g_glHeight) * meshHeight, 1);
            spin *= 0.25f;
            leaf->spin = spin;
            leaf->rippled = 1.0f;
        }
        leaf->x = x + leaf->deltaX * g_DT;
        leaf->y = y + leaf->deltaY * g_DT;
        r += spin;
        leaf->angle = r;
    } else {
        a -= 0.15f * g_DT;
        leaf->altitude = a;
        r += spin * 2.0f;
        leaf->angle = r;
    }

    int newLeaf = 0;
    if (-LEAF_SIZE * s + x > g_glWidth || LEAF_SIZE * s + x < -g_glWidth ||
            LEAF_SIZE * s + y < -g_glHeight * 0.5f) {

        int sprite = rsRand(LEAVES_TEXTURES_COUNT);

        leaf->x = rsRand(-g_glWidth, g_glWidth);
        leaf->y = rsRand(-g_glHeight * 0.5f, g_glHeight * 0.5f);

        leaf->scale = rsRand(0.4f, 0.5f);
        leaf->spin = degrees(rsRand(-0.02f, 0.02f)) * 0.35f;
        leaf->u1 = sprite / (float) LEAVES_TEXTURES_COUNT;
        leaf->u2 = (sprite + 1) / (float) LEAVES_TEXTURES_COUNT;
        leaf->altitude = 0.7f;
        leaf->rippled = -1.0f;
        leaf->deltaX = rsRand(-0.01f, 0.01f);
        leaf->deltaY = -rsRand(0.036f, 0.044f);
        leaf->newLeaf = 1;
        newLeaf = 1;
    }
    return newLeaf;
}

static void drawLeaves() {
    rsgBindProgramFragment(g_PFSky);
    rsgBindProgramStore(g_PFSLeaf);
    rsgBindProgramVertex(g_PVSky);
    rsgBindTexture(g_PFSky, 0, g_TLeaves);

    int newLeaves = 0;
    int i = 0;
    for ( ; i < LEAVES_COUNT; i += 1) {
        if (drawLeaf(gLeaves[i])) {
            newLeaves = 1;

        }
    }

    if (newLeaves > 0) {
        int index = 0;

        // Copy all the old leaves to the beginning of gNextLeaves
        for (i=0; i < LEAVES_COUNT; i++) {
            if (gLeaves[i]->newLeaf == 0) {
                gNextLeaves[index] = gLeaves[i];
                index++;
            }
        }

        // Now copy all the newly falling leaves to the end of gNextLeaves
        for (i=0; i < LEAVES_COUNT; i++) {
            if (gLeaves[i]->newLeaf > 0) {
                gNextLeaves[index] = gLeaves[i];
                gNextLeaves[index]->newLeaf = 0;
                index++;
            }
        }

        // And move everything in gNextLeaves back to gLeaves
        for (i=0; i < LEAVES_COUNT; i++) {
            gLeaves[i] = gNextLeaves[i];
        }
    }

    rs_matrix4x4 matrix;
    rsMatrixLoadIdentity(&matrix);
    rsgProgramVertexLoadModelMatrix(&matrix);
}

static void drawRiverbed() {
    rsgBindProgramFragment(g_PFBackground);
    rsgBindProgramStore(g_PFSBackground);
    rsgBindTexture(g_PFBackground, 0, g_TRiverbed);
    rsgDrawMesh(g_WaterMesh);
}

void addDrop(int x, int y) {
    drop(x, y, 2);
}

int root(void) {
    rsgClearColor(0.f, 0.f, 0.f, 1.f);

    // Compute dt in seconds.
    int64_t newTime = rsUptimeMillis();
    g_DT = (newTime - g_LastTime) * 0.001f;
    g_DT = min(g_DT, 0.2f);
    g_LastTime = newTime;

    g_Constants->Rotate = (float) g_rotate;

    int ct;
    int add = 0;
    for (ct = 0; ct < gMaxDrops; ct++) {
        if (gDrops[ct].ampE < 0.005f) {
            add = 1;
        }
    }

    if (add) {
        int i = (int)rsRand(LEAVES_COUNT);
        genLeafDrop(gLeaves[i], rsRand(0.3f) + 0.1f);
    }

    rsgBindProgramVertex(g_PVWater);
    generateRipples();
    rsgAllocationSyncAll(rsGetAllocation(g_Constants));
    drawRiverbed();

    drawLeaves();

    return 50;
}
