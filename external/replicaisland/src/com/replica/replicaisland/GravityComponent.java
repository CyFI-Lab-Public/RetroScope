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
 * A game component that implements gravity.  Adding this component to a game object will cause
 * it to be pulled down towards the ground.
 */
public class GravityComponent extends GameComponent {
    private Vector2 mGravity;
    private Vector2 mScaledGravity;
    private static final Vector2 sDefaultGravity = new Vector2(0.0f, -400.0f);
    
    public GravityComponent() {
        super();
        mGravity = new Vector2(sDefaultGravity);
        mScaledGravity = new Vector2();
        setPhase(ComponentPhases.PHYSICS.ordinal());
    }
    
    @Override
    public void reset() {
        mGravity.set(sDefaultGravity);
    }

    @Override
    public void update(float timeDelta, BaseObject parent) {
    	mScaledGravity.set(mGravity);
        mScaledGravity.multiply(timeDelta);
        ((GameObject) parent).getVelocity().add(mScaledGravity);
    }

    public Vector2 getGravity() {
        return mGravity;
    }
    
    public void setGravityMultiplier(float multiplier) {
        mGravity.set(sDefaultGravity);
        mGravity.multiply(multiplier);
    }
}
