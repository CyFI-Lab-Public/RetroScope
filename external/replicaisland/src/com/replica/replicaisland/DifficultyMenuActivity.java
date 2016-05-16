package com.replica.replicaisland;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class DifficultyMenuActivity extends Activity {
	private View mBabyButton;
    private View mKidsButton;
    private View mAdultsButton;
    private View mBackground;
    private View mBabyText;
    private View mKidsText;
    private View mAdultsText;
    private Animation mButtonFlickerAnimation;
    private Animation mFadeOutAnimation;
    private Animation mAlternateFadeOutAnimation;
    
    private View.OnClickListener sBabyButtonListener = new View.OnClickListener() {
        public void onClick(View v) {
        
            Intent i = new Intent(getBaseContext(), AndouKun.class);
            i.putExtras(getIntent());
            i.putExtra("difficulty", 0);

            v.startAnimation(mButtonFlickerAnimation);
            mFadeOutAnimation.setAnimationListener(new StartActivityAfterAnimation(i));
            mBackground.startAnimation(mFadeOutAnimation);
            mKidsButton.startAnimation(mAlternateFadeOutAnimation);
            mAdultsButton.startAnimation(mAlternateFadeOutAnimation);
            
            mBabyText.startAnimation(mAlternateFadeOutAnimation);
            mKidsText.startAnimation(mAlternateFadeOutAnimation);
            mAdultsText.startAnimation(mAlternateFadeOutAnimation);
        }
    };
    
    private View.OnClickListener sKidsButtonListener = new View.OnClickListener() {
        public void onClick(View v) {
        
            Intent i = new Intent(getBaseContext(), AndouKun.class);
            i.putExtras(getIntent());
            i.putExtra("difficulty", 1);
            
            v.startAnimation(mButtonFlickerAnimation);
            mFadeOutAnimation.setAnimationListener(new StartActivityAfterAnimation(i));
            mBackground.startAnimation(mFadeOutAnimation);
            mBabyButton.startAnimation(mAlternateFadeOutAnimation);
            mAdultsButton.startAnimation(mAlternateFadeOutAnimation);
            
            mBabyText.startAnimation(mAlternateFadeOutAnimation);
            mKidsText.startAnimation(mAlternateFadeOutAnimation);
            mAdultsText.startAnimation(mAlternateFadeOutAnimation);
        }
    };
    
    private View.OnClickListener sAdultsButtonListener = new View.OnClickListener() {
        public void onClick(View v) {
        
            Intent i = new Intent(getBaseContext(), AndouKun.class);
            i.putExtras(getIntent());
            i.putExtra("difficulty", 2);
            
            v.startAnimation(mButtonFlickerAnimation);
            mFadeOutAnimation.setAnimationListener(new StartActivityAfterAnimation(i));
            mBackground.startAnimation(mFadeOutAnimation);
            mBabyButton.startAnimation(mAlternateFadeOutAnimation);
            mKidsButton.startAnimation(mAlternateFadeOutAnimation);
            
            mBabyText.startAnimation(mAlternateFadeOutAnimation);
            mKidsText.startAnimation(mAlternateFadeOutAnimation);
            mAdultsText.startAnimation(mAlternateFadeOutAnimation);
        }
    };
    
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.difficulty_menu);
        
        
        mBabyButton = findViewById(R.id.babyButton);
        mKidsButton = findViewById(R.id.kidsButton);
        mAdultsButton = findViewById(R.id.adultsButton);
        mBabyText = findViewById(R.id.babyText);
        mKidsText = findViewById(R.id.kidsText);
        mAdultsText = findViewById(R.id.adultsText);
        mBackground = findViewById(R.id.mainMenuBackground);
        
        mBabyButton.setOnClickListener(sBabyButtonListener);
        mKidsButton.setOnClickListener(sKidsButtonListener);
        mAdultsButton.setOnClickListener(sAdultsButtonListener);
        
        mButtonFlickerAnimation = AnimationUtils.loadAnimation(this, R.anim.button_flicker);
        mFadeOutAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_out);
        mAlternateFadeOutAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_out);        
        
        // Keep the volume control type consistent across all activities.
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		boolean result = true;
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			finish();
			
			if (UIConstants.mOverridePendingTransition != null) {
 		       try {
 		    	  UIConstants.mOverridePendingTransition.invoke(DifficultyMenuActivity.this, R.anim.activity_fade_in, R.anim.activity_fade_out);
 		       } catch (InvocationTargetException ite) {
 		           DebugLog.d("Activity Transition", "Invocation Target Exception");
 		       } catch (IllegalAccessException ie) {
 		    	   DebugLog.d("Activity Transition", "Illegal Access Exception");
 		       }
             }
		} else {
			result = super.onKeyDown(keyCode, event);
		}
		return result;
	}
 
	 protected class StartActivityAfterAnimation implements Animation.AnimationListener {
	        private Intent mIntent;
	        
	        StartActivityAfterAnimation(Intent intent) {
	            mIntent = intent;
	        }
	            

	        public void onAnimationEnd(Animation animation) {
	        	mBabyButton.setVisibility(View.INVISIBLE);
	        	mBabyButton.clearAnimation();
	        	mKidsButton.setVisibility(View.INVISIBLE);
	        	mKidsButton.clearAnimation();
	        	mAdultsButton.setVisibility(View.INVISIBLE);
	        	mAdultsButton.clearAnimation();
	            startActivity(mIntent);     
	            finish();	// This activity dies when it spawns a new intent.
	            
	            if (UIConstants.mOverridePendingTransition != null) {
	 		       try {
	 		    	  UIConstants.mOverridePendingTransition.invoke(DifficultyMenuActivity.this, R.anim.activity_fade_in, R.anim.activity_fade_out);
	 		       } catch (InvocationTargetException ite) {
	 		           DebugLog.d("Activity Transition", "Invocation Target Exception");
	 		       } catch (IllegalAccessException ie) {
	 		    	   DebugLog.d("Activity Transition", "Illegal Access Exception");
	 		       }
	             }
	        }

	        public void onAnimationRepeat(Animation animation) {
	            // TODO Auto-generated method stub
	            
	        }

	        public void onAnimationStart(Animation animation) {
	            // TODO Auto-generated method stub
	            
	        }
	        
	    }
	
}
