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

#include <android/native_window.h>

#include <stdlib.h>

#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>

#include "ContextSwitchRenderer.h"
#include <graphics/GLUtils.h>

#define LOG_TAG "CTS_OPENGL"
#define LOG_NDEBUG 0
#include <utils/Log.h>
#include <Trace.h>

static const EGLint contextAttribs[] = {
        EGL_CONTEXT_CLIENT_VERSION, 2,
        EGL_NONE };

static const int NUM_WORKER_CONTEXTS = 7;

static const int CS_TEXTURE_SIZE = 64;

static const int CS_NUM_VERTICES = 6;

static const float CS_VERTICES[CS_NUM_VERTICES * 3] = {
        0.1f, 0.1f, -0.1f,
        -0.1f, 0.1f, -0.1f,
        -0.1f, -0.1f, -0.1f,
        -0.1f, -0.1f, -0.1f,
        0.1f, -0.1f, -0.1f,
        0.1f, 0.1f, -0.1f };

static const float CS_TEX_COORDS[CS_NUM_VERTICES * 2] = {
        1.0f, 1.0f,
        0.0f, 1.0f,
        0.0f, 0.0f,
        0.0f, 0.0f,
        1.0f, 0.0f,
        1.0f, 1.0f };

static const char* CS_VERTEX =
        "attribute vec4 a_Position;"
        "attribute vec2 a_TexCoord;"
        "uniform float u_Translate;"
        "varying vec2 v_TexCoord;"
        "void main() {"
        "  v_TexCoord = a_TexCoord;"
        "  gl_Position.x = a_Position.x + u_Translate;"
        "  gl_Position.yzw = a_Position.yzw;"
        "}";

static const char* CS_FRAGMENT =
        "precision mediump float;"
        "uniform sampler2D u_Texture;"
        "varying vec2 v_TexCoord;"
        "void main() {"
        "  gl_FragColor = texture2D(u_Texture, v_TexCoord);"
        "}";

ContextSwitchRenderer::ContextSwitchRenderer(ANativeWindow* window, bool offscreen, int workload) :
        Renderer(window, offscreen, workload), mContexts(NULL) {
}

bool ContextSwitchRenderer::setUp() {
    SCOPED_TRACE();
    if (!Renderer::setUp()) {
        return false;
    }

    // Setup texture.
    mTextureId = GLUtils::genTexture(CS_TEXTURE_SIZE, CS_TEXTURE_SIZE, GLUtils::RANDOM_FILL);
    if (mTextureId == 0) {
        return false;
    }

    // Create program.
    mProgramId = GLUtils::createProgram(&CS_VERTEX, &CS_FRAGMENT);
    if (mProgramId == 0) {
        return false;
    }
    // Bind attributes.
    mTextureUniformHandle = glGetUniformLocation(mProgramId, "u_Texture");
    mTranslateUniformHandle = glGetUniformLocation(mProgramId, "u_Translate");
    mPositionHandle = glGetAttribLocation(mProgramId, "a_Position");
    mTexCoordHandle = glGetAttribLocation(mProgramId, "a_TexCoord");

    mContexts = new EGLContext[NUM_WORKER_CONTEXTS];
    mFboIds = new GLuint[NUM_WORKER_CONTEXTS];
    for (int i = 0; i < NUM_WORKER_CONTEXTS; i++) {
        // Create the contexts, they share data with the main one.
        mContexts[i] = eglCreateContext(mEglDisplay, mGlConfig, mEglContext, contextAttribs);
        if (EGL_NO_CONTEXT == mContexts[i] || EGL_SUCCESS != eglGetError()) {
            return false;
        }

        if (!eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mContexts[i])
                || EGL_SUCCESS != eglGetError()) {
            return false;
        }
        if (mOffscreen) {
            // FBOs are not shared across contexts, textures and renderbuffers are though.
            glGenFramebuffers(1, &mFboIds[i]);
            glBindFramebuffer(GL_FRAMEBUFFER, mFboIds[i]);

            glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT,
                                      GL_RENDERBUFFER, mFboDepthId);

            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0,
                                   GL_TEXTURE_2D, mFboTexId, 0);
            GLenum status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
            if (status != GL_FRAMEBUFFER_COMPLETE) {
                ALOGE("Framebuffer not complete: %d", status);
                return false;
            }
        }
    }
    return true;
}

