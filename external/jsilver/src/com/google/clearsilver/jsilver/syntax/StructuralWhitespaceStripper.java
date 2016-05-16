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

package com.google.clearsilver.jsilver.syntax;

import com.google.clearsilver.jsilver.syntax.analysis.DepthFirstAdapter;
import com.google.clearsilver.jsilver.syntax.node.AAltCommand;
import com.google.clearsilver.jsilver.syntax.node.ACallCommand;
import com.google.clearsilver.jsilver.syntax.node.ADataCommand;
import com.google.clearsilver.jsilver.syntax.node.ADefCommand;
import com.google.clearsilver.jsilver.syntax.node.AEachCommand;
import com.google.clearsilver.jsilver.syntax.node.AEscapeCommand;
import com.google.clearsilver.jsilver.syntax.node.AEvarCommand;
import com.google.clearsilver.jsilver.syntax.node.AIfCommand;
import com.google.clearsilver.jsilver.syntax.node.ALoopCommand;
import com.google.clearsilver.jsilver.syntax.node.ALoopIncCommand;
import com.google.clearsilver.jsilver.syntax.node.ALoopToCommand;
import com.google.clearsilver.jsilver.syntax.node.ALvarCommand;
import com.google.clearsilver.jsilver.syntax.node.ANameCommand;
import com.google.clearsilver.jsilver.syntax.node.ANoopCommand;
import com.google.clearsilver.jsilver.syntax.node.ASetCommand;
import com.google.clearsilver.jsilver.syntax.node.AUvarCommand;
import com.google.clearsilver.jsilver.syntax.node.AVarCommand;
import com.google.clearsilver.jsilver.syntax.node.AWithCommand;
import com.google.clearsilver.jsilver.syntax.node.Start;
import com.google.clearsilver.jsilver.syntax.node.TData;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects sequences of commands corresponding to a line in the template containing only structural
 * commands, comments or whitespace and rewrites the syntax tree to effectively remove any data
 * (text) associated with that line (including the trailing whitespace).
 * <p>
 * A structural command is any command that never emits any output. These come in three types:
 * <ul>
 * <li>Commands that can contain other commands (eg, "alt", "each", "escape", "if", "loop", "with",
 * etc...).
 * <li>Commands that operate on the template itself (eg, "include", "autoescape", etc...).
 * <li>Comments.
 * </ul>
 * <p>
 * This makes it much easier to write human readable templates in cases where the output format is
 * whitespace sensitive.
 * <p>
 * Thus the input:
 * 
 * <pre>
 * {@literal
 * ----------------
 * Value is:
 * <?cs if:x>0 ?>
 *   positive
 * <?cs elif:x<0 ?>
 *   negative
 * <?cs else ?>
 *   zero
 * <?cs /if ?>.
 * ----------------
 * }
 * </pre>
 * is equivalent to:
 * 
 * <pre>
 * {@literal
 * ----------------
 * Value is:
 * <?cs if:x>0 ?>  positive
 * <?cs elif:x<0 ?>  negative
 * <?cs else ?>  zero
 * <?cs /if ?>.
 * ----------------
 * }
 * </pre>
 * but is much easier to read.
 * <p>
 * Where data commands become empty they are replaced with Noop commands, which effectively removes
 * them from the tree. These can be removed (if needed) by a later optimization step but shouldn't
 * cause any issues.
 */
public class StructuralWhitespaceStripper extends DepthFirstAdapter {
  /**
   * A regex snippet to match sequences of inline whitespace. The easiest way to define this is as
   * "not (non-space or newline)".
   */
  private static final String IWS = "[^\\S\\n]*";

  /** Pattern to match strings that consist only of inline whitespace. */
  private static final Pattern INLINE_WHITESPACE = Pattern.compile(IWS);

  /**
   * Pattern to match strings that start with arbitrary (inline) whitespace, followed by a newline.
   */
  private static final Pattern STARTS_WITH_NEWLINE = Pattern.compile("^" + IWS + "\\n");

  /**
   * Pattern to match strings that end with a newline, followed by trailing (inline) whitespace.
   */
  private static final Pattern ENDS_WITH_NEWLINE = Pattern.compile("\\n" + IWS + "$");

  /**
   * Pattern to capture the content of a string after a leading newline. Only ever used on input
   * that previously matched STARTS_WITH_NEWLINE.
   */
  private static final Pattern LEADING_WHITESPACE_AND_NEWLINE =
      Pattern.compile("^" + IWS + "\\n(.*)$", Pattern.DOTALL);

  /**
   * Pattern to capture the content of a string before a trailing newline. Note that this may have
   * to match text that has already had the final newline removed so we must greedily match the
   * whitespace rather than the content.
   */
  private static final Pattern TRAILING_WHITESPACE =
      Pattern.compile("^(.*?)" + IWS + "$", Pattern.DOTALL);

