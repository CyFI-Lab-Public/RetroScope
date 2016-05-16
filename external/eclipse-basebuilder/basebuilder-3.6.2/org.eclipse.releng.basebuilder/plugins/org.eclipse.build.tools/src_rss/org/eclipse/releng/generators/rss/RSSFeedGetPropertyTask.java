/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.releng.generators.rss;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import org.eclipse.releng.util.rss.Messages;
import org.eclipse.releng.util.rss.RSSFeedUtil;

/**
 * Parameters: 
 *   debug - more output to console - eg., 0|1|2
 *   
 *   file - path to the XML file that will be read - eg., /path/to/file.to.read.xml
 *   xpath - xpath string representing the object to read
 * 
 * @author nickb
 *
 */
public class RSSFeedGetPropertyTask extends Task {

  private int debug = 0;

  //required fields
  private File file;

  private String xpath;

  //optional
  public void setDebug(int debug) { this.debug = debug; }

  //required fields
  public void setFile(String file) { 
    if (isNullString(file))
    { System.err.println(Messages.getString("RSSFeedCommon.FileError")); } //$NON-NLS-1$
    else
    { this.file = new File(file); } 
  }
  public void setXpath(String xpath) { 
    if (isNullString(xpath))
    { System.err.println(Messages.getString("RSSFeedCommon.XpathError")); } //$NON-NLS-1$
    else
    { this.xpath = xpath; } 
  }

  // The method executing the task
  public void execute() throws BuildException {
    RSSFeedUpdateEntryTask updater = new RSSFeedUpdateEntryTask();
    updater.setFile(file.toString());
    updater.setXpath(xpath);
    updater.setDebug(debug);
    updater.execute();
  }

  private static boolean isNullString(String str)
  {
    return RSSFeedUtil.isNullString(str);
  }

}