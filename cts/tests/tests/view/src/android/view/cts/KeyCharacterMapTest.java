/*
 * Copyright (C) 2008 The Android Open Source Project
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

package android.view.cts;


import android.test.AndroidTestCase;
import android.text.TextUtils;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.KeyCharacterMap.KeyData;

public class KeyCharacterMapTest extends AndroidTestCase {

    private KeyCharacterMap mKeyCharacterMap;
    private final char[] chars = {'A', 'B', 'C'};

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mKeyCharacterMap = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD);
    }

    public void testIsPrintingKey() throws Exception {

        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_UNKNOWN));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_SOFT_LEFT));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_SOFT_RIGHT));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_HOME));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_BACK));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_CALL));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_ENDCALL));

        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_0));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_1));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_2));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_3));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_4));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_5));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_6));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_7));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_8));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_9));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_STAR));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_POUND));

        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_DPAD_UP));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_DPAD_DOWN));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_DPAD_LEFT));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_DPAD_RIGHT));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_DPAD_CENTER));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_VOLUME_UP));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_VOLUME_DOWN));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_POWER));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_CAMERA));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_CLEAR));

        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_A));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_B));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_C));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_D));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_E));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_F));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_G));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_H));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_I));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_J));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_K));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_L));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_M));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_N));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_O));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_P));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_Q));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_R));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_S));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_T));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_U));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_V));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_W));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_X));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_Y));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_Z));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_COMMA));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_PERIOD));

        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_ALT_LEFT));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_ALT_RIGHT));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_SHIFT_LEFT));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_SHIFT_RIGHT));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_TAB));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_SPACE));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_SYM));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_NUM));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_EXPLORER));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_ENVELOPE));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_ENTER));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_DEL));

        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_GRAVE));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_MINUS));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_EQUALS));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_LEFT_BRACKET));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_RIGHT_BRACKET));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_BACKSLASH));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_SEMICOLON));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_APOSTROPHE));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_SLASH));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_AT));

        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_NUM));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_HEADSETHOOK));

        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_FOCUS));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_PLUS));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_MENU));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_NOTIFICATION));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_SEARCH));

        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_MEDIA_STOP));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_MEDIA_NEXT));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_MEDIA_REWIND));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_MUTE));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_PAGE_UP));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_PAGE_DOWN));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_PICTSYMBOLS));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_SWITCH_CHARSET));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_BUTTON_A));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_BUTTON_B));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_BUTTON_C));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_BUTTON_X));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_BUTTON_Y));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_BUTTON_Z));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_BUTTON_L1));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_BUTTON_R1));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_BUTTON_L2));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_BUTTON_R2));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_BUTTON_THUMBL));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_BUTTON_THUMBR));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_BUTTON_START));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_BUTTON_SELECT));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_BUTTON_MODE));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_ESCAPE));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_FORWARD_DEL));

        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_CTRL_LEFT));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_CTRL_RIGHT));

        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_CAPS_LOCK));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_SCROLL_LOCK));

        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_META_LEFT));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_META_RIGHT));

        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_FUNCTION));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_SYSRQ));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_BREAK));

        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_MOVE_HOME));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_MOVE_END));

        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_INSERT));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_FORWARD));

        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_MEDIA_PLAY));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_MEDIA_PAUSE));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_MEDIA_CLOSE));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_MEDIA_EJECT));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_MEDIA_RECORD));

        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_F1));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_F2));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_F3));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_F4));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_F5));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_F6));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_F7));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_F8));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_F9));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_F10));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_F11));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_F12));

        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_NUM_LOCK));

        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_NUMPAD_0));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_NUMPAD_1));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_NUMPAD_2));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_NUMPAD_3));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_NUMPAD_4));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_NUMPAD_5));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_NUMPAD_6));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_NUMPAD_7));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_NUMPAD_8));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_NUMPAD_9));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_NUMPAD_DIVIDE));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_NUMPAD_MULTIPLY));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_NUMPAD_SUBTRACT));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_NUMPAD_ADD));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_NUMPAD_DOT));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_NUMPAD_COMMA));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_NUMPAD_ENTER));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_NUMPAD_EQUALS));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_NUMPAD_LEFT_PAREN));
        assertTrue(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_NUMPAD_RIGHT_PAREN));

        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_VOLUME_MUTE));

        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_INFO));

        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_CHANNEL_UP));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_CHANNEL_DOWN));

        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_ZOOM_IN));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_ZOOM_OUT));

        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_TV));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_WINDOW));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_GUIDE));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_DVR));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_BOOKMARK));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_CAPTIONS));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_SETTINGS));

        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_TV_POWER));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_TV_INPUT));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_STB_POWER));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_STB_INPUT));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_AVR_POWER));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_AVR_INPUT));

        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_PROG_RED));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_PROG_GREEN));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_PROG_YELLOW));
        assertFalse(mKeyCharacterMap.isPrintingKey(KeyEvent.KEYCODE_PROG_BLUE));
    }

    public void testLoad() throws Exception {
        mKeyCharacterMap = null;
        mKeyCharacterMap = KeyCharacterMap.load(KeyCharacterMap.BUILT_IN_KEYBOARD);
        assertNotNull(mKeyCharacterMap);
    }

    public void testGetNumber() throws Exception {
        assertEquals('0', mKeyCharacterMap.getNumber(KeyEvent.KEYCODE_0));
        assertEquals('1', mKeyCharacterMap.getNumber(KeyEvent.KEYCODE_1));
        assertEquals('2', mKeyCharacterMap.getNumber(KeyEvent.KEYCODE_2));
        assertEquals('3', mKeyCharacterMap.getNumber(KeyEvent.KEYCODE_3));
        assertEquals('4', mKeyCharacterMap.getNumber(KeyEvent.KEYCODE_4));
        assertEquals('5', mKeyCharacterMap.getNumber(KeyEvent.KEYCODE_5));
        assertEquals('6', mKeyCharacterMap.getNumber(KeyEvent.KEYCODE_6));
        assertEquals('7', mKeyCharacterMap.getNumber(KeyEvent.KEYCODE_7));
        assertEquals('8', mKeyCharacterMap.getNumber(KeyEvent.KEYCODE_8));
        assertEquals('9', mKeyCharacterMap.getNumber(KeyEvent.KEYCODE_9));
        assertEquals('*', mKeyCharacterMap.getNumber(KeyEvent.KEYCODE_STAR));
        assertEquals('#', mKeyCharacterMap.getNumber(KeyEvent.KEYCODE_POUND));
    }

    public void testGetMatch1() throws Exception {
        try {
            mKeyCharacterMap.getMatch(KeyEvent.KEYCODE_0, null);
            fail("should throw exception");
        } catch (Exception e) {
        }

        assertEquals('\0', mKeyCharacterMap.getMatch(getCharacterKeyCode('E'), chars));
        assertEquals('A', mKeyCharacterMap.getMatch(getCharacterKeyCode('A'), chars));
        assertEquals('B', mKeyCharacterMap.getMatch(getCharacterKeyCode('B'), chars));
    }

    private int getCharacterKeyCode(char oneChar) {
        // Lowercase the character to avoid getting modifiers in the KeyEvent array.
        char[] chars = new char[] {Character.toLowerCase(oneChar)};
        KeyEvent[] events = mKeyCharacterMap.getEvents(chars);
        return events[0].getKeyCode();
    }

    public void testGetMatch2() throws Exception {
        try {
            mKeyCharacterMap.getMatch(KeyEvent.KEYCODE_0, null, 1);
            fail("should throw exception");
        } catch (Exception e) {
        }
        assertEquals('\0', mKeyCharacterMap.getMatch(1000, chars, 2));
        assertEquals('\0', mKeyCharacterMap.getMatch(10000, chars, 2));
        assertEquals('\0', mKeyCharacterMap.getMatch(getCharacterKeyCode('E'), chars));
        assertEquals('A', mKeyCharacterMap.getMatch(getCharacterKeyCode('A'), chars));
        assertEquals('B', mKeyCharacterMap.getMatch(getCharacterKeyCode('B'), chars));
    }

    public void testGetKeyboardType() throws Exception {
        mKeyCharacterMap.getKeyboardType();
    }

    public void testGetEvents() {
        try {
            mKeyCharacterMap.getEvents(null);
            fail("should throw exception");
        } catch (Exception e) {
        }
        CharSequence mCharSequence = "TestMessage123";
        int len = mCharSequence.length();
        char[] charsArray = new char[len];
        TextUtils.getChars(mCharSequence, 1, len, charsArray, 0);
        mKeyCharacterMap.getEvents(charsArray);
    }

    public void testGetKeyData() throws Exception {
        KeyData result = new KeyData();
        result.meta = new char[2];
        try {
            mKeyCharacterMap.getKeyData(KeyEvent.KEYCODE_HOME, result);
            fail("should throw exception");
        } catch (Exception e) {
        }
        result.meta = new char[4];
        assertFalse(mKeyCharacterMap.getKeyData(KeyEvent.KEYCODE_HOME, result));
        assertTrue(mKeyCharacterMap.getKeyData(KeyEvent.KEYCODE_0, result));
        assertEquals(48, result.meta[0]);

        // here just call deviceHasKey and deviceHasKeys.
        KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_0);
        final int[] keyChar = new int[] {
                KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_3
        };
        boolean[] keys = KeyCharacterMap.deviceHasKeys(keyChar);
        assertEquals(keyChar.length, keys.length);
    }

}
