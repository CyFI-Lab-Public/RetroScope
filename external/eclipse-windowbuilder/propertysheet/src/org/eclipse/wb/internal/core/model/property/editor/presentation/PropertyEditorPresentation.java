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
package org.eclipse.wb.internal.core.model.property.editor.presentation;

import org.eclipse.wb.internal.core.model.property.Property;
import org.eclipse.wb.internal.core.model.property.editor.PropertyEditor;
import org.eclipse.wb.internal.core.model.property.table.PropertyTable;

/**
 * Implementations of {@link PropertyEditorPresentation} are used to show some presentation for
 * visible, but not activated yet {@link PropertyEditor}.
 * 
 * @author scheglov_ke
 * @coverage core.model.property.editor
 */
public abstract class PropertyEditorPresentation {
  /**
   * Shows presentation for given {@link Property}.
   * 
   * @return the width that this presentation occupies on the right of given rectangle.
   */
  public abstract int show(PropertyTable propertyTable,
      Property property,
      int x,
      int y,
      int width,
      int height);

  /**
   * Hides presentation.
   */
  public abstract void hide(PropertyTable propertyTable, Property property);
}
