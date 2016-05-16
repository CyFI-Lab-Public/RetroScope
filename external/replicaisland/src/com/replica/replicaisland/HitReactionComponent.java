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
import com.replica.replicaisland.GameObject.Team;
import com.replica.replicaisland.GameObjectFactory.GameObjectType;

/** 
 * A general-purpose component that responds to dynamic collision notifications.  This component
 * may be configured to produce common responses to hit (taking damage, being knocked back, etc), or
 * it can be derived for entirely different responses.  This component must exist on an object for
 * that object to respond to dynamic collisions.
 */
public class HitReactionComponent extends GameComponent {
    private static final float ATTACK_PAUSE_DELAY = (1.0f / 60) * 4;
    private final static float DEFAULT_BOUNCE_MAGNITUDE = 200.0f;
    private final static float EVENT_SEND_DELAY = 5.0f;
    
    private boolean mPauseOnAttack;
    private float mPauseOnAttackTime;
    private boolean mBounceOnHit;
    private float mBounceMagnitude;
    private float mInvincibleAfterHitTime;
    private float mLastHitTime;
    private boolean mInvincible;
    private boolean mDieOnCollect;
    private boolean mDieOnAttack;
    private ChangeComponentsComponent mPossessionComponent;
    private InventoryComponent.UpdateRecord mInventoryUpdate;
    private LauncherComponent mLauncherComponent;
    private int mLauncherHitType;
    private float mInvincibleTime;
    private int mGameEventHitType;
    private int mGameEventOnHit;
    private int mGameEventIndexData;
    private float mLastGameEventTime;
    private boolean mForceInvincibility;
    private SoundSystem.Sound mTakeHitSound;
    private SoundSystem.Sound mDealHitSound;
    private int mDealHitSoundHitType;
    private int mTakeHitSoundHitType;

    private GameObjectFactory.GameObjectType mSpawnOnDealHitObjectType;
    private int mSpawnOnDealHitHitType;
    private boolean mAlignDealHitObjectToVictimX;
    private boolean mAlignDealHitObjectToVictimY;
    
    
    public HitReactionComponent() {
        super();
        reset();
        setPhase(ComponentPhases.PRE_DRAW.ordinal());
    }
    
    @Override
    public void reset() {
        mPauseOnAttack = false;
        mPauseOnAttackTime = ATTACK_PAUSE_DELAY;
        mBounceOnHit = false;
        mBounceMagnitude = DEFAULT_BOUNCE_MAGNITUDE;
        mInvincibleAfterHitTime = 0.0f;
        mInvincible = false;
        mDieOnCollect = false;
        mDieOnAttack = false;
        mPossessionComponent = null;
        mInventoryUpdate = null;
        mLauncherComponent = null;
        mLauncherHitType = HitType.LAUNCH;
        mInvincibleTime = 0.0f;
        mGameEventOnHit = -1;
        mGameEventIndexData = 0;
        mLastGameEventTime = -1.0f;
        mGameEventHitType = CollisionParameters.HitType.INVALID;
        mForceInvincibility = false;
        mTakeHitSound = null;
        mDealHitSound = null;
        mSpawnOnDealHitObjectType = GameObjectType.INVALID;
        mSpawnOnDealHitHitType = CollisionParameters.HitType.INVALID;
        mDealHitSoundHitType = CollisionParameters.HitType.INVALID;
        mAlignDealHitObjectToVictimX = false;
        mAlignDealHitObjectToVictimY = false;
    }
    
