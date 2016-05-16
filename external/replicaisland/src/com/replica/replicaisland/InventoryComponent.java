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

public class InventoryComponent extends GameComponent {
    private UpdateRecord mInventory;
    private boolean mInventoryChanged;
    
    public InventoryComponent() {
        super();
        mInventory = new UpdateRecord();
        reset();
        setPhase(ComponentPhases.FRAME_END.ordinal());
    }
    
    @Override
    public void reset() {
        mInventoryChanged = true;
        mInventory.reset();
    }
    
    public void applyUpdate(UpdateRecord record) {
        mInventory.add(record);
        mInventoryChanged = true;
    }
    
    @Override
    public void update(float timeDelta, BaseObject parent) {
        if (mInventoryChanged) {
            HudSystem hud = sSystemRegistry.hudSystem;
            if (hud != null) {
                hud.updateInventory(mInventory);
            }
            mInventoryChanged = false;
        }
    }
    
    public UpdateRecord getRecord() {
        return mInventory;
    }
    
    public void setChanged() {
        mInventoryChanged = true;
    }
    
    public static class UpdateRecord extends BaseObject {
        public int rubyCount;
        public int coinCount;
        public int diaryCount;
        
        public UpdateRecord() {
            super();
        }
        
        public void reset() {
            rubyCount = 0;
            coinCount = 0;
            diaryCount = 0;
        }
        
        public void add(UpdateRecord other) {
            rubyCount += other.rubyCount;
            coinCount += other.coinCount;
            diaryCount += other.diaryCount;
        }
    }
}
