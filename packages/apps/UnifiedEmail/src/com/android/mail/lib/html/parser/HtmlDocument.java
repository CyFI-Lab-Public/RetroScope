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

import com.android.mail.lib.base.CharEscapers;
import com.android.mail.lib.base.CharMatcher;
import com.android.mail.lib.base.StringUtil;
import com.android.mail.lib.base.X;
import com.google.common.collect.Lists;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * HtmlDocument is a container for a list of html nodes, and represents the
 * entire html document. It contains toHTML() method which prints out the html
 * text, toXHTML for printing out XHTML text and toString() which prints out in
 * debug format.
 *
 * @author jlim@google.com (Jing Yee Lim)
 */
public class HtmlDocument {
  /** List of Node objects */
  private final List<Node> nodes;

  /**
   * Creates a Html document.
   * @param nodes list of html nodes
   */
  public HtmlDocument(List<Node> nodes) {
    this.nodes = nodes;
  }

  /** Gets the list of nodes */
  public List<Node> getNodes() {
    return nodes;
  }

  /** Returns a HTML string for the current document */
  public String toHTML() {
    StringBuilder sb = new StringBuilder(nodes.size() * 10);
    for (Node n : nodes) {
      n.toHTML(sb);
    }
    return sb.toString();
  }

  /** Returns a XHTML string for the current document */
  public String toXHTML() {
    StringBuilder sb = new StringBuilder(nodes.size() * 10);
    for (Node n : nodes) {
      n.toXHTML(sb);
    }
    return sb.toString();
  }

  /**
   * Returns, as much as possible, original content of preparsed nodes.  This
   * is only different from toHTML() if the nodes were created with original
   * content, e.g., by HtmlParser in preserve mode.
   */
  public String toOriginalHTML() {
    StringBuilder sb = new StringBuilder(nodes.size() * 10);
    for (Node n : nodes) {
      n.toOriginalHTML(sb);
    }
    return sb.toString();
  }

  /** Returns the HTML document in debug format */
  @Override
  public String toString() {
    StringWriter strWriter = new StringWriter();
    accept(new DebugPrinter(new PrintWriter(strWriter)));
    return strWriter.toString();
  }

  /**
   * Creates start Tag Node.
   * @see HtmlDocument#createTag(HTML.Element, List, String, String)
   */
  public static Tag createTag(HTML.Element element, List<TagAttribute> attributes) {
    return createTag(element, attributes, null, null);
  }

  /**
   * Creates start Tag Node.
   * @see HtmlDocument.Tag#Tag(HTML.Element, List, boolean, String, String)
   */
  public static Tag createTag(HTML.Element element,
      List<TagAttribute> attributes, String originalHtmlBeforeAttributes,
      String originalHtmlAfterAttributes) {
    return new Tag(element, attributes, false, originalHtmlBeforeAttributes,
        originalHtmlAfterAttributes);
  }

  /**
   * Creates self-terminating Tag Node.
   * @see HtmlDocument#createSelfTerminatingTag(HTML.Element, List, String, String)
   */
  public static Tag createSelfTerminatingTag(HTML.Element element,
      List<TagAttribute> attributes) {
    return createSelfTerminatingTag(element, attributes, null, null);
  }

  /**
   * Creates self-terminating Tag Node.
   * @see HtmlDocument#createTag(HTML.Element, List, String, String)
   */
  public static Tag createSelfTerminatingTag(HTML.Element element,
      List<TagAttribute> attributes, String originalHtmlBeforeAttributes,
      String originalHtmlAfterAttributes) {
    return new Tag(element, attributes, true, originalHtmlBeforeAttributes,
        originalHtmlAfterAttributes);
  }

  /**
   * @see HtmlDocument#createEndTag(HTML.Element, String)
   */
  public static EndTag createEndTag(HTML.Element element) {
    return createEndTag(element, null);
  }

  /**
   * @see HtmlDocument.EndTag#EndTag(HTML.Element, String)
   */
  public static EndTag createEndTag(HTML.Element element, String originalHtml) {
    return new EndTag(element, originalHtml);
  }

