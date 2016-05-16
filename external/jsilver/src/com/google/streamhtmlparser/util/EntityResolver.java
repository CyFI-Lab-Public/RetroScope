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
import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * <p>Decodes (unescapes) HTML entities with the complication that these
 * are received one character at a time hence must be stored temporarily.
 * Also, we may receive some "junk" characters before the actual
 * entity which we will discard.
 *
 * <p>This class is designed to be 100% compatible with the corresponding
 * logic in the C-version of the
 * {@link com.google.security.streamhtmlparser.HtmlParser}, found
 * in <code>htmlparser.c</code>. There are however a few intentional
 * differences outlines below:
 * <ul>
 *   <li>We accept lower and upper-case hex NCRs, the C-version
 *       accepts only lower-case ones.
 *   <li>The output on some invalid inputs may be different. This is
 *       currently in the process of consolidation with Filipe.
 *   <li>The API is a bit different, I find this one better suited
 *       for Java. In particular, the C method <code>processChar</code>
 *       returns the output {@code String} whereas in Java, we return
 *       a status code and then provide the {@code String} in a separate
 *       method <code>getEntity</code>. It is cleaner as it avoids the
 *       need to return empty {@code String}s during incomplete processing.
 * </ul>
 *
 * <p>Valid HTML entities have one of the following three forms:
 * <ul>
 *   <li><code>&amp;dd;</code> where dd is a number in decimal (base 10) form.
 *   <li><code>&amp;x|Xyy;</code> where yy is a hex-number (base 16).
 *   <li><code>&&lt;html-entity&gt;;</code> where
 *       <code>&lt;html-entity&gt;</code> is one of <code>lt</code>,
 *       <code>gt</code>, <code>amp</code>, <code>quot</code> or
 *       <code>apos</code>.
 * </ul>
 *
 * <p>A <code>reset</code> method is provided to facilitate object re-use.
 */
public class EntityResolver {

  /**
   * Returned in <code>processChar</code> method.
   * <p>
   * <ul>
   *   <li><code>NOT_STARTED</code> indicates we are still processing
   *       trailing characters before the start of an entity.
   *       The caller may want to save the characters it provided us.
   *   <li><code>IN_PROGRESS</code> indicates we are currently processing
   *       characters part of an entity.
   *   <li><code>COMPLETED</code> indicates we have finished processing
   *       an entity. The caller can then invoke <code>getEntity</code>
   *       then re-set the object for future re-use.
   * </ul>
   */
  public enum Status {
    NOT_STARTED("Not Started"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed");

    private final String message;

    private Status(String message) {
      this.message = message;
    }

    /**
     * Returns a brief description of the {@code Status} for
     * debugging purposes. The format of the returned {@code String}
     * is not fully specified nor guaranteed to remain the same.
     */
    @Override
    public String toString() {
      return message;
    }
  }

  /**
   * How many characters to store as we are processing an entity. Once we
   * reach that size, we know the entity is definitely invalid. The size
   * is higher than needed but keeping it as-is for compatibility with
   * the C-version.
   */
  private static final int MAX_ENTITY_SIZE = 10;

  /**
   * Map containing the recognized HTML entities and their decoded values.
   * The trailing ';' is not included in the key but it is accounted for.
   */
  private static final Map<String, String> HTML_ENTITIES_MAP =
      new ImmutableMap.Builder<String, String>()
          .put("&lt", "<")
          .put("&gt", ">")
          .put("&amp", "&")
          .put("&apos", "'")
          .build();

  /** Storage for received until characters until an HTML entity is complete. */
  private final StringBuilder sb;

  /**
   * Indicates the state we are in. see {@link EntityResolver.Status}.
   */
  private Status status;
  private String entity;

  /**
   * Constructs an entity resolver that is initially empty and
   * with status {@code NOT_STARTED}, see {@link EntityResolver.Status}.
   * 
   */
  public EntityResolver() {
    sb = new StringBuilder();
    status = Status.NOT_STARTED;
    entity = "";
  }

