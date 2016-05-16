/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.replica.replicaisland;

/**
 * Implements a bitmap that can be scrolled in place, such as the background of a scrolling
 * world.
 */
public class ScrollableBitmap extends DrawableBitmap {
    private float mScrollOriginX;
    private float mScrollOriginY;
    
    public ScrollableBitmap(Texture texture, int width, int height) {
        super(texture, width, height);
    }

    public void setScrollOrigin(float x, float y) {
        mScrollOriginX = x;
        mScrollOriginY = y;
    }

    @Override
    public void draw(float x, float y, float scaleX, float scaleY) {
        super.draw(x - mScrollOriginX, y - mScrollOriginY, scaleX, scaleY);
    }

    public float getScrollOriginX() {
        return mScrollOriginX;
    }

    public void setScrollOriginX(float scrollOriginX) {
        mScrollOriginX = scrollOriginX;
    }

    public float getScrollOriginY() {
        return mScrollOriginY;
    }

    public void setScrollOriginY(float scrollOriginY) {
        mScrollOriginY = scrollOriginY;
    }
}
