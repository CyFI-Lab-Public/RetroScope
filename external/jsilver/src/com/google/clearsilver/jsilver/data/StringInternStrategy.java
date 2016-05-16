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

package com.google.clearsilver.jsilver.data;

/**
 * Encapsulates the {@code WeakInterningPool<String>} functionality with added optimizations. To be
 * used to optimize the memory usage and garbage collection during text processing.
 */
public interface StringInternStrategy {
  /**
   * Interns a String object in a pool and returns a String equal to the one provided.
   * 
   * <p>
   * If there exists a String in the pool equal to the provided value then it will be returned.
   * Otherwise provided String <b>may</b> be interned.
   * 
   * <p>
   * There is no guarantees on when the pool will return the same object as provided. It is possible
   * that value == intern(value) will never be true.
   * 
   * @param value String to be interned
   * @return a String that is equal to the one provided.
   */
  String intern(String value);
}
