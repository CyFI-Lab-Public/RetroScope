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

import org.eclipse.wb.internal.core.utils.binding.editors.controls.DefaultControlActionsManager;

import org.eclipse.swt.widgets.Text;

/**
 * Manager for installing/unistalling global handlers for {@link Text} actions commands.
 * 
 * @author mitin_aa
 * @author sablin_aa
 * @coverage core.model.property.editor
 */
public final class TextControlActionsManager extends DefaultControlActionsManager {
  private final Text m_text;

  ////////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  ////////////////////////////////////////////////////////////////////////////
  public TextControlActionsManager(final Text text) {
    super(text);
    m_text = text;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Handlers
  //
  ////////////////////////////////////////////////////////////////////////////
  @Override
  protected void selectAllExecuted() {
    m_text.selectAll();
  }
}
