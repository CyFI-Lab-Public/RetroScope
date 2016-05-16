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

#ifndef PREVIEW_RENDERER_H_

#define PREVIEW_RENDERER_H_

#include <media/stagefright/ColorConverter.h>
#include <utils/RefBase.h>
#include <system/window.h>
#include <ui/GraphicBufferMapper.h>


namespace android {

class Surface;

class PreviewRenderer {
public:

static PreviewRenderer* CreatePreviewRenderer (
        const sp<Surface> &surface,
        size_t width, size_t height);

    ~PreviewRenderer();

    void getBufferYV12(uint8_t **data, size_t *stride);

    void renderYV12();

    static size_t ALIGN(size_t x, size_t alignment) {
        return (x + alignment - 1) & ~(alignment - 1);
    }

private:
    PreviewRenderer(
            const sp<Surface> &surface,
            size_t width, size_t height);

    int init();

    sp<Surface> mSurface;
    size_t mWidth, mHeight;

    ANativeWindowBuffer *mBuf;

    PreviewRenderer(const PreviewRenderer &);
    PreviewRenderer &operator=(const PreviewRenderer &);
};

}  // namespace android

#endif  // PREVIEW_RENDERER_H_
