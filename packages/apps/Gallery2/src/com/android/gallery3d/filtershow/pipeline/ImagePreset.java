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

package com.android.gallery3d.filtershow.pipeline;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.support.v8.renderscript.Allocation;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.cache.BitmapCache;
import com.android.gallery3d.filtershow.cache.ImageLoader;
import com.android.gallery3d.filtershow.filters.BaseFiltersManager;
import com.android.gallery3d.filtershow.filters.FilterCropRepresentation;
import com.android.gallery3d.filtershow.filters.FilterFxRepresentation;
import com.android.gallery3d.filtershow.filters.FilterImageBorderRepresentation;
import com.android.gallery3d.filtershow.filters.FilterMirrorRepresentation;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import com.android.gallery3d.filtershow.filters.FilterRotateRepresentation;
import com.android.gallery3d.filtershow.filters.FilterStraightenRepresentation;
import com.android.gallery3d.filtershow.filters.FilterUserPresetRepresentation;
import com.android.gallery3d.filtershow.filters.FiltersManager;
import com.android.gallery3d.filtershow.filters.ImageFilter;
import com.android.gallery3d.filtershow.imageshow.GeometryMathUtils;
import com.android.gallery3d.filtershow.imageshow.MasterImage;
import com.android.gallery3d.filtershow.state.State;
import com.android.gallery3d.filtershow.state.StateAdapter;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Vector;

public class ImagePreset {

    private static final String LOGTAG = "ImagePreset";
    public static final String JASON_SAVED = "Saved";

    private Vector<FilterRepresentation> mFilters = new Vector<FilterRepresentation>();

    private boolean mDoApplyGeometry = true;
    private boolean mDoApplyFilters = true;

    private boolean mPartialRendering = false;
    private Rect mPartialRenderingBounds;
    private static final boolean DEBUG = false;

    public ImagePreset() {
    }

    public ImagePreset(ImagePreset source) {
        for (int i = 0; i < source.mFilters.size(); i++) {
            FilterRepresentation sourceRepresentation = source.mFilters.elementAt(i);
            mFilters.add(sourceRepresentation.copy());
        }
    }

    public Vector<FilterRepresentation> getFilters() {
        return mFilters;
    }

    public FilterRepresentation getFilterRepresentation(int position) {
        FilterRepresentation representation = null;

        representation = mFilters.elementAt(position).copy();

        return representation;
    }

    private static boolean sameSerializationName(String a, String b) {
        if (a != null && b != null) {
            return a.equals(b);
        } else {
            return a == null && b == null;
        }
    }

    public static boolean sameSerializationName(FilterRepresentation a, FilterRepresentation b) {
        if (a == null || b == null) {
            return false;
        }
        return sameSerializationName(a.getSerializationName(), b.getSerializationName());
    }

    public int getPositionForRepresentation(FilterRepresentation representation) {
        for (int i = 0; i < mFilters.size(); i++) {
            if (sameSerializationName(mFilters.elementAt(i), representation)) {
                return i;
            }
        }
        return -1;
    }

    private FilterRepresentation getFilterRepresentationForType(int type) {
        for (int i = 0; i < mFilters.size(); i++) {
            if (mFilters.elementAt(i).getFilterType() == type) {
                return mFilters.elementAt(i);
            }
        }
        return null;
    }

    public int getPositionForType(int type) {
        for (int i = 0; i < mFilters.size(); i++) {
            if (mFilters.elementAt(i).getFilterType() == type) {
                return i;
            }
        }
        return -1;
    }

    public FilterRepresentation getFilterRepresentationCopyFrom(
            FilterRepresentation filterRepresentation) {
        // TODO: add concept of position in the filters (to allow multiple instances)
        if (filterRepresentation == null) {
            return null;
        }
        int position = getPositionForRepresentation(filterRepresentation);
        if (position == -1) {
            return null;
        }
        FilterRepresentation representation = mFilters.elementAt(position);
        if (representation != null) {
            representation = representation.copy();
        }
        return representation;
    }

    public void updateFilterRepresentations(Collection<FilterRepresentation> reps) {
        for (FilterRepresentation r : reps) {
            updateOrAddFilterRepresentation(r);
        }
    }

    public void updateOrAddFilterRepresentation(FilterRepresentation rep) {
        int pos = getPositionForRepresentation(rep);
        if (pos != -1) {
            mFilters.elementAt(pos).useParametersFrom(rep);
        } else {
            addFilter(rep.copy());
        }
    }

    public void setDoApplyGeometry(boolean value) {
        mDoApplyGeometry = value;
    }

