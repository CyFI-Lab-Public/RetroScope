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

public class PlayerComponent extends GameComponent {
    
    private static final float GROUND_IMPULSE_SPEED = 5000.0f;
    private static final float AIR_HORIZONTAL_IMPULSE_SPEED = 4000.0f;
    private static final float AIR_VERTICAL_IMPULSE_SPEED = 1200.0f;
    private static final float AIR_VERTICAL_IMPULSE_SPEED_FROM_GROUND = 250.0f;
    private static final float AIR_DRAG_SPEED = 4000.0f;
    private static final float MAX_GROUND_HORIZONTAL_SPEED = 500.0f;
    private static final float MAX_AIR_HORIZONTAL_SPEED = 150.0f;
    private static final float MAX_UPWARD_SPEED = 250.0f;
    private static final float VERTICAL_IMPULSE_TOLERANCE = 50.0f;
    private static final float FUEL_AMOUNT = 1.0f;
    
    private static final float JUMP_TO_JETS_DELAY = 0.5f;
    
    private static final float STOMP_VELOCITY = -1000.0f;
    private static final float STOMP_DELAY_TIME = 0.15f;
    private static final float STOMP_AIR_HANG_TIME = 0.0f; //0.25f;
    private static final float STOMP_SHAKE_MAGNITUDE = 15.0f;
    private static final float STOMP_VIBRATE_TIME = 0.05f;
    private static final float HIT_REACT_TIME = 0.5f;

    private static final float GHOST_REACTIVATION_DELAY = 0.3f;
    private static final float GHOST_CHARGE_TIME = 0.75f;
    
    private static final int MAX_GEMS_PER_LEVEL = 3;
    
    private static final float NO_GEMS_GHOST_TIME = 3.0f;
    private static final float ONE_GEM_GHOST_TIME = 8.0f;
    private static final float TWO_GEMS_GHOST_TIME = 0.0f; // no limit.
    
    
    public enum State {
        MOVE,
        STOMP,
        HIT_REACT,
        DEAD,
        WIN,
        FROZEN,
        POST_GHOST_DELAY
    }
    
    private boolean mTouchingGround;
    private State mState;
    private float mTimer;
    private float mTimer2;
    private float mFuel;
    private float mJumpTime;
    private boolean mGhostActive;
    private float mGhostDeactivatedTime;
    private float mGhostChargeTime;
    private InventoryComponent mInventory;
    private Vector2 mHotSpotTestPoint;
    private ChangeComponentsComponent mInvincibleSwap;
    private float mInvincibleEndTime;
    private HitReactionComponent mHitReaction;
    private float mFuelAirRefillSpeed;
    private DifficultyConstants mDifficultyConstants;
    private final static DifficultyConstants sDifficultyArray[] = { 
    	new BabyDifficultyConstants(), 
    	new KidsDifficultyConstants(),
    	new AdultsDifficultyConstants()
    };
    private FadeDrawableComponent mInvincibleFader;	// HACK!
    
    // Variables recorded for animation decisions.
    private boolean mRocketsOn;

    public PlayerComponent() {
        super();
        mHotSpotTestPoint = new Vector2();
        reset();
        setPhase(ComponentPhases.THINK.ordinal());
    }
    
    @Override
    public void reset() {
        mTouchingGround = false;
        mState = State.MOVE;
        mTimer = 0.0f;
        mTimer2 = 0.0f;
        mFuel = 0.0f;
        mJumpTime = 0.0f;
        mGhostActive = false;
        mGhostDeactivatedTime = 0.0f;
        mInventory = null;
        mGhostChargeTime = 0.0f;
        mHotSpotTestPoint.zero();
        mInvincibleSwap = null;
        mInvincibleEndTime = 0.0f;
        mHitReaction = null;
        mDifficultyConstants = getDifficultyConstants();
        mFuelAirRefillSpeed = mDifficultyConstants.getFuelAirRefillSpeed();
        mInvincibleFader = null;
    }

