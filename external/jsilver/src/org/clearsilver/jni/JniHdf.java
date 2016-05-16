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

package org.clearsilver.jni;

import org.clearsilver.CSFileLoader;
import org.clearsilver.HDF;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * This class is a wrapper around the HDF C API.  Many features of the C API
 * are not yet exposed through this wrapper.
 */
public class JniHdf implements HDF {

  long hdfptr;  // stores the C HDF* pointer
  JniHdf root; // If this is a child HDF node, points at the root node of
               // the tree.  For root nodes this is null.  A child node needs
               // to hold a reference on the root to prevent the root from
               // being GC-ed.

  static {
    JNI.loadLibrary();
  }

  static JniHdf cast(HDF hdf) {
    if (!(hdf instanceof JniHdf)) {
      throw new IllegalArgumentException("HDF object not of type JniHdf.  "
          + "Make sure you use the same ClearsilverFactory to construct all "
          + "related HDF and CS objects.");
    }
    return (JniHdf)hdf;
  }

  /**
   * Default public constructor.
   */
  public JniHdf() {
    hdfptr = _init();
    root = null;
  }

  protected JniHdf(long hdfptr, JniHdf parent) {
    this.hdfptr = hdfptr;
    this.root = (parent.root != null) ? parent.root : parent;
  }

  /** Constructs an HDF child node.  Used by other methods in this class when
   * a child node needs to be constructed.
   */
  protected JniHdf newHdf(long hdfptr, HDF parent) {
    return new JniHdf(hdfptr, cast(parent));
  }

  /** Clean up allocated memory if neccesary. close() allows application
   *  to force clean up.
   */
  public void close() {
    // Only root nodes have ownership of the C HDF pointer, so only a root
    // node needs to dealloc hdfptr.dir
    if (root == null) {
      if (hdfptr != 0) {
        _dealloc(hdfptr);
        hdfptr = 0;
      }
    }
  }

  /** Call close() just in case when deallocating Java object.
   */
  protected void finalize() throws Throwable {
    close();
    super.finalize();
  }

  /** Loads the contents of the specified HDF file from disk into the current
   *  HDF object.  The loaded contents are merged with the existing contents.
   *  @param filename the name of file to read in and parse.
   *  @throws java.io.FileNotFoundException if the specified file does not
   *  exist.
   *  @throws IOException other problems reading the file.
   */
  public boolean readFile(String filename) throws IOException {
    if (hdfptr == 0) {
      throw new NullPointerException("HDF is closed.");
    }
    return _readFile(hdfptr, filename, fileLoader != null);
  }

  protected String fileLoad(String filename) throws IOException {
    if (hdfptr == 0) {
      throw new NullPointerException("HDF is closed.");
    }
    CSFileLoader aFileLoader = fileLoader;
    if (aFileLoader == null) {
      throw new NullPointerException("No fileLoader specified.");
    } else {
      String result = aFileLoader.load(this, filename);
      if (result == null) {
        throw new NullPointerException("CSFileLoader.load() returned null");
      }
      return result;
    }
  }

  // The optional CS file loader to use to read in files
  private CSFileLoader fileLoader = null;

  /**
   * Get the file loader in use, if any.
   * @return the file loader in use.
   */
  public CSFileLoader getFileLoader() {
    return fileLoader;
  }

  /**
   * Set the CS file loader to use
   * @param fileLoader the file loader that should be used.
   */
  public void setFileLoader(CSFileLoader fileLoader) {
    this.fileLoader = fileLoader;
  }

  /** Serializes HDF contents to a file (readable by readFile)
   */
  public boolean writeFile(String filename) throws IOException {
    if (hdfptr == 0) {
      throw new NullPointerException("HDF is closed.");
    }
    return _writeFile(hdfptr, filename);
  }

  /** Parses/loads the contents of the given string as HDF into the current
   *  HDF object.  The loaded contents are merged with the existing contents.
   */
  public boolean readString(String data) {
    if (hdfptr == 0) {
      throw new NullPointerException("HDF is closed.");
    }
    return _readString(hdfptr, data);
  }

  /** Serializes HDF contents to a string (readable by readString)
   */
  public String writeString() {
    if (hdfptr == 0) {
      throw new NullPointerException("HDF is closed.");
    }
    return _writeString(hdfptr);
  }

  /** Retrieves the integer value at the specified path in this HDF node's
   *  subtree.  If the value does not exist, or cannot be converted to an
   *  integer, default_value will be returned. */
  public int getIntValue(String hdfname, int default_value) {
    if (hdfptr == 0) {
      throw new NullPointerException("HDF is closed.");
    }
    return _getIntValue(hdfptr,hdfname,default_value);
  }

