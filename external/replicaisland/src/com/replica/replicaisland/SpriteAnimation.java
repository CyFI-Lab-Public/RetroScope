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

import java.util.Arrays;

/**
 * Describes a single animation for a sprite.
 */
public class SpriteAnimation extends PhasedObject {
    private final static int LINEAR_SEARCH_CUTOFF = 16;
    
    private FixedSizeArray<AnimationFrame> mFrames;
    private float[] mFrameStartTimes;
    private boolean mLoop;
    private float mLength;
    
    public SpriteAnimation(int animationId, int frameCount) {
        super();
        mFrames = new FixedSizeArray<AnimationFrame>(frameCount);
        mFrameStartTimes = new float[frameCount];
        mLoop = false;
        mLength = 0.0f;
        setPhase(animationId);
    }

    public AnimationFrame getFrame(float animationTime) {
        AnimationFrame result = null;
        final float length = mLength;
        if (length > 0.0f) {
        	final FixedSizeArray<AnimationFrame> frames = mFrames;
        	assert frames.getCount() == frames.getCapacity();
            final int frameCount = frames.getCount();
            result = frames.get(frameCount - 1);

            if (frameCount > 1) {
	            float currentTime = 0.0f;
	            float cycleTime = animationTime;
	            if (mLoop) {
	                cycleTime = animationTime % length;
	            }
	
	            if (cycleTime < length) {
	            	// When there are very few frames it's actually slower to do a binary search
	            	// of the frame list.  So we'll use a linear search for small animations
	            	// and only pull the binary search out when the frame count is large.
	            	if (mFrameStartTimes.length > LINEAR_SEARCH_CUTOFF) {
		            	int index = Arrays.binarySearch(mFrameStartTimes, cycleTime);
		            	if (index < 0) {
		            		index = -(index + 1) - 1;
		            	}
		            	result = frames.get(index);
	            	} else {
		                for (int x = 0; x < frameCount; x++) {
		                    AnimationFrame frame = frames.get(x);
		                    currentTime += frame.holdTime;
		                    if (currentTime > cycleTime) {
		                        result = frame;
		                        break;
		                    }
		                }
	            	}
	            }
	        }
        }
        return result;
    }

    public void addFrame(AnimationFrame frame) {
    	mFrameStartTimes[mFrames.getCount()] = mLength;
    	mFrames.add(frame);
        mLength += frame.holdTime;
    }
    
    public float getLength() {
        return mLength;
    }

    public void setLoop(boolean loop) {
        mLoop = loop;
    }

    public boolean getLoop() {
        return mLoop;
    }
}