    protected void move(float time, float timeDelta, GameObject parentObject) {
        VectorPool pool = sSystemRegistry.vectorPool;
        InputGameInterface input = sSystemRegistry.inputGameInterface;
        
        if (pool != null && input != null) {

            if (mFuel < FUEL_AMOUNT) {
                if (mTouchingGround) {
                    mFuel += mDifficultyConstants.getFuelGroundRefillSpeed() * timeDelta;
                } else {
                    mFuel += mFuelAirRefillSpeed * timeDelta;
                }
                
                if (mFuel > FUEL_AMOUNT) {
                    mFuel = FUEL_AMOUNT;
                }
            }
            
            final InputXY dpad = input.getDirectionalPad();
            final InputButton jumpButton = input.getJumpButton();
            
            if (dpad.getPressed() || jumpButton.getPressed()) {
                Vector2 impulse = pool.allocate();

                if (dpad.getPressed()) {
                    impulse.set(dpad.getX(), 0.0f);
                }
                                
                if (jumpButton.getPressed()) {
                    if (jumpButton.getTriggered(time) && mTouchingGround) {
                    	// In this case, velocity is instant so we don't need to scale
                    	// it by time.
                        impulse.y = AIR_VERTICAL_IMPULSE_SPEED_FROM_GROUND; 
                        mJumpTime = time;
                    } else if (time > mJumpTime + JUMP_TO_JETS_DELAY) {
                        if (mFuel > 0.0f) {
                            mFuel -= timeDelta;
                            impulse.y = AIR_VERTICAL_IMPULSE_SPEED * timeDelta;
                            mRocketsOn = true;
                        }
                        
                    }
                }
                
                float horziontalSpeed = GROUND_IMPULSE_SPEED;
                float maxHorizontalSpeed = MAX_GROUND_HORIZONTAL_SPEED; 
                final boolean inTheAir = !mTouchingGround 
                    || impulse.y > VERTICAL_IMPULSE_TOLERANCE; 
                if (inTheAir) {
                    horziontalSpeed = AIR_HORIZONTAL_IMPULSE_SPEED;
                    maxHorizontalSpeed = MAX_AIR_HORIZONTAL_SPEED;
                } 
            
                impulse.x = (impulse.x * horziontalSpeed * timeDelta);
                         
                // Don't let our jets move us past specific speed thresholds.
                float currentSpeed = parentObject.getVelocity().x;
                final float newSpeed = Math.abs(currentSpeed + impulse.x);
                if (newSpeed > maxHorizontalSpeed) {
                    if (Math.abs(currentSpeed) < maxHorizontalSpeed) {
                        currentSpeed = maxHorizontalSpeed * Utils.sign(impulse.x);
                        parentObject.getVelocity().x = (currentSpeed);
                    }
                    impulse.x = (0.0f); 
                }
                
                if (parentObject.getVelocity().y + impulse.y > MAX_UPWARD_SPEED
                        && Utils.sign(impulse.y) > 0) {
                    impulse.y = (0.0f);
                    if (parentObject.getVelocity().y < MAX_UPWARD_SPEED) {
                        parentObject.getVelocity().y = (MAX_UPWARD_SPEED);
                    }
                }
             
                if (inTheAir) {
                    // Apply drag while in the air.
                    if (Math.abs(currentSpeed) > maxHorizontalSpeed) {
                        float postDragSpeed = currentSpeed - 
                            (AIR_DRAG_SPEED * timeDelta * Utils.sign(currentSpeed));
                        if (Utils.sign(currentSpeed) != Utils.sign(postDragSpeed)) {
                            postDragSpeed = 0.0f;
                        } else if (Math.abs(postDragSpeed) < maxHorizontalSpeed) {
                            postDragSpeed = maxHorizontalSpeed * Utils.sign(postDragSpeed);
                        }
                        parentObject.getVelocity().x = (postDragSpeed);
                    }
                }
    
                parentObject.getImpulse().add(impulse);
                pool.release(impulse);
            }
            
        }
    }
    
