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
package org.eclipse.wb.core.controls.flyout;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tracker;
import org.eclipse.wb.core.controls.Messages;
import org.eclipse.wb.draw2d.IColorConstants;
import org.eclipse.wb.draw2d.ICursorConstants;
import org.eclipse.wb.internal.core.utils.ui.DrawUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link FlyoutControlComposite} is container for two {@link Control}'s. One (client control) is
 * used to fill client area. Second (flyout control) can be docked to any enabled position or
 * temporary hidden.
 *
 * @author scheglov_ke
 * @coverage core.control
 */
public final class FlyoutControlComposite extends Composite {
  private static final int RESIZE_WIDTH = 5;
  private static final int TITLE_LINES = 30;
  private static final int TITLE_MARGIN = 5;
  private static final Font TITLE_FONT = JFaceResources.getFontRegistry().getBold(
      JFaceResources.DEFAULT_FONT);
  ////////////////////////////////////////////////////////////////////////////
  //
  // Images
  //
  ////////////////////////////////////////////////////////////////////////////
  private static final Image PIN = loadImage("icons/pin.gif");
  private static final Image ARROW_LEFT = loadImage("icons/arrow_left.gif");
  private static final Image ARROW_RIGHT = loadImage("icons/arrow_right.gif");
  private static final Image ARROW_TOP = loadImage("icons/arrow_top.gif");
  private static final Image ARROW_BOTTOM = loadImage("icons/arrow_bottom.gif");

