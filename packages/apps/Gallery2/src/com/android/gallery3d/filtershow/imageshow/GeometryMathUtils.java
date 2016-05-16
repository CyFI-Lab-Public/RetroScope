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

package com.android.gallery3d.filtershow.imageshow;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

import com.android.gallery3d.filtershow.cache.BitmapCache;
import com.android.gallery3d.filtershow.cache.ImageLoader;
import com.android.gallery3d.filtershow.filters.FilterCropRepresentation;
import com.android.gallery3d.filtershow.filters.FilterMirrorRepresentation;
import com.android.gallery3d.filtershow.filters.FilterMirrorRepresentation.Mirror;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import com.android.gallery3d.filtershow.filters.FilterRotateRepresentation;
import com.android.gallery3d.filtershow.filters.FilterRotateRepresentation.Rotation;
import com.android.gallery3d.filtershow.filters.FilterStraightenRepresentation;
import com.android.gallery3d.filtershow.pipeline.ImagePreset;

import java.util.Collection;
import java.util.Iterator;

public final class GeometryMathUtils {
    private static final String TAG = "GeometryMathUtils";
    public static final float SHOW_SCALE = .9f;

    private GeometryMathUtils() {};

    // Holder class for Geometry data.
    public static final class GeometryHolder {
        public Rotation rotation = FilterRotateRepresentation.getNil();
        public float straighten = FilterStraightenRepresentation.getNil();
        public RectF crop = FilterCropRepresentation.getNil();
        public Mirror mirror = FilterMirrorRepresentation.getNil();

        public void set(GeometryHolder h) {
            rotation = h.rotation;
            straighten = h.straighten;
            crop.set(h.crop);
            mirror = h.mirror;
        }

        public void wipe() {
            rotation = FilterRotateRepresentation.getNil();
            straighten = FilterStraightenRepresentation.getNil();
            crop = FilterCropRepresentation.getNil();
            mirror = FilterMirrorRepresentation.getNil();
        }

