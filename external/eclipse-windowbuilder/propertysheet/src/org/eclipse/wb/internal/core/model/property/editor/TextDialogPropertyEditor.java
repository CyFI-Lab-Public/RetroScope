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

import org.eclipse.wb.internal.core.model.property.Property;
import org.eclipse.wb.internal.core.model.property.editor.presentation.ButtonPropertyEditorPresentation;
import org.eclipse.wb.internal.core.model.property.editor.presentation.PropertyEditorPresentation;
import org.eclipse.wb.internal.core.model.property.table.PropertyTable;

import org.eclipse.swt.graphics.Point;

/**
 * Abstract {@link PropertyEditor} that displays text and button to open dialog.
 * 
 * @author scheglov_ke
 * @coverage core.model.property.editor
 */
public abstract class TextDialogPropertyEditor extends TextDisplayPropertyEditor {
  ////////////////////////////////////////////////////////////////////////////
  //
  // Presentation
  //
  ////////////////////////////////////////////////////////////////////////////
  private final PropertyEditorPresentation m_presentation = new ButtonPropertyEditorPresentation() {
    @Override
    protected void onClick(PropertyTable propertyTable, Property property) throws Exception {
      openDialog(property);
    }
  };

  @Override
  public final PropertyEditorPresentation getPresentation() {
    return m_presentation;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Editing
  //
  ////////////////////////////////////////////////////////////////////////////
  @Override
  public boolean activate(PropertyTable propertyTable, Property property, Point location)
      throws Exception {
    // activate using keyboard
    if (location == null) {
      openDialog(property);
    }
    // don't activate
    return false;
  }

  /**
   * Opens editing dialog.
   */
  protected abstract void openDialog(Property property) throws Exception;
}