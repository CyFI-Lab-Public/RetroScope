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
package org.eclipse.wb.internal.core.utils.ui;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Control;

/**
 * Helper class for converting DLU and char size into pixels.
 * 
 * Based on code from JDT UI.
 * 
 * @author scheglov_ke
 */
public class PixelConverter {
  private final FontMetrics fFontMetrics;

  ////////////////////////////////////////////////////////////////////////////
  //
  // Constructors
  //
  ////////////////////////////////////////////////////////////////////////////
  public PixelConverter(Control control) {
    GC gc = new GC(control);
    gc.setFont(control.getFont());
    fFontMetrics = gc.getFontMetrics();
    gc.dispose();
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Conversions
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * see org.eclipse.jface.dialogs.DialogPage#convertHeightInCharsToPixels(int)
   */
  public int convertHeightInCharsToPixels(int chars) {
    return Dialog.convertHeightInCharsToPixels(fFontMetrics, chars);
  }

  /**
   * see org.eclipse.jface.dialogs.DialogPage#convertHorizontalDLUsToPixels(int)
   */
  public int convertHorizontalDLUsToPixels(int dlus) {
    return Dialog.convertHorizontalDLUsToPixels(fFontMetrics, dlus);
  }

  /**
   * see org.eclipse.jface.dialogs.DialogPage#convertVerticalDLUsToPixels(int)
   */
  public int convertVerticalDLUsToPixels(int dlus) {
    return Dialog.convertVerticalDLUsToPixels(fFontMetrics, dlus);
  }

  /**
   * see org.eclipse.jface.dialogs.DialogPage#convertWidthInCharsToPixels(int)
   */
  public int convertWidthInCharsToPixels(int chars) {
    return Dialog.convertWidthInCharsToPixels(fFontMetrics, chars);
  }
}