        public boolean isNil() {
            return rotation == FilterRotateRepresentation.getNil() &&
                    straighten == FilterStraightenRepresentation.getNil() &&
                    crop.equals(FilterCropRepresentation.getNil()) &&
                    mirror == FilterMirrorRepresentation.getNil();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof GeometryHolder)) {
                return false;
            }
            GeometryHolder h = (GeometryHolder) o;
            return rotation == h.rotation && straighten == h.straighten &&
                    ((crop == null && h.crop == null) || (crop != null && crop.equals(h.crop))) &&
                    mirror == h.mirror;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[" + "rotation:" + rotation.value()
                    + ",straighten:" + straighten + ",crop:" + crop.toString()
                    + ",mirror:" + mirror.value() + "]";
        }
    }

    // Math operations for 2d vectors
    public static float clamp(float i, float low, float high) {
        return Math.max(Math.min(i, high), low);
    }

    public static float[] lineIntersect(float[] line1, float[] line2) {
        float a0 = line1[0];
        float a1 = line1[1];
        float b0 = line1[2];
        float b1 = line1[3];
        float c0 = line2[0];
        float c1 = line2[1];
        float d0 = line2[2];
        float d1 = line2[3];
        float t0 = a0 - b0;
        float t1 = a1 - b1;
        float t2 = b0 - d0;
        float t3 = d1 - b1;
        float t4 = c0 - d0;
        float t5 = c1 - d1;

        float denom = t1 * t4 - t0 * t5;
        if (denom == 0)
            return null;
        float u = (t3 * t4 + t5 * t2) / denom;
        float[] intersect = {
                b0 + u * t0, b1 + u * t1
        };
        return intersect;
    }

    public static float[] shortestVectorFromPointToLine(float[] point, float[] line) {
        float x1 = line[0];
        float x2 = line[2];
        float y1 = line[1];
        float y2 = line[3];
        float xdelt = x2 - x1;
        float ydelt = y2 - y1;
        if (xdelt == 0 && ydelt == 0)
            return null;
        float u = ((point[0] - x1) * xdelt + (point[1] - y1) * ydelt)
                / (xdelt * xdelt + ydelt * ydelt);
        float[] ret = {
                (x1 + u * (x2 - x1)), (y1 + u * (y2 - y1))
        };
        float[] vec = {
                ret[0] - point[0], ret[1] - point[1]
        };
        return vec;
    }

    // A . B
    public static float dotProduct(float[] a, float[] b) {
        return a[0] * b[0] + a[1] * b[1];
    }

    public static float[] normalize(float[] a) {
        float length = (float) Math.sqrt(a[0] * a[0] + a[1] * a[1]);
        float[] b = {
                a[0] / length, a[1] / length
        };
        return b;
    }

    // A onto B
    public static float scalarProjection(float[] a, float[] b) {
        float length = (float) Math.sqrt(b[0] * b[0] + b[1] * b[1]);
        return dotProduct(a, b) / length;
    }

    public static float[] getVectorFromPoints(float[] point1, float[] point2) {
        float[] p = {
                point2[0] - point1[0], point2[1] - point1[1]
        };
        return p;
    }

    public static float[] getUnitVectorFromPoints(float[] point1, float[] point2) {
        float[] p = {
                point2[0] - point1[0], point2[1] - point1[1]
        };
        float length = (float) Math.sqrt(p[0] * p[0] + p[1] * p[1]);
        p[0] = p[0] / length;
        p[1] = p[1] / length;
        return p;
    }

    public static void scaleRect(RectF r, float scale) {
        r.set(r.left * scale, r.top * scale, r.right * scale, r.bottom * scale);
    }

    // A - B
    public static float[] vectorSubtract(float[] a, float[] b) {
        int len = a.length;
        if (len != b.length)
            return null;
        float[] ret = new float[len];
        for (int i = 0; i < len; i++) {
            ret[i] = a[i] - b[i];
        }
        return ret;
    }

    public static float vectorLength(float[] a) {
        return (float) Math.sqrt(a[0] * a[0] + a[1] * a[1]);
    }

    public static float scale(float oldWidth, float oldHeight, float newWidth, float newHeight) {
        if (oldHeight == 0 || oldWidth == 0 || (oldWidth == newWidth && oldHeight == newHeight)) {
            return 1;
        }
        return Math.min(newWidth / oldWidth, newHeight / oldHeight);
    }

    public static Rect roundNearest(RectF r) {
        Rect q = new Rect(Math.round(r.left), Math.round(r.top), Math.round(r.right),
                Math.round(r.bottom));
        return q;
    }

    private static void concatMirrorMatrix(Matrix m, GeometryHolder holder) {
        Mirror type = holder.mirror;
        if (type == Mirror.HORIZONTAL) {
            if (holder.rotation.value() == 90
                    || holder.rotation.value() == 270) {
                type = Mirror.VERTICAL;
            }
        } else if (type == Mirror.VERTICAL) {
            if (holder.rotation.value() == 90
                    || holder.rotation.value() == 270) {
                type = Mirror.HORIZONTAL;
            }
        }
        if (type == Mirror.HORIZONTAL) {
            m.postScale(-1, 1);
        } else if (type == Mirror.VERTICAL) {
            m.postScale(1, -1);
        } else if (type == Mirror.BOTH) {
            m.postScale(1, -1);
            m.postScale(-1, 1);
        }
    }

    private static int getRotationForOrientation(int orientation) {
        switch (orientation) {
            case ImageLoader.ORI_ROTATE_90:
                return 90;
            case ImageLoader.ORI_ROTATE_180:
                return 180;
            case ImageLoader.ORI_ROTATE_270:
                return 270;
            default:
                return 0;
        }
    }

    public static GeometryHolder unpackGeometry(Collection<FilterRepresentation> geometry) {
        GeometryHolder holder = new GeometryHolder();
        unpackGeometry(holder, geometry);
        return holder;
    }

    public static void unpackGeometry(GeometryHolder out,
            Collection<FilterRepresentation> geometry) {
        out.wipe();
        // Get geometry data from filters
        for (FilterRepresentation r : geometry) {
            if (r.isNil()) {
                continue;
            }
            if (r.getSerializationName() == FilterRotateRepresentation.SERIALIZATION_NAME) {
                out.rotation = ((FilterRotateRepresentation) r).getRotation();
            } else if (r.getSerializationName() ==
                    FilterStraightenRepresentation.SERIALIZATION_NAME) {
                out.straighten = ((FilterStraightenRepresentation) r).getStraighten();
            } else if (r.getSerializationName() == FilterCropRepresentation.SERIALIZATION_NAME) {
                ((FilterCropRepresentation) r).getCrop(out.crop);
            } else if (r.getSerializationName() == FilterMirrorRepresentation.SERIALIZATION_NAME) {
                out.mirror = ((FilterMirrorRepresentation) r).getMirror();
            }
        }
    }

    public static void replaceInstances(Collection<FilterRepresentation> geometry,
            FilterRepresentation rep) {
        Iterator<FilterRepresentation> iter = geometry.iterator();
        while (iter.hasNext()) {
            FilterRepresentation r = iter.next();
            if (ImagePreset.sameSerializationName(rep, r)) {
                iter.remove();
            }
        }
        if (!rep.isNil()) {
            geometry.add(rep);
        }
    }

    public static void initializeHolder(GeometryHolder outHolder,
            FilterRepresentation currentLocal) {
        Collection<FilterRepresentation> geometry = MasterImage.getImage().getPreset()
                .getGeometryFilters();
        replaceInstances(geometry, currentLocal);
        unpackGeometry(outHolder, geometry);
    }

    public static Rect finalGeometryRect(int width, int height,
                                         Collection<FilterRepresentation> geometry) {
        GeometryHolder holder = unpackGeometry(geometry);
        RectF crop = getTrueCropRect(holder, width, height);
        Rect frame = new Rect();
        crop.roundOut(frame);
        return frame;
    }

    private static Bitmap applyFullGeometryMatrix(Bitmap image, GeometryHolder holder) {
        int width = image.getWidth();
        int height = image.getHeight();
        RectF crop = getTrueCropRect(holder, width, height);
        Rect frame = new Rect();
        crop.roundOut(frame);
        Matrix m = getCropSelectionToScreenMatrix(null, holder, width, height, frame.width(),
                frame.height());
        BitmapCache bitmapCache = MasterImage.getImage().getBitmapCache();
        Bitmap temp = bitmapCache.getBitmap(frame.width(),
                frame.height(), BitmapCache.UTIL_GEOMETRY);
        Canvas canvas = new Canvas(temp);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);
        canvas.drawBitmap(image, m, paint);
        return temp;
    }

    public static Matrix getImageToScreenMatrix(Collection<FilterRepresentation> geometry,
            boolean reflectRotation, Rect bmapDimens, float viewWidth, float viewHeight) {
        GeometryHolder h = unpackGeometry(geometry);
        return GeometryMathUtils.getOriginalToScreen(h, reflectRotation, bmapDimens.width(),
                bmapDimens.height(), viewWidth, viewHeight);
    }

    public static Matrix getPartialToScreenMatrix(Collection<FilterRepresentation> geometry,
                                                  Rect originalBounds, float w, float h,
                                                  float pw, float ph) {
        GeometryHolder holder = unpackGeometry(geometry);
        RectF rCrop = new RectF(0, 0, originalBounds.width(), originalBounds.height());
        float angle = holder.straighten;
        int rotation = holder.rotation.value();

        ImageStraighten.getUntranslatedStraightenCropBounds(rCrop, angle);
        float dx = (w - pw) / 2f;
        float dy = (h - ph) / 2f;
        Matrix compensation = new Matrix();
        compensation.postTranslate(dx, dy);
        float cScale = originalBounds.width() / rCrop.width();
        if (rCrop.width() < rCrop.height()) {
            cScale = originalBounds.height() / rCrop.height();
        }
        float scale = w / pw;
        if (w < h) {
            scale = h / ph;
        }
        scale = scale * cScale;
        float cx = w / 2f;
        float cy = h / 2f;

        compensation.postScale(scale, scale, cx, cy);
        compensation.postRotate(angle, cx, cy);
        compensation.postRotate(rotation, cx, cy);
        compensation.postTranslate(-cx, -cy);
        concatMirrorMatrix(compensation, holder);
        compensation.postTranslate(cx, cy);
        return compensation;
    }

    public static Matrix getOriginalToScreen(GeometryHolder holder, boolean rotate,
            float originalWidth,
            float originalHeight, float viewWidth, float viewHeight) {
        int orientation = MasterImage.getImage().getZoomOrientation();
        int rotation = getRotationForOrientation(orientation);
        Rotation prev = holder.rotation;
        rotation = (rotation + prev.value()) % 360;
        holder.rotation = Rotation.fromValue(rotation);
        Matrix m = getCropSelectionToScreenMatrix(null, holder, (int) originalWidth,
                (int) originalHeight, (int) viewWidth, (int) viewHeight);
        holder.rotation = prev;
        return m;
    }

    public static Bitmap applyGeometryRepresentations(Collection<FilterRepresentation> res,
            Bitmap image) {
        GeometryHolder holder = unpackGeometry(res);
        Bitmap bmap = image;
        // If there are geometry changes, apply them to the image
        if (!holder.isNil()) {
            bmap = applyFullGeometryMatrix(bmap, holder);
            if (bmap != image) {
                BitmapCache cache = MasterImage.getImage().getBitmapCache();
                cache.cache(image);
            }
        }
        return bmap;
    }

    public static RectF drawTransformedCropped(GeometryHolder holder, Canvas canvas,
            Bitmap photo, int viewWidth, int viewHeight) {
        if (photo == null) {
            return null;
        }
        RectF crop = new RectF();
        Matrix m = getCropSelectionToScreenMatrix(crop, holder, photo.getWidth(), photo.getHeight(),
                viewWidth, viewHeight);
        canvas.save();
        canvas.clipRect(crop);
        Paint p = new Paint();
        p.setAntiAlias(true);
        canvas.drawBitmap(photo, m, p);
        canvas.restore();
        return crop;
    }

    public static boolean needsDimensionSwap(Rotation rotation) {
        switch (rotation) {
            case NINETY:
            case TWO_SEVENTY:
                return true;
            default:
                return false;
        }
    }

    // Gives matrix for rotated, straightened, mirrored bitmap centered at 0,0.
    private static Matrix getFullGeometryMatrix(GeometryHolder holder, int bitmapWidth,
            int bitmapHeight) {
        float centerX = bitmapWidth / 2f;
        float centerY = bitmapHeight / 2f;
        Matrix m = new Matrix();
        m.setTranslate(-centerX, -centerY);
        m.postRotate(holder.straighten + holder.rotation.value());
        concatMirrorMatrix(m, holder);
        return m;
    }

    public static Matrix getFullGeometryToScreenMatrix(GeometryHolder holder, int bitmapWidth,
            int bitmapHeight, int viewWidth, int viewHeight) {
        int bh = bitmapHeight;
        int bw = bitmapWidth;
        if (GeometryMathUtils.needsDimensionSwap(holder.rotation)) {
            bh = bitmapWidth;
            bw = bitmapHeight;
        }
        float scale = GeometryMathUtils.scale(bw, bh, viewWidth, viewHeight);
        scale *= SHOW_SCALE;
        float s = Math.min(viewWidth / (float) bitmapWidth, viewHeight / (float) bitmapHeight);
        Matrix m = getFullGeometryMatrix(holder, bitmapWidth, bitmapHeight);
        m.postScale(scale, scale);
        m.postTranslate(viewWidth / 2f, viewHeight / 2f);
        return m;
    }

    public static RectF getTrueCropRect(GeometryHolder holder, int bitmapWidth, int bitmapHeight) {
        RectF r = new RectF(holder.crop);
        FilterCropRepresentation.findScaledCrop(r, bitmapWidth, bitmapHeight);
        float s = holder.straighten;
        holder.straighten = 0;
        Matrix m1 = getFullGeometryMatrix(holder, bitmapWidth, bitmapHeight);
        holder.straighten = s;
        m1.mapRect(r);
        return r;
    }

    public static Matrix getCropSelectionToScreenMatrix(RectF outCrop, GeometryHolder holder,
            int bitmapWidth, int bitmapHeight, int viewWidth, int viewHeight) {
        Matrix m = getFullGeometryMatrix(holder, bitmapWidth, bitmapHeight);
        RectF crop = getTrueCropRect(holder, bitmapWidth, bitmapHeight);
        float scale = GeometryMathUtils.scale(crop.width(), crop.height(), viewWidth, viewHeight);
        m.postScale(scale, scale);
        GeometryMathUtils.scaleRect(crop, scale);
        m.postTranslate(viewWidth / 2f - crop.centerX(), viewHeight / 2f - crop.centerY());
        if (outCrop != null) {
            crop.offset(viewWidth / 2f - crop.centerX(), viewHeight / 2f - crop.centerY());
            outCrop.set(crop);
        }
        return m;
    }

    public static Matrix getCropSelectionToScreenMatrix(RectF outCrop,
            Collection<FilterRepresentation> res, int bitmapWidth, int bitmapHeight, int viewWidth,
            int viewHeight) {
        GeometryHolder holder = unpackGeometry(res);
        return getCropSelectionToScreenMatrix(outCrop, holder, bitmapWidth, bitmapHeight,
                viewWidth, viewHeight);
    }
}
