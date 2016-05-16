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
#include "FlockingScene.h"

#include "WaterMeshNode.h"

#include <cstdlib>
#include <cmath>

#include <Trace.h>

#include <graphics/PerspectiveMeshNode.h>
#include <graphics/PerspectiveProgram.h>
#include <graphics/GLUtils.h>
#include <graphics/Matrix.h>
#include <graphics/Mesh.h>
#include <graphics/ProgramNode.h>
#include <graphics/TransformationNode.h>

FlockingScene::FlockingScene(int width, int height) :
        Scene(width, height), mMainProgram(NULL), mWaterProgram(NULL) {
    for (int i = 0; i < NUM_BOIDS; i++) {
        // Generate a boid with a random position. (-50, 50)
        float x = (rand() % 101) - 50.0f;
        float y = (rand() % 101) - 50.0f;
        mBoids[i] = new Boid(x, y);
    }
}

bool FlockingScene::setUpPrograms() {
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
    // Water Program
    vertex = GLUtils::openTextFile("vertex/water");
    fragment = GLUtils::openTextFile("fragment/water");
    if (vertex == NULL || fragment == NULL) {
        return false;
    }
    programId = GLUtils::createProgram(&vertex, &fragment);
    delete[] vertex;
    delete[] fragment;
    if (programId == 0) {
        return false;
    }
    mWaterProgram = new PerspectiveProgram(programId);
    return true;
}

Matrix* FlockingScene::setUpModelMatrix() {
    return new Matrix();
}

Matrix* FlockingScene::setUpViewMatrix() {
    // Position the eye in front of the origin.
    float eyeX = 0.0f;
    float eyeY = 0.0f;
    float eyeZ = 10.0f;

    // We are looking at the origin
    float centerX = 0.0f;
    float centerY = 0.0f;
    float centerZ = 0.0f;

    // Set our up vector.
    float upX = 0.0f;
    float upY = 1.0f;
    float upZ = 0.0f;

    // Set the view matrix.
    return Matrix::newLookAt(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ);
}

Matrix* FlockingScene::setUpProjectionMatrix(float width, float height) {
    // Create a new perspective projection matrix. The height will stay the same
    // while the width will vary as per aspect ratio.
    mDisplayRatio = width / height;
    // Set board dimensions
    mBoardHeight = 1000.0f;
    mBoardWidth = mDisplayRatio * mBoardHeight;
    float left = -mDisplayRatio;
    float right = mDisplayRatio;
    float bottom = -1.0f;
    float top = 1.0f;
    float near = 8.0f;
    float far = 12.0f;

    return Matrix::newFrustum(left, right, bottom, top, near, far);
}

bool FlockingScene::setUpTextures() {
    SCOPED_TRACE();
    mTextureIds.add(GLUtils::loadTexture("texture/fish_dark.png"));
    mTextureIds.add(GLUtils::loadTexture("texture/background.png"));
    mTextureIds.add(GLUtils::loadTexture("texture/water1.png"));
    mTextureIds.add(GLUtils::loadTexture("texture/water2.png"));
    return true;
}

bool FlockingScene::setUpMeshes() {
    SCOPED_TRACE();
    mMeshes.add(GLUtils::loadMesh("mesh/fish.cob"));
    mMeshes.add(GLUtils::loadMesh("mesh/plane.cob"));
    return true;
}

bool FlockingScene::tearDown() {
    SCOPED_TRACE();
    for (int i = 0; i < NUM_BOIDS; i++) {
        delete mBoids[i];
    }
    delete mMainProgram;
    delete mWaterProgram;
    return Scene::tearDown();
}

bool FlockingScene::updateSceneGraphs(int frame) {
    const float MAIN_SCALE = 1.25f; // Scale up as the camera is far away.
    const float LIMIT_X = mBoardWidth / 2.0f;
    const float LIMIT_Y = mBoardHeight / 2.0f;

    ProgramNode* mainSceneGraph = new ProgramNode(*mMainProgram);
    mSceneGraphs.add(mainSceneGraph);
    // Bottom
    Matrix* transformMatrix = Matrix::newScale(MAIN_SCALE * mDisplayRatio, MAIN_SCALE, 0.0f);
    TransformationNode* transformNode = new TransformationNode(transformMatrix);
    mainSceneGraph->addChild(transformNode);
    MeshNode* meshNode = new PerspectiveMeshNode(mMeshes[1], mTextureIds[1]);
    transformNode->addChild(meshNode);
    // Boids
    const float MARGIN = 30.0f; // So the fish dont disappear and appear at the edges.
    for (int i = 0; i < NUM_BOIDS; i++) {
        Boid* b = mBoids[i];
        b->flock((const Boid**) &mBoids, NUM_BOIDS, i, LIMIT_X + MARGIN, LIMIT_Y + MARGIN);
        Vector2D* pos = &(b->mPosition);
        Vector2D* vel = &(b->mVelocity);

        // Normalize to (-1,1)
        float x = pos->mX / (LIMIT_X * BOID_SCALE) * mDisplayRatio;
        float y = pos->mY / (LIMIT_Y * BOID_SCALE);

        const float SCALE = BOID_SCALE * MAIN_SCALE;
        transformMatrix = Matrix::newScale(SCALE, SCALE, SCALE);
        transformMatrix->translate(x, y, 1.0f);
        transformMatrix->rotate(atan2(vel->mY, vel->mX) + M_PI, 0, 0, 1);
        transformNode = new TransformationNode(transformMatrix);
        mainSceneGraph->addChild(transformNode);
        meshNode = new PerspectiveMeshNode(mMeshes[0], mTextureIds[0]);
        transformNode->addChild(meshNode);
    }
    ProgramNode* waterSceneGraph = new ProgramNode(*mWaterProgram);
    mSceneGraphs.add(waterSceneGraph);
    // Top
    transformMatrix = Matrix::newScale(MAIN_SCALE * mDisplayRatio, MAIN_SCALE, 1.0f);
    transformMatrix->translate(0, 0, 0.1f);
    transformNode = new TransformationNode(transformMatrix);
    waterSceneGraph->addChild(transformNode);
    meshNode = new WaterMeshNode(mMeshes[1], frame, mTextureIds[2], mTextureIds[3]);
    transformNode->addChild(meshNode);
    return true;
}

bool FlockingScene::draw() {
    SCOPED_TRACE();
    drawSceneGraph(0); // Draw fish and pond bottom
    // Use blending.
    glEnable (GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    drawSceneGraph(1); // Draw water
    glDisable(GL_BLEND);
    return true;
}
