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

import com.replica.replicaisland.CollisionParameters.HitType;
import com.replica.replicaisland.GameObject.ActionType;

public class NPCComponent extends GameComponent {
    private float mPauseTime;
    private float mTargetXVelocity;
    private int mLastHitTileX;
    private int mLastHitTileY;
    
    private int mDialogEvent;
    private int mDialogIndex;
    
    private HitReactionComponent mHitReactComponent;
    
    private int[] mQueuedCommands;
    private int mQueueTop;
    private int mQueueBottom;
    private boolean mExecutingQueue;
    
    private Vector2 mPreviousPosition;
    
    private float mUpImpulse;
    private float mDownImpulse;
    private float mHorizontalImpulse;
    private float mSlowHorizontalImpulse;
    private float mAcceleration;
    
    private int mGameEvent;
    private int mGameEventIndex;
    private boolean mSpawnGameEventOnDeath;
    
    private boolean mReactToHits;
    private boolean mFlying;
    private boolean mPauseOnAttack;
    
    private float mDeathTime;
	private float mDeathFadeDelay;
    
    private static final float UP_IMPULSE = 400.0f;
    private static final float DOWN_IMPULSE = -10.0f;
    private static final float HORIZONTAL_IMPULSE = 200.0f;
    private static final float SLOW_HORIZONTAL_IMPULSE = 50.0f;
    private static final float ACCELERATION = 300.0f;
    private static final float HIT_IMPULSE = 300.0f;
    private static final float HIT_ACCELERATION = 700.0f;

    private static final float DEATH_FADE_DELAY = 4.0f;
    
    private static final float PAUSE_TIME_SHORT = 1.0f;
    private static final float PAUSE_TIME_MEDIUM = 4.0f;
    private static final float PAUSE_TIME_LONG = 8.0f;
    private static final float PAUSE_TIME_ATTACK = 1.0f;
    private static final float PAUSE_TIME_HIT_REACT = 1.0f;
    
    private static final int COMMAND_QUEUE_SIZE = 16;
    
    public NPCComponent() {
        super();
        setPhase(ComponentPhases.THINK.ordinal());
        mQueuedCommands = new int[COMMAND_QUEUE_SIZE];
        mPreviousPosition = new Vector2();
        reset();
    }
    
    @Override
    public void reset() {
        mPauseTime = 0.0f;
        mTargetXVelocity = 0.0f;
        mLastHitTileX = 0;
        mLastHitTileY = 0;
        mDialogEvent = GameFlowEvent.EVENT_SHOW_DIALOG_CHARACTER1;
        mDialogIndex = 0;
        mHitReactComponent = null;
        mQueueTop = 0;
        mQueueBottom = 0;
        mPreviousPosition.zero();
        mExecutingQueue = false;
        mUpImpulse = UP_IMPULSE;
        mDownImpulse = DOWN_IMPULSE;
        mHorizontalImpulse = HORIZONTAL_IMPULSE;
        mSlowHorizontalImpulse = SLOW_HORIZONTAL_IMPULSE;
        mAcceleration = ACCELERATION;
        mGameEvent = -1;
        mGameEventIndex = -1;
        mSpawnGameEventOnDeath = false;
        mReactToHits = false;
        mFlying = false;
        mDeathTime = 0.0f;
        mDeathFadeDelay = DEATH_FADE_DELAY;
        mPauseOnAttack = true;
    }

