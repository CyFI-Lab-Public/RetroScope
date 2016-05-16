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

import android.content.res.AssetManager;

import java.io.IOException;
import java.io.InputStream;

/**
 * Collision detection system.  Provides a ray-based interface for finding surfaces in the collision
 * world.   This version is based on a collision world of line segments, organized into an array of
 * tiles.  The underlying detection algorithm isn't relevant to calling code, however, so this class
 * may be extended to provide a completely different collision detection scheme.  
 * 
 * This class also provides a system for runtime-generated collision segments.  These temporary
 * segments are cleared each frame, and consequently must be constantly re-submitted if they are
 * intended to persist.  Temporary segments are useful for dynamic solid objects, such as moving
 * platforms.
 * 
 * CollisionSystem.TileVisitor is an interface for traversing individual collision tiles.  Ray casts
 * can be used to run user code over the collision world by passing different TileVisitor
 * implementations to executeRay.  Provided is TileTestVisitor, a visitor that compares the segments
 * of each tile visited with the ray and searches for points of intersection.
 *
 */
public class CollisionSystem extends BaseObject {
    private TiledWorld mWorld;
    private CollisionTile[] mCollisionTiles;
    private LineSegmentPool mSegmentPool;
    private int mTileWidth;
    private int mTileHeight;
    private TileTestVisitor mTileSegmentTester;
    private FixedSizeArray<LineSegment> mTemporarySegments;
    private FixedSizeArray<LineSegment> mPendingTemporarySegments;
    private byte[] mWorkspaceBytes;     // Included here to avoid runtime allocation during file io.
    
    private static final int MAX_TEMPORARY_SEGMENTS = 256;

    public CollisionSystem() {
        super();
        mTileSegmentTester = new TileTestVisitor();
        mSegmentPool = new LineSegmentPool(MAX_TEMPORARY_SEGMENTS);
        
        mTemporarySegments = new FixedSizeArray<LineSegment>(MAX_TEMPORARY_SEGMENTS);
        mPendingTemporarySegments = new FixedSizeArray<LineSegment>(MAX_TEMPORARY_SEGMENTS);
        
        mWorkspaceBytes = new byte[4];
    }
    
    @Override
    public void reset() {
        mWorld = null;
        mCollisionTiles = null;
        
        final int count = mTemporarySegments.getCount();
        for (int x = 0; x < count; x++) {
            mSegmentPool.release(mTemporarySegments.get(x));
            mTemporarySegments.set(x, null);
        }
        mTemporarySegments.clear();
        
        final int pendingCount = mPendingTemporarySegments.getCount();
        for (int x = 0; x < pendingCount; x++) {
            mSegmentPool.release(mPendingTemporarySegments.get(x));
            mPendingTemporarySegments.set(x, null);
        }
        mPendingTemporarySegments.clear();
    }
    
    /* Sets the current collision world to the supplied tile world. */
    public void initialize(TiledWorld world, int tileWidth, int tileHeight) {
        mWorld = world;
        
        mTileWidth = tileWidth;
        mTileHeight = tileHeight;
    }
    
