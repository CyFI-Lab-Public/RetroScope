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

#pragma rs java_package_name(com.android.musicvis)

#include "rs_graphics.rsh"

float gYRotation;
int gIdle;
int gWaveCounter;
int gWidth;

rs_program_vertex gPVBackground;
rs_program_fragment gPFBackground;

typedef struct Vertex {
    float2 position;
    float2 texture0;
} Vertex_t;
Vertex_t *gPoints;

rs_allocation gPointBuffer;
rs_allocation gTlinetexture;
rs_mesh gCubeMesh;

#define RSID_POINTS 1

#define FADEOUT_LENGTH 100
#define FADEOUT_FACTOR 0.95f
#define FADEIN_LENGTH 15

static int fadeoutcounter = 0;
static int fadeincounter = 0;
static int wave1pos = 0;
static int wave1amp = 0;
static int wave2pos = 0;
static int wave2amp= 0;
static int wave3pos = 0;
static int wave3amp= 0;
static int wave4pos = 0;
static int wave4amp= 0;
static float idle[8192];
static int waveCounter = 0;

static void makeIdleWave(float *points) {
    // show a number of superimposed moving sinewaves
    float amp1 = sin(0.007f * wave1amp) * 120;
    float amp2 = sin(0.023f * wave2amp) * 80;
    float amp3 = sin(0.011f * wave3amp) * 40;
    float amp4 = sin(0.031f * wave4amp) * 20;
    for (int i = 0; i < 1024; i++) {
        float val = fabs(sin(0.013f * (wave1pos + i)) * amp1
                  + sin(0.029f * (wave2pos + i)) * amp2);
        float off = sin(0.005f * (wave3pos + i)) * amp3
                  + sin(0.017f * (wave4pos + i)) * amp4;
        if (val < 2.f && val > -2.f) val = 2.f;
        points[i*8+1] = val + off;
        points[i*8+5] = -val + off;
    }
    wave1pos++;
    wave1amp++;
    wave2pos--;
    wave2amp++;
    wave3pos++;
    wave3amp++;
    wave4pos++;
    wave4amp++;
}

int root(void) {
    rsgClearColor(0.f, 0.f, 0.f, 1.f);

    int i;

    if (gIdle) {

        // idle state animation
        float *points = (float*)gPoints;
        if (fadeoutcounter > 0) {
            // fade waveform to 0
            for (i = 0; i < 1024; i++) {
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
                for (i = 0; i < 1024; i++) {
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

    rs_matrix4x4 mat1;
    float yrot = gYRotation;
    float scale = 0.004165f * (1.0f + 2.f * fabs(sin(radians(yrot))));

    // Draw the visualizer.
    rsgBindProgramVertex(gPVBackground);
    rsgBindProgramFragment(gPFBackground);
    rsgBindTexture(gPFBackground, 0, gTlinetexture);

    // Change the model matrix to account for the large model
    // and to do the necessary rotations.
    rsMatrixLoadIdentity(&mat1);
    rsMatrixRotate(&mat1, yrot, 0.f, 0.f, 1.f);
    rsMatrixScale(&mat1, scale, scale, scale);
    rsgProgramVertexLoadModelMatrix(&mat1);

    rsgDrawMesh(gCubeMesh);

    return 1;
}

