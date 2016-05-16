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
 * An Axis-Aligned rectangular collision volume.  This code treats other volumes as if they are
 * also rectangles when calculating intersections.  Therefore certain types of intersections, such
 * as sphere vs rectangle, may not be absolutely precise (in the case of a sphere vs a rectangle,
 * for example, a new rectangle that fits the sphere is used to perform the intersection test, so
 * there is some potential for false-positives at the corners).  However, for our purposes absolute
 * precision isn't necessary, so this simple implementation is sufficient.
 */
public class AABoxCollisionVolume extends CollisionVolume {
    private Vector2 mWidthHeight;
    private Vector2 mBottomLeft;
    
    public AABoxCollisionVolume(float offsetX, float offsetY, float width, float height) {
        super();
        mBottomLeft = new Vector2(offsetX, offsetY);
        mWidthHeight = new Vector2(width, height); 
    }
    
    public AABoxCollisionVolume(float offsetX, float offsetY, float width, float height, 
            int hit) {
        super(hit);
        mBottomLeft = new Vector2(offsetX, offsetY);
        mWidthHeight = new Vector2(width, height);
    }
    
    @Override
    public final float getMaxX() {
        return mBottomLeft.x + mWidthHeight.x;
    }

    @Override
    public final float getMinX() {
        return mBottomLeft.x;
    }
    
    @Override
    public final float getMaxY() {
        return mBottomLeft.y + mWidthHeight.y;
    }

    @Override
    public final float getMinY() {
        return mBottomLeft.y;
    }
    
    /**
     * Calculates the intersection of this volume and another, and returns true if the
     * volumes intersect.  This test treats the other volume as an AABox.
     * @param position The world position of this volume.
     * @param other The volume to test for intersections.
     * @param otherPosition The world position of the other volume.
     * @return true if the volumes overlap, false otherwise.
     */
    @Override
    public boolean intersects(Vector2 position, FlipInfo flip, CollisionVolume other, 
            Vector2 otherPosition, FlipInfo otherFlip) {
        final float left = getMinXPosition(flip) + position.x;
        final float right = getMaxXPosition(flip) + position.x;
        final float bottom = getMinYPosition(flip) + position.y;
        final float top = getMaxYPosition(flip) + position.y;
        
        final float otherLeft = other.getMinXPosition(otherFlip) + otherPosition.x;
        final float otherRight = other.getMaxXPosition(otherFlip) + otherPosition.x;
        final float otherBottom = other.getMinYPosition(otherFlip) + otherPosition.y;
        final float otherTop = other.getMaxYPosition(otherFlip) + otherPosition.y;
       
        final boolean result = boxIntersect(left, right, top, bottom, 
                    otherLeft, otherRight, otherTop, otherBottom) 
                || boxIntersect(otherLeft, otherRight, otherTop, otherBottom, 
                    left, right, top, bottom);
        
        return result;
    }
    
    /** Tests two axis-aligned boxes for overlap. */
    private boolean boxIntersect(float left1, float right1, float top1, float bottom1, 
            float left2, float right2, float top2, float bottom2) {
        final boolean horizontalIntersection = left1 < right2 && left2 < right1;
        final boolean verticalIntersection = top1 > bottom2 && top2 > bottom1;
        final boolean intersecting = horizontalIntersection && verticalIntersection;
        return intersecting;
    }
    
    /** Increases the size of this volume as necessary to fit the passed volume. */
    public void growBy(CollisionVolume other) {
        final float maxX;
        final float minX;
        
        final float maxY;
        final float minY;
        
        if (mWidthHeight.length2() > 0) {
            maxX = Math.max(getMaxX(), other.getMaxX());
            minX = Math.max(getMinX(), other.getMinX());
            maxY = Math.max(getMaxY(), other.getMaxY());
            minY = Math.max(getMinY(), other.getMinY());
        } else {
            maxX = other.getMaxX();
            minX = other.getMinX();
            maxY = other.getMaxY();
            minY = other.getMinY();
        }
        final float horizontalDelta = maxX - minX;
        final float verticalDelta = maxY - minY;
        mBottomLeft.set(minX, minY);
        mWidthHeight.set(horizontalDelta, verticalDelta);
    }

}
