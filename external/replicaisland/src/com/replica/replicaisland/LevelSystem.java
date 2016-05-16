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
 * Manages information about the current level, including setup, deserialization, and tear-down.
 */
public class LevelSystem extends BaseObject {

    public int mWidthInTiles;
    public int mHeightInTiles;
    public int mTileWidth;
    public int mTileHeight;
    public GameObject mBackgroundObject;
    public ObjectManager mRoot;
    private byte[] mWorkspaceBytes;
    private TiledWorld mSpawnLocations;
    private GameFlowEvent mGameFlowEvent;
    private int mAttempts;
    private LevelTree.Level mCurrentLevel;
    
    public LevelSystem() {
        super();
        mWorkspaceBytes = new byte[4];
        mGameFlowEvent = new GameFlowEvent();
        reset();
    }
    
    @Override
    public void reset() {
        if (mBackgroundObject != null && mRoot != null) {
            mBackgroundObject.removeAll();
            mBackgroundObject.commitUpdates();
            mRoot.remove(mBackgroundObject);
            mBackgroundObject = null;
            mRoot = null;
        }
        mSpawnLocations = null;
        mAttempts = 0;
        mCurrentLevel = null;
    }
    
    public float getLevelWidth() {
        return mWidthInTiles * mTileWidth;
    }
    
    public float getLevelHeight() {
        return mHeightInTiles * mTileHeight;
    }
    
    public void sendRestartEvent() {
        mGameFlowEvent.post(GameFlowEvent.EVENT_RESTART_LEVEL, 0,
                sSystemRegistry.contextParameters.context);
    }
    
    public void sendNextLevelEvent() {
        mGameFlowEvent.post(GameFlowEvent.EVENT_GO_TO_NEXT_LEVEL, 0,
                sSystemRegistry.contextParameters.context);
    }
    
    public void sendGameEvent(int type, int index, boolean immediate) {
        if (immediate) {
        	mGameFlowEvent.postImmediate(type, index,
                sSystemRegistry.contextParameters.context);
        } else {
        	mGameFlowEvent.post(type, index,
                    sSystemRegistry.contextParameters.context);
        }
    }
    
    /**
     * Loads a level from a binary file.  The file consists of several layers, including background
     * tile layers and at most one collision layer.  Each layer is used to bootstrap related systems
     * and provide them with layer data.
     * @param stream  The input stream for the level file resource.
     * @param tiles   A tile library to use when constructing tiled background layers.
     * @param background  An object to assign background layer rendering components to.
     * @return
     */
    public boolean loadLevel(LevelTree.Level level, InputStream stream, ObjectManager root) {
        boolean success = false;
        mCurrentLevel = level;
        AssetManager.AssetInputStream byteStream = (AssetManager.AssetInputStream) stream;
        int signature;
        try {
            signature = (byte)byteStream.read();
            if (signature == 96) {
                final int layerCount = (byte)byteStream.read();
                final int backgroundIndex = (byte)byteStream.read();
                
                mRoot = root;
                mTileWidth = 32;
                mTileHeight = 32;
                
                ContextParameters params = sSystemRegistry.contextParameters;
                int currentPriority = SortConstants.BACKGROUND_START + 1;
                for (int x = 0; x < layerCount; x++) {
                    final int type = (byte)byteStream.read();
                    final int tileIndex = (byte)byteStream.read();
                    byteStream.read(mWorkspaceBytes, 0, 4);
                    final float scrollSpeed = Utils.byteArrayToFloat(mWorkspaceBytes);

                    // TODO: use a pool here?  Seems pointless.
                    TiledWorld world = new TiledWorld(byteStream);
                    
                    if (type == 0) { // it's a background layer
                        assert mWidthInTiles != 0;
                        assert mTileWidth != 0;
                        
                        // We require a collision layer to set up the tile sizes before we load.
                        // TODO: this really sucks.  there's no reason each layer can't have its
                        // own tile widths and heights.  Refactor this crap.
                        if (mWidthInTiles > 0 && mTileWidth > 0) {
                        
                            LevelBuilder builder = sSystemRegistry.levelBuilder;
                             
                            if (mBackgroundObject == null) {
                                mBackgroundObject = 
                                    builder.buildBackground(
                                    		backgroundIndex, 
                                    		mWidthInTiles * mTileWidth,
                                    		mHeightInTiles * mTileHeight);
                                root.add(mBackgroundObject);
                            }
                            
                            
                            builder.addTileMapLayer(mBackgroundObject, currentPriority, 
                                    scrollSpeed, params.gameWidth, params.gameHeight, 
                                    mTileWidth, mTileHeight, world, tileIndex);
                            
                            
                            currentPriority++;
                        }

                    } else if (type == 1) { // collision
                        // Collision always defines the world boundaries.
                        mWidthInTiles = world.getWidth();
                        mHeightInTiles = world.getHeight();
                        
                        
                        CollisionSystem collision = sSystemRegistry.collisionSystem;
                        if (collision != null) {
                            collision.initialize(world, mTileWidth, mTileHeight);
                        }
                    } else if (type == 2) { // objects
                        mSpawnLocations = world;
                        spawnObjects();
                    } else if (type == 3) { // hot spots
                        HotSpotSystem hotSpots = sSystemRegistry.hotSpotSystem;
                        if (hotSpots != null) {
                            hotSpots.setWorld(world);
                        }
                        
                    }
                }
                
                // hack!
                sSystemRegistry.levelBuilder.promoteForegroundLayer(mBackgroundObject);

            }

        } catch (IOException e) {
            //TODO: figure out the best way to deal with this.  Assert?
        }

        return success;
    }
    
    public void spawnObjects() {
        GameObjectFactory factory = sSystemRegistry.gameObjectFactory;
        if (factory != null && mSpawnLocations != null) {
            DebugLog.d("LevelSystem", "Spawning Objects!");
            
            factory.spawnFromWorld(mSpawnLocations, mTileWidth, mTileHeight);
        }
    }

    public void incrementAttemptsCount() {
        mAttempts++;
    }
    
    public int getAttemptsCount() {
        return mAttempts;
    }
    
    public LevelTree.Level getCurrentLevel() {
    	return mCurrentLevel;
    }
}
