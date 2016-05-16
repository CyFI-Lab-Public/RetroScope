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

public class FrameRateWatcherComponent extends GameComponent {
	private RenderComponent mRenderComponent;
	private DrawableObject mDrawable;
	private float mMaxFrameTime = 1.0f / 30.0f;
	
	public FrameRateWatcherComponent() {
        super();
        setPhase(GameComponent.ComponentPhases.THINK.ordinal());
    }
    
    @Override
    public void reset() {
    	mRenderComponent = null;
    	mDrawable = null;
    }
    
    @Override
    public void update(float timeDelta, BaseObject parent) {   
    	if (mRenderComponent != null && mDrawable != null) {
    		if (timeDelta > mMaxFrameTime) {
    			mRenderComponent.setDrawable(mDrawable);
    		} else {
    			mRenderComponent.setDrawable(null);
    		}
    	}
	}
    
    public void setup(RenderComponent render, DrawableObject drawable) {
    	mRenderComponent = render;
    	mDrawable = drawable;
    }
}
