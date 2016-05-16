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
 * A component to include dynamic collision volumes (such as those produced every frame from
 * animating sprites) in the dynamic collision world.  Given a set of "attack" volumes and 
 * "vulnerability" volumes (organized such that only attack vs vulnerability intersections result
 * in valid "hits"), this component creates a bounding volume that encompasses the set and submits
 * it to the dynamic collision system.  Including this component in a game object will allow it to
 * send and receive hits to other game objects.
 */
public class DynamicCollisionComponent extends GameComponent {
    private FixedSizeArray<CollisionVolume> mAttackVolumes;
    private FixedSizeArray<CollisionVolume> mVulnerabilityVolumes;
    private SphereCollisionVolume mBoundingVolume;
    private HitReactionComponent mHitReactionComponent;
    
    public DynamicCollisionComponent() {
        super();
        mBoundingVolume = new SphereCollisionVolume(0.0f, 0.0f, 0.0f);
        setPhase(ComponentPhases.FRAME_END.ordinal());
        reset();
    }
    
    @Override
    public void reset() {
       mAttackVolumes = null;
       mVulnerabilityVolumes = null;
       mBoundingVolume.setCenter(Vector2.ZERO);
       mBoundingVolume.setRadius(0.0f);
       mHitReactionComponent = null;     
    }
    
    @Override
    public void update(float timeDelta, BaseObject parent) {
        GameObjectCollisionSystem collision = sSystemRegistry.gameObjectCollisionSystem;
        if (collision != null && mBoundingVolume.getRadius() > 0.0f) {
            collision.registerForCollisions((GameObject)parent, mHitReactionComponent, mBoundingVolume, 
                    mAttackVolumes, mVulnerabilityVolumes);
        }
    }
    
    public void setHitReactionComponent(HitReactionComponent component) {
        mHitReactionComponent = component;
    }
    
    public void setCollisionVolumes(FixedSizeArray<CollisionVolume> attackVolumes, 
            FixedSizeArray<CollisionVolume> vulnerableVolumes) {
        if (mVulnerabilityVolumes != vulnerableVolumes || mAttackVolumes != attackVolumes) {
            mAttackVolumes = attackVolumes;
            mVulnerabilityVolumes = vulnerableVolumes;
            mBoundingVolume.reset();
            if (mAttackVolumes != null) {
               final int count = mAttackVolumes.getCount();
               for (int x = 0; x < count; x++) {
                   mBoundingVolume.growBy(mAttackVolumes.get(x));
               }
            }
            
            if (mVulnerabilityVolumes != null) {
                final int count = mVulnerabilityVolumes.getCount();
                for (int x = 0; x < count; x++) {
                    mBoundingVolume.growBy(mVulnerabilityVolumes.get(x));
                }
             }
        }
    }
}
