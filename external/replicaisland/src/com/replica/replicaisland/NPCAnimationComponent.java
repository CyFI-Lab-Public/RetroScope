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

import com.replica.replicaisland.ChannelSystem.Channel;
import com.replica.replicaisland.GameObject.ActionType;

public class NPCAnimationComponent extends GameComponent {

    // Animations
    public static final int IDLE = 0;
    public static final int WALK = 1;
    public static final int RUN_START = 2;
    public static final int RUN = 3;
    public static final int SHOOT = 4;
    public static final int JUMP_START = 5;
    public static final int JUMP_AIR = 6;
    public static final int TAKE_HIT = 7;
    public static final int SURPRISED = 8;
    public static final int DEATH = 9;

    
    protected static final float RUN_SPEED_THRESHOLD = 100.0f;
    protected static final float JUMP_SPEED_THRESHOLD = 25.0f;
    protected static final float FALL_SPEED_THRESHOLD = -25.0f;
    protected static final float FALL_TIME_THRESHOLD = 0.2f;
    
    private int mCurrentAnimation;
    private SpriteComponent mSprite;
    private ChannelSystem.Channel mChannel;
    private int mChannelTrigger;
    private boolean mFlying;
    private boolean mStopAtWalls;	// Controls whether or not the character will go back 
    								// to idle when running into a wall

    
    public NPCAnimationComponent() {
        super();
        reset();
        setPhase(GameComponent.ComponentPhases.ANIMATION.ordinal());
    }
    
    @Override
    public void reset() {
        mCurrentAnimation = IDLE;
        mChannel = null;
        mSprite = null;
        mFlying = false;
        mStopAtWalls = true;
    }
    
    @Override
    public void update(float timeDelta, BaseObject parent) {
        if (mSprite != null) {
            GameObject parentObject = (GameObject)parent;
            
            final int oldAnimation = mCurrentAnimation;
            switch(mCurrentAnimation) {
                case IDLE:
                    idle(parentObject);
                    break;
                case WALK:
                    walk(parentObject);
                    break;
                case RUN_START:
                    runStart(parentObject);
                    break;
                case RUN:
                    run(parentObject);
                    break;
                case SHOOT:
                    shoot(parentObject);
                    break;
                case JUMP_START:
                    jumpStart(parentObject);
                    break;
                case JUMP_AIR:
                    jumpAir(parentObject);
                    break;
                case TAKE_HIT:
                	takeHit(parentObject);
                	break;
                case SURPRISED:
                	surprised(parentObject);
                	break;
                case DEATH:
                	death(parentObject);
                	break;
               default:
                    assert(false);
            }
            
            if (mChannel != null) {
            	if (mChannel.value != null 
            			&& ((ChannelSystem.ChannelBooleanValue)mChannel.value).value) {
            		mCurrentAnimation = mChannelTrigger;
            	}
            }
            
            if (oldAnimation != mCurrentAnimation) {
                mSprite.playAnimation(mCurrentAnimation);
            }
        }
    }
    
    protected boolean shouldFall(GameObject parentObject) {
        boolean result = false;
        TimeSystem time = sSystemRegistry.timeSystem;
        final float airTime = time.getGameTime() - parentObject.getLastTouchedFloorTime();
        if (!mFlying && !parentObject.touchingGround() && airTime > FALL_TIME_THRESHOLD) {
            final Vector2 velocity = parentObject.getVelocity();
            if (velocity.y < FALL_SPEED_THRESHOLD) {
                result = true;
            }
        }
        return result;
    }
    
    protected boolean shouldJump(GameObject parentObject) {
        boolean result = false;
        
        if (!mFlying) {
	        final Vector2 velocity = parentObject.getVelocity();
	        if (velocity.y > JUMP_SPEED_THRESHOLD) {
	            result = true;
	        }
        }
        return result;
    }
    
    protected boolean shouldRun(GameObject parentObject) {
        boolean result = false;
        if (!mFlying && parentObject.touchingGround()) {
            final Vector2 velocity = parentObject.getVelocity();
            if (Math.abs(velocity.x) >= RUN_SPEED_THRESHOLD) {
                result = true;
            }
        }
        return result;
    }
    
