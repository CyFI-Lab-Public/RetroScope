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
package org.eclipse.wb.internal.core.utils.ui.dialogs;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.wb.internal.core.utils.execution.ExecutionUtils;
import org.eclipse.wb.internal.core.utils.execution.RunnableObjectEx;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.List;

/**
 * The dialog for editing array of {@link String}'s.
 *
 * @author scheglov_ke
 * @coverage core.ui
 */
public class StringsDialog extends TextDialog {
  ////////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  ////////////////////////////////////////////////////////////////////////////
  public StringsDialog(Shell parentShell,
      AbstractUIPlugin plugin,
      String titleText,
      String headerText,
      String footerText) {
    super(parentShell, plugin, titleText, headerText, footerText);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Items
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Sets the items to edit.
   */
  public void setItems(String[] items) {
    setText(Joiner.on('\n').join(items));
  }

  /**
   * @return the edited items.
   */
  public String[] getItems() {
    return ExecutionUtils.runObjectLog(new RunnableObjectEx<String[]>() {
      @Override
    public String[] runObject() throws Exception {
        List<String> strings = Lists.newArrayList();
        BufferedReader br = new BufferedReader(new StringReader(getText()));
        while (true) {
          String s = br.readLine();
          if (s == null) {
            break;
          }
          strings.add(s);
        }
        return strings.toArray(new String[strings.size()]);
      }
    }, new String[0]);
  }
}
