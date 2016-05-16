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
package org.eclipse.wb.internal.core.utils.ui;

import com.google.common.io.Closeables;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.wb.draw2d.IColorConstants;

import java.io.InputStream;
import java.net.URL;

/**
 * Utilities for drawing on {@link GC}.
 *
 * @author scheglov_ke
 * @coverage core.ui
 */
public class DrawUtils {
  private static final String DOTS = "...";

  ////////////////////////////////////////////////////////////////////////////
  //
  // Drawing
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Draws given text clipped horizontally and centered vertically.
   */
  public static final void drawStringCV(GC gc, String text, int x, int y, int width, int height) {
    Rectangle oldClipping = gc.getClipping();
    try {
      gc.setClipping(new Rectangle(x, y, width, height));
      //
      int textStartY = y + (height - gc.getFontMetrics().getHeight()) / 2;
      gc.drawString(clipString(gc, text, width), x, textStartY, true);
    } finally {
      gc.setClipping(oldClipping);
    }
  }

  /**
   * Draws given text clipped or centered horizontally and centered vertically.
   */
  public static final void drawStringCHCV(GC gc, String text, int x, int y, int width, int height) {
    int textStartY = y + (height - gc.getFontMetrics().getHeight()) / 2;
    Point textSize = gc.stringExtent(text);
    //
    if (textSize.x > width) {
      gc.drawString(clipString(gc, text, width), x, textStartY);
    } else {
      gc.drawString(text, x + (width - textSize.x) / 2, textStartY);
    }
  }

  /**
   * Draws image at given <code>x</code> and centered vertically.
   */
  public static final void drawImageCV(GC gc, Image image, int x, int y, int height) {
    if (image != null) {
      Rectangle imageBounds = image.getBounds();
      gc.drawImage(image, x, y + (height - imageBounds.height) / 2);
    }
  }

  /**
   * Draws image at given <code>x</code> and centered vertically.
   */
  public static final void drawImageCHCV(GC gc, Image image, int x, int y, int width, int height) {
    if (image != null) {
      Rectangle imageBounds = image.getBounds();
      int centerX = (width - imageBounds.width) / 2;
      int centerY = y + (height - imageBounds.height) / 2;
      gc.drawImage(image, x + centerX, centerY);
    }
  }

  /**
   * Draws {@link Image} on {@link GC} centered in given {@link Rectangle}. If {@link Image} is
   * bigger that {@link Rectangle}, {@link Image} will be scaled down as needed with keeping
   * proportions.
   */
  public static void drawScaledImage(GC gc, Image image, Rectangle targetRectangle) {
    int imageWidth = image.getBounds().width;
    int imageHeight = image.getBounds().height;
    // prepare scaled image size
    int newImageWidth;
    int newImageHeight;
    if (imageWidth <= targetRectangle.width && imageHeight <= targetRectangle.height) {
      newImageWidth = imageWidth;
      newImageHeight = imageHeight;
    } else {
      // prepare minimal scale
      double k;
      {
        double k_w = targetRectangle.width / (double) imageWidth;
        double k_h = targetRectangle.height / (double) imageHeight;
        k = Math.min(k_w, k_h);
      }
      // calculate scaled image size
      newImageWidth = (int) (imageWidth * k);
      newImageHeight = (int) (imageHeight * k);
    }
    // draw image centered in target rectangle
    int destX = targetRectangle.x + (targetRectangle.width - newImageWidth) / 2;
    int destY = targetRectangle.y + (targetRectangle.height - newImageHeight) / 2;
    gc.drawImage(image, 0, 0, imageWidth, imageHeight, destX, destY, newImageWidth, newImageHeight);
  }

  /**
   * @return the string clipped to have width less than given. Clipping is done as trailing "...".
   */
  public static String clipString(GC gc, String text, int width) {
    if (width <= 0) {
      return "";
    }
    // check if text already fits in given width
    if (gc.stringExtent(text).x <= width) {
      return text;
    }
    // use average count of characters as base
    int count = Math.min(width / gc.getFontMetrics().getAverageCharWidth(), text.length());
    if (gc.stringExtent(text.substring(0, count) + DOTS).x > width) {
      while (count > 0 && gc.stringExtent(text.substring(0, count) + DOTS).x > width) {
        count--;
      }
    } else {
      while (count < text.length() - 1
          && gc.stringExtent(text.substring(0, count + 1) + DOTS).x < width) {
        count++;
      }
    }
    return text.substring(0, count) + DOTS;
  }

