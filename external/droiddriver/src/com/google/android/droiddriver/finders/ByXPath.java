/*
 * Copyright (C) 2013 DroidDriver committers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.droiddriver.finders;

import android.util.Log;

import com.google.android.droiddriver.UiElement;
import com.google.android.droiddriver.base.AbstractUiElement;
import com.google.android.droiddriver.exceptions.DroidDriverException;
import com.google.android.droiddriver.exceptions.ElementNotFoundException;
import com.google.android.droiddriver.util.FileUtils;
import com.google.android.droiddriver.util.Logs;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.BufferedOutputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

/**
 * Find matching UiElement by XPath.
 */
public class ByXPath implements Finder {
  private static final XPath XPATH_COMPILER = XPathFactory.newInstance().newXPath();
  private static final String UI_ELEMENT = "UiElement";
  // document needs to be static so that when buildDomNode is called recursively
  // on children they are in the same document to be appended.
  private static Document document;
  private final String xPathString;
  private final XPathExpression xPathExpression;

  protected ByXPath(String xPathString) {
    this.xPathString = Preconditions.checkNotNull(xPathString);
    try {
      xPathExpression = XPATH_COMPILER.compile(xPathString);
    } catch (XPathExpressionException e) {
      throw new DroidDriverException("xPathString=" + xPathString, e);
    }
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this).addValue(xPathString).toString();
  }

  @Override
  public UiElement find(UiElement context) {
    Element domNode = ((AbstractUiElement) context).getDomNode();
    try {
      getDocument().appendChild(domNode);
      Element foundNode = (Element) xPathExpression.evaluate(domNode, XPathConstants.NODE);
      if (foundNode == null) {
        Logs.log(Log.DEBUG, "XPath evaluation returns null for " + xPathString);
        throw new ElementNotFoundException(this);
      }

      UiElement match = (UiElement) foundNode.getUserData(UI_ELEMENT);
      Logs.log(Log.INFO, "Found match: " + match);
      return match;
    } catch (XPathExpressionException e) {
      throw new ElementNotFoundException(this, e);
    } finally {
      try {
        getDocument().removeChild(domNode);
      } catch (DOMException e) {
        Logs.log(Log.ERROR, e, "Failed to clear document");
        document = null; // getDocument will create new
      }
    }
  }

  private static Document getDocument() {
    if (document == null) {
      try {
        document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
      } catch (ParserConfigurationException e) {
        throw new DroidDriverException(e);
      }
    }
    return document;
  }

  /**
   * Used internally in {@link AbstractUiElement}.
   */
  public static Element buildDomNode(AbstractUiElement uiElement) {
    String className = uiElement.getClassName();
    if (className == null) {
      className = "UNKNOWN";
    }
    Element element = getDocument().createElement(XPaths.tag(className));
    element.setUserData(UI_ELEMENT, uiElement, null /* UserDataHandler */);

    setAttribute(element, Attribute.CLASS, className);
    setAttribute(element, Attribute.RESOURCE_ID, uiElement.getResourceId());
    setAttribute(element, Attribute.PACKAGE, uiElement.getPackageName());
    setAttribute(element, Attribute.CONTENT_DESC, uiElement.getContentDescription());
    setAttribute(element, Attribute.TEXT, uiElement.getText());
    setAttribute(element, Attribute.CHECKABLE, uiElement.isCheckable());
    setAttribute(element, Attribute.CHECKED, uiElement.isChecked());
    setAttribute(element, Attribute.CLICKABLE, uiElement.isClickable());
    setAttribute(element, Attribute.ENABLED, uiElement.isEnabled());
    setAttribute(element, Attribute.FOCUSABLE, uiElement.isFocusable());
    setAttribute(element, Attribute.FOCUSED, uiElement.isFocused());
    setAttribute(element, Attribute.SCROLLABLE, uiElement.isScrollable());
    setAttribute(element, Attribute.LONG_CLICKABLE, uiElement.isLongClickable());
    setAttribute(element, Attribute.PASSWORD, uiElement.isPassword());
    setAttribute(element, Attribute.SELECTED, uiElement.isSelected());
    element.setAttribute(Attribute.BOUNDS.getName(), uiElement.getBounds().toShortString());

    // TODO: visitor pattern
    int childCount = uiElement.getChildCount();
    for (int i = 0; i < childCount; i++) {
      AbstractUiElement child = uiElement.getChild(i);
      if (child == null) {
        Logs.log(Log.INFO, "Skip null child for " + uiElement);
        continue;
      }
      if (!child.isVisible()) {
        Logs.log(Log.VERBOSE, "Skip invisible child: " + child);
        continue;
      }

      element.appendChild(child.getDomNode());
    }
    return element;
  }

  private static void setAttribute(Element element, Attribute attr, String value) {
    if (value != null) {
      element.setAttribute(attr.getName(), value);
    }
  }

  // add attribute only if it's true
  private static void setAttribute(Element element, Attribute attr, boolean value) {
    if (value) {
      element.setAttribute(attr.getName(), "");
    }
  }

  public static boolean dumpDom(String path, AbstractUiElement uiElement) {
    BufferedOutputStream bos = null;
    try {
      bos = FileUtils.open(path);
      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.transform(new DOMSource(uiElement.getDomNode()), new StreamResult(bos));
      Logs.log(Log.INFO, "Wrote dom to " + path);
    } catch (Exception e) {
      Logs.log(Log.ERROR, e, "Failed to transform node");
      return false;
    } finally {
      if (bos != null) {
        try {
          bos.close();
        } catch (Exception e) {
          // ignore
        }
      }
    }
    return true;
  }
}
