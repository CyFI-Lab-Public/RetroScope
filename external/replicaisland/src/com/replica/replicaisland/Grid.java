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

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

import android.util.Log;

/**
 * A 2D rectangular mesh. Can be drawn textured or untextured.
 * This version is modified from the original Grid.java (found in
 * the SpriteText package in the APIDemos Android sample) to support hardware
 * vertex buffers and to insert edges between grid squares for tiling.
 */
class Grid {
	private static final int FLOAT_SIZE = 4;
	private static final int FIXED_SIZE = 4;
	private static final int CHAR_SIZE = 2;
	
    private FloatBuffer mFloatVertexBuffer;
    private FloatBuffer mFloatTexCoordBuffer;
    private IntBuffer mFixedVertexBuffer;
    private IntBuffer mFixedTexCoordBuffer;
    private CharBuffer mIndexBuffer;
    
    private Buffer mVertexBuffer;
    private Buffer mTexCoordBuffer;
    private int mCoordinateSize;
    private int mCoordinateType;

    private int mVertsAcross;
    private int mVertsDown;
    private int mIndexCount;
    private boolean mUseHardwareBuffers;
    private int mVertBufferIndex;
    private int mIndexBufferIndex;
    private int mTextureCoordBufferIndex;
    
    public Grid(int quadsAcross, int quadsDown, boolean useFixedPoint) {
    	final int vertsAcross = quadsAcross * 2;
    	final int vertsDown = quadsDown * 2;
        if (vertsAcross < 0 || vertsAcross >= 65536) {
            throw new IllegalArgumentException("quadsAcross");
        }
        if (vertsDown < 0 || vertsDown >= 65536) {
            throw new IllegalArgumentException("quadsDown");
        }
        if (vertsAcross * vertsDown >= 65536) {
            throw new IllegalArgumentException("quadsAcross * quadsDown >= 32768");
        }

        mUseHardwareBuffers = false;
        
        mVertsAcross = vertsAcross;
        mVertsDown = vertsDown;
        int size = vertsAcross * vertsDown;
        
        
        if (useFixedPoint) {
        	mFixedVertexBuffer = ByteBuffer.allocateDirect(FIXED_SIZE * size * 3)
            	.order(ByteOrder.nativeOrder()).asIntBuffer();
        	mFixedTexCoordBuffer = ByteBuffer.allocateDirect(FIXED_SIZE * size * 2)
            	.order(ByteOrder.nativeOrder()).asIntBuffer();
        	
        	mVertexBuffer = mFixedVertexBuffer;
        	mTexCoordBuffer = mFixedTexCoordBuffer;
        	mCoordinateSize = FIXED_SIZE;
        	mCoordinateType = GL10.GL_FIXED;
        	
        } else {
        	mFloatVertexBuffer = ByteBuffer.allocateDirect(FLOAT_SIZE * size * 3)
            	.order(ByteOrder.nativeOrder()).asFloatBuffer();
        	mFloatTexCoordBuffer = ByteBuffer.allocateDirect(FLOAT_SIZE * size * 2)
            	.order(ByteOrder.nativeOrder()).asFloatBuffer();
        	
        	mVertexBuffer = mFloatVertexBuffer;
        	mTexCoordBuffer = mFloatTexCoordBuffer;
        	mCoordinateSize = FLOAT_SIZE;
        	mCoordinateType = GL10.GL_FLOAT;
        }
        
        

        
        int quadCount = quadsAcross * quadsDown;
        int indexCount = quadCount * 6;
        mIndexCount = indexCount;
        mIndexBuffer = ByteBuffer.allocateDirect(CHAR_SIZE * indexCount)
            .order(ByteOrder.nativeOrder()).asCharBuffer();

        /*
         * Initialize triangle list mesh.
         *
         *     [0]------[1]   [2]------[3] ...
         *      |    /   |     |    /   |
         *      |   /    |     |   /    |
         *      |  /     |     |  /     |
         *     [w]-----[w+1] [w+2]----[w+3]...
         *      |       |
         *
         */

        {
            int i = 0;
            for (int y = 0; y < quadsDown; y++) {
            	final int indexY = y * 2;
                for (int x = 0; x < quadsAcross; x++) {
                	final int indexX = x * 2;
                    char a = (char) (indexY * mVertsAcross + indexX);
                    char b = (char) (indexY * mVertsAcross + indexX + 1);
                    char c = (char) ((indexY + 1) * mVertsAcross + indexX);
                    char d = (char) ((indexY + 1) * mVertsAcross + indexX + 1);

                    mIndexBuffer.put(i++, a);
                    mIndexBuffer.put(i++, b);
                    mIndexBuffer.put(i++, c);

                    mIndexBuffer.put(i++, b);
                    mIndexBuffer.put(i++, c);
                    mIndexBuffer.put(i++, d);
                }
            }
        }
        
        mVertBufferIndex = 0;
    }

