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

#pragma rs java_package_name(com.android.wallpaper.grass)

#include "rs_graphics.rsh"

#define RSID_BLADES_BUFFER 2

#define TESSELATION 0.5f
#define HALF_TESSELATION 0.25f
#define MAX_BEND 0.09f
#define SECONDS_IN_DAY 86400.0f
#define PI 3.1415926f
#define HALF_PI 1.570796326f
#define REAL_TIME 1

int gBladesCount;
int gIndexCount;
int gWidth;
int gHeight;
float gXOffset;
float gDawn;
float gMorning;
float gAfternoon;
float gDusk;
int gIsPreview;
rs_program_vertex gPVBackground;
rs_program_fragment gPFBackground;
rs_program_fragment gPFGrass;
rs_program_store gPSBackground;
rs_allocation gTNight;
rs_allocation gTSunset;
rs_allocation gTSunrise;
rs_allocation gTSky;
rs_allocation gTAa;
rs_mesh gBladesMesh;


typedef struct Blade {
    float angle;
    int size;
    float xPos;
    float yPos;
    float offset;
    float scale;
    float lengthX;
    float lengthY;
    float hardness;
    float h;
    float s;
    float b;
    float turbulencex;
} Blade_t;
Blade_t *Blades;

typedef struct RS_PACKED Vertex {
    uchar4 color;
    float2 position;
    float2 texture0;
} __attribute__((packed,aligned(4))) Vertex_t;
Vertex_t *Verticies;

#define B 0x100
#define BM 0xff
#define N 0x1000

static int p[B + B + 2];
static float g3[B + B + 2][3];
static float g2[B + B + 2][2];
static float g1[B + B + 2];

static float noise_sCurve(float t)
{
    return t * t * (3.0f - 2.0f * t);
}

static void normalizef2(float v[])
{
    float s = (float)sqrt(v[0] * v[0] + v[1] * v[1]);
    v[0] = v[0] / s;
    v[1] = v[1] / s;
}

static void normalizef3(float v[])
{
    float s = (float)sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
    v[0] = v[0] / s;
    v[1] = v[1] / s;
    v[2] = v[2] / s;
}

void init()
{
    int i, j, k;

    for (i = 0; i < B; i++) {
        p[i] = i;

        g1[i] = (float)(rsRand(B * 2) - B) / B;

        for (j = 0; j < 2; j++)
            g2[i][j] = (float)(rsRand(B * 2) - B) / B;
        normalizef2(g2[i]);

        for (j = 0; j < 3; j++)
            g3[i][j] = (float)(rsRand(B * 2) - B) / B;
        normalizef3(g3[i]);
    }

    for (i = B-1; i >= 0; i--) {
        k = p[i];
        p[i] = p[j = rsRand(B)];
        p[j] = k;
    }

    for (i = 0; i < B + 2; i++) {
        p[B + i] = p[i];
        g1[B + i] = g1[i];
        for (j = 0; j < 2; j++)
            g2[B + i][j] = g2[i][j];
        for (j = 0; j < 3; j++)
            g3[B + i][j] = g3[i][j];
    }
}

static float noisef(float x)
{
    int bx0, bx1;
    float rx0, rx1, sx, t, u, v;

    t = x + N;
    bx0 = ((int)t) & BM;
    bx1 = (bx0+1) & BM;
    rx0 = t - (int)t;
    rx1 = rx0 - 1.0f;

    sx = noise_sCurve(rx0);

    u = rx0 * g1[p[bx0]];
    v = rx1 * g1[p[bx1]];
    return 2.3f * mix(u, v, sx);
}

static float noisef2(float x, float y)
{
    int bx0, bx1, by0, by1, b00, b10, b01, b11;
    float rx0, rx1, ry0, ry1, sx, sy, a, b, t, u, v;
    float *q;
    int i, j;

    t = x + N;
    bx0 = ((int)t) & BM;
    bx1 = (bx0+1) & BM;
    rx0 = t - (int)t;
    rx1 = rx0 - 1.0f;

    t = y + N;
    by0 = ((int)t) & BM;
    by1 = (by0+1) & BM;
    ry0 = t - (int)t;
    ry1 = ry0 - 1.0f;

    i = p[bx0];
    j = p[bx1];

    b00 = p[i + by0];
    b10 = p[j + by0];
    b01 = p[i + by1];
    b11 = p[j + by1];

    sx = noise_sCurve(rx0);
    sy = noise_sCurve(ry0);

    q = g2[b00]; u = rx0 * q[0] + ry0 * q[1];
    q = g2[b10]; v = rx1 * q[0] + ry0 * q[1];
    a = mix(u, v, sx);

    q = g2[b01]; u = rx0 * q[0] + ry1 * q[1];
    q = g2[b11]; v = rx1 * q[0] + ry1 * q[1];
    b = mix(u, v, sx);

    return 1.5f * mix(a, b, sy);
}

