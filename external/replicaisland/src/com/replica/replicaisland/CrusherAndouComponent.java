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

public class CrusherAndouComponent extends GameComponent {
    private ChangeComponentsComponent mSwap;
    
    public CrusherAndouComponent() {
        super();
        setPhase(ComponentPhases.THINK.ordinal());
        reset();
    }
    
    @Override
    public void reset() {
    	mSwap = null;
    }
    
    @Override
    public void update(float timeDelta, BaseObject parent) {
        GameObject parentObject = (GameObject)parent;
        
    	if (mSwap.getCurrentlySwapped()) {
    		if (parentObject.touchingGround()) {
    			parentObject.setCurrentAction(GameObject.ActionType.IDLE);
    		}
        } else {
        	InputSystem input = sSystemRegistry.inputSystem;
        	if (input.getTouchScreen().getTriggered(sSystemRegistry.timeSystem.getGameTime())) {
        		parentObject.setCurrentAction(GameObject.ActionType.ATTACK);
        		mSwap.activate(parentObject);
        	}
        }
    }
    
    public void setSwap(ChangeComponentsComponent swap) {
    	mSwap = swap;
    }
}
