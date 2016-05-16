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
package com.android.mail.lib.html.parser;

import com.android.mail.lib.base.CharMatcher;
import com.android.mail.lib.base.Preconditions;
import com.android.mail.lib.base.X;
import com.google.common.collect.ImmutableSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Logger;

/**
 * HtmlTree represents a parsed and well-formed html text, it provides
 * methods to convert to plain text. It also provides methods to find
 * well-formed blocks of text, for quote detection.
 *
 * We don't really build a html tree data structure. Instead, for
 * efficiency, and for the ability to do a simple in-order-traversal
 * of the tree, we simply keeps a linear list of nodes (in-order).
 * The begin_ and end_ arrays keeps track of the starting end ending
 * nodes:
 *
 * For a string node, begin_[node] = end_[node] = node
 * For an open tag, begin_[node] = node, end_[node] = the matching end tag
 * For a close tag, end_[node] = the matching open tag, end_[node] = node
 *
 * @author jlim@google.com (Jing Yee Lim)
 */
public class HtmlTree {

  /**
   * An interface that allows clients to provide their own implementation
   * of a {@link PlainTextConverter}.
   */
  public static interface PlainTextConverterFactory {
    /**
     * Creates a new instance of a {@link PlainTextConverter} to convert
     * the contents of an {@link HtmlTree} to plain text.
     */
    PlainTextConverter createInstance();
  }

  /**
   * An interface for an object which converts a single HtmlTree into
   * plaintext.
   */
  public static interface PlainTextConverter {
    /**
     * Adds the given node {@code n} to plain text.
     *
     * @param n The node to convert to text.
     * @param nodeNum The number of the node among the list of all notes.
     * @param endNum The number of the ending node if this is a start node,
     *    otherwise the same as {@code nodeNum}.
     */
    void addNode(HtmlDocument.Node n, int nodeNum, int endNum);

    /**
     * Returns the current length of the plain text.
     */
    int getPlainTextLength();

    /**
     * Returns the current plain text.
     */
    String getPlainText();
  }

  /** A factory that produces converters of the default implementation. */
  private static final PlainTextConverterFactory DEFAULT_CONVERTER_FACTORY =
      new PlainTextConverterFactory() {
        public PlainTextConverter createInstance() {
          return new DefaultPlainTextConverter();
        }
      };

  /** Contains html nodes */
  private final List<HtmlDocument.Node> nodes = new ArrayList<HtmlDocument.Node>();

  /** Keeps track of beginning and end of each node */
  private final Stack<Integer> begins = new Stack<Integer>();
  private final Stack<Integer> ends = new Stack<Integer>();

  /** Plain text (lazy creation) */
  private String plainText;

  /** The html string (lazy creation) */
  private String html;

  /** textPositions[node pos] = the text position */
  private int[] textPositions;

  private PlainTextConverterFactory converterFactory = DEFAULT_CONVERTER_FACTORY;

  // For debugging only
  private static final boolean DEBUG = false;

  private static final Logger logger = Logger.getLogger(HtmlTree.class.getName());

  //------------------------------------------------------------------------

  /** HtmlTree can only be constructed from this package */
  HtmlTree() {
  }

  /**
   * Sets a new {@link PlainTextConverterFactory} to be used to convert
   * the contents of this tree to plaintext.
   */
  public void setPlainTextConverterFactory(PlainTextConverterFactory factory) {
    if (factory == null) {
      throw new NullPointerException("factory must not be null");
    }
    converterFactory = factory;
  }

  /**
   * Gets the list of node objects. A node can be either a
   * Tag, EngTag or a String object.
   * @return the nodes of the tree
   */
  public List<HtmlDocument.Node> getNodesList() {
    return Collections.unmodifiableList(nodes);
  }

  /**
   * @return number of nodes
   */
  public int getNumNodes() {
    return nodes.size();
  }

  /**
   * Gets the entire html.
   */
  public String getHtml() {
    return getHtml(-1);
  }

  /**
   * Gets the entire html, if wrapSize is > 0, it tries to do wrapping at the
   * specified size.
   */
  public String getHtml(int wrapSize) {
    if (html == null) {
      html = getHtml(0, nodes.size(), wrapSize);
    }
    return html;
  }

