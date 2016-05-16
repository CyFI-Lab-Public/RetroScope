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
package org.eclipse.wb.internal.core.model.property.editor.presentation;

import org.eclipse.wb.internal.core.DesignerPlugin;
import org.eclipse.wb.internal.core.EnvironmentUtils;
import org.eclipse.wb.internal.core.model.property.Property;
import org.eclipse.wb.internal.core.model.property.table.PropertyTable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;

/**
 * Implementation of {@link PropertyEditorPresentation} for displaying {@link Button}.
 * 
 * @author scheglov_ke
 * @author mitin_aa
 * @coverage core.model.property.editor
 */
public abstract class ButtonPropertyEditorPresentation extends PropertyEditorPresentation {
  private final int m_style;
  private final ButtonPropertyEditorPresentationImpl m_impl;

  ////////////////////////////////////////////////////////////////////////////
  //
  // Constructors
  //
  ////////////////////////////////////////////////////////////////////////////
  public ButtonPropertyEditorPresentation() {
    this(SWT.NONE);
  }

  public ButtonPropertyEditorPresentation(int style) {
    m_style = style;
    m_impl =
        EnvironmentUtils.IS_MAC
            ? new ButtonPropertyEditorPresentationImplMac(this)
            : new ButtonPropertyEditorPresentationImpl(this);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Access
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Sets "selection" property of {@link Button}.
   */
  public final void setSelection(PropertyTable propertyTable, Property property, boolean selected) {
    m_impl.setSelection(propertyTable, property, selected);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PropertyEditorPresentation
  //
  ////////////////////////////////////////////////////////////////////////////
  @Override
  public final int show(final PropertyTable propertyTable,
      final Property property,
      final int x,
      final int y,
      final int width,
      final int height) {
    return m_impl.show(propertyTable, property, x, y, width, height);
  }

  @Override
  public final void hide(PropertyTable propertyTable, Property property) {
    m_impl.hide(propertyTable, property);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Access
  //
  ////////////////////////////////////////////////////////////////////////////
  final int getStyle() {
    return m_style;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Implementation
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * @return the {@link Image} to display on {@link Button}.
   */
  protected Image getImage() {
    return DesignerPlugin.getImage("properties/dots.gif");
  }

  /**
   * @return the tooltip text to display for {@link Button}.
   */
  protected String getTooltip() {
    return null;
  }

  /**
   * Handles click on {@link Button}.
   */
  protected abstract void onClick(PropertyTable propertyTable, Property property) throws Exception;

  // Temporary workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=388574
  public static boolean isInWorkaround;
  public void click(PropertyTable propertyTable, Property property) throws Exception {
    try {
      isInWorkaround = true;
      onClick(propertyTable, property);
    } finally {
        isInWorkaround = false;
    }
  }
}
