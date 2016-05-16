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

import org.eclipse.wb.core.controls.CFlatButton;
import org.eclipse.wb.internal.core.model.property.Property;
import org.eclipse.wb.internal.core.model.property.table.PropertyTable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;

/**
 * Internal implementation of {@link PropertyEditorPresentation} for displaying special owner-draw
 * button for Mac OSX.
 * 
 * @author mitin_aa
 * @coverage core.model.property.editor
 */
final class ButtonPropertyEditorPresentationImplMac extends ButtonPropertyEditorPresentationImpl {
  ////////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  ////////////////////////////////////////////////////////////////////////////
  public ButtonPropertyEditorPresentationImplMac(ButtonPropertyEditorPresentation presentation) {
    super(presentation);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Control
  //
  ////////////////////////////////////////////////////////////////////////////
  @Override
  protected final Control createControlImpl(final PropertyTable propertyTable, Property property) {
    CFlatButton button = new CFlatButton(propertyTable, SWT.NONE);
    button.setImage(getPresentation().getImage());
    button.setToolTipText(getPresentation().getTooltip());
    return button;
  }

  @Override
  public final void setSelection(PropertyTable propertyTable, Property property, boolean selected) {
    CFlatButton button = (CFlatButton) m_propertyToControl.get(propertyTable, property);
    if (button != null) {
      button.setSelected(selected);
    }
  }
}
