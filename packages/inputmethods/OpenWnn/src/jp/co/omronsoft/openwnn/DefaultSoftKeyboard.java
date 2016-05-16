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

package jp.co.omronsoft.openwnn;

import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.TextView;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.content.res.*;
import android.os.Vibrator;
import android.media.MediaPlayer;
import android.content.Context;

import android.util.Log;

import jp.co.omronsoft.openwnn.Keyboard;
import jp.co.omronsoft.openwnn.KeyboardView;
import jp.co.omronsoft.openwnn.KeyboardView.OnKeyboardActionListener;

/**
 * The default software keyboard class.
 *
 * @author Copyright (C) 2009-2011 OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
public class DefaultSoftKeyboard implements InputViewManager, KeyboardView.OnKeyboardActionListener {
    /*
     *----------------------------------------------------------------------
     * key codes for a software keyboard
     *----------------------------------------------------------------------
     */
    /** Change the keyboard language */
    public static final int KEYCODE_CHANGE_LANG = -500;

    /* for Japanese 12-key keyboard */
    /** Japanese 12-key keyboard [1] */
    public static final int KEYCODE_JP12_1 = -201;
    /** Japanese 12-key keyboard [2] */
    public static final int KEYCODE_JP12_2 = -202;
    /** Japanese 12-key keyboard [3] */
    public static final int KEYCODE_JP12_3 = -203;
    /** Japanese 12-key keyboard [4] */
    public static final int KEYCODE_JP12_4 = -204;
    /** Japanese 12-key keyboard [5] */
    public static final int KEYCODE_JP12_5 = -205;
    /** Japanese 12-key keyboard [6] */
    public static final int KEYCODE_JP12_6 = -206;
    /** Japanese 12-key keyboard [7] */
    public static final int KEYCODE_JP12_7 = -207;
    /** Japanese 12-key keyboard [8] */
    public static final int KEYCODE_JP12_8 = -208;
    /** Japanese 12-key keyboard [9] */
    public static final int KEYCODE_JP12_9 = -209;
    /** Japanese 12-key keyboard [0] */
    public static final int KEYCODE_JP12_0 = -210;
    /** Japanese 12-key keyboard [#] */
    public static final int KEYCODE_JP12_SHARP = -211;
    /** Japanese 12-key keyboard [*] */
    public static final int KEYCODE_JP12_ASTER = -213;
    /** Japanese 12-key keyboard [DEL] */
    public static final int KEYCODE_JP12_BACKSPACE = -214;
    /** Japanese 12-key keyboard [SPACE] */
    public static final int KEYCODE_JP12_SPACE = -215;
    /** Japanese 12-key keyboard [ENTER] */
    public static final int KEYCODE_JP12_ENTER = -216;
    /** Japanese 12-key keyboard [RIGHT ARROW] */
    public static final int KEYCODE_JP12_RIGHT = -217;
    /** Japanese 12-key keyboard [LEFT ARROW] */
    public static final int KEYCODE_JP12_LEFT = -218;
    /** Japanese 12-key keyboard [REVERSE TOGGLE] */
    public static final int KEYCODE_JP12_REVERSE = -219;
    /** Japanese 12-key keyboard [CLOSE] */
    public static final int KEYCODE_JP12_CLOSE   = -220;
    /** Japanese 12-key keyboard [KEYBOARD TYPE CHANGE] */
    public static final int KEYCODE_JP12_KBD   = -221;
    /** Japanese 12-key keyboard [EMOJI] */
    public static final int KEYCODE_JP12_EMOJI      = -222;
    /** Japanese 12-key keyboard [FULL-WIDTH HIRAGANA MODE] */
    public static final int KEYCODE_JP12_ZEN_HIRA   = -223;
    /** Japanese 12-key keyboard [FULL-WIDTH NUMBER MODE] */
    public static final int KEYCODE_JP12_ZEN_NUM    = -224;
    /** Japanese 12-key keyboard [FULL-WIDTH ALPHABET MODE] */
    public static final int KEYCODE_JP12_ZEN_ALPHA  = -225;
    /** Japanese 12-key keyboard [FULL-WIDTH KATAKANA MODE] */
    public static final int KEYCODE_JP12_ZEN_KATA   = -226;
    /** Japanese 12-key keyboard [HALF-WIDTH KATAKANA MODE] */
    public static final int KEYCODE_JP12_HAN_KATA   = -227;
    /** Japanese 12-key keyboard [HALF-WIDTH NUMBER MODE] */
    public static final int KEYCODE_JP12_HAN_NUM    = -228;
    /** Japanese 12-key keyboard [HALF-WIDTH ALPHABET MODE] */
    public static final int KEYCODE_JP12_HAN_ALPHA  = -229;
    /** Japanese 12-key keyboard [MODE TOOGLE CHANGE] */
    public static final int KEYCODE_JP12_TOGGLE_MODE = -230;

    /** Key code for symbol keyboard alt key */
    public static final int KEYCODE_4KEY_MODE        = -300;
    /** Key code for symbol keyboard up key */
    public static final int KEYCODE_4KEY_UP          = -301;
    /** Key code for symbol keyboard down key */
    public static final int KEYCODE_4KEY_DOWN        = -302;
    /** Key code for symbol keyboard del key */
    public static final int KEYCODE_4KEY_CLEAR       = -303;

    /* for Qwerty keyboard */
    /** Qwerty keyboard [DEL] */
    public static final int KEYCODE_QWERTY_BACKSPACE = -100;
    /** Qwerty keyboard [ENTER] */
    public static final int KEYCODE_QWERTY_ENTER = -101;
    /** Qwerty keyboard [SHIFT] */
    public static final int KEYCODE_QWERTY_SHIFT = Keyboard.KEYCODE_SHIFT;
    /** Qwerty keyboard [ALT] */
    public static final int KEYCODE_QWERTY_ALT   = -103;
    /** Qwerty keyboard [KEYBOARD TYPE CHANGE] */
    public static final int KEYCODE_QWERTY_KBD   = -104;
    /** Qwerty keyboard [CLOSE] */
    public static final int KEYCODE_QWERTY_CLOSE = -105;
    /** Japanese Qwerty keyboard [EMOJI] */
    public static final int KEYCODE_QWERTY_EMOJI = -106;
    /** Japanese Qwerty keyboard [FULL-WIDTH HIRAGANA MODE] */
    public static final int KEYCODE_QWERTY_ZEN_HIRA   = -107;
    /** Japanese Qwerty keyboard [FULL-WIDTH NUMBER MODE] */
    public static final int KEYCODE_QWERTY_ZEN_NUM    = -108;
    /** Japanese Qwerty keyboard [FULL-WIDTH ALPHABET MODE] */
    public static final int KEYCODE_QWERTY_ZEN_ALPHA  = -109;
    /** Japanese Qwerty keyboard [FULL-WIDTH KATAKANA MODE] */
    public static final int KEYCODE_QWERTY_ZEN_KATA   = -110;
    /** Japanese Qwerty keyboard [HALF-WIDTH KATAKANA MODE] */
    public static final int KEYCODE_QWERTY_HAN_KATA   = -111;
    /** Qwerty keyboard [NUMBER MODE] */
    public static final int KEYCODE_QWERTY_HAN_NUM    = -112;
    /** Qwerty keyboard [ALPHABET MODE] */
    public static final int KEYCODE_QWERTY_HAN_ALPHA  = -113;
    /** Qwerty keyboard [MODE TOOGLE CHANGE] */
    public static final int KEYCODE_QWERTY_TOGGLE_MODE = -114;
    /** Qwerty keyboard [PINYIN MODE] */
    public static final int KEYCODE_QWERTY_PINYIN  = -115;
    
    /** OpenWnn instance which hold this software keyboard*/
    protected OpenWnn      mWnn;
    
    /** Current keyboard view */
    protected KeyboardView mKeyboardView;
    
    /** View objects (main side) */
    protected BaseInputView mMainView;
    /** View objects (sub side) */
    protected ViewGroup mSubView;
    
    /** Current keyboard definition */
    protected Keyboard mCurrentKeyboard;
    
    /** Caps lock state */
    protected boolean mCapsLock;
    
    /** Input restraint */
    protected boolean mDisableKeyInput = true;
    /**
     * Keyboard surfaces 
     * <br>
     * Keyboard[language][portrait/landscape][keyboard type][shift off/on][key-mode]
     */
    protected Keyboard[][][][][][] mKeyboard;

    /* languages */
    /** Current language */
    protected int mCurrentLanguage;
    /** Language (English) */
    public static final int LANG_EN  = 0;
    /** Language (Japanese) */
    public static final int LANG_JA  = 1;
    /** Language (Chinese) */
    public static final int LANG_CN  = 2;

    /* portrait/landscape */
    /** State of the display */
    protected int mDisplayMode = 0;
    /** Display mode (Portrait) */
    public static final int PORTRAIT  = 0;
    /** Display mode (Landscape) */
    public static final int LANDSCAPE = 1;

    /* keyboard type */
    /** Current keyboard type */
    protected int mCurrentKeyboardType;
    /** Keyboard (QWERTY keyboard) */
    public static final int KEYBOARD_QWERTY  = 0;
    /** Keyboard (12-keys keyboard) */
    public static final int KEYBOARD_12KEY   = 1;
    /** State of the shift key */
    protected int mShiftOn = 0;
    /** Shift key off */
    public static final int KEYBOARD_SHIFT_OFF = 0;
    /** Shift key on */
    public static final int KEYBOARD_SHIFT_ON  = 1;

    /* key-modes */
    /** Current key-mode */
    protected int mCurrentKeyMode;

    /* key-modes for English */
    /** English key-mode (alphabet) */
    public static final int KEYMODE_EN_ALPHABET = 0;
    /** English key-mode (number) */
    public static final int KEYMODE_EN_NUMBER   = 1;
    /** English key-mode (phone number) */
    public static final int KEYMODE_EN_PHONE    = 2;

    /* key-modes for Japanese */
    /** Japanese key-mode (Full-width Hiragana) */
    public static final int KEYMODE_JA_FULL_HIRAGANA = 0;
    /** Japanese key-mode (Full-width alphabet) */
    public static final int KEYMODE_JA_FULL_ALPHABET = 1;
    /** Japanese key-mode (Full-width number) */
    public static final int KEYMODE_JA_FULL_NUMBER   = 2;
    /** Japanese key-mode (Full-width Katakana) */
    public static final int KEYMODE_JA_FULL_KATAKANA = 3;
    /** Japanese key-mode (Half-width alphabet) */
    public static final int KEYMODE_JA_HALF_ALPHABET = 4;
    /** Japanese key-mode (Half-width number) */
    public static final int KEYMODE_JA_HALF_NUMBER   = 5;
    /** Japanese key-mode (Half-width Katakana) */
    public static final int KEYMODE_JA_HALF_KATAKANA = 6;
    /** Japanese key-mode (Half-width phone number) */
    public static final int KEYMODE_JA_HALF_PHONE    = 7;

    /* key-modes for Chinese */
    /** Chinese key-mode (pinyin) */
    public static final int KEYMODE_CN_PINYIN   = 0;
    /** Chinese key-mode (Full-width number) */
    public static final int KEYMODE_CN_FULL_NUMBER   = 1;
    /** Chinese key-mode (alphabet) */
    public static final int KEYMODE_CN_ALPHABET = 2;
    /** Chinese key-mode (phone) */
    public static final int KEYMODE_CN_PHONE    = 3;
    /** Chinese key-mode (Half-width number) */
    public static final int KEYMODE_CN_HALF_NUMBER   = 4;
    
    /* key-modes for HARD */
    /** HARD key-mode (SHIFT_OFF_ALT_OFF) */
    public static final int HARD_KEYMODE_SHIFT_OFF_ALT_OFF     = 2;
    /** HARD key-mode (SHIFT_ON_ALT_OFF) */
    public static final int HARD_KEYMODE_SHIFT_ON_ALT_OFF      = 3;
    /** HARD key-mode (SHIFT_OFF_ALT_ON) */
    public static final int HARD_KEYMODE_SHIFT_OFF_ALT_ON      = 4;
    /** HARD key-mode (SHIFT_ON_ALT_ON) */
    public static final int HARD_KEYMODE_SHIFT_ON_ALT_ON       = 5;
    /** HARD key-mode (SHIFT_LOCK_ALT_OFF) */
    public static final int HARD_KEYMODE_SHIFT_LOCK_ALT_OFF    = 6;
    /** HARD key-mode (SHIFT_LOCK_ALT_ON) */
    public static final int HARD_KEYMODE_SHIFT_LOCK_ALT_ON     = 7;
    /** HARD key-mode (SHIFT_LOCK_ALT_LOCK) */
    public static final int HARD_KEYMODE_SHIFT_LOCK_ALT_LOCK   = 8;
    /** HARD key-mode (SHIFT_OFF_ALT_LOCK) */
    public static final int HARD_KEYMODE_SHIFT_OFF_ALT_LOCK    = 9;
    /** HARD key-mode (SHIFT_ON_ALT_LOCK) */
    public static final int HARD_KEYMODE_SHIFT_ON_ALT_LOCK     = 10;

    /** Whether the H/W keyboard is hidden. */
    protected boolean mHardKeyboardHidden = true;

    /** Whether the H/W 12key keyboard. */
    protected boolean mEnableHardware12Keyboard = false;

    /** Symbol keyboard */
    protected Keyboard mSymbolKeyboard;

    /** Symbol keyboard state */
    protected boolean mIsSymbolKeyboard = false;

    /**
     * Status of the composing text
     * <br>
     * {@code true} if there is no composing text.
     */
    protected boolean mNoInput = true;
    
    /** Vibratior for key click vibration */
    protected Vibrator mVibrator = null;
    
    /** MediaPlayer for key click sound */
    protected MediaPlayer mSound = null;
    
    /** Key toggle cycle table currently using */
    protected String[] mCurrentCycleTable;

    /** Event listener for symbol keyboard */
    private OnKeyboardActionListener mSymbolOnKeyboardAction = new OnKeyboardActionListener() {
        public void onKey(int primaryCode, int[] keyCodes) {
            switch (primaryCode) {
            case KEYCODE_4KEY_MODE:
                mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.INPUT_KEY,
                                              new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK)));
                break;

            case KEYCODE_4KEY_UP:
                mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.CANDIDATE_VIEW_SCROLL_UP));
                break;

            case KEYCODE_4KEY_DOWN:
                mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.CANDIDATE_VIEW_SCROLL_DOWN));
                break;

            case KEYCODE_4KEY_CLEAR:
                InputConnection connection = mWnn.getCurrentInputConnection();
                if (connection != null) {
                    connection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
                }
                return;

            default:
                break;
            }
        }
        public void onPress(int primaryCode) {
            playSoundAndVibration();
        }
        public void onText(CharSequence text) { }
        public void swipeLeft() { }
        public void swipeRight() { }
        public void swipeUp() { }
        public void swipeDown() { }
        public void onRelease(int primaryCode) { }
        public boolean onLongPress(Keyboard.Key key) {
            switch (key.codes[0]) {
            case KEYCODE_4KEY_UP:
                mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.CANDIDATE_VIEW_SCROLL_FULL_UP));
                return true;

            case KEYCODE_4KEY_DOWN:
                mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.CANDIDATE_VIEW_SCROLL_FULL_DOWN));
                return true;

            default:
                break;
            }
            return false;
        }
    };

    /**
     * Constructor
     */
    public DefaultSoftKeyboard() { }

    /**
     * Create keyboard views
     *
     * @param parent   OpenWnn using the keyboards.
     */
    protected void createKeyboards(OpenWnn parent) {
        /*
         *  Keyboard[# of Languages][portrait/landscape][# of keyboard type]
         *          [shift off/on][max # of key-modes][non-input/input]
         */
        mKeyboard = new Keyboard[3][2][4][2][7][2];
    }

    /**
     * Get the keyboard changed the specified shift state.
     *
     * @param shift     Shift state
     * @return          Keyboard view
     */
    protected Keyboard getShiftChangeKeyboard(int shift) {
        try {
            Keyboard[] kbd = mKeyboard[mCurrentLanguage][mDisplayMode][mCurrentKeyboardType][shift][mCurrentKeyMode];

            if (!mNoInput && kbd[1] != null) {
                return kbd[1];
            }
            return kbd[0];
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Get the keyboard changed the specified input mode.
     *
     * @param mode      Input mode
     * @return          Keyboard view
     */
    protected Keyboard getModeChangeKeyboard(int mode) {
        try {
            Keyboard[] kbd = mKeyboard[mCurrentLanguage][mDisplayMode][mCurrentKeyboardType][mShiftOn][mode];

            if (!mNoInput && kbd[1] != null) {
                return kbd[1];
            }
            return kbd[0];
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Get the keyboard changed the specified keyboard type
     *
     * @param type      Keyboard type
     * @return          Keyboard view
     */
    protected Keyboard getTypeChangeKeyboard(int type) {
        try {
            Keyboard[] kbd = mKeyboard[mCurrentLanguage][mDisplayMode][type][mShiftOn][mCurrentKeyMode];

            if (!mNoInput && kbd[1] != null) {
                return kbd[1];
            }
            return kbd[0];
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Get the keyboard when some characters are input or no character is input.
     *
     * @param inputed   {@code true} if some characters are inputed; {@code false} if no character is inputed.
     * @return          Keyboard view
     */
    protected Keyboard getKeyboardInputed(boolean inputed) {
        try {
            Keyboard[] kbd = mKeyboard[mCurrentLanguage][mDisplayMode][mCurrentKeyboardType][mShiftOn][mCurrentKeyMode];

            if (inputed && kbd[1] != null) {
                return kbd[1];
            }
            return kbd[0];
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Change the circulative key-mode.
     */
    protected void toggleKeyMode() {
        /* unlock shift */
        mShiftOn = KEYBOARD_SHIFT_OFF;

        /* search next defined key-mode */
        Keyboard[][] keyboardList = mKeyboard[mCurrentLanguage][mDisplayMode][mCurrentKeyboardType][mShiftOn];
        do {
            if (++mCurrentKeyMode >= keyboardList.length) {
                mCurrentKeyMode = 0;
            }
        } while (keyboardList[mCurrentKeyMode][0] == null);

        Keyboard kbd;
        if (!mNoInput && keyboardList[mCurrentKeyMode][1] != null) {
            kbd = keyboardList[mCurrentKeyMode][1];
        } else {
            kbd = keyboardList[mCurrentKeyMode][0];
        }
        changeKeyboard(kbd);

        mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.CHANGE_MODE,
                                      OpenWnnEvent.Mode.DEFAULT));
    }

    /**
     * Toggle change the shift lock state.
     */
    protected void toggleShiftLock() {
        if (mShiftOn == 0) {
            /* turn shift on */
            Keyboard newKeyboard = getShiftChangeKeyboard(KEYBOARD_SHIFT_ON);
            if (newKeyboard != null) {
                mShiftOn = 1;
                changeKeyboard(newKeyboard);
            }
            mCapsLock = true;
        } else {
            /* turn shift off */
            Keyboard newKeyboard = getShiftChangeKeyboard(KEYBOARD_SHIFT_OFF);
            if (newKeyboard != null) {
                mShiftOn = 0;
                changeKeyboard(newKeyboard);
            }
            mCapsLock = false;
        }
    }

    /**
     * Handling Alt key event.
     */
    protected void processAltKey() {
        /* invalid if it is not qwerty mode */
        if (mCurrentKeyboardType != KEYBOARD_QWERTY) {
            return;
        }

        int mode = -1;
        int keymode = mCurrentKeyMode;
        switch (mCurrentLanguage) {
        case LANG_EN:
            if (keymode == KEYMODE_EN_ALPHABET) {
                mode = KEYMODE_EN_NUMBER;
            } else if (keymode == KEYMODE_EN_NUMBER) {
                mode = KEYMODE_EN_ALPHABET;
            }
            break;

        case LANG_JA:
            if (keymode == KEYMODE_JA_HALF_ALPHABET) {
                mode = KEYMODE_JA_HALF_NUMBER;
            } else if (keymode == KEYMODE_JA_HALF_NUMBER) {
                mode = KEYMODE_JA_HALF_ALPHABET;
            } else if (keymode == KEYMODE_JA_FULL_ALPHABET) {
                mode = KEYMODE_JA_FULL_NUMBER;
            } else if (keymode == KEYMODE_JA_FULL_NUMBER) {
                mode = KEYMODE_JA_FULL_ALPHABET;
            }
            break;

        default:
            /* invalid */
        }

        if (mode >= 0) {
            Keyboard kbd = getModeChangeKeyboard(mode);
            if (kbd != null) {
                mCurrentKeyMode = mode;
                changeKeyboard(kbd);
            }
        }
    }

    /**
     * Change the keyboard type.
     *
     * @param type  Type of the keyboard
     * @see jp.co.omronsoft.openwnn.DefaultSoftKeyboard#KEYBOARD_QWERTY
     * @see jp.co.omronsoft.openwnn.DefaultSoftKeyboard#KEYBOARD_12KEY
     */
    public void changeKeyboardType(int type) {
        /* ignore invalid parameter */
        if (type != KEYBOARD_QWERTY && type != KEYBOARD_12KEY) {
            return;
        }
        
        /* change keyboard view */
        Keyboard kbd = getTypeChangeKeyboard(type);
        if (kbd != null) {
            mCurrentKeyboardType = type;
            changeKeyboard(kbd);
        }

        /* notice that the keyboard is changed */
        mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.CHANGE_MODE,
                                      OpenWnnEvent.Mode.DEFAULT));
    }

    /**
     * Change the keyboard.
     *
     * @param keyboard  The new keyboard
     * @return          {@code true} if the keyboard is changed; {@code false} if not changed.
     */
    protected boolean changeKeyboard(Keyboard keyboard) {

        if (keyboard == null) {
            return false;
        }
        if (mCurrentKeyboard != keyboard) {
            mKeyboardView.setKeyboard(keyboard);
            mKeyboardView.setShifted((mShiftOn == 0) ? false : true);
            mCurrentKeyboard = keyboard;
            return true;
        } else {
            mKeyboardView.setShifted((mShiftOn == 0) ? false : true);
            return false;
        }
    }
    /** @see jp.co.omronsoft.openwnn.InputViewManager#initView */
    public View initView(OpenWnn parent, int width, int height) {
        mWnn = parent;
        mDisplayMode = 
            (parent.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
            ? LANDSCAPE : PORTRAIT;

        /*
         * create keyboards & the view.
         * To re-display the input view when the display mode is changed portrait <-> landscape,
         * create keyboards every time.
         */
        createKeyboards(parent);

        /* create symbol keyboard */
        mSymbolKeyboard = new Keyboard(parent, R.xml.keyboard_4key);

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(parent);
        String skin = pref.getString("keyboard_skin",
                                     mWnn.getResources().getString(R.string.keyboard_skin_id_default));
        int id = parent.getResources().getIdentifier(skin, "layout", "jp.co.omronsoft.openwnn");

        mKeyboardView = (KeyboardView) mWnn.getLayoutInflater().inflate(id, null);
        mKeyboardView.setOnKeyboardActionListener(this);
        mCurrentKeyboard = null;

        mMainView = (BaseInputView) parent.getLayoutInflater().inflate(R.layout.keyboard_default_main, null);
        mSubView = (ViewGroup) parent.getLayoutInflater().inflate(R.layout.keyboard_default_sub, null);

        if (!mHardKeyboardHidden) {
            if (!mEnableHardware12Keyboard) {
                mMainView.addView(mSubView);
            }
        } else if (mKeyboardView != null) {
            mMainView.addView(mKeyboardView);
        }

        return mMainView;
    }
    
    /**
     * Update the SHFIT/ALT keys indicator.
     * 
     * @param mode  The state of SHIFT/ALT keys.
     */
    public void updateIndicator(int mode) {
        Resources res = mWnn.getResources();
        TextView text1 = (TextView)mSubView.findViewById(R.id.shift);
        TextView text2 = (TextView)mSubView.findViewById(R.id.alt);

        switch (mode) {
        case HARD_KEYMODE_SHIFT_OFF_ALT_OFF:
            text1.setTextColor(res.getColor(R.color.indicator_textcolor_caps_off));
            text2.setTextColor(res.getColor(R.color.indicator_textcolor_alt_off));
            text1.setBackgroundColor(res.getColor(R.color.indicator_textbackground_default));
            text2.setBackgroundColor(res.getColor(R.color.indicator_textbackground_default));
            break;
        case HARD_KEYMODE_SHIFT_ON_ALT_OFF:
            text1.setTextColor(res.getColor(R.color.indicator_textcolor_caps_on));
            text2.setTextColor(res.getColor(R.color.indicator_textcolor_alt_off));
            text1.setBackgroundColor(res.getColor(R.color.indicator_textbackground_default));
            text2.setBackgroundColor(res.getColor(R.color.indicator_textbackground_default));
            break;
        case HARD_KEYMODE_SHIFT_LOCK_ALT_OFF:
            text1.setTextColor(res.getColor(R.color.indicator_textcolor_caps_lock));
            text2.setTextColor(res.getColor(R.color.indicator_textcolor_alt_off));
            text1.setBackgroundColor(res.getColor(R.color.indicator_background_lock_caps));
            text2.setBackgroundColor(res.getColor(R.color.indicator_textbackground_default));
            break;
        case HARD_KEYMODE_SHIFT_OFF_ALT_ON:
            text1.setTextColor(res.getColor(R.color.indicator_textcolor_caps_off));
            text2.setTextColor(res.getColor(R.color.indicator_textcolor_alt_on));
            text1.setBackgroundColor(res.getColor(R.color.indicator_textbackground_default));
            text2.setBackgroundColor(res.getColor(R.color.indicator_textbackground_default));
            break;
        case HARD_KEYMODE_SHIFT_OFF_ALT_LOCK:
            text1.setTextColor(res.getColor(R.color.indicator_textcolor_caps_off));
            text2.setTextColor(res.getColor(R.color.indicator_textcolor_alt_lock));
            text1.setBackgroundColor(res.getColor(R.color.indicator_textbackground_default));
            text2.setBackgroundColor(res.getColor(R.color.indicator_background_lock_alt));
            break;
        case HARD_KEYMODE_SHIFT_ON_ALT_ON:
            text1.setTextColor(res.getColor(R.color.indicator_textcolor_caps_on));
            text2.setTextColor(res.getColor(R.color.indicator_textcolor_alt_on));
            text1.setBackgroundColor(res.getColor(R.color.indicator_textbackground_default));
            text2.setBackgroundColor(res.getColor(R.color.indicator_textbackground_default));
            break;
        case HARD_KEYMODE_SHIFT_ON_ALT_LOCK:
            text1.setTextColor(res.getColor(R.color.indicator_textcolor_caps_on));
            text2.setTextColor(res.getColor(R.color.indicator_textcolor_alt_lock));
            text1.setBackgroundColor(res.getColor(R.color.indicator_textbackground_default));
            text2.setBackgroundColor(res.getColor(R.color.indicator_background_lock_alt));
            break;
        case HARD_KEYMODE_SHIFT_LOCK_ALT_ON:
            text1.setTextColor(res.getColor(R.color.indicator_textcolor_caps_lock));
            text2.setTextColor(res.getColor(R.color.indicator_textcolor_alt_on));
            text1.setBackgroundColor(res.getColor(R.color.indicator_background_lock_caps));
            text2.setBackgroundColor(res.getColor(R.color.indicator_textbackground_default));
            break;
        case HARD_KEYMODE_SHIFT_LOCK_ALT_LOCK:
            text1.setTextColor(res.getColor(R.color.indicator_textcolor_caps_lock));
            text2.setTextColor(res.getColor(R.color.indicator_textcolor_alt_lock));
            text1.setBackgroundColor(res.getColor(R.color.indicator_background_lock_caps));
            text2.setBackgroundColor(res.getColor(R.color.indicator_background_lock_alt));
            break;
        default:
            text1.setTextColor(res.getColor(R.color.indicator_textcolor_caps_off));
            text2.setTextColor(res.getColor(R.color.indicator_textcolor_alt_off));
            text1.setBackgroundColor(res.getColor(R.color.indicator_textbackground_default));
            text2.setBackgroundColor(res.getColor(R.color.indicator_textbackground_default));
            break;
        }
        return;
    }
    
    /** @see jp.co.omronsoft.openwnn.InputViewManager#getCurrentView */
    public View getCurrentView() {
        return mMainView;
    }

    /** @see jp.co.omronsoft.openwnn.InputViewManager#onUpdateState */
    public void onUpdateState(OpenWnn parent) {
        try {
            if (parent.mComposingText.size(1) == 0) {
                if (!mNoInput) {
                    /* when the mode changed to "no input" */
                    mNoInput = true;
                    Keyboard newKeyboard = getKeyboardInputed(false);
                    if (mCurrentKeyboard != newKeyboard) {
                        changeKeyboard(newKeyboard);
                    }
                }
            } else {
                if (mNoInput) {
                    /* when the mode changed to "input some characters" */
                    mNoInput = false;
                    Keyboard newKeyboard = getKeyboardInputed(true);
                    if (mCurrentKeyboard != newKeyboard) {
                        changeKeyboard(newKeyboard);
                    }
                }
            }
        } catch (Exception ex) {
        }
    }

    /** @see jp.co.omronsoft.openwnn.InputViewManager#setPreferences */
    public void setPreferences(SharedPreferences pref, EditorInfo editor) {

        /* vibrator */
        try {
            if (pref.getBoolean("key_vibration", false)) {
                mVibrator = (Vibrator)mWnn.getSystemService(Context.VIBRATOR_SERVICE);
            } else {
                mVibrator = null;
            }
        } catch (Exception ex) {
            Log.d("OpenWnn", "NO VIBRATOR");
        }

        /* sound */
        try {
            if (pref.getBoolean("key_sound", false)) {
                mSound = MediaPlayer.create(mWnn, R.raw.type);
            } else {
                mSound = null;
            }
        } catch (Exception ex) {
            Log.d("OpenWnn", "NO SOUND");
        }

        /* pop-up preview */
        if (OpenWnn.isXLarge()) {
            mKeyboardView.setPreviewEnabled(false);
        } else {
            mKeyboardView.setPreviewEnabled(pref.getBoolean("popup_preview", true));
            mKeyboardView.clearWindowInfo();
        }

    }

    /** @see jp.co.omronsoft.openwnn.InputViewManager#closing */
    public void closing() {
        if (mKeyboardView != null) {
            mKeyboardView.closing();
        }
        mDisableKeyInput = true;
    }

    /** @see jp.co.omronsoft.openwnn.InputViewManager#showInputView */
    public void showInputView() {
        if (mKeyboardView != null) {
            mKeyboardView.setVisibility(View.VISIBLE);
        }
    }

    /** @see jp.co.omronsoft.openwnn.InputViewManager#hideInputView */
    public void hideInputView() {
        mKeyboardView.setVisibility(View.GONE);
    }

    /***********************************************************************
     * onKeyboardActionListener
     ***********************************************************************/
    /** @see jp.co.omronsoft.openwnn.KeyboardView.OnKeyboardActionListener#onKey */
    public void onKey(int primaryCode, int[] keyCodes) { }

    /** @see jp.co.omronsoft.openwnn.KeyboardView.OnKeyboardActionListener#swipeRight */
    public void swipeRight() { }

    /** @see jp.co.omronsoft.openwnn.KeyboardView.OnKeyboardActionListener#swipeLeft */
    public void swipeLeft() { }

    /** @see jp.co.omronsoft.openwnn.KeyboardView.OnKeyboardActionListener#swipeDown */
    public void swipeDown() { }

    /** @see jp.co.omronsoft.openwnn.KeyboardView.OnKeyboardActionListener#swipeUp */
    public void swipeUp() { }

    /** @see jp.co.omronsoft.openwnn.KeyboardView.OnKeyboardActionListener#onRelease */
    public void onRelease(int x) { }

    /** @see jp.co.omronsoft.openwnn.KeyboardView.OnKeyboardActionListener#onPress */
    public void onPress(int x) {
        playSoundAndVibration();
    }

    /** @see android.jp.co.omronsoft.openwnn.KeyboardView.OnKeyboardActionListener#onLongPress */
    public boolean onLongPress(Keyboard.Key key) {
        return false;
    }

    /**
     * Play sound & vibration.
     */
    private void playSoundAndVibration() {
        /* key click sound & vibration */
        if (mVibrator != null) {
            try { mVibrator.vibrate(5); } catch (Exception ex) { }
        }
        if (mSound != null) {
            try { mSound.seekTo(0); mSound.start(); } catch (Exception ex) { }
        }
    }

    /** @see jp.co.omronsoft.openwnn.KeyboardView.OnKeyboardActionListener#onText */
    public void onText(CharSequence text) {}

    /**
     * Get current key mode.
     * 
     * @return Current key mode
     */
    public int getKeyMode() {
        return mCurrentKeyMode;
    }

    /**
     * Get current keyboard type.
     * 
     * @return Current keyboard type
     */
    public int getKeyboardType() {
        return mCurrentKeyboardType;
    }

    /**
     * Set the H/W keyboard's state.
     * 
     * @param hidden {@code true} if hidden.
     */
    public void setHardKeyboardHidden(boolean hidden) {
        mHardKeyboardHidden = hidden;
    }

    /**
     * Set the H/W keyboard's type.
     *
     * @param type12Key {@code true} if 12Key.
     */
    public void setHardware12Keyboard(boolean type12Key) {
        mEnableHardware12Keyboard = type12Key;
    }

    /**
     * Get current keyboard view.
     */
    public View getKeyboardView() {
        return mKeyboardView;
    }

    /**
     * Reset the current keyboard
     */
    public void resetCurrentKeyboard() {
        closing();
        Keyboard keyboard = mCurrentKeyboard;
        mCurrentKeyboard = null;
        changeKeyboard(keyboard);
    }

    /**
     * Set the normal keyboard.
     */
    public void setNormalKeyboard() {
        if (mCurrentKeyboard == null) {
            return;
        }
        mKeyboardView.setKeyboard(mCurrentKeyboard);
        mKeyboardView.setOnKeyboardActionListener(this);
        mIsSymbolKeyboard = false;
    }

    /**
     * Set the symbol keyboard.
     */
    public void setSymbolKeyboard() {
        mKeyboardView.setKeyboard(mSymbolKeyboard);
        mKeyboardView.setOnKeyboardActionListener(mSymbolOnKeyboardAction);
        mIsSymbolKeyboard = true;
    }
}
