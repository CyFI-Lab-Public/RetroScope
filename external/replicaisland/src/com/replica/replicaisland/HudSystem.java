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
 * A very simple manager for orthographic in-game UI elements.
 * TODO: This should probably manage a number of hud objects in keeping with the component-centric
 * architecture of this engine.  The current code is monolithic and should be refactored.
 */
public class HudSystem extends BaseObject {
    private static final int FUEL_BAR_EDGE_PADDING = 15;
    private static final float FUEL_DECREASE_BAR_SPEED = 0.75f;
    private static final float FUEL_INCREASE_BAR_SPEED = 2.0f;
    private static final float FLY_BUTTON_X = -12.0f;
    private static final float FLY_BUTTON_Y = -5.0f;
    private static final float STOMP_BUTTON_X = 85.0f;
    private static final float STOMP_BUTTON_Y = -10.0f;
    private static final float STOMP_BUTTON_SCALE = 0.65f;
    private static final int COLLECTABLE_EDGE_PADDING = 8;
    private static final int MAX_DIGITS = 4;
    private static final float MOVEMENT_SLIDER_BASE_X = 20.0f;
    private static final float MOVEMENT_SLIDER_BASE_Y = 32.0f;
    private static final float MOVEMENT_SLIDER_BUTTON_X = MOVEMENT_SLIDER_BASE_X + 32.0f;
    private static final float MOVEMENT_SLIDER_BUTTON_Y =  MOVEMENT_SLIDER_BASE_Y - 16.0f;
    private static final float FLY_BUTTON_WIDTH = 128;
    private static final float STOMP_BUTTON_WIDTH = FLY_BUTTON_WIDTH * STOMP_BUTTON_SCALE;
    private static final float MOVEMENT_SLIDER_WIDTH = 128;

    private DrawableBitmap mFuelDrawable;
    private DrawableBitmap mFuelBackgroundDrawable;
    private float mFuelPercent;
    private float mFuelTargetPercent;
    
    private Texture mFadeTexture;
    private float mFadeStartTime;
    private float mFadeDuration;
    private boolean mFadeIn;
    private boolean mFading;
    private int mFadePendingEventType;
	private int mFadePendingEventIndex;
    
    private DrawableBitmap mFlyButtonEnabledDrawable;
    private DrawableBitmap mFlyButtonDisabledDrawable;
    private DrawableBitmap mFlyButtonDepressedDrawable;
    
    private DrawableBitmap mStompButtonEnabledDrawable;
    private DrawableBitmap mStompButtonDepressedDrawable;
    
    private DrawableBitmap mMovementSliderBaseDrawable;
    private DrawableBitmap mMovementSliderButtonDrawable;
    private DrawableBitmap mMovementSliderButtonDepressedDrawable;

    
    private Vector2 mFlyButtonLocation;
    private boolean mFlyButtonActive;
    private boolean mFlyButtonPressed;
    
    private Vector2 mStompButtonLocation;
    private boolean mStompButtonPressed;
    
    private Vector2 mMovementSliderBaseLocation;
    private Vector2 mMovementSliderButtonLocation;
    private boolean mMovementSliderMode;
    private boolean mMovementSliderButtonPressed;
    
    private DrawableBitmap mRubyDrawable;
    private DrawableBitmap mCoinDrawable;
    
    private int mCoinCount;
    private int mRubyCount;
    private Vector2 mCoinLocation;
    private Vector2 mRubyLocation;
    private int[] mCoinDigits;
    private int[] mRubyDigits;
    private boolean mCoinDigitsChanged;
    private boolean mRubyDigitsChanged;
    
    private int mFPS;
    private Vector2 mFPSLocation;
    private int[] mFPSDigits;
    private boolean mFPSDigitsChanged;
    private boolean mShowFPS;
    
    private DrawableBitmap[] mDigitDrawables;
    private DrawableBitmap mXDrawable;
	
    
    public HudSystem() {
        super();
        mFlyButtonLocation = new Vector2();
        mStompButtonLocation = new Vector2();
        mCoinLocation = new Vector2();
        mRubyLocation = new Vector2();
        mFPSLocation = new Vector2();
        mDigitDrawables = new DrawableBitmap[10];
        mCoinDigits = new int[MAX_DIGITS];
        mRubyDigits = new int[MAX_DIGITS];
        mFPSDigits = new int[MAX_DIGITS];
        mMovementSliderBaseLocation = new Vector2();
        mMovementSliderButtonLocation = new Vector2();
        
        reset();
    }
    
