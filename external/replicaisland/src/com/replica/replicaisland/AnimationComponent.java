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
import com.replica.replicaisland.SoundSystem.Sound;

/**
 * Player Animation game object component.  Responsible for selecting an animation to describe the 
 * player's current state.  Requires the object to contain a SpriteComponent to play animations.
 */
public class AnimationComponent extends GameComponent {
    
    public enum PlayerAnimations {
        IDLE,
        MOVE,
        MOVE_FAST,
        BOOST_UP,
        BOOST_MOVE,
        BOOST_MOVE_FAST,
        STOMP,
        HIT_REACT,
        DEATH,
        FROZEN
    }
    
    private static final float MIN_ROCKET_TIME = 0.0f;
    private static final float FLICKER_INTERVAL = 0.15f;
    private static final float FLICKER_DURATION = 3.0f;
    private static final float LAND_THUMP_DELAY = 0.5f;
    
    private SpriteComponent mSprite;
    private SpriteComponent mJetSprite;
    private SpriteComponent mSparksSprite;
    
    private PlayerComponent mPlayer;
    private float mLastFlickerTime;
    private boolean mFlickerOn;
    private float mFlickerTimeRemaining;
    
    private GameObject.ActionType mPreviousAction;
    
    private float mLastRocketsOnTime;
    private boolean mExplodingDeath;
    
    private ChangeComponentsComponent mDamageSwap;
    private Sound mLandThump;
    private Sound mRocketSound;
    private Sound mExplosionSound;
    private float mLandThumpDelay;
    private int mRocketSoundStream;
    private boolean mRocketSoundPaused;
    
    private int mLastRubyCount;
    private Sound mRubySound1;
    private Sound mRubySound2;
    private Sound mRubySound3;
    private InventoryComponent mInventory;
    

    public AnimationComponent() {
        super();
        reset();
        setPhase(ComponentPhases.ANIMATION.ordinal());
    }
    
    @Override
    public void reset() {
        mPreviousAction = ActionType.INVALID;
        mSprite = null;
        mJetSprite = null;
        mSparksSprite = null;
        mPlayer = null;
        mLastFlickerTime = 0.0f;
        mFlickerOn = false;
        mFlickerTimeRemaining = 0.0f;
        mLastRocketsOnTime = 0.0f;
        mExplodingDeath = false;
        mDamageSwap = null;
        mLandThump = null;
        mLandThumpDelay = 0.0f;
        mRocketSound = null;
        mRocketSoundStream = -1;
        mLastRubyCount = 0;
        mInventory = null;
        mExplosionSound = null;
    }

