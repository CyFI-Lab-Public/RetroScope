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

import android.view.KeyEvent;

public class InputGameInterface extends BaseObject {
	private static final float ORIENTATION_DEAD_ZONE_MIN = 0.03f;
	private static final float ORIENTATION_DEAD_ZONE_MAX = 0.1f;
	private static final float ORIENTATION_DEAD_ZONE_SCALE = 0.75f;

	private final static float ROLL_TIMEOUT = 0.1f;
	private final static float ROLL_RESET_DELAY = 0.075f;
	
    // Raw trackball input is filtered by this value. Increasing it will 
    // make the control more twitchy, while decreasing it will make the control more precise.
    private final static float ROLL_FILTER = 0.4f;
    private final static float ROLL_DECAY = 8.0f;
	
    private final static float KEY_FILTER = 0.25f;
    private final static float SLIDER_FILTER = 0.25f;

    
	private InputButton mJumpButton = new InputButton();
	private InputButton mAttackButton = new InputButton();
	private InputXY mDirectionalPad = new InputXY();
	private InputXY mTilt = new InputXY();
		
	private int mLeftKeyCode = KeyEvent.KEYCODE_DPAD_LEFT;
	private int mRightKeyCode = KeyEvent.KEYCODE_DPAD_RIGHT;
	private int mJumpKeyCode = KeyEvent.KEYCODE_SPACE;
	private int mAttackKeyCode = KeyEvent.KEYCODE_SHIFT_LEFT;
		
	private float mOrientationDeadZoneMin = ORIENTATION_DEAD_ZONE_MIN;
	private float mOrientationDeadZoneMax = ORIENTATION_DEAD_ZONE_MAX;
	private float mOrientationDeadZoneScale = ORIENTATION_DEAD_ZONE_SCALE;
	private float mOrientationSensitivity = 1.0f;
	private float mOrientationSensitivityFactor = 1.0f;
	private float mMovementSensitivity = 1.0f;
	
	private boolean mUseClickButtonForAttack = true;
	private boolean mUseOrientationForMovement = false;
	private boolean mUseOnScreenControls = false;
	
	private float mLastRollTime;
	
	public InputGameInterface() {
		super();
		reset();
	}

	@Override
	public void reset() {
		mJumpButton.release();
		mAttackButton.release();
		mDirectionalPad.release();
		mTilt.release();
	}
	
	
	
