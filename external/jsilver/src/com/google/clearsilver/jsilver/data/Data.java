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

import com.google.clearsilver.jsilver.autoescape.EscapeMode;

import java.io.IOException;
import java.util.Map;

/**
 * Represents a hierarchical data set of primitives.
 * 
 * This is the JSilver equivalent to ClearSilver's HDF object.
 */
public interface Data {

  // ******************* Node data *******************

  /**
   * Returns the name of this HDF node. The root node has no name, so calling this on the root node
   * will return null.
   */
  String getName();

  /**
   * Returns the value of this HDF node, or null if this node has no value. Every node in the tree
   * can have a value, a child, and a next peer.
   */
  String getValue();

  /**
   * Returns the integer value of this HDF node, or 0 if this node has no value.
   * 
   * Note: The fact that this method returns a primitive type, rather than an Integer means that its
   * value cannot be used to determine whether the data node exists or not. Note also that, when
   * implementing a Data object that caches these values, care must be taken to ensure that a node
   * with an integer value of '0' is not mistaken for a non-existent node.
   */
  int getIntValue();

  /**
   * Returns the boolean value of this HDF node, or false if this node has no value.
   * 
   * Note: The fact that this method returns a primitive type, rather than a Boolean means that its
   * value cannot be used to determine whether the data node exists or not. Note also that, when
   * implementing a Data object that caches these values, care must be taken to ensure that a node
   * with a boolean value of 'false' is not mistaken for a non-existent node.
   */
  boolean getBooleanValue();

  /**
   * Set the value of this node. Any symlink that may have been set for this node will be replaced.
   */
  void setValue(String value);

  /**
   * Returns the full path to this node via its parent links.
   */
  String getFullPath();

  // ******************* Attributes *******************

  /**
   * Sets an attribute key and value on the current node, replacing any existing value.
   * 
   * @param key the name of the attribute to add/modify.
   * @param value the value to assign it. Value of {@code null} will clear the attribute.
   */
  void setAttribute(String key, String value);

  /**
   * Returns the value of the node attribute with the given name, or {@code null} if there is no
   * value.
   */
  String getAttribute(String key);

  /**
   * Returns {@code true} if the node contains an attribute with the given name, {@code false}
   * otherwise.
   */
  boolean hasAttribute(String key);

  /**
   * Returns the number of attributes on this node.
   */
  int getAttributeCount();

  /**
   * Returns an iterable collection of attribute name/value pairs.
   * 
   * @return an object that can be iterated over to get all the attribute name/value pairs. Should
   *         return empty iterator if there are no attributes.
   */
  Iterable<Map.Entry<String, String>> getAttributes();

  // ******************* Children *******************

  /**
   * Return the root of the tree where the current node lies. If the current node is the root,
   * return this.
   */
  Data getRoot();

  /**
   * Get the parent node.
   */
  Data getParent();

  /**
   * Is this the first of its siblings?
   */
  boolean isFirstSibling();

  /**
   * Is this the last of its siblings?
   */
  boolean isLastSibling();

  /**
   * Retrieves the node representing the next sibling of this Data node, if any.
   * 
   * @return the next sibling Data object or {@code null} if this is the last sibling.
   */
  Data getNextSibling();

  /**
   * Returns number of child nodes.
   */
  int getChildCount();

  /**
   * Returns children of this node.
   */
  Iterable<? extends Data> getChildren();

  /**
   * Retrieves the object that is the root of the subtree at hdfpath, returning null if the subtree
   * doesn't exist
   */
  Data getChild(String path);

  /**
   * Retrieves the HDF object that is the root of the subtree at hdfpath, create the subtree if it
   * doesn't exist
   */
  Data createChild(String path);

  /**
   * Remove the specified subtree.
   */
  void removeTree(String path);

  // ******************* Symbolic links *******************

  /**
   * Set the source node to be a symbolic link to the destination.
   */
  void setSymlink(String sourcePath, String destinationPath);

  /**
   * Set the source node to be a symbolic link to the destination.
   */
  void setSymlink(String sourcePath, Data destination);

  /**
   * Set this node to be a symbolic link to another node.
   */
  void setSymlink(Data symLink);

  /**
   * Retrieve the symbolic link this node points to. Will return reference to self if not a symlink.
   */
  Data getSymlink();

  // **************************** Copy **************************

  /**
   * Does a deep copy of the attributes and values from one node to another.
   * 
   * @param toPath destination path for the deep copy.
   * @param from Data object that should be copied over.
   */
  void copy(String toPath, Data from);

  /**
   * Does a deep copy the attributes and values from one node to another
   * 
   * @param from Data object whose value should be copied over.
   */
  void copy(Data from);

  // ******************* Convenience methods *******************

  /**
   * Retrieves the value at the specified path in this HDF node's subtree.
   */
  String getValue(String path, String defaultValue);

  /**
   * Retrieves the integer value at the specified path in this HDF node's subtree. If the value does
   * not exist, or cannot be converted to an integer, default_value will be returned.
   */
  int getIntValue(String path, int defaultValue);

  /**
   * Retrieves the value at the specified path in this HDF node's subtree. If not found, returns
   * null.
   */
  String getValue(String path);

  /**
   * Retrieves the value at the specified path in this HDF node's subtree. If not found or invalid,
   * returns 0.
   */
  int getIntValue(String path);

  /**
   * Retrieves the value at the specified path in this HDF node's subtree. If not found or invalid,
   * returns false.
   */
  boolean getBooleanValue(String path);

  /**
   * Sets the value at the specified path in this HDF node's subtree.
   */
  void setValue(String path, String value);

  // ******************* String representation *******************

  String toString();

  void toString(StringBuilder out, int indent);

  /**
   * Write out the String representation of this HDF node.
   */
  void write(Appendable out, int indent) throws IOException;

  /**
   * Optimizes the Data structure for performance. This is a somewhat expensive operation that
   * should improve CPU and/or memory usage for long-lived Data objects. For example, it may
   * internalize all Strings to reduce redundant copies.
   */
  void optimize();

  /**
   * Indicates the escaping, if any that was applied to this HDF node.
   * 
   * @return EscapeMode that was applied to this node's value. {@code EscapeMode.ESCAPE_NONE} if the
   *         value is not escaped. {@code EscapeMode.ESCAPE_IS_CONSTANT} if value is a string or
   *         numeric literal.
   * 
   * @see #setEscapeMode
   * @see EscapeMode
   */
  EscapeMode getEscapeMode();

  /**
   * Set the escaping that was applied to this HDF node. This method may be called by the template
   * renderer, for instance, when a "set" command sets the node to a constant string. It may also be
   * explicitly called if populating the HDF with pre-escaped or trusted values.
   * 
   * @see #getEscapeMode
   */
  void setEscapeMode(EscapeMode mode);
}
