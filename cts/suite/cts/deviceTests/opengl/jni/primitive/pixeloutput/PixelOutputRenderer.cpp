/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
#include "PixelOutputRenderer.h"
#include <graphics/GLUtils.h>

#include <Trace.h>

static const int PO_NUM_VERTICES = 6;

static const float PO_VERTICES[PO_NUM_VERTICES * 3] = {
        1.0f, 1.0f, -1.0f,
        -1.0f, 1.0f, -1.0f,
        -1.0f, -1.0f, -1.0f,
        -1.0f, -1.0f, -1.0f,
        1.0f, -1.0f, -1.0f,
        1.0f, 1.0f, -1.0f };
static const float PO_TEX_COORDS[PO_NUM_VERTICES * 2] = {
        1.0f, 1.0f,
        0.0f, 1.0f,
        0.0f, 0.0f,
        0.0f, 0.0f,
        1.0f, 0.0f,
        1.0f, 1.0f };

static const char* PO_VERTEX =
        "attribute vec4 a_Position;"
        "attribute vec2 a_TexCoord;"
        "varying vec2 v_TexCoord;"
        "void main() {"
        "  v_TexCoord = a_TexCoord;"
        "  gl_Position = a_Position;"
        "}";

static const char* PO_FRAGMENT =
        "precision mediump float;"
        "uniform sampler2D u_Texture;"
        "varying vec2 v_TexCoord;"
        "void main() {"
        "  gl_FragColor = texture2D(u_Texture, v_TexCoord);"
        "}";

PixelOutputRenderer::PixelOutputRenderer(ANativeWindow* window, bool offscreen, int workload) :
        Renderer(window, offscreen, workload) {
}

bool PixelOutputRenderer::setUp() {
    SCOPED_TRACE();
    if (!Renderer::setUp()) {
        return false;
    }

    // Create program.
    mProgramId = GLUtils::createProgram(&PO_VERTEX, &PO_FRAGMENT);
    if (mProgramId == 0) {
        return false;
    }
    // Bind attributes.
    mTextureUniformHandle = glGetUniformLocation(mProgramId, "u_Texture");
    mPositionHandle = glGetAttribLocation(mProgramId, "a_Position");
    mTexCoordHandle = glGetAttribLocation(mProgramId, "a_TexCoord");

    // Setup texture.
    mTextureId = GLUtils::genTexture(mWidth, mHeight, GLUtils::RANDOM_FILL);
    if (mTextureId == 0) {
        return false;
    }
    return true;
}

bool PixelOutputRenderer::tearDown() {
    SCOPED_TRACE();
    if (mTextureId != 0) {
        glDeleteTextures(1, &mTextureId);
        mTextureId = 0;
    }
    if (!Renderer::tearDown()) {
        return false;
    }
    return true;
}

void PixelOutputRenderer::drawWorkload() {
    SCOPED_TRACE();
    glUseProgram(mProgramId);
    // Set the background clear color to black.
    glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
    glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT);

    // No culling of back faces
    glDisable(GL_CULL_FACE);

    // No depth testing
    glDisable(GL_DEPTH_TEST);

    // Enable blending
    glEnable(GL_BLEND);
    glBlendFunc(GL_ONE, GL_ONE);

    glActiveTexture(GL_TEXTURE0);
    // Bind the texture to this unit.
    glBindTexture(GL_TEXTURE_2D, mTextureId);
    // Tell the texture uniform sampler to use this texture in the shader by binding to texture
    // unit 0.
    glUniform1i(mTextureUniformHandle, 0);

    glEnableVertexAttribArray(mPositionHandle);
    glEnableVertexAttribArray(mTexCoordHandle);
    glVertexAttribPointer(mPositionHandle, 3, GL_FLOAT, false, 0, PO_VERTICES);
    glVertexAttribPointer(mTexCoordHandle, 2, GL_FLOAT, false, 0, PO_TEX_COORDS);

    for (int i = 0; i < mWorkload; i++) {
        glDrawArrays(GL_TRIANGLES, 0, PO_NUM_VERTICES);
    }
}