    @Override
    public void reset() {
        mFuelDrawable = null;
        mFadeTexture = null;
        mFuelPercent = 1.0f;
        mFuelTargetPercent = 1.0f;
        mFading = false;
        mFlyButtonDisabledDrawable = null;
        mFlyButtonEnabledDrawable = null;
        mFlyButtonDepressedDrawable = null;
        mFlyButtonLocation.set(FLY_BUTTON_X, FLY_BUTTON_Y);
        mFlyButtonActive = true;
        mFlyButtonPressed = false;
        mStompButtonEnabledDrawable = null;
        mStompButtonDepressedDrawable = null;
        mStompButtonLocation.set(STOMP_BUTTON_X, STOMP_BUTTON_Y);
        mStompButtonPressed = false;
        mCoinCount = 0;
        mRubyCount = 0;
        mCoinDigits[0] = 0;
        mCoinDigits[1] = -1;
        mRubyDigits[0] = 0;
        mRubyDigits[1] = -1;
        mCoinDigitsChanged = true;
        mRubyDigitsChanged = true;
        mFPS = 0;
        mFPSDigits[0] = 0;
        mFPSDigits[1] = -1;
        mFPSDigitsChanged = true;
        mShowFPS = false;
        for (int x = 0; x < mDigitDrawables.length; x++) {
            mDigitDrawables[x] = null;
        }
        mXDrawable = null;
        mFadePendingEventType = GameFlowEvent.EVENT_INVALID;
        mFadePendingEventIndex = 0;
        
        mMovementSliderBaseDrawable = null;
        mMovementSliderButtonDrawable = null;
        mMovementSliderButtonDepressedDrawable = null;
        mMovementSliderBaseLocation.set(MOVEMENT_SLIDER_BASE_X, MOVEMENT_SLIDER_BASE_Y);
        mMovementSliderButtonLocation.set(MOVEMENT_SLIDER_BUTTON_X, MOVEMENT_SLIDER_BUTTON_Y);
        mMovementSliderMode = false;
        mMovementSliderButtonPressed = false;
    }
    
    public void setFuelPercent(float percent) {
        mFuelTargetPercent = percent;
    }
    
    public void setFuelDrawable(DrawableBitmap fuel, DrawableBitmap background) {
        mFuelDrawable = fuel;
        mFuelBackgroundDrawable = background;
    }
    
    public void setFadeTexture(Texture texture) {
        mFadeTexture = texture;
    }
    
    public void setButtonDrawables(DrawableBitmap disabled, DrawableBitmap enabled, DrawableBitmap depressed,
    		DrawableBitmap stompEnabled, DrawableBitmap stompDepressed,
    		DrawableBitmap sliderBase, DrawableBitmap sliderButton, DrawableBitmap sliderDepressed) {
        mFlyButtonDisabledDrawable = disabled;
        mFlyButtonEnabledDrawable = enabled;
        mFlyButtonDepressedDrawable = depressed;
        mStompButtonEnabledDrawable = stompEnabled;
        mStompButtonDepressedDrawable = stompDepressed;
        mMovementSliderBaseDrawable = sliderBase;
        mMovementSliderButtonDrawable = sliderButton;
        mMovementSliderButtonDepressedDrawable = sliderDepressed;
    }
    
    public void setDigitDrawables(DrawableBitmap[] digits, DrawableBitmap xMark) {
        mXDrawable = xMark;
        for (int x = 0; x < mDigitDrawables.length && x < digits.length; x++) {
            mDigitDrawables[x] = digits[x];
        }
    }
    
    public void setCollectableDrawables(DrawableBitmap coin, DrawableBitmap ruby) {
        mCoinDrawable = coin;
        mRubyDrawable = ruby;
    }
    
    public void setButtonState(boolean pressed, boolean attackPressed, boolean sliderPressed) {
        mFlyButtonPressed = pressed;
        mStompButtonPressed = attackPressed;
        mMovementSliderButtonPressed = sliderPressed;
    }
    
    public void startFade(boolean in, float duration) {
        mFadeStartTime = sSystemRegistry.timeSystem.getRealTime();
        mFadeDuration = duration;
        mFadeIn = in;
        mFading = true;
    }
    
    public void clearFade() {
        mFading = false;
    }
    
    public boolean isFading() {
        return mFading;
    }
    
