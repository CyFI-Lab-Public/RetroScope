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

import com.replica.replicaisland.GameObject.ActionType;

/**
 * A component that implements the "pop-out" AI behavior.  Pop-out characters alternate between 
 * hiding and appearing based on their distance from the player.  They do not move or normally
 * attack.
 */
public class SleeperComponent extends GameComponent {
    private final static int STATE_SLEEPING = 0;
    private final static int STATE_WAKING = 1;
    private final static int STATE_ATTACKING = 2;
    private final static int STATE_SLAM = 3;
    private final static float DEFAULT_WAKE_UP_DURATION = 3.0f;
    private float mWakeUpDuration;
    private float mStateTime;
    private int mState;
    private float mSlamDuration;
    private float mSlamMagnitude;
    private float mAttackImpulseX;
    private float mAttackImpulseY;
    
    public SleeperComponent() {
        super();
        setPhase(GameComponent.ComponentPhases.THINK.ordinal());
        reset();
    }
    
    @Override
    public void reset() {
        mWakeUpDuration = DEFAULT_WAKE_UP_DURATION;
        mState = STATE_SLEEPING;
        mStateTime = 0.0f;
        mSlamDuration = 0.0f;
        mSlamMagnitude = 0.0f;
        mAttackImpulseX = 0.0f;
        mAttackImpulseY = 0.0f;
    }
    
    @Override
    public void update(float timeDelta, BaseObject parent) {   
        GameObject parentObject = (GameObject) parent;
        
        if (parentObject.getCurrentAction() == ActionType.INVALID) {
            parentObject.setCurrentAction(GameObject.ActionType.IDLE);
            mState = STATE_SLEEPING;
        }
        
        CameraSystem camera = sSystemRegistry.cameraSystem;
        switch(mState) {
            case STATE_SLEEPING:
                if (camera.shaking() && camera.pointVisible(parentObject.getPosition(), parentObject.width / 2.0f)) {
                    mState = STATE_WAKING;
                    mStateTime = mWakeUpDuration;
                    parentObject.setCurrentAction(GameObject.ActionType.MOVE);
                } 
                break;
            case STATE_WAKING:
                mStateTime -= timeDelta;
                if (mStateTime <= 0.0f) {
                    mState = STATE_ATTACKING;
                    parentObject.setCurrentAction(GameObject.ActionType.ATTACK);
                    parentObject.getImpulse().x += mAttackImpulseX * parentObject.facingDirection.x;
                    parentObject.getImpulse().y += mAttackImpulseY;
                }
                break;
            case STATE_ATTACKING:
                if (parentObject.touchingGround() && parentObject.getVelocity().y < 0.0f) {
                    mState = STATE_SLAM;
                    camera.shake(mSlamDuration, mSlamMagnitude);
                    parentObject.getVelocity().zero();
                }
                break;
            case STATE_SLAM:
                if (!camera.shaking()) {
                    mState = STATE_SLEEPING;
                    parentObject.setCurrentAction(GameObject.ActionType.IDLE);
                }
                break;
                
        }
    }


    public void setWakeUpDuration(float duration) {
        mWakeUpDuration = duration;
    }
    
    public void setSlam(float duration, float magnitude) {
        mSlamDuration = duration;
        mSlamMagnitude = magnitude;
    }
    
    public void setAttackImpulse(float x, float y) {
        mAttackImpulseX = x;
        mAttackImpulseY = y;
    }
}


