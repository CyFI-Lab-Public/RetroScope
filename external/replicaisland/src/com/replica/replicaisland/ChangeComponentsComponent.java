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
 * A game component that can swap other components in and out of its parent game object.  The
 * purpose of the ChangeComponentsComponent is to allow game objects to have different "modes" 
 * defined by different combinations of GameComponents.  ChangeComponentsComponent manages the
 * switching in and out of those modes by activating and deactivating specific game components.
 */
public class ChangeComponentsComponent extends GameComponent {
    private final static int MAX_COMPONENT_SWAPS = 16;
    private FixedSizeArray<GameComponent> mComponentsToInsert;
    private FixedSizeArray<GameComponent> mComponentsToRemove;
    private boolean mPingPong;
    private boolean mActivated;
    private boolean mCurrentlySwapped;
    private GameObject.ActionType mSwapOnAction;
    private GameObject.ActionType mLastAction;
    
    public ChangeComponentsComponent() {
        super();
        
        mComponentsToInsert = new FixedSizeArray<GameComponent>(MAX_COMPONENT_SWAPS);
        mComponentsToRemove = new FixedSizeArray<GameComponent>(MAX_COMPONENT_SWAPS);
        
        reset();
    }
    
    @Override
    public void reset() {
        
        GameObjectFactory factory = sSystemRegistry.gameObjectFactory;
        // GameComponents hanging out in the mComponentsToInsert list are not part of the object
        // hierarchy, so we need to manually release them.
        if (factory != null) {
            FixedSizeArray<GameComponent> unrelasedComponents = mComponentsToInsert;
            if (mActivated) {
                if (!mPingPong) {
                    // if we've activated and are not set to ping pong, the contents of 
                    // mComponentsToInsert have already been inserted into the object and
                    // will be cleaned up with all the other of the object's components.
                    // In that case, mComponentsToRemove contains objects that need manual
                    // clean up.
                    unrelasedComponents = mComponentsToRemove;
                }
            }
            final int inactiveComponentCount = unrelasedComponents.getCount();
            for (int x = 0; x < inactiveComponentCount; x++) {
                GameComponent component = unrelasedComponents.get(x);
                if (!component.shared) {
                    factory.releaseComponent(component);
                }
            }
        }
        mComponentsToInsert.clear();
        mComponentsToRemove.clear();
        mPingPong = false;
        mActivated = false;
        mCurrentlySwapped = false;
        mSwapOnAction = GameObject.ActionType.INVALID;
        mLastAction = GameObject.ActionType.INVALID;
    }
    
    
    
    @Override
	public void update(float timeDelta, BaseObject parent) {
		if (mSwapOnAction != GameObject.ActionType.INVALID) {
			GameObject parentObject = (GameObject)parent;
			GameObject.ActionType currentAction = parentObject.getCurrentAction();
			if (currentAction != mLastAction) {
				mLastAction = currentAction;
				if (currentAction == mSwapOnAction) {
					activate(parentObject);
				}
			}
		}
	}

	public void addSwapInComponent(GameComponent component) {
        mComponentsToInsert.add(component);
    }
    
    public void addSwapOutComponent(GameComponent component) {
        mComponentsToRemove.add(component);
    }
    
    public void setPingPongBehavior(boolean pingPong) {
        mPingPong = pingPong;
    }
    
    public void setSwapAction(GameObject.ActionType action) {
    	mSwapOnAction = action;
    }
    
    /** Inserts and removes components added to the swap-in and swap-out list, respectively. 
     * Unless mPingPong is set, this may only be called once.
     * @param parent  The parent object to swap components on.
     */
    public void activate(GameObject parent) {
        if (!mActivated || mPingPong) {
            final int removeCount = mComponentsToRemove.getCount();
            for (int x = 0; x < removeCount; x++) {
                parent.remove(mComponentsToRemove.get(x));
            }
            
            final int addCount = mComponentsToInsert.getCount();
            for (int x = 0; x < addCount; x++) {
                parent.add(mComponentsToInsert.get(x));
            }
            
            mActivated = true;
            mCurrentlySwapped = !mCurrentlySwapped;
            if (mPingPong) {
                FixedSizeArray<GameComponent> swap = mComponentsToInsert;
                mComponentsToInsert = mComponentsToRemove;
                mComponentsToRemove = swap;
            }
        }
    }

    public boolean getCurrentlySwapped() {
        return mCurrentlySwapped;
    }
}
