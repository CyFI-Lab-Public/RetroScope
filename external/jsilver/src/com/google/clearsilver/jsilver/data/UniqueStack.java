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

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

/**
 * The {@code ResourceStack} represents a LIFO stack of unique objects (resources).
 * 
 * <p>
 * An attempt to insert on a stack an object that is already there will fail and result with a
 * method {@link #push(Object)} returning false.
 * 
 * <p>
 * All provided operations ({@link #pop()} and {@link #push(Object)}) are done in average constant
 * time.
 * 
 * <p>
 * This class is iterable
 */
public class UniqueStack<T> implements Iterable<T> {
  // Field used for optimization: when only one object was
  // added we postpone the initialization and use this field.
  private T firstObject = null;

  // Contains a stack of all stored objects.
  private LinkedList<T> objectStack = null;
  // A HashSet allowing quick look-ups on the stack.
  private HashSet<T> objectsSet = null;

  /**
   * A wrapper for a {@code Iterator<T>} object that provides immutability.
   * 
   * @param <T>
   */
  private static class ImmutableIterator<T> implements Iterator<T> {
    private static final String MODIFICATION_ERROR_MESSAGE =
        "ResourceStack cannot be modyfied by Iterator.remove()";

    private final Iterator<T> iterator;

    private ImmutableIterator(Iterator<T> iterator) {
      this.iterator = iterator;
    }

    @Override
    public boolean hasNext() {
      return iterator.hasNext();
    }

    @Override
    public T next() {
      return iterator.next();
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException(MODIFICATION_ERROR_MESSAGE);
    }
  }

  /**
   * Add an object to a stack. Object will be added only if it is not already on the stack.
   * 
   * @param object to be added. If it is {@code null} a {@code NullPointerException} will be thrown.
   * @return true if the object was added successfully
   */
  public boolean push(T object) {
    if (object == null) {
      throw new NullPointerException();
    }

    if (objectStack == null) {
      if (firstObject == null) {
        firstObject = object;
        return true;
      } else {
        if (firstObject.equals(object)) {
          return false;
        }
        initStackAndSet();
      }
    } else {
      if (objectsSet.contains(object)) {
        return false;
      }
    }

    objectStack.offerLast(object);
    objectsSet.add(object);
    return true;
  }

  private void initStackAndSet() {
    objectStack = new LinkedList<T>();
    objectsSet = new HashSet<T>();

    objectStack.offerLast(firstObject);
    objectsSet.add(firstObject);

    // there is no need for using firstObject pointer anymore
    firstObject = null;
  }

  /**
   * Removes last added object from the stack.
   * 
   * @return last added object
   * @throws NoSuchElementException - if the stack is empty
   */
  public T pop() {
    T returnedValue = null;

    if (isEmpty()) {
      throw new NoSuchElementException();
    }

    if (objectStack == null) {
      returnedValue = firstObject;
      firstObject = null;
    } else {
      returnedValue = objectStack.pollLast();
      objectsSet.remove(returnedValue);
    }
    return returnedValue;
  }

  /**
   * Returns {@code true} if this stack contains no elements.
   * 
   * @return {@code true} if this stack contains no elements.
   */
  public boolean isEmpty() {
    if (firstObject != null) {
      return false;
    }
    return (objectStack == null || objectStack.isEmpty());
  }

  @Override
  public Iterator<T> iterator() {
    if (objectStack == null) {
      initStackAndSet();
    }
    return new ImmutableIterator<T>(objectStack.iterator());
  }
}