  /**
   * Constructs an entity resolver that is an exact copy of
   * the one provided. In particular it has the same contents
   * and status.
   *
   * @param aEntityResolver the entity resolver to copy
   */
  public EntityResolver(EntityResolver aEntityResolver) {
    sb = new StringBuilder();
    sb.replace(0, sb.length(), aEntityResolver.sb.toString());
    entity = aEntityResolver.entity;
    status = aEntityResolver.status;
  }

  /**
   * Returns the object to its original state for re-use, deleting any
   * stored characters that may be present.
   */
  public void reset() {
    status = Status.NOT_STARTED;
    sb.setLength(0);
    entity = "";
  }

  /**
   * Returns the full state of the <code>StreamEntityResolver</code>
   * in a human readable form. The format of the returned <code>String</code>
   * is not specified and is subject to change.
   *
   * @return full state of this object
   */
  @Override
  public String toString() {
    return String.format("Status: %s; Contents (%d): %s", status.toString(),
                         sb.length(), sb.toString());
  }

  /**
   * Returns the decoded HTML Entity. Should only be called
   * after {@code processChar} returned status {@code COMPLETED}.
   *
   * @return the decoded HTML Entity or an empty {@code String} if
   *         we were called with any status other than {@code COMPLETED}
   */
  public String getEntity() {
    return entity;
  }

  /**
   * Processes a character from the input stream and decodes any html entities
   * from that processed input stream.
   *
   * @param input the {@code char} to process
   * @return the processed {@code String}. Typically returns an empty
   *         {@code String} while awaiting for more characters to complete
   *         processing of the entity.
   */
  public Status processChar(char input) {
    // Developer error if the precondition fails.
    Preconditions.checkState(status != Status.NOT_STARTED || sb.length() == 0);
    if (status == Status.NOT_STARTED) {
      if (input == '&') {
        sb.append(input);
        status = Status.IN_PROGRESS;
      }
    } else if (status == Status.IN_PROGRESS) {
      if ((input == ';') || (HtmlUtils.isHtmlSpace(input))) {
        status = Status.COMPLETED;
        entity = convertEntity(input);
      } else {
        if (sb.length() < MAX_ENTITY_SIZE) {
          sb.append(input);
        } else {
          status = Status.COMPLETED;
          entity = uncovertedInput(input);
        }
      }
    } else {
      // Status.COMPLETED, ignore character, do nothing.
    }
    return status;
  }

  /**
   * Performs the decoding of a complete HTML entity and saves the
   * result back into the buffer.
   * <a href="http://www.w3.org/TR/REC-html40/charset.html#h-5.3.1">
   * Numeric Character References</a>
   *
   * @param terminator the last character read, unused on successful
   *        conversions since it is the end delimiter of the entity
   * @return The decoded entity or the original input if we could not decode it.
   */
  private String convertEntity(char terminator) {
    // Developer error if the buffer was empty or does not start with '&'.
    Preconditions.checkArgument(sb.length() > 0);
    Preconditions.checkArgument(sb.charAt(0) == '&');

    if (sb.length() > 1) {
      if (sb.charAt(1) == '#') {
        if (sb.length() <= 2) {    // Error => return content as-is.
          return uncovertedInput(terminator);
        }
        try {
          if ((sb.charAt(2) == 'x') || (sb.charAt(2) == 'X')) {    // Hex NCR
            return new String(Character.toChars(
                Integer.parseInt(sb.substring(3), 16)));
          } else {                                              // Decimal NCR
            return new String(Character.toChars(
                Integer.parseInt(sb.substring(2))));
          }
        } catch (NumberFormatException e) {
          return uncovertedInput(terminator);
        }
      }

      // See if it matches any of the few recognized entities.
      String key = sb.toString();
      if (HTML_ENTITIES_MAP.containsKey(key)) {
        return HTML_ENTITIES_MAP.get(key);
      }
    }
    // Covers the case of a lonely '&' given or valid/invalid unknown entities.
    return uncovertedInput(terminator);
  }

  private String uncovertedInput(char terminator) {
    return String.format("%s%c", sb.toString(), terminator);
  }
}
