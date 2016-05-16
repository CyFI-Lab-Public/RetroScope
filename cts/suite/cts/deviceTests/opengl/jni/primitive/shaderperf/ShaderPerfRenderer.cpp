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
#include "ShaderPerfRenderer.h"
#include <graphics/GLUtils.h>

#include <math.h>

#include <Trace.h>

static const float GOLDEN_RATIO = (1.0f + sqrt(5.0f)) / 2.0f;

static const int SP_NUM_VERTICES = 6;

static const float SP_VERTICES[SP_NUM_VERTICES * 3] = {
        1.0f, 1.0f, -1.0f,
        -1.0f, 1.0f, -1.0f,
        -1.0f, -1.0f, -1.0f,
        -1.0f, -1.0f, -1.0f,
        1.0f, -1.0f, -1.0f,
        1.0f, 1.0f, -1.0f };

static const float SP_TEX_COORDS[SP_NUM_VERTICES * 2] = {
        1.0f, 1.0f,
        0.0f, 1.0f,
        0.0f, 0.0f,
        0.0f, 0.0f,
        1.0f, 0.0f,
        1.0f, 1.0f };

static const char* SP_VERTEX =
        "attribute vec4 a_Position;"
        "attribute vec2 a_TexCoord;"
        "varying vec2 v_TexCoord;"
        "void main() {"
        "  v_TexCoord = a_TexCoord;"
        "  gl_Position = a_Position;"
        "}";

static const char* SP_FRAGMENT_1 =
        "precision mediump float;"
        "uniform vec2 u_Seed;"
        "uniform sampler2D u_Texture;"
        "varying vec2 v_TexCoord;"
        "void main() {"
        "  int count = ";

//Add workload here

static const char* SP_FRAGMENT_2 =
        " * 4;"//workload * 4 (4 is a tweaking number, bigger = more work)
        "  vec2 z;"
        "  z.x = 3.0 * (v_TexCoord.x - 0.5);"
        "  z.y = 2.0 * (v_TexCoord.y - 0.5);"
        "  float u = 0.0;"
        "  for (int i = 0; i < count; i++) {"
        "    float x = (z.x * z.x - z.y * z.y) + u_Seed.x;"
        "    float y = (z.y * z.x + z.x * z.y) + u_Seed.y;"
        "    if (((x * x + y * y) > 4.0) && (u == 0.0)) {"
        "      u = float(i) / float(count);"
        "    }"
        "    z.x = x;"
        "    z.y = y;"
        "  }"
        "  gl_FragColor = texture2D(u_Texture, vec2(u, 0.0));"
        "}";

// Copies the source array from 0 up to and including the '\0' character to the
// destination array starting from the given start position. Unlike strcpy, this
// returns the number of characters which were copied.
static int charCopy(const char* source, char* dest, int destStart) {
    int srcAddr = 0;
    int destAddr = destStart;
    char current;
    do {
        current = source[srcAddr];
        dest[destAddr] = current;
        srcAddr++;
        destAddr++;
    } while (current != '\0');
    return destAddr - destStart;
}

ShaderPerfRenderer::ShaderPerfRenderer(ANativeWindow* window, bool offscreen, int workload) :
        Renderer(window, offscreen, workload) {
}

bool ShaderPerfRenderer::setUp() {
    SCOPED_TRACE();
    if (!Renderer::setUp()) {
        return false;
    }

    const int MAX_FRAGMENT_SHADER_SIZE = 1000;
    char* spFragment = new char[MAX_FRAGMENT_SHADER_SIZE];
    // Add the first part.
    int index = charCopy(SP_FRAGMENT_1, spFragment, 0);
    // Add the count, overwriting the '\0' added by charCopy.
    spFragment[index - 1] = (char) (((int) '0') + mWorkload);
    // Add the second part.
    index += charCopy(SP_FRAGMENT_2, spFragment, index);
    // Create program.
    mProgramId = GLUtils::createProgram(&SP_VERTEX, const_cast<const char**>(&spFragment));
    delete[] spFragment;
    if (mProgramId == 0) {
        return false;
    }
    // Bind attributes.
    mTextureUniformHandle = glGetUniformLocation(mProgramId, "u_Texture");
    mSeedUniformHandle = glGetUniformLocation(mProgramId, "u_Seed");
    mPositionHandle = glGetAttribLocation(mProgramId, "a_Position");
    mTexCoordHandle = glGetAttribLocation(mProgramId, "a_TexCoord");

    const int SIZE = 256;
    uint32_t* m = new uint32_t[SIZE];
    if (m != NULL) {
        uint32_t* d = m;
        for (int i = 0; i < SIZE; i++) {
            *d = 0xff000000 | ((i & 0xff) << 16);
            d++;
        }
        glGenTextures(1, &mTextureId);
        glBindTexture(GL_TEXTURE_2D, mTextureId);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, SIZE, 1, 0, GL_RGBA, GL_UNSIGNED_BYTE, m);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
    }
    delete[] m;

    return true;
}

void ShaderPerfRenderer::drawWorkload() {
    SCOPED_TRACE();
    glUseProgram(mProgramId);
    // Set the background clear color.
    glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
    glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT);

    // No culling of back faces
    glDisable(GL_CULL_FACE);

    // Bind the texture.
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, mTextureId);
    glUniform1i(mTextureUniformHandle, 0);

    // Bind the seed.
    glUniform2f(mSeedUniformHandle, GOLDEN_RATIO - 2.0f, GOLDEN_RATIO - 1.0f);

    // Bind the vertices.
    glEnableVertexAttribArray(mPositionHandle);
    glEnableVertexAttribArray(mTexCoordHandle);
    glVertexAttribPointer(mPositionHandle, 3, GL_FLOAT, false, 0, SP_VERTICES);
    glVertexAttribPointer(mTexCoordHandle, 2, GL_FLOAT, false, 0, SP_TEX_COORDS);

    glDrawArrays(GL_TRIANGLES, 0, SP_NUM_VERTICES);
}
