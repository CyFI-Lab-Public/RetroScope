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

#include "Mesh.h"

namespace android {

Mesh::Mesh(Primitive primitive, size_t vertexCount, size_t vertexSize, size_t texCoordSize)
    : mVertexCount(vertexCount), mVertexSize(vertexSize), mTexCoordsSize(texCoordSize),
      mPrimitive(primitive)
{
    mVertices = new float[(vertexSize + texCoordSize) * vertexCount];
    mStride = mVertexSize + mTexCoordsSize;
}

Mesh::~Mesh() {
    delete [] mVertices;
}

Mesh::Primitive Mesh::getPrimitive() const {
    return mPrimitive;
}


float const* Mesh::getPositions() const {
    return mVertices;
}
float* Mesh::getPositions() {
    return mVertices;
}

float const* Mesh::getTexCoords() const {
    return mVertices + mVertexSize;
}
float* Mesh::getTexCoords() {
    return mVertices + mVertexSize;
}


size_t Mesh::getVertexCount() const {
    return mVertexCount;
}

size_t Mesh::getVertexSize() const {
    return mVertexSize;
}

size_t Mesh::getTexCoordsSize() const {
    return mTexCoordsSize;
}

size_t Mesh::getByteStride() const {
    return mStride*sizeof(float);
}

size_t Mesh::getStride() const {
    return mStride;
}

} /* namespace android */
