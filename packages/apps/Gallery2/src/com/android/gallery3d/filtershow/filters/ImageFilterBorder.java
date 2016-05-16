/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.gallery3d.filtershow.filters;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import java.util.HashMap;

public class ImageFilterBorder extends ImageFilter {
    private static final float NINEPATCH_ICON_SCALING = 10;
    private static final float BITMAP_ICON_SCALING = 1 / 3.0f;
    private FilterImageBorderRepresentation mParameters = null;
    private Resources mResources = null;

    private HashMap<Integer, Drawable> mDrawables = new HashMap<Integer, Drawable>();

    public ImageFilterBorder() {
        mName = "Border";
    }

    public void useRepresentation(FilterRepresentation representation) {
        FilterImageBorderRepresentation parameters = (FilterImageBorderRepresentation) representation;
        mParameters = parameters;
    }

    public FilterImageBorderRepresentation getParameters() {
        return mParameters;
    }

    public void freeResources() {
       mDrawables.clear();
    }

    public Bitmap applyHelper(Bitmap bitmap, float scale1, float scale2 ) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        Rect bounds = new Rect(0, 0, (int) (w * scale1), (int) (h * scale1));
        Canvas canvas = new Canvas(bitmap);
        canvas.scale(scale2, scale2);
        Drawable drawable = getDrawable(getParameters().getDrawableResource());
        drawable.setBounds(bounds);
        drawable.draw(canvas);
        return bitmap;
    }

    @Override
    public Bitmap apply(Bitmap bitmap, float scaleFactor, int quality) {
        if (getParameters() == null || getParameters().getDrawableResource() == 0) {
            return bitmap;
        }
        float scale2 = scaleFactor * 2.0f;
        float scale1 = 1 / scale2;
        return applyHelper(bitmap, scale1, scale2);
    }

    public void setResources(Resources resources) {
        if (mResources != resources) {
            mResources = resources;
            mDrawables.clear();
        }
    }

    public Drawable getDrawable(int rsc) {
        Drawable drawable = mDrawables.get(rsc);
        if (drawable == null && mResources != null && rsc != 0) {
            drawable = new BitmapDrawable(mResources, BitmapFactory.decodeResource(mResources, rsc));
            mDrawables.put(rsc, drawable);
        }
        return drawable;
    }

}
