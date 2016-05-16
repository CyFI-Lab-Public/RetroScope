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
package org.eclipse.wb.internal.core.utils.ui.dialogs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.wb.internal.core.utils.ui.GridDataFactory;
import org.eclipse.wb.internal.core.utils.ui.GridLayoutFactory;

/**
 * The dialog for editing multiline text.
 *
 * @author scheglov_ke
 * @coverage core.ui
 */
public class TextDialog extends ResizableDialog {
  private final String m_titleText;
  private final String m_headerText;
  private final String m_footerText;

  ////////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  ////////////////////////////////////////////////////////////////////////////
  public TextDialog(Shell parentShell,
      AbstractUIPlugin plugin,
      String titleText,
      String headerText,
      String footerText) {
    super(parentShell, plugin);
    m_titleText = titleText;
    m_headerText = headerText;
    m_footerText = footerText;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Text
  //
  ////////////////////////////////////////////////////////////////////////////
  private String m_text;

  /**
   * Sets the text to edit.
   */
  public final void setText(String text) {
    m_text = text;
  }

  /**
   * @return the edited text.
   */
  public final String getText() {
    return m_text;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // GUI
  //
  ////////////////////////////////////////////////////////////////////////////
  protected Text m_textWidget;

  @Override
  protected Control createDialogArea(Composite parent) {
    Composite area = (Composite) super.createDialogArea(parent);
    GridLayoutFactory.create(area);
    // header
    new Label(area, SWT.NONE).setText(m_headerText);
    // Text widget
    {
      m_textWidget = new Text(area, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
      GridDataFactory.create(m_textWidget).grab().fill().hintVC(10);
      m_textWidget.setText(m_text);
      // handle Ctrl+Enter as OK
      m_textWidget.addKeyListener(new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
          if (e.stateMask == SWT.CTRL && e.keyCode == SWT.CR) {
            okPressed();
          }
        }
      });
    }
    // footer
    new Label(area, SWT.NONE).setText(m_footerText);
    //
    return area;
  }

  @Override
  protected void configureShell(Shell newShell) {
    super.configureShell(newShell);
    newShell.setText(m_titleText);
  }

  @Override
  protected void okPressed() {
    m_text = m_textWidget.getText();
    super.okPressed();
  }
}
