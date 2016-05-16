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
package org.eclipse.wb.internal.core.editor.structure.property;

import com.google.common.collect.Lists;

import org.eclipse.wb.internal.core.model.property.Property;
import org.eclipse.wb.internal.core.model.property.PropertyManager;

import java.util.Iterator;
import java.util.List;

/**
 * Helper for computing intersection of {@link Property} arrays.
 * 
 * @author scheglov_ke
 * @coverage core.editor.structure
 */
public final class PropertyListIntersector {
  private List<PropertyGroup> m_intersection;

  ////////////////////////////////////////////////////////////////////////////
  //
  // Access
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Updates intersection by intersecting with new given array.
   */
  public void intersect(Property[] properties) {
    if (m_intersection == null) {
      m_intersection = Lists.newArrayList();
      for (int i = 0; i < properties.length; i++) {
        Property property = properties[i];
        m_intersection.add(new PropertyGroup(property));
      }
    } else {
      for (Iterator<PropertyGroup> I = m_intersection.iterator(); I.hasNext();) {
        PropertyGroup propertyGroup = I.next();
        if (!propertyGroup.add(properties)) {
          I.remove();
        }
      }
    }
  }

  /**
   * @return the array of matched composite {@link Property}'s.
   */
  public Property[] getProperties() {
    List<Property> properties = Lists.newArrayList();
    for (PropertyGroup propertyGroup : m_intersection) {
      Property compositeProperty = propertyGroup.getCompositeProperty();
      if (compositeProperty != null) {
        properties.add(compositeProperty);
      }
    }
    //
    return properties.toArray(new Property[properties.size()]);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PropertyGroup
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * The group of {@link Property}'s that match.
   */
  private static final class PropertyGroup {
    private final List<Property> m_properties = Lists.newArrayList();

    ////////////////////////////////////////////////////////////////////////////
    //
    // Constructor
    //
    ////////////////////////////////////////////////////////////////////////////
    public PropertyGroup(Property property) {
      m_properties.add(property);
    }

    ////////////////////////////////////////////////////////////////////////////
    //
    // Access
    //
    ////////////////////////////////////////////////////////////////////////////
    /**
     * @return <code>true</code> if new matched {@link Property} from given array was added.
     */
    public boolean add(Property[] properties) {
      for (Property property : properties) {
        if (add(property)) {
          return true;
        }
      }
      // no match
      return false;
    }

    /**
     * @return the composite {@link Property} for this group.
     */
    public Property getCompositeProperty() {
      Property properties[] = m_properties.toArray(new Property[m_properties.size()]);
      return properties[0].getComposite(properties);
    }

    ////////////////////////////////////////////////////////////////////////////
    //
    // Internal
    //
    ////////////////////////////////////////////////////////////////////////////
    /**
     * @return <code>true</code> if given {@link Property} matches and was added.
     */
    private boolean add(Property property) {
      Property example = m_properties.get(0);
      if (example.getClass() == property.getClass()
          && example.getTitle().equals(property.getTitle())
          && PropertyManager.getCategory(example) == PropertyManager.getCategory(property)) {
        m_properties.add(property);
        return true;
      }
      // no match
      return false;
    }
  }
}
