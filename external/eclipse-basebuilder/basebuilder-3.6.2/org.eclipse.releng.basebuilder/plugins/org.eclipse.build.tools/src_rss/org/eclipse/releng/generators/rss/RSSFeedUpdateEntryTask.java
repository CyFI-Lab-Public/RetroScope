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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.util.DateUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import org.eclipse.releng.util.rss.Messages;
import org.eclipse.releng.util.rss.RSSFeedUtil;

/**
 * Parameters: 
 *   debug - more output to console - eg., 0|1|2
 *   
 *   file - path to the XML file that will be read - eg., /path/to/file.to.read.xml
 *   xpath - xpath string representing the object to modify
 *   replacement - string to use as replacement
 * 
 * @author nickb
 *
 */
public class RSSFeedUpdateEntryTask extends Task {

  private int debug = 0;

  private static final String now = getTimestamp();

  private static final XPath xp = XPathFactory.newInstance().newXPath();

  private static final String NS = ""; //$NON-NLS-1$
  private static final String SEP = "----"; //$NON-NLS-1$ 
  private static final String SP = " "; //$NON-NLS-1$

  //required fields
  private File file;

  private String xpath;
  private String replacement;

  private Transformer transformer = null;

  private boolean isNodeFound = false;
  private boolean isNodeChanged = false;
  private Node foundNode = null;
  
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

  //optional - if null, display value found instead of changing it - see RSSFeedGetPropertyTask
  public void setReplacement(String replacement) { this.replacement = replacement; } 

