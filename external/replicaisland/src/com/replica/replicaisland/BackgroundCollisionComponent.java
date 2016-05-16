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

/**
 * Handles collision against the background.  Snaps colliding objects out of collision and reports
 * the hit to the parent game object.
 */
public class BackgroundCollisionComponent extends GameComponent {
    private Vector2 mPreviousPosition;
    private int mWidth;
    private int mHeight;
    private int mHorizontalOffset;
    private int mVerticalOffset;
    
    // Workspace vectors.  Allocated up front for speed.
    private Vector2 mCurrentPosition;
    private Vector2 mPreviousCenter;
    private Vector2 mDelta;
    private Vector2 mFilterDirection;
    private Vector2 mHorizontalHitPoint;
    private Vector2 mHorizontalHitNormal;
    private Vector2 mVerticalHitPoint;
    private Vector2 mVerticalHitNormal;
    private Vector2 mRayStart;
    private Vector2 mRayEnd;
    private Vector2 mTestPointStart;
    private Vector2 mTestPointEnd;
    private Vector2 mMergedNormal;
    
    /**
     * Sets up the collision bounding box.  This box may be a different size than the bounds of the
     * sprite that this object controls.
     * @param width  The width of the collision box.
     * @param height  The height of the collision box.
     * @param horzOffset  The offset of the collision box from the object's origin in the x axis.
     * @param vertOffset  The offset of the collision box from the object's origin in the y axis.
     */
    public BackgroundCollisionComponent(int width, int height, int horzOffset, int vertOffset) {
        super();
        setPhase(ComponentPhases.COLLISION_RESPONSE.ordinal());
        mPreviousPosition = new Vector2();
        mWidth = width;
        mHeight = height;
        mHorizontalOffset = horzOffset;
        mVerticalOffset = vertOffset;
        
        mCurrentPosition = new Vector2();
        mPreviousCenter = new Vector2();
        mDelta = new Vector2();
        mFilterDirection = new Vector2();
        mHorizontalHitPoint = new Vector2();
        mHorizontalHitNormal = new Vector2();
        mVerticalHitPoint = new Vector2();
        mVerticalHitNormal = new Vector2();
        mRayStart = new Vector2();
        mRayEnd = new Vector2();
        mTestPointStart = new Vector2();
        mTestPointEnd = new Vector2();
        mMergedNormal = new Vector2();
    }
    
    public BackgroundCollisionComponent() {
        super();
        setPhase(ComponentPhases.COLLISION_RESPONSE.ordinal());
        mPreviousPosition = new Vector2();
        mCurrentPosition = new Vector2();
        mPreviousCenter = new Vector2();
        mDelta = new Vector2();
        mFilterDirection = new Vector2();
        mHorizontalHitPoint = new Vector2();
        mHorizontalHitNormal = new Vector2();
        mVerticalHitPoint = new Vector2();
        mVerticalHitNormal = new Vector2();
        mRayStart = new Vector2();
        mRayEnd = new Vector2();
        mTestPointStart = new Vector2();
        mTestPointEnd = new Vector2();
        mMergedNormal = new Vector2();
    }
    
    @Override
    public void reset() {
        mPreviousPosition.zero();
    }
    
    public void setSize(int width, int height) {
        mWidth = width;
        mHeight = height;
        // TODO: Resize might cause new collisions.
    }
    
    public void setOffset(int horzOffset, int vertOffset) {
        mHorizontalOffset = horzOffset;
        mVerticalOffset = vertOffset;
    }

