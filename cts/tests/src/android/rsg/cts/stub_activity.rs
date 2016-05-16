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

#pragma version(1)

// Tell which java package name the reflected files should belong to
#pragma rs java_package_name(android.renderscriptgraphics.cts)

// Built-in header with graphics API's
#include "rs_graphics.rsh"

int root(void) {

    // Clear the background color
    rsgClearColor(0.0f, 0.0f, 0.0f, 0.0f);
    // Tell the runtime what the font color should be
    rsgFontColor(1.0f, 1.0f, 1.0f, 1.0f);
    // Introuduce ourselves to the world
    rsgDrawText("Hello World!", 50, 50);

    // Return value tells RS roughly how often to redraw
    // in this case 20 ms
    return 20;
}