static float turbulencef2(float x, float y, float octaves)
{
    float t = 0.0f;

    for (float f = 1.0f; f <= octaves; f *= 2)
        t += fabs(noisef2(f * x, f * y)) / f;
    return t;
}

void updateBlades()
{
    Blade_t *bladeStruct = Blades;
    for (int i = 0; i < gBladesCount; i ++) {
        float xpos = rsRand(-gWidth, gWidth);
        bladeStruct->xPos = xpos;
        bladeStruct->turbulencex = xpos * 0.006f;
        bladeStruct->yPos = gHeight;
        bladeStruct++;
    }
}

static float time(int isPreview) {
    if (REAL_TIME && !isPreview) {
        rs_time_t currentTime = rsTime(0);
        rs_tm localTime;
        rsLocaltime(&localTime, &currentTime);
        return (localTime.tm_hour * 3600.0f +
                localTime.tm_min * 60.0f +
                localTime.tm_sec) / SECONDS_IN_DAY;
    }
    float t = rsUptimeMillis() / 30000.0f;
    return t - (int) t;
}

static void alpha(float a) {
    rsgProgramFragmentConstantColor(gPFBackground, 1.0f, 1.0f, 1.0f, a);
}

static float normf(float start, float stop, float value) {
    return (value - start) / (stop - start);
}

static void drawNight(int width, int height) {
    rsgBindTexture(gPFBackground, 0, gTNight);
    rsgDrawQuadTexCoords(
            0.0f, -32.0f, 0.0f,
            0.0f, 1.0f,
            0.0f, height, 0.0f,
            0.0f, 0.0f,
            width, height, 0.0f,
            2.0f, 0.0f,
            width, -32.0f, 0.0f,
            2.0f, 1.0f);
}

static void drawSunrise(int width, int height) {
    rsgBindTexture(gPFBackground, 0, gTSunrise);
    rsgDrawRect(0.0f, 0.0f, width, height, 0.0f);
}

static void drawNoon(int width, int height) {
    rsgBindTexture(gPFBackground, 0, gTSky);
    rsgDrawRect(0.0f, 0.0f, width, height, 0.0f);
}

static void drawSunset(int width, int height) {
    rsgBindTexture(gPFBackground, 0, gTSunset);
    rsgDrawRect(0.0f, 0.0f, width, height, 0.0f);
}


static uchar4 hsbToRgb(float h, float s, float b)
{
    float red = 0.0f;
    float green = 0.0f;
    float blue = 0.0f;

    float x = h;
    float y = s;
    float z = b;

    float hf = (x - (int) x) * 6.0f;
    int ihf = (int) hf;
    float f = hf - ihf;
    float pv = z * (1.0f - y);
    float qv = z * (1.0f - y * f);
    float tv = z * (1.0f - y * (1.0f - f));

    switch (ihf) {
        case 0:         // Red is the dominant color
            red = z;
            green = tv;
            blue = pv;
            break;
        case 1:         // Green is the dominant color
            red = qv;
            green = z;
            blue = pv;
            break;
        case 2:
            red = pv;
            green = z;
            blue = tv;
            break;
        case 3:         // Blue is the dominant color
            red = pv;
            green = qv;
            blue = z;
            break;
        case 4:
            red = tv;
            green = pv;
            blue = z;
            break;
        case 5:         // Red is the dominant color
            red = z;
            green = pv;
            blue = qv;
            break;
    }

    return rsPackColorTo8888(red, green, blue);
}