    @Override
    public void update(float timeDelta, BaseObject parent) {

        GameObject parentObject = (GameObject)parent;
        
        if (mReactToHits && 
        		mPauseTime <= 0.0f && 
        		parentObject.getCurrentAction() == ActionType.HIT_REACT) {
        	mPauseTime = PAUSE_TIME_HIT_REACT;
            pauseMovement(parentObject);
        	parentObject.getVelocity().x = -parentObject.facingDirection.x * HIT_IMPULSE;
        	parentObject.getAcceleration().x = HIT_ACCELERATION;

        } else if (parentObject.getCurrentAction() == ActionType.DEATH) {        	
        	if (mSpawnGameEventOnDeath && mGameEvent != -1) {
        		if (Utils.close(parentObject.getVelocity().x, 0.0f) 
        				&& parentObject.touchingGround()) {

        			if (mDeathTime < mDeathFadeDelay && mDeathTime + timeDelta >= mDeathFadeDelay) {
        				HudSystem hud = sSystemRegistry.hudSystem;
        	        	
        	        	if (hud != null) {
        	        		hud.startFade(false, 1.5f);
        	        		hud.sendGameEventOnFadeComplete(mGameEvent, mGameEventIndex);
        	        		mGameEvent = -1;
        	        	}
        			}
        			mDeathTime += timeDelta;

        		}
        	}
        	// nothing else to do.
        	return;
        } else if (parentObject.life <= 0) {
        	parentObject.setCurrentAction(ActionType.DEATH);
        	parentObject.getTargetVelocity().x = 0;
        	return;
        } else if (parentObject.getCurrentAction() == ActionType.INVALID ||
        		(!mReactToHits && parentObject.getCurrentAction() == ActionType.HIT_REACT)) {
        	parentObject.setCurrentAction(ActionType.MOVE);
        } 
        
        if (mPauseTime <= 0.0f) {

            HotSpotSystem hotSpotSystem = sSystemRegistry.hotSpotSystem;

            if (hotSpotSystem != null) {
            	final float centerX = parentObject.getCenteredPositionX();
                final int hitTileX = hotSpotSystem.getHitTileX(centerX);
                final int hitTileY = hotSpotSystem.getHitTileY(parentObject.getPosition().y + 10.0f);
                boolean accepted = true;

                if (hitTileX != mLastHitTileX || hitTileY != mLastHitTileY) {

            	    final int hotSpot = hotSpotSystem.getHotSpotByTile(hitTileX, hitTileY);
                    
                    if (hotSpot >= HotSpotSystem.HotSpotType.NPC_GO_RIGHT && hotSpot <= HotSpotSystem.HotSpotType.NPC_SLOW) {
                    	// movement-related commands are immediate
                        parentObject.setCurrentAction(ActionType.MOVE);
                    	accepted = executeCommand(hotSpot, parentObject, timeDelta);
                    } else if (hotSpot == HotSpotSystem.HotSpotType.ATTACK && !mPauseOnAttack) {
                    	// when mPauseOnAttack is false, attacks are also immediate.
                    	accepted = executeCommand(hotSpot, parentObject, timeDelta);
                    } else if (hotSpot == HotSpotSystem.HotSpotType.NPC_RUN_QUEUED_COMMANDS) {
                    	if (!mExecutingQueue && mQueueTop != mQueueBottom) {
                    		mExecutingQueue = true;
                        }
                    } else if (hotSpot > HotSpotSystem.HotSpotType.NONE) {
                    	queueCommand(hotSpot);
                    }
                }
                
                if (mExecutingQueue) {
                	if (mQueueTop != mQueueBottom) {
                		accepted = executeCommand(nextCommand(), parentObject, timeDelta);
                		if (accepted) {
                			advanceQueue();
                		}
                	} else {
                		mExecutingQueue = false;
                	}
                }
                
                if (accepted) {
                	mLastHitTileX = hitTileX;
                	mLastHitTileY = hitTileY;
                }
            	
            }
        } else {
            mPauseTime -= timeDelta;
            if (mPauseTime < 0.0f) {
                resumeMovement(parentObject);
                mPauseTime = 0.0f;
                parentObject.setCurrentAction(ActionType.MOVE);
            }
        }
        
        mPreviousPosition.set(parentObject.getPosition());
    }
    
