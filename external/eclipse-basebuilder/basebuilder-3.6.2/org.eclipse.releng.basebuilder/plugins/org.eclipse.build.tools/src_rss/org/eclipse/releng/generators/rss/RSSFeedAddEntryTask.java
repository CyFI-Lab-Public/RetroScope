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

//TODO: bug - can't run CreateFeed and AddEntry together when debug=2 - file locking problem?

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.util.DateUtils;

import org.eclipse.releng.util.rss.Messages;
import org.eclipse.releng.util.rss.RSSFeedUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * Parameters: 
 *   debug      - more output to console - eg., 0|1|2
 *   
 *   file       - path to the XML file that will be created - eg., /path/to/file.to.create.xml
 *   project    - project's name, used to label the feed - eg., Eclipse, EMF, UML2
 *   branch     - build's branch, eg., 2.2.0
 *   buildID    - build's ID, eg., S200605051234
 *   feedURL    - URL of the feed where it will be published - eg., http://servername/path/to/feed.xml
 *      note that feedURL is not required if the feed already exists, only if a new feed file must be created
 *   buildURL   - URL of the build being added to the feed - eg., http://servername/path/to/project/branch/buildID/
 *
 *   buildAlias - build's alias, eg., 2.2.0RC2
 *   
 *   dependencyURLs   - upstream dependencies, eg., UML2 depends on emf and eclipse, so specify TWO URLs in properties file or ant task
 *   
 *   releaseNotesURL  - URL of the build's release notes page - eg., http://www.eclipse.org/project/news/release-notes.php 
 *   updateManagerURL - URL of the build's Update Manager site - eg., http://servername/path/to/project/updates/
 *   downloadsURL     - URL of the build's downloads - eg., http://servername/path/to/project/downloads/
 *   
 *   jarSigningStatus - code to define jar signing status - eg., one of:
 *      NONE (or '')  - no status available or not participating 
 *      UNSIGNED      - no jar signage available or done yet
 *      SIGNREADY     - jars promoted to eclipse.org, ready for signing
 *      BUILDREADY    - signed on eclipse.org, ready to be collected and bundled as zips and copied to UM site
 *      SIGNED        - signed & bundled on download page and on UM site
 *
 *   callistoStatus   - code to define Callisto status, eg., one of:
 *      NONE (or '')         - not part of Callisto or unknown status
 *      BUILDCOMPLETE        - Have you finished your RC1 bits?
 *      2006-05-02T20:50:00Z - When do you expect to finish them?
 *      TPTP                 - If you're waiting for another project, which one(s)? (TPTP is just an example)
 *      UMSITEREADY          - Have you placed those bits in your update site?
 *      CALLISTOSITEREADY    - Have you updated the features.xml file in the Callisto CVS directory?
 *      COMPLETE             - Are you ready for RC1 to be declared?
 *   
 *   buildType - code to define type of build, eg., one of: 
 *      N      - Nightly
 *      I      - Integration
 *      M      - Maintenance
 *      S      - Stable (Milestone or Release Candidate)
 *      R      - Release
 *      MC     - Maintenance-Callisto
 *      SC     - Stable-Callisto
 *      RC     - Release-Callisto
 *   
 *   Releases           - comma or space-separated list of releases in quints of os,ws,arch,type/name,filename,... 
 *                      - eg., win32,win,x86,SDK,eclipse-SDK-3.2RC5-win32.zip,linux,gtk,x86_64,SDK,eclipse-SDK-3.2RC5-linux-gtk.tar.gz   
 *                      - (for examples and definitions of ws, os + arch, see below) 
 *
 *   JUnitTestURL       - URL of the build's JUnit test results - eg., http://servername/path/to/project/branch/buildID/testResults.php
 *   performanceTestURL - URL of the build's performance tests - eg., http://servername/path/to/project/branch/buildID/performance/performance.php
 *   APITestURL         - URL of the build's API test results - eg., http://servername/path/to/project/branch/buildID/testResults.php
 *   
 *   JUnitTestResults       - comma or space-separated list of test results in quads of os,ws,arch,status,os,ws,status,arch,... - eg., win32,win,x86,PASS,linux,gtk,x86,PASS
 *   performanceTestResults - comma or space-separated list of test results in quads of os,ws,arch,status,os,ws,status,arch,... - eg., win32,win,x86_64,PASS,linux,gtk,x86_64,PASS
 *   APITestResults         - comma or space-separated list of test results in quads of os,ws,arch,status,os,ws,status,arch,... - eg., win32,win,ppc,PASS,linux,gtk,ppc,PASS
 *      ws     - window system - eg., ALL, win32, win64, linux, macos...
 *      os     - operating system - eg., ALL, win, gtk, motif, carbon, ...
 *      arch   - architecture, eg., ALL, x86, x86_64, ppc, ...
 *      status - status code for test results - eg., one of: PASS, PENDING, FAIL, UNKNOWN, SKIPPED
 *      
 * @author nickb
 *
 */