  /**
   * @see HtmlDocument#createTagAttribute(HTML.Attribute, String, String)
   */
  public static TagAttribute createTagAttribute(HTML.Attribute attr, String value) {
    return createTagAttribute(attr, value, null);
  }

  /**
   * @see HtmlDocument.TagAttribute#TagAttribute(HTML.Attribute, String, String)
   */
  public static TagAttribute createTagAttribute(HTML.Attribute attr,
      String value, String originalHtml) {
    X.assertTrue(attr != null);
    return new TagAttribute(attr, value, originalHtml);
  }

  /**
   * @see HtmlDocument#createText(String, String)
   */
  public static Text createText(String text) {
    return createText(text, null);
  }

  /**
   * Creates a Text node.
   * @see UnescapedText#UnescapedText(String, String)
   */
  public static Text createText(String text, String original) {
    return new UnescapedText(text, original);
  }

  /**
   * Creates a Text node where the content hasn't been unescaped yet (this will
   * be done lazily).
   */
  public static Text createEscapedText(String htmlText, String original) {
    return new EscapedText(htmlText, original);
  }

  /**
   * Creates an Comment node.
   * @see Comment#Comment(String)
   */
  public static Comment createHtmlComment(String content) {
    return new Comment(content);
  }

  /**
   * Creates a CDATA node.
   * @see CDATA#CDATA(String)
   */
  public static CDATA createCDATA(String text) {
    return new CDATA(text);
  }

  /** Accepts a Visitor */
  public void accept(Visitor v) {
    v.start();
    for (Node node : nodes) {
      node.accept(v);
    }
    v.finish();
  }

  /**
   * @param filter results of this filter replace the existing nodes
   * @return new document with filtered nodes
   */
  public HtmlDocument filter(MultiplexFilter filter) {
    filter.start();
    List<Node> newNodes = new ArrayList<Node>();
    for (Node node : nodes) {
      filter.filter(node, newNodes);
    }
    filter.finish(newNodes);
    return new HtmlDocument(newNodes);
  }

  /**
   * Html node
   */
  public static abstract class Node {

    /** Accepts a visitor */
    public abstract void accept(Visitor visitor);

    /** Converts to HTML */
    public String toHTML() {
      StringBuilder sb = new StringBuilder();
      toHTML(sb);
      return sb.toString();
    }

    /** Converts to HTML */
    public abstract void toHTML(StringBuilder sb);

    /** Converts to XHTML */
    public String toXHTML() {
      StringBuilder sb = new StringBuilder();
      toXHTML(sb);
      return sb.toString();
    }

    /** Converts to XHTML */
    public abstract void toXHTML(StringBuilder sb);

    /**
     * @return Original if it's available; otherwise, returns
     * <code>toHTML()</code>
     */
    public String toOriginalHTML() {
      StringBuilder sb = new StringBuilder();
      toOriginalHTML(sb);
      return sb.toString();
    }

    /**
     * @param sb Destination of HTML to be appended.  Appends original if it's
     * available; otherwise, appends <code>toHTML()</code>
     */
    public abstract void toOriginalHTML(StringBuilder sb);
  }

  /**
   * HTML comment node.
   */
  public static class Comment extends Node {

    private final String content;

    /**
     * @param content Raw comment, including "&lt;!--" and "--&gt;".
     */
    public Comment(String content) {
      this.content = content;
    }

    @Override
    public void accept(Visitor visitor) {
      visitor.visitComment(this);
    }

    /**
     * Emit original unchanged.
     * @param sb Destination of result.
     */
    @Override
    public void toHTML(StringBuilder sb) {
      sb.append(content);
    }

    /**
     * Emit original unchanged.
     * @param sb Destination of result.
     */
    @Override
    public void toXHTML(StringBuilder sb) {
      sb.append(content);
    }

    /**
     * Emit original unchanged.
     * @param sb Destination of result.
     */
    @Override
    public void toOriginalHTML(StringBuilder sb) {
      sb.append(content);
    }

