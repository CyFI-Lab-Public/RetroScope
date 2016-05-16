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

import javax.microedition.khronos.opengles.GL10;

public class BufferLibrary extends BaseObject {
    private static final int GRID_LIST_SIZE = 256;
    private FixedSizeArray<Grid> mGridList;
   
    public BufferLibrary() {
        super();
        
        mGridList = new FixedSizeArray<Grid>(GRID_LIST_SIZE);
    }
    
    @Override
    public void reset() {
        removeAll();
    }
    
    public void add(Grid grid) { 
         mGridList.add(grid);
    }
    
    public void removeAll() {
        mGridList.clear();
    }
    
    public void generateHardwareBuffers(GL10 gl) {
    	if (sSystemRegistry.contextParameters.supportsVBOs) {
	        final int count = mGridList.getCount();
	        for (int x = 0; x < count; x++) {
	            Grid grid = mGridList.get(x);
	            grid.generateHardwareBuffers(gl);
	        }
    	}
    }
    
    public void releaseHardwareBuffers(GL10 gl) {
    	if (sSystemRegistry.contextParameters.supportsVBOs) {
	        final int count = mGridList.getCount();
	        for (int x = 0; x < count; x++) {
	            Grid grid = mGridList.get(x);
	            grid.releaseHardwareBuffers(gl);
	        }
    	}
    }
    
    public void invalidateHardwareBuffers() {
    	if (sSystemRegistry.contextParameters.supportsVBOs) {
	        final int count = mGridList.getCount();
	        for (int x = 0; x < count; x++) {
	            Grid grid = mGridList.get(x);
	            grid.invalidateHardwareBuffers();
	        }
    	}
    }

}
