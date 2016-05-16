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

import com.replica.replicaisland.SoundSystem.Sound;

public class GhostComponent extends GameComponent {
    private float mMovementSpeed;
    private float mJumpImpulse;
    private float mAcceleration;
    private boolean mUseOrientationSensor;
    private float mDelayOnRelease;
    private boolean mKillOnRelease;
    private GameObject.ActionType mTargetAction;
    private float mLifeTime;
    private boolean mChangeActionOnButton;
    private GameObject.ActionType mButtonPressedAction;
    private Sound mAmbientSound;
    private int mAmbientSoundStream;
    
    public GhostComponent() {
        super();
        setPhase(GameComponent.ComponentPhases.THINK.ordinal());
        reset();
    }
    
    @Override
    public void reset() {
        mMovementSpeed = 0.0f;
        mJumpImpulse = 0.0f;
        mAcceleration = 0.0f;
        mUseOrientationSensor = false;
        mDelayOnRelease = 0.0f;
        mKillOnRelease = false;
        mTargetAction = GameObject.ActionType.MOVE;
        mLifeTime = 0.0f;
        mChangeActionOnButton = false;
        mButtonPressedAction = GameObject.ActionType.INVALID;
        mAmbientSound = null;
        mAmbientSoundStream = -1;
    }
    
    @Override
    public void update(float timeDelta, BaseObject parent) {   
        GameObject parentObject = (GameObject) parent;
        boolean timeToRelease = false;
        final InputGameInterface input = sSystemRegistry.inputGameInterface;
        final CameraSystem camera = sSystemRegistry.cameraSystem;

        if (parentObject.life > 0) {
            
            if (mLifeTime > 0.0f) {
                mLifeTime -= timeDelta;
                if (mLifeTime <= 0.0f) {
                    timeToRelease = true;
                } else if (mLifeTime < 1.0f) {
                    // Do we have a sprite we can fade out?
                    SpriteComponent sprite = parentObject.findByClass(SpriteComponent.class);
                    if (sprite != null) {
                        sprite.setOpacity(mLifeTime);
                    }
                }
            }
            
            if (parentObject.getPosition().y < -parentObject.height) {
                // we fell off the bottom of the screen, die.
                parentObject.life = 0;
                timeToRelease = true;
            }
            
            parentObject.setCurrentAction(mTargetAction);
            if (camera != null) {
                camera.setTarget(parentObject);
            }
            
            if (input != null) {
                
                if (mUseOrientationSensor) {
                	final InputXY tilt = input.getTilt();
                    parentObject.getTargetVelocity().x = 
                    	tilt.getX() * mMovementSpeed;
                    
                    parentObject.getTargetVelocity().y = 
                    	tilt.getY() * mMovementSpeed;
                   
                    parentObject.getAcceleration().x = mAcceleration;
                    parentObject.getAcceleration().y = mAcceleration; 
                } else {
                	final InputXY dpad = input.getDirectionalPad();
                    parentObject.getTargetVelocity().x = 
                    	dpad.getX() * mMovementSpeed;
               
                    parentObject.getAcceleration().x = mAcceleration;
                }

                final InputButton jumpButton = input.getJumpButton();
                final TimeSystem time = sSystemRegistry.timeSystem;
                final float gameTime = time.getGameTime();
                
                if (jumpButton.getTriggered(gameTime) 
                        && parentObject.touchingGround()
                        && parentObject.getVelocity().y <= 0.0f
                        && !mChangeActionOnButton) {
                    parentObject.getImpulse().y += mJumpImpulse;
                } else if (mChangeActionOnButton && jumpButton.getPressed()) {
                	parentObject.setCurrentAction(mButtonPressedAction);
                }
                
                final InputButton attackButton = input.getAttackButton();

                if (attackButton.getTriggered(gameTime)) {
                    timeToRelease = true;
                }
            }
            
            if (!timeToRelease && mAmbientSound != null && mAmbientSoundStream == -1) {
            	SoundSystem sound = BaseObject.sSystemRegistry.soundSystem;
            	if (sound != null) {
            		mAmbientSoundStream = sound.play(mAmbientSound, true, SoundSystem.PRIORITY_NORMAL);
            	}
            }
        } 
        
        if (parentObject.life == 0) {
        	if (mAmbientSoundStream > -1) {
            	SoundSystem sound = BaseObject.sSystemRegistry.soundSystem;
            	if (sound != null) {
            		sound.stop(mAmbientSoundStream);
            		mAmbientSoundStream = -1;
            	}
            }
        }
        
        if (timeToRelease) {
            releaseControl(parentObject);
        }
        
    }
    
    public final void releaseControl(GameObject parentObject) {
        GameObjectManager manager = sSystemRegistry.gameObjectManager;
        GameObject player = null;
        if (manager != null) {
            player = manager.getPlayer();
        }
        
        final CameraSystem camera = sSystemRegistry.cameraSystem;
        if (camera != null) {
            camera.setTarget(null);
        }
        
        if (player != null) {
            
            if (mKillOnRelease) {
                parentObject.life = 0;
            } else {
                // See if there's a component swap we can run.
                ChangeComponentsComponent swap = parentObject.findByClass(ChangeComponentsComponent.class);
                if (swap != null) {
                    swap.activate(parentObject);
                }
            }
            
            PlayerComponent control = player.findByClass(PlayerComponent.class);
            if (camera.pointVisible(player.getPosition(), player.width)) {
                control.deactivateGhost(0.0f);
            } else {
                control.deactivateGhost(mDelayOnRelease);
            }
           /* final InputSystem input = sSystemRegistry.inputSystem;
            if (input != null) {
                input.clearClickTriggered();
            }*/
        }
        
        if (mAmbientSoundStream > -1) {
        	SoundSystem sound = BaseObject.sSystemRegistry.soundSystem;
        	if (sound != null) {
        		sound.stop(mAmbientSoundStream);
        		mAmbientSoundStream = -1;
        	}
        }
    }

    public final void setMovementSpeed(float movementSpeed) {
        mMovementSpeed = movementSpeed;
    }

    public final void setJumpImpulse(float jumpImpulse) {
        mJumpImpulse = jumpImpulse;
    }

    public final void setAcceleration(float accceleration) {
        mAcceleration = accceleration;
    }
    
    public final void setUseOrientationSensor(boolean useSensor) {
        mUseOrientationSensor = useSensor;
    }

    public final void setDelayOnRelease(float delayOnRelease) {
        mDelayOnRelease = delayOnRelease;
    }

    public final void setKillOnRelease(boolean killOnRelease) {
        mKillOnRelease = killOnRelease;
    }
    
    public final void setTargetAction(GameObject.ActionType action) {
        mTargetAction = action;
    }
    
    public final void setLifeTime(float lifeTime) {
        mLifeTime = lifeTime;
    }
    
    public final void changeActionOnButton(GameObject.ActionType pressedAction) {
        mButtonPressedAction = pressedAction;
        mChangeActionOnButton = true;
    }
    
    public final void setAmbientSound(Sound sound) {
    	mAmbientSound = sound;
    }
}