  // The method executing the task
  public void execute() throws BuildException {
    if (debug>0) { 
      System.out.println(Messages.getString("RSSFeedUpdateEntryTask.SearchingFor") + SP + xpath + (!isNullString(replacement)?", " + Messages.getString("RSSFeedUpdateEntryTask.ReplacingWith") + " '" + replacement + "'":NS)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
    }
    updateFeedXML(file); // load previous
  }

  //$ANALYSIS-IGNORE codereview.java.rules.exceptions.RuleExceptionsSpecificExceptions
  private void updateFeedXML(File file){
    if (file.exists()) {
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

      try {
        transformer = RSSFeedAddEntryTask.createTransformer("UTF-8"); //$NON-NLS-1$
      } catch (TransformerException e) {
        e.printStackTrace();
      }

      if (!isNullString(replacement)) {
        setEntryNodeUpdate(document.getDocumentElement());
      }
      Node newNode=findAndReplace(document);
      if (debug > 1 && newNode != null) {
        try {
          System.out.println(SEP);
          transformer.transform(new DOMSource(newNode),new StreamResult(System.out));
          System.out.println(SEP);
        }
        catch (TransformerException e) {
          e.printStackTrace();
        }
      }
      if (!isNullString(replacement) && newNode != null) {
        try {
          transformer.transform(new DOMSource(document),new StreamResult(new PrintStream(file)));
        }
        catch (FileNotFoundException e) {
          e.printStackTrace();
        }
        catch (TransformerException e) {
          e.printStackTrace();
        }
      }
    }
    else {
      System.out.println(Messages.getString("RSSFeedCommon.RSSFeedFile") + SP + file.toString()+ " "+ Messages.getString("RSSFeedUpdateEntryTask.DoesNotExist")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    }
  }

  // get/set the desired node
  public Node getFoundNode()
  {
    return this.foundNode;
  }
  private void setFoundNode(Node foundNode)
  {
    this.foundNode = foundNode;
  }

  // has the desired node been found?
  public boolean getNodeFound()
  {
    return this.isNodeFound;
  }
  private void setNodeFound(boolean isNodeFound)
  {
    this.isNodeFound = isNodeFound;
  }

  // has the desired node been changed?
  public boolean getNodeChanged()
  {
    return this.isNodeChanged;
  }
  private void setNodeChanged(boolean isNodeChanged)
  {
    this.isNodeChanged = isNodeChanged;
  }

  /**
   * Modify an entry:
   * 
   *   <entry>
   *      <title/>
   *      <link href=""/>
   *      <id/>
   *      <updated/>
   *      <summary>
   *       ...
   *     </summary>
   *   </entry>
   */
  private Node findAndReplace(Document document) {
    Node parentEntryNode = null;
    Node aNode = null;
    if (debug==0) { System.out.print(xpath + (isNullString(replacement)?" = ":" :: ")); } //$NON-NLS-1$ //$NON-NLS-2$
    NodeList nodelist = getNodeList(document, xpath);
    // Process the elements in the nodelist
    if (nodelist != null && nodelist.getLength()>0) {
      for (int i=0; i<nodelist.getLength(); i++) {
        Node node = (Node)nodelist.item(i);
        switch (node.getNodeType())
        {
          case Node.ATTRIBUTE_NODE :
            aNode = (Attr)nodelist.item(i);
            if (debug>0) { System.out.print(Messages.getString("RSSFeedUpdateEntryTask.DebugFoundAttribute")); }  //$NON-NLS-1$
            break;

          case Node.ELEMENT_NODE :
            aNode = (Element)nodelist.item(i);
            if (debug>0) { System.out.print(Messages.getString("RSSFeedUpdateEntryTask.DebugFoundElement")); } //$NON-NLS-1$
            break;

          case Node.TEXT_NODE :
            aNode = (Text)nodelist.item(i);
            if (debug>0) { System.out.print(Messages.getString("RSSFeedUpdateEntryTask.DebugFoundText")); } //$NON-NLS-1$
            break;

          default:
            aNode = null;
          break;
        }
        if (aNode != null) {
          setFoundNode(aNode);
          setNodeFound(true);
          System.out.print((debug>0?aNode.getNodeName() + " = ":NS) + aNode.getNodeValue()); //$NON-NLS-1$ //$NON-NLS-2$
          if (!isNullString(replacement)) { aNode.setTextContent(replacement); }
          System.out.println(isNullString(replacement)?NS:" => " + replacement); //$NON-NLS-1$ //$NON-NLS-2$
          if (debug>0) { 
            try
            {
              // write to console
              System.out.println(SEP); //$NON-NLS-1$
              transformer.transform(new DOMSource(getParentNode(document,aNode,null,NS)), new StreamResult(System.out));  //$NON-NLS-1$
              System.out.println(SEP); //$NON-NLS-1$
            }
            catch (TransformerException e)
            {
              e.printStackTrace();
            }
          }
          if (!isNullString(replacement)) { 
            parentEntryNode = getParentNode(document, aNode, "entry", NS); //$NON-NLS-1$ //$NON-NLS-2$
            setEntryNodeUpdate(parentEntryNode); 
          }
        }
      }
    } else {
      System.out.println(Messages.getString("RSSFeedUpdateEntryTask.XpathNodeNotFound")); //$NON-NLS-1$
    }
    return parentEntryNode;
  }

  private Node getParentNode(Document document, Node nodeIn, String target, String indent)
  {
    Node node = nodeIn;
    if (node.getNodeType() != Node.ELEMENT_NODE) {
      if (debug>1) { System.out.println(indent + Messages.getString("RSSFeedUpdateEntryTask.DebugGotATNode") + node.getNodeName()); } //$NON-NLS-1$
      // get the element for the attrib/text node
      NodeList nodelist = getNodeList(document, xpath.substring(0, xpath.lastIndexOf("/")));
      if (nodelist !=null && nodelist.getLength()>0)
      {
        for (int i=0; i<nodelist.getLength(); i++) {
          node = (Node)nodelist.item(i);
          break;
        }
      }
    }
    if (debug>1) { System.out.println(indent + Messages.getString("RSSFeedUpdateEntryTask.DebugGotENode") + node.getNodeName() + " (" + node.getNodeType() + ")"); } //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    if (!isNullString(target) && !node.getNodeName().equals(target)) 
    {
      node = getParentNode(document, node.getParentNode(), target, indent + "_ "); //$NON-NLS-1$
    }
    return node;
  }

  private NodeList getNodeList(Document document, String xpath)
  {
    NodeList nodelist = null;
    try
    {
      xp.reset();
      Object o = xp.evaluate(xpath, document, XPathConstants.NODESET);
      if (o instanceof NodeList)
      {
        nodelist = (NodeList)o;
      }
    }
    catch (XPathExpressionException e)
    {
      e.printStackTrace();
    }
    return nodelist;
  }

  //$ANALYSIS-IGNORE codereview.java.rules.exceptions.RuleExceptionsSpecificExceptions
  private void setEntryNodeUpdate(Node parentEntryNode){
    for (Node child=parentEntryNode.getFirstChild(); child != null; child=child.getNextSibling()) {
      if ("updated".equals(child.getLocalName())) { //$NON-NLS-1$
        if (debug > 0) {
          System.out.println(Messages.getString("RSSFeedCommon.Set") + " <" + child.getLocalName()+ ">"+ now+ "</"+ child.getLocalName()+ ">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
        }
        ((Element)child).setTextContent(now);
        setNodeChanged(true);
        break;
      }
    }
  }


  private static String getTimestamp() { // eg., 2006-04-10T20:40:08Z
    return DateUtils.format(new Date(), DateUtils.ISO8601_DATETIME_PATTERN) + "Z";  //$NON-NLS-1$
  }

  private static boolean isNullString(String str)
  {
    return RSSFeedUtil.isNullString(str);
  }

}