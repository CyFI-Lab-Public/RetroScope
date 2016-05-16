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

import com.replica.replicaisland.SoundSystem.Sound;

public class ButtonAnimationComponent extends GameComponent {
    public static final class Animation {
        // Animations
        public static final int UP = 0;
        public static final int DOWN = 1;
    }
    private ChannelSystem.Channel mChannel;
    private SpriteComponent mSprite;
    private ChannelSystem.ChannelFloatValue mLastPressedTime;
    private Sound mDepressSound;
    
    public ButtonAnimationComponent() {
        super();
        setPhase(ComponentPhases.ANIMATION.ordinal());
        mLastPressedTime = new ChannelSystem.ChannelFloatValue();
        reset();
    }
    
    @Override
    public void reset() {
        mSprite = null;
        mChannel = null;
        mLastPressedTime.value = 0.0f;
        mDepressSound = null;
    }
    
    @Override
    public void update(float timeDelta, BaseObject parent) {
        if (mSprite != null) {
            GameObject parentObject = (GameObject)parent;
            
            if (parentObject.getCurrentAction() == GameObject.ActionType.HIT_REACT &&
                    parentObject.lastReceivedHitType == CollisionParameters.HitType.DEPRESS) {
            	if (mSprite.getCurrentAnimation() == Animation.UP) {
            		SoundSystem sound = sSystemRegistry.soundSystem;
            		if (sound != null) {
            			sound.play(mDepressSound, false, SoundSystem.PRIORITY_NORMAL);
            		}
            	}
                mSprite.playAnimation(Animation.DOWN);
                parentObject.setCurrentAction(GameObject.ActionType.IDLE);
                if (mChannel != null) {
                    TimeSystem time = sSystemRegistry.timeSystem;
                    mLastPressedTime.value = time.getGameTime();
                    mChannel.value = mLastPressedTime;
                }
            } else {
                mSprite.playAnimation(Animation.UP);
                
            }
        }
    }
    
    public void setSprite(SpriteComponent sprite) {
        mSprite = sprite;
    }
    
    public void setChannel(ChannelSystem.Channel channel) {
        mChannel = channel;
    }

	public void setDepressSound(Sound sound) {
		mDepressSound = sound;
	}
}
