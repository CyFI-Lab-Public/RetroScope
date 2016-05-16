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

import org.eclipse.wb.internal.core.model.property.editor.TextDisplayPropertyEditor;
import org.eclipse.wb.internal.core.model.property.editor.complex.IComplexPropertyEditor;
import org.eclipse.wb.internal.core.model.property.editor.presentation.PropertyEditorPresentation;
import org.eclipse.wb.internal.core.model.property.table.PropertyTable;
import org.eclipse.wb.internal.core.model.property.table.PropertyTooltipProvider;
import org.eclipse.wb.internal.core.model.property.table.PropertyTooltipTextProvider;

import org.eclipse.swt.graphics.Point;

import java.util.List;

/**
 * Implementation of {@link Property} that shows given inner {@link Property}'s using
 * {@link IComplexPropertyEditor}.
 * 
 * @author scheglov_ke
 * @coverage core.model.property
 */
public class ComplexProperty extends Property {
  private final String m_title;
  private String m_text;
  private String m_tooltip;
  private boolean m_modified;
  private Property[] m_properties;

  ////////////////////////////////////////////////////////////////////////////
  //
  // Constructors
  //
  ////////////////////////////////////////////////////////////////////////////
  public ComplexProperty(String title, String text) {
    this(title, text, new Property[0]);
  }

  public ComplexProperty(String title, String text, Property[] properties) {
    super(new ComplexPropertyEditor());
    m_title = title;
    m_text = text;
    setText(text);
    setProperties(properties);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Access
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Sets the text.
   */
  public void setText(String text) {
    m_text = text;
  }

  /**
   * @return the text to display as value.
   */
  public String getText() throws Exception {
    return m_text;
  }

  /**
   * Sets the tooltip text.
   */
  public void setTooltip(String tooltip) {
    m_tooltip = tooltip;
  }

  /**
   * Specifies the {@link PropertyEditorPresentation}, for example to displaying "..." button.
   */
  public void setEditorPresentation(PropertyEditorPresentation presentation) {
    ((ComplexPropertyEditor) getEditor()).m_presentation = presentation;
  }

  /**
   * @return the sub-properties.
   */
  public Property[] getProperties() {
    return m_properties;
  }

  /**
   * Sets the sub-properties.
   */
  public void setProperties(Property[] properties) {
    m_properties = properties;
  }

  /**
   * Sets the sub-properties.
   */
  public void setProperties(List<Property> properties) {
    Property[] propertiesArray = properties.toArray(new Property[properties.size()]);
    setProperties(propertiesArray);
  }

  /**
   * Sets the "modified" flag.
   */
  public void setModified(boolean modified) {
    m_modified = modified;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Property
  //
  ////////////////////////////////////////////////////////////////////////////
  @Override
  public String getTitle() {
    return m_title;
  }

  @Override
  public boolean isModified() throws Exception {
    return m_modified;
  }

  @Override
  public Object getValue() throws Exception {
    return null;
  }

  @Override
  public void setValue(Object value) throws Exception {
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Adapter
  //
  ////////////////////////////////////////////////////////////////////////////
  @Override
  public <T> T getAdapter(Class<T> adapter) {
    if (adapter == PropertyTooltipProvider.class && m_tooltip != null) {
      return adapter.cast(new PropertyTooltipTextProvider() {
        @Override
        protected String getText(Property property) throws Exception {
          return m_tooltip;
        }
      });
    }
    return super.getAdapter(adapter);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // ComplexPropertyEditor
  //
  ////////////////////////////////////////////////////////////////////////////
  private static final class ComplexPropertyEditor extends TextDisplayPropertyEditor
      implements
        IComplexPropertyEditor {
    private PropertyEditorPresentation m_presentation;

    ////////////////////////////////////////////////////////////////////////////
    //
    // IComplexPropertyEditor
    //
    ////////////////////////////////////////////////////////////////////////////
    public Property[] getProperties(Property property) throws Exception {
      return ((ComplexProperty) property).getProperties();
    }

    ////////////////////////////////////////////////////////////////////////////
    //
    // TextDisplayPropertyEditor
    //
    ////////////////////////////////////////////////////////////////////////////
    @Override
    protected String getText(Property property) throws Exception {
      return ((ComplexProperty) property).getText();
    }

    ////////////////////////////////////////////////////////////////////////////
    //
    // PropertyEditor
    //
    ////////////////////////////////////////////////////////////////////////////
    @Override
    public boolean activate(PropertyTable propertyTable, Property property, Point location)
        throws Exception {
      return false;
    }

    ////////////////////////////////////////////////////////////////////////////
    //
    // Presentation
    //
    ////////////////////////////////////////////////////////////////////////////
    @Override
    public PropertyEditorPresentation getPresentation() {
      return m_presentation;
    }
  }
}
