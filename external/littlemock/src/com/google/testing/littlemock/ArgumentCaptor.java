/*
 * Copyright 2011 Google Inc.
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

package com.google.testing.littlemock;

import java.util.List;

/**
 * Simple capture object for use in tests.
 *
 * @author hugohudson@gmail.com (Hugo Hudson)
 *
 * @param <T> the type we are going to capture
 */
public interface ArgumentCaptor<T extends Object> extends LittleMock.ArgumentMatcher {
  /** Gets the last value captured, or null if no value has been captured. */
  public T getValue();

  /** Gets the list of all values that have been captured. */
  public List<T> getAllValues();

  /** Use this argument captor to perform the capture. */
  public T capture();

}