public class RSSFeedAddEntryTask extends Task {

  private int debug = 0;

  private static final String now = getTimestamp();

  //$ANALYSIS-IGNORE codereview.java.rules.portability.RulePortabilityLineSeparators
  private static final String NL="\n"; //$NON-NLS-1$
  private static final String NS = ""; //$NON-NLS-1$
  private static final String SEP = "----"; //$NON-NLS-1$ 
  private static final String SP = " "; //$NON-NLS-1$

  private static final String splitter = "[,\t " + NL + "]+"; //$NON-NLS-1$ //$NON-NLS-2$

  //required fields
  private File file;
  private String project;
  private String branch;
  private String buildID;
  private String feedURL;
  private String buildURL;

  //optional
  private String buildAlias;

  //optional
  private String[] dependencyURLs = new String[] {};

  //optional
  private String releaseNotesURL;
  private String updateManagerURL;
  private String downloadsURL;
  private String jarSigningStatus;
  private String callistoStatus;
  private String buildType;

  //optional
  private String[] releases = new String[] {};

  //optional
  private String JUnitTestURL;     
  private String performanceTestURL;
  private String APITestURL;
  private String[] JUnitTestResults;
  private String[] performanceTestResults;
  private String[] APITestResults;

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
  public void setBranch(String branch) { 
    if (isNullString(branch))
    { System.err.println(Messages.getString("RSSFeedAddEntryTask.BranchError")); }  //$NON-NLS-1$
    else
    { this.branch = branch; }
  }
  public void setBuildID(String buildID) { 
    if (isNullString(buildID))
    { System.err.println(Messages.getString("RSSFeedAddEntryTask.BuildIDError")); }  //$NON-NLS-1$
    else
    { this.buildID = buildID; }
  }
  public void setFeedURL(String feedURL) { 
    if (isNullString(feedURL))
    { System.err.println(Messages.getString("RSSFeedCommon.FeedURLError")); }  //$NON-NLS-1$
    else
    { this.feedURL = feedURL; }
  }
  public void setBuildURL(String buildURL) { 
    if (isNullString(buildURL))
    { System.err.println(Messages.getString("RSSFeedAddEntryTask.BuildURLError")); }  //$NON-NLS-1$
    else
    { this.buildURL = buildURL; }
  }

  //optional: alias is usually something like "3.2.0M6"
  public void setBuildAlias(String buildAlias) { this.buildAlias = buildAlias; }

  //optional: upstream dependencies, eg., UML2 depends on emf and eclipse, so specify TWO URLs in properties file or ant task
  public void setDependencyURLs(String dependencyURLs) { if (!isNullString(dependencyURLs)) { this.dependencyURLs = dependencyURLs.split(splitter); } }

  //optional: define releases available in this build for a series of operating systems, windowing systems, and type
  public void setReleases(String releases) { if (!isNullString(releases)) { this.releases = releases.split(splitter); } }

  //optional: informational links to release notes, downloads, update manager
  public void setReleaseNotesURL(String releaseNotesURL) { this.releaseNotesURL = releaseNotesURL; }
  public void setUpdateManagerURL(String updateManagerURL) { this.updateManagerURL = updateManagerURL; }
  public void setDownloadsURL(String downloadsURL) { this.downloadsURL = downloadsURL; }
  public void setJarSigningStatus(String jarSigningStatus) { this.jarSigningStatus = jarSigningStatus; }
  public void setCallistoStatus(String callistoStatus) { this.callistoStatus = callistoStatus; }
  public void setBuildType(String buildType) {
    if (!isNullString(buildType)) 
    {
      this.buildType = buildType;
    }
    else
    {
      this.buildType = buildID.replaceAll("[^NIMSR]", NS); //$NON-NLS-1$
      if (this.buildType.length()>1) 
      {
        this.buildType=this.buildType.substring(0, 1);
      }
    }

  }

