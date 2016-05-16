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
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

/**
 * Simple control for displaying image and text.
 * 
 * For unknown reason CLabel shows such things not very good - vertical text alignment is strange
 * (bottom?).
 * 
 * @author scheglov_ke
 * @coverage core.control
 */
public class CImageLabel extends Canvas {
  private static final int SPACE = 5;
  private Image m_image;
  private String m_text;

  ////////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  ////////////////////////////////////////////////////////////////////////////
  public CImageLabel(Composite parent, int style) {
    super(parent, style | SWT.NO_BACKGROUND);
    addListener(SWT.Dispose, new Listener() {
      public void handleEvent(Event event) {
        if (m_backImage != null) {
          m_backImage.dispose();
          m_backImage = null;
        }
      }
    });
    addListener(SWT.Paint, new Listener() {
      public void handleEvent(Event event) {
        doPaint(event.gc);
      }
    });
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Access
  //
  ////////////////////////////////////////////////////////////////////////////
  public Image getImage() {
    return m_image;
  }

  public void setImage(Image image) {
    m_image = image;
    redraw();
  }

  public String getText() {
    return m_text;
  }

  public void setText(String text) {
    m_text = text;
    redraw();
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Paint
  //
  ////////////////////////////////////////////////////////////////////////////
  private Image m_backImage;

  private void doPaint(GC paintGC) {
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
      //
      gc = new GC(m_backImage);
      gc.setBackground(paintGC.getBackground());
      gc.setForeground(paintGC.getForeground());
      gc.fillRectangle(clientArea);
    }
    //
    Point textExtent = m_text == null ? new Point(0, 0) : gc.textExtent(m_text);
    Rectangle imageBounds = m_image == null ? new Rectangle(0, 0, 0, 0) : m_image.getBounds();
    //
    if (m_image != null) {
      int x = clientArea.x;
      int y = clientArea.y + (clientArea.height - imageBounds.height) / 2;
      gc.drawImage(m_image, x, y);
    }
    if (m_text != null) {
      int x = clientArea.x + imageBounds.width + SPACE;
      int y = clientArea.y + (clientArea.height - textExtent.y) / 2;
      gc.drawText(m_text, x, y);
    }
    // flush back image
    {
      paintGC.drawImage(m_backImage, 0, 0);
      gc.dispose();
    }
  }

  @Override
  public Point computeSize(int wHint, int hHint, boolean changed) {
    // prepare text size
    GC gc = new GC(this);
    Point textExtent = m_text == null ? new Point(0, 0) : gc.textExtent(m_text);
    gc.dispose();
    // prepare image size
    Rectangle imageBounds = m_image == null ? new Rectangle(0, 0, 0, 0) : m_image.getBounds();
    // calculate control size
    int width = imageBounds.width + SPACE + textExtent.x;
    int height = Math.max(imageBounds.height, textExtent.y);
    return new Point(width, height);
  }
}
