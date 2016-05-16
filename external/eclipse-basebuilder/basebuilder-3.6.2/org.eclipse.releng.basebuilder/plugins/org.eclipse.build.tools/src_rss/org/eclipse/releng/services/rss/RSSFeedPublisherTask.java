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

//TODO: enable support for running task on Windows (problems with ssh, scp, cvs)
//TODO: enable support for connecting to Windows server? (`mkdir -p` not supported)

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import org.eclipse.releng.util.rss.Messages;
import org.eclipse.releng.util.rss.RSSFeedUtil;

/**
 * Parameters: 
 *   debug - more output to console - eg., 0|1|2
 *   
 *   file - path to the XML file that will be published - eg., /path/to/file.to.publish.xml
 *   
 *   cvsExec - path to the executable for cvs, eg., /usr/bin/cvs
 *   cvsRoot - cvs root used to commit the file - eg., username@cvsserver:/cvsroot/path 
 *   cvsPath - cvs module to update - eg., project/news/ (into which builds.xml would go)
 *   cvsTemp - path to the temp folder to use for cvs checkout
 * 
 *   scpExec - path to the executable for scp, eg., /usr/bin/scp
 *   scpTarget - scp target path for publishing the file - eg., username@server:/path/to/target/file.xml
 *   
 * Optionally, if the target folder might not exist, you can use ssh to create it before scp'ing
 *   sshExec - path to the executable for ssh, eg., /usr/bin/ssh
 *    
 * @author nickb
 *
 */
public class RSSFeedPublisherTask extends Task {

  private int debug = 0;

  private static final String CL = ":"; //$NON-NLS-1$
  private static final String FS = File.separator;
  private static final String SP = " "; //$NON-NLS-1$

  // default values for optional fields 
  private static final String DEFAULT_CVSTemp = "/tmp/tmp-RSSFeedPublisherTask"; //$NON-NLS-1$
  private static final String DEFAULT_CVSExec = "cvs"; //$NON-NLS-1$
  private static final String DEFAULT_SCPExec = "scp"; //$NON-NLS-1$

  private static final RSSFeedUtil util = new RSSFeedUtil();

  //required fields
  private File file;

  // required if doing CVS
  private String CVSExec;
  private String CVSRoot;
  private String CVSPath;
  private String CVSTemp;

  // required if doing SCP
  private String SCPExec;
  private String SCPTarget;

  // required if doing SCP and target dir may not already exist
  private String SSHExec;

  //optional
  public void setDebug(int debug) { this.debug = debug; }

  //required
  public void setFile(String file) { 
    if (!isNullString(file)) { this.file = new File(file); }     
  }

  //required for CVS commit (with default)
  public void setCVSExec(String CVSExec) { 
    if (!isNullString(CVSExec)) { 
      this.CVSExec = CVSExec; 
    } else { 
      this.CVSExec = DEFAULT_CVSExec; 
    }
  }

  //required for CVS commit
  public void setCVSRoot(String CVSRoot) { this.CVSRoot = CVSRoot; }
  public void setCVSPath(String CVSPath) { this.CVSPath = CVSPath; }

  //required for CVS commit (with default)
  public void setCVSTemp(String CVSTemp) { 
    if (!isNullString(CVSTemp)) { 
      this.CVSTemp = CVSTemp; 
    } else { 
      this.CVSTemp = DEFAULT_CVSTemp; 
    }
  }

  //required for CVS commit (with default)
  public void setSCPExec(String SCPExec) {
    if (!isNullString(SCPExec)) { 
      this.SCPExec = SCPExec; 
    } else { 
      this.SCPExec = DEFAULT_SCPExec; 
    }
  }
  public void setSCPTarget(String SCPTarget) { this.SCPTarget = SCPTarget; }

  // required if doing SCP and target dir may not already exist (with default, not assigned)
  public void setSSHExec(String SSHExec) { 
      this.SSHExec = SSHExec; 
  }

  // The method executing the task
  public void execute() throws BuildException {
    
    if (file==null || !file.exists() || !file.isFile()) {
      System.err.println(Messages.getString("RSSFeedPublisherTask.ErrorInvalidFile") + CL + SP + file + "!"); //$NON-NLS-1$ //$NON-NLS-2$
    } else {
      if (debug>0) { System.out.println(Messages.getString("RSSFeedPublisherTask.Publish") + SP + file); } //$NON-NLS-1$
      if ((!isNullString(CVSRoot) && !isNullString(CVSPath)) || !isNullString(SCPTarget)) {
        if ((!isNullString(CVSRoot) && !isNullString(CVSPath))) {
          commitFeedToCVS();
        }
        if (!isNullString(SCPTarget)) {
          publishFeedWithSCP();
        }
      } else {
        System.err.println(Messages.getString("RSSFeedPublisherTask.ErrorNothingToDo")); //$NON-NLS-1$
      }
    }
  }

