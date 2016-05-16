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

#pragma rs java_package_name(com.android.wallpaper.nexus)

#include "rs_graphics.rsh"
#pragma stateVertex(parent)

#define MAX_PULSES           20
#define MAX_EXTRAS           40
#define PULSE_SIZE           14 // Size in pixels of a cell
#define HALF_PULSE_SIZE      7
#define GLOW_SIZE            64 // Size of the leading glow in pixels
#define HALF_GLOW_SIZE       32
#define SPEED                0.2f // (200 / 1000) Pixels per ms
#define SPEED_DELTA_MIN      0.7f
#define SPEED_DELTA_MAX      1.7f
#define PULSE_NORMAL         0
#define PULSE_EXTRA          1
#define TRAIL_SIZE           40 // Number of cells in a trail
#define MAX_DELAY          2000 // Delay between a pulse going offscreen and restarting

typedef struct pulse_s {
    int pulseType;
    float originX;
    float originY;
    int color;
    int startTime;
    float dx;
    float dy;
    float scale;
    int active;
} pulse_t;

static pulse_t gPulses[MAX_PULSES];
static pulse_t gExtras[MAX_EXTRAS];
static int gNow;
static int gWidth;
static int gHeight;
static int gRotate;

float gWorldScaleX;
float gWorldScaleY;
float gXOffset;
int gIsPreview;
int gMode;

rs_program_fragment gPFTexture;
rs_program_store gPSBlend;
rs_program_fragment gPFTexture565;

rs_allocation gTBackground;
rs_allocation gTPulse;
rs_allocation gTGlow;

static void setColor(int c) {
    if (gMode == 1) {
        // sholes red
        rsgProgramFragmentConstantColor(gPFTexture, 0.9f, 0.1f, 0.1f, 0.8f);
    } else if (c == 0) {
        // red
        rsgProgramFragmentConstantColor(gPFTexture, 1.0f, 0.0f, 0.0f, 0.8f);
    } else if (c == 1) {
        // green
        rsgProgramFragmentConstantColor(gPFTexture, 0.0f, 0.8f, 0.0f, 0.8f);
    } else if (c == 2) {
        // blue
        rsgProgramFragmentConstantColor(gPFTexture, 0.0f, 0.4f, 0.9f, 0.8f);
    } else if (c == 3) {
        // yellow
        rsgProgramFragmentConstantColor(gPFTexture, 1.0f, 0.8f, 0.0f, 0.8f);
    }
}

static void initPulse(struct pulse_s * pulse, int pulseType) {
    float scale = rsRand(SPEED_DELTA_MIN, SPEED_DELTA_MAX);
    pulse->scale = scale;
    gWidth = rsgGetWidth();
    gHeight = rsgGetHeight();
    if (rsRand(1.f) > 0.5f) {
        pulse->originX = rsRand(gWidth * 2 / PULSE_SIZE) * PULSE_SIZE;
        pulse->dx = 0;
        if (rsRand(1.f) > 0.5f) {
            // Top
            pulse->originY = 0;
            pulse->dy = scale;
        } else {
            // Bottom
            pulse->originY = gHeight / scale;
            pulse->dy = -scale;
        }
    } else {
        pulse->originY = rsRand(gHeight / PULSE_SIZE) * PULSE_SIZE;
        pulse->dy = 0;
        if (rsRand(1.f) > 0.5f) {
            // Left
            pulse->originX = 0;
            pulse->dx = scale;
        } else {
            // Right
            pulse->originX = gWidth * 2 / scale;
            pulse->dx = -scale;
        }
    }
    pulse->startTime = gNow + rsRand(MAX_DELAY);

    pulse->color = rsRand(4);

    pulse->pulseType = pulseType;
    if (pulseType == PULSE_EXTRA) {
        pulse->active = 0;
    } else {
        pulse->active = 1;
    }
}

void initPulses() {
    gNow = (int)rsUptimeMillis();
    int i;
    for (i=0; i<MAX_PULSES; i++) {
        initPulse(&gPulses[i], PULSE_NORMAL);
    }
    for (i=0; i<MAX_EXTRAS; i++) {
        struct pulse_s * p = &gExtras[i];
        p->pulseType = PULSE_EXTRA;
        p->active = 0;
    }
}

