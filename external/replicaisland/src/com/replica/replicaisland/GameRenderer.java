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

import android.content.Context;
import android.os.Build;
import android.os.SystemClock;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.replica.replicaisland.RenderSystem.RenderElement;

/**
 * GameRenderer the top-level rendering interface for the game engine.  It is called by
 * GLSurfaceView and is responsible for submitting commands to OpenGL.  GameRenderer receives a
 * queue of renderable objects from the thread and uses that to draw the scene every frame.  If
 * no queue is available then no drawing is performed.  If the queue is not changed from frame to
 * frame, the same scene will be redrawn every frame.
 * The GameRenderer also invokes texture loads when it is activated.
 */
public class GameRenderer implements GLSurfaceView.Renderer {
    private static final int PROFILE_REPORT_DELAY = 3 * 1000;

    private int mWidth;
    private int mHeight;
    private int mHalfWidth;
    private int mHalfHeight;
    
    private float mScaleX;
    private float mScaleY;
    private Context mContext;
    private long mLastTime;
    private int mProfileFrames;
    private long mProfileWaitTime;
    private long mProfileFrameTime;
    private long mProfileSubmitTime;
    private int mProfileObjectCount;
    
    private ObjectManager mDrawQueue;
    private boolean mDrawQueueChanged;
    private Game mGame;
    private Object mDrawLock;
    
    float mCameraX;
    float mCameraY;
    
    boolean mCallbackRequested;
        