    private boolean executeCommand(int hotSpot, GameObject parentObject, float timeDelta) {
    	boolean hitAccepted = true;
    	final CameraSystem camera = sSystemRegistry.cameraSystem;
    	
    	switch(hotSpot) {
        case HotSpotSystem.HotSpotType.WAIT_SHORT:
            if (mPauseTime == 0.0f) {
                mPauseTime = PAUSE_TIME_SHORT;
                pauseMovement(parentObject);
            }
            break;
        case HotSpotSystem.HotSpotType.WAIT_MEDIUM:
            if (mPauseTime == 0.0f) {
                mPauseTime = PAUSE_TIME_MEDIUM;
                pauseMovement(parentObject);
            }
            break;
        case HotSpotSystem.HotSpotType.WAIT_LONG:
            if (mPauseTime == 0.0f) {
                mPauseTime = PAUSE_TIME_LONG;
                pauseMovement(parentObject);
            }
            break;
        case HotSpotSystem.HotSpotType.ATTACK:
        	if (mPauseOnAttack) {
	            if (mPauseTime == 0.0f) {
	                mPauseTime = PAUSE_TIME_ATTACK;
	                pauseMovement(parentObject);
	
	            }
        	}
            parentObject.setCurrentAction(ActionType.ATTACK);

            break;
            
        case HotSpotSystem.HotSpotType.TALK:
        	if (mHitReactComponent != null) {
            	if (parentObject.lastReceivedHitType != HitType.COLLECT) {
            		mHitReactComponent.setSpawnGameEventOnHit(
            				HitType.COLLECT, mDialogEvent, mDialogIndex);
            		if (parentObject.getVelocity().x != 0.0f) {
            			pauseMovement(parentObject);
            		}
            		hitAccepted = false;
            	} else {
                    parentObject.setCurrentAction(ActionType.MOVE);

            		resumeMovement(parentObject);	                        		
            		mHitReactComponent.setSpawnGameEventOnHit(HitType.INVALID, 0, 0);
            		parentObject.lastReceivedHitType = HitType.INVALID;
            	}
        	}
        	break;
        
        case HotSpotSystem.HotSpotType.WALK_AND_TALK:
        	if (mDialogEvent != GameFlowEvent.EVENT_INVALID) {
        		LevelSystem level = sSystemRegistry.levelSystem;
        		level.sendGameEvent(mDialogEvent, mDialogIndex, true);
        		mDialogEvent = GameFlowEvent.EVENT_INVALID;
        	}
        	break;
        	
        case HotSpotSystem.HotSpotType.TAKE_CAMERA_FOCUS: 
        	if (camera != null) {
        		camera.setTarget(parentObject);
        	}
        	break;
        	
        case HotSpotSystem.HotSpotType.RELEASE_CAMERA_FOCUS:

        	if (camera != null) {
        		GameObjectManager gameObjectManager = sSystemRegistry.gameObjectManager;
        		camera.setTarget(gameObjectManager.getPlayer());
        	}
        	break;
        	
        case HotSpotSystem.HotSpotType.END_LEVEL:
        	HudSystem hud = sSystemRegistry.hudSystem;
        	
        	if (hud != null) {
        		hud.startFade(false, 1.5f);
        		hud.sendGameEventOnFadeComplete(GameFlowEvent.EVENT_GO_TO_NEXT_LEVEL, 0);
        	}
        	break;
        case HotSpotSystem.HotSpotType.GAME_EVENT:
        	if (mGameEvent != -1) {
    			LevelSystem level = sSystemRegistry.levelSystem;
    			if (level != null) {
    				level.sendGameEvent(mGameEvent, mGameEventIndex, true);
    				mGameEvent = -1;
    			}
    		}
        	break;
        	
        case HotSpotSystem.HotSpotType.NPC_GO_UP_FROM_GROUND:
            if (!parentObject.touchingGround()) {
                hitAccepted = false;
                break;
            }
            // fall through
        case HotSpotSystem.HotSpotType.NPC_GO_UP:
        	parentObject.getVelocity().y = mUpImpulse;
        	parentObject.getTargetVelocity().y = 0.0f;
            mTargetXVelocity = 0.0f;
            
            break;
        case HotSpotSystem.HotSpotType.NPC_GO_DOWN_FROM_CEILING:
            if (!parentObject.touchingCeiling()) {
                hitAccepted = false;
                break;
            }
            // fall through
        case HotSpotSystem.HotSpotType.NPC_GO_DOWN:
        	parentObject.getVelocity().y = mDownImpulse;
        	parentObject.getTargetVelocity().y = 0.0f;
        	if (mFlying) {
        		mTargetXVelocity = 0.0f;
        	}
            break;
        case HotSpotSystem.HotSpotType.NPC_GO_LEFT:
        	parentObject.getTargetVelocity().x = -mHorizontalImpulse;
        	parentObject.getAcceleration().x = mAcceleration;
        	if (mFlying) {
        		parentObject.getVelocity().y = 0.0f;
        		parentObject.getTargetVelocity().y = 0.0f;
        	}
            break;
        case HotSpotSystem.HotSpotType.NPC_GO_RIGHT:
        	parentObject.getTargetVelocity().x = mHorizontalImpulse;
        	parentObject.getAcceleration().x = mAcceleration;
        	if (mFlying) {
        		parentObject.getVelocity().y = 0.0f;
        		parentObject.getTargetVelocity().y = 0.0f;
        	}

            break;
        case HotSpotSystem.HotSpotType.NPC_GO_UP_RIGHT:
        	parentObject.getVelocity().y = mUpImpulse;
        	parentObject.getTargetVelocity().x = mHorizontalImpulse;
        	parentObject.getAcceleration().x = mAcceleration;

            
            break;
        case HotSpotSystem.HotSpotType.NPC_GO_UP_LEFT:
        	parentObject.getVelocity().y = mUpImpulse;
        	parentObject.getTargetVelocity().x = -mHorizontalImpulse;
        	parentObject.getAcceleration().x = mAcceleration;

            
            break;
        case HotSpotSystem.HotSpotType.NPC_GO_DOWN_RIGHT:
        	parentObject.getVelocity().y = mDownImpulse;
        	parentObject.getTargetVelocity().x = mHorizontalImpulse;
        	parentObject.getAcceleration().x = mAcceleration;

            
            break;
        case HotSpotSystem.HotSpotType.NPC_GO_DOWN_LEFT:
        	parentObject.getVelocity().y = mDownImpulse;
        	parentObject.getTargetVelocity().x = -mHorizontalImpulse;
        	parentObject.getAcceleration().x = mAcceleration;

            
            break;
        case HotSpotSystem.HotSpotType.NPC_GO_TOWARDS_PLAYER:
            int direction = 1;
            GameObjectManager manager = sSystemRegistry.gameObjectManager;
            if (manager != null) {
                GameObject player = manager.getPlayer();
                if (player != null) {
                    direction = Utils.sign(
                            player.getCenteredPositionX() -
                            parentObject.getCenteredPositionX());
                }
            }
            parentObject.getTargetVelocity().x = mHorizontalImpulse * direction;
            if (mFlying) {
            	parentObject.getVelocity().y = 0.0f;
        		parentObject.getTargetVelocity().y = 0.0f;
        	}
            break;
        case HotSpotSystem.HotSpotType.NPC_GO_RANDOM:
        	parentObject.getTargetVelocity().x = mHorizontalImpulse * (Math.random() > 0.5f ? -1.0f : 1.0f);
        	if (mFlying) {
        		parentObject.getVelocity().y = 0.0f;
        		parentObject.getTargetVelocity().y = 0.0f;
        	}
            break;
        
        case HotSpotSystem.HotSpotType.NPC_STOP:
        	parentObject.getTargetVelocity().x = 0.0f;
        	parentObject.getVelocity().x = 0.0f;
            break;
        
        case HotSpotSystem.HotSpotType.NPC_SLOW:
        	parentObject.getTargetVelocity().x = mSlowHorizontalImpulse * Utils.sign(parentObject.getTargetVelocity().x);
            break;
            
        case HotSpotSystem.HotSpotType.NPC_SELECT_DIALOG_1_1:
        case HotSpotSystem.HotSpotType.NPC_SELECT_DIALOG_1_2:
        case HotSpotSystem.HotSpotType.NPC_SELECT_DIALOG_1_3:
        case HotSpotSystem.HotSpotType.NPC_SELECT_DIALOG_1_4:
        case HotSpotSystem.HotSpotType.NPC_SELECT_DIALOG_1_5:
        case HotSpotSystem.HotSpotType.NPC_SELECT_DIALOG_2_1:
        case HotSpotSystem.HotSpotType.NPC_SELECT_DIALOG_2_2:
        case HotSpotSystem.HotSpotType.NPC_SELECT_DIALOG_2_3:
        case HotSpotSystem.HotSpotType.NPC_SELECT_DIALOG_2_4:
        case HotSpotSystem.HotSpotType.NPC_SELECT_DIALOG_2_5:
        	selectDialog(hotSpot);
        	break;
        case HotSpotSystem.HotSpotType.NONE:
            if (parentObject.touchingGround() && parentObject.getVelocity().y <= 0.0f) {
                //resumeMovement(parentObject);
            }
            break;
    	}
    	
    	return hitAccepted;
    }
    