    /**
     * Casts a ray into the collision world.  The ray is bound by the start and end points supplied.
     * The first intersecting segment that is found is returned; in the case where more than one
     * segment is found, the segment closest to the start point is returned.
     * 
     * @param startPoint  The starting point for the ray in world units.
     * @param endPoint  The end point for the ray in world units.
     * @param movementDirection  If set, only segments with normals that oppose this direction will
     *      be counted as valid intersections.  If null, all intersecting segments will be
     *      considered valid.
     * @param hitPoint  The point of intersection between a ray and a surface, if one is found.
     * @param hitNormal  The normal of the intersecting surface if an intersection is found.
     * @param excludeObject If set, dynamic surfaces from this object will be ignored.  
     * @return  true if a valid intersecting surface was found, false otherwise.
     */
    // TODO: switch to return data as a HitPoint.
    public boolean castRay(Vector2 startPoint, Vector2 endPoint, Vector2 movementDirection,
            Vector2 hitPoint, Vector2 hitNormal, GameObject excludeObject) {
        
        boolean hit = false;
        
        mTileSegmentTester.setup(movementDirection, mTileWidth, mTileHeight);
        
        if (mCollisionTiles != null &&
                executeRay(startPoint, endPoint, hitPoint, hitNormal, mTileSegmentTester) != -1) {
            hit = true;
        }
        
        if (mTemporarySegments.getCount() > 0) {
            VectorPool vectorPool = sSystemRegistry.vectorPool;
            Vector2 tempHitPoint = vectorPool.allocate();
            Vector2 tempHitNormal = vectorPool.allocate();
            
            if (testSegmentAgainstList(mTemporarySegments, startPoint, endPoint, tempHitPoint,
                    tempHitNormal, movementDirection, excludeObject)) {
                if (hit) {
                    // Check to see whether this collision is closer to the one we already found or
                    // not.
                    final float firstCollisionDistance = startPoint.distance2(hitPoint);
                    if (firstCollisionDistance > startPoint.distance2(tempHitPoint)) {
                        // The temporary surface is closer.
                        hitPoint.set(tempHitPoint);
                        hitNormal.set(tempHitNormal);
                    }
                } else {
                    hit = true;
                    hitPoint.set(tempHitPoint);
                    hitNormal.set(tempHitNormal);
                }
            }
            
            vectorPool.release(tempHitPoint);
            vectorPool.release(tempHitNormal);
        }
        
        return hit;
    }
    
    public boolean testBox(float left, float right, float top, float bottom, 
            Vector2 movementDirection, FixedSizeArray<HitPoint> hitPoints,
            GameObject excludeObject, boolean testDynamicSurfacesOnly) {
        
        boolean foundHit = false;
        
        // Test against the background.
        if (!testDynamicSurfacesOnly) {
            float startX = left;
            float endX = right;
            float startY = bottom;
            float endY = top;
            int xIncrement = 1;
            int yIncrement = 1;
            
            if (movementDirection != null) {
                if (movementDirection.x < 0.0f) {
                    startX = right;
                    endX = left;
                    xIncrement = -1;
                }
                if (movementDirection.y < 0.0f) {
                    startY = top;
                    endY = bottom;
                    yIncrement = -1;
                }
            }
            final int startTileX = Utils.clamp((int)(startX / mTileWidth), 0, mWorld.getWidth() - 1);
            final int endTileX = Utils.clamp((int)(endX / mTileWidth), 0, mWorld.getWidth() - 1);
            final int startTileY = Utils.clamp((int)(startY / mTileHeight), 0, mWorld.getHeight() - 1);
            final int endTileY = Utils.clamp((int)(endY / mTileHeight), 0, mWorld.getHeight() - 1);
            
            VectorPool vectorPool = sSystemRegistry.vectorPool;
            Vector2 worldTileOffset = vectorPool.allocate();
            
            final int[][] tileArray = mWorld.getTiles();
            final int worldHeight = mWorld.getHeight() - 1;
           
            
            for (int y = startTileY; y != endTileY + yIncrement; y += yIncrement) {
                for (int x = startTileX; x != endTileX + xIncrement; x += xIncrement) {
                    final int tileIndex = tileArray[x][worldHeight - y];
                    if (tileIndex >= 0 && tileIndex < mCollisionTiles.length 
                            && mCollisionTiles[tileIndex] != null) {
                        
                        final float xOffset = x * mTileWidth;
                        final float yOffset = y * mTileHeight;
                        
                        final float tileSpaceLeft = left - xOffset;
                        final float tileSpaceRight = right - xOffset;
                        final float tileSpaceTop = top - yOffset;
                        final float tileSpaceBottom = bottom - yOffset;
                        
                        worldTileOffset.set(xOffset, yOffset);
                        
                        boolean hit = testBoxAgainstList(mCollisionTiles[tileIndex].segments,
                                tileSpaceLeft, tileSpaceRight, tileSpaceTop, tileSpaceBottom,
                                movementDirection, excludeObject, worldTileOffset, hitPoints);
                        
                        if (hit) {
                            foundHit = true;
                        }
                        
                    }
                }
            }
            
            vectorPool.release(worldTileOffset);
        }
        // temporary segments
        boolean tempHit = testBoxAgainstList(mTemporarySegments,
                left, right, top, bottom,
                movementDirection, excludeObject, Vector2.ZERO, hitPoints);
        
        if (tempHit) {
            foundHit = true;
        }
        
        
        
        return foundHit;        
    }
    
