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

public class InputKeyboard {
	private InputButton[] mKeys;
	
	public InputKeyboard() {
		final int count = KeyEvent.getMaxKeyCode();
		mKeys = new InputButton[count];
		for (int x = 0; x < count; x++) {
			mKeys[x] = new InputButton();
		}
	}
	
	public void press(float currentTime, int keycode) {
		assert keycode >= 0 && keycode < mKeys.length;	
		if (keycode >= 0 && keycode < mKeys.length){
			mKeys[keycode].press(currentTime, 1.0f);
		}
	}
	
	public void release(int keycode) {
		assert keycode >= 0 && keycode < mKeys.length;
		if (keycode >= 0 && keycode < mKeys.length){
			mKeys[keycode].release();
		}
	}
	
	public void releaseAll() {
		final int count = mKeys.length;
		for (int x = 0; x < count; x++) {
			mKeys[x].release();
		}
	}
	
	public InputButton[] getKeys() {
		return mKeys;
	}
	
	public void resetAll() {
		final int count = mKeys.length;
		for (int x = 0; x < count; x++) {
			mKeys[x].reset();
		}
	}
}