    public void setDoApplyFilters(boolean value) {
        mDoApplyFilters = value;
    }

    public boolean getDoApplyFilters() {
        return mDoApplyFilters;
    }

    public boolean hasModifications() {
        for (int i = 0; i < mFilters.size(); i++) {
            FilterRepresentation filter = mFilters.elementAt(i);
            if (!filter.isNil()) {
                return true;
            }
        }
        return false;
    }

    public boolean contains(byte type) {
        for (FilterRepresentation representation : mFilters) {
            if (representation.getFilterType() == type
                    && !representation.isNil()) {
                return true;
            }
        }
        return false;
    }

    public boolean isPanoramaSafe() {
        for (FilterRepresentation representation : mFilters) {
            if (representation.getFilterType() == FilterRepresentation.TYPE_GEOMETRY
                    && !representation.isNil()) {
                return false;
            }
            if (representation.getFilterType() == FilterRepresentation.TYPE_BORDER
                    && !representation.isNil()) {
                return false;
            }
            if (representation.getFilterType() == FilterRepresentation.TYPE_VIGNETTE
                    && !representation.isNil()) {
                return false;
            }
            if (representation.getFilterType() == FilterRepresentation.TYPE_TINYPLANET
                    && !representation.isNil()) {
                return false;
            }
        }
        return true;
    }

    public boolean same(ImagePreset preset) {
        if (preset == null) {
            return false;
        }

        if (preset.mFilters.size() != mFilters.size()) {
            return false;
        }

        if (mDoApplyGeometry != preset.mDoApplyGeometry) {
            return false;
        }

        if (mDoApplyFilters != preset.mDoApplyFilters) {
            if (mFilters.size() > 0 || preset.mFilters.size() > 0) {
                return false;
            }
        }

        if (mDoApplyFilters && preset.mDoApplyFilters) {
            for (int i = 0; i < preset.mFilters.size(); i++) {
                FilterRepresentation a = preset.mFilters.elementAt(i);
                FilterRepresentation b = mFilters.elementAt(i);

                if (!a.same(b)) {
                    return false;
                }
            }
        }

        return true;
    }

    public boolean equals(ImagePreset preset) {
        if (preset == null) {
            return false;
        }

        if (preset.mFilters.size() != mFilters.size()) {
            return false;
        }

        if (mDoApplyGeometry != preset.mDoApplyGeometry) {
            return false;
        }

        if (mDoApplyFilters != preset.mDoApplyFilters) {
            if (mFilters.size() > 0 || preset.mFilters.size() > 0) {
                return false;
            }
        }

        for (int i = 0; i < preset.mFilters.size(); i++) {
            FilterRepresentation a = preset.mFilters.elementAt(i);
            FilterRepresentation b = mFilters.elementAt(i);
            boolean isGeometry = false;
            if (a instanceof FilterRotateRepresentation
                    || a instanceof FilterMirrorRepresentation
                    || a instanceof FilterCropRepresentation
                    || a instanceof FilterStraightenRepresentation) {
                isGeometry = true;
            }
            boolean evaluate = true;
            if (!isGeometry && mDoApplyGeometry && !mDoApplyFilters) {
                evaluate = false;
            } else if (isGeometry && !mDoApplyGeometry && mDoApplyFilters) {
                evaluate = false;
            }
            if (evaluate && !a.equals(b)) {
                return false;
            }
        }

        return true;
    }

    public int similarUpTo(ImagePreset preset) {
        for (int i = 0; i < preset.mFilters.size(); i++) {
            FilterRepresentation a = preset.mFilters.elementAt(i);
            if (i < mFilters.size()) {
                FilterRepresentation b = mFilters.elementAt(i);
                if (!a.same(b)) {
                    return i;
                }
                if (!a.equals(b)) {
                    return i;
                }
            } else {
                return i;
            }
        }
        return preset.mFilters.size();
    }

    public void showFilters() {
        Log.v(LOGTAG, "\\\\\\ showFilters -- " + mFilters.size() + " filters");
        int n = 0;
        for (FilterRepresentation representation : mFilters) {
            Log.v(LOGTAG, " filter " + n + " : " + representation.toString());
            n++;
        }
        Log.v(LOGTAG, "/// showFilters -- " + mFilters.size() + " filters");
    }

    public FilterRepresentation getLastRepresentation() {
        if (mFilters.size() > 0) {
            return mFilters.lastElement();
        }
        return null;
    }

