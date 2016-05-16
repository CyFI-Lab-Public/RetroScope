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

import org.eclipse.wb.internal.core.model.property.Property;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

/**
 * Provider for tooltip controls.
 * 
 * @author scheglov_ke
 * @coverage core.model.property.table
 */
public abstract class PropertyTooltipProvider {
  /**
   * Show tooltip directly on property row.
   */
  public static final int ON = 0;
  /**
   * Show tooltip below property row.
   */
  public static final int BELOW = 1;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PropertyTooltipProvider
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Create tooltip control.
   */
  public abstract Control createTooltipControl(Property property,
      Composite parent,
      int availableWidth,
      IPropertyTooltipSite site);

  /**
   * Shows tooltip {@link Shell}.
   */
  public void show(Shell shell) {
    shell.setVisible(true);
  }

  /**
   * Returns position for tooltip control. Usually we should show directly on same row, because we
   * use tooltip to show just longer (full) text of property. But for "class" property we show
   * hierarchy, so it is better show it below and allow user see also property row.
   */
  public int getTooltipPosition() {
    return ON;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Tooltip listener
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * {@link Listener} that hides tooltip on mouse exit or click.
   */
  protected static final class HideListener implements Listener {
    private final IPropertyTooltipSite m_site;

    ////////////////////////////////////////////////////////////////////////////
    //
    // Constructor
    //
    ////////////////////////////////////////////////////////////////////////////
    public HideListener(IPropertyTooltipSite site) {
      m_site = site;
    }

    ////////////////////////////////////////////////////////////////////////////
    //
    // Listener
    //
    ////////////////////////////////////////////////////////////////////////////
    public void handleEvent(Event event) {
      Control tooltipControl = (Control) event.widget;
      switch (event.type) {
        case SWT.MouseDown : {
          PropertyTable table = m_site.getTable();
          // convert location from tooltip to table
          Point p = new Point(event.x, event.y);
          p = tooltipControl.toDisplay(p);
          p = table.toControl(p);
          // send MouseDown to table
          Event newEvent = new Event();
          newEvent.x = p.x;
          newEvent.y = p.y;
          table.notifyListeners(SWT.MouseDown, newEvent);
          // hide tooltip
          m_site.hideTooltip();
          break;
        }
        case SWT.MouseExit :
          m_site.hideTooltip();
          break;
      }
    }
  }
}
