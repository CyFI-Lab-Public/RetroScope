/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.replica.replicaisland;

/**
 * Helper class for interpolating velocity over time given a target velocity and acceleration.
 * The current velocity will be accelerated towards the target until the target is reached.
 * Note that acceleration is effectively an absolute value--it always points in the direction of
 * the target velocity.
 */
public class Interpolator extends AllocationGuard {

    private float mCurrent;
    private float mTarget;
    private float mAcceleration;
    
    public Interpolator() {
        super();
    }
    
    // Rather than simply interpolating acceleration and velocity for each time step
    // (as in, position += (velocity * time); velocity += (acceleration * time);),
    // we actually perform the work needed to calculate the integral of velocity with respect to
    // time.
    //
    // The integral of velocity is:
    //
    // integral[(v + aT)dT]
    //
    // Simplified to:
    //
    // vT + 1/2 * aT^2
    //
    // Thus:
    // change in position = velocity * time + (0.5 * acceleration * (time^2))
    // change in velocity = acceleration * time

    public void set(float current, float target, float acceleration) {
        mCurrent = current;
        mTarget = target;
        mAcceleration = acceleration;
    }

    // While this function writes directly to velocity, it doesn't affect
    // position.  Instead, the position offset is returned so that it can be blended.
    public float interpolate(float secondsDelta) {
        float oldVelocity = mCurrent;

        // point the acceleration at the target, or zero it if we are already
        // there
        float directionalAcceleration = calculateAcceleration(oldVelocity, mAcceleration, mTarget);

        // calculate scaled acceleration (0.5 * acceleration * (time^2))
        float scaledAcceleration;
        scaledAcceleration = scaleAcceleration(directionalAcceleration, secondsDelta);

        // calculate the change in position
        float positionOffset = (oldVelocity * secondsDelta) + scaledAcceleration;

        // change in velocity = v + aT
        float newVelocity = oldVelocity + (directionalAcceleration * secondsDelta);

        // check to see if we've passed our target velocity since the last time
        // step.  If so, clamp to the target
        if (passedTarget(oldVelocity, newVelocity, mTarget)) {
            newVelocity = mTarget;
        }

        mCurrent = newVelocity;

        return positionOffset;
    }

    public float getCurrent() {
        return mCurrent;
    }

    private boolean passedTarget(float oldVelocity, float newVelocity, float targetVelocity) {
        boolean result = false;

        if (oldVelocity < targetVelocity && newVelocity > targetVelocity) {
            result = true;
        } else if (oldVelocity > targetVelocity && newVelocity < targetVelocity) {
            result = true;
        }

        return result;
    }

    // find the magnitude and direction of acceleration.
    // in this system, acceleration always points toward target velocity
    private float calculateAcceleration(float velocity, float acceleration, float target) {
        if (Math.abs(velocity - target) < 0.0001f) {
            // no accel needed
            acceleration = 0.0f;
        } else if (velocity > target) {
            // accel must be negative
            acceleration *= -1.0f;
        }

        return acceleration;
    }

    // calculates 1/2 aT^2
    private float scaleAcceleration(float acceleration, float secondsDelta) {
        float timeSquared = (secondsDelta * secondsDelta);
        float scaledAccel = acceleration * timeSquared;
        scaledAccel *= 0.5f;

        return scaledAccel;
    }
}
