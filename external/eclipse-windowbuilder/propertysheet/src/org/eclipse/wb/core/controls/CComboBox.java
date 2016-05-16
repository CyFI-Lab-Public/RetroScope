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

import com.google.common.collect.Lists;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TypedEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TypedListener;
import org.eclipse.wb.internal.core.model.property.editor.TextControlActionsManager;
import org.eclipse.wb.internal.core.model.property.table.PropertyTable;
import org.eclipse.wb.internal.core.utils.check.Assert;

import java.util.ArrayList;

/**
 * Extended ComboBox control for {@link PropertyTable} and combo property editors.
 *
 * @author sablin_aa
 * @coverage core.control
 */
public class CComboBox extends Composite {
  private Text m_text;
  private Button m_button;
  private Canvas m_canvas;
  private Shell m_popup;
  private TableViewer m_table;
  private boolean m_fullDropdownTableWidth = false;
  private boolean m_wasFocused;

  ////////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  ////////////////////////////////////////////////////////////////////////////
  public CComboBox(Composite parent, int style) {
    super(parent, style);
    createContents(this);
    m_wasFocused = isComboFocused();
    // add display hook
    final Listener displayFocusInHook = new Listener() {
      @Override
    public void handleEvent(Event event) {
        boolean focused = isComboFocused();
        if (m_wasFocused && !focused) {
          // close DropDown on focus out ComboBox
          comboDropDown(false);
        }
        if (event.widget != CComboBox.this) {
          // forward to ComboBox listeners
          if (!m_wasFocused && focused) {
            event.widget = CComboBox.this;
            notifyListeners(SWT.FocusIn, event);
          }
          if (m_wasFocused && !focused) {
            event.widget = CComboBox.this;
            notifyListeners(SWT.FocusOut, event);
          }
        }
        m_wasFocused = focused;
      }
    };
    final Listener displayFocusOutHook = new Listener() {
      @Override
    public void handleEvent(Event event) {
        m_wasFocused = isComboFocused();
      }
    };
    {
      Display display = getDisplay();
      display.addFilter(SWT.FocusIn, displayFocusInHook);
      display.addFilter(SWT.FocusOut, displayFocusOutHook);
    }
    // combo listeners
    addControlListener(new ControlAdapter() {
      @Override
      public void controlResized(ControlEvent e) {
        resizeInner();
      }
    });
    addDisposeListener(new DisposeListener() {
      @Override
    public void widgetDisposed(DisposeEvent e) {
        {
          // remove Display hooks
          Display display = getDisplay();
          display.removeFilter(SWT.FocusIn, displayFocusInHook);
          display.removeFilter(SWT.FocusOut, displayFocusOutHook);
        }
        disposeInner();
      }
    });
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Contents
  //
  ////////////////////////////////////////////////////////////////////////////
  protected void createContents(Composite parent) {
    createText(parent);
    createButton(parent);
    createImage(parent);
    createPopup(parent);
  }

  /**
   * Create Text widget.
   */
  protected void createText(Composite parent) {
    m_text = new Text(parent, SWT.NONE);
    new TextControlActionsManager(m_text);
    // key press processing
    m_text.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        switch (e.keyCode) {
          case SWT.ESC :
            if (isDroppedDown()) {
              // close dropdown
              comboDropDown(false);
              e.doit = false;
            } else {
              // forward to ComboBox listeners
              notifyListeners(SWT.KeyDown, convert2event(e));
            }
            break;
          case SWT.ARROW_UP :
            if (isDroppedDown()) {
              // prev item in dropdown list
              Table table = m_table.getTable();
              int index = table.getSelectionIndex() - 1;
              table.setSelection(index < 0 ? table.getItemCount() - 1 : index);
              e.doit = false;
            } else {
              // forward to ComboBox listeners
              notifyListeners(SWT.KeyDown, convert2event(e));
            }
            break;
          case SWT.ARROW_DOWN :
            if (isDroppedDown()) {
              // next item in dropdown list
              Table table = m_table.getTable();
              int index = table.getSelectionIndex() + 1;
              table.setSelection(index == table.getItemCount() ? 0 : index);
              e.doit = false;
            } else if ((e.stateMask & SWT.ALT) != 0) {
              // force drop down combo
              comboDropDown(true);
              e.doit = false;
              // return focus to text
              setFocus2Text(false);
            } else {
              // forward to ComboBox listeners
              notifyListeners(SWT.KeyDown, convert2event(e));
            }
            break;
          case '\r' :
            Table table = m_table.getTable();
            if (isDroppedDown() && table.getSelectionIndex() != -1) {
              // forward to Table listeners
              table.notifyListeners(SWT.Selection, convert2event(e));
            } else {
              m_text.selectAll();
              setSelectionText(getEditText());
              // forward to ComboBox listeners
              notifyListeners(SWT.Selection, convert2event(e));
            }
            break;
        }
      }
    });
    // modifications processing
    m_text.addModifyListener(new ModifyListener() {
      @Override
    public void modifyText(ModifyEvent e) {
        if (isDroppedDown()) {
          m_table.refresh();
        } else {
          // force drop down combo
          if (m_text.isFocusControl()) {
            comboDropDown(true);
            // return focus to text
            setFocus2Text(false);
          }
        }
      }
    });
  }

  /**
   * Create arrow button.
   */
  protected void createButton(Composite parent) {
    m_button = new Button(parent, SWT.ARROW | SWT.DOWN);
    m_button.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        comboDropDown(!isDroppedDown());
        // return focus to text
        setFocus2Text(true);
      }
    });
  }

  /**
   * Create image canvas.
   */
  protected void createImage(Composite parent) {
    m_canvas = new Canvas(parent, SWT.BORDER);
    m_canvas.addPaintListener(new PaintListener() {
      @Override
    public void paintControl(PaintEvent e) {
        Image selectionImage = getSelectionImage();
        if (selectionImage != null) {
          e.gc.drawImage(selectionImage, 0, 0);
        } else {
          e.gc.fillRectangle(m_canvas.getClientArea());
        }
      }
    });
  }

  /**
   * Create popup shell with table.
   */
  protected void createPopup(Composite parent) {
    m_popup = new Shell(getShell(), SWT.BORDER);
    m_popup.setLayout(new FillLayout());
    createTable(m_popup);
  }

  /**
   * Create table.
   */
  protected void createTable(Composite parent) {
    m_table = new TableViewer(parent, SWT.FULL_SELECTION);
    new TableViewerColumn(m_table, SWT.LEFT);
    m_table.getTable().addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        int selectionIndex = m_table.getTable().getSelectionIndex();
        setSelectionIndex(selectionIndex);
        comboDropDown(false);
        // forward to ComboBox listeners
        notifyListeners(SWT.Selection, convert2event(e));
      }
    });
    m_table.setContentProvider(getContentProvider());
    m_table.setLabelProvider(getLabelProvider());
    m_table.addFilter(getFilterProvider());
  }

  /**
   * Placement inner widgets.
   */
  protected void resizeInner() {
    Rectangle clientArea = getClientArea();
    int rightOccupied = 0;
    int leftOccupied = 0;
    {
      // button
      m_button.setBounds(
          clientArea.width - clientArea.height,
          0,
          clientArea.height,
          clientArea.height);
      rightOccupied = clientArea.height;
    }
    {
      Image selectionImage = getSelectionImage();
      if (selectionImage != null) {
        // image
        m_canvas.setSize(clientArea.height, clientArea.height);
        leftOccupied = clientArea.height;
      } else {
        m_canvas.setSize(1, clientArea.height);
        leftOccupied = 1;
      }
    }
    {
      // text
      m_text.setBounds(
          leftOccupied,
          0,
          clientArea.width - rightOccupied - leftOccupied,
          clientArea.height);
    }
  }

  /**
   * Dispose inner widgets.
   */
  protected void disposeInner() {
    if (!m_popup.isDisposed()) {
      m_popup.dispose();
    }
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Providers
  //
  ////////////////////////////////////////////////////////////////////////////
  protected IContentProvider getContentProvider() {
    return new IStructuredContentProvider() {
      @Override
    public Object[] getElements(Object inputElement) {
        return m_items.toArray(new ComboBoxItem[m_items.size()]);
      }

      @Override
    public void dispose() {
      }

      @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      }
    };
  }

  protected IBaseLabelProvider getLabelProvider() {
    return new LabelProvider() {
      @Override
      public Image getImage(Object element) {
        ComboBoxItem item = (ComboBoxItem) element;
        return item.m_image;
      }

      @Override
      public String getText(Object element) {
        ComboBoxItem item = (ComboBoxItem) element;
        return item.m_label;
      }
    };
  }

  protected ViewerFilter getFilterProvider() {
    return new ViewerFilter() {
      @Override
      public boolean select(Viewer viewer, Object parentElement, Object element) {
        String lookingString = m_text.getText().toLowerCase();
        if (isDroppedDown() && lookingString.length() > 0) {
          ComboBoxItem item = (ComboBoxItem) element;
          return item.m_label.toLowerCase().indexOf(lookingString) != -1;
        }
        return true;
      }
    };
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Items
  //
  ////////////////////////////////////////////////////////////////////////////
  protected static class ComboBoxItem {
    public final String m_label;
    public final Image m_image;

    public ComboBoxItem(String label, Image image) {
      m_label = label;
      m_image = image;
    }
  }

  ArrayList<ComboBoxItem> m_items = Lists.newArrayList();

  /**
   * Add new item.
   */
  public void addItem(String label, Image image) {
    Assert.isTrue(!isDroppedDown());
    m_items.add(new ComboBoxItem(label, image));
  }

  public void addItem(String label) {
    addItem(label, null);
  }

  public void removeAll() {
    m_items.clear();
  }

  public int getItemCount() {
    return m_items.size();
  }

  public String getItemLabel(int index) {
    return m_items.get(index).m_label;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Access
  //
  ////////////////////////////////////////////////////////////////////////////
  public boolean isComboFocused() {
    return isFocusControl()
        || m_text.isFocusControl()
        || m_button.isFocusControl()
        || m_canvas.isFocusControl()
        || m_popup.isFocusControl()
        || m_table.getTable().isFocusControl();
  }

  /**
   * Edit text.
   */
  public String getEditText() {
    return m_text.getText();
  }

  public void setEditText(String text) {
    m_text.setText(text == null ? "" : text);
    m_text.selectAll();
  }

  public void setEditSelection(int start, int end) {
    m_text.setSelection(start, end);
  }

  /**
   * Read only.
   */
  public void setReadOnly(boolean value) {
    m_text.setEditable(!value);
    m_button.setEnabled(!value);
  }

  /**
   * Drop down width.
   */
  public boolean isFullDropdownTableWidth() {
    return m_fullDropdownTableWidth;
  }

  public void setFullDropdownTableWidth(boolean value) {
    Assert.isTrue(!isDroppedDown());
    m_fullDropdownTableWidth = value;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Selection
  //
  ////////////////////////////////////////////////////////////////////////////
  private int m_selectionIndex = -1;

  /**
   * Selection index.
   */
  public int getSelectionIndex() {
    return m_selectionIndex;
  }

  public void setSelectionIndex(int index) {
    m_selectionIndex = index;
    if (isDroppedDown()) {
      m_table.getTable().setSelection(m_selectionIndex);
    }
    setEditText(getSelectionText());
  }

  /**
   * Selection text.
   */
  private String getSelectionText() {
    if (m_selectionIndex != -1 && isDroppedDown()) {
      Object itemData = m_table.getTable().getItem(m_selectionIndex).getData();
      return ((ComboBoxItem) itemData).m_label;
    }
    return null;
  }

  /**
   * Selection image.
   */
  private Image getSelectionImage() {
    return m_selectionIndex != -1 ? m_items.get(m_selectionIndex).m_image : null;
  }

  public void setSelectionText(String label) {
    TableItem[] items = m_table.getTable().getItems();
    for (int i = 0; i < items.length; i++) {
      TableItem item = items[i];
      if (item.getText().equals(label)) {
        setSelectionIndex(i);
        return;
      }
    }
    // no such item
    setSelectionIndex(-1);
    setEditText(label);
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
  // Popup
  //
  ////////////////////////////////////////////////////////////////////////////
  public boolean isDroppedDown() {
    return m_popup.isVisible();
  }

  public void comboDropDown(boolean dropdown) {
    // check, may be we already in this drop state
    if (dropdown == isDroppedDown()) {
      return;
    }
    // close combo
    if (dropdown) {
      // initialize
      m_table.setInput(m_items);
      Table table = m_table.getTable();
      TableColumn column = table.getColumn(0);
      column.pack();
      table.pack();
      m_popup.pack();
      // compute table size
      Rectangle tableBounds = table.getBounds();
      tableBounds.height = Math.min(tableBounds.height, table.getItemHeight() * 15);// max 15 items without scrolling
      table.setBounds(tableBounds);
      // prepare popup point
      Point comboLocation = toDisplay(new Point(0, 0));
      Point comboSize = getSize();
      // compute popup size
      Display display = getDisplay();
      Rectangle clientArea = display.getClientArea();
      int remainingDisplayHeight = clientArea.height - comboLocation.y - comboSize.y - 10;
      int preferredHeight = Math.min(tableBounds.height, remainingDisplayHeight);
      int remainingDisplayWidth = clientArea.width - comboLocation.x - 10;
      int preferredWidth =
          isFullDropdownTableWidth()
              ? Math.min(tableBounds.width, remainingDisplayWidth)
              : comboSize.x;
      Rectangle popupBounds =
          new Rectangle(comboLocation.x,
              comboLocation.y + comboSize.y,
              preferredWidth,
              preferredHeight);
      Rectangle trimBounds =
          m_popup.computeTrim(popupBounds.x, popupBounds.y, popupBounds.width, popupBounds.height);
      m_popup.setBounds(popupBounds.x, popupBounds.y, 2 * popupBounds.width - trimBounds.width, 2
          * popupBounds.height
          - trimBounds.height);
      // adjust column size
      column.setWidth(table.getClientArea().width);
      // show popup
      m_popup.setVisible(true);
      table.setSelection(getSelectionIndex());
    } else {
      // hide popup
      m_popup.setVisible(false);
    }
  }

  protected final void setFocus2Text(final boolean selectAll) {
    getDisplay().asyncExec(new Runnable() {
      final boolean m_selectAll = selectAll;

      @Override
    public void run() {
        if (!m_text.isDisposed()) {
          m_text.setFocus();
          if (m_selectAll) {
            m_text.selectAll();
          }
        }
      }
    });
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Utilities
  //
  ////////////////////////////////////////////////////////////////////////////
  protected static Event convert2event(TypedEvent tEvent) {
    Event event = new Event();
    event.widget = tEvent.widget;
    event.display = tEvent.display;
    event.widget = tEvent.widget;
    event.time = tEvent.time;
    event.data = tEvent.data;
    if (tEvent instanceof KeyEvent) {
      KeyEvent kEvent = (KeyEvent) tEvent;
      event.character = kEvent.character;
      event.keyCode = kEvent.keyCode;
      event.stateMask = kEvent.stateMask;
      event.doit = kEvent.doit;
    }
    if (tEvent instanceof SelectionEvent) {
      SelectionEvent sEvent = (SelectionEvent) tEvent;
      event.item = sEvent.item;
      event.x = sEvent.x;
      event.y = sEvent.y;
      event.width = sEvent.width;
      event.height = sEvent.height;
      event.detail = sEvent.detail;
      event.stateMask = sEvent.stateMask;
      event.text = sEvent.text;
      event.doit = sEvent.doit;
    }
    return event;
  }
}