    public void removeFilter(FilterRepresentation filterRepresentation) {
        if (filterRepresentation.getFilterType() == FilterRepresentation.TYPE_BORDER) {
            for (int i = 0; i < mFilters.size(); i++) {
                if (mFilters.elementAt(i).getFilterType()
                == filterRepresentation.getFilterType()) {
                    mFilters.remove(i);
                    break;
                }
            }
        } else {
            for (int i = 0; i < mFilters.size(); i++) {
                if (sameSerializationName(mFilters.elementAt(i), filterRepresentation)) {
                    mFilters.remove(i);
                    break;
                }
            }
        }
    }

    // If the filter is an "None" effect or border, then just don't add this filter.
    public void addFilter(FilterRepresentation representation) {
        if (representation instanceof FilterUserPresetRepresentation) {
            ImagePreset preset = ((FilterUserPresetRepresentation) representation).getImagePreset();
            if (preset.nbFilters() == 1
                && preset.contains(FilterRepresentation.TYPE_FX)) {
                FilterRepresentation rep = preset.getFilterRepresentationForType(
                        FilterRepresentation.TYPE_FX);
                addFilter(rep);
            } else {
                // user preset replaces everything
                mFilters.clear();
                for (int i = 0; i < preset.nbFilters(); i++) {
                    addFilter(preset.getFilterRepresentation(i));
                }
            }
        } else if (representation.getFilterType() == FilterRepresentation.TYPE_GEOMETRY) {
            // Add geometry filter, removing duplicates and do-nothing operations.
            for (int i = 0; i < mFilters.size(); i++) {
                if (sameSerializationName(representation, mFilters.elementAt(i))) {
                    mFilters.remove(i);
                }
            }
            int index = 0;
            for (; index < mFilters.size(); index++) {
                FilterRepresentation rep = mFilters.elementAt(index);
                if (rep.getFilterType() != FilterRepresentation.TYPE_GEOMETRY) {
                    break;
                }
            }
            if (!representation.isNil()) {
                mFilters.insertElementAt(representation, index);
            }
        } else if (representation.getFilterType() == FilterRepresentation.TYPE_BORDER) {
            removeFilter(representation);
            if (!isNoneBorderFilter(representation)) {
                mFilters.add(representation);
            }
        } else if (representation.getFilterType() == FilterRepresentation.TYPE_FX) {
            boolean replaced = false;
            for (int i = 0; i < mFilters.size(); i++) {
                FilterRepresentation current = mFilters.elementAt(i);
                if (current.getFilterType() == FilterRepresentation.TYPE_FX) {
                    mFilters.remove(i);
                    replaced = true;
                    if (!isNoneFxFilter(representation)) {
                        mFilters.add(i, representation);
                    }
                    break;
                }
            }
            if (!replaced && !isNoneFxFilter(representation)) {
                mFilters.add(0, representation);
            }
        } else {
            mFilters.add(representation);
        }
        // Enforces Filter type ordering for borders
        FilterRepresentation border = null;
        for (int i = 0; i < mFilters.size();) {
            FilterRepresentation rep = mFilters.elementAt(i);
            if (rep.getFilterType() == FilterRepresentation.TYPE_BORDER) {
                border = rep;
                mFilters.remove(i);
                continue;
            }
            i++;
        }
        if (border != null) {
            mFilters.add(border);
        }
    }

    private boolean isNoneBorderFilter(FilterRepresentation representation) {
        return representation instanceof FilterImageBorderRepresentation &&
                ((FilterImageBorderRepresentation) representation).getDrawableResource() == 0;
    }

    private boolean isNoneFxFilter(FilterRepresentation representation) {
        return representation instanceof FilterFxRepresentation &&
                ((FilterFxRepresentation) representation).getNameResource() == R.string.none;
    }

    public FilterRepresentation getRepresentation(FilterRepresentation filterRepresentation) {
        for (int i = 0; i < mFilters.size(); i++) {
            FilterRepresentation representation = mFilters.elementAt(i);
            if (sameSerializationName(representation, filterRepresentation)) {
                return representation;
            }
        }
        return null;
    }

    public Bitmap apply(Bitmap original, FilterEnvironment environment) {
        Bitmap bitmap = original;
        bitmap = applyFilters(bitmap, -1, -1, environment);
        return applyBorder(bitmap, environment);
    }

    public Collection<FilterRepresentation> getGeometryFilters() {
        ArrayList<FilterRepresentation> geometry = new ArrayList<FilterRepresentation>();
        for (FilterRepresentation r : mFilters) {
            if (r.getFilterType() == FilterRepresentation.TYPE_GEOMETRY) {
                geometry.add(r);
            }
        }
        return geometry;
    }

