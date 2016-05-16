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
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.TypedListener;

import java.util.Locale;

/**
 * {@link Control} like {@link Combo} or {@link CCombo} that shows {@link Table} with image/text as
 * drop-down.
 * 
 * @author mitin_aa
 * @author scheglov_ke
 * @coverage core.control
 */
public class CTableCombo extends Composite {
  protected Button m_arrow;
  protected CImageLabel m_text;
  protected Shell m_popup;
  protected Table m_table;
  protected boolean hasFocus;

  //
  public CTableCombo(Composite parent, int style) {
    super(parent, style = checkStyle(style));
    init(parent, style);
  }

  static int checkStyle(int style) {
    int mask = SWT.BORDER | SWT.READ_ONLY | SWT.FLAT;
    return style & mask;
  }

  private void init(Composite parent, int style) {
    m_arrow = new Button(this, SWT.ARROW | SWT.DOWN | SWT.NO_FOCUS);
    m_text = new CImageLabel(this, style & ~SWT.BORDER);
    m_text.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
    final Shell shell = getShell();
    m_popup = new Shell(shell, SWT.NONE);
    m_table = new Table(m_popup, SWT.FULL_SELECTION);
    new TableColumn(m_table, SWT.NONE);
    Listener listener = new Listener() {
      public void handleEvent(Event event) {
        if (m_popup == event.widget) {
          handlePopupEvent(event);
          return;
        }
        if (m_text == event.widget) {
          handleTextEvent(event);
          return;
        }
        if (m_table == event.widget) {
          handleTableEvent(event);
          return;
        }
        if (m_arrow == event.widget) {
          handleArrowEvent(event);
          return;
        }
        if (CTableCombo.this == event.widget) {
          handleComboEvent(event);
          return;
        }
      }
    };
    final Listener shellListener = new Listener() {
      public void handleEvent(Event event) {
        switch (event.type) {
          case SWT.Dispose :
          case SWT.Move :
          case SWT.Resize :
            if (!isDisposed()) {
              dropDown(false);
            }
            break;
        }
      }
    };
    final int[] comboEvents = {SWT.Dispose, SWT.Move, SWT.Resize};
    for (int i = 0; i < comboEvents.length; i++) {
      addListener(comboEvents[i], listener);
      // HACK: hide popup when parent changed
      shell.addListener(comboEvents[i], shellListener);
    }
    addDisposeListener(new DisposeListener() {
      public void widgetDisposed(DisposeEvent e) {
        for (int i = 0; i < comboEvents.length; i++) {
          shell.removeListener(comboEvents[i], shellListener);
        }
      }
    });
    int[] popupEvents = {SWT.Close, SWT.Paint, SWT.Deactivate};
    for (int i = 0; i < popupEvents.length; i++) {
      m_popup.addListener(popupEvents[i], listener);
    }
    int[] textEvents =
        {
            SWT.KeyDown,
            SWT.KeyUp,
            SWT.Modify,
            SWT.MouseDown,
            SWT.MouseUp,
            SWT.MouseDoubleClick,
            SWT.Traverse,
            SWT.FocusIn,
            SWT.FocusOut};
    for (int i = 0; i < textEvents.length; i++) {
      m_text.addListener(textEvents[i], listener);
    }
    int[] tableEvents =
        {
            SWT.MouseUp,
            SWT.Selection,
            SWT.Traverse,
            SWT.KeyDown,
            SWT.KeyUp,
            SWT.FocusIn,
            SWT.FocusOut};
    for (int i = 0; i < tableEvents.length; i++) {
      m_table.addListener(tableEvents[i], listener);
    }
    int[] arrowEvents = {SWT.Selection, SWT.FocusIn, SWT.FocusOut};
    for (int i = 0; i < arrowEvents.length; i++) {
      m_arrow.addListener(arrowEvents[i], listener);
    }
  }

