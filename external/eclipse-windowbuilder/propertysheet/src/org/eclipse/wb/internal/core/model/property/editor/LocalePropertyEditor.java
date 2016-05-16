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

import java.util.Locale;

/**
 * {@link PropertyEditor} for {@link Locale}.
 *
 * @author sablin_aa
 * @coverage core.model.property.editor
 */
public final class LocalePropertyEditor extends TextDialogPropertyEditor {
  ////////////////////////////////////////////////////////////////////////////
  //
  // Instance
  //
  ////////////////////////////////////////////////////////////////////////////
  public static final PropertyEditor INSTANCE = new LocalePropertyEditor();

  private LocalePropertyEditor() {
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Presentation
  //
  ////////////////////////////////////////////////////////////////////////////
  @Override
  protected String getText(Property property) throws Exception {
    Object value = property.getValue();
    if (value instanceof Locale) {
      return ((Locale) value).getDisplayName();
    }
    // unknown value
    return null;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Editing
  //
  ////////////////////////////////////////////////////////////////////////////
  @Override
  protected void openDialog(Property property) throws Exception {
    Object value = property.getValue();
//    ChooseLocaleDialog localeDialog;
//    if (value instanceof Locale) {
//      localeDialog = new ChooseLocaleDialog(DesignerPlugin.getShell(), (Locale) value);
//    } else {
//      localeDialog = new ChooseLocaleDialog(DesignerPlugin.getShell(), null);
//    }
//    // open dialog
//    if (localeDialog.open() == Window.OK) {
//      property.setValue(localeDialog.getSelectedLocale().getLocale());
//    }
    System.out.println("TODO: Custom locale chooser here");
  }
}