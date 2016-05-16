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

import org.eclipse.wb.core.controls.CCombo3;
import org.eclipse.wb.internal.core.model.property.Property;
import org.eclipse.wb.internal.core.model.property.table.PropertyTable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

/**
 * The {@link PropertyEditor} for selecting single value using {@link CCombo3}.
 * 
 * @author scheglov_ke
 * @coverage core.model.property.editor
 */
public abstract class AbstractComboPropertyEditor extends TextDisplayPropertyEditor {
  ////////////////////////////////////////////////////////////////////////////
  //
  // Editing
  //
  ////////////////////////////////////////////////////////////////////////////
  private CCombo3 m_combo;
  private boolean m_doDropDown;

  @Override
  public boolean activate(final PropertyTable propertyTable, final Property property, Point location)
      throws Exception {
    // create combo
    {
      m_combo = new CCombo3(propertyTable, SWT.NONE);
      m_doDropDown = true;
      // add items
      addItems(property, m_combo);
      // select item
      selectItem(property, m_combo);
    }
    // add listeners
    m_combo.addFocusListener(new FocusAdapter() {
      @Override
      public void focusLost(FocusEvent e) {
        propertyTable.deactivateEditor(true);
      }
    });
    m_combo.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        int index = m_combo.getSelectionIndex();
        toProperty(propertyTable, property, index);
      }
    });
    m_combo.addListener(SWT.KeyDown, new Listener() {
      public void handleEvent(Event event) {
        switch (event.keyCode) {
          case SWT.ESC :
            propertyTable.deactivateEditor(false);
            break;
          case SWT.DEL :
            try {
              property.setValue(Property.UNKNOWN_VALUE);
              event.doit = false;
              selectItem(property, m_combo);
            } catch (Throwable e) {
              propertyTable.handleException(e);
              propertyTable.deactivateEditor(false);
            }
            m_combo.doDropDown(false);
            break;
        }
      }
    });
    m_combo.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseDoubleClick(MouseEvent e) {
        int index = (m_combo.getSelectionIndex() + 1) % m_combo.getItemCount();
        toProperty(propertyTable, property, index);
      }
    });
    // keep editor active
    return true;
  }

  @Override
  public final void setBounds(Rectangle bounds) {
    m_combo.setBounds(bounds);
    // editor created without bounds, so activate it after first setBounds()
    if (m_doDropDown) {
      m_doDropDown = false;
      m_combo.setFocus();
      m_combo.doDropDown(true);
      m_combo.startDrag();
    }
  }

  @Override
  public final void deactivate(PropertyTable propertyTable, Property property, boolean save) {
    if (m_combo != null) {
      m_combo.dispose();
      m_combo = null;
    }
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Abstract methods
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Adds items to given {@link CCombo3}.
   */
  protected abstract void addItems(Property property, CCombo3 combo) throws Exception;

  /**
   * Selects current item in given {@link CCombo3}.
   */
  protected abstract void selectItem(Property property, CCombo3 combo) throws Exception;

  /**
   * Transfers data from widget to {@link Property}.
   */
  protected abstract void toPropertyEx(Property property, CCombo3 combo, int index)
      throws Exception;

  /**
   * Transfers data from widget to {@link Property}.
   */
  private void toProperty(PropertyTable propertyTable, Property property, int index) {
    try {
      toPropertyEx(property, m_combo, index);
    } catch (Throwable e) {
      propertyTable.handleException(e);
    }
    propertyTable.deactivateEditor(false);
  }
}
