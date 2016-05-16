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

#pragma rs java_package_name(com.android.musicvis.vis5)

#include "rs_graphics.rsh"

float gAngle;
int   gPeak;
float gRotate;
float gTilt;
int   gIdle;
int   gWaveCounter;

rs_program_vertex gPVBackground;
rs_program_fragment gPFBackgroundMip;
rs_program_fragment gPFBackgroundNoMip;
rs_program_raster gPR;

rs_allocation gTvumeter_background;
rs_allocation gTvumeter_peak_on;
rs_allocation gTvumeter_peak_off;
rs_allocation gTvumeter_needle;
rs_allocation gTvumeter_black;
rs_allocation gTvumeter_frame;
rs_allocation gTvumeter_album;

rs_program_store gPFSBackground;

typedef struct Vertex {
    float2 position;
    float2 texture0;
} Vertex_t;
Vertex_t *gPoints;


rs_allocation gPointBuffer;
rs_allocation gTlinetexture;
rs_mesh gCubeMesh;

#define RSID_POINTS 1

static void drawVU(rs_matrix4x4 *ident) {
    rs_matrix4x4 mat1;
    float scale = 0.0041;

    rsMatrixLoad(&mat1,ident);
    rsMatrixRotate(&mat1, 0.f, 0.f, 0.f, 1.f);
    rsMatrixScale(&mat1, scale, scale, scale);
    rsgProgramVertexLoadModelMatrix(&mat1);

    rsgBindProgramFragment(gPFBackgroundMip);
    rsgBindProgramStore(gPFSBackground);

    // draw the background image (416x233)
    rsgBindTexture(gPFBackgroundMip, 0, gTvumeter_background);
    rsgDrawQuadTexCoords(
            -208.0f, -33.0f, 600.0f,       // space
                0.0f, 1.0f,               // texture
            208, -33.0f, 600.0f,           // space
                1.0f, 1.0f,               // texture
            208, 200.0f, 600.0f,           // space
                1.0f, 0.0f,               // texture
            -208.0f, 200.0f, 600.0f,       // space
                0.0f, 0.0f);              // texture

    // draw the peak indicator light (56x58)
    if (gPeak > 0) {
        rsgBindTexture(gPFBackgroundMip, 0, gTvumeter_peak_on);
    } else {
        rsgBindTexture(gPFBackgroundMip, 0, gTvumeter_peak_off);
    }
    rsgDrawQuadTexCoords(
            140.0f, 70.0f, 600.0f,         // space
                0.0f, 1.0f,               // texture
            196, 70.0f, 600.0f,            // space
                1.0f, 1.0f,               // texture
            196, 128.0f, 600.0f,           // space
                1.0f, 0.0f,               // texture
            140.0f, 128.0f, 600.0f,        // space
                0.0f, 0.0f);              // texture



    // Draw the needle (88x262, center of rotation at 44,217 from top left)

    // set matrix so point of rotation becomes origin
    rsMatrixLoad(&mat1,ident);
    rsMatrixTranslate(&mat1, 0.f, -57.0f * scale, 0.f);
    rsMatrixRotate(&mat1, gAngle - 90.f, 0.f, 0.f, 1.f);
    rsMatrixScale(&mat1, scale, scale, scale);
    rsgProgramVertexLoadModelMatrix(&mat1);
    rsgBindTexture(gPFBackgroundMip, 0, gTvumeter_needle);
    rsgDrawQuadTexCoords(
            -44.0f, -102.0f+57.f, 600.0f,         // space
                0.0f, 1.0f,             // texture
            44.0f, -102.0f+57.f, 600.0f,             // space
                1.0f, 1.0f,              // texture
            44.0f, 160.0f+57.f, 600.0f,             // space
                1.0f, 0.0f,              // texture
            -44.0f, 160.0f+57.f, 600.0f,         // space
                0.0f, 0.0f);             // texture


    // restore matrix
    rsMatrixLoad(&mat1,ident);
    rsMatrixRotate(&mat1, 0.f, 0.f, 0.f, 1.f);
    rsMatrixScale(&mat1, scale, scale, scale);
    rsgProgramVertexLoadModelMatrix(&mat1);

    // erase the part of the needle we don't want to show
    rsgBindTexture(gPFBackgroundMip, 0, gTvumeter_black);
    rsgDrawQuad(-100.f, -55.f, 600.f,
             -100.f, -105.f, 600.f,
              100.f, -105.f, 600.f,
              100.f, -55.f, 600.f);


    // draw the frame (472x290)
    rsgBindTexture(gPFBackgroundMip, 0, gTvumeter_frame);
    rsgDrawQuadTexCoords(
            -236.0f, -60.0f, 600.0f,           // space
                0.0f, 1.0f,                  // texture
            236, -60.0f, 600.0f,               // space
                1.0f, 1.0f,                  // texture
            236, 230.0f, 600.0f,               // space
                1.0f, 0.0f,                  // texture
            -236.0f, 230.0f, 600.0f,           // space
                0.0f, 0.0f);                 // texture


}

int fadeoutcounter = 0;
int fadeincounter = 0;
int wave1pos = 0;
int wave1amp = 0;
int wave2pos = 0;
int wave2amp= 0;
int wave3pos = 0;
int wave3amp= 0;
int wave4pos = 0;
int wave4amp= 0;
float idle[4096];
int waveCounter = 0;
int lastuptime = 0;
float autorotation = 0;

#define FADEOUT_LENGTH 100
#define FADEOUT_FACTOR 0.95f
#define FADEIN_LENGTH 15

