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
#ifndef SCENE_H
#define SCENE_H

#include <graphics/Matrix.h>
#include <graphics/Mesh.h>
#include <graphics/Program.h>
#include <graphics/ProgramNode.h>

#include <utils/Vector.h>

class Scene {
public:
    Scene(int width, int height);
    virtual ~Scene() {};
    virtual bool setUpContext();
    virtual bool setUpTextures() = 0;
    virtual bool setUpMeshes() = 0;
    virtual bool tearDown();
    virtual bool update(int frame);
    virtual bool draw() = 0;
    void drawSceneGraph(int index);
protected:
    virtual bool setUpPrograms() = 0;
    virtual Matrix* setUpModelMatrix() = 0;
    virtual Matrix* setUpViewMatrix() = 0;
    virtual Matrix* setUpProjectionMatrix(float width, float height) = 0;
    virtual bool updateSceneGraphs(int frame) = 0;
    int mWidth;
    int mHeight;
    android::Vector<Mesh*> mMeshes;
    android::Vector<GLuint> mTextureIds;
    android::Vector<ProgramNode*> mSceneGraphs;
private:
    Matrix* mModelMatrix;
    Matrix* mViewMatrix;
    Matrix* mProjectionMatrix;
};
#endif
