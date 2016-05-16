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
package org.eclipse.wb.internal.core;

import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * Helper for environment state access.
 *
 * @author scheglov_ke
 * @coverage core
 */
public final class EnvironmentUtils extends AbstractUIPlugin {
  ////////////////////////////////////////////////////////////////////////////
  //
  // Operating systems
  //
  ////////////////////////////////////////////////////////////////////////////
  /** True if this is running on Windows */
  public static boolean IS_WINDOWS;
  /** True if this is running on Mac */
  public static boolean IS_MAC;
  /** True if this is running on Linux */
  public static boolean IS_LINUX;
}
