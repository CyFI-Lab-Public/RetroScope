/*
 * Copyright (C) 2012 Google Inc.
 * Licensed to The Android Open Source Project.
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

package com.android.mail.utils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.Deque;
import java.util.Map;

/**
 * A Map of Deques. Each entry at key K has a deque of values V.
 *
 */
public class DequeMap<K, V> {

    public interface Visitor<V> {
        void visit(V item);
    }

    private final Map<K, Deque<V>> mMap = Maps.newHashMap();

    /**
     * Add a value V to the deque stored under key K.
     *
     */
    public void add(K key, V item) {
        Deque<V> pile = mMap.get(key);
        if (pile == null) {
            pile = Lists.newLinkedList();
            mMap.put(key, pile);
        }
        pile.add(item);
    }

    /**
     * Removes and returns the first value V from the deque of Vs for key K, or null if no such Vs
     * exist.
     *
     * @see Deque#poll()
     *
     * @param key
     * @return a V, or null
     */
    public V poll(K key) {
        final Deque<V> pile = mMap.get(key);
        if (pile == null) {
            return null;
        }
        return pile.poll();
    }

    /**
     * Returns, but does not remove, the first value V from the deque of Vs for key K, or null if
     * no such Vs exist.
     *
     * @see Deque#peek()
     *
     * @param key
     * @return a V, or null
     */
    public V peek(K key) {
        final Deque<V> pile = mMap.get(key);
        if (pile == null) {
            return null;
        }
        return pile.peek();
    }

    public void clear() {
        mMap.clear();
    }

    /**
     * Allows a {@link Visitor} to operate on each value V in this structure, irrespective of each
     * value's key. Modifying this map during iteration is not supported. Iteration order is not
     * guaranteed.
     *
     * @param visitor
     */
    // An iterator would also suffice, but this is easier to write and understand.
    public void visitAll(Visitor<V> visitor) {
        for (Map.Entry<K, Deque<V>> entry : mMap.entrySet()) {
            for (V item : entry.getValue()) {
                visitor.visit(item);
            }
        }
    }

}