    public FilterRepresentation getFilterWithSerializationName(String serializationName) {
        for (FilterRepresentation r : mFilters) {
            if (r != null) {
                if (sameSerializationName(r.getSerializationName(), serializationName)) {
                    return r.copy();
                }
            }
        }
        return null;
    }

    public Rect finalGeometryRect(int width, int height) {
        return GeometryMathUtils.finalGeometryRect(width, height, getGeometryFilters());
    }

    public Bitmap applyGeometry(Bitmap bitmap, FilterEnvironment environment) {
        // Apply any transform -- 90 rotate, flip, straighten, crop
        // Returns a new bitmap.
        if (mDoApplyGeometry) {
            Bitmap bmp = GeometryMathUtils.applyGeometryRepresentations(
                    getGeometryFilters(), bitmap);
            if (bmp != bitmap) {
                environment.cache(bitmap);
            }
            return bmp;
        }
        return bitmap;
    }

    public Bitmap applyBorder(Bitmap bitmap, FilterEnvironment environment) {
        // get the border from the list of filters.
        FilterRepresentation border = getFilterRepresentationForType(
                FilterRepresentation.TYPE_BORDER);
        if (border != null && mDoApplyGeometry) {
            bitmap = environment.applyRepresentation(border, bitmap);
            if (environment.getQuality() == FilterEnvironment.QUALITY_FINAL) {
            }
        }
        return bitmap;
    }

    public int nbFilters() {
        return mFilters.size();
    }

    public Bitmap applyFilters(Bitmap bitmap, int from, int to, FilterEnvironment environment) {
        if (mDoApplyFilters) {
            if (from < 0) {
                from = 0;
            }
            if (to == -1) {
                to = mFilters.size();
            }
            for (int i = from; i < to; i++) {
                FilterRepresentation representation = mFilters.elementAt(i);
                if (representation.getFilterType() == FilterRepresentation.TYPE_GEOMETRY) {
                    // skip the geometry as it's already applied.
                    continue;
                }
                if (representation.getFilterType() == FilterRepresentation.TYPE_BORDER) {
                    // for now, let's skip the border as it will be applied in
                    // applyBorder()
                    // TODO: might be worth getting rid of applyBorder.
                    continue;
                }
                Bitmap tmp = bitmap;
                bitmap = environment.applyRepresentation(representation, bitmap);
                if (tmp != bitmap) {
                    environment.cache(tmp);
                }
                if (environment.needsStop()) {
                    return bitmap;
                }
            }
        }

        return bitmap;
    }

    public void applyBorder(Allocation in, Allocation out,
            boolean copyOut, FilterEnvironment environment) {
        FilterRepresentation border = getFilterRepresentationForType(
                FilterRepresentation.TYPE_BORDER);
        if (border != null && mDoApplyGeometry) {
            // TODO: should keep the bitmap around
            Allocation bitmapIn = in;
            if (copyOut) {
                bitmapIn = Allocation.createTyped(
                        CachingPipeline.getRenderScriptContext(), in.getType());
                bitmapIn.copyFrom(out);
            }
            environment.applyRepresentation(border, bitmapIn, out);
        }
    }

    public void applyFilters(int from, int to, Allocation in, Allocation out,
            FilterEnvironment environment) {
        if (mDoApplyFilters) {
            if (from < 0) {
                from = 0;
            }
            if (to == -1) {
                to = mFilters.size();
            }
            for (int i = from; i < to; i++) {
                FilterRepresentation representation = mFilters.elementAt(i);
                if (representation.getFilterType() == FilterRepresentation.TYPE_GEOMETRY
                        || representation.getFilterType() == FilterRepresentation.TYPE_BORDER) {
                    continue;
                }
                if (i > from) {
                    in.copyFrom(out);
                }
                environment.applyRepresentation(representation, in, out);
            }
        }
    }

    public boolean canDoPartialRendering() {
        if (MasterImage.getImage().getZoomOrientation() != ImageLoader.ORI_NORMAL) {
            return false;
        }
        for (int i = 0; i < mFilters.size(); i++) {
            FilterRepresentation representation = mFilters.elementAt(i);
            if (!representation.supportsPartialRendering()) {
                return false;
            }
        }
        return true;
    }

    public void fillImageStateAdapter(StateAdapter imageStateAdapter) {
        if (imageStateAdapter == null) {
            return;
        }
        Vector<State> states = new Vector<State>();
        for (FilterRepresentation filter : mFilters) {
            if (filter instanceof FilterUserPresetRepresentation) {
                // do not show the user preset itself in the state panel
                continue;
            }
            State state = new State(filter.getName());
            state.setFilterRepresentation(filter);
            states.add(state);
        }
        imageStateAdapter.fill(states);
    }

