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

#pragma rs java_package_name(com.android.musicvis.vis4)

#include "rs_graphics.rsh"

// State
float gAngle;
int   gPeak;

rs_program_vertex gPVBackground;
rs_program_fragment gPFBackground;

rs_allocation gTvumeter_background;
rs_allocation gTvumeter_peak_on;
rs_allocation gTvumeter_peak_off;
rs_allocation gTvumeter_needle;
rs_allocation gTvumeter_black;
rs_allocation gTvumeter_frame;

rs_program_store gPFSBackground;

#define RSID_POINTS 1

int root(void) {
    rsgClearColor(0.f, 0.f, 0.f, 1.f);

    // Draw the visualizer.
    rsgBindProgramVertex(gPVBackground);
    rsgBindProgramFragment(gPFBackground);
    rsgBindProgramStore(gPFSBackground);

    rs_matrix4x4 mat1;
    float scale = 0.0041;
    rsMatrixLoadTranslate(&mat1, 0.f, -90.0f * scale, 0.f);
    rsMatrixScale(&mat1, scale, scale, scale);
    rsgProgramVertexLoadModelMatrix(&mat1);

    // draw the background image (416x233)
    rsgBindTexture(gPFBackground, 0, gTvumeter_background);
    rsgDrawQuadTexCoords(
            -208.0f, -33.0f, 0.0f,        // space
                0.0f, 1.0f,               // texture
            208, -33.0f, 0.0f,            // space
                1.0f, 1.0f,               // texture
            208, 200.0f, 0.0f,            // space
                1.0f, 0.0f,               // texture
            -208.0f, 200.0f, 0.0f,        // space
                0.0f, 0.0f);              // texture

    // draw the peak indicator light (56x58)
    if (gPeak > 0) {
        rsgBindTexture(gPFBackground, 0, gTvumeter_peak_on);
    } else {
        rsgBindTexture(gPFBackground, 0, gTvumeter_peak_off);
    }
    rsgDrawQuadTexCoords(
            140.0f, 70.0f, -1.0f,         // space
                0.0, 1.0f,                // texture
            196, 70.0f, -1.0f,            // space
                1.0f, 1.0f,               // texture
            196, 128.0f, -1.0f,           // space
                1.0f, 0.0f,               // texture
            140.0f, 128.0f, -1.0f,        // space
                0.0f, 0.0f);              // texture



    // Draw the needle (88x262, center of rotation at 44,217 from top left)

    // set matrix so point of rotation becomes origin
    rsMatrixLoadTranslate(&mat1, 0.f, -147.0f * scale, 0.f);
    rsMatrixRotate(&mat1, gAngle - 90.f, 0.f, 0.f, 1.f);
    rsMatrixScale(&mat1, scale, scale, scale);
    rsgProgramVertexLoadModelMatrix(&mat1);
    rsgBindTexture(gPFBackground, 0, gTvumeter_needle);
    rsgDrawQuadTexCoords(
            -44.0f, -102.0f+57.f, 0.0f,    // space
                0.0f, 1.0f,                // texture
            44.0f, -102.0f+57.f, 0.0f,     // space
                1.0f, 1.0f,                // texture
            44.0f, 160.0f+57.f, 0.0f,      // space
                1.0f, 0.0f,                // texture
            -44.0f, 160.0f+57.f, 0.0f,     // space
                0.0f, 0.0f);               // texture


    // restore matrix
    rsMatrixLoadTranslate(&mat1, 0.f, -90.0f * scale, 0.f);
    rsMatrixScale(&mat1, scale, scale, scale);
    rsgProgramVertexLoadModelMatrix(&mat1);

    // erase the part of the needle we don't want to show
    rsgBindTexture(gPFBackground, 0, gTvumeter_black);
    rsgDrawQuad(-100.f, -55.f, 0.f,
             -100.f, -105.f, 0.f,
              100.f, -105.f, 0.f,
              100.f, -55.f, 0.f);


    // draw the frame (472x290)
    rsgBindTexture(gPFBackground, 0, gTvumeter_frame);
    rsgDrawQuadTexCoords(
            -236.0f, -60.0f, 0.0f,           // space
                0.0f, 1.0f,                  // texture
            236, -60.0f, 0.0f,               // space
                1.0f, 1.0f,                  // texture
            236, 230.0f, 0.0f,               // space
                1.0f, 0.0f,                  // texture
            -236.0f, 230.0f, 0.0f,           // space
                0.0f, 0.0f);                 // texture

    return 1;
}
