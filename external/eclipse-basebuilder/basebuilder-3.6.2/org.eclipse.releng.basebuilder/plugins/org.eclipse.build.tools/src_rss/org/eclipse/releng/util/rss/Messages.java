/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   IBM - Initial API and implementation
 *
 * </copyright>
 *
 * $Id: Messages.java,v 1.2 2006/09/19 15:53:51 kmoir Exp $
 * /
 *******************************************************************************/
package org.eclipse.releng.util.rss;


import java.util.MissingResourceException;
import java.util.ResourceBundle;


public class Messages
{
  private static final String BUNDLE_NAME = "org.eclipse.releng.util.rss.messages"; //$NON-NLS-1$

  private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

  private Messages()
  {
  }

  public static String getString(String key)
  {
    try
    {
      return RESOURCE_BUNDLE.getString(key);
    }
    catch (MissingResourceException e)
    {
      return '!' + key + '!';
    }
  }
}
