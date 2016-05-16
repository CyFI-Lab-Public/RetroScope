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
package org.eclipse.wb.internal.core.editor.structure;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.wb.core.controls.CImageLabel;
import org.eclipse.wb.internal.core.utils.check.Assert;
import org.eclipse.wb.internal.core.utils.ui.GridDataFactory;
import org.eclipse.wb.internal.core.utils.ui.GridLayoutFactory;

/**
 * The site {@link Composite} for {@link IPage}.
 *
 * @author scheglov_ke
 * @coverage core.editor.structure
 */
public final class PageSiteComposite extends Composite {
  private final CImageLabel m_title;
  private final ToolBarManager m_toolBarManager;
  private final ToolBar m_toolBar;
  private IPage m_page;

  ////////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  ////////////////////////////////////////////////////////////////////////////
  public PageSiteComposite(Composite parent, int style) {
    super(parent, style);
    GridLayoutFactory.create(this).noMargins().spacingV(0).columns(2);
    // title
    {
      m_title = new CImageLabel(this, SWT.NONE);
      GridDataFactory.create(m_title).grabH().fill();
    }
    // toolbar
    {
      m_toolBar = new ToolBar(this, SWT.FLAT | SWT.RIGHT);
      GridDataFactory.create(m_toolBar).fill();
      m_toolBarManager = new ToolBarManager(m_toolBar);
    }
    // separator
    {
      Label separator = new Label(this, SWT.SEPARATOR | SWT.HORIZONTAL);
      GridDataFactory.create(separator).spanH(2).grabH().fillH();
    }
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Access
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Sets the {@link Image} for title;
   */
  public void setTitleImage(Image image) {
    m_title.setImage(image);
  }

  /**
   * Sets the text for title.
   */
  public void setTitleText(String title) {
    m_title.setText(title);
  }

  /**
   * Sets the {@link IPage} to display.
   */
  public void setPage(IPage page) {
    Assert.isNull(m_page);
    Assert.isNotNull(page);
    m_page = page;
    // create Control
    m_page.createControl(this);
    GridDataFactory.create(m_page.getControl()).spanH(2).grab().fill();
    // set toolbar
    m_page.setToolBar(m_toolBarManager);
  }

  // BEGIN ADT MODIFICATIONS
  public ToolBar getToolBar() {
      return m_toolBar;
  }
  // END ADT MODIFICATIONS

}
