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

package com.google.clearsilver.jsilver.functions;

import com.google.clearsilver.jsilver.values.Value;

/**
 * Plugin for JSilver functions made available to templates. e.g &lt;cs var:my_function(x, y) &gt;
 */
public interface Function {

  /**
   * Execute a function. Should always return a result.
   */
  Value execute(Value... args);

  boolean isEscapingFunction();

}
