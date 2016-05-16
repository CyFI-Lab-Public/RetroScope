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

public class HitPlayerComponent extends GameComponent {
	float mDistance2;
	Vector2 mPlayerPosition;
	Vector2 mMyPosition;
	HitReactionComponent mHitReact;
	int mHitType;
	boolean mHitDirection;
	
	public HitPlayerComponent() {
        super();
        mPlayerPosition = new Vector2();
        mMyPosition = new Vector2();
        reset();
        setPhase(ComponentPhases.THINK.ordinal());
    }
    
    @Override
    public void reset() {
    	mDistance2 = 0.0f;
    	mPlayerPosition.zero();
    	mMyPosition.zero();
    	mHitReact = null;
    	mHitType = CollisionParameters.HitType.INVALID;
    	mHitDirection = false; // by default, hit myself
    }

    @Override
    public void update(float timeDelta, BaseObject parent) {
        GameObjectManager manager = sSystemRegistry.gameObjectManager;
        if (manager != null && mHitReact != null) {
        	GameObject player = manager.getPlayer();
        	if (player != null && player.life > 0) {
        		mPlayerPosition.set(player.getCenteredPositionX(), player.getCenteredPositionY());
        		GameObject parentObject = (GameObject)parent;
        		mMyPosition.set(parentObject.getCenteredPositionX(), parentObject.getCenteredPositionY());
        		if (mMyPosition.distance2(mPlayerPosition) <= mDistance2) {
        			HitReactionComponent playerHitReact = player.findByClass(HitReactionComponent.class);
        			if (playerHitReact != null) {
        				if (!mHitDirection) {
        					// hit myself
        					boolean accepted = mHitReact.receivedHit(parentObject, player, mHitType);
        					playerHitReact.hitVictim(player, parentObject, mHitType, accepted);
        				} else {
        					// hit the player
        					boolean accepted = playerHitReact.receivedHit(player, parentObject, mHitType);
        					mHitReact.hitVictim(parentObject, player, mHitType, accepted);
        				}
        			}
        		}
        	}
        }
    }
    
    public void setup(float distance, HitReactionComponent hitReact, int hitType, boolean hitPlayer) {
    	mDistance2 = distance * distance;
    	mHitReact = hitReact;
    	mHitType = hitType;
    	mHitDirection = hitPlayer;
    }
}
