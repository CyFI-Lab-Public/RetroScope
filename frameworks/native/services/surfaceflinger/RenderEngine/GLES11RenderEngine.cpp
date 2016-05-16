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
#include <GLES/glext.h>

#include <utils/String8.h>
#include <cutils/compiler.h>

#include "GLES11RenderEngine.h"
#include "Mesh.h"
#include "Texture.h"

// ---------------------------------------------------------------------------
namespace android {
// ---------------------------------------------------------------------------

GLES11RenderEngine::GLES11RenderEngine() {

    glGetIntegerv(GL_MAX_TEXTURE_SIZE, &mMaxTextureSize);
    glGetIntegerv(GL_MAX_VIEWPORT_DIMS, mMaxViewportDims);

    glPixelStorei(GL_UNPACK_ALIGNMENT, 4);
    glPixelStorei(GL_PACK_ALIGNMENT, 4);
    glEnableClientState(GL_VERTEX_ARRAY);
    glShadeModel(GL_FLAT);
    glDisable(GL_DITHER);
    glDisable(GL_CULL_FACE);

    struct pack565 {
        inline uint16_t operator() (int r, int g, int b) const {
            return (r<<11)|(g<<5)|b;
        }
    } pack565;

    const uint16_t protTexData[] = { 0 };
    glGenTextures(1, &mProtectedTexName);
    glBindTexture(GL_TEXTURE_2D, mProtectedTexName);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, 1, 1, 0,
            GL_RGB, GL_UNSIGNED_SHORT_5_6_5, protTexData);
}

GLES11RenderEngine::~GLES11RenderEngine() {
}


size_t GLES11RenderEngine::getMaxTextureSize() const {
    return mMaxTextureSize;
}

size_t GLES11RenderEngine::getMaxViewportDims() const {
    return
        mMaxViewportDims[0] < mMaxViewportDims[1] ?
            mMaxViewportDims[0] : mMaxViewportDims[1];
}

void GLES11RenderEngine::setViewportAndProjection(
        size_t vpw, size_t vph, size_t w, size_t h, bool yswap) {
    glViewport(0, 0, vpw, vph);
    glMatrixMode(GL_PROJECTION);
    glLoadIdentity();
    // put the origin in the left-bottom corner
    if (yswap)  glOrthof(0, w, h, 0, 0, 1);
    else        glOrthof(0, w, 0, h, 0, 1);
    glMatrixMode(GL_MODELVIEW);
}

void GLES11RenderEngine::setupLayerBlending(
    bool premultipliedAlpha, bool opaque, int alpha) {
    GLenum combineRGB;
    GLenum combineAlpha;
    GLenum src0Alpha;
    GLfloat envColor[4];

    if (CC_UNLIKELY(alpha < 0xFF)) {
        // Cv = premultiplied ? Cs*alpha : Cs
        // Av = !opaque       ? As*alpha : As
        combineRGB   = premultipliedAlpha ? GL_MODULATE : GL_REPLACE;
        combineAlpha = !opaque            ? GL_MODULATE : GL_REPLACE;
        src0Alpha    = GL_CONSTANT;
        envColor[0]  = alpha * (1.0f / 255.0f);
    } else {
        // Cv = Cs
        // Av = opaque ? 1.0 : As
        combineRGB   = GL_REPLACE;
        combineAlpha = GL_REPLACE;
        src0Alpha    = opaque ? GL_CONSTANT : GL_TEXTURE;
        envColor[0]  = 1.0f;
    }

    glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_COMBINE);
    glTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_RGB, combineRGB);
    glTexEnvi(GL_TEXTURE_ENV, GL_SRC0_RGB, GL_TEXTURE);
    glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND0_RGB, GL_SRC_COLOR);
    if (combineRGB == GL_MODULATE) {
        glTexEnvi(GL_TEXTURE_ENV, GL_SRC1_RGB, GL_CONSTANT);
        glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND1_RGB, GL_SRC_COLOR);
    }
    glTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_ALPHA, combineAlpha);
    glTexEnvi(GL_TEXTURE_ENV, GL_SRC0_ALPHA, src0Alpha);
    glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND0_ALPHA, GL_SRC_ALPHA);
    if (combineAlpha == GL_MODULATE) {
        glTexEnvi(GL_TEXTURE_ENV, GL_SRC1_ALPHA, GL_TEXTURE);
        glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND1_ALPHA, GL_SRC_ALPHA);
    }
    if (combineRGB == GL_MODULATE || src0Alpha == GL_CONSTANT) {
        envColor[1] = envColor[0];
        envColor[2] = envColor[0];
        envColor[3] = envColor[0];
        glTexEnvfv(GL_TEXTURE_ENV, GL_TEXTURE_ENV_COLOR, envColor);
    }

    if (alpha < 0xFF || !opaque) {
        glEnable(GL_BLEND);
        glBlendFunc(premultipliedAlpha ? GL_ONE : GL_SRC_ALPHA,
                    GL_ONE_MINUS_SRC_ALPHA);
    } else {
        glDisable(GL_BLEND);
    }
}