    public void update(float timeDelta, BaseObject parent) {

        TimeSystem time = sSystemRegistry.timeSystem;
        GameObject parentObject = (GameObject)parent;
        
        final float gameTime = time.getGameTime();
        mTouchingGround = parentObject.touchingGround();

        mRocketsOn = false;
        
        
        if (parentObject.getCurrentAction() == ActionType.INVALID) {
            gotoMove(parentObject);
        }
        
        if (mInventory != null && mState != State.WIN) {
            InventoryComponent.UpdateRecord inventory = mInventory.getRecord();
            if (inventory.coinCount >= mDifficultyConstants.getCoinsPerPowerup()) {
                inventory.coinCount = 0;
                mInventory.setChanged();
                parentObject.life = mDifficultyConstants.getMaxPlayerLife();
                if (mInvincibleEndTime < gameTime) {
	                mInvincibleSwap.activate(parentObject);
	                mInvincibleEndTime = gameTime + mDifficultyConstants.getGlowDuration();
	                if (mHitReaction != null) {
	                    mHitReaction.setForceInvincible(true);
	                }
                } else {
                	// invincibility is already active, extend it.
                	mInvincibleEndTime = gameTime + mDifficultyConstants.getGlowDuration();
                	// HACK HACK HACK.  This really doesn't go here.
                	// To extend the invincible time we need to increment the value above (easy)
                	// and also tell the component managing the glow sprite to reset its
                	// timer (not easy).  Next time, make a shared value system for this
                	// kind of case!!
                	if (mInvincibleFader != null) {
                		mInvincibleFader.resetPhase();
                	}
                }
            }
            if (inventory.rubyCount >= MAX_GEMS_PER_LEVEL) {
                gotoWin(gameTime);
            }
        }
        
        if (mInvincibleEndTime > 0.0f && (mInvincibleEndTime < gameTime || mState == State.DEAD)) {
            mInvincibleSwap.activate(parentObject);
            mInvincibleEndTime = 0.0f;
            if (mHitReaction != null) {
                mHitReaction.setForceInvincible(false);
            }
        }
       
        
     // Watch for hit reactions or death interrupting the state machine.
        if (mState != State.DEAD && mState != State.WIN ) {
            if (parentObject.life <= 0) {
                gotoDead(gameTime);
            } else if (parentObject.getPosition().y < -parentObject.height) {
                // we fell off the bottom of the screen, die.
                parentObject.life = 0;
                gotoDead(gameTime);
            } else if (mState != State.HIT_REACT
            		&& parentObject.lastReceivedHitType != HitType.INVALID
                    && parentObject.getCurrentAction() == ActionType.HIT_REACT) {
                gotoHitReact(parentObject, gameTime);
            } else {
                HotSpotSystem hotSpot = sSystemRegistry.hotSpotSystem;
                if (hotSpot != null) {
                    // TODO: HACK!  Unify all this code.
                    if (hotSpot.getHotSpot(parentObject.getCenteredPositionX(), 
                            parentObject.getPosition().y + 10.0f) == HotSpotSystem.HotSpotType.DIE) {
                        parentObject.life = 0;
                        gotoDead(gameTime);
                    }
                }
            }
        }
        
        switch(mState) {
            case MOVE: 
                stateMove(gameTime, timeDelta, parentObject);
                break;
            case STOMP:
                stateStomp(gameTime, timeDelta, parentObject);
                break;
            case HIT_REACT:
                stateHitReact(gameTime, timeDelta, parentObject);
                break;
            case DEAD:
                stateDead(gameTime, timeDelta, parentObject);
                break;
            case WIN:
                stateWin(gameTime, timeDelta, parentObject);
                break;
            case FROZEN:
                stateFrozen(gameTime, timeDelta, parentObject);
                break;
            case POST_GHOST_DELAY:
                statePostGhostDelay(gameTime, timeDelta, parentObject);
                break;
            default:
                break;
        }
        
        final HudSystem hud = sSystemRegistry.hudSystem;
        final InputGameInterface input = sSystemRegistry.inputGameInterface;
        if (hud != null) {
            hud.setFuelPercent(mFuel / FUEL_AMOUNT);
        }
    
    }
    
    protected void gotoMove(GameObject parentObject) {
        parentObject.setCurrentAction(GameObject.ActionType.MOVE);
        mState = State.MOVE;
    }
    
