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
#ifndef FLOCKINGSCENE_H
#define FLOCKINGSCENE_H

#include <graphics/Program.h>

#include "../Scene.h"
#include "Boid.h"

class FlockingScene : public Scene {
public:
    FlockingScene(int width, int height);
    virtual ~FlockingScene() {};
    bool setUpTextures();
    bool setUpMeshes();
    bool tearDown();
    bool draw();
    static const int NUM_BOIDS = 100;
protected:
    bool setUpPrograms();
    Matrix* setUpModelMatrix();
    Matrix* setUpViewMatrix();
    Matrix* setUpProjectionMatrix(float width, float height);
    bool updateSceneGraphs(int frame);
private:
    Boid* mBoids[NUM_BOIDS];
    float mDisplayRatio;
    float mBoardWidth;
    float mBoardHeight;
    Program* mMainProgram;
    Program* mWaterProgram;
    static const float BOID_SCALE = 1.0f / 50.0f;
};
#endif