    /**
     * @return Original unchanged.
     */
    public String getContent() {
      return content;
    }
  }

  /**
   * Text node
   */
  public static abstract class Text extends Node {

    /**
     * unaltered original content of this node
     */
    private final String originalHtml;

    /**
     * content of this node in HTML format
     */
    private String html;

    /**
     * @param originalHtml Unaltered original HTML. If not null,
     *        toOriginalHTML() will return this.
     */
    protected Text(String originalHtml) {
      this.originalHtml = originalHtml;
    }

    /**
     * Gets the plain, unescaped text.
     */
    abstract public String getText();

    // Returns true if it contains only white space
    public boolean isWhitespace() {
      String text = getText();
      int len = text.length();
      for (int i = 0; i < len; i++) {
        if (!Character.isWhitespace(text.charAt(i))) {
          return false;
        }
      }
      return true;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof Text) {
        Text that = (Text) o;

        return this.originalHtml == null ? that.originalHtml == null
            : this.originalHtml.equals(that.originalHtml);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return originalHtml == null ? 0 : originalHtml.hashCode();
    }

    @Override
    public String toString() {
      return getText();
    }

    /** Extends Node.accept */
    @Override
    public void accept(Visitor visitor) {
      visitor.visitText(this);
    }

    /**
     * Gets the HTML, with HTML entities escaped.
     */
    @Override
    public void toHTML(StringBuilder sb) {
      if (html == null) {
        html = CharEscapers.asciiHtmlEscaper().escape(getText());
      }
      sb.append(html);
    }

    /**
     * @see HtmlDocument.Text#toHTML(StringBuilder)
     */
    @Override
    public void toXHTML(StringBuilder sb) {
      toHTML(sb);
    }

    /**
     * @param sb Appends original HTML to this if available.  Otherwise,
     * same as toHTML().
     */
    @Override
    public void toOriginalHTML(StringBuilder sb) {
      if (originalHtml != null) {
        sb.append(originalHtml);
      } else {
        toHTML(sb);
      }
    }

    /**
     * @return the original HTML (possibly with entities unescaped if the
     * document was malformed). May be null if original HTML was not preserved
     * (see constructor argument of {@link HtmlParser})
     */
    public String getOriginalHTML() {
      return originalHtml;
    }
  }

  /**
   * {@link Text} implementation where the given text is assumed to have been
   * already HTML unescaped.
   */
  private static class UnescapedText extends Text {
    /**
     * content of this node as plain, unescaped text
     */
    protected final String text;

    private UnescapedText(String plainText, String originalHtml) {
      super(originalHtml);
      X.assertTrue(plainText != null);
      this.text = plainText;
    }

    @Override public String getText() {
      return text;
    }
  }

  /**
   * {@link Text} implementation where the given text is not unescaped yet, and
   * unescaping will only be done lazily.
   */
  private static class EscapedText extends Text {
    private final String htmlText;
    private String text;

    private EscapedText(String htmlText, String originalHtml) {
      super(originalHtml);
      this.htmlText = htmlText;
    }

    @Override public String getText() {
      if (text == null) {
        text = StringUtil.unescapeHTML(htmlText);
      }
      return text;
    }
  }

  /**
   * CDATA node is a subclass of Text node.
   */
  public static class CDATA extends UnescapedText {
    private CDATA(String text) {
      super(text, text);
    }

    @Override public void toHTML(StringBuilder sb) {
      // Do not htmlescape CDATA text
      sb.append(text);
    }

    @Override public void toXHTML(StringBuilder sb) {
      sb.append("<![CDATA[")
        .append(text)
        .append("]]>");
    }
  }

  /**
   * Tag is a HTML open tag.
   */
  public static class Tag extends Node {
    // The element
    private final HTML.Element element;

    // List of TagAttribute objects. This may be null.
    private List<TagAttribute> attributes;

    private final boolean isSelfTerminating;

    private final String originalHtmlBeforeAttributes;

    private final String originalHtmlAfterAttributes;

