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
import org.eclipse.wb.internal.core.utils.ui.DrawUtils;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

/**
 * Class representing flat push button as it looks in Mac OSX.
 * 
 * It doesn't draw text, not need for now. ;-)
 * 
 * @author mitin_aa
 */
public final class CFlatButton extends Canvas {
  // colors
  private static final Color COLOR_FACE = DrawUtils.getShiftedColor(IColorConstants.button, 12);
  private static final Color COLOR_FACE_SELECTED = IColorConstants.buttonDarker;
  private static final Color COLOR_BORDER_GRADIENT1 = DrawUtils.getShiftedColor(
      IColorConstants.button,
      -12);
  private static final Color COLOR_BORDER_GRADIENT1_SELECTED = DrawUtils.getShiftedColor(
      IColorConstants.buttonDarker,
      64);
  private static final Color COLOR_BORDER_GRADIENT2 = DrawUtils.getShiftedColor(COLOR_FACE, -8);
  private static final Color COLOR_BORDER_GRADIENT2_SELECTED = DrawUtils.getShiftedColor(
      COLOR_FACE_SELECTED,
      -8);
  // fields
  private Image m_image;
  private boolean m_down;
  private boolean m_selected;

  ////////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  ////////////////////////////////////////////////////////////////////////////
  public CFlatButton(Composite parent, int style) {
    super(parent, style);
    addPaintListener(new PaintListener() {
      public void paintControl(PaintEvent e) {
        boolean isSelected = m_down | m_selected;
        Color faceColor = isSelected ? COLOR_FACE_SELECTED : COLOR_FACE;
        Color borderGradientColor1 =
            isSelected ? COLOR_BORDER_GRADIENT1_SELECTED : COLOR_BORDER_GRADIENT1;
        Color borderGradientColor2 =
            isSelected ? COLOR_BORDER_GRADIENT2_SELECTED : COLOR_BORDER_GRADIENT2;
        GC gc = e.gc;
        Rectangle ca = getClientArea();
        // draw client area
        // dark border
        gc.setForeground(IColorConstants.buttonDarker);
        gc.drawRectangle(ca.x, ca.y, ca.width - 1, ca.height - 1);
        cropClientArea(ca);
        // gradient border
        gc.setForeground(borderGradientColor1);
        gc.setBackground(borderGradientColor2);
        gc.fillGradientRectangle(ca.x, ca.y, ca.width, ca.height, true);
        cropClientArea(ca);
        // fill background
        gc.setBackground(faceColor);
        gc.fillRectangle(ca);
        // draw face upper-half gradient 
        Rectangle ca1 = getClientArea();
        cropClientArea(ca1);
        gc.setForeground(faceColor);
        gc.setBackground(borderGradientColor1);
        gc.fillGradientRectangle(ca1.x, ca1.y, ca1.width, ca1.height / 4, true);
        // draw face down-half gradient 
        ca1.x += 1;
        ca1.width -= 2;
        gc.setForeground(borderGradientColor1);
        gc.setBackground(faceColor);
        gc.fillGradientRectangle(ca1.x, ca1.y + ca1.height / 4 - 1, ca1.width, ca1.height / 2, true);
        // draw image
        Image image = getImage();
        if (image != null) {
          Rectangle imageBounds = image.getBounds();
          // center it in client area
          int x = ca.x + (ca.width - imageBounds.width) / 2;
          int y = ca.y + (ca.height - imageBounds.height) / 2;
          gc.drawImage(image, x, y);
        }
      }
    });
    addListener(SWT.MouseDown, new Listener() {
      public void handleEvent(Event e) {
        m_down = true;
        redraw();
      }
    });
    addListener(SWT.MouseUp, new Listener() {
      public void handleEvent(Event e) {
        m_down = false;
        redraw();
        update();
        if (getClientArea().contains(e.x, e.y)) {
          fireSelectionEvent(e.time, e.stateMask);
        }
      }
    });
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Utils
  //
  ////////////////////////////////////////////////////////////////////////////
  private void fireSelectionEvent(int time, int stateMask) {
    Event event = new Event();
    event.time = time;
    event.stateMask = stateMask;
    notifyListeners(SWT.Selection, event);
  }

  private void cropClientArea(Rectangle ca) {
    ca.x += 1;
    ca.y += 1;
    ca.width -= 2;
    ca.height -= 2;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Access
  //
  ////////////////////////////////////////////////////////////////////////////
  public final Image getImage() {
    return m_image;
  }

  public void setImage(Image image) {
    m_image = image;
  }

  public void setSelected(boolean selected) {
    m_selected = selected;
  }
}
