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


#ifndef SF_RENDERENGINE_H_
#define SF_RENDERENGINE_H_

#include <stdint.h>
#include <sys/types.h>

#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <ui/mat4.h>

// ---------------------------------------------------------------------------
namespace android {
// ---------------------------------------------------------------------------

class String8;
class Rect;
class Region;
class Mesh;
class Texture;

class RenderEngine {
    enum GlesVersion {
        GLES_VERSION_1_0    = 0x10000,
        GLES_VERSION_1_1    = 0x10001,
        GLES_VERSION_2_0    = 0x20000,
        GLES_VERSION_3_0    = 0x30000,
    };
    static GlesVersion parseGlesVersion(const char* str);

    EGLContext mEGLContext;
    void setEGLContext(EGLContext ctxt);

    virtual void bindImageAsFramebuffer(EGLImageKHR image, uint32_t* texName, uint32_t* fbName, uint32_t* status) = 0;
    virtual void unbindFramebuffer(uint32_t texName, uint32_t fbName) = 0;

protected:
    RenderEngine();
    virtual ~RenderEngine() = 0;

public:
    static RenderEngine* create(EGLDisplay display, EGLConfig config);

    // dump the extension strings. always call the base class.
    virtual void dump(String8& result);

    // helpers
    void clearWithColor(float red, float green, float blue, float alpha);
    void fillRegionWithColor(const Region& region, uint32_t height,
            float red, float green, float blue, float alpha);

    // common to all GL versions
    void setScissor(uint32_t left, uint32_t bottom, uint32_t right, uint32_t top);
    void disableScissor();
    void genTextures(size_t count, uint32_t* names);
    void deleteTextures(size_t count, uint32_t const* names);
    void readPixels(size_t l, size_t b, size_t w, size_t h, uint32_t* pixels);

    class BindImageAsFramebuffer {
        RenderEngine& mEngine;
        uint32_t mTexName, mFbName;
        uint32_t mStatus;
    public:
        BindImageAsFramebuffer(RenderEngine& engine, EGLImageKHR image);
        ~BindImageAsFramebuffer();
        int getStatus() const;
    };

    // set-up
    virtual void checkErrors() const;
    virtual void setViewportAndProjection(size_t vpw, size_t vph, size_t w, size_t h, bool yswap) = 0;
    virtual void setupLayerBlending(bool premultipliedAlpha, bool opaque, int alpha) = 0;
    virtual void setupDimLayerBlending(int alpha) = 0;
    virtual void setupLayerTexturing(const Texture& texture) = 0;
    virtual void setupLayerBlackedOut() = 0;
    virtual void setupFillWithColor(float r, float g, float b, float a) = 0;

    virtual void disableTexturing() = 0;
    virtual void disableBlending() = 0;

    // drawing
    virtual void drawMesh(const Mesh& mesh) = 0;

    // grouping
    // creates a color-transform group, everything drawn in the group will be
    // transformed by the given color transform when endGroup() is called.
    virtual void beginGroup(const mat4& colorTransform) = 0;
    virtual void endGroup() = 0;

    // queries
    virtual size_t getMaxTextureSize() const = 0;
    virtual size_t getMaxViewportDims() const = 0;

    EGLContext getEGLContext() const;
};

// ---------------------------------------------------------------------------
}; // namespace android
// ---------------------------------------------------------------------------

#endif /* SF_RENDERENGINE_H_ */
