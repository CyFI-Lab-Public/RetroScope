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
 * Maintains a canonical time step, in seconds, for the entire game engine.  This time step
 * represents real changes in time but is only updated once per frame.
 */
// TODO: time distortion effects could go here, or they could go into a special object manager.
public class TimeSystem extends BaseObject {
    private float mGameTime;
    private float mRealTime;
    private float mFreezeDelay;
    private float mGameFrameDelta;
    private float mRealFrameDelta;
    
    private float mTargetScale;
    private float mScaleDuration;
    private float mScaleStartTime;
    private boolean mEaseScale;
    
    private static final float EASE_DURATION = 0.5f;
    
    public TimeSystem() {
        super();
        reset();
    }
    
    @Override
    public void reset() {
        mGameTime = 0.0f; 
        mRealTime = 0.0f;
        mFreezeDelay = 0.0f;
        mGameFrameDelta = 0.0f;
        mRealFrameDelta = 0.0f;
        
        mTargetScale = 1.0f;
        mScaleDuration = 0.0f;
        mScaleStartTime = 0.0f;
        mEaseScale = false;
    }

    @Override
    public void update(float timeDelta, BaseObject parent) {
    	mRealTime += timeDelta;
    	mRealFrameDelta = timeDelta;
    	
        if (mFreezeDelay > 0.0f) {
            mFreezeDelay -= timeDelta;
            mGameFrameDelta = 0.0f;
        } else {
        	float scale = 1.0f;
        	if (mScaleStartTime > 0.0f) {
        		final float scaleTime = mRealTime - mScaleStartTime;
        		if (scaleTime > mScaleDuration) {
        			mScaleStartTime = 0;
        		} else {
        			if (mEaseScale) {
        				if (scaleTime <= EASE_DURATION) {
        					// ease in
        					scale = Lerp.ease(1.0f, mTargetScale, EASE_DURATION, scaleTime);
        				} else if (mScaleDuration - scaleTime < EASE_DURATION) {
        					// ease out
        					final float easeOutTime = EASE_DURATION - (mScaleDuration - scaleTime);
        					scale = Lerp.ease(mTargetScale, 1.0f, EASE_DURATION, easeOutTime);
        				} else {
        					scale = mTargetScale;
        				}
        			} else {
        				scale = mTargetScale;
        			}
        		}
            }
        	 
            mGameTime += (timeDelta * scale);
            mGameFrameDelta = (timeDelta * scale);
        }
        
       
    }

    public float getGameTime() {
        return mGameTime;
    }
    
    public float getRealTime() {
        return mRealTime;
    }
    
    public float getFrameDelta() {
        return mGameFrameDelta;
    }
    
    public float getRealTimeFrameDelta() {
        return mRealFrameDelta;
    }
    
    public void freeze(float seconds) {
        mFreezeDelay = seconds;
    }
    
    public void appyScale(float scaleFactor, float duration, boolean ease) {
    	mTargetScale = scaleFactor;
    	mScaleDuration = duration;
    	mEaseScale = ease;
    	if (mScaleStartTime <= 0.0f) {
    		mScaleStartTime = mRealTime;
    	}
    }

}
