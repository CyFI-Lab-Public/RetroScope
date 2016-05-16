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

/**
 * Extension for {@link PropertyEditor} that can be used to convert {@link Object} value into Java
 * source.
 * 
 * @author scheglov_ke
 * @coverage core.model.property.editor
 */
public interface IValueSourcePropertyEditor {
  /**
   * @return the Java source for given value.
   */
  String getValueSource(Object value) throws Exception;
}
