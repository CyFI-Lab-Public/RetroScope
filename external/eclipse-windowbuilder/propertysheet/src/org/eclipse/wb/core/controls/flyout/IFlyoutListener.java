/*******************************************************************************
 * Copyright (c) 2012 Google, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Google, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.wb.core.controls.flyout;

// BEGIN ADT MODIFICATIONS
/** Interface for listeners interested in window state transitions in flyout windows */
public interface IFlyoutListener {
  /**
   * The flyout has changed state from the old state to the new state (see the
   * state constants in {@link IFlyoutPreferences})
   *
   * @param oldState the old state
   * @param newState the new state
   */
  void stateChanged(int oldState, int newState);
}
// END ADT MODIFICATIONS