  private void commitFeedToCVS()
  {
    if (debug>1) {
      System.out.println(Messages.getString("RSSFeedPublisherTask.UsingCVSRoot") + SP + CVSRoot); //$NON-NLS-1$
      System.out.println(Messages.getString("RSSFeedPublisherTask.UsingCVSPath") + SP + CVSPath); //$NON-NLS-1$
    }
//  <!-- 3. get filename (eg., builds.xml) from file (which could include a path, eg. ./data/news/builds.xml) -->
//  <pathconvert property="filename"><path path="${file}"/><mapper type="flatten"/></pathconvert>
    String filename = file.getName();

//  <!-- 4. create target temp folder & check out existing version from CVS -->
//  <mkdir dir="${cvsTemp}"/>
    File CVSTempDir = new File(CVSTemp);
    if (CVSTempDir.isFile()) { // if dir exists as a file, we need a new tmp folder name 
      CVSTemp += ".tmp";  //$NON-NLS-1$
      CVSTempDir = new File(CVSTemp);
    } 
    if (CVSTempDir.isDirectory()) {
      if (!CVSTempDir.delete()) {
        System.err.println(Messages.getString("RSSFeedPublisherTask.ErrorCouldNotDeleteTempFolder") + SP + CVSTempDir); //$NON-NLS-1$
      }
    }
    CVSTempDir.mkdir();
    if (debug>1) {
      System.out.println(Messages.getString("RSSFeedPublisherTask.UsingCVSTemp") + SP + CVSTempDir); //$NON-NLS-1$
    }

//  <exec executable="${cvsExec}" dir="${cvsTemp}"><arg line="-d ${cvsRoot} co -d checkoutDir ${cvsPath}"/></exec>
    runCVSExecTask("co -d checkoutDir" + SP + CVSPath, CVSTemp); //$NON-NLS-1$

//  <!-- 5. check if the file already exists in CVS to see if we need to add it -->
//  <available file="${cvsTemp}/checkoutDir/${filename}" type="file" property="fileInCVS"/>
    File destFile = new File(CVSTemp + FS + "checkoutDir" + FS + filename); //$NON-NLS-1$
    boolean fileInCVS = destFile.isFile();

//  <!-- 6. overwrite CVS copy with new version; or if new, copy file to destination for add then check-in -->
//  <copy file="../${file}" overwrite="true" todir="${cvsTemp}/checkoutDir"/>
    try
    {
      RSSFeedUtil.transferData(new FileInputStream(file),new FileOutputStream(destFile));
    }
    catch (FileNotFoundException e)
    {
      e.printStackTrace();
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }

//  <!-- 7. add to CVS (if new) -->
//  <antcall target="addFileToCVS"/>
//  <!-- 7. Add file to CVS (if file is new) -->
//  <target name="addFileToCVS" depends="init" unless="fileInCVS" description="Add file to CVS (if file is new)">
//  <exec executable="${cvsExec}" dir="${cvsTemp}/checkoutDir"><arg line="-d ${cvsRoot} add ${filename}"/></exec>
//  </target>
    if (!fileInCVS) { 
      runCVSExecTask("add " + filename, CVSTemp + FS + "checkoutDir");  //$NON-NLS-1$ //$NON-NLS-2$
    }

//  <!-- 8. check in file -->
//  <exec executable="${cvsExec}" dir="${cvsTemp}/checkoutDir"><arg line="-d ${cvsRoot} ci -m '' ${filename}"/></exec>
    runCVSExecTask("ci -m '' " + filename, CVSTemp + FS + "checkoutDir"); //$NON-NLS-1$ //$NON-NLS-2$
  }

  private void publishFeedWithSCP()
  {
    if (debug>1) {
      System.out.println(Messages.getString("RSSFeedPublisherTask.PublishToSCPTarget") + SP + SCPTarget); //$NON-NLS-1$
    }

//    <exec executable="${sshExec}"><arg line="${sshMakeDirCommand}"/></exec>
    if (!isNullString(SSHExec) && SCPTarget.indexOf(CL)>0) {
      String userAtHost = SCPTarget.substring(0, SCPTarget.indexOf(CL));
      String targetPath = SCPTarget.substring(SCPTarget.indexOf(CL)+1,SCPTarget.lastIndexOf(FS));
      util.runExecTask(SSHExec, userAtHost + " \"mkdir -p" + SP + targetPath + "\"", null); //$NON-NLS-1$ //$NON-NLS-2$
    }
    
//    <exec executable="${scpExec}" dir="../"><arg line="${file} ${scpTarget}"/></exec>
    util.runExecTask(SCPExec, file.toString() + SP + SCPTarget, null);
  }

  private void runCVSExecTask(String task, String dir)
  {
    util.runExecTask(CVSExec, "-d " + CVSRoot + " -q " + task, dir); //$NON-NLS-1$ //$NON-NLS-2$
  }
  
  private static boolean isNullString(String str)
  {
    return RSSFeedUtil.isNullString(str);
  }

}