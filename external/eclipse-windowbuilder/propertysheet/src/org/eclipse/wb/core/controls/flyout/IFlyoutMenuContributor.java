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

import org.eclipse.jface.action.IMenuManager;

/**
 * Contributes items into {@link IMenuManager} or {@link FlyoutControlComposite}.
 * 
 * @author scheglov_ke
 * @coverage core.control
 */
public interface IFlyoutMenuContributor {
  void contribute(IMenuManager manager);
}