static void drawBackground() {
    rsgBindProgramFragment(gPFTexture565);
    rsgBindTexture(gPFTexture565, 0, gTBackground);
    if (gRotate) {
        rsgDrawRect(0.0f, 0.0f, gHeight*2, gWidth, 0.0f);
    } else {
        rsgDrawRect(0.0f, 0.0f, gWidth*2, gHeight, 0.0f);
    }
}

static void drawPulses(pulse_t * pulseSet, int setSize) {
    rsgBindProgramFragment(gPFTexture);
    rsgBindProgramStore(gPSBlend);

    rs_matrix4x4 matrix;
    rs_matrix4x4 modelMatrix;
    for (int i=0; i<setSize; i++) {
        struct pulse_s * p = &pulseSet[i];
        int delta = gNow - p->startTime;

        if (p->active != 0 && delta >= 0) {
        
            rsMatrixLoadIdentity(&modelMatrix);
            if (gRotate) {
                //matrixLoadRotate(modelMatrix, 90.0f, 0.0f, 0.0f, 1.0f);
                //matrixTranslate(modelMatrix, 0.0f, -height, 1.0f);
                // XXX: HAX: do not slide display in landscape
            } else {
                 rsMatrixTranslate(&modelMatrix, -(gXOffset * gWidth), 0, 0);
            }
            rsMatrixScale(&modelMatrix, p->scale * gWorldScaleX, p->scale * gWorldScaleY, 1.0f);
            rsgProgramVertexLoadModelMatrix(&modelMatrix);

           float x = p->originX + (p->dx * SPEED * delta);
           float y = p->originY + (p->dy * SPEED * delta);

           rsMatrixLoadIdentity(&matrix);
           if (p->dx < 0) {
               rsgProgramVertexLoadTextureMatrix(&matrix);
               float xx = x + (TRAIL_SIZE * PULSE_SIZE);
               if (xx <= 0) {
                   initPulse(p, p->pulseType);
               } else {
                   setColor(p->color);
                   rsgBindTexture(gPFTexture, 0, gTPulse);
                   rsgDrawRect(x, y, xx, y + PULSE_SIZE, 0.0f);
                   rsgBindTexture(gPFTexture, 0, gTGlow);
                   rsgDrawRect(x + HALF_PULSE_SIZE - HALF_GLOW_SIZE,
                       y + HALF_PULSE_SIZE - HALF_GLOW_SIZE,
                       x + HALF_PULSE_SIZE + HALF_GLOW_SIZE,
                       y + HALF_PULSE_SIZE + HALF_GLOW_SIZE,
                       0.0f);
               }
           } else if (p->dx > 0) {
               x += PULSE_SIZE; // need to start on the other side of this cell
               rsMatrixRotate(&matrix, 180.0f, 0.0f, 0.0f, 1.0f);
               rsgProgramVertexLoadTextureMatrix(&matrix);
               float xx = x - (TRAIL_SIZE * PULSE_SIZE);
              if (xx >= gWidth * 2) {
                  initPulse(p, p->pulseType);
               } else {
                   setColor(p->color);
                   rsgBindTexture(gPFTexture, 0, gTPulse);
                   rsgDrawRect(xx, y, x, y + PULSE_SIZE, 0.0f);
                   rsgBindTexture(gPFTexture, 0, gTGlow);
                   rsgDrawRect(x - HALF_PULSE_SIZE - HALF_GLOW_SIZE,
                       y + HALF_PULSE_SIZE - HALF_GLOW_SIZE,
                       x - HALF_PULSE_SIZE + HALF_GLOW_SIZE,
                       y + HALF_PULSE_SIZE + HALF_GLOW_SIZE,
                       0.0f);
               }
           } else if (p->dy < 0) {
               rsMatrixRotate(&matrix, -90.0f, 0.0f, 0.0f, 1.0f);
               rsgProgramVertexLoadTextureMatrix(&matrix);
               float yy = y + (TRAIL_SIZE * PULSE_SIZE);
               if (yy <= 0) {
                  initPulse(p, p->pulseType);
               } else {
                   setColor(p->color);
                   rsgBindTexture(gPFTexture, 0, gTPulse);
                   rsgDrawRect(x, y, x + PULSE_SIZE, yy, 0.0f);
                   rsgBindTexture(gPFTexture, 0, gTGlow);
                   rsgDrawRect(x + HALF_PULSE_SIZE - HALF_GLOW_SIZE,
                       y + HALF_PULSE_SIZE - HALF_GLOW_SIZE,
                       x + HALF_PULSE_SIZE + HALF_GLOW_SIZE,
                       y + HALF_PULSE_SIZE + HALF_GLOW_SIZE,
                       0.0f);
               }
           } else if (p->dy > 0) {
            y += PULSE_SIZE; // need to start on the other side of this cell
               rsMatrixRotate(&matrix, 90.0f, 0.0f, 0.0f, 1.0f);
               rsgProgramVertexLoadTextureMatrix(&matrix);
               float yy = y - (TRAIL_SIZE * PULSE_SIZE);
               if (yy >= gHeight) {
                  initPulse(p, p->pulseType);
               } else {
                   setColor(p->color);
                   rsgBindTexture(gPFTexture, 0, gTPulse);
                   rsgDrawRect(x, yy, x + PULSE_SIZE, y, 0.0f);
                   rsgBindTexture(gPFTexture, 0, gTGlow);
                   rsgDrawRect(x + HALF_PULSE_SIZE - HALF_GLOW_SIZE,
                       y - HALF_PULSE_SIZE - HALF_GLOW_SIZE,
                       x + HALF_PULSE_SIZE + HALF_GLOW_SIZE,
                       y - HALF_PULSE_SIZE + HALF_GLOW_SIZE,
                       0.0f);
               }
           }
       }
    }

    rsMatrixLoadIdentity(&matrix);
    rsgProgramVertexLoadTextureMatrix(&matrix);
}