  /** Gets parts of the html */
  public String getHtml(int fromNode, int toNode) {
    return getHtml(fromNode, toNode, -1);
  }

  /**
   * Gets parts of the html, if wrapSize is > 0, it tries
   * to do wrapping at the specified size.
   */
  public String getHtml(int fromNode, int toNode, int wrapSize) {
    X.assertTrue(fromNode >= 0 && toNode <= nodes.size());

    int estSize = (toNode - fromNode) * 10;
    StringBuilder sb = new StringBuilder(estSize);
    int lastWrapIndex = 0;      // used for wrapping
    for (int n = fromNode; n < toNode; n++) {
      HtmlDocument.Node node = nodes.get(n);
      node.toHTML(sb);
      // TODO: maybe we can be smarter about this and not add newlines
      // within <pre> tags, unless the whole long line is encompassed
      // by the <pre> tag.
      if (wrapSize > 0) {
        // We can only wrap if the last outputted node is an element that
        // breaks the flow. Otherwise, we risk the possibility of inserting
        // spaces where they shouldn't be.
        if ((node instanceof HtmlDocument.Tag &&
              ((HtmlDocument.Tag) node).getElement().breaksFlow()) ||
            (node instanceof HtmlDocument.EndTag &&
              ((HtmlDocument.EndTag) node).getElement().breaksFlow())) {
          // Check to see if there is a newline in the most recent node's html.
          int recentNewLine = sb.substring(lastWrapIndex + 1).lastIndexOf('\n');
          if (recentNewLine != -1) {
            lastWrapIndex += recentNewLine;
          }
          // If the last index - last index of a newline is greater than
          // wrapSize, add a newline.
          if (((sb.length() - 1) - lastWrapIndex) > wrapSize) {
            sb.append('\n');
            lastWrapIndex = sb.length() - 1;
          }
        }
      }
    }

    return sb.toString();
  }

  /**
   * Convert a html region into chunks of html code, each containing
   * roughly chunkSize characters.
   */
  public ArrayList<String> getHtmlChunks(int fromNode, int toNode, int chunkSize) {
    X.assertTrue(fromNode >= 0 && toNode <= nodes.size());

    ArrayList<String> chunks = new ArrayList<String>();

    // Do a best effort attempt to not split apart certain elements (as of now,
    // just the <textarea>). We cannot guarantee that they will not be split
    // because the client may specify endpoint nodes that land in the middle
    // of an element (although this shouldn't happen if the endpoints returned
    // by createBlocks() are properly used).
    int stack = 0;
    boolean balanced = true;

    StringBuilder sb = new StringBuilder(chunkSize + 256);
    for (int n = fromNode; n < toNode; n++) {
      HtmlDocument.Node node = nodes.get(n);
      node.toHTML(sb);

      if (node instanceof HtmlDocument.Tag) {
        if (HTML4.TEXTAREA_ELEMENT.equals(
            ((HtmlDocument.Tag)node).getElement())) {
          stack++;
        }
      }
      if (node instanceof HtmlDocument.EndTag) {
        if (HTML4.TEXTAREA_ELEMENT.equals(
            ((HtmlDocument.EndTag)node).getElement())) {
          if (stack == 0) {
            balanced = false;
          } else {
            stack--;
          }
        }
      }

      if (stack == 0 && sb.length() >= chunkSize) {
        chunks.add(sb.toString());
        sb.setLength(0);
      }
    }

    // Don't forget the last chunk!
    if (sb.length() > 0) {
      chunks.add(sb.toString());
    }

    // If the tree is not balanced (cut off in the middle of a node), log
    // debug data. Clients should fix their code so that the endpoints from
    // createBlocks() are properly used.
    if (!balanced || stack != 0) {
      StringBuilder debug = new StringBuilder("Returning unbalanced HTML:\n");
      debug.append(getHtml());
      debug.append("\nfromNode: ").append(fromNode);
      debug.append("\ntoNode: ").append(toNode);
      debug.append("\nNum nodes_: ").append(getNumNodes());
      for (String chunk : chunks) {
        debug.append("\nChunk:\n").append(chunk);
      }
      logger.severe(debug.toString());
    }

    return chunks;
  }