    /* Inserts a temporary surface into the collision world.  It will persist for one frame. */
    public void addTemporarySurface(Vector2 startPoint, Vector2 endPoint, Vector2 normal, 
            GameObject ownerObject) {
        LineSegment newSegment = mSegmentPool.allocate();
        
        newSegment.set(startPoint, endPoint, normal);
        newSegment.setOwner(ownerObject);
       
        mPendingTemporarySegments.add(newSegment);
    }
    
    @Override
    public void update(float timeDelta, BaseObject parent) {
        // Clear temporary surfaces
        final int count = mTemporarySegments.getCount();
        if (mCollisionTiles != null && count > 0) {
            for (int x = 0; x < count; x++) {
                mSegmentPool.release(mTemporarySegments.get(x));
                mTemporarySegments.set(x, null);
            }
            mTemporarySegments.clear();
        }
        
        // Temporary surfaces must persist for one frame in order to be reliable independent of
        // frame execution order.  So each frame we queue up inserted segments and then swap them
        // into activity when this system is updated.
        FixedSizeArray<LineSegment> swap = mTemporarySegments;
        mTemporarySegments = mPendingTemporarySegments;
        mPendingTemporarySegments = swap;
    }
    
    /**
     * Shoots a ray through the collision world.  This function is similar to executeRay() below,
     * except that it is optimized for straight lines (either completely horizontal or completely
     * vertical).
     * 
     * @param startPoint  The starting point for the ray, in world space.
     * @param endPoint  The ending point for the ray in world space.
     * @param hitPoint  Set to the intersection coordinates if an intersection is found.
     * @param hitNormal  Set to the normal of the intersecting surface if an intersection is found.
     * @param visitor  Class defining what work to perform at each tile step.
     * @return  The index of the tile that intersected the ray, or -1 if no intersection was found.
     */
    protected int executeStraigtRay(final Vector2 startPoint, final Vector2 endPoint, 
            final int startTileX, final int startTileY, final int endTileX, final int endTileY,
            final int deltaX, final int deltaY,
            Vector2 hitPoint, Vector2 hitNormal, TileVisitor visitor) {
           
        int currentX = startTileX;
        int currentY = startTileY;

        int xIncrement = 0;
        int yIncrement = 0;
        int distance = 0;
        
        if (deltaX != 0) {
            distance = Math.abs(deltaX) + 1;
            xIncrement = Utils.sign(deltaX);
        } else if (deltaY != 0) {
            distance = Math.abs(deltaY) + 1;
            yIncrement = Utils.sign(deltaY);
        }
        
        int hitTile = -1;
        final int worldHeight = mWorld.getHeight() - 1;
        final int[][] tileArray = mWorld.getTiles();
        for (int x = 0; x < distance; x++) {
            final int tileIndex = tileArray[currentX][worldHeight - currentY];
            if (tileIndex >= 0 && tileIndex < mCollisionTiles.length 
                    && mCollisionTiles[tileIndex] != null) {
                if (visitor.visit(mCollisionTiles[tileIndex], startPoint, endPoint, 
                        hitPoint, hitNormal, currentX, currentY)) {
                    hitTile = tileIndex;
                    break;
                }
            }
            currentX += xIncrement;
            currentY += yIncrement;
        }
        
        return hitTile;
    }
    