    protected boolean shouldMove(GameObject parentObject) {
        boolean result = true;
        final Vector2 velocity = parentObject.getVelocity();
        
        if (mStopAtWalls) {
	        if ((velocity.x < 0.0f && parentObject.touchingLeftWall()) 
	                || (velocity.x > 0.0f && parentObject.touchingRightWall())) {
	            result = false;
	        }
        }
        return result;
    }
    
    protected boolean shouldTakeHit(GameObject parentObject) {
    	boolean result = false;
    	if (parentObject.getCurrentAction() == ActionType.HIT_REACT 
    			&& mSprite.findAnimation(TAKE_HIT) != null) {
    		result = true;
    	}
    	return result;
    }
    
    protected void gotoRunStart() {
        if (mSprite.findAnimation(RUN_START) != null) {
            mCurrentAnimation = RUN_START;
        } else {
            mCurrentAnimation = RUN;
        }
    }
    
    protected void gotoRun() {
        mCurrentAnimation = RUN;
    }
    
    protected void idle(GameObject parentObject) {
        final GameObject.ActionType currentAction = parentObject.getCurrentAction();
        if (currentAction == ActionType.MOVE) {
            final Vector2 velocity = parentObject.getVelocity();
            if (shouldFall(parentObject)) {
                mCurrentAnimation = JUMP_AIR;
            } else if (shouldJump(parentObject)) {
                mCurrentAnimation = JUMP_START;
                parentObject.positionLocked = true;
            } else if (Math.abs(velocity.x) > 0.0f && shouldMove(parentObject)) {
                if (shouldRun(parentObject)) {
                	gotoRunStart();
                	parentObject.positionLocked = true;
                } else {
                    mCurrentAnimation = WALK;
                }
            }
        } else if (currentAction == ActionType.ATTACK) {
            mCurrentAnimation = SHOOT;
        } else if (shouldTakeHit(parentObject)) {
        	mCurrentAnimation = TAKE_HIT;
        } else if (parentObject.getCurrentAction() == ActionType.DEATH) {
        	mCurrentAnimation = DEATH;
        }
     }
    
    protected void walk(GameObject parentObject) {
        final GameObject.ActionType currentAction = parentObject.getCurrentAction();
        if (currentAction == ActionType.MOVE) {
            final Vector2 velocity = parentObject.getVelocity();
            if (shouldFall(parentObject)) {
                mCurrentAnimation = JUMP_AIR;
            } else if (shouldJump(parentObject)) {
                mCurrentAnimation = JUMP_START;
                parentObject.positionLocked = true;
            } else if (Math.abs(velocity.x) > 0.0f) {
                if (shouldRun(parentObject)) {
                    gotoRun();
                }
                if (velocity.x > 0.0f) {
                    parentObject.facingDirection.x = 1;
                } else {
                    parentObject.facingDirection.x = -1; 
                }
            } else {
                mCurrentAnimation = IDLE;
            }
        } else if (currentAction == ActionType.ATTACK) {
            mCurrentAnimation = SHOOT;
        } else if (shouldTakeHit(parentObject)) {
        	mCurrentAnimation = TAKE_HIT;
        } else if (parentObject.getCurrentAction() == ActionType.DEATH) {
        	mCurrentAnimation = DEATH;
        }
    }