    /**
     * This function is the meat of the collision response logic.  Our collision detection and
     * response must be capable of dealing with arbitrary surfaces and must be frame rate
     * independent (we must sweep the space in-between frames to find collisions reliably).  The
     * following algorithm is used to keep the collision box out of the collision world.
     *      1.  Cast a ray from the center point of the box at its position last frame to the edge
     *          of the box at its current position.  If the ray intersects anything, snap the box
     *          back to the point of intersection.  
     *      2.  Perform Step 1 twice: once looking for surfaces opposing horizontal movement and
     *          again for surfaces opposing vertical movement.  These two ray tests approximate the
     *          movement of the box between the previous frame and this one.
     *      3.  Since most collisions are collisions with the ground, more precision is required for
     *          vertical intersections.  Perform another ray test, this time from the top of the
     *          box's position (after snapping in Step 2) to the bottom.  Snap out of any vertical
     *          surfaces that the ray encounters.  This will ensure consistent snapping behavior on
     *          incline surfaces.
     *      4.  Add the normals of the surfaces that were hit up and normalize the result to produce
     *          a direction describing the average slope of the surfaces that the box is resting on.
     *          Physics will use this value as a normal to resolve collisions with the background.
     */
    @Override
    public void update(float timeDelta, BaseObject parent) {
        GameObject parentObject = (GameObject) parent;
        parentObject.setBackgroundCollisionNormal(Vector2.ZERO);
        if (mPreviousPosition.length2() != 0) {
            CollisionSystem collision = sSystemRegistry.collisionSystem;
            if (collision != null) {
                final int left = mHorizontalOffset;
                final int bottom = mVerticalOffset;
                final int right = left + mWidth;
                final int top = bottom + mHeight;
                final float centerOffsetX = ((mWidth) / 2.0f) + left;
                final float centerOffsetY = ((mHeight) / 2.0f) + bottom;

                mCurrentPosition.set(parentObject.getPosition());
                mDelta.set(mCurrentPosition);
                mDelta.subtract(mPreviousPosition);
                
                mPreviousCenter.set(centerOffsetX, centerOffsetY);
                mPreviousCenter.add(mPreviousPosition);
                
                boolean horizontalHit = false;
                boolean verticalHit = false;
                
                mVerticalHitPoint.zero();
                mVerticalHitNormal.zero();
                mHorizontalHitPoint.zero();
                mHorizontalHitNormal.zero();
              
               
                // The order in which we sweep the horizontal and vertical space can affect the
                // final result because we perform incremental snapping mid-sweep.  So it is
                // necessary to sweep in the primary direction of movement first.
                if (Math.abs(mDelta.x) > Math.abs(mDelta.y)) {
                    horizontalHit = sweepHorizontal(mPreviousCenter, mCurrentPosition, mDelta, left, 
                            right, centerOffsetY, mHorizontalHitPoint, mHorizontalHitNormal,
                            parentObject);
                    verticalHit = sweepVertical(mPreviousCenter, mCurrentPosition, mDelta, bottom, 
                            top, centerOffsetX, mVerticalHitPoint, mVerticalHitNormal,
                            parentObject);
                    
                } else {
                    verticalHit = sweepVertical(mPreviousCenter, mCurrentPosition, mDelta, bottom, 
                            top, centerOffsetX, mVerticalHitPoint, mVerticalHitNormal,
                            parentObject);
                    horizontalHit = sweepHorizontal(mPreviousCenter, mCurrentPosition, mDelta, left, 
                            right, centerOffsetY, mHorizontalHitPoint, mHorizontalHitNormal,
                            parentObject);
                }
                
                // force the collision volume to stay within the bounds of the world.
                LevelSystem level = sSystemRegistry.levelSystem;
                if (level != null) {
                    if (mCurrentPosition.x + left < 0.0f) {
                        mCurrentPosition.x = (-left + 1);
                        horizontalHit = true;
                        mHorizontalHitNormal.x = (mHorizontalHitNormal.x + 1.0f);
                        mHorizontalHitNormal.normalize();
                    } else if (mCurrentPosition.x + right > level.getLevelWidth()) {
                        mCurrentPosition.x = (level.getLevelWidth() - right - 1);
                        mHorizontalHitNormal.x = (mHorizontalHitNormal.x - 1.0f);
                        mHorizontalHitNormal.normalize();
                        horizontalHit = true;
                    }
                    
                    /*if (mCurrentPosition.y + bottom < 0.0f) {
                        mCurrentPosition.y = (-bottom + 1);
                        verticalHit = true;
                        mVerticalHitNormal.y = (mVerticalHitNormal.y + 1.0f);
                        mVerticalHitNormal.normalize();
                    } else*/ if (mCurrentPosition.y + top > level.getLevelHeight()) {
                        mCurrentPosition.y = (level.getLevelHeight() - top - 1);
                        mVerticalHitNormal.y = (mVerticalHitNormal.y - 1.0f);
                        mVerticalHitNormal.normalize();
                        verticalHit = true;
                    }
                    
                }
                
                
                // One more set of tests to make sure that we are aligned with the surface. 
                // This time we will just check the inside of the bounding box for intersections.
                // The sweep tests above will keep us out of collision in most cases, but this 
                // test will ensure that we are aligned to incline surfaces correctly. 
                
                // Shoot a vertical line through the middle of the box.
                if (mDelta.x != 0.0f && mDelta.y != 0.0f) {
                    float yStart = top;
                    float yEnd = bottom;
                    
                  
                    mRayStart.set(centerOffsetX, yStart);
                    mRayStart.add(mCurrentPosition);
                    
                    mRayEnd.set(centerOffsetX, yEnd);
                    mRayEnd.add(mCurrentPosition);
                    
                    mFilterDirection.set(mDelta);
                    
                    if (collision.castRay(mRayStart, mRayEnd, mFilterDirection, mVerticalHitPoint,
                            mVerticalHitNormal, parentObject)) {
                        
                        // If we found a collision, use this surface as our vertical intersection
                        // for this frame, even if the sweep above also found something.
                        verticalHit = true;
                        // snap
                        if (mVerticalHitNormal.y > 0.0f) {
                            mCurrentPosition.y = (mVerticalHitPoint.y - bottom);
                        } else if (mVerticalHitNormal.y < 0.0f) {
                            mCurrentPosition.y = (mVerticalHitPoint.y - top);
                        }
                    }
                    
                    
                    // Now the horizontal version of the same test
                    float xStart = left;
                    float xEnd = right;
                    if (mDelta.x < 0.0f) {
                    	xStart = right;
                    	xEnd = left;
                    }
                    
                  
                    mRayStart.set(xStart, centerOffsetY);
                    mRayStart.add(mCurrentPosition);
                    
                    mRayEnd.set(xEnd, centerOffsetY);
                    mRayEnd.add(mCurrentPosition);
                    
                    mFilterDirection.set(mDelta);
                    
                    if (collision.castRay(mRayStart, mRayEnd, mFilterDirection, mHorizontalHitPoint,
                            mHorizontalHitNormal, parentObject)) {
                        
                        // If we found a collision, use this surface as our horizontal intersection
                        // for this frame, even if the sweep above also found something.
                        horizontalHit = true;
                        // snap
                        if (mHorizontalHitNormal.x > 0.0f) {
                            mCurrentPosition.x = (mHorizontalHitPoint.x - left);
                        } else if (mHorizontalHitNormal.x < 0.0f) {
                            mCurrentPosition.x = (mHorizontalHitPoint.x - right);
                        }
                    }
                }
           
                
                // Record the intersection for other systems to use.
                final TimeSystem timeSystem = sSystemRegistry.timeSystem;

                if (timeSystem != null) {
                    float time = timeSystem.getGameTime();
                    if (horizontalHit) {
                        if (mHorizontalHitNormal.x > 0.0f) {
                            parentObject.setLastTouchedLeftWallTime(time);
                        } else {
                            parentObject.setLastTouchedRightWallTime(time);
                        }
                        //parentObject.setBackgroundCollisionNormal(mHorizontalHitNormal);
                    }
                    
                    if (verticalHit) {
                        if (mVerticalHitNormal.y > 0.0f) {
                            parentObject.setLastTouchedFloorTime(time);
                        } else {
                            parentObject.setLastTouchedCeilingTime(time);
                        }
                        //parentObject.setBackgroundCollisionNormal(mVerticalHitNormal);
                    }
                    
                   
                    // If we hit multiple surfaces, merge their normals together to produce an
                    // average direction of obstruction.
                    if (true) { //(verticalHit && horizontalHit) {
                        mMergedNormal.set(mVerticalHitNormal);
                        mMergedNormal.add(mHorizontalHitNormal);
                        mMergedNormal.normalize();
                        parentObject.setBackgroundCollisionNormal(mMergedNormal);
                    }

                    parentObject.setPosition(mCurrentPosition);
                }
               
            }
        }
        mPreviousPosition.set(parentObject.getPosition());
    }
    
