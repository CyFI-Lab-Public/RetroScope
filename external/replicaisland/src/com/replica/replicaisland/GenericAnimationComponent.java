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

public class GenericAnimationComponent extends GameComponent {
    private SpriteComponent mSprite;
    
    public GenericAnimationComponent() {
        super();
        setPhase(ComponentPhases.ANIMATION.ordinal());
        reset();
    }
    
    @Override
    public void reset() {
        mSprite = null;
    }
    
    @Override
    public void update(float timeDelta, BaseObject parent) {
        if (mSprite != null) {
            GameObject parentObject = (GameObject) parent;
            if (parentObject.facingDirection.x != 0.0f && parentObject.getVelocity().x != 0.0f) {
                parentObject.facingDirection.x = Utils.sign(parentObject.getVelocity().x);
            }
            switch(parentObject.getCurrentAction()) {
                
                case IDLE:
                    mSprite.playAnimation(Animation.IDLE);
                    break;
                case MOVE:
                    mSprite.playAnimation(Animation.MOVE);
                    break;
                case ATTACK:
                    mSprite.playAnimation(Animation.ATTACK);
                    break;
                case HIT_REACT:
                    mSprite.playAnimation(Animation.HIT_REACT);
                    break;
                case DEATH:
                    mSprite.playAnimation(Animation.DEATH);
                    break;
                case HIDE:
                    mSprite.playAnimation(Animation.HIDE);
                    break;
                case FROZEN:
                    mSprite.playAnimation(Animation.FROZEN);
                    break;
                case INVALID:
                default:  
                    mSprite.playAnimation(-1);
                    break;
            }
        }
    }
    
    public void setSprite(SpriteComponent sprite) {
        mSprite = sprite;
    }
   
    
    public static final class Animation {
        public static final int IDLE = 0;
        public static final int MOVE = 1;
        public static final int ATTACK = 2;
        public static final int HIT_REACT = 3;
        public static final int DEATH = 4;
        public static final int HIDE = 5;
        public static final int FROZEN = 6;
    }
}
