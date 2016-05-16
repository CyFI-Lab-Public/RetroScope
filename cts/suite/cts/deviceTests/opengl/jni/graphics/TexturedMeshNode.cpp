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

#include "TexturedMeshNode.h"

TexturedMeshNode::TexturedMeshNode(const Mesh* mesh, const GLuint textureId) :
        MeshNode(mesh), mTextureId(textureId) {
}

void TexturedMeshNode::before(Program& program, Matrix& model, Matrix& view, Matrix& projection) {
    int textureUniformHandle = glGetUniformLocation(program.mProgramId, "u_Texture");
    int positionHandle = glGetAttribLocation(program.mProgramId, "a_Position");
    int texCoordHandle = glGetAttribLocation(program.mProgramId, "a_TexCoordinate");

    // Set the texture.
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, mTextureId);
    glUniform1i(textureUniformHandle, 0);

    glEnableVertexAttribArray(positionHandle);
    glVertexAttribPointer(positionHandle, 3, GL_FLOAT, false, 0, mMesh->mVertices);
    glEnableVertexAttribArray(texCoordHandle);
    glVertexAttribPointer(texCoordHandle, 2, GL_FLOAT, false, 0, mMesh->mTexCoords);

    glDrawArrays(GL_TRIANGLES, 0, mMesh->mNumVertices);
}

void TexturedMeshNode::after(Program& program, Matrix& model, Matrix& view, Matrix& projection) {
}
