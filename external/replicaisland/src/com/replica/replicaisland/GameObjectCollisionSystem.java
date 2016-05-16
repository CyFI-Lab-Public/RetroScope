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

import java.util.Comparator;

import com.replica.replicaisland.CollisionParameters.HitType;

/**
 * A system for calculating collisions between moving game objects.  This system accepts collision
 * volumes from game objects each frame and performs a series of tests to see which of them
 * overlap.  Collisions are only considered between offending "attack" volumes and receiving
 * "vulnerability" volumes.  This implementation works by using a sweep-and-prune algorithm:
 * objects to be considered are sorted in the x axis and then compared in one dimension for
 * overlaps.  A bounding volume that encompasses all attack and vulnerability volumes is used for
 * this test, and when an intersection is found the actual offending and receiving volumes are 
 * compared.  If an intersection is detected both objects receive notification via a
 * HitReactionComponent, if one has been specified.
 */
public class GameObjectCollisionSystem extends BaseObject {
    private static final int MAX_COLLIDING_OBJECTS = 256;
    private static final int COLLISION_RECORD_POOL_SIZE = 256;
    private static final CollisionVolumeComparator sCollisionVolumeComparator 
        = new CollisionVolumeComparator();
    private static CollisionVolume.FlipInfo sFlip = new CollisionVolume.FlipInfo();
    private static CollisionVolume.FlipInfo sOtherFlip = new CollisionVolume.FlipInfo();

    FixedSizeArray<CollisionVolumeRecord> mObjects;
    CollisionVolumeRecordPool mRecordPool;
	private boolean mDrawDebugBoundingVolume = false;
	private boolean mDrawDebugCollisionVolumes = false;
    
    
    public GameObjectCollisionSystem() {
        super();
        mObjects = new FixedSizeArray<CollisionVolumeRecord>(MAX_COLLIDING_OBJECTS);
        mObjects.setComparator(sCollisionVolumeComparator);
        //mObjects.setSorter(new ShellSorter<CollisionVolumeRecord>());
        mRecordPool = new CollisionVolumeRecordPool(COLLISION_RECORD_POOL_SIZE);
    }
    
    @Override
    public void reset() {
        final int count = mObjects.getCount();
        
        for (int x = 0; x < count; x++) {
            mRecordPool.release(mObjects.get(x));
        }
        mObjects.clear();
        
        mDrawDebugBoundingVolume = false;
        mDrawDebugCollisionVolumes = false;
    }
    
    /** 
     * Adds a game object, and its related volumes, to the dynamic collision world for one frame.
     * Once registered for collisions the object may damage other objects via attack volumes or
     * receive damage from other volumes via vulnerability volumes.
     * @param object  The object to consider for collision.
     * @param reactionComponent  A HitReactionComponent to notify when an intersection is calculated.
     * If null, the intersection will still occur and no notification will be sent.
     * @param boundingVolume  A volume that describes the game object in space.  It should encompass
     * all of the attack and vulnerability volumes.
     * @param attackVolumes  A list of volumes that can hit other game objects.  May be null.
     * @param vulnerabilityVolumes  A list of volumes that can receive hits from other game objects.
     * May be null.
     */
    public void registerForCollisions(GameObject object, 
            HitReactionComponent reactionComponent,
            CollisionVolume boundingVolume,
            FixedSizeArray<CollisionVolume> attackVolumes,
            FixedSizeArray<CollisionVolume> vulnerabilityVolumes) {
        CollisionVolumeRecord record = mRecordPool.allocate();
        if (record != null && object != null && boundingVolume != null 
                && (attackVolumes != null || vulnerabilityVolumes != null)) {
            record.object = object;
            record.boundingVolume = boundingVolume;
            record.attackVolumes = attackVolumes;
            record.vulnerabilityVolumes = vulnerabilityVolumes;
            record.reactionComponent = reactionComponent;
            mObjects.add(record);
        }
    }
    
