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
#include "ReferenceRenderer.h"

#include "scene/flocking/FlockingScene.h"
#include "scene/glowing/GlowingScene.h"

#include <graphics/GLUtils.h>
#include <graphics/ProgramNode.h>

#include <Trace.h>

ReferenceRenderer::ReferenceRenderer(ANativeWindow* window) :
        Renderer(window, false, 0) {
}

bool ReferenceRenderer::setUp() {
    SCOPED_TRACE();
    // Reset the times.
    for (int i = 0; i < NUM_SETUP_TIMES; i++) {
        mSetUpTimes[i] = 0;
    }
    // Set up OpenGLES.
    double start = GLUtils::currentTimeMillis();
    if (!Renderer::setUp()) {
        return false;
    }
    mSetUpTimes[0] = GLUtils::currentTimeMillis() - start;

    // Create the scenes.
    mScenes[0] = new FlockingScene(mWidth, mHeight);
    mScenes[1] = new GlowingScene(mWidth, mHeight);
    // TODO add more scenes to do a comprehensive test.

    // Set up the scenes.
    double times[NUM_SETUP_TIMES];
    for (int i = 0; i < NUM_SCENES; i++) {
        times[0] = GLUtils::currentTimeMillis();
        mScenes[i]->setUpContext();
        times[1] = GLUtils::currentTimeMillis();
        mScenes[i]->setUpTextures();
        times[2] = GLUtils::currentTimeMillis();
        mScenes[i]->setUpMeshes();
        times[3] = GLUtils::currentTimeMillis();

        for (int i = 1; i < NUM_SETUP_TIMES; i++) {
            // Add on the set up times.
            mSetUpTimes[i] += times[i] - times[i - 1];
        }
    }
    return true;
}

bool ReferenceRenderer::tearDown() {
    SCOPED_TRACE();
    for (int i = 0; i < NUM_SCENES; i++) {
        mScenes[i]->tearDown();
        delete mScenes[i];
    }
    mCurrentScene = NULL;
    if (!Renderer::tearDown()) {
        return false;
    }
    return true;
}

bool ReferenceRenderer::update(int frame) {
    SCOPED_TRACE();
    int sceneId = frame / ReferenceRenderer::FRAMES_PER_SCENE;
    int localFrame = frame % ReferenceRenderer::FRAMES_PER_SCENE;
    mCurrentScene = mScenes[sceneId];
    mCurrentScene->update(localFrame);
    return true;
}

void ReferenceRenderer::drawWorkload() {
    SCOPED_TRACE();
    // Set the background clear color to black.
    glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
    // Use culling to remove back faces.
    glEnable (GL_CULL_FACE);
    // Use depth testing.
    glEnable (GL_DEPTH_TEST);

    glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT);
    if (!mCurrentScene->draw()) {
        ALOGE("Error when rendering scene");
    }
}
