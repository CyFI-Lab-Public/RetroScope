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

/**
 * Extension of {@link PropertyEditor} that can set value using its text presentation.
 * 
 * @author scheglov_ke
 * @coverage core.model.property.editor
 */
public interface ITextValuePropertyEditor {
  /**
   * Sets value that corresponds given text.
   */
  void setText(Property property, String text) throws Exception;
}
