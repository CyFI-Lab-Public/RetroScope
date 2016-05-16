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

#ifndef ANDROID_SURFACE_TEXTURE_LAYER_H
#define ANDROID_SURFACE_TEXTURE_LAYER_H

#include <stdlib.h>
#include <stdint.h>
#include <sys/types.h>

#include <utils/Errors.h>
#include <gui/BufferQueue.h>

namespace android {
// ---------------------------------------------------------------------------

class Layer;
class SurfaceFlinger;

/*
 * This is a thin wrapper around BufferQueue, used by the Layer class.
 */
class SurfaceTextureLayer : public BufferQueue {
    sp<SurfaceFlinger> flinger;
public:
    SurfaceTextureLayer(const sp<SurfaceFlinger>& flinger);
    virtual ~SurfaceTextureLayer();
};

// ---------------------------------------------------------------------------
}; // namespace android

#endif // ANDROID_SURFACE_TEXTURE_LAYER_H
