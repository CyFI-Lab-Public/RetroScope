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
package com.google.android.droiddriver.scroll;

import com.google.android.droiddriver.actions.ScrollDirection;
import com.google.android.droiddriver.exceptions.ActionException;

/**
 * Interfaces and constants for scroll directions.
 */
public interface Direction {
  /** Logical directions */
  public enum LogicalDirection {
    FORWARD, BACKWARD;
  }

  public enum Axis {
    HORIZONTAL {
      private final ScrollDirection[] directions = {ScrollDirection.LEFT, ScrollDirection.RIGHT};

      @Override
      public ScrollDirection[] getDirections() {
        return directions;
      }
    },
    VERTICAL {
      private final ScrollDirection[] directions = {ScrollDirection.UP, ScrollDirection.DOWN};

      @Override
      public ScrollDirection[] getDirections() {
        return directions;
      }
    };
    public abstract ScrollDirection[] getDirections();
  }

  /**
   * Converts ScrollDirection to LogicalDirection.
   */
  public interface PhysicalToLogicalConverter {
    /**
     * Converts ScrollDirection to LogicalDirection. It's possible to override
     * this for RTL (right-to-left) views, for example.
     */
    LogicalDirection toLogicalDirection(ScrollDirection direction);

    /** Follows standard directions: up-to-down, left-to-right */
    PhysicalToLogicalConverter STANDARD_CONVERTER = new PhysicalToLogicalConverter() {
      @Override
      public LogicalDirection toLogicalDirection(ScrollDirection direction) {
        switch (direction) {
          case UP:
            return LogicalDirection.BACKWARD;
          case DOWN:
            return LogicalDirection.FORWARD;
          case LEFT:
            return LogicalDirection.BACKWARD;
          case RIGHT:
            return LogicalDirection.FORWARD;
        }
        throw new ActionException("Unknown scroll direction: " + direction);
      }
    };
  }
}
