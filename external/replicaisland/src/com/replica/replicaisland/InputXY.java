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


public class InputXY {
	private InputButton mXAxis;
	private InputButton mYAxis;
	
	public InputXY() {
		mXAxis = new InputButton();
		mYAxis = new InputButton();
	}
	
	public InputXY(InputButton xAxis, InputButton yAxis) {
		mXAxis = xAxis;
		mYAxis = yAxis;
	}
	
	public final void press(float currentTime, float x, float y) {
		mXAxis.press(currentTime, x);
		mYAxis.press(currentTime, y);
	}
	
	public final void release() {
		mXAxis.release();
		mYAxis.release();
	}
	
	public boolean getTriggered(float time) {
		return mXAxis.getTriggered(time) || mYAxis.getTriggered(time);
	}
	
	public boolean getPressed() {
		return mXAxis.getPressed() || mYAxis.getPressed();
	}
	
	public final void setVector(Vector2 vector) {
		vector.x = mXAxis.getMagnitude();
		vector.y = mYAxis.getMagnitude();
	}
	
	public final float getX() {
		return mXAxis.getMagnitude();
	}
	
	public final float getY() {
		return mYAxis.getMagnitude();
	}
	
	public final float getLastPressedTime() {
		return Math.max(mXAxis.getLastPressedTime(), mYAxis.getLastPressedTime());
	}
	
	public final void releaseX() {
		mXAxis.release();
	}
	
	public final void releaseY() {
		mYAxis.release();
	}
	

	public void setMagnitude(float x, float y) {
		mXAxis.setMagnitude(x);
		mYAxis.setMagnitude(y);
	}
	
	public void reset() {
		mXAxis.reset();
		mYAxis.reset();
	}
	
	public void clone(InputXY other) {
		if (other.getPressed()) {
			press(other.getLastPressedTime(), other.getX(), other.getY());
		} else {
			release();
		}
	}
}
