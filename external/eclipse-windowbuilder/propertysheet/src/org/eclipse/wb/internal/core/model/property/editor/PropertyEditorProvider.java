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
 * Provider for creating {@link PropertyEditor}'s.
 * 
 * @author lobas_av
 * @coverage core.model.property.editor
 */
public class PropertyEditorProvider {
  /**
   * @return the {@link PropertyEditor} for given property type or <code>null</code>.
   */
  public PropertyEditor getEditorForType(Class<?> propertyType) throws Exception {
    return null;
  }

  /**
   * @return the {@link PropertyEditor} for given {@link java.beans.PropertyEditor} editor type or
   *         <code>null</code>.
   */
  public PropertyEditor getEditorForEditorType(Class<?> editorType) throws Exception {
    return null;
  }

  /**
   * @return the {@link PropertyEditor} for given {@link PropertyDescriptor} or <code>null</code>.
   */
  public PropertyEditor getEditorForPropertyDescriptor(PropertyDescriptor descriptor)
      throws Exception {
    return null;
  }
}