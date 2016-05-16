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

import java.util.Comparator;

/**
 * A derivation of ObjectManager that sorts its children if they are of type PhasedObject.
 * Sorting is performed on add.
 */
public class PhasedObjectManager extends ObjectManager {
    private final static PhasedObjectComparator sPhasedObjectComparator 
        = new PhasedObjectComparator();
    private boolean mDirty;
    private PhasedObject mSearchDummy;  // A dummy object allocated up-front for searching by phase.

    public PhasedObjectManager() {
        super();
        mDirty = false;
        getObjects().setComparator(sPhasedObjectComparator);
        getPendingObjects().setComparator(sPhasedObjectComparator);
        mSearchDummy = new PhasedObject();
    }
    
    public PhasedObjectManager(int arraySize) {
        super(arraySize);
        mDirty = false;
        getObjects().setComparator(sPhasedObjectComparator);
        getPendingObjects().setComparator(sPhasedObjectComparator);
        mSearchDummy = new PhasedObject();
    }

    @Override
    public void commitUpdates() {
        super.commitUpdates();
        if (mDirty) {
            getObjects().sort(true);
            mDirty = false;
        }
    }
    
    @Override
    public void add(BaseObject object) {
        
        if (object instanceof PhasedObject) {
            super.add(object);
            mDirty = true;
        } else {
            // The only reason to restrict PhasedObjectManager to PhasedObjects is so that
            // the PhasedObjectComparator can assume all of its contents are PhasedObjects and
            // avoid calling instanceof every time.
            assert false : "Can't add a non-PhasedObject to a PhasedObjectManager!";
        }
    }
    
    public BaseObject find(int phase) {
        mSearchDummy.setPhase(phase);
        int index = getObjects().find(mSearchDummy, false);
        BaseObject result = null;
        if (index != -1) {
            result = getObjects().get(index);
        } else {
            index = getPendingObjects().find(mSearchDummy, false);
            if (index != -1) {
                result = getPendingObjects().get(index);
            }
        }
        return result;
    }

    /** Comparator for phased objects. */
    private static class PhasedObjectComparator implements Comparator<BaseObject>  {
        public int compare(BaseObject object1, BaseObject object2) {
            int result = 0;
            if (object1 != null && object2 != null) {
                result = ((PhasedObject) object1).phase - ((PhasedObject) object2).phase;
            } else if (object1 == null && object2 != null) {
                result = 1;
            } else if (object2 == null && object1 != null) {
                result = -1;
            } 
            return result;
        }
    }
    
}
