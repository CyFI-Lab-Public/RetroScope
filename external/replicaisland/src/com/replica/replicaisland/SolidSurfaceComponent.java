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
 * A component that allows a game object to act like a solid object by submitting surfaces to the
 * background collision system every frame.
 */
public class SolidSurfaceComponent extends GameComponent {
    private FixedSizeArray<Vector2> mStartPoints;
    private FixedSizeArray<Vector2> mEndPoints;
    private FixedSizeArray<Vector2> mNormals;
    private Vector2 mStart;
    private Vector2 mEnd;
    private Vector2 mNormal;
    
    public SolidSurfaceComponent(int maxSurfaceCount) {
        super();
        
        inititalize(maxSurfaceCount);
        
        mStart = new Vector2();
        mEnd = new Vector2();
        mNormal = new Vector2();
        
        setPhase(ComponentPhases.POST_COLLISION.ordinal());
        reset();
    }
    
    @Override
    public void reset() {
        mStartPoints.clear();
        mEndPoints.clear();
        mNormals.clear();
    }
    
    public SolidSurfaceComponent() {
        super();

        mStart = new Vector2();
        mEnd = new Vector2();
        mNormal = new Vector2();
        
        setPhase(ComponentPhases.POST_COLLISION.ordinal());
    }
    
    public void inititalize(int maxSurfaceCount) {
        if (mStartPoints == null 
                || (mStartPoints != null && mStartPoints.getCount() != maxSurfaceCount)) {
            mStartPoints = new FixedSizeArray<Vector2>(maxSurfaceCount);
            mEndPoints = new FixedSizeArray<Vector2>(maxSurfaceCount);
            mNormals = new FixedSizeArray<Vector2>(maxSurfaceCount);
        }
        
        mStartPoints.clear();
        mEndPoints.clear();
        mNormals.clear();
    }
    
    // Note that this function keeps direct references to the arguments it is passed.
    public void addSurface(Vector2 startPoint, Vector2 endPoint, Vector2 normal) {
        mStartPoints.add(startPoint);
        mEndPoints.add(endPoint);
        mNormals.add(normal);
    }

    @Override
    public void update(float timeDelta, BaseObject parent) {
        CollisionSystem collision = sSystemRegistry.collisionSystem;
        
        final FixedSizeArray<Vector2> startPoints = mStartPoints;
        final FixedSizeArray<Vector2> endPoints = mEndPoints;
        final FixedSizeArray<Vector2> normals = mNormals;
        
        final int surfaceCount = startPoints.getCount();
        if (collision != null && surfaceCount > 0) {
            GameObject parentObject = (GameObject)parent;
            final Vector2 position = parentObject.getPosition();
            Vector2 start = mStart;
            Vector2 end = mEnd;
            Vector2 normal = mNormal;
            
            for (int x = 0; x < surfaceCount; x++) {
               start.set(startPoints.get(x));
               if (parentObject.facingDirection.x < 0.0f) {
                   start.flipHorizontal(parentObject.width); 
               }
               
               if (parentObject.facingDirection.y < 0.0f) {
                   start.flipVertical(parentObject.height); 
               }
               start.add(position);
               
               end.set(endPoints.get(x));
               if (parentObject.facingDirection.x < 0.0f) {
                   end.flipHorizontal(parentObject.width); 
               }
               
               if (parentObject.facingDirection.y < 0.0f) {
                   end.flipVertical(parentObject.height); 
               }
               end.add(position);
                
               normal.set(normals.get(x));
               if (parentObject.facingDirection.x < 0.0f) {
                   normal.flipHorizontal(0); 
               }
               
               if (parentObject.facingDirection.y < 0.0f) {
                   normal.flipVertical(0); 
               }
               
               collision.addTemporarySurface(start, end, normal, parentObject);
            }
            
            
        }
    }

}
