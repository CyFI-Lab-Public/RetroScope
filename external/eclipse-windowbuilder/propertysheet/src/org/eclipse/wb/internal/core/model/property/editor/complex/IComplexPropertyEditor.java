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
package org.eclipse.wb.internal.core.model.property.editor.complex;

import org.eclipse.wb.internal.core.model.property.Property;
import org.eclipse.wb.internal.core.model.property.editor.PropertyEditor;

/**
 * Extension for {@link PropertyEditor} that specifies that it has sub-properties.
 * 
 * @author scheglov_ke
 * @coverage core.model.property.editor
 */
public interface IComplexPropertyEditor {
  /**
   * @return sub-properties of given complex property.
   */
  Property[] getProperties(Property property) throws Exception;
}