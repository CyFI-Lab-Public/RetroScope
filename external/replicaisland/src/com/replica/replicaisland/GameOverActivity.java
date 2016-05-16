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
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class GameOverActivity extends Activity {
	private float mPearlPercent = 100.0f;
	private float mEnemiesDestroyedPercent = 100.0f;
	private float mPlayTime = 0.0f;
	private int mEnding = AnimationPlayerActivity.KABOCHA_ENDING;
	
	private IncrementingTextView mPearlView;
	private IncrementingTextView mEnemiesDestroyedView;
	private IncrementingTextView mPlayTimeView;
	private TextView mEndingView;
	
    public static class IncrementingTextView extends TextView {
    	private static final int INCREMENT_DELAY_MS = 2 * 1000;
    	private static final int MODE_NONE = 0;
    	private static final int MODE_PERCENT = 1;
    	private static final int MODE_TIME = 2;
    	
    	private float mTargetValue;
    	private float mIncrement = 1.0f;
    	private float mCurrentValue = 0.0f;
    	private long mLastTime = 0;
    	private int mMode = MODE_NONE;
    	
    	public IncrementingTextView(Context context) {
            super(context);
        }
        
        public IncrementingTextView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }
        
        public IncrementingTextView(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
        }
        
        public void setTargetValue(float target) {
        	mTargetValue = target;
        	postInvalidate();
        }
        
        public void setMode(int mode) {
        	mMode = mode;
        }
        
        public void setIncrement(float increment) {
        	mIncrement = increment;
        }
        
        @Override
        public void onDraw(Canvas canvas) {
            final long time = SystemClock.uptimeMillis();
            final long delta = time - mLastTime;
            if (delta > INCREMENT_DELAY_MS) {
            	if (mCurrentValue < mTargetValue) {
            		mCurrentValue += mIncrement;
            		mCurrentValue = Math.min(mCurrentValue, mTargetValue);
            		String value;
            		if (mMode == MODE_PERCENT) {
            			value = mCurrentValue + "%";
            		} else if (mMode == MODE_TIME) {
            			float seconds = mCurrentValue;
            			float minutes = seconds / 60.0f;
            			float hours = minutes / 60.0f;
            			
            			int totalHours = (int)Math.floor(hours);
            			float totalHourMinutes = totalHours * 60.0f;
            			int totalMinutes = (int)(minutes - totalHourMinutes);
            			float totalMinuteSeconds = totalMinutes * 60.0f;
            			float totalHourSeconds = totalHourMinutes * 60.0f;
            			int totalSeconds = (int)(seconds - (totalMinuteSeconds + totalHourSeconds));
            			
            			value = totalHours + ":" + totalMinutes + ":" + totalSeconds;
            		} else {
            			value = mCurrentValue + "";
            		}
            		setText(value);
                    postInvalidateDelayed(INCREMENT_DELAY_MS);
            	}
            }
            super.onDraw(canvas);
        }
    }
    
    private View.OnClickListener sOKClickListener = new View.OnClickListener() {
        public void onClick(View v) {
        	finish();
        	if (UIConstants.mOverridePendingTransition != null) {
   		       try {
   		    	  UIConstants.mOverridePendingTransition.invoke(GameOverActivity.this, R.anim.activity_fade_in, R.anim.activity_fade_out);
   		       } catch (InvocationTargetException ite) {
   		           DebugLog.d("Activity Transition", "Invocation Target Exception");
   		       } catch (IllegalAccessException ie) {
   		    	   DebugLog.d("Activity Transition", "Illegal Access Exception");
   		       }
         	}
        }
    };
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game_over);
        
        mPearlView = (IncrementingTextView)findViewById(R.id.pearl_percent);
        mEnemiesDestroyedView = (IncrementingTextView)findViewById(R.id.enemy_percent);
        mPlayTimeView = (IncrementingTextView)findViewById(R.id.total_play_time);
        mEndingView = (TextView)findViewById(R.id.ending);
        
        SharedPreferences prefs = getSharedPreferences(PreferenceConstants.PREFERENCE_NAME, MODE_PRIVATE);
        final float playTime = prefs.getFloat(PreferenceConstants.PREFERENCE_TOTAL_GAME_TIME, 0.0f);
        final int ending = prefs.getInt(PreferenceConstants.PREFERENCE_LAST_ENDING, -1);
        final int pearlsCollected = prefs.getInt(PreferenceConstants.PREFERENCE_PEARLS_COLLECTED, 0);
        final int pearlsTotal = prefs.getInt(PreferenceConstants.PREFERENCE_PEARLS_TOTAL, 0);
     
        final int enemies = prefs.getInt(PreferenceConstants.PREFERENCE_ROBOTS_DESTROYED, 0);
        
        if (pearlsCollected > 0 && pearlsTotal > 0) {
        	mPearlView.setTargetValue((int)((pearlsCollected / (float)pearlsTotal) * 100.0f));
        } else {
        	mPearlView.setText("--");
        }
        mPearlView.setMode(IncrementingTextView.MODE_PERCENT);
        mEnemiesDestroyedView.setTargetValue(enemies);
        mPlayTimeView.setTargetValue(playTime);
        mPlayTimeView.setIncrement(90.0f);
        mPlayTimeView.setMode(IncrementingTextView.MODE_TIME);
        
        if (ending == AnimationPlayerActivity.KABOCHA_ENDING) {
        	mEndingView.setText(R.string.game_results_kabocha_ending);
        } else if (ending == AnimationPlayerActivity.ROKUDOU_ENDING) {
        	mEndingView.setText(R.string.game_results_rokudou_ending);
        } else {
        	mEndingView.setText(R.string.game_results_wanda_ending);
        }
        
        Button okButton = (Button)findViewById(R.id.ok);
        okButton.setOnClickListener(sOKClickListener);

	}
	
	
}