    /** Called when this object attacks another object. */
    public void hitVictim(GameObject parent, GameObject victim, int hitType, 
            boolean hitAccepted) {
        if (hitAccepted) {
            if (mPauseOnAttack && hitType == CollisionParameters.HitType.HIT) {
                TimeSystem time = sSystemRegistry.timeSystem;
                time.freeze(mPauseOnAttackTime);
            }
            
            if (mDieOnAttack) {
                parent.life = 0;
            }
            
            if (hitType == mLauncherHitType && mLauncherComponent != null) {
                mLauncherComponent.prepareToLaunch(victim, parent);
            }
            
            if (mDealHitSound != null && 
            		(hitType == mDealHitSoundHitType || 
            				mDealHitSoundHitType == CollisionParameters.HitType.INVALID)) {
                SoundSystem sound = sSystemRegistry.soundSystem;
                if (sound != null) {
                    sound.play(mDealHitSound, false, SoundSystem.PRIORITY_NORMAL);
                }
            }
            
            if (mSpawnOnDealHitObjectType != GameObjectType.INVALID && 
                    hitType == mSpawnOnDealHitHitType) {
                final float x = mAlignDealHitObjectToVictimX ? 
                        victim.getPosition().x : parent.getPosition().x;
                final float y = mAlignDealHitObjectToVictimY ? 
                        victim.getPosition().y : parent.getPosition().y;     
                
                GameObjectFactory factory = sSystemRegistry.gameObjectFactory;
                GameObjectManager manager = sSystemRegistry.gameObjectManager;
 
                if (factory != null) {
                    GameObject object = factory.spawn(mSpawnOnDealHitObjectType, x, 
                            y, parent.facingDirection.x < 0.0f);
    
                    if (object != null && manager != null) {
                        manager.add(object);
                    }
                }
            }
        }
    }
    
    /** Called when this object is hit by another object. */
    public boolean receivedHit(GameObject parent, GameObject attacker, int hitType) {
        final TimeSystem time = sSystemRegistry.timeSystem;
        final float gameTime = time.getGameTime();
         
        if (mGameEventHitType == hitType && 
                mGameEventHitType != CollisionParameters.HitType.INVALID ) {
        	if (mLastGameEventTime < 0.0f || gameTime > mLastGameEventTime + EVENT_SEND_DELAY) {
	            LevelSystem level = sSystemRegistry.levelSystem;
	            level.sendGameEvent(mGameEventOnHit, mGameEventIndexData, true);
	        } else {
	        	// special case.  If we're waiting for a hit type to spawn an event and
	        	// another event has just happened, eat this hit so we don't miss
	        	// the chance to send the event.
	        	hitType = CollisionParameters.HitType.INVALID;
	        }
        	mLastGameEventTime = gameTime;
        }
        
        switch(hitType) {
            case CollisionParameters.HitType.INVALID:
                break;
            
            case CollisionParameters.HitType.HIT:
                // don't hit our friends, if we have friends.
                final boolean sameTeam = (parent.team == attacker.team && parent.team != Team.NONE);
                if (!mForceInvincibility && !mInvincible && parent.life > 0 && !sameTeam) {
                    parent.life -= 1;

                    if (mBounceOnHit && parent.life > 0) {
                        VectorPool pool = sSystemRegistry.vectorPool;
                        Vector2 newVelocity = pool.allocate(parent.getPosition());
                        newVelocity.subtract(attacker.getPosition());
                        newVelocity.set(0.5f * Utils.sign(newVelocity.x), 
                                0.5f * Utils.sign(newVelocity.y));
                        newVelocity.multiply(mBounceMagnitude);
                        parent.setVelocity(newVelocity);
                        parent.getTargetVelocity().zero();
                        pool.release(newVelocity);
                    }

                    if (mInvincibleAfterHitTime > 0.0f) {
                        mInvincible = true;
                        mInvincibleTime = mInvincibleAfterHitTime;
                    }
                    
                } else {
                    // Ignore this hit.
                    hitType = CollisionParameters.HitType.INVALID;
                }
                break;
            case CollisionParameters.HitType.DEATH:
                // respect teams?
                parent.life = 0;
                break;
            case CollisionParameters.HitType.COLLECT:
                if (mInventoryUpdate != null && parent.life > 0) {
                    InventoryComponent attackerInventory = attacker.findByClass(InventoryComponent.class);
                    if (attackerInventory != null) {
                        attackerInventory.applyUpdate(mInventoryUpdate);
                    }
                }
                if (mDieOnCollect && parent.life > 0) {
                    parent.life = 0;
                } 
                break;
            case CollisionParameters.HitType.POSSESS:
                if (mPossessionComponent != null && parent.life > 0 && attacker.life > 0) {
                    mPossessionComponent.activate(parent);
                } else {
                    hitType = CollisionParameters.HitType.INVALID;
                }
                break;
            case CollisionParameters.HitType.LAUNCH:   
                break;
                
            default:
                break;
        }
        
        
        if (hitType != CollisionParameters.HitType.INVALID) {
            if (mTakeHitSound != null && hitType == mTakeHitSoundHitType) {
                SoundSystem sound = sSystemRegistry.soundSystem;
                if (sound != null) {
                    sound.play(mTakeHitSound, false, SoundSystem.PRIORITY_NORMAL);
                }
            }
            mLastHitTime = gameTime;
            parent.setCurrentAction(ActionType.HIT_REACT);
            parent.lastReceivedHitType = hitType;
            
        }
        
        return hitType != CollisionParameters.HitType.INVALID;
    }
    
