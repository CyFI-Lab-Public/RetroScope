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
#include "GlowingScene.h"
#include "BlurMeshNode.h"

#include <Trace.h>

#include <graphics/PerspectiveMeshNode.h>
#include <graphics/PerspectiveProgram.h>
#include <graphics/Program.h>
#include <graphics/GLUtils.h>
#include <graphics/Mesh.h>
#include <graphics/ProgramNode.h>
#include <graphics/TransformationNode.h>

GlowingScene::GlowingScene(int width, int height) :
        Scene(width, height), mFboId(0), mMainProgram(NULL), mBlurProgram(NULL) {
    mFboWidth = GLUtils::roundUpToSmallestPowerOf2(width);
    mFboHeight = GLUtils::roundUpToSmallestPowerOf2(height);
    mFboRatio = mFboWidth / mFboHeight;
    mFboModelMatrix = setUpModelMatrix();
    mFboViewMatrix = setUpViewMatrix();
    mFboProjectionMatrix = setUpProjectionMatrix(mFboWidth, mFboHeight);
}

bool GlowingScene::setUpContext() {
    if (!Scene::setUpContext()) {
        return false;
    }
    // Create a fbo
    glGenFramebuffers(1, &mFboId);
    return true;
}

bool GlowingScene::setUpPrograms() {
    // Main Program
    const char* vertex = GLUtils::openTextFile("vertex/perspective");
    const char* fragment = GLUtils::openTextFile("fragment/perspective");
    if (vertex == NULL || fragment == NULL) {
        return false;
    }
    GLuint programId = GLUtils::createProgram(&vertex, &fragment);
    delete[] vertex;
    delete[] fragment;
    if (programId == 0) {
        return false;
    }
    mMainProgram = new PerspectiveProgram(programId);
    // Blur Program
    vertex = GLUtils::openTextFile("vertex/blur");
    fragment = GLUtils::openTextFile("fragment/blur");
    if (vertex == NULL || fragment == NULL) {
        return false;
    }
    programId = GLUtils::createProgram(&vertex, &fragment);
    delete[] vertex;
    delete[] fragment;
    if (programId == 0) {
        return false;
    }
    mBlurProgram = new Program(programId);
    return true;
}

Matrix* GlowingScene::setUpModelMatrix() {
    return new Matrix();
}

Matrix* GlowingScene::setUpViewMatrix() {
    // Position the eye in front of the origin.
    float eyeX = 0.0f;
    float eyeY = 0.0f;
    float eyeZ = 10.0f;

    // Look at the origin
    float centerX = 0.0f;
    float centerY = 0.0f;
    float centerZ = 0.0f;

    // Set up vector.
    float upX = 0.0f;
    float upY = 1.0f;
    float upZ = 0.0f;

    // Set the view matrix.
    return Matrix::newLookAt(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ);
}

Matrix* GlowingScene::setUpProjectionMatrix(float width, float height) {
    // Create a new perspective projection matrix. The height will stay the same
    // while the width will vary as per aspect ratio.
    float ratio = width / height;
    float left = -ratio;
    float right = ratio;
    float bottom = -1.0f;
    float top = 1.0f;
    float near = 8.0f;
    float far = 12.0f;

    return Matrix::newFrustum(left, right, bottom, top, near, far);
}

bool GlowingScene::setUpTextures() {
    SCOPED_TRACE();
    mTextureIds.add(GLUtils::genTexture(mWidth, mHeight, 0)); // fbo
    mTextureIds.add(GLUtils::genTexture(mWidth, mHeight, 0)); // tmp1
    mTextureIds.add(GLUtils::genTexture(mWidth, mHeight, 0)); // tmp2
    mTextureIds.add(GLUtils::loadTexture("texture/arc.png"));
    return true;
}

bool GlowingScene::setUpMeshes() {
    SCOPED_TRACE();
    mMeshes.add(GLUtils::loadMesh("mesh/plane.cob"));
    mMeshes.add(GLUtils::loadMesh("mesh/arc.cob"));
    return true;
}

bool GlowingScene::tearDown() {
    SCOPED_TRACE();
    if (mMainProgram != NULL) {
        delete mMainProgram;
        mMainProgram = NULL;
    }
    if (mBlurProgram != NULL) {
        delete mBlurProgram;
        mBlurProgram = NULL;
    }
    if (mFboId != 0) {
        glDeleteFramebuffers(1, &mFboId);
        mFboId = 0;
    }
    delete mFboModelMatrix;
    mFboModelMatrix = NULL;
    delete mFboViewMatrix;
    mFboViewMatrix = NULL;
    delete mFboProjectionMatrix;
    mFboProjectionMatrix = NULL;
    return Scene::tearDown();
}

bool GlowingScene::updateSceneGraphs(int frame) {
    // To render the mesh to the FBO
    ProgramNode* lightSceneGraph = new ProgramNode(*mMainProgram);
    mSceneGraphs.add(lightSceneGraph);
    MeshNode* meshNode = new PerspectiveMeshNode(mMeshes[1], mTextureIds[3]);
    lightSceneGraph->addChild(meshNode);

    // To blur the image
    ProgramNode* blurSceneGraph = new ProgramNode(*mBlurProgram);
    mSceneGraphs.add(blurSceneGraph);
    meshNode = new BlurMeshNode(mMeshes[0], mTextureIds[0], mTextureIds[1], mTextureIds[2],
            mFboWidth, mFboHeight);
    blurSceneGraph->addChild(meshNode);

    // Blur To screen
    ProgramNode* glowSceneGraph = new ProgramNode(*mMainProgram);
    mSceneGraphs.add(glowSceneGraph);
    Matrix* transformMatrix = Matrix::newScale(mFboRatio, 1.0f, 1.0f);
    TransformationNode* transformNode = new TransformationNode(transformMatrix);
    glowSceneGraph->addChild(transformNode);
    meshNode = new PerspectiveMeshNode(mMeshes[0], mTextureIds[2]);
    transformNode->addChild(meshNode);
    return true;
}

bool GlowingScene::draw() {
    SCOPED_TRACE();
    glBindFramebuffer(GL_FRAMEBUFFER, mFboId); // Use FBO
    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, mTextureIds[0], 0);
    if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
        return false;
    }
    glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
    glClear (GL_COLOR_BUFFER_BIT);
    glViewport(0, 0, mFboWidth, mFboHeight);
    mFboModelMatrix->identity();
    mSceneGraphs[0]->drawProgram(*mFboModelMatrix, *mFboViewMatrix, *mFboProjectionMatrix); // Mesh
    mFboModelMatrix->identity();
    mSceneGraphs[1]->drawProgram(*mFboModelMatrix, *mFboViewMatrix, *mFboProjectionMatrix); // Blur

    glBindFramebuffer(GL_FRAMEBUFFER, 0); // Use Screen
    glViewport(0, 0, mWidth, mHeight);
    Scene::drawSceneGraph(2); // Blur to Screen
    return true;
}