  /** Retrieves the value at the specified path in this HDF node's subtree.
   */
  public String getValue(String hdfname, String default_value) {
    if (hdfptr == 0) {
      throw new NullPointerException("HDF is closed.");
    }
    return _getValue(hdfptr,hdfname,default_value);
  }

  /** Sets the value at the specified path in this HDF node's subtree. */
  public void setValue(String hdfname, String value) {
    if (hdfptr == 0) {
      throw new NullPointerException("HDF is closed.");
    }
    _setValue(hdfptr,hdfname,value);
  }

  /** Remove the specified subtree. */
  public void removeTree(String hdfname) {
    if (hdfptr == 0) {
      throw new NullPointerException("HDF is closed.");
    }
    _removeTree(hdfptr,hdfname);
  }

  /** Links the src hdf name to the dest. */
  public void setSymLink(String hdf_name_src, String hdf_name_dest) {
    if (hdfptr == 0) {
      throw new NullPointerException("HDF is closed.");
    }
    _setSymLink(hdfptr,hdf_name_src,hdf_name_dest);
  }

  /** Export a date to a clearsilver tree using a specified timezone */
  public void exportDate(String hdfname, TimeZone timeZone, Date date) {
    if (hdfptr == 0) {
      throw new NullPointerException("HDF is closed.");
    }

    Calendar cal = Calendar.getInstance(timeZone);
    cal.setTime(date);

    String sec = Integer.toString(cal.get(Calendar.SECOND));
    setValue(hdfname + ".sec", sec.length() == 1 ? "0" + sec : sec);

    String min = Integer.toString(cal.get(Calendar.MINUTE));
    setValue(hdfname + ".min", min.length() == 1 ? "0" + min : min);

    setValue(hdfname + ".24hour",
        Integer.toString(cal.get(Calendar.HOUR_OF_DAY)));
    // java.util.Calendar uses represents 12 o'clock as 0
    setValue(hdfname + ".hour",
        Integer.toString(
            cal.get(Calendar.HOUR) == 0 ? 12 : cal.get(Calendar.HOUR)));
    setValue(hdfname + ".am",
        cal.get(Calendar.AM_PM) == Calendar.AM ? "1" : "0");
    setValue(hdfname + ".mday",
        Integer.toString(cal.get(Calendar.DAY_OF_MONTH)));
    setValue(hdfname + ".mon",
        Integer.toString(cal.get(Calendar.MONTH)+1));
    setValue(hdfname + ".year",
        Integer.toString(cal.get(Calendar.YEAR)));
    setValue(hdfname + ".2yr",
        Integer.toString(cal.get(Calendar.YEAR)).substring(2));

    // Java DAY_OF_WEEK puts Sunday .. Saturday as 1 .. 7 respectively
    // See http://java.sun.com/j2se/1.5.0/docs/api/java/util/Calendar.html#DAY_OF_WEEK
    // However, C and Python export Sun .. Sat as 0 .. 6, because
    // POSIX localtime_r produces wday 0 .. 6.  So, adjust.
    setValue(hdfname + ".wday",
        Integer.toString(cal.get(Calendar.DAY_OF_WEEK) - 1));

    boolean tzNegative = timeZone.getRawOffset() < 0;
    int tzAbsolute = java.lang.Math.abs(timeZone.getRawOffset()/1000);
    String tzHour = Integer.toString(tzAbsolute/3600);
    String tzMin = Integer.toString(tzAbsolute/60 - (tzAbsolute/3600)*60);
    String tzString = (tzNegative ? "-" : "+")
        + (tzHour.length() == 1 ? "0" + tzHour : tzHour)
        + (tzMin.length() == 1 ? "0" + tzMin : tzMin);
    setValue(hdfname + ".tzoffset", tzString);
  }

  /** Export a date to a clearsilver tree using a specified timezone */
  public void exportDate(String hdfname, String tz, int tt) {
    if (hdfptr == 0) {
      throw new NullPointerException("HDF is closed.");
    }

    TimeZone timeZone = TimeZone.getTimeZone(tz);

    if (timeZone == null) {
      throw new RuntimeException("Unknown timezone: " + tz);
    }

    Date date = new Date((long)tt * 1000);

    exportDate(hdfname, timeZone, date);
  }

  /** Retrieves the HDF object that is the root of the subtree at hdfpath, or
   *  null if no object exists at that path. */
  public JniHdf getObj(String hdfpath) {
    if (hdfptr == 0) {
      throw new NullPointerException("HDF is closed.");
    }
    long obj_ptr = _getObj(hdfptr, hdfpath);
    if ( obj_ptr == 0 ) {
      return null;
    }
    return newHdf(obj_ptr, this);
  }

