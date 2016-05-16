/**
 * Copyright (c) 2012, Google Inc.
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

import com.google.common.collect.Lists;

import java.util.Deque;

/**
 * This class maintains a cache of soft references to objects.  This allows callers to use a pool
 * of object instances
 * @param <T>
 */
public class ObjectCache<T> {
    public interface Callback<T> {
        T newInstance();
        void onObjectReleased(T object);
    }

    private final Deque<T> mDataStore = Lists.newLinkedList();

    private final Callback<T> mCallback;
    private final int mMaxSize;

    /**
     * Creates a new ObjectCache instance
     * @param callbacks Callback object that that will return a new instance of the object, and
     *        perform any cleanup when the object is released back to the cache
     */
    public ObjectCache(Callback<T> callbacks, int maxSize) {
        mCallback = callbacks;
        mMaxSize = maxSize;
    }

    /**
     * Returns an instance of the specified object type, creating a new instance if needed.
     */
    public T get() {
        T result;
        synchronized (mDataStore) {
            result = mDataStore.poll();
        }
        if (result == null) {
            result = mCallback.newInstance();
        }
        return result;
    }

    /**
     * Releases the specified object back to the cache.  Once an object is released, it can be
     * returned by subsequent calls to get()
     */
    public void release(T objectToCache) {
        synchronized (mDataStore) {
            if (mDataStore.size() < mMaxSize) {
                mCallback.onObjectReleased(objectToCache);
                mDataStore.add(objectToCache);
            }
        }
    }
}
