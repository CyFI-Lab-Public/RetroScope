/*
 * Copyright (C) 2013 The Android Open Source Project
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
package com.android.nfc.cardemulation;

import android.util.Log;
import android.util.SparseArray;

import com.android.nfc.NfcService;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AidRoutingManager {
    static final String TAG = "AidRoutingManager";

    static final boolean DBG = false;

    // This is the default IsoDep protocol route; it means
    // that for any AID that needs to be routed to this
    // destination, we won't need to add a rule to the routing
    // table, because this destination is already the default route.
    //
    // For Nexus devices, the default route is always 0x00.
    static final int DEFAULT_ROUTE = 0x00;

    // For Nexus devices, just a static route to the eSE
    // OEMs/Carriers could manually map off-host AIDs
    // to the correct eSE/UICC based on state they keep.
    static final int DEFAULT_OFFHOST_ROUTE = 0xF4;

    final Object mLock = new Object();

    // mAidRoutingTable contains the current routing table. The index is the route ID.
    // The route can include routes to a eSE/UICC.
    final SparseArray<Set<String>> mAidRoutingTable = new SparseArray<Set<String>>();

    // Easy look-up what the route is for a certain AID
    final HashMap<String, Integer> mRouteForAid = new HashMap<String, Integer>();

    // Whether the routing table is dirty
    boolean mDirty;

    public AidRoutingManager() {

    }

    public boolean aidsRoutedToHost() {
        synchronized(mLock) {
            Set<String> aidsToHost = mAidRoutingTable.get(0);
            return aidsToHost != null && aidsToHost.size() > 0;
        }
    }

    public Set<String> getRoutedAids() {
        Set<String> routedAids = new HashSet<String>();
        synchronized (mLock) {
            for (Map.Entry<String, Integer> aidEntry : mRouteForAid.entrySet()) {
                routedAids.add(aidEntry.getKey());
            }
        }
        return routedAids;
    }

    public boolean setRouteForAid(String aid, boolean onHost) {
        int route;
        synchronized (mLock) {
            int currentRoute = getRouteForAidLocked(aid);
            if (DBG) Log.d(TAG, "Set route for AID: " + aid + ", host: " + onHost + " , current: 0x" +
                    Integer.toHexString(currentRoute));
            route = onHost ? 0 : DEFAULT_OFFHOST_ROUTE;
            if (route == currentRoute) return true;

            if (currentRoute != -1) {
                // Remove current routing
                removeAid(aid);
            }
            Set<String> aids = mAidRoutingTable.get(route);
            if (aids == null) {
               aids = new HashSet<String>();
               mAidRoutingTable.put(route, aids);
            }
            aids.add(aid);
            mRouteForAid.put(aid, route);
            if (route != DEFAULT_ROUTE) {
                NfcService.getInstance().routeAids(aid, route);
                mDirty = true;
            }
        }
        return true;
    }

    /**
     * This notifies that the AID routing table in the controller
     * has been cleared (usually due to NFC being turned off).
     */
    public void onNfccRoutingTableCleared() {
        // The routing table in the controller was cleared
        // To stay in sync, clear our own tables.
        synchronized (mLock) {
            mAidRoutingTable.clear();
            mRouteForAid.clear();
        }
    }

    public boolean removeAid(String aid) {
        synchronized (mLock) {
            Integer route = mRouteForAid.get(aid);
            if (route == null) {
               if (DBG) Log.d(TAG, "removeAid(): No existing route for " + aid);
               return false;
            }
            Set<String> aids = mAidRoutingTable.get(route);
            if (aids == null) return false;
            aids.remove(aid);
            mRouteForAid.remove(aid);
            if (route.intValue() != DEFAULT_ROUTE) {
                NfcService.getInstance().unrouteAids(aid);
                mDirty = true;
            }
        }
        return true;
    }

    public void commitRouting() {
        synchronized (mLock) {
            if (mDirty) {
                NfcService.getInstance().commitRouting();
                mDirty = false;
            } else {
                if (DBG) Log.d(TAG, "Not committing routing because table not dirty.");
            }
        }
    }

    int getRouteForAidLocked(String aid) {
        Integer route = mRouteForAid.get(aid);
        return route == null ? -1 : route;
    }
}
