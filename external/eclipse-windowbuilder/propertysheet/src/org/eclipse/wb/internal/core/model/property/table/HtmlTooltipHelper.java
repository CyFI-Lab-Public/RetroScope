/*******************************************************************************
 * Copyright (c) 2011 Google, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Google, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.wb.internal.core.model.property.table;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.wb.draw2d.IColorConstants;
import org.eclipse.wb.internal.core.DesignerPlugin;
import org.eclipse.wb.internal.core.EnvironmentUtils;
import org.eclipse.wb.internal.core.utils.reflect.ReflectionUtils;
import org.eclipse.wb.internal.core.utils.ui.GridDataFactory;
import org.eclipse.wb.internal.core.utils.ui.PixelConverter;

import java.io.StringReader;
import java.net.URL;
import java.text.MessageFormat;

/**
 * Helper for displaying HTML tooltips.
 *
 * @author scheglov_ke
 * @coverage core.model.property.table
 */
public final class HtmlTooltipHelper {
  public static Control createTooltipControl(Composite parent, String header, String details) {
    return createTooltipControl(parent, header, details, 0);
  }

  public static Control createTooltipControl(Composite parent,
      String header,
      String details,
      int heightLimit) {
    // prepare Control
    Control control;
    try {
      String html = "<table cellspacing=2 cellpadding=0 border=0 margins=0 id=_wbp_tooltip_body>";
      if (header != null) {
        html += "<tr align=center><td><b>" + header + "</b></td></tr>";
      }
      html += "<tr><td align=justify>" + details + "</td></tr>";
      html += "</table>";
      control = createTooltipControl_Browser(parent, html, heightLimit);
    } catch (Throwable e) {
      control = createTooltipControl_Label(parent, details);
    }
    // set listeners
    {
      Listener listener = new Listener() {
        @Override
        public void handleEvent(Event event) {
          Control tooltipControl = (Control) event.widget;
          hideTooltip(tooltipControl);
        }
      };
      control.addListener(SWT.MouseExit, listener);
    }
    // done
    return control;
  }

  /**
   * Creates {@link Browser} for displaying tooltip.
   */
  private static Control createTooltipControl_Browser(Composite parent,
      String html,
      final int heightLimitChars) {
    // prepare styles
    String styles;
    try {
        styles = DesignerPlugin.readFile(PropertyTable.class.getResourceAsStream("Tooltip.css"),
                Charsets.US_ASCII);
        if (styles == null) {
            styles = "";
        }
    } catch (Throwable e) {
      styles = "";
    }
    // prepare HTML with styles and tags
    String wrappedHtml;
    {
      String bodyAttributes =
          MessageFormat.format(
              "text=''{0}'' bgcolor=''{1}''",
              getColorWebString(IColorConstants.tooltipForeground),
              getColorWebString(IColorConstants.tooltipBackground));
      String closeElement =
          EnvironmentUtils.IS_LINUX
              ? "    <a href='' style='position:absolute;right:1em;' id=_wbp_close>Close</a>"
              : "";
      wrappedHtml =
          /*CodeUtils.*/getSource(
              "<html>",
              "  <style CHARSET='ISO-8859-1' TYPE='text/css'>",
              styles,
              "  </style>",
              "  <body " + bodyAttributes + ">",
              closeElement,
              html,
              "  </body>",
              "</html>");
    }
    // prepare Browser
    final Browser browser = new Browser(parent, SWT.NONE);
    browser.setText(wrappedHtml);
    // open URLs in new window
    browser.addLocationListener(new LocationAdapter() {
      @Override
      public void changing(LocationEvent event) {
        event.doit = false;
        hideTooltip((Browser) event.widget);
        if (!"about:blank".equals(event.location)) {
          try {
            IWorkbenchBrowserSupport support = PlatformUI.getWorkbench().getBrowserSupport();
            IWebBrowser browserSupport = support.createBrowser("wbp.browser");
            browserSupport.openURL(new URL(event.location));
          } catch (Throwable e) {
            DesignerPlugin.log(e);
          }
        }
      }
    });
    // set size
    {
      int textLength = getTextLength(html);
      // horizontal hint
      int hintH = 50;
      if (textLength < 100) {
        hintH = 40;
      }
      // vertical hint
      int hintV = textLength / hintH + 3;
      hintV = Math.min(hintV, 8);
      // do set
      GridDataFactory.create(browser).hintC(hintH, hintV);
    }
    // tweak size after rendering HTML
    browser.addProgressListener(new ProgressAdapter() {
      @Override
      public void completed(ProgressEvent event) {
        browser.removeProgressListener(this);
        tweakBrowserSize(browser, heightLimitChars);
        browser.getShell().setVisible(true);
      }
    });
    // done
    return browser;
  }

  private static void tweakBrowserSize(Browser browser, int heightLimitChars) {
    GridDataFactory.create(browser).grab().fill();
    // limit height
    if (heightLimitChars != 0) {
      PixelConverter pixelConverter = new PixelConverter(browser);
      int maxHeight = pixelConverter.convertHeightInCharsToPixels(heightLimitChars);
      expandShellToShowFullPage_Height(browser, maxHeight);
    }
    // if no limit, then show all, so make as tall as required
    if (heightLimitChars == 0) {
      expandShellToShowFullPage_Height(browser, Integer.MAX_VALUE);
    }
  }