    protected void stateMove(float time, float timeDelta, GameObject parentObject) {
        if (!mGhostActive) {
            move(time, timeDelta, parentObject);
            
            final InputGameInterface input = sSystemRegistry.inputGameInterface;
            final InputButton attackButton = input.getAttackButton();
            
            if (attackButton.getTriggered(time) && !mTouchingGround) {
                gotoStomp(parentObject);
            } else if (attackButton.getPressed() && mTouchingGround
                    && mGhostDeactivatedTime + GHOST_REACTIVATION_DELAY < time) {
                mGhostChargeTime += timeDelta;
                if (mGhostChargeTime > GHOST_CHARGE_TIME) {
                    GameObjectFactory factory = sSystemRegistry.gameObjectFactory;
                    GameObjectManager manager = sSystemRegistry.gameObjectManager;
                    if (factory != null && manager != null) {
                        final float x = parentObject.getPosition().x;
                        final float y = parentObject.getPosition().y;
                        float ghostTime = NO_GEMS_GHOST_TIME;
                        if (mInventory != null) {
                            InventoryComponent.UpdateRecord inventory = mInventory.getRecord();
                            if (inventory.rubyCount == 1) {
                                ghostTime = ONE_GEM_GHOST_TIME;
                            } else if (inventory.rubyCount == 2) {
                                ghostTime = TWO_GEMS_GHOST_TIME;
                            }
                        }
                        GameObject ghost = factory.spawnPlayerGhost(x, y, parentObject, ghostTime);
                        
                        manager.add(ghost);
                        mGhostActive = true;
                        CameraSystem camera = sSystemRegistry.cameraSystem;
                        if (camera != null) {
                            camera.setTarget(ghost);
                        }
                                            }
                } 
            } else if (!attackButton.getPressed()) {
                mGhostChargeTime = 0.0f;
            }
        }
        
    }
    
    protected void gotoStomp(GameObject parentObject) {
        parentObject.setCurrentAction(GameObject.ActionType.ATTACK);
        mState = State.STOMP;
        mTimer = -1.0f;
        mTimer2 = -1.0f;
        parentObject.getImpulse().zero();
        parentObject.getVelocity().set(0.0f, 0.0f);
        parentObject.positionLocked = true;
    }
    
    protected void stateStomp(float time, float timeDelta, GameObject parentObject) {
        if (mTimer < 0.0f) {
            // first frame
            mTimer = time;
        } else if (time - mTimer > STOMP_AIR_HANG_TIME) {
            // hang time complete
            parentObject.getVelocity().set(0.0f, STOMP_VELOCITY);
            parentObject.positionLocked = false;
        } 
        
        if (mTouchingGround && mTimer2 < 0.0f) {
            mTimer2 = time;
            CameraSystem camera = sSystemRegistry.cameraSystem;
            if (camera != null) {
                camera.shake(STOMP_DELAY_TIME, STOMP_SHAKE_MAGNITUDE);
            }
            VibrationSystem vibrator = sSystemRegistry.vibrationSystem;
            
            if (vibrator != null) {
                vibrator.vibrate(STOMP_VIBRATE_TIME);
            }
            
            GameObjectFactory factory = sSystemRegistry.gameObjectFactory;
            GameObjectManager manager = sSystemRegistry.gameObjectManager;
            if (factory != null && manager != null) {
                final float x = parentObject.getPosition().x;
                final float y = parentObject.getPosition().y;
                GameObject smoke1 = factory.spawnDust(x, y - 16, true);
                GameObject smoke2 = factory.spawnDust(x + 32, y - 16, false);
                manager.add(smoke1);
                manager.add(smoke2);
            }
        }
        
        if (mTimer2 > 0.0f && time - mTimer2 > STOMP_DELAY_TIME) {
            parentObject.positionLocked = false;
            gotoMove(parentObject);
        }
    }
    
    protected void gotoHitReact(GameObject parentObject, float time) {
    	if (parentObject.lastReceivedHitType == CollisionParameters.HitType.LAUNCH) {
            if (mState != State.FROZEN) {
                gotoFrozen(parentObject);
            }
    	} else {
            mState = State.HIT_REACT;
            mTimer = time;
 
        }
    }
    
    protected void stateHitReact(float time, float timeDelta, GameObject parentObject) {
        // This state just waits until the timer is expired.
        if (time - mTimer > HIT_REACT_TIME) {
            gotoMove(parentObject);
        }
    }
    
    protected void gotoDead(float time) {
        mState = State.DEAD;
        mTimer = time;
    }
    
