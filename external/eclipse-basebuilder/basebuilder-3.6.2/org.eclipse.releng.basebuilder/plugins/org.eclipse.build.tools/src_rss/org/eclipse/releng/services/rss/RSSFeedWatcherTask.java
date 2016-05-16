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
package org.eclipse.releng.services.rss;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.ExecTask;

import org.eclipse.releng.generators.rss.RSSFeedUpdateEntryTask;
import org.eclipse.releng.util.rss.Messages;
import org.eclipse.releng.util.rss.RSSFeedUtil;

/**
 * Parameters: 
 *   debug - more output to console - eg., 0|1|2
 *   
 *   file - path to the XML file that will be published - eg., /path/to/file.to.publish.xml
 *   feedURL - URL of the feed where it will be published - eg., http://servername/path/to/feed.xml
 *   
 *   feedWatchActions - semi-comma-separated list of triplets: 
 *   	(Xpath to watch for); (what to execute if condition is met); (commandline args to the executable)...
 *   	eg., to watch for ANY change in the feed and respond by sending email
 *   		/*[name() = 'feed']/*[name() = 'updated']/text(); sendEmailAlert.sh; null
 *      eg., to watch for ANY changes in the current build
 *   		//*[name() = 'entry'][1]/*[name() = 'updated']/text(); sendEmailAlert.sh; null
 *      eg., to watch for changes in the current build's performance test results on linux-gtk
 *      	//*[name() = 'entry'][1]/*[name() = 'summary']/*[@type = 'performance'][1]/*[name() = 'results'][@os = 'linux'][@ws = 'gtk']/text(); sendEmailAlert.sh; null
 *    
 * @author nickb
 *
 */
public class RSSFeedWatcherTask extends Task {

  private int debug = 0;

  private static final String CL = ":"; //$NON-NLS-1$
  private static final String DOT = "."; //$NON-NLS-1$
  private static final String NS = ""; //$NON-NLS-1$
  private static final String SP = " "; //$NON-NLS-1$
  
  private static final String splitter = "[;\t\r\n]+"; //$NON-NLS-1$ 

  private static final String feedWatchActionError = "feedWatchAction.Error"; //$NON-NLS-1$
  private static final String feedWatchActionOuput = "feedWatchAction.Output"; //$NON-NLS-1$
  private static final String feedWatchActionResult = "feedWatchAction.Result"; //$NON-NLS-1$
  private static final String feedWatchActionNewValue = "feedWatchAction.NewValue"; //$NON-NLS-1$
  private static final String feedWatchActionOldValue = "feedWatchAction.OldValue"; //$NON-NLS-1$
  private static final String feedWatchActionTheValue = "feedWatchAction.TheValue"; //$NON-NLS-1$

  private static final RSSFeedUtil util = new RSSFeedUtil();

  //required fields
  private File file;
  private File tmpFile;
  private String feedURL;
  private String[] feedWatchActions = new String[] {};

  //optional
  public void setDebug(int debug) { this.debug = debug; }

  //required
  public void setFile(String file) { 
    if (!isNullString(file)) { 
      this.file = new File(file); 
      this.tmpFile = new File(file + ".tmp");  //$NON-NLS-1$
    }     
  }
  public void setFeedURL(String feedURL) { 
    if (isNullString(feedURL))
    { System.err.println(Messages.getString("RSSFeedCommon.FeedURLError")); }  //$NON-NLS-1$
    else
    { this.feedURL = feedURL; }
  }
  public void setFeedWatchActions(String feedWatchActions) {
    int missingActions = 0;
    if (!isNullString(feedWatchActions)) { 
      this.feedWatchActions = feedWatchActions.split(splitter); 
      missingActions = this.feedWatchActions.length % 3; if (missingActions > 0) { missingActions = 3 - missingActions; }
    }
    if (missingActions > 0) {
      for (int i = 0; i < missingActions; i++)
      {
        System.out.println((i==0 && missingActions==2 ? Messages.getString("RSSFeedWatcherTask.WarningNoScriptAction") : Messages.getString("RSSFeedWatcherTask.WarningNoCommandlineParams")) + SP + feedWatchActions ); //$NON-NLS-1$ //$NON-NLS-2$
        feedWatchActions += "; null"; //$NON-NLS-1$
      }
      this.feedWatchActions = feedWatchActions.split(splitter);
    }
  }

  // The method executing the task
  public void execute() throws BuildException {
    if (debug>0) { util.setDebug(debug); }
    if (file==null || !file.exists() || !file.isFile()) {
      // if there's no local copy of the feed, get a copy, then exit with instructions 
      downloadFeed(file,debug>=0);
      System.out.println(Messages.getString("RSSFeedWatcherTask.PleaseRunThisTaskLater") + SP + file); //$NON-NLS-1$
      System.out.println(Messages.getString("RSSFeedWatcherTask.ToTheLatestVersion") + SP + feedURL); //$NON-NLS-1$
    } else {
      if (feedWatchActions==null || feedWatchActions.length<1) {
        System.err.println(Messages.getString("RSSFeedWatcherTask.ErrorNoWatchActions")); //$NON-NLS-1$
      } else {
        checkFeed();
      }
    }
  }