  private static Image loadImage(String path) {
    return DrawUtils.loadImage(FlyoutControlComposite.class, path);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Instance fields
  //
  ////////////////////////////////////////////////////////////////////////////
  private final IFlyoutPreferences m_preferences;
  private final FlyoutContainer m_flyoutContainer;
  private int m_minWidth = 150;
  private int m_validDockLocations = -1;
  private final List<IFlyoutMenuContributor> m_menuContributors =
      new ArrayList<IFlyoutMenuContributor>();

  ////////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  ////////////////////////////////////////////////////////////////////////////
  public FlyoutControlComposite(Composite parent, int style, IFlyoutPreferences preferences) {
    super(parent, style);
    m_preferences = preferences;
    // add listeners
    addListener(SWT.Resize, new Listener() {
      @Override
    public void handleEvent(Event event) {
        if (getShell().getMinimized()) {
          return;
        }
        layout();
      }
    });
    // create container for flyout control
    m_flyoutContainer = new FlyoutContainer(this, SWT.NO_BACKGROUND);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Parents
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * @return the parent {@link Composite} for flyout {@link Control}.
   */
  public Composite getFlyoutParent() {
    return m_flyoutContainer;
  }

  /**
   * @return the parent {@link Composite} for client {@link Control}.
   */
  public Composite getClientParent() {
    return this;
  }

  /**
   * Sets the bit set with valid docking locations.
   */
  public void setValidDockLocations(int validDockLocations) {
    m_validDockLocations = validDockLocations;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Access
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Sets the minimal width of flyout.
   */
  public void setMinWidth(int minWidth) {
    m_minWidth = minWidth;
  }

  /**
   * Sets the text of title.
   */
  public void setTitleText(String text) {
    m_flyoutContainer.setTitleText(text);
  }

  /**
   * Adds new {@link IFlyoutMenuContributor}.
   */
  public void addMenuContributor(IFlyoutMenuContributor contributor) {
    if (!m_menuContributors.contains(contributor)) {
      m_menuContributors.add(contributor);
    }
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Layout
  //
  ////////////////////////////////////////////////////////////////////////////
  @Override
  public void layout() {
    Rectangle clientArea = getClientArea();
    int state = m_preferences.getState();
    Control client = getChildren()[1];
    // check, may be "clientArea" is empty, for example because CTabFolder page is not visible
    if (clientArea.width == 0 || clientArea.height == 0) {
      return;
    }
    // check, maybe flyout has no Control, so "client" should fill client area
    if (m_flyoutContainer.getControl() == null
            // BEGIN ADT MODIFICATIONS
            || !m_flyoutContainer.getControl().getVisible()
            // END ADT MODIFICATIONS
            ) {
      m_flyoutContainer.setBounds(0, 0, 0, 0);
      client.setBounds(clientArea);
      return;
    }
    // prepare width to display
    int width;
    int offset;
    if (state == IFlyoutPreferences.STATE_OPEN) {
      width = m_preferences.getWidth();
      // limit maximum value
      if (isHorizontal()) {
        width = Math.min(clientArea.width / 2, width);
      } else {
        width = Math.min(clientArea.height / 2, width);
      }
      // limit minimum value
      width = Math.max(width, m_minWidth);
      width = Math.max(width, 2 * m_flyoutContainer.m_titleHeight + m_flyoutContainer.m_titleWidth);
      // remember actual width
      m_preferences.setWidth(width);
      //
      offset = width;
    } else if (state == IFlyoutPreferences.STATE_EXPANDED) {
      offset = m_flyoutContainer.m_titleHeight;
      width = m_preferences.getWidth();
    } else {
      width = m_flyoutContainer.m_titleHeight;
      offset = width;
    }
    // change bounds for flyout container and client control
    {
      if (isWest()) {
        m_flyoutContainer.setBounds(0, 0, width, clientArea.height);
        client.setBounds(offset, 0, clientArea.width - offset, clientArea.height);
      } else if (isEast()) {
        m_flyoutContainer.setBounds(clientArea.width - width, 0, width, clientArea.height);
        client.setBounds(0, 0, clientArea.width - offset, clientArea.height);
      } else if (isNorth()) {
        m_flyoutContainer.setBounds(0, 0, clientArea.width, width);
        client.setBounds(0, offset, clientArea.width, clientArea.height - offset);
      } else if (isSouth()) {
        m_flyoutContainer.setBounds(0, clientArea.height - width, clientArea.width, width);
        client.setBounds(0, 0, clientArea.width, clientArea.height - offset);
      }
    }
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Internal utils
  //
  ////////////////////////////////////////////////////////////////////////////
  private boolean isHorizontal() {
    return isWest() || isEast();
  }

  private boolean isWest() {
    return getDockLocation() == IFlyoutPreferences.DOCK_WEST;
  }

  private boolean isEast() {
    return getDockLocation() == IFlyoutPreferences.DOCK_EAST;
  }

  private boolean isNorth() {
    return getDockLocation() == IFlyoutPreferences.DOCK_NORTH;
  }

  private boolean isSouth() {
    return getDockLocation() == IFlyoutPreferences.DOCK_SOUTH;
  }

  /**
   * @return <code>true</code> if given docking location is valid.
   */
  private boolean isValidDockLocation(int location) {
    return (location & m_validDockLocations) == location;
  }

  /**
   * @return current docking location.
   */
  private int getDockLocation() {
    return m_preferences.getDockLocation();
  }

  /**
   * Sets new docking location.
   */
  private void setDockLocation(int dockLocation) {
    m_preferences.setDockLocation(dockLocation);
    layout();
  }

  // BEGIN ADT MODIFICATIONS
  /**
   * Applies the given preferences into the preferences of this flyout
   * control. This does not cause any visual updates; call {@link #layout()}
   * to update the widget.
   *
   * @param preferences the preferences to apply
   */
  public void apply(IFlyoutPreferences preferences) {
    m_preferences.setDockLocation(preferences.getDockLocation());
    m_preferences.setState(preferences.getState());
    m_preferences.setWidth(preferences.getWidth());
  }

  /** If the flyout hover is showing, dismiss it */
  public void dismissHover() {
    if (m_flyoutContainer != null) {
      m_flyoutContainer.dismissHover();
    }
  }

  /** Sets a listener to be modified when windows are opened, collapsed and expanded */
  public void setListener(IFlyoutListener listener) {
    assert m_listener == null; // Only one listener supported
    m_listener = listener;
  }
  private IFlyoutListener m_listener;
  // END ADT MODIFICATIONS

  ////////////////////////////////////////////////////////////////////////////
  //
  // FlyoutContainer
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Container for flyout {@link Control}.
   *
   * @author scheglov_ke
   */
  private final class FlyoutContainer extends Composite {
    ////////////////////////////////////////////////////////////////////////////
    //
    // Container
    //
    ////////////////////////////////////////////////////////////////////////////
    public FlyoutContainer(Composite parent, int style) {
      super(parent, style);
      configureMenu();
      updateTitleImage("Flyout");
      // add listeners
      addListener(SWT.Dispose, new Listener() {
        @Override
        public void handleEvent(Event event) {
          if (m_titleImage != null) {
            m_titleImage.dispose();
            m_titleImageRotated.dispose();
            m_titleImage = null;
            m_titleImageRotated = null;
          }
          if (m_backImage != null) {
            m_backImage.dispose();
            m_backImage = null;
          }
        }
      });
      {
        Listener listener = new Listener() {
          @Override
        public void handleEvent(Event event) {
            layout();
          }
        };
        addListener(SWT.Move, listener);
        addListener(SWT.Resize, listener);
      }
      addListener(SWT.Paint, new Listener() {
        @Override
        public void handleEvent(Event event) {
          handlePaint(event.gc);
        }
      });
      // mouse listeners
      addMouseListener(new MouseAdapter() {
        @Override
        public void mouseDown(MouseEvent event) {
          if (event.button == 1) {
            handle_mouseDown(event);
          }
        }

        @Override
        public void mouseUp(MouseEvent event) {
          if (event.button == 1) {
            handle_mouseUp(event);
          }
        }
      });
      addMouseTrackListener(new MouseTrackAdapter() {
        @Override
        public void mouseExit(MouseEvent e) {
          m_stateHover = false;
          redraw();
          setCursor(null);
        }

        @Override
        public void mouseHover(MouseEvent e) {
          handle_mouseHover();
        }
      });
      addMouseMoveListener(new MouseMoveListener() {
        @Override
        public void mouseMove(MouseEvent event) {
          handle_mouseMove(event);
        }
      });
    }

    // BEGIN ADT MODIFICATIONS
    private void dismissHover() {
      int state = m_preferences.getState();
      if (state == IFlyoutPreferences.STATE_EXPANDED) {
        state = IFlyoutPreferences.STATE_COLLAPSED;
        m_preferences.setState(state);
        redraw();
        FlyoutControlComposite.this.layout();
        if (m_listener != null) {
            m_listener.stateChanged(IFlyoutPreferences.STATE_EXPANDED, state);
        }
      }
    }
    // END END MODIFICATIONS

    ////////////////////////////////////////////////////////////////////////////
    //
    // Events: mouse
    //
    ////////////////////////////////////////////////////////////////////////////
    private boolean m_resize;
    private boolean m_stateHover;

    /**
     * Handler for {@link SWT#MouseDown} event.
     */
    private void handle_mouseDown(MouseEvent event) {
      if (m_stateHover) {
        int state = m_preferences.getState();
        // BEGIN ADT MODIFICATIONS
        int oldState = state;
        // END ADT MODIFICATIONS
        if (state == IFlyoutPreferences.STATE_OPEN) {
          state = IFlyoutPreferences.STATE_COLLAPSED;
        } else {
          state = IFlyoutPreferences.STATE_OPEN;
        }
        m_preferences.setState(state);
        redraw();
        FlyoutControlComposite.this.layout();
        // BEGIN ADT MODIFICATIONS
        if (m_listener != null) {
          m_listener.stateChanged(oldState, state);
        }
        // END ADT MODIFICATIONS
      } else if (getCursor() == ICursorConstants.SIZEWE || getCursor() == ICursorConstants.SIZENS) {
        m_resize = true;
      } else if (getCursor() == ICursorConstants.SIZEALL) {
        handleDocking();
      }
    }

    /**
     * Handler for {@link SWT#MouseUp} event.
     */
    private void handle_mouseUp(MouseEvent event) {
      if (m_resize) {
        m_resize = false;
        handle_mouseMove(event);
      }
    }

    /**
     * Handler for {@link SWT#MouseMove} event.
     */
    private void handle_mouseMove(MouseEvent event) {
      final FlyoutControlComposite container = FlyoutControlComposite.this;
      if (m_resize) {
        // prepare width
        int width;
        if (isHorizontal()) {
          width = getSize().x;
        } else {
          width = getSize().y;
        }
        // prepare new width
        int newWidth = width;
        if (isWest()) {
          newWidth = event.x + RESIZE_WIDTH / 2;
        } else if (isEast()) {
          newWidth = width - event.x + RESIZE_WIDTH / 2;
        } else if (isNorth()) {
          newWidth = event.y + RESIZE_WIDTH / 2;
        } else if (isSouth()) {
          newWidth = width - event.y + RESIZE_WIDTH / 2;
        }
        // update width
        if (newWidth != width) {
          m_preferences.setWidth(newWidth);
          redraw();
          container.layout();
        }
      } else {
        Rectangle clientArea = getClientArea();
        boolean inside = clientArea.contains(event.x, event.y);
        int x = event.x;
        int y = event.y;
        if (inside) {
          // check for state
          {
            boolean oldStateHover = m_stateHover;
            if (isEast()) {
              m_stateHover = x > clientArea.width - m_titleHeight && y < m_titleHeight;
            } else {
              m_stateHover = x < m_titleHeight && y < m_titleHeight;
            }
            if (m_stateHover != oldStateHover) {
              redraw();
            }
            if (m_stateHover) {
              setCursor(null);
              return;
            }
          }
          // check for resize band
          if (isOpenExpanded()) {
            if (isWest() && x >= clientArea.width - RESIZE_WIDTH) {
              setCursor(ICursorConstants.SIZEWE);
            } else if (isEast() && x <= RESIZE_WIDTH) {
              setCursor(ICursorConstants.SIZEWE);
            } else if (isNorth() && y >= clientArea.height - RESIZE_WIDTH) {
              setCursor(ICursorConstants.SIZENS);
            } else if (isSouth() && y <= RESIZE_WIDTH) {
              setCursor(ICursorConstants.SIZENS);
            } else {
              setCursor(null);
            }
          }
          // check for docking
          if (getCursor() == null) {
            setCursor(ICursorConstants.SIZEALL);
          }
        } else {
          setCursor(null);
        }
      }
    }

    /**
     * Handler for {@link SWT#MouseHover} event - temporary expands flyout and collapse again when
     * mouse moves above client.
     */
    private void handle_mouseHover() {
      if (m_preferences.getState() == IFlyoutPreferences.STATE_COLLAPSED && !m_stateHover) {
        m_preferences.setState(IFlyoutPreferences.STATE_EXPANDED);
        //
        final FlyoutControlComposite container = FlyoutControlComposite.this;
        container.layout();
        // BEGIN ADT MODIFICATIONS
        if (m_listener != null) {
            m_listener.stateChanged(IFlyoutPreferences.STATE_COLLAPSED,
                    IFlyoutPreferences.STATE_EXPANDED);
        }
        // END ADT MODIFICATIONS
        // add listeners
        Listener listener = new Listener() {
          @Override
        public void handleEvent(Event event) {
            if (event.type == SWT.Dispose) {
              getDisplay().removeFilter(SWT.MouseMove, this);
            } else {
              Point p = ((Control) event.widget).toDisplay(event.x, event.y);
              // during resize mouse can be temporary outside of flyout - ignore
              if (m_resize) {
                return;
              }
              // mouse in in flyout container - ignore
              if (getClientArea().contains(toControl(p.x, p.y))) {
                return;
              }
              // mouse is in full container - collapse
              if (container.getClientArea().contains(container.toControl(p.x, p.y))) {
                getDisplay().removeFilter(SWT.MouseMove, this);
                // it is possible, that user restored (OPEN) flyout, so collapse only if we still in expand state
                if (m_preferences.getState() == IFlyoutPreferences.STATE_EXPANDED) {
                  m_preferences.setState(IFlyoutPreferences.STATE_COLLAPSED);
                  container.layout();
                  // BEGIN ADT MODIFICATIONS
                  if (m_listener != null) {
                      m_listener.stateChanged(IFlyoutPreferences.STATE_EXPANDED,
                              IFlyoutPreferences.STATE_COLLAPSED);
                  }
                  // END ADT MODIFICATIONS
                }
              }
            }
          }
        };
        addListener(SWT.Dispose, listener);
        getDisplay().addFilter(SWT.MouseMove, listener);
      }
    }

    /**
     * Handler for docking.
     */
    private void handleDocking() {
      final FlyoutControlComposite container = FlyoutControlComposite.this;
      final int width = m_preferences.getWidth();
      final int oldDockLocation = getDockLocation();
      final int[] newDockLocation = new int[]{oldDockLocation};
      final Tracker dockingTracker = new Tracker(container, SWT.NONE);
      dockingTracker.setRectangles(new Rectangle[]{getBounds()});
      dockingTracker.setStippled(true);
      dockingTracker.addListener(SWT.Move, new Listener() {
        @Override
        public void handleEvent(Event event2) {
          Rectangle clientArea = container.getClientArea();
          Point location = container.toControl(event2.x, event2.y);
          int h3 = clientArea.height / 3;
          // check locations
          if (location.y < h3 && isValidDockLocation(IFlyoutPreferences.DOCK_NORTH)) {
            dockingTracker.setRectangles(new Rectangle[]{new Rectangle(0,
                0,
                clientArea.width,
                width)});
            newDockLocation[0] = IFlyoutPreferences.DOCK_NORTH;
          } else if (location.y > 2 * h3 && isValidDockLocation(IFlyoutPreferences.DOCK_SOUTH)) {
            dockingTracker.setRectangles(new Rectangle[]{new Rectangle(0,
                clientArea.height - width,
                clientArea.width,
                width)});
            newDockLocation[0] = IFlyoutPreferences.DOCK_SOUTH;
          } else if (location.x < clientArea.width / 2
              && isValidDockLocation(IFlyoutPreferences.DOCK_WEST)) {
            dockingTracker.setRectangles(new Rectangle[]{new Rectangle(0,
                0,
                width,
                clientArea.height)});
            newDockLocation[0] = IFlyoutPreferences.DOCK_WEST;
          } else if (isValidDockLocation(IFlyoutPreferences.DOCK_EAST)) {
            dockingTracker.setRectangles(new Rectangle[]{new Rectangle(clientArea.width - width,
                0,
                width,
                clientArea.height)});
            newDockLocation[0] = IFlyoutPreferences.DOCK_EAST;
          } else {
            dockingTracker.setRectangles(new Rectangle[]{getBounds()});
            newDockLocation[0] = oldDockLocation;
          }
        }
      });
      // start tracking
      if (dockingTracker.open()) {
        setDockLocation(newDockLocation[0]);
      }
      // dispose tracker
      dockingTracker.dispose();
    }

    ////////////////////////////////////////////////////////////////////////////
    //
    // Access
    //
    ////////////////////////////////////////////////////////////////////////////
    /**
     * @return the {@link Control} installed on this {@link FlyoutControlComposite}, or
     *         <code>null</code> if there are no any {@link Control}.
     */
    private Control getControl() {
      Control[] children = getChildren();
      return children.length == 1 ? children[0] : null;
    }

    /**
     * Sets the text of title.
     */
    public void setTitleText(String text) {
      updateTitleImage(text);
    }

    ////////////////////////////////////////////////////////////////////////////
    //
    // Layout
    //
    ////////////////////////////////////////////////////////////////////////////
    @Override
    public void layout() {
      Control control = getControl();
      if (control == null) {
        return;
      }
      // OK, we have control, so can continue layout
      Rectangle clientArea = getClientArea();
      if (isOpenExpanded()) {
        if (isWest()) {
          int y = m_titleHeight;
          control.setBounds(0, y, clientArea.width - RESIZE_WIDTH, clientArea.height - y);
        } else if (isEast()) {
          int y = m_titleHeight;
          control.setBounds(RESIZE_WIDTH, y, clientArea.width - RESIZE_WIDTH, clientArea.height - y);
        } else if (isNorth()) {
          int y = m_titleHeight;
          control.setBounds(0, y, clientArea.width, clientArea.height - y - RESIZE_WIDTH);
        } else if (isSouth()) {
          int y = RESIZE_WIDTH + m_titleHeight;
          control.setBounds(0, y, clientArea.width, clientArea.height - y);
        }
      } else {
        control.setBounds(0, 0, 0, 0);
      }
    }

    ////////////////////////////////////////////////////////////////////////////
    //
    // Paint
    //
    ////////////////////////////////////////////////////////////////////////////
    private Image m_backImage;

    /**
     * Handler for {@link SWT#Paint} event.
     */
    private void handlePaint(GC paintGC) {
      Rectangle clientArea = getClientArea();
      // prepare back image
      GC gc;
      {
        if (m_backImage == null || !m_backImage.getBounds().equals(clientArea)) {
          if (m_backImage != null) {
            m_backImage.dispose();
          }
          m_backImage = new Image(getDisplay(), clientArea.width, clientArea.height);
        }
        // prepare GC
        gc = new GC(m_backImage);
        gc.setBackground(paintGC.getBackground());
        gc.setForeground(paintGC.getForeground());
        gc.fillRectangle(clientArea);
      }
      //
      if (isOpenExpanded()) {
        // draw header
        {
          // draw title
          if (isWest()) {
            drawStateImage(gc, 0, 0);
            gc.drawImage(m_titleImage, m_titleHeight, 0);
          } else if (isEast()) {
            int x = clientArea.width - m_titleHeight;
            drawStateImage(gc, x, 0);
            gc.drawImage(m_titleImage, x - m_titleWidth, 0);
          } else if (isNorth()) {
            drawStateImage(gc, 0, 0);
            gc.drawImage(m_titleImage, m_titleHeight, 0);
          } else if (isSouth()) {
            int y = RESIZE_WIDTH;
            drawStateImage(gc, 0, y);
            gc.drawImage(m_titleImage, m_titleHeight, y);
          }
        }
        // draw resize band
        drawResizeBand(gc);
      } else {
        if (isHorizontal()) {
          drawStateImage(gc, 0, 0);
          gc.drawImage(m_titleImageRotated, 0, m_titleHeight);
        } else {
          drawStateImage(gc, 0, 0);
          gc.drawImage(m_titleImage, m_titleHeight, 0);
        }
        DrawUtils.drawHighlightRectangle(gc, 0, 0, clientArea.width, clientArea.height);
      }
      // flush back image
      {
        gc.dispose();
        paintGC.drawImage(m_backImage, 0, 0);
      }
    }

    /**
     * Draws the state image (arrow) at given location.
     */
    private void drawStateImage(GC gc, int x, int y) {
      DrawUtils.drawImageCHCV(gc, getStateImage(), x, y, m_titleHeight, m_titleHeight);
      if (m_stateHover) {
        DrawUtils.drawHighlightRectangle(gc, x, y, m_titleHeight, m_titleHeight);
      }
    }

    /**
     * @return the {@link Image} corresponding to current state (open or collapsed).
     */
    private Image getStateImage() {
      int location = getDockLocation();
      int state = m_preferences.getState();
      if (state == IFlyoutPreferences.STATE_OPEN) {
        switch (location) {
          case IFlyoutPreferences.DOCK_WEST :
            return ARROW_LEFT;
          case IFlyoutPreferences.DOCK_EAST :
            return ARROW_RIGHT;
          case IFlyoutPreferences.DOCK_NORTH :
            return ARROW_TOP;
          case IFlyoutPreferences.DOCK_SOUTH :
            return ARROW_BOTTOM;
        }
      } else if (state == IFlyoutPreferences.STATE_EXPANDED) {
        return PIN;
      } else {
        switch (location) {
          case IFlyoutPreferences.DOCK_WEST :
            return ARROW_RIGHT;
          case IFlyoutPreferences.DOCK_EAST :
            return ARROW_LEFT;
          case IFlyoutPreferences.DOCK_NORTH :
            return ARROW_BOTTOM;
          case IFlyoutPreferences.DOCK_SOUTH :
            return ARROW_TOP;
        }
      }
      //
      return null;
    }

    /**
     * Draws that resize band, {@link Sash} like.
     */
    private void drawResizeBand(GC gc) {
      Rectangle clientArea = getClientArea();
      // prepare locations
      int x, y, width, height;
      if (isHorizontal()) {
        if (isWest()) {
          x = clientArea.width - RESIZE_WIDTH;
        } else {
          x = 0;
        }
        y = 0;
        width = RESIZE_WIDTH;
        height = clientArea.height;
      } else {
        x = 0;
        if (isNorth()) {
          y = clientArea.height - RESIZE_WIDTH;
        } else {
          y = 0;
        }
        width = clientArea.width;
        height = RESIZE_WIDTH;
      }
      // draw band
      DrawUtils.drawHighlightRectangle(gc, x, y, width, height);
    }

    /**
     * @return <code>true</code> if flyout is open or expanded.
     */
    private boolean isOpenExpanded() {
      int state = m_preferences.getState();
      return state == IFlyoutPreferences.STATE_OPEN || state == IFlyoutPreferences.STATE_EXPANDED;
    }

    ////////////////////////////////////////////////////////////////////////////
    //
    // Title image
    //
    ////////////////////////////////////////////////////////////////////////////
    private int m_titleWidth;
    private int m_titleHeight;
    private Image m_titleImage;
    private Image m_titleImageRotated;

    /**
     * Creates {@link Image} for given title text.
     */
    private void updateTitleImage(String text) {
      // prepare size of text
      Point textSize;
      {
        GC gc = new GC(this);
        gc.setFont(TITLE_FONT);
        textSize = gc.textExtent(text);
        gc.dispose();
      }
      // dispose existing image
      if (m_titleImage != null) {
        m_titleImage.dispose();
        m_titleImageRotated.dispose();
      }
      // prepare new image
      {
        m_titleWidth = textSize.x + 2 * TITLE_LINES + 4 * TITLE_MARGIN;
        m_titleHeight = textSize.y;
        m_titleImage = new Image(getDisplay(), m_titleWidth, m_titleHeight);
        GC gc = new GC(m_titleImage);
        try {
          gc.setBackground(getBackground());
          gc.fillRectangle(0, 0, m_titleWidth, m_titleHeight);
          int x = 0;
          // draw left lines
          {
            x += TITLE_MARGIN;
            drawTitleLines(gc, x, m_titleHeight, TITLE_LINES);
            x += TITLE_LINES + TITLE_MARGIN;
          }
          // draw text
          {
            gc.setForeground(IColorConstants.black);
            gc.setFont(TITLE_FONT);
            gc.drawText(text, x, 0);
            x += textSize.x;
          }
          // draw right lines
          {
            x += TITLE_MARGIN;
            drawTitleLines(gc, x, m_titleHeight, TITLE_LINES);
          }
        } finally {
          gc.dispose();
        }
      }
      // prepare rotated image
      m_titleImageRotated = DrawUtils.createRotatedImage(m_titleImage);
    }

    /**
     * Draws two title lines.
     */
    private void drawTitleLines(GC gc, int x, int height, int width) {
      drawTitleLine(gc, x, height / 3, width);
      drawTitleLine(gc, x, 2 * height / 3, width);
    }

    /**
     * Draws single title line.
     */
    private void drawTitleLine(GC gc, int x, int y, int width) {
      int right = x + TITLE_LINES;
      //
      gc.setForeground(IColorConstants.buttonLightest);
      gc.drawLine(x, y, right - 2, y);
      gc.drawLine(x, y + 1, right - 2, y + 1);
      //
      gc.setForeground(IColorConstants.buttonDarker);
      gc.drawLine(right - 2, y, right - 1, y);
      gc.drawLine(x + 2, y + 1, right - 2, y + 1);
    }

    ////////////////////////////////////////////////////////////////////////////
    //
    // Menu
    //
    ////////////////////////////////////////////////////////////////////////////
    private void configureMenu() {
      final MenuManager manager = new MenuManager();
      manager.setRemoveAllWhenShown(true);
      manager.addMenuListener(new IMenuListener() {
        @Override
        public void menuAboutToShow(IMenuManager menuMgr) {
          addDockActions();
          for (IFlyoutMenuContributor contributor : m_menuContributors) {
            contributor.contribute(manager);
          }
        }

        private void addDockActions() {
          MenuManager dockManager = new MenuManager(Messages.FlyoutControlComposite_dockManager);
          addDockAction(
              dockManager,
              Messages.FlyoutControlComposite_dockLeft,
              IFlyoutPreferences.DOCK_WEST);
          addDockAction(
              dockManager,
              Messages.FlyoutControlComposite_dockRight,
              IFlyoutPreferences.DOCK_EAST);
          addDockAction(
              dockManager,
              Messages.FlyoutControlComposite_dockTop,
              IFlyoutPreferences.DOCK_NORTH);
          addDockAction(
              dockManager,
              Messages.FlyoutControlComposite_dockBottom,
              IFlyoutPreferences.DOCK_SOUTH);
          manager.add(dockManager);
        }

        private void addDockAction(MenuManager dockManager, String text, int location) {
          if ((m_validDockLocations & location) != 0) {
            dockManager.add(new DockAction(text, location));
          }
        }
      });
      // set menu
      setMenu(manager.createContextMenu(this));
      // dispose it later
      addDisposeListener(new DisposeListener() {
        @Override
        public void widgetDisposed(DisposeEvent e) {
          manager.dispose();
        }
      });
    }
  }
  ////////////////////////////////////////////////////////////////////////////
  //
  // DockAction
  //
  ////////////////////////////////////////////////////////////////////////////
  private class DockAction extends Action {
    private final int m_location;

    ////////////////////////////////////////////////////////////////////////////
    //
    // Constructor
    //
    ////////////////////////////////////////////////////////////////////////////
    public DockAction(String text, int location) {
      super(text, AS_RADIO_BUTTON);
      m_location = location;
    }

    ////////////////////////////////////////////////////////////////////////////
    //
    // Action
    //
    ////////////////////////////////////////////////////////////////////////////
    @Override
    public boolean isChecked() {
      return getDockLocation() == m_location;
    }

    @Override
    public void run() {
      setDockLocation(m_location);
    }
  }
}
