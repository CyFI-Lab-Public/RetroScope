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
package org.eclipse.wb.internal.core.model.property.editor.string;

import org.eclipse.wb.internal.core.model.property.Property;
import org.eclipse.wb.internal.core.model.property.editor.AbstractTextPropertyEditor;
import org.eclipse.wb.internal.core.model.property.editor.PropertyEditor;
import org.eclipse.wb.internal.core.model.property.editor.presentation.ButtonPropertyEditorPresentation;
import org.eclipse.wb.internal.core.model.property.editor.presentation.PropertyEditorPresentation;
import org.eclipse.wb.internal.core.model.property.table.PropertyTable;

import org.eclipse.jface.window.Window;

/**
 * The {@link PropertyEditor} for {@link String}.
 * 
 * @author scheglov_ke
 * @coverage core.model.property.editor
 */
public class StringPropertyEditor extends AbstractTextPropertyEditor {
  ////////////////////////////////////////////////////////////////////////////
  //
  // Instance
  //
  ////////////////////////////////////////////////////////////////////////////
  public static final PropertyEditor INSTANCE = new StringPropertyEditor();

  private StringPropertyEditor() {
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Presentation
  //
  ////////////////////////////////////////////////////////////////////////////
  private final PropertyEditorPresentation m_presentation = new ButtonPropertyEditorPresentation() {
    @Override
    protected void onClick(PropertyTable propertyTable, Property property) throws Exception {
      openDialog(propertyTable, property);
    }
  };

  @Override
  public PropertyEditorPresentation getPresentation() {
    return m_presentation;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Presentation
  //
  ////////////////////////////////////////////////////////////////////////////
  @Override
  public String getText(Property property) throws Exception {
    Object value = property.getValue();
    if (value instanceof String) {
      return (String) value;
    }
    return null;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Editing
  //
  ////////////////////////////////////////////////////////////////////////////
  @Override
  protected String getEditorText(Property property) throws Exception {
    return getText(property);
  }

  @Override
  protected boolean setEditorText(Property property, String text) throws Exception {
    property.setValue(text);
    return true;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Editing in dialog
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Opens editing dialog.
   */
  private void openDialog(PropertyTable propertyTable, Property property) throws Exception {
    StringPropertyDialog dialog = new StringPropertyDialog(propertyTable.getShell(), property);
    if (dialog.open() == Window.OK) {
    }
  }
}
