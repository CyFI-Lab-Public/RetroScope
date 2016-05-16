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
#ifndef REFERENCERENDERER_H
#define REFERENCERENDERER_H

#include "scene/Scene.h"

#include <graphics/Mesh.h>
#include <graphics/Renderer.h>

class ReferenceRenderer: public Renderer {
public:
    ReferenceRenderer(ANativeWindow* window);
    virtual ~ReferenceRenderer() {};
    bool setUp();
    bool tearDown();
    bool update(int frame);
    void drawWorkload();
    double mSetUpTimes[4];
    static const int FRAMES_PER_SCENE = 500;
    static const int NUM_SCENES = 2;
    static const int NUM_SETUP_TIMES = 4;
private:
    Scene* mScenes[NUM_SCENES];
    Scene* mCurrentScene;

};

#endif
