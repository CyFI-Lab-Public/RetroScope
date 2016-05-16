/*
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.clearsilver.jsilver.adaptor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * This class implements a cache of a list of loadpaths and a file name to the absolute file name
 * where the file is located on the filesystem. The purpose is to avoid filesystem calls for common
 * operations, like in which of these directories does this file exist? This class is threadsafe.
 * 
 * Some of this code is copied from {@link com.google.clearsilver.base.CSFileCache}.
 */
public class LoadPathToFileCache {

  private final LRUCache<String, String> cache;
  private final ReadWriteLock cacheLock = new ReentrantReadWriteLock();

  public LoadPathToFileCache(int capacity) {
    cache = new LRUCache<String, String>(capacity);
  }

  /**
   * Lookup in the cache to see if we have a mapping from the given loadpaths and filename to an
   * absolute file path.
   * 
   * @param loadPaths the ordered list of directories to search for the file.
   * @param filename the name of the file.
   * @return the absolute filepath location of the file, or {@code null} if not in the cache.
   */
  public String lookup(List<String> loadPaths, String filename) {
    String filePathMapKey = makeCacheKey(loadPaths, filename);
    cacheLock.readLock().lock();
    try {
      return cache.get(filePathMapKey);
    } finally {
      cacheLock.readLock().unlock();
    }
  }

  /**
   * Add a new mapping to the cache.
   * 
   * @param loadPaths the ordered list of directories to search for the file.
   * @param filename the name of the file.
   * @param filePath the absolute filepath location of the file
   */
  public void add(List<String> loadPaths, String filename, String filePath) {
    String filePathMapKey = makeCacheKey(loadPaths, filename);
    cacheLock.writeLock().lock();
    try {
      cache.put(filePathMapKey, filePath);
    } finally {
      cacheLock.writeLock().unlock();
    }
  }

  public static String makeCacheKey(List<String> loadPaths, String filename) {
    if (loadPaths == null) {
      throw new NullPointerException("Loadpaths cannot be null");
    }
    if (filename == null) {
      throw new NullPointerException("Filename cannot be null");
    }
    String loadPathsHash = Long.toHexString(hashLoadPath(loadPaths));
    StringBuilder sb = new StringBuilder(loadPathsHash);
    sb.append('|').append(filename);
    return sb.toString();
  }

  /**
   * Generate a hashCode to represent the ordered list of loadpaths. Used as a key into the fileMap
   * since a concatenation of the loadpaths is likely to be huge (greater than 1K) but very
   * repetitive. Algorithm comes from Effective Java by Joshua Bloch.
   * <p>
   * We don't use the builtin hashCode because it is only 32-bits, and while the expect different
   * values of loadpaths is very small, we want to minimize any chance of collision since we use the
   * hash as the key and throw away the loadpath list
   * 
   * @param list an ordered list of loadpaths.
   * @return a long representing a hash of the loadpaths.
   */
  static long hashLoadPath(List<String> list) {
    long hash = 17;
    for (String path : list) {
      hash = 37 * hash + path.hashCode();
    }
    return hash;
  }

  /**
   * This code is copied from {@link com.google.common.cache.LRUCache} but is distilled to basics in
   * order to not require importing Google code. Hopefully there is an open-source implementation,
   * although given the design of LinkHashMap, this is trivial.
   */
  static class LRUCache<K, V> extends LinkedHashMap<K, V> {

    private final int capacity;

    LRUCache(int capacity) {
      super(capacity, 0.75f, true);
      this.capacity = capacity;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Necessary to override because HashMap increases the capacity of the hashtable before
     * inserting the elements. However, here we have set the max capacity already and will instead
     * remove eldest elements instead of increasing capacity.
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
      for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
        put(e.getKey(), e.getValue());
      }
    }

    /**
     * This method is called by LinkedHashMap to check whether the eldest entry should be removed.
     * 
     * @param eldest
     * @return true if element should be removed.
     */
    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
      return size() > capacity;
    }
  }
}