void GLES11RenderEngine::setupDimLayerBlending(int alpha) {
    glDisable(GL_TEXTURE_EXTERNAL_OES);
    glDisable(GL_TEXTURE_2D);
    if (alpha == 0xFF) {
        glDisable(GL_BLEND);
    } else {
        glEnable(GL_BLEND);
        glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
    }
    glColor4f(0, 0, 0, alpha/255.0f);
}

void GLES11RenderEngine::setupLayerTexturing(const Texture& texture) {
    GLuint target = texture.getTextureTarget();
    glBindTexture(target, texture.getTextureName());
    GLenum filter = GL_NEAREST;
    if (texture.getFiltering()) {
        filter = GL_LINEAR;
    }
    glTexParameterx(target, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameterx(target, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    glTexParameterx(target, GL_TEXTURE_MAG_FILTER, filter);
    glTexParameterx(target, GL_TEXTURE_MIN_FILTER, filter);
    glMatrixMode(GL_TEXTURE);
    glLoadMatrixf(texture.getMatrix().asArray());
    glMatrixMode(GL_MODELVIEW);
    glDisable(GL_TEXTURE_2D);
    glEnable(GL_TEXTURE_EXTERNAL_OES);
}

void GLES11RenderEngine::setupLayerBlackedOut() {
    glBindTexture(GL_TEXTURE_2D, mProtectedTexName);
    glMatrixMode(GL_TEXTURE);
    glLoadIdentity();
    glMatrixMode(GL_MODELVIEW);
    glDisable(GL_TEXTURE_EXTERNAL_OES);
    glEnable(GL_TEXTURE_2D);
}

void GLES11RenderEngine::disableTexturing() {
    glDisable(GL_TEXTURE_EXTERNAL_OES);
    glDisable(GL_TEXTURE_2D);
}

void GLES11RenderEngine::disableBlending() {
    glDisable(GL_BLEND);
}

void GLES11RenderEngine::bindImageAsFramebuffer(EGLImageKHR image,
        uint32_t* texName, uint32_t* fbName, uint32_t* status) {
    GLuint tname, name;
    // turn our EGLImage into a texture
    glGenTextures(1, &tname);
    glBindTexture(GL_TEXTURE_2D, tname);
    glEGLImageTargetTexture2DOES(GL_TEXTURE_2D, (GLeglImageOES)image);

    // create a Framebuffer Object to render into
    glGenFramebuffersOES(1, &name);
    glBindFramebufferOES(GL_FRAMEBUFFER_OES, name);
    glFramebufferTexture2DOES(GL_FRAMEBUFFER_OES,
            GL_COLOR_ATTACHMENT0_OES, GL_TEXTURE_2D, tname, 0);

    *status = glCheckFramebufferStatusOES(GL_FRAMEBUFFER_OES);
    *texName = tname;
    *fbName = name;
}

void GLES11RenderEngine::unbindFramebuffer(uint32_t texName, uint32_t fbName) {
    glBindFramebufferOES(GL_FRAMEBUFFER_OES, 0);
    glDeleteFramebuffersOES(1, &fbName);
    glDeleteTextures(1, &texName);
}

void GLES11RenderEngine::setupFillWithColor(float r, float g, float b, float a) {
    glColor4f(r, g, b, a);
    glDisable(GL_TEXTURE_EXTERNAL_OES);
    glDisable(GL_TEXTURE_2D);
    glDisable(GL_BLEND);
}

void GLES11RenderEngine::drawMesh(const Mesh& mesh) {
    if (mesh.getTexCoordsSize()) {
        glEnableClientState(GL_TEXTURE_COORD_ARRAY);
        glTexCoordPointer(mesh.getTexCoordsSize(),
                GL_FLOAT,
                mesh.getByteStride(),
                mesh.getTexCoords());
    }

    glVertexPointer(mesh.getVertexSize(),
            GL_FLOAT,
            mesh.getByteStride(),
            mesh.getPositions());

    glDrawArrays(mesh.getPrimitive(), 0, mesh.getVertexCount());

    if (mesh.getTexCoordsSize()) {
        glDisableClientState(GL_TEXTURE_COORD_ARRAY);
    }
}

void GLES11RenderEngine::beginGroup(const mat4& colorTransform) {
    // doesn't do anything in GLES 1.1
}

void GLES11RenderEngine::endGroup() {
    // doesn't do anything in GLES 1.1
}

void GLES11RenderEngine::dump(String8& result) {
    RenderEngine::dump(result);
}

// ---------------------------------------------------------------------------
}; // namespace android
// ---------------------------------------------------------------------------

#if defined(__gl2_h_)
#error "don't include gl2/gl2.h in this file"
#endif
