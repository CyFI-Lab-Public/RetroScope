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

package com.google.clearsilver.jsilver.template;

import java.io.IOException;

/**
 * HTML whitespace stripper to be used by JSilver.  It removes leading and
 * trailing whitespace, it reduces contiguous whitespace characters with just
 * the first character, and removes lines of nothing but whitespace.
 *
 * It does not strip whitespace inside the following elements:
 * <ul>
 * <li> PRE
 * <li> VERBATIM
 * <li> TEXTAREA
 * <li> SCRIPT
 * </ul>
 * It also strips out empty lines and leading whitespace inside HTML tags (i.e.
 * between '<' and '>') and inside SCRIPT elements.  It leaves trailing
 * whitespace since that is more costly to remove and tends to not be common
 * based on how templates are created (they don't have trailing whitespace).
 * <p>
 * Loadtests indicate that this class can strip whitespace almost as quickly
 * as just reading every character from a string (20% slower).
 * <p>
 * While not strictly compatible with the JNI Clearsilver whitestripping
 * function, we are not aware of any differences that yield functionally
 * different HTML output. However, we encourage users to verify for themselves
 * and report any differences.
 */
public class HtmlWhiteSpaceStripper implements Appendable {

  // Object to output stripped content to.
  private final Appendable out;
  // Level of whitespace stripping to perform. (Currently not used).
  // TODO: Determine what the exact differences are in levels in
  // JNI Clearsilver and see if it is worth porting it.
  private final int level;

  // Has any non-whitespace character been seen since the start of the line.
  private boolean nonWsSeen = false;
  // Was there previously one or more whitespace chars? If so, we should output
  // the first whitespace char in the sequence before any other non-whitespace
  // character. 0 signifies no pending whitespace.
  private char pendingWs = 0;

  // We just saw the start of an HTML tag '<'.
  private boolean startHtmlTag = false;
  // Are we currently in an opening HTML tag (not "</").
  private boolean inOpenTag = false;
  // Are we currently in a closing HTML tag.
  private boolean inCloseTag = false;
  // Are we currently in an HTML tag name.
  private boolean inTagName = false;

  // Are we between <textarea> tags
  private int textAreaScope = 0;
  // Are we between <pre> tags
  private int preScope = 0;
  // Are we between verbatim flags
  private int verbatimScope = 0;
  // Are we between <script> tags
  private int scriptScope = 0;

  // Used to hold HTML tag element name.
  private StringBuilder tagName = new StringBuilder(16);

  /**
   * Intermediate Appendable object that strips whitespace as it passes through characters to
   * another Appendable object.
   * 
   * @param out The Appendable object to dump the stripped output to.
   */
  public HtmlWhiteSpaceStripper(Appendable out) {
    this(out, 1);
  }

  /**
   * Intermediate Appendable object that strips whitespace as it passes through characters to
   * another Appendable object.
   * 
   * @param out The Appendable object to dump the stripped output to.
   * @param level Ignored for now.
   */
  public HtmlWhiteSpaceStripper(Appendable out, int level) {
    this.out = out;
    this.level = level;
  }

  @Override
  public String toString() {
    return out.toString();
  }

  @Override
  public Appendable append(CharSequence csq) throws IOException {
    return append(csq, 0, csq.length());
  }

  @Override
  public Appendable append(CharSequence csq, int start, int end) throws IOException {
    for (int i = start; i < end; i++) {
      append(csq.charAt(i));
    }
    return this;
  }

  @Override
  public Appendable append(char c) throws IOException {
    if (inOpenTag || inCloseTag) {
      // In an HTML tag.
      if (startHtmlTag) {
        // This is the first character in an HTML tag.
        if (c == '/') {
          // We are in a close tag.
          inOpenTag = false;
          inCloseTag = true;
        } else {
          // This is the first non-'/' character in an HTML tag.
          startHtmlTag = false;
          if (isTagNameStartChar(c)) {
            // we have a valid tag name first char.
            inTagName = true;
            tagName.append(c);
          }
        }
      } else if (inTagName) {
        // We were last parsing the name of an HTML attribute.
        if (isTagNameChar(c)) {
          tagName.append(c);
        } else {
          processTagName();
          inTagName = false;
        }
      }
      if (c == '>') {
        // We are at the end of the tag.
        inOpenTag = inCloseTag = false;
        nonWsSeen = true;
      }
      stripLeadingWsAndEmptyLines(c);
    } else {
      // Outside of HTML tag.
      if (c == '<') {
        // Starting a new HTML tag.
        inOpenTag = true;
        startHtmlTag = true;
      }
      if (preScope > 0 || verbatimScope > 0 || textAreaScope > 0) {
        // In an HTML element that we want to preserve whitespace in.
        out.append(c);
      } else if (scriptScope > 0) {
        // Want to remove newlines only.
        stripLeadingWsAndEmptyLines(c);
      } else {
        stripAll(c);
      }
    }

    return this;
  }

  private void stripLeadingWsAndEmptyLines(char c) throws IOException {
    // Detect and delete empty lines.
    switch (c) {
      case '\n':
        if (nonWsSeen) {
          out.append(c);
        }
        nonWsSeen = false;
        break;
      case ' ':
      case '\t':
      case '\r':
        if (nonWsSeen) {
          out.append(c);
        }
        break;
      default:
        if (!nonWsSeen) {
          nonWsSeen = true;
        }
        out.append(c);
    }
  }

  private void stripAll(char c) throws IOException {
    // All that remains is content that is safe to remove whitespace from.
    switch (c) {
      case '\n':
        if (nonWsSeen) {
          // We don't want blank lines so we don't output linefeed unless we
          // saw non-whitespace.
          out.append(c);
        }
        // We don't want trailing whitespace.
        pendingWs = 0;
        nonWsSeen = false;
        break;
      case ' ':
      case '\t':
      case '\r':
        if (nonWsSeen) {
          pendingWs = c;
        } else {
          // Omit leading whitespace
        }
        break;
      default:
        if (pendingWs != 0) {
          out.append(pendingWs);
          pendingWs = 0;
        }
        nonWsSeen = true;
        out.append(c);
    }
  }

  private int updateScope(int current, int inc) {
    current += inc;
    return current < 0 ? 0 : current;
  }

  /**
   * This code assumes well-formed HTML as input with HTML elements opening and closing properly in
   * the right order.
   */
  private void processTagName() {
    inTagName = false;
    String name = tagName.toString();
    tagName.delete(0, tagName.length());
    int inc = inOpenTag ? 1 : -1;
    if ("textarea".equalsIgnoreCase(name)) {
      textAreaScope = updateScope(textAreaScope, inc);
    } else if ("pre".equalsIgnoreCase(name)) {
      preScope = updateScope(preScope, inc);
    } else if ("verbatim".equalsIgnoreCase(name)) {
      verbatimScope = updateScope(verbatimScope, inc);
    } else if ("script".equalsIgnoreCase(name)) {
      scriptScope = updateScope(scriptScope, inc);
    }
  }

  private boolean isTagNameStartChar(char c) {
    return ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z');
  }

  // From W3C HTML spec.
  private boolean isTagNameChar(char c) {
    return ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z') || ('0' <= c && c <= '9') || (c == '_')
        || (c == '-') || (c == ':') || (c == '.');
  }

  /**
   * Note, we treat '\n' as a separate special character as it has special rules since it determines
   * what a 'line' of content is for doing leading and trailing whitespace removal and empty line
   * removal.
   */
  private boolean isWs(char c) {
    return c == ' ' || c == '\t' || c == '\r';
  }
}
