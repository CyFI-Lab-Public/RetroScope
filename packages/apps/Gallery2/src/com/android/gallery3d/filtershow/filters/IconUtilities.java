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

package com.android.gallery3d.filtershow.filters;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.android.gallery3d.R;

public class IconUtilities {
    public static final int PUNCH = R.drawable.filtershow_fx_0005_punch;
    public static final int VINTAGE = R.drawable.filtershow_fx_0000_vintage;
    public static final int BW_CONTRAST = R.drawable.filtershow_fx_0004_bw_contrast;
    public static final int BLEACH = R.drawable.filtershow_fx_0002_bleach;
    public static final int INSTANT = R.drawable.filtershow_fx_0001_instant;
    public static final int WASHOUT = R.drawable.filtershow_fx_0007_washout;
    public static final int BLUECRUSH = R.drawable.filtershow_fx_0003_blue_crush;
    public static final int WASHOUT_COLOR = R.drawable.filtershow_fx_0008_washout_color;
    public static final int X_PROCESS = R.drawable.filtershow_fx_0006_x_process;

    public static Bitmap getFXBitmap(Resources res, int id) {
        Bitmap ret;
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inScaled = false;

        if (id != 0) {
            return BitmapFactory.decodeResource(res, id, o);
        }
        return null;
    }

    public static Bitmap loadBitmap(Resources res, int resource) {

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeResource(
                res,
                resource, options);

        return bitmap;
    }

    public static Bitmap applyFX(Bitmap bitmap, final Bitmap fxBitmap) {
        ImageFilterFx fx = new ImageFilterFx() {
            @Override
            public Bitmap apply(Bitmap bitmap, float scaleFactor, int quality) {

                int w = bitmap.getWidth();
                int h = bitmap.getHeight();
                int fxw = fxBitmap.getWidth();
                int fxh = fxBitmap.getHeight();
                int start = 0;
                int end = w * h * 4;
                nativeApplyFilter(bitmap, w, h, fxBitmap, fxw, fxh, start, end);
                return bitmap;
            }
        };
        return fx.apply(bitmap, 0, 0);
    }
}