	@Override
    public void update(float timeDelta, BaseObject parent) {
		InputSystem input = sSystemRegistry.inputSystem;
		final InputButton[] keys = input.getKeyboard().getKeys();
		final InputXY orientation = input.getOrientationSensor();
		
		// tilt is easy
		mTilt.clone(orientation);
		
		final InputTouchScreen touch = input.getTouchScreen();
		final float gameTime = sSystemRegistry.timeSystem.getGameTime();

		float sliderOffset = 0;
		
		// update movement inputs
		if (mUseOnScreenControls) {
			final InputXY sliderTouch = touch.findPointerInRegion(
					ButtonConstants.MOVEMENT_SLIDER_REGION_X, 
                    ButtonConstants.MOVEMENT_SLIDER_REGION_Y, 
                    ButtonConstants.MOVEMENT_SLIDER_REGION_WIDTH, 
                    ButtonConstants.MOVEMENT_SLIDER_REGION_HEIGHT);
			
			if (sliderTouch != null) {
				final float halfWidth = ButtonConstants.MOVEMENT_SLIDER_BAR_WIDTH / 2.0f;
				final float center = ButtonConstants.MOVEMENT_SLIDER_X + halfWidth;
				final float offset = sliderTouch.getX() - center;
				float magnitudeRamp = Math.abs(offset) > halfWidth ? 1.0f : (Math.abs(offset) / halfWidth);
				
				final float magnitude = magnitudeRamp * Utils.sign(offset) * SLIDER_FILTER * mMovementSensitivity;
				sliderOffset = magnitudeRamp * Utils.sign(offset);
				mDirectionalPad.press(gameTime, magnitude, 0.0f);
			} else {
				mDirectionalPad.release();
			}
		} else if (mUseOrientationForMovement) {
			mDirectionalPad.clone(orientation);
			mDirectionalPad.setMagnitude(
					filterOrientationForMovement(orientation.getX()), 
					filterOrientationForMovement(orientation.getY()));
		} else {
			// keys or trackball
			final InputXY trackball = input.getTrackball();
			final InputButton left = keys[mLeftKeyCode];
			final InputButton right = keys[mRightKeyCode];
			final float leftPressedTime = left.getLastPressedTime();
			final float rightPressedTime = right.getLastPressedTime();
			
			
			if (trackball.getLastPressedTime() > Math.max(leftPressedTime, rightPressedTime)) {
				// The trackball never goes "up", so force it to turn off if it wasn't triggered in the last frame.
				// What follows is a bunch of code to filter trackball events into something like a dpad event.
				// The goals here are:
				// 	- For roll events that occur in quick succession to accumulate.
				//	- For roll events that occur with more time between them, lessen the impact of older events
				//	- In the absence of roll events, fade the roll out over time.
				if (gameTime - trackball.getLastPressedTime() < ROLL_TIMEOUT) {
					float newX;
					float newY;
					final float delay = Math.max(ROLL_RESET_DELAY, timeDelta);
					if (gameTime - mLastRollTime <= delay) {
						newX = mDirectionalPad.getX() + (trackball.getX() * ROLL_FILTER * mMovementSensitivity);
						newY = mDirectionalPad.getY() + (trackball.getY() * ROLL_FILTER * mMovementSensitivity);
					} else {
						float oldX = mDirectionalPad.getX() != 0.0f ? mDirectionalPad.getX() / 2.0f : 0.0f;
						float oldY = mDirectionalPad.getX() != 0.0f ? mDirectionalPad.getX() / 2.0f : 0.0f;
						newX = oldX + (trackball.getX() * ROLL_FILTER * mMovementSensitivity);
						newY = oldY + (trackball.getX() * ROLL_FILTER * mMovementSensitivity);
					}
					
					mDirectionalPad.press(gameTime, newX, newY);
					mLastRollTime = gameTime;
					trackball.release();
				} else {
					float x = mDirectionalPad.getX();
					float y = mDirectionalPad.getY();
					if (x != 0.0f) {
						int sign = Utils.sign(x);
						x = x - (sign * ROLL_DECAY * timeDelta);
						if (Utils.sign(x) != sign) {
							x = 0.0f;
						}
					}
					
					if (y != 0.0f) {
						int sign = Utils.sign(y);
						y = y - (sign * ROLL_DECAY * timeDelta);
						if (Utils.sign(x) != sign) {
							y = 0.0f;
						}
					}
					
					
					if (x == 0 && y == 0) {
						mDirectionalPad.release();
					} else {
						mDirectionalPad.setMagnitude(x, y);
					}
				}
				
			} else {
				float xMagnitude = 0.0f;
				float yMagnitude = 0.0f;
				float pressTime = 0.0f;
				// left and right are mutually exclusive
				if (leftPressedTime > rightPressedTime) {
					xMagnitude = -left.getMagnitude() * KEY_FILTER * mMovementSensitivity;
					pressTime = leftPressedTime;
				} else {
					xMagnitude = right.getMagnitude() * KEY_FILTER * mMovementSensitivity;
					pressTime = rightPressedTime;
				}
				
				if (xMagnitude != 0.0f) {
					mDirectionalPad.press(pressTime, xMagnitude, yMagnitude);
				} else {
					mDirectionalPad.release();
				}
			}
		}
		
		// update other buttons
		final InputButton jumpKey = keys[mJumpKeyCode];
		
		// when on-screen movement controls are on, the fly and attack buttons are flipped.
		float flyButtonRegionX = ButtonConstants.FLY_BUTTON_REGION_X;
		float stompButtonRegionX = ButtonConstants.STOMP_BUTTON_REGION_X;

		if (mUseOnScreenControls) {
			ContextParameters params = sSystemRegistry.contextParameters;
			flyButtonRegionX = params.gameWidth - ButtonConstants.FLY_BUTTON_REGION_WIDTH - ButtonConstants.FLY_BUTTON_REGION_X;
			stompButtonRegionX = params.gameWidth - ButtonConstants.STOMP_BUTTON_REGION_WIDTH - ButtonConstants.STOMP_BUTTON_REGION_X;
		}
		
		final InputXY jumpTouch = touch.findPointerInRegion(
				flyButtonRegionX, 
                ButtonConstants.FLY_BUTTON_REGION_Y, 
                ButtonConstants.FLY_BUTTON_REGION_WIDTH, 
                ButtonConstants.FLY_BUTTON_REGION_HEIGHT);
		
		if (jumpKey.getPressed()) {
			mJumpButton.press(jumpKey.getLastPressedTime(), jumpKey.getMagnitude());
		} else if (jumpTouch != null) {
			if (!mJumpButton.getPressed()) {
				mJumpButton.press(jumpTouch.getLastPressedTime(), 1.0f);
			}
		} else {
			mJumpButton.release();
		}
		
		final InputButton attackKey = keys[mAttackKeyCode];
		final InputButton clickButton = keys[KeyEvent.KEYCODE_DPAD_CENTER]; // special case
		
		final InputXY stompTouch = touch.findPointerInRegion(
				stompButtonRegionX, 
                ButtonConstants.STOMP_BUTTON_REGION_Y, 
                ButtonConstants.STOMP_BUTTON_REGION_WIDTH, 
                ButtonConstants.STOMP_BUTTON_REGION_HEIGHT);
		
		if (mUseClickButtonForAttack && clickButton.getPressed()) {
			mAttackButton.press(clickButton.getLastPressedTime(), clickButton.getMagnitude());
		} else if (attackKey.getPressed()) {
			mAttackButton.press(attackKey.getLastPressedTime(), attackKey.getMagnitude());
		} else if (stompTouch != null) {
			// Since touch events come in constantly, we only want to press the attack button
			// here if it's not already down.  That makes it act like the other buttons (down once then up).
			if (!mAttackButton.getPressed()) {
				mAttackButton.press(stompTouch.getLastPressedTime(), 1.0f);
			}
		} else {
			mAttackButton.release();
		}

		// This doesn't seem like exactly the right place to write to the HUD, but on the other hand,
		// putting this code elsewhere causes dependencies between exact HUD content and physics, which
		// we sometimes wish to avoid.
		final HudSystem hud = sSystemRegistry.hudSystem;
        if (hud != null) {
            hud.setButtonState(mJumpButton.getPressed(), mAttackButton.getPressed(), mDirectionalPad.getPressed());
            hud.setMovementSliderOffset(sliderOffset);
        }
	}
	
	
	private float filterOrientationForMovement(float magnitude) {
		float scaledMagnitude = magnitude * mOrientationSensitivityFactor;
		
		return deadZoneFilter(scaledMagnitude, mOrientationDeadZoneMin, mOrientationDeadZoneMax, mOrientationDeadZoneScale);
	}
	