    /**
     * Shoots a ray through the collision world.  Since the collision world is a 2D array of tiles,
     * this algorithm traces a line in tile space and tests against each non-empty tile it visits.
     * The Bresenham line algorithm is used for the actual traversal, but the action taken at each
     * tile is defined by the visitor class passed to this function.
     * 
     * @param startPoint  The starting point for the ray, in world space.
     * @param endPoint  The ending point for the ray in world space.
     * @param hitPoint  Set to the intersection coordinates if an intersection is found.
     * @param hitNormal  Set to the normal of the intersecting surface if an intersection is found.
     * @param visitor  Class defining what work to perform at each tile step.
     * @return  The index of the tile that intersected the ray, or -1 if no intersection was found.
     */
    protected int executeRay(Vector2 startPoint, Vector2 endPoint, 
            Vector2 hitPoint, Vector2 hitNormal, TileVisitor visitor) {
        
        final int worldHeight = mWorld.getHeight();
        final int worldWidth = mWorld.getWidth();
        
        final int startTileX = worldToTileColumn(startPoint.x, worldWidth);
        final int startTileY = worldToTileRow(startPoint.y, worldHeight);
        
        final int endTileX = worldToTileColumn(endPoint.x, worldWidth);
        final int endTileY = worldToTileRow(endPoint.y, worldHeight);
        
        int currentX = startTileX;
        int currentY = startTileY;
        
        final int deltaX = endTileX - startTileX;
        final int deltaY = endTileY - startTileY;
        
        int hitTile = -1;
        
        if (deltaX == 0 || deltaY == 0) {
            hitTile = executeStraigtRay(startPoint, endPoint, startTileX, startTileY,
                    endTileX, endTileY, deltaX, deltaY, hitPoint, hitNormal, visitor);
        } else {
            
            final int xIncrement = deltaX != 0 ? Utils.sign(deltaX) : 0;
            final int yIncrement = deltaY != 0 ? Utils.sign(deltaY) : 0;
            
            // Note: I'm deviating from the Bresenham algorithm here by adding one to force the end
            // tile to be visited.
            final int lateralDelta = (endTileX > 0 && endTileX < worldWidth - 1) ? Math.abs(deltaX) + 1 : Math.abs(deltaX);
            final int verticalDelta = (endTileY > 0 && endTileY < worldHeight - 1) ? Math.abs(deltaY) + 1 : Math.abs(deltaY);
                
            final int deltaX2 = lateralDelta * 2;
            final int deltaY2 = verticalDelta * 2;
            
            final int worldHeightMinusOne = worldHeight - 1;
            final int[][]tileArray = mWorld.getTiles();
            
            // Bresenham line algorithm in tile space.
            if (lateralDelta >= verticalDelta) {
                int error = deltaY2 - lateralDelta;
                for (int i = 0; i < lateralDelta; i++) {
                    final int tileIndex = tileArray[currentX][worldHeightMinusOne - currentY];
                    if (tileIndex >= 0 && tileIndex < mCollisionTiles.length 
                            && mCollisionTiles[tileIndex] != null) {
                        if (visitor.visit(mCollisionTiles[tileIndex], startPoint, endPoint, 
                                hitPoint, hitNormal, currentX, currentY)) {
                            hitTile = tileIndex;
                            break;
                        }
                    }
    
                    if (error > 0) {
                        currentY += yIncrement;
                        error -= deltaX2;
                    }
                                
                    error += deltaY2;
                    currentX += xIncrement;
                }
            } 
            else if (verticalDelta >= lateralDelta) {
                int error = deltaX2 - verticalDelta;
                        
                for (int i = 0; i < verticalDelta; i++) {
                    final int tileIndex = tileArray[currentX][worldHeightMinusOne - currentY];
                    if (tileIndex >= 0 && tileIndex < mCollisionTiles.length
                            && mCollisionTiles[tileIndex] != null) {
                        if (visitor.visit(mCollisionTiles[tileIndex], startPoint, endPoint, 
                                hitPoint, hitNormal, currentX, currentY)) {
                            hitTile = tileIndex;
                            break;
                        }
                    }
    
                    if (error > 0) {
                        currentX += xIncrement;
                        error -= deltaY2;
                    }
                                
                    error += deltaX2;
                    currentY += yIncrement;
                }
            } 
        }
        return hitTile;
    }
    