  protected void handleTableEvent(Event event) {
    switch (event.type) {
      case SWT.FocusIn : {
        if (hasFocus) {
          return;
        }
        hasFocus = true;
        Event e = new Event();
        e.time = event.time;
        notifyListeners(SWT.FocusIn, e);
        break;
      }
      case SWT.FocusOut : {
        final int time = event.time;
        event.display.asyncExec(new Runnable() {
          public void run() {
            if (CTableCombo.this.isDisposed()) {
              return;
            }
            Control focusControl = getDisplay().getFocusControl();
            if (focusControl == m_text || focusControl == m_arrow) {
              return;
            }
            hasFocus = false;
            Event e = new Event();
            e.time = time;
            notifyListeners(SWT.FocusOut, e);
          }
        });
        break;
      }
      case SWT.MouseUp : {
        if (event.button != 1) {
          return;
        }
        dropDown(false);
        Event e = new Event();
        e.time = event.time;
        notifyListeners(SWT.DefaultSelection, e);
        break;
      }
      case SWT.Selection : {
        int index = m_table.getSelectionIndex();
        if (index == -1) {
          return;
        }
        TableItem item = m_table.getItem(index);
        m_text.setText(item.getText());
        m_text.setImage(item.getImage());
        //m_text.selectAll();
        m_table.setSelection(index);
        Event e = new Event();
        e.time = event.time;
        e.stateMask = event.stateMask;
        e.doit = event.doit;
        notifyListeners(SWT.Selection, e);
        event.doit = e.doit;
        dropDown(false);
        break;
      }
      case SWT.Traverse : {
        switch (event.detail) {
          case SWT.TRAVERSE_TAB_NEXT :
          case SWT.TRAVERSE_RETURN :
          case SWT.TRAVERSE_ESCAPE :
          case SWT.TRAVERSE_ARROW_PREVIOUS :
          case SWT.TRAVERSE_ARROW_NEXT :
            event.doit = false;
            break;
        }
        Event e = new Event();
        e.time = event.time;
        e.detail = event.detail;
        e.doit = event.doit;
        e.keyCode = event.keyCode;
        notifyListeners(SWT.Traverse, e);
        event.doit = e.doit;
        break;
      }
      case SWT.KeyUp : {
        Event e = new Event();
        e.time = event.time;
        e.character = event.character;
        e.keyCode = event.keyCode;
        e.stateMask = event.stateMask;
        notifyListeners(SWT.KeyUp, e);
        break;
      }
      case SWT.KeyDown : {
        if (event.character == SWT.ESC) {
          // escape key cancels popups
          dropDown(false);
        }
        if (event.character == SWT.CR || event.character == '\t') {
          // Enter and Tab cause default selection
          dropDown(false);
          Event e = new Event();
          e.time = event.time;
          e.stateMask = event.stateMask;
          notifyListeners(SWT.DefaultSelection, e);
        }
        // At this point the widget may have been disposed.
        // If so, do not continue.
        if (isDisposed()) {
          break;
        }
        Event e = new Event();
        e.time = event.time;
        e.character = event.character;
        e.keyCode = event.keyCode;
        e.stateMask = event.stateMask;
        notifyListeners(SWT.KeyDown, e);
        break;
      }
    }
  }

  protected void handlePopupEvent(Event event) {
    switch (event.type) {
      case SWT.Paint :
        // draw black rectangle around list
        Rectangle listRect = m_table.getBounds();
        Color black = getDisplay().getSystemColor(SWT.COLOR_BLACK);
        event.gc.setForeground(black);
        event.gc.drawRectangle(0, 0, listRect.width + 1, listRect.height + 1);
        break;
      case SWT.Close :
        event.doit = false;
        dropDown(false);
        break;
    }
  }

  protected void handleComboEvent(Event event) {
    switch (event.type) {
      case SWT.Dispose :
        if (m_popup != null && !m_popup.isDisposed()) {
          m_popup.dispose();
        }
        m_popup = null;
        m_text = null;
        m_arrow = null;
        break;
      case SWT.Move :
        dropDown(false);
        break;
      case SWT.Resize :
        internalLayout();
        break;
    }
  }

  protected void handleArrowEvent(Event event) {
    switch (event.type) {
      case SWT.FocusIn : {
        if (hasFocus) {
          return;
        }
        hasFocus = true;
        Event e = new Event();
        e.time = event.time;
        notifyListeners(SWT.FocusIn, e);
        break;
      }
      case SWT.Selection : {
        boolean wasDropped = isDropped();
        dropDown(!wasDropped);
        if (wasDropped) {
          m_text.forceFocus();
        }
        break;
      }
    }
  }

