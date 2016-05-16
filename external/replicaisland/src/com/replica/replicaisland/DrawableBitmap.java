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

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11Ext;

/** 
 * Draws a screen-aligned bitmap to the screen.
 */
public class DrawableBitmap extends DrawableObject {
    
    private Texture mTexture;
    private int mWidth;
    private int mHeight;
    private int mCrop[];
    private int mViewWidth;
    private int mViewHeight;
    private float mOpacity;
    
    DrawableBitmap(Texture texture, int width, int height) {
        super();
        mTexture = texture;
        mWidth = width;
        mHeight = height;
        mCrop = new int[4];
        mViewWidth = 0;
        mViewHeight = 0;
        mOpacity = 1.0f;
        setCrop(0, height, width, height);
    }

    public void reset() {
        mTexture = null;
        mViewWidth = 0;
        mViewHeight = 0;
        mOpacity = 1.0f;
        
    }
  
    public void setViewSize(int width, int height) {
        mViewHeight = height;
        mViewWidth = width;
    }
    
    public void setOpacity(float opacity) {
        mOpacity = opacity;
    }
    
    /**
     * Begins drawing bitmaps. Sets the OpenGL state for rapid drawing.
     * 
     * @param gl  A pointer to the OpenGL context.
     * @param viewWidth  The width of the screen.
     * @param viewHeight  The height of the screen.
     */
    public static void beginDrawing(GL10 gl, float viewWidth, float viewHeight) {
        gl.glShadeModel(GL10.GL_FLAT);
        gl.glEnable(GL10.GL_BLEND);
        gl.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA);
        gl.glColor4x(0x10000, 0x10000, 0x10000, 0x10000);

        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glOrthof(0.0f, viewWidth, 0.0f, viewHeight, 0.0f, 1.0f);
        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glLoadIdentity();
       
        gl.glEnable(GL10.GL_TEXTURE_2D);

    }

    /**
     * Draw the bitmap at a given x,y position, expressed in pixels, with the
     * lower-left-hand-corner of the view being (0,0).
     * 
     * @param gl  A pointer to the OpenGL context
     * @param x  The number of pixels to offset this drawable's origin in the x-axis.
     * @param y  The number of pixels to offset this drawable's origin in the y-axis
     * @param scaleX The horizontal scale factor between the bitmap resolution and the display resolution.
     * @param scaleY The vertical scale factor between the bitmap resolution and the display resolution.
     */
    @Override
    public void draw(float x, float y, float scaleX, float scaleY) {
        GL10 gl = OpenGLSystem.getGL();
        final Texture texture = mTexture;
        
        if (gl != null && texture != null) {
            assert texture.loaded;
            
            final float snappedX = (int) x;
            final float snappedY = (int) y;
                 
            final float opacity = mOpacity;
            final float width = mWidth;
            final float height = mHeight;
            final float viewWidth = mViewWidth;
            final float viewHeight = mViewHeight;
            
            boolean cull = false;
            if (viewWidth > 0) {
                if (snappedX + width < 0.0f 
                		|| snappedX > viewWidth 
                        || snappedY + height < 0.0f
                        || snappedY > viewHeight 
                        || opacity == 0.0f
                        || !texture.loaded) {
                    cull = true;
                }
            }
            if (!cull) {
                OpenGLSystem.bindTexture(GL10.GL_TEXTURE_2D, texture.name);

                // This is necessary because we could be drawing the same texture with different
                // crop (say, flipped horizontally) on the same frame.
                OpenGLSystem.setTextureCrop(mCrop);
               
                if (opacity < 1.0f) {
                    gl.glColor4f(opacity, opacity, opacity, opacity);
                }
                
                ((GL11Ext) gl).glDrawTexfOES(snappedX * scaleX, snappedY * scaleY, 
                		getPriority(), width * scaleX, height * scaleY);
                
                if (opacity < 1.0f) {
                    gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
                }
            }
        }
    }

    /**
     * Ends the drawing and restores the OpenGL state.
     * 
     * @param gl  A pointer to the OpenGL context.
     */
    public static void endDrawing(GL10 gl) {
        gl.glDisable(GL10.GL_BLEND);
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glPopMatrix();
        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glPopMatrix();
    }

    public void resize(int width, int height) {
        mWidth = width;
        mHeight = height;
        setCrop(0, height, width, height);
    }

    public int getWidth() {
        return mWidth;
    }

    public void setWidth(int width) {
        mWidth = width;
    }

    public int getHeight() {
        return mHeight;
    }

    public void setHeight(int height) {
        mHeight = height;
    }

    /**
     * Changes the crop parameters of this bitmap.  Note that the underlying OpenGL texture's
     * parameters are not changed immediately The crop is updated on the
     * next call to draw().  Note that the image may be flipped by providing a negative width or
     * height.
     * 
     * @param left
     * @param bottom
     * @param width
     * @param height
     */
    public void setCrop(int left, int bottom, int width, int height) {
        // Negative width and height values will flip the image.
        mCrop[0] = left;
        mCrop[1] = bottom;
        mCrop[2] = width;
        mCrop[3] = -height;
    }

    public int[] getCrop() {
        return mCrop;
    }

    public void setTexture(Texture texture) {
        mTexture = texture;
    }

    @Override
    public Texture getTexture() {
        return mTexture;
    }

   @Override
   public boolean visibleAtPosition(Vector2 position) {
       boolean cull = false;
       if (mViewWidth > 0) {
           if (position.x + mWidth < 0 || position.x > mViewWidth 
                   || position.y + mHeight < 0 || position.y > mViewHeight) {
               cull = true;
           }
       }
       return !cull;
   }
   
   protected final void setFlip(boolean horzFlip, boolean vertFlip) {
       setCrop(horzFlip ? mWidth : 0, 
               vertFlip ? 0 : mHeight, 
               horzFlip ? -mWidth : mWidth,
               vertFlip ? -mHeight : mHeight);
   }
}
