package com.replica.replicaisland;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class ExtrasMenuActivity extends Activity {
	private View mLinearModeButton;
	private View mLevelSelectButton;
    private View mControlsButton;
    private View mBackground;
    private View mLevelSelectLocked;
    private View mLinearModeLocked;
    private Animation mButtonFlickerAnimation;
    private Animation mFadeOutAnimation;
    private Animation mAlternateFadeOutAnimation;
    private Animation mLockedAnimation;
    
    private int mPendingGameStart;
    
    
    public static final int NEW_GAME_DIALOG = 0;
    public static final int EXTRAS_LOCKED_DIALOG = 1;

    private static final int START_LINEAR_MODE = 0;
    private static final int START_LEVEL_SELECT = 1;

    
    private View.OnClickListener sLinearModeButtonListener = new View.OnClickListener() {
        public void onClick(View v) {
        	SharedPreferences prefs = getSharedPreferences(PreferenceConstants.PREFERENCE_NAME, MODE_PRIVATE);
            final int row = prefs.getInt(PreferenceConstants.PREFERENCE_LEVEL_ROW, 0);
            final int index = prefs.getInt(PreferenceConstants.PREFERENCE_LEVEL_INDEX, 0);
            if (row != 0 || index != 0) {
            	mPendingGameStart = START_LINEAR_MODE;
            	showDialog(NEW_GAME_DIALOG);
            } else {
            	startGame(START_LINEAR_MODE);
            }
        }
    };
    
    private View.OnClickListener sLevelSelectButtonListener = new View.OnClickListener() {
        public void onClick(View v) {
        	SharedPreferences prefs = getSharedPreferences(PreferenceConstants.PREFERENCE_NAME, MODE_PRIVATE);
            final int row = prefs.getInt(PreferenceConstants.PREFERENCE_LEVEL_ROW, 0);
            final int index = prefs.getInt(PreferenceConstants.PREFERENCE_LEVEL_INDEX, 0);
            if (row != 0 || index != 0) {
            	mPendingGameStart = START_LEVEL_SELECT;
            	showDialog(NEW_GAME_DIALOG);
            } else {
            	startGame(START_LEVEL_SELECT);
            }
        }
    };
    
    private View.OnClickListener sLockedSelectButtonListener = new View.OnClickListener() {
        public void onClick(View v) {
        	showDialog(EXTRAS_LOCKED_DIALOG);  
        }
    };
    
    private View.OnClickListener sControlsButtonListener = new View.OnClickListener() {
        public void onClick(View v) {
        
            Intent i = new Intent(getBaseContext(), SetPreferencesActivity.class);
            i.putExtra("controlConfig", true);
            
            v.startAnimation(mButtonFlickerAnimation);
            mFadeOutAnimation.setAnimationListener(new StartActivityAfterAnimation(i));
            mBackground.startAnimation(mFadeOutAnimation);
            mLinearModeButton.startAnimation(mAlternateFadeOutAnimation);
            mLevelSelectButton.startAnimation(mAlternateFadeOutAnimation);
        }
    };
    
	 @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.extras_menu);
        
        SharedPreferences prefs = getSharedPreferences(PreferenceConstants.PREFERENCE_NAME, MODE_PRIVATE);
        final boolean extrasUnlocked = prefs.getBoolean(PreferenceConstants.PREFERENCE_EXTRAS_UNLOCKED, false);
        
        mLinearModeButton = findViewById(R.id.linearModeButton);
        mLevelSelectButton = findViewById(R.id.levelSelectButton);
        mControlsButton = findViewById(R.id.controlsButton);
        mLinearModeLocked = findViewById(R.id.linearModeLocked);
        mLevelSelectLocked = findViewById(R.id.levelSelectLocked);
        
        mBackground = findViewById(R.id.mainMenuBackground);
        
        mButtonFlickerAnimation = AnimationUtils.loadAnimation(this, R.anim.button_flicker);
        mFadeOutAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_out);
        mAlternateFadeOutAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_out);  

        
        if (extrasUnlocked) {
        	mLinearModeButton.setOnClickListener(sLinearModeButtonListener);
        	mLevelSelectButton.setOnClickListener(sLevelSelectButtonListener);
        	mLinearModeLocked.setVisibility(View.GONE);
        	mLevelSelectLocked.setVisibility(View.GONE);
        } else {
            mLockedAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in_out);

        	mLinearModeButton.setOnClickListener(sLockedSelectButtonListener);
        	mLevelSelectButton.setOnClickListener(sLockedSelectButtonListener);
        	mLinearModeLocked.startAnimation(mLockedAnimation);
        	mLevelSelectLocked.startAnimation(mLockedAnimation);
        }
        mControlsButton.setOnClickListener(sControlsButtonListener);
        
        
              
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
 		    	  UIConstants.mOverridePendingTransition.invoke(ExtrasMenuActivity.this, R.anim.activity_fade_in, R.anim.activity_fade_out);
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

	@Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        if (id == NEW_GAME_DIALOG) {
        	
            dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.new_game_dialog_title)
                .setPositiveButton(R.string.new_game_dialog_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    	startGame(mPendingGameStart);
                    }
                })
                .setNegativeButton(R.string.new_game_dialog_cancel, null)
                .setMessage(R.string.new_game_dialog_message)
                .create();
        } else if (id == EXTRAS_LOCKED_DIALOG) {
            dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.extras_locked_dialog_title)
                .setPositiveButton(R.string.extras_locked_dialog_ok, null)
                .setMessage(R.string.extras_locked_dialog_message)
                .create();
        }
        return dialog;
    }

	protected void startGame(int type) {
		 if (type == START_LINEAR_MODE) {
            Intent i = new Intent(getBaseContext(), DifficultyMenuActivity.class);
            i.putExtra("linearMode", true);
            i.putExtra("newGame", true);
            mLinearModeButton.startAnimation(mButtonFlickerAnimation);
            mButtonFlickerAnimation.setAnimationListener(new StartActivityAfterAnimation(i));
            
        } else if (type == START_LEVEL_SELECT) {
            Intent i = new Intent(getBaseContext(), DifficultyMenuActivity.class);
            i.putExtra("startAtLevelSelect", true);
            i.putExtra("newGame", true);
            mLevelSelectButton.startAnimation(mButtonFlickerAnimation);
            mButtonFlickerAnimation.setAnimationListener(new StartActivityAfterAnimation(i));
           
        }
	}

	protected class StartActivityAfterAnimation implements Animation.AnimationListener {
	        private Intent mIntent;
	        
	        StartActivityAfterAnimation(Intent intent) {
	            mIntent = intent;
	        }
	            

	        public void onAnimationEnd(Animation animation) {
	        	mLinearModeButton.setVisibility(View.INVISIBLE);
	        	mLinearModeButton.clearAnimation();
	        	mLevelSelectButton.setVisibility(View.INVISIBLE);
	        	mLevelSelectButton.clearAnimation();
	        	mControlsButton.setVisibility(View.INVISIBLE);
	        	mControlsButton.clearAnimation();
	            startActivity(mIntent);   
	            finish();
	            if (UIConstants.mOverridePendingTransition != null) {
	 		       try {
	 		    	  UIConstants.mOverridePendingTransition.invoke(ExtrasMenuActivity.this, R.anim.activity_fade_in, R.anim.activity_fade_out);
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