  private static void expandShellToShowFullPage_Height(Browser browser, int maxHeight) {
    try {
      Shell shell = browser.getShell();
      // calculate required
      int contentHeight;
      {
        getContentOffsetHeight(browser);
        contentHeight = getContentScrollHeight(browser);
      }
      // apply height
      int useHeight = Math.min(contentHeight + ((EnvironmentUtils.IS_LINUX) ? 2 : 10), maxHeight);
      shell.setSize(shell.getSize().x, useHeight);
      // trim height to content
      {
        int offsetHeight = getBodyOffsetHeight(browser);
        int scrollHeight = getBodyScrollHeight(browser);
        int delta = scrollHeight - offsetHeight;
        if (delta != 0 && delta < 10) {
          Point size = shell.getSize();
          shell.setSize(size.x, size.y + delta + 1);
        }
      }
      // trim width to content
      {
        int offsetWidth = getContentOffsetWidth(browser);
        {
          Point size = shell.getSize();
          shell.setSize(offsetWidth + ((EnvironmentUtils.IS_MAC) ? 6 : 10), size.y);
        }
      }
      // hide 'Close' if too narrow
      if (EnvironmentUtils.IS_LINUX) {
        if (shell.getSize().y < 30) {
          hideCloseElement(browser);
        }
      }
    } catch (Throwable e) {
    }
  }

  private static int getContentOffsetWidth(Browser browser) throws Exception {
    return evaluateScriptInt(
        browser,
        "return document.getElementById('_wbp_tooltip_body').offsetWidth;");
  }

  private static int getContentOffsetHeight(Browser browser) throws Exception {
    return evaluateScriptInt(
        browser,
        "return document.getElementById('_wbp_tooltip_body').offsetHeight;");
  }

  private static int getContentScrollHeight(Browser browser) throws Exception {
    return evaluateScriptInt(
        browser,
        "return document.getElementById('_wbp_tooltip_body').scrollHeight;");
  }

  private static int getBodyOffsetHeight(Browser browser) throws Exception {
    return evaluateScriptInt(browser, "return document.body.offsetHeight;");
  }

  private static int getBodyScrollHeight(Browser browser) throws Exception {
    return evaluateScriptInt(browser, "return document.body.scrollHeight;");
  }

  private static int evaluateScriptInt(Browser browser, String script) throws Exception {
    Object o = ReflectionUtils.invokeMethod(browser, "evaluate(java.lang.String)", script);
    return ((Number) o).intValue();
  }

  private static void hideCloseElement(Browser browser) throws Exception {
    String script = "document.getElementById('_wbp_close').style.display = 'none'";
    ReflectionUtils.invokeMethod(browser, "evaluate(java.lang.String)", script);
  }

  /**
   * @return the length of text in given HTML. Uses internal class, so may fail, in this case
   *         returns length on HTML.
   */
  private static int getTextLength(String html) {
    StringReader htmlStringReader = new StringReader(html);
    try {
      ClassLoader classLoader = PropertyTable.class.getClassLoader();
      Class<?> readerClass =
          classLoader.loadClass("org.eclipse.jface.internal.text.html.HTML2TextReader");
      Object reader = readerClass.getConstructors()[0].newInstance(htmlStringReader, null);
      String text = (String) ReflectionUtils.invokeMethod(reader, "getString()");
      return text.length();
    } catch (Throwable e) {
      return html.length();
    }
  }

  /**
   * Returns a string representation of {@link Color} suitable for web pages.
   *
   * @param color
   *          the {@link Color} instance, not <code>null</code>.
   * @return a string representation of {@link Color} suitable for web pages.
   */
  private static String getColorWebString(final Color color) {
    String colorString = "#" + Integer.toHexString(color.getRed());
    colorString += Integer.toHexString(color.getGreen());
    colorString += Integer.toHexString(color.getBlue());
    return colorString;
  }

  /**
   * Creates {@link Label} if {@link Browser} can not be used.
   */
  private static Control createTooltipControl_Label(Composite parent, String html) {
    // prepare Label
    final Label label = new Label(parent, SWT.WRAP);
    label.setText(html);
    // set size
    int requiredWidth = label.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
    GridDataFactory.create(label).hintHC(50).hintHMin(requiredWidth);
    // copy colors
    label.setForeground(parent.getForeground());
    label.setBackground(parent.getBackground());
    // done
    parent.getDisplay().asyncExec(new Runnable() {
      @Override
    public void run() {
        Shell shell = label.getShell();
        shell.setVisible(true);
      }
    });
    return label;
  }

  private static void hideTooltip(Control tooltip) {
    tooltip.getShell().dispose();
  }

  // Copied from CodeUtils.java: CodeUtils.getSource()
  /**
   * @return the source as single {@link String}, lines joined using "\n".
   */
  public static String getSource(String... lines) {
      return Joiner.on('\n').join(lines);
  }
}