  /**
   * Flag to tell us if we are in whitespace chomping mode. By default we start in this mode because
   * the content of the first line in a template is not preceded by a newline (but should behave as
   * if it was). Once this flag has been set to false, it remains unset until a new line is
   * encountered.
   * <p>
   * Note that we only actually remove whitespace when we find the terminating condition rather than
   * when as visit the nodes (ie, this mode can be aborted and any visited whitespace will be left
   * untouched).
   */
  private boolean maybeChompWhitespace = true;

  /**
   * Flag to tell us if the line we are processing has an inline command in it.
   * <p>
   * An inline command is a complex command (eg. 'if', 'loop') where both the start and end of the
   * command exists on the same line. Non-complex commands (eg. 'var', 'name') cannot be considered
   * inline.
   * <p>
   * This flag is set when we process the start of a complex command and unset when we finish
   * processing a line. Thus if the flag is still true when we encounter the end of a complex
   * command, it tells us that (at least one) complex command was entirely contained within the
   * current line and that we should stop chomping whitespace for the current line.
   * <p>
   * This means we can detect input such as:
   * 
   * <pre>
   * {@literal <?cs if:x?>   <?cs /if?>}
   * </pre>
   * for which the trailing newline and surrounding whitespace should not be removed, as opposed to:
   * 
   * <pre>
   * {@literal <?cs if:x?>
   *    something
   *  <?cs /if?>
   * }
   * </pre>
   * where the trailing newlines for both the opening and closing of the 'if' command should be
   * removed.
   */
  private boolean currentLineContainsInlineComplexCommand = false;

  /**
   * First data command we saw when we started 'chomping' whitespace (note that this can be null if
   * we are at the beginning of a file or when we have chomped a previous data command down to
   * nothing).
   */
  private ADataCommand firstChompedData = null;

  /**
   * Intermediate whitespace-only data commands that we may need to remove.
   * <p>
   * This list is built up as we visit commands and is either processed when we need to remove
   * structural whitespace or cleared if we encounter situations that prohibit whitespace removal.
   */
  private List<ADataCommand> whitespaceData = new ArrayList<ADataCommand>();

  private static boolean isInlineWhitespace(String text) {
    return INLINE_WHITESPACE.matcher(text).matches();
  }

  private static boolean startsWithNewline(String text) {
    return STARTS_WITH_NEWLINE.matcher(text).find();
  }

  private static boolean endsWithNewline(String text) {
    return ENDS_WITH_NEWLINE.matcher(text).find();
  }

  /**
   * Removes leading whitespace (including first newline) from the given string. The text must start
   * with optional whitespace followed by a newline.
   */
  private static String stripLeadingWhitespaceAndNewline(String text) {
    Matcher matcher = LEADING_WHITESPACE_AND_NEWLINE.matcher(text);
    if (!matcher.matches()) {
      throw new IllegalStateException("Text '" + text + "' should have leading whitespace/newline.");
    }
    return matcher.group(1);
  }

  /**
   * Removes trailing whitespace (if present) from the given string.
   */
  private static String stripTrailingWhitespace(String text) {
    Matcher matcher = TRAILING_WHITESPACE.matcher(text);
    if (!matcher.matches()) {
      // The trailing whitespace regex should never fail to match a string.
      throw new AssertionError("Error in regular expression");
    }
    return matcher.group(1);
  }

  /**
   * Remove whitespace (including first newline) from the start of the given data command (replacing
   * it with a Noop command if it becomes empty). Returns a modified data command, or null if all
   * text was removed.
   * <p>
   * The given command can be null at the beginning of the file or if the original data command was
   * entirely consumed by a previous strip operation (remember that data commands can be processed
   * twice, at both the start and end of a whitespace sequence).
   */
  private static ADataCommand stripLeadingWhitespaceAndNewline(ADataCommand data) {
    if (data != null) {
      String text = stripLeadingWhitespaceAndNewline(data.getData().getText());
      if (text.isEmpty()) {
        data.replaceBy(new ANoopCommand());
        // Returning null just means we have chomped the whitespace to nothing.
        data = null;
      } else {
        data.setData(new TData(text));
      }
    }
    return data;
  }

  /**
   * Removes whitespace from the end of the given data command (replacing it with a Noop command if
   * it becomes empty).
   */
  private static void stripTrailingWhitespace(ADataCommand data) {
    if (data != null) {
      String text = stripTrailingWhitespace(data.getData().getText());
      if (text.isEmpty()) {
        data.replaceBy(new ANoopCommand());
      } else {
        data.setData(new TData(text));
      }
    }
  }

  /**
   * Removes all data commands collected while chomping the current line and clears the given list.
   */
  private static void removeWhitespace(List<ADataCommand> whitespaceData) {
    for (ADataCommand data : whitespaceData) {
      data.replaceBy(new ANoopCommand());
    }
    whitespaceData.clear();
  }

