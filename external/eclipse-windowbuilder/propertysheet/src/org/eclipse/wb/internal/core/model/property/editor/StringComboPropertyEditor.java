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

import org.eclipse.wb.core.controls.CCombo3;
import org.eclipse.wb.internal.core.model.property.Property;

/**
 * The {@link PropertyEditor} for selecting single {@link String} value from given array.
 * 
 * @author scheglov_ke
 * @coverage core.model.property.editor
 */
public class StringComboPropertyEditor extends AbstractComboPropertyEditor {
  private final String[] m_items;

  ////////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  ////////////////////////////////////////////////////////////////////////////
  public StringComboPropertyEditor(String... items) {
    m_items = items;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Presentation
  //
  ////////////////////////////////////////////////////////////////////////////
  @Override
  protected String getText(Property property) throws Exception {
    return (String) property.getValue();
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // AbstractComboPropertyEditor
  //
  ////////////////////////////////////////////////////////////////////////////
  @Override
  protected void addItems(Property property, CCombo3 combo) throws Exception {
    for (String item : m_items) {
      combo.add(item);
    }
  }

  @Override
  protected void selectItem(Property property, CCombo3 combo) throws Exception {
    combo.setText(getText(property));
  }

  @Override
  protected void toPropertyEx(Property property, CCombo3 combo, int index) throws Exception {
    property.setValue(m_items[index]);
  }
}