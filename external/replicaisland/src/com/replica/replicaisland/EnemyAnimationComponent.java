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
 * A general-purpose animation selection system for animating enemy characters.  Most enemy
 * characters behave similarly, so this code tries to decide which animation bets fits their current
 * state.  Other code (such as enemy AI) may move these characters around and change the current
 * ActionType, which will result in this code figuring out which sequence of animations is best to
 * play.
 */
public class EnemyAnimationComponent extends GameComponent {
    
    public enum EnemyAnimations {
        IDLE,
        MOVE,
        ATTACK,
        HIDDEN,
        APPEAR,
    }
    
    private enum AnimationState {
        IDLING,
        MOVING,
        HIDING,
        APPEARING,
        ATTACKING
    }
    
    private SpriteComponent mSprite;
    private AnimationState mState;
    private boolean mFacePlayer;
    
    public EnemyAnimationComponent() {
        super();
        setPhase(ComponentPhases.ANIMATION.ordinal());
        reset();
    }
    
    @Override
    public void reset() {
        mState = AnimationState.IDLING;
        mFacePlayer = false;
        mSprite = null;
    }
    
    @Override
    public void update(float timeDelta, BaseObject parent) {
        if (mSprite != null) {
            GameObject parentObject = (GameObject) parent;
            final float velocityX = parentObject.getVelocity().x;

            GameObject.ActionType currentAction = parentObject.getCurrentAction();

            switch(mState) {
                case IDLING:
                    mSprite.playAnimation(EnemyAnimations.IDLE.ordinal());
                    if (mFacePlayer) {
                        facePlayer(parentObject);
                    }
                    
                    if (currentAction == GameObject.ActionType.ATTACK) {
                        mState = AnimationState.ATTACKING;
                    } else if (currentAction == GameObject.ActionType.HIDE) {
                        mState = AnimationState.HIDING;
                    } else if (Math.abs(velocityX) > 0.0f) {
                        mState = AnimationState.MOVING;
                    } 
                    
                    break;
                    
                case MOVING:
                    mSprite.playAnimation(EnemyAnimations.MOVE.ordinal());
                    final float targetVelocityX = parentObject.getTargetVelocity().x;
                    
                    if (!Utils.close(velocityX, 0.0f)) {
                        if (velocityX < 0.0f && targetVelocityX < 0.0f) {
                            parentObject.facingDirection.x = -1.0f;
                        } else if (velocityX > 0.0f && targetVelocityX > 0.0f) {
                            parentObject.facingDirection.x = 1.0f;
                        }
                    }
                    if (currentAction == GameObject.ActionType.ATTACK) {
                        mState = AnimationState.ATTACKING;
                    } else if (currentAction == GameObject.ActionType.HIDE) {
                        mState = AnimationState.HIDING;
                    } else if (Math.abs(velocityX) == 0.0f) {
                        mState = AnimationState.IDLING;
                    }
                    break;
                case ATTACKING:
                    mSprite.playAnimation(EnemyAnimations.ATTACK.ordinal());
                    if (currentAction != GameObject.ActionType.ATTACK 
                            && mSprite.animationFinished()) {
                        mState = AnimationState.IDLING;
                    }
                    break;
                case HIDING:
                    mSprite.playAnimation(EnemyAnimations.HIDDEN.ordinal());
                    if (currentAction != GameObject.ActionType.HIDE) {
                        mState = AnimationState.APPEARING;
                    }
                    break;
                case APPEARING:
                    if (mFacePlayer) {
                        facePlayer(parentObject);
                    }
                    
                    mSprite.playAnimation(EnemyAnimations.APPEAR.ordinal());
                    if (mSprite.animationFinished()) {
                        mState = AnimationState.IDLING;
                    }
                    break;
                    
            }
           
            
        }
    }
    
    private void facePlayer(GameObject parentObject) {
        GameObjectManager manager = sSystemRegistry.gameObjectManager;
        if (manager != null) {
            GameObject player = manager.getPlayer();
            if (player != null) {
                if (player.getPosition().x < parentObject.getPosition().x) {
                    parentObject.facingDirection.x = -1.0f;
                } else {
                    parentObject.facingDirection.x = 1.0f;
                }
            }
        }  
    }
    
    public void setSprite(SpriteComponent sprite) {
        mSprite = sprite;
    }
    
    public void setFacePlayer(boolean facePlayer) {
        mFacePlayer = facePlayer;
    }
}
