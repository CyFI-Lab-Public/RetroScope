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
 * Manages input from a roller wheel and touch screen.  Reduces frequent UI messages to
 * an average direction over a short period of time.
 */
public class InputSystem extends BaseObject {
	private InputTouchScreen mTouchScreen = new InputTouchScreen();
	private InputXY mOrientationSensor = new InputXY();
	private InputXY mTrackball = new InputXY();
    private InputKeyboard mKeyboard = new InputKeyboard();
    private int mScreenRotation = 0;
    private float mOrientationInput[] = new float[3];
    private float mOrientationOutput[] = new float[3];
               
    public InputSystem() {
        super();
        reset();
    }
    
    @Override
    public void reset() {
    	mTrackball.reset();
    	mTouchScreen.reset();
    	mKeyboard.resetAll();
    	mOrientationSensor.reset();
    }

    public void roll(float x, float y) {
        TimeSystem time = sSystemRegistry.timeSystem;
    	mTrackball.press(time.getGameTime(), mTrackball.getX() + x, mTrackball.getY() + y);
    }
    
    public void touchDown(int index, float x, float y) {
	   ContextParameters params = sSystemRegistry.contextParameters;
	   TimeSystem time = sSystemRegistry.timeSystem;
	   // Change the origin of the touch location from the top-left to the bottom-left to match
	   // OpenGL space.
	   // TODO: UNIFY THIS SHIT
	   mTouchScreen.press(index, time.getGameTime(), x, params.gameHeight - y);   
    }
    
    public void touchUp(int index, float x, float y) {
    	// TODO: record up location?
    	mTouchScreen.release(index);
    }
    
    
    public void setOrientation(float x, float y, float z) {
    	// The order of orientation axes changes depending on the rotation of the screen.
    	// Some devices call landscape "ROTAION_90" (e.g. phones), while others call it
    	// "ROTATION_0" (e.g. tablets).  So we need to adjust the axes from canonical
    	// space into screen space depending on the rotation of the screen from
    	// whatever this device calls "default." 
    	mOrientationInput[0] = x;
    	mOrientationInput[1] = y;
    	mOrientationInput[2] = z;
    	
    	canonicalOrientationToScreenOrientation(mScreenRotation, mOrientationInput, mOrientationOutput);
    	
    	// Now we have screen space rotations around xyz.
    	final float horizontalMotion = mOrientationOutput[1] / 90.0f;
        final float verticalMotion = mOrientationOutput[0] / 90.0f;
        
        TimeSystem time = sSystemRegistry.timeSystem;
        mOrientationSensor.press(time.getGameTime(), horizontalMotion, verticalMotion);
        
    }
    
    public void keyDown(int keycode) {
    	TimeSystem time = sSystemRegistry.timeSystem;
        final float gameTime = time.getGameTime();
        mKeyboard.press(gameTime, keycode);
    }
    
    public void keyUp(int keycode) {
    	mKeyboard.release(keycode);
    }
    
    public void releaseAllKeys() {
    	mTrackball.releaseX();
    	mTrackball.releaseY();
    	mTouchScreen.resetAll();
    	mKeyboard.releaseAll();
    	mOrientationSensor.release();
    }

	public InputTouchScreen getTouchScreen() {
		return mTouchScreen;
	}

	public InputXY getOrientationSensor() {
		return mOrientationSensor;
	}

	public InputXY getTrackball() {
		return mTrackball;
	}

	public InputKeyboard getKeyboard() {
		return mKeyboard;
	}
	
	public void setScreenRotation(int rotation) {
		mScreenRotation = rotation;
	}
    
	// Thanks to NVIDIA for this useful canonical-to-screen orientation function.
	// More here: http://developer.download.nvidia.com/tegra/docs/tegra_android_accelerometer_v5f.pdf
	static void canonicalOrientationToScreenOrientation(
			int displayRotation, float[] canVec, float[] screenVec) { 
		final int axisSwap[][] = { 
			{ 1, -1, 0, 1 },   // ROTATION_0 
			{-1, -1, 1, 0 },   // ROTATION_90 
			{-1,  1, 0, 1 },   // ROTATION_180 
			{ 1,  1, 1, 0 } }; // ROTATION_270 
		
		final int[] as = axisSwap[displayRotation]; 
		screenVec[0] = (float)as[0] * canVec[ as[2] ]; 
		screenVec[1] = (float)as[1] * canVec[ as[3] ]; 
		screenVec[2] = canVec[2]; 
	} 

    

}
