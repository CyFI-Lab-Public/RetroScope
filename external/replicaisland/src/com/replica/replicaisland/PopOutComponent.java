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
 * A component that implements the "pop-out" AI behavior.  Pop-out characters alternate between 
 * hiding and appearing based on their distance from the player.  They do not move or normally
 * attack.
 */
public class PopOutComponent extends GameComponent {
    private static final int DEFAULT_APPEAR_DISTANCE = 120;
    private static final int DEFAULT_HIDE_DISTANCE = 190;
    private static final int DEFAULT_ATTACK_DISTANCE = 0;   // No attacking by default.
    private float mAppearDistance;
    private float mHideDistance;
    private float mAttackDistance;
    private float mAttackDelay;
    private float mAttackLength;
    private float mAttackStartTime;
    private Vector2 mDistance;
    private int mState;
    private float mLastAttackCompletedTime;
    
    private final static int STATE_HIDDEN = 0;
    private final static int STATE_VISIBLE = 1;
    private final static int STATE_ATTACKING = 2;
    
    public PopOutComponent() {
        super();
        setPhase(GameComponent.ComponentPhases.THINK.ordinal());
        mDistance = new Vector2();
        reset();
    }
    
    @Override
    public void reset() {
        mAttackDelay = 0;
        mAttackLength = 0;
        mAttackDistance = DEFAULT_ATTACK_DISTANCE;
        mAppearDistance = DEFAULT_APPEAR_DISTANCE;
        mHideDistance = DEFAULT_HIDE_DISTANCE;
        mState = STATE_HIDDEN;
        mLastAttackCompletedTime = 0.0f;
    }
    
    @Override
    public void update(float timeDelta, BaseObject parent) {   
        GameObject parentObject = (GameObject) parent;

        GameObjectManager manager = sSystemRegistry.gameObjectManager;
        if (manager != null) {
            GameObject player = manager.getPlayer();
            if (player != null) {
                mDistance.set(player.getPosition());
                mDistance.subtract(parentObject.getPosition());
                
                TimeSystem time = sSystemRegistry.timeSystem;
                final float currentTime = time.getGameTime();
                
                switch(mState) {
                    case STATE_HIDDEN:
                        parentObject.setCurrentAction(GameObject.ActionType.HIDE);
                        if (mDistance.length2() < (mAppearDistance * mAppearDistance)) {
                            mState = STATE_VISIBLE;
                            mLastAttackCompletedTime = currentTime;
                        }
                        break;
                    case STATE_VISIBLE:
                        parentObject.setCurrentAction(GameObject.ActionType.IDLE);
                        if (mDistance.length2() > (mHideDistance * mHideDistance)) {
                            mState = STATE_HIDDEN;
                        } else if (mDistance.length2() < (mAttackDistance * mAttackDistance) 
                                && currentTime > mLastAttackCompletedTime + mAttackDelay) {
                            mAttackStartTime = currentTime;
                            mState = STATE_ATTACKING;
                        }
                        break;
                    case STATE_ATTACKING:
                        parentObject.setCurrentAction(GameObject.ActionType.ATTACK);
                        if (currentTime > mAttackStartTime + mAttackLength) {
                            mState = STATE_VISIBLE;
                            mLastAttackCompletedTime = currentTime;
                        }
                        break;
                    default:
                        assert false;
                        break;
                }
                
            }
        }
       
    }

    public void setupAttack(float distance, float delay, float duration) {
        mAttackDistance = distance;
        mAttackDelay = delay;
        mAttackLength = duration;
    }
    
    public void setAppearDistance(float appearDistance) {
        mAppearDistance = appearDistance;
    }

    public void setHideDistance(float hideDistance) {
        mHideDistance = hideDistance;
    }
    
    
}


