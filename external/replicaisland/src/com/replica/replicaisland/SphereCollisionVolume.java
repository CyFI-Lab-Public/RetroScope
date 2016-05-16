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

/** A sphere collision volume. */
public class SphereCollisionVolume extends CollisionVolume {
    private float mRadius;
    private Vector2 mCenter;
    private Vector2 mWorkspaceVector;
    private Vector2 mWorkspaceVector2;
    
    public SphereCollisionVolume(float radius, float centerX, float centerY) {
        super();
        mRadius = radius;
        mCenter = new Vector2(centerX, centerY);
        mWorkspaceVector = new Vector2();
        mWorkspaceVector2 = new Vector2();
    }
    
    public SphereCollisionVolume(float radius, float centerX, float centerY, int hit) {
        super(hit);
        mRadius = radius;
        mCenter = new Vector2(centerX, centerY);
        mWorkspaceVector = new Vector2();
        mWorkspaceVector2 = new Vector2();
    }
    
    @Override
    public float getMaxX() {
        return mCenter.x + mRadius;
    }

    @Override
    public float getMinX() {
        return mCenter.x - mRadius;
    }
    
    @Override
    public float getMaxY() {
        return mCenter.y + mRadius;
    }

    @Override
    public float getMinY() {
        return mCenter.y - mRadius;
    }

    public Vector2 getCenter() {
        return mCenter;
    }
    
    public void setCenter(Vector2 center) {
        mCenter.set(center);
    }
    
    public float getRadius() {
        return mRadius;
    }
    
    public void setRadius(float radius) {
        mRadius = radius;
    }
    
    public void reset() {
        mCenter.zero();
        mRadius = 0;
    }
    
    @Override
    public boolean intersects(Vector2 position, FlipInfo flip, CollisionVolume other, 
            Vector2 otherPosition, FlipInfo otherFlip) {
        boolean result = false;
        
        if (other instanceof AABoxCollisionVolume) {
            // It's more accurate to do a sphere-as-box test than a box-as-sphere test.
            result = other.intersects(otherPosition, otherFlip, this, position, flip);
        } else {
            mWorkspaceVector.set(position);
            offsetByCenter(mWorkspaceVector, mCenter, flip);
            
            float otherRadius = 0;
            if (other instanceof SphereCollisionVolume) {
                SphereCollisionVolume sphereOther = (SphereCollisionVolume)other;
                mWorkspaceVector2.set(otherPosition);
                offsetByCenter(mWorkspaceVector2, sphereOther.getCenter(), otherFlip);
                mWorkspaceVector.subtract(mWorkspaceVector2);
                otherRadius = sphereOther.getRadius();
            } else {
                // Whatever this volume is, pretend it's a sphere.
                final float deltaX = other.getMaxXPosition(otherFlip) 
                    - other.getMinXPosition(otherFlip);
                final float deltaY = other.getMaxYPosition(otherFlip) 
                    - other.getMinYPosition(otherFlip);
                final float centerX = deltaX / 2.0f;
                final float centerY = deltaY / 2.0f;
                
                mWorkspaceVector2.set(otherPosition);
                mWorkspaceVector2.x += centerX;
                mWorkspaceVector2.y += centerY;
                otherRadius = Math.max(deltaX, deltaY);
            }
            
            final float maxDistance = mRadius + otherRadius;
            final float distance2 = mWorkspaceVector.length2();
            final float maxDistance2 = (maxDistance * maxDistance);
            if (distance2 < maxDistance2) {
                result = true;
            }
        }
        
        return result;
    }
    
    public void growBy(CollisionVolume other) {
        final float maxX;
        final float minX;
        
        final float maxY;
        final float minY;
        
        if (mRadius > 0) {
            maxX = Math.max(getMaxX(), other.getMaxX());
            minX = Math.min(getMinX(), other.getMinX());
            maxY = Math.max(getMaxY(), other.getMaxY());
            minY = Math.min(getMinY(), other.getMinY());
        } else {
            maxX = other.getMaxX();
            minX = other.getMinX();
            maxY = other.getMaxY();
            minY = other.getMinY();
        }
        final float horizontalDelta = maxX - minX;
        final float verticalDelta = maxY - minY;
        final float diameter = Math.max(horizontalDelta, verticalDelta);
        
        final float newCenterX = minX + (horizontalDelta / 2.0f);
        final float newCenterY = minY + (verticalDelta / 2.0f);
        final float newRadius = diameter / 2.0f;
        
        mCenter.set(newCenterX, newCenterY);
        mRadius = newRadius;
    }
    
    private static void offsetByCenter(Vector2 position, Vector2 center, FlipInfo flip) {
        if (flip != null && (flip.flipX || flip.flipY)) {
            if (flip.flipX) {
                position.x += flip.parentWidth - center.x;
            } else {
                position.x += center.x;
            }
            
            if (flip.flipY) {
                position.y += flip.parentHeight - center.y;
            } else {
                position.y += center.y;
            }
        } else {
            position.add(center);
        }
    }

}