    protected final int worldToTileColumn(final float x, final int width) {
        return Utils.clamp((int)Math.floor(x / mTileWidth), 0, width - 1);
    }
    
    protected final int worldToTileRow(float y, final int height) {
        return Utils.clamp((int)Math.floor(y / mTileHeight), 0, height - 1);
    }
    
    /* 
     * Given a list of segments and a ray, this function performs an intersection search and
     * returns the closest intersecting segment, if any exists.
     */
    protected static boolean testSegmentAgainstList(FixedSizeArray<LineSegment> segments, 
            Vector2 startPoint, Vector2 endPoint, Vector2 hitPoint, Vector2 hitNormal, 
            Vector2 movementDirection, GameObject excludeObject) {
        boolean foundHit = false;
        float closestDistance = -1;
        float hitX = 0;
        float hitY = 0;
        float normalX = 0;
        float normalY = 0;
        final int count = segments.getCount();
        final Object[] segmentArray = segments.getArray();
        for (int x = 0; x < count; x++) {
            LineSegment segment = (LineSegment)segmentArray[x];
            // If a movement direction has been passed, filter out invalid surfaces by ignoring
            // those that do not oppose movement.  If no direction has been passed, accept all
            // surfaces.
            final float dot = movementDirection.length2() > 0.0f ? 
                    movementDirection.dot(segment.mNormal) : -1.0f;
                    
            if (dot < 0.0f &&
                    (excludeObject == null || segment.owner != excludeObject) &&
                    segment.calculateIntersection(startPoint, endPoint, hitPoint)) {
                final float distance = hitPoint.distance2(startPoint);

                if (!foundHit || closestDistance > distance) {
                    closestDistance = distance;
                    foundHit = true;
                    normalX = segment.mNormal.x;
                    normalY = segment.mNormal.y;
                    // Store the components on their own so we don't have to allocate a vector
                    // in this loop.
                    hitX = hitPoint.x;
                    hitY = hitPoint.y;
                }
            }
        }
        
        if (foundHit) {
            hitPoint.set(hitX, hitY);
            hitNormal.set(normalX, normalY);
        }
        return foundHit;
    }
    
    protected static boolean testBoxAgainstList(FixedSizeArray<LineSegment> segments, 
            float left, float right, float top, float bottom,
            Vector2 movementDirection, GameObject excludeObject, Vector2 outputOffset, 
            FixedSizeArray<HitPoint> outputHitPoints) {
        int hitCount = 0;
        final int maxSegments = outputHitPoints.getCapacity() - outputHitPoints.getCount();
        final int count = segments.getCount();
        final Object[] segmentArray = segments.getArray();
        
        VectorPool vectorPool = sSystemRegistry.vectorPool;
        HitPointPool hitPool = sSystemRegistry.hitPointPool;

        Vector2 tempHitPoint = vectorPool.allocate();
        
        for (int x = 0; x < count && hitCount < maxSegments; x++) {
            LineSegment segment = (LineSegment)segmentArray[x];
            // If a movement direction has been passed, filter out invalid surfaces by ignoring
            // those that do not oppose movement.  If no direction has been passed, accept all
            // surfaces.
            final float dot = movementDirection.length2() > 0.0f ? 
                    movementDirection.dot(segment.mNormal) : -1.0f;
                    
            if (dot < 0.0f &&
                    (excludeObject == null || segment.owner != excludeObject) &&
                    segment.calculateIntersectionBox(left, right, top, bottom, tempHitPoint)) {

                Vector2 hitPoint = vectorPool.allocate(tempHitPoint);
                Vector2 hitNormal = vectorPool.allocate(segment.mNormal);
               
                hitPoint.add(outputOffset);
                HitPoint hit = hitPool.allocate();
                
                hit.hitPoint = hitPoint;
                hit.hitNormal = hitNormal;
                
                outputHitPoints.add(hit);
                
                hitCount++;
            }
        }
        
        vectorPool.release(tempHitPoint);
        
        return hitCount > 0;
    }
    