    /* Sweeps the space between two points looking for surfaces that oppose horizontal movement. */
    protected boolean sweepHorizontal(Vector2 previousPosition, Vector2 currentPosition, Vector2 delta, 
            int left, int right, float centerY, Vector2 hitPoint, Vector2 hitNormal, 
            GameObject parentObject) {
        boolean hit = false;
        if (!Utils.close(delta.x, 0.0f)) {
            CollisionSystem collision = sSystemRegistry.collisionSystem;
            
            // Shoot a ray from the center of the previous frame's box to the edge (left or right,
            // depending on the direction of movement) of the current box.
            mTestPointStart.y = (centerY);
            mTestPointStart.x = (left);
            int offset = -left;
            if (delta.x > 0.0f) {
                mTestPointStart.x = (right);
                offset = -right;
            }
            
            // Filter out surfaces that do not oppose motion in the horizontal direction, or
            // push in the same direction as movement.
            mFilterDirection.set(delta);
            mFilterDirection.y = (0);
            
            mTestPointEnd.set(currentPosition);
            mTestPointEnd.add(mTestPointStart);
            if (collision.castRay(previousPosition, mTestPointEnd, mFilterDirection,
                    hitPoint, hitNormal, parentObject)) {
                // snap
                currentPosition.x = (hitPoint.x + offset);
                hit = true;
            }
        }
        return hit;
    }
    
