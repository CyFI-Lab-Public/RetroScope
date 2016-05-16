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

import android.graphics.Rect;
import android.util.JsonReader;
import android.util.JsonWriter;

import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.editors.EditorGrad;
import com.android.gallery3d.filtershow.imageshow.MasterImage;
import com.android.gallery3d.filtershow.imageshow.Line;

import java.io.IOException;
import java.util.Vector;

public class FilterGradRepresentation extends FilterRepresentation
        implements Line {
    private static final String LOGTAG = "FilterGradRepresentation";
    public static final int MAX_POINTS = 16;
    public static final int PARAM_BRIGHTNESS = 0;
    public static final int PARAM_SATURATION = 1;
    public static final int PARAM_CONTRAST = 2;
    private static final double ADD_MIN_DIST = .05;
    private static String LINE_NAME = "Point";
    private static final  String SERIALIZATION_NAME = "grad";

    public FilterGradRepresentation() {
        super("Grad");
        setSerializationName(SERIALIZATION_NAME);
        creatExample();
        setOverlayId(R.drawable.filtershow_button_grad);
        setFilterClass(ImageFilterGrad.class);
        setTextId(R.string.grad);
        setEditorId(EditorGrad.ID);
    }

    public void trimVector(){
        int n = mBands.size();
        for (int i = n; i < MAX_POINTS; i++) {
            mBands.add(new Band());
        }
        for (int i = MAX_POINTS; i <  n; i++) {
            mBands.remove(i);
        }
    }

    Vector<Band> mBands = new Vector<Band>();
    Band mCurrentBand;

    static class Band {
        private boolean mask = true;

        private int xPos1 = -1;
        private int yPos1 = 100;
        private int xPos2 = -1;
        private int yPos2 = 100;
        private int brightness = -40;
        private int contrast = 0;
        private int saturation = 0;


        public Band() {
        }

        public Band(int x, int y) {
            xPos1 = x;
            yPos1 = y+30;
            xPos2 = x;
            yPos2 = y-30;
        }

        public Band(Band copy) {
            mask = copy.mask;
            xPos1 = copy.xPos1;
            yPos1 = copy.yPos1;
            xPos2 = copy.xPos2;
            yPos2 = copy.yPos2;
            brightness = copy.brightness;
            contrast = copy.contrast;
            saturation = copy.saturation;
        }

    }

    @Override
    public String toString() {
        int count = 0;
        for (Band point : mBands) {
            if (!point.mask) {
                count++;
            }
        }
        return "c=" + mBands.indexOf(mBands) + "[" + mBands.size() + "]" + count;
    }

    private void creatExample() {
        Band p = new Band();
        p.mask = false;
        p.xPos1 = -1;
        p.yPos1 = 100;
        p.xPos2 = -1;
        p.yPos2 = 100;
        p.brightness = -50;
        p.contrast = 0;
        p.saturation = 0;
        mBands.add(0, p);
        mCurrentBand = p;
        trimVector();
    }

    @Override
    public void useParametersFrom(FilterRepresentation a) {
            FilterGradRepresentation rep = (FilterGradRepresentation) a;
            Vector<Band> tmpBands = new Vector<Band>();
            int n = (rep.mCurrentBand == null) ? 0 : rep.mBands.indexOf(rep.mCurrentBand);
            for (Band band : rep.mBands) {
                tmpBands.add(new Band(band));
            }
            mCurrentBand = null;
            mBands = tmpBands;
            mCurrentBand = mBands.elementAt(n);
    }

    @Override
    public FilterRepresentation copy() {
        FilterGradRepresentation representation = new FilterGradRepresentation();
        copyAllParameters(representation);
        return representation;
    }

    @Override
    protected void copyAllParameters(FilterRepresentation representation) {
        super.copyAllParameters(representation);
        representation.useParametersFrom(this);
    }

    @Override
    public boolean equals(FilterRepresentation representation) {
        if (representation instanceof FilterGradRepresentation) {
            FilterGradRepresentation rep = (FilterGradRepresentation) representation;
            int n = getNumberOfBands();
            if (rep.getNumberOfBands() != n) {
                return false;
            }
            for (int i = 0; i < mBands.size(); i++) {
                Band b1 = mBands.get(i);
                Band b2 = rep.mBands.get(i);
                if (b1.mask != b2.mask
                        || b1.brightness != b2.brightness
                        || b1.contrast != b2.contrast
                        || b1.saturation != b2.saturation
                        || b1.xPos1 != b2.xPos1
                        || b1.xPos2 != b2.xPos2
                        || b1.yPos1 != b2.yPos1
                        || b1.yPos2 != b2.yPos2) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public int getNumberOfBands() {
        int count = 0;
        for (Band point : mBands) {
            if (!point.mask) {
                count++;
            }
        }
        return count;
    }

    public int addBand(Rect rect) {
        mBands.add(0, mCurrentBand = new Band(rect.centerX(), rect.centerY()));
        mCurrentBand.mask = false;
        int x = (mCurrentBand.xPos1 + mCurrentBand.xPos2)/2;
        int y = (mCurrentBand.yPos1 + mCurrentBand.yPos2)/2;
        double addDelta = ADD_MIN_DIST * Math.max(rect.width(), rect.height());
        boolean moved = true;
        int count = 0;
        int toMove =  mBands.indexOf(mCurrentBand);

        while (moved) {
            moved = false;
            count++;
            if (count > 14) {
                break;
            }

            for (Band point : mBands) {
                if (point.mask) {
                    break;
                }
            }

            for (Band point : mBands) {
                if (point.mask) {
                    break;
                }
                int index = mBands.indexOf(point);

                if (toMove != index) {
                    double dist = Math.hypot(point.xPos1 - x, point.yPos1 - y);
                    if (dist < addDelta) {
                        moved = true;
                        mCurrentBand.xPos1 += addDelta;
                        mCurrentBand.yPos1 += addDelta;
                        mCurrentBand.xPos2 += addDelta;
                        mCurrentBand.yPos2 += addDelta;
                        x = (mCurrentBand.xPos1 + mCurrentBand.xPos2)/2;
                        y = (mCurrentBand.yPos1 + mCurrentBand.yPos2)/2;

                        if (mCurrentBand.yPos1 > rect.bottom) {
                            mCurrentBand.yPos1 = (int) (rect.top + addDelta);
                        }
                        if (mCurrentBand.xPos1 > rect.right) {
                            mCurrentBand.xPos1 = (int) (rect.left + addDelta);
                        }
                    }
                }
            }
        }
        trimVector();
        return 0;
    }

    public void deleteCurrentBand() {
        int index = mBands.indexOf(mCurrentBand);
        mBands.remove(mCurrentBand);
        trimVector();
        if (getNumberOfBands() == 0) {
            addBand(MasterImage.getImage().getOriginalBounds());
        }
        mCurrentBand = mBands.get(0);
    }

    public void  nextPoint(){
        int index = mBands.indexOf(mCurrentBand);
        int tmp = index;
        Band point;
        int k = 0;
        do  {
            index =   (index+1)% mBands.size();
            point = mBands.get(index);
            if (k++ >= mBands.size()) {
                break;
            }
        }
        while (point.mask == true);
        mCurrentBand = mBands.get(index);
    }

    public void setSelectedPoint(int pos) {
        mCurrentBand = mBands.get(pos);
    }

    public int getSelectedPoint() {
        return mBands.indexOf(mCurrentBand);
    }

    public boolean[] getMask() {
        boolean[] ret = new boolean[mBands.size()];
        int i = 0;
        for (Band point : mBands) {
            ret[i++] = !point.mask;
        }
        return ret;
    }

    public int[] getXPos1() {
        int[] ret = new int[mBands.size()];
        int i = 0;
        for (Band point : mBands) {
            ret[i++] = point.xPos1;
        }
        return ret;
    }

    public int[] getYPos1() {
        int[] ret = new int[mBands.size()];
        int i = 0;
        for (Band point : mBands) {
            ret[i++] = point.yPos1;
        }
        return ret;
    }

    public int[] getXPos2() {
        int[] ret = new int[mBands.size()];
        int i = 0;
        for (Band point : mBands) {
            ret[i++] = point.xPos2;
        }
        return ret;
    }

    public int[] getYPos2() {
        int[] ret = new int[mBands.size()];
        int i = 0;
        for (Band point : mBands) {
            ret[i++] = point.yPos2;
        }
        return ret;
    }

    public int[] getBrightness() {
        int[] ret = new int[mBands.size()];
        int i = 0;
        for (Band point : mBands) {
            ret[i++] = point.brightness;
        }
        return ret;
    }

    public int[] getContrast() {
        int[] ret = new int[mBands.size()];
        int i = 0;
        for (Band point : mBands) {
            ret[i++] = point.contrast;
        }
        return ret;
    }

    public int[] getSaturation() {
        int[] ret = new int[mBands.size()];
        int i = 0;
        for (Band point : mBands) {
            ret[i++] = point.saturation;
        }
        return ret;
    }

    public int getParameter(int type) {
        switch (type){
            case PARAM_BRIGHTNESS:
                return mCurrentBand.brightness;
            case PARAM_SATURATION:
                return mCurrentBand.saturation;
            case PARAM_CONTRAST:
                return mCurrentBand.contrast;
        }
        throw new IllegalArgumentException("no such type " + type);
    }

    public int getParameterMax(int type) {
        switch (type) {
            case PARAM_BRIGHTNESS:
                return 100;
            case PARAM_SATURATION:
                return 100;
            case PARAM_CONTRAST:
                return 100;
        }
        throw new IllegalArgumentException("no such type " + type);
    }

    public int getParameterMin(int type) {
        switch (type) {
            case PARAM_BRIGHTNESS:
                return -100;
            case PARAM_SATURATION:
                return -100;
            case PARAM_CONTRAST:
                return -100;
        }
        throw new IllegalArgumentException("no such type " + type);
    }

    public void setParameter(int type, int value) {
        mCurrentBand.mask = false;
        switch (type) {
            case PARAM_BRIGHTNESS:
                mCurrentBand.brightness = value;
                break;
            case PARAM_SATURATION:
                mCurrentBand.saturation = value;
                break;
            case PARAM_CONTRAST:
                mCurrentBand.contrast = value;
                break;
            default:
                throw new IllegalArgumentException("no such type " + type);
        }
    }

    @Override
    public void setPoint1(float x, float y) {
        mCurrentBand.xPos1 = (int)x;
        mCurrentBand.yPos1 = (int)y;
    }

    @Override
    public void setPoint2(float x, float y) {
        mCurrentBand.xPos2 = (int)x;
        mCurrentBand.yPos2 = (int)y;
    }

    @Override
    public float getPoint1X() {
        return mCurrentBand.xPos1;
    }

    @Override
    public float getPoint1Y() {
        return mCurrentBand.yPos1;
    }
    @Override
    public float getPoint2X() {
        return mCurrentBand.xPos2;
    }

    @Override
    public float getPoint2Y() {
        return mCurrentBand.yPos2;
    }

    @Override
    public void serializeRepresentation(JsonWriter writer) throws IOException {
        writer.beginObject();
        int len = mBands.size();
        int count = 0;

        for (int i = 0; i < len; i++) {
            Band point = mBands.get(i);
            if (point.mask) {
                continue;
            }
            writer.name(LINE_NAME + count);
            count++;
            writer.beginArray();
            writer.value(point.xPos1);
            writer.value(point.yPos1);
            writer.value(point.xPos2);
            writer.value(point.yPos2);
            writer.value(point.brightness);
            writer.value(point.contrast);
            writer.value(point.saturation);
            writer.endArray();
        }
        writer.endObject();
    }

    @Override
    public void deSerializeRepresentation(JsonReader sreader) throws IOException {
        sreader.beginObject();
        Vector<Band> points = new Vector<Band>();

        while (sreader.hasNext()) {
            String name = sreader.nextName();
            if (name.startsWith(LINE_NAME)) {
                int pointNo = Integer.parseInt(name.substring(LINE_NAME.length()));
                sreader.beginArray();
                Band p = new Band();
                p.mask = false;
                sreader.hasNext();
                p.xPos1 = sreader.nextInt();
                sreader.hasNext();
                p.yPos1 = sreader.nextInt();
                sreader.hasNext();
                p.xPos2 = sreader.nextInt();
                sreader.hasNext();
                p.yPos2 = sreader.nextInt();
                sreader.hasNext();
                p.brightness = sreader.nextInt();
                sreader.hasNext();
                p.contrast = sreader.nextInt();
                sreader.hasNext();
                p.saturation = sreader.nextInt();
                sreader.hasNext();
                sreader.endArray();
                points.add(p);

            } else {
                sreader.skipValue();
            }
        }
        mBands = points;
        trimVector();
        mCurrentBand = mBands.get(0);
        sreader.endObject();
    }
}
