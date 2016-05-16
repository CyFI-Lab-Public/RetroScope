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
 * The {@link PropertyEditor} for selecting single string from given set.
 *
 * @author sablin_aa
 * @coverage core.model.property.editor
 */
public final class StringListPropertyEditor extends AbstractListPropertyEditor {
  private boolean m_ignoreCase;
  private String[] m_strings;

  ////////////////////////////////////////////////////////////////////////////
  //
  // Combo
  //
  ////////////////////////////////////////////////////////////////////////////
  @Override
  protected void toPropertyEx_simpleProperty(Property property, CCombo3 combo, int index)
      throws Exception {
    property.setValue(m_strings[index]);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Access to list items
  //
  ////////////////////////////////////////////////////////////////////////////
  @Override
  protected int getCount() {
    return m_strings.length;
  }

  @Override
  protected int getValueIndex(Object value) {
    if (value instanceof String) {
      String string = (String) value;
      for (int i = 0; i < getCount(); i++) {
        if (m_ignoreCase) {
          if (string.equalsIgnoreCase(m_strings[i])) {
            return i;
          }
        } else {
          if (string.equals(m_strings[i])) {
            return i;
          }
        }
      }
    }
    return -1;
  }

  @Override
  protected String getTitle(int index) {
    return m_strings[index];
  }

  @Override
  protected String getExpression(int index) throws Exception {
    //return StringConverter.INSTANCE.toJavaSource(null, m_strings[index]);
      // HACK!!
      System.out.println("HACK!");
      return m_strings[index];
  }
//
//  ////////////////////////////////////////////////////////////////////////////
//  //
//  // IConfigurablePropertyObject
//  //
//  ////////////////////////////////////////////////////////////////////////////
//  public void configure(EditorState state, Map<String, Object> parameters) throws Exception {
//    m_strings = getParameterAsArray(parameters, "strings");
//    m_ignoreCase = "true".equals(parameters.get("ignoreCase"));
//  }
//
//  /**
//   * Configures this editor externally.
//   */
//  public void configure(String[] strings) {
//    m_strings = strings;
//  }
}
