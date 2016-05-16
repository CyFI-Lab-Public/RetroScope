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
 * A single animation frame.  Frames contain a texture, a hold time, and collision volumes to
 * use for "attacking" or "vulnerability."  This allows animated sprites to cheaply interact with
 * other objects in the game world by associating collision information with particular animation
 * frames.  Note that an animation frame may have a null texture and null collision volumes.  Null
 * collision volumes will exclude that frame from collision detection and a null texture will
 * prevent the sprite from drawing.
 */
public class AnimationFrame extends AllocationGuard {
    public Texture texture;
    public float holdTime;
    FixedSizeArray<CollisionVolume> attackVolumes;
    FixedSizeArray<CollisionVolume> vulnerabilityVolumes;
    
    public AnimationFrame(Texture textureObject, float animationHoldTime) {
        super();
        texture = textureObject;
        holdTime = animationHoldTime;
    }
    
    public AnimationFrame(Texture textureObject, float animationHoldTime, 
            FixedSizeArray<CollisionVolume> attackVolumeList,
            FixedSizeArray<CollisionVolume> vulnerabilityVolumeList) {
        super();
        texture = textureObject;
        holdTime = animationHoldTime;
        attackVolumes = attackVolumeList;
        vulnerabilityVolumes = vulnerabilityVolumeList;
    }
}
