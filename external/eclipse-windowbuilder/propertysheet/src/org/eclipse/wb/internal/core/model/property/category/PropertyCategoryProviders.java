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
package org.eclipse.wb.internal.core.model.property.category;

import org.eclipse.wb.internal.core.model.property.Property;
import org.eclipse.wb.internal.core.model.property.PropertyManager;

/**
 * Factory for {@link PropertyCategoryProvider} instances.
 * 
 * @author scheglov_ke
 * @coverage core.model.property
 */
public final class PropertyCategoryProviders {
  ////////////////////////////////////////////////////////////////////////////
  //
  // Simple providers
  //
  ////////////////////////////////////////////////////////////////////////////
  private static final PropertyCategoryProvider FROM_PROPERTY = new PropertyCategoryProvider() {
    public PropertyCategory getCategory(Property property) {
      return property.getCategory();
    }
  };

  /**
   * Returns result of {@link Property#getCategory()}, never <code>null</code>.
   */
  public static PropertyCategoryProvider fromProperty() {
    return FROM_PROPERTY;
  }

  private static final PropertyCategoryProvider FORCED_BY_USER = new PropertyCategoryProvider() {
    public PropertyCategory getCategory(Property property) {
      return PropertyManager.getCategoryForced(property);
    }
  };

  /**
   * Returns category forced by user, may be <code>null</code>.
   */
  public static PropertyCategoryProvider forcedByUser() {
    return FORCED_BY_USER;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Compound
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Returns first not <code>null</code> category returned by provider.
   */
  public static PropertyCategoryProvider combine(final PropertyCategoryProvider... providers) {
    return new PropertyCategoryProvider() {
      public PropertyCategory getCategory(Property property) {
        for (PropertyCategoryProvider provider : providers) {
          PropertyCategory category = provider.getCategory(property);
          if (category != null) {
            return category;
          }
        }
        throw new IllegalStateException("Can not provide category for " + property.getTitle());
      }
    };
  }

  private static final PropertyCategoryProvider DEF = combine(forcedByUser(), fromProperty());

  /**
   * Returns the default combination of {@link PropertyCategoryProvider}s - first
   * {@link #forcedByUser()}, then {@link #fromProperty()}.
   */
  public static PropertyCategoryProvider def() {
    return DEF;
  }
}
