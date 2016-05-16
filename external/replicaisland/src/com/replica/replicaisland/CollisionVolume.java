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
 * CollisionVolume describes a volume (rectangle, sphere, etc) used for dynamic collision detection.
 * Volumes can be tested for intersection against other volumes, and can be grown to contain a set
 * of other volumes.  The volume itself is stored in object-relative space (in terms of offsets from
 * some origin); when used with game objects the position of the parent object must be passed to
 * a parameter of the intersection test.  This means that a single instance of a CollisionVolume and
 * its derivatives is safe to share amongst many game object instances.
 */
public abstract class CollisionVolume extends AllocationGuard {
    // TODO: does this really belong here?
    // When used as an attack volume, mHitType specifies the type of hit that the volume deals.
    // When used as a vulnerability volume, it specifies which type the volume is vulernable to
    // (invalid = all types).
    public int mHitType;
    
    public CollisionVolume() {
        super();
        mHitType = HitType.INVALID;
    }
    
    public CollisionVolume(int type) {
        super();
        mHitType = type;
    }
    
    public void setHitType(int type) {
        mHitType = type;
    }
    
    public int getHitType() {
        return mHitType;
    }
    
    
    public abstract boolean intersects(Vector2 position, FlipInfo flip, CollisionVolume other, 
            Vector2 otherPosition, FlipInfo otherFlip);
    
    public float getMinXPosition(FlipInfo flip) {
        float value = 0;
        if (flip != null && flip.flipX) {
            final float maxX = getMaxX();
            value = flip.parentWidth - maxX;
        } else {
            value = getMinX();
        }
        return value;
    }
    
    public float getMaxXPosition(FlipInfo flip) {
        float value = 0;
        if (flip != null && flip.flipX) {
            final float minX = getMinX();
            value = flip.parentWidth - minX;
        } else {
            value = getMaxX();
        }
        return value;
    }
    
    public float getMinYPosition(FlipInfo flip) {
        float value = 0;
        if (flip != null && flip.flipY) {
            final float maxY = getMaxY();
            value = flip.parentHeight - maxY;
        } else {
            value = getMinY();
        }
        return value;
    }
    
    public float getMaxYPosition(FlipInfo flip) {
        float value = 0;
        if (flip != null && flip.flipY) {
            final float minY = getMinY();
            value = flip.parentHeight - minY;
        } else {
            value = getMaxY();
        }
        return value;
    }
    
    protected abstract float getMinX();
    protected abstract float getMaxX();
    protected abstract float getMinY();
    protected abstract float getMaxY();
    

    public static class FlipInfo {
        public boolean flipX;
        public boolean flipY;
        public float parentWidth;
        public float parentHeight;
    }
}
