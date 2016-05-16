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

import org.eclipse.wb.draw2d.IColorConstants;
import org.eclipse.wb.internal.core.model.property.table.PropertyTable;
import org.eclipse.wb.internal.core.utils.binding.editors.controls.DefaultControlActionsManager;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.TypedListener;
import org.eclipse.swt.widgets.Widget;

/**
 * Combo control for {@link PropertyTable} and combo property editors.
 * 
 * @author scheglov_ke
 * @coverage core.control
 */
public class CCombo3 extends Composite {
  private final long m_createTime = System.currentTimeMillis();
  private final CImageLabel m_text;
  private final Button m_arrow;
  private final Shell m_popup;
  private final Table m_table;
  private boolean m_fullDropdownTableSize = false;

  ////////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  ////////////////////////////////////////////////////////////////////////////
  public CCombo3(Composite parent, int style) {
    super(parent, style);
    addEvents(this, m_comboListener, new int[]{SWT.Dispose, SWT.Move, SWT.Resize});
    // create label
    {
      m_text = new CImageLabel(this, SWT.NONE);
      new DefaultControlActionsManager(m_text);
      addEvents(m_text, m_textListener, new int[]{
          SWT.KeyDown,
          SWT.KeyUp,
          SWT.MouseDown,
          SWT.MouseUp,
          SWT.MouseMove,
          SWT.MouseDoubleClick,
          SWT.Traverse,
          SWT.FocusIn,
          SWT.FocusOut});
    }
    // create arrow
    {
      m_arrow = new Button(this, SWT.ARROW | SWT.DOWN);
      addEvents(m_arrow, m_arrowListener, new int[]{SWT.Selection, SWT.FocusIn, SWT.FocusOut});
    }
    // create popup Shell
    {
      Shell shell = getShell();
      m_popup = new Shell(shell, SWT.NONE);
      m_popup.setLayout(new FillLayout());
    }
    // create table for items
    {
      m_table = new Table(m_popup, SWT.FULL_SELECTION);
      addEvents(m_table, m_tableListener, new int[]{SWT.Selection, SWT.FocusIn, SWT.FocusOut});
      //
      new TableColumn(m_table, SWT.NONE);
    }
    // Focus tracking filter
    {
      final Listener filter = new Listener() {
        private boolean hasFocus;

        public void handleEvent(Event event) {
          boolean old_hasFocus = hasFocus;
          hasFocus =
              m_text.isFocusControl()
                  || m_arrow.isFocusControl()
                  || m_popup.isFocusControl()
                  || m_table.isFocusControl();
          // configure colors
          if (hasFocus) {
            m_text.setBackground(IColorConstants.listSelection);
            m_text.setForeground(IColorConstants.listSelectionText);
          } else {
            m_text.setBackground(IColorConstants.listBackground);
            m_text.setForeground(IColorConstants.listForeground);
          }
          // send FocusOut event
          if (old_hasFocus && !hasFocus) {
            Event e = new Event();
            e.widget = CCombo3.this;
            e.time = event.time;
            notifyListeners(SWT.FocusOut, e);
          }
        }
      };
      getDisplay().addFilter(SWT.FocusIn, filter);
      addListener(SWT.Dispose, new Listener() {
        public void handleEvent(Event event) {
          getDisplay().removeFilter(SWT.FocusIn, filter);
        }
      });
    }
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Events handling
  //
  ////////////////////////////////////////////////////////////////////////////
  private final Listener m_comboListener = new Listener() {
    public void handleEvent(Event event) {
      switch (event.type) {
        case SWT.Dispose :
          if (!m_popup.isDisposed()) {
            m_popup.dispose();
          }
          break;
        case SWT.Move :
          doDropDown(false);
          break;
        case SWT.Resize :
          doResize();
          break;
      }
    }
  };
  private final Listener m_textListener = new Listener() {
    public void handleEvent(final Event event) {
      switch (event.type) {
        case SWT.MouseDown :
          if (System.currentTimeMillis() - m_createTime < 400) {
            // send "logical" double click for case when we just activated combo
            // and almost right away click second time (but first time on editor)
            event.detail = -1;
            notifyListeners(SWT.MouseDoubleClick, event);
            // when we use "auto drop on editor activation" option, this click is
            // is "logically" second one, so it should close combo
            if (!isDisposed()) {
              doDropDown(false);
            }
          } else {
            m_text.setCapture(true);
            doDropDown(!isDropped());
          }
          break;
        case SWT.MouseUp : {
          m_text.setCapture(false);
          TableItem item = getItemUnderCursor(event);
          if (item != null) {
            doDropDown(false);
            sendSelectionEvent(event);
          }
          break;
        }
        case SWT.MouseDoubleClick :
          // prevent resending MouseDoubleClick that we sent on fast MouseDown
          if (event.detail != -1) {
            notifyListeners(SWT.MouseDoubleClick, event);
          }
          break;
        case SWT.MouseMove : {
          TableItem item = getItemUnderCursor(event);
          if (item != null) {
            m_table.setSelection(new TableItem[]{item});
          }
          break;
        }
        case SWT.KeyDown : {
          // check for keyboard navigation and selection
          {
            int selectionIndex = m_table.getSelectionIndex();
            if (event.keyCode == SWT.ARROW_UP) {
              selectionIndex--;
              if (selectionIndex < 0) {
                selectionIndex = m_table.getItemCount() - 1;
              }
              m_table.setSelection(selectionIndex);
              return;
            } else if (event.keyCode == SWT.ARROW_DOWN) {
              m_table.setSelection((selectionIndex + 1) % m_table.getItemCount());
              return;
            } else if (event.character == SWT.CR || event.character == ' ') {
              sendSelectionEvent(event);
              return;
            }
          }
          // be default just resend event
          resendKeyEvent(event);
          break;
        }
        case SWT.KeyUp :
          resendKeyEvent(event);
          break;
      }
    }

    private TableItem getItemUnderCursor(Event event) {
      Point displayLocation = m_text.toDisplay(new Point(event.x, event.y));
      Point tableLocation = m_table.toControl(displayLocation);
      return m_table.getItem(tableLocation);
    }
  };
  private final Listener m_arrowListener = new Listener() {
    public void handleEvent(Event event) {
      switch (event.type) {
      /*case SWT.FocusIn : {
       resendFocusEvent(event);
       break;
       }*/
        case SWT.Selection : {
          doDropDown(!isDropped());
          break;
        }
      }
    }
  };
  private final Listener m_tableListener = new Listener() {
    public void handleEvent(Event event) {
      switch (event.type) {
        case SWT.Selection : {
          doDropDown(false);
          // show selected item in text
          {
            int index = m_table.getSelectionIndex();
            select(index);
          }
          // send selection event
          sendSelectionEvent(event);
          break;
        }
      }
    }
  };

  ////////////////////////////////////////////////////////////////////////////
  //
  // Events utils
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Sends selection event.
   */
  private void sendSelectionEvent(Event event) {
    Event e = new Event();
    e.time = event.time;
    e.stateMask = event.stateMask;
    notifyListeners(SWT.Selection, e);
  }

  /**
   * Resends KeyDown/KeyUp events.
   */
  private void resendKeyEvent(Event event) {
    Event e = new Event();
    e.time = event.time;
    e.character = event.character;
    e.keyCode = event.keyCode;
    e.stateMask = event.stateMask;
    notifyListeners(event.type, e);
  }

  /**
   * Adds given listener as handler for events in given widget.
   */
  private void addEvents(Widget widget, Listener listener, int[] events) {
    for (int i = 0; i < events.length; i++) {
      widget.addListener(events[i], listener);
    }
  }

  /**
   * Adds the listener to receive events.
   */
  public void addSelectionListener(SelectionListener listener) {
    checkWidget();
    if (listener == null) {
      SWT.error(SWT.ERROR_NULL_ARGUMENT);
    }
    TypedListener typedListener = new TypedListener(listener);
    addListener(SWT.Selection, typedListener);
    addListener(SWT.DefaultSelection, typedListener);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Activity
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Sets drop state of combo.
   */
  public void doDropDown(boolean drop) {
    // check, may be we already in this drop state
    if (drop == isDropped()) {
      return;
    }
    // close combo
    if (!drop) {
      m_popup.setVisible(false);
      m_text.setFocus();
      return;
    }
    // open combo
    {
      // prepare popup location
      Point comboSize = getSize();
      Point popupLocation;
      {
        //popupLocation = getParent().toDisplay(getLocation());
        popupLocation = toDisplay(new Point(0, 0));
        popupLocation.y += comboSize.y;
      }
      // calculate and set popup location
      {
        TableColumn tableColumn = m_table.getColumn(0);
        // pack everything
        tableColumn.pack();
        m_table.pack();
        m_popup.pack();
        // calculate bounds
        Rectangle tableBounds = m_table.getBounds();
        tableBounds.height = Math.min(tableBounds.height, m_table.getItemHeight() * 20); // max 20 items without scrolling
        m_table.setBounds(tableBounds);
        // calculate size
        int remainingDisplayHeight = getDisplay().getClientArea().height - popupLocation.y - 10;
        int preferredHeight = Math.min(tableBounds.height, remainingDisplayHeight);
        int remainingDisplayWidth = getDisplay().getClientArea().width - popupLocation.x - 5;
        int preferredWidth =
            isFullDropdownTableWidth()
                ? Math.min(tableBounds.width, remainingDisplayWidth)
                : comboSize.x;
        // set popup bounds calculated as computeTrim basing on combo width and table height paying attention on remaining display space
        Rectangle popupBounds =
            m_popup.computeTrim(popupLocation.x, popupLocation.y, preferredWidth, preferredHeight);
        m_popup.setBounds(popupBounds);
        // adjust column size
        tableColumn.setWidth(m_table.getClientArea().width);
      }
      m_popup.setVisible(true);
      // scroll to selection if needed
      m_table.showSelection();
    }
  }

  /**
   * Initiates "press-hold-drag" sequence.
   */
  public void startDrag() {
    m_text.setCapture(true);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Access
  //
  ////////////////////////////////////////////////////////////////////////////
  public void setFullDropdownTableWidth(boolean freeTableSize) {
    m_fullDropdownTableSize = freeTableSize;
  }

  public boolean isFullDropdownTableWidth() {
    return m_fullDropdownTableSize;
  }

  public boolean isDropped() {
    return m_popup.isVisible();
  }

  public void setQuickSearch(boolean value) {
    // TODO
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Access: items
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Removes all items.
   */
  public void removeAll() {
    TableItem[] items = m_table.getItems();
    for (int index = 0; index < items.length; index++) {
      TableItem item = items[index];
      item.dispose();
    }
  }

  /**
   * Adds new item with given text.
   */
  public void add(String text) {
    add(text, null);
  }

  /**
   * Adds new item with given text and image.
   */
  public void add(String text, Image image) {
    checkWidget();
    TableItem item = new TableItem(m_table, SWT.NONE);
    item.setText(text);
    item.setImage(image);
  }

  /**
   * @return an item at given index
   */
  public String getItem(int index) {
    checkWidget();
    return m_table.getItem(index).getText();
  }

  /**
   * @return the number of items
   */
  public int getItemCount() {
    checkWidget();
    return m_table.getItemCount();
  }

  /**
   * @return the index of the selected item
   */
  public int getSelectionIndex() {
    checkWidget();
    return m_table.getSelectionIndex();
  }

  /**
   * Selects an item with given index.
   */
  public void select(int index) {
    checkWidget();
    if (index == -1) {
      m_table.deselectAll();
      m_text.setText(null);
      m_text.setImage(null);
      return;
    } else {
      TableItem item = m_table.getItem(index);
      m_text.setText(item.getText());
      m_text.setImage(item.getImage());
      m_table.select(index);
    }
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Access: text and image
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Selects item with given text.
   */
  public void setText(String text) {
    // try to find item with given text
    TableItem[] items = m_table.getItems();
    for (int index = 0; index < items.length; index++) {
      TableItem item = items[index];
      if (item.getText().equals(text)) {
        select(index);
        return;
      }
    }
    // not found, remove selection
    select(-1);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Resize support
  // TODO: computeSize
  //
  ////////////////////////////////////////////////////////////////////////////
  protected void doResize() {
    Rectangle clientArea = getClientArea();
    int areaWidth = clientArea.width;
    int areaHeight = clientArea.height;
    // compute sizes of controls
    Point buttonSize = m_arrow.computeSize(areaHeight, areaHeight);
    Point textSize = m_text.computeSize(areaWidth - buttonSize.x, areaHeight);
    // set controls location/size
    m_arrow.setLocation(areaWidth - buttonSize.x, 0);
    m_arrow.setSize(buttonSize);
    m_text.setSize(areaWidth - buttonSize.x, Math.max(textSize.y, areaHeight));
  }
}
