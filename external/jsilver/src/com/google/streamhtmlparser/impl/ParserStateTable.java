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

package com.google.streamhtmlparser.impl;

import com.google.common.base.Preconditions;

/**
 * Holds a state table which is defined as the set of all
 * recognized state transitions and the set of characters that
 * trigger them.
 *
 * <p>The logic of what character causes what state transition is derived from
 * a base definition written as a Python configuration file in the original
 * C++ parser.
 *
 * <p>This class provides methods to initially build the state table and then
 * methods at parsing time to determine the transitions to subsequent states.
 *
 * <p>Note on characters outside the extended ASCII range: Currently, all state
 * transitions in the Python configuration file trigger only on extended
 * ASCII characters, that is characters in the Unicode space of [U+0000 to
 * U+00FF]. We use that property to design a more efficient state transition
 * representation. When receiving characters outside that ASCII range, we
 * simply apply the DEFAULT transition for the given state - as we do for any
 * character that is not a hot character for that state. If no default
 * transition exists, we switch to the Internal Error state.
 *
 * <p>Technical note: In Java, a {@code char} is a code unit and in some cases
 * may not represent a complete Unicode code point. However, when that happens,
 * the code units that follow for that code point are all in the surrogate area
 * [U+D800 - U+DFFF] and hence outside of the ASCII range and will not trigger
 * any incorrect state transitions.
 *
 * <p>This class is storage-inefficient but it is static at least
 * and not generated for each Parser instance.
 */
class ParserStateTable {

  /**
   * A limit on how many different states we can have in one state table.
   * Can be increased should it no longer be sufficient.
   */
  private static final int MAX_STATES = 256;

  /**
   * We only check transitions for (extended) ASCII characters, hence
   * characters in the range 0 to MAX_CHARS -1.
   */
  private static final int MAX_CHARS = 256;

  /**
   * Records all state transitions except those identified as DEFAULT
   * transitions. It is two dimensional: Stores a target {@code InternalState}
   * given a source state (referenced by its numeric ID) and the current
   * character.
   */
  private final InternalState[][] stateTable;

  /**
   * Records all DEFAULT state transitions. These are transitions provided
   * using the {@code "[:default:]"} syntax in the Python configuration file.
   * There can be only one such transition for any given source state, hence
   * the array is one dimensional.
   */
  private final InternalState[] defaultStateTable;

  public ParserStateTable() {
    stateTable = new InternalState[MAX_STATES][MAX_CHARS];
    defaultStateTable = new InternalState[MAX_STATES];
  }

  /**
   * Returns the state to go to when receiving the current {@code char}
   * in the {@code from} state.
   * Returns {@code InternalState.INTERNAL_ERROR_STATE} if there is no
   * state transition for that character and no default state transition
   * for that state.
   *
   * <p>For ASCII characters, first look-up an explicit state transition for
   * the current character. If none is found, look-up a default transition. For
   * non-ASCII characters, look-up a default transition only.
   *
   * @param from the source state
   * @param currentChar the character received
   * @return the state to move to or {@code InternalState.INTERNAL_ERROR_STATE}
   */
  InternalState getNextState(InternalState from, int currentChar) {
    // TODO: Consider throwing run-time error here.
    if (from == null || currentChar < 0)
      return InternalState.INTERNAL_ERROR_STATE;

    int id = from.getId();
    if (id < 0 || id >= MAX_STATES) {
      return InternalState.INTERNAL_ERROR_STATE;
    }

    InternalState result = null;
    if (currentChar < MAX_CHARS) {
      result = stateTable[id][currentChar];
    }
    if (result == null) {
        result = defaultStateTable[from.getId()];
    }
    return result != null ? result : InternalState.INTERNAL_ERROR_STATE;
  }

  void setExpression(String expr, InternalState from, InternalState to) {
    if ((expr == null) || (from == null) || (to == null)) {
      return;
    }

    // This special string indicates a default state transition.
    if ("[:default:]".equals(expr)) {
      setDefaultDestination(from, to);
      return;
    }
    int i = 0;
    while (i < expr.length()) {
      // If next char is a '-' which is not the last character of the expr
      if (i < (expr.length() - 2) && expr.charAt(i + 1) == '-') {
        setRange(from, expr.charAt(i), expr.charAt(i + 2), to);
        i += 2;
      } else {
        setDestination(from, expr.charAt(i), to);
        i++;
      }
    }
  }

  private void fill(InternalState from, InternalState to) {
    char c;
    for (c = 0; c < MAX_CHARS; c++) {
      setDestination(from, c, to);
    }
  }

  private void setDefaultDestination(InternalState from, InternalState to) {
    Preconditions.checkNotNull(from);   // Developer error if it triggers
    Preconditions.checkNotNull(to);     // Developer error if it triggers
    int id = from.getId();
    if ((id < 0) || (id >= MAX_STATES)) {
      return;
    }
    // TODO: Consider asserting if there was a state transition defined.
    defaultStateTable[from.getId()] = to;
  }

  private void setDestination(InternalState from, char chr, InternalState to) {
    Preconditions.checkNotNull(from);   // Developer error if it triggers
    Preconditions.checkNotNull(to);     // Developer error if it triggers
    Preconditions.checkArgument(chr >= 0 && chr < MAX_CHARS,
                                "char must be in ASCII set: %c", chr);
    int id = from.getId();
    if ((id < 0) || (id >= MAX_STATES)) {
      return;
    }
    stateTable[from.getId()][chr] = to;
  }

  private void setRange(InternalState from, char start, char end,
                        InternalState to) {
    // Developer error if either trigger.
    Preconditions.checkArgument(start >= 0 && start < MAX_CHARS,
                                "char must be in ASCII set: %c", start);
    Preconditions.checkArgument(end >= 0 && end < MAX_CHARS,
                                "char must be in ASCII set: %c", end);
    char c;
    for (c = start; c <= end; c++) {
      setDestination(from, c, to);
    }
  }
}
