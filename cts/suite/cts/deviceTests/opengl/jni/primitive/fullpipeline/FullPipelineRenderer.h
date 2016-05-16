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
#ifndef FULLPIPELINERENDERER_H
#define FULLPIPELINERENDERER_H

#include <graphics/PerspectiveProgram.h>
#include <graphics/Matrix.h>
#include <graphics/Mesh.h>
#include <graphics/Renderer.h>
#include <graphics/ProgramNode.h>

class FullPipelineRenderer: public Renderer {
public:
    FullPipelineRenderer(ANativeWindow* window, bool offscreen, int workload);
    virtual ~FullPipelineRenderer() {};
    bool setUp();
    bool tearDown();
    void drawWorkload();
private:
    Program* mProgram;
    ProgramNode* mSceneGraph;
    Matrix* mModelMatrix;
    Matrix* mViewMatrix;
    Matrix* mProjectionMatrix;
    Mesh* mMesh;
    GLuint mTextureId;
};
#endif