    /* 
     * Loads line segments from a binary file and builds the tiled collision database
     * accordingly. 
     */
    public boolean loadCollisionTiles(InputStream stream) {
        boolean success = false;
        AssetManager.AssetInputStream byteStream = (AssetManager.AssetInputStream) stream;
        int signature;
        
        // TODO: this is a hack.  I really should only allocate an array that is the size of the
        // tileset, but at this point I don't actually know that size, so I allocate a buffer that's
        // probably large enough.  
        mCollisionTiles = new CollisionTile[256]; 
        try {
            signature = (byte)byteStream.read();
            if (signature == 52) {
                // This file has the following deserialization format:
                //   read the number of tiles
                //   for each tile
                //     read the tile id
                //     read the number of segments
                //     for each segment
                //       read startx, starty, endx, endy, normalx, normaly
                final int tileCount = byteStream.read();
                final int size = (1 + 1 + 4 + 4 + 4 + 4 + 4 + 4) * tileCount;
                if (byteStream.available() >= size) {
                    for (int x = 0; x < tileCount; x++) {
                        final int tileIndex = byteStream.read();
                        final int segmentCount = byteStream.read();
                        
                        if (mCollisionTiles[tileIndex] == null && segmentCount > 0) {
                            mCollisionTiles[tileIndex] = new CollisionTile(segmentCount);
                        }
                        
                        for (int y = 0; y < segmentCount; y++) {
                            byteStream.read(mWorkspaceBytes, 0, 4);
                            final float startX = Utils.byteArrayToFloat(mWorkspaceBytes);
                            byteStream.read(mWorkspaceBytes, 0, 4);
                            final float startY = Utils.byteArrayToFloat(mWorkspaceBytes);
                            byteStream.read(mWorkspaceBytes, 0, 4);
                            final float endX = Utils.byteArrayToFloat(mWorkspaceBytes);
                            byteStream.read(mWorkspaceBytes, 0, 4);
                            final float endY = Utils.byteArrayToFloat(mWorkspaceBytes);
                            byteStream.read(mWorkspaceBytes, 0, 4);
                            final float normalX = Utils.byteArrayToFloat(mWorkspaceBytes);
                            byteStream.read(mWorkspaceBytes, 0, 4);
                            final float normalY = Utils.byteArrayToFloat(mWorkspaceBytes);
                            
                            // TODO: it might be wise to pool line segments.  I don't think that
                            // this data will be loaded very often though, so this is ok for now.
                            LineSegment newSegment = new LineSegment();
                            newSegment.mStartPoint.set(startX, startY);
                            newSegment.mEndPoint.set(endX, endY);
                            newSegment.mNormal.set(normalX, normalY);
                            
                            mCollisionTiles[tileIndex].addSegment(newSegment);
                        }
                    }
                }
            }
        } catch (IOException e) {
            //TODO: figure out the best way to deal with this.  Assert?
        }
        
        return success;
    }
    
    
    /**
     * An interface for visiting tiles during a ray cast.  Implementations of TileVisitor
     * can be passed to executeRay(); the visit() function will be invoked for each tile touched by 
     * the ray until the traversal is completed or visit() returns false.
     */
    public abstract class TileVisitor extends AllocationGuard {
        public TileVisitor() {
            super();
        }
        
        // If true is returned, tile scanning continues.  Otherwise it stops.
        public abstract boolean visit(CollisionTile tile, Vector2 startPoint, Vector2 endPoint,
            Vector2 hitPoint, Vector2 hitNormal, int tileX, int tileY);
    }
    
