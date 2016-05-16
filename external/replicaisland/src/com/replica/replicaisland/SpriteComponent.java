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
 * Provides an interface for controlling a sprite with animations.  Manages a list of animations
 * and provides a drawable surface with the correct animation frame to a render component each
 * frame.  Also manages horizontal and vertical flipping.
 */
public class SpriteComponent extends GameComponent {
    
    private PhasedObjectManager mAnimations;
    private float mAnimationTime;
    private int mCurrentAnimationIndex;
    private int mWidth;
    private int mHeight;
    private float mOpacity;
    private RenderComponent mRenderComponent;
    private DynamicCollisionComponent mCollisionComponent;
    private boolean mVisible;
    private SpriteAnimation mCurrentAnimation;
    private boolean mAnimationsDirty;
    
    public SpriteComponent(int width, int height) {
        super();
        mAnimations = new PhasedObjectManager();
        
        reset();
        mWidth = width;
        mHeight = height;
        setPhase(ComponentPhases.PRE_DRAW.ordinal());
    }
    
    public SpriteComponent() {
        super();
        mAnimations = new PhasedObjectManager();

        reset();
        
        setPhase(ComponentPhases.PRE_DRAW.ordinal());
    }
    
    @Override
    public void reset() {
        mWidth = 0;
        mHeight = 0;
        mVisible = true;
        mCurrentAnimationIndex = -1;
        mAnimations.removeAll();
        mAnimations.commitUpdates();
        mAnimationTime = 0.0f;
        mRenderComponent = null;
        mCollisionComponent = null;
        mCurrentAnimation = null;
        mOpacity = 1.0f;
        mAnimationsDirty = false;
    }

    @Override
    public void update(float timeDelta, BaseObject parent) {
        mAnimationTime += timeDelta;
        final PhasedObjectManager animations = mAnimations;
        final int currentAnimIndex = mCurrentAnimationIndex;
        
        if (mAnimationsDirty) {
        	animations.commitUpdates();
        	mAnimationsDirty = false;
        }
        boolean validFrameAvailable = false;
        if (animations.getCount() > 0 && currentAnimIndex != -1) {
        	SpriteAnimation currentAnimation = mCurrentAnimation;
        	
            if (currentAnimation == null && currentAnimIndex != -1) {
            	currentAnimation = findAnimation(currentAnimIndex);
                if (currentAnimation == null) {
                    // We were asked to play an animation that doesn't exist.  Revert to our
                    // default animation. 
                    // TODO: throw an assert here?
                    mCurrentAnimation = (SpriteAnimation)animations.get(0);
                    currentAnimation = mCurrentAnimation;
                } else {
                	mCurrentAnimation = currentAnimation;
                }
            }
            
            GameObject parentObject = (GameObject)parent;
            AnimationFrame currentFrame = currentAnimation.getFrame(mAnimationTime);
            if (currentFrame != null) {
                validFrameAvailable = true;
                final RenderComponent render = mRenderComponent;
                if (render != null) {
                    final DrawableFactory factory = sSystemRegistry.drawableFactory;
                    if (mVisible && currentFrame.texture != null && factory != null) {
                        // Fire and forget.  Allocate a new bitmap for this animation frame, set it up, and
                        // pass it off to the render component for drawing.
                        DrawableBitmap bitmap = factory.allocateDrawableBitmap();
                        bitmap.setWidth(mWidth);
                        bitmap.setHeight(mHeight);
                        bitmap.setOpacity(mOpacity);
                        updateFlip(bitmap, parentObject.facingDirection.x < 0.0f, 
                                parentObject.facingDirection.y < 0.0f);
                        bitmap.setTexture(currentFrame.texture);
                        render.setDrawable(bitmap);
                    } else {
                    	render.setDrawable(null);
                    }
                }
                
                if (mCollisionComponent != null) {
                    mCollisionComponent.setCollisionVolumes(currentFrame.attackVolumes, 
                            currentFrame.vulnerabilityVolumes);
                }
            } 
        }
        
        if (!validFrameAvailable) {
            // No current frame = draw nothing!
            if (mRenderComponent != null) {
                mRenderComponent.setDrawable(null);
            }
            if (mCollisionComponent != null) {
                mCollisionComponent.setCollisionVolumes(null, null);
            }
        }
    }

    public final void playAnimation(int index) {
        if (mCurrentAnimationIndex != index) {
            mAnimationTime = 0;
            mCurrentAnimationIndex = index;
            mCurrentAnimation = null;
        }
    }
    
    public final SpriteAnimation findAnimation(int index) {
        return (SpriteAnimation)mAnimations.find(index);
    }

    public final void addAnimation(SpriteAnimation anim) {
        mAnimations.add(anim);
        mAnimationsDirty = true;
    }

    public final boolean animationFinished() {
        boolean result = false;
        if (mCurrentAnimation != null 
                && !mCurrentAnimation.getLoop() 
                && mAnimationTime > mCurrentAnimation.getLength()) {
            result = true;
        }
        return result;
    }
    
    
    public final float getWidth() {
        return mWidth;
    }
    
    public final float getHeight() {
        return mHeight;
    }
    
    public final void setSize(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    protected final void updateFlip(DrawableBitmap bitmap, boolean horzFlip, boolean vertFlip) {
        bitmap.setFlip(horzFlip, vertFlip);
    }

    public final void setRenderComponent(RenderComponent component) {
        mRenderComponent = component;
    }
    
    public final void setCollisionComponent(DynamicCollisionComponent component) {
        mCollisionComponent = component;
    }
    
    public final boolean getVisible() {
        return mVisible;
    }

    public final void setVisible(boolean visible) {
        mVisible = visible;
    }
    
    public final float getCurrentAnimationTime() {
        return mAnimationTime;
    }
    
    public final void setCurrentAnimationTime(float time) {
        mAnimationTime = time;
    }
    
    public final void setOpacity(float opacity) {
        mOpacity = opacity;
    }
    
    public final int getCurrentAnimation() {
        return mCurrentAnimationIndex;
    }

    public final int getAnimationCount() {
        return mAnimations.getConcreteCount();
    }
}
