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
import com.google.clearsilver.jsilver.data.DataContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Dynamic typing system used by JSilver interpreter. A value (e.g. "2") can act as a string,
 * integer and boolean. Values can be literal or references to variables held elsewhere (e.g. in
 * external data structures such as HDF).
 */
public abstract class Value {

  private static final Map<EscapeMode, Value> EMPTY_PART_ESCAPED;
  private static final Map<EscapeMode, Value> EMPTY_UNESCAPED;
  private static final Map<EscapeMode, Value> ZERO_PART_ESCAPED;
  private static final Map<EscapeMode, Value> ZERO_UNESCAPED;
  private static final Map<EscapeMode, Value> ONE_PART_ESCAPED;
  private static final Map<EscapeMode, Value> ONE_UNESCAPED;

  static {
    // Currently a Value's EscapeMode is either ESCAPE_NONE (no escaping) or
    // ESCAPE_IS_CONSTANT (is a constant or has some escaping applied).
    // This may change in the future if we implement stricter auto escaping.
    // See EscapeMode.combineModes.
    EMPTY_PART_ESCAPED = new HashMap<EscapeMode, Value>(2);
    EMPTY_PART_ESCAPED.put(EscapeMode.ESCAPE_NONE,
        new StringValue("", EscapeMode.ESCAPE_NONE, true));
    EMPTY_PART_ESCAPED.put(EscapeMode.ESCAPE_IS_CONSTANT, new StringValue("",
        EscapeMode.ESCAPE_IS_CONSTANT, true));

    EMPTY_UNESCAPED = new HashMap<EscapeMode, Value>(2);
    EMPTY_UNESCAPED.put(EscapeMode.ESCAPE_NONE, new StringValue("", EscapeMode.ESCAPE_NONE, false));
    EMPTY_UNESCAPED.put(EscapeMode.ESCAPE_IS_CONSTANT, new StringValue("",
        EscapeMode.ESCAPE_IS_CONSTANT, false));

    ZERO_PART_ESCAPED = new HashMap<EscapeMode, Value>(2);
    ZERO_PART_ESCAPED.put(EscapeMode.ESCAPE_NONE, new NumberValue(0, EscapeMode.ESCAPE_NONE, true));
    ZERO_PART_ESCAPED.put(EscapeMode.ESCAPE_IS_CONSTANT, new NumberValue(0,
        EscapeMode.ESCAPE_IS_CONSTANT, true));

    ZERO_UNESCAPED = new HashMap<EscapeMode, Value>(2);
    ZERO_UNESCAPED.put(EscapeMode.ESCAPE_NONE, new NumberValue(0, EscapeMode.ESCAPE_NONE, false));
    ZERO_UNESCAPED.put(EscapeMode.ESCAPE_IS_CONSTANT, new NumberValue(0,
        EscapeMode.ESCAPE_IS_CONSTANT, false));

    ONE_PART_ESCAPED = new HashMap<EscapeMode, Value>(2);
    ONE_PART_ESCAPED.put(EscapeMode.ESCAPE_NONE, new NumberValue(1, EscapeMode.ESCAPE_NONE, true));
    ONE_PART_ESCAPED.put(EscapeMode.ESCAPE_IS_CONSTANT, new NumberValue(1,
        EscapeMode.ESCAPE_IS_CONSTANT, true));

    ONE_UNESCAPED = new HashMap<EscapeMode, Value>(2);
    ONE_UNESCAPED.put(EscapeMode.ESCAPE_NONE, new NumberValue(1, EscapeMode.ESCAPE_NONE, false));
    ONE_UNESCAPED.put(EscapeMode.ESCAPE_IS_CONSTANT, new NumberValue(1,
        EscapeMode.ESCAPE_IS_CONSTANT, false));
  }

  /**
   * True if either the {@code Value} was escaped, or it was created from a combination of escaped
   * and unescaped values.
   */
  private final boolean partiallyEscaped;
  private final EscapeMode escapeMode;

  public Value(EscapeMode escapeMode, boolean partiallyEscaped) {
    this.escapeMode = escapeMode;
    this.partiallyEscaped = partiallyEscaped;
  }

  /**
   * Fetch value as boolean. All non empty strings and numbers != 0 are treated as true.
   */
  public abstract boolean asBoolean();

  /**
   * Fetch value as string.
   */
  public abstract String asString();

  /**
   * Fetch value as number. If number is not parseable, 0 is returned (this is consistent with
   * ClearSilver).
   */
  public abstract int asNumber();

  /**
   * Whether this value exists. Literals always return true, but variable references will return
   * false if the value behind it is null.
   */
  public abstract boolean exists();

  public abstract boolean isEmpty();

  /**
   * Create a literal value using an int.
   */
  public static Value literalValue(int value, EscapeMode mode, boolean partiallyEscaped) {
    return getIntValue(mode, partiallyEscaped, value);
  }

