/**
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.releng.services.bugzilla;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import org.eclipse.releng.util.bugzilla.Messages;


public class UpdateBugStateTask extends Task
{
  private static final String UTF_8 = "UTF-8"; //$NON-NLS-1$

  private static final String GET = "GET"; //$NON-NLS-1$

  private static final String CTYPE_RDF = "&ctype=rdf"; //$NON-NLS-1$

  private static final String URL_TARGET_MILESTONE = "&target_milestone="; //$NON-NLS-1$

  private static final String URL_CHFIELDTO = "&chfieldto="; //$NON-NLS-1$

  private static final String URL_BUG_STATUS = "&bug_status="; //$NON-NLS-1$

  private static final String HTTPS_BUGS_ECLIPSE_ORG_BUGS_BUGLIST_CGI_PRODUCT = "https://bugs.eclipse.org/bugs/buglist.cgi?product="; //$NON-NLS-1$

  private static final String COOKIE = "Cookie"; //$NON-NLS-1$

  private static final String APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded"; //$NON-NLS-1$

  private static final String CONTENT_TYPE = "Content-type"; //$NON-NLS-1$

  private static final String POST = "POST"; //$NON-NLS-1$

  private static final String HTTPS_BUGS_ECLIPSE_ORG_BUGS_PROCESS_BUG_CGI = "https://bugs.eclipse.org/bugs/process_bug.cgi"; //$NON-NLS-1$

  private static final String BUG_STATUS = "bug_status"; //$NON-NLS-1$

  private static final String AMP = "&"; //$NON-NLS-1$

  private static final String EQ = "="; //$NON-NLS-1$

  private static final String HTTPS_BUGS_ECLIPSE_ORG_BUGS_SHOW_BUG_CGI_ID = "https://bugs.eclipse.org/bugs/show_bug.cgi?id="; //$NON-NLS-1$

  private static final String CTYPE_XML = "&ctype=xml"; //$NON-NLS-1$

  private static final String RESOLVE = "resolve"; //$NON-NLS-1$

  private static final String RESOLUTION = "resolution"; //$NON-NLS-1$

  private static final String KNOB = "knob"; //$NON-NLS-1$

  private static final String LONGDESCLENGTH = "longdesclength"; //$NON-NLS-1$

  private static final String SHORT_DESC = "short_desc"; //$NON-NLS-1$

  private static final String BUG_FILE_LOC = "bug_file_loc"; //$NON-NLS-1$

  private static final String BUG_SEVERITY = "bug_severity"; //$NON-NLS-1$

  private static final String PRIORITY = "priority"; //$NON-NLS-1$

  private static final String OP_SYS = "op_sys"; //$NON-NLS-1$

  private static final String REP_PLATFORM = "rep_platform"; //$NON-NLS-1$

  private static final String TARGET_MILESTONE = "target_milestone"; //$NON-NLS-1$

  private static final String COMPONENT = "component"; //$NON-NLS-1$

  private static final String VERSION = "version"; //$NON-NLS-1$

  private static final String PRODUCT = "product"; //$NON-NLS-1$

  private static final String ID = "id"; //$NON-NLS-1$

  private static final String COMMENT = "comment"; //$NON-NLS-1$

  private static final String PROCESS_BUG = "process_bug"; //$NON-NLS-1$

  private static final String FORM_NAME = "form_name"; //$NON-NLS-1$

  private static final String BUGZILLA_LOGINCOOKIE = "; Bugzilla_logincookie="; //$NON-NLS-1$

  private static final String BUGZILLA_LOGIN = "Bugzilla_login="; //$NON-NLS-1$

  private static final String DIGITS_REGEX = "(\\d+)"; //$NON-NLS-1$

  private static final String COLON = ":"; //$NON-NLS-1$

  private static final String DASH = "-"; //$NON-NLS-1$

  private static final String BUGID_REGEX = "<bz:id(?: nc:parseType=\"Integer\")>(\\d+)</bz:id>"; //$NON-NLS-1$

  private static final String BUILDID_REGEX = "([IMNRS]?-?)(\\d{4})(\\d{2})(\\d{2})-?(\\d{2})(\\d{2})"; //$NON-NLS-1$

  private static final String TIMESTAMP_REGEX = "(\\d{4})(\\d{2})(\\d{2})(\\d{2})(\\d{2})"; //$NON-NLS-1$

  private static final String JS = "Java said:"; //$NON-NLS-1$

  private static final String SP = " "; //$NON-NLS-1$

  private static final String XML_REGEX = "<(\\S+)>([^<]+)</\\1>"; //$NON-NLS-1$

  private static final String NL = "\n"; //$NON-NLS-1$

  private static final String CSO = ", or "; //$NON-NLS-1$

  private static final String CS = ", "; //$NON-NLS-1$

  private static final String BZ_IV = "INVALID"; //$NON-NLS-1$

  private static final String BZ_WF = "WONTFIX"; //$NON-NLS-1$

  private static final String BZ_LT = "LATER"; //$NON-NLS-1$

  private static final String BZ_RM = "REMIND"; //$NON-NLS-1$

  private static final String BZ_WK = "WORKSFORME"; //$NON-NLS-1$

  private static final String BZ_FX = "FIXED"; //$NON-NLS-1$

  private static final String BZ_RE = "REOPENED"; //$NON-NLS-1$

  private static final String BZ_AS = "ASSIGNED"; //$NON-NLS-1$

  private static final String BZ_NEW = "NEW"; //$NON-NLS-1$

  private static final String BZ_UC = "UNCONFIRMED"; //$NON-NLS-1$

  private static final String EMPTY = ""; //$NON-NLS-1$

  private static final String LT = "<"; //$NON-NLS-1$

  private static final String GT = ">"; //$NON-NLS-1$

  private static final String QUOT = "\""; //$NON-NLS-1$

  private static final String APOS = "'"; //$NON-NLS-1$

  private static final String HTML_APOS = "&apos;"; //$NON-NLS-1$

  private static final String HTML_QUOT = "&quot;"; //$NON-NLS-1$

  private static final String HTML_LT = "&lt;"; //$NON-NLS-1$

  private static final String HTML_GT = "&gt;"; //$NON-NLS-1$

  private static final String HTML_NBSP = "&nbsp;"; //$NON-NLS-1$

  private static final String HTML_AMP = "&amp;"; //$NON-NLS-1$

  private int debug;

  private int login;

  private int loginCookie;

  private String product;

  private String status;

  private String buildID;

  private String buildAlias;

  private String milestone;

  private String bugList;

  private String resolution;

  private String endDate;

  private LinkedHashMap trans;

  public UpdateBugStateTask()
  {
    debug = 1;

    login = 0;
    loginCookie = 0;
    product = EMPTY;
    status = EMPTY;
    buildID = EMPTY;
    buildAlias = EMPTY;
    endDate = EMPTY;
    milestone = EMPTY;
    bugList = EMPTY;
    resolution = BZ_FX;

    trans = new LinkedHashMap(8, 0.75f, false);
    trans.put(HTML_APOS, APOS);
    trans.put(HTML_QUOT, QUOT);
    trans.put(HTML_LT, LT);
    trans.put(HTML_GT, GT);
    trans.put(HTML_NBSP, SP);
    trans.put(HTML_AMP, AMP);
  }

  public void setDebug(int d)
  {
    debug = d;
  }

  public void setBugList(String b)
  {
    bugList = b;
  }

  public void setProduct(String p)
  {
    product = p;
  }

  public void setStatus(String s)
  {
    if (s.equals(BZ_UC) || s.equals(BZ_NEW) || s.equals(BZ_AS) || s.equals(BZ_RE))
    {
      status = s;
    }
    else
    {
      throw new BuildException(Messages.getString("UpdateBugStateTask.invalidStatus") + "!" + SP + //$NON-NLS-1$ //$NON-NLS-2$
        Messages.getString("UpdateBugStateTask.expectedOne") + SP + BZ_UC + CS + BZ_NEW + CS + BZ_AS + CSO + BZ_RE); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }
  }

  public void setLogin(int l)
  {
    login = l;
  }

  public void setLoginCookie(int lc)
  {
    loginCookie = lc;
  }

  public void setResolution(String r)
  {
    if (r.equals(BZ_FX) || r.equals(BZ_IV) || r.equals(BZ_WF) || r.equals(BZ_LT) || r.equals(BZ_RM) || r.equals(BZ_WK))
    {
      resolution = r;
    }
    else
    {
      System.err.println(Messages.getString("UpdateBugStateTask.invalidResolution") + "!" + SP + //$NON-NLS-1$ //$NON-NLS-2$
        Messages.getString("UpdateBugStateTask.expected") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        + SP + BZ_FX + CS + BZ_IV + CS + BZ_WF + CS + BZ_LT + CS + BZ_RM + CSO + BZ_WK + SP
        + "(" + Messages.getString("UpdateBugStateTask.default") + COLON + SP + BZ_FX + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }
  }

  public void setEndDate(String t)
  {
    Pattern p = Pattern.compile(TIMESTAMP_REGEX);
    Matcher m = p.matcher(t);
    if (m.matches())
    {
      endDate = m.group(1) + DASH + m.group(2) + DASH + m.group(3) + SP + m.group(4) + COLON + m.group(5);
    }
    else
    {
      throw new BuildException(Messages.getString("UpdateBugStateTask.invalidTimestamp") + COLON + SP + t + "!"); //$NON-NLS-1$ //$NON-NLS-2$
    }
  }

  public void setBuildID(String t)
  {
    Pattern p = Pattern.compile(BUILDID_REGEX);
    Matcher m = p.matcher(t);
    if (m.matches())
    {
      buildID = m.group(1) + m.group(2) + m.group(3) + m.group(4) + m.group(5) + m.group(6);
    }
    else
    {
      throw new BuildException(Messages.getString("UpdateBugStateTask.invalidBuildID") + COLON + SP + t + "!"); //$NON-NLS-1$ //$NON-NLS-2$
    }
  }

  public void setBuildAlias(String b)
  {
    buildAlias = b;
  }

  public void setMilestone(String m)
  {
    milestone = m;
  }

public void execute() throws BuildException
  {
    if (login == 0)
    {
      throw new BuildException(Messages.getString("UpdateBugStateTask.expectingLogin") + "!"); //$NON-NLS-1$ //$NON-NLS-2$
    }
    if (loginCookie == 0)
    {
      throw new BuildException(Messages.getString("UpdateBugStateTask.expectingLogincookie") + "!"); //$NON-NLS-1$ //$NON-NLS-2$
    }
    if (status.equals(EMPTY))
    {
      throw new BuildException(Messages.getString("UpdateBugStateTask.expectingStatus") + "!"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /* we take an explicit list OR do a query, not both */
    if (!bugList.equals(EMPTY) && endDate.equals(EMPTY) && milestone.equals(EMPTY) && product.equals(EMPTY))
    {
      if (debug > 1)
      {
        System.err.println(Messages.getString("UpdateBugStateTask.usingBugList")); //$NON-NLS-1$
      }
      Pattern p = Pattern.compile(DIGITS_REGEX);
      Matcher m = p.matcher(bugList);
      while (m.find())
      {
        int bugID = Integer.parseInt(m.group(1));
        if (debug > 1)
        {
          System.err.println(Messages.getString("UpdateBugStateTask.found") + SP + bugID); //$NON-NLS-1$
        }
        doBug(bugID);
      }
    }
    else if (bugList.equals(EMPTY))
    {
      if (product.equals(EMPTY))
      {
        throw new BuildException(Messages.getString("UpdateBugStateTask.expectingProduct") + "!"); //$NON-NLS-1$ //$NON-NLS-2$
      }

      if (debug > 1)
      {
        System.err.println(Messages.getString("UpdateBugStateTask.queryingFor") + SP + //$NON-NLS-1$ 
          (!status.equals(EMPTY) ? status + SP : EMPTY) + 
          (!product.equals(EMPTY) ? product + SP : EMPTY) + 
          (!milestone.equals(EMPTY) ? milestone + SP : EMPTY) + 
          Messages.getString("UpdateBugStateTask.bugs")); //$NON-NLS-1$ //$NON-NLS-2$
      }

      /* the Bugzilla search form generates a massive URL, but thankfully doesn't
       * demand all sorts of superfluous fields like when updating a bug */
      String url = HTTPS_BUGS_ECLIPSE_ORG_BUGS_BUGLIST_CGI_PRODUCT + urlEncode(product) + URL_BUG_STATUS + urlEncode(status)
        + URL_CHFIELDTO + urlEncode(endDate) + URL_TARGET_MILESTONE + urlEncode(milestone) + CTYPE_RDF;
      if (debug > 1)
      {
        System.err.println(Messages.getString("UpdateBugStateTask.connectingTo") + SP + //$NON-NLS-1$
          url + SP + "..."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
      }
      HttpsURLConnection bugsconn = getConn(url, GET, true, false, EMPTY);
      String bugs = slurpStream(bugsconn);
      if (debug > 1)
      {
        System.err.println(Messages.getString("UpdateBugStateTask.gotBugList") + COLON); //$NON-NLS-1$
        System.err.println(bugs);
      }

      Pattern p = Pattern.compile(BUGID_REGEX);
      Matcher m = p.matcher(bugs);
      if (m.find()) {
        while (m.find())
        {
          int bugID = Integer.parseInt(m.group(1));
          if (debug > 1)
          {
            System.out.println(Messages.getString("UpdateBugStateTask.found") + SP + bugID); //$NON-NLS-1$
          }
          doBug(bugID);
        }
      }
      else
      {
        System.out.println("No bugs found matching specified state" + SP + "(" + status + "). Nothing to do!");
      }
    }
    else
    {
      throw new BuildException(Messages.getString("UpdateBugStateTask.ambiguousRequest") + CS + //$NON-NLS-1$
        Messages.getString("UpdateBugStateTask.mutuallyExclusive") + "!"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }
  }  private void doBug(int bugID) throws BuildException
  {
    if (bugID == 0)
    {
      throw new BuildException(Messages.getString("UpdateBugStateTask.invalidBugID") + SP + bugID + "!"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    String bugcookie = BUGZILLA_LOGIN + login + BUGZILLA_LOGINCOOKIE + loginCookie;
    String buildstring = EMPTY;
    if (buildAlias.equals(EMPTY) && buildID.equals(EMPTY))
    {
      buildstring = Messages.getString("UpdateBugStateTask.latestBuild"); //$NON-NLS-1$
    }
    else if (!buildAlias.equals(EMPTY) && !buildID.equals(EMPTY))
    {
      buildstring = buildAlias + SP + "(" + buildID + ")"; //$NON-NLS-1$ //$NON-NLS-2$
    }
    else
    {
      buildstring = (!buildAlias.equals(EMPTY) ? buildAlias : buildID);
    }

    Hashtable args = new Hashtable();
    args.put(FORM_NAME, PROCESS_BUG);
    args.put(COMMENT, Messages.getString("UpdateBugStateTask.fixedIn") + SP + buildstring + "."); //$NON-NLS-1$ //$NON-NLS-2$
    args.put(ID, new Integer(bugID));
    args.put(PRODUCT, EMPTY);
    args.put(VERSION, EMPTY);
    args.put(COMPONENT, EMPTY);
    args.put(TARGET_MILESTONE, EMPTY);
    args.put(REP_PLATFORM, EMPTY);
    args.put(OP_SYS, EMPTY);
    args.put(PRIORITY, EMPTY);
    args.put(BUG_SEVERITY, EMPTY);
    args.put(BUG_FILE_LOC, EMPTY);
    args.put(SHORT_DESC, EMPTY);
    args.put(LONGDESCLENGTH, new Integer(1)); //Bugzilla doesn't seem to use this, but demands it anyways
    args.put(KNOB, RESOLVE);
    args.put(RESOLUTION, resolution);

    if (debug > 1)
    {
      System.err.println(Messages.getString("UpdateBugStateTask.usingCookie") + COLON + SP + bugcookie); //$NON-NLS-1$
      System.err.println(Messages.getString("UpdateBugStateTask.usingComment") + COLON + SP + args.get(COMMENT).toString()); //$NON-NLS-1$
    }

    /* slurp xml for bugID */
    String url = HTTPS_BUGS_ECLIPSE_ORG_BUGS_SHOW_BUG_CGI_ID + urlEncode(args.get(ID).toString()) + CTYPE_XML;
    if (debug > 1)
    {
      System.err.println(Messages.getString("UpdateBugStateTask.connectingTo") + SP + //$NON-NLS-1$
        url + SP + "..."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    }
    HttpsURLConnection xmlconn = getConn(url, GET, true, false, EMPTY);
    String xml = slurpStream(xmlconn);
    if (debug > 1)
    {
      System.err.println(Messages.getString("UpdateBugStateTask.gotXML") + COLON); //$NON-NLS-1$
      System.err.println(xml);
    }
    xmlconn.disconnect();

    /* parse xml, build post string */
    String req = EMPTY;
    Hashtable pxml = parseXML(xml);
    for (Enumeration e = args.keys(); e.hasMoreElements();)
    {
      String elem = e.nextElement().toString();
      /* sometimes Bugzilla omits bug_file_loc if it's blank... */
      if (args.get(elem).equals(EMPTY) && pxml.get(elem) != null)
      {
        args.put(elem, pxml.get(elem));
      }

      req += urlEncode(elem) + EQ + urlEncode(args.get(elem).toString()) + AMP;
    }

    req = req.substring(0, req.length() - 1);

    /* update bug, if applicable */
    if (pxml.get(BUG_STATUS) == null)
    {
      if (debug > 0)
      {
        System.out.println(Messages.getString("UpdateBugStateTask.noBugStatus") + SP + //$NON-NLS-1$
          bugID + CS + Messages.getString("UpdateBugStateTask.missingBug")); //$NON-NLS-1$ //$NON-NLS-2$
      }
    }
    else if (pxml.get(BUG_STATUS).equals(status))
    {
      String bugurl = HTTPS_BUGS_ECLIPSE_ORG_BUGS_PROCESS_BUG_CGI;
      if (debug > 1)
      {
        System.err.println(Messages.getString("UpdateBugStateTask.connectingTo") + SP + //$NON-NLS-1$
          bugurl + SP + "..."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
      }
      HttpsURLConnection bugconn = getConn(bugurl, POST, true, true, bugcookie);

      if (debug > 1)
      {
        System.err.println(Messages.getString("UpdateBugStateTask.postingData") + COLON); //$NON-NLS-1$
        System.err.println(req);
      }
      sendStream(bugconn, req);
      String response = slurpStream(bugconn);

      // trap for invalid login cookie 
      if (response.indexOf(Messages.getString("UpdateBugStateTask.legitimateLoginAndPassword")) > 0) //$NON-NLS-1$
      {
        System.err.println(Messages.getString("UpdateBugStateTask.couldNotLogIn")); //$NON-NLS-1$
        System.err.println(Messages.getString("UpdateBugStateTask.BugzillaReplied") + COLON + SP + //$NON-NLS-1$
          "\"" + Messages.getString("UpdateBugStateTask.legitimateLoginAndPassword") + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
        if (debug > 1)
        {
          System.err.println(Messages.getString("UpdateBugStateTask.gotResponse") + COLON); //$NON-NLS-1$
          System.err.println(response);
        }
        bugconn.disconnect();
        System.err.println(Messages.getString("UpdateBugStateTask.setBugFailed") + SP + bugID + SP + //$NON-NLS-1$
          Messages.getString("UpdateBugStateTask.to") + SP + resolution + SP + //$NON-NLS-1$
          "(" + Messages.getString("UpdateBugStateTask.was") + SP + pxml.get(BUG_STATUS) + ")" + COLON + SP + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
          "\"" + Messages.getString("UpdateBugStateTask.fixedIn") + SP + buildstring + ".\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      }
      else
      {
        if (debug > 1)
        {
          System.err.println(Messages.getString("UpdateBugStateTask.gotResponse") + COLON); //$NON-NLS-1$
          System.err.println(response);
        }
        bugconn.disconnect();
        if (debug > 0)
        {
          System.out.println(Messages.getString("UpdateBugStateTask.setBug") + SP + bugID + SP + //$NON-NLS-1$
            Messages.getString("UpdateBugStateTask.to") + SP + resolution + SP + //$NON-NLS-1$
            "(" + Messages.getString("UpdateBugStateTask.was") + SP + pxml.get(BUG_STATUS) + ")" + COLON + SP + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "\"" + Messages.getString("UpdateBugStateTask.fixedIn") + SP + buildstring + ".\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
      }
    }
    else
    {
      if (debug > 0)
      {
        System.out.println(Messages.getString("UpdateBugStateTask.ignoreBug") + SP + args.get(ID).toString() + SP + //$NON-NLS-1$
          "(" + Messages.getString("UpdateBugStateTask.notInExpectedState") + SP + status + ")" + COLON + SP + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
          Messages.getString("UpdateBugStateTask.was") + SP + pxml.get(BUG_STATUS).toString() + "."); //$NON-NLS-1$ //$NON-NLS-2$
      }
    }
  }

  private String urlEncode(String elem)
  {
    elem = htmlDecode(elem);

    try
    {
      elem = URLEncoder.encode(elem, UTF_8);
    }
    catch (java.io.UnsupportedEncodingException e)
    {
      throw new BuildException(Messages.getString("UpdateBugStateTask.couldntEncode") + SP + //$NON-NLS-1$
        "'" + elem + "'" + "!" + SP + JS + SP + e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    return elem;
  }

  private HttpsURLConnection getConn(String url, String method, boolean in, boolean out, String cookie)
  {
    URL u = null;
    try
    {
      u = new URL(url);
    }
    catch (java.net.MalformedURLException e)
    {
      throw new BuildException(Messages.getString("UpdateBugStateTask.badURL") + CS + //$NON-NLS-1$
        url + "!" + SP + JS + SP + e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    }

    URLConnection conn = null;
    try
    {
      conn = u.openConnection();
    }
    catch (java.io.IOException e)
    {
      throw new BuildException(Messages.getString("UpdateBugStateTask.failedConnection") + "!" + SP + JS + SP + e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
    }
    HttpsURLConnection sconn = (HttpsURLConnection)conn;

    try
    {
      sconn.setRequestMethod(method);
    }
    catch (java.net.ProtocolException e)
    {
      throw new BuildException(Messages.getString("UpdateBugStateTask.badHTTPMethod") + "!" + SP + JS + SP + e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
    }

    if (method.equals(POST))
    {
      sconn.setRequestProperty(CONTENT_TYPE, APPLICATION_X_WWW_FORM_URLENCODED);
    }

    if (!cookie.equals(EMPTY))
    {
      sconn.setRequestProperty(COOKIE, cookie);
    }

    sconn.setDoInput(in);
    sconn.setDoOutput(out);

    try
    {
      sconn.connect();
    }
    catch (java.io.IOException e)
    {
      throw new BuildException(Messages.getString("UpdateBugStateTask.connectError") + SP + //$NON-NLS-1$
        url + "!" + SP + JS + SP + e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    }

    return sconn;
  }

  private void sendStream(HttpsURLConnection conn, String req)
  {
    try
    {
      PrintWriter out = new PrintWriter(conn.getOutputStream());
      out.print(req);
      out.flush();
      out.close();
    }
    catch (java.io.IOException e)
    {
      throw new BuildException(Messages.getString("UpdateBugStateTask.streamWriteError") + "!" + SP + JS + SP + e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
    }
  }

  private String slurpStream(HttpsURLConnection conn)
  {
    String ret = EMPTY;
    try
    {
      BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
      String tmp;
      while ((tmp = in.readLine()) != EMPTY && tmp != null)
      {
        ret += tmp + NL;
      }

      in.close();
    }
    catch (java.io.IOException e)
    {
      throw new BuildException(Messages.getString("UpdateBugStateTask.streamReadError") + "!" + SP + JS + SP + e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
    }

    return ret;
  }

  /* this will only keep the last comment, but we don't use the comments anyways */
  private Hashtable parseXML(String xml)
  {
    if (debug > 1)
    {
      System.err.println(Messages.getString("UpdateBugStateTask.parsingXML") + "..."); //$NON-NLS-1$ //$NON-NLS-2$
    }
    Hashtable pxml = new Hashtable();
    Pattern p = Pattern.compile(XML_REGEX);
    Matcher m = p.matcher(xml);
    while (m.find())
    {
      if (debug > 1)
      {
        System.err.println(Messages.getString("UpdateBugStateTask.found") + SP + m.group(1) + SP + EQ + SP + m.group(2)); //$NON-NLS-1$
      }
      pxml.put(m.group(1), m.group(2));
    }

    return pxml;
  }

  private String htmlDecode(String str)
  {
    for (Iterator i = trans.keySet().iterator(); i.hasNext();)
    {
      String elem = i.next().toString();

      str = Pattern.compile(elem).matcher(str).replaceAll(trans.get(elem).toString());
    }

    return str;
  }

  public static void main(String args[])
  {
    new UpdateBugStateTask();
  }
}
