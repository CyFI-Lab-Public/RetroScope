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

import java.beans.PropertyDescriptor;

/**
 * {@link PropertyEditorProvider} that creates editors based on {@link PropertyDescriptor}
 * attributes, such as "enumerationValues".
 * 
 * @author scheglov_ke
 * @coverage core.model.property.editor
 */
public final class PropertyDescriptorEditorProvider extends PropertyEditorProvider {
  ////////////////////////////////////////////////////////////////////////////
  //
  // PropertyEditorProvider
  //
  ////////////////////////////////////////////////////////////////////////////
  @Override
  public PropertyEditor getEditorForPropertyDescriptor(PropertyDescriptor descriptor)
      throws Exception {
    {
      Object attributeValue = descriptor.getValue("enumerationValues");
      if (isEnumerationProperty(descriptor)) {
        return new EnumerationValuesPropertyEditor(attributeValue);
      }
    }
    return null;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Utils
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * @return <code>true</code> if given {@link PropertyDescriptor} has attribute "enumerationValues"
   *         with valid value structure.
   */
  private static boolean isEnumerationProperty(PropertyDescriptor descriptor) {
    Object attributeValue = descriptor.getValue("enumerationValues");
    // should be Object[]
    if (!(attributeValue instanceof Object[])) {
      return false;
    }
    Object[] enumElements = (Object[]) attributeValue;
    // should be multiple 3
    if (enumElements.length % 3 != 0) {
      return false;
    }
    // elements should be sequence of [String,Object,String]
    for (int i = 0; i < enumElements.length; i++) {
      Object element = enumElements[i];
      if (i % 3 == 0 && !(element instanceof String)) {
        return false;
      }
      if (i % 3 == 2 && !(element instanceof String)) {
        return false;
      }
    }
    // OK
    return true;
  }
}
