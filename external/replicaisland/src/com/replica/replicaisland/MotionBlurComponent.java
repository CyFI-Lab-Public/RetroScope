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

public class MotionBlurComponent extends GameComponent {
	private static final int STEP_COUNT = 4;
	private static final float STEP_DELAY = 0.1f;
	private static final float OPACITY_STEP = 1.0f / (STEP_COUNT + 1);
	private BlurRecord[] mHistory;
	private RenderComponent mBlurTarget;
	private float mStepDelay;
	private int mCurrentStep;
	private float mTimeSinceLastStep;
	private int mTargetPriority;
	
	private class BlurRecord {
		public Vector2 position = new Vector2();
		public Texture texture;
		public int width;
		public int height;
		public int[] crop = new int[4];
	}
	public MotionBlurComponent() {
        super();
        mHistory = new BlurRecord[STEP_COUNT];
        for (int x = 0; x < STEP_COUNT; x++) {
        	mHistory[x] = new BlurRecord();
        }
        reset();
        setPhase(ComponentPhases.PRE_DRAW.ordinal());
    }
	
	@Override
	public void reset() {
		for (int x = 0; x < STEP_COUNT; x++) {
			mHistory[x].texture = null;
			mHistory[x].position.zero();
        }
		mStepDelay = STEP_DELAY;
		mBlurTarget = null;
		mCurrentStep = 0;
		mTimeSinceLastStep = 0.0f;
	}
	
	public void setTarget(RenderComponent target) {
		mBlurTarget = target;
	}
	
	@Override
	public void update(float timeDelta, BaseObject parent) {
		if (mBlurTarget != null) {
			mTimeSinceLastStep += timeDelta;
			if (mTimeSinceLastStep > mStepDelay) {
				DrawableBitmap drawable = (DrawableBitmap)mBlurTarget.getDrawable();
				if (drawable != null) {
					Texture currentTexture = drawable.getTexture();
					mTargetPriority = mBlurTarget.getPriority();
					mHistory[mCurrentStep].texture = currentTexture;
					mHistory[mCurrentStep].position.set(((GameObject)parent).getPosition());
					mHistory[mCurrentStep].width = drawable.getWidth();
					mHistory[mCurrentStep].height = drawable.getHeight();
					final int[] drawableCrop = drawable.getCrop();
					mHistory[mCurrentStep].crop[0] = drawableCrop[0];
					mHistory[mCurrentStep].crop[1] = drawableCrop[1];
					mHistory[mCurrentStep].crop[2] = drawableCrop[2];
					mHistory[mCurrentStep].crop[3] = drawableCrop[3];
					mCurrentStep = (mCurrentStep + 1) % STEP_COUNT;
					mTimeSinceLastStep = 0.0f;
				}
			}
			

            RenderSystem renderer = sSystemRegistry.renderSystem;
            
            
			final int startStep = mCurrentStep > 0 ? mCurrentStep - 1 : STEP_COUNT - 1;
			// draw each step
			for (int x = 0; x < STEP_COUNT; x++) {
				final int step = (startStep - x) < 0 ? (STEP_COUNT + (startStep - x)) : (startStep - x);
				final BlurRecord record = mHistory[step];
				if (record.texture != null) {
					DrawableBitmap stepImage = sSystemRegistry.drawableFactory.allocateDrawableBitmap();
					stepImage.setTexture(record.texture);
					stepImage.setWidth(record.width);
					stepImage.setHeight(record.height);
					stepImage.setCrop(record.crop[0], record.crop[1], record.crop[2], -record.crop[3]);
					final float opacity = (STEP_COUNT - x) * OPACITY_STEP;
					stepImage.setOpacity(opacity);
					
	             
                    renderer.scheduleForDraw(stepImage, record.position, mTargetPriority - (x + 1), true);
				}
			}
		}
	}
}
