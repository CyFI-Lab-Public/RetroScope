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
package org.eclipse.wb.draw2d;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;

/**
 * A collection of cursors.
 *
 * @author lobas_av
 * @coverage gef.draw2d
 */
public interface ICursorConstants {
  /**
   * System resize west-east cursor
   */
  Cursor SIZEWE = new Cursor(null, SWT.CURSOR_SIZEWE);
  /**
   * System resize north-south cursor
   */
  Cursor SIZENS = new Cursor(null, SWT.CURSOR_SIZENS);
  /**
   * System resize all directions cursor.
   */
  // BEGIN ADT MODIFICATIONS
  // The SWT CURSOR_SIZEALL cursor looks wrong; it's cross hairs. Use a hand for resizing
  // instead. See the icons shown in
  //  http://www.eclipse.org/articles/Article-SWT-images/graphics-resources.html
  //Cursor SIZEALL = new Cursor(null, SWT.CURSOR_SIZEALL);
  Cursor SIZEALL = new Cursor(null, SWT.CURSOR_HAND);
  // END ADT MODIFICATIONS
}