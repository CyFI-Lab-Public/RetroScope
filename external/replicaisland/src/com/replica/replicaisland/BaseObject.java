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
 * The core object from which most other objects are derived.  Anything that will be managed by
 * an ObjectManager, and anything that requires an update per frame should be derived from
 * BaseObject.  BaseObject also defines the interface for the object-wide system registry.
 */
public abstract class BaseObject extends AllocationGuard {
    static ObjectRegistry sSystemRegistry = new ObjectRegistry();

    public BaseObject() {
        super();
    }
    
    /**
     * Update this object.
     * @param timeDelta  The duration since the last update (in seconds).
     * @param parent  The parent of this object (may be NULL).
     */
    public void update(float timeDelta, BaseObject parent) {
        // Base class does nothing.
    }
    
    
    public abstract void reset();

}