	private float deadZoneFilter(float magnitude, float min, float max, float scale) {
		float smoothedMagnatude = magnitude;
    	if (Math.abs(magnitude) < min) {
    		smoothedMagnatude = 0.0f;	// dead zone
    	} else if (Math.abs(magnitude) < max) {
    		smoothedMagnatude *= scale;
    	}
    	
    	return smoothedMagnatude;
	}
	
	
	public final InputXY getDirectionalPad() {
		return mDirectionalPad;
	}
	
	public final InputXY getTilt() {
		return mTilt;
	}
	
	public final InputButton getJumpButton() {
		return mJumpButton;
	}
	
	public final InputButton getAttackButton() {
		return mAttackButton;
	}
	
	public void setKeys(int left, int right, int jump, int attack) {
		mLeftKeyCode = left;
		mRightKeyCode = right;
		mJumpKeyCode = jump;
		mAttackKeyCode = attack;
	}
	
	public void setUseClickForAttack(boolean click) {
		mUseClickButtonForAttack = click;
	}
	
	public void setUseOrientationForMovement(boolean orientation) {
		mUseOrientationForMovement = orientation;
	}
	
	public void setOrientationMovementSensitivity(float sensitivity) {
		mOrientationSensitivity = sensitivity;
		mOrientationSensitivityFactor = 2.9f * sensitivity + 0.1f;
	}

	public void setMovementSensitivity(float sensitivity) {
		mMovementSensitivity  = sensitivity;
	}
	
	public void setUseOnScreenControls(boolean onscreen) {
		mUseOnScreenControls = onscreen;
	}
	
}