  @Override
  public void caseStart(Start node) {
    // Process the hierarchy.
    super.caseStart(node);
    // We might end after processing a non-data node, so deal with any
    // unprocessed whitespace before we exit.
    if (maybeChompWhitespace) {
      stripTrailingWhitespace(firstChompedData);
      removeWhitespace(whitespaceData);
      firstChompedData = null;
    }
    // Verify we have consumed (and cleared) any object references.
    if (firstChompedData != null) {
      throw new IllegalStateException("Unexpected first data node.");
    }
    if (!whitespaceData.isEmpty()) {
      throw new IllegalStateException("Unexpected data nodes.");
    }
  }

  @Override
  public void caseADataCommand(ADataCommand data) {
    final String originalText = data.getData().getText();
    if (maybeChompWhitespace) {
      if (isInlineWhitespace(originalText)) {
        // This data command is whitespace between two commands on the same
        // line, simply chomp it and continue ("Om-nom-nom").
        whitespaceData.add(data);
        return;
      }
      if (startsWithNewline(originalText)) {
        // This data command is at the end of a line that contains only
        // structural commands and whitespace. We remove all whitespace
        // associated with this line by:
        // * Stripping whitespace from the end of the data command at the start
        // of this line.
        // * Removing all intermediate (whitespace only) data commands.
        // * Stripping whitespace from the start of the current data command.
        stripTrailingWhitespace(firstChompedData);
        removeWhitespace(whitespaceData);
        data = stripLeadingWhitespaceAndNewline(data);
        currentLineContainsInlineComplexCommand = false;
      } else {
        // This data command contains some non-whitespace text so we must abort
        // the chomping of this line and output it normally.
        abortWhitespaceChompingForCurrentLine();
      }
    }
    // Test to see if we should start chomping on the next line.
    maybeChompWhitespace = endsWithNewline(originalText);
    // Note that data can be null here if we stripped all the whitespace from
    // it (which means that firstChompedData can be null next time around).
    firstChompedData = maybeChompWhitespace ? data : null;
  }

  /**
   * Helper method to abort whitespace processing for the current line. This method is idempotent on
   * a per line basis, and once it has been called the state is only reset at the start of the next
   * line.
   */
  private void abortWhitespaceChompingForCurrentLine() {
    maybeChompWhitespace = false;
    currentLineContainsInlineComplexCommand = false;
    whitespaceData.clear();
  }

  // ---- Inline commands that prohibit whitespace removal. ----

  @Override
  public void inAAltCommand(AAltCommand node) {
    abortWhitespaceChompingForCurrentLine();
  }

  @Override
  public void inACallCommand(ACallCommand node) {
    abortWhitespaceChompingForCurrentLine();
  }

  @Override
  public void inAEvarCommand(AEvarCommand node) {
    abortWhitespaceChompingForCurrentLine();
  }

  @Override
  public void inALvarCommand(ALvarCommand node) {
    abortWhitespaceChompingForCurrentLine();
  }

  @Override
  public void inANameCommand(ANameCommand node) {
    abortWhitespaceChompingForCurrentLine();
  }

  @Override
  public void inASetCommand(ASetCommand node) {
    abortWhitespaceChompingForCurrentLine();
  }

  @Override
  public void inAUvarCommand(AUvarCommand node) {
    abortWhitespaceChompingForCurrentLine();
  }

  @Override
  public void inAVarCommand(AVarCommand node) {
    abortWhitespaceChompingForCurrentLine();
  }

  // ---- Two part (open/close) commands that can have child commands. ----

  public void enterComplexCommand() {
    currentLineContainsInlineComplexCommand = true;
  }

  public void exitComplexCommand() {
    if (currentLineContainsInlineComplexCommand) {
      abortWhitespaceChompingForCurrentLine();
    }
  }

  @Override
  public void caseAAltCommand(AAltCommand node) {
    enterComplexCommand();
    super.caseAAltCommand(node);
    exitComplexCommand();
  }

  @Override
  public void caseADefCommand(ADefCommand node) {
    enterComplexCommand();
    super.caseADefCommand(node);
    exitComplexCommand();
  }

  @Override
  public void caseAEachCommand(AEachCommand node) {
    enterComplexCommand();
    super.caseAEachCommand(node);
    exitComplexCommand();
  }

  @Override
  public void caseAEscapeCommand(AEscapeCommand node) {
    enterComplexCommand();
    super.caseAEscapeCommand(node);
    exitComplexCommand();
  }

  @Override
  public void caseAIfCommand(AIfCommand node) {
    enterComplexCommand();
    super.caseAIfCommand(node);
    exitComplexCommand();
  }

  @Override
  public void caseALoopCommand(ALoopCommand node) {
    enterComplexCommand();
    super.caseALoopCommand(node);
    exitComplexCommand();
  }

  @Override
  public void caseALoopIncCommand(ALoopIncCommand node) {
    enterComplexCommand();
    super.caseALoopIncCommand(node);
    exitComplexCommand();
  }

  @Override
  public void caseALoopToCommand(ALoopToCommand node) {
    enterComplexCommand();
    super.caseALoopToCommand(node);
    exitComplexCommand();
  }

  @Override
  public void caseAWithCommand(AWithCommand node) {
    enterComplexCommand();
    super.caseAWithCommand(node);
    exitComplexCommand();
  }
}
