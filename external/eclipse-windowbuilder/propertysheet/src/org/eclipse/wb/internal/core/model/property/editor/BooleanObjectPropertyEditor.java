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
package org.eclipse.wb.internal.core.model.property.editor;

import org.eclipse.wb.internal.core.DesignerPlugin;
import org.eclipse.wb.internal.core.model.property.Property;
import org.eclipse.wb.internal.core.model.property.table.PropertyTable;
import org.eclipse.wb.internal.core.utils.ui.DrawUtils;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

/**
 * The {@link PropertyEditor} for <code>Boolean</code>.
 * 
 * @author scheglov_ke
 * @coverage core.model.property.editor
 */
public final class BooleanObjectPropertyEditor extends PropertyEditor {
  private static final Image m_nullImage = DesignerPlugin.getImage("properties/BooleanNull.png");
  private static final Image m_trueImage = DesignerPlugin.getImage("properties/true.png");
  private static final Image m_falseImage = DesignerPlugin.getImage("properties/false.png");
  ////////////////////////////////////////////////////////////////////////////
  //
  // Instance
  //
  ////////////////////////////////////////////////////////////////////////////
  public static final PropertyEditor INSTANCE = new BooleanObjectPropertyEditor();

  private BooleanObjectPropertyEditor() {
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Presentation
  //
  ////////////////////////////////////////////////////////////////////////////
  @Override
  public void paint(Property property, GC gc, int x, int y, int width, int height) throws Exception {
    Object value = property.getValue();
    if (value instanceof Boolean) {
      boolean booleanValue = ((Boolean) value).booleanValue();
      Image image = booleanValue ? m_trueImage : m_falseImage;
      String text = Boolean.toString(booleanValue);
      paint(gc, x, y, width, height, text, image);
    }
    if (value == null) {
      Image image = m_nullImage;
      String text = "null";
      paint(gc, x, y, width, height, text, image);
    }
  }

  private void paint(GC gc, int x, int y, int width, int height, String text, Image image) {
    // draw image
    {
      DrawUtils.drawImageCV(gc, image, x, y, height);
      // prepare new position/width
      int imageWidth = image.getBounds().width + 2;
      x += imageWidth;
      width -= imageWidth;
    }
    // draw text
    {
      DrawUtils.drawStringCV(gc, text, x, y, width, height);
    }
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Editing
  //
  ////////////////////////////////////////////////////////////////////////////
  @Override
  public boolean activate(PropertyTable propertyTable, Property property, Point location)
      throws Exception {
    // check that user clicked on image
    if (location == null || location.x < m_trueImage.getBounds().width + 2) {
      invertValue(property);
    }
    // don't activate
    return false;
  }

  @Override
  public void doubleClick(Property property, Point location) throws Exception {
    invertValue(property);
  }

  /**
   * Inverts the value of given boolean {@link Property}.
   */
  private void invertValue(Property property) throws Exception {
    Object value = property.getValue();
    // null
    if (value == null) {
      property.setValue(true);
      return;
    }
    // boolean
    if (value instanceof Boolean) {
      boolean booleanValue = ((Boolean) value).booleanValue();
      property.setValue(!booleanValue);
      return;
    }
    // unknown
    property.setValue(true);
  }
}