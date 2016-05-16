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
 * Implementation of {@link IFlyoutPreferences} for keeping settings in memory.
 * 
 * @author scheglov_ke
 * @coverage core.control
 */
public final class MemoryFlyoutPreferences implements IFlyoutPreferences {
  private int m_dockLocation;
  private int m_state;
  private int m_width;

  ////////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  ////////////////////////////////////////////////////////////////////////////
  public MemoryFlyoutPreferences(int dockLocation, int state, int width) {
    m_dockLocation = dockLocation;
    m_state = state;
    m_width = width;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // IFlyoutPreferences
  //
  ////////////////////////////////////////////////////////////////////////////
  public int getDockLocation() {
    return m_dockLocation;
  }

  public int getState() {
    return m_state;
  }

  public int getWidth() {
    return m_width;
  }

  public void setDockLocation(int location) {
    m_dockLocation = location;
  }

  public void setState(int state) {
    m_state = state;
  }

  public void setWidth(int width) {
    m_width = width;
  }
}