void addTap(int x, int y) {
    int count = 0;
    int color = rsRand(4);
    float scale = rsRand(0.9f, 1.9f);
    x = (x / PULSE_SIZE) * PULSE_SIZE;
    y = (y / PULSE_SIZE) * PULSE_SIZE;
    for (int i=0; i<MAX_EXTRAS; i++) {
        struct pulse_s * p = &gExtras[i];
        if (p->active == 0) {
            p->originX = x/scale;
            p->originY = y/scale;
            p->scale = scale;

            if (count == 0) {
                p->dx = scale;
                p->dy = 0.0f;
            } else if (count == 1) {
                p->dx = -scale;
                p->dy = 0.0f;
            } else if (count == 2) {
                p->dx = 0.0f;
                p->dy = scale;
            } else if (count == 3) {
                p->dx = 0.0f;
                p->dy = -scale;
            }

            p->active = 1;
            p->color = color;
            color++;
            if (color >= 4) {
                color = 0;
            }
            p->startTime = gNow;
            count++;
            if (count == 4) {
                break;
            }
        }
    }
}

int root() {
    rsgClearColor(0.f, 0.f, 0.f, 1.f);

    gWidth = rsgGetWidth();
    gHeight = rsgGetHeight();
    gRotate = gWidth > gHeight ? 1 : 0;

    gNow = (int)rsUptimeMillis();

    rs_matrix4x4 matrix;
    rsMatrixLoadIdentity(&matrix);
    rsMatrixScale(&matrix, gWorldScaleX, gWorldScaleY, 1.0f);

    if (gRotate) {
        //matrixLoadRotate(matrix, 90.0f, 0.0f, 0.0f, 1.0f);
        //matrixTranslate(matrix, 0.0f, -height, 1.0f);
        // XXX: HAX: do not slide display in landscape
    } else {
         rsMatrixTranslate(&matrix, -(gXOffset * gWidth), 0, 0);
    }

    rsgProgramVertexLoadModelMatrix(&matrix);

    drawBackground();
    drawPulses(gPulses, MAX_PULSES);
    drawPulses(gExtras, MAX_EXTRAS);

    return 45;
}
