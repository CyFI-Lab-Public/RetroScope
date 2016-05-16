package com.android.nfc.cardemulation;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.nfc.cardemulation.ApduServiceInfo;
import android.nfc.cardemulation.CardEmulation;
import android.nfc.cardemulation.ApduServiceInfo.AidGroup;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;

import com.google.android.collect.Maps;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class RegisteredAidCache implements RegisteredServicesCache.Callback {
    static final String TAG = "RegisteredAidCache";

    static final boolean DBG = false;

    // mAidServices is a tree that maps an AID to a list of handling services
    // on Android. It is only valid for the current user.
    final TreeMap<String, ArrayList<ApduServiceInfo>> mAidToServices =
            new TreeMap<String, ArrayList<ApduServiceInfo>>();

    // mAidCache is a lookup table for quickly mapping an AID to one or
    // more services. It differs from mAidServices in the sense that it
    // has already accounted for defaults, and hence its return value
    // is authoritative for the current set of services and defaults.
    // It is only valid for the current user.
    final HashMap<String, AidResolveInfo> mAidCache =
            Maps.newHashMap();

    final HashMap<String, ComponentName> mCategoryDefaults =
            Maps.newHashMap();

    final class AidResolveInfo {
        List<ApduServiceInfo> services;
        ApduServiceInfo defaultService;
        String aid;
    }

    /**
     * AIDs per category
     */
    public final HashMap<String, Set<String>> mCategoryAids =
            Maps.newHashMap();

    final Handler mHandler = new Handler(Looper.getMainLooper());
    final RegisteredServicesCache mServiceCache;

    final Object mLock = new Object();
    final Context mContext;
    final AidRoutingManager mRoutingManager;
    final SettingsObserver mSettingsObserver;

    ComponentName mNextTapComponent = null;
    boolean mNfcEnabled = false;

    private final class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            synchronized (mLock) {
                // Do it just for the current user. If it was in fact
                // a change made for another user, we'll sync it down
                // on user switch.
                int currentUser = ActivityManager.getCurrentUser();
                boolean changed = updateFromSettingsLocked(currentUser);
                if (changed) {
                    generateAidCacheLocked();
                    updateRoutingLocked();
                } else {
                    if (DBG) Log.d(TAG, "Not updating aid cache + routing: nothing changed.");
                }
            }
        }
    };

    public RegisteredAidCache(Context context, AidRoutingManager routingManager) {
        mSettingsObserver = new SettingsObserver(mHandler);
        mContext = context;
        mServiceCache = new RegisteredServicesCache(context, this);
        mRoutingManager = routingManager;

        mContext.getContentResolver().registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.NFC_PAYMENT_DEFAULT_COMPONENT),
                true, mSettingsObserver, UserHandle.USER_ALL);
        updateFromSettingsLocked(ActivityManager.getCurrentUser());
    }

    public boolean isNextTapOverriden() {
        synchronized (mLock) {
            return mNextTapComponent != null;
        }
    }

    public AidResolveInfo resolveAidPrefix(String aid) {
        synchronized (mLock) {
            char nextAidChar = (char) (aid.charAt(aid.length() - 1) + 1);
            String nextAid = aid.substring(0, aid.length() - 1) + nextAidChar;
            SortedMap<String, ArrayList<ApduServiceInfo>> matches =
                    mAidToServices.subMap(aid, nextAid);
            // The first match is lexicographically closest to what the reader asked;
            if (matches.isEmpty()) {
                return null;
            } else {
                AidResolveInfo resolveInfo = mAidCache.get(matches.firstKey());
                // Let the caller know which AID got selected
                resolveInfo.aid = matches.firstKey();
                return resolveInfo;
            }
        }
    }

    public String getCategoryForAid(String aid) {
        synchronized (mLock) {
            Set<String> paymentAids = mCategoryAids.get(CardEmulation.CATEGORY_PAYMENT);
            if (paymentAids != null && paymentAids.contains(aid)) {
                return CardEmulation.CATEGORY_PAYMENT;
            } else {
                return CardEmulation.CATEGORY_OTHER;
            }
        }
    }

    public boolean isDefaultServiceForAid(int userId, ComponentName service, String aid) {
        AidResolveInfo resolveInfo = null;
        boolean serviceFound = false;
        synchronized (mLock) {
            serviceFound = mServiceCache.hasService(userId, service);
        }
        if (!serviceFound) {
            // If we don't know about this service yet, it may have just been enabled
            // using PackageManager.setComponentEnabledSetting(). The PackageManager
            // broadcasts are delayed by 10 seconds in that scenario, which causes
            // calls to our APIs referencing that service to fail.
            // Hence, update the cache in case we don't know about the service.
            if (DBG) Log.d(TAG, "Didn't find passed in service, invalidating cache.");
            mServiceCache.invalidateCache(userId);
        }
        synchronized (mLock) {
            resolveInfo = mAidCache.get(aid);
        }
        if (resolveInfo.services == null || resolveInfo.services.size() == 0) return false;

        if (resolveInfo.defaultService != null) {
            return service.equals(resolveInfo.defaultService.getComponent());
        } else if (resolveInfo.services.size() == 1) {
            return service.equals(resolveInfo.services.get(0).getComponent());
        } else {
            // More than one service, not the default
            return false;
        }
    }

    public boolean setDefaultServiceForCategory(int userId, ComponentName service,
            String category) {
        if (!CardEmulation.CATEGORY_PAYMENT.equals(category)) {
            Log.e(TAG, "Not allowing defaults for category " + category);
            return false;
        }
        synchronized (mLock) {
            // TODO Not really nice to be writing to Settings.Secure here...
            // ideally we overlay our local changes over whatever is in
            // Settings.Secure
            if (service == null || mServiceCache.hasService(userId, service)) {
                Settings.Secure.putStringForUser(mContext.getContentResolver(),
                        Settings.Secure.NFC_PAYMENT_DEFAULT_COMPONENT,
                        service != null ? service.flattenToString() : null, userId);
            } else {
                Log.e(TAG, "Could not find default service to make default: " + service);
            }
        }
        return true;
    }

    public boolean isDefaultServiceForCategory(int userId, String category,
            ComponentName service) {
        boolean serviceFound = false;
        synchronized (mLock) {
            // If we don't know about this service yet, it may have just been enabled
            // using PackageManager.setComponentEnabledSetting(). The PackageManager
            // broadcasts are delayed by 10 seconds in that scenario, which causes
            // calls to our APIs referencing that service to fail.
            // Hence, update the cache in case we don't know about the service.
            serviceFound = mServiceCache.hasService(userId, service);
        }
        if (!serviceFound) {
            if (DBG) Log.d(TAG, "Didn't find passed in service, invalidating cache.");
            mServiceCache.invalidateCache(userId);
        }
        ComponentName defaultService =
                getDefaultServiceForCategory(userId, category, true);
        return (defaultService != null && defaultService.equals(service));
    }

    ComponentName getDefaultServiceForCategory(int userId, String category,
            boolean validateInstalled) {
        if (!CardEmulation.CATEGORY_PAYMENT.equals(category)) {
            Log.e(TAG, "Not allowing defaults for category " + category);
            return null;
        }
        synchronized (mLock) {
            // Load current payment default from settings
            String name = Settings.Secure.getStringForUser(
                    mContext.getContentResolver(), Settings.Secure.NFC_PAYMENT_DEFAULT_COMPONENT,
                    userId);
            if (name != null) {
                ComponentName service = ComponentName.unflattenFromString(name);
                if (!validateInstalled || service == null) {
                    return service;
                } else {
                    return mServiceCache.hasService(userId, service) ? service : null;
                }
            } else {
                return null;
            }
        }
    }

    public List<ApduServiceInfo> getServicesForCategory(int userId, String category) {
        return mServiceCache.getServicesForCategory(userId, category);
    }

    public boolean setDefaultForNextTap(int userId, ComponentName service) {
        synchronized (mLock) {
            if (service != null) {
                mNextTapComponent = service;
            } else {
                mNextTapComponent = null;
            }
            // Update cache and routing table
            generateAidCacheLocked();
            updateRoutingLocked();
        }
        return true;
    }

    /**
     * Resolves an AID to a set of services that can handle it.
     */
     AidResolveInfo resolveAidLocked(List<ApduServiceInfo> resolvedServices, String aid) {
        if (resolvedServices == null || resolvedServices.size() == 0) {
            if (DBG) Log.d(TAG, "Could not resolve AID " + aid + " to any service.");
            return null;
        }
        AidResolveInfo resolveInfo = new AidResolveInfo();
        if (DBG) Log.d(TAG, "resolveAidLocked: resolving AID " + aid);
        resolveInfo.services = new ArrayList<ApduServiceInfo>();
        resolveInfo.services.addAll(resolvedServices);
        resolveInfo.defaultService = null;

        ComponentName defaultComponent = mNextTapComponent;
        if (DBG) Log.d(TAG, "resolveAidLocked: next tap component is " + defaultComponent);
        Set<String> paymentAids = mCategoryAids.get(CardEmulation.CATEGORY_PAYMENT);
        if (paymentAids != null && paymentAids.contains(aid)) {
            if (DBG) Log.d(TAG, "resolveAidLocked: AID " + aid + " is a payment AID");
            // This AID has been registered as a payment AID by at least one service.
            // Get default component for payment if no next tap default.
            if (defaultComponent == null) {
                defaultComponent = mCategoryDefaults.get(CardEmulation.CATEGORY_PAYMENT);
            }
            if (DBG) Log.d(TAG, "resolveAidLocked: default payment component is "
                    + defaultComponent);
            if (resolvedServices.size() == 1) {
                ApduServiceInfo resolvedService = resolvedServices.get(0);
                if (DBG) Log.d(TAG, "resolveAidLocked: resolved single service " +
                        resolvedService.getComponent());
                if (defaultComponent != null &&
                        defaultComponent.equals(resolvedService.getComponent())) {
                    if (DBG) Log.d(TAG, "resolveAidLocked: DECISION: routing to (default) " +
                        resolvedService.getComponent());
                    resolveInfo.defaultService = resolvedService;
                } else {
                    // So..since we resolved to only one service, and this AID
                    // is a payment AID, we know that this service is the only
                    // service that has registered for this AID and in fact claimed
                    // it was a payment AID.
                    // There's two cases:
                    // 1. All other AIDs in the payment group are uncontended:
                    //    in this case, just route to this app. It won't get
                    //    in the way of other apps, and is likely to interact
                    //    with different terminal infrastructure anyway.
                    // 2. At least one AID in the payment group is contended:
                    //    in this case, we should ask the user to confirm,
                    //    since it is likely to contend with other apps, even
                    //    when touching the same terminal.
                    boolean foundConflict = false;
                    for (AidGroup aidGroup : resolvedService.getAidGroups()) {
                        if (aidGroup.getCategory().equals(CardEmulation.CATEGORY_PAYMENT)) {
                            for (String registeredAid : aidGroup.getAids()) {
                                ArrayList<ApduServiceInfo> servicesForAid =
                                        mAidToServices.get(registeredAid);
                                if (servicesForAid != null && servicesForAid.size() > 1) {
                                    foundConflict = true;
                                }
                            }
                        }
                    }
                    if (!foundConflict) {
                        if (DBG) Log.d(TAG, "resolveAidLocked: DECISION: routing to " +
                            resolvedService.getComponent());
                        // Treat this as if it's the default for this AID
                        resolveInfo.defaultService = resolvedService;
                    } else {
                        // Allow this service to handle, but don't set as default
                        if (DBG) Log.d(TAG, "resolveAidLocked: DECISION: routing AID " + aid +
                                " to " + resolvedService.getComponent() +
                                ", but will ask confirmation because its AID group is contended.");
                    }
                }
            } else if (resolvedServices.size() > 1) {
                // More services have registered. If there's a default and it
                // registered this AID, go with the default. Otherwise, add all.
                if (DBG) Log.d(TAG, "resolveAidLocked: multiple services matched.");
                if (defaultComponent != null) {
                    for (ApduServiceInfo service : resolvedServices) {
                        if (service.getComponent().equals(defaultComponent)) {
                            if (DBG) Log.d(TAG, "resolveAidLocked: DECISION: routing to (default) "
                                    + service.getComponent());
                            resolveInfo.defaultService = service;
                            break;
                        }
                    }
                    if (resolveInfo.defaultService == null) {
                        if (DBG) Log.d(TAG, "resolveAidLocked: DECISION: routing to all services");
                    }
                }
            } // else -> should not hit, we checked for 0 before.
        } else {
            // This AID is not a payment AID, just return all components
            // that can handle it, but be mindful of (next tap) defaults.
            for (ApduServiceInfo service : resolvedServices) {
                if (service.getComponent().equals(defaultComponent)) {
                    if (DBG) Log.d(TAG, "resolveAidLocked: DECISION: cat OTHER AID, " +
                            "routing to (default) " + service.getComponent());
                    resolveInfo.defaultService = service;
                    break;
                }
            }
            if (resolveInfo.defaultService == null) {
                // If we didn't find the default, mark the first as default
                // if there is only one.
                if (resolveInfo.services.size() == 1) {
                    resolveInfo.defaultService = resolveInfo.services.get(0);
                    if (DBG) Log.d(TAG, "resolveAidLocked: DECISION: cat OTHER AID, " +
                            "routing to (default) " + resolveInfo.defaultService.getComponent());
                } else {
                    if (DBG) Log.d(TAG, "resolveAidLocked: DECISION: cat OTHER AID, routing all");
                }
            }
        }
        return resolveInfo;
    }

    void generateAidTreeLocked(List<ApduServiceInfo> services) {
        // Easiest is to just build the entire tree again
        mAidToServices.clear();
        for (ApduServiceInfo service : services) {
            if (DBG) Log.d(TAG, "generateAidTree component: " + service.getComponent());
            for (String aid : service.getAids()) {
                if (DBG) Log.d(TAG, "generateAidTree AID: " + aid);
                // Check if a mapping exists for this AID
                if (mAidToServices.containsKey(aid)) {
                    final ArrayList<ApduServiceInfo> aidServices = mAidToServices.get(aid);
                    aidServices.add(service);
                } else {
                    final ArrayList<ApduServiceInfo> aidServices =
                            new ArrayList<ApduServiceInfo>();
                    aidServices.add(service);
                    mAidToServices.put(aid, aidServices);
                }
            }
        }
    }

    void generateAidCategoriesLocked(List<ApduServiceInfo> services) {
        // Trash existing mapping
        mCategoryAids.clear();

        for (ApduServiceInfo service : services) {
            ArrayList<AidGroup> aidGroups = service.getAidGroups();
            if (aidGroups == null) continue;
            for (AidGroup aidGroup : aidGroups) {
                String groupCategory = aidGroup.getCategory();
                Set<String> categoryAids = mCategoryAids.get(groupCategory);
                if (categoryAids == null) {
                    categoryAids = new HashSet<String>();
                }
                categoryAids.addAll(aidGroup.getAids());
                mCategoryAids.put(groupCategory, categoryAids);
            }
        }
    }

    boolean updateFromSettingsLocked(int userId) {
        // Load current payment default from settings
        String name = Settings.Secure.getStringForUser(
                mContext.getContentResolver(), Settings.Secure.NFC_PAYMENT_DEFAULT_COMPONENT,
                userId);
        ComponentName newDefault = name != null ? ComponentName.unflattenFromString(name) : null;
        ComponentName oldDefault = mCategoryDefaults.put(CardEmulation.CATEGORY_PAYMENT,
                newDefault);
        if (DBG) Log.d(TAG, "Updating default component to: " + (name != null ?
                ComponentName.unflattenFromString(name) : "null"));
        return newDefault != oldDefault;
    }

    void generateAidCacheLocked() {
        mAidCache.clear();
        for (Map.Entry<String, ArrayList<ApduServiceInfo>> aidEntry:
                    mAidToServices.entrySet()) {
            String aid = aidEntry.getKey();
            if (!mAidCache.containsKey(aid)) {
                mAidCache.put(aid, resolveAidLocked(aidEntry.getValue(), aid));
            }
        }
    }

    void updateRoutingLocked() {
        if (!mNfcEnabled) {
            if (DBG) Log.d(TAG, "Not updating routing table because NFC is off.");
            return;
        }
        final Set<String> handledAids = new HashSet<String>();
        // For each AID, find interested services
        for (Map.Entry<String, AidResolveInfo> aidEntry:
                mAidCache.entrySet()) {
            String aid = aidEntry.getKey();
            AidResolveInfo resolveInfo = aidEntry.getValue();
            if (resolveInfo.services.size() == 0) {
                // No interested services, if there is a current routing remove it
                mRoutingManager.removeAid(aid);
            } else if (resolveInfo.defaultService != null) {
                // There is a default service set, route to that service
                mRoutingManager.setRouteForAid(aid, resolveInfo.defaultService.isOnHost());
            } else if (resolveInfo.services.size() == 1) {
                // Only one service, but not the default, must route to host
                // to ask the user to confirm.
                mRoutingManager.setRouteForAid(aid, true);
            } else if (resolveInfo.services.size() > 1) {
                // Multiple services, need to route to host to ask
                mRoutingManager.setRouteForAid(aid, true);
            }
            handledAids.add(aid);
        }
        // Now, find AIDs in the routing table that are no longer routed to
        // and remove them.
        Set<String> routedAids = mRoutingManager.getRoutedAids();
        for (String aid : routedAids) {
            if (!handledAids.contains(aid)) {
                if (DBG) Log.d(TAG, "Removing routing for AID " + aid + ", because " +
                        "there are no no interested services.");
                mRoutingManager.removeAid(aid);
            }
        }
        // And commit the routing
        mRoutingManager.commitRouting();
    }

    void showDefaultRemovedDialog() {
        Intent intent = new Intent(mContext, DefaultRemovedActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivityAsUser(intent, UserHandle.CURRENT);
    }

    void onPaymentDefaultRemoved(int userId, List<ApduServiceInfo> services) {
        int numPaymentServices = 0;
        ComponentName lastFoundPaymentService = null;
        for (ApduServiceInfo service : services) {
            if (service.hasCategory(CardEmulation.CATEGORY_PAYMENT))  {
                numPaymentServices++;
                lastFoundPaymentService = service.getComponent();
            }
        }
        if (DBG) Log.d(TAG, "Number of payment services is " +
                Integer.toString(numPaymentServices));
        if (numPaymentServices == 0) {
            if (DBG) Log.d(TAG, "Default removed, no services left.");
            // No payment services left, unset default and don't ask the user
            setDefaultServiceForCategory(userId, null,
                    CardEmulation.CATEGORY_PAYMENT);
        } else if (numPaymentServices == 1) {
            // Only one left, automatically make it the default
            if (DBG) Log.d(TAG, "Default removed, making remaining service default.");
            setDefaultServiceForCategory(userId, lastFoundPaymentService,
                    CardEmulation.CATEGORY_PAYMENT);
        } else if (numPaymentServices > 1) {
            // More than one left, unset default and ask the user if he wants
            // to set a new one
            if (DBG) Log.d(TAG, "Default removed, asking user to pick.");
            setDefaultServiceForCategory(userId, null,
                    CardEmulation.CATEGORY_PAYMENT);
            showDefaultRemovedDialog();
        }
    }

    void setDefaultIfNeededLocked(int userId, List<ApduServiceInfo> services) {
        int numPaymentServices = 0;
        ComponentName lastFoundPaymentService = null;
        for (ApduServiceInfo service : services) {
            if (service.hasCategory(CardEmulation.CATEGORY_PAYMENT))  {
                numPaymentServices++;
                lastFoundPaymentService = service.getComponent();
            }
        }
        if (numPaymentServices > 1) {
            // More than one service left, leave default unset
            if (DBG) Log.d(TAG, "No default set, more than one service left.");
        } else if (numPaymentServices == 1) {
            // Make single found payment service the default
            if (DBG) Log.d(TAG, "No default set, making single service default.");
            setDefaultServiceForCategory(userId, lastFoundPaymentService,
                    CardEmulation.CATEGORY_PAYMENT);
        } else {
            // No payment services left, leave default at null
            if (DBG) Log.d(TAG, "No default set, last payment service removed.");
        }
    }

    void checkDefaultsLocked(int userId, List<ApduServiceInfo> services) {
        ComponentName defaultPaymentService =
                getDefaultServiceForCategory(userId, CardEmulation.CATEGORY_PAYMENT, false);
        if (DBG) Log.d(TAG, "Current default: " + defaultPaymentService);
        if (defaultPaymentService != null) {
            // Validate the default is still installed and handling payment
            ApduServiceInfo serviceInfo = mServiceCache.getService(userId, defaultPaymentService);
            if (serviceInfo == null) {
                Log.e(TAG, "Default payment service unexpectedly removed.");
                onPaymentDefaultRemoved(userId, services);
            } else if (!serviceInfo.hasCategory(CardEmulation.CATEGORY_PAYMENT)) {
                if (DBG) Log.d(TAG, "Default payment service had payment category removed");
                onPaymentDefaultRemoved(userId, services);
            } else {
                // Default still exists and handles the category, nothing do
                if (DBG) Log.d(TAG, "Default payment service still ok.");
            }
        } else {
            // A payment service may have been removed, leaving only one;
            // in that case, automatically set that app as default.
            setDefaultIfNeededLocked(userId, services);
        }
        updateFromSettingsLocked(userId);
    }

    @Override
    public void onServicesUpdated(int userId, List<ApduServiceInfo> services) {
        synchronized (mLock) {
            if (ActivityManager.getCurrentUser() == userId) {
                // Rebuild our internal data-structures
                checkDefaultsLocked(userId, services);
                generateAidTreeLocked(services);
                generateAidCategoriesLocked(services);
                generateAidCacheLocked();
                updateRoutingLocked();
            } else {
                if (DBG) Log.d(TAG, "Ignoring update because it's not for the current user.");
            }
        }
    }

    public void invalidateCache(int currentUser) {
        mServiceCache.invalidateCache(currentUser);
    }

    public void onNfcDisabled() {
        synchronized (mLock) {
            mNfcEnabled = false;
        }
        mServiceCache.onNfcDisabled();
        mRoutingManager.onNfccRoutingTableCleared();
    }

    public void onNfcEnabled() {
        synchronized (mLock) {
            mNfcEnabled = true;
            updateFromSettingsLocked(ActivityManager.getCurrentUser());
        }
        mServiceCache.onNfcEnabled();
    }

    String dumpEntry(Map.Entry<String, AidResolveInfo> entry) {
        StringBuilder sb = new StringBuilder();
        sb.append("    \"" + entry.getKey() + "\"\n");
        ApduServiceInfo defaultService = entry.getValue().defaultService;
        ComponentName defaultComponent = defaultService != null ?
                defaultService.getComponent() : null;

        for (ApduServiceInfo service : entry.getValue().services) {
            sb.append("        ");
            if (service.getComponent().equals(defaultComponent)) {
                sb.append("*DEFAULT* ");
            }
            sb.append(service.getComponent() +
                    " (Description: " + service.getDescription() + ")\n");
        }
        return sb.toString();
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
       mServiceCache.dump(fd, pw, args);
       pw.println("AID cache entries: ");
       for (Map.Entry<String, AidResolveInfo> entry : mAidCache.entrySet()) {
           pw.println(dumpEntry(entry));
       }
       pw.println("Category defaults: ");
       for (Map.Entry<String, ComponentName> entry : mCategoryDefaults.entrySet()) {
           pw.println("    " + entry.getKey() + "->" + entry.getValue());
       }
       pw.println("");
    }
}
