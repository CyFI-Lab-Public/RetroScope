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

import com.replica.replicaisland.CollisionParameters.HitType;

/**
 * GameObject defines any object that resides in the game world (character, background, special
 * effect, enemy, etc).  It is a collection of GameComponents which implement its behavior;
 * GameObjects themselves have no intrinsic behavior.  GameObjects are also "bags of data" that
 * components can use to share state (direct component-to-component communication is discouraged).
 */
public class GameObject extends PhasedObjectManager {
    private final static float COLLISION_SURFACE_DECAY_TIME = 0.3f;
    // These fields are managed by components.
    private Vector2 mPosition;
    private Vector2 mVelocity;
    private Vector2 mTargetVelocity;
    private Vector2 mAcceleration;
    private Vector2 mImpulse;

    private Vector2 mBackgroundCollisionNormal;

    private float mLastTouchedFloorTime;
    private float mLastTouchedCeilingTime;
    private float mLastTouchedLeftWallTime;
    private float mLastTouchedRightWallTime;
  
    public boolean positionLocked;
    
    public float activationRadius;
    public boolean destroyOnDeactivation;
    
    public int life;
    
    public int lastReceivedHitType;
    
    public Vector2 facingDirection;
    public float width;
    public float height;
    
    private static final int DEFAULT_LIFE = 1;
    
    public enum ActionType {
        INVALID,
        IDLE,
        MOVE,
        ATTACK,
        HIT_REACT,
        DEATH,
        HIDE,
        FROZEN
    }
    
    private ActionType mCurrentAction;
    
    public enum Team {
        NONE,
        PLAYER,
        ENEMY
    }
    
    public Team team;
    
    public GameObject() {
        super();

        mPosition = new Vector2();
        mVelocity = new Vector2();
        mTargetVelocity = new Vector2();
        mAcceleration = new Vector2();
        mImpulse = new Vector2();
        mBackgroundCollisionNormal = new Vector2();
        
        facingDirection = new Vector2(1, 0);
        
        reset();
    }
    
    @Override
    public void reset() {
        removeAll();
        commitUpdates();
        
        mPosition.zero();
        mVelocity.zero();
        mTargetVelocity.zero();
        mAcceleration.zero();
        mImpulse.zero();
        mBackgroundCollisionNormal.zero();
        facingDirection.set(1.0f, 1.0f);
        
        mCurrentAction = ActionType.INVALID;
        positionLocked = false;
        activationRadius = 0;
        destroyOnDeactivation = false;
        life = DEFAULT_LIFE;
        team = Team.NONE;
        width = 0.0f;
        height = 0.0f;
        
        lastReceivedHitType = HitType.INVALID;
    }
    
    // Utility functions
    public final boolean touchingGround() {
        final TimeSystem time = sSystemRegistry.timeSystem;
        final float gameTime = time.getGameTime();
        final boolean touching = gameTime > 0.1f &&
            Utils.close(mLastTouchedFloorTime, time.getGameTime(), COLLISION_SURFACE_DECAY_TIME);
        return touching;
    }
    
    public final boolean touchingCeiling() {
        final TimeSystem time = sSystemRegistry.timeSystem;
        final float gameTime = time.getGameTime();
        final boolean touching = gameTime > 0.1f && 
            Utils.close(mLastTouchedCeilingTime, time.getGameTime(), COLLISION_SURFACE_DECAY_TIME);
        return touching;
    }
    
    public final boolean touchingLeftWall() {
        final TimeSystem time = sSystemRegistry.timeSystem;
        final float gameTime = time.getGameTime();
        final boolean touching = gameTime > 0.1f &&
            Utils.close(mLastTouchedLeftWallTime, time.getGameTime(), COLLISION_SURFACE_DECAY_TIME);
        return touching;
    }
    
    public final boolean touchingRightWall() {
        final TimeSystem time = sSystemRegistry.timeSystem;
        final float gameTime = time.getGameTime();
        final boolean touching = gameTime > 0.1f &&
            Utils.close(mLastTouchedRightWallTime, time.getGameTime(), COLLISION_SURFACE_DECAY_TIME);
        return touching;
    }

    public final Vector2 getPosition() {
        return mPosition;
    }

    public final void setPosition(Vector2 position) {
        mPosition.set(position);
    }
    
    public final float getCenteredPositionX() {
        return mPosition.x + (width / 2.0f);
    }
    
    public final float getCenteredPositionY() {
        return mPosition.y + (height / 2.0f);
    }

    public final Vector2 getVelocity() {
        return mVelocity;
    }

    public final void setVelocity(Vector2 velocity) {
        mVelocity.set(velocity);
    }

    public final Vector2 getTargetVelocity() {
        return mTargetVelocity;
    }

    public final void setTargetVelocity(Vector2 targetVelocity) {
        mTargetVelocity.set(targetVelocity);
    }

    public final Vector2 getAcceleration() {
        return mAcceleration;
    }

    public final void setAcceleration(Vector2 acceleration) {
        mAcceleration.set(acceleration);
    }

    public final Vector2 getImpulse() {
        return mImpulse;
    }

    public final void setImpulse(Vector2 impulse) {
        mImpulse.set(impulse);
    }

    public final Vector2 getBackgroundCollisionNormal() {
        return mBackgroundCollisionNormal;
    }

    public final void setBackgroundCollisionNormal(Vector2 normal) {
        mBackgroundCollisionNormal.set(normal);
    }

    public final float getLastTouchedFloorTime() {
        return mLastTouchedFloorTime;
    }

    public final void setLastTouchedFloorTime(float lastTouchedFloorTime) {
        mLastTouchedFloorTime = lastTouchedFloorTime;
    }

    public final float getLastTouchedCeilingTime() {
        return mLastTouchedCeilingTime;
    }

    public final void setLastTouchedCeilingTime(float lastTouchedCeilingTime) {
        mLastTouchedCeilingTime = lastTouchedCeilingTime;
    }

    public final float getLastTouchedLeftWallTime() {
        return mLastTouchedLeftWallTime;
    }

    public final void setLastTouchedLeftWallTime(float lastTouchedLeftWallTime) {
        mLastTouchedLeftWallTime = lastTouchedLeftWallTime;
    }

    public final float getLastTouchedRightWallTime() {
        return mLastTouchedRightWallTime;
    }

    public final void setLastTouchedRightWallTime(float lastTouchedRightWallTime) {
        mLastTouchedRightWallTime = lastTouchedRightWallTime;
    }
    
    public final ActionType getCurrentAction() {
        return mCurrentAction;
    }
    
    public final void setCurrentAction(ActionType type) {
        mCurrentAction = type;
    }
}
