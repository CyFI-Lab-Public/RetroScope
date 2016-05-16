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

package com.google.clearsilver.jsilver.data;


import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Represents a hierarchical data set of primitives.
 * 
 * This is the JSilver equivalent to ClearSilver's HDF object.
 * 
 * This class has no synchronization logic. Follow the same thread-safety semantics as you would for
 * a java.util.ArrayList (i.e. concurrent reads allowed, but ensure you have exclusive access when
 * modifying - should not read whilst another thread writes).
 */
public class NestedMapData extends AbstractData {

  /**
   * Number of children a node can have before we bother allocating a HashMap. We currently allocate
   * the HashMap on the 5th child.
   */
  private static final int CHILD_MAP_THRESHOLD = 4;

  private String name;
  private NestedMapData parent;
  private final NestedMapData root;

  // Lazily intitialized after CHILD_MAP_THRESHOLD is hit.
  private Map<String, NestedMapData> children = null;
  // Number of children
  private int childCount = 0;
  // First child (first sibling of children)
  private NestedMapData firstChild = null;
  // Last child (last sibling of children)
  private NestedMapData lastChild = null;

  /**
   * Single object returned by getChildren(). Constructs ChildrenIterator objects.
   */
  private Iterable<NestedMapData> iterableChildren = null;

  // Holds the attributes for this HDF node.
  private Map<String, String> attributeList = null;

  private String value = null;
  private NestedMapData symLink = this;

  // Doubly linked list of siblings.
  private NestedMapData prevSibling = null;
  private NestedMapData nextSibling = null;

  public NestedMapData() {
    name = null;
    parent = null;
    root = this;
  }

  protected NestedMapData(String name, NestedMapData parent, NestedMapData root) {
    this.name = name;
    this.parent = parent;
    this.root = root;
  }

  // ******************* Node creation and removal *******************
  // Must be kept in sync.

  /**
   * Creates a child of this node.
   * 
   * @param chunk name to give the new child node.
   * @return the NestedMapData object corresponding to the new node.
   */
  protected NestedMapData createChildNode(String chunk) {
    NestedMapData sym = followSymLinkToTheBitterEnd();
    NestedMapData data = new NestedMapData(chunk, sym, sym.root);

    // If the parent node's child count is 5 or more and it does not have a
    // children Hashmap, initialize it now.
    if (sym.children == null && sym.childCount >= CHILD_MAP_THRESHOLD) {
      sym.children = new HashMap<String, NestedMapData>();
      // Copy in existing children.
      NestedMapData curr = sym.firstChild;
      while (curr != null) {
        sym.children.put(curr.getName(), curr);
        curr = curr.nextSibling;
      }
    }
    // If the parent node has a children map, add the new child node to it.
    if (sym.children != null) {
      sym.children.put(chunk, data);
    }

    data.prevSibling = sym.lastChild;
    if (sym.lastChild != null) {
      // Update previous lastChild to point to new child.
      sym.lastChild.nextSibling = data;
    } else {
      // There were no previous children so this is the first.
      sym.firstChild = data;
    }
    sym.lastChild = data;

    sym.childCount++;

    return data;
  }

  private void severNode() {
    if (parent == null) {
      return;
    }
    if (parent.children != null) {
      parent.children.remove(name);
    }
    if (prevSibling != null) {
      prevSibling.nextSibling = nextSibling;
    } else {
      parent.firstChild = nextSibling;
    }
    if (nextSibling != null) {
      nextSibling.prevSibling = prevSibling;
    } else {
      parent.lastChild = prevSibling;
    }
    parent.childCount--;

    // Need to cleal the parent pointer or else if someone has a direct reference to this node
    // they will get very strange results.
    parent = null;
  }

  // ******************* Node data *******************

  /**
   * Returns the name of this HDF node. The root node has no name, so calling this on the root node
   * will return null.
   */
  public String getName() {
    return name;
  }

  /**
   * Recursive method that builds the full path to this node via its parent links into the given
   * StringBuilder.
   */
  private void getPathName(StringBuilder sb) {
    if (parent != null && parent != root) {
      parent.getPathName(sb);
      sb.append(".");
    }
    String name = getName();
    if (name != null) {
      sb.append(name);
    }
  }