    /**
     * TileTestVisitor tests the ray against a list of segments assigned to each tile.  If any
     * segment in any tile of the ray's path is found to be intersecting with the ray, traversal
     * stops and intersection information is recorded.
     */
    protected class TileTestVisitor extends TileVisitor {
        // These vectors are all temporary storage variables allocated as class members to avoid
        // runtime allocation.
        private Vector2 mDelta;
        private Vector2 mTileSpaceStart;
        private Vector2 mTileSpaceEnd;
        private Vector2 mTileSpaceOffset;
        private int mTileHeight;
        private int mTileWidth;
        
        public TileTestVisitor() {
            super();
            mDelta = new Vector2();
            mTileSpaceStart = new Vector2();
            mTileSpaceEnd = new Vector2();
            mTileSpaceOffset = new Vector2();
        }
        
        /**
         * Sets the visitor up for a ray test.  Initializes the size of the tiles and the direction
         * of movement by which intersections should be filtered.
         */
        public void setup(Vector2 movementDirection, int tileWidth, int tileHeight) {
            if (movementDirection != null) {
                mDelta.set(movementDirection);
                mDelta.normalize();
            } else {
                mDelta.zero();
            }
            mTileWidth = tileWidth;
            mTileHeight = tileHeight;
        }
        
        /** 
         * Converts the ray into tile space and then compares it to the segments
         * stored in the current tile.
         */
        @Override
        public boolean visit(CollisionTile tile, Vector2 startPoint, Vector2 endPoint,
                Vector2 hitPoint, Vector2 hitNormal, int tileX, int tileY) {
            mTileSpaceOffset.set(tileX * mTileWidth, tileY * mTileHeight);
            mTileSpaceStart.set(startPoint);
            mTileSpaceStart.subtract(mTileSpaceOffset);
            mTileSpaceEnd.set(endPoint);
            mTileSpaceEnd.subtract(mTileSpaceOffset);
            // find all the hits in the tile and pick the closest to the start point.
            boolean foundHit = testSegmentAgainstList(tile.segments, mTileSpaceStart, mTileSpaceEnd, 
                    hitPoint, hitNormal, mDelta, null);
            
            if (foundHit) {
                // The hitPoint is in tile space, so convert it back to world space.
                hitPoint.add(mTileSpaceOffset);
            }
            
            return foundHit;
        }
    }
    
    /**
     * A class describing a single surface in the collision world.  Surfaces are stored as a line
     * segment and a normal. The normal must be normalized (its length must be 1.0) and should 
     * describe the direction that the segment "pushes against" in a collision.
     */
    protected class LineSegment extends AllocationGuard {
        private Vector2 mStartPoint;
        private Vector2 mEndPoint;
        public Vector2 mNormal;
        public GameObject owner;
        
        public LineSegment() {
            super();
            mStartPoint = new Vector2();
            mEndPoint = new Vector2();
            mNormal = new Vector2();
        }
        
        /* Sets up the line segment.  Values are copied to local storage. */
        public void set(Vector2 start, Vector2 end, Vector2 norm) {
            mStartPoint.set(start);
            mEndPoint.set(end);
            mNormal.set(norm);
        }
        
        public void setOwner(GameObject ownerObject) {
            owner = ownerObject;
        }
        /**
         * Checks to see if these lines intersect by projecting one onto the other and then
         * assuring that the collision point is within the range of each segment.
         */
        public boolean calculateIntersection(Vector2 otherStart, Vector2 otherEnd,
                Vector2 hitPoint) {
            boolean intersecting = false;
            
            // Reference: http://local.wasp.uwa.edu.au/~pbourke/geometry/lineline2d/
            final float x1 = mStartPoint.x;
            final float x2 = mEndPoint.x;
            final float x3 = otherStart.x;
            final float x4 = otherEnd.x;
            final float y1 = mStartPoint.y;
            final float y2 = mEndPoint.y;
            final float y3 = otherStart.y;
            final float y4 = otherEnd.y;
            
            final float denom = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1);
            if (denom != 0) {
             final float uA = ((x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3)) / denom;
             final float uB = ((x2 - x1) * (y1 - y3) - (y2 - y1) * (x1 - x3)) / denom; 
             
             if (uA >= 0.0f && uA <= 1.0f && uB >= 0.0f && uB <= 1.0f) {
                 final float hitX = x1 + (uA * (x2 - x1));
                 final float hitY = y1 + (uA * (y2 - y1));
                 hitPoint.set(hitX, hitY);
                 intersecting = true;
             }
            }
            return intersecting;
        }
        
