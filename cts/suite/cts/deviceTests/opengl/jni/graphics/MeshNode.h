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
#ifndef MESHNODE_H
#define MESHNODE_H

#include "Matrix.h"
#include "Mesh.h"
#include "Program.h"
#include "SceneGraphNode.h"

class MeshNode: public SceneGraphNode {
public:
    MeshNode(const Mesh* mesh);
    virtual ~MeshNode() {};
protected:
    virtual void before(Program& program, Matrix& model, Matrix& view, Matrix& projection) = 0;
    virtual void after(Program& program, Matrix& model, Matrix& view, Matrix& projection) = 0;
    const Mesh* mMesh;
};

#endif
