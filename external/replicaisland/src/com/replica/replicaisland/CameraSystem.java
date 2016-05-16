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

/**
 * Manages the position of the camera based on a target game object.
 */
public class CameraSystem extends BaseObject {
    private GameObject mTarget;
    private float mShakeTime;
    private float mShakeMagnitude;
    private float mShakeOffsetY;
    private Vector2 mCurrentCameraPosition;
    private Vector2 mFocalPosition;
    private Vector2 mPreInterpolateCameraPosition;
    private Vector2 mTargetPosition;
    private Vector2 mBias;
    private float mTargetChangedTime;
    
    private static final float X_FOLLOW_DISTANCE = 0.0f;
    private static final float Y_UP_FOLLOW_DISTANCE = 90.0f; 
    private static final float Y_DOWN_FOLLOW_DISTANCE = 0.0f; 
    
    private static final float MAX_INTERPOLATE_TO_TARGET_DISTANCE = 300.0f;
    private static final float INTERPOLATE_TO_TARGET_TIME = 1.0f;
    
    private static int SHAKE_FREQUENCY = 40;
    
    private static float BIAS_SPEED = 400.0f;
    
    public CameraSystem() {
        super();
        mCurrentCameraPosition = new Vector2();
        mFocalPosition = new Vector2();
        mPreInterpolateCameraPosition = new Vector2();
        mTargetPosition = new Vector2();
        mBias = new Vector2();
    }
    
    @Override
    public void reset() {
        mTarget = null;
        mCurrentCameraPosition.zero();
        mShakeTime = 0.0f;
        mShakeMagnitude = 0.0f;
        mFocalPosition.zero();
        mTargetChangedTime = 0.0f;
        mPreInterpolateCameraPosition.zero();
        mTargetPosition.zero();
    }
    
    void setTarget(GameObject target) {
        if (target != null && mTarget != target) {
            mPreInterpolateCameraPosition.set(mCurrentCameraPosition);
            mPreInterpolateCameraPosition.subtract(target.getPosition());
            if (mPreInterpolateCameraPosition.length2() < 
                    MAX_INTERPOLATE_TO_TARGET_DISTANCE * MAX_INTERPOLATE_TO_TARGET_DISTANCE) {
                final TimeSystem time = sSystemRegistry.timeSystem;
                mTargetChangedTime = time.getGameTime();
                mPreInterpolateCameraPosition.set(mCurrentCameraPosition);
            } else {
            	mTargetChangedTime = 0.0f;
                mCurrentCameraPosition.set(target.getPosition());
            }
        }
        
        mTarget = target;
        
    }
    
    public GameObject getTarget() {
		return mTarget;
	}
    
    void shake(float duration, float magnitude) {
        mShakeTime = duration;
        mShakeMagnitude = magnitude;
    }
    
    public boolean shaking() {
        return mShakeTime > 0.0f;
    }

    @Override
    public void update(float timeDelta, BaseObject parent) {
        
        mShakeOffsetY = 0.0f;
        
        if (mShakeTime > 0.0f) {
            mShakeTime -= timeDelta;
            mShakeOffsetY = (float) (Math.sin(mShakeTime * SHAKE_FREQUENCY) * mShakeMagnitude);
        }
        
        if (mTarget != null) {
        	mTargetPosition.set(mTarget.getCenteredPositionX(), mTarget.getCenteredPositionY());
            final Vector2 targetPosition = mTargetPosition;
            
            if (mTargetChangedTime > 0.0f) {
                final TimeSystem time = sSystemRegistry.timeSystem;
                final float delta = time.getGameTime() - mTargetChangedTime;
                
                mCurrentCameraPosition.x = Lerp.ease(mPreInterpolateCameraPosition.x, 
                        targetPosition.x, INTERPOLATE_TO_TARGET_TIME, delta);
                
                mCurrentCameraPosition.y = Lerp.ease(mPreInterpolateCameraPosition.y, 
                        targetPosition.y, INTERPOLATE_TO_TARGET_TIME, delta);
                
                if (delta > INTERPOLATE_TO_TARGET_TIME) {
                    mTargetChangedTime = -1;
                }
            } else {
            	
            	// Only respect the bias if the target is moving.  No camera motion without 
            	// player input!
            	if (mBias.length2() > 0.0f && mTarget.getVelocity().length2() > 1.0f) {
                	mBias.normalize();
                	mBias.multiply(BIAS_SPEED * timeDelta);
                	mCurrentCameraPosition.add(mBias);
                }
            	
                final float xDelta = targetPosition.x - mCurrentCameraPosition.x;
                if (Math.abs(xDelta) > X_FOLLOW_DISTANCE) {
                    mCurrentCameraPosition.x = targetPosition.x - (X_FOLLOW_DISTANCE * Utils.sign(xDelta));
                }
                
               
                final float yDelta = targetPosition.y - mCurrentCameraPosition.y;
                if (yDelta > Y_UP_FOLLOW_DISTANCE) {
                    mCurrentCameraPosition.y = targetPosition.y - Y_UP_FOLLOW_DISTANCE;
                } else if (yDelta < -Y_DOWN_FOLLOW_DISTANCE) {
                    mCurrentCameraPosition.y = targetPosition.y + Y_DOWN_FOLLOW_DISTANCE;
                }

            }
            
        	mBias.zero();

        }

        mFocalPosition.x = (float) Math.floor(mCurrentCameraPosition.x);
        mFocalPosition.x = snapFocalPointToWorldBoundsX(mFocalPosition.x);
        
        mFocalPosition.y = (float) Math.floor(mCurrentCameraPosition.y + mShakeOffsetY);
        mFocalPosition.y = snapFocalPointToWorldBoundsY(mFocalPosition.y);
    }
    
