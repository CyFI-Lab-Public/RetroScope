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

import org.eclipse.jface.preference.IPreferenceStore;

/**
 * Implementation of {@link IFlyoutPreferences} for {@link IPreferenceStore}.
 * 
 * @author scheglov_ke
 * @coverage core.control
 */
public final class PluginFlyoutPreferences implements IFlyoutPreferences {
  private final IPreferenceStore m_store;
  private final String m_dockLocationKey;
  private final String m_stateKey;
  private final String m_widthKey;

  ////////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  ////////////////////////////////////////////////////////////////////////////
  public PluginFlyoutPreferences(IPreferenceStore store, String prefix) {
    m_store = store;
    m_dockLocationKey = prefix + ".flyout.dockLocation";
    m_stateKey = prefix + ".flyout.state";
    m_widthKey = prefix + ".flyout.width";
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Access
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Initializes defaults using given values.
   */
  public void initializeDefaults(int location, int state, int width) {
    m_store.setDefault(m_dockLocationKey, location);
    m_store.setDefault(m_stateKey, state);
    m_store.setDefault(m_widthKey, width);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // IFlyoutPreferences
  //
  ////////////////////////////////////////////////////////////////////////////
  public int getDockLocation() {
    return m_store.getInt(m_dockLocationKey);
  }

  public int getState() {
    return m_store.getInt(m_stateKey);
  }

  public int getWidth() {
    return m_store.getInt(m_widthKey);
  }

  public void setDockLocation(int location) {
    m_store.setValue(m_dockLocationKey, location);
  }

  public void setState(int state) {
    m_store.setValue(m_stateKey, state);
  }

  public void setWidth(int width) {
    m_store.setValue(m_widthKey, width);
  }
}
