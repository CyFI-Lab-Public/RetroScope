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

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnKeyListener;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;


public class KeyboardConfigDialogPreference extends DialogPreference implements OnKeyListener {
	private SharedPreferences mSharedPrefs;
	private Context mContext;
	private String mLeftPrefKey;
	private String mRightPrefKey;
	private String mJumpPrefKey;
	private String mAttackPrefKey;
	private String[] mKeyLabels;
	private int mListeningId = 0;
	private View mLeftBorder;
	private View mRightBorder;
	private View mJumpBorder;
	private View mAttackBorder;
	private Drawable mUnselectedBorder;
	private Drawable mSelectedBorder;
	private int mLeftKeyCode;
	private int mRightKeyCode;
	private int mJumpKeyCode;
	private int mAttackKeyCode;
	private TextView mLeftText;
	private TextView mRightText;
	private TextView mJumpText;
	private TextView mAttackText;
	
	private class ConfigClickListener implements View.OnClickListener {
		private int mId;
		public ConfigClickListener(int id) {
			mId = id;
		}
		
		public void onClick(View v) {
			selectId(mId);
		}
		
	}
	
	public KeyboardConfigDialogPreference(Context context, AttributeSet attrs) {
		this(context, attrs, android.R.attr.dialogPreferenceStyle);
	}

	public KeyboardConfigDialogPreference(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		
		TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.KeyConfigPreference, defStyle, 0);
		mLeftPrefKey = a.getString(R.styleable.KeyConfigPreference_leftKey);
		mRightPrefKey = a.getString(R.styleable.KeyConfigPreference_rightKey);
		mJumpPrefKey = a.getString(R.styleable.KeyConfigPreference_jumpKey);
		mAttackPrefKey = a.getString(R.styleable.KeyConfigPreference_attackKey);
		
        a.recycle();
	}
	
	public KeyboardConfigDialogPreference(Context context) {
        this(context, null);
    }

	@Override
	protected void onBindDialogView(View view) {
		super.onBindDialogView(view);
		if (mSharedPrefs != null) {
			mLeftKeyCode = mSharedPrefs.getInt(mLeftPrefKey, KeyEvent.KEYCODE_DPAD_LEFT);
			mRightKeyCode = mSharedPrefs.getInt(mRightPrefKey, KeyEvent.KEYCODE_DPAD_RIGHT);
			mJumpKeyCode = mSharedPrefs.getInt(mJumpPrefKey, KeyEvent.KEYCODE_SPACE);
			mAttackKeyCode = mSharedPrefs.getInt(mAttackPrefKey, KeyEvent.KEYCODE_SHIFT_LEFT);
			
			
			mLeftText = (TextView)view.findViewById(R.id.key_left);
			mLeftText.setText(getKeyLabel(mLeftKeyCode));
			
			mRightText = (TextView)view.findViewById(R.id.key_right);
			mRightText.setText(getKeyLabel(mRightKeyCode));
			
			mJumpText = (TextView)view.findViewById(R.id.key_jump);
			mJumpText.setText(getKeyLabel(mJumpKeyCode));
			
			mAttackText = (TextView)view.findViewById(R.id.key_attack);
			mAttackText.setText(getKeyLabel(mAttackKeyCode));
			
			mLeftBorder = view.findViewById(R.id.left_border);
			mRightBorder = view.findViewById(R.id.right_border);
			mJumpBorder = view.findViewById(R.id.jump_border);
			mAttackBorder = view.findViewById(R.id.attack_border);
			
			mLeftBorder.setOnClickListener(new ConfigClickListener(R.id.key_left));
			mRightBorder.setOnClickListener(new ConfigClickListener(R.id.key_right));
			mJumpBorder.setOnClickListener(new ConfigClickListener(R.id.key_jump));
			mAttackBorder.setOnClickListener(new ConfigClickListener(R.id.key_attack));
			
			mUnselectedBorder = mContext.getResources().getDrawable(R.drawable.key_config_border);
			mSelectedBorder = mContext.getResources().getDrawable(R.drawable.key_config_border_active);
		}
		
		mListeningId = 0;
		
		
	}
	
	@Override
	protected void showDialog(Bundle state) {
		super.showDialog(state);
		getDialog().setOnKeyListener(this);
		getDialog().takeKeyEvents(true);
	}

	protected String getKeyLabel(int keycode) {
		String result = "Unknown Key";
		if (mKeyLabels == null) {
			mKeyLabels = mContext.getResources().getStringArray(R.array.keycode_labels);
		}
		
		if (keycode > 0 && keycode < mKeyLabels.length) {
			result = mKeyLabels[keycode - 1];
		}
		return result;
	}
	
	public void selectId(int id) {
		if (mListeningId != 0) {
			// unselect the current box
			View border = getConfigViewById(mListeningId);
			border.setBackgroundDrawable(mUnselectedBorder);
		}
		
		if (id == mListeningId || id == 0) {
			mListeningId = 0; // toggle off and end.
		} else {
			// select the new box
			View border = getConfigViewById(id);
			border.setBackgroundDrawable(mSelectedBorder);
			mListeningId = id;
		}
	}
	
	private View getConfigViewById(int id) {
		View config = null;
		switch(id) {
		case R.id.key_left:
			config = mLeftBorder;
			break;
		case R.id.key_right:
			config = mRightBorder;
			break;
		case R.id.key_jump:
			config = mJumpBorder;
			break;
		case R.id.key_attack:
			config = mAttackBorder;
			break;
		}
		
		return config;
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);
		
		if (positiveResult) {
			// save changes
			SharedPreferences.Editor editor = mSharedPrefs.edit();
			editor.putInt(mLeftPrefKey, mLeftKeyCode);
			editor.putInt(mRightPrefKey, mRightKeyCode);
			editor.putInt(mJumpPrefKey, mJumpKeyCode);
			editor.putInt(mAttackPrefKey, mAttackKeyCode);
			editor.commit();
		}
	}

	public void setPrefs(SharedPreferences sharedPreferences) {
		mSharedPrefs = sharedPreferences;
	}
	
	public void setContext(Context context) {
		mContext = context;
	}

	public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
		boolean eatKey = false;
		if (mListeningId != 0) {
			eatKey = true;
			switch (mListeningId) {
			case R.id.key_left:
				mLeftText.setText(getKeyLabel(keyCode));
				mLeftKeyCode = keyCode;
				break;
			case R.id.key_right:
				mRightText.setText(getKeyLabel(keyCode));
				mRightKeyCode = keyCode;
				break;
			case R.id.key_jump:
				mJumpText.setText(getKeyLabel(keyCode));
				mJumpKeyCode = keyCode;
				break;
			case R.id.key_attack:
				mAttackText.setText(getKeyLabel(keyCode));
				mAttackKeyCode = keyCode;
				break;
			}
			
			selectId(0);	// deselect the current config box;
		}
		return eatKey;
	}

}
