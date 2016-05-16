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

import org.eclipse.wb.internal.core.DesignerPlugin;
import org.eclipse.wb.internal.core.model.ModelMessages;
import org.eclipse.wb.internal.core.model.property.Property;
import org.eclipse.wb.internal.core.utils.ui.UiUtils;

import java.text.MessageFormat;

/**
 * The {@link PropertyEditor} for {@link Double}.
 * 
 * @author scheglov_ke
 * @coverage core.model.property.editor
 */
public final class DoubleObjectPropertyEditor extends AbstractTextPropertyEditor {
  ////////////////////////////////////////////////////////////////////////////
  //
  // Instance
  //
  ////////////////////////////////////////////////////////////////////////////
  public static final DoubleObjectPropertyEditor INSTANCE = new DoubleObjectPropertyEditor();

  private DoubleObjectPropertyEditor() {
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Presentation
  //
  ////////////////////////////////////////////////////////////////////////////
  @Override
  public String getText(Property property) throws Exception {
    Object value = property.getValue();
    if (value == null) {
      return "null";
    }
    if (value instanceof Double) {
      return value.toString();
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
    text = text.trim();
    // check for delete
    if (text.length() == 0) {
      property.setValue(Property.UNKNOWN_VALUE);
      return true;
    }
    // check for "null"
    if (text.equals("null")) {
      property.setValue(null);
      return true;
    }
    // prepare value
    Double value;
    try {
      value = Double.valueOf(text);
    } catch (Throwable e) {
      UiUtils.openWarning(
          DesignerPlugin.getShell(),
          property.getTitle(),
          MessageFormat.format(ModelMessages.DoubleObjectPropertyEditor_notValidDouble, text));
      return false;
    }
    // modify property
    property.setValue(value);
    return true;
  }
}
