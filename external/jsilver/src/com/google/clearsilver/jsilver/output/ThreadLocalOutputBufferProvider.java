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

package com.google.clearsilver.jsilver.output;

/**
 * Implementation of OutputBufferProvider that reuses the same StringBuilder in each Thread.
 */
public class ThreadLocalOutputBufferProvider implements OutputBufferProvider {

  private final ThreadLocal<StringBuilder> pool;
  private final ThreadLocal<Boolean> available;

  public ThreadLocalOutputBufferProvider(final int bufferSize) {
    pool = new ThreadLocal<StringBuilder>() {
      protected StringBuilder initialValue() {
        return new StringBuilder(bufferSize);
      }
    };
    available = new ThreadLocal<Boolean>() {
      protected Boolean initialValue() {
        return true;
      }
    };
  }

  @Override
  public Appendable get() {
    if (!available.get()) {
      throw new IllegalStateException("Thread buffer is not free.");
    }
    StringBuilder sb = pool.get();
    available.set(false);
    sb.setLength(0);
    return sb;
  }

  @Override
  public void release(Appendable buffer) {
    if (buffer != pool.get()) {
      throw new IllegalArgumentException("Can't release buffer that does not "
          + "correspond to this thread.");
    }
    available.set(true);
  }
}
