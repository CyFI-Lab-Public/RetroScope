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

public class FadeDrawableComponent extends GameComponent {
	public static final int LOOP_TYPE_NONE = 0;
	public static final int LOOP_TYPE_LOOP = 1;
	public static final int LOOP_TYPE_PING_PONG = 2;
	
	public static final int FADE_LINEAR = 0;
	public static final int FADE_EASE = 1;
	
	private Texture mTexture;
	private RenderComponent mRenderComponent;
	private float mInitialOpacity;
	private float mTargetOpacity;
	private float mStartTime;
	private float mDuration;
	private int mLoopType;
	private int mFunction;
	private float mInitialDelay;
	private float mInitialDelayTimer;
	private float mActivateTime;
	private float mPhaseDuration;
	
	public FadeDrawableComponent() {
		super();
        setPhase(ComponentPhases.PRE_DRAW.ordinal());
	}
	
	@Override
	public void reset() {
		mTexture = null;
		mRenderComponent = null;
		mInitialOpacity = 0.0f;
		mTargetOpacity = 0.0f;
		mDuration = 0.0f;
		mLoopType = LOOP_TYPE_NONE;
		mFunction = FADE_LINEAR;
		mStartTime = 0.0f;
		mInitialDelay = 0.0f;
		mActivateTime = 0.0f;
		mPhaseDuration = 0.0f;
		mInitialDelayTimer = 0.0f;
	}
	
	 @Override
	 public void update(float timeDelta, BaseObject parent) {
		 if (mRenderComponent != null) {
			 final TimeSystem time = sSystemRegistry.timeSystem;
			 final float currentTime = time.getGameTime();
			 
			 // Support repeating "phases" on top of the looping fade itself.
			 // Complexity++, but it lets this component handle several
			 // different use cases.
			 if (mActivateTime == 0.0f) {
				 mActivateTime = currentTime;
				 mInitialDelayTimer = mInitialDelay;
			 } else if (mPhaseDuration > 0.0f && currentTime - mActivateTime > mPhaseDuration) {
				 mActivateTime = currentTime;
				 mInitialDelayTimer = mInitialDelay;
				 mStartTime = 0.0f;
			 }
			 
			 if (mInitialDelayTimer > 0.0f) {
				 mInitialDelayTimer -= timeDelta;
			 } else {
				 if (mStartTime == 0) {
					 mStartTime = currentTime;
				 }
				 float elapsed = currentTime - mStartTime;
				 float opacity = mInitialOpacity;
				 if (mLoopType != LOOP_TYPE_NONE && elapsed > mDuration) {
					 final float endTime = mStartTime + mDuration;
					 elapsed = endTime - currentTime;
					 mStartTime = endTime;
					 if (mLoopType == LOOP_TYPE_PING_PONG) {
						 float temp = mInitialOpacity;
						 mInitialOpacity = mTargetOpacity;
						 mTargetOpacity = temp;
					 }
				 }
				 
				 if (elapsed > mDuration) {
					 opacity = mTargetOpacity;
				 } else if (elapsed != 0.0f) {
					 if (mFunction == FADE_LINEAR) {
						 opacity = Lerp.lerp(mInitialOpacity, mTargetOpacity, mDuration, elapsed);
					 } else if (mFunction == FADE_EASE) {
						 opacity = Lerp.ease(mInitialOpacity, mTargetOpacity, mDuration, elapsed);
					 }
				 }
				 
				 if (mTexture != null) {
					 // If a texture is set then we supply a drawable to the render component.
					 // If not, we take whatever drawable the renderer already has.
					 final DrawableFactory factory = sSystemRegistry.drawableFactory;
		             if (factory != null) {
		                 GameObject parentObject = ((GameObject)parent);
		                 DrawableBitmap bitmap = factory.allocateDrawableBitmap();
		                 bitmap.resize((int)mTexture.width, (int)mTexture.height);
		                 //TODO: Super tricky scale.  fix this!
		                 bitmap.setWidth((int)parentObject.width);
		                 bitmap.setHeight((int)parentObject.height);
		                 bitmap.setOpacity(opacity);
		                 bitmap.setTexture(mTexture);
		                 mRenderComponent.setDrawable(bitmap);
		             }
				 } else {
					 DrawableObject drawable = mRenderComponent.getDrawable();
					 // TODO: ack, instanceof!  Fix this!
					 if (drawable != null && drawable instanceof DrawableBitmap) {
						 ((DrawableBitmap)drawable).setOpacity(opacity);
					 }
				 }
			 }
		 }
	 }
	 
	 public void setupFade(float startOpacity, float endOpacity, float duration, int loopType, int function, float initialDelay) {
		 mInitialOpacity = startOpacity;
		 mTargetOpacity = endOpacity;
		 mDuration = duration;
		 mLoopType = loopType;
		 mFunction = function;
		 mInitialDelay = initialDelay;
	 }
	 
	 /** Enables phases; the initial delay will be re-started when the phase ends. **/
	 public void setPhaseDuration(float duration) {
		 mPhaseDuration = duration;
	 }
	 
	 /** If set to something non-null, this component will overwrite the drawable on the target render component. **/
	 public void setTexture(Texture texture) {
		 mTexture = texture;
	 }
	 
	 public void setRenderComponent(RenderComponent component) {
		 mRenderComponent = component;
	 }
	 
	 public void resetPhase() {
		 mActivateTime = 0.0f;
	 }
}
