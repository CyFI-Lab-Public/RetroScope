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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wb.internal.core.EnvironmentUtils;
import org.eclipse.wb.internal.core.model.property.Property;
import org.eclipse.wb.internal.core.utils.ui.GridLayoutFactory;

/**
 * Helper class for displaying tooltips.
 *
 * @author scheglov_ke
 * @coverage core.model.property.table
 */
class PropertyTableTooltipHelper implements IPropertyTooltipSite {
  private final PropertyTable m_table;
  private Shell m_tooltip;

  ////////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  ////////////////////////////////////////////////////////////////////////////
  public PropertyTableTooltipHelper(PropertyTable table) {
    m_table = table;
    m_table.addListener(SWT.MouseHover, new Listener() {
      @Override
    public void handleEvent(Event event) {
        if (event.stateMask == 0) {
          showTooltip();
        }
      }
    });
    m_table.addListener(SWT.MouseExit, new Listener() {
      @Override
    public void handleEvent(Event event) {
        // check, may be cursor is now on tooltip, so ignore this MouseExit
        {
          Control control = Display.getCurrent().getCursorControl();
          while (control != null) {
            if (control == m_tooltip) {
              return;
            }
            control = control.getParent();
          }
        }
        // no, we should hide tooltip
        hideTooltip();
      }
    });
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Access
  //
  ////////////////////////////////////////////////////////////////////////////
  private Property m_property;
  private boolean m_onTitle;
  private boolean m_onValue;
  private int m_beginX;
  private int m_endX;
  private int m_y;
  private int m_rowHeight;

  /**
   * {@link PropertyTable} call this method to inform that cursor location was changed.
   */
  public void update(Property property,
      boolean onTitle,
      boolean onValue,
      int beginX,
      int endX,
      int y,
      int rowHeight) {
    m_property = property;
    m_onTitle = onTitle;
    m_onValue = onValue;
    m_beginX = beginX;
    m_endX = endX;
    m_y = y;
    m_rowHeight = rowHeight;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // IPropertyTooltipSite
  //
  ////////////////////////////////////////////////////////////////////////////
  @Override
public PropertyTable getTable() {
    return m_table;
  }

  @Override
public void hideTooltip() {
    if (m_tooltip != null && !m_tooltip.isDisposed()) {
      m_tooltip.dispose();
    }
    m_tooltip = null;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Showing tooltip
  //
  ////////////////////////////////////////////////////////////////////////////
  private void showTooltip() {
    hideTooltip();
    // check for property
    if (m_property == null) {
      return;
    }
    //
    if (m_onTitle) {
      showTooltip(m_property.getAdapter(PropertyTooltipProvider.class), m_beginX, m_endX);
    }
    if (m_onValue) {
      showTooltip(m_property.getEditor().getAdapter(PropertyTooltipProvider.class),
              m_beginX, m_endX);
    }
  }

  private void showTooltip(PropertyTooltipProvider provider, int startX, int endX) {
      if (provider == null) {
        return;
      }
    // create Shell
    {
      m_tooltip = new Shell(m_table.getShell(), SWT.NO_FOCUS | SWT.ON_TOP | SWT.TOOL | SWT.SINGLE);
      configureColors(m_tooltip);
      GridLayoutFactory.create(m_tooltip).noMargins();
    }
    // prepare control
    Control control = provider.createTooltipControl(m_property, m_tooltip, endX - startX, this);
    if (control == null) {
      hideTooltip();
      return;
    }
    // show Shell
    {
      // prepare tooltip location
      Point tooltipLocation;
      if (provider.getTooltipPosition() == PropertyTooltipProvider.ON) {
        tooltipLocation = m_table.toDisplay(new Point(startX, m_y));
      } else {
        tooltipLocation = m_table.toDisplay(new Point(startX, m_y + m_rowHeight));
      }
      // set location/size and open
      m_tooltip.setLocation(tooltipLocation.x, tooltipLocation.y);
      // for non-windows systems the tooltip may have invalid tooltip bounds
      // because some widget's API functions may fail if tooltip content is not visible
      // ex., on MacOSX tree widget's items has zero bounds since they are not yet visible.
      // the workaround is to preset tooltip size to big values before any computeSize called.
      if (!EnvironmentUtils.IS_WINDOWS) {
        m_tooltip.setSize(1000, 1000);
      }
      m_tooltip.setSize(m_tooltip.computeSize(SWT.DEFAULT, SWT.DEFAULT));
      provider.show(m_tooltip);
    }
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Utils
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Sets given {@link Control} correct background/foreground for tooltips.
   */
  private void configureColors(Control control) {
    Display display = Display.getCurrent();
    control.setForeground(display.getSystemColor(SWT.COLOR_INFO_FOREGROUND));
    control.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
  }
}
