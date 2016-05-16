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
package org.eclipse.wb.internal.core.utils.check;

/**
 * <code>AssertionFailedException</code> is a runtime exception thrown by some of the methods in
 * <code>Assert</code>.
 * 
 * @author scheglov_ke
 * @coverage core.util
 */
public final class AssertionFailedException extends RuntimeException {
  private static final long serialVersionUID = 0L;

  ////////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Constructs a new exception with the given message.
   * 
   * @param detail
   *          the message
   */
  public AssertionFailedException(String detail) {
    super(detail);
  }
}