    public void set(int quadX, int quadY, float[][] positions, float[][] uvs) {
        if (quadX < 0 || quadX * 2 >= mVertsAcross) {
            throw new IllegalArgumentException("quadX");
        }
        if (quadY < 0 || quadY * 2 >= mVertsDown) {
            throw new IllegalArgumentException("quadY");
        }
        if (positions.length < 4) {
            throw new IllegalArgumentException("positions");
        }
        if (uvs.length < 4) {
            throw new IllegalArgumentException("quadY");
        }

        int i = quadX * 2;
        int j = quadY * 2;
        
        setVertex(i, j, 		positions[0][0], positions[0][1], positions[0][2], uvs[0][0], uvs[0][1]);
        setVertex(i + 1, j, 	positions[1][0], positions[1][1], positions[1][2], uvs[1][0], uvs[1][1]);
        setVertex(i, j + 1, 	positions[2][0], positions[2][1], positions[2][2], uvs[2][0], uvs[2][1]);
        setVertex(i + 1, j + 1, positions[3][0], positions[3][1], positions[3][2], uvs[3][0], uvs[3][1]);
    }
    
    
    private void setVertex(int i, int j, float x, float y, float z, float u, float v) {
	  if (i < 0 || i >= mVertsAcross) {
	       throw new IllegalArgumentException("i");
	   }
	   if (j < 0 || j >= mVertsDown) {
	       throw new IllegalArgumentException("j");
	   }

	   final int index = mVertsAcross * j + i;

	   final int posIndex = index * 3;
	   final int texIndex = index * 2;

	   
	   if (mCoordinateType == GL10.GL_FLOAT) {
	    mFloatVertexBuffer.put(posIndex, x);
	    mFloatVertexBuffer.put(posIndex + 1, y);
	    mFloatVertexBuffer.put(posIndex + 2, z);

	    mFloatTexCoordBuffer.put(texIndex, u);
	    mFloatTexCoordBuffer.put(texIndex + 1, v);
	   } else {
	    mFixedVertexBuffer.put(posIndex, (int)(x * (1 << 16)));
	    mFixedVertexBuffer.put(posIndex + 1, (int)(y * (1 << 16)));
	    mFixedVertexBuffer.put(posIndex + 2, (int)(z * (1 << 16)));

	    mFixedTexCoordBuffer.put(texIndex, (int)(u * (1 << 16)));
	    mFixedTexCoordBuffer.put(texIndex + 1, (int)(v * (1 << 16)));
	   }
	}
    
    public static void beginDrawing(GL10 gl, boolean useTexture) {
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        
        if (useTexture) {
            gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
            gl.glEnable(GL10.GL_TEXTURE_2D);
        } else {
            gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
            gl.glDisable(GL10.GL_TEXTURE_2D);
        }
    }
    
    public void beginDrawingStrips(GL10 gl, boolean useTexture) {
        beginDrawing(gl, useTexture);
        if (!mUseHardwareBuffers) {
            gl.glVertexPointer(3, mCoordinateType, 0, mVertexBuffer);
    
            if (useTexture) {
                gl.glTexCoordPointer(2, mCoordinateType, 0, mTexCoordBuffer);
            } 
            
        } else {
            GL11 gl11 = (GL11)gl;
            // draw using hardware buffers
            gl11.glBindBuffer(GL11.GL_ARRAY_BUFFER, mVertBufferIndex);
            gl11.glVertexPointer(3, mCoordinateType, 0, 0);
            
            gl11.glBindBuffer(GL11.GL_ARRAY_BUFFER, mTextureCoordBufferIndex);
            gl11.glTexCoordPointer(2, mCoordinateType, 0, 0);
            
            gl11.glBindBuffer(GL11.GL_ELEMENT_ARRAY_BUFFER, mIndexBufferIndex);
        }
    }
    
    // Assumes beginDrawingStrips() has been called before this.
    public void drawStrip(GL10 gl, boolean useTexture, int startIndex, int indexCount) {
    	int count = indexCount;
    	if (startIndex + indexCount >= mIndexCount) {
    		count = mIndexCount - startIndex;
    	}
    	if (!mUseHardwareBuffers) {
            gl.glDrawElements(GL10.GL_TRIANGLES, count,
                    GL10.GL_UNSIGNED_SHORT, mIndexBuffer.position(startIndex));
        } else {
        	GL11 gl11 = (GL11)gl;
            gl11.glDrawElements(GL11.GL_TRIANGLES, count,
                    GL11.GL_UNSIGNED_SHORT, startIndex * CHAR_SIZE);
 
        }
    }
    
