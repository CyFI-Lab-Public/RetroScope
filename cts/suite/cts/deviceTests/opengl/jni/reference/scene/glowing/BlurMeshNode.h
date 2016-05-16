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

#ifndef BLURMESHNODE_H
#define BLURMESHNODE_H

#include <graphics/Matrix.h>
#include <graphics/Mesh.h>
#include <graphics/MeshNode.h>
#include <graphics/Program.h>

class BlurMeshNode: public MeshNode {
public:
    BlurMeshNode(const Mesh* mesh, GLuint fboTexId, GLuint tmpTexId1, GLuint tmpTexId2, float width, float height);
    virtual ~BlurMeshNode() {};
protected:
    virtual void before(Program& program, Matrix& model, Matrix& view, Matrix& projection);
    virtual void after(Program& program, Matrix& model, Matrix& view, Matrix& projection);
    const GLuint mFboTexId;
    const GLuint mTmpTexId1;
    const GLuint mTmpTexId2;
    const float mWidth;
    const float mHeight;
};

#endif
