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

package com.google.clearsilver.jsilver.values;

import com.google.clearsilver.jsilver.autoescape.EscapeMode;
import com.google.clearsilver.jsilver.data.TypeConverter;

/**
 * Base class for values of variant types (i.e. those that can be treated as different types at
 * runtime - e.g. "33").
 * 
 * @see Value
 */
abstract class VariantValue extends Value {

  private static final String EMPTY = "";

  VariantValue(EscapeMode escapeMode, boolean partiallyEscaped) {
    super(escapeMode, partiallyEscaped);
  }

  protected abstract String value();

  @Override
  public boolean asBoolean() {
    return TypeConverter.asBoolean(value());
  }

  @Override
  public String asString() {
    String value = value();
    return value == null ? EMPTY : value;
  }

  @Override
  public int asNumber() {
    // TODO: Cache the result for constant values (or just get rid of this class)
    return TypeConverter.asNumber(value());
  }

  @Override
  public boolean isEmpty() {
    return asString().isEmpty();
  }

}
