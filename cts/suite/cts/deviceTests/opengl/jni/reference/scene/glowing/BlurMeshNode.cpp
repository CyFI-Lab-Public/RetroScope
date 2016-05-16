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

#include "BlurMeshNode.h"

BlurMeshNode::BlurMeshNode(const Mesh* mesh, GLuint fboTexId, GLuint tmpTexId1, GLuint tmpTexId2,
        float width, float height) :
        MeshNode(mesh), mFboTexId(fboTexId), mTmpTexId1(tmpTexId1), mTmpTexId2(tmpTexId2),
        mWidth(width), mHeight(height) {
}

void BlurMeshNode::before(Program& program, Matrix& model, Matrix& view, Matrix& projection) {
    int textureUniformHandle = glGetUniformLocation(program.mProgramId, "u_Texture");
    int scaleUniformHandle = glGetUniformLocation(program.mProgramId, "u_Scale");
    int positionHandle = glGetAttribLocation(program.mProgramId, "a_Position");
    int texCoordHandle = glGetAttribLocation(program.mProgramId, "a_TexCoordinate");

    glEnableVertexAttribArray(positionHandle);
    glVertexAttribPointer(positionHandle, 3, GL_FLOAT, false, 0, mMesh->mVertices);
    glEnableVertexAttribArray(texCoordHandle);
    glVertexAttribPointer(texCoordHandle, 2, GL_FLOAT, false, 0, mMesh->mTexCoords);

    // Blur X
    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, mTmpTexId1, 0);
    glActiveTexture (GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, mFboTexId);
    glUniform1i(textureUniformHandle, 0);

    glUniform2f(scaleUniformHandle, 1.0f / mWidth, 0);

    glDrawArrays(GL_TRIANGLES, 0, mMesh->mNumVertices);

    // Blur Y
    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, mTmpTexId2, 0);
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, mTmpTexId1);
    glUniform1i(textureUniformHandle, 0);

    glUniform2f(scaleUniformHandle, 0, 1.0f / mHeight);

    glDrawArrays(GL_TRIANGLES, 0, mMesh->mNumVertices);
}

void BlurMeshNode::after(Program& program, Matrix& model, Matrix& view, Matrix& projection) {
}