    @Override
    public void update(float timeDelta, BaseObject parent) {
        GameObject parentObject = (GameObject)parent;
        TimeSystem time = sSystemRegistry.timeSystem;
        
        final float gameTime = time.getGameTime();
       
        if (mInvincible && mInvincibleTime > 0) {
            if (time.getGameTime() > mLastHitTime + mInvincibleTime) {
                mInvincible = false;
            }
        }
        
        // This means that the lastReceivedHitType will persist for two frames, giving all systems
        // a chance to react.
        if (gameTime - mLastHitTime > timeDelta) {
            parentObject.lastReceivedHitType = CollisionParameters.HitType.INVALID;
        }
    }
    
    public void setPauseOnAttack(boolean pause) {
        mPauseOnAttack = pause;
    }
    
    public void setPauseOnAttackTime(float seconds) {
        mPauseOnAttackTime = seconds;
    }
    
    public void setBounceOnHit(boolean bounce) {
        mBounceOnHit = bounce;
    }
    
    public void setBounceMagnitude(float magnitude) {
        mBounceMagnitude = magnitude;
    }
    
    public void setInvincibleTime(float time) {
        mInvincibleAfterHitTime = time;
    }
    
    public void setDieWhenCollected(boolean die) {
        mDieOnCollect = true;
    }
    
    public void setDieOnAttack(boolean die) {
        mDieOnAttack = die;
    }
    
    public void setInvincible(boolean invincible) {
        mInvincible = invincible;
    }
    
    public void setPossessionComponent(ChangeComponentsComponent component) {
        mPossessionComponent = component;
    }
    
    public void setInventoryUpdate(InventoryComponent.UpdateRecord update) {
        mInventoryUpdate = update;
    }
    
    public void setLauncherComponent(LauncherComponent component, int launchHitType) {
        mLauncherComponent = component;
        mLauncherHitType = launchHitType;
    }
    
    public void setSpawnGameEventOnHit(int hitType, int gameFlowEventType, int indexData) {
        mGameEventHitType = hitType;
        mGameEventOnHit = gameFlowEventType;
        mGameEventIndexData = indexData;
        if (hitType == HitType.INVALID) {
        	// The game event has been cleared, so reset the timer blocking a
        	// subsequent event.
        	mLastGameEventTime = -1.0f;
        }
    }

    public final void setForceInvincible(boolean force) {
        mForceInvincibility = force;
    }
    
    public final void setTakeHitSound(int hitType, SoundSystem.Sound sound) {
    	mTakeHitSoundHitType = hitType;
        mTakeHitSound = sound;
    }
    
    public final void setDealHitSound(int hitType, SoundSystem.Sound sound) {
        mDealHitSound = sound;
        mDealHitSoundHitType = hitType;
    }
    
    public final void setSpawnOnDealHit(int hitType, GameObjectType objectType, boolean alignToVictimX,
            boolean alignToVicitmY) {
        mSpawnOnDealHitObjectType = objectType;
        mSpawnOnDealHitHitType = hitType;
        mAlignDealHitObjectToVictimX = alignToVictimX;
        mAlignDealHitObjectToVictimY = alignToVicitmY;
    }
    
}