    /** Returns the x position of the camera's look-at point. */
    public float getFocusPositionX() {
        return mFocalPosition.x;
    }
    
    /** Returns the y position of the camera's look-at point. */
    public float getFocusPositionY() {
        return mFocalPosition.y;
    }
    
    public boolean pointVisible(Vector2 point, float radius) {
        boolean visible = false;
        final float width = sSystemRegistry.contextParameters.gameWidth / 2.0f;
        final float height = sSystemRegistry.contextParameters.gameHeight / 2.0f;
        if (Math.abs(mFocalPosition.x - point.x) < (width + radius)) {
            if (Math.abs(mFocalPosition.y - point.y) < (height + radius)) {
                visible = true;
            }
        }
        return visible;
    }

    /** Snaps a coordinate against the bounds of the world so that it may not pass out
     * of the visible area of the world.
     * @param worldX An x-coordinate in world units.
     * @return An x-coordinate that is guaranteed not to expose the edges of the world.
     */
    public float snapFocalPointToWorldBoundsX(float worldX) {
        float focalPositionX = worldX;
        final float width = sSystemRegistry.contextParameters.gameWidth;
        final LevelSystem level = sSystemRegistry.levelSystem;
        if (level != null) {
            final float worldPixelWidth = Math.max(level.getLevelWidth(), width);
            final float rightEdge = focalPositionX + (width / 2.0f);
            final float leftEdge = focalPositionX - (width / 2.0f);
    
            if (rightEdge > worldPixelWidth) {
                focalPositionX = worldPixelWidth - (width / 2.0f);
            } else if (leftEdge < 0) {
                focalPositionX = width / 2.0f;
            }
        }
        return focalPositionX;
    }

    /** Snaps a coordinate against the bounds of the world so that it may not pass out
     * of the visible area of the world.
     * @param worldY A y-coordinate in world units.
     * @return A y-coordinate that is guaranteed not to expose the edges of the world.
     */
    public float snapFocalPointToWorldBoundsY(float worldY) {
        float focalPositionY = worldY;

        final float height = sSystemRegistry.contextParameters.gameHeight;
        final LevelSystem level = sSystemRegistry.levelSystem;
        if (level != null) {
            final float worldPixelHeight = Math.max(level.getLevelHeight(), sSystemRegistry.contextParameters.gameHeight);
            final float topEdge = focalPositionY + (height / 2.0f);
            final float bottomEdge = focalPositionY - (height / 2.0f);
    
            if (topEdge > worldPixelHeight) {
                focalPositionY = worldPixelHeight - (height / 2.0f);
            } else if (bottomEdge < 0) {
                focalPositionY = height / 2.0f;
            }
        }
        
        return focalPositionY;
    }

	public void addCameraBias(Vector2 bias) {
		final float x = bias.x - mFocalPosition.x;
		final float y = bias.y - mFocalPosition.y;
		final float biasX = mBias.x;
		final float biasY = mBias.y;
		mBias.set(x, y);
		mBias.normalize();
		mBias.add(biasX, biasY);
	}
    
    
}
