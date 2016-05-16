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
package org.eclipse.wb.internal.core.model.property.editor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.wb.internal.core.model.property.Property;
import org.eclipse.wb.internal.core.model.property.editor.presentation.PropertyEditorPresentation;
import org.eclipse.wb.internal.core.model.property.table.PropertyTable;

/**
 * Abstract editor for {@link Property}.
 *
 * @author scheglov_ke
 * @coverage core.model.property.editor
 */
public abstract class PropertyEditor {
  ////////////////////////////////////////////////////////////////////////////
  //
  // Presentation
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * @return the instance of {@link PropertyEditorPresentation}.
   */
  public PropertyEditorPresentation getPresentation() {
    return null;
  }

  /**
   * Paints given {@link Property} given rectangle <code>(x, y, width, height)</code> of {@link GC}.
   */
  public abstract void paint(Property property, GC gc, int x, int y, int width, int height)
      throws Exception;

  ////////////////////////////////////////////////////////////////////////////
  //
  // Editing
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Activates editor for given {@link Property} at given place of {@link Composite}. Activation
   * happens when user selects property in {@link PropertyTable}. {@link PropertyEditor} should
   * create here any {@link Control}'s required to edit {@link Property}.
   *
   * If any exception happens, {@link PropertyEditor} will be deactivated.
   *
   * @param location
   *          the mouse location, if editor is activated using mouse click, or <code>null</code> if
   *          it is activated using keyboard.
   *
   * @return <code>true</code> if editor should be remembered as active for future
   *         {@link #setBounds(Rectangle)} and {@link #deactivate(boolean)} invocation. Some editors
   *         need such local activation (for example for String), some - not (for boolean).
   */
  public boolean activate(PropertyTable propertyTable, Property property, Point location)
      throws Exception {
    return false;
  }

  /**
   * Sets the new bounds for editor's control.
   */
  public void setBounds(Rectangle bounds) {
  }

  /**
   * Deactivates editor for current {@link Property}. {@link PropertyEditor} should dispose any
   * {@link Control}'s created before in {@link #activate(PropertyTable, Property, Point)}.
   *
   * If any exception happened during activation, editor still should be able to deactivate
   * correctly.
   *
   * @param save
   *          is <code>true</code> if property should save value to {@link Property}.
   */
  public void deactivate(PropertyTable propertyTable, Property property, boolean save) {
  }

  /**
   * Handles double click on {@link Property} value in {@link PropertyTable}.
   *
   * @param location
   *          the mouse location, relative to editor
   */
  public void doubleClick(Property property, Point location) throws Exception {
  }

  /**
   * Handles {@link SWT#KeyDown} event in {@link PropertyTable}.
   */
  public void keyDown(PropertyTable propertyTable, Property property, KeyEvent event)
      throws Exception {
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // IAdaptable
  //
  ////////////////////////////////////////////////////////////////////////////
  public <T> T getAdapter(Class<T> adapter) {
    return null;
  }
}
