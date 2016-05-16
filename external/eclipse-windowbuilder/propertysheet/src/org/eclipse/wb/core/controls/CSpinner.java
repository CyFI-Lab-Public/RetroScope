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
package org.eclipse.wb.core.controls;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.ParseException;

/**
 * Custom implementation of {@link Spinner}.
 * 
 * @author scheglov_ke
 * @coverage core.control
 */
public class CSpinner extends Composite {
  private static final Color COLOR_VALID = Display.getCurrent().getSystemColor(
      SWT.COLOR_LIST_BACKGROUND);
  private static final Color COLOR_INVALID = new Color(null, 255, 230, 230);
  private int m_minimum = 0;
  private int m_maximum = 100;
  private int m_increment = 1;
  private int m_value = 0;
  private int m_multiplier = 1;
  private String m_formatPattern = "0";
  private DecimalFormat m_format = new DecimalFormat(m_formatPattern);
  ////////////////////////////////////////////////////////////////////////////
  //
  // GUI fields
  //
  ////////////////////////////////////////////////////////////////////////////
  private final Button m_button;
  private final Text m_text;
  private final Spinner m_spinner;
  private Composite win32Hack;

  ////////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  ////////////////////////////////////////////////////////////////////////////
  public CSpinner(Composite parent, int style) {
    super(parent, style);
    m_button = new Button(this, SWT.ARROW | SWT.DOWN);
    {
      int textStyle = SWT.SINGLE | SWT.RIGHT;
      if (IS_OS_MAC_OSX_COCOA) {
        textStyle |= SWT.BORDER;
      }
      m_text = new Text(this, textStyle);
      m_text.setText("" + m_value);
      m_text.addKeyListener(new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
          if (e.keyCode == SWT.ARROW_UP || e.keyCode == SWT.ARROW_DOWN) {
            e.doit = false;
            updateValue(e.keyCode);
          }
        }

        @Override
        public void keyReleased(KeyEvent e) {
          try {
            m_value = (int) (m_format.parse(m_text.getText()).doubleValue() * m_multiplier);
            if (m_value < m_minimum || m_value > m_maximum) {
              m_text.setBackground(COLOR_INVALID);
              setState(MessageFormat.format(
                  Messages.CSpinner_outOfRange,
                  m_value,
                  m_minimum,
                  m_maximum));
              notifySelectionListeners(false);
            } else {
              setState(null);
              notifySelectionListeners(true);
            }
          } catch (ParseException ex) {
            setState(MessageFormat.format(
                Messages.CSpinner_canNotParse,
                m_text.getText(),
                m_formatPattern));
            notifySelectionListeners(false);
          }
        }
      });
    }
    if (!IS_OS_MAC_OSX) {
      win32Hack = new Composite(this, SWT.NONE);
      win32Hack.setBackground(getDisplay().getSystemColor(SWT.COLOR_WHITE));
      win32Hack.moveAbove(null);
      win32Hack.moveBelow(m_text);
    }
    {
      m_spinner = new Spinner(this, SWT.VERTICAL);
      m_spinner.setMinimum(0);
      m_spinner.setMaximum(50);
      m_spinner.setIncrement(1);
      m_spinner.setPageIncrement(1);
      m_spinner.setSelection(25);
      m_spinner.addFocusListener(new FocusAdapter() {
        @Override
        public void focusGained(FocusEvent e) {
          setFocus();
        }
      });
      m_spinner.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          m_text.forceFocus();
          if (m_spinner.getSelection() > 25) {
            updateValue(SWT.ARROW_UP);
          } else {
            updateValue(SWT.ARROW_DOWN);
          }
          m_spinner.setSelection(25);
        }
      });
      setBackground(getDisplay().getSystemColor(SWT.COLOR_WHITE));
      if (IS_OS_WINDOWS_XP || IS_OS_WINDOWS_2003) {
        setLayout(new WindowsXpLayout());
      } else if (IS_OS_WINDOWS_VISTA || IS_OS_WINDOWS_7) {
        setLayout(new WindowsVistaLayout());
      } else if (IS_OS_LINUX) {
        setLayout(new LinuxLayout());
      } else if (IS_OS_MAC_OSX) {
        if (IS_OS_MAC_OSX_COCOA) {
          setLayout(new MacCocoaLayout());
        } else {
          setLayout(new MacLayout());
        }
      } else {
        setLayout(new WindowsXpLayout());
      }
    }
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Access
  //
  ////////////////////////////////////////////////////////////////////////////
  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    m_text.setEnabled(enabled);
    m_spinner.setEnabled(enabled);
  }

  /**
   * Sets the number of decimal places used by the receiver.
   * <p>
   * See {@link Spinner#setDigits(int)}.
   */
  public void setDigits(int digits) {
    m_formatPattern = "0.";
    m_multiplier = 1;
    for (int i = 0; i < digits; i++) {
      m_formatPattern += "0";
      m_multiplier *= 10;
    }
    m_format = new DecimalFormat(m_formatPattern);
    updateText();
  }

  /**
   * Sets minimum and maximum using single invocation.
   */
  public void setRange(int minimum, int maximum) {
    setMinimum(minimum);
    setMaximum(maximum);
  }

  /**
   * @return the minimum value that the receiver will allow.
   */
  public int getMinimum() {
    return m_minimum;
  }

  /**
   * Sets the minimum value that the receiver will allow.
   */
  public void setMinimum(int minimum) {
    m_minimum = minimum;
    setSelection(Math.max(m_value, m_minimum));
  }

  /**
   * Sets the maximum value that the receiver will allow.
   */
  public void setMaximum(int maximum) {
    m_maximum = maximum;
    setSelection(Math.min(m_value, m_maximum));
  }

  /**
   * Sets the amount that the receiver's value will be modified by when the up/down arrows are
   * pressed to the argument, which must be at least one.
   */
  public void setIncrement(int increment) {
    m_increment = increment;
  }

  /**
   * Sets the <em>value</em>, which is the receiver's position, to the argument. If the argument is
   * not within the range specified by minimum and maximum, it will be adjusted to fall within this
   * range.
   */
  public void setSelection(int newValue) {
    newValue = Math.min(Math.max(m_minimum, newValue), m_maximum);
    if (newValue != m_value) {
      m_value = newValue;
      updateText();
      // set valid state
      setState(null);
    }
  }

  private void updateText() {
    String text = m_format.format((double) m_value / m_multiplier);
    m_text.setText(text);
    m_text.selectAll();
  }

  /**
   * @return the <em>selection</em>, which is the receiver's position.
   */
  public int getSelection() {
    return m_value;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Update
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Updates {@link #m_value} into given direction.
   */
  private void updateValue(int direction) {
    // prepare new value
    int newValue;
    {
      newValue = m_value;
      if (direction == SWT.ARROW_UP) {
        newValue += m_increment;
      }
      if (direction == SWT.ARROW_DOWN) {
        newValue -= m_increment;
      }
    }
    // update value
    setSelection(newValue);
    notifySelectionListeners(true);
  }

  /**
   * Sets the valid/invalid state.
   * 
   * @param message
   *          the message to show, or <code>null</code> if valid.
   */
  private void setState(String message) {
    m_text.setToolTipText(message);
    if (message == null) {
      m_text.setBackground(COLOR_VALID);
    } else {
      m_text.setBackground(COLOR_INVALID);
    }
  }

  /**
   * Notifies {@link SWT#Selection} listeners with value and state.
   */
  private void notifySelectionListeners(boolean valid) {
    Event event = new Event();
    event.detail = m_value;
    event.doit = valid;
    notifyListeners(SWT.Selection, event);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Windows XP
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Implementation of {@link Layout} for Windows XP.
   */
  private class WindowsXpLayout extends Layout {
    @Override
    protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {
      Point size = m_text.computeSize(SWT.DEFAULT, SWT.DEFAULT);
      size.x += m_spinner.computeSize(SWT.DEFAULT, SWT.DEFAULT).x - m_spinner.getClientArea().width;
      // add Text widget margin
      size.y += 2;
      // apply hints
      if (wHint != SWT.DEFAULT) {
        size.x = Math.min(size.x, wHint);
      }
      if (hHint != SWT.DEFAULT) {
        size.y = Math.min(size.y, hHint);
      }
      // OK, final size
      return size;
    }

    @Override
    protected void layout(Composite composite, boolean flushCache) {
      Rectangle cRect = composite.getClientArea();
      if (cRect.isEmpty()) {
        return;
      }
      // prepare size of Text
      Point tSize = m_text.computeSize(SWT.DEFAULT, SWT.DEFAULT);
      // prepare size of Spinner
      Point sSize;
      sSize = m_spinner.computeSize(SWT.DEFAULT, SWT.DEFAULT, flushCache);
      sSize.y = Math.min(sSize.y, Math.min(tSize.y, cRect.height));
      sSize.x = Math.min(sSize.x, cRect.width);
      // prepare width of arrows part of Spinner
      int arrowWidth = m_button.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
      // set bounds for Spinner and Text
      m_spinner.setBounds(
          cRect.x + cRect.width - sSize.x + 1,
          cRect.y - 1,
          sSize.x,
          cRect.height + 2);
      m_text.setBounds(cRect.x, cRect.y + 1, cRect.width - arrowWidth, tSize.y);
      win32Hack.setBounds(cRect.x, cRect.y, cRect.width - arrowWidth, sSize.y);
    }
  }
  ////////////////////////////////////////////////////////////////////////////
  //
  // Windows Vista
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Implementation of {@link Layout} for Windows Vista.
   */
  private class WindowsVistaLayout extends Layout {
    @Override
    protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {
      Point size = m_text.computeSize(SWT.DEFAULT, SWT.DEFAULT);
      size.x += m_spinner.computeSize(SWT.DEFAULT, SWT.DEFAULT).x - m_spinner.getClientArea().width;
      // add Text widget margin
      size.y += 3;
      // apply hints
      if (wHint != SWT.DEFAULT) {
        size.x = Math.min(size.x, wHint);
      }
      if (hHint != SWT.DEFAULT) {
        size.y = Math.min(size.y, hHint);
      }
      // OK, final size
      return size;
    }

    @Override
    protected void layout(Composite composite, boolean flushCache) {
      Rectangle cRect = composite.getClientArea();
      if (cRect.isEmpty()) {
        return;
      }
      // prepare size of Text
      Point tSize = m_text.computeSize(SWT.DEFAULT, SWT.DEFAULT);
      // prepare size of Spinner
      Point sSize;
      sSize = m_spinner.computeSize(SWT.DEFAULT, SWT.DEFAULT, flushCache);
      sSize.y = Math.min(sSize.y, Math.min(tSize.y, cRect.height));
      sSize.x = Math.min(sSize.x, cRect.width);
      // prepare width of arrows part of Spinner
      int arrowWidth = m_button.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
      // set bounds for Spinner and Text
      m_spinner.setBounds(
          cRect.x + cRect.width - sSize.x + 1,
          cRect.y - 1,
          sSize.x,
          cRect.height + 2);
      m_text.setBounds(cRect.x, cRect.y + 1, cRect.width - arrowWidth, tSize.y);
      win32Hack.setBounds(cRect.x, cRect.y, cRect.width - arrowWidth, sSize.y);
    }
  }
  ////////////////////////////////////////////////////////////////////////////
  //
  // Linux
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Implementation of {@link Layout} for Linux.
   */
  private class LinuxLayout extends Layout {
    @Override
    protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {
      Point size = m_text.computeSize(SWT.DEFAULT, SWT.DEFAULT);
      size.x += m_spinner.computeSize(SWT.DEFAULT, SWT.DEFAULT).x - m_spinner.getClientArea().width;
      // apply hints
      if (wHint != SWT.DEFAULT) {
        size.x = Math.min(size.x, wHint);
      }
      if (hHint != SWT.DEFAULT) {
        size.y = Math.min(size.y, hHint);
      }
      // OK, final size
      return size;
    }

    @Override
    protected void layout(Composite composite, boolean flushCache) {
      Rectangle cRect = composite.getClientArea();
      if (cRect.isEmpty()) {
        return;
      }
      // prepare size of Text
      Point tSize = m_text.computeSize(SWT.DEFAULT, SWT.DEFAULT);
      // prepare size of Spinner
      Point sSize;
      sSize = m_spinner.computeSize(SWT.DEFAULT, SWT.DEFAULT, flushCache);
      sSize.y = Math.min(sSize.y, Math.min(tSize.y, cRect.height));
      sSize.x = Math.min(sSize.x, cRect.width);
      // prepare width of arrows part of Spinner
      int arrowWidth;
      {
        m_spinner.setSize(sSize);
        arrowWidth = sSize.x - m_spinner.getClientArea().width;
      }
      // set bounds for Spinner and Text
      m_spinner.setBounds(cRect.x + cRect.width - sSize.x, cRect.y - 2, sSize.x, cRect.height + 4);
      m_text.setBounds(cRect.x, cRect.y, cRect.width - arrowWidth, tSize.y);
    }
  }
  ////////////////////////////////////////////////////////////////////////////
  //
  // MacOSX
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Implementation of {@link Layout} for MacOSX.
   */
  private class MacLayout extends Layout {
    @Override
    protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {
      Point size = m_text.computeSize(SWT.DEFAULT, SWT.DEFAULT);
      size.x += m_spinner.computeSize(SWT.DEFAULT, SWT.DEFAULT).x - m_spinner.getClientArea().width;
      // add Text widget margin
      size.y += 4;
      // apply hints
      if (wHint != SWT.DEFAULT) {
        size.x = Math.min(size.x, wHint);
      }
      if (hHint != SWT.DEFAULT) {
        size.y = Math.min(size.y, hHint);
      }
      // OK, final size
      return size;
    }

    @Override
    protected void layout(Composite composite, boolean flushCache) {
      Rectangle cRect = composite.getClientArea();
      if (cRect.isEmpty()) {
        return;
      }
      // prepare size of Text
      Point tSize = m_text.computeSize(SWT.DEFAULT, SWT.DEFAULT);
      tSize.y += 4;
      // prepare size of Spinner
      Point sSize;
      sSize = m_spinner.computeSize(SWT.DEFAULT, SWT.DEFAULT, flushCache);
      sSize.y = Math.min(sSize.y, Math.min(tSize.y, cRect.height));
      sSize.x = Math.min(sSize.x, cRect.width);
      // prepare width of arrows part of Spinner
      int arrowWidth = m_button.computeSize(-1, -1).x;
      // set bounds for Spinner and Text
      m_spinner.setBounds(cRect.x + cRect.width - sSize.x, cRect.y, sSize.x, cRect.height);
      m_text.setBounds(cRect.x, cRect.y + 2, cRect.width - arrowWidth - 2, tSize.y);
    }
  }
  /**
   * Implementation of {@link Layout} for MacOSX Cocoa.
   */
  private class MacCocoaLayout extends Layout {
    @Override
    protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {
      Point textSize = m_text.computeSize(SWT.DEFAULT, SWT.DEFAULT);
      Point spinnerSize = m_spinner.computeSize(SWT.DEFAULT, SWT.DEFAULT);
      int arrowWidth = m_button.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
      int width = textSize.x + arrowWidth;
      int height = Math.max(spinnerSize.y, textSize.y);
      // apply hints
      if (wHint != SWT.DEFAULT) {
        width = Math.min(width, wHint);
      }
      if (hHint != SWT.DEFAULT) {
        height = Math.min(height, hHint);
      }
      return new Point(width, height);
    }

    @Override
    protected void layout(Composite composite, boolean flushCache) {
      Rectangle clientArea = composite.getClientArea();
      if (clientArea.isEmpty()) {
        return;
      }
      // prepare size of Spinner
      Point spinnerSize = m_spinner.computeSize(SWT.DEFAULT, SWT.DEFAULT, flushCache);
      // prepare width of arrows part of Spinner
      int arrowWidth = m_button.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
      m_spinner.setBounds(clientArea.x + clientArea.width - arrowWidth - 1, clientArea.y
          + clientArea.height
          - spinnerSize.y, arrowWidth + 2, spinnerSize.y);
      m_text.setBounds(
          clientArea.x + 2,
          clientArea.y + 2,
          clientArea.width - arrowWidth - 5,
          clientArea.y + clientArea.height - 4);
    }
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // System utils
  //
  ////////////////////////////////////////////////////////////////////////////
  private static final String OS_NAME = System.getProperty("os.name");
  private static final String OS_VERSION = System.getProperty("os.version");
  private static final String WS_TYPE = SWT.getPlatform();
  private static final boolean IS_OS_MAC_OSX = isOS("Mac OS X");
  private static final boolean IS_OS_MAC_OSX_COCOA = IS_OS_MAC_OSX && "cocoa".equals(WS_TYPE);
  private static final boolean IS_OS_LINUX = isOS("Linux") || isOS("LINUX");
  private static final boolean IS_OS_WINDOWS_XP = isWindowsVersion("5.1");
  private static final boolean IS_OS_WINDOWS_2003 = isWindowsVersion("5.2");
  private static final boolean IS_OS_WINDOWS_VISTA = isWindowsVersion("6.0");
  private static final boolean IS_OS_WINDOWS_7 = isWindowsVersion("6.1");

  private static boolean isOS(String osName) {
    return OS_NAME != null && OS_NAME.startsWith(osName);
  }

  private static boolean isWindowsVersion(String windowsVersion) {
    return isOS("Windows") && OS_VERSION != null && OS_VERSION.startsWith(windowsVersion);
  }
}
