/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.phone;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.ContactsContract.CommonDataKinds.Callable;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Data;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

import java.util.HashMap;
import java.util.Map.Entry;

/**
 * Holds "custom ringtone" and "send to voicemail" information for each contact as a fallback of
 * contacts database. The cached information is refreshed periodically and used when database
 * lookup (via ContentResolver) takes longer time than expected.
 *
 * The data inside this class shouldn't be treated as "primary"; they may not reflect the
 * latest information stored in the original database.
 */
public class CallerInfoCache {
    private static final String LOG_TAG = CallerInfoCache.class.getSimpleName();
    private static final boolean DBG =
            (PhoneGlobals.DBG_LEVEL >= 1) && (SystemProperties.getInt("ro.debuggable", 0) == 1);

    /** This must not be set to true when submitting changes. */
    private static final boolean VDBG = false;

    /**
     * Interval used with {@link AlarmManager#setInexactRepeating(int, long, long, PendingIntent)},
     * which means the actually interval may not be very accurate.
     */
    private static final int CACHE_REFRESH_INTERVAL = 8 * 60 * 60 * 1000; // 8 hours in millis.

    public static final int MESSAGE_UPDATE_CACHE = 0;

    // Assuming DATA.DATA1 corresponds to Phone.NUMBER and SipAddress.ADDRESS, we just use
    // Data columns as much as we can. One exception: because normalized numbers won't be used in
    // SIP cases, Phone.NORMALIZED_NUMBER is used as is instead of using Data.
    private static final String[] PROJECTION = new String[] {
        Data.DATA1,                  // 0
        Phone.NORMALIZED_NUMBER,     // 1
        Data.CUSTOM_RINGTONE,        // 2
        Data.SEND_TO_VOICEMAIL       // 3
    };

    private static final int INDEX_NUMBER            = 0;
    private static final int INDEX_NORMALIZED_NUMBER = 1;
    private static final int INDEX_CUSTOM_RINGTONE   = 2;
    private static final int INDEX_SEND_TO_VOICEMAIL = 3;

    private static final String SELECTION = "("
            + "(" + Data.CUSTOM_RINGTONE + " IS NOT NULL OR " + Data.SEND_TO_VOICEMAIL + "=1)"
            + " AND " + Data.DATA1 + " IS NOT NULL)";

    public static class CacheEntry {
        public final String customRingtone;
        public final boolean sendToVoicemail;
        public CacheEntry(String customRingtone, boolean shouldSendToVoicemail) {
            this.customRingtone = customRingtone;
            this.sendToVoicemail = shouldSendToVoicemail;
        }

        @Override
        public String toString() {
            return "ringtone: " + customRingtone + ", " + sendToVoicemail;
        }
    }

    private class CacheAsyncTask extends AsyncTask<Void, Void, Void> {

        private PowerManager.WakeLock mWakeLock;

        /**
         * Call {@link PowerManager.WakeLock#acquire} and call {@link AsyncTask#execute(Object...)},
         * guaranteeing the lock is held during the asynchronous task.
         */
        public void acquireWakeLockAndExecute() {
            // Prepare a separate partial WakeLock than what PhoneApp has so to avoid
            // unnecessary conflict.
            PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, LOG_TAG);
            mWakeLock.acquire();
            execute();
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (DBG) log("Start refreshing cache.");
            refreshCacheEntry();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (VDBG) log("CacheAsyncTask#onPostExecute()");
            super.onPostExecute(result);
            releaseWakeLock();
        }

        @Override
        protected void onCancelled(Void result) {
            if (VDBG) log("CacheAsyncTask#onCanceled()");
            super.onCancelled(result);
            releaseWakeLock();
        }

