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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * {@link Dialog} that remembers location/size between usage sessions.
 * 
 * @author scheglov_ke
 * @coverage core.ui
 */
public abstract class ResizableDialog extends Dialog {
  /**
   * Key for accessing {@link Dialog} from its {@link Shell}.
   */
  public static final String KEY_DIALOG = "KEY_DIALOG";
  ////////////////////////////////////////////////////////////////////////////
  //
  // Internal constants
  //
  ////////////////////////////////////////////////////////////////////////////
  private static final String X = "x";
  private static final String Y = "y";
  private static final String WIDTH = "width";
  private static final String HEIGHT = "height";
  ////////////////////////////////////////////////////////////////////////////
  //
  // Instance fields
  //
  ////////////////////////////////////////////////////////////////////////////
  private final AbstractUIPlugin m_plugin;

  ////////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  ////////////////////////////////////////////////////////////////////////////
  public ResizableDialog(Shell parentShell, AbstractUIPlugin plugin) {
    super(parentShell);
    m_plugin = plugin;
    setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Size
  //
  ////////////////////////////////////////////////////////////////////////////
  @Override
  protected Point getInitialSize() {
    // track the current dialog bounds
    installDialogBoundsTracker();
    // answer the size from the previous incarnation
    Point defaultSize = getDefaultSize();
    if ((getShellStyle() & SWT.RESIZE) != 0) {
      Rectangle oldBounds = loadBounds();
      if (oldBounds != null) {
        Rectangle displayBounds = getShell().getDisplay().getBounds();
        int width = Math.min(displayBounds.width, Math.max(oldBounds.width, defaultSize.x));
        int height = Math.min(displayBounds.height, Math.max(oldBounds.height, defaultSize.y));
        return new Point(width, height);
      }
    }
    // use default size
    return defaultSize;
  }

  /**
   * @return the default size of dialog.
   */
  protected Point getDefaultSize() {
    return super.getInitialSize();
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Location
  //
  ////////////////////////////////////////////////////////////////////////////
  @Override
  protected Point getInitialLocation(Point initialSize) {
    Rectangle windowBounds;
    {
      Shell windowShell = m_plugin.getWorkbench().getActiveWorkbenchWindow().getShell();
      windowBounds = windowShell.getBounds();
    }
    // answer the location from the previous incarnation
    Rectangle bounds = loadBounds();
    if (bounds != null) {
      int x = bounds.x;
      int y = bounds.y;
      int maxX = windowBounds.x + windowBounds.width - initialSize.x;
      int maxY = windowBounds.y + windowBounds.height - initialSize.y;
      if (x > maxX) {
        x = maxX;
      }
      if (y > maxY) {
        y = maxY;
      }
      if (x < windowBounds.x) {
        x = windowBounds.x;
      }
      if (y < windowBounds.y) {
        y = windowBounds.y;
      }
      return new Point(x, y);
    }
    // default location - centered on workbench window
    int x = windowBounds.x + (windowBounds.width - initialSize.x) / 2;
    int y = windowBounds.y + (windowBounds.height - initialSize.y) / 2;
    return new Point(x, y);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Bounds
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Loads bounds from {@link IDialogSettings}.
   */
  private Rectangle loadBounds() {
    IDialogSettings settings = getDialogSettings();
    try {
      return new Rectangle(settings.getInt(X),
          settings.getInt(Y),
          settings.getInt(WIDTH),
          settings.getInt(HEIGHT));
    } catch (NumberFormatException e) {
      return null;
    }
  }

  /**
   * Saves bounds to {@link IDialogSettings}.
   */
  private void saveBounds(Rectangle bounds) {
    IDialogSettings settings = getDialogSettings();
    settings.put(X, bounds.x);
    settings.put(Y, bounds.y);
    settings.put(WIDTH, bounds.width);
    settings.put(HEIGHT, bounds.height);
  }

  /**
   * @return the {@link IDialogSettings} for this dialog with this type.
   */
  protected IDialogSettings getDialogSettings() {
    IDialogSettings settings = m_plugin.getDialogSettings();
    String sectionName = getDialogSettingsSectionName();
    if (settings.getSection(sectionName) == null) {
      return settings.addNewSection(sectionName);
    }
    return settings.getSection(sectionName);
  }

  /**
   * @return the name of section for dialog specific bounds. By default uses name of {@link Class},
   *         but if same dialog is used for displaying different content, then may be overridden.
   */
  protected String getDialogSettingsSectionName() {
    return getClass().getName();
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Size tracking
  //
  ////////////////////////////////////////////////////////////////////////////
  protected Rectangle cachedBounds;

  private void installDialogBoundsTracker() {
    getShell().addControlListener(new ControlListener() {
      public void controlMoved(ControlEvent e) {
        cachedBounds = getShell().getBounds();
      }

      public void controlResized(ControlEvent e) {
        cachedBounds = getShell().getBounds();
      }
    });
  }

  @Override
  public boolean close() {
    boolean shellMaximized = getShell().getMaximized();
    boolean closed = super.close();
    if (closed && !shellMaximized && cachedBounds != null) {
      saveBounds(cachedBounds);
    }
    return closed;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Shell
  //
  ////////////////////////////////////////////////////////////////////////////
  @Override
  protected void configureShell(Shell newShell) {
    super.configureShell(newShell);
    newShell.setData(KEY_DIALOG, this);
  }
}
