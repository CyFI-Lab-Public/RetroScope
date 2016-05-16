/**
 * Copyright (c) 2004, Google Inc.
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
package com.google.android.mail.common.html.parser;

import com.google.android.mail.common.base.X;
import com.google.android.mail.common.html.parser.HtmlDocument.EndTag;
import com.google.common.io.ByteStreams;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * HtmlTreeBuilder builds a well-formed HtmlTree.
 *
 * @see HtmlTree
 * @author jlim@google.com (Jing Yee Lim)
 */
public class HtmlTreeBuilder implements HtmlDocument.Visitor {

  private static final Logger logger = Logger.getLogger(HtmlTreeBuilder.class.getName());

  /** Stack contains HTML4.Element objects to keep track of unclosed tags */
  private final List<HTML.Element> stack = new ArrayList<HTML.Element>();
  private final TableFixer tableFixer = new TableFixer();
  private HtmlTree tree;
  private boolean built = false;

  /** Gets the built html tree */
  public HtmlTree getTree() {
    X.assertTrue(built);
    return tree;
  }

  /** Implements HtmlDocument.Visitor.start */
  public void start() {
    tree = new HtmlTree();
    tree.start();
  }

  /** Implements HtmlDocument.Visitor.finish */
  public void finish() {
    // Close all tags
    while (stack.size() > 0) {
      addMissingEndTag();
    }
    tableFixer.finish();
    tree.finish();

    built = true;
  }

  /** Implements HtmlDocument.Visitor.visitTag */
  public void visitTag(HtmlDocument.Tag t) {
    tableFixer.seeTag(t);

    HTML.Element element = t.getElement();
    if (element.isEmpty()) {
      tree.addSingularTag(t);
    } else if (t.isSelfTerminating()) {
      // Explicitly create a non-selfterminating open tag and add it to the tree
      // and also immediately add the corresponding close tag. This is done
      // so that the toHTML, toXHTML and toOriginalHTML of the tree's node list
      // will be balanced consistently.
      // Otherwise there is a possibility of "<span /></span>" for example, if
      // the created tree is converted to string through toXHTML.
      tree.addStartTag(HtmlDocument.createTag(element,
          t.getAttributes(), t.getOriginalHtmlBeforeAttributes(),
          t.getOriginalHtmlAfterAttributes()));
      EndTag end = HtmlDocument.createEndTag(element);
      tableFixer.seeEndTag(end);
      tree.addEndTag(end);
    } else {
      tree.addStartTag(t);
      push(element);                       // Track the open tags
    }
  }

  /** Implements HtmlVisitor.visit */
  public void visitEndTag(HtmlDocument.EndTag t) {

    // Here we pop back to the start tag
    HTML.Element element = t.getElement();
    int pos = findStartTag(element);
    if (pos >= 0) {

      // Add missing end-tags if any
      while (pos < stack.size() - 1) {
        addMissingEndTag();
      }

      pop();
      tableFixer.seeEndTag(t);
      tree.addEndTag(t);

    } else {
      // Not found, ignore this end tag
      logger.finest("Ignoring end tag: " + element.getName());
    }
  }

  /** Implements HtmlDocument.Visitor.visitText */
  public void visitText(HtmlDocument.Text t) {
    tableFixer.seeText(t);
    tree.addText(t);
  }

  /** Implements HtmlDocument.Visitor.visitComment */
  public void visitComment(HtmlDocument.Comment n) {
    // ignore
  }

