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

#include "PerspectiveMeshNode.h"

#include "PerspectiveProgram.h"

PerspectiveMeshNode::PerspectiveMeshNode(const Mesh* mesh, const GLuint textureId) :
        TexturedMeshNode(mesh, textureId) {
}

void PerspectiveMeshNode::before(Program& program, Matrix& model, Matrix& view, Matrix& projection) {
    PerspectiveProgram& prog = (PerspectiveProgram&) program;

    int textureUniformHandle = glGetUniformLocation(prog.mProgramId, "u_Texture");
    int positionHandle = glGetAttribLocation(prog.mProgramId, "a_Position");
    int normalHandle = glGetAttribLocation(prog.mProgramId, "a_Normal");
    int texCoordHandle = glGetAttribLocation(prog.mProgramId, "a_TexCoordinate");

    // Set the texture.
    glActiveTexture (GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, mTextureId);
    glUniform1i(textureUniformHandle, 0);

    glEnableVertexAttribArray(positionHandle);
    glVertexAttribPointer(positionHandle, 3, GL_FLOAT, false, 0, mMesh->mVertices);
    glEnableVertexAttribArray(normalHandle);
    glVertexAttribPointer(normalHandle, 3, GL_FLOAT, false, 0, mMesh->mNormals);
    glEnableVertexAttribArray(texCoordHandle);
    glVertexAttribPointer(texCoordHandle, 2, GL_FLOAT, false, 0, mMesh->mTexCoords);

    // This multiplies the view matrix by the model matrix, and stores the result in the MVP
    // matrix (which currently contains model * view).
    prog.mMVMatrix.multiply(view, model);

    // Pass in the modelview matrix.
    glUniformMatrix4fv(prog.mMVMatrixHandle, 1, false, prog.mMVMatrix.mData);

    // This multiplies the modelview matrix by the projection matrix, and stores the result in
    // the MVP matrix (which now contains model * view * projection).
    prog.mMVPMatrix.multiply(projection, prog.mMVMatrix);

    // Pass in the combined matrix.
    glUniformMatrix4fv(prog.mMVPMatrixHandle, 1, false, prog.mMVPMatrix.mData);

    // Pass in the light position in eye space.
    glUniform3f(prog.mLightPosHandle, prog.mLightPosInEyeSpace[0], prog.mLightPosInEyeSpace[1],
            prog.mLightPosInEyeSpace[2]);

    glDrawArrays(GL_TRIANGLES, 0, mMesh->mNumVertices);
}

void PerspectiveMeshNode::after(Program& program, Matrix& model, Matrix& view, Matrix& projection) {
}