    @Override
    public void update(float timeDelta, BaseObject parent) {
        // Sort the objects by their x position.
        mObjects.sort(true);
        
        final int count = mObjects.getCount();
        for (int x = 0; x < count; x++) {
            final CollisionVolumeRecord record = mObjects.get(x);
            final Vector2 position = record.object.getPosition();
            sFlip.flipX = (record.object.facingDirection.x < 0.0f);
            sFlip.flipY = (record.object.facingDirection.y < 0.0f);
            sFlip.parentWidth = record.object.width;
            sFlip.parentHeight = record.object.height;
            
            if (sSystemRegistry.debugSystem != null) {
            	drawDebugVolumes(record);
            }
            
            final float maxX = record.boundingVolume.getMaxXPosition(sFlip) + position.x;
            for (int y = x + 1; y < count; y++) {
                final CollisionVolumeRecord other = mObjects.get(y);
                final Vector2 otherPosition = other.object.getPosition();
                sOtherFlip.flipX = (other.object.facingDirection.x < 0.0f);
                sOtherFlip.flipY = (other.object.facingDirection.y < 0.0f);
                sOtherFlip.parentWidth = other.object.width;
                sOtherFlip.parentHeight = other.object.height;
                
                if (otherPosition.x + other.boundingVolume.getMinXPosition(sOtherFlip) > maxX) {
                    // These objects can't possibly be colliding.  And since the list is sorted,
                    // there are no potentially colliding objects after this object
                    // either, so we're done!
                    break;
                } else {
                	final boolean testRequired = (record.attackVolumes != null && other.vulnerabilityVolumes != null) ||
                		(record.vulnerabilityVolumes != null && other.attackVolumes != null);
                    if (testRequired && record.boundingVolume.intersects(position, sFlip,
                        other.boundingVolume, otherPosition, sOtherFlip)) {
                        // These two objects are potentially colliding.
                        // Now we must test all attack vs vulnerability boxes.
                        final int hit = testAttackAgainstVulnerability(
                                record.attackVolumes,
                                other.vulnerabilityVolumes, 
                                position,
                                otherPosition,
                                sFlip,
                                sOtherFlip);
                        if (hit != HitType.INVALID) {
                            boolean hitAccepted = false;
                            if (other.reactionComponent != null) {
                                hitAccepted = other.reactionComponent.receivedHit(
                                        other.object, record.object, hit);
                            }
                            if (record.reactionComponent != null) {
                                record.reactionComponent.hitVictim(
                                        record.object, other.object, hit, hitAccepted);
                            }
                            
                        }
                        
                        final int hit2 = testAttackAgainstVulnerability(
                                other.attackVolumes,
                                record.vulnerabilityVolumes, 
                                otherPosition, 
                                position,
                                sOtherFlip,
                                sFlip);
                        if (hit2 != HitType.INVALID) {
                            boolean hitAccepted = false;
                            if (record.reactionComponent != null) {
                                hitAccepted = record.reactionComponent.receivedHit(
                                        record.object, other.object, hit2);
                            }
                            if (other.reactionComponent != null) {
                                other.reactionComponent.hitVictim(
                                        other.object, record.object, hit2, hitAccepted);
                            }
                            
                        }
                    }
                }
            }
            // This is a little tricky.  Since we always sweep forward in the list it's safe
            // to invalidate the current record after we've tested it.  This way we don't have to
            // iterate over the object list twice.
            mRecordPool.release(record);
        }
        
        mObjects.clear();
    }
    
    /** Compares the passed list of attack volumes against the passed list of vulnerability volumes
     * and returns a hit type if an intersection is found.
     * @param attackVolumes  Offensive collision volumes.
     * @param vulnerabilityVolumes  Receiving collision volumes.
     * @param attackPosition  The world position of the attacking object.
     * @param vulnerabilityPosition  The world position of the receiving object.
     * @return  The hit type of the first attacking volume that intersects a vulnerability volume, 
     * or HitType.INVALID if no intersections are found.
     */
    private int testAttackAgainstVulnerability(
            FixedSizeArray<CollisionVolume> attackVolumes,
            FixedSizeArray<CollisionVolume> vulnerabilityVolumes,
            Vector2 attackPosition,
            Vector2 vulnerabilityPosition,
            CollisionVolume.FlipInfo attackFlip,
            CollisionVolume.FlipInfo vulnerabilityFlip) {
        int intersectionType = HitType.INVALID;
        if (attackVolumes != null && vulnerabilityVolumes != null) {
            final int attackCount = attackVolumes.getCount();
            for (int x = 0; x < attackCount && intersectionType == HitType.INVALID; x++) {
                final CollisionVolume attackVolume = attackVolumes.get(x);
                final int hitType = attackVolume.getHitType();
                if (hitType != HitType.INVALID) {
                    final int vulnerabilityCount = vulnerabilityVolumes.getCount();
                    for (int y = 0; y < vulnerabilityCount; y++) {
                        final CollisionVolume vulnerabilityVolume = vulnerabilityVolumes.get(y);
                        final int vulnerableType = vulnerabilityVolume.getHitType();
                        if (vulnerableType == HitType.INVALID || vulnerableType == hitType) {
                            if (attackVolume.intersects(attackPosition, attackFlip,
                                    vulnerabilityVolume, vulnerabilityPosition, 
                                    vulnerabilityFlip)) {
                                intersectionType = hitType;
                                break;
                            }
                        }
                    }
                }
            }
        }
        
        return intersectionType;
    }
    
