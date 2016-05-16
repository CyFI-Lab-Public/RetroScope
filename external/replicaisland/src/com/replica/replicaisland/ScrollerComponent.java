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
 * Adjusts the scroll position of a drawable object based on the camera's focus position.
 * May be used to scroll a ScrollableBitmap or TiledWorld to match the camera.  Uses DrawableFactory
 * to allocate fire-and-forget drawable objects every frame.
 */
public class ScrollerComponent extends GameComponent {
    private int mWidth;
    private int mHeight;
    private float mHalfWidth;
    private float mHalfHeight;
    private RenderComponent mRenderComponent;
    private float mSpeedX;
    private float mSpeedY;
    private Texture mTexture;
    private TiledVertexGrid mVertGrid;
    
    public ScrollerComponent(float speedX, float speedY, int width, int height, Texture texture) {
        super();
        reset();
        setup(speedX, speedY, width, height);
        setUseTexture(texture);
        setPhase(ComponentPhases.PRE_DRAW.ordinal());
    }
    
    public ScrollerComponent(float speedX, float speedY, int width, int height, TiledVertexGrid grid) {
        super();
        reset();
        setup(speedX, speedY, width, height);
        mVertGrid = grid;
        setPhase(ComponentPhases.PRE_DRAW.ordinal());
    }
    
    public ScrollerComponent() {
        super();
        reset();
        setPhase(ComponentPhases.PRE_DRAW.ordinal());
    }

    @Override
    public void reset() {
        mWidth = 0;
        mHeight = 0;
        mHalfWidth = 0.0f;
        mHalfHeight = 0.0f;
        mRenderComponent = null;
        mSpeedX = 0.0f;
        mSpeedY = 0.0f;
        mTexture = null;
        mVertGrid = null;
    }
    
    public void setScrollSpeed(float speedX, float speedY) {
        mSpeedX = speedX;
        mSpeedY = speedY;
    }
    
    public void setup(float speedX, float speedY, int width, int height) {
        mSpeedX = speedX;
        mSpeedY = speedY;
        mWidth = width;
        mHeight = height;
        mHalfWidth = sSystemRegistry.contextParameters.gameWidth / 2.0f; //width / 2.0f;
        mHalfHeight = sSystemRegistry.contextParameters.gameHeight / 2.0f; //height / 2.0f;
    }
    
    public void setUseTexture(Texture texture) {
        mTexture = texture;
    }
    
    @Override
    public void update(float timeDelta, BaseObject parent) {
        final DrawableFactory drawableFactory = sSystemRegistry.drawableFactory;
        if (mRenderComponent != null && drawableFactory != null) {
            ScrollableBitmap background;
            if (mVertGrid != null) {
                TiledBackgroundVertexGrid bg = drawableFactory.allocateTiledBackgroundVertexGrid();
                bg.setGrid(mVertGrid);
                background = bg;
            } else {
                background = drawableFactory.allocateScrollableBitmap();
                background.setTexture(mTexture);
            }

            background.setWidth(mWidth);
            background.setHeight(mHeight);

            CameraSystem camera = sSystemRegistry.cameraSystem;

            float originX = camera.getFocusPositionX() - mHalfWidth;
            float originY = camera.getFocusPositionY() - mHalfHeight;

            originX *= mSpeedX;
            originY *= mSpeedY;

            background.setScrollOrigin(originX, originY);
            mRenderComponent.setDrawable(background);
        }
    }

    public void setRenderComponent(RenderComponent render) {
        mRenderComponent = render;
    }
}
