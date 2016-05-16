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
 * A component that allows an object to spawn other objects and apply velocity to them at 
 * specific intervals.  Can be used to launch projectiles, particle effects, or any other type
 * of game object.
 */
public class LaunchProjectileComponent extends GameComponent {
    private GameObjectFactory.GameObjectType mObjectTypeToSpawn;
    private float mOffsetX;
    private float mOffsetY;
    private float mVelocityX;
    private float mVelocityY;
    private float mThetaError;
    private GameObject.ActionType mRequiredAction;
    private float mDelayBetweenShots;
    private int mProjectilesInSet;
    private float mDelayBetweenSets;
    private int mSetsPerActivation;
    private float mDelayBeforeFirstSet;
    
    private float mLastProjectileTime;
    private float mSetStartedTime;
    private int mLaunchedCount;
    private int mSetCount;
    
    private boolean mTrackProjectiles;
    private int mMaxTrackedProjectiles;
    private int mTrackedProjectileCount;
    
    private Vector2 mWorkingVector;
    
    private SoundSystem.Sound mShootSound;
    
    
    public LaunchProjectileComponent() {
        super();
        setPhase(ComponentPhases.POST_COLLISION.ordinal());
        mWorkingVector = new Vector2();
        reset();
    }
    
    @Override
    public void reset() {
        mRequiredAction = ActionType.INVALID;
        mObjectTypeToSpawn = GameObjectFactory.GameObjectType.INVALID;
        mOffsetX = 0.0f;
        mOffsetY = 0.0f;
        mVelocityX = 0.0f;
        mVelocityY = 0.0f;
        mDelayBetweenShots = 0.0f;
        mProjectilesInSet = 0;
        mDelayBetweenSets = 0.0f;
        mLastProjectileTime = 0.0f;
        mSetStartedTime = -1.0f;
        mLaunchedCount = 0;
        mSetCount = 0;
        mSetsPerActivation = -1;
        mProjectilesInSet = 0;
        mDelayBeforeFirstSet = 0.0f;
        mTrackProjectiles = false;
        mMaxTrackedProjectiles = 0;
        mTrackedProjectileCount = 0;
        mThetaError = 0.0f;
        mShootSound = null;
    }
    
    @Override
    public void update(float timeDelta, BaseObject parent) {   
        GameObject parentObject = (GameObject) parent;
        
        final TimeSystem time = sSystemRegistry.timeSystem;
        final float gameTime = time.getGameTime();
        
        if (mTrackedProjectileCount < mMaxTrackedProjectiles || !mTrackProjectiles) {
            if (parentObject.getCurrentAction() == mRequiredAction 
                    || mRequiredAction == ActionType.INVALID) {
                
                if (mSetStartedTime == -1.0f) {
                    mLaunchedCount = 0;
                    mLastProjectileTime = 0.0f;
                    mSetStartedTime = gameTime;
                }
    
                final float setDelay = mSetCount > 0 ? mDelayBetweenSets : mDelayBeforeFirstSet;
                
                if (gameTime - mSetStartedTime >= setDelay && 
                        (mSetCount < mSetsPerActivation || mSetsPerActivation == -1)) {
                    // We can start shooting.
                    final float timeSinceLastShot = gameTime - mLastProjectileTime;
                    
                    if (timeSinceLastShot >= mDelayBetweenShots) {
                   
                        launch(parentObject);
                        mLastProjectileTime = gameTime;
                        
                        if (mLaunchedCount >= mProjectilesInSet && mProjectilesInSet > 0) {
                            mSetStartedTime = -1.0f;
                            mSetCount++;
                        }
                    }
                }
            } else {
                // Force the timer to start counting when the right action is activated.
                mSetStartedTime = -1.0f;
                mSetCount = 0;
            }
        }
    }
    