    protected void stateDead(float time, float timeDelta, GameObject parentObject) {
        if (mTouchingGround && parentObject.getCurrentAction() != ActionType.DEATH) {
            parentObject.setCurrentAction(ActionType.DEATH);
            parentObject.getVelocity().zero();
            parentObject.getTargetVelocity().zero();
        }
        
        if (parentObject.getPosition().y < -parentObject.height) {
            // fell off the bottom of the screen.
            parentObject.setCurrentAction(ActionType.DEATH);
            parentObject.getVelocity().zero();
            parentObject.getTargetVelocity().zero();
        }
        
        if (parentObject.getCurrentAction() == ActionType.DEATH && mTimer > 0.0f) {
            final float elapsed = time - mTimer;
            HudSystem hud = sSystemRegistry.hudSystem;
            if (hud != null && !hud.isFading()) {
                if (elapsed > 2.0f) {
                    hud.startFade(false, 1.5f);
                    hud.sendGameEventOnFadeComplete(GameFlowEvent.EVENT_RESTART_LEVEL, 0);
                    EventRecorder recorder = sSystemRegistry.eventRecorder;
                    if (recorder != null) {
                    	recorder.setLastDeathPosition(parentObject.getPosition());
                    }
                }
            }
           
        }
    }
    
    protected void gotoWin(float time) {
        mState = State.WIN;
        TimeSystem timeSystem = sSystemRegistry.timeSystem;
        mTimer = timeSystem.getRealTime();
        timeSystem.appyScale(0.1f, 8.0f, true);
    }
    
    protected void stateWin(float time, float timeDelta, GameObject parentObject) {
       if (mTimer > 0.0f) {
        	TimeSystem timeSystem = sSystemRegistry.timeSystem;
            final float elapsed = timeSystem.getRealTime() - mTimer;
            HudSystem hud = sSystemRegistry.hudSystem;
            if (hud != null && !hud.isFading()) {
                if (elapsed > 2.0f) {
                    hud.startFade(false, 1.5f);
                    hud.sendGameEventOnFadeComplete(GameFlowEvent.EVENT_GO_TO_NEXT_LEVEL, 0);

                }
            }
            
        }
    }
    
    protected void gotoFrozen(GameObject parentObject) {
        mState = State.FROZEN;
        parentObject.setCurrentAction(ActionType.FROZEN);
    }
    
    protected void stateFrozen(float time, float timeDelta, GameObject parentObject) {
        if (parentObject.getCurrentAction() == ActionType.MOVE) {
            gotoMove(parentObject);
        }
    }
    
    protected void gotoPostGhostDelay() {
        mState = State.POST_GHOST_DELAY;
    }
    
    protected void statePostGhostDelay(float time, float timeDelta, GameObject parentObject) {
        if (time > mGhostDeactivatedTime) {
            if (!mGhostActive) { // The ghost might have activated again during this delay.
                CameraSystem camera = sSystemRegistry.cameraSystem;
                if (camera != null) {
                    camera.setTarget(parentObject);
                }
            }
            gotoMove(parentObject);
        }
    }
    
    public final boolean getRocketsOn() {
        return mRocketsOn;
    }
    
    public final boolean getGhostActive() {
        return mGhostActive;
    }
    
    public final void deactivateGhost(float delay) {
        mGhostActive = false;
        mGhostDeactivatedTime = sSystemRegistry.timeSystem.getGameTime() + delay;
        gotoPostGhostDelay();
    }
    
    public final void setInventory(InventoryComponent inventory) {
        mInventory = inventory;
    }

    public final void setInvincibleSwap(ChangeComponentsComponent invincibleSwap) {
        mInvincibleSwap = invincibleSwap;
    }

    public final void setHitReactionComponent(HitReactionComponent hitReact) {
        mHitReaction = hitReact;
    }
    
    public final void setInvincibleFader(FadeDrawableComponent fader) {
    	mInvincibleFader = fader;
    }
    
    public final void adjustDifficulty(GameObject parent, int levelAttemps ) {
    	// Super basic DDA.
    	// If we've tried this levels several times secretly increase our
        // hit points so the level gets easier.
    	// Also make fuel refill faster in the air after we've died too many times.
    	
    	if (levelAttemps >= mDifficultyConstants.getDDAStage1Attempts()) {
            if (levelAttemps >= mDifficultyConstants.getDDAStage2Attempts()) {
            	parent.life += mDifficultyConstants.getDDAStage2LifeBoost();
            	mFuelAirRefillSpeed = mDifficultyConstants.getDDAStage2FuelAirRefillSpeed();
            } else {
            	parent.life += mDifficultyConstants.getDDAStage1LifeBoost();
            	mFuelAirRefillSpeed = mDifficultyConstants.getDDAStage1FuelAirRefillSpeed();
            }
        }
    	
    	
    }
    
    public static DifficultyConstants getDifficultyConstants() {
    	return sDifficultyArray[sSystemRegistry.contextParameters.difficulty];
    }

}