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
import com.replica.replicaisland.HotSpotSystem.HotSpotType;

/**
 * This component implements the "patrolling" behavior for AI characters.  Patrolling characters
 * will walk forward on the map until they hit a direction hot spot or a wall, in which case they
 * may change direction.  Patrollers can also be configured via this component to attack the player
 * if appropriate conditions are met.
 */
public class PatrolComponent extends GameComponent {
    private float mMaxSpeed;
    private float mAcceleration;
    private boolean mAttack;
    private float mAttackAtDistance;
    private boolean mAttackStopsMovement;
    private float mAttackDuration;
    private float mAttackDelay;
    private boolean mTurnToFacePlayer;
    private boolean mFlying;
    
    private float mLastAttackTime;
    Vector2 mWorkingVector;
    Vector2 mWorkingVector2;

    
    public PatrolComponent() {
        super();
        mWorkingVector = new Vector2();
        mWorkingVector2 = new Vector2();

        reset();
        setPhase(GameComponent.ComponentPhases.THINK.ordinal());
    }
    
    @Override
    public void reset() {
        mTurnToFacePlayer = false;
        mMaxSpeed = 0.0f;
        mAcceleration = 0.0f;
        mAttack = false;
        mAttackAtDistance = 0.0f;
        mAttackStopsMovement = false;
        mAttackDuration = 0.0f;
        mAttackDelay = 0.0f;
        mWorkingVector.zero();
        mWorkingVector2.zero();
        mFlying = false;
    }
    
    @Override
    public void update(float timeDelta, BaseObject parent) {   
        GameObject parentObject = (GameObject) parent;
        
        if (parentObject.getCurrentAction() == ActionType.INVALID
        	|| parentObject.getCurrentAction() == ActionType.HIT_REACT) {
            parentObject.setCurrentAction(GameObject.ActionType.MOVE);
        }
        
        if ((mFlying || parentObject.touchingGround()) && parentObject.life > 0) {
            GameObjectManager manager = sSystemRegistry.gameObjectManager;
            GameObject player = null;
            if (manager != null) {
                player = manager.getPlayer();
            }
            
            if (mAttack) {
                updateAttack(player, parentObject);
            }
            
            
            if (parentObject.getCurrentAction() == GameObject.ActionType.MOVE
                    && mMaxSpeed > 0.0f) {
                int hotSpot = HotSpotSystem.HotSpotType.NONE;
                HotSpotSystem hotSpotSystem = sSystemRegistry.hotSpotSystem;
                if (hotSpotSystem != null) {
                    // TODO: ack, magic number
                    hotSpot = hotSpotSystem.getHotSpot(parentObject.getCenteredPositionX(), 
                            parentObject.getPosition().y + 10.0f);
                }
                final float targetVelocityX = parentObject.getTargetVelocity().x;
                final float targetVelocityY = parentObject.getTargetVelocity().y;

                boolean goLeft = (parentObject.touchingRightWall() 
                        || hotSpot == HotSpotType.GO_LEFT) && targetVelocityX >= 0.0f;
                    
                boolean goRight = (parentObject.touchingLeftWall() 
                        || hotSpot == HotSpotType.GO_RIGHT) && targetVelocityX <= 0.0f;
                    
                boolean pause = (mMaxSpeed == 0.0f) || hotSpot == HotSpotType.GO_DOWN;
                
                if (mTurnToFacePlayer && player != null && player.life > 0) {
                    final float horizontalDelta = player.getCenteredPositionX() 
                        - parentObject.getCenteredPositionX();
                    final int targetFacingDirection = Utils.sign(horizontalDelta);
                    final float closestDistance = player.width / 2.0f;
                    
                    if (targetFacingDirection < 0.0f) { // we want to turn to the left
                        if (goRight) {
                            goRight = false;
                            pause = true;
                        } else if (targetFacingDirection 
                                != Utils.sign(parentObject.facingDirection.x)) {
                            goLeft = true;
                        }
                    } else if (targetFacingDirection > 0.0f) { // we want to turn to the right
                        if (goLeft) {
                            goLeft = false;
                            pause = true;
                        } else if (targetFacingDirection 
                                != Utils.sign(parentObject.facingDirection.x)) {
                            goRight = true;
                        }
                    }
                    
                    if (Math.abs(horizontalDelta) < closestDistance) {
                        goRight = false;
                        goLeft = false;
                        pause = true;
                    }
                }
                
                if (!mFlying) {
                    if (!pause && !goLeft && !goRight && targetVelocityX == 0.0f) {
                        if (parentObject.facingDirection.x < 0.0f) {
                            goLeft = true;
                        } else {
                            goRight = true;
                        }
                    }
                    
                    
                    if (goRight) {
                        parentObject.getTargetVelocity().x = mMaxSpeed;
                        parentObject.getAcceleration().x = mAcceleration;
                    } else if (goLeft) {
                        parentObject.getTargetVelocity().x = -mMaxSpeed;
                        parentObject.getAcceleration().x = mAcceleration;
                    } else if (pause) {
                        parentObject.getTargetVelocity().x = 0;
                        parentObject.getAcceleration().x = mAcceleration;
                    }
                } else {
                    final boolean goUp = (parentObject.touchingGround() && targetVelocityY < 0.0f) 
                    	|| hotSpot == HotSpotType.GO_UP;
                        
                    final boolean goDown = (parentObject.touchingCeiling() && targetVelocityY > 0.0f)
                            || hotSpot == HotSpotType.GO_DOWN;
                            
                    if (goUp) {
                        parentObject.getTargetVelocity().x = 0.0f;
                        parentObject.getTargetVelocity().y = mMaxSpeed;
                        parentObject.getAcceleration().y = mAcceleration;
                        parentObject.getAcceleration().x = mAcceleration;

                    } else if (goDown) {
                        parentObject.getTargetVelocity().x = 0.0f;
                        parentObject.getTargetVelocity().y = -mMaxSpeed;
                        parentObject.getAcceleration().y = mAcceleration;
                        parentObject.getAcceleration().x = mAcceleration;

                    } else if (goRight) {
                        parentObject.getTargetVelocity().x = mMaxSpeed;
                        parentObject.getAcceleration().x = mAcceleration;
                        parentObject.getAcceleration().y = mAcceleration;
                        parentObject.getTargetVelocity().y = 0.0f;
                    } else if (goLeft) {
                        parentObject.getTargetVelocity().x = -mMaxSpeed;
                        parentObject.getAcceleration().x = mAcceleration;
                        parentObject.getAcceleration().y = mAcceleration;
                        parentObject.getTargetVelocity().y = 0.0f;
                    } 
                }
            }
        } else if (!mFlying && !parentObject.touchingGround() && parentObject.life > 0) {
        	// A non-flying unit is in the air.  In this case, just watch for bounces off walls.
        	if (Utils.sign(parentObject.getTargetVelocity().x) != Utils.sign(parentObject.getVelocity().x)) {
        		// Todo: maybe the physics code should adjust target velocity instead in this case?
        		parentObject.getTargetVelocity().x *= -1.0f;
        	}
        }
    }
    
