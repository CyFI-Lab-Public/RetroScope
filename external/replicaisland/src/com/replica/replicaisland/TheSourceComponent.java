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

public class TheSourceComponent extends GameComponent {
	public final static float SHAKE_TIME = 0.6f;
	private final static float DIE_TIME = 30.0f;
	private final static float EXPLOSION_TIME = 0.1f;
	private final static float SHAKE_MAGNITUDE = 5.0f;
	private final static float SHAKE_SCALE = 300.0f;
	private final static float CAMERA_HIT_SHAKE_MAGNITUDE = 3.0f;

	private final static float SINK_SPEED = -20.0f;
	private float mTimer;
	private float mExplosionTimer;
	private float mShakeStartPosition;
	private ChannelSystem.Channel mChannel;
	private int mGameEvent;
	private int mGameEventIndex;
	private boolean mDead;
	
	private static ChannelSystem.ChannelBooleanValue sChannelValue = new ChannelSystem.ChannelBooleanValue();
	
	public TheSourceComponent() {
		super();
        reset();
        setPhase(ComponentPhases.THINK.ordinal());
	}
	
	@Override
	public void reset() {
		mTimer = 0.0f;
		mExplosionTimer = 0.0f;
		mShakeStartPosition = 0.0f;
		mChannel = null;
		sChannelValue.value = false;
		mGameEvent = -1;
		mGameEventIndex = -1;
		mDead = false;
	}

	@Override
	public void update(float timeDelta, BaseObject parent) {
		GameObject parentObject = (GameObject)parent;
		GameObject.ActionType currentAction = parentObject.getCurrentAction();

		CameraSystem camera = sSystemRegistry.cameraSystem;

		if (currentAction == ActionType.HIT_REACT) {
			if (parentObject.life > 0) {
				mTimer = SHAKE_TIME;
				camera.shake(SHAKE_TIME, CAMERA_HIT_SHAKE_MAGNITUDE);
				mShakeStartPosition = parentObject.getPosition().x;
				parentObject.setCurrentAction(ActionType.IDLE); 
				currentAction = ActionType.IDLE;
			} else {
				parentObject.setCurrentAction(ActionType.DEATH);
				currentAction = ActionType.DEATH;
				mTimer = DIE_TIME;
				mExplosionTimer = EXPLOSION_TIME;
				if (mChannel != null) {
					mChannel.value = sChannelValue;
					sChannelValue.value = true;
				}
				mDead = true;
			}
			
		}
		
		mTimer -= timeDelta;

		if (mDead) {
			// Wait for the player to take the camera back, then steal it!
			GameObjectManager manager = sSystemRegistry.gameObjectManager;

			if (camera != null && manager != null && camera.getTarget() == manager.getPlayer()) {
				camera.setTarget(parentObject);
			}
			
			final float offset = SINK_SPEED * timeDelta;
			parentObject.getPosition().y += offset;
			
			mExplosionTimer -= timeDelta;
			if (mExplosionTimer < 0.0f) {
				GameObjectFactory factory = sSystemRegistry.gameObjectFactory;
				if (factory != null) {
					float x = ((float)Math.random() - 0.5f) * (parentObject.width * 0.75f);
					float y = ((float)Math.random() - 0.5f) * (parentObject.height * 0.75f);
					GameObject object =
						factory.spawn(GameObjectFactory.GameObjectType.EXPLOSION_GIANT, 
							parentObject.getCenteredPositionX() + x, 
							parentObject.getCenteredPositionY() + y, 
							false);
					if (object != null) {
						manager.add(object);
					}
					mExplosionTimer = EXPLOSION_TIME;
				}
			}
				
			if (mTimer - timeDelta <= 0.0f) {
				mTimer = 0.0f;
				if (mGameEvent != -1) {
					HudSystem hud = sSystemRegistry.hudSystem;
    	        	if (hud != null) {
    	        		hud.startFade(false, 1.5f);
    	        		hud.sendGameEventOnFadeComplete(mGameEvent, mGameEventIndex);
    	        		mGameEvent = -1;
    	        	}
	    		}
			}
		} else if (mTimer > 0) {
			// shake
			float delta = (float)Math.sin(mTimer * SHAKE_SCALE);
			delta *= SHAKE_MAGNITUDE;
			parentObject.getPosition().x = mShakeStartPosition + delta;
			if (mTimer - timeDelta <= 0.0f) {
				// end one step early and fix the position.
				mTimer = 0;
				parentObject.getPosition().x = mShakeStartPosition;
			}
		}
	}
	
	public void setChannel(ChannelSystem.Channel channel) {
		mChannel = channel;
	}

	public void setGameEvent(int event, int index) {
		mGameEvent = event;
		mGameEventIndex = index;
	}
}
