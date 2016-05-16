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
package org.eclipse.wb.internal.core.utils.ui;

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Layout;

/**
 * GridLayoutFactory provides a convenient shorthand for creating and initializing GridLayout.
 *
 * @author scheglov_ke
 */
public final class GridLayoutFactory {
  private final GridLayout m_layout;

  ////////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  ////////////////////////////////////////////////////////////////////////////
  private GridLayoutFactory(Composite composite, GridLayout layout) {
    m_layout = layout;
    composite.setLayout(m_layout);
  }

  public static GridLayoutFactory create(Composite composite) {
    return new GridLayoutFactory(composite, new GridLayout());
  }

  public static GridLayoutFactory modify(Composite composite) {
    Layout layout = composite.getLayout();
    if (layout instanceof GridLayout) {
      return new GridLayoutFactory(composite, (GridLayout) layout);
    }
    return create(composite);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Access
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Sets number of columns in {@link GridLayout}.
   */
  public GridLayoutFactory columns(int numColumns) {
    m_layout.numColumns = numColumns;
    return this;
  }

  /**
   * Specifies whether all columns in the layout will be forced to have the same width.
   */
  public GridLayoutFactory equalColumns() {
    m_layout.makeColumnsEqualWidth = true;
    return this;
  }

  /**
   * Sets the horizontal margins.
   */
  public GridLayoutFactory marginsH(int margins) {
    m_layout.marginWidth = margins;
    return this;
  }

  /**
   * Sets the vertical margins.
   */
  public GridLayoutFactory marginsV(int margins) {
    m_layout.marginHeight = margins;
    return this;
  }

  /**
   * Sets the horizontal/vertical margins.
   */
  public GridLayoutFactory margins(int margins) {
    m_layout.marginWidth = m_layout.marginHeight = margins;
    return this;
  }

  /**
   * Sets zero horizontal and vertical margins.
   */
  public GridLayoutFactory noMargins() {
    m_layout.marginWidth = m_layout.marginHeight = 0;
    return this;
  }

  /**
   * Sets zero horizontal and vertical spacing.
   */
  public GridLayoutFactory noSpacing() {
    m_layout.horizontalSpacing = m_layout.verticalSpacing = 0;
    return this;
  }

  /**
   * Sets horizontal spacing.
   */
  public GridLayoutFactory spacingH(int spacing) {
    m_layout.horizontalSpacing = spacing;
    return this;
  }

  /**
   * Sets vertical spacing.
   */
  public GridLayoutFactory spacingV(int spacing) {
    m_layout.verticalSpacing = spacing;
    return this;
  }
}