    private void updateAttack(GameObject player, GameObject parentObject) {
        TimeSystem time = sSystemRegistry.timeSystem;
        final float gameTime = time.getGameTime();
        
        boolean visible = true;
        CameraSystem camera = sSystemRegistry.cameraSystem;
        ContextParameters context = sSystemRegistry.contextParameters;
        final float dx = 
            Math.abs(parentObject.getCenteredPositionX() - camera.getFocusPositionX());
        final float dy = 
            Math.abs(parentObject.getCenteredPositionY() - camera.getFocusPositionY());
        if (dx > context.gameWidth / 2.0f || dy > context.gameHeight / 2.0f) {
            visible = false;
        }
        if (visible && parentObject.getCurrentAction() == GameObject.ActionType.MOVE) {
            boolean closeEnough = false;
            boolean timeToAttack = (gameTime - mLastAttackTime) > mAttackDelay;
            if (mAttackAtDistance > 0 && player != null && player.life > 0 
                    && timeToAttack) {
                // only attack if we are facing the player
                if (Utils.sign(player.getPosition().x - parentObject.getPosition().x)
                        == Utils.sign(parentObject.facingDirection.x)) {
                    mWorkingVector.set(parentObject.getPosition());
                    mWorkingVector.x = parentObject.getCenteredPositionX();
                    mWorkingVector2.set(player.getPosition());
                    mWorkingVector2.x = player.getCenteredPositionX();
                    if (mWorkingVector2.distance2(mWorkingVector) < 
                        mAttackAtDistance * mAttackAtDistance) {
                        closeEnough = true;
                    }
                } 
            } else {
                closeEnough = true;  // If no distance has been set, don't worry about
                                     // the player's position.
            }
            
            if (timeToAttack && closeEnough) {
                // Time to attack.
                parentObject.setCurrentAction(GameObject.ActionType.ATTACK);
                mLastAttackTime = gameTime;
                if (mAttackStopsMovement) {
                    parentObject.getVelocity().zero();
                    parentObject.getTargetVelocity().zero();
                }
            }
        } else if (parentObject.getCurrentAction() == GameObject.ActionType.ATTACK) {
            if (gameTime - mLastAttackTime > mAttackDuration) {
                parentObject.setCurrentAction(GameObject.ActionType.MOVE);
                if (mAttackStopsMovement) {
                    parentObject.getTargetVelocity().x = 
                        mMaxSpeed * Utils.sign(parentObject.facingDirection.x);
                    parentObject.getAcceleration().x = mAcceleration;
                }
            }
        }
    }
    
    public void setMovementSpeed(float speed, float acceleration) {
        mMaxSpeed = speed;
        mAcceleration = acceleration;
    }
    
    public void setupAttack(float distance, float duration, float delay, boolean stopMovement) {
        mAttack = true;
        mAttackAtDistance = distance;
        mAttackStopsMovement = stopMovement;
        mAttackDuration = duration;
        mAttackDelay = delay;
    }
    
    public void setTurnToFacePlayer(boolean turn) {
        mTurnToFacePlayer = turn;
    }
    
    public void setFlying(boolean flying) {
        mFlying = flying;
    }
}
