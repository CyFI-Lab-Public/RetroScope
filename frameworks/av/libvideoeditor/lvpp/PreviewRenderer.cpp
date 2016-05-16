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


#define LOG_NDEBUG 1
#define LOG_TAG "PreviewRenderer"
#include <utils/Log.h>

#include "PreviewRenderer.h"

#include <media/stagefright/foundation/ADebug.h>
#include <gui/Surface.h>

namespace android {

PreviewRenderer* PreviewRenderer::CreatePreviewRenderer (
        const sp<Surface> &surface, size_t width, size_t height) {

    PreviewRenderer* renderer = new PreviewRenderer(surface, width, height);

    if (renderer->init() != 0) {
        delete renderer;
        return NULL;
    }

    return renderer;
}

PreviewRenderer::PreviewRenderer(
        const sp<Surface> &surface,
        size_t width, size_t height)
    : mSurface(surface),
      mWidth(width),
      mHeight(height) {
}

int PreviewRenderer::init() {
    int err = 0;
    ANativeWindow* anw = mSurface.get();

    err = native_window_api_connect(anw, NATIVE_WINDOW_API_CPU);
    if (err) goto fail;

    err = native_window_set_usage(
            anw, GRALLOC_USAGE_SW_READ_NEVER | GRALLOC_USAGE_SW_WRITE_OFTEN);
    if (err) goto fail;

    err = native_window_set_buffer_count(anw, 3);
    if (err) goto fail;

    err = native_window_set_scaling_mode(
            anw, NATIVE_WINDOW_SCALING_MODE_SCALE_TO_WINDOW);
    if (err) goto fail;

    err = native_window_set_buffers_geometry(
            anw, mWidth, mHeight, HAL_PIXEL_FORMAT_YV12);
    if (err) goto fail;

    err = native_window_set_buffers_transform(anw, 0);
    if (err) goto fail;

fail:
    return err;
}

PreviewRenderer::~PreviewRenderer() {
    native_window_api_disconnect(mSurface.get(), NATIVE_WINDOW_API_CPU);
}


//
// Provides a buffer and associated stride
// This buffer is allocated by the SurfaceFlinger
//
// For optimal display performances, you should :
// 1) call getBufferYV12()
// 2) fill the buffer with your data
// 3) call renderYV12() to take these changes into account
//
// For each call to getBufferYV12(), you must also call renderYV12()
// Expected format in the buffer is YV12 formats (similar to YUV420 planar fromat)
// for more details on this YV12 cf hardware/libhardware/include/hardware/hardware.h
//
void PreviewRenderer::getBufferYV12(uint8_t **data, size_t *stride) {
    int err = OK;

    if ((err = native_window_dequeue_buffer_and_wait(mSurface.get(),
            &mBuf)) != 0) {
        ALOGW("native_window_dequeue_buffer_and_wait returned error %d", err);
        return;
    }

    GraphicBufferMapper &mapper = GraphicBufferMapper::get();

    Rect bounds(mWidth, mHeight);

    void *dst;
    CHECK_EQ(0, mapper.lock(mBuf->handle,
            GRALLOC_USAGE_SW_READ_NEVER | GRALLOC_USAGE_SW_WRITE_OFTEN,
            bounds, &dst));

    *data   = (uint8_t*)dst;
    *stride = mBuf->stride;
}


//
// Display the content of the buffer provided by last call to getBufferYV12()
//
// See getBufferYV12() for details.
//
void PreviewRenderer::renderYV12() {
    int err = OK;

    GraphicBufferMapper &mapper = GraphicBufferMapper::get();

    if (mBuf!= NULL) {
        CHECK_EQ(0, mapper.unlock(mBuf->handle));

        if ((err = mSurface->ANativeWindow::queueBuffer(mSurface.get(), mBuf, -1)) != 0) {
            ALOGW("Surface::queueBuffer returned error %d", err);
        }
    }
    mBuf = NULL;
}

}  // namespace android
