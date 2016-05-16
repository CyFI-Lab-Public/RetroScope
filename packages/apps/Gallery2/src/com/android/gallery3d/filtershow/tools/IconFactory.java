/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.gallery3d.filtershow.tools;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

/**
 * A factory class for producing bitmaps to use as UI icons.
 */
public class IconFactory {

    /**
     * Builds an icon with the dimensions iconWidth:iconHeight. If scale is set
     * the source image is stretched to fit within the given dimensions;
     * otherwise, the source image is cropped to the proper aspect ratio.
     *
     * @param sourceImage image to create an icon from.
     * @param iconWidth width of the icon bitmap.
     * @param iconHeight height of the icon bitmap.
     * @param scale if true, stretch sourceImage to fit the icon dimensions.
     * @return an icon bitmap with the dimensions iconWidth:iconHeight.
     */
    public static Bitmap createIcon(Bitmap sourceImage, int iconWidth, int iconHeight,
            boolean scale) {
        if (sourceImage == null) {
            throw new IllegalArgumentException("Null argument to buildIcon");
        }

        int sourceWidth = sourceImage.getWidth();
        int sourceHeight = sourceImage.getHeight();

        if (sourceWidth == 0 || sourceHeight == 0 || iconWidth == 0 || iconHeight == 0) {
            throw new IllegalArgumentException("Bitmap with dimension 0 used as input");
        }

        Bitmap icon = Bitmap.createBitmap(iconWidth, iconHeight,
                Bitmap.Config.ARGB_8888);
        drawIcon(icon, sourceImage, scale);
        return icon;
    }

    /**
     * Draws an icon in the destination bitmap. If scale is set the source image
     * is stretched to fit within the destination dimensions; otherwise, the
     * source image is cropped to the proper aspect ratio.
     *
     * @param dest bitmap into which to draw the icon.
     * @param sourceImage image to create an icon from.
     * @param scale if true, stretch sourceImage to fit the destination.
     */
    public static void drawIcon(Bitmap dest, Bitmap sourceImage, boolean scale) {
        if (dest == null || sourceImage == null) {
            throw new IllegalArgumentException("Null argument to buildIcon");
        }

        int sourceWidth = sourceImage.getWidth();
        int sourceHeight = sourceImage.getHeight();
        int iconWidth = dest.getWidth();
        int iconHeight = dest.getHeight();

        if (sourceWidth == 0 || sourceHeight == 0 || iconWidth == 0 || iconHeight == 0) {
            throw new IllegalArgumentException("Bitmap with dimension 0 used as input");
        }

        Rect destRect = new Rect(0, 0, iconWidth, iconHeight);
        Canvas canvas = new Canvas(dest);

        Rect srcRect = null;
        if (scale) {
            // scale image to fit in icon (stretches if aspect isn't the same)
            srcRect = new Rect(0, 0, sourceWidth, sourceHeight);
        } else {
            // crop image to aspect ratio iconWidth:iconHeight
            float wScale = sourceWidth / (float) iconWidth;
            float hScale = sourceHeight / (float) iconHeight;
            float s = Math.min(hScale, wScale);

            float iw = iconWidth * s;
            float ih = iconHeight * s;

            float borderW = (sourceWidth - iw) / 2.0f;
            float borderH = (sourceHeight - ih) / 2.0f;
            RectF rec = new RectF(borderW, borderH, borderW + iw, borderH + ih);
            srcRect = new Rect();
            rec.roundOut(srcRect);
        }

        canvas.drawBitmap(sourceImage, srcRect, destRect, new Paint(Paint.FILTER_BITMAP_FLAG));
    }
}
