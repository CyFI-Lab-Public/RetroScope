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
package org.eclipse.wb.internal.core.model.property.editor.string;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wb.internal.core.DesignerPlugin;
import org.eclipse.wb.internal.core.model.ModelMessages;
import org.eclipse.wb.internal.core.model.property.Property;
import org.eclipse.wb.internal.core.utils.execution.ExecutionUtils;
import org.eclipse.wb.internal.core.utils.execution.RunnableEx;
import org.eclipse.wb.internal.core.utils.ui.GridDataFactory;
import org.eclipse.wb.internal.core.utils.ui.dialogs.ResizableDialog;

/**
 * {@link Dialog} for editing value in {@link StringPropertyEditor}.
 *
 * @author scheglov_ke
 * @coverage core.model.property.editor
 */
public class StringPropertyDialog extends ResizableDialog {
    // NOTE: In WindowBuilder this class had a lot of support for
    // editing Java strings, dealing with automatic localization
    // etc. This will need to be done differently in ADT (and had hooks
    // into a bunch of other parts of WindowBuilder we're not including)
    // so this was all stripped down to a plain String editor.

  ////////////////////////////////////////////////////////////////////////////
  //
  // Final fields
  //
  ////////////////////////////////////////////////////////////////////////////
  private final Property m_property;

  ////////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  ////////////////////////////////////////////////////////////////////////////
  public StringPropertyDialog(Shell parentShell, Property property) throws Exception {
    super(parentShell, DesignerPlugin.getDefault());
    m_property = property;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // GUI
  //
  ////////////////////////////////////////////////////////////////////////////
  private Text m_valueText;

  @Override
  public void create() {
    super.create();
    m_valueText.selectAll();
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    Composite area = (Composite) super.createDialogArea(parent);
    // value
    {
      // BEGIN ADT MODIFICATIONS
      if (isMultiLine()) {
      // END ADT MODIFICATIONS
        m_valueText = new Text(area, SWT.BORDER | SWT.MULTI | SWT.WRAP);
        GridDataFactory.create(m_valueText).grab().hintC(80, 8).fill();
      // BEGIN ADT MODIFICATIONS
      } else {
        m_valueText = new Text(area, SWT.BORDER | SWT.SINGLE);
        GridDataFactory.create(m_valueText).grab().hintC(50, 1).fill();
      }
      // END ADT MODIFICATIONS
      // initial value
      ExecutionUtils.runLog(new RunnableEx() {
        @Override
        public void run() throws Exception {
          Object value = m_property.getValue();
          if (value instanceof String) {
            m_valueText.setText((String) value);
          }
        }
      });
      // handle Ctrl+Enter as OK
      m_valueText.addKeyListener(new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
          if (e.stateMask == SWT.CTRL && e.keyCode == SWT.CR) {
            okPressed();
          }
        }
      });
    }

    return area;
  }

  // BEGIN ADT MODIFICATIONS
  protected boolean isMultiLine() {
      return true;
  }
  // END ADT MODIFICATIONS

  ////////////////////////////////////////////////////////////////////////////
  //
  // Shell
  //
  ////////////////////////////////////////////////////////////////////////////
  @Override
  protected void configureShell(Shell newShell) {
    super.configureShell(newShell);
    newShell.setText(ModelMessages.StringPropertyDialog_title);
  }

  @Override
  protected void okPressed() {
    final String value = m_valueText.getText();
    ExecutionUtils.runLog(new RunnableEx() {
        @Override
        public void run() throws Exception {
          m_property.setValue(value);
        }
    });
    // close dialog
    super.okPressed();
  }
}