    private void launch(GameObject parentObject) {
        mLaunchedCount++;
        GameObjectFactory factory = sSystemRegistry.gameObjectFactory;
        GameObjectManager manager = sSystemRegistry.gameObjectManager;
        if (factory != null && manager != null) {
            float offsetX = mOffsetX;
            float offsetY = mOffsetY;
            boolean flip = false;
            if (parentObject.facingDirection.x < 0.0f) {
                offsetX = parentObject.width - mOffsetX;
                flip = true;
            }
                
            if (parentObject.facingDirection.y < 0.0f) {
                offsetY = parentObject.height - mOffsetY;
            }
            
            final float x = parentObject.getPosition().x + offsetX;
            final float y = parentObject.getPosition().y + offsetY;
            GameObject object = factory.spawn(mObjectTypeToSpawn, x, y, flip);
            if (object != null) {
	            mWorkingVector.set(1.0f, 1.0f);
	            if (mThetaError > 0.0f) {
	                final float angle = (float)(Math.random() * mThetaError * Math.PI * 2.0f);
	                mWorkingVector.x = (float)Math.sin(angle);
	                mWorkingVector.y = (float)Math.cos(angle);
	                if (Utils.close(mWorkingVector.length2(), 0.0f)) {
	                    mWorkingVector.set(1.0f, 1.0f);
	                }
	            }
	            mWorkingVector.x *= flip ? -mVelocityX : mVelocityX;
	            mWorkingVector.y *= mVelocityY;  
	            
	            object.getVelocity().set(mWorkingVector);
	            object.getTargetVelocity().set(mWorkingVector);
	            // Center the projectile on the spawn point.
	            object.getPosition().x -= object.width / 2.0f;
	            object.getPosition().y -= object.height / 2.0f;
	            
	            
	            if (mTrackProjectiles) {
	                object.commitUpdates();
	                LifetimeComponent projectileLife = object.findByClass(LifetimeComponent.class);
	                if (projectileLife != null) {
	                    projectileLife.setTrackingSpawner(this);
	                    mTrackedProjectileCount++;
	                }
	            }
	            manager.add(object);
	            
	            if (mShootSound != null) {
	            	SoundSystem sound = sSystemRegistry.soundSystem;
	            	if (sound != null) {
	            		sound.play(mShootSound, false, SoundSystem.PRIORITY_NORMAL);
	            	}
	            }
            }
        }
        
        
    }

    public final void setObjectTypeToSpawn(GameObjectFactory.GameObjectType objectTypeToSpawn) {
        mObjectTypeToSpawn = objectTypeToSpawn;
    }

    public final void setOffsetX(float offsetX) {
        mOffsetX = offsetX;
    }

    public final void setOffsetY(float offsetY) {
        mOffsetY = offsetY;
    }

    public final void setVelocityX(float velocityX) {
        mVelocityX = velocityX;
    }

    public final void setVelocityY(float velocityY) {
        mVelocityY = velocityY;
    }

    public final void setRequiredAction(GameObject.ActionType requiredAction) {
        mRequiredAction = requiredAction;
    }

    public final void setDelayBetweenShots(float launchDelay) {
        mDelayBetweenShots = launchDelay;
    }
    
    public final void setDelayBetweenSets(float delayBetweenSets) {
        mDelayBetweenSets = delayBetweenSets;
    }
    
    public final void setDelayBeforeFirstSet(float delayBeforeFirstSet) {
        mDelayBeforeFirstSet = delayBeforeFirstSet;
    }

    public final void setShotsPerSet(int shotCount) {
        mProjectilesInSet = shotCount;
    }
    
    public final void setSetsPerActivation(int setCount) {
        mSetsPerActivation = setCount;
    }
    
    public final void enableProjectileTracking(int max) {
        mMaxTrackedProjectiles = max;
        mTrackProjectiles = true;
    }
    
    public final void disableProjectileTracking() {
        mMaxTrackedProjectiles = 0;
        mTrackProjectiles = false;
    }
    
    public final void trackedProjectileDestroyed() {
        assert mTrackProjectiles;
        if (mTrackedProjectileCount == mMaxTrackedProjectiles) {
            // Let's restart the set.
            mSetStartedTime = -1.0f;
            mSetCount = 0;
        }
        mTrackedProjectileCount--;
    }
    
    public final void setThetaError(float error) {
        mThetaError = error;
    }
    
    public final void setShootSound(SoundSystem.Sound shoot) {
    	mShootSound = shoot;
    }
    
}
