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
import com.replica.replicaisland.SoundSystem.Sound;

public class LauncherComponent extends GameComponent {
    private final static float DEFAULT_LAUNCH_DELAY = 2.0f;
    private final static float DEFAULT_LAUNCH_MAGNITUDE = 2000.0f;
    private final static float DEFAULT_POST_LAUNCH_DELAY = 1.0f;
    private GameObject mShot;
    private float mLaunchTime;
    private float mAngle;
    private float mLaunchDelay;
    private Vector2 mLaunchDirection;
    private float mLaunchMagnitude;
    private float mPostLaunchDelay;
    private boolean mDriveActions;
    private GameObjectFactory.GameObjectType mLaunchEffect;
    private float mLaunchEffectOffsetX;
    private float mLaunchEffectOffsetY;
    private Sound mLaunchSound;
    
    public LauncherComponent() {
        super();
        mLaunchDirection = new Vector2();
        reset();
        setPhase(ComponentPhases.THINK.ordinal());
    }
    
    @Override
    public void reset() {
        mShot = null;
        mLaunchTime = 0.0f;
        mAngle = 0.0f;
        mLaunchDelay = DEFAULT_LAUNCH_DELAY;
        mLaunchMagnitude = DEFAULT_LAUNCH_MAGNITUDE;
        mPostLaunchDelay = DEFAULT_POST_LAUNCH_DELAY;
        mDriveActions = true;
        mLaunchEffect = GameObjectFactory.GameObjectType.INVALID;
        mLaunchEffectOffsetX = 0.0f;
        mLaunchEffectOffsetY = 0.0f;
        mLaunchSound = null;
    }
    
    @Override
    public void update(float timeDelta, BaseObject parent) {
        final TimeSystem time = sSystemRegistry.timeSystem;
        final float gameTime = time.getGameTime();
        GameObject parentObject = (GameObject)parent;
        
        if (mShot != null) {
            if (mShot.life <= 0) {
                // Looks like the shot is dead.  Let's forget about it.
                // TODO: this is unreliable.  We should have a "notify on death" event or something.
                mShot = null;
            } else {
                if (gameTime > mLaunchTime) {
                    fire(mShot, parentObject, mAngle);
                    mShot = null;
                    if (mDriveActions) {
                    	parentObject.setCurrentAction(ActionType.ATTACK);
                    }
                } else {
                    mShot.setPosition(parentObject.getPosition());
                }
            }
        } else if (gameTime > mLaunchTime + mPostLaunchDelay) {
        	if (mDriveActions) {
        		parentObject.setCurrentAction(ActionType.IDLE);
        	}
        }
    } 
    
    public void prepareToLaunch(GameObject object, GameObject parentObject) {
        if (mShot != object) {
            if (mShot != null) {
                // We already have a shot loaded and we are asked to shoot something else.
                // Shoot the current shot off and then load the new one.
                fire(mShot, parentObject, mAngle);
            }
            final TimeSystem time = sSystemRegistry.timeSystem;
            final float gameTime = time.getGameTime();
            mShot = object;    
            mLaunchTime = gameTime + mLaunchDelay;
        }
    }
    
    private void fire(GameObject object, GameObject parentObject, float mAngle) {
        if (mDriveActions) {
        	object.setCurrentAction(ActionType.MOVE);
        }
        mLaunchDirection.set((float)Math.sin(mAngle), (float)Math.cos(mAngle));
        mLaunchDirection.multiply(parentObject.facingDirection);
        mLaunchDirection.multiply(mLaunchMagnitude);
        object.setVelocity(mLaunchDirection);
        
        if (mLaunchSound != null) {
        	SoundSystem sound = sSystemRegistry.soundSystem;
        	if (sound != null) {
        		sound.play(mLaunchSound, false, SoundSystem.PRIORITY_NORMAL);
        	}
        }
        
        if (mLaunchEffect != GameObjectFactory.GameObjectType.INVALID) {
        	GameObjectFactory factory = sSystemRegistry.gameObjectFactory;
        	GameObjectManager manager = sSystemRegistry.gameObjectManager;
        	if (factory != null && manager != null) {
        		final Vector2 position = parentObject.getPosition();
        		
        		GameObject effect = factory.spawn(mLaunchEffect, 
        				position.x + (mLaunchEffectOffsetX * parentObject.facingDirection.x),
        				position.y + (mLaunchEffectOffsetY * parentObject.facingDirection.y),
        				false);
        		if (effect != null) {
        			manager.add(effect);
        		}
        	}
        }
    }
    
    public void setup(float angle, float magnitude, float launchDelay, float postLaunchDelay, boolean driveActions) {
    	mAngle = angle;
    	mLaunchMagnitude = magnitude;
    	mLaunchDelay = launchDelay;
    	mPostLaunchDelay = postLaunchDelay;
    	mDriveActions = driveActions;
    }
    
    public void setLaunchEffect(GameObjectFactory.GameObjectType effectType, float offsetX, float offsetY) {
    	mLaunchEffect = effectType;
    	mLaunchEffectOffsetX = offsetX;
    	mLaunchEffectOffsetY = offsetY;
    }
    
    public void setLaunchSound(Sound sound) {
    	mLaunchSound = sound;
    }
   
}
