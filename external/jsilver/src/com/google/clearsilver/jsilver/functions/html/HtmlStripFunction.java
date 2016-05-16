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

package com.google.clearsilver.jsilver.functions.html;

import com.google.clearsilver.jsilver.functions.TextFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * This class implements the html_strip function. It removes html tags from text, and expands
 * numbered and named html entities to their corresponding special characters.
 */
public class HtmlStripFunction implements TextFilter {

  // The maximum length of an entity (preceded by an &)
  private static final int MAX_AMP_LENGTH = 9;

  // The state the strip function can be, normal, in an amp escaped entity or
  // inside a html tag.
  private enum State {
    DEFAULT, IN_AMP, IN_TAG
  }

  // Map of entity names to special characters.
  private static final Map<String, String> entityValues;

  // Initialize the entityName lookup map.
  static {
    Map<String, String> tempMap = new HashMap<String, String>();

    // Html specific characters.
    tempMap.put("amp", "&");
    tempMap.put("quot", "\"");
    tempMap.put("gt", ">");
    tempMap.put("lt", "<");

    tempMap.put("agrave", "\u00e0");
    tempMap.put("aacute", "\u00e1");
    tempMap.put("acirc", "\u00e2");
    tempMap.put("atilde", "\u00e3");
    tempMap.put("auml", "\u00e4");
    tempMap.put("aring", "\u00e5");
    tempMap.put("aelig", "\u00e6");
    tempMap.put("ccedil", "\u00e7");
    tempMap.put("egrave", "\u00e8");
    tempMap.put("eacute", "\u00e9");
    tempMap.put("ecirc", "\u00ea");
    tempMap.put("euml", "\u00eb");
    tempMap.put("eth", "\u00f0");
    tempMap.put("igrave", "\u00ec");
    tempMap.put("iacute", "\u00ed");
    tempMap.put("icirc", "\u00ee");
    tempMap.put("iuml", "\u00ef");
    tempMap.put("ntilde", "\u00f1");
    tempMap.put("nbsp", " ");
    tempMap.put("ograve", "\u00f2");
    tempMap.put("oacute", "\u00f3");
    tempMap.put("ocirc", "\u00f4");
    tempMap.put("otilde", "\u00f5");
    tempMap.put("ouml", "\u00f6");
    tempMap.put("oslash", "\u00f8");
    tempMap.put("szlig", "\u00df");
    tempMap.put("thorn", "\u00fe");
    tempMap.put("ugrave", "\u00f9");
    tempMap.put("uacute", "\u00fa");
    tempMap.put("ucirc", "\u00fb");
    tempMap.put("uuml", "\u00fc");
    tempMap.put("yacute", "\u00fd");

    // Clearsilver's Copyright symbol!
    tempMap.put("copy", "(C)");

    // Copy the temporary map to an unmodifiable map for the static lookup.
    entityValues = Collections.unmodifiableMap(tempMap);
  }

  @Override
  public void filter(String in, Appendable out) throws IOException {
    char[] inChars = in.toCharArray();

    // Holds the contents of an & (amp) entity before its decoded.
    StringBuilder amp = new StringBuilder();
    State state = State.DEFAULT;

    // Loop over the input string, ignoring tags, and decoding entities.
    for (int i = 0; i < inChars.length; i++) {
      char c = inChars[i];
      switch (state) {

        case DEFAULT:
          switch (c) {
            case '&':
              state = State.IN_AMP;
              break;
            case '<':
              state = State.IN_TAG;
              break;
            default:
              // If this is isn't the start of an amp of a tag, treat as plain
              // text and just output.
              out.append(c);
          }
          break;

        case IN_TAG:
          // When in a tag, all input is ignored until the end of the tag.
          if (c == '>') {
            state = State.DEFAULT;
          }
          break;

        case IN_AMP:
          // Semi-colon terminates an entity, try and decode it.
          if (c == ';') {
            state = State.DEFAULT;
            appendDecodedEntityReference(out, amp);
            amp = new StringBuilder();
          } else {
            if (amp.length() < MAX_AMP_LENGTH) {
              // If this is not the last character in the input, append to the
              // amp buffer and continue, if it is the last, dump the buffer
              // to stop the contents of it being lost.
              if (i != inChars.length - 1) {
                amp.append(c);
              } else {
                out.append('&').append(amp).append(c);
              }
            } else {
              // More than 8 chars, so not a valid entity, dump as plain text.
              out.append('&').append(amp).append(c);
              amp = new StringBuilder();
              state = State.DEFAULT;
            }
          }
          break;
      }
    }
  }

  /**
   * Attempts to decode the entity provided, if it succeeds appends it to the out string.
   * 
   * @param out the string builder to add the decoded entity to.
   * @param entityName to decode.
   */
  private void appendDecodedEntityReference(Appendable out, CharSequence entityName)
      throws IOException {

    // All the valid entities are at least two characters long.
    if (entityName.length() < 2) {
      return;
    }

    entityName = entityName.toString().toLowerCase();

    // Numbered entity.
    if (entityName.charAt(0) == '#') {
      appendNumberedEntity(out, entityName.subSequence(1, entityName.length()));
      return;
    }

    // If the entity is not a numeric value, try looking it up by name.
    String entity = entityValues.get(entityName);

    // If there is an entity by that name add it to the output.
    if (entity != null) {
      out.append(entity);
    }
  }

  /**
   * Appends an entity to a string by numeric code.
   * 
   * @param out the string to add the entity to.
   * @param entity the numeric code for the entity as a char sequence.
   */
  private void appendNumberedEntity(Appendable out, CharSequence entity) throws IOException {

    if (entity.length() != 0) {
      try {
        char c;
        // Hex numbered entities start with x.
        if (entity.charAt(0) == 'x') {
          c = (char) Integer.parseInt(entity.subSequence(1, entity.length()).toString(), 16);
        } else {
          // If its numbered, but not hex, its decimal.
          c = (char) Integer.parseInt(entity.toString(), 10);
        }

        // Don't append null characters, this is to remain Clearsilver compatible.
        if (c != '\u0000') {
          out.append(c);
        }
      } catch (NumberFormatException e) {
        // Do nothing if this is not a valid numbered entity.
      }
    }
  }
}
