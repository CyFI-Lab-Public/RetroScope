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
 * This is the basic implementation of the DataContext. It stores the root Data node and a stack of
 * Data objects that hold local variables. By definition, local variables are limited to single HDF
 * names, with no '.' allowed. We use this to limit our search in the stack for the first occurence
 * of the first chunk in the variable name.
 */
public class DefaultDataContext implements DataContext {

  /**
   * Root node of the Data structure used by the current context.
   */
  private final Data rootData;

  /**
   * Head of the linked list of local variables, starting with the newest variable created.
   */
  private LocalVariable head = null;

  /**
   * Indicates whether the renderer has pushed a new variable scope but no variable has been created
   * yet.
   */
  private boolean newScope = false;

  public DefaultDataContext(Data data) {
    if (data == null) {
      throw new IllegalArgumentException("rootData is null");
    }
    this.rootData = data;
  }

  @Override
  public Data getRootData() {
    return rootData;
  }

  /**
   * Starts a new variable scope. It is illegal to call this twice in a row without declaring a
   * local variable.
   */
  @Override
  public void pushVariableScope() {
    if (newScope) {
      throw new IllegalStateException("PushVariableScope called twice with no "
          + "variables declared in between.");
    }
    newScope = true;
  }

  /**
   * Removes the current variable scope and references to the variables in it. It is illegal to call
   * this more times than {@link #pushVariableScope} has been called.
   */
  @Override
  public void popVariableScope() {
    if (newScope) {
      // We pushed but created no local variables.
      newScope = false;
    } else {
      // Note, this will throw a NullPointerException if there is no scope to
      // pop.
      head = head.nextScope;
    }
  }

  @Override
  public void createLocalVariableByValue(String name, String value) {
    createLocalVariableByValue(name, value, EscapeMode.ESCAPE_NONE);
  }

  @Override
  public void createLocalVariableByValue(String name, String value, EscapeMode mode) {
    LocalVariable local = createLocalVariable(name);
    local.value = value;
    local.isPath = false;
    local.setEscapeMode(mode);
  }

  @Override
  public void createLocalVariableByValue(String name, String value, boolean isFirst, boolean isLast) {
    LocalVariable local = createLocalVariable(name);
    local.value = value;
    local.isPath = false;
    local.isFirst = isFirst;
    local.isLast = isLast;
  }

  @Override
  public void createLocalVariableByPath(String name, String path) {
    LocalVariable local = createLocalVariable(name);
    local.value = path;
    local.isPath = true;
  }

  private LocalVariable createLocalVariable(String name) {
    if (head == null && !newScope) {
      throw new IllegalStateException("Must call pushVariableScope before "
          + "creating local variable.");
    }
    // First look for a local variable with the same name in the current scope
    // and return that if it exists. If we don't do this then loops and each
    // can cause the list of local variables to explode.
    //
    // We only look at the first local variable (head) if it is part of the
    // current scope (we're not defining a new scope). Since each and loop
    // variables are always in their own scope, there would only be one variable
    // in the current scope if it was a reuse case. For macro calls (which are
    // the only other way createLocalVariable is called multiple times in a
    // single scope and thus head may not be the only local variable in the
    // current scope) it is illegal to use the same argument name more than
    // once. Therefore we don't need to worry about checking to see if the new
    // local variable name matches beyond the first local variable in the
    // current scope.

    if (!newScope && head != null && name.equals(head.name)) {
      // Clear out the fields that aren't set by the callers.
      head.isFirst = true;
      head.isLast = true;
      head.node = null;
      return head;
    }

    LocalVariable local = new LocalVariable();
    local.name = name;
    local.next = head;
    if (newScope) {
      local.nextScope = head;
      newScope = false;
    } else if (head != null) {
      local.nextScope = head.nextScope;
    } else {
      local.nextScope = null;
    }
    head = local;
    return local;
  }

  @Override
  public Data findVariable(String name, boolean create) {
    return findVariable(name, create, head);
  }

  @Override
  public EscapeMode findVariableEscapeMode(String name) {
    Data var = findVariable(name, false);
    if (var == null) {
      return EscapeMode.ESCAPE_NONE;
    } else {
      return var.getEscapeMode();
    }
  }