  /** Finds the start tag from the stack, returns -1 if not found */
  private int findStartTag(HTML.Element element) {
    for (int i = stack.size() - 1; i >= 0; i--) {
      HTML.Element e = stack.get(i);
      if (e == element) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Adds a close tag corresponding to a tag on the stack, if
   * the tag needs a close tag.
   */
  private void addMissingEndTag() {
    HTML.Element element = pop();

    HtmlDocument.EndTag endTag = HtmlDocument.createEndTag(element);
    tableFixer.seeEndTag(endTag);
    tree.addEndTag(endTag);
  }

  /** Pushes a tag onto the stack */
  private void push(HTML.Element element) {
    stack.add(element);
  }

  /** Pops an elemnt from the stack */
  private HTML.Element pop() {
    return stack.remove(stack.size() - 1);
  }

  /**
   * The TableFixer makes sure that a <table> structure is more or less well
   * formed. Note that it only ensures that data within the <table> tag doesn't
   * "leak out" of the table.
   *
   * For instance, all the tags here are balanced with end tags. But the
   * 'outside' text ends up leaking out of the table.
   * <table><tr><td bgcolor=yellow>
   * <table><table>inside</table><td>outside</td></table>
   * </td></tr></table>
   *
   * The TableFixer makes sure that
   * 1) Within a table:, text and other elements are enclosed within a TD.
   *    A TD tag is inserted where necessary.
   * 2) All table structure tags are enclosed within a <table>. A TABLE tag
   *    is inserted where necessary.
   *
   * Note that the TableFixer only adds open tags, it doesn't add end tags.
   * The HtmlTreeVerifier ensures that all open tags are properly matched
   * up and closed.
   *
   * @author Jing Yee Lim (jlim@google.com)
   */
  class TableFixer {

    private int tables = 0;             // table nesting level

    // States within a <table>
    static final int NULL = 0;
    static final int IN_CELL = 1;       // in a <td> or <th> tag
    static final int IN_CAPTION = 2;    // in a <caption> tag

    private int state;

    void seeTag(HtmlDocument.Tag tag) {
      HTML.Element element = tag.getElement();
      if (element.getType() == HTML.Element.TABLE_TYPE) {

        if (HTML4.TABLE_ELEMENT.equals(element)) {
          if (tables > 0) {
            ensureCellState();
          }
          tables++;
          state = NULL;

        } else {
          // Make sure that we're in a table
          ensureTableState();

          // In cell/caption?
          if (HTML4.TD_ELEMENT.equals(element) ||
              HTML4.TH_ELEMENT.equals(element)) {
            state = IN_CELL;

          } else if (HTML4.CAPTION_ELEMENT.equals(element)) {
            state = IN_CAPTION;
          }
        }
      } else {
        if (tables > 0) {

          // Ok to have a form element outside a table cell.
          // e.g. <TR><FORM><TD>...
          if (!HTML4.FORM_ELEMENT.equals(element)) {
            ensureCellState();
          }
        }
      }
    }

    void seeEndTag(HtmlDocument.EndTag endTag) {
      HTML.Element element= endTag.getElement();

      if (tables > 0 && element.getType() == HTML.Element.TABLE_TYPE) {

        if (HTML4.TD_ELEMENT.equals(element) ||
            HTML4.TR_ELEMENT.equals(element) ||
            HTML4.TH_ELEMENT.equals(element)) {
          // End of a cell
          state = NULL;

        } else if (HTML4.CAPTION_ELEMENT.equals(element)) { // End caption
          state = NULL;

        } else if (HTML4.TABLE_ELEMENT.equals(element)) { // End table
          X.assertTrue(tables > 0);
          tables--;
          state = (tables > 0) ? IN_CELL : NULL;
        }
      }
    }

    void seeText(HtmlDocument.Text textNode) {
      // If we're in a table, but not in a cell or caption, and the
      // text is not whitespace, add a <TD>
      if (tables > 0 &&
          state == NULL &&
          !textNode.isWhitespace()) {
        ensureCellState();
      }
    }

    void finish() {
      X.assertTrue(tables == 0);
      X.assertTrue(state == NULL);
    }

    // Ensure that we're within a TABLE
    private void ensureTableState() {
      if (tables == 0) {
        push(HTML4.TABLE_ELEMENT);

        HtmlDocument.Tag tableTag =
          HtmlDocument.createTag(HTML4.TABLE_ELEMENT, null);
        tree.addStartTag(tableTag);

        tables++;
      }
    }

    // Ensure that we're within a TD or TH cell
    private void ensureCellState() {
      if (state != IN_CELL) {
        push(HTML4.TD_ELEMENT);

        HtmlDocument.Tag tdTag = HtmlDocument.createTag(HTML4.TD_ELEMENT, null);
        tree.addStartTag(tdTag);

        state = IN_CELL;
      }
    }
  }

  /** For testing */
  public static void main(String[] args) throws IOException {
    logger.setLevel(Level.FINEST);

    String html = new String(ByteStreams.toByteArray(System.in));
    HtmlParser parser = new HtmlParser();
    HtmlDocument doc = parser.parse(html);

    HtmlTreeBuilder builder = new HtmlTreeBuilder();
    doc.accept(builder);
    String outputHtml = builder.getTree().getHtml();

    System.out.println(outputHtml);
  }
}