static void makeIdleWave(float *points) {
    int i;
    // show a number of superimposed moving sinewaves
    float amp1 = sin(0.007f * wave1amp) * 120 * 1024;
    float amp2 = sin(0.023f * wave2amp) * 80 * 1024;
    float amp3 = sin(0.011f * wave3amp) * 40 * 1024;
    float amp4 = sin(0.031f * wave4amp) * 20 * 1024;
    for (i = 0; i < 256; i++) {
        float val = sin(0.013f * (wave1pos + i * 4)) * amp1
                  + sin(0.029f * (wave2pos + i * 4)) * amp2;
        float off = sin(0.005f * (wave3pos + i * 4)) * amp3
                  + sin(0.017f * (wave4pos + i * 4)) * amp4;
        if (val < 2.f && val > -2.f) val = 2.f;
        points[i*8+1] = val + off;
        points[i*8+5] = -val + off;
    }
}


static void drawWave(rs_matrix4x4 *ident) {
    float scale = .008f;
    rs_matrix4x4 mat1;
    rsMatrixLoad(&mat1, ident);
    rsMatrixScale(&mat1, scale, scale / 2048.f, scale);
    rsMatrixTranslate(&mat1, 0.f, 81920.f, 350.f);
    rsgProgramVertexLoadModelMatrix(&mat1);
    int i;

    if (gIdle) {

        // idle state animation
        float *points = (float*)gPoints;
        if (fadeoutcounter > 0) {
            // fade waveform to 0
            for (i = 0; i < 256; i++) {
                float val = fabs(points[i*8+1]);
                val = val * FADEOUT_FACTOR;
                if (val < 2.f) val = 2.f;
                points[i*8+1] = val;
                points[i*8+5] = -val;
            }
            fadeoutcounter--;
            if (fadeoutcounter == 0) {
                wave1amp = 0;
                wave2amp = 0;
                wave3amp = 0;
                wave4amp = 0;
            }
        } else {
            // idle animation
            makeIdleWave(points);
        }
        fadeincounter = FADEIN_LENGTH;
    } else {
        if (fadeincounter > 0 && fadeoutcounter == 0) {
            // morph from idle animation back to waveform
            makeIdleWave(idle);
            if (waveCounter != gWaveCounter) {
                waveCounter = gWaveCounter;
                float *points = (float*)gPoints;
                for (i = 0; i < 256; i++) {
                    float val = fabs(points[i*8+1]);
                    points[i*8+1] = (val * (FADEIN_LENGTH - fadeincounter) + idle[i*8+1] * fadeincounter) / FADEIN_LENGTH;
                    points[i*8+5] = (-val * (FADEIN_LENGTH - fadeincounter) + idle[i*8+5] * fadeincounter) / FADEIN_LENGTH;
                }
            }
            fadeincounter--;
            if (fadeincounter == 0) {
                fadeoutcounter = FADEOUT_LENGTH;
            }
        } else {
            fadeoutcounter = FADEOUT_LENGTH;
        }
    }

    rsgBindProgramRaster(gPR);
    rsgBindProgramFragment(gPFBackgroundNoMip);
    rsgBindTexture(gPFBackgroundNoMip, 0, gTlinetexture);
    rsgDrawMesh(gCubeMesh);
}


static void drawVizLayer(rs_matrix4x4 *ident) {
    for (int i = 0; i < 6; i++) {
        if (i & 1) {
            drawVU(ident);
        } else {
            drawWave(ident);
        }

        rsMatrixRotate(ident, 60.f, 0.f, 1.f, 0.f);
    }
}


int root(void) {
    rsgClearColor(0.f, 0.f, 0.f, 1.f);

    rsgBindProgramVertex(gPVBackground);

    int i;
    rs_matrix4x4 ident;
    int now = (int)rsUptimeMillis();
    int delta = now - lastuptime;
    lastuptime = now;
    if (delta > 80) {
        // Limit the delta to avoid jumps when coming back from sleep.
        // A value of 80 will make the rotation keep the same speed
        // until the frame rate drops to 12.5 fps, at which point it
        // will start slowing down.
        delta = 80;
    }
    autorotation += .3 * delta / 35;
    while (autorotation > 360.f) autorotation -= 360.f;

    rsMatrixLoadIdentity(&ident);
    rsMatrixRotate(&ident, gTilt, 1.f, 0.f, 0.f);
    rsMatrixRotate(&ident, autorotation + gRotate, 0.f, 1.f, 0.f);

    // draw the reflections
    rsMatrixTranslate(&ident, 0.f, -1.f, 0.f);
    rsMatrixScale(&ident, 1.f, -1.f, 1.f);
    drawVizLayer(&ident);

    // draw the reflecting plane
    rsgBindProgramFragment(gPFBackgroundMip);
    rsgBindTexture(gPFBackgroundMip, 0, gTvumeter_album);
    rsgDrawQuadTexCoords(
            -1500.0f, -60.0f, 1500.0f,           // space
                0.f, 1.f,    // texture
            1500, -60.0f, 1500.0f,               // space
                1.f, 1.f,    // texture
            1500, -60.0f, -1500.0f,               // space
                1.f, 0.f,    // texture
            -1500.0f, -60.0f, -1500.0f,           // space
                0.f, 0.f);   // texture

    // draw the visualizer
    rsMatrixScale(&ident, 1.f, -1.f, 1.f);
    rsMatrixTranslate(&ident, 0.f, 1.f, 0.f);

    drawVizLayer(&ident);

    wave1pos++;
    wave1amp++;
    wave2pos--;
    wave2amp++;
    wave3pos++;
    wave3amp++;
    wave4pos++;
    wave4amp++;

    return 1;
}
