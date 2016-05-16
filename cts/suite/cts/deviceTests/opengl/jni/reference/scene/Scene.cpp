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
#include "Scene.h"

#include <graphics/GLUtils.h>
#include <graphics/ProgramNode.h>

#include <Trace.h>

Scene::Scene(int width, int height) :
        mWidth(width), mHeight(height) {
}

bool Scene::setUpContext() {
    SCOPED_TRACE();
    if (!setUpPrograms()) {
        return false;
    }
    mModelMatrix = setUpModelMatrix();
    if (mModelMatrix == NULL) {
        return false;
    }
    mViewMatrix = setUpViewMatrix();
    if (mViewMatrix == NULL) {
        return false;
    }
    mProjectionMatrix = setUpProjectionMatrix(mWidth, mHeight);
    if (mProjectionMatrix == NULL) {
        return false;
    }
    return true;
}

bool Scene::tearDown() {
    SCOPED_TRACE();
    for (size_t i = 0; i < mTextureIds.size(); i++) {
        glDeleteTextures(1, &(mTextureIds[i]));
    }
    for (size_t i = 0; i < mMeshes.size(); i++) {
        delete mMeshes[i];
    }
    for (size_t i = 0; i < mSceneGraphs.size(); i++) {
        delete mSceneGraphs[i];
    }
    delete mModelMatrix;
    mModelMatrix = NULL;
    delete mViewMatrix;
    mViewMatrix = NULL;
    delete mProjectionMatrix;
    mProjectionMatrix = NULL;
    return true;
}

bool Scene::update(int frame) {
    SCOPED_TRACE();
    // Delete the old scene graphs.
    for (size_t i = 0; i < mSceneGraphs.size(); i++) {
        delete mSceneGraphs[i];
    }
    mSceneGraphs.clear();
    return updateSceneGraphs(frame);
}

void Scene::drawSceneGraph(int index) {
    mModelMatrix->identity();
    mSceneGraphs[index]->drawProgram(*mModelMatrix, *mViewMatrix, *mProjectionMatrix);
}