    public void updateInventory(InventoryComponent.UpdateRecord newInventory) {
    	mCoinDigitsChanged = (mCoinCount != newInventory.coinCount);
    	mRubyDigitsChanged = (mRubyCount != newInventory.rubyCount);

        mCoinCount = newInventory.coinCount;
        mRubyCount = newInventory.rubyCount;
    }
    
    public void setFPS(int fps) {
    	mFPSDigitsChanged = (fps != mFPS);
    	mFPS = fps;
    }
    
    public void setShowFPS(boolean show) {
    	mShowFPS = show;
    }
    
    public void setMovementSliderMode(boolean sliderOn) {
    	mMovementSliderMode = sliderOn;
    	if (sliderOn) {
    		ContextParameters params = sSystemRegistry.contextParameters;
    		mFlyButtonLocation.set(params.gameWidth - FLY_BUTTON_WIDTH - FLY_BUTTON_X, FLY_BUTTON_Y);
    		mStompButtonLocation.set(params.gameWidth - STOMP_BUTTON_WIDTH - STOMP_BUTTON_X, STOMP_BUTTON_Y);
    	} else {
    		mFlyButtonLocation.set(FLY_BUTTON_X, FLY_BUTTON_Y);
    		mStompButtonLocation.set(STOMP_BUTTON_X, STOMP_BUTTON_Y);
    	}
    }
    public void setMovementSliderOffset(float offset) {
        mMovementSliderButtonLocation.set(MOVEMENT_SLIDER_BUTTON_X + (offset * (MOVEMENT_SLIDER_WIDTH / 2.0f)), MOVEMENT_SLIDER_BUTTON_Y);
    }

