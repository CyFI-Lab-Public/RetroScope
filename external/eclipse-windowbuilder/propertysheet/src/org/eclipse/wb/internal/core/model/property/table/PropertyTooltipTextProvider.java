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
package org.eclipse.wb.internal.core.model.property.table;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wb.internal.core.model.property.Property;

/**
 * Implementation of {@link PropertyTooltipProvider} for text.
 *
 * @author scheglov_ke
 * @coverage core.model.property.table
 */
public abstract class PropertyTooltipTextProvider extends PropertyTooltipProvider {
  ////////////////////////////////////////////////////////////////////////////
  //
  // PropertyTooltipProvider
  //
  ////////////////////////////////////////////////////////////////////////////
  @Override
  public Control createTooltipControl(Property property,
      Composite parent,
      int availableWidth,
      IPropertyTooltipSite site) {
    // prepare header and content
    String header = null;
    String content = null;
    try {
        // BEGIN ADT MODIFICATIONS
        // was: header = property.getTitle();
        header = property.getName();
        // END ADT MODIFICATIONS
      content = getText(property);
    } catch (Throwable e) {
    }
    if (header == null || content == null) {
      return null;
    }
    // create tooltip Control
    return HtmlTooltipHelper.createTooltipControl(parent, header, content, 8);
  }

  @Override
  public void show(Shell shell) {
    // do nothing, Shell will be displayed when Browser will complete rendering
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Text
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * @return the text to show as tooltip.
   */
  protected abstract String getText(Property property) throws Exception;
}
