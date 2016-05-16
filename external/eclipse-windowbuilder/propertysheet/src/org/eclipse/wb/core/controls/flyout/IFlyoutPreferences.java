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
package org.eclipse.wb.core.controls.flyout;

/**
 * Provider for preferences of flyout control of {@link FlyoutControlComposite}.
 * 
 * @author scheglov_ke
 * @coverage core.control
 */
public interface IFlyoutPreferences {
  ////////////////////////////////////////////////////////////////////////////
  //
  // Docking constants
  //
  ////////////////////////////////////////////////////////////////////////////
  int DOCK_WEST = 1;
  int DOCK_EAST = 2;
  int DOCK_NORTH = 4;
  int DOCK_SOUTH = 8;
  ////////////////////////////////////////////////////////////////////////////
  //
  // State constants
  //
  ////////////////////////////////////////////////////////////////////////////
  int STATE_OPEN = 0;
  int STATE_COLLAPSED = 1;
  int STATE_EXPANDED = 2;

  ////////////////////////////////////////////////////////////////////////////
  //
  // Access
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * @return the docking location - {@link #DOCK_WEST}, {@link #DOCK_EAST}, {@link #DOCK_NORTH} or
   *         {@link #DOCK_SOUTH}.
   */
  int getDockLocation();

  /**
   * @return the state of flyout - {@link #STATE_OPEN} or {@link #STATE_COLLAPSED}.
   */
  int getState();

  /**
   * @return the width of flyout.
   */
  int getWidth();

  ////////////////////////////////////////////////////////////////////////////
  //
  // Modification
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Sets the docking location.
   */
  void setDockLocation(int location);

  /**
   * Sets the state of flyout - {@link #STATE_OPEN} or {@link #STATE_COLLAPSED}.
   */
  void setState(int state);

  /**
   * Sets the width of flyout.
   */
  void setWidth(int width);
}