  /**
   * Create a literal value using a String.
   */
  public static Value literalValue(String value, EscapeMode mode, boolean partiallyEscaped) {
    if (value.isEmpty()) {
      Value v = (partiallyEscaped ? EMPTY_PART_ESCAPED : EMPTY_UNESCAPED).get(mode);
      if (v != null) {
        return v;
      }
    }

    return new StringValue(value, mode, partiallyEscaped);
  }

  /**
   * Create a literal value using a boolean.
   */
  public static Value literalValue(boolean value, EscapeMode mode, boolean partiallyEscaped) {
    return getIntValue(mode, partiallyEscaped, value ? 1 : 0);
  }

  private static Value getIntValue(EscapeMode mode, boolean partiallyEscaped, int num) {
    Value v = null;
    if (num == 0) {
      v = (partiallyEscaped ? ZERO_PART_ESCAPED : ZERO_UNESCAPED).get(mode);
    } else if (num == 1) {
      v = (partiallyEscaped ? ONE_PART_ESCAPED : ONE_UNESCAPED).get(mode);
    }

    if (v != null) {
      return v;
    }

    return new NumberValue(num, mode, partiallyEscaped);
  }

  /**
   * Create a literal value using an int with a {@code escapeMode} of {@code
   * EscapeMode.ESCAPE_IS_CONSTANT} and {@code partiallyEscaped} based on the {@code
   * partiallyEscaped} values of the inputs.
   * 
   * @param value integer value of the literal
   * @param inputs Values that were used to compute the integer value.
   */
  public static Value literalConstant(int value, Value... inputs) {
    boolean isPartiallyEscaped = false;
    for (Value input : inputs) {
      if (input.isPartiallyEscaped()) {
        isPartiallyEscaped = true;
        break;
      }
    }
    return literalValue(value, EscapeMode.ESCAPE_IS_CONSTANT, isPartiallyEscaped);
  }

  /**
   * Create a literal value using a string with a {@code escapeMode} of {@code
   * EscapeMode.ESCAPE_IS_CONSTANT} and {@code partiallyEscaped} based on the {@code
   * partiallyEscaped} values of the inputs.
   * 
   * @param value String value of the literal
   * @param inputs Values that were used to compute the string value.
   */
  public static Value literalConstant(String value, Value... inputs) {
    boolean isPartiallyEscaped = false;
    for (Value input : inputs) {
      if (input.isPartiallyEscaped()) {
        isPartiallyEscaped = true;
        break;
      }
    }
    return literalValue(value, EscapeMode.ESCAPE_IS_CONSTANT, isPartiallyEscaped);
  }

  /**
   * Create a literal value using a boolean with a {@code escapeMode} of {@code
   * EscapeMode.ESCAPE_IS_CONSTANT} and {@code partiallyEscaped} based on the {@code
   * partiallyEscaped} values of the inputs.
   * 
   * @param value boolean value of the literal
   * @param inputs Values that were used to compute the boolean value.
   */
  public static Value literalConstant(boolean value, Value... inputs) {
    boolean isPartiallyEscaped = false;
    for (Value input : inputs) {
      if (input.isPartiallyEscaped()) {
        isPartiallyEscaped = true;
        break;
      }
    }
    return literalValue(value, EscapeMode.ESCAPE_IS_CONSTANT, isPartiallyEscaped);
  }

  /**
   * Create a value linked to a variable name.
   * 
   * @param name The pathname of the variable relative to the given {@link DataContext}
   * @param dataContext The DataContext defining the scope and Data objects to use when
   *        dereferencing the name.
   * @return A Value object that allows access to the variable name, the variable Data object (if it
   *         exists) and to the value of the variable.
   */
  public static Value variableValue(String name, DataContext dataContext) {
    return new VariableValue(name, dataContext);
  }

  @Override
  public boolean equals(Object other) {
    if (other == null || !(other instanceof Value)) {
      return false;
    }
    Value otherValue = (Value) other;
    // This behaves the same way as ClearSilver.
    return exists() == otherValue.exists()
        && (asString().equals(otherValue.asString()) || (isEmpty() && otherValue.isEmpty()));
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  @Override
  public String toString() {
    return asString();
  }

  public boolean isPartiallyEscaped() {
    return partiallyEscaped;
  }

  /**
   * Indicates the escaping that was applied to the expression represented by this value.
   * 
   * <p>
   * May be checked by the JSilver code before applying autoescaping. It differs from {@code
   * isEscaped}, which is true iff any part of the variable expression contains an escaping
   * function, even if the entire expression has not been escaped. Both methods are required,
   * {@code isEscaped} to determine whether &lt;?cs escape &gt; commands should be applied, and
   * {@code getEscapeMode} for autoescaping. This is done to maintain compatibility with
   * ClearSilver's behaviour.
   * 
   * @return {@code EscapeMode.ESCAPE_IS_CONSTANT} if the value represents a constant string
   *         literal. Or the appropriate {@link EscapeMode} if the value is the output of an
   *         escaping function.
   * 
   * @see EscapeMode
   */
  public EscapeMode getEscapeMode() {
    return escapeMode;
  }

}
