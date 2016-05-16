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

import java.io.IOException;
import java.io.InputStream;
import android.content.res.AssetManager;

/**
 * TiledWorld manages a 2D map of tile indexes that define a "world" of tiles.  These may be 
 * foreground or background layers in a scrolling game, or a layer of collision tiles, or some other
 * type of tile map entirely.  The TiledWorld maps xy positions to tile indices and also handles
 * deserialization of tilemap files.
 */
public class TiledWorld extends AllocationGuard {
    private int[][] mTilesArray;
    private int mRowCount;
    private int mColCount;
    private byte[] mWorkspaceBytes;
    
    public TiledWorld(int cols, int rows) {
        super();
        mTilesArray = new int[cols][rows];
        mRowCount = rows;
        mColCount = cols;

        for (int x = 0; x < cols; x++) {
            for (int y = 0; y < rows; y++) {
                mTilesArray[x][y] = -1;
            }
        }
        
        mWorkspaceBytes = new byte[4];
        
        calculateSkips();
    }

    public TiledWorld(InputStream stream) {
        super();
        mWorkspaceBytes = new byte[4];
        parseInput(stream);
        calculateSkips();
    }

    public int getTile(int x, int y) {
        int result = -1;
        if (x >= 0 && x < mColCount && y >= 0 && y < mRowCount) {
            result = mTilesArray[x][y];
        }
        return result;
    }

    // Builds a tiled world from a simple map file input source.  The map file format is as follows:
    // First byte: signature.  Must always be decimal 42.
    // Second byte: width of the world in tiles.
    // Third byte: height of the world in tiles.
    // Subsequent bytes: actual tile data in column-major order.
    // TODO: add a checksum in here somewhere.
    protected boolean parseInput(InputStream stream) {
        boolean success = false;
        AssetManager.AssetInputStream byteStream = (AssetManager.AssetInputStream) stream;
        int signature;
        try {
            signature = (byte)byteStream.read();
            if (signature == 42) {
                byteStream.read(mWorkspaceBytes, 0, 4);
                final int width = Utils.byteArrayToInt(mWorkspaceBytes);
                byteStream.read(mWorkspaceBytes, 0, 4);
                final int height = Utils.byteArrayToInt(mWorkspaceBytes);

                final int totalTiles = width * height;
                final int bytesRemaining = byteStream.available();
                assert bytesRemaining >= totalTiles;
                if (bytesRemaining >= totalTiles) {
                    mTilesArray = new int[width][height];
                    mRowCount = height;
                    mColCount = width;
                    for (int y = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                            mTilesArray[x][y] = (byte)byteStream.read();
                        }
                    }
                    success = true;
                }
            }

        } catch (IOException e) {
            //TODO: figure out the best way to deal with this.  Assert?
        }

        return success;
    }
    
    protected void calculateSkips() {
        int emptyTileCount = 0;
        for (int y = mRowCount - 1; y >= 0; y--) {
            for (int x = mColCount - 1; x >= 0; x--) {
                if (mTilesArray[x][y] < 0) {
                    emptyTileCount++;
                    mTilesArray[x][y] = -emptyTileCount;
                } else {
                    emptyTileCount = 0;
                }
            }
        }
    }

    public final int getWidth() {
        return mColCount;
    }

    public final int getHeight() {
        return mRowCount;
    }
    
    public final int[][] getTiles() {
        return mTilesArray;
    }

}