static int drawBlade(Blade_t *bladeStruct, Vertex_t *v,
        float brightness, float xOffset, float now) {

    float scale = bladeStruct->scale;
    float angle = bladeStruct->angle;
    float xpos = bladeStruct->xPos + xOffset;
    int size = bladeStruct->size;

    uchar4 color = hsbToRgb(bladeStruct->h, bladeStruct->s,
                            mix(0.f, bladeStruct->b, brightness));

    float newAngle = (turbulencef2(bladeStruct->turbulencex, now, 4.0f) - 0.5f) * 0.5f;
    angle = clamp(angle + (newAngle + bladeStruct->offset - angle) * 0.15f, -MAX_BEND, MAX_BEND);

    float currentAngle = HALF_PI;

    float bottomX = xpos;
    float bottomY = bladeStruct->yPos;

    float d = angle * bladeStruct->hardness;


    float si = size * scale;
    float bottomLeft = bottomX - si;
    float bottomRight = bottomX + si;
    float bottom = bottomY + HALF_TESSELATION;

    v[0].color = color;                          // V1.ABGR
    v[0].position.x = bottomLeft;                    // V1.X
    v[0].position.y = bottom;                        // V1.Y
    v[0].texture0.x = 0.f;                           // V1.s
    v[0].texture0.y = 0.f;                           // V1.t
                                                    //
    v[1].color = color;                          // V2.ABGR
    v[1].position.x = bottomRight;                   // V2.X
    v[1].position.y = bottom;                        // V2.Y
    v[1].texture0.x = 1.f;                           // V2.s
    v[1].texture0.y = 0.f;                           // V2.t
    v += 2;

    for ( ; size > 0; size -= 1) {
        float topX = bottomX - cos(currentAngle) * bladeStruct->lengthX;
        float topY = bottomY - sin(currentAngle) * bladeStruct->lengthY;

        si = (float)size * scale;
        float spi = si - scale;

        float topLeft = topX - spi;
        float topRight = topX + spi;

        v[0].color = color;                          // V1.ABGR
        v[0].position.x = topLeft;                       // V1.X
        v[0].position.y = topY;                          // V1.Y
        v[0].texture0.x = 0.f;                           // V1.s
        v[0].texture0.y = 0.f;                           // V1.t

        v[1].color = color;                          // V2.ABGR
        v[1].position.x = topRight;                      // V2.X
        v[1].position.y = topY;                          // V2.Y
        v[1].texture0.x = 1.f;                           // V2.s
        v[1].texture0.y = 0.f;                           // V2.t

        v += 2;
        bottomX = topX;
        bottomY = topY;
        currentAngle += d;
    }

    bladeStruct->angle = angle;

    // 2 vertices per triangle, 5 properties per vertex (RGBA, X, Y, S, T)
    return bladeStruct->size * 2 + 2;
}

static void drawBlades(float brightness, float xOffset) {
    // For anti-aliasing
    rsgBindTexture(gPFGrass, 0, gTAa);

    Blade_t *bladeStruct = Blades;
    Vertex_t *vtx = Verticies;
    float now = rsUptimeMillis() * 0.00004f;

    for (int i = 0; i < gBladesCount; i += 1) {
        int offset = drawBlade(bladeStruct, vtx, brightness, xOffset, now);
        vtx += offset;
        bladeStruct ++;
    }

    rsgDrawMesh(gBladesMesh, 0, 0, gIndexCount);
}

int root(void) {
    float x = mix((float)gWidth, 0.f, gXOffset);

    float now = time(gIsPreview);

    rsgBindProgramVertex(gPVBackground);
    rsgBindProgramFragment(gPFBackground);
    rsgBindProgramStore(gPSBackground);
    alpha(1.0f);

    float newB = 1.0f;
    if (now >= 0.0f && now < gDawn) {                    // Draw night
        drawNight(gWidth, gHeight);
        newB = 0.0f;
    } else if (now >= gDawn && now <= gMorning) {         // Draw sunrise
        float half = gDawn + (gMorning - gDawn) * 0.5f;
        if (now <= half) {                              // Draw night->sunrise
            drawNight(gWidth, gHeight);
            newB = normf(gDawn, half, now);
            alpha(newB);
            drawSunrise(gWidth, gHeight);
        } else {                                        // Draw sunrise->day
            drawSunrise(gWidth, gHeight);
            alpha(normf(half, gMorning, now));
            drawNoon(gWidth, gHeight);
        }
    } else if (now > gMorning && now < gAfternoon) {      // Draw day
        drawNoon(gWidth, gHeight);
    } else if (now >= gAfternoon && now <= gDusk) {       // Draw sunset
        float half = gAfternoon + (gDusk - gAfternoon) * 0.5f;
        if (now <= half) {                              // Draw day->sunset
            drawNoon(gWidth, gHeight);
            newB = normf(gAfternoon, half, now);
            alpha(newB);
            newB = 1.0f - newB;
            drawSunset(gWidth, gHeight);
        } else {                                        // Draw sunset->night
            drawSunset(gWidth, gHeight);
            alpha(normf(half, gDusk, now));
            drawNight(gWidth, gHeight);
            newB = 0.0f;
        }
    } else if (now > gDusk) {                            // Draw night
        drawNight(gWidth, gHeight);
        newB = 0.0f;
    }

    rsgBindProgramFragment(gPFGrass);
    drawBlades(newB, x);

    return 50;
}
