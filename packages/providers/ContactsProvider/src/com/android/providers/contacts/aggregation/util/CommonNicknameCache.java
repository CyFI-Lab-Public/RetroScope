/*
 * Copyright (C) 2009 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.providers.contacts.aggregation.util;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.android.providers.contacts.ContactsDatabaseHelper.NicknameLookupColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.Tables;
import com.google.android.collect.Maps;

import java.lang.ref.SoftReference;
import java.util.BitSet;
import java.util.HashMap;

/**
 * Cache for common nicknames.
 */
public class CommonNicknameCache  {

    // We will use this much memory (in bits) to optimize the nickname cluster lookup
    private static final int NICKNAME_BLOOM_FILTER_SIZE = 0x1FFF;   // =long[128]
    private BitSet mNicknameBloomFilter;

    private HashMap<String, SoftReference<String[]>> mNicknameClusterCache = Maps.newHashMap();

    private final SQLiteDatabase mDb;

    public CommonNicknameCache(SQLiteDatabase db) {
        mDb = db;
    }

    private final static class NicknameLookupPreloadQuery {
        public final static String TABLE = Tables.NICKNAME_LOOKUP;

        public final static String[] COLUMNS = new String[] {
            NicknameLookupColumns.NAME
        };

        public final static int NAME = 0;
    }

    /**
     * Read all known common nicknames from the database and populate a Bloom
     * filter using the corresponding hash codes. The idea is to eliminate most
     * of unnecessary database lookups for nicknames. Given a name, we will take
     * its hash code and see if it is set in the Bloom filter. If not, we will know
     * that the name is not in the database. If it is, we still need to run a
     * query.
     * <p>
     * Given the size of the filter and the expected size of the nickname table,
     * we should expect the combination of the Bloom filter and cache will
     * prevent around 98-99% of unnecessary queries from running.
     */
    private void preloadNicknameBloomFilter() {
        mNicknameBloomFilter = new BitSet(NICKNAME_BLOOM_FILTER_SIZE + 1);
        Cursor cursor = mDb.query(NicknameLookupPreloadQuery.TABLE,
                NicknameLookupPreloadQuery.COLUMNS,
                null, null, null, null, null);
        try {
            int count = cursor.getCount();
            for (int i = 0; i < count; i++) {
                cursor.moveToNext();
                String normalizedName = cursor.getString(NicknameLookupPreloadQuery.NAME);
                int hashCode = normalizedName.hashCode();
                mNicknameBloomFilter.set(hashCode & NICKNAME_BLOOM_FILTER_SIZE);
            }
        } finally {
            cursor.close();
        }
    }

    /**
     * Returns nickname cluster IDs or null. Maintains cache.
     */
    public String[] getCommonNicknameClusters(String normalizedName) {
        if (mNicknameBloomFilter == null) {
            preloadNicknameBloomFilter();
        }

        int hashCode = normalizedName.hashCode();
        if (!mNicknameBloomFilter.get(hashCode & NICKNAME_BLOOM_FILTER_SIZE)) {
            return null;
        }

        SoftReference<String[]> ref;
        String[] clusters = null;
        synchronized (mNicknameClusterCache) {
            if (mNicknameClusterCache.containsKey(normalizedName)) {
                ref = mNicknameClusterCache.get(normalizedName);
                if (ref == null) {
                    return null;
                }
                clusters = ref.get();
            }
        }

        if (clusters == null) {
            clusters = loadNicknameClusters(normalizedName);
            ref = clusters == null ? null : new SoftReference<String[]>(clusters);
            synchronized (mNicknameClusterCache) {
                mNicknameClusterCache.put(normalizedName, ref);
            }
        }
        return clusters;
    }

    private interface NicknameLookupQuery {
        String TABLE = Tables.NICKNAME_LOOKUP;

        String[] COLUMNS = new String[] {
            NicknameLookupColumns.CLUSTER
        };

        int CLUSTER = 0;
    }

    protected String[] loadNicknameClusters(String normalizedName) {
        String[] clusters = null;
        Cursor cursor = mDb.query(NicknameLookupQuery.TABLE, NicknameLookupQuery.COLUMNS,
                NicknameLookupColumns.NAME + "=?", new String[] { normalizedName },
                null, null, null);
        try {
            int count = cursor.getCount();
            if (count > 0) {
                clusters = new String[count];
                for (int i = 0; i < count; i++) {
                    cursor.moveToNext();
                    clusters[i] = cursor.getString(NicknameLookupQuery.CLUSTER);
                }
            }
        } finally {
            cursor.close();
        }
        return clusters;
    }
}