  /**
   * Returns the full path to this node via its parent links.
   */
  public String getFullPath() {
    StringBuilder sb = new StringBuilder();
    getPathName(sb);
    return sb.toString();
  }

  /**
   * Returns the value of this HDF node, or null if this node has no value. Every node in the tree
   * can have a value, a child, and a next peer.
   */
  public String getValue() {
    return followSymLinkToTheBitterEnd().value;
  }

  /**
   * Set the value of this node. Any symlink that may have been set for this node will be replaced.
   */
  public void setValue(String value) {
    // Clearsilver behaviour is to replace any symlink that may already exist
    // here with the new value, rather than following the symlink.
    this.symLink = this;
    this.value = value;
  }

  // ******************* Attributes *******************

  // We don't expect attributes to be heavily used. They are not used for template parsing.

  public void setAttribute(String key, String value) {
    if (key == null) {
      throw new NullPointerException("Attribute name cannot be null.");
    }
    if (attributeList == null) {
      // Do we need to worry about synchronization?
      attributeList = new HashMap<String, String>();
    }
    if (value == null) {
      attributeList.remove(key);
    } else {
      attributeList.put(key, value);
    }
  }

  public String getAttribute(String key) {
    return attributeList == null ? null : attributeList.get(key);
  }

  public boolean hasAttribute(String key) {
    return attributeList != null && attributeList.containsKey(key);
  }

  public int getAttributeCount() {
    return attributeList == null ? 0 : attributeList.size();
  }

  public Iterable<Map.Entry<String, String>> getAttributes() {
    if (attributeList == null) {
      return Collections.emptySet();
    }
    return attributeList.entrySet();
  }

  // ******************* Children *******************

  /**
   * Return the root of the tree where the current node lies. If the current node is the root,
   * return this.
   */
  public Data getRoot() {
    return root;
  }

  /**
   * Get the parent node.
   */
  public Data getParent() {
    return parent;
  }

  /**
   * Is this the first of its siblings?
   */
  @Override
  public boolean isFirstSibling() {
    return prevSibling == null;
  }

  /**
   * Is this the last of its siblings?
   */
  @Override
  public boolean isLastSibling() {
    return nextSibling == null;
  }

  public Data getNextSibling() {
    return nextSibling;
  }

  /**
   * Returns number of child nodes.
   */
  @Override
  public int getChildCount() {
    return followSymLinkToTheBitterEnd().childCount;
  }

  /**
   * Returns children of this node.
   */
  @Override
  public Iterable<? extends Data> getChildren() {
    if (iterableChildren == null) {
      iterableChildren = new IterableChildren();
    }
    return iterableChildren;
  }

  /**
   * Retrieves the object that is the root of the subtree at hdfpath, returning null if the subtree
   * doesn't exist
   */
  public NestedMapData getChild(String path) {
    NestedMapData current = this;
    for (int lastDot = 0, nextDot = 0; nextDot != -1 && current != null; lastDot = nextDot + 1) {
      nextDot = path.indexOf('.', lastDot);
      String chunk = nextDot == -1 ? path.substring(lastDot) : path.substring(lastDot, nextDot);
      current = current.followSymLinkToTheBitterEnd().getChildNode(chunk);
    }
    return current;
  }

  /**
   * Retrieves the HDF object that is the root of the subtree at hdfpath, create the subtree if it
   * doesn't exist
   */
  public NestedMapData createChild(String path) {
    NestedMapData current = this;
    for (int lastDot = 0, nextDot = 0; nextDot != -1; lastDot = nextDot + 1) {
      nextDot = path.indexOf('.', lastDot);
      String chunk = nextDot == -1 ? path.substring(lastDot) : path.substring(lastDot, nextDot);
      NestedMapData currentSymLink = current.followSymLinkToTheBitterEnd();
      current = currentSymLink.getChildNode(chunk);
      if (current == null) {
        current = currentSymLink.createChildNode(chunk);
      }
    }
    return current;
  }

