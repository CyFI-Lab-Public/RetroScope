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
package org.eclipse.wb.internal.core.model.property;

import org.eclipse.wb.internal.core.model.property.category.PropertyCategory;


/**
 * {@link PropertyManager} is used to get/set attributes of {@link Property}.
 *
 * @author scheglov_ke
 * @coverage core.model.property
 */
public final class PropertyManager {
  public static PropertyCategory getCategory(Property property) {
    // Note: In WindowBuilder there was a bunch of support for loading custom
    // categories here based on toolkits; in ADT we'll need to do it differently
    // so this code was all stripped out.
    return property.getCategory();
  }

  /**
   * @return the forced {@link PropertyCategory} of given Property, may be <code>null</code>.
   */
  public static PropertyCategory getCategoryForced(Property property) {
    return null;
  }
}
