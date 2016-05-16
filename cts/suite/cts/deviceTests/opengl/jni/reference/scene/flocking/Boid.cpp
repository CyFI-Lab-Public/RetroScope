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
#include "Boid.h"

Boid::Boid(float x, float y) :
        mPosition(x, y) {
}

void Boid::flock(const Boid* boids[], int numBoids, int index, float limitX, float limitY) {
    // Reset the acceleration.
    mAcceleration.mX = 0;
    mAcceleration.mY = 0;
    Vector2D separation;
    int separationCount = 0;
    Vector2D alignment;
    int alignmentCount = 0;
    Vector2D cohesion;
    int cohesionCount = 0;
    for (int i = 0; i < numBoids; i++) {
        if (i != index) {
            const Boid* b = boids[i];
            float dist = mPosition.distance(b->mPosition);
            if (dist != 0) {
                // Separation.
                if (dist < DESIRED_BOID_DIST) {
                    Vector2D tmp = mPosition.copy();
                    tmp.sub(b->mPosition);
                    tmp.normalize();
                    tmp.scale(1.0f / dist);
                    separation.add(tmp);
                    separationCount++;
                }
                if (dist < NEIGHBOUR_RADIUS) {
                    // Alignment.
                    alignment.add(b->mVelocity);
                    alignmentCount++;
                    // Cohesion.
                    cohesion.add(b->mPosition);
                    cohesionCount++;
                }
            }
        }
    }

    if (separationCount > 0) {
        separation.scale(1.0f / separationCount);
        separation.scale(SEPARATION_WEIGHT);
        mAcceleration.add(separation);
    }
    if (alignmentCount > 0) {
        alignment.scale(1.0f / alignmentCount);
        alignment.limit(MAX_FORCE);
        alignment.scale(ALIGNMENT_WEIGHT);
        mAcceleration.add(alignment);
    }
    if (cohesionCount > 0) {
        cohesion.scale(1.0f / cohesionCount);
        cohesion.scale(COHESION_WEIGHT);
        Vector2D desired = cohesion.copy();
        desired.sub(mPosition);
        float d = desired.magnitude();
        if (d > 0) {
            desired.normalize();
            desired.scale(MAX_SPEED * ((d < 100.0f) ? d / 100.0f : 1));
            desired.sub(mVelocity);
            desired.limit(MAX_FORCE);
            mAcceleration.add(desired);
        }
    }

    mVelocity.add(mAcceleration);
    mVelocity.limit(MAX_SPEED);
    mPosition.add(mVelocity);
    // Wrap around.
    if (mPosition.mX < -limitX) {
        mPosition.mX = limitX;
    } else if (mPosition.mX > limitX) {
        mPosition.mX = -limitX;
    }
    if (mPosition.mY < -limitY) {
        mPosition.mY = limitY;
    } else if (mPosition.mY > limitY) {
        mPosition.mY = -limitY;
    }
}