    public void setPartialRendering(boolean partialRendering, Rect bounds) {
        mPartialRendering = partialRendering;
        mPartialRenderingBounds = bounds;
    }

    public boolean isPartialRendering() {
        return mPartialRendering;
    }

    public Rect getPartialRenderingBounds() {
        return mPartialRenderingBounds;
    }

    public Vector<ImageFilter> getUsedFilters(BaseFiltersManager filtersManager) {
        Vector<ImageFilter> usedFilters = new Vector<ImageFilter>();
        for (int i = 0; i < mFilters.size(); i++) {
            FilterRepresentation representation = mFilters.elementAt(i);
            ImageFilter filter = filtersManager.getFilterForRepresentation(representation);
            usedFilters.add(filter);
        }
        return usedFilters;
    }

    public String getJsonString(String name) {
        StringWriter swriter = new StringWriter();
        try {
            JsonWriter writer = new JsonWriter(swriter);
            writeJson(writer, name);
            writer.close();
        } catch (IOException e) {
            return null;
        }
        return swriter.toString();
    }

    public void writeJson(JsonWriter writer, String name) {
        int numFilters = mFilters.size();
        try {
            writer.beginObject();
            for (int i = 0; i < numFilters; i++) {
                FilterRepresentation filter = mFilters.get(i);
                if (filter instanceof FilterUserPresetRepresentation) {
                    continue;
                }
                String sname = filter.getSerializationName();
                if (DEBUG) {
                    Log.v(LOGTAG, "Serialization: " + sname);
                    if (sname == null) {
                        Log.v(LOGTAG, "Serialization name null for filter: " + filter);
                    }
                }
                writer.name(sname);
                filter.serializeRepresentation(writer);
            }
            writer.endObject();

        } catch (IOException e) {
           Log.e(LOGTAG,"Error encoding JASON",e);
        }
    }

    /**
     * populates preset from JSON string
     *
     * @param filterString a JSON string
     * @return true on success if false ImagePreset is undefined
     */
    public boolean readJsonFromString(String filterString) {
        if (DEBUG) {
            Log.v(LOGTAG, "reading preset: \"" + filterString + "\"");
        }
        StringReader sreader = new StringReader(filterString);
        try {
            JsonReader reader = new JsonReader(sreader);
            boolean ok = readJson(reader);
            if (!ok) {
                reader.close();
                return false;
            }
            reader.close();
        } catch (Exception e) {
            Log.e(LOGTAG, "\""+filterString+"\"");
            Log.e(LOGTAG, "parsing the filter parameters:", e);
            return false;
        }
        return true;
    }

    /**
     * populates preset from JSON stream
     *
     * @param sreader a JSON string
     * @return true on success if false ImagePreset is undefined
     */
    public boolean readJson(JsonReader sreader) throws IOException {
        sreader.beginObject();

        while (sreader.hasNext()) {
            String name = sreader.nextName();
            FilterRepresentation filter = creatFilterFromName(name);
            if (filter == null) {
                Log.w(LOGTAG, "UNKNOWN FILTER! " + name);
                return false;
            }
            filter.deSerializeRepresentation(sreader);
            addFilter(filter);
        }
        sreader.endObject();
        return true;
    }

    FilterRepresentation creatFilterFromName(String name) {
        if (FilterRotateRepresentation.SERIALIZATION_NAME.equals(name)) {
            return new FilterRotateRepresentation();
        } else if (FilterMirrorRepresentation.SERIALIZATION_NAME.equals(name)) {
            return new FilterMirrorRepresentation();
        } else if (FilterStraightenRepresentation.SERIALIZATION_NAME.equals(name)) {
            return new FilterStraightenRepresentation();
        } else if (FilterCropRepresentation.SERIALIZATION_NAME.equals(name)) {
            return new FilterCropRepresentation();
        }
        FiltersManager filtersManager = FiltersManager.getManager();
        return filtersManager.createFilterFromName(name);
    }

    public void updateWith(ImagePreset preset) {
        if (preset.mFilters.size() != mFilters.size()) {
            Log.e(LOGTAG, "Updating a preset with an incompatible one");
            return;
        }
        for (int i = 0; i < mFilters.size(); i++) {
            FilterRepresentation destRepresentation = mFilters.elementAt(i);
            FilterRepresentation sourceRepresentation = preset.mFilters.elementAt(i);
            destRepresentation.useParametersFrom(sourceRepresentation);
        }
    }
}