    /**
     * @param element the HTML4 element
     * @param attributes list of TagAttribute objects, may be null
     * @param isSelfTerminating
     * @param originalHtmlBeforeAttributes Original tag's full content before
     *        first attribute, including beginning '&lt;'. This should not
     *        include preceeding whitespace for the first attribute, as that
     *        should be included in the attribute node. If not null, tag will
     *        preserve this original content. e.g., if original tag were
     *        "&lt;foO bar='zbc'&gt;", case of foO would be preserved. This
     *        method does not validate that
     *        <code>originalHtmlBeforeAttributes</code> is a valid tag String.
     * @param originalHtmlAfterAttributes Full content of original tag after
     *        last attribute, including ending '>'. If not null, tag will
     *        preserve this original content. e.g., if original tag were
     *        "&lt;foo bar='zbc'  &gt;", the spaces before '&gt;' be preserved.
     *        This method does not validate that
     *        <code>originalHtmlAfterAttributes</code> is a valid tag String.
     */
    private Tag(HTML.Element element, List<TagAttribute> attributes,
        boolean isSelfTerminating, String originalHtmlBeforeAttributes,
        String originalHtmlAfterAttributes) {
      X.assertTrue(element != null);
      this.element = element;
      this.attributes = attributes;
      this.isSelfTerminating = isSelfTerminating;
      this.originalHtmlBeforeAttributes = originalHtmlBeforeAttributes;
      this.originalHtmlAfterAttributes = originalHtmlAfterAttributes;
    }

    /** Gets the name */
    public String getName() {
      return element.getName();
    }

    /** Gets the element */
    public HTML.Element getElement() {
      return element;
    }

    /** Adds an attribute */
    public void addAttribute(HTML.Attribute attr, String value) {
      X.assertTrue(attr != null);
      addAttribute(new TagAttribute(attr, value, null));
    }

    /** Adds an attribute */
    public void addAttribute(TagAttribute attr) {
      X.assertTrue(attr != null);
      if (attributes == null) {
        attributes = new ArrayList<TagAttribute>();
      }
      attributes.add(attr);
    }

    /** Gets the list of attributes, note that this maybe null. */
    public List<TagAttribute> getAttributes() {
      return attributes;
    }

    /** Finds and returns a TagAttribute, or null if not found */
    public TagAttribute getAttribute(HTML.Attribute attr) {
      if (attributes != null) {
        for (TagAttribute attribute : attributes) {
          if (attribute.getAttribute().equals(attr)) {
            return attribute;
          }
        }
      }
      return null;
    }

    /**
     * Finds and returns list of TagAttribute of given attribute
     * type, or empty list if not found,
     */
    public List<TagAttribute> getAttributes(HTML.Attribute attr) {
      List<TagAttribute> result = Lists.newArrayList();
      if (attributes != null) {
        for (TagAttribute attribute : attributes) {
          if (attribute.getAttribute().equals(attr)) {
            result.add(attribute);
          }
        }
      }
      return result;
    }

    /** Returns debug string */
    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("Start Tag: ");
      sb.append(element.getName());
      if (attributes != null) {
        for (TagAttribute attr : attributes) {
          sb.append(' ');
          sb.append(attr.toString());
        }
      }
      return sb.toString();
    }

    /** Implements Node.accept */
    @Override
    public void accept(Visitor visitor) {
      visitor.visitTag(this);
    }

    /** Implements Node.toHTML */
    @Override
    public void toHTML(StringBuilder sb) {
      serialize(sb, SerializeType.HTML);
    }

    @Override
    public void toXHTML(StringBuilder sb) {
      serialize(sb, SerializeType.XHTML);
    }

    @Override
    public void toOriginalHTML(StringBuilder sb) {
      serialize(sb, SerializeType.ORIGINAL_HTML);
    }

    /**
     * Specifies format of serialized output.
     */
    private enum SerializeType {
      ORIGINAL_HTML, HTML, XHTML
    }