        // Based on http://www.garagegames.com/community/resources/view/309
        public boolean calculateIntersectionBox(float left, float right, float top, float bottom, 
                Vector2 hitPoint) {
            
            final float x1 = mStartPoint.x;
            final float x2 = mEndPoint.x;
            final float y1 = mStartPoint.y;
            final float y2 = mEndPoint.y;
            
            float startIntersect;
            float endIntersect;
            float intersectTimeStart = 0.0f;
            float intersectTimeEnd = 1.0f;
            
            if (x1 < x2) {
                if (x1 > right || x2 < left) {
                    return false;
                }
                final float deltaX = x2 - x1;
                startIntersect = (x1 < left) ? (left - x1) / deltaX : 0.0f;
                endIntersect = (x2 > right) ? (right - x1) / deltaX : 1.0f;
            } else {
                if (x2 > right || x1 < left) {
                    return false;
                }
                final float deltaX = x2 - x1;
                startIntersect = (x1 > right) ? (right - x1) / deltaX : 0.0f;
                endIntersect = (x2 < left) ? (left - x1) / deltaX : 1.0f;
            }
            
            if (startIntersect > intersectTimeStart) {
                intersectTimeStart = startIntersect;
            }
            if (endIntersect < intersectTimeEnd) {
                intersectTimeEnd = endIntersect;
            }
            if (intersectTimeEnd < intersectTimeStart) {
                return false;
            }
            
            // y
            if (y1 < y2) {
                if (y1 > top || y2 < bottom) {
                    return false;
                }
                final float deltaY = y2 - y1;
                startIntersect = (y1 < bottom) ? (bottom - y1) / deltaY : 0.0f;
                endIntersect = (y2 > top) ? (top - y1) / deltaY : 1.0f;
            } else {
                if (y2 > top || y1 < bottom) {
                    return false;
                }
                final float deltaY = y2 - y1;
                startIntersect = (y1 > top) ? (top - y1) / deltaY : 0.0f;
                endIntersect = (y2 < bottom) ? (bottom - y1) / deltaY : 1.0f;
            }
            
            if (startIntersect > intersectTimeStart) {
                intersectTimeStart = startIntersect;
            }
            if (endIntersect < intersectTimeEnd) {
                intersectTimeEnd = endIntersect;
            }
            if (intersectTimeEnd < intersectTimeStart) {
                return false;
            }
         
            hitPoint.set(mEndPoint);
            hitPoint.subtract(mStartPoint);
            hitPoint.multiply(intersectTimeStart);
            hitPoint.add(mStartPoint);
            
            return true;
        }
        
    }
    
    /**
     * A pool of line segments.
     */
    protected class LineSegmentPool extends TObjectPool<LineSegment> {
        public LineSegmentPool() {
            super();
        }
        
        public LineSegmentPool(int count) {
            super(count);
        }
        
        @Override
        public void reset() {
            
        }
        
        @Override
        protected void fill() {
            for (int x = 0; x < getSize(); x++) {
                getAvailable().add(new LineSegment());
            }
        }

        @Override
        public void release(Object entry) {
            ((LineSegment)entry).owner = null;
            super.release(entry);
        }
        
        
    }
    
    /**
     * A single collision tile.  Manages a list of line segments.
     */
    protected class CollisionTile extends AllocationGuard {
        public FixedSizeArray<LineSegment> segments;
        
        public CollisionTile(int maxSegments) {
            super();
            segments = new FixedSizeArray<LineSegment>(maxSegments);
        }
        
        public boolean addSegment(LineSegment segment) {
            boolean success = false;
            if (segments.getCount() < segments.getCapacity()) {
                success = true;
            }
            segments.add(segment);
            return success;
        }
    }
}
