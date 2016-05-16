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

import com.google.common.base.Joiner;

import org.eclipse.jface.window.Window;
import org.eclipse.wb.internal.core.DesignerPlugin;
import org.eclipse.wb.internal.core.model.ModelMessages;
import org.eclipse.wb.internal.core.model.property.Property;
import org.eclipse.wb.internal.core.utils.ui.dialogs.StringsDialog;

/**
 * {@link PropertyEditor} for array of {@link String}'s.
 *
 * @author scheglov_ke
 * @coverage core.model.property.editor
 */
public final class StringArrayPropertyEditor extends TextDialogPropertyEditor {
  ////////////////////////////////////////////////////////////////////////////
  //
  // Instance
  //
  ////////////////////////////////////////////////////////////////////////////
  public static final PropertyEditor INSTANCE = new StringArrayPropertyEditor();

  private StringArrayPropertyEditor() {
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Presentation
  //
  ////////////////////////////////////////////////////////////////////////////
  @Override
  protected String getText(Property property) throws Exception {
    String[] items = getItems(property);
    return "[" + Joiner.on(", ").join(items) + "]";
  }

  /**
   * @return the items specified in value of given {@link Property}.
   */
  private static String[] getItems(Property property) throws Exception {
    Object value = property.getValue();
    if (value instanceof String[]) {
      return (String[]) value;
    }
    // no items
    return new String[0];
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Editing
  //
  ////////////////////////////////////////////////////////////////////////////
  @Override
  protected void openDialog(Property property) throws Exception {
    StringsDialog itemsDialog =
        new StringsDialog(DesignerPlugin.getShell(),
            DesignerPlugin.getDefault(),
            property.getTitle(),
            ModelMessages.StringArrayPropertyEditor_itemsLabel,
            ModelMessages.StringArrayPropertyEditor_hint);
    itemsDialog.setItems(getItems(property));
    // open dialog
    if (itemsDialog.open() == Window.OK) {
      property.setValue(itemsDialog.getItems());
    }
  }
}
