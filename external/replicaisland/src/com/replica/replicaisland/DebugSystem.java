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

public final class DebugSystem extends BaseObject {
	public static final int COLOR_RED = 0;
	public static final int COLOR_BLUE = 1;
	public static final int COLOR_OUTLINE = 2;
	public static final int SHAPE_BOX = 0;
	public static final int SHAPE_CIRCLE = 1;
	
	private Texture mRedBoxTexture;
	private Texture mBlueBoxTexture;
	private Texture mOutlineBoxTexture;
	private Texture mRedCircleTexture;
	private Texture mBlueCircleTexture;
	private Texture mOutlineCircleTexture;
	
	private Vector2 mWorkVector;
	
	public DebugSystem(TextureLibrary library) {
		super();
		if (library != null) {
			mRedBoxTexture = library.allocateTexture(R.drawable.debug_box_red);
			mBlueBoxTexture = library.allocateTexture(R.drawable.debug_box_blue);
			mOutlineBoxTexture = library.allocateTexture(R.drawable.debug_box_outline);
			mRedCircleTexture = library.allocateTexture(R.drawable.debug_circle_red);
			mBlueCircleTexture = library.allocateTexture(R.drawable.debug_circle_blue);
			mOutlineCircleTexture = library.allocateTexture(R.drawable.debug_circle_outline);
			
		}
		
		mWorkVector = new Vector2();
	}
	
	@Override
	public void reset() {
	}
	
	public void drawShape(float x, float y, float width, float height, int shapeType, int colorType) {
        final RenderSystem render = sSystemRegistry.renderSystem;
        final DrawableFactory factory = sSystemRegistry.drawableFactory;
        CameraSystem camera = sSystemRegistry.cameraSystem;
        ContextParameters params = sSystemRegistry.contextParameters;
        mWorkVector.set(x, y);
        mWorkVector.x = (mWorkVector.x - camera.getFocusPositionX()
                        + (params.gameWidth / 2));
        mWorkVector.y = (mWorkVector.y - camera.getFocusPositionY()
                        + (params.gameHeight / 2));

        if (mWorkVector.x + width >= 0.0f && mWorkVector.x < params.gameWidth 
                && mWorkVector.y + height >= 0.0f && mWorkVector.y < params.gameHeight) {
	        DrawableBitmap bitmap = factory.allocateDrawableBitmap();
	        if (bitmap != null) {
	        	Texture texture = getTexture(shapeType, colorType);
	            bitmap.resize((int)texture.width, (int)texture.height);
	            // TODO: scale stretch hack.  fix!
	            bitmap.setWidth((int)width);
	            bitmap.setHeight((int)height);
	            bitmap.setTexture(texture);
	            mWorkVector.set(x, y);

	            render.scheduleForDraw(bitmap, mWorkVector, SortConstants.HUD, true);
	        }
        }
	}
	
	private final Texture getTexture(int shapeType, int colorType) {
		Texture result = null;
		if (shapeType == SHAPE_BOX) {
			switch (colorType) {
				case COLOR_RED:
					result = mRedBoxTexture;
					break;
				case COLOR_BLUE:
					result = mBlueBoxTexture;
					break;
				case COLOR_OUTLINE:
					result = mOutlineBoxTexture;
					break;
			}
		} else if (shapeType == SHAPE_CIRCLE) {
			switch (colorType) {
			case COLOR_RED:
				result = mRedCircleTexture;
				break;
			case COLOR_BLUE:
				result = mBlueCircleTexture;
				break;
			case COLOR_OUTLINE:
				result = mOutlineCircleTexture;
				break;
			}
		}
		return result;
	}
	

}