    private final void drawDebugVolumes(CollisionVolumeRecord record) {
    	final Vector2 position = record.object.getPosition();
    	if (mDrawDebugBoundingVolume) {
	    	final CollisionVolume boundingVolume = record.boundingVolume;
	    	sSystemRegistry.debugSystem.drawShape(
	    			position.x + boundingVolume.getMinXPosition(sFlip), position.y + boundingVolume.getMinYPosition(sFlip), 
	    			boundingVolume.getMaxX() - boundingVolume.getMinX(), 
	    			boundingVolume.getMaxY() - boundingVolume.getMinY(), 
	    			DebugSystem.SHAPE_CIRCLE,
	    			DebugSystem.COLOR_OUTLINE);
    	}
    	if (mDrawDebugCollisionVolumes) {
	    	if (record.attackVolumes != null) {
	    		final int attackVolumeCount = record.attackVolumes.getCount();
	    		for (int y = 0; y < attackVolumeCount; y++) {
	    			CollisionVolume volume = record.attackVolumes.get(y);
	    			sSystemRegistry.debugSystem.drawShape(
	    					position.x + volume.getMinXPosition(sFlip), position.y + volume.getMinYPosition(sFlip), 
	    					volume.getMaxX() - volume.getMinX(), 
	    					volume.getMaxY() - volume.getMinY(), 
	    	    			volume.getClass() == AABoxCollisionVolume.class ? DebugSystem.SHAPE_BOX : DebugSystem.SHAPE_CIRCLE,
	    	    			DebugSystem.COLOR_RED);
	    		}
	    	}
	    	
	    	if (record.vulnerabilityVolumes != null) {
	    		final int vulnVolumeCount = record.vulnerabilityVolumes.getCount();
	    		for (int y = 0; y < vulnVolumeCount; y++) {
	    			CollisionVolume volume = record.vulnerabilityVolumes.get(y);
	    			sSystemRegistry.debugSystem.drawShape(
	    					position.x + volume.getMinXPosition(sFlip), position.y + volume.getMinYPosition(sFlip), 
	    					volume.getMaxX() - volume.getMinX(), 
	    					volume.getMaxY() - volume.getMinY(), 
	    	    			volume.getClass() == AABoxCollisionVolume.class ? DebugSystem.SHAPE_BOX : DebugSystem.SHAPE_CIRCLE,
	    	    			DebugSystem.COLOR_BLUE);
	    		}
	    	}
    	}
    }
    
    public void setDebugPrefs(boolean drawBoundingVolumes, boolean drawCollisionVolumes) {
		mDrawDebugBoundingVolume = drawBoundingVolumes;
		mDrawDebugCollisionVolumes = drawCollisionVolumes;
	}
    
    /** A record of a single game object and its associated collision info.  */
    private class CollisionVolumeRecord extends AllocationGuard {
        public GameObject object;
        public HitReactionComponent reactionComponent;
        public CollisionVolume boundingVolume;
        public FixedSizeArray<CollisionVolume> attackVolumes;
        public FixedSizeArray<CollisionVolume> vulnerabilityVolumes;
        
        public void reset() {
            object = null;
            attackVolumes = null;
            vulnerabilityVolumes = null;
            boundingVolume = null;
            reactionComponent = null;
        }
    }
    
    /** A pool of collision volume records.  */
    private class CollisionVolumeRecordPool extends TObjectPool<CollisionVolumeRecord> {

        public CollisionVolumeRecordPool(int count) {
            super(count);
        }
        
        @Override
        protected void fill() {
            for (int x = 0; x < getSize(); x++) {
                getAvailable().add(new CollisionVolumeRecord());
            }
        }

        @Override
        public void release(Object entry) {
            ((CollisionVolumeRecord)entry).reset();
            super.release(entry);
        }

    }
    
    /** 
     * Comparator for game objects that considers the world position of the object's bounding
     * volume and sorts objects from left to right on the x axis. */
    public final static class CollisionVolumeComparator implements Comparator<CollisionVolumeRecord> {
        private static CollisionVolume.FlipInfo sCompareFlip = new CollisionVolume.FlipInfo();
        public int compare(CollisionVolumeRecord object1, CollisionVolumeRecord object2) {
            int result = 0;
            if (object1 == null && object2 != null) {
                result = 1;
            } else if (object1 != null && object2 == null) {
                result = -1;
            } else if (object1 != null && object2 != null) {
                sCompareFlip.flipX = (object1.object.facingDirection.x < 0.0f);
                sCompareFlip.flipY = (object1.object.facingDirection.y < 0.0f);
                sCompareFlip.parentWidth = object1.object.width;
                sCompareFlip.parentHeight = object1.object.height;
                
                final float minX1 = object1.object.getPosition().x 
                    + object1.boundingVolume.getMinXPosition(sCompareFlip);
                
                sCompareFlip.flipX = (object2.object.facingDirection.x < 0.0f);
                sCompareFlip.flipY = (object2.object.facingDirection.y < 0.0f);
                sCompareFlip.parentWidth = object2.object.width;
                sCompareFlip.parentHeight = object2.object.height;
                
                final float minX2 = object2.object.getPosition().x 
                    + object2.boundingVolume.getMinXPosition(sCompareFlip);
                
                final float delta = minX1 - minX2;
                if (delta < 0.0f) {
                    result = -1;
                } else if (delta > 0.0f) {
                    result = 1;
                }
            }
            return result;
        }
    }

	
   
}