  /**
   * Returns height (maximum length from root to a leaf) of the HTML tree.
   * @return height of the HTML tree.
   */
  public int getTreeHeight() {
    int currentHeight = 0;
    int maxHeight = 0;

    for (int i = 0; i < nodes.size(); i++) {
      HtmlDocument.Node node = nodes.get(i);
      if (node instanceof HtmlDocument.Tag) {
        currentHeight++;
        if (currentHeight > maxHeight) {
          maxHeight = currentHeight;
        }
        if (((HtmlDocument.Tag) node).getElement().isEmpty()) {
          // Empty tags have no closing pair, so decrease counter here.
          currentHeight--;
        }
      } else if (node instanceof HtmlDocument.EndTag) {
        currentHeight--;
      }
    }

    // TODO(anatol): make this value cachable?
    return maxHeight;
  }

  //------------------------------------------------------------------------
  // Creating well-formed blocks within the html tree.
  //------------------------------------------------------------------------
  /**
   * A Block represents a region of a html tree that
   * 1) is well-formed, i.e. for each node in the block, all its descendants
   * are also contained in the block. So it's safe to wrap the region
   * within a <table> or <div>, etc.
   * 2) starts at the beginning of a "line", e.g. a <div>, a <br>.
   */
  public static class Block {
    /* The starting node */
    public int start_node;

    /* The ending node (non-inclusive to the block) */
    public int end_node;
  }

  /**
   * Creates a list of Blocks, given a text-range.
   * We may create multiple blocks if one single well-formed Block cannot be
   * created.
   *
   * @param textStart beginning plain-text offset
   * @param textEnd beginning plain-text offset
   * @param minNode the smallest node number
   * @param maxNode the largest node number
   * @return a list of 0 or more Block objects, never null
   */
  public ArrayList<Block> createBlocks(int textStart, int textEnd, int minNode, int maxNode) {

    ArrayList<Block> blocks = new ArrayList<Block>();
    int startNode = Math.max(getBlockStart(textStart), minNode);
    int endNode = Math.min(getBlockEnd(textEnd), maxNode);

    if (DEBUG) {
      debug("Creating block: " +
            "text pos: " + textStart + "-" + textEnd + "\n" +
            "node pos: " + startNode + "-" + endNode + "\n" +
            plainText.substring(textStart, textEnd));
    }

    // Split up the block [start, end) into one or more blocks that
    // are well-formed, and begins at a "line" boundary.
    int blockStart = -1;
    for (int n = startNode; n < endNode;) {

      // The node n spans [nBegin, nEnd]
      int nBegin = begins.get(n);
      int nEnd = ends.get(n);

      if (blockStart == -1) {
        // Check if this is a valid start node
        if (nBegin >= n && nEnd <= endNode &&
            canBeginBlockAt(n)) {
          blockStart = n;
          n = nEnd + 1;
        } else {
          n++;
        }
        continue;
      }

      // If the node [nBegin, nEnd) lies completely within
      // the region then proceed to the (nEnd + 1).
      if (nBegin >= blockStart && nEnd < endNode) {
        n = nEnd + 1;
        continue;
      }

      // If we got here, we have to break up the region into one
      // or more blocks because the current node cannot be included
      // in the region.
      if (DEBUG) {
        debug("Forcing new block: " + n + " ("  + nBegin + " " + nEnd +
              ") exceeds (" + blockStart + " " + endNode + ")");
      }
      Block b = new Block();
      b.start_node = blockStart;
      b.end_node = n;
      blocks.add(b);

      blockStart = -1;
      n++;
    }

    // Last block
    if (blockStart != -1) {
      Block b = new Block();
      b.start_node = blockStart;
      b.end_node = endNode;
      blocks.add(b);
    }

    if (DEBUG) {
      for (int i = 0; i < blocks.size(); i++) {
        Block b = blocks.get(i);
        debug("Block " + i + "/" + blocks.size() + ": " +
              b.start_node + "-" + b.end_node + " " +
              getPlainText(b.start_node, b.end_node));
      }
    }

    return blocks;
  }

