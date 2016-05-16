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

public class PlaySingleSoundComponent extends GameComponent {
	private SoundSystem.Sound mSound;
	private int mSoundHandle;
	
	public PlaySingleSoundComponent() {
		super();
		reset();
        setPhase(ComponentPhases.THINK.ordinal());
	}
	
	@Override
	public void reset() {
		mSoundHandle = -1;
		mSound = null;
	}
	
	public void setSound(SoundSystem.Sound sound) {
		mSound = sound;
	}
	
	@Override
    public void update(float timeDelta, BaseObject parent) {
		if (mSoundHandle == -1 && mSound != null) {
			SoundSystem sound = sSystemRegistry.soundSystem;
			mSoundHandle = sound.play(mSound, false, SoundSystem.PRIORITY_NORMAL);
		}
	}
}