  private Data findVariable(String name, boolean create, LocalVariable start) {
    // When traversing the stack, we first look for the first chunk of the
    // name. This is so we respect scope. If we are searching for 'foo.bar'
    // and 'foo' is defined in many scopes, we should stop searching
    // after the first time we find 'foo', even if that local variable does
    // not have a child 'bar'.
    String firstChunk = name;
    int dot = name.indexOf('.');
    if (dot != -1) {
      firstChunk = name.substring(0, dot);
    }

    LocalVariable curr = start;

    while (curr != null) {
      if (curr.name.equals(firstChunk)) {
        if (curr.isPath) {
          // The local variable references another Data node, dereference it.
          if (curr.node == null) {
            // We haven't resolved the path yet. Do it now and cache it if
            // not null. Note we begin looking for the dereferenced in the next
            // scope.
            curr.node = findVariable(curr.value, create, curr.nextScope);
            if (curr.node == null) {
              // Node does not exist. Any children won't either.
              return null;
            }
          }
          // We have a reference to the Data node directly. Use it.
          if (dot == -1) {
            // This is the node we're interested in.
            return curr.node;
          } else {
            if (create) {
              return curr.node.createChild(name.substring(dot + 1));
            } else {
              return curr.node.getChild(name.substring(dot + 1));
            }
          }
        } else {
          // This is a literal value local variable. It has no children, nor
          // can it. We want to throw an error on creation of children.
          if (dot == -1) {
            return curr;
          }
          if (create) {
            throw new IllegalStateException("Cannot create children of a "
                + "local literal variable");
          } else {
            // No children.
            return null;
          }
        }
      }
      curr = curr.next;
    }
    if (create) {
      return rootData.createChild(name);
    } else {
      return rootData.getChild(name);
    }
  }

  /**
   * This class holds the name and value/path of a local variable. Objects of this type should only
   * be exposed outside of this class for value-based local variables.
   * <p>
   * Fields are not private to avoid the performance overhead of hidden access methods used for
   * outer classes to access private fields of inner classes.
   */
  private static class LocalVariable extends AbstractData {
    // Pointer to next LocalVariable in the list.
    LocalVariable next;
    // Pointer to the first LocalVariable in the next scope down.
    LocalVariable nextScope;

    String name;
    String value;
    // True if value represents the path to another Data variable.
    boolean isPath;
    // Once the path resolves to a valid Data node, store it here to avoid
    // refetching.
    Data node = null;

    // Used only for loop local variables
    boolean isFirst = true;
    boolean isLast = true;

    public String getName() {
      return name;
    }

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }

    public String getFullPath() {
      return name;
    }

    public void setAttribute(String key, String value) {
      throw new UnsupportedOperationException("Not allowed on local variables.");
    }

    public String getAttribute(String key) {
      return null;
    }

    public boolean hasAttribute(String key) {
      return false;
    }

    public int getAttributeCount() {
      return 0;
    }

    public Iterable<Map.Entry<String, String>> getAttributes() {
      return null;
    }

    public Data getRoot() {
      return null;
    }

    public Data getParent() {
      return null;
    }

    public boolean isFirstSibling() {
      return isFirst;
    }

    public boolean isLastSibling() {
      return isLast;
    }

    public Data getNextSibling() {
      throw new UnsupportedOperationException("Not allowed on local variables.");
    }

    public int getChildCount() {
      return 0;
    }

    public Iterable<? extends Data> getChildren() {
      return null;
    }

    public Data getChild(String path) {
      return null;
    }

    public Data createChild(String path) {
      throw new UnsupportedOperationException("Not allowed on local variables.");
    }

    public void removeTree(String path) {
      throw new UnsupportedOperationException("Not allowed on local variables.");
    }

    public void setSymlink(String sourcePath, String destinationPath) {
      throw new UnsupportedOperationException("Not allowed on local variables.");
    }

    public void setSymlink(String sourcePath, Data destination) {
      throw new UnsupportedOperationException("Not allowed on local variables.");
    }

    public void setSymlink(Data symLink) {
      throw new UnsupportedOperationException("Not allowed on local variables.");
    }

    public Data getSymlink() {
      return this;
    }

    public void copy(String toPath, Data from) {
      throw new UnsupportedOperationException("Not allowed on local variables.");
    }

    public void copy(Data from) {
      throw new UnsupportedOperationException("Not allowed on local variables.");
    }

    public String getValue(String path, String defaultValue) {
      throw new UnsupportedOperationException("Not allowed on local variables.");
    }

    public void write(Appendable out, int indent) throws IOException {
      for (int i = 0; i < indent; i++) {
        out.append("  ");
      }
      out.append(getName()).append(" = ").append(getValue());
    }
  }
}
