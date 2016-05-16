// Copyright (C) 2011 The Android Open Source Project
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

#include "shared.rsh"
#include "rs_graphics.rsh"

#include "structs.rsh"

static void drawQuad() {
    float startX = 0, startY = 0;
    float width = 4, height = 4;
    rsgDrawQuadTexCoords(startX, startY, 0, 0, 0,
                         startX, startY + height, 0, 0, 1,
                         startX + width, startY + height, 0, 1, 1,
                         startX + width, startY, 0, 1, 0);
}

void testProgramVertex(rs_program_vertex pv) {
    rsDebug("Set Program Vertex, drew quad", 0);
    rsgBindProgramVertex(pv);
    drawQuad();
}

void testProgramFragment(rs_program_fragment pf) {
    rsDebug("Set Program Fragment, drew quad", 0);
    rsgBindProgramFragment(pf);
    drawQuad();
}

// Just draw a quad with previously setup state
int root(void) {
    rsDebug("Running script", 0);
    return 0;
}
