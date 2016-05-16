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
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Date;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.util.DateUtils;

import org.eclipse.releng.util.rss.Messages;
import org.eclipse.releng.util.rss.RSSFeedUtil;

//TODO: bug - can't run CreateFeed and AddEntry back when debug=2

/**
 * Parameters: 
 *   debug - more output to console - eg., 0|1|2
 *   
 *   file - path to the XML file that will be created - eg., /path/to/file.to.create.xml
 *   project - project's name, used to label the feed - eg., Eclipse, EMF, UML2
 *   feedURL - URL of the feed where it will be published - eg., http://servername/path/to/feed.xml
 * @author nickb
 *
 */
public class RSSFeedCreateFeedTask extends Task {

  private int debug = 0;

  //$ANALYSIS-IGNORE codereview.java.rules.portability.RulePortabilityLineSeparators
  private static final String NL="\n"; //$NON-NLS-1$
  private static final String NS = ""; //$NON-NLS-1$
  private static final String SP = " "; //$NON-NLS-1$
  
  //required fields
  private File file;
  private String project;
  private String feedURL;

  //optional
  public void setDebug(int debug) { this.debug = debug; }

  //required fields
  public void setFile(String file) { 
    if (isNullString(file))
    { System.err.println(Messages.getString("RSSFeedCommon.FileError")); }  //$NON-NLS-1$
    else
    { this.file = new File(file); }
  }
  public void setProject(String project) { 
    if (isNullString(project))
    { System.err.println(Messages.getString("RSSFeedCommon.ProjectError")); }  //$NON-NLS-1$
    else
    { this.project = project; }
  }
  public void setFeedURL(String feedURL) { 
    if (isNullString(feedURL))
    { System.err.println(Messages.getString("RSSFeedCommon.FeedURLError")); }  //$NON-NLS-1$
    else
    { this.feedURL = feedURL; }
  }

  // The method executing the task
  public void execute() throws BuildException {
    if (debug>0) { 
      System.out.println(Messages.getString("RSSFeedCreateFeedTask.Creating") + project + SP + Messages.getString("RSSFeedCommon.RSSFeedFile") + SP + file.toString() + ", " + Messages.getString("RSSFeedCommon.ToBePublishedAt") + feedURL); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
    }
    writeFeedXML(createFeedXML(),file);
    if (debug>1) { 
      writeFeedXML(createFeedXML(),System.out);
    }
  }

  private String createFeedXML() {
    StringBuffer sb = new StringBuffer();
    sb.append("<?xml-stylesheet href=\"http://www.blogger.com/styles/atom.css\" type=\"text/css\"?>" + NL); //$NON-NLS-1$
    sb.append("<feed xmlns=\"http://www.w3.org/2005/Atom\">" + NL); //$NON-NLS-1$
    sb.append("  <title>" + project + SP + Messages.getString("RSSFeedCreateFeedTask.Builds") + "</title>" + NL); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    sb.append("  <link rel=\"self\" type=\"application/atom+xml\" href=\"" + (!isNullString(feedURL)?feedURL:NS) + "\"/>" + NL); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    sb.append("  <updated>" + getTimestamp() + "</updated>" + NL); //$NON-NLS-1$ //$NON-NLS-2$
    sb.append("  <author>" + NL); //$NON-NLS-1$
    sb.append("    <name>" + (!isNullString(project)?project + SP : NS) + Messages.getString("RSSFeedCreateFeedTask.BuildTeam") + "</name>" + NL); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
    sb.append("  </author>" + NL); //$NON-NLS-1$
    sb.append("  <id>" + (!isNullString(feedURL)?feedURL:NS) + "</id>" + NL); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    sb.append("</feed>" + NL + NL); //$NON-NLS-1$
    return sb.toString();
  }

  private void writeFeedXML(String feedXML,File file) {
    try{
      PrintWriter writer = new PrintWriter(new FileWriter(file));
      writer.println(feedXML);
      writer.flush();
      writer.close();
    } catch (IOException e){
      System.out.println(Messages.getString("RSSFeedCreateFeedTask.UnableToWriteToFile")+file); //$NON-NLS-1$
    }

  }

  private void writeFeedXML(String feedXML, PrintStream ps) {
    PrintWriter writer = new PrintWriter(ps);
    writer.println(feedXML);
    writer.flush();
    writer.close();
  }

  private String getTimestamp() { // eg., 2006-04-10T20:40:08Z
    return DateUtils.format(new Date(), DateUtils.ISO8601_DATETIME_PATTERN) + "Z";  //$NON-NLS-1$
  }

  private static boolean isNullString(String str)
  {
    return RSSFeedUtil.isNullString(str);
  }

}