    /* Sweeps the space between two points looking for surfaces that oppose vertical movement. */
    protected boolean sweepVertical(Vector2 previousPosition, Vector2 currentPosition, Vector2 delta, 
            int bottom, int top, float centerX, Vector2 hitPoint, Vector2 hitNormal,
            GameObject parentObject) {
        boolean hit = false;
        if (!Utils.close(delta.y, 0.0f)) {
            CollisionSystem collision = sSystemRegistry.collisionSystem;            
            // Shoot a ray from the center of the previous frame's box to the edge (top or bottom,
            // depending on the direction of movement) of the current box.
            mTestPointStart.x = (centerX);
            mTestPointStart.y = (bottom);
            int offset = -bottom;
            if (delta.y > 0.0f) {
                mTestPointStart.y = (top);
                offset = -top;
            }
            
            mFilterDirection.set(delta);
            mFilterDirection.x = (0);
            
            mTestPointEnd.set(currentPosition);
            mTestPointEnd.add(mTestPointStart);
            if (collision.castRay(previousPosition, mTestPointEnd, mFilterDirection,
                    hitPoint, hitNormal, parentObject)) {
                hit = true;
                // snap
                currentPosition.y = (hitPoint.y + offset);
            }
         
        }
        return hit;
    }
    
    /** Comparator for hit points. */
    private static class HitPointDistanceComparator implements Comparator<HitPoint>  {
        private Vector2 mOrigin;
        
        public HitPointDistanceComparator() {
            super();
            mOrigin = new Vector2();
        }
        
        public final void setOrigin(Vector2 origin) {
            mOrigin.set(origin);
        }
        
        public final void setOrigin(float x, float y) {
            mOrigin.set(x, y);
        }
        
        public int compare(HitPoint object1, HitPoint object2) {
            int result = 0;
            if (object1 != null && object2 != null) {
                final float obj1Distance = object1.hitPoint.distance2(mOrigin);
                final float obj2Distance = object2.hitPoint.distance2(mOrigin);
                final float distanceDelta = obj1Distance - obj2Distance;
                result = distanceDelta < 0.0f ? -1 : 1;
            } else if (object1 == null && object2 != null) {
                result = 1;
            } else if (object2 == null && object1 != null) {
                result = -1;
            } 
            return result;
        }
    }
}