    protected void runStart(GameObject parentObject) {
    	parentObject.positionLocked = true;
        if (mSprite.animationFinished()) {
            mCurrentAnimation = RUN;
            parentObject.positionLocked = false;
        }
    }
    protected void run(GameObject parentObject) {
        final GameObject.ActionType currentAction = parentObject.getCurrentAction();
        if (currentAction == ActionType.MOVE) {
            final Vector2 velocity = parentObject.getVelocity();
            if (shouldFall(parentObject)) {
                mCurrentAnimation = JUMP_AIR;
            } else if (shouldJump(parentObject)) {
                parentObject.positionLocked = true;
                mCurrentAnimation = JUMP_START;
            } else if (Math.abs(velocity.x) > 0.0f) {
                if (!shouldRun(parentObject)) {
                    mCurrentAnimation = WALK;
                }
                
                if (velocity.x > 0.0f) {
                    parentObject.facingDirection.x = 1;
                } else {
                    parentObject.facingDirection.x = -1; 
                }
            } else {
            	mCurrentAnimation = IDLE;
            }
        } else if (currentAction == ActionType.ATTACK) {
            mCurrentAnimation = SHOOT;
        } else if (shouldTakeHit(parentObject)) {
        	mCurrentAnimation = TAKE_HIT;
        } else if (parentObject.getCurrentAction() == ActionType.DEATH) {
        	mCurrentAnimation = DEATH;
        }
    }

    protected void shoot(GameObject parentObject) {
        if (mSprite.animationFinished() || parentObject.getCurrentAction() != ActionType.ATTACK) {
            mCurrentAnimation = IDLE;
        } else if (shouldTakeHit(parentObject)) {
        	mCurrentAnimation = TAKE_HIT;
        } else if (parentObject.getCurrentAction() == ActionType.DEATH) {
            	mCurrentAnimation = DEATH;
        } else {
            final Vector2 velocity = parentObject.getVelocity();

        	if (velocity.x > 0.0f) {
                parentObject.facingDirection.x = 1;
            } else if (velocity.x < 0.0f) {
                parentObject.facingDirection.x = -1; 
            }
        }
    }

    protected void jumpStart(GameObject parentObject) {
        final Vector2 velocity = parentObject.getVelocity();

        if (velocity.x > 0.0f) {
            parentObject.facingDirection.x = 1;
        } else if (velocity.x < 0.0f) {
            parentObject.facingDirection.x = -1; 
        }
        parentObject.positionLocked = true;
        
        if (mSprite.animationFinished()) {
            mCurrentAnimation = JUMP_AIR;
            parentObject.positionLocked = false;
        }
    }
    
    protected void jumpAir(GameObject parentObject) {
        final GameObject.ActionType currentAction = parentObject.getCurrentAction();
        if (currentAction == ActionType.MOVE) {
            final Vector2 velocity = parentObject.getVelocity();

            if (parentObject.touchingGround()) {
                if (Math.abs(velocity.x) > 0.0f) {
                    if (shouldRun(parentObject)) {
                        mCurrentAnimation = RUN;
                    } else {
                        mCurrentAnimation = WALK;
                    }
                } else {
                    mCurrentAnimation = IDLE;
                }
            } else {
                
                if (velocity.x > 0.0f) {
                    parentObject.facingDirection.x = 1;
                } else if (velocity.x < 0.0f) {
                    parentObject.facingDirection.x = -1; 
                }
                
            }
        } else {
            mCurrentAnimation = IDLE;
        }
    }
      
    protected void takeHit(GameObject parentObject) {
    	if (mSprite.animationFinished()) {
    		if (parentObject.life > 0 && parentObject.getCurrentAction() != ActionType.DEATH) {
    			if (parentObject.getCurrentAction() != ActionType.HIT_REACT) {
    				mCurrentAnimation = IDLE;
    			}
			} else {
    			mCurrentAnimation = DEATH;
    		}
    	}	
    }
    
    protected void surprised(GameObject parentObject) {
    	if (mSprite.animationFinished()) {
    		mCurrentAnimation = IDLE;
    	}
    }
    
    protected void death(GameObject parentObject) {
    }
    
    public void setSprite(SpriteComponent sprite) {
        mSprite = sprite;
    }

	public void setChannel(Channel channel) {
		mChannel = channel;
	}

	public void setChannelTrigger(int animation) {
		mChannelTrigger = animation;
	}
	
	public void setFlying(boolean flying) {
		mFlying = flying;
	}
	
	public void setStopAtWalls(boolean stop) {
		mStopAtWalls = stop;
	}
}