  /**
   * Checks if a block can begin starting from a node position
   */
  private boolean canBeginBlockAt(int nodePos) {
    int textPos = textPositions[nodePos];

    // Make sure that we don't exceed the text position, this happens
    // for the last tag nodes.
    if (textPos == plainText.length()) {
      textPos--;
    }

    // Scan backwards to check if a nodePos is at the beginning
    // of a line.
    for (int i = textPos; i > 0; i--) {
      char ch = plainText.charAt(i);
      if (ch == '\n') {
        return true;
      }
      if (i < textPos && !Character.isWhitespace(ch)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns the start of a block given a text-pos
   */
  private int getBlockStart(int textPos) {
    int nodenum = Arrays.binarySearch(textPositions, textPos);
    if (nodenum >= 0) {
      // Got an exact node alignment. Get the outer most pos that
      // matches the text position
      while ((nodenum - 1) >= 0 && textPositions[nodenum - 1] == textPos) {
        nodenum--;
      }
    } else {
      // textPos matches the middle of a node.
      nodenum = -nodenum - 1;
    }

    X.assertTrue(nodenum >= 0 && nodenum <= nodes.size());
    return nodenum;
  }

  /**
   * Returns the end of a block given a text-pos
   */
  private int getBlockEnd(int textPos) {
    int nodenum = Arrays.binarySearch(textPositions, textPos);
    if (nodenum >= 0) {
      // Got an exact node alignment.
      while ((nodenum + 1) < textPositions.length && textPositions[nodenum + 1] == textPos) {
        nodenum++;
      }
    } else {
      // textPos matches the middle of a node.
      nodenum = -nodenum - 2;
    }
    X.assertTrue(nodenum >= 0 && nodenum <= nodes.size());
    return nodenum;
  }

  //------------------------------------------------------------------------
  // Plain text view of the html tree
  //------------------------------------------------------------------------
  /**
   * @return the plain-text position corresponding to the node
   */
  public int getTextPosition(int node) {
    return textPositions[node];
  }

  /**
   * @return a plain-text String of the html tree
   */
  public String getPlainText() {
    if (plainText == null) {
      convertToPlainText();
    }
    return plainText;
  }

  /**
   * @return a plain-text String of a part of the html tree
   */
  public String getPlainText(int fromNode, int toNode) {
    if (plainText == null) {
      convertToPlainText();
    }
    int textstart = textPositions[fromNode];
    int textend = textPositions[toNode];
    return plainText.substring(textstart, textend);
  }

  /**
   * Converts the html tree to plain text.
   * We simply iterate through the nodes in the tree.
   * As we output the plain-text, we keep track of the text position
   * of each node.
   * For String nodes, we replace '\n' with ' ' unless we're in a
   * <pre> block.
   */
  private void convertToPlainText() {
    X.assertTrue(plainText == null && textPositions == null);

    int numNodes = nodes.size();

    // Keeps track of start text position of each node, including a last
    // entry for the size of the text.
    textPositions = new int[numNodes + 1];

    PlainTextConverter converter = converterFactory.createInstance();

    for (int i = 0; i < numNodes; i++) {
      textPositions[i] = converter.getPlainTextLength();
      converter.addNode(nodes.get(i), i, ends.get(i));
    }

    // Add a last entry, so that textPositions_[nodes_.size()] is valid.
    textPositions[numNodes] = converter.getPlainTextLength();

    plainText = converter.getPlainText();

    if (DEBUG) {
      debug("Plain text: " + plainText);

      for (int i = 0; i < nodes.size(); i++) {
        int textPos = textPositions[i];
        String text = plainText.substring(textPos, textPositions[i + 1]);
        debug("At " + i + ": pos=" + textPos + " " +  text);
      }
    }
  }

  /**
   * Encapsulates the logic for outputting plain text with respect to text
   * segments, white space separators, line breaks, and quote marks.
   */
  static final class PlainTextPrinter {
    /**
     * Separators are whitespace inserted between segments of text. The
     * semantics are such that between any two segments of text, there is
     * at most one separator. As such, separators are ordered in increasing
     * priority, and setting a separator multiple times between text will
     * result in the single separator with the highest priority being used.
     * For example, a LineBreak (one newline) will override a Space, but will
     * be overriden by a BlankLine (two newlines).
     */
    static enum Separator {
      // The values here must be ordered by increasing priority, as the
      // enum's ordinal() method is used when determining if a new separator
      // should override an existing one.
      None,
      Space,      // single space
      LineBreak,  // single new line
      BlankLine   // two new lines
    }

    // White space characters that are collapsed as a single space.
    // Note that characters such as the non-breaking whitespace
    // and full-width spaces are not equivalent to the normal spaces.
    private static final String HTML_SPACE_EQUIVALENTS = " \n\r\t\f";

    /**
     * Determines if the given character is considered an HTML space character.
     * Consecutive HTML space characters are collapsed into a single space when
     * not within a PRE element.
     */
    private static boolean isHtmlWhiteSpace(char ch) {
      return HTML_SPACE_EQUIVALENTS.indexOf(ch) >= 0;
    }

    // The buffer in which we accumulate the converted plain text
    private final StringBuilder sb = new StringBuilder();

    // How many <blockquote> blocks we are in.
    private int quoteDepth = 0;

    // How many logical newlines are at the end of the buffer we've outputted.
    // Note that we can't simply count the newlines at the end of the output
    // buffer because a logical new line may be followed by quote marks.
    //
    // We initialize the value to 2 so that we consume any initial separators,
    // since we don't need separators at the beginning of the output. This also
    // results in correctly outputting any quote marks at the beginning of the
    // output if the first piece of text is within a BLOCKQUOTE element.
    private int endingNewLines = 2;

    // The next separator to be inserted between two text nodes.
    private Separator separator = Separator.None;

    /** Returns the current length of the text. */
    final int getTextLength() {
      return sb.length();
    }

    /** Returns the current text. */
    final String getText() {
      return sb.toString();
    }

    /**
     * Sets the next separator between two text nodes. A Space separator is
     * used if there is any whitespace between the two text nodes when there is
     * no intervening element that breaks flow. This is automatically handled
     * by the {@link #appendNormalText} function so the client never needs to
     * specify this separator.
     * <p>
     * A LineBreak separator (single new line) is used if text segments are
     * separated or enclosed by elements that break flow (e.g. DIV, TABLE, HR,
     * etc.). The client should set this separator for opening and closing tags
     * of any element that breaks flow.
     * <p>
     * A BlankLine separator (two new lines) should be set for opening and
     * closing P tags.
     * <p>
     * If this method is called multiple times between text nodes, a
     * separator with a higher priority will override that of a lower priority.
     */
    final void setSeparator(Separator newSeparator) {
      if (newSeparator.ordinal() > separator.ordinal()) {
        separator = newSeparator;
      }
    }

    /** Increments the current quote depth of the text. */
    final void incQuoteDepth() {
      quoteDepth++;
    }

    /** Decrements the current quote depth of the text. */
    final void decQuoteDepth() {
      quoteDepth = Math.max(0, quoteDepth - 1);
    }

    /**
     * Normalizes the HTML whitespace in the given {@code text} and appends it
     * as the next segment of text. This will flush any separator that should
     * be appended before the text, as well as any quote marks that should
     * follow the last newline if the quote depth is non-zero.
     */
    final void appendNormalText(String text) {
      if (text.length() == 0) {
        return;
      }
      boolean startsWithSpace = isHtmlWhiteSpace(text.charAt(0));
      boolean endsWithSpace = isHtmlWhiteSpace(text.charAt(text.length() - 1));

      // Strip beginning and ending whitespace.
      text = CharMatcher.anyOf(HTML_SPACE_EQUIVALENTS).trimFrom(text);

      // Collapse whitespace within the text.
      text = CharMatcher.anyOf(HTML_SPACE_EQUIVALENTS).collapseFrom(text, ' ');

      if (startsWithSpace) {
        setSeparator(Separator.Space);
      }

      appendTextDirect(text);

      if (endsWithSpace) {
        setSeparator(Separator.Space);
      }
    }

    /**
     * Appends the given text, preserving all whitespace. This is used for
     * appending text in a PRE element.
     */
    final void appendPreText(String text) {
      // We're in a <pre> block. Split the text into lines, and append
      // each line with appendTextDirect() to preserve white space.
      String[] lines = text.split("[\\r\\n]", -1);

      // split() will always return an array with at least one element.
      appendTextDirect(lines[0]);

      // For all of the remaining lines, we append a newline first, which
      // takes care of any quote marks that we need to output if the quote
      // depth is non-zero.
      for (int i = 1; i < lines.length; i++) {
        appendNewLine();
        appendTextDirect(lines[i]);
      }
    }

    /**
     * Appends the {@code text} directly to the output, taking into account
     * any separator that should be appended before it, and any quote marks
     * that should follow the last newline if the quote depth is non-zero.
     * <p>
     * {@code text} must not contain any new lines--in order to handle
     * quoting correctly, it is up to the caller to either normalize away the
     * newlines, or split the text up into separate lines and handle new lines
     * with the {@link #appendNewLine} method.
     * <p>
     * The original {@code text} is not modified in any way. Use this method
     * when you need to preserve the original white space.
     * <p>
     * If the given {@code text} is non empty, this method will result in
     * {@code endingNewLines} being reset to 0.
     */
    private void appendTextDirect(String text) {
      if (text.length() == 0) {
        return;
      }
      Preconditions.checkArgument(text.indexOf('\n') < 0,
                                  "text must not contain newlines.");
      flushSeparator();
      maybeAddQuoteMarks(true);
      sb.append(text);
      endingNewLines = 0;
    }

    /**
     * Appends a forced line break, which is the equivalent of a BR element.
     */
    final void appendForcedLineBreak() {
      flushSeparator();
      appendNewLine();
    }

    /**
     * Appends any pending separator to the output buffer. This should be
     * called before appending text to the buffer.
     */
    private void flushSeparator() {
      switch (separator) {
        case Space:
          if (endingNewLines == 0) {
            // Only append a space separator if we are not following a new
            // line character. For example, we don't append a separator
            // space after a <br> tag, since the <br>'s newline fulfills the
            // space separation requirement.
            sb.append(" ");
          }
          break;
        case LineBreak:
          while (endingNewLines < 1) {
            appendNewLine();
          }
          break;
        case BlankLine:
          while (endingNewLines < 2) {
            appendNewLine();
          }
          break;
      }
      separator = Separator.None;
    }

    /**
     * Adds a newline to the output. This handles any quote marks that should
     * follow any previous new lines, and increments {@code endingNewLines}.
     */
    private void appendNewLine() {
      maybeAddQuoteMarks(false);
      sb.append('\n');
      endingNewLines++;
    }

    /**
     * Adds quote marks to the output if we are at the beginning of a line.
     * One '>' character is used for every level of quoting we are in.
     *
     * @param includeEndingSpace Includes a single space after the quote marks.
     */
    private void maybeAddQuoteMarks(boolean includeEndingSpace) {
      // We only need to add quote marks if we are at the beginning of line.
      if (endingNewLines > 0 && quoteDepth > 0) {
        for (int i = 0; i < quoteDepth; i++) {
          sb.append('>');
        }
        if (includeEndingSpace) {
          sb.append(' ');
        }
      }
    }
  }

  /**
   * Contains the logic for converting the contents of one HtmlTree into
   * plaintext.
   */
  public static class DefaultPlainTextConverter implements PlainTextConverter {

    private static final Set<HTML.Element> BLANK_LINE_ELEMENTS =
        ImmutableSet.of(
            HTML4.P_ELEMENT,
            HTML4.BLOCKQUOTE_ELEMENT,
            HTML4.PRE_ELEMENT);

    private final PlainTextPrinter printer = new PlainTextPrinter();

    private int preDepth = 0;

    public void addNode(HtmlDocument.Node n, int nodeNum, int endNum) {
      if (n instanceof HtmlDocument.Text) {        // A string node

        HtmlDocument.Text textNode = (HtmlDocument.Text) n;
        String str = textNode.getText();

        if (preDepth > 0) {
          printer.appendPreText(str);

        } else {
          printer.appendNormalText(str);
        }

      } else if (n instanceof HtmlDocument.Tag) {

        // Check for linebreaking tags.
        HtmlDocument.Tag tag = (HtmlDocument.Tag) n;
        HTML.Element element = tag.getElement();

        if (BLANK_LINE_ELEMENTS.contains(element)) {
          printer.setSeparator(PlainTextPrinter.Separator.BlankLine);

        } else if (HTML4.BR_ELEMENT.equals(element)) {
          // The <BR> element is special in that it always adds a newline.
          printer.appendForcedLineBreak();

        } else if (element.breaksFlow()) {
          // All other elements that break the flow add a LineBreak separator.
          printer.setSeparator(PlainTextPrinter.Separator.LineBreak);

          if (HTML4.HR_ELEMENT.equals(element)) {
            printer.appendNormalText("________________________________");
            printer.setSeparator(PlainTextPrinter.Separator.LineBreak);
          }
        }

        if (HTML4.BLOCKQUOTE_ELEMENT.equals(element)) {
          printer.incQuoteDepth();

        } else if (HTML4.PRE_ELEMENT.equals(element)) {
          preDepth++;
        }

      } else if (n instanceof HtmlDocument.EndTag) {

        // Check for linebreaking tags.
        HtmlDocument.EndTag endTag = (HtmlDocument.EndTag) n;
        HTML.Element element = endTag.getElement();

        if (BLANK_LINE_ELEMENTS.contains(element)) {
          printer.setSeparator(PlainTextPrinter.Separator.BlankLine);

        } else if (element.breaksFlow()) {
          // All other elements that break the flow add a LineBreak separator.
          printer.setSeparator(PlainTextPrinter.Separator.LineBreak);
        }

        if (HTML4.BLOCKQUOTE_ELEMENT.equals(element)) {
          printer.decQuoteDepth();

        } else if (HTML4.PRE_ELEMENT.equals(element)) {
          preDepth--;
        }
      }
    }

    public final int getPlainTextLength() {
      return printer.getTextLength();
    }

    public final String getPlainText() {
      return printer.getText();
    }
  }

  //------------------------------------------------------------------------
  // The following methods are used to build the html tree.
  //------------------------------------------------------------------------
  /** For building the html tree */
  private Stack<Integer> stack;
  private int parent;

  /** Starts the build process */
  void start() {
    stack = new Stack<Integer>();
    parent = -1;
  }

  /** Finishes the build process */
  void finish() {
    X.assertTrue(stack.size() == 0);
    X.assertTrue(parent == -1);
  }

  /**
   * to add the matching end tag
   */
  void addStartTag(HtmlDocument.Tag t) {
    int nodenum = nodes.size();
    addNode(t, nodenum, -1);

    stack.add(parent);
    parent = nodenum;
  }

  /**
   * Adds a html end tag, this must be preceded by a previous matching open tag
   */
  void addEndTag(HtmlDocument.EndTag t) {
    int nodenum = nodes.size();
    addNode(t, parent, nodenum);

    if (parent != -1) {
      ends.set(parent, nodenum);
    }

    //is this the right pop?
    parent = stack.pop();
  }

  /** Adds a singular tag that does not have a corresponding end tag */
  void addSingularTag(HtmlDocument.Tag t) {
    int nodenum = nodes.size();
    addNode(t, nodenum, nodenum);
  }

  /**
   * Adds a text
   * @param t a plain-text string
   */
  void addText(HtmlDocument.Text t) {
    int nodenum = nodes.size();
    addNode(t, nodenum, nodenum);
  }

  /** Adds a node */
  private void addNode(HtmlDocument.Node n, int begin, int end) {

    nodes.add(n);
    begins.add(begin);
    ends.add(end);
  }

  /** For debugging */
  private static final void debug(String str) {
    logger.finest(str);
  }

}