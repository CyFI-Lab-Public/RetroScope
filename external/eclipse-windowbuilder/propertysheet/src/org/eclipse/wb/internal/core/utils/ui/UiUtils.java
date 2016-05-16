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
package org.eclipse.wb.internal.core.utils.ui;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

/**
 * Utilities for UI.
 *
 * @author scheglov_ke
 */
public class UiUtils {
  ////////////////////////////////////////////////////////////////////////////
  //
  // Message dialogs
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Opens standard warning dialog.
   */
  public static void openWarning(Shell parent, String title, String message) {
    MessageDialog dialog =
        new MessageDialog(parent,
            title,
            null,
            message,
            MessageDialog.WARNING,
            new String[]{IDialogConstants.OK_LABEL},
            0);
    dialog.open();
  }
}