    private void pauseMovement(GameObject parentObject) {
    	mTargetXVelocity = parentObject.getTargetVelocity().x;
    	parentObject.getTargetVelocity().x = 0.0f;
    	parentObject.getVelocity().x = 0.0f;
    }
    
    private void resumeMovement(GameObject parentObject) {
    	parentObject.getTargetVelocity().x = mTargetXVelocity;
    	parentObject.getAcceleration().x = mAcceleration;
    }
    
    private void selectDialog(int hitSpot) {
		mDialogEvent = GameFlowEvent.EVENT_SHOW_DIALOG_CHARACTER1;
    	mDialogIndex = hitSpot - HotSpotSystem.HotSpotType.NPC_SELECT_DIALOG_1_1;
    	
    	if (hitSpot >= HotSpotSystem.HotSpotType.NPC_SELECT_DIALOG_2_1) {
    		mDialogEvent = GameFlowEvent.EVENT_SHOW_DIALOG_CHARACTER2;
    		mDialogIndex = hitSpot - HotSpotSystem.HotSpotType.NPC_SELECT_DIALOG_2_1;
    	}
    }
    
    private int nextCommand() {
    	int result = HotSpotSystem.HotSpotType.NONE;
    	if (mQueueTop != mQueueBottom) {
    		result = mQueuedCommands[mQueueTop];
    	}
    	return result;
    }
    