    private void serialize(StringBuilder sb, SerializeType type) {
      // before attributes
      if (type == SerializeType.ORIGINAL_HTML && originalHtmlBeforeAttributes != null) {
        sb.append(originalHtmlBeforeAttributes);
      } else {
        sb.append('<');
        sb.append(element.getName());
      }

      // attributes
      if (attributes != null) {
        for (TagAttribute attr : attributes) {
          // attribute includes leading whitespace, so we needn't add it here
          if (type == SerializeType.ORIGINAL_HTML) {
            attr.toOriginalHTML(sb);
          } else if (type == SerializeType.HTML) {
            attr.toHTML(sb);
          } else {
            attr.toXHTML(sb);
          }
        }
      }

      // after attributes
      if (type == SerializeType.ORIGINAL_HTML && originalHtmlAfterAttributes != null) {
        sb.append(originalHtmlAfterAttributes);
      } else if (type == SerializeType.XHTML && (isSelfTerminating || getElement().isEmpty())) {
        sb.append(" />");
      } else {
        sb.append('>');
      }
    }

    public boolean isSelfTerminating() {
      return isSelfTerminating;
    }

    public String getOriginalHtmlBeforeAttributes() {
      return originalHtmlBeforeAttributes;
    }

    public String getOriginalHtmlAfterAttributes() {
      return originalHtmlAfterAttributes;
    }
  }

  /**
   * EndTag is a closing HTML tag.
   */
  public static class EndTag extends Node {
    // The element
    private final HTML.Element element;

    private final String originalHtml;

    /**
     * @param element The HTML.Element element.  Can not be null.
     * @param originalHtml Full content of original tag, including beginning
     * and ending '<' and '>'.  If not null, tag will preserve this original
     * content. e.g., if original tag were "&lt;/foo &gt;", the space after foo
     * would be preserved.  This method does not validate that originalHtml is a
     * valid tag String.
     */
    private EndTag(HTML.Element element, String originalHtml) {
      X.assertTrue(element != null);
      this.element = element;
      this.originalHtml = originalHtml;
    }

    /** Gets the name */
    public String getName() {
      return element.getName();
    }

    /** Gets the element */
    public HTML.Element getElement() {
      return element;
    }

    /** Returns debug string */
    @Override
    public String toString() {
      return "End Tag: " + element.getName();
    }

    /** Implements Node.accept */
    @Override
    public void accept(Visitor visitor) {
      visitor.visitEndTag(this);
    }

    /** Implements Node.toHTML */
    @Override
    public void toHTML(StringBuilder sb) {
      sb.append("</");
      sb.append(element.getName());
      sb.append('>');
    }

    @Override
    public void toXHTML(StringBuilder sb) {
      toHTML(sb);
    }

    @Override
    public void toOriginalHTML(StringBuilder sb) {
      if (originalHtml != null) {
        sb.append(originalHtml);
      } else {
        toHTML(sb);
      }
    }
  }

  /**
   * TagAttribute represents an attribute in a HTML tag.
   */
  public static class TagAttribute {
    private final HTML.Attribute attribute;
    private String value;
    private String originalHtml;

    /**
     * @param attribute the HTML.Attribute. Can't be null.
     * @param value The value in plain-text format. This can be null if the
     *        attribute has no value.
     * @param originalHtml If not null, toOriginalHTML() will preserve original
     *        content. This should contain any leading whitespace from the
     *        original.
     */
    private TagAttribute(HTML.Attribute attribute, String value, String originalHtml) {
      X.assertTrue(attribute != null);
      this.attribute = attribute;
      this.value = value;
      this.originalHtml = originalHtml;
    }

    /** Gets the name */
    public String getName() {
      return attribute.getName();
    }

    /** Gets the HTML.Attribute information */
    public HTML.Attribute getAttribute() {
      return attribute;
    }

    /**
     * Sets the attribute value.
     * This value must be in plain-text, not html-escaped.
     * This can be null, if the attribute has no values.
     * This clears <code>originalHtml_</code> if it were set, so
     * <code>toOriginalHTML()</code> might not preserve original any more.
     */
    public void setValue(String value) {
      this.value = value;
      originalHtml = null;
    }

    /** Returns the attribute value in plain-text, never null */
    public String getValue() {
      return value != null ? value : "";
    }

    /** Returns true if the attribute value is not empty */
    public boolean hasValue() {
      return value != null;
    }

    /**
     * Writes out the attribute in HTML format with all necessary preceding
     * whitespace. Emits originalHtml_ if it were specified to the constructor.
     * Otherwise, emits a new name="value" string with a single preceding space.
     */
    public void toHTML(StringBuilder sb) {
      sb.append(' ');
      sb.append(attribute.getName());
      if (value != null && attribute.getType() != HTML.Attribute.BOOLEAN_TYPE) {
        sb.append("=\"");
        sb.append(CharEscapers.asciiHtmlEscaper().escape(value));
        sb.append("\"");
      }
    }

    /** Returns the attribute html string */
    public String toHTML() {
      StringBuilder sb = new StringBuilder();
      toHTML(sb);
      return sb.toString();
    }

    /**
     * Writes out the attribute in XHTML format (value is always appended,
     * even if it is empty) with all necessary preceeding whitespace.
     */
    public void toXHTML(StringBuilder sb) {
      sb.append(' ');
      sb.append(attribute.getName()).append("=\"");

      // Assume that value-less attribute are boolean attributes like "disabled"
      if (hasValue()) {
        sb.append(CharEscapers.asciiHtmlEscaper().escape(value));
      } else {
        sb.append(attribute.getName());
      }

      sb.append("\"");
    }

    /** Returns the attribute XHTML string */
    public String toXHTML() {
      StringBuilder sb = new StringBuilder();
      toXHTML(sb);
      return sb.toString();
    }

    /**
     * @param sb Destination to which attribute is written, in its original
     * preparsed form if possible.
     */
    public void toOriginalHTML(StringBuilder sb) {
      if (originalHtml != null) {
        sb.append(originalHtml);
      } else {
        toHTML(sb);
      }
    }

    /**
     * Writes out the attribute in its original form as it was parsed..
     */
    public String toOriginalHTML() {
      StringBuilder sb = new StringBuilder();
      toOriginalHTML(sb);
      return sb.toString();
    }

    @Override
    public String toString() {
      return "{" + attribute.getName() + "=" + value + "}";
    }
  }

  /**
   * Filter is like Visitor, except it implies that the nodes may be changed,
   * whereas HtmlDocument.Visitor just implies that the nodes are iterated
   * over. A Filter can behave just like a Visitor if it merely returns the
   * same node that it visited. Also, methods may be called on a node to change
   * the values it contains. Alternatively, a new node entirely can be created
   * and returned, which will essentially replace the previous node with the
   * new node in the document tree. A node may be removed by returning null
   * instead of a node.
   */
  public static interface Filter {
    /** This is called first */
    void start();

    /** A text node */
    Text visitText(Text n);

    /** An open tag */
    Tag visitTag(Tag n);

    /** End tag */
    EndTag visitEndTag(EndTag n);

    /** HTML comment */
    Comment visitComment(Comment n);

    /* Called at the end. */
    void finish();
  }

  /**
   * Like Filter, except each node may be replaced by multiple nodes.  Also,
   * does not do double dispatch accept/visit.
   */
  public static interface MultiplexFilter {
    /**
     * Called first.
     */
    void start();

    /**
     * @param originalNode node to filter
     * @param out Destination to which this object appends nodes to replace
     * originalNode.  Can not be null.
     */
    void filter(Node originalNode, List<Node> out);

    /**
     * Called at the end.
     * @param out Destination to which this object appends nodes at the end of
     * the document.  Can not be null.
     */
    void finish(List<Node> out);
  }

  /**
   * Converts a normal {@link Filter} into a {@link MultiplexFilter}.
   */
  public static class MultiplexFilterAdapter implements MultiplexFilter {

    private final Filter filter;

    public MultiplexFilterAdapter(Filter filter) {
      this.filter = filter;
    }

    public void start() {
      filter.start();
    }

    public void filter(Node originalNode, List<Node> out) {
      if (originalNode == null) {
        return;
      }

      Node resultNode;
      if (originalNode instanceof Tag) {
        resultNode = filter.visitTag((Tag) originalNode);
      } else if (originalNode instanceof Text) {
        resultNode = filter.visitText((Text) originalNode);
      } else if (originalNode instanceof EndTag) {
        resultNode = filter.visitEndTag((EndTag) originalNode);
      } else if (originalNode instanceof Comment) {
        resultNode = filter.visitComment((Comment) originalNode);
      } else {
        throw new IllegalArgumentException("unknown node type: " + originalNode.getClass());
      }

      if (resultNode != null) {
        out.add(resultNode);
      }
    }

    public void finish(List<Node> out) {
      filter.finish();
    }
  }

  /**
   * Like Filter, except each node may be replaced by multiple nodes.  Also,
   * does not do double dispatch accept/visit.  Dispatches filterNode() to
   * node-specific methods.
   */
  public static abstract class SimpleMultiplexFilter implements MultiplexFilter {

    /**
     * @see HtmlDocument.MultiplexFilter#filter(HtmlDocument.Node, List)
     */
    public void filter(Node originalNode, List<Node> out) {
      if (originalNode == null) {
        return;
      }

      if (originalNode instanceof Tag) {
        filterTag((Tag) originalNode, out);
      } else if (originalNode instanceof Text) {
        filterText((Text) originalNode, out);
      } else if (originalNode instanceof EndTag) {
        filterEndTag((EndTag) originalNode, out);
      } else if (originalNode instanceof Comment) {
        filterComment((Comment) originalNode, out);
      } else {
        throw new IllegalArgumentException("unknown node type: "
            + originalNode.getClass());
      }
    }

    public abstract void filterTag(Tag originalTag, List<Node> out);

    public abstract void filterText(Text originalText, List<Node> out);

    public abstract void filterEndTag(EndTag originalEndTag, List<Node> out);

    public void filterComment(Comment originalComment, List<Node> out) {
    }
  }

  /**
   * Contains a list of filters which are applied, in order, to each Node.  The
   * output of each becomes the input to the next.  As soon as one returns an
   * empty list it breaks the chain.
   */
  public static class MultiplexFilterChain implements MultiplexFilter {

    private final List<MultiplexFilter> filters = new ArrayList<MultiplexFilter>();

    /**
     * @param sourceFilters these filters are applied in List order
     */
    public MultiplexFilterChain(List<MultiplexFilter> sourceFilters) {
      filters.addAll(sourceFilters);
    }

    /**
     * @see HtmlDocument.MultiplexFilter#start()
     */
    public void start() {
      for (MultiplexFilter filter : filters) {
        filter.start();
      }
    }

    /**
     * @see HtmlDocument.MultiplexFilter#filter(HtmlDocument.Node, List)
     */
    public void filter(Node originalNode, List<Node> out) {
      List<Node> result = new ArrayList<Node>();
      result.add(originalNode);

      // loop through filters until one returns nothing, or until we're out of
      // filters
      for (MultiplexFilter filter : filters) {
        if (result.isEmpty()) {
          return;
        }

        // apply filter to each node and collect results
        List<Node> newResult = new ArrayList<Node>();
        for (Node node : result) {
          filter.filter(node, newResult);
        }
        result = newResult;
      }

      out.addAll(result);
    }

    /**
     * @see HtmlDocument.MultiplexFilter#finish(List)
     */
    public void finish(List<Node> out) {
      List<Node> result = new ArrayList<Node>();

      // loop through filters until one returns nothing, or until we're out of
      // filters
      for (MultiplexFilter filter : filters) {
        // apply filter to each node and collect results
        List<Node> newResult = new ArrayList<Node>();
        for (Node node : result) {
          filter.filter(node, newResult);
        }
        filter.finish(newResult);
        result = newResult;
      }

      out.addAll(result);
    }
  }

  /**
   * Html visitor allows external code to iterate through the nodes in the
   * document. See HtmlDocument.accept.
   */
  public static interface Visitor {
    /** This is called first */
    void start();

    /** A text node */
    void visitText(Text n);

    /** An open tag */
    void visitTag(Tag n);

    /** End tag */
    void visitEndTag(EndTag n);

    /** comment */
    void visitComment(Comment n);

    /* Called at the end. */
    void finish();
  }

  /**
   * An implementation of the Visitor interface which simply delegates its
   * methods to a wrapped instance of another Visitor.
   *
   * <p>This is useful for chaining Visitors together.
   */
  public static class VisitorWrapper implements Visitor {
    private final Visitor wrapped;

    protected VisitorWrapper(Visitor wrap) {
      wrapped = wrap;
    }

    public void start() {
      wrapped.start();
    }

    public void visitText(Text n) {
      wrapped.visitText(n);
    }

    public void visitTag(Tag n) {
      wrapped.visitTag(n);
    }

    public void visitEndTag(EndTag n) {
      wrapped.visitEndTag(n);
    }

    public void visitComment(Comment n) {
      wrapped.visitComment(n);
    }

    public void finish() {
      wrapped.finish();
    }
  }

  /**
   * A special helper Visitor that builds a HtmlDocument.
   */
  public static class Builder implements Visitor {
    private final boolean preserveComments;
    private final List<Node> nodes = new ArrayList<Node>();
    private HtmlDocument doc;

    /**
     * @see Builder#Builder(boolean)
     */
    public Builder() {
      this(false);
    }

    /**
     * @param preserveComments If false, ignores Comment nodes
     */
    public Builder(boolean preserveComments) {
      this.preserveComments = preserveComments;
    }

    public void addNode(Node node) {
      nodes.add(node);
    }
    public void start() {
    }
    public void visitText(Text t) {
      addNode(t);
    }
    public void visitTag(Tag t) {
      addNode(t);
    }
    public void visitComment(Comment n) {
      if (preserveComments) {
        addNode(n);
      }
    }
    public void visitEndTag(EndTag t) {
      addNode(t);
    }
    public void finish() {
      doc = new HtmlDocument(nodes);
    }

    /** Gets the html document that has been constructed */
    public HtmlDocument getDocument() {
      return doc;
    }
  }

  /**
   * A Visitor that prints out the html document in debug format.
   */
  public static class DebugPrinter implements Visitor {

    private final PrintWriter writer;

    public DebugPrinter(PrintWriter writer) {
      this.writer = writer;
    }

    public void start() {
    }

    public void visitText(Text t) {
      writeCollapsed("TEXT", t.getText());
    }

    public void visitComment(Comment n) {
      writeCollapsed("COMMENT", n.getContent());
    }

    private void writeCollapsed(String type, String s) {
      writer.print(type);
      writer.print(": ");
      String noNewlines = s.replace("\n", " ");
      // Use CharMatcher#WHITESPACE?
      String collapsed = CharMatcher.LEGACY_WHITESPACE.trimAndCollapseFrom(noNewlines, ' ');
      writer.print(collapsed);
    }

    public void visitTag(Tag tag) {
      writer.print("==<" + tag.getName() + ">");
      List<TagAttribute> attributes = tag.getAttributes();
      if (attributes != null) {

        // Attribute values
        List<String> attrs = new ArrayList<String>();
        for (TagAttribute a : attributes) {
          attrs.add("[" + a.getName() + " : " + a.getValue() + "]");
        }
        String[] array = attrs.toArray(new String[attrs.size()]);

        // Sort the attributes so that it's easier to read and compare
        Arrays.sort(array);
        for (int i = 0; i < array.length; i++) {
          writer.print(" " + array[i]);
        }
      }
      writer.println();
    }

    public void visitEndTag(EndTag endtag) {
      writer.println("==</" + endtag.getName() + ">");
    }

    public void finish() {
    }
  }

}