    @Override
    public void update(float timeDelta, BaseObject parent) {
        final RenderSystem render = sSystemRegistry.renderSystem;
        final VectorPool pool = sSystemRegistry.vectorPool;
        final ContextParameters params = sSystemRegistry.contextParameters;
        final DrawableFactory factory = sSystemRegistry.drawableFactory;

        final GameObjectManager manager = sSystemRegistry.gameObjectManager;
        
        if (manager != null && manager.getPlayer() != null) {
        	// Only draw player-specific HUD elements when there's a player.
	        if (mFuelDrawable != null && mFuelBackgroundDrawable != null 
	                && render != null && pool != null && factory != null && params != null) {
	            if (mFuelPercent < mFuelTargetPercent) {
	                mFuelPercent += (FUEL_INCREASE_BAR_SPEED * timeDelta);
	                if (mFuelPercent > mFuelTargetPercent) {
	                    mFuelPercent = mFuelTargetPercent;
	                }
	            } else if (mFuelPercent > mFuelTargetPercent) {
	                mFuelPercent -= (FUEL_DECREASE_BAR_SPEED * timeDelta);
	                if (mFuelPercent < mFuelTargetPercent) {
	                    mFuelPercent = mFuelTargetPercent;
	                }
	            }
	            
	            if (mFuelBackgroundDrawable.getWidth() == 0) {
	                // first time init
	                Texture tex = mFuelDrawable.getTexture();
	                mFuelDrawable.resize(tex.width, tex.height);
	                Texture backgroundTex = mFuelBackgroundDrawable.getTexture();
	                mFuelBackgroundDrawable.resize(backgroundTex.width, backgroundTex.height);
	            }
	            
	            final int height = mFuelDrawable.getHeight();
	            
	        
	            Vector2 location = pool.allocate();
	            location.set(FUEL_BAR_EDGE_PADDING, 
	                    params.gameHeight - height - FUEL_BAR_EDGE_PADDING);
	            render.scheduleForDraw(mFuelBackgroundDrawable, location, SortConstants.HUD, false);
	            location.x += 2;
	            location.y += 2;
	            final int barWidth = (int)((100 - 4) * mFuelPercent);
	            if (barWidth >= 1) {
		            DrawableBitmap bitmap = factory.allocateDrawableBitmap();
		            if (bitmap != null) {
		                bitmap.resize(barWidth, mFuelDrawable.getHeight());
		                bitmap.setTexture(mFuelDrawable.getTexture());
		                render.scheduleForDraw(bitmap, location, SortConstants.HUD + 1, false);
		            }
	            }
	          
	            pool.release(location);
	        }
	        
	        if (mFlyButtonDisabledDrawable != null && mFlyButtonEnabledDrawable != null 
	                && mFlyButtonDepressedDrawable != null) {
	            
	            DrawableBitmap bitmap = mFlyButtonEnabledDrawable;
	            if (mFlyButtonActive && mFlyButtonPressed) {
	                bitmap = mFlyButtonDepressedDrawable;
	            } else if (!mFlyButtonActive) {
	                bitmap = mFlyButtonDisabledDrawable; 
	            }
	            
	            if (bitmap.getWidth() == 0) {
	                // first time init
	                Texture tex = bitmap.getTexture();
	                bitmap.resize(tex.width, tex.height);
	            }
	            
	            render.scheduleForDraw(bitmap, mFlyButtonLocation, SortConstants.HUD, false);   
	        }
	        
	        
	        
	        if (mStompButtonEnabledDrawable != null && mStompButtonDepressedDrawable != null) {
	            
	            DrawableBitmap bitmap = mStompButtonEnabledDrawable;
	            if (mStompButtonPressed) {
	                bitmap = mStompButtonDepressedDrawable;
	            } 
	            
	            if (bitmap.getWidth() == 0) {
	                // first time init
	                Texture tex = bitmap.getTexture();
	                bitmap.resize(tex.width, tex.height);
	                bitmap.setWidth((int)(tex.width * STOMP_BUTTON_SCALE));
	                bitmap.setHeight((int)(tex.height * STOMP_BUTTON_SCALE));
	            }
	            
	            render.scheduleForDraw(bitmap, mStompButtonLocation, SortConstants.HUD, false);   
	        }
	        
	        if (mMovementSliderMode && 
	        		mMovementSliderBaseDrawable != null && mMovementSliderButtonDrawable != null) {
	               
	            if (mMovementSliderBaseDrawable.getWidth() == 0) {
	                // first time init
	                Texture tex = mMovementSliderBaseDrawable.getTexture();
	                mMovementSliderBaseDrawable.resize(tex.width, tex.height);
	            }
	            
	            if (mMovementSliderButtonDrawable.getWidth() == 0) {
	                // first time init
	                Texture tex = mMovementSliderButtonDrawable.getTexture();
	                mMovementSliderButtonDrawable.resize(tex.width, tex.height);
	            }
	            
	            if (mMovementSliderButtonDepressedDrawable.getWidth() == 0) {
	                // first time init
	                Texture tex = mMovementSliderButtonDepressedDrawable.getTexture();
	                mMovementSliderButtonDepressedDrawable.resize(tex.width, tex.height);
	            }
	            
	            DrawableBitmap bitmap = mMovementSliderButtonDrawable;

	            if (mMovementSliderButtonPressed) {
	            	bitmap = mMovementSliderButtonDepressedDrawable;
	            }
	            
	            render.scheduleForDraw(mMovementSliderBaseDrawable, mMovementSliderBaseLocation, SortConstants.HUD, false);   
	            render.scheduleForDraw(bitmap, mMovementSliderButtonLocation, SortConstants.HUD + 1, false);   

	        }
	        
	        
	        if (mCoinDrawable != null) {
	            if (mCoinDrawable.getWidth() == 0) {
	                // first time init
	                Texture tex = mCoinDrawable.getTexture();
	                mCoinDrawable.resize(tex.width, tex.height);
	                mCoinLocation.x = (params.gameWidth / 2.0f) - tex.width / 2.0f;
	                mCoinLocation.y = params.gameHeight - tex.height - COLLECTABLE_EDGE_PADDING;
	            }
	            
	            render.scheduleForDraw(mCoinDrawable, mCoinLocation, SortConstants.HUD, false);
	            if (mCoinDigitsChanged) {
	            	intToDigitArray(mCoinCount, mCoinDigits);
	            	mCoinDigitsChanged = false;
	            }
	            final float offset = mCoinDrawable.getWidth() * 0.75f;
	            mCoinLocation.x += offset;
	            drawNumber(mCoinLocation, mCoinDigits, true);
	            mCoinLocation.x -= offset;
	        }
	        
	        if (mRubyDrawable != null) {
	            if (mRubyDrawable.getWidth() == 0) {
	                // first time init
	                Texture tex = mRubyDrawable.getTexture();
	                mRubyDrawable.resize(tex.width, tex.height);
	                mRubyLocation.x = (params.gameWidth / 2.0f) + 100.0f;
	                mRubyLocation.y = params.gameHeight - tex.height - COLLECTABLE_EDGE_PADDING;
	            }
	            render.scheduleForDraw(mRubyDrawable, mRubyLocation, SortConstants.HUD, false);
	            if (mRubyDigitsChanged) {
	            	intToDigitArray(mRubyCount, mRubyDigits);
	            	mRubyDigitsChanged = false;
	            }
	            final float offset = mRubyDrawable.getWidth() * 0.75f;
	            mRubyLocation.x += offset;
	            drawNumber(mRubyLocation, mRubyDigits, true);    
	            mRubyLocation.x -= offset;
	        }
        }
        
        if (mShowFPS) {
        	if (mFPSDigitsChanged) {
            	int count = intToDigitArray(mFPS, mFPSDigits);
            	mFPSDigitsChanged = false;
                mFPSLocation.set(params.gameWidth - 10.0f - ((count + 1) * (mDigitDrawables[0].getWidth() / 2.0f)), 10.0f);

            }
            drawNumber(mFPSLocation, mFPSDigits, false);
        }
        
        if (mFading && factory != null) {
            
            final float time = sSystemRegistry.timeSystem.getRealTime();
            final float fadeDelta = (time - mFadeStartTime);
            
            float percentComplete = 1.0f;
            if (fadeDelta < mFadeDuration) {
                percentComplete = fadeDelta / mFadeDuration;
            } else if (mFadeIn) {
                // We've faded in.  Turn fading off.
                mFading = false;
            } 
            
            if (percentComplete < 1.0f || !mFadeIn) {
                float opacityValue = percentComplete;
                if (mFadeIn) {
                    opacityValue = 1.0f - percentComplete;
                }
                
                DrawableBitmap bitmap = factory.allocateDrawableBitmap();
                if (bitmap != null) {
                    bitmap.setWidth(params.gameWidth);
                    bitmap.setHeight(params.gameHeight);
                    bitmap.setTexture(mFadeTexture);
                    bitmap.setCrop(0, mFadeTexture.height, mFadeTexture.width, mFadeTexture.height);
                    bitmap.setOpacity(opacityValue);
                    render.scheduleForDraw(bitmap, Vector2.ZERO, SortConstants.FADE, false);
                }
            }
            
            if (percentComplete >= 1.0f && mFadePendingEventType != GameFlowEvent.EVENT_INVALID) {
            	LevelSystem level = sSystemRegistry.levelSystem;
            	if (level != null) {
            		level.sendGameEvent(mFadePendingEventType, mFadePendingEventIndex, false);
            		mFadePendingEventType = GameFlowEvent.EVENT_INVALID;
            		mFadePendingEventIndex = 0;
            	}
            }
        }
    }
    
