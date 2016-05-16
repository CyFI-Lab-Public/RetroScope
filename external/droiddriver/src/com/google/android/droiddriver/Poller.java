/*
 * Copyright (C) 2013 DroidDriver committers
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

package com.google.android.droiddriver;

import com.google.android.droiddriver.exceptions.ElementNotFoundException;
import com.google.android.droiddriver.finders.Finder;

/**
 * Interface for polling mechanism.
 */
public interface Poller {
  /**
   * Interface for a callback to be invoked when {@link #pollFor} times out.
   */
  interface TimeoutListener {
    /**
     * Called when {@link #pollFor} times out. Should return quickly (no
     * polling).
     */
    void onTimeout(DroidDriver driver, Finder finder);
  }

  /**
   * Interface for a callback to be invoked when {@link #pollFor} polls.
   */
  interface PollingListener {
    /**
     * Called when {@link #pollFor} polls. Should return quickly (no polling).
     */
    void onPolling(DroidDriver driver, Finder finder);
  }
  /**
   * Interface for removing a listener.
   */
  interface ListenerRemover {
    /**
     * A ListenerRemover that does nothing. Can be used as initial value for
     * ListenerRemovers.
     */
    ListenerRemover NOP_LISTENER_REMOVER = new ListenerRemover() {
      @Override
      public void remove() {}
    };

    /**
     * Removes the associated listener.
     */
    void remove();
  }

  /**
   * Used by Poller to check conditions.
   *
   * @param <T> type of the value returned by {@link #check}
   */
  interface ConditionChecker<T> {
    /**
     * Checks condition that overriding methods provide.
     *
     * @throws UnsatisfiedConditionException If the condition is not met
     */
    T check(DroidDriver driver, Finder finder) throws UnsatisfiedConditionException;
  }

  /** Thrown to indicate condition not met. Used in {@link ConditionChecker}. */
  @SuppressWarnings("serial")
  class UnsatisfiedConditionException extends Exception {
  }

  /**
   * A ConditionChecker that returns the matching {@link UiElement}.
   */
  ConditionChecker<UiElement> EXISTS = new ConditionChecker<UiElement>() {
    @Override
    public UiElement check(DroidDriver driver, Finder finder) throws UnsatisfiedConditionException {
      try {
        return driver.find(finder);
      } catch (ElementNotFoundException e) {
        throw new UnsatisfiedConditionException();
      }
    }

    @Override
    public String toString() {
      return "to appear";
    }
  };
  /**
   * A ConditionChecker that does not throw only if the matching
   * {@link UiElement} is gone.
   */
  ConditionChecker<Void> GONE = new ConditionChecker<Void>() {
    @Override
    public Void check(DroidDriver driver, Finder finder) throws UnsatisfiedConditionException {
      if (driver.has(finder)) {
        throw new UnsatisfiedConditionException();
      }
      return null;
    }

    @Override
    public String toString() {
      return "to disappear";
    }
  };

  /**
   * Polls until {@code checker} does not throw
   * {@link UnsatisfiedConditionException}, up to the default timeout.
   *
   * @return An object of type T returned by {@code checker}
   */
  <T> T pollFor(DroidDriver driver, Finder finder, ConditionChecker<T> checker);

  /**
   * Polls until {@code checker} does not throw
   * {@link UnsatisfiedConditionException}, up to {@code timeoutMillis}.
   *
   * @return An object of type T returned by {@code checker}
   */
  <T> T pollFor(DroidDriver driver, Finder finder, ConditionChecker<T> checker, long timeoutMillis);

  /**
   * Adds a {@link TimeoutListener}.
   */
  ListenerRemover addListener(TimeoutListener timeoutListener);

  /**
   * Adds a {@link PollingListener}.
   */
  ListenerRemover addListener(PollingListener pollingListener);

  /**
   * Sets default timeoutMillis.
   */
  void setTimeoutMillis(long timeoutMillis);

  /**
   * @return default timeoutMillis
   */
  long getTimeoutMillis();

  /**
   * Sets intervalMillis.
   */
  void setIntervalMillis(long intervalMillis);

  /**
   * @return intervalMillis
   */
  long getIntervalMillis();
}
