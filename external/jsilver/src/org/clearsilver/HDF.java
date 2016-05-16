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

package org.clearsilver;

import java.io.Closeable;
import java.io.IOException;
import java.util.Date;
import java.util.TimeZone;

/**
 * This interface establishes the API for an HDF data structure used by
 * Clearsilver templates when rendering content.
 */
public interface HDF extends Closeable {

  /**
   * Clean up CS object state.
   */
  void close();

  /**
   * Loads the contents of the specified HDF file from disk into the current
   * HDF object.  The loaded contents are merged with the existing contents.
   */
  boolean readFile(String filename) throws IOException;

  /**
   * Get the file loader in use, if any.
   * @return the file loader in use.
   */
  CSFileLoader getFileLoader();

  /**
   * Set the CS file loader to use
   * @param fileLoader the file loader that should be used.
   */
  void setFileLoader(CSFileLoader fileLoader);

  /**
   * Serializes HDF contents to a file (readable by readFile)
   */
  boolean writeFile(String filename) throws IOException;

  /**
   * Parses/loads the contents of the given string as HDF into the current
   * HDF object.  The loaded contents are merged with the existing contents.
   */
  boolean readString(String data);

  /**
   * Serializes HDF contents to a string (readable by readString)
   */
  String writeString();

  /**
   * Retrieves the integer value at the specified path in this HDF node's
   * subtree.  If the value does not exist, or cannot be converted to an
   * integer, default_value will be returned.
   */
  int getIntValue(String hdfName, int defaultValue);

  /**
   * Retrieves the value at the specified path in this HDF node's subtree.
   */
  String getValue(String hdfName, String defaultValue);

  /**
   * Sets the value at the specified path in this HDF node's subtree.
   */
  void setValue(String hdfName, String value);

  /**
   * Remove the specified subtree.
   */
  void removeTree(String hdfName);

  /**
   * Links the src hdf name to the dest.
   */
  void setSymLink(String hdfNameSrc, String hdfNameDest);

  /**
   * Export a date to a clearsilver tree using a specified timezone
   */
  void exportDate(String hdfName, TimeZone timeZone, Date date);

  /**
   * Export a date to a clearsilver tree using a specified timezone
   */
  void exportDate(String hdfName, String tz, int tt);

  /**
   * Retrieves the HDF object that is the root of the subtree at hdfpath, or
   * null if no object exists at that path.
   */
  HDF getObj(String hdfpath);

  /**
   * Retrieves the HDF for the first child of the root of the subtree
   * at hdfpath, or null if no child exists of that path or if the
   * path doesn't exist.
   */
  HDF getChild(String hdfpath);

  /**
   * Return the root of the tree where the current node lies.  If the
   * current node is the root, return this. Implementations may not
   * necessarily return the same instance of {@code HDF} every time.
   * Use {@link #belongsToSameRoot(HDF)} to check if two {@code HDF}s
   * belong to the same root.
   */
  HDF getRootObj();

  /**
   * Checks if the given hdf object belongs to the same root HDF object
   * as this one.
   *
   * @param hdf The hdf object to compare to.
   * @throws IllegalArgumentException If the supplied hdf object is from
   *         a different implementation (e.g. mixing JNI and jsilver).
   */
  boolean belongsToSameRoot(HDF hdf);

  /**
   * Retrieves the HDF object that is the root of the subtree at
   * hdfpath, create the subtree if it doesn't exist
   */
  HDF getOrCreateObj(String hdfpath);

  /**
   * Returns the name of this HDF node.   The root node has no name, so
   * calling this on the root node will return null.
   */
  String objName();

  /**
   * Returns the value of this HDF node, or null if this node has no value.
   * Every node in the tree can have a value, a child, and a next peer.
   */
  String objValue();

  /**
   * Returns the child of this HDF node, or null if there is no child.
   * Use this in conjunction with objNext to walk the HDF tree.  Every node
   * in the tree can have a value, a child, and a next peer.
   */
  HDF objChild();

  /**
   * Returns the child of this HDF node, or null if there is no child.
   * Use this in conjunction with objNext to walk the HDF tree.  Every node
   * in the tree can have a value, a child, and a next peer.
   */
  HDF objNext();

  /**
   * Deep copy of the contents of the source HDF structure to this HDF
   * starting at the specified HDF path node.
   * <p>
   * This method copies over the attributes and value of the node and recurses
   * through all the children of the source node.  Any symlink in the source
   * node becomes a symlink in the copy.
   * <p>
   * @param hdfpath the node within this HDF where the source structure should
   * be copied to.
   * @param src the source HDF to copy over.
   */
  void copy(String hdfpath, HDF src);

  /**
   * Generates a string representing the content of the HDF tree rooted at
   * this node.
   */
  String dump();
}

