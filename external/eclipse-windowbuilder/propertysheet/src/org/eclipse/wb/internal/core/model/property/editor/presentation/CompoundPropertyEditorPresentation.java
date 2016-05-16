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

import com.google.common.collect.Lists;

import org.eclipse.wb.internal.core.model.property.Property;
import org.eclipse.wb.internal.core.model.property.table.PropertyTable;

import java.util.List;

/**
 * Implementation of {@link PropertyEditorPresentation} that contains zero or more other
 * {@link PropertyEditorPresentation}'s.
 * 
 * @author scheglov_ke
 * @coverage core.model.property.editor
 */
public class CompoundPropertyEditorPresentation extends PropertyEditorPresentation {
  private final List<PropertyEditorPresentation> m_presentations = Lists.newArrayList();

  ////////////////////////////////////////////////////////////////////////////
  //
  // Access
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Adds child {@link PropertyEditorPresentation}.<br>
   * Child {@link PropertyEditorPresentation}'s are displayed from right to left.
   */
  public void add(PropertyEditorPresentation presentation) {
    m_presentations.add(presentation);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PropertyEditorPresentation
  //
  ////////////////////////////////////////////////////////////////////////////
  @Override
  public int show(PropertyTable propertyTable,
      Property property,
      int x,
      int y,
      int width,
      int height) {
    int sumWidth = 0;
    for (PropertyEditorPresentation presentation : m_presentations) {
      int presentationWidth = presentation.show(propertyTable, property, x, y, width, height);
      sumWidth += presentationWidth;
      width -= presentationWidth;
    }
    return sumWidth;
  }

  @Override
  public void hide(PropertyTable propertyTable, Property property) {
    for (PropertyEditorPresentation presentation : m_presentations) {
      presentation.hide(propertyTable, property);
    }
  }
}
