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
 * AllocationGuard is a utility class for tracking down memory leaks.  It implements a 
 * "checkpoint" memory scheme.  After the static sGuardActive flag has been set, any further
 * allocation of AllocationGuard or its derivatives will cause an error log entry.  Note
 * that AllocationGuard requires all of its derivatives to call super() in their constructor. 
 */
public class AllocationGuard {
    public static boolean sGuardActive = false;
    public AllocationGuard() {
        if (sGuardActive) {
            // An allocation has occurred while the guard is active!  Report it.
            DebugLog.e("AllocGuard", "An allocation of type " + this.getClass().getName() 
                    + " occurred while the AllocGuard is active.");
            
            
        }
    }
}