bool ContextSwitchRenderer::tearDown() {
    SCOPED_TRACE();
    if (mContexts) {
        // Destroy the contexts, the main one will be handled by Renderer::tearDown().
        for (int i = 0; i < NUM_WORKER_CONTEXTS; i++) {
            if (mOffscreen) {
                if (mFboIds[i] != 0) {
                    eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mContexts[i]);
                    glDeleteFramebuffers(1, &mFboIds[i]);
                    mFboIds[i] = 0;
                }
            }
            eglDestroyContext(mEglDisplay, mContexts[i]);
        }
        delete[] mContexts;
    }
    eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext);
    if (mTextureId != 0) {
        glDeleteTextures(1, &mTextureId);
        mTextureId = 0;
    }
    if (!Renderer::tearDown()) {
        return false;
    }
    return true;
}

void ContextSwitchRenderer::drawWorkload() {
    SCOPED_TRACE();
    // Set the background clear color to black.
    glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
    glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT);
    // No culling of back faces
    glDisable(GL_CULL_FACE);
    // No depth testing
    glDisable(GL_DEPTH_TEST);

    EGLSyncKHR fence = eglCreateSyncKHR(mEglDisplay, EGL_SYNC_FENCE_KHR, NULL);

    const int TOTAL_NUM_CONTEXTS = NUM_WORKER_CONTEXTS + 1;
    const float TRANSLATION = 0.9f - (TOTAL_NUM_CONTEXTS * 0.2f);
    for (int i = 0; i < TOTAL_NUM_CONTEXTS; i++) {
        eglWaitSyncKHR(mEglDisplay, fence, 0);
        eglDestroySyncKHR(mEglDisplay, fence);
        glUseProgram(mProgramId);

        // Set the texture.
        glActiveTexture (GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, mTextureId);
        glUniform1i(mTextureUniformHandle, 0);

        // Set the x translate.
        glUniform1f(mTranslateUniformHandle, (i * 0.2f) + TRANSLATION);

        glEnableVertexAttribArray(mPositionHandle);
        glEnableVertexAttribArray(mTexCoordHandle);
        glVertexAttribPointer(mPositionHandle, 3, GL_FLOAT, false, 0, CS_VERTICES);
        glVertexAttribPointer(mTexCoordHandle, 2, GL_FLOAT, false, 0, CS_TEX_COORDS);

        glDrawArrays(GL_TRIANGLES, 0, CS_NUM_VERTICES);
        fence = eglCreateSyncKHR(mEglDisplay, EGL_SYNC_FENCE_KHR, NULL);

        // Switch to next context.
        if (i < (mWorkload - 1)) {
            eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mContexts[i]);
            // Switch to FBO and re-attach.
            if (mOffscreen) {
                glBindFramebuffer(GL_FRAMEBUFFER, mFboIds[i]);
                glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT,
                                        GL_RENDERBUFFER, mFboDepthId);
                glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0,
                                     GL_TEXTURE_2D, mFboTexId, 0);
                glViewport(0, 0, mFboWidth, mFboHeight);
            }
        }
        GLuint err = glGetError();
        if (err != GL_NO_ERROR) {
            ALOGE("GLError %d in drawWorkload", err);
            break;
        }
    }

    eglDestroySyncKHR(mEglDisplay, fence);

    // Switch back to the main context.
    eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext);
    if (mOffscreen) {
        glBindFramebuffer(GL_FRAMEBUFFER, mFboId);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT,
                                GL_RENDERBUFFER, mFboDepthId);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0,
                             GL_TEXTURE_2D, mFboTexId, 0);
        glViewport(0, 0, mFboWidth, mFboHeight);
    }
}