  /** Retrieves the HDF for the first child of the root of the subtree
   *  at hdfpath, or null if no child exists of that path or if the
   *  path doesn't exist. */
  public JniHdf getChild(String hdfpath) {
    if (hdfptr == 0) {
      throw new NullPointerException("HDF is closed.");
    }
    long obj_ptr = _getChild(hdfptr, hdfpath);
    if ( obj_ptr == 0 ) {
      return null;
    }
    return newHdf(obj_ptr, this);
  }

  /** Return the root of the tree where the current node lies.  If the
   *  current node is the root, return this. */
  public JniHdf getRootObj() {
    return root != null ? root : this;
  }

  public boolean belongsToSameRoot(HDF hdf) {
    JniHdf jniHdf = cast(hdf);
    return this.getRootObj() == jniHdf.getRootObj();
  }

  /** Retrieves the HDF object that is the root of the subtree at
   *  hdfpath, create the subtree if it doesn't exist */
  public JniHdf getOrCreateObj(String hdfpath) {
    if (hdfptr == 0) {
      throw new NullPointerException("HDF is closed.");
    }
    long obj_ptr = _getObj(hdfptr, hdfpath);
    if ( obj_ptr == 0 ) {
      // Create a node
      _setValue(hdfptr, hdfpath, "");
      obj_ptr = _getObj( hdfptr, hdfpath );
      if ( obj_ptr == 0 ) {
        return null;
      }
    }
    return newHdf(obj_ptr, this);
  }

  /** Returns the name of this HDF node.   The root node has no name, so
   *  calling this on the root node will return null. */
  public String objName() {
    if (hdfptr == 0) {
      throw new NullPointerException("HDF is closed.");
    }
    return _objName(hdfptr);
  }

  /** Returns the value of this HDF node, or null if this node has no value.
   *  Every node in the tree can have a value, a child, and a next peer. */
  public String objValue() {
    if (hdfptr == 0) {
      throw new NullPointerException("HDF is closed.");
    }
    return _objValue(hdfptr);
  }

  /** Returns the child of this HDF node, or null if there is no child.
   *  Use this in conjunction with objNext to walk the HDF tree.  Every node
   *  in the tree can have a value, a child, and a next peer.
   */
  public JniHdf objChild() {
    if (hdfptr == 0) {
      throw new NullPointerException("HDF is closed.");
    }
    long child_ptr = _objChild(hdfptr);
    if ( child_ptr == 0 ) {
      return null;
    }
    return newHdf(child_ptr, this);
  }

  /** Returns the next sibling of this HDF node, or null if there is no next
   *  sibling.  Use this in conjunction with objChild to walk the HDF tree.
   *  Every node in the tree can have a value, a child, and a next peer.
   */
  public JniHdf objNext() {
    if (hdfptr == 0) {
      throw new NullPointerException("HDF is closed.");
    }
    long next_ptr = _objNext(hdfptr);
    if ( next_ptr == 0 ) {
      return null;
    }
    return newHdf(next_ptr, this);
  }

  public void copy(String hdfpath, HDF src) {
    JniHdf source = cast(src);
    if (hdfptr == 0 || source.hdfptr == 0) {
      throw new NullPointerException("HDF is closed.");
    }
    _copy(hdfptr, hdfpath, source.hdfptr);
  }

  /**
   * Generates a string representing the content of the HDF tree rooted at
   * this node.
   */
  public String dump() {
    if (hdfptr == 0) {
      throw new NullPointerException("HDF is closed.");
    }
    return _dump(hdfptr);
  }

  private static native long _init();
  private static native void _dealloc(long ptr);
  private native boolean _readFile(long ptr, String filename, boolean use_cb)
      throws IOException;
  private static native boolean _writeFile(long ptr, String filename);
  private static native boolean _readString(long ptr, String data);
  private static native String _writeString(long ptr);
  private static native int _getIntValue(long ptr, String hdfname,
      int default_value);
  private static native String _getValue(long ptr, String hdfname,
      String default_value);
  private static native void _setValue(long ptr, String hdfname,
      String hdf_value);
  private static native void _removeTree(long ptr, String hdfname);
  private static native void _setSymLink(long ptr, String hdf_name_src,
      String hdf_name_dest);
  private static native long _getObj(long ptr, String hdfpath);
  private static native long _getChild(long ptr, String hdfpath);
  private static native long _objChild(long ptr);
  private static native long _objNext(long ptr);
  private static native String _objName(long ptr);
  private static native String _objValue(long ptr);
  private static native void _copy(long destptr, String hdfpath, long srcptr);

  private static native String _dump(long ptr);
}