    private void drawNumber(Vector2 location, int[] digits, boolean drawX) {
        final RenderSystem render = sSystemRegistry.renderSystem;
        
        if (mDigitDrawables[0].getWidth() == 0) {
            // first time init
            for (int x = 0; x < mDigitDrawables.length; x++) {
                Texture tex = mDigitDrawables[x].getTexture();
                mDigitDrawables[x].resize(tex.width, tex.height);
            }
        }
        
        if (mXDrawable.getWidth() == 0) {
            // first time init
            Texture tex = mXDrawable.getTexture();
            mXDrawable.resize(tex.width, tex.height);
        }
        
        final float characterWidth = mDigitDrawables[0].getWidth() / 2.0f;
        float offset = 0.0f;
        
        if (mXDrawable != null && drawX) {
            render.scheduleForDraw(mXDrawable, location, SortConstants.HUD, false); 
            location.x += characterWidth;
            offset += characterWidth;
         }
        
        for (int x = 0; x < digits.length && digits[x] != -1; x++) {
            int index = digits[x];
            DrawableBitmap digit = mDigitDrawables[index];
            if (digit != null) {
                render.scheduleForDraw(digit, location, SortConstants.HUD, false);
                location.x += characterWidth;
                offset += characterWidth;
            }
        }
        
        location.x -= offset;
        
        
    }

    public int intToDigitArray(int value, int[] digits) {
    	int characterCount = 1;
        if (value >= 1000) {
            characterCount = 4;
        } else if (value >= 100) {
            characterCount = 3;
        } else if (value >= 10) {
            characterCount = 2;
        }
        
    	int remainingValue = value;
        int count = 0;
	    do {
	        int index = remainingValue != 0 ? remainingValue % 10 : 0;
	        remainingValue /= 10;
	        digits[characterCount - 1 - count] = index;
	        count++;
	    } while (remainingValue > 0 && count < digits.length);
	    
	    if (count < digits.length) {
	    	digits[count] = -1;
	    }
	    return characterCount;
    }
    
	public void sendGameEventOnFadeComplete(int eventType, int eventIndex) {
		mFadePendingEventType = eventType;
		mFadePendingEventIndex = eventIndex;
	}
    
    
}
