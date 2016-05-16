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


#ifndef SF_GLES10RENDERENGINE_H_
#define SF_GLES10RENDERENGINE_H_

#include <stdint.h>
#include <sys/types.h>

#include "GLES11RenderEngine.h"

// ---------------------------------------------------------------------------
namespace android {
// ---------------------------------------------------------------------------

class GLES10RenderEngine : public GLES11RenderEngine {
    virtual ~GLES10RenderEngine();
protected:
    virtual void setupLayerBlending(bool premultipliedAlpha, bool opaque, int alpha);
};

// ---------------------------------------------------------------------------
}; // namespace android
// ---------------------------------------------------------------------------

#endif /* SF_GLES10RENDERENGINE_H_ */
