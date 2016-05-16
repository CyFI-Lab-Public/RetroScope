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

public class LevelBuilder extends BaseObject {
    private final static int THEME_GRASS = 0;
    private final static int THEME_ISLAND = 1;
    private final static int THEME_SEWER = 2;
    private final static int THEME_UNDERGROUND = 3;
    private final static int THEME_LAB = 4;
    private final static int THEME_LIGHTING = 5;
    private final static int THEME_TUTORIAL = 6;

    
    private final static int BACKGROUND_SUNSET = 0;
    private final static int BACKGROUND_ISLAND = 1;
    private final static int BACKGROUND_SEWER = 2;
    private final static int BACKGROUND_UNDERGROUND = 3;
    private final static int BACKGROUND_FOREST = 4;
    private final static int BACKGROUND_ISLAND2 = 5;
    private final static int BACKGROUND_LAB = 6;
    
    public LevelBuilder() {
        super();
    }
    
    @Override
    public void reset() {
    }
    
    
    public GameObject buildBackground(int backgroundImage, int levelWidth, int levelHeight) {
        // Generate the scrolling background.
        TextureLibrary textureLibrary = sSystemRegistry.shortTermTextureLibrary;
        
        GameObject background = new GameObject();

        if (textureLibrary != null) {
            
            int backgroundResource = -1;
            
            switch (backgroundImage) {
                case BACKGROUND_SUNSET:
                    backgroundResource = R.drawable.background_sunset;
                    break;
                case BACKGROUND_ISLAND:
                    backgroundResource = R.drawable.background_island;
                    break;
                case BACKGROUND_SEWER:
                    backgroundResource = R.drawable.background_sewage;
                    break;
                case BACKGROUND_UNDERGROUND:
                    backgroundResource = R.drawable.background_underground;
                    break;
                case BACKGROUND_FOREST:
                    backgroundResource = R.drawable.background_grass2;
                    break;
                case BACKGROUND_ISLAND2:
                    backgroundResource = R.drawable.background_island2;
                    break;
                case BACKGROUND_LAB:
                    backgroundResource = R.drawable.background_lab01;
                    break;
                default:
                    assert false;
            }
            
            if (backgroundResource > -1) {
                
                // Background Layer //
                RenderComponent backgroundRender = new RenderComponent();
                backgroundRender.setPriority(SortConstants.BACKGROUND_START);
                
                ContextParameters params = sSystemRegistry.contextParameters;
                // The background image is ideally 1.5 times the size of the largest screen axis
                // (normally the width, but just in case, let's calculate it).
                final int idealSize = (int)Math.max(params.gameWidth * 1.5f, params.gameHeight * 1.5f);
                int width = idealSize;
                int height = idealSize;
                
                ScrollerComponent scroller3 = 
                        new ScrollerComponent(0.0f, 0.0f, width, height, 
                            textureLibrary.allocateTexture(backgroundResource));
                scroller3.setRenderComponent(backgroundRender);
                
                // Scroll speeds such that the background will evenly match the beginning
                // and end of the level.  Don't allow speeds > 1.0, though; that would be faster than
                // the foreground, which is disorienting and looks like rotation.
                final float scrollSpeedX = Math.min((float)(width - params.gameWidth) / (levelWidth - params.gameWidth), 1.0f);
                final float scrollSpeedY = Math.min((float)(height - params.gameHeight) / (levelHeight - params.gameHeight), 1.0f);
                
                 
                scroller3.setScrollSpeed(scrollSpeedX, scrollSpeedY);
                
                backgroundRender.setCameraRelative(false);
                
                background.add(scroller3);
                background.add(backgroundRender);
            }
        }
        return background;
    }
    
    public void addTileMapLayer(GameObject background, int priority, float scrollSpeed, 
            int width, int height, int tileWidth, int tileHeight, TiledWorld world, 
            int theme) {
        
        int tileMapIndex = 0;
        switch(theme) {
            case THEME_GRASS:
                tileMapIndex = R.drawable.grass;
                break;
            case THEME_ISLAND:
                tileMapIndex = R.drawable.island;
                break;
            case THEME_SEWER:
                tileMapIndex = R.drawable.sewage;
                break;
            case THEME_UNDERGROUND:
                tileMapIndex = R.drawable.cave;
                break;
            case THEME_LAB:
                tileMapIndex = R.drawable.lab;
                break;
            case THEME_LIGHTING:
                tileMapIndex = R.drawable.titletileset;
                priority = SortConstants.OVERLAY; //hack!
                break;
            case THEME_TUTORIAL:
                tileMapIndex = R.drawable.tutorial;
                break;
            default:
                assert false;
        }
        
        RenderComponent backgroundRender = new RenderComponent();
        backgroundRender.setPriority(priority);
        
        //Vertex Buffer Code
        TextureLibrary textureLibrary = sSystemRegistry.shortTermTextureLibrary;
        TiledVertexGrid bg = new TiledVertexGrid(textureLibrary.allocateTexture(tileMapIndex), 
                width, height, tileWidth, tileHeight);
        bg.setWorld(world);
        
        //TODO: The map format should really just output independent speeds for x and y,
        // but as a short term solution we can assume parallax layers lock in the smaller
        // direction of movement.
        float xScrollSpeed = 1.0f;
        float yScrollSpeed = 1.0f;
        
        if (world.getWidth() > world.getHeight()) {
        	xScrollSpeed = scrollSpeed;
        } else {
        	yScrollSpeed = scrollSpeed;
        }
        
        ScrollerComponent scroller = new ScrollerComponent(xScrollSpeed, yScrollSpeed,
                width, height, bg);
        scroller.setRenderComponent(backgroundRender);

        background.add(scroller);
        background.add(backgroundRender);
        backgroundRender.setCameraRelative(false);
    }

    // This method is a HACK to workaround the stupid map file format.
    // We want the foreground layer to be render priority FOREGROUND, but
    // we don't know which is the foreground layer until we've added them all.
    // So now that we've added them all, find foreground layer and make sure
    // its render priority is set.
	public void promoteForegroundLayer(GameObject backgroundObject) {
		backgroundObject.commitUpdates();	// Make sure layers are sorted.
		final int componentCount = backgroundObject.getCount();
		for (int x = componentCount - 1; x >= 0; x--) {
			GameComponent component = (GameComponent)backgroundObject.get(x);
			if (component instanceof RenderComponent) {
				RenderComponent render = (RenderComponent)component;
				if (render.getPriority() != SortConstants.OVERLAY) {
					// found it.
					render.setPriority(SortConstants.FOREGROUND);
					break;
				}
			}
		}
	}
    
}
