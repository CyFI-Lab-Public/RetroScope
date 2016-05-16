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

import org.xmlpull.v1.XmlPullParserException;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.nfc.cardemulation.ApduServiceInfo;
import android.nfc.cardemulation.HostApduService;
import android.nfc.cardemulation.OffHostApduService;
import android.os.UserHandle;
import android.util.Log;
import android.util.SparseArray;

import com.google.android.collect.Maps;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class RegisteredServicesCache {
    static final String TAG = "RegisteredServicesCache";
    static final boolean DEBUG = false;

    final Context mContext;
    final AtomicReference<BroadcastReceiver> mReceiver;

    final Object mLock = new Object();
    // All variables below synchronized on mLock

    // mUserServices holds the card emulation services that are running for each user
    final SparseArray<UserServices> mUserServices = new SparseArray<UserServices>();
    final Callback mCallback;

    public interface Callback {
        void onServicesUpdated(int userId, final List<ApduServiceInfo> services);
    };

    private static class UserServices {
        /**
         * All services that have registered
         */
        public final HashMap<ComponentName, ApduServiceInfo> services =
                Maps.newHashMap(); // Re-built at run-time
    };

    private UserServices findOrCreateUserLocked(int userId) {
        UserServices services = mUserServices.get(userId);
        if (services == null) {
            services = new UserServices();
            mUserServices.put(userId, services);
        }
        return services;
    }

    public RegisteredServicesCache(Context context, Callback callback) {
        mContext = context;
        mCallback = callback;

        final BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final int uid = intent.getIntExtra(Intent.EXTRA_UID, -1);
                String action = intent.getAction();
                if (DEBUG) Log.d(TAG, "Intent action: " + action);
                if (uid != -1) {
                    boolean replaced = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false) &&
                            (Intent.ACTION_PACKAGE_ADDED.equals(action) ||
                             Intent.ACTION_PACKAGE_REMOVED.equals(action));
                    if (!replaced) {
                        int currentUser = ActivityManager.getCurrentUser();
                        if (currentUser == UserHandle.getUserId(uid)) {
                            invalidateCache(UserHandle.getUserId(uid));
                        } else {
                            // Cache will automatically be updated on user switch
                        }
                    } else {
                        if (DEBUG) Log.d(TAG, "Ignoring package intent due to package being replaced.");
                    }
                }
            }
        };
        mReceiver = new AtomicReference<BroadcastReceiver>(receiver);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_FIRST_LAUNCH);
        intentFilter.addAction(Intent.ACTION_PACKAGE_RESTARTED);
        intentFilter.addDataScheme("package");
        mContext.registerReceiverAsUser(mReceiver.get(), UserHandle.ALL, intentFilter, null, null);

        // Register for events related to sdcard operations
        IntentFilter sdFilter = new IntentFilter();
        sdFilter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
        sdFilter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE);
        mContext.registerReceiverAsUser(mReceiver.get(), UserHandle.ALL, sdFilter, null, null);
    }

    void dump(ArrayList<ApduServiceInfo> services) {
        for (ApduServiceInfo service : services) {
            if (DEBUG) Log.d(TAG, service.toString());
        }
    }

    boolean containsServiceLocked(ArrayList<ApduServiceInfo> services, ComponentName serviceName) {
        for (ApduServiceInfo service : services) {
            if (service.getComponent().equals(serviceName)) return true;
        }
        return false;
    }

    public boolean hasService(int userId, ComponentName service) {
        return getService(userId, service) != null;
    }

    public ApduServiceInfo getService(int userId, ComponentName service) {
        synchronized (mLock) {
            UserServices userServices = findOrCreateUserLocked(userId);
            return userServices.services.get(service);
        }
    }

    public List<ApduServiceInfo> getServices(int userId) {
        final ArrayList<ApduServiceInfo> services = new ArrayList<ApduServiceInfo>();
        synchronized (mLock) {
            UserServices userServices = findOrCreateUserLocked(userId);
            services.addAll(userServices.services.values());
        }
        return services;
    }

    public List<ApduServiceInfo> getServicesForCategory(int userId, String category) {
        final ArrayList<ApduServiceInfo> services = new ArrayList<ApduServiceInfo>();
        synchronized (mLock) {
            UserServices userServices = findOrCreateUserLocked(userId);
            for (ApduServiceInfo service : userServices.services.values()) {
                if (service.hasCategory(category)) services.add(service);
            }
        }
        return services;
    }

    public void onNfcDisabled() {

    }

    public void onNfcEnabled() {
        invalidateCache(ActivityManager.getCurrentUser());
    }

    public void invalidateCache(int userId) {
        PackageManager pm;
        try {
            pm = mContext.createPackageContextAsUser("android", 0,
                    new UserHandle(userId)).getPackageManager();
        } catch (NameNotFoundException e) {
            Log.e(TAG, "Could not create user package context");
            return;
        }

        ArrayList<ApduServiceInfo> validServices = new ArrayList<ApduServiceInfo>();

        List<ResolveInfo> resolvedServices = pm.queryIntentServicesAsUser(
                new Intent(HostApduService.SERVICE_INTERFACE),
                PackageManager.GET_META_DATA, userId);

        List<ResolveInfo> resolvedOffHostServices = pm.queryIntentServicesAsUser(
                new Intent(OffHostApduService.SERVICE_INTERFACE),
                PackageManager.GET_META_DATA, userId);
        resolvedServices.addAll(resolvedOffHostServices);

        for (ResolveInfo resolvedService : resolvedServices) {
            try {
                boolean onHost = !resolvedOffHostServices.contains(resolvedService);
                ServiceInfo si = resolvedService.serviceInfo;
                ComponentName componentName = new ComponentName(si.packageName, si.name);
                // Check if the package holds the NFC permission
                if (pm.checkPermission(android.Manifest.permission.NFC, si.packageName) !=
                        PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "Skipping APDU service " + componentName +
                            ": it does not require the permission " +
                            android.Manifest.permission.NFC);
                    continue;
                }
                if (!android.Manifest.permission.BIND_NFC_SERVICE.equals(
                        si.permission)) {
                    Log.e(TAG, "Skipping APDU service " + componentName +
                            ": it does not require the permission " +
                            android.Manifest.permission.BIND_NFC_SERVICE);
                    continue;
                }
                ApduServiceInfo service = new ApduServiceInfo(pm, resolvedService, onHost);
                if (service != null) {
                    validServices.add(service);
                }
            } catch (XmlPullParserException e) {
                Log.w(TAG, "Unable to load component info " + resolvedService.toString(), e);
            } catch (IOException e) {
                Log.w(TAG, "Unable to load component info " + resolvedService.toString(), e);
            }
        }

        synchronized (mLock) {
            UserServices userServices = findOrCreateUserLocked(userId);

            // Find removed services
            Iterator<Map.Entry<ComponentName, ApduServiceInfo>> it =
                    userServices.services.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<ComponentName, ApduServiceInfo> entry =
                        (Map.Entry<ComponentName, ApduServiceInfo>) it.next();
                if (!containsServiceLocked(validServices, entry.getKey())) {
                    Log.d(TAG, "Service removed: " + entry.getKey());
                    it.remove();
                }
            }
            for (ApduServiceInfo service : validServices) {
                if (DEBUG) Log.d(TAG, "Adding service: " + service.getComponent() +
                        " AIDs: " + service.getAids());
                userServices.services.put(service.getComponent(), service);
            }
        }

        mCallback.onServicesUpdated(userId, Collections.unmodifiableList(validServices));
        dump(validServices);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("Registered HCE services for current user: ");
        UserServices userServices = findOrCreateUserLocked(ActivityManager.getCurrentUser());
        for (ApduServiceInfo service : userServices.services.values()) {
            pw.println("    " + service.getComponent() +
                    " (Description: " + service.getDescription() + ")");
        }
        pw.println("");
    }
}
