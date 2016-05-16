/*******************************************************************************
 * Copyright (c) 2011 Google, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Google, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.wb.internal.core.utils;

import com.google.common.base.Objects;

/**
 * Pair of two objects.
 *
 * @author scheglov_ke
 * @coverage core.util
 */
public final class Pair<L, R> {
  private final L left;
  private final R right;

  ////////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  ////////////////////////////////////////////////////////////////////////////
  public Pair(L left, R right) {
    this.left = left;
    this.right = right;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Object
  //
  ////////////////////////////////////////////////////////////////////////////
  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof Pair<?, ?>)) {
      return false;
    }
    Pair<?, ?> other = (Pair<?, ?>) o;
    return Objects.equal(getLeft(), other.getLeft())
        && Objects.equal(getRight(), other.getRight());
  }

  @Override
  public int hashCode() {
    int hLeft = getLeft() == null ? 0 : getLeft().hashCode();
    int hRight = getRight() == null ? 0 : getRight().hashCode();
    return hLeft + 37 * hRight;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Access
  //
  ////////////////////////////////////////////////////////////////////////////
  public L getLeft() {
    return left;
  }

  public R getRight() {
    return right;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Factory
  //
  ////////////////////////////////////////////////////////////////////////////
  public static <L, R> Pair<L, R> create(L left, R right) {
    return new Pair<L, R>(left, right);
  }
}