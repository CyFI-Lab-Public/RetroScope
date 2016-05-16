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

#include "WaterMeshNode.h"

#include <graphics/PerspectiveProgram.h>

WaterMeshNode::WaterMeshNode(const Mesh* mesh, int time, GLuint textureId1, GLuint textureId2) :
        MeshNode(mesh), mTime(time), mTextureId1(textureId1), mTextureId2(textureId2) {
}

void WaterMeshNode::before(Program& program, Matrix& model, Matrix& view, Matrix& projection) {
    PerspectiveProgram& prog = (PerspectiveProgram&) program;

    int textureUniformHandle1 = glGetUniformLocation(prog.mProgramId, "u_Texture1");
    int textureUniformHandle2 = glGetUniformLocation(prog.mProgramId, "u_Texture2");
    int timeUniformHandle = glGetUniformLocation(prog.mProgramId, "u_Time");
    int positionHandle = glGetAttribLocation(prog.mProgramId, "a_Position");
    int texCoordHandle = glGetAttribLocation(prog.mProgramId, "a_TexCoordinate");

    glActiveTexture (GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, mTextureId1);
    glUniform1i(textureUniformHandle1, 0);

    glActiveTexture (GL_TEXTURE1);
    glBindTexture(GL_TEXTURE_2D, mTextureId2);
    glUniform1i(textureUniformHandle2, 1);

    glUniform1i(timeUniformHandle, mTime);

    glEnableVertexAttribArray(positionHandle);
    glVertexAttribPointer(positionHandle, 3, GL_FLOAT, false, 0, mMesh->mVertices);
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

void WaterMeshNode::after(Program& program, Matrix& model, Matrix& view, Matrix& projection) {
}
