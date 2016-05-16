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

package com.android.mail.ui;

import android.graphics.Bitmap;

/**
 * A canvas to draw loaded photos.
 */
public interface ImageCanvas {

    /**
     * Dimensions holds the desired width, height, and scale for a bitmap being
     * placed in the ImageCanvas.
     */
    public static class Dimensions {
        public int width;
        public int height;
        public float scale;

        public static final float SCALE_ONE = 1.0f;
        public static final float SCALE_HALF = 0.5f;
        public static final float SCALE_QUARTER = 0.25f;

        public Dimensions() {
        }

        public Dimensions(int w, int h, float s) {
            width = w;
            height = h;
            scale = s;
        }

        @Override
        public String toString() {
            return String.format("Dimens [%d x %d]", width, height);
        }
    }

    /**
     * Draw/composite the given Bitmap corresponding with the key 'id'. It will be sized according
     * to whatever {@link #getDesiredDimensions(Object, Dimensions)} reported when the
     * decode request was made.
     *
     * @param decoded an exactly-sized, decoded bitmap to display
     * @param key
     */
    void drawImage(Bitmap decoded, Object key);

    /**
     * Reset all state associated with this view so that it can be reused.
     */
    void reset();

    /**
     * Outputs the desired dimensions that the object with key 'id' would like to be drawn to.
     *
     * @param key
     * @param outDim caller-allocated {@link Dimensions} object to house the result
     */
    void getDesiredDimensions(Object key, Dimensions outDim);

    /**
     * Return an arbitrary integer to associate with any asynchronous requests for images that
     * currently belong to this canvas. If, later on when results are available, the generation
     * that is then reported does not match, the photo manager will assume the image is no longer
     * desired and will not offer the image.
     * <p>
     * Implementors should basically treat this as a counter to increment upon reset() or
     * data binding.
     */
    int getGeneration();
}
