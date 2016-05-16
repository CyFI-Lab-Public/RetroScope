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

#ifndef NATIVE_WINDOW_RENDERER_H_
#define NATIVE_WINDOW_RENDERER_H_

#include <EGL/egl.h>
#include <GLES2/gl2.h>
#include <media/stagefright/MediaBuffer.h>
#include <media/stagefright/MetaData.h>
#include <utils/RefBase.h>
#include <utils/threads.h>

#include "M4xVSS_API.h"

// The NativeWindowRenderer draws video frames stored in MediaBuffers to
// an ANativeWindow.  It can apply "rendering mode" and color effects to
// the frames. "Rendering mode" is the option to do resizing, cropping,
// or black-bordering when the source and destination aspect ratio are
// different. Color effects include sepia, negative, and gradient.
//
// The input to NativeWindowRenderer is provided by the RenderInput class,
// and there can be multiple active RenderInput at the same time. Although
// we only expect that happens briefly when one clip is about to finish
// and the next clip is about to start.
//
// We allocate a Surface for each RenderInput and the user can use
// the getTargetWindow() function to get the corresponding ANativeWindow
// for that Surface. The intention is that the user can pass that
// ANativeWindow to OMXCodec::Create() so the codec can decode directly
// to buffers provided by the texture.

namespace android {

class GLConsumer;
class Surface;
class RenderInput;

class NativeWindowRenderer {
public:
    NativeWindowRenderer(sp<ANativeWindow> nativeWindow, int width, int height);
    ~NativeWindowRenderer();

    RenderInput* createRenderInput();
    void destroyRenderInput(RenderInput* input);

private:
    // No copy constructor and assignment
    NativeWindowRenderer(const NativeWindowRenderer &);
    NativeWindowRenderer &operator=(const NativeWindowRenderer &);

    // Initialization and finialization
    void initializeEGL();
    void terminateEGL();
    void createPrograms();
    void createProgram(
            GLuint vertexShader, GLuint fragmentShader, GLuint* outPgm);
    void loadShader(
            GLenum shaderType, const char* pSource, GLuint* outShader);

    // These functions are executed every frame.
    void render(RenderInput* input);
    void queueInternalBuffer(ANativeWindow* anw, MediaBuffer* buffer);
    void queueExternalBuffer(ANativeWindow* anw, MediaBuffer* buffer,
            int width, int height);
    void copyI420Buffer(MediaBuffer* src, uint8_t* dst,
            int srcWidth, int srcHeight, int stride);
    void updateProgramAndHandle(uint32_t videoEffect);
    void calculatePositionCoordinates(M4xVSS_MediaRendering renderingMode,
            int srcWidth, int srcHeight);

    // These variables are initialized once and doesn't change afterwards.
    sp<ANativeWindow> mNativeWindow;
    int mDstWidth, mDstHeight;
    EGLDisplay mEglDisplay;
    EGLSurface mEglSurface;
    EGLContext mEglContext;
    enum {
        EFFECT_NORMAL,
        EFFECT_SEPIA,
        EFFECT_NEGATIVE,
        EFFECT_GRADIENT,
        NUMBER_OF_EFFECTS
    };
    GLuint mProgram[NUMBER_OF_EFFECTS];

    // We use one shader program for each effect. mLastVideoEffect remembers
    // the program used for the last frame. when the effect used changes,
    // we change the program used and update the handles.
    uint32_t mLastVideoEffect;
    GLint mPositionHandle;
    GLint mTexPosHandle;
    GLint mTexMatrixHandle;

    // This is the vertex coordinates used for the frame texture.
    // It's calculated according the the rendering mode and the source and
    // destination aspect ratio.
    GLfloat mPositionCoordinates[8];

    // We use a different GL id for each Surface.
    GLuint mNextTextureId;

    // Number of existing RenderInputs, just for debugging.
    int mActiveInputs;

    // The GL thread functions
    static int threadStart(void* self);
    void glThread();

    // These variables are used to communicate between the GL thread and
    // other threads.
    Mutex mLock;
    Condition mCond;
    enum {
        CMD_IDLE,
        CMD_RENDER_INPUT,
        CMD_RESERVE_TEXTURE,
        CMD_DELETE_TEXTURE,
        CMD_QUIT,
    };
    int mThreadCmd;
    RenderInput* mThreadRenderInput;
    GLuint mThreadTextureId;

    // These functions are used to send commands to the GL thread.
    // sendRequest() also waits for the GL thread acknowledges the
    // command is finished.
    void startRequest(int cmd);
    void sendRequest();

    friend class RenderInput;
};

class RenderInput {
public:
    // Returns the ANativeWindow corresponds to the Surface.
    ANativeWindow* getTargetWindow();

    // Updates video frame size from the MediaSource's metadata. Specifically
    // we look for kKeyWidth, kKeyHeight, and (optionally) kKeyCropRect.
    void updateVideoSize(sp<MetaData> meta);

    // Renders the buffer with the given video effect and rending mode.
    // The video effets are defined in VideoEditorTools.h
    // Set isExternalBuffer to true only when the buffer given is not
    // provided by the Surface.
    void render(MediaBuffer *buffer, uint32_t videoEffect,
        M4xVSS_MediaRendering renderingMode, bool isExternalBuffer);
private:
    RenderInput(NativeWindowRenderer* renderer, GLuint textureId);
    ~RenderInput();
    NativeWindowRenderer* mRenderer;
    GLuint mTextureId;
    sp<GLConsumer> mST;
    sp<Surface> mSTC;
    int mWidth, mHeight;

    // These are only valid during render() calls
    uint32_t mVideoEffect;
    M4xVSS_MediaRendering mRenderingMode;
    bool mIsExternalBuffer;
    MediaBuffer* mBuffer;

    friend class NativeWindowRenderer;
};

}  // namespace android

#endif  // NATIVE_WINDOW_RENDERER_H_