        private void releaseWakeLock() {
            if (mWakeLock != null && mWakeLock.isHeld()) {
                mWakeLock.release();
            }
        }
    }

    private final Context mContext;

    /**
     * The mapping from number to CacheEntry.
     *
     * The number will be:
     * - last 7 digits of each "normalized phone number when it is for PSTN phone call, or
     * - a full SIP address for SIP call
     *
     * When cache is being refreshed, this whole object will be replaced with a newer object,
     * instead of updating elements inside the object.  "volatile" is used to make
     * {@link #getCacheEntry(String)} access to the newer one every time when the object is
     * being replaced.
     */
    private volatile HashMap<String, CacheEntry> mNumberToEntry;

    /**
     * Used to remember if the previous task is finished or not. Should be set to null when done.
     */
    private CacheAsyncTask mCacheAsyncTask;

    public static CallerInfoCache init(Context context) {
        if (DBG) log("init()");
        CallerInfoCache cache = new CallerInfoCache(context);
        // The first cache should be available ASAP.
        cache.startAsyncCache();
        cache.setRepeatingCacheUpdateAlarm();
        return cache;
    }

    private CallerInfoCache(Context context) {
        mContext = context;
        mNumberToEntry = new HashMap<String, CacheEntry>();
    }

    /* package */ void startAsyncCache() {
        if (DBG) log("startAsyncCache");

        if (mCacheAsyncTask != null) {
            Log.w(LOG_TAG, "Previous cache task is remaining.");
            mCacheAsyncTask.cancel(true);
        }
        mCacheAsyncTask = new CacheAsyncTask();
        mCacheAsyncTask.acquireWakeLockAndExecute();
    }

    /**
     * Set up periodic alarm for cache update.
     */
    private void setRepeatingCacheUpdateAlarm() {
        if (DBG) log("setRepeatingCacheUpdateAlarm");

        Intent intent = new Intent(CallerInfoCacheUpdateReceiver.ACTION_UPDATE_CALLER_INFO_CACHE);
        intent.setClass(mContext, CallerInfoCacheUpdateReceiver.class);
        PendingIntent pendingIntent =
                PendingIntent.getBroadcast(mContext, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        // We don't need precise timer while this should be power efficient.
        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
                SystemClock.uptimeMillis() + CACHE_REFRESH_INTERVAL,
                CACHE_REFRESH_INTERVAL, pendingIntent);
    }

    private void refreshCacheEntry() {
        if (VDBG) log("refreshCacheEntry() started");

        // There's no way to know which part of the database was updated. Also we don't want
        // to block incoming calls asking for the cache. So this method just does full query
        // and replaces the older cache with newer one. To refrain from blocking incoming calls,
        // it keeps older one as much as it can, and replaces it with newer one inside a very small
        // synchronized block.

        Cursor cursor = null;
        try {
            cursor = mContext.getContentResolver().query(Callable.CONTENT_URI,
                    PROJECTION, SELECTION, null, null);
            if (cursor != null) {
                // We don't want to block real in-coming call, so prepare a completely fresh
                // cache here again, and replace it with older one.
                final HashMap<String, CacheEntry> newNumberToEntry =
                        new HashMap<String, CacheEntry>(cursor.getCount());

                while (cursor.moveToNext()) {
                    final String number = cursor.getString(INDEX_NUMBER);
                    String normalizedNumber = cursor.getString(INDEX_NORMALIZED_NUMBER);
                    if (normalizedNumber == null) {
                        // There's no guarantee normalized numbers are available every time and
                        // it may become null sometimes. Try formatting the original number.
                        normalizedNumber = PhoneNumberUtils.normalizeNumber(number);
                    }
                    final String customRingtone = cursor.getString(INDEX_CUSTOM_RINGTONE);
                    final boolean sendToVoicemail = cursor.getInt(INDEX_SEND_TO_VOICEMAIL) == 1;

                    if (PhoneNumberUtils.isUriNumber(number)) {
                        // SIP address case
                        putNewEntryWhenAppropriate(
                                newNumberToEntry, number, customRingtone, sendToVoicemail);
                    } else {
                        // PSTN number case
                        // Each normalized number may or may not have full content of the number.
                        // Contacts database may contain +15001234567 while a dialed number may be
                        // just 5001234567. Also we may have inappropriate country
                        // code in some cases (e.g. when the location of the device is inconsistent
                        // with the device's place). So to avoid confusion we just rely on the last
                        // 7 digits here. It may cause some kind of wrong behavior, which is
                        // unavoidable anyway in very rare cases..
                        final int length = normalizedNumber.length();
                        final String key = length > 7
                                ? normalizedNumber.substring(length - 7, length)
                                        : normalizedNumber;
                        putNewEntryWhenAppropriate(
                                newNumberToEntry, key, customRingtone, sendToVoicemail);
                    }
                }

                if (VDBG) {
                    Log.d(LOG_TAG, "New cache size: " + newNumberToEntry.size());
                    for (Entry<String, CacheEntry> entry : newNumberToEntry.entrySet()) {
                        Log.d(LOG_TAG, "Number: " + entry.getKey() + " -> " + entry.getValue());
                    }
                }

                mNumberToEntry = newNumberToEntry;

                if (DBG) {
                    log("Caching entries are done. Total: " + newNumberToEntry.size());
                }
            } else {
                // Let's just wait for the next refresh..
                //
                // If the cursor became null at that exact moment, probably we don't want to
                // drop old cache. Also the case is fairly rare in usual cases unless acore being
                // killed, so we don't take care much of this case.
                Log.w(LOG_TAG, "cursor is null");
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        if (VDBG) log("refreshCacheEntry() ended");
    }

    private void putNewEntryWhenAppropriate(HashMap<String, CacheEntry> newNumberToEntry,
            String numberOrSipAddress, String customRingtone, boolean sendToVoicemail) {
        if (newNumberToEntry.containsKey(numberOrSipAddress)) {
            // There may be duplicate entries here and we should prioritize
            // "send-to-voicemail" flag in any case.
            final CacheEntry entry = newNumberToEntry.get(numberOrSipAddress);
            if (!entry.sendToVoicemail && sendToVoicemail) {
                newNumberToEntry.put(numberOrSipAddress,
                        new CacheEntry(customRingtone, sendToVoicemail));
            }
        } else {
            newNumberToEntry.put(numberOrSipAddress,
                    new CacheEntry(customRingtone, sendToVoicemail));
        }
    }

    /**
     * Returns CacheEntry for the given number (PSTN number or SIP address).
     *
     * @param number OK to be unformatted.
     * @return CacheEntry to be used. Maybe null if there's no cache here. Note that this may
     * return null when the cache itself is not ready. BE CAREFUL. (or might be better to throw
     * an exception)
     */
    public CacheEntry getCacheEntry(String number) {
        if (mNumberToEntry == null) {
            // Very unusual state. This implies the cache isn't ready during the request, while
            // it should be prepared on the boot time (i.e. a way before even the first request).
            Log.w(LOG_TAG, "Fallback cache isn't ready.");
            return null;
        }

        CacheEntry entry;
        if (PhoneNumberUtils.isUriNumber(number)) {
            if (VDBG) log("Trying to lookup " + number);

            entry = mNumberToEntry.get(number);
        } else {
            final String normalizedNumber = PhoneNumberUtils.normalizeNumber(number);
            final int length = normalizedNumber.length();
            final String key =
                    (length > 7 ? normalizedNumber.substring(length - 7, length)
                            : normalizedNumber);
            if (VDBG) log("Trying to lookup " + key);

            entry = mNumberToEntry.get(key);
        }
        if (VDBG) log("Obtained " + entry);
        return entry;
    }

    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
}