  /**
   * Non-recursive method that only returns a Data node if this node has a child whose name matches
   * the specified name.
   * 
   * @param name String containing the name of the child to look for.
   * @return a Data node that is the child of this node and named 'name', otherwise {@code null}.
   */
  private NestedMapData getChildNode(String name) {
    NestedMapData sym = followSymLinkToTheBitterEnd();
    if (sym.getChildCount() == 0) {
      // No children. Just return null.
      return null;
    }
    if (sym.children != null) {
      // children map exists. Look it up there.
      return sym.children.get(name);
    }
    // Iterate through linked list of children and look for a name match.
    NestedMapData curr = sym.firstChild;
    while (curr != null) {
      if (curr.getName().equals(name)) {
        return curr;
      }
      curr = curr.nextSibling;
    }
    return null;
  }

  /**
   * Remove the specified subtree.
   */
  public void removeTree(String path) {
    NestedMapData removed = getChild(path);
    if (removed != null) {
      removed.severNode();
    }
  }

  private NestedMapData followSymLinkToTheBitterEnd() {
    NestedMapData current;
    for (current = this; current.symLink != current; current = current.symLink);
    return current;
  }

  // ******************* Symbolic links *******************

  /**
   * Set the source node to be a symbolic link to the destination.
   */
  public void setSymlink(String sourcePath, String destinationPath) {
    setSymlink(sourcePath, createChild(destinationPath));
  }

  /**
   * Set the source node to be a symbolic link to the destination.
   */
  public void setSymlink(String sourcePath, Data destination) {
    createChild(sourcePath).setSymlink(destination);
  }

  /**
   * Set this node to be a symbolic link to another node.
   */
  public void setSymlink(Data symLink) {
    if (symLink instanceof NestedMapData) {
      this.symLink = (NestedMapData) symLink;
    } else {
      String errorMessage =
          "Cannot set symlink of incompatible Data type: " + symLink.getClass().getName();
      if (symLink instanceof ChainedData) {
        errorMessage +=
            "\nOther type is ChainedData indicating there are "
                + "multiple valid Data nodes for the path: " + symLink.getFullPath();
      }
      throw new IllegalArgumentException(errorMessage);
    }
  }

  /**
   * Retrieve the symbolic link this node points to. Will return reference to self if not a symlink.
   */
  public Data getSymlink() {
    return symLink;
  }

  // ************************ Copy *************************

  public void copy(String toPath, Data from) {
    if (toPath == null) {
      throw new NullPointerException("Invalid copy destination path");
    }
    if (from == null) {
      // Is this a nop or should we throw an error?
      return;
    }
    Data to = createChild(toPath);
    to.copy(from);
  }

  public void copy(Data from) {
    if (from == null) {
      // Is this a nop or should we throw an error?
      return;
    }
    // Clear any existing symlink.
    this.symLink = this;

    // Clear any existing attributes and copy the ones from the source.
    if (this.attributeList != null) {
      this.attributeList.clear();
    }
    for (Map.Entry<String, String> attribute : from.getAttributes()) {
      setAttribute(attribute.getKey(), attribute.getValue());
    }

    // If the source node was a symlink, just copy the link over and we're done.
    if (from.getSymlink() != from) {
      setSymlink(from.getSymlink());
      return;
    }

    // Copy over the node's value.
    setValue(from.getValue());

    // For each source child, create a new child with the same name and recurse.
    for (Data fromChild : from.getChildren()) {
      Data toChild = createChild(fromChild.getName());
      toChild.copy(fromChild);
    }
  }

  /**
   * Write out the String representation of this HDF node.
   */
  public void write(Appendable out, int indent) throws IOException {
    if (symLink != this) {
      indent(out, indent);
      writeNameAttrs(out);
      out.append(" : ").append(symLink.getFullPath()).append('\n');
      return;
    }
    if (getValue() != null) {
      indent(out, indent);
      writeNameAttrs(out);
      if (getValue().contains("\n")) {
        writeMultiline(out);
      } else {
        out.append(" = ").append(getValue()).append('\n');
      }
    }
    if (getChildCount() > 0) {
      int childIndent = indent;
      if (this != root) {
        indent(out, indent);
        writeNameAttrs(out);
        out.append(" {\n");
        childIndent++;
      }
      for (Data child : getChildren()) {
        child.write(out, childIndent);
      }
      if (this != root) {
        indent(out, indent);
        out.append("}\n");
      }
    }
  }