  private void checkFeed() {
    if (file.isDirectory()) {
      System.err.println(Messages.getString("RSSFeedWatcherTask.ErrorDestinationFileIsADirectory")); //$NON-NLS-1$
    } else {
      downloadFeed(tmpFile,debug>0);
    } 

    if (tmpFile.isFile()) {
      if (debug>0) { System.out.println(Messages.getString("RSSFeedWatcherTask.Compare") + SP + file + Messages.getString("RSSFeedWatcherTask.with") + tmpFile + CL); } //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

      RSSFeedUpdateEntryTask oldFeedWatcher = null;
      RSSFeedUpdateEntryTask newFeedWatcher = null;
      int j=0;
      
      for (int i = 0; i < feedWatchActions.length; i+=3)
      {
        String xpath = feedWatchActions[i].trim();
        String action = feedWatchActions[i+1].trim();
        String commandline = feedWatchActions[i+2].trim();

        oldFeedWatcher = new RSSFeedUpdateEntryTask();
        oldFeedWatcher.setFile(file.toString());
        if (debug>0) { oldFeedWatcher.setDebug(debug); }
        oldFeedWatcher.setXpath(xpath);
        oldFeedWatcher.execute();

        if (oldFeedWatcher.getFoundNode() != null) {
          newFeedWatcher = new RSSFeedUpdateEntryTask();
          newFeedWatcher.setFile(tmpFile.toString());
          if (debug>0) { newFeedWatcher.setDebug(debug); }
          newFeedWatcher.setXpath(xpath);
          newFeedWatcher.execute();

          String oldContent = oldFeedWatcher.getFoundNode().getTextContent();
          String newContent = newFeedWatcher.getFoundNode().getTextContent();

          if (debug>1) {
            System.out.println(Messages.getString("RSSFeedWatcherTask.GotOldNodeContents") + CL + SP + oldContent); //$NON-NLS-1$
            System.out.println(Messages.getString("RSSFeedWatcherTask.GotNewNodeContents") + CL + SP + newContent); //$NON-NLS-1$
          }

          if (!"null".equals(action)) { //$NON-NLS-1$
            commandline = 
              (debug>0?"-debug " + debug + SP:NS) + ("null".equals(commandline)?NS:commandline) + //$NON-NLS-1$ //$NON-NLS-2$ 
              " -feedURL " + feedURL + //$NON-NLS-1$  
              " -xpath \"" + xpath + "\"" + //$NON-NLS-1$ //$NON-NLS-2$
              " -oldvalue \"" + oldContent + "\"" + //$NON-NLS-1$ //$NON-NLS-2$
              " -newvalue \"" + newContent + "\""; //$NON-NLS-1$ //$NON-NLS-2$
          }

          // store actual value - either the changed value or the original value (if unchanged)
          this.getProject().setProperty(feedWatchActionTheValue + DOT + j,!isNullString(newContent)?newContent:oldContent);

          if (newFeedWatcher.getFoundNode() == null || // changed from exists to not exists, or 
              !oldContent.equals(newContent) // node has changed
          ) {
            // collect property from newNode and pass it to THIS task so that the local ant script can see it 
            if (!isNullString(oldContent)) { this.getProject().setProperty(feedWatchActionOldValue + DOT + j,oldContent); }
            if (!isNullString(newContent)) { this.getProject().setProperty(feedWatchActionNewValue + DOT + j,newContent); }
            
            if (!"null".equals(action)) { //$NON-NLS-1$
              System.out.println(Messages.getString("RSSFeedWatcherTask.RunExecTask") + CL + SP + action + SP + commandline); //$NON-NLS-1$ 
              ExecTask exec = util.runExecTask((new File(action)).getAbsolutePath(), commandline, null);

              // collect properties from exec task and pass them to THIS task so that the local ant script can see them 
              String out = null;
              
              out = exec.getProject().getProperty(RSSFeedUtil.RUN_EXEC_TASK_ERROR);
              if (!isNullString(out)) { this.getProject().setProperty(feedWatchActionError + DOT + j, out); }
              
              out = exec.getProject().getProperty(RSSFeedUtil.RUN_EXEC_TASK_RESULT);
              if (!isNullString(out)) { this.getProject().setProperty(feedWatchActionOuput + DOT + j, out); }
              
              out = exec.getProject().getProperty(RSSFeedUtil.RUN_EXEC_TASK_RESULT);
              if (!RSSFeedUtil.EXPECTED_RESULT.equals(out)) { this.getProject().setProperty(feedWatchActionResult + DOT + j, out); }
            } 
          } else {
            System.out.println(Messages.getString("RSSFeedWatcherTask.NodeUnchanged")); //$NON-NLS-1$
          }
        } else {
          System.out.println(Messages.getString("RSSFeedWatcherTask.NodeNotFound")); //$NON-NLS-1$
        }
        j++;
      }

      try
      {
        RSSFeedUtil.transferData(new FileInputStream(tmpFile), new FileOutputStream(file));
        tmpFile.deleteOnExit();
      }
      catch (FileNotFoundException e)
      {
        e.printStackTrace();
      }
      catch (IOException e)
      {
        e.printStackTrace();
      }
    }
  }

  private void downloadFeed(File destFile, boolean verbose) 
  {
    try
    {
      if (verbose) {
        System.out.println(Messages.getString("RSSFeedWatcherTask.Download") + CL + SP + feedURL); //$NON-NLS-1$
        System.out.println(Messages.getString("RSSFeedWatcherTask.To") + CL + SP + destFile + SP); //$NON-NLS-1$
      }
      RSSFeedUtil.transferData((new URL(feedURL)).openStream(), new FileOutputStream(destFile));
      if (verbose) {
        System.out.println(Messages.getString("RSSFeedWatcherTask.Done")); //$NON-NLS-1$
        System.out.println(SP);
      }
    }
    catch (MalformedURLException e)
    {
      e.printStackTrace();
    }
    catch (FileNotFoundException e)
    {
      e.printStackTrace();
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
  }

  private static boolean isNullString(String str)
  {
    return RSSFeedUtil.isNullString(str);
  }

}