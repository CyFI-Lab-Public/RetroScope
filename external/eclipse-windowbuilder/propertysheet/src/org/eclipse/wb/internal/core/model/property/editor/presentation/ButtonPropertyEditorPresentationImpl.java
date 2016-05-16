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
package org.eclipse.wb.internal.core.model.property.editor.presentation;

import com.google.common.collect.Maps;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.wb.internal.core.model.property.Property;
import org.eclipse.wb.internal.core.model.property.table.PropertyTable;
import org.eclipse.wb.internal.core.utils.Pair;

import java.util.Map;

/**
 * Internal implementation of {@link PropertyEditorPresentation} for displaying {@link Button}.
 *
 * @author scheglov_ke
 * @author mitin_aa
 * @coverage core.model.property.editor
 */
class ButtonPropertyEditorPresentationImpl extends PropertyEditorPresentation {
  protected final PropertyToControlMap m_propertyToControl = new PropertyToControlMap();
  private final ButtonPropertyEditorPresentation m_presentation;

  ////////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  ////////////////////////////////////////////////////////////////////////////
  public ButtonPropertyEditorPresentationImpl(ButtonPropertyEditorPresentation presentation) {
    m_presentation = presentation;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PropertyEditorPresentation
  //
  ////////////////////////////////////////////////////////////////////////////
  @Override
  public final void hide(PropertyTable propertyTable, Property property) {
    Control control = m_propertyToControl.remove(propertyTable, property);
    if (control != null) {
      control.dispose();
    }
  }

  @Override
  public final int show(PropertyTable propertyTable,
      Property property,
      int x,
      int y,
      int width,
      int height) {
    // prepare control
    Control control = m_propertyToControl.get(propertyTable, property);
    if (control == null) {
      control = createControl(propertyTable, property);
    }
    // set bounds
    final int controlWidth = height;
    final int controlX = x + width - controlWidth;
    setBounds(control, controlX, y, controlWidth, height);
    return controlWidth;
  }

  /**
   * Finds and select the appropriate {@link Control} belonging to given property.
   */
  public void setSelection(PropertyTable propertyTable, Property property, boolean selected) {
    Button button = (Button) m_propertyToControl.get(propertyTable, property);
    if (button != null) {
      button.setSelection(selected);
    }
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Control
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Creates the control for given property and initializes newly created control.
   */
  private Control createControl(final PropertyTable propertyTable, final Property property) {
    Control control = createControlImpl(propertyTable, property);
    m_propertyToControl.put(propertyTable, property, control);
    // when Control disposed, remove Control/Property from map to avoid memory leak
    control.addListener(SWT.Dispose, new Listener() {
      @Override
    public void handleEvent(Event e) {
        m_propertyToControl.remove(propertyTable, property);
      }
    });
    // activate property on mouse down
    control.addListener(SWT.MouseDown, new Listener() {
      @Override
    public void handleEvent(Event event) {
        propertyTable.deactivateEditor(true);
        propertyTable.setActiveProperty(property);
      }
    });
    // return focus on propertyTable after click
    control.addListener(SWT.MouseUp, new Listener() {
      @Override
    public void handleEvent(Event event) {
        propertyTable.forceFocus();
      }
    });
    // handle selection
    control.addListener(SWT.Selection, new Listener() {
      @Override
    public void handleEvent(Event event) {
        try {
          getPresentation().onClick(propertyTable, property);
        } catch (Throwable e) {
          propertyTable.deactivateEditor(false);
          propertyTable.handleException(e);
        }
      }
    });
    return control;
  }

  /**
   * Creates the {@link Control} instance. By default, {@link Button} instance created.
   */
  protected Control createControlImpl(final PropertyTable propertyTable, final Property property) {
    Button button = new Button(propertyTable, getPresentation().getStyle());
    button.setImage(getPresentation().getImage());
    button.setToolTipText(getPresentation().getTooltip());
    return button;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Access
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * @return the 'parent' presentation. Internal usage only.
   */
  protected final ButtonPropertyEditorPresentation getPresentation() {
    return m_presentation;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Utils
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Sets new bounds for {@link Control}, optimizing when possible.
   */
  private static void setBounds(Control control, int newX, int newY, int newWidth, int newHeight) {
    // check, may be Control is invisible, so no reason to change bounds
    {
      // is in negative zone
      if (newY + newHeight < 0) {
        control.setVisible(false);
        return;
      }
      // is out of client area height
      Rectangle parentArea = control.getParent().getClientArea();
      if (newY > parentArea.height) {
        control.setVisible(false);
        return;
      }
    }
    // well, now we sure that Control is visible
    if (!control.getVisible()) {
      control.setVisible(true);
    }
    // prepare old size, remember new
    Integer oldWidthObject = (Integer) control.getData("oldWidth");
    Integer oldHeightObject = (Integer) control.getData("oldHeight");
    control.setData("oldWidth", newWidth);
    control.setData("oldHeight", newHeight);
    // check, may be same size
    if (oldWidthObject != null) {
      int oldWidth = oldWidthObject.intValue();
      int oldHeight = oldHeightObject.intValue();
      if (oldWidth == newWidth && oldHeight == newHeight) {
        control.setLocation(newX, newY);
        return;
      }
    }
    // no any optimization possible, just set bounds
    control.setBounds(newX, newY, newWidth, newHeight);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Controls map
  //
  ////////////////////////////////////////////////////////////////////////////
  protected static final class PropertyToControlMap {
    private final Map<Pair<PropertyTable, Property>, Control> m_map = Maps.newHashMap();

    void put(PropertyTable propertyTable, Property property, Control control) {
      m_map.put(Pair.create(propertyTable, property), control);
    }

    Control remove(PropertyTable propertyTable, Property property) {
      return m_map.remove(Pair.create(propertyTable, property));
    }

    Control get(PropertyTable propertyTable, Property property) {
      return m_map.get(Pair.create(propertyTable, property));
    }
  }
}
