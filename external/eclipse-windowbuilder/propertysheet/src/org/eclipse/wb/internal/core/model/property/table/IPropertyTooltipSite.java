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

/**
 * Interface that allows control of {@link PropertyTooltipProvider} interact with
 * {@link PropertyTableTooltipHelper}.
 * 
 * @author scheglov_ke
 * @coverage core.model.property.table
 */
public interface IPropertyTooltipSite {
  /**
   * @return the {@link PropertyTable} of this site.
   */
  PropertyTable getTable();

  /**
   * Hides current tooltip.
   */
  void hideTooltip();
}
