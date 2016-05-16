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

import org.eclipse.swt.graphics.GC;
import org.eclipse.wb.internal.core.model.property.Property;
import org.eclipse.wb.internal.core.model.property.table.PropertyTable;
import org.eclipse.wb.internal.core.model.property.table.PropertyTooltipProvider;
import org.eclipse.wb.internal.core.utils.ui.DrawUtils;

/**
 * Abstract {@link PropertyEditor} for displaying text as {@link Property} value in
 * {@link PropertyTable}.
 *
 * @author scheglov_ke
 * @coverage core.model.property.editor
 */
public abstract class TextDisplayPropertyEditor extends PropertyEditor {
  ////////////////////////////////////////////////////////////////////////////
  //
  // Presentation
  //
  ////////////////////////////////////////////////////////////////////////////
  @Override
  public void paint(Property property, GC gc, int x, int y, int width, int height) throws Exception {
    String text = getText(property);
    if (text != null) {
      DrawUtils.drawStringCV(gc, text, x, y, width, height);
    }
  }

  /**
   * @return the text for displaying value of given {@link Property} or <code>null</code> if value
   *         of {@link Property} is unknown.
   */
  protected abstract String getText(Property property) throws Exception;

  ////////////////////////////////////////////////////////////////////////////
  //
  // IAdaptable
  //
  ////////////////////////////////////////////////////////////////////////////
  @Override
  public <T> T getAdapter(Class<T> adapter) {
    // tooltip for value text
    if (adapter == PropertyTooltipProvider.class) {
      return adapter.cast(createPropertyTooltipProvider());
    }
    return super.getAdapter(adapter);
  }

  /**
   * @return the {@link PropertyTooltipProvider} to display value tooltip.
   */
  protected PropertyTooltipProvider createPropertyTooltipProvider() {
    return null;
  }
}