  /**
   * Here we optimize the structure for long-term use. We call intern() on all Strings to reduce the
   * copies of the same string that appear
   */
  @Override
  public void optimize() {
    name = name == null ? null : name.intern();
    value = value == null ? null : value.intern();
    if (attributeList != null) {
      Map<String, String> newList = new HashMap<String, String>(attributeList.size());
      for (Map.Entry<String, String> entry : attributeList.entrySet()) {
        String key = entry.getKey();
        String value = entry.getValue();
        key = key == null ? null : key.intern();
        value = value == null ? null : value.intern();
        newList.put(key, value);
      }
      attributeList = newList;
    }
    for (NestedMapData child = firstChild; child != null; child = child.nextSibling) {
      child.optimize();
    }
  }

  private void writeMultiline(Appendable out) throws IOException {
    String marker = "EOM";
    while (getValue().contains(marker)) {
      marker += System.nanoTime() % 10;
    }
    out.append(" << ").append(marker).append('\n').append(getValue());
    if (!getValue().endsWith("\n")) {
      out.append('\n');
    }
    out.append(marker).append('\n');
  }

  private void indent(Appendable out, int indent) throws IOException {
    for (int i = 0; i < indent; i++) {
      out.append("  ");
    }
  }

  private void writeNameAttrs(Appendable out) throws IOException {
    // Output name
    out.append(getName());
    if (attributeList != null && !attributeList.isEmpty()) {
      // Output parameters.
      out.append(" [");
      boolean first = true;
      for (Map.Entry<String, String> attr : attributeList.entrySet()) {
        if (first) {
          first = false;
        } else {
          out.append(", ");
        }
        out.append(attr.getKey());
        if (attr.getValue().equals("1")) {
          continue;
        }
        out.append(" = \"");
        writeAttributeValue(out, attr.getValue());
        out.append('"');
      }
      out.append(']');
    }
  }

  // Visible for testing
  static void writeAttributeValue(Appendable out, String value) throws IOException {
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      switch (c) {
        case '"':
          out.append("\\\"");
          break;
        case '\n':
          out.append("\\n");
          break;
        case '\t':
          out.append("\\t");
          break;
        case '\\':
          out.append("\\\\");
          break;
        case '\r':
          out.append("\\r");
          break;
        default:
          out.append(c);
      }
    }
  }

  /**
   * A single instance of this is created per NestedMapData object. Its single method returns an
   * iterator over the children of this node.
   * <p>
   * Note: This returns an iterator that starts with the first child at the time of iterator() being
   * called, not when this Iterable object was handed to the code. This might result in slightly
   * unexpected behavior if the children list is modified between when getChildren() is called and
   * iterator is called on the returned object but this should not be an issue in practice as
   * iterator is usually called immediately after getChildren().
   * 
   */
  private class IterableChildren implements Iterable<NestedMapData> {
    public Iterator<NestedMapData> iterator() {
      return new ChildrenIterator(followSymLinkToTheBitterEnd().firstChild);
    }
  }

  /**
   * Iterator implementation for children. We do not check for concurrent modification like in other
   * Collection iterators.
   */
  private static class ChildrenIterator implements Iterator<NestedMapData> {
    NestedMapData current;
    NestedMapData next;

    ChildrenIterator(NestedMapData first) {
      next = first;
      current = null;
    }

    public boolean hasNext() {
      return next != null;
    }

    public NestedMapData next() {
      if (next == null) {
        throw new NoSuchElementException();
      }
      current = next;
      next = next.nextSibling;
      return current;
    }

    public void remove() {
      if (current == null) {
        throw new NoSuchElementException();
      }
      current.severNode();
      current = null;
    }
  }
}