    public void draw(GL10 gl, boolean useTexture) {
        if (!mUseHardwareBuffers) {
            gl.glVertexPointer(3, mCoordinateType, 0, mVertexBuffer);
    
            if (useTexture) {
                gl.glTexCoordPointer(2, mCoordinateType, 0, mTexCoordBuffer);
            } 
    
            gl.glDrawElements(GL10.GL_TRIANGLES, mIndexCount,
                    GL10.GL_UNSIGNED_SHORT, mIndexBuffer);
        } else {
            GL11 gl11 = (GL11)gl;
            // draw using hardware buffers
            gl11.glBindBuffer(GL11.GL_ARRAY_BUFFER, mVertBufferIndex);
            gl11.glVertexPointer(3, mCoordinateType, 0, 0);
            
            gl11.glBindBuffer(GL11.GL_ARRAY_BUFFER, mTextureCoordBufferIndex);
            gl11.glTexCoordPointer(2, mCoordinateType, 0, 0);
            
            gl11.glBindBuffer(GL11.GL_ELEMENT_ARRAY_BUFFER, mIndexBufferIndex);
            gl11.glDrawElements(GL11.GL_TRIANGLES, mIndexCount,
                    GL11.GL_UNSIGNED_SHORT, 0);
            
            gl11.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0);
            gl11.glBindBuffer(GL11.GL_ELEMENT_ARRAY_BUFFER, 0);


        }
    }
    
    public static void endDrawing(GL10 gl) {
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    }
    
    public boolean usingHardwareBuffers() {
        return mUseHardwareBuffers;
    }
    
    /** 
     * When the OpenGL ES device is lost, GL handles become invalidated.
     * In that case, we just want to "forget" the old handles (without
     * explicitly deleting them) and make new ones.
     */
    public void invalidateHardwareBuffers() {
        mVertBufferIndex = 0;
        mIndexBufferIndex = 0;
        mTextureCoordBufferIndex = 0;
        mUseHardwareBuffers = false;
    }
    
    /**
     * Deletes the hardware buffers allocated by this object (if any).
     */
    public void releaseHardwareBuffers(GL10 gl) {
        if (mUseHardwareBuffers) {
            if (gl instanceof GL11) {
                GL11 gl11 = (GL11)gl;
                int[] buffer = new int[1];
                buffer[0] = mVertBufferIndex;
                gl11.glDeleteBuffers(1, buffer, 0);
                
                buffer[0] = mTextureCoordBufferIndex;
                gl11.glDeleteBuffers(1, buffer, 0);
                
                buffer[0] = mIndexBufferIndex;
                gl11.glDeleteBuffers(1, buffer, 0);
            }
            
            invalidateHardwareBuffers();
        }
    }
    
    /** 
     * Allocates hardware buffers on the graphics card and fills them with
     * data if a buffer has not already been previously allocated.  Note that
     * this function uses the GL_OES_vertex_buffer_object extension, which is
     * not guaranteed to be supported on every device.
     * @param gl  A pointer to the OpenGL ES context.
     */
    public void generateHardwareBuffers(GL10 gl) {
        if (!mUseHardwareBuffers) {
        	DebugLog.i("Grid", "Using Hardware Buffers");
            if (gl instanceof GL11) {
                GL11 gl11 = (GL11)gl;
                int[] buffer = new int[1];
                
                // Allocate and fill the vertex buffer.
                gl11.glGenBuffers(1, buffer, 0);
                mVertBufferIndex = buffer[0];
                gl11.glBindBuffer(GL11.GL_ARRAY_BUFFER, mVertBufferIndex);
                final int vertexSize = mVertexBuffer.capacity() * mCoordinateSize;
                // too fast task switching leaves buffers in the middle pos which
                // crashes app
                mVertexBuffer.position(0);
                gl11.glBufferData(GL11.GL_ARRAY_BUFFER, vertexSize, 
                        mVertexBuffer, GL11.GL_STATIC_DRAW);
                
                // Allocate and fill the texture coordinate buffer.
                gl11.glGenBuffers(1, buffer, 0);
                mTextureCoordBufferIndex = buffer[0];
                gl11.glBindBuffer(GL11.GL_ARRAY_BUFFER, 
                        mTextureCoordBufferIndex);
                final int texCoordSize = 
                    mTexCoordBuffer.capacity() * mCoordinateSize;
                mTexCoordBuffer.position(0);
                gl11.glBufferData(GL11.GL_ARRAY_BUFFER, texCoordSize, 
                        mTexCoordBuffer, GL11.GL_STATIC_DRAW);    
                
                // Unbind the array buffer.
                gl11.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0);
                
                // Allocate and fill the index buffer.
                gl11.glGenBuffers(1, buffer, 0);
                mIndexBufferIndex = buffer[0];
                gl11.glBindBuffer(GL11.GL_ELEMENT_ARRAY_BUFFER, 
                        mIndexBufferIndex);
                // A char is 2 bytes.
                final int indexSize = mIndexBuffer.capacity() * 2;

                mIndexBuffer.position(0);
                gl11.glBufferData(GL11.GL_ELEMENT_ARRAY_BUFFER, indexSize, mIndexBuffer, 
                        GL11.GL_STATIC_DRAW);
                
                // Unbind the element array buffer.
                gl11.glBindBuffer(GL11.GL_ELEMENT_ARRAY_BUFFER, 0);
                
                mUseHardwareBuffers = true;
                
                assert mVertBufferIndex != 0;
                assert mTextureCoordBufferIndex != 0;
                assert mIndexBufferIndex != 0;
                assert gl11.glGetError() == 0;
                
            
            }
        }
    }

}
