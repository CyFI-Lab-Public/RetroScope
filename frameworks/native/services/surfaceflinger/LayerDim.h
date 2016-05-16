/*
 * Copyright (C) 2007 The Android Open Source Project
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

#ifndef ANDROID_LAYER_DIM_H
#define ANDROID_LAYER_DIM_H

#include <stdint.h>
#include <sys/types.h>

#include "Layer.h"

// ---------------------------------------------------------------------------

namespace android {

class LayerDim : public Layer
{
public:    
                LayerDim(SurfaceFlinger* flinger, const sp<Client>& client,
                        const String8& name, uint32_t w, uint32_t h, uint32_t flags);
        virtual ~LayerDim();

    virtual const char* getTypeId() const { return "LayerDim"; }
    virtual void onDraw(const sp<const DisplayDevice>& hw, const Region& clip) const;
    virtual bool isOpaque() const         { return false; }
    virtual bool isSecure() const         { return false; }
    virtual bool isFixedSize() const      { return true; }
    virtual bool isVisible() const;
};

// ---------------------------------------------------------------------------

}; // namespace android

#endif // ANDROID_LAYER_DIM_H