  protected void handleTextEvent(Event event) {
    switch (event.type) {
      case SWT.FocusIn : {
        if (hasFocus) {
          return;
        }
        hasFocus = true;
        //if (getEditable())
        Event e = new Event();
        e.time = event.time;
        notifyListeners(SWT.FocusIn, e);
        break;
      }
      case SWT.FocusOut : {
        final int time = event.time;
        event.display.asyncExec(new Runnable() {
          public void run() {
            if (CTableCombo.this.isDisposed()) {
              return;
            }
            Control focusControl = getDisplay().getFocusControl();
            if (focusControl == m_table
                || focusControl == m_arrow
                || focusControl != null
                && focusControl.getParent() == CTableCombo.this) {
              return;
            }
            hasFocus = false;
            Event e = new Event();
            e.time = time;
            notifyListeners(SWT.FocusOut, e);
          }
        });
        break;
      }
      case SWT.KeyDown : {
        if (event.character == SWT.ESC) { // escape key cancels popup
          dropDown(false);
        }
        if (event.character == SWT.CR) {
          dropDown(false);
          Event e = new Event();
          e.time = event.time;
          e.stateMask = event.stateMask;
          notifyListeners(SWT.DefaultSelection, e);
        }
        // At this point the widget may have been disposed.
        // If so, do not continue.
        if (isDisposed()) {
          break;
        }
        if (event.character == '+') {
          dropDown(true);
        }
        if (isDropped()) {
          if (event.keyCode == SWT.ARROW_UP || event.keyCode == SWT.ARROW_DOWN) {
            int oldIndex = getSelectionIndex();
            if (event.keyCode == SWT.ARROW_UP) {
              select(Math.max(oldIndex - 1, 0));
            } else {
              select(Math.min(oldIndex + 1, getItemCount() - 1));
            }
            if (oldIndex != getSelectionIndex()) {
              Event e = new Event();
              e.time = event.time;
              e.stateMask = event.stateMask;
              notifyListeners(SWT.Selection, e);
            }
            // At this point the widget may have been disposed.
            // If so, do not continue.
            if (isDisposed()) {
              break;
            }
          }
        }
        if (Character.isLetter(event.character)) {
          int oldIndex = getSelectionIndex();
          int index = -1;
          for (int i = 0; i < getItemCount(); i++) {
            String item = getItem(i).toUpperCase(Locale.ENGLISH);
            if (item.length() != 0 && item.charAt(0) == Character.toUpperCase(event.character)) {
              index = i;
              break;
            }
          }
          if (index != -1) {
            select(Math.max(index, 0));
            if (oldIndex != getSelectionIndex()) {
              Event e = new Event();
              e.time = event.time;
              e.stateMask = event.stateMask;
              notifyListeners(SWT.Selection, e);
            }
          }
        }
        Event e = new Event();
        e.time = event.time;
        e.character = event.character;
        e.keyCode = event.keyCode;
        e.stateMask = event.stateMask;
        if (m_text != null && !m_text.isDisposed()) {
          notifyListeners(SWT.KeyDown, e);
        }
        break;
      }
      case SWT.KeyUp : {
        Event e = new Event();
        e.time = event.time;
        e.character = event.character;
        e.keyCode = event.keyCode;
        e.stateMask = event.stateMask;
        notifyListeners(SWT.KeyUp, e);
        break;
      }
      case SWT.Modify : {
        m_table.deselectAll();
        Event e = new Event();
        e.time = event.time;
        notifyListeners(SWT.Modify, e);
        break;
      }
      case SWT.MouseDown : {
        if (event.button != 1) {
          return;
        }
        m_text.forceFocus();
        boolean dropped = isDropped();
        dropDown(!dropped);
        if (!dropped) {
          m_text.forceFocus();
        }
        break;
      }
      case SWT.MouseDoubleClick : {
        notifyListeners(SWT.MouseDoubleClick, event);
        break;
      }
      case SWT.Traverse : {
        switch (event.detail) {
          case SWT.TRAVERSE_RETURN :
          case SWT.TRAVERSE_ARROW_PREVIOUS :
          case SWT.TRAVERSE_ARROW_NEXT :
            // The enter causes default selection and
            // the arrow keys are used to manipulate the list contents so
            // do not use them for traversal.
            event.doit = false;
            break;
          case SWT.TRAVERSE_TAB_NEXT :
          case SWT.TRAVERSE_TAB_PREVIOUS :
            event.doit = true;
            break;
        }
        Event e = new Event();
        e.time = event.time;
        e.detail = event.detail;
        e.doit = event.doit;
        e.keyCode = event.keyCode;
        notifyListeners(SWT.Traverse, e);
        event.doit = e.doit;
        break;
      }
    }
  }