    private int advanceQueue() {
    	int result = HotSpotSystem.HotSpotType.NONE;
    	if (mQueueTop != mQueueBottom) {
    		result = mQueuedCommands[mQueueTop];
    		mQueueTop = (mQueueTop + 1) % COMMAND_QUEUE_SIZE;
    	}
    	return result;
    }
    
    private void queueCommand(int hotspot) {
    	int nextSlot = (mQueueBottom + 1) % COMMAND_QUEUE_SIZE;
    	if (nextSlot != mQueueTop) { // only comply if there is space left in the buffer 
    		mQueuedCommands[mQueueBottom] = hotspot;
    		mQueueBottom = nextSlot;
    	}
    }
    
    public void setHitReactionComponent(HitReactionComponent hitReact) {
    	mHitReactComponent = hitReact;
    }
    
    public void setSpeeds(float horizontalImpulse, float slowHorizontalImpulse, float upImpulse, float downImpulse, float acceleration) {
    	mHorizontalImpulse = horizontalImpulse;
    	mSlowHorizontalImpulse = slowHorizontalImpulse;
    	mUpImpulse = upImpulse;
    	mDownImpulse = downImpulse;
    	mAcceleration = acceleration;
    }
    
    public void setGameEvent(int event, int index, boolean spawnOnDeath) {
    	mGameEvent = event;
    	mGameEventIndex = index;
    	mSpawnGameEventOnDeath = spawnOnDeath;
    }
    
    public void setReactToHits(boolean react) {
    	mReactToHits = react;
    }

    public void setFlying(boolean flying) {
    	mFlying = flying;
    }
    
    public void setPauseOnAttack(boolean pauseOnAttack) {
    	mPauseOnAttack = pauseOnAttack;
    }
}
