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
 * Main game loop.  Updates the time system and passes the result down to the rest of the game
 * graph.  This object is effectively the root of the game graph.
 */
public class MainLoop extends ObjectManager {

    // Ensures that time updates before everything else.
    public MainLoop() {
        super();
        mTimeSystem = new TimeSystem();
        sSystemRegistry.timeSystem = mTimeSystem;
        sSystemRegistry.registerForReset(mTimeSystem);
    }

    @Override
    public void update(float timeDelta, BaseObject parent) {
        mTimeSystem.update(timeDelta, parent);
        final float newTimeDelta = mTimeSystem.getFrameDelta();  // The time system may warp time.
        super.update(newTimeDelta, parent);
    }

    private TimeSystem mTimeSystem;
}