    @Override
    public void update(float timeDelta, BaseObject parent) {
        if (mSprite != null) {

            GameObject parentObject = (GameObject) parent;
            
            final float velocityX = parentObject.getVelocity().x;
            final float velocityY = parentObject.getVelocity().y;
            

            GameObject.ActionType currentAction = parentObject.getCurrentAction();
            
            if (mJetSprite != null) {
                mJetSprite.setVisible(false);
            }
            
            if (mSparksSprite != null) {
                mSparksSprite.setVisible(false);
            }
            
            
            final TimeSystem time = sSystemRegistry.timeSystem;
            final float gameTime = time.getGameTime();
            
            if (currentAction != ActionType.HIT_REACT && mPreviousAction == ActionType.HIT_REACT) {
                mFlickerTimeRemaining = FLICKER_DURATION;
            }
            
            
            final boolean touchingGround = parentObject.touchingGround();
           
            boolean boosting = mPlayer != null ? mPlayer.getRocketsOn() : false;
            
            boolean visible = true;
            
            SoundSystem sound = sSystemRegistry.soundSystem;
            
            // It's usually not necessary to test to see if sound is enabled or not (when it's disabled,
            // play() is just a nop), but in this case I have a stream that is maintained for the rocket
            // sounds.  So it's simpler to just avoid that code if sound is off.
            if (sound.getSoundEnabled()) {
	            if (boosting) {
	                mLastRocketsOnTime = gameTime;
	            } else {
	                if (gameTime - mLastRocketsOnTime < MIN_ROCKET_TIME 
	                        && velocityY >= 0.0f) {
	                    boosting = true;
	                }
	            }
	            
	            if (mRocketSound != null) {
		            if (boosting) {
		            	if (mRocketSoundStream == -1) {
		            		mRocketSoundStream = sound.play(mRocketSound, true, SoundSystem.PRIORITY_HIGH);
		            		mRocketSoundPaused = false;
		            	} else if (mRocketSoundPaused) {
		            		sound.resume(mRocketSoundStream);
		            		mRocketSoundPaused = false;
		            	}
		            } else {
		            	sound.pause(mRocketSoundStream);
		            	mRocketSoundPaused = true;
		            }
	            }
            }
            
            // Normally, for collectables like the coin, we could just tell the object to play
            // a sound when it is collected.  The gems are a special case, though, as we
            // want to pick a different sound depending on how many have been collected.
            if (mInventory != null && mRubySound1 != null && mRubySound2 != null && mRubySound3 != null) {
            	InventoryComponent.UpdateRecord inventory = mInventory.getRecord();
            	final int rubyCount = inventory.rubyCount;
            	if (rubyCount != mLastRubyCount) {
            		mLastRubyCount = rubyCount;
            		switch (rubyCount) {
            		case 1:
            			sound.play(mRubySound1, false, SoundSystem.PRIORITY_NORMAL);
            			break;
            		case 2:
            			sound.play(mRubySound2, false, SoundSystem.PRIORITY_NORMAL);
            			break;
            		case 3:
            			sound.play(mRubySound3, false, SoundSystem.PRIORITY_NORMAL);
            			break;
            		}
            		
            	}
            }
            
            // Turn on visual effects (smoke, etc) when the player's life reaches 1.
            if (mDamageSwap != null) {
                if (parentObject.life == 1 && !mDamageSwap.getCurrentlySwapped()) {
                    mDamageSwap.activate(parentObject);
                } else if (parentObject.life != 1 && mDamageSwap.getCurrentlySwapped()) {
                    mDamageSwap.activate(parentObject);
                }
            }
            
            float opacity = 1.0f;
            
            if (currentAction == ActionType.MOVE) {
                InputGameInterface input = sSystemRegistry.inputGameInterface;
                final InputXY dpad = input.getDirectionalPad();
                if (dpad.getX() < 0.0f) {
                    parentObject.facingDirection.x = -1.0f; 
                } else if (dpad.getX() > 0.0f) {
                    parentObject.facingDirection.x = 1.0f;
                }
                
                // TODO: get rid of these magic numbers!
                if (touchingGround) {
                    
                    if (Utils.close(velocityX, 0.0f, 30.0f)) {
                        mSprite.playAnimation(PlayerAnimations.IDLE.ordinal());
                    } else if (Math.abs(velocityX) > 300.0f) {
                        mSprite.playAnimation(PlayerAnimations.MOVE_FAST.ordinal());
                    } else {
                        mSprite.playAnimation(PlayerAnimations.MOVE.ordinal());
                    }  
                    
                    final InputButton attackButton = input.getAttackButton();
                    
                    if (attackButton.getPressed()) {
                        // charge
                        final float pressedTime = gameTime - attackButton.getLastPressedTime();
                        final float wave = (float)Math.cos(pressedTime * (float)Math.PI * 2.0f);
                        opacity = (wave * 0.25f) + 0.75f;
                    }
                    
                } else {
                    if (boosting) {
                        if (mJetSprite != null) {
                            mJetSprite.setVisible(true);
                        }
                        
                        if (Math.abs(velocityX) < 100.0f && velocityY > 10.0f) {
                            mSprite.playAnimation(PlayerAnimations.BOOST_UP.ordinal());
                        } else if (Math.abs(velocityX) > 300.0f) {
                            mSprite.playAnimation(PlayerAnimations.BOOST_MOVE_FAST.ordinal());
                        } else {
                            mSprite.playAnimation(PlayerAnimations.BOOST_MOVE.ordinal());
                        }
                    } else {
                        
                        if (Utils.close(velocityX, 0.0f, 1.0f)) {
                            mSprite.playAnimation(PlayerAnimations.IDLE.ordinal());
                        } else if (Math.abs(velocityX) > 300.0f) {
                            mSprite.playAnimation(PlayerAnimations.MOVE_FAST.ordinal());
                        } else {
                            mSprite.playAnimation(PlayerAnimations.MOVE.ordinal());
                        } 
                    }
                    
                }
            } else if (currentAction == ActionType.ATTACK) {
                mSprite.playAnimation(PlayerAnimations.STOMP.ordinal()); 
                if (touchingGround && gameTime > mLandThumpDelay) {
                    if (mLandThump != null && sound != null) {
                        // modulate the sound slightly to avoid sounding too similar
                        sound.play(mLandThump, false, SoundSystem.PRIORITY_HIGH, 1.0f, 
                                (float)(Math.random() * 0.5f) + 0.75f); 
                        mLandThumpDelay = gameTime + LAND_THUMP_DELAY;
                    }
                }
            } else if (currentAction == ActionType.HIT_REACT) {
                mSprite.playAnimation(PlayerAnimations.HIT_REACT.ordinal());
              
                if (velocityX > 0.0f) {
                    parentObject.facingDirection.x = -1.0f;  
                } else if (velocityX < 0.0f) {
                    parentObject.facingDirection.x = 1.0f;
                }
                
                if (mSparksSprite != null) {
                    mSparksSprite.setVisible(true);
                }
            } else if (currentAction == ActionType.DEATH) {
                if (mPreviousAction != currentAction) {
                	if (mExplosionSound != null) {
                		sound.play(mExplosionSound, false, SoundSystem.PRIORITY_NORMAL);
                	}
                	// by default, explode when hit with the DEATH hit type.
                    boolean explodingDeath = parentObject.lastReceivedHitType == HitType.DEATH;
                    // or if touching a death tile.
                    HotSpotSystem hotSpot = sSystemRegistry.hotSpotSystem;
                    if (hotSpot != null) {
                        // TODO: HACK!  Unify all this code.
                        if (hotSpot.getHotSpot(parentObject.getCenteredPositionX(), 
                                parentObject.getPosition().y + 10.0f) == HotSpotSystem.HotSpotType.DIE) {
                            explodingDeath = true;
                        }
                    }
                    if (explodingDeath) {
                        mExplodingDeath = true;
                        GameObjectFactory factory = sSystemRegistry.gameObjectFactory;
                        GameObjectManager manager = sSystemRegistry.gameObjectManager;
                        if (factory != null && manager != null) {
                            GameObject explosion = factory.spawnEffectExplosionGiant(parentObject.getPosition().x, parentObject.getPosition().y);
                            if (explosion != null) {
                                manager.add(explosion);
                            }
                        }
                    } else {
                        mSprite.playAnimation(PlayerAnimations.DEATH.ordinal()); 
                        mExplodingDeath = false;
                    }
                    
                    mFlickerTimeRemaining = 0.0f;
                    if (mSparksSprite != null) {
                        if (!mSprite.animationFinished()) {
                            mSparksSprite.setVisible(true);
                        }
                    }
                }
                if (mExplodingDeath) {
                    visible = false;
                }
            } else if (currentAction == ActionType.FROZEN) {
                mSprite.playAnimation(PlayerAnimations.FROZEN.ordinal());
            }
            
            if (mFlickerTimeRemaining > 0.0f) {
                mFlickerTimeRemaining -= timeDelta;
                if (gameTime > mLastFlickerTime + FLICKER_INTERVAL) {
                    mLastFlickerTime = gameTime;
                    mFlickerOn = !mFlickerOn;
                }
                mSprite.setVisible(mFlickerOn);
                if (mJetSprite != null && mJetSprite.getVisible()) {
                    mJetSprite.setVisible(mFlickerOn);
                }
            } else {
                mSprite.setVisible(visible);
                mSprite.setOpacity(opacity);
            }
            
            mPreviousAction = currentAction;
        }
    }

    public void setSprite(SpriteComponent sprite) {
        mSprite = sprite;
    }
    
    public void setJetSprite(SpriteComponent sprite) {
        mJetSprite = sprite;
    }
    
    public void setSparksSprite(SpriteComponent sprite) {
        mSparksSprite = sprite;
    }
    
    public void setPlayer(PlayerComponent player) {
        mPlayer = player;
    }

    public final void setDamageSwap(ChangeComponentsComponent damageSwap) {
        mDamageSwap = damageSwap;
    }

    public void setLandThump(Sound land) {
        mLandThump = land;
    }

	public void setRocketSound(Sound sound) {
		mRocketSound = sound;
	}
	
	public void setRubySounds(Sound one, Sound two, Sound three) {
		mRubySound1 = one;
		mRubySound2 = two;
		mRubySound3 = three;
	}
	
	public void setInventory(InventoryComponent inventory) {
		mInventory = inventory;
	}

	public void setExplosionSound(Sound sound) {
		mExplosionSound = sound;
	}
}
