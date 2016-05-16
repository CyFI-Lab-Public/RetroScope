/**
 * Copyright (c) 2011, Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.mail.utils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A simple in-memory LRU cache, which is trivial to implement on top
 * of JDK's {@link LinkedHashMap}.
 *
 * LRU policy is ensured by the underlying LinkedHashMap functionality.
 */
public final class LruCache<K, V> extends LinkedHashMap<K, V> {
    private static final long serialVersionUID = 1L;
    private final int maxCapacity;

    /**
     * Creates an instance of LRUCache, with given capacity.
     *
     * @param capacity maximum number of elements in the cache. This is also
     * used as initialCapacity i.e. memory is allocated upfront.
     */
    public LruCache(int capacity) {
        this(capacity, capacity);
    }

    /**
     * Creates an instance of LRUCache, with given capacity.
     *
     * @param initialCapacity initial capacity of the cache.
     * @param maxCapacity maximum number of elements in the cache.
     */
    private LruCache(int initialCapacity, int maxCapacity) {
        super(initialCapacity, (float) 0.75, true);
        this.maxCapacity = maxCapacity;
    }

    // These methods are needed because the default implementation of LinkedHashMap is *not*
    // synchronized.
    /**
     * Gets an element from the cache, returning the element if found, or null if not
     * @param key
     * @return
     */
    public synchronized V getElement(K key) {
        return get(key);
    }

    /**
     * Puts an element into the cache.
     * @param key
     * @param value, a non-null value.
     */
    public synchronized void putElement(K key, V value) {
        put(key, value);
    }

    /**
     * Removes an element from the cache. Returns the removed element, or null if no such element
     * existed in the cache.
     * @param key
     * @return
     */
    public synchronized V removeElement(K key) {
        return remove(key);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Necessary to override because HashMap increases the capacity of the
     * hashtable before inserting the elements. However, here we call put() which
     * checks if we can remove the eldest element before increasing the capacity.
     */
    @Override
    public synchronized void putAll(Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    /**
     * This method is called by LinkedHashMap to check whether the eldest entry
     * should be removed.
     *
     * @param eldest
     * @return true if element should be removed.
     */
    @Override
    protected synchronized boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxCapacity;
    }
}
