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

public class AttackAtDistanceComponent extends GameComponent {
    private static final int DEFAULT_ATTACK_DISTANCE = 100;
    private float mAttackDistance;
    private float mAttackDelay;
    private float mAttackLength;
    private float mAttackStartTime;
    private boolean mRequireFacing;
    private Vector2 mDistance;
    
    public AttackAtDistanceComponent() {
        super();
        setPhase(GameComponent.ComponentPhases.THINK.ordinal());
        mDistance = new Vector2();
        reset();
    }
    
    @Override
    public void reset() {
        mAttackDelay = 0;
        mAttackLength = 0;
        mAttackDistance = DEFAULT_ATTACK_DISTANCE;
        mRequireFacing = false;
    }
    
    @Override
    public void update(float timeDelta, BaseObject parent) {   
        GameObject parentObject = (GameObject) parent;

        GameObjectManager manager = sSystemRegistry.gameObjectManager;
        if (manager != null) {
            GameObject player = manager.getPlayer();
            if (player != null) {
                mDistance.set(player.getPosition());
                mDistance.subtract(parentObject.getPosition());
                
                TimeSystem time = sSystemRegistry.timeSystem;
                final float currentTime = time.getGameTime();
                final boolean facingPlayer = 
                    (Utils.sign(player.getPosition().x - parentObject.getPosition().x)
                        == Utils.sign(parentObject.facingDirection.x));
                final boolean facingDirectionCorrect = (mRequireFacing && facingPlayer)
                    || !mRequireFacing;
                if (parentObject.getCurrentAction() == GameObject.ActionType.ATTACK) {
                    if (currentTime > mAttackStartTime + mAttackLength) {
                        parentObject.setCurrentAction(GameObject.ActionType.IDLE);
                    }
                } else if (mDistance.length2() < (mAttackDistance * mAttackDistance) 
                            && currentTime > mAttackStartTime + mAttackLength + mAttackDelay
                            && facingDirectionCorrect) {
                    mAttackStartTime = currentTime;
                    parentObject.setCurrentAction(GameObject.ActionType.ATTACK); 
                } else {
                    parentObject.setCurrentAction(GameObject.ActionType.IDLE);
                }
            } 
        }
       
    }

    public void setupAttack(float distance, float delay, float duration, boolean requireFacing) {
        mAttackDistance = distance;
        mAttackDelay = delay;
        mAttackLength = duration;
        mRequireFacing = requireFacing;
    }
    
  
}
