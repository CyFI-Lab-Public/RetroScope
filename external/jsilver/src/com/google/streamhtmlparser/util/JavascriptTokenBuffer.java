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

package com.google.streamhtmlparser.util;

import com.google.common.base.Preconditions;

import java.util.Arrays;

/**
 * Implements a circular (ring) buffer of characters with specialized
 * application logic in order to determine the context of some
 * Javascript content that is being parsed.
 *
 * This is a specialized class - of no use to external code -
 * which aims to be 100% compatible with the corresponding logic
 * in the C-version of the HtmlParser, specifically
 * <code>jsparser.c</code>. In particular:
 * <ul>
 *   <li> The API is odd, using negative indexes to access content in
 *        the buffer. Changing the API would mean changing the test
 *        cases and have more difficulty determining whether we are
 *        remaining compatible with the C-version. It is left as an
 *        exercise for once the code is very stable and proven.
 *   <li> Repeated whitespace is folded into just one character to
 *        use the space available efficiently.
 *   <li> The buffer size is fixed. There is currently no need to
 *        make it variable so we avoid the need for constructors.
 * </ul>
 */
public class JavascriptTokenBuffer {

  /**
   * Size of the ring buffer used to lookup the last token in the javascript
   * stream. The size is somewhat arbitrary but must be larger than
   * the biggest token we want to lookup plus three: Two delimiters plus
   * an empty ring buffer slot.
   */
  private static final int BUFFER_SIZE = 18;

  /** Storage implementing the circular buffer. */
  private final char[] buffer;

  /** Index of the first item in our circular buffer. */
  private int startIndex;

  /** Index of the last item in our circular buffer. */
  private int endIndex;

  /**
   * Constructs an empty javascript token buffer. The size is fixed,
   * see {@link #BUFFER_SIZE}.
   */
  public JavascriptTokenBuffer() {
    buffer = new char[BUFFER_SIZE];
    startIndex = 0;
    endIndex = 0;
  }

  /**
   * Constructs a javascript token buffer that is identical to
   * the one given. In particular, it has the same size and contents.
   *
   * @param aJavascriptTokenBuffer the {@code JavascriptTokenBuffer} to copy
   */
  public JavascriptTokenBuffer(JavascriptTokenBuffer aJavascriptTokenBuffer) {
    buffer = Arrays.copyOf(aJavascriptTokenBuffer.buffer,
                           aJavascriptTokenBuffer.buffer.length);
    startIndex = aJavascriptTokenBuffer.startIndex;
    endIndex = aJavascriptTokenBuffer.endIndex;
  }

  /**
   * A simple wrapper over <code>appendChar</code>, it appends a string
   * to the buffer. Sequences of whitespace and newlines
   * are folded into one character to save space. Null strings are
   * not allowed.
   *
   * @param input the {@code String} to append, cannot be {@code null}
   */
  // TODO: Move to testing since not used in code.
  public void appendString(String input) {
    if (input == null) {
      throw new NullPointerException("input == null is not allowed");
    }
    for (int i = 0; i < input.length(); i++) {
      appendChar(input.charAt(i));
    }
  }

  /**
   * Appends a character to the buffer. We fold sequences of whitespace and
   * newlines into one to save space.
   *
   * @param input the {@code char} to append
   */
  public void appendChar(char input) {
    if (HtmlUtils.isJavascriptWhitespace(input) &&
        HtmlUtils.isJavascriptWhitespace(getChar(-1))) {
      return;
    }
    buffer[endIndex] = input;
    endIndex = (endIndex + 1) % buffer.length;
    if (endIndex == startIndex) {
      startIndex = (endIndex + 1) % buffer.length;
    }
  }

  /**
   * Returns the last character in the buffer and removes it from the buffer
   * or the NUL character '\0' if the buffer is empty.
   *
   * @return last character in the buffer or '\0' if the buffer is empty
   */
  public char popChar() {
    if (startIndex == endIndex) {
      return '\0';
    }
    endIndex--;
    if (endIndex < 0) {
      endIndex += buffer.length;
    }
    return buffer[endIndex];
  }

  /**
   * Returns the character at a given index in the buffer or nul ('\0')
   * if the index is outside the range of the buffer. Such could happen
   * if the buffer is not filled enough or the index is larger than the
   * size of the buffer.
   *
   * <p>Position must be negative where -1 is the index of the last
   * character in the buffer.
   *
   * @param position The index into the buffer
   *
   * @return character at the requested index
   */
  public char getChar(int position) {
    assert(position < 0);   // Developer error if it triggers.

    int absolutePosition = getAbsolutePosition(position);
    if (absolutePosition < 0) {
      return '\0';
    }

    return buffer[absolutePosition];
  }

  /**
   * Sets the given {@code input} at the given {@code position} of the buffer.
   * Returns {@code true} if we succeeded or {@code false} if we
   * failed (i.e. the write was beyond the buffer boundary).
   *
   * <p>Index positions are negative where -1 is the index of the
   * last character in the buffer.
   *
   * @param position The index at which to set the character
   * @param input The character to set in the buffer
   * @return {@code true} if we succeeded, {@code false} otherwise
   */
  public boolean setChar(int position, char input) {
    assert(position < 0);   // Developer error if it triggers.

    int absolutePosition = getAbsolutePosition(position);
    if (absolutePosition < 0) {
      return false;
    }

    buffer[absolutePosition] = input;
    return true;
  }


  /**
   * Returns the last javascript identifier/keyword in the buffer.
   *
   * @return the last identifier or {@code null} if none was found
   */
  public String getLastIdentifier() {
    int end = -1;

    if (HtmlUtils.isJavascriptWhitespace(getChar(-1))) {
      end--;
    }
    int position;
    for (position = end; HtmlUtils.isJavascriptIdentifier(getChar(position));
         position--) {
    }
    if ((position + 1) >= end) {
      return null;
    }
    return slice(position + 1, end);
  }

  /**
   * Returns a slice of the buffer delimited by the given indices.
   *
   * The start and end indexes represent the start and end of the
   * slice to copy. If the start argument extends beyond the beginning
   * of the buffer, the slice will only contain characters
   * starting from the beginning of the buffer.
   *
   * @param start The index of the first character the copy
   * @param end the index of the last character to copy
   *
   * @return {@code String} between the given indices
   */
  public String slice(int start, int end) {
    // Developer error if any of the asserts below fail.
    Preconditions.checkArgument(start <= end);
    Preconditions.checkArgument(start < 0);
    Preconditions.checkArgument(end < 0);
    
    StringBuffer output = new StringBuffer();
    for (int position = start; position <= end; position++) {
      char c = getChar(position);
      if (c != '\0') {
        output.append(c);
      }
    }
    return new String(output);
  }

  /**
   * Returns the position relative to the start of the buffer or -1
   * if the position is past the size of the buffer.
   *
   * @param position the index to be translated
   * @return the position relative to the start of the buffer
   */
  private int getAbsolutePosition(int position) {
    assert (position < 0);   // Developer error if it triggers.
    if (position <= -buffer.length) {
      return -1;
    }
    int len = endIndex - startIndex;
    if (len < 0) {
      len += buffer.length;
    }
    if (position < -len) {
      return -1;
    }
    int absolutePosition = (position + endIndex) % buffer.length;
    if (absolutePosition < 0) {
      absolutePosition += buffer.length;
    }
    return absolutePosition;
  }
}
