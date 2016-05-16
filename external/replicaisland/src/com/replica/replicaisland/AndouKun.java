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
import java.lang.reflect.Method;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Debug;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

/**
 * Core activity for the game.  Sets up a surface view for OpenGL, bootstraps
 * the game engine, and manages UI events.  Also manages game progression,
 * transitioning to other activites, save game, and input events.
 */
public class AndouKun extends Activity implements SensorEventListener {
    private static final int ACTIVITY_CHANGE_LEVELS = 0;
    private static final int ACTIVITY_CONVERSATION = 1;
    private static final int ACTIVITY_DIARY = 2;
    private static final int ACTIVITY_ANIMATION_PLAYER = 3;

    private static final int CHANGE_LEVEL_ID = Menu.FIRST;
    private static final int TEST_ANIMATION_ID = CHANGE_LEVEL_ID + 1;
    private static final int TEST_DIARY_ID = CHANGE_LEVEL_ID + 2;
    private static final int METHOD_TRACING_ID = CHANGE_LEVEL_ID + 3;
    
    private static final int ROLL_TO_FACE_BUTTON_DELAY = 400;
    
    public static final int QUIT_GAME_DIALOG = 0;
    
    // If the version is a negative number, debug features (logging and a debug menu)
    // are enabled.
    public static final int VERSION = 14;

    private GLSurfaceView mGLSurfaceView;
    private Game mGame;
    private boolean mMethodTracing;
    private int mLevelRow;
    private int mLevelIndex;
    private float mTotalGameTime;
    private int mRobotsDestroyed;
    private int mPearlsCollected;
    private int mPearlsTotal;
    private int mLastEnding = -1;
    private int mLinearMode = 0;
    private int mDifficulty = 1;
    private boolean mExtrasUnlocked;
    private SensorManager mSensorManager;
    private SharedPreferences.Editor mPrefsEditor;
    private long mLastTouchTime = 0L;
    private long mLastRollTime = 0L;
    private View mPauseMessage = null;
    private View mWaitMessage = null;
    private View mLevelNameBox = null;
    private TextView mLevelName = null;
    private Animation mWaitFadeAnimation = null;
    
    private EventReporter mEventReporter;
    private Thread mEventReporterThread;
    
    private long mSessionId = 0L;
    
