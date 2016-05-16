/*
 * Copyright (C) 2011 The Android Open Source Project
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

#define LOG_TAG "NativeWindowRenderer"
#include "NativeWindowRenderer.h"

#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>
#include <cutils/log.h>
#include <gui/GLConsumer.h>
#include <gui/Surface.h>
#include <media/stagefright/MediaBuffer.h>
#include <media/stagefright/MetaData.h>
#include <media/stagefright/foundation/ADebug.h>
#include "VideoEditorTools.h"

#define CHECK_EGL_ERROR CHECK(EGL_SUCCESS == eglGetError())
#define CHECK_GL_ERROR CHECK(GLenum(GL_NO_ERROR) == glGetError())

//
// Vertex and fragment programs
//

// The matrix is derived from
// frameworks/base/media/libstagefright/colorconversion/ColorConverter.cpp
//
// R * 255 = 1.164 * (Y - 16) + 1.596 * (V - 128)
// G * 255 = 1.164 * (Y - 16) - 0.813 * (V - 128) - 0.391 * (U - 128)
// B * 255 = 1.164 * (Y - 16) + 2.018 * (U - 128)
//
// Here we assume YUV are in the range of [0,255], RGB are in the range of
// [0, 1]
#define RGB2YUV_MATRIX \
"const mat4 rgb2yuv = mat4("\
"    65.52255,   -37.79398,   111.98732,     0.00000,"\
"   128.62729,   -74.19334,   -93.81088,     0.00000,"\
"    24.92233,   111.98732,   -18.17644,     0.00000,"\
"    16.00000,   128.00000,   128.00000,     1.00000);\n"

#define YUV2RGB_MATRIX \
"const mat4 yuv2rgb = mat4("\
"   0.00456,   0.00456,   0.00456,   0.00000,"\
"   0.00000,  -0.00153,   0.00791,   0.00000,"\
"   0.00626,  -0.00319,   0.00000,   0.00000,"\
"  -0.87416,   0.53133,  -1.08599,   1.00000);\n"

static const char vSrcNormal[] =
    "attribute vec4 vPosition;\n"
    "attribute vec2 vTexPos;\n"
    "uniform mat4 texMatrix;\n"
    "varying vec2 texCoords;\n"
    "varying float topDown;\n"
    "void main() {\n"
    "  gl_Position = vPosition;\n"
    "  texCoords = (texMatrix * vec4(vTexPos, 0.0, 1.0)).xy;\n"
    "  topDown = vTexPos.y;\n"
    "}\n";

static const char fSrcNormal[] =
    "#extension GL_OES_EGL_image_external : require\n"
    "precision mediump float;\n"
    "uniform samplerExternalOES texSampler;\n"
    "varying vec2 texCoords;\n"
    "void main() {\n"
    "  gl_FragColor = texture2D(texSampler, texCoords);\n"
    "}\n";

static const char fSrcSepia[] =
    "#extension GL_OES_EGL_image_external : require\n"
    "precision mediump float;\n"
    "uniform samplerExternalOES texSampler;\n"
    "varying vec2 texCoords;\n"
    RGB2YUV_MATRIX
    YUV2RGB_MATRIX
    "void main() {\n"
    "  vec4 rgb = texture2D(texSampler, texCoords);\n"
    "  vec4 yuv = rgb2yuv * rgb;\n"
    "  yuv = vec4(yuv.x, 117.0, 139.0, 1.0);\n"
    "  gl_FragColor = yuv2rgb * yuv;\n"
    "}\n";

static const char fSrcNegative[] =
    "#extension GL_OES_EGL_image_external : require\n"
    "precision mediump float;\n"
    "uniform samplerExternalOES texSampler;\n"
    "varying vec2 texCoords;\n"
    RGB2YUV_MATRIX
    YUV2RGB_MATRIX
    "void main() {\n"
    "  vec4 rgb = texture2D(texSampler, texCoords);\n"
    "  vec4 yuv = rgb2yuv * rgb;\n"
    "  yuv = vec4(255.0 - yuv.x, yuv.y, yuv.z, 1.0);\n"
    "  gl_FragColor = yuv2rgb * yuv;\n"
    "}\n";

static const char fSrcGradient[] =
    "#extension GL_OES_EGL_image_external : require\n"
    "precision mediump float;\n"
    "uniform samplerExternalOES texSampler;\n"
    "varying vec2 texCoords;\n"
    "varying float topDown;\n"
    RGB2YUV_MATRIX
    YUV2RGB_MATRIX
    "void main() {\n"
    "  vec4 rgb = texture2D(texSampler, texCoords);\n"
    "  vec4 yuv = rgb2yuv * rgb;\n"
    "  vec4 mixin = vec4(15.0/31.0, 59.0/63.0, 31.0/31.0, 1.0);\n"
    "  vec4 yuv2 = rgb2yuv * vec4((mixin.xyz * topDown), 1);\n"
    "  yuv = vec4(yuv.x, yuv2.y, yuv2.z, 1);\n"
    "  gl_FragColor = yuv2rgb * yuv;\n"
    "}\n";

namespace android {

NativeWindowRenderer::NativeWindowRenderer(sp<ANativeWindow> nativeWindow,
        int width, int height)
    : mNativeWindow(nativeWindow)
    , mDstWidth(width)
    , mDstHeight(height)
    , mLastVideoEffect(-1)
    , mNextTextureId(100)
    , mActiveInputs(0)
    , mThreadCmd(CMD_IDLE) {
    createThread(threadStart, this);
}

// The functions below run in the GL thread.
//
// All GL-related work is done in this thread, and other threads send
// requests to this thread using a command code. We expect most of the
// time there will only be one thread sending in requests, so we let
// other threads wait until the request is finished by GL thread.

int NativeWindowRenderer::threadStart(void* self) {
    ALOGD("create thread");
    ((NativeWindowRenderer*)self)->glThread();
    return 0;
}

void NativeWindowRenderer::glThread() {
    initializeEGL();
    createPrograms();

    Mutex::Autolock autoLock(mLock);
    bool quit = false;
    while (!quit) {
        switch (mThreadCmd) {
            case CMD_IDLE:
                mCond.wait(mLock);
                continue;
            case CMD_RENDER_INPUT:
                render(mThreadRenderInput);
                break;
            case CMD_RESERVE_TEXTURE:
                glBindTexture(GL_TEXTURE_EXTERNAL_OES, mThreadTextureId);
                CHECK_GL_ERROR;
                break;
            case CMD_DELETE_TEXTURE:
                glDeleteTextures(1, &mThreadTextureId);
                break;
            case CMD_QUIT:
                terminateEGL();
                quit = true;
                break;
        }
        // Tell the requester that the command is finished.
        mThreadCmd = CMD_IDLE;
        mCond.broadcast();
    }
    ALOGD("quit");
}

void NativeWindowRenderer::initializeEGL() {
    mEglDisplay = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    CHECK_EGL_ERROR;

    EGLint majorVersion;
    EGLint minorVersion;
    eglInitialize(mEglDisplay, &majorVersion, &minorVersion);
    CHECK_EGL_ERROR;

    EGLConfig config;
    EGLint numConfigs = -1;
    EGLint configAttribs[] = {
        EGL_SURFACE_TYPE, EGL_WINDOW_BIT,
        EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
        EGL_RED_SIZE, 8,
        EGL_GREEN_SIZE, 8,
        EGL_BLUE_SIZE, 8,
        EGL_NONE
    };
    eglChooseConfig(mEglDisplay, configAttribs, &config, 1, &numConfigs);
    CHECK_EGL_ERROR;

    mEglSurface = eglCreateWindowSurface(mEglDisplay, config,
        mNativeWindow.get(), NULL);
    CHECK_EGL_ERROR;

    EGLint contextAttribs[] = { EGL_CONTEXT_CLIENT_VERSION, 2, EGL_NONE };
    mEglContext = eglCreateContext(mEglDisplay, config, EGL_NO_CONTEXT,
        contextAttribs);
    CHECK_EGL_ERROR;

    eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext);
    CHECK_EGL_ERROR;
}

void NativeWindowRenderer::terminateEGL() {
    eglDestroyContext(mEglDisplay, mEglContext);
    eglDestroySurface(mEglDisplay, mEglSurface);
    eglMakeCurrent(mEglDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
    eglTerminate(mEglDisplay);
}

void NativeWindowRenderer::createPrograms() {
    GLuint vShader;
    loadShader(GL_VERTEX_SHADER, vSrcNormal, &vShader);

    const char* fSrc[NUMBER_OF_EFFECTS] = {
        fSrcNormal, fSrcSepia, fSrcNegative, fSrcGradient
    };

    for (int i = 0; i < NUMBER_OF_EFFECTS; i++) {
        GLuint fShader;
        loadShader(GL_FRAGMENT_SHADER, fSrc[i], &fShader);
        createProgram(vShader, fShader, &mProgram[i]);
        glDeleteShader(fShader);
        CHECK_GL_ERROR;
    }

    glDeleteShader(vShader);
    CHECK_GL_ERROR;
}

void NativeWindowRenderer::createProgram(
    GLuint vertexShader, GLuint fragmentShader, GLuint* outPgm) {

    GLuint program = glCreateProgram();
    CHECK_GL_ERROR;

    glAttachShader(program, vertexShader);
    CHECK_GL_ERROR;

    glAttachShader(program, fragmentShader);
    CHECK_GL_ERROR;

    glLinkProgram(program);
    CHECK_GL_ERROR;

    GLint linkStatus = GL_FALSE;
    glGetProgramiv(program, GL_LINK_STATUS, &linkStatus);
    if (linkStatus != GL_TRUE) {
        GLint infoLen = 0;
        glGetProgramiv(program, GL_INFO_LOG_LENGTH, &infoLen);
        if (infoLen) {
            char* buf = (char*) malloc(infoLen);
            if (buf) {
                glGetProgramInfoLog(program, infoLen, NULL, buf);
                ALOGE("Program link log:\n%s\n", buf);
                free(buf);
            }
        }
        glDeleteProgram(program);
        program = 0;
    }

    *outPgm = program;
}

void NativeWindowRenderer::loadShader(GLenum shaderType, const char* pSource,
        GLuint* outShader) {
    GLuint shader = glCreateShader(shaderType);
    CHECK_GL_ERROR;

    glShaderSource(shader, 1, &pSource, NULL);
    CHECK_GL_ERROR;

    glCompileShader(shader);
    CHECK_GL_ERROR;

    GLint compiled = 0;
    glGetShaderiv(shader, GL_COMPILE_STATUS, &compiled);
    if (!compiled) {
        GLint infoLen = 0;
        glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &infoLen);
        char* buf = (char*) malloc(infoLen);
        if (buf) {
            glGetShaderInfoLog(shader, infoLen, NULL, buf);
            ALOGE("Shader compile log:\n%s\n", buf);
            free(buf);
        }
        glDeleteShader(shader);
        shader = 0;
    }
    *outShader = shader;
}

NativeWindowRenderer::~NativeWindowRenderer() {
    CHECK(mActiveInputs == 0);
    startRequest(CMD_QUIT);
    sendRequest();
}

void NativeWindowRenderer::render(RenderInput* input) {
    sp<GLConsumer> ST = input->mST;
    sp<Surface> STC = input->mSTC;

    if (input->mIsExternalBuffer) {
        queueExternalBuffer(STC.get(), input->mBuffer,
            input->mWidth, input->mHeight);
    } else {
        queueInternalBuffer(STC.get(), input->mBuffer);
    }

    ST->updateTexImage();
    glClearColor(0, 0, 0, 0);
    glClear(GL_COLOR_BUFFER_BIT);

    calculatePositionCoordinates(input->mRenderingMode,
        input->mWidth, input->mHeight);

    const GLfloat textureCoordinates[] = {
         0.0f,  1.0f,
         0.0f,  0.0f,
         1.0f,  0.0f,
         1.0f,  1.0f,
    };

    updateProgramAndHandle(input->mVideoEffect);

    glVertexAttribPointer(mPositionHandle, 2, GL_FLOAT, GL_FALSE, 0,
        mPositionCoordinates);
    CHECK_GL_ERROR;

    glEnableVertexAttribArray(mPositionHandle);
    CHECK_GL_ERROR;

    glVertexAttribPointer(mTexPosHandle, 2, GL_FLOAT, GL_FALSE, 0,
        textureCoordinates);
    CHECK_GL_ERROR;

    glEnableVertexAttribArray(mTexPosHandle);
    CHECK_GL_ERROR;

    GLfloat texMatrix[16];
    ST->getTransformMatrix(texMatrix);
    glUniformMatrix4fv(mTexMatrixHandle, 1, GL_FALSE, texMatrix);
    CHECK_GL_ERROR;

    glBindTexture(GL_TEXTURE_EXTERNAL_OES, input->mTextureId);
    CHECK_GL_ERROR;

    glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(
        GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(
        GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    CHECK_GL_ERROR;

    glDrawArrays(GL_TRIANGLE_FAN, 0, 4);
    CHECK_GL_ERROR;

    eglSwapBuffers(mEglDisplay, mEglSurface);
}

void NativeWindowRenderer::queueInternalBuffer(ANativeWindow *anw,
    MediaBuffer* buffer) {
    int64_t timeUs;
    CHECK(buffer->meta_data()->findInt64(kKeyTime, &timeUs));
    native_window_set_buffers_timestamp(anw, timeUs * 1000);
    status_t err = anw->queueBuffer(anw, buffer->graphicBuffer().get(), -1);
    if (err != 0) {
        ALOGE("queueBuffer failed with error %s (%d)", strerror(-err), -err);
        return;
    }

    sp<MetaData> metaData = buffer->meta_data();
    metaData->setInt32(kKeyRendered, 1);
}

void NativeWindowRenderer::queueExternalBuffer(ANativeWindow* anw,
    MediaBuffer* buffer, int width, int height) {
    native_window_set_buffers_geometry(anw, width, height,
            HAL_PIXEL_FORMAT_YV12);
    native_window_set_usage(anw, GRALLOC_USAGE_SW_WRITE_OFTEN);

    ANativeWindowBuffer* anb;
    CHECK(NO_ERROR == native_window_dequeue_buffer_and_wait(anw, &anb));
    CHECK(anb != NULL);

    // Copy the buffer
    uint8_t* img = NULL;
    sp<GraphicBuffer> buf(new GraphicBuffer(anb, false));
    buf->lock(GRALLOC_USAGE_SW_WRITE_OFTEN, (void**)(&img));
    copyI420Buffer(buffer, img, width, height, buf->getStride());
    buf->unlock();
    CHECK(NO_ERROR == anw->queueBuffer(anw, buf->getNativeBuffer(), -1));
}

void NativeWindowRenderer::copyI420Buffer(MediaBuffer* src, uint8_t* dst,
        int srcWidth, int srcHeight, int stride) {
    int strideUV = (stride / 2 + 0xf) & ~0xf;
    uint8_t* p = (uint8_t*)src->data() + src->range_offset();
    // Y
    for (int i = srcHeight; i > 0; i--) {
        memcpy(dst, p, srcWidth);
        dst += stride;
        p += srcWidth;
    }
    // The src is I420, the dst is YV12.
    // U
    p += srcWidth * srcHeight / 4;
    for (int i = srcHeight / 2; i > 0; i--) {
        memcpy(dst, p, srcWidth / 2);
        dst += strideUV;
        p += srcWidth / 2;
    }
    // V
    p -= srcWidth * srcHeight / 2;
    for (int i = srcHeight / 2; i > 0; i--) {
        memcpy(dst, p, srcWidth / 2);
        dst += strideUV;
        p += srcWidth / 2;
    }
}

void NativeWindowRenderer::updateProgramAndHandle(uint32_t videoEffect) {
    if (mLastVideoEffect == videoEffect) {
        return;
    }

    mLastVideoEffect = videoEffect;
    int i;
    switch (mLastVideoEffect) {
        case VIDEO_EFFECT_NONE:
            i = 0;
            break;
        case VIDEO_EFFECT_SEPIA:
            i = 1;
            break;
        case VIDEO_EFFECT_NEGATIVE:
            i = 2;
            break;
        case VIDEO_EFFECT_GRADIENT:
            i = 3;
            break;
        default:
            i = 0;
            break;
    }
    glUseProgram(mProgram[i]);
    CHECK_GL_ERROR;

    mPositionHandle = glGetAttribLocation(mProgram[i], "vPosition");
    mTexPosHandle = glGetAttribLocation(mProgram[i], "vTexPos");
    mTexMatrixHandle = glGetUniformLocation(mProgram[i], "texMatrix");
    CHECK_GL_ERROR;
}

void NativeWindowRenderer::calculatePositionCoordinates(
        M4xVSS_MediaRendering renderingMode, int srcWidth, int srcHeight) {
    float x, y;
    switch (renderingMode) {
        case M4xVSS_kResizing:
        default:
            x = 1;
            y = 1;
            break;
        case M4xVSS_kCropping:
            x = float(srcWidth) / mDstWidth;
            y = float(srcHeight) / mDstHeight;
            // Make the smaller side 1
            if (x > y) {
                x /= y;
                y = 1;
            } else {
                y /= x;
                x = 1;
            }
            break;
        case M4xVSS_kBlackBorders:
            x = float(srcWidth) / mDstWidth;
            y = float(srcHeight) / mDstHeight;
            // Make the larger side 1
            if (x > y) {
                y /= x;
                x = 1;
            } else {
                x /= y;
                y = 1;
            }
            break;
    }

    mPositionCoordinates[0] = -x;
    mPositionCoordinates[1] = y;
    mPositionCoordinates[2] = -x;
    mPositionCoordinates[3] = -y;
    mPositionCoordinates[4] = x;
    mPositionCoordinates[5] = -y;
    mPositionCoordinates[6] = x;
    mPositionCoordinates[7] = y;
}

//
//  The functions below run in other threads.
//

void NativeWindowRenderer::startRequest(int cmd) {
    mLock.lock();
    while (mThreadCmd != CMD_IDLE) {
        mCond.wait(mLock);
    }
    mThreadCmd = cmd;
}

void NativeWindowRenderer::sendRequest() {
    mCond.broadcast();
    while (mThreadCmd != CMD_IDLE) {
        mCond.wait(mLock);
    }
    mLock.unlock();
}

RenderInput* NativeWindowRenderer::createRenderInput() {
    ALOGD("new render input %d", mNextTextureId);
    RenderInput* input = new RenderInput(this, mNextTextureId);

    startRequest(CMD_RESERVE_TEXTURE);
    mThreadTextureId = mNextTextureId;
    sendRequest();

    mNextTextureId++;
    mActiveInputs++;
    return input;
}

void NativeWindowRenderer::destroyRenderInput(RenderInput* input) {
    ALOGD("destroy render input %d", input->mTextureId);
    GLuint textureId = input->mTextureId;
    delete input;

    startRequest(CMD_DELETE_TEXTURE);
    mThreadTextureId = textureId;
    sendRequest();

    mActiveInputs--;
}

//
//  RenderInput
//

RenderInput::RenderInput(NativeWindowRenderer* renderer, GLuint textureId)
    : mRenderer(renderer)
    , mTextureId(textureId) {
    sp<BufferQueue> bq = new BufferQueue();
    mST = new GLConsumer(bq, mTextureId);
    mSTC = new Surface(bq);
    native_window_connect(mSTC.get(), NATIVE_WINDOW_API_MEDIA);
}

RenderInput::~RenderInput() {
}

ANativeWindow* RenderInput::getTargetWindow() {
    return mSTC.get();
}

void RenderInput::updateVideoSize(sp<MetaData> meta) {
    CHECK(meta->findInt32(kKeyWidth, &mWidth));
    CHECK(meta->findInt32(kKeyHeight, &mHeight));

    int left, top, right, bottom;
    if (meta->findRect(kKeyCropRect, &left, &top, &right, &bottom)) {
        mWidth = right - left + 1;
        mHeight = bottom - top + 1;
    }

    // If rotation degrees is 90 or 270, swap width and height
    // (mWidth and mHeight are the _rotated_ source rectangle).
    int32_t rotationDegrees;
    if (!meta->findInt32(kKeyRotation, &rotationDegrees)) {
        rotationDegrees = 0;
    }

    if (rotationDegrees == 90 || rotationDegrees == 270) {
        int tmp = mWidth;
        mWidth = mHeight;
        mHeight = tmp;
    }
}

void RenderInput::render(MediaBuffer* buffer, uint32_t videoEffect,
        M4xVSS_MediaRendering renderingMode, bool isExternalBuffer) {
    mVideoEffect = videoEffect;
    mRenderingMode = renderingMode;
    mIsExternalBuffer = isExternalBuffer;
    mBuffer = buffer;

    mRenderer->startRequest(NativeWindowRenderer::CMD_RENDER_INPUT);
    mRenderer->mThreadRenderInput = this;
    mRenderer->sendRequest();
}

}  // namespace android