  //optional: test URLs and results
  public void setJUnitTestURL(String JUnitTestURL) { this.JUnitTestURL = JUnitTestURL; }
  public void setPerformanceTestURL(String performanceTestURL) { this.performanceTestURL = performanceTestURL; }
  public void setAPITestURL(String APITestURL) { this.APITestURL = APITestURL; }
  public void setJUnitTestResults(String JUnitTestResults) { if (!isNullString(JUnitTestResults)) { this.JUnitTestResults = JUnitTestResults.split(splitter); } }
  public void setPerformanceTestResults(String performanceTestResults) { if (!isNullString(performanceTestResults)) { this.performanceTestResults = performanceTestResults.split(splitter); } }
  public void setAPITestResults(String APITestResults) { if (!isNullString(APITestResults)) { this.APITestResults = APITestResults.split(splitter); } }

  // The method executing the task
  public void execute() throws BuildException {
    if (debug>0) { 
      System.out.println(Messages.getString("RSSFeedAddEntryTask.AddingEntryTo") + project + SP + Messages.getString("RSSFeedCommon.RSSFeedFile") + SP + file.toString() + ", " + Messages.getString("RSSFeedCommon.ToBePublishedAt") + feedURL); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
    }
    updateFeedXML(file); // load previous
  }

  //$ANALYSIS-IGNORE codereview.java.rules.exceptions.RuleExceptionsSpecificExceptions
  private void updateFeedXML(File file){
    if (!file.exists()) {
      System.out.println(Messages.getString("RSSFeedCommon.RSSFeedFile") + SP + file.toString() + SP + Messages.getString("RSSFeedAddEntryTask.DoesNotExist")); //$NON-NLS-1$ //$NON-NLS-2$
      RSSFeedCreateFeedTask creator=new RSSFeedCreateFeedTask();
      creator.setFile(file.toString());
      creator.setFeedURL(feedURL);
      creator.setProject(project);
      creator.setDebug(debug);
      creator.execute();
    }
    DocumentBuilderFactory documentBuilderFactory=DocumentBuilderFactory.newInstance();
    documentBuilderFactory.setNamespaceAware(true);
    DocumentBuilder documentBuilder=null;
    try {
      documentBuilder=documentBuilderFactory.newDocumentBuilder();
    }
    catch (ParserConfigurationException e) {
      e.printStackTrace();
    }
    Document document=null;
    try {
      document=documentBuilder.parse(file);
    }
    catch (SAXException e) {
      e.printStackTrace();
    }
    catch (IOException e) {
      e.printStackTrace();
    }

    Transformer transformer = null;
    try {
      transformer = createTransformer("UTF-8"); //$NON-NLS-1$
    } catch (TransformerException e) {
      e.printStackTrace();
    }

    Element element=document.getDocumentElement();
    for (Node child=element.getFirstChild(); child != null; child=child.getNextSibling()) {
      if ("updated".equals(child.getLocalName())) { //$NON-NLS-1$
        if (debug > 0) {
          System.out.println(Messages.getString("RSSFeedCommon.Set") + " <" + child.getLocalName()+ ">"+ now+ "</"+ child.getLocalName()+ ">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
        }
        ((Element)child).setTextContent(now);
      }
      else if ("id".equals(child.getLocalName())) { //$NON-NLS-1$
        Node newNode=createEntry(document);
        if (debug > 0) {
          System.out.println(Messages.getString("RSSFeedAddEntryTask.AttachNew") + " <entry/>"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        try {
          if (debug > 0) {
            System.out.println(SEP); //$NON-NLS-1$
            transformer.transform(new DOMSource(newNode),new StreamResult(System.out));
            System.out.println(SEP); //$NON-NLS-1$
          }
        }
        catch (TransformerException e) {
          e.printStackTrace();
        }
        Node refNode=child.getNextSibling();
        element.insertBefore(document.createTextNode(NL + "  "),refNode); //$NON-NLS-1$
        element.insertBefore(newNode,refNode);
        break;
      }
    }
    try {
      transformer.transform(new DOMSource(document),new StreamResult(new OutputStreamWriter(new FileOutputStream(file))));
      if (debug > 1) {
        System.out.println(SEP); //$NON-NLS-1$
        transformer.transform(new DOMSource(document),new StreamResult(System.out));
        System.out.println(SEP); //$NON-NLS-1$
      }
    }
    catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    catch (TransformerException e) {
      e.printStackTrace();
    }
  }


  private Element createEntry(Document document) {

//  <entry>
    Element entry =  document.createElement("entry"); //$NON-NLS-1$

    String[] txt = { NL + "  ", NL + "    ", NL + "      ", NL + "        ", NL + "          " , NL + "            " }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
    Element elem = null;

    String projectVersionString = project + SP + (!isNullString(buildAlias)?  //$NON-NLS-1$
      (buildAlias.startsWith(branch) ? 
        buildAlias + " (" + buildID + ")" :                   // 2.2.0RC2 (S200605051234) //$NON-NLS-1$ //$NON-NLS-2$
          buildAlias + " (" + branch + "." + buildID + ")") : // Foobar (2.2.0.S200605051234)  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            branch + SP + buildID);                                // 2.2.0.S200605051234 //$NON-NLS-1$

    doVarSubs();

//  <title>[announce] " + project + SP + branch + SP + buildID + " is available</title>
    elem = document.createElement("title"); //$NON-NLS-1$
    elem.setTextContent(Messages.getString("RSSFeedAddEntryTask.AnnouncePrefix") + projectVersionString + SP + Messages.getString("RSSFeedAddEntryTask.IsAvailable")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    attachNode(document, entry, elem, txt[1]);

//  <link href=\"" + buildURL + "\"/>
    elem = document.createElement("link"); //$NON-NLS-1$
    elem.setAttribute("href", !isNullString(buildURL) ? buildURL : projectVersionString); //$NON-NLS-1$
    attachNode(document, entry, elem, txt[1]);

//  <id>" + buildURL + "</id>
    elem = document.createElement("id"); //$NON-NLS-1$
    elem.setTextContent(!isNullString(buildURL) ? buildURL : projectVersionString);
    attachNode(document, entry, elem, txt[1]);

//  <updated>" + getTimestamp() + "</updated>
    elem = document.createElement("updated"); //$NON-NLS-1$
    elem.setTextContent(now);
    attachNode(document, entry, elem, txt[1]);

//  <summary>
    Element summary = document.createElement("summary"); //$NON-NLS-1$
    attachNode(document, entry, summary, txt[1]);

//  <build callisto="" jars="" type="" href="" xmlns="http://www.eclipse.org/2006/BuildFeed">
    Element build = document.createElement("build"); //$NON-NLS-1$
    build.setAttribute("jars", jarSigningStatus); //$NON-NLS-1$
    build.setAttribute("callisto", callistoStatus); //$NON-NLS-1$
    build.setAttribute("type", buildType); //$NON-NLS-1$
    build.setAttribute("xmlns", "http://www.eclipse.org/2006/BuildFeed"); //$NON-NLS-1$ //$NON-NLS-2$
    if (!isNullString(buildURL)) {
      build.setAttribute("href",buildURL); //$NON-NLS-1$
    }
    attachNode(document, summary, build, txt[2]);

//  <update>" + usiteURL + "</update>
    if (!isNullString(updateManagerURL)) {
      elem = document.createElement("update"); //$NON-NLS-1$
      elem.setTextContent(updateManagerURL);
      attachNode(document, build, elem, txt[3]);
    }

//  <downloads>" + dropsURL + "</downloads>
    if (!isNullString(downloadsURL)) {
      elem = document.createElement("downloads"); //$NON-NLS-1$
      elem.setTextContent(downloadsURL);
      attachNode(document, build, elem, txt[3]);
    }

//  <releasenotes>" + releaseNotesURL + "</releasenotes>
    if (!isNullString(releaseNotesURL)) {
      elem = document.createElement("releasenotes"); //$NON-NLS-1$
      elem.setTextContent(releaseNotesURL);
      attachNode(document, build, elem, txt[3]);
    }

//  <releases>
//    <release os="" ws="" type=""> + filename + </release>
    if (releases!=null && releases.length>0) {
      if (releases.length % 5 != 0) { 
        System.err.println(Messages.getString("RSSFeedAddEntryTask.WrongNumberOfVariables") + SP + Messages.getString("RSSFeedAddEntryTask.MustBeMultipleOf5") + SP + Messages.getString("RSSFeedAddEntryTask.InProperty") + SP + "releases"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
      } 
      Element releasesElem = document.createElement("releases"); //$NON-NLS-1$
      for (int i = 0; i < releases.length; i+=5)
      {
        Element release = document.createElement("release"); //$NON-NLS-1$
        release.setAttribute("os", releases[i]); //$NON-NLS-1$
        release.setAttribute("ws", releases[i+1]); //$NON-NLS-1$
        release.setAttribute("arch", releases[i+2]); //$NON-NLS-1$
        release.setAttribute("type", releases[i+3]); //$NON-NLS-1$
        release.setTextContent(varSub(releases[i+4]));
        attachNode(document, releasesElem, release, txt[4]);          
      }
      attachNode(document, build, releasesElem, txt[3]);
    }

//  <tests>
    Element tests = document.createElement("tests"); //$NON-NLS-1$

//    <test type=\"junit\" href=\"" + JUnitTestURL + "\"/>
    if (!isNullString(JUnitTestURL)) {
      Element test = document.createElement("test"); //$NON-NLS-1$
      test.setAttribute("type", "junit"); //$NON-NLS-1$ //$NON-NLS-2$
      test.setAttribute("href", JUnitTestURL); //$NON-NLS-1$
      if (JUnitTestResults!=null && JUnitTestResults.length>0) {
        if (JUnitTestResults.length % 4 != 0) { 
          System.err.println(Messages.getString("RSSFeedAddEntryTask.WrongNumberOfVariables") + SP + Messages.getString("RSSFeedAddEntryTask.MustBeMultipleOf4") + SP + Messages.getString("RSSFeedAddEntryTask.InProperty") + SP + "JUnitTestResults"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        }
        for (int i = 0; i < JUnitTestResults.length; i+=4)
        {
          Element result = document.createElement("result"); //$NON-NLS-1$
          result.setAttribute("os", JUnitTestResults[i]); //$NON-NLS-1$
          result.setAttribute("ws", JUnitTestResults[i+1]); //$NON-NLS-1$
          result.setAttribute("arch", JUnitTestResults[i+2]); //$NON-NLS-1$
          result.setTextContent(JUnitTestResults[i+3]);
          attachNode(document, test, result, txt[5]);          
        }
        // extra space to close containing tag
        elem.appendChild(document.createTextNode(txt[4]));
      }
      attachNode(document, tests, test, txt[4]);
    }

//    <test type=\"performance\" href=\"" + performanceTestURL + "\"/>
    if (!isNullString(performanceTestURL)) {
      Element test = document.createElement("test"); //$NON-NLS-1$
      test.setAttribute("type", "performance"); //$NON-NLS-1$ //$NON-NLS-2$
      test.setAttribute("href", performanceTestURL); //$NON-NLS-1$
      if (performanceTestResults!=null && performanceTestResults.length>0) {
        if (performanceTestResults.length % 4 != 0) { 
          System.err.println(Messages.getString("RSSFeedAddEntryTask.WrongNumberOfVariables") + SP + Messages.getString("RSSFeedAddEntryTask.MustBeMultipleOf4") + SP + Messages.getString("RSSFeedAddEntryTask.InProperty") + SP + "performanceTestResults"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        }
        for (int i = 0; i < performanceTestResults.length; i+=4)
        {
          Element result = document.createElement("result"); //$NON-NLS-1$
          result.setAttribute("os", performanceTestResults[i]); //$NON-NLS-1$
          result.setAttribute("ws", performanceTestResults[i+1]); //$NON-NLS-1$
          result.setAttribute("arch", performanceTestResults[i+2]); //$NON-NLS-1$
          result.setTextContent(performanceTestResults[i+3]);
          attachNode(document, test, result, txt[5]);          
        }
        // extra space to close containing tag
        test.appendChild(document.createTextNode(txt[4]));
      }
      attachNode(document, tests, test, txt[4]);
    }

//    <test type=\"performance\" href=\"" + performanceTestURL + "\"/>
    if (!isNullString(APITestURL)) {
      Element test = document.createElement("test"); //$NON-NLS-1$
      test.setAttribute("type", "api"); //$NON-NLS-1$ //$NON-NLS-2$
      test.setAttribute("href", APITestURL); //$NON-NLS-1$
      if (APITestResults!=null && APITestResults.length>0) {
        if (APITestResults.length % 4 != 0) { 
          System.err.println(Messages.getString("RSSFeedAddEntryTask.WrongNumberOfVariables") + SP + Messages.getString("RSSFeedAddEntryTask.MustBeMultipleOf4") + SP + Messages.getString("RSSFeedAddEntryTask.InProperty") + SP + "APITestResults"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        }
        for (int i = 0; i < APITestResults.length; i+=4)
        {
          Element result = document.createElement("result"); //$NON-NLS-1$
          result.setAttribute("os", APITestResults[i]); //$NON-NLS-1$
          result.setAttribute("ws", APITestResults[i+1]); //$NON-NLS-1$
          result.setAttribute("arch", APITestResults[i+2]); //$NON-NLS-1$
          result.setTextContent(APITestResults[i+3]);
          attachNode(document, tests, result, txt[5]);          
        }
        // extra space to close containing tag
        test.appendChild(document.createTextNode(txt[4]));
      }
      attachNode(document, tests, test, txt[4]);
    }

    attachNode(document, build, tests, txt[3]);

    if (dependencyURLs!=null && dependencyURLs.length>0) {
  //  <dependencies>
  //    <dependency>" + dependencyURL + "</dependency>
      Element dependencies = document.createElement("dependencies"); //$NON-NLS-1$
      for (int i = 0; i < dependencyURLs.length; i++)
      {
        elem = document.createElement("dependency"); //$NON-NLS-1$
        elem.setTextContent(dependencyURLs[i]);
        attachNode(document, dependencies, elem, txt[4]);
      }
      attachNode(document, build, dependencies, txt[3]);
    }
    
    return entry;
  }

  //$ANALYSIS-IGNORE codereview.java.rules.exceptions.RuleExceptionsSpecificExceptions
  private void attachNode(Document document,Element entry,Element elem,String txt){
    entry.appendChild(document.createTextNode(txt));
    entry.appendChild(elem);
  }

  private static String getTimestamp() { // eg., 2006-04-10T20:40:08Z
    return DateUtils.format(new Date(), DateUtils.ISO8601_DATETIME_PATTERN) + "Z";  //$NON-NLS-1$
  }

  private void doVarSubs()
  {
    feedURL = varSub(feedURL);
    buildURL = varSub(buildURL);

    releaseNotesURL = varSub(releaseNotesURL);
    updateManagerURL = varSub(updateManagerURL);
    downloadsURL = varSub(downloadsURL);

    JUnitTestURL = varSub(JUnitTestURL);          
    performanceTestURL = varSub(performanceTestURL);
    APITestURL = varSub(APITestURL);
  }

  public static Transformer createTransformer(String encoding) throws TransformerException
  {
    TransformerFactory transformerFactory = TransformerFactory.newInstance();

    try
    {
      transformerFactory.setAttribute("indent-number", new Integer(2)); //$NON-NLS-1$
    }
    catch (IllegalArgumentException exception)
    {
    }

    Transformer transformer = transformerFactory.newTransformer();

    transformer.setOutputProperty(OutputKeys.INDENT, "yes"); //$NON-NLS-1$
    transformer.setOutputProperty(OutputKeys.METHOD, "xml"); //$NON-NLS-1$

    // Unless a width is set, there will be only line breaks but no indentation.
    // The IBM JDK and the Sun JDK don't agree on the property name,
    // so we set them both.
    //
    transformer.setOutputProperty("{http://xml.apache.org/xalan}indent-amount", "2"); //$NON-NLS-1$ //$NON-NLS-2$
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2"); //$NON-NLS-1$ //$NON-NLS-2$
    if (encoding != null)
    {
      transformer.setOutputProperty(OutputKeys.ENCODING, encoding);
    }
    return transformer;
  }

  /*
   * variable substitution in URLs - eg., replace %%branch%% and %%buildID%% in buildURL
   */
  private String varSub(String urlstring)
  {
    if (!isNullString(urlstring) && urlstring.indexOf("%%")>=0) //$NON-NLS-1$
    {
      return urlstring.replaceAll(Messages.getString("RSSFeedAddEntryTask.BranchKeyword"), branch).replaceAll(Messages.getString("RSSFeedAddEntryTask.BuildIDKeyword"), buildID).replaceAll(Messages.getString("RSSFeedAddEntryTask.BuildAliasKeyword"), buildAlias); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }
    return urlstring;
  }

  private static boolean isNullString(String str)
  {
    return RSSFeedUtil.isNullString(str);
  }

}