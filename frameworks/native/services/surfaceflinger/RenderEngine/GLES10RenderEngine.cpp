/*
 * Copyright 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <GLES/gl.h>

#include <cutils/compiler.h>

#include "GLES10RenderEngine.h"

// ---------------------------------------------------------------------------
namespace android {
// ---------------------------------------------------------------------------

GLES10RenderEngine::~GLES10RenderEngine() {
}

void GLES10RenderEngine::setupLayerBlending(
    bool premultipliedAlpha, bool opaque, int alpha) {
    // OpenGL ES 1.0 doesn't support texture combiners.
    // This path doesn't properly handle opaque layers that have non-opaque
    // alpha values. The alpha channel will be copied into the framebuffer or
    // screenshot, so if the framebuffer or screenshot is blended on top of
    // something else,  whatever is below the window will incorrectly show
    // through.
    if (CC_UNLIKELY(alpha < 0xFF)) {
        GLfloat floatAlpha = alpha * (1.0f / 255.0f);
        if (premultipliedAlpha) {
            glColor4f(floatAlpha, floatAlpha, floatAlpha, floatAlpha);
        } else {
            glColor4f(1.0f, 1.0f, 1.0f, floatAlpha);
        }
        glTexEnvx(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
    } else {
        glTexEnvx(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);
    }

    if (alpha < 0xFF || !opaque) {
        glEnable(GL_BLEND);
        glBlendFunc(premultipliedAlpha ? GL_ONE : GL_SRC_ALPHA,
                    GL_ONE_MINUS_SRC_ALPHA);
    } else {
        glDisable(GL_BLEND);
    }
}

// ---------------------------------------------------------------------------
}; // namespace android
// ---------------------------------------------------------------------------
