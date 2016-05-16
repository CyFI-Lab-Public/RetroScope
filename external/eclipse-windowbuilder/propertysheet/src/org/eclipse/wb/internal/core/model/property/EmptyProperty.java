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
package org.eclipse.wb.internal.core.model.property;

import org.eclipse.wb.internal.core.model.property.editor.PropertyEditor;
import org.eclipse.wb.internal.core.model.property.editor.string.StringPropertyEditor;

/**
 * Empty {@link Property}, that has no title or value.
 * 
 * @author scheglov_ke
 * @coverage core.model.property
 */
public class EmptyProperty extends Property {
  ////////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  ////////////////////////////////////////////////////////////////////////////
  public EmptyProperty() {
    super(StringPropertyEditor.INSTANCE);
  }

  public EmptyProperty(PropertyEditor editor) {
    super(editor);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Property
  //
  ////////////////////////////////////////////////////////////////////////////
  @Override
  public String getTitle() {
    return null;
  }

  @Override
  public boolean isModified() throws Exception {
    return false;
  }

  @Override
  public Object getValue() throws Exception {
    return UNKNOWN_VALUE;
  }

  @Override
  public void setValue(Object value) throws Exception {
  }
}