  private void dropDown(boolean drop) {
    if (drop == isDropped()) {
      return;
    }
    if (!drop) {
      m_popup.setVisible(false);
      m_text.setFocus();
      return;
    }
    int index = m_table.getSelectionIndex();
    if (index != -1) {
      m_table.setTopIndex(index);
      m_table.setSelection(index);
    }
    m_table.pack();
    Point point = getParent().toDisplay(getLocation());
    Point comboSize = getSize();
    //Rectangle tableRect = m_table.getBounds();
    //int width = Math.max(comboSize.x, tableRect.width + 2);
    int width = comboSize.x - 1;
    // only one column 
    m_table.getColumn(0).setWidth(width - 5);
    if (!(m_popup.getLayout() instanceof FillLayout)) {
      m_popup.setLayout(new FillLayout());
    }
    int itemCount = m_table.getItemCount();
    if (itemCount > 20) {
      itemCount = 20;
    }
    int height =
        Math.min(
            m_table.getItemHeight() * itemCount + 5,
            Display.getCurrent().getClientArea().height - point.y - 20);
    m_popup.setBounds(point.x, point.y + comboSize.y, width, height);
    m_popup.layout();
    m_popup.setVisible(true);
    m_text.setFocus();
    if (index != -1) {
      m_table.setTopIndex(index);
      m_table.setSelection(index);
    }
  }

  @Override
  public Point computeSize(int wHint, int hHint, boolean changed) {
    checkWidget();
    int width = 0, height = 0;
    Point textSize = m_text.computeSize(wHint, SWT.DEFAULT, changed);
    Point arrowSize = m_arrow.computeSize(SWT.DEFAULT, SWT.DEFAULT, changed);
    int tableWidth;
    {
      TableColumn column = m_table.getColumn(0);
      column.pack();
      tableWidth = column.getWidth();
    }
    //
    int borderWidth = getBorderWidth();
    height = Math.max(hHint, Math.max(textSize.y, arrowSize.y) + 2 * borderWidth);
    width = Math.max(wHint, Math.max(textSize.x + arrowSize.x, tableWidth) + 2 * borderWidth);
    //
    return new Point(width, height);
  }

  private void internalLayout() {
    if (isDropped()) {
      dropDown(false);
    }
    Rectangle rect = getClientArea();
    int width = rect.width;
    int height = rect.height;
    Point arrowSize = m_arrow.computeSize(SWT.DEFAULT, height);
    m_text.setBounds(rect.x, rect.y, width - arrowSize.x, height);
    m_arrow.setBounds(rect.x + width - arrowSize.x, rect.y, arrowSize.x, arrowSize.y);
  }

  private boolean isDropped() {
    return m_popup.isVisible();
  }

  @Override
  public boolean isFocusControl() {
    checkWidget();
    if (m_text.isFocusControl()
        || m_arrow.isFocusControl()
        || m_table.isFocusControl()
        || m_popup.isFocusControl()) {
      return true;
    }
    return super.isFocusControl();
  }

  public void select(int index) {
    checkWidget();
    if (index == -1) {
      m_table.deselectAll();
      m_text.setText(""); //$NON-NLS-1$
      m_text.setImage(null);
      return;
    }
    if (0 <= index && index < m_table.getItemCount()) {
      if (index != getSelectionIndex()) {
        TableItem item = m_table.getItem(index);
        m_text.setText(item.getText());
        m_text.setImage(item.getImage());
        m_table.select(index);
        m_table.showSelection();
      }
    }
  }

  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    if (enabled) {
      m_text.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
    } else {
      m_text.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
    }
  }

  public String getItem(int index) {
    checkWidget();
    return m_table.getItem(index).getText();
  }

  public int getSelectionIndex() {
    checkWidget();
    return m_table.getSelectionIndex();
  }

  public void removeAll() {
    checkWidget();
    m_text.setText(""); //$NON-NLS-1$
    m_text.setImage(null);
    m_table.removeAll();
  }

  public int indexOf(String string) {
    return indexOf(string, 0);
  }

  public int indexOf(String string, int start) {
    checkWidget();
    if (string == null) {
      return -1;
    }
    TableItem[] items = m_table.getItems();
    for (int i = start; i < items.length; i++) {
      TableItem item = items[i];
      if (item.getText().equalsIgnoreCase(string)) {
        return i;
      }
    }
    return -1;
  }

  public String getText() {
    return m_text.getText();
  }

  public int getItemCount() {
    checkWidget();
    return m_table.getItemCount();
  }

  protected void setText(String string) {
    m_text.setText(string);
  }

  protected void setImage(Image image) {
    m_text.setImage(image);
  }

  public void add(String text) {
    add(text, null);
  }

  public void add(String text, Image image) {
    checkWidget();
    TableItem item = new TableItem(m_table, SWT.NONE);
    item.setText(text);
    item.setImage(image);
  }

  public void addSelectionListener(SelectionListener listener) {
    checkWidget();
    if (listener == null) {
      SWT.error(SWT.ERROR_NULL_ARGUMENT);
    }
    TypedListener typedListener = new TypedListener(listener);
    addListener(SWT.Selection, typedListener);
    addListener(SWT.DefaultSelection, typedListener);
  }
}
