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

import java.lang.reflect.InvocationTargetException;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;


public class AnimationPlayerActivity extends Activity {
	public static final int KYLE_DEATH = 0;
	public static final int WANDA_ENDING = 1;
	public static final int KABOCHA_ENDING = 2;
	public static final int ROKUDOU_ENDING = 3;
	
	private AnimationDrawable mAnimation;
	private int mAnimationType;
	private long mAnimationEndTime;
	
	private KillActivityHandler mKillActivityHandler = new KillActivityHandler();

    class KillActivityHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
        	AnimationPlayerActivity.this.finish();
        	if (UIConstants.mOverridePendingTransition != null) {
  		       try {
  		    	  UIConstants.mOverridePendingTransition.invoke(AnimationPlayerActivity.this, R.anim.activity_fade_in, R.anim.activity_fade_out);
  		       } catch (InvocationTargetException ite) {
  		           DebugLog.d("Activity Transition", "Invocation Target Exception");
  		       } catch (IllegalAccessException ie) {
  		    	   DebugLog.d("Activity Transition", "Illegal Access Exception");
  		       }
        	}
        }

        public void sleep(long delayMillis) {
        	this.removeMessages(0);
            sendMessageDelayed(obtainMessage(0), delayMillis);
        }
    };
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
	  super.onCreate(savedInstanceState);
	  	  
	  final Intent callingIntent = getIntent();
	  mAnimationType = callingIntent.getIntExtra("animation", KYLE_DEATH);
	  
	  if (mAnimationType == KYLE_DEATH) {
		  setContentView(R.layout.animation_player);
	
		  ImageView canvasImage = (ImageView) findViewById(R.id.animation_canvas);
		  canvasImage.setImageResource(R.anim.kyle_fall);
		  mAnimation = (AnimationDrawable) canvasImage.getDrawable();
	  } else {
		  
		  
		  if (mAnimationType == WANDA_ENDING || mAnimationType == KABOCHA_ENDING) {
			  float startX = 0.0f;
			  DisplayMetrics metrics = new DisplayMetrics();
			  getWindowManager().getDefaultDisplay().getMetrics(metrics);
			  if (mAnimationType == WANDA_ENDING) {
				  setContentView(R.layout.good_ending_animation);
				  startX = 200 * metrics.density;
				 
			  } else {
				  setContentView(R.layout.kabocha_ending_animation);
				  startX = -200 * metrics.density;
			  }
			  
			  // HACK
			  // the TranslateAnimation system doesn't support device independent pixels.
			  // So for the Wanda ending and Kabocha endings, in which the game over text
			  // scrolls in horizontally, compute the size based on the actual density of
			  // the display and just generate the anim in code.  The Rokudou animation
			  // can be safely loaded from a file.
			  Animation gameOverAnim = new TranslateAnimation(startX, 0, 0, 0);
			  gameOverAnim.setDuration(6000);
			  gameOverAnim.setFillAfter(true);
			  gameOverAnim.setFillEnabled(true);
			  gameOverAnim.setStartOffset(8000);
			  
			  View background = findViewById(R.id.animation_background);
			  View foreground = findViewById(R.id.animation_foreground);
			  View gameOver = findViewById(R.id.game_over);

			  Animation foregroundAnim = AnimationUtils.loadAnimation(this, R.anim.horizontal_layer2_slide);
			  Animation backgroundAnim = AnimationUtils.loadAnimation(this, R.anim.horizontal_layer1_slide);
			  
			  background.startAnimation(backgroundAnim);
			  foreground.startAnimation(foregroundAnim);
			  gameOver.startAnimation(gameOverAnim);
			  
			  mAnimationEndTime = gameOverAnim.getDuration() + System.currentTimeMillis();
		  } else if (mAnimationType == ROKUDOU_ENDING) {
			  setContentView(R.layout.rokudou_ending_animation);
			  View background = findViewById(R.id.animation_background);
			  View sphere = findViewById(R.id.animation_sphere);
			  View cliffs = findViewById(R.id.animation_cliffs);
			  View rokudou = findViewById(R.id.animation_rokudou);
			  View gameOver = findViewById(R.id.game_over);

			  
			  Animation backgroundAnim = AnimationUtils.loadAnimation(this, R.anim.rokudou_slide_bg);
			  Animation sphereAnim = AnimationUtils.loadAnimation(this, R.anim.rokudou_slide_sphere);
			  Animation cliffsAnim = AnimationUtils.loadAnimation(this, R.anim.rokudou_slide_cliffs);
			  Animation rokudouAnim = AnimationUtils.loadAnimation(this, R.anim.rokudou_slide_rokudou);
			  Animation gameOverAnim = AnimationUtils.loadAnimation(this, R.anim.rokudou_game_over);

			  background.startAnimation(backgroundAnim);
			  sphere.startAnimation(sphereAnim);
			  cliffs.startAnimation(cliffsAnim);
			  rokudou.startAnimation(rokudouAnim);
			  gameOver.startAnimation(gameOverAnim);
			  mAnimationEndTime = gameOverAnim.getDuration() + System.currentTimeMillis();
		  } else {
			  assert false;
		  }
		  
	  }
	  
	  // Pass the calling intent back so that we can figure out which animation just played.
	  setResult(RESULT_OK, callingIntent);
	  
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		long time = System.currentTimeMillis();
		if (time > mAnimationEndTime) {
			finish();
		} else {
			try {
				Thread.sleep(32);
			} catch (InterruptedException e) {
				// Safe to ignore.
			}
		}
		return true;
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
	  if (hasFocus && mAnimation != null) {
		  mAnimation.start();
		  mKillActivityHandler.sleep(mAnimation.getDuration(0) * mAnimation.getNumberOfFrames());
	  }
	}
	
}