    /** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        SharedPreferences prefs = getSharedPreferences(PreferenceConstants.PREFERENCE_NAME, MODE_PRIVATE);
        final boolean debugLogs = prefs.getBoolean(PreferenceConstants.PREFERENCE_ENABLE_DEBUG, false);
        
        if (VERSION < 0 || debugLogs) {
        	DebugLog.setDebugLogging(true);
        } else {
        	DebugLog.setDebugLogging(false);
        }
        
        DebugLog.d("AndouKun", "onCreate");
        
        
        setContentView(R.layout.main);
        mGLSurfaceView = (GLSurfaceView) findViewById(R.id.glsurfaceview);
        mPauseMessage = findViewById(R.id.pausedMessage);
        mWaitMessage = findViewById(R.id.pleaseWaitMessage);
        mLevelNameBox = findViewById(R.id.levelNameBox);
        mLevelName = (TextView)findViewById(R.id.levelName);
        mWaitFadeAnimation = AnimationUtils.loadAnimation(this, R.anim.wait_message_fade);

        
        //mGLSurfaceView.setGLWrapper(new GLErrorLogger());
        mGLSurfaceView.setEGLConfigChooser(false); // 16 bit, no z-buffer
        //mGLSurfaceView.setDebugFlags(GLSurfaceView.DEBUG_CHECK_GL_ERROR | GLSurfaceView.DEBUG_LOG_GL_CALLS);
        mGame = new Game();
        mGame.setSurfaceView(mGLSurfaceView);
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        int defaultWidth = 480;
        int defaultHeight = 320;
        if (dm.widthPixels != defaultWidth) {
        	float ratio =((float)dm.widthPixels) / dm.heightPixels;
        	defaultWidth = (int)(defaultHeight * ratio);
        }
        
 
        mLevelRow = 0;
        mLevelIndex = 0;
        
        
        mPrefsEditor = prefs.edit();
        // Make sure that old game information is cleared when we start a new game.
        // CTS: clear settings to force start from beginning
        mPrefsEditor.remove(PreferenceConstants.PREFERENCE_LEVEL_ROW);
        mPrefsEditor.remove(PreferenceConstants.PREFERENCE_LEVEL_INDEX);
        mPrefsEditor.remove(PreferenceConstants.PREFERENCE_LEVEL_COMPLETED);
        mPrefsEditor.remove(PreferenceConstants.PREFERENCE_LINEAR_MODE);
        mPrefsEditor.remove(PreferenceConstants.PREFERENCE_TOTAL_GAME_TIME);
        mPrefsEditor.remove(PreferenceConstants.PREFERENCE_PEARLS_COLLECTED);
        mPrefsEditor.remove(PreferenceConstants.PREFERENCE_PEARLS_TOTAL);
        mPrefsEditor.remove(PreferenceConstants.PREFERENCE_ROBOTS_DESTROYED);
        mPrefsEditor.remove(PreferenceConstants.PREFERENCE_DIFFICULTY);
        mPrefsEditor.commit();
        
        
        mLevelRow = prefs.getInt(PreferenceConstants.PREFERENCE_LEVEL_ROW, 0);
        mLevelIndex = prefs.getInt(PreferenceConstants.PREFERENCE_LEVEL_INDEX, 0);
        int completed = prefs.getInt(PreferenceConstants.PREFERENCE_LEVEL_COMPLETED, 0);
        mTotalGameTime = prefs.getFloat(PreferenceConstants.PREFERENCE_TOTAL_GAME_TIME, 0.0f);
        mRobotsDestroyed = prefs.getInt(PreferenceConstants.PREFERENCE_ROBOTS_DESTROYED, 0);
        mPearlsCollected = prefs.getInt(PreferenceConstants.PREFERENCE_PEARLS_COLLECTED, 0);
        mPearlsTotal = prefs.getInt(PreferenceConstants.PREFERENCE_PEARLS_TOTAL, 0);
        mLinearMode = prefs.getInt(PreferenceConstants.PREFERENCE_LINEAR_MODE, 
        		getIntent().getBooleanExtra("linearMode", false) ? 1 : 0);
        mExtrasUnlocked = prefs.getBoolean(PreferenceConstants.PREFERENCE_EXTRAS_UNLOCKED, false);
        mDifficulty = prefs.getInt(PreferenceConstants.PREFERENCE_DIFFICULTY, getIntent().getIntExtra("difficulty", 1));

        mGame.bootstrap(this, dm.widthPixels, dm.heightPixels, defaultWidth, defaultHeight, mDifficulty);
        mGLSurfaceView.setRenderer(mGame.getRenderer());
        
       
        int levelTreeResource = R.xml.level_tree;
	    if (mLinearMode != 0) {
	    	levelTreeResource = R.xml.linear_level_tree;
	    }
        
        
        // Android activity lifecycle rules make it possible for this activity to be created
        // and come to the foreground without the MainMenu Activity ever running, so in that
        // case we need to make sure that this static data is valid.
        if (!LevelTree.isLoaded(levelTreeResource)) {
        	LevelTree.loadLevelTree(levelTreeResource, this);
        	LevelTree.loadAllDialog(this);
        }
        
        if (getIntent().getBooleanExtra("startAtLevelSelect", false)) {
        	Intent i = new Intent(this, LevelSelectActivity.class);
        	i.putExtra("unlockAll", true);
            startActivityForResult(i, ACTIVITY_CHANGE_LEVELS);
        } else {
	        if (!LevelTree.levelIsValid(mLevelRow, mLevelIndex)) {
	        	// bad data?  Let's try to recover.
	        	
	        	// is the row valid?
	        	if (LevelTree.rowIsValid(mLevelRow)) {
	        		// In that case, just start the row over.
	        		mLevelIndex = 0;
	        		completed = 0;
	        	} else if (LevelTree.rowIsValid(mLevelRow - 1)) {
	        		// If not, try to back up a row.
	        		mLevelRow--;
	        		mLevelIndex = 0;
	        		completed = 0;
	        	}
	        	
	        	
	        	if (!LevelTree.levelIsValid(mLevelRow, mLevelIndex)) {
		        	// if all else fails, start the game over.
		        	mLevelRow = 0;
		        	mLevelIndex = 0;
		        	completed = 0;
	        	}
	        } 
	        
	        LevelTree.updateCompletedState(mLevelRow, completed);
	        
	        mGame.setPendingLevel(LevelTree.get(mLevelRow, mLevelIndex));
	        if (LevelTree.get(mLevelRow, mLevelIndex).showWaitMessage) {
	    		showWaitMessage();
	        } else {
	    		hideWaitMessage();
	        }
        }
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // This activity uses the media stream.
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        
          
        mSessionId = prefs.getLong(PreferenceConstants.PREFERENCE_SESSION_ID, System.currentTimeMillis());
        
        
        mEventReporter = null;
        mEventReporterThread = null;
        final boolean statsEnabled = prefs.getBoolean(PreferenceConstants.PREFERENCE_STATS_ENABLED, true);
        if (statsEnabled) {
	        mEventReporter = new EventReporter();
	        mEventReporterThread = new Thread(mEventReporter);
	        mEventReporterThread.setName("EventReporter");
	        mEventReporterThread.start();
        }
    }

    
    @Override
    protected void onDestroy() {
        DebugLog.d("AndouKun", "onDestroy()");
        mGame.stop();
        if (mEventReporterThread != null) {
	        mEventReporter.stop();
	        try {
				mEventReporterThread.join();
			} catch (InterruptedException e) {
				mEventReporterThread.interrupt();
			}
        }
        super.onDestroy();
        
    }


    @Override
    protected void onPause() {
        super.onPause();
        DebugLog.d("AndouKun", "onPause");

        hidePauseMessage();
        
        mGame.onPause();
        mGLSurfaceView.onPause();
        mGame.getRenderer().onPause();	// hack!
        
        if (mMethodTracing) {
            Debug.stopMethodTracing();
            mMethodTracing = false;
        }
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // Preferences may have changed while we were paused.
        SharedPreferences prefs = getSharedPreferences(PreferenceConstants.PREFERENCE_NAME, MODE_PRIVATE);
        final boolean debugLogs = prefs.getBoolean(PreferenceConstants.PREFERENCE_ENABLE_DEBUG, false);
        
        if (VERSION < 0 || debugLogs) {
        	DebugLog.setDebugLogging(true);
        } else {
        	DebugLog.setDebugLogging(false);
        }
        
        DebugLog.d("AndouKun", "onResume");
        mGLSurfaceView.onResume();
        mGame.onResume(this, false);
       
        
        final boolean soundEnabled = prefs.getBoolean(PreferenceConstants.PREFERENCE_SOUND_ENABLED, true);
        final boolean safeMode = prefs.getBoolean(PreferenceConstants.PREFERENCE_SAFE_MODE, false);
        final boolean clickAttack = prefs.getBoolean(PreferenceConstants.PREFERENCE_CLICK_ATTACK, true);
        final boolean tiltControls = prefs.getBoolean(PreferenceConstants.PREFERENCE_TILT_CONTROLS, false);
        final int tiltSensitivity = prefs.getInt(PreferenceConstants.PREFERENCE_TILT_SENSITIVITY, 50);
        final int movementSensitivity = prefs.getInt(PreferenceConstants.PREFERENCE_MOVEMENT_SENSITIVITY, 100);
        final boolean onScreenControls = prefs.getBoolean(PreferenceConstants.PREFERENCE_SCREEN_CONTROLS, false);

        final int leftKey = prefs.getInt(PreferenceConstants.PREFERENCE_LEFT_KEY, KeyEvent.KEYCODE_DPAD_LEFT);
        final int rightKey = prefs.getInt(PreferenceConstants.PREFERENCE_RIGHT_KEY, KeyEvent.KEYCODE_DPAD_RIGHT);
        final int jumpKey = prefs.getInt(PreferenceConstants.PREFERENCE_JUMP_KEY, KeyEvent.KEYCODE_SPACE);
        final int attackKey = prefs.getInt(PreferenceConstants.PREFERENCE_ATTACK_KEY, KeyEvent.KEYCODE_SHIFT_LEFT);
        
        mGame.setSoundEnabled(soundEnabled);
        mGame.setControlOptions(clickAttack, tiltControls, tiltSensitivity, movementSensitivity, onScreenControls);
        mGame.setKeyConfig(leftKey, rightKey, jumpKey, attackKey);
        mGame.setSafeMode(safeMode);
        
        if (mSensorManager != null) {
            Sensor orientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
            if (orientation != null) {
                mSensorManager.registerListener(this, 
                    orientation,
                    SensorManager.SENSOR_DELAY_GAME,
                    null);
            }
        }
    }

    @Override
    public boolean onTrackballEvent(MotionEvent event) {
    	if (!mGame.isPaused()) {
	        mGame.onTrackballEvent(event);
	        final long time = System.currentTimeMillis();
	        mLastRollTime = time;
    	}
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
    	if (!mGame.isPaused()) {
    		mGame.onTouchEvent(event);
	    	
	        final long time = System.currentTimeMillis();
	        if (event.getAction() == MotionEvent.ACTION_MOVE && time - mLastTouchTime < 32) {
		        // Sleep so that the main thread doesn't get flooded with UI events.
		        try {
		            Thread.sleep(32);
		        } catch (InterruptedException e) {
		            // No big deal if this sleep is interrupted.
		        }
		        mGame.getRenderer().waitDrawingComplete();
	        }
	        mLastTouchTime = time;
    	}
        return true;
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	boolean result = true;
    	if (keyCode == KeyEvent.KEYCODE_BACK) {
			final long time = System.currentTimeMillis();
    		if (time - mLastRollTime > ROLL_TO_FACE_BUTTON_DELAY &&
    				time - mLastTouchTime > ROLL_TO_FACE_BUTTON_DELAY) {
    			showDialog(QUIT_GAME_DIALOG);
    			result = true;
    		}
    	} else if (keyCode == KeyEvent.KEYCODE_MENU) {
    		result = true;
    		if (mGame.isPaused()) {
    			hidePauseMessage();
    			mGame.onResume(this, true);
    		} else {
    			final long time = System.currentTimeMillis();
    	        if (time - mLastRollTime > ROLL_TO_FACE_BUTTON_DELAY &&
    	        		time - mLastTouchTime > ROLL_TO_FACE_BUTTON_DELAY) {
    	        	showPauseMessage();
    	        	mGame.onPause();
    	        }
    	        if (VERSION < 0) {
    	        	result = false;	// Allow the debug menu to come up in debug mode.
    	        }
    		}
    	} else {
		    result = mGame.onKeyDownEvent(keyCode);
		    // Sleep so that the main thread doesn't get flooded with UI events.
		    try {
		        Thread.sleep(4);
		    } catch (InterruptedException e) {
		        // No big deal if this sleep is interrupted.
		    }
    	}
        return result;
    }
     
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
    	boolean result = false;
    	if (keyCode == KeyEvent.KEYCODE_BACK) {
    		result = true;
    	} else if (keyCode == KeyEvent.KEYCODE_MENU){ 
	        if (VERSION < 0) {
	        	result = false;	// Allow the debug menu to come up in debug mode.
	        }
    	} else {
    		result = mGame.onKeyUpEvent(keyCode);
	        // Sleep so that the main thread doesn't get flooded with UI events.
	        try {
	            Thread.sleep(4);
	        } catch (InterruptedException e) {
	            // No big deal if this sleep is interrupted.
	        }
    	}
        return result;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        boolean handled = false;
        // Only allow the debug menu in development versions.
        if (VERSION < 0) {
	        menu.add(0, CHANGE_LEVEL_ID, 0, R.string.change_level);
	        menu.add(0, TEST_ANIMATION_ID, 0, R.string.test_animation);
	        menu.add(0, TEST_DIARY_ID, 0, R.string.test_diary);
	
	        menu.add(0, METHOD_TRACING_ID, 0, R.string.method_tracing);
	        handled = true;
        }
        
        return handled;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        Intent i;
        switch(item.getItemId()) {
        case CHANGE_LEVEL_ID:
            i = new Intent(this, LevelSelectActivity.class);
            startActivityForResult(i, ACTIVITY_CHANGE_LEVELS);
            return true;
        case TEST_ANIMATION_ID:
            i = new Intent(this, AnimationPlayerActivity.class);
            i.putExtra("animation", AnimationPlayerActivity.ROKUDOU_ENDING);
            startActivity(i);
            return true;
        case TEST_DIARY_ID:
            i = new Intent(this, DiaryActivity.class);
            i.putExtra("text", R.string.Diary10);
            startActivity(i);
            return true;
        case METHOD_TRACING_ID:
            if (mMethodTracing) {
                Debug.stopMethodTracing();
            } else {
                Debug.startMethodTracing("andou");
            }
            mMethodTracing = !mMethodTracing;
            return true;
        }
        
        return super.onMenuItemSelected(featureId, item);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        
        if (requestCode == ACTIVITY_CHANGE_LEVELS) {
	        if (resultCode == RESULT_OK) {
	            mLevelRow = intent.getExtras().getInt("row");
	            mLevelIndex = intent.getExtras().getInt("index");
		        LevelTree.updateCompletedState(mLevelRow, 0);

	            saveGame();
	            
	            mGame.setPendingLevel(LevelTree.get(mLevelRow, mLevelIndex));    
	            if (LevelTree.get(mLevelRow, mLevelIndex).showWaitMessage) {
            		showWaitMessage();
	            } else {
            		hideWaitMessage();
	            }
	            
	        }  
        } else if (requestCode == ACTIVITY_ANIMATION_PLAYER) {
        	int lastAnimation = intent.getIntExtra("animation", -1);
        	// record ending events.
        	if (lastAnimation > -1) {
        		mGame.setLastEnding(lastAnimation);
        	}
        	// on finishing animation playback, force a level change.
        	onGameFlowEvent(GameFlowEvent.EVENT_GO_TO_NEXT_LEVEL, 0);
        }
    }
    
    /*
     *  When the game thread needs to stop its own execution (to go to a new level, or restart the
     *  current level), it registers a runnable on the main thread which orders the action via this
     *  function.
     */
    public void onGameFlowEvent(int eventCode, int index) {
       switch (eventCode) {
           case GameFlowEvent.EVENT_END_GAME: 
               mGame.stop();
               finish();
               break;
           case GameFlowEvent.EVENT_RESTART_LEVEL:
        	   if (LevelTree.get(mLevelRow, mLevelIndex).restartable) {
        		   if (mEventReporter != null) {
	        		   mEventReporter.addEvent(EventReporter.EVENT_DEATH,
	        				   mGame.getLastDeathPosition().x, 
	        				   mGame.getLastDeathPosition().y, 
	        				   mGame.getGameTime(), 
	        				   LevelTree.get(mLevelRow, mLevelIndex).name, 
	        				   VERSION, 
	        				   mSessionId);
        		   }
        		   mGame.restartLevel();
        		   break;
        	   }
        	   // else, fall through and go to the next level.
           case GameFlowEvent.EVENT_GO_TO_NEXT_LEVEL:
               LevelTree.get(mLevelRow, mLevelIndex).completed = true;
               final LevelTree.LevelGroup currentGroup = LevelTree.levels.get(mLevelRow);
               final int count = currentGroup.levels.size();
               boolean groupCompleted = true;
               if (mEventReporter != null) {
	               mEventReporter.addEvent(EventReporter.EVENT_BEAT_LEVEL,
	    				   0, 
	    				   0, 
	    				   mGame.getGameTime(), 
	    				   LevelTree.get(mLevelRow, mLevelIndex).name, 
	    				   VERSION, 
	    				   mSessionId);
               }
               for (int x = 0; x < count; x++) {
            	   if (currentGroup.levels.get(x).completed == false) {
            		   // We haven't completed the group yet.
            		   mLevelIndex = x;
            		   groupCompleted = false;
            		   break;
            	   }
               }
               
               if (groupCompleted) {
                   mLevelIndex = 0;
                   mLevelRow++;
               }
               
    		   mTotalGameTime += mGame.getGameTime();
    		   mRobotsDestroyed += mGame.getRobotsDestroyed();
    		   mPearlsCollected += mGame.getPearlsCollected();
    		   mPearlsTotal += mGame.getPearlsTotal();
               
               if (mLevelRow < LevelTree.levels.size()) {
            	   final LevelTree.Level currentLevel = LevelTree.get(mLevelRow, mLevelIndex);
            	   if (currentLevel.inThePast || LevelTree.levels.get(mLevelRow).levels.size() > 1) {
            		   // go to the level select.
            		   Intent i = new Intent(this, LevelSelectActivity.class);
                       startActivityForResult(i, ACTIVITY_CHANGE_LEVELS);
                       if (UIConstants.mOverridePendingTransition != null) {
        	 		       try {
        	 		    	  UIConstants.mOverridePendingTransition.invoke(AndouKun.this, R.anim.activity_fade_in, R.anim.activity_fade_out);
        	 		       } catch (InvocationTargetException ite) {
        	 		           DebugLog.d("Activity Transition", "Invocation Target Exception");
        	 		       } catch (IllegalAccessException ie) {
        	 		    	   DebugLog.d("Activity Transition", "Illegal Access Exception");
        	 		       }
        	            }
            	   } else {
            		   // go directly to the next level
	                   mGame.setPendingLevel(currentLevel);
	                   if (currentLevel.showWaitMessage) {
	                	   showWaitMessage();
	                   } else {
	                	   hideWaitMessage();
	                   }
	                   mGame.requestNewLevel();
            	   }
            	   saveGame();
            	   
               } else {
            	   if (mEventReporter != null) {
	            	   mEventReporter.addEvent(EventReporter.EVENT_BEAT_GAME,
	        				   0, 
	        				   0, 
	        				   mGame.getGameTime(), 
	        				   "end", 
	        				   VERSION, 
	        				   mSessionId);
            	   }
                   // We beat the game!
            	   mLevelRow = 0;
            	   mLevelIndex = 0;
            	   mLastEnding = mGame.getLastEnding();
            	   mExtrasUnlocked = true;
            	   saveGame();
                   mGame.stop();
                   Intent i = new Intent(this, GameOverActivity.class);
                   startActivity(i);
                   if (UIConstants.mOverridePendingTransition != null) {
    	 		       try {
    	 		    	  UIConstants.mOverridePendingTransition.invoke(AndouKun.this, R.anim.activity_fade_in, R.anim.activity_fade_out);
    	 		       } catch (InvocationTargetException ite) {
    	 		           DebugLog.d("Activity Transition", "Invocation Target Exception");
    	 		       } catch (IllegalAccessException ie) {
    	 		    	   DebugLog.d("Activity Transition", "Illegal Access Exception");
    	 		       }
    	            }
                   finish();
                   
               }
               break;
           case GameFlowEvent.EVENT_SHOW_DIARY:
               Intent i = new Intent(this, DiaryActivity.class);
               LevelTree.Level level = LevelTree.get(mLevelRow, mLevelIndex);
               level.diaryCollected = true;
               i.putExtra("text", level.dialogResources.diaryEntry);
               startActivity(i);
               if (UIConstants.mOverridePendingTransition != null) {
 	 		       try {
 	 		    	  UIConstants.mOverridePendingTransition.invoke(AndouKun.this, R.anim.activity_fade_in, R.anim.activity_fade_out);
 	 		       } catch (InvocationTargetException ite) {
 	 		           DebugLog.d("Activity Transition", "Invocation Target Exception");
 	 		       } catch (IllegalAccessException ie) {
 	 		    	   DebugLog.d("Activity Transition", "Illegal Access Exception");
 	 		       }
 	            }
               break;
               
           case GameFlowEvent.EVENT_SHOW_DIALOG_CHARACTER1:
        	   i = new Intent(this, ConversationDialogActivity.class);
               i.putExtra("levelRow", mLevelRow);
               i.putExtra("levelIndex", mLevelIndex);
               i.putExtra("index", index);
               i.putExtra("character", 1);
               startActivity(i);
               break;
               
           case GameFlowEvent.EVENT_SHOW_DIALOG_CHARACTER2:
        	   i = new Intent(this, ConversationDialogActivity.class);
               i.putExtra("levelRow", mLevelRow);
               i.putExtra("levelIndex", mLevelIndex);
               i.putExtra("index", index);
               i.putExtra("character", 2);
               startActivity(i);
               break;
           case GameFlowEvent.EVENT_SHOW_ANIMATION:
        	   i = new Intent(this, AnimationPlayerActivity.class);
               i.putExtra("animation", index);
               startActivityForResult(i, ACTIVITY_ANIMATION_PLAYER);
               if (UIConstants.mOverridePendingTransition != null) {
	 		       try {
	 		    	  UIConstants.mOverridePendingTransition.invoke(AndouKun.this, R.anim.activity_fade_in, R.anim.activity_fade_out);
	 		       } catch (InvocationTargetException ite) {
	 		           DebugLog.d("Activity Transition", "Invocation Target Exception");
	 		       } catch (IllegalAccessException ie) {
	 		    	   DebugLog.d("Activity Transition", "Illegal Access Exception");
	 		       }
	            }
               break;
        	   
       }
    }
    