  /**
   * Draws {@link String} in rectangle, wraps at any character (not by words).
   */
  public static void drawTextWrap(GC gc, String text, int x, int y, int width, int height) {
    int y_ = y;
    int x_ = x;
    int lineHeight = 0;
    for (int i = 0; i < text.length(); i++) {
      String c = text.substring(i, i + 1);
      Point extent = gc.stringExtent(c);
      if (x_ + extent.x > x + width) {
        y_ += lineHeight;
        if (y_ > y + height) {
          return;
        }
        x_ = x;
      }
      gc.drawText(c, x_, y_);
      x_ += extent.x;
      lineHeight = Math.max(lineHeight, extent.y);
    }
  }

  /**
   * Draws 3D highlight rectangle.
   */
  public static void drawHighlightRectangle(GC gc, int x, int y, int width, int height) {
    int right = x + width - 1;
    int bottom = y + height - 1;
    //
    Color oldForeground = gc.getForeground();
    try {
      gc.setForeground(IColorConstants.buttonLightest);
      gc.drawLine(x, y, right, y);
      gc.drawLine(x, y, x, bottom);
      //
      gc.setForeground(IColorConstants.buttonDarker);
      gc.drawLine(right, y, right, bottom);
      gc.drawLine(x, bottom, right, bottom);
    } finally {
      gc.setForeground(oldForeground);
    }
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Images
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * @return the {@link Image} loaded relative to given {@link Class}.
   */
  public static Image loadImage(Class<?> clazz, String path) {
    try {
      URL resource = clazz.getResource(path);
      if (resource != null) {
        InputStream stream = resource.openStream();
        try {
          return new Image(null, stream);
        } finally {
          Closeables.closeQuietly(stream);
        }
      }
    } catch (Throwable e) {
    }
    return null;
  }

  /**
   * @return the thumbnail {@link Image} of required size for given "big" {@link Image}, centered or
   *         scaled down.
   */
  public static Image getThubmnail(Image image,
      int minWidth,
      int minHeight,
      int maxWidth,
      int maxHeight) {
    Rectangle imageBounds = image.getBounds();
    int imageWidth = imageBounds.width;
    int imageHeight = imageBounds.height;
    if (imageWidth < minWidth && imageHeight < minHeight) {
      // create "thumbnail" Image with required size
      Image thumbnail = new Image(null, minWidth, minHeight);
      GC gc = new GC(thumbnail);
      try {
        drawImageCHCV(gc, image, 0, 0, minWidth, minHeight);
      } finally {
        gc.dispose();
      }
      // recreate "thumbnail" Image with transparent pixel
      try {
        ImageData thumbnailData = thumbnail.getImageData();
        thumbnailData.transparentPixel = thumbnailData.getPixel(0, 0);
        return new Image(null, thumbnailData);
      } finally {
        thumbnail.dispose();
      }
    } else if (imageWidth <= maxWidth && imageHeight <= maxHeight) {
      return new Image(null, image, SWT.IMAGE_COPY);
    } else {
      double kX = (double) maxWidth / imageWidth;
      double kY = (double) maxHeight / imageHeight;
      double k = Math.max(kX, kY);
      int dWidth = (int) (imageWidth * k);
      int dHeight = (int) (imageHeight * k);
      ImageData scaledImageData = image.getImageData().scaledTo(dWidth, dHeight);
      return new Image(null, scaledImageData);
    }
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Rotated images
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Returns a new Image that is the given Image rotated left by 90 degrees. The client is
   * responsible for disposing the returned Image. This method MUST be invoked from the
   * user-interface (Display) thread.
   *
   * @param srcImage
   *          the Image that is to be rotated left
   * @return the rotated Image (the client is responsible for disposing it)
   */
  public static Image createRotatedImage(Image srcImage) {
    // prepare Display
    Display display = Display.getCurrent();
    if (display == null) {
      SWT.error(SWT.ERROR_THREAD_INVALID_ACCESS);
    }
    // rotate ImageData
    ImageData destData;
    {
      ImageData srcData = srcImage.getImageData();
      if (srcData.depth < 8) {
        destData = rotatePixelByPixel(srcData);
      } else {
        destData = rotateOptimized(srcData);
      }
    }
    // create new image
    return new Image(display, destData);
  }

  private static ImageData rotatePixelByPixel(ImageData srcData) {
    ImageData destData =
        new ImageData(srcData.height, srcData.width, srcData.depth, srcData.palette);
    for (int y = 0; y < srcData.height; y++) {
      for (int x = 0; x < srcData.width; x++) {
        destData.setPixel(y, srcData.width - x - 1, srcData.getPixel(x, y));
      }
    }
    return destData;
  }

  private static ImageData rotateOptimized(ImageData srcData) {
    int bytesPerPixel = Math.max(1, srcData.depth / 8);
    int destBytesPerLine =
        ((srcData.height * bytesPerPixel - 1) / srcData.scanlinePad + 1) * srcData.scanlinePad;
    byte[] newData = new byte[destBytesPerLine * srcData.width];
    for (int srcY = 0; srcY < srcData.height; srcY++) {
      for (int srcX = 0; srcX < srcData.width; srcX++) {
        int destX = srcY;
        int destY = srcData.width - srcX - 1;
        int destIndex = destY * destBytesPerLine + destX * bytesPerPixel;
        int srcIndex = srcY * srcData.bytesPerLine + srcX * bytesPerPixel;
        System.arraycopy(srcData.data, srcIndex, newData, destIndex, bytesPerPixel);
      }
    }
    return new ImageData(srcData.height,
        srcData.width,
        srcData.depth,
        srcData.palette,
        srcData.scanlinePad,
        newData);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Colors
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * @return new {@link Color} based on given {@link Color} and shifted on given value to make it
   *         darker or lighter.
   */
  public static Color getShiftedColor(Color color, int delta) {
    int r = Math.max(0, Math.min(color.getRed() + delta, 255));
    int g = Math.max(0, Math.min(color.getGreen() + delta, 255));
    int b = Math.max(0, Math.min(color.getBlue() + delta, 255));
    return new Color(color.getDevice(), r, g, b);
  }

  /**
   * @return <code>true</code> if the given <code>color</code> is dark.
   */
  public static boolean isDarkColor(Color c) {
    int value =
        (int) Math.sqrt(c.getRed()
            * c.getRed()
            * .241
            + c.getGreen()
            * c.getGreen()
            * .691
            + c.getBlue()
            * c.getBlue()
            * .068);
    return value < 130;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Fonts
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * @return the bold version of given {@link Font}.
   */
  public static Font getBoldFont(Font baseFont) {
    FontData[] boldData = getModifiedFontData(baseFont, SWT.BOLD);
    return new Font(Display.getCurrent(), boldData);
  }

  /**
   * @return the italic version of given {@link Font}.
   */
  public static Font getBoldItalicFont(Font baseFont) {
    FontData[] boldData = getModifiedFontData(baseFont, SWT.BOLD | SWT.ITALIC);
    return new Font(Display.getCurrent(), boldData);
  }

  /**
   * @return the italic version of given {@link Font}.
   */
  public static Font getItalicFont(Font baseFont) {
    FontData[] boldData = getModifiedFontData(baseFont, SWT.ITALIC);
    return new Font(Display.getCurrent(), boldData);
  }

  /**
   * @return the array of {@link FontData} with the specified style.
   */
  private static FontData[] getModifiedFontData(Font baseFont, int style) {
    FontData[] baseData = baseFont.getFontData();
    FontData[] styleData = new FontData[baseData.length];
    for (int i = 0; i < styleData.length; i++) {
      FontData base = baseData[i];
      styleData[i] = new FontData(base.getName(), base.getHeight(), base.getStyle() | style);
    }
    return styleData;
  }
}
