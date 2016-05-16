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
package org.eclipse.releng.util.rss;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.ExecTask;
import org.apache.tools.ant.types.Commandline.Argument;

/**
 *
 *    Helper methods
 *    
 * @author nickb
 *
 */
public class RSSFeedUtil {

  private static final String SP = " "; //$NON-NLS-1$
  private static final String CL = ":"; //$NON-NLS-1$
  public static final String EXPECTED_RESULT = "0"; //$NON-NLS-1$

  public static final String RUN_EXEC_TASK_ERROR = "runExecTask.Error"; //$NON-NLS-1$
  public static final String RUN_EXEC_TASK_OUTPUT = "runExecTask.Output"; //$NON-NLS-1$
  public static final String RUN_EXEC_TASK_RESULT = "runExecTask.Result"; //$NON-NLS-1$

  private int debug = 0;

  /**
   * A buffer.
   */
  private static byte[] buffer = new byte [8192];

  public ExecTask runExecTask(String executable, String commandline, String dir)
  {
    if (dir==null) {
      dir = ".";  //$NON-NLS-1$
    }

    ExecTask exec = new ExecTask();
    exec.setExecutable(executable);
    exec.setResolveExecutable(true);
    exec.setDir((new File(dir)).getAbsoluteFile());
    Project project = new Project(); project.setName(executable);
    exec.setProject(project);
    exec.setFailIfExecutionFails(true);
    exec.setFailonerror(true);
    exec.setErrorProperty(RUN_EXEC_TASK_ERROR);
    exec.setOutputproperty(RUN_EXEC_TASK_OUTPUT);
    exec.setResultProperty(RUN_EXEC_TASK_RESULT);
    exec.setLogError(true);

    if (commandline != null || "".equals(commandline)) { //$NON-NLS-1$
      Argument execArg = exec.createArg();
      execArg.setLine(commandline);
    }
    try
    {
      if (debug>0) { 
        System.out.println(Messages.getString("RSSFeedPublisherTask.Execute") + SP + executable + (commandline==null?"":SP + commandline)); //$NON-NLS-1$ //$NON-NLS-2$
      }
      exec.execute();
      handleExecTaskReturn(project);
    }
    catch (Exception e)
    {
      handleExecTaskReturn(project);
      System.err.println(Messages.getString("RSSFeedPublisherTask.ForProject") + SP + project.getName() + CL); //$NON-NLS-1$
      e.printStackTrace();
    }
    
    return exec;

  }

  private void handleExecTaskReturn(Project project)
  {
    String out = null;
    
    out = project.getProperty(RUN_EXEC_TASK_RESULT);
    if (debug>1) {
      if (!isNullString(out) && !EXPECTED_RESULT.equals(out)) { 
        System.err.println(Messages.getString("RSSFeedPublisherTask.Result") + SP + out); //$NON-NLS-1$
      }
    }
    
    out = project.getProperty(RUN_EXEC_TASK_OUTPUT);
    if (!isNullString(out)) { 
      System.out.println(out);
    }
    
    out = project.getProperty(RUN_EXEC_TASK_ERROR);
    if (!isNullString(out)) {
      if (debug>1 && out.equals(Messages.getString("RSSFeedPublisherTask.CVSWarning"))) { //$NON-NLS-1$
        System.out.println(out);
      } else if (!out.equals(Messages.getString("RSSFeedPublisherTask.CVSWarning"))) { //$NON-NLS-1$
        System.err.println(Messages.getString("RSSFeedPublisherTask.Error") + SP + out); //$NON-NLS-1$
      }
    }
  }

  /**
   * Copies all bytes in the given source stream to the given destination
   * stream. Neither streams are closed. 
   * 
   * From: org.eclipse.emf/tests/org.eclipse.emf.test.build/src/org/eclipse/emf/test/build/FileTool.java,v 1.2
   * 
   * @param source
   *            the given source stream
   * @param destination
   *            the given destination stream
   */
  public static void transferData(InputStream source, OutputStream destination) throws IOException
  {
    int bytesRead = 0;
    while (bytesRead != -1)
    {
      bytesRead = source.read(buffer, 0, buffer.length);
      if (bytesRead != -1)
      {
        destination.write(buffer, 0, bytesRead);
      }
    }
  }

  public static boolean isNullString(String str)
  {
    return str==null||"".equals(str); //$NON-NLS-1$
  }

  public void setDebug(int debug)
  {
    this.debug = debug;
  }

}