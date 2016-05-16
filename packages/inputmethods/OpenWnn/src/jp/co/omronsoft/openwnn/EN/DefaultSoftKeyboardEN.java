/*
 * Copyright (C) 2008-2012  OMRON SOFTWARE Co., Ltd.
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

package jp.co.omronsoft.openwnn.EN;

import jp.co.omronsoft.openwnn.*;
import android.content.SharedPreferences;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import jp.co.omronsoft.openwnn.Keyboard;

import android.util.Log;

/**
 * The default Software Keyboard class for English IME.
 *
 * @author Copyright (C) 2009 OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
public class DefaultSoftKeyboardEN extends DefaultSoftKeyboard {
    /** 12-key keyboard [PHONE MODE] */
    public static final int KEYCODE_PHONE  = -116;

	/**
     * Keyboards toggled by ALT key.
     * <br>
     * The normal keyboard(KEYMODE_EN_ALPHABET) and the number/symbol
     * keyboard(KEYMODE_EN_NUMBER) is active.  The phone number
     * keyboard(KEYMODE_EN_PHONE) is disabled.
     */
    private static final boolean[] TOGGLE_KEYBOARD = {true, true, false};

    /** Auto caps mode */
    private boolean mAutoCaps = false;

	/**
     * Default constructor
     */
    public DefaultSoftKeyboardEN() { }
	
    /**
     * Dismiss the pop-up keyboard.
     * <br>
     * Nothing will be done if no pop-up keyboard is displaying.
     */
    public void dismissPopupKeyboard() {
    	try {
    		if (mKeyboardView != null) {
    			mKeyboardView.handleBack();
    		}
    	} catch (Exception ex) {
    		/* ignore */
    	}
    }

    /** @see jp.co.omronsoft.openwnn.DefaultSoftKeyboard#createKeyboards */
    @Override protected void createKeyboards(OpenWnn parent) {
        mKeyboard = new Keyboard[3][2][4][2][7][2];
		
        Keyboard[][] keyList;
        /***********************************************************************
         * English
         ***********************************************************************/
        /* qwerty shift_off */
        keyList = mKeyboard[LANG_EN][PORTRAIT][KEYBOARD_QWERTY][KEYBOARD_SHIFT_OFF];
        keyList[KEYMODE_EN_ALPHABET][0] = new Keyboard(parent, R.xml.default_en_qwerty);
        keyList[KEYMODE_EN_NUMBER][0]   = new Keyboard(parent, R.xml.default_en_symbols);
        keyList[KEYMODE_EN_PHONE][0]    = new Keyboard(parent, R.xml.keyboard_12key_phone);
		
        /* qwerty shift_on */
        keyList = mKeyboard[LANG_EN][PORTRAIT][KEYBOARD_QWERTY][KEYBOARD_SHIFT_ON];
        keyList[KEYMODE_EN_ALPHABET][0] =
            mKeyboard[LANG_EN][PORTRAIT][KEYBOARD_QWERTY][KEYBOARD_SHIFT_OFF][KEYMODE_EN_ALPHABET][0];
        keyList[KEYMODE_EN_NUMBER][0]   = new Keyboard(parent, R.xml.default_en_symbols_shift);
        keyList[KEYMODE_EN_PHONE][0]    = new Keyboard(parent, R.xml.keyboard_12key_phone);
    }

    /**
     * Get the shift key state from the editor.
     *
     * @param editor	The information of editor
     * @return 		state ID of the shift key (0:off, 1:on)
     */
    private int getShiftKeyState(EditorInfo editor) {
        InputConnection connection = mWnn.getCurrentInputConnection();
        if (connection != null) {
            int caps = connection.getCursorCapsMode(editor.inputType);
            return (caps == 0) ? 0 : 1;
        } else {
            return 0;
        }
    }
	
    /**
     * Switch the keymode
     *
     * @param keyMode		Keymode
     */
	private void changeKeyMode(int keyMode) {
		Keyboard keyboard = super.getModeChangeKeyboard(keyMode);
    	if (keyboard != null) {
    		mCurrentKeyMode = keyMode;
    		super.changeKeyboard(keyboard);
		}
	}

    /***********************************************************************
     * from DefaultSoftKeyboard
     ***********************************************************************/
    /** @see jp.co.omronsoft.openwnn.DefaultSoftKeyboard#initView */
    @Override public View initView(OpenWnn parent, int width, int height) {
        View view = super.initView(parent, width, height);
	
    	/* default setting */
    	mCurrentLanguage     = LANG_EN;
    	mCurrentKeyboardType = KEYBOARD_QWERTY;
    	mShiftOn             = KEYBOARD_SHIFT_OFF;
    	mCurrentKeyMode      = KEYMODE_EN_ALPHABET;

    	Keyboard kbd = mKeyboard[mCurrentLanguage][mDisplayMode][mCurrentKeyboardType][mShiftOn][mCurrentKeyMode][0];
    	if (kbd == null) {
    		if(mDisplayMode == LANDSCAPE){
    			return view;
    		}
    		return null;
    	}
    	mCurrentKeyboard = null;
    	changeKeyboard(kbd);
    	return view;
    }
	
    /** @see jp.co.omronsoft.openwnn.DefaultSoftKeyboard#setPreferences */
    @Override public void setPreferences(SharedPreferences pref, EditorInfo editor) {
        super.setPreferences(pref, editor);

        /* auto caps mode */
        mAutoCaps = pref.getBoolean("auto_caps", true);

        switch (editor.inputType & EditorInfo.TYPE_MASK_CLASS) {
        case EditorInfo.TYPE_CLASS_NUMBER:
        case EditorInfo.TYPE_CLASS_DATETIME:
            mCurrentLanguage     = LANG_EN;
            mCurrentKeyboardType = KEYBOARD_QWERTY;
            mShiftOn             = KEYBOARD_SHIFT_OFF;
            mCurrentKeyMode      = KEYMODE_EN_NUMBER;
				
            Keyboard kbdn =
                mKeyboard[mCurrentLanguage][mDisplayMode][mCurrentKeyboardType][mShiftOn][mCurrentKeyMode][0];
				
            changeKeyboard(kbdn);           
            break;
				
        case EditorInfo.TYPE_CLASS_PHONE:
            mCurrentLanguage     = LANG_EN;
            mCurrentKeyboardType = KEYBOARD_QWERTY;
            mShiftOn             = KEYBOARD_SHIFT_OFF;
            mCurrentKeyMode      = KEYMODE_EN_PHONE;
				
            Keyboard kbdp =
                mKeyboard[mCurrentLanguage][mDisplayMode][mCurrentKeyboardType][mShiftOn][mCurrentKeyMode][0];
				
            changeKeyboard(kbdp);           
				
            break;
				
        default:
            mCurrentLanguage     = LANG_EN;
            mCurrentKeyboardType = KEYBOARD_QWERTY;
            mShiftOn             = KEYBOARD_SHIFT_OFF;
            mCurrentKeyMode      = KEYMODE_EN_ALPHABET;
			
            Keyboard kbdq =
                mKeyboard[mCurrentLanguage][mDisplayMode][mCurrentKeyboardType][mShiftOn][mCurrentKeyMode][0];
			
            changeKeyboard(kbdq);           
             break;
        }

        int shift = (mAutoCaps)? getShiftKeyState(mWnn.getCurrentInputEditorInfo()) : 0;
        if (shift != mShiftOn) {
            Keyboard kbd = getShiftChangeKeyboard(shift);
            mShiftOn = shift;
            changeKeyboard(kbd);
        }
    }
    
    /** @see jp.co.omronsoft.openwnn.DefaultSoftKeyboard#onKey */
    @Override public void onKey(int primaryCode, int[] keyCodes) {
        switch (primaryCode) {
        case KEYCODE_QWERTY_HAN_ALPHA:
            this.changeKeyMode(KEYMODE_EN_ALPHABET);
            break;

        case KEYCODE_QWERTY_HAN_NUM:
            this.changeKeyMode(KEYMODE_EN_NUMBER);
            break;

        case KEYCODE_PHONE:
            this.changeKeyMode(KEYMODE_EN_PHONE);
            break;

        case KEYCODE_QWERTY_EMOJI:
            mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.LIST_SYMBOLS));
            break;

        case KEYCODE_QWERTY_TOGGLE_MODE:
            switch(mCurrentKeyMode){
            case KEYMODE_EN_ALPHABET:
                if (TOGGLE_KEYBOARD[KEYMODE_EN_NUMBER]){
                    mCurrentKeyMode = KEYMODE_EN_NUMBER;
                } else if (TOGGLE_KEYBOARD[KEYMODE_EN_PHONE]) {
                    mCurrentKeyMode = KEYMODE_EN_PHONE;
                }
                break;
            case KEYMODE_EN_NUMBER:
                if (TOGGLE_KEYBOARD[KEYMODE_EN_PHONE]) {
                    mCurrentKeyMode = KEYMODE_EN_PHONE;
                } else if(TOGGLE_KEYBOARD[KEYMODE_EN_ALPHABET]) {
                    mCurrentKeyMode = KEYMODE_EN_ALPHABET;
                }        			
                break;
            case KEYMODE_EN_PHONE:
                if (TOGGLE_KEYBOARD[KEYMODE_EN_ALPHABET]) {
                    mCurrentKeyMode = KEYMODE_EN_ALPHABET;
                } else if (TOGGLE_KEYBOARD[KEYMODE_EN_NUMBER]) {
                    mCurrentKeyMode = KEYMODE_EN_NUMBER;
                }
                break;
            }
            Keyboard kbdp =
                mKeyboard[mCurrentLanguage][mDisplayMode][mCurrentKeyboardType][mShiftOn][mCurrentKeyMode][0];
            super.changeKeyboard(kbdp);	
            break;

        case DefaultSoftKeyboard.KEYCODE_QWERTY_BACKSPACE:
        case DefaultSoftKeyboard.KEYCODE_JP12_BACKSPACE:
            mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.INPUT_SOFT_KEY,
                                          new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL)));
            break;
            
        case DefaultSoftKeyboard.KEYCODE_QWERTY_SHIFT:
            toggleShiftLock();
            break;
            
        case DefaultSoftKeyboard.KEYCODE_QWERTY_ALT:
            processAltKey();
            break;

        case KEYCODE_QWERTY_ENTER:
        case KEYCODE_JP12_ENTER:
            mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.INPUT_SOFT_KEY,
                                          new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)));
            break;

        case KEYCODE_JP12_LEFT:
            mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.INPUT_SOFT_KEY,
                                          new KeyEvent(KeyEvent.ACTION_DOWN,
                                                       KeyEvent.KEYCODE_DPAD_LEFT)));
            break;
            
        case KEYCODE_JP12_RIGHT:
            mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.INPUT_SOFT_KEY,
                                          new KeyEvent(KeyEvent.ACTION_DOWN,
                                                       KeyEvent.KEYCODE_DPAD_RIGHT)));
        default:
            if (primaryCode >= 0) {
                if (mKeyboardView.isShifted()) {
                    primaryCode = Character.toUpperCase(primaryCode);
                }
                mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.INPUT_CHAR, (char)primaryCode));
            }
        }
		
        /* update shift key's state */
        if (!mCapsLock && primaryCode != KEYCODE_QWERTY_SHIFT) {
            if(mCurrentKeyMode != KEYMODE_EN_NUMBER){
                int shift = (mAutoCaps)? getShiftKeyState(mWnn.getCurrentInputEditorInfo()) : 0;
                if (shift != mShiftOn) {
                    Keyboard kbd = getShiftChangeKeyboard(shift);
                    mShiftOn = shift;
                    changeKeyboard(kbd);
                }
            }else{
                mShiftOn = KEYBOARD_SHIFT_OFF;
                Keyboard kbd = getShiftChangeKeyboard(mShiftOn);
                changeKeyboard(kbd);
            }
        }
    }
}



