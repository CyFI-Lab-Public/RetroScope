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
 * This class manages drawable objects that have short lifetimes (one or two frames).  It provides
 * type-specific allocator functions and a type-insensitive release function.  This class manages
 * pools of objects so no actual allocations occur after bootstrap.
 */
public class DrawableFactory extends BaseObject {
    private final static int BITMAP_POOL_SIZE = 768;
    
    private DrawableBitmapPool mBitmapPool;
    private ScrollableBitmapPool mScrollableBitmapPool;
    private TiledBackgroundVertexGridPool mTiledBackgroundVertexGridPool;
    
    // This class wraps several object pools and provides a type-sensitive release function.
    public DrawableFactory() {
        super();
        mBitmapPool = new DrawableBitmapPool(BITMAP_POOL_SIZE);
        mTiledBackgroundVertexGridPool = new TiledBackgroundVertexGridPool();
        mScrollableBitmapPool = new ScrollableBitmapPool();
    }
    
    @Override
    public void reset() {
    }

    public DrawableBitmap allocateDrawableBitmap() {
        return mBitmapPool.allocate();
    }
    
    public TiledBackgroundVertexGrid allocateTiledBackgroundVertexGrid() {
        return mTiledBackgroundVertexGridPool.allocate();
    }

    public ScrollableBitmap allocateScrollableBitmap() {
        return mScrollableBitmapPool.allocate();
    }

    public void release(DrawableObject object) {
        ObjectPool pool = object.getParentPool();
        if (pool != null) {
            pool.release(object);
        }
        // Objects with no pool weren't created by this factory.  Ignore them.
    }

    private class DrawableBitmapPool extends TObjectPool<DrawableBitmap> {
        
        public DrawableBitmapPool(int size) {
            super(size);
        }
        
        @Override
        public void reset() {
        }
        
        @Override
        protected void fill() {
            int size = getSize();
            for (int x = 0; x < size; x++) {
                DrawableBitmap entry = new DrawableBitmap(null, 0, 0);
                entry.setParentPool(this);
                getAvailable().add(entry);
            }
        }

        @Override
        public void release(Object entry) {
            ((DrawableBitmap)entry).reset();
            super.release(entry);
        }
        
        @Override
        public DrawableBitmap allocate() {
            DrawableBitmap result = super.allocate();
            ContextParameters params = sSystemRegistry.contextParameters;
            if (result != null && params != null) {
                result.setViewSize(params.gameWidth, params.gameHeight);
            }
            return result;
        }
    }

    private class ScrollableBitmapPool extends TObjectPool<ScrollableBitmap> {
        
        public ScrollableBitmapPool() {
            super();
        }
        
        @Override
        public void reset() {
        }
        
        @Override
        protected void fill() {
            int size = getSize();
            for (int x = 0; x < size; x++) {
                ScrollableBitmap entry = new ScrollableBitmap(null, 0, 0);
                entry.setParentPool(this);
                getAvailable().add(entry);
            }
        }

        @Override
        public void release(Object entry) {
            ((ScrollableBitmap)entry).reset();
            super.release(entry);
        }
        
        
    }
    
    private class TiledBackgroundVertexGridPool extends TObjectPool<TiledBackgroundVertexGrid> {
        
        public TiledBackgroundVertexGridPool() {
            super();
        }
        
        @Override
        public void reset() {
        }
        
        @Override
        protected void fill() {
            int size = getSize();
            for (int x = 0; x < size; x++) {
                TiledBackgroundVertexGrid entry = new TiledBackgroundVertexGrid();
                entry.setParentPool(this);
                getAvailable().add(entry);
            }
        }

        @Override
        public void release(Object entry) {
            TiledBackgroundVertexGrid bg = (TiledBackgroundVertexGrid)entry;
            bg.reset();
            super.release(entry);
        }
       
    }
}