    protected void saveGame() {
    	if (mPrefsEditor != null) {
    		final int completed = LevelTree.packCompletedLevels(mLevelRow);
    		mPrefsEditor.putInt(PreferenceConstants.PREFERENCE_LEVEL_ROW, mLevelRow);
    		mPrefsEditor.putInt(PreferenceConstants.PREFERENCE_LEVEL_INDEX, mLevelIndex);
    		mPrefsEditor.putInt(PreferenceConstants.PREFERENCE_LEVEL_COMPLETED, completed);
    		mPrefsEditor.putLong(PreferenceConstants.PREFERENCE_SESSION_ID, mSessionId);
    		mPrefsEditor.putFloat(PreferenceConstants.PREFERENCE_TOTAL_GAME_TIME, mTotalGameTime);
    		mPrefsEditor.putInt(PreferenceConstants.PREFERENCE_LAST_ENDING, mLastEnding);
    		mPrefsEditor.putInt(PreferenceConstants.PREFERENCE_ROBOTS_DESTROYED, mRobotsDestroyed);
    		mPrefsEditor.putInt(PreferenceConstants.PREFERENCE_PEARLS_COLLECTED, mPearlsCollected);
    		mPrefsEditor.putInt(PreferenceConstants.PREFERENCE_PEARLS_TOTAL, mPearlsTotal);
    		mPrefsEditor.putInt(PreferenceConstants.PREFERENCE_LINEAR_MODE, mLinearMode);
    		mPrefsEditor.putBoolean(PreferenceConstants.PREFERENCE_EXTRAS_UNLOCKED, mExtrasUnlocked);
    		mPrefsEditor.putInt(PreferenceConstants.PREFERENCE_DIFFICULTY, mDifficulty);
    		mPrefsEditor.commit();
    	}
    }
    
