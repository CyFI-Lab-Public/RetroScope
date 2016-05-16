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
#ifndef SHADERPERFRENDERER_H
#define SHADERPERFRENDERER_H

#include <graphics/Renderer.h>

class ShaderPerfRenderer: public Renderer {
public:
    ShaderPerfRenderer(ANativeWindow* window, bool offscreen, int workload);
    virtual ~ShaderPerfRenderer() {};
    bool setUp();
    void drawWorkload();
private:
    GLuint mTextureId;
    GLuint mTextureUniformHandle;
    GLuint mPositionHandle;
    GLuint mTexCoordHandle;
    GLuint mSeedUniformHandle;
};

#endif