    public GameRenderer(Context context, Game game, int gameWidth, int gameHeight) {
        mContext = context;
        mGame = game;
        mWidth = gameWidth;
        mHeight = gameHeight;
        mHalfWidth = gameWidth / 2;
        mHalfHeight = gameHeight / 2;
        mScaleX = 1.0f;
        mScaleY = 1.0f;
        mDrawQueueChanged = false;
        mDrawLock = new Object();
        mCameraX = 0.0f;
        mCameraY = 0.0f;
        mCallbackRequested = false;
    }

    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        /*
         * Some one-time OpenGL initialization can be made here probably based
         * on features of this particular context
         */
        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST);

        gl.glClearColor(0.0f, 0.0f, 0.0f, 1);
        gl.glShadeModel(GL10.GL_FLAT);
        gl.glDisable(GL10.GL_DEPTH_TEST);
        gl.glEnable(GL10.GL_TEXTURE_2D);
        /*
         * By default, OpenGL enables features that improve quality but reduce
         * performance. One might want to tweak that especially on software
         * renderer.
         */
        gl.glDisable(GL10.GL_DITHER);
        gl.glDisable(GL10.GL_LIGHTING);

        gl.glTexEnvx(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, GL10.GL_MODULATE);

        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
       
        String extensions = gl.glGetString(GL10.GL_EXTENSIONS); 
        String version = gl.glGetString(GL10.GL_VERSION);
        String renderer = gl.glGetString(GL10.GL_RENDERER);
        boolean isSoftwareRenderer = renderer.contains("PixelFlinger");
        boolean isOpenGL10 = version.contains("1.0");
        boolean supportsDrawTexture = extensions.contains("draw_texture");
        // VBOs are standard in GLES1.1
        // No use using VBOs when software renderering, esp. since older versions of the software renderer
        // had a crash bug related to freeing VBOs.
        boolean supportsVBOs = !isSoftwareRenderer && (!isOpenGL10 || extensions.contains("vertex_buffer_object"));
        ContextParameters params = BaseObject.sSystemRegistry.contextParameters;
        params.supportsDrawTexture = supportsDrawTexture;
        params.supportsVBOs = supportsVBOs;
          
        hackBrokenDevices();
        
        DebugLog.i("Graphics Support", version + " (" + renderer + "): " +(supportsDrawTexture ?  "draw texture," : "") + (supportsVBOs ? "vbos" : ""));
        
        mGame.onSurfaceCreated();

    }
    
    private void hackBrokenDevices() {
    	// Some devices are broken.  Fix them here.  This is pretty much the only
    	// device-specific code in the whole project.  Ugh.
        ContextParameters params = BaseObject.sSystemRegistry.contextParameters;

       
    	if (Build.PRODUCT.contains("morrison")) {
    		// This is the Motorola Cliq.  This device LIES and says it supports
    		// VBOs, which it actually does not (or, more likely, the extensions string
    		// is correct and the GL JNI glue is broken).
    		params.supportsVBOs = false;
    		// TODO: if Motorola fixes this, I should switch to using the fingerprint
    		// (blur/morrison/morrison/morrison:1.5/CUPCAKE/091007:user/ota-rel-keys,release-keys)
    		// instead of the product name so that newer versions use VBOs.
    	}
    }
    
    public void loadTextures(GL10 gl, TextureLibrary library) {
        if (gl != null) {
            library.loadAll(mContext, gl);
            DebugLog.d("AndouKun", "Textures Loaded.");
        }
    }
    
    public void flushTextures(GL10 gl, TextureLibrary library) {
        if (gl != null) {
            library.deleteAll(gl);
            DebugLog.d("AndouKun", "Textures Unloaded.");
        }
    }
    
    public void loadBuffers(GL10 gl, BufferLibrary library) {
        if (gl != null) {
            library.generateHardwareBuffers(gl);
            DebugLog.d("AndouKun", "Buffers Created.");
        }
    }
    
    public void flushBuffers(GL10 gl, BufferLibrary library) {
        if (gl != null) {
            library.releaseHardwareBuffers(gl);
            DebugLog.d("AndouKun", "Buffers Released.");
        }
    }
    
    public void onSurfaceLost() {
        mGame.onSurfaceLost();
    }
  
    public void requestCallback() {
    	mCallbackRequested = true;
    }

    /** Draws the scene.  Note that the draw queue is locked for the duration of this function. */
    public void onDrawFrame(GL10 gl) {
      
        long time = SystemClock.uptimeMillis();
        long time_delta = (time - mLastTime);
        
        synchronized(mDrawLock) {
            if (!mDrawQueueChanged) {
                while (!mDrawQueueChanged) {
                    try {
                    	mDrawLock.wait();
                    } catch (InterruptedException e) {
                        // No big deal if this wait is interrupted.
                    }
                }
            }
            mDrawQueueChanged = false;
        }
        
        final long wait = SystemClock.uptimeMillis();
        
        if (mCallbackRequested) {
        	mGame.onSurfaceReady();
        	mCallbackRequested = false;
        }
        
        DrawableBitmap.beginDrawing(gl, mWidth, mHeight);

        synchronized (this) {
            if (mDrawQueue != null && mDrawQueue.getObjects().getCount() > 0) {
                OpenGLSystem.setGL(gl);
                FixedSizeArray<BaseObject> objects = mDrawQueue.getObjects();
                Object[] objectArray = objects.getArray();
                final int count = objects.getCount();
                final float scaleX = mScaleX;
                final float scaleY = mScaleY;
                final float halfWidth = mHalfWidth;
                final float halfHeight = mHalfHeight;
                mProfileObjectCount += count;
                for (int i = 0; i < count; i++) {
                    RenderElement element = (RenderElement)objectArray[i];
                    float x = element.x;
                    float y = element.y;
                    if (element.cameraRelative) {
                    	x = (x - mCameraX) + halfWidth;
                    	y = (y - mCameraY) + halfHeight;
                    }
                    element.mDrawable.draw(x, y, scaleX, scaleY);
                }
                OpenGLSystem.setGL(null);
            } else if (mDrawQueue == null) {
                // If we have no draw queue, clear the screen.  If we have a draw queue that
                // is empty, we'll leave the frame buffer alone.
                gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
            }
        }
        
        DrawableBitmap.endDrawing(gl);
        
        long time2 = SystemClock.uptimeMillis();
        mLastTime = time2;

        mProfileFrameTime += time_delta;
        mProfileSubmitTime += time2 - time;
        mProfileWaitTime += wait - time;
        
        mProfileFrames++;
        if (mProfileFrameTime > PROFILE_REPORT_DELAY) {
        	final int validFrames = mProfileFrames;
            final long averageFrameTime = mProfileFrameTime / validFrames;
            final long averageSubmitTime = mProfileSubmitTime / validFrames;
            final float averageObjectsPerFrame = (float)mProfileObjectCount / validFrames;
            final long averageWaitTime = mProfileWaitTime / validFrames;

            DebugLog.d("Render Profile", 
            		"Average Submit: " + averageSubmitTime 
            		+ "  Average Draw: " + averageFrameTime 
            		+ " Objects/Frame: " + averageObjectsPerFrame
            		+ " Wait Time: " + averageWaitTime);
           
            mProfileFrameTime = 0;
            mProfileSubmitTime = 0;
            mProfileFrames = 0;
            mProfileObjectCount = 0;
        }
        
    }

    public void onSurfaceChanged(GL10 gl, int w, int h) {
        DebugLog.d("AndouKun", "Surface Size Change: " + w + ", " + h);
        
        //mWidth = w;0
        //mHeight = h;
    	// ensure the same aspect ratio as the game
    	float scaleX = (float)w / mWidth;
    	float scaleY =  (float)h / mHeight;
    	final int viewportWidth = (int)(mWidth * scaleX);
    	final int viewportHeight = (int)(mHeight * scaleY);
        gl.glViewport(0, 0, viewportWidth, viewportHeight);
        mScaleX = scaleX;
        mScaleY = scaleY;

        
        /*
         * Set our projection matrix. This doesn't have to be done each time we
         * draw, but usually a new projection needs to be set when the viewport
         * is resized.
         */
        float ratio = (float) mWidth / mHeight;
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glFrustumf(-ratio, ratio, -1, 1, 1, 10);
        
        
        mGame.onSurfaceReady();
    }

    public synchronized void setDrawQueue(ObjectManager queue, float cameraX, float cameraY) {
		mDrawQueue = queue;
		mCameraX = cameraX;
		mCameraY = cameraY;
    	synchronized(mDrawLock) {
    		mDrawQueueChanged = true;
    		mDrawLock.notify();
    	}
    }
    
    public synchronized void onPause() {
    	// Stop waiting to avoid deadlock.
    	// TODO: this is a hack.  Probably this renderer
    	// should just use GLSurfaceView's non-continuious render
    	// mode.
    	synchronized(mDrawLock) {
    		mDrawQueueChanged = true;
    		mDrawLock.notify();
    	}
    }

    /**
     * This function blocks while drawFrame() is in progress, and may be used by other threads to
     * determine when drawing is occurring.
     */
    
    public synchronized void waitDrawingComplete() {
    }
    
    public void setContext(Context newContext) {
        mContext = newContext;
    }
    
    
}