    protected void showPauseMessage() {
    	if (mPauseMessage != null) {
    		mPauseMessage.setVisibility(View.VISIBLE);
    	}
    	if (mLevelNameBox != null && mLevelName != null) {
    		mLevelName.setText(LevelTree.get(mLevelRow, mLevelIndex).name);
    		mLevelNameBox.setVisibility(View.VISIBLE);
    	}
    }
    
    protected void hidePauseMessage() {
    	if (mPauseMessage != null) {
    		mPauseMessage.setVisibility(View.GONE);
    	}
    	if (mLevelNameBox != null) {
    		mLevelNameBox.setVisibility(View.GONE);
    	}
    }
    
    protected void showWaitMessage() {
    	if (mWaitMessage != null) {
    		mWaitMessage.setVisibility(View.VISIBLE);
    		mWaitMessage.startAnimation(mWaitFadeAnimation);
    	}
    }
    
    protected void hideWaitMessage() {
    	if (mWaitMessage != null) {
    		mWaitMessage.setVisibility(View.GONE);
    		mWaitMessage.clearAnimation();
    	}
    }


    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub
        
    }


    public void onSensorChanged(SensorEvent event) {
       synchronized (this) {
           if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
               final float x = event.values[1];
               final float y = event.values[2];
               final float z = event.values[0];
               mGame.onOrientationEvent(x, y, z);
           }
       }
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        if (id == QUIT_GAME_DIALOG) {
        	
            dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.quit_game_dialog_title)
                .setPositiveButton(R.string.quit_game_dialog_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    	finish();
                    	if (UIConstants.mOverridePendingTransition != null) {
         	 		       try {
         	 		    	  UIConstants.mOverridePendingTransition.invoke(AndouKun.this, R.anim.activity_fade_in, R.anim.activity_fade_out);
         	 		       } catch (InvocationTargetException ite) {
         	 		           DebugLog.d("Activity Transition", "Invocation Target Exception");
         	 		       } catch (IllegalAccessException ie) {
         	 		    	   DebugLog.d("Activity Transition", "Illegal Access Exception");
         	 		       }
         	            }
                    }
                })
                .setNegativeButton(R.string.quit_game_dialog_cancel, null)
                .setMessage(R.string.quit_game_dialog_message)
                .create();
        }
        return dialog;
    }
}
