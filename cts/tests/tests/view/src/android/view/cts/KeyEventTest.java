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


import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.test.AndroidTestCase;
import android.text.method.MetaKeyKeyListener;
import android.view.KeyEvent;
import android.view.KeyCharacterMap.KeyData;
import android.view.KeyEvent.Callback;

import junit.framework.Assert;

/**
 * Test {@link KeyEvent}.
 */
public class KeyEventTest extends AndroidTestCase {
    private KeyEvent mKeyEvent;
    private long mDownTime;
    private long mEventTime;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mKeyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_0);

        mDownTime = SystemClock.uptimeMillis();
        mEventTime = SystemClock.uptimeMillis();
    }

    public void testConstructor() {
        new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_0);

        new KeyEvent(mDownTime, mEventTime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_0, 5);

        new KeyEvent(mDownTime, mEventTime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_0, 5,
                KeyEvent.META_SHIFT_ON);

        new KeyEvent(mDownTime, mEventTime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_0, 5,
                KeyEvent.META_SHIFT_ON, 1, 1);

        new KeyEvent(mDownTime, mEventTime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_0, 5,
                KeyEvent.META_SHIFT_ON, 1, 1, KeyEvent.FLAG_SOFT_KEYBOARD);

        KeyEvent keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_0);
        new KeyEvent(keyEvent);
        new KeyEvent(keyEvent, mEventTime, 1);

        new KeyEvent(mDownTime, "test", 0, KeyEvent.FLAG_SOFT_KEYBOARD);
    }

    public void testGetCharacters() {
        String characters = "android_test";
        mKeyEvent = new KeyEvent(mDownTime, characters, 0, KeyEvent.FLAG_SOFT_KEYBOARD);
        assertEquals(KeyEvent.ACTION_MULTIPLE, mKeyEvent.getAction());
        assertEquals(KeyEvent.KEYCODE_UNKNOWN, mKeyEvent.getKeyCode());
        assertEquals(characters, mKeyEvent.getCharacters());

        mKeyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_0);
        assertNull(mKeyEvent.getCharacters());
    }

    public void testGetMaxKeyCode() {
        assertTrue(KeyEvent.getMaxKeyCode() > 0);
    }

    public void testIsShiftPressed() {
        assertFalse(mKeyEvent.isShiftPressed());
        mKeyEvent = new KeyEvent(mDownTime, mEventTime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_0, 5,
                KeyEvent.META_SHIFT_ON);
        assertTrue(mKeyEvent.isShiftPressed());
        mKeyEvent = new KeyEvent(mDownTime, mEventTime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_0, 5,
                KeyEvent.META_ALT_ON);
        assertFalse(mKeyEvent.isShiftPressed());
    }

    public void testGetDeadChar() {
        // decimal number of &egrave; is 232.
        assertEquals(232, KeyEvent.getDeadChar('`', 'e'));
    }

    public void testGetKeyData() {
        KeyEvent keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_Z);
        KeyData keyData = new KeyData();
        assertTrue(keyEvent.getKeyData(keyData));

        assertEquals('Z', keyData.displayLabel);
        assertEquals(0, keyData.number);
        assertEquals('z', keyData.meta[0]);
        assertEquals('Z', keyData.meta[1]);
        assertEquals(0, keyData.meta[3]);
    }

    public void testDispatch() {
        MockCallback callback = new MockCallback();
        mKeyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_0);
        callback.reset();
        assertFalse(callback.isKeyDown());
        assertTrue(mKeyEvent.dispatch(callback));
        assertTrue(callback.isKeyDown());
        assertEquals(KeyEvent.KEYCODE_0, callback.getKeyCode());
        assertSame(mKeyEvent, callback.getKeyEvent());

        mKeyEvent = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_0);
        callback.reset();
        assertFalse(callback.isKeyUp());
        assertTrue(mKeyEvent.dispatch(callback));
        assertTrue(callback.isKeyUp());
        assertEquals(KeyEvent.KEYCODE_0, callback.getKeyCode());
        assertSame(mKeyEvent, callback.getKeyEvent());

        callback.reset();
        int count = 2;
        mKeyEvent = new KeyEvent(mDownTime, mEventTime, KeyEvent.ACTION_MULTIPLE,
                KeyEvent.KEYCODE_0, count);
        assertFalse(callback.isKeyMultiple());
        assertTrue(mKeyEvent.dispatch(callback));
        assertTrue(callback.isKeyMultiple());
        assertEquals(KeyEvent.KEYCODE_0, callback.getKeyCode());
        assertSame(mKeyEvent, callback.getKeyEvent());
        assertEquals(count, callback.getCount());

        callback.reset();
        count = 0;
        mKeyEvent = new KeyEvent(mDownTime, mEventTime, KeyEvent.ACTION_MULTIPLE,
                KeyEvent.KEYCODE_0, count);
        assertTrue(mKeyEvent.dispatch(callback));
        assertTrue(callback.isKeyMultiple());
        assertTrue(callback.isKeyDown());
        assertTrue(callback.isKeyUp());
        assertEquals(count, callback.getCount());
        assertEquals(KeyEvent.KEYCODE_0, callback.getKeyCode());
    }

    public void testGetMetaState() {
        int metaState = KeyEvent.META_ALT_ON;
        mKeyEvent = new KeyEvent(mDownTime, mEventTime, KeyEvent.ACTION_MULTIPLE,
                KeyEvent.KEYCODE_1, 1, metaState);
        assertEquals(metaState, mKeyEvent.getMetaState());
    }

    public void testGetEventTime() {
        mKeyEvent = new KeyEvent(mDownTime, mEventTime, KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_0, 5);
        assertEquals(mEventTime, mKeyEvent.getEventTime());
    }

    public void testGetDownTime() {
        mKeyEvent = new KeyEvent(mDownTime, mEventTime, KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_0, 5);
        assertEquals(mDownTime, mKeyEvent.getDownTime());
    }

    public void testGetUnicodeChar1() {
        // 48 is Unicode character of '0'
        assertEquals(48, mKeyEvent.getUnicodeChar());

        mKeyEvent = new KeyEvent(mDownTime, mEventTime, KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_9, 5, 0);
        // 57 is Unicode character of '9'
        assertEquals(57, mKeyEvent.getUnicodeChar());

        mKeyEvent = new KeyEvent(mDownTime, mEventTime, KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_ALT_LEFT, 5, KeyEvent.META_SHIFT_ON);
        // 'ALT' key is not a type Unicode character.
        assertEquals(0, mKeyEvent.getUnicodeChar());
    }

    public void testGetUnicodeChar2() {
        // 48 is Unicode character of '0'
        assertEquals(48, mKeyEvent.getUnicodeChar(MetaKeyKeyListener.META_CAP_LOCKED));
        mKeyEvent = new KeyEvent(mDownTime, mEventTime, KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_9, 5, 0);

        // 57 is Unicode character of '9'
        assertEquals(57, mKeyEvent.getUnicodeChar(0));

        mKeyEvent = new KeyEvent(mDownTime, mEventTime, KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_ALT_LEFT, 5, KeyEvent.META_SHIFT_ON);
        // 'ALT' key is not a type Unicode character.
        assertEquals(0, mKeyEvent.getUnicodeChar(0));
    }

    public void testGetNumber() {
        // 48 is associated with key '0'
        assertEquals(48, mKeyEvent.getNumber());

        mKeyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_3);
        // 51 is associated with key '3'
        assertEquals(51, mKeyEvent.getNumber());
    }

    public void testIsSymPressed() {
        mKeyEvent = new KeyEvent(mDownTime, mEventTime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_0, 5,
                KeyEvent.META_SYM_ON);
        assertTrue(mKeyEvent.isSymPressed());

        mKeyEvent = new KeyEvent(mDownTime, mEventTime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_0, 5,
                KeyEvent.META_SHIFT_ON);
        assertFalse(mKeyEvent.isSymPressed());
    }

    public void testGetDeviceId() {
        int deviceId = 1;
        mKeyEvent = new KeyEvent(mDownTime, mEventTime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_0, 5,
                KeyEvent.META_SHIFT_ON, deviceId, 1);
        assertEquals(deviceId, mKeyEvent.getDeviceId());
    }

    public void testToString() {
        // make sure it does not throw any exception.
        mKeyEvent.toString();
    }

    public void testIsAltPressed() {
        mKeyEvent = new KeyEvent(mDownTime, mEventTime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_0, 5,
                KeyEvent.META_ALT_ON);
        assertTrue(mKeyEvent.isAltPressed());

        mKeyEvent = new KeyEvent(mDownTime, mEventTime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_0, 5,
                KeyEvent.META_SHIFT_ON);
        assertFalse(mKeyEvent.isAltPressed());
    }

    public void testGetModifierMetaStateMask() {
        int mask = KeyEvent.getModifierMetaStateMask();
        assertTrue((mask & KeyEvent.META_SHIFT_ON) != 0);
        assertTrue((mask & KeyEvent.META_SHIFT_LEFT_ON) != 0);
        assertTrue((mask & KeyEvent.META_SHIFT_RIGHT_ON) != 0);
        assertTrue((mask & KeyEvent.META_ALT_ON) != 0);
        assertTrue((mask & KeyEvent.META_ALT_LEFT_ON) != 0);
        assertTrue((mask & KeyEvent.META_ALT_RIGHT_ON) != 0);
        assertTrue((mask & KeyEvent.META_CTRL_ON) != 0);
        assertTrue((mask & KeyEvent.META_CTRL_LEFT_ON) != 0);
        assertTrue((mask & KeyEvent.META_CTRL_RIGHT_ON) != 0);
        assertTrue((mask & KeyEvent.META_META_ON) != 0);
        assertTrue((mask & KeyEvent.META_META_LEFT_ON) != 0);
        assertTrue((mask & KeyEvent.META_META_RIGHT_ON) != 0);
        assertTrue((mask & KeyEvent.META_SYM_ON) != 0);
        assertTrue((mask & KeyEvent.META_FUNCTION_ON) != 0);

        assertFalse((mask & KeyEvent.META_CAPS_LOCK_ON) != 0);
        assertFalse((mask & KeyEvent.META_NUM_LOCK_ON) != 0);
        assertFalse((mask & KeyEvent.META_SCROLL_LOCK_ON) != 0);
    }

    public void testIsModifierKey() {
        assertTrue(KeyEvent.isModifierKey(KeyEvent.KEYCODE_SHIFT_LEFT));
        assertTrue(KeyEvent.isModifierKey(KeyEvent.KEYCODE_SHIFT_RIGHT));
        assertTrue(KeyEvent.isModifierKey(KeyEvent.KEYCODE_ALT_LEFT));
        assertTrue(KeyEvent.isModifierKey(KeyEvent.KEYCODE_ALT_RIGHT));
        assertTrue(KeyEvent.isModifierKey(KeyEvent.KEYCODE_CTRL_LEFT));
        assertTrue(KeyEvent.isModifierKey(KeyEvent.KEYCODE_CTRL_RIGHT));
        assertTrue(KeyEvent.isModifierKey(KeyEvent.KEYCODE_META_LEFT));
        assertTrue(KeyEvent.isModifierKey(KeyEvent.KEYCODE_META_RIGHT));
        assertTrue(KeyEvent.isModifierKey(KeyEvent.KEYCODE_SYM));
        assertTrue(KeyEvent.isModifierKey(KeyEvent.KEYCODE_NUM));
        assertTrue(KeyEvent.isModifierKey(KeyEvent.KEYCODE_FUNCTION));

        assertFalse(KeyEvent.isModifierKey(KeyEvent.KEYCODE_0));
    }

    private static final int UNDEFINED_META_STATE = 0x80000000;

    public void testNormalizeMetaState() {
        // Already normalized values.
        assertEquals(0, KeyEvent.normalizeMetaState(0));
        assertEquals(KeyEvent.getModifierMetaStateMask(),
                KeyEvent.normalizeMetaState(KeyEvent.getModifierMetaStateMask()));

        // Values that require normalization.
        assertEquals(KeyEvent.META_SHIFT_LEFT_ON | KeyEvent.META_SHIFT_ON,
                KeyEvent.normalizeMetaState(KeyEvent.META_SHIFT_LEFT_ON));
        assertEquals(KeyEvent.META_SHIFT_RIGHT_ON | KeyEvent.META_SHIFT_ON,
                KeyEvent.normalizeMetaState(KeyEvent.META_SHIFT_RIGHT_ON));
        assertEquals(KeyEvent.META_ALT_LEFT_ON | KeyEvent.META_ALT_ON,
                KeyEvent.normalizeMetaState(KeyEvent.META_ALT_LEFT_ON));
        assertEquals(KeyEvent.META_ALT_RIGHT_ON | KeyEvent.META_ALT_ON,
                KeyEvent.normalizeMetaState(KeyEvent.META_ALT_RIGHT_ON));
        assertEquals(KeyEvent.META_CTRL_LEFT_ON | KeyEvent.META_CTRL_ON,
                KeyEvent.normalizeMetaState(KeyEvent.META_CTRL_LEFT_ON));
        assertEquals(KeyEvent.META_CTRL_RIGHT_ON | KeyEvent.META_CTRL_ON,
                KeyEvent.normalizeMetaState(KeyEvent.META_CTRL_RIGHT_ON));
        assertEquals(KeyEvent.META_META_LEFT_ON | KeyEvent.META_META_ON,
                KeyEvent.normalizeMetaState(KeyEvent.META_META_LEFT_ON));
        assertEquals(KeyEvent.META_META_RIGHT_ON | KeyEvent.META_META_ON,
                KeyEvent.normalizeMetaState(KeyEvent.META_META_RIGHT_ON));
        assertEquals(KeyEvent.META_CAPS_LOCK_ON,
                KeyEvent.normalizeMetaState(MetaKeyKeyListener.META_CAP_LOCKED));
        assertEquals(KeyEvent.META_ALT_ON,
                KeyEvent.normalizeMetaState(MetaKeyKeyListener.META_ALT_LOCKED));
        assertEquals(KeyEvent.META_SYM_ON,
                KeyEvent.normalizeMetaState(MetaKeyKeyListener.META_SYM_LOCKED));
        assertEquals(KeyEvent.META_SHIFT_ON,
                KeyEvent.normalizeMetaState(KeyEvent.META_SHIFT_ON | UNDEFINED_META_STATE));
    }

    public void testMetaStateHasNoModifiers() {
        assertTrue(KeyEvent.metaStateHasNoModifiers(0));
        assertTrue(KeyEvent.metaStateHasNoModifiers(KeyEvent.META_CAPS_LOCK_ON));
        assertTrue(KeyEvent.metaStateHasNoModifiers(KeyEvent.META_NUM_LOCK_ON));
        assertTrue(KeyEvent.metaStateHasNoModifiers(KeyEvent.META_SCROLL_LOCK_ON));

        assertFalse(KeyEvent.metaStateHasNoModifiers(KeyEvent.META_SHIFT_ON));
        assertFalse(KeyEvent.metaStateHasNoModifiers(KeyEvent.META_SHIFT_LEFT_ON));
        assertFalse(KeyEvent.metaStateHasNoModifiers(KeyEvent.META_SHIFT_RIGHT_ON));
        assertFalse(KeyEvent.metaStateHasNoModifiers(KeyEvent.META_ALT_ON));
        assertFalse(KeyEvent.metaStateHasNoModifiers(KeyEvent.META_ALT_LEFT_ON));
        assertFalse(KeyEvent.metaStateHasNoModifiers(KeyEvent.META_ALT_RIGHT_ON));
        assertFalse(KeyEvent.metaStateHasNoModifiers(KeyEvent.META_CTRL_ON));
        assertFalse(KeyEvent.metaStateHasNoModifiers(KeyEvent.META_CTRL_LEFT_ON));
        assertFalse(KeyEvent.metaStateHasNoModifiers(KeyEvent.META_CTRL_RIGHT_ON));
        assertFalse(KeyEvent.metaStateHasNoModifiers(KeyEvent.META_META_ON));
        assertFalse(KeyEvent.metaStateHasNoModifiers(KeyEvent.META_META_LEFT_ON));
        assertFalse(KeyEvent.metaStateHasNoModifiers(KeyEvent.META_META_RIGHT_ON));
        assertFalse(KeyEvent.metaStateHasNoModifiers(KeyEvent.META_SYM_ON));
        assertFalse(KeyEvent.metaStateHasNoModifiers(KeyEvent.META_FUNCTION_ON));
    }

    public void testMetaStateHasModifiers() {
        assertTrue(KeyEvent.metaStateHasModifiers(0, 0));
        assertTrue(KeyEvent.metaStateHasModifiers(
                KeyEvent.META_NUM_LOCK_ON | KeyEvent.META_CAPS_LOCK_ON
                        | KeyEvent.META_SCROLL_LOCK_ON, 0));
        assertTrue(KeyEvent.metaStateHasModifiers(
                KeyEvent.META_SHIFT_ON | KeyEvent.META_SHIFT_LEFT_ON,
                KeyEvent.META_SHIFT_LEFT_ON));
        assertTrue(KeyEvent.metaStateHasModifiers(
                KeyEvent.META_SHIFT_LEFT_ON | KeyEvent.META_SHIFT_RIGHT_ON,
                KeyEvent.META_SHIFT_LEFT_ON | KeyEvent.META_SHIFT_RIGHT_ON));
        assertTrue(KeyEvent.metaStateHasModifiers(
                KeyEvent.META_SHIFT_LEFT_ON,
                KeyEvent.META_SHIFT_LEFT_ON));
        assertTrue(KeyEvent.metaStateHasModifiers(
                KeyEvent.META_NUM_LOCK_ON | KeyEvent.META_CAPS_LOCK_ON
                        | KeyEvent.META_SCROLL_LOCK_ON | KeyEvent.META_SHIFT_LEFT_ON,
                KeyEvent.META_SHIFT_LEFT_ON));
        assertTrue(KeyEvent.metaStateHasModifiers(
                KeyEvent.META_SHIFT_ON | KeyEvent.META_SHIFT_LEFT_ON,
                KeyEvent.META_SHIFT_ON));
        assertTrue(KeyEvent.metaStateHasModifiers(
                KeyEvent.META_ALT_ON | KeyEvent.META_ALT_RIGHT_ON,
                KeyEvent.META_ALT_ON));
        assertTrue(KeyEvent.metaStateHasModifiers(
                KeyEvent.META_ALT_LEFT_ON | KeyEvent.META_SHIFT_LEFT_ON,
                KeyEvent.META_ALT_ON | KeyEvent.META_SHIFT_ON));
        assertTrue(KeyEvent.metaStateHasModifiers(
                KeyEvent.META_CTRL_RIGHT_ON | KeyEvent.META_META_LEFT_ON,
                KeyEvent.META_CTRL_RIGHT_ON | KeyEvent.META_META_ON));
        assertTrue(KeyEvent.metaStateHasModifiers(
                KeyEvent.META_SYM_ON | KeyEvent.META_FUNCTION_ON | KeyEvent.META_CAPS_LOCK_ON,
                KeyEvent.META_SYM_ON | KeyEvent.META_FUNCTION_ON));

        assertFalse(KeyEvent.metaStateHasModifiers(0, KeyEvent.META_SHIFT_ON));
        assertFalse(KeyEvent.metaStateHasModifiers(
                KeyEvent.META_ALT_ON | KeyEvent.META_SHIFT_LEFT_ON,
                KeyEvent.META_SHIFT_ON));
        assertFalse(KeyEvent.metaStateHasModifiers(
                KeyEvent.META_ALT_LEFT_ON | KeyEvent.META_SHIFT_LEFT_ON,
                KeyEvent.META_SHIFT_ON));
        assertFalse(KeyEvent.metaStateHasModifiers(
                KeyEvent.META_ALT_LEFT_ON,
                KeyEvent.META_ALT_RIGHT_ON));
        assertFalse(KeyEvent.metaStateHasModifiers(
                KeyEvent.META_ALT_LEFT_ON,
                KeyEvent.META_CTRL_LEFT_ON));

        final int[] invalidModifiers = new int[] {
                KeyEvent.META_CAPS_LOCK_ON,
                KeyEvent.META_NUM_LOCK_ON,
                KeyEvent.META_SCROLL_LOCK_ON,
                MetaKeyKeyListener.META_CAP_LOCKED,
                MetaKeyKeyListener.META_ALT_LOCKED,
                MetaKeyKeyListener.META_SYM_LOCKED,
                KeyEvent.META_SHIFT_ON | KeyEvent.META_SHIFT_LEFT_ON,
                KeyEvent.META_SHIFT_ON | KeyEvent.META_SHIFT_RIGHT_ON,
                KeyEvent.META_SHIFT_ON | KeyEvent.META_SHIFT_LEFT_ON| KeyEvent.META_SHIFT_RIGHT_ON,
                KeyEvent.META_ALT_ON | KeyEvent.META_ALT_LEFT_ON,
                KeyEvent.META_ALT_ON | KeyEvent.META_ALT_RIGHT_ON,
                KeyEvent.META_ALT_ON | KeyEvent.META_ALT_LEFT_ON| KeyEvent.META_ALT_RIGHT_ON,
                KeyEvent.META_CTRL_ON | KeyEvent.META_CTRL_LEFT_ON,
                KeyEvent.META_CTRL_ON | KeyEvent.META_CTRL_RIGHT_ON,
                KeyEvent.META_CTRL_ON | KeyEvent.META_CTRL_LEFT_ON| KeyEvent.META_CTRL_RIGHT_ON,
                KeyEvent.META_META_ON | KeyEvent.META_META_LEFT_ON,
                KeyEvent.META_META_ON | KeyEvent.META_META_RIGHT_ON,
                KeyEvent.META_META_ON | KeyEvent.META_META_LEFT_ON| KeyEvent.META_META_RIGHT_ON,
        };
        for (int modifiers : invalidModifiers) {
            try {
                KeyEvent.metaStateHasModifiers(0, modifiers);
                Assert.fail("Expected IllegalArgumentException");
            } catch (IllegalArgumentException ex) {
            }
        }

        assertFalse(KeyEvent.metaStateHasModifiers(0, UNDEFINED_META_STATE));
    }

    public void testHasNoModifiers() {
        KeyEvent ev = new KeyEvent(0, 0, KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_A, 0, KeyEvent.META_CAPS_LOCK_ON);
        assertTrue(ev.hasNoModifiers());

        ev = new KeyEvent(0, 0, KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_A, 0, KeyEvent.META_CAPS_LOCK_ON | KeyEvent.META_SHIFT_ON);
        assertFalse(ev.hasNoModifiers());
    }

    public void testHasModifiers() {
        KeyEvent ev = new KeyEvent(0, 0, KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_A, 0, KeyEvent.META_CAPS_LOCK_ON);
        assertTrue(ev.hasModifiers(0));

        ev = new KeyEvent(0, 0, KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_A, 0, KeyEvent.META_CAPS_LOCK_ON | KeyEvent.META_SHIFT_ON);
        assertTrue(ev.hasModifiers(KeyEvent.META_SHIFT_ON));

        ev = new KeyEvent(0, 0, KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_A, 0,
                KeyEvent.META_CAPS_LOCK_ON | KeyEvent.META_SHIFT_ON | KeyEvent.META_SHIFT_RIGHT_ON);
        assertFalse(ev.hasModifiers(KeyEvent.META_SHIFT_LEFT_ON));
    }

    public void testGetDisplayLabel() {
        assertTrue(mKeyEvent.getDisplayLabel() > 0);
    }

    public void testIsSystem() {
        mKeyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MENU);
        assertTrue(mKeyEvent.isSystem());

        mKeyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SOFT_RIGHT);
        assertTrue(mKeyEvent.isSystem());

        mKeyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_HOME);
        assertTrue(mKeyEvent.isSystem());

        mKeyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK);
        assertTrue(mKeyEvent.isSystem());

        mKeyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_CALL);
        assertTrue(mKeyEvent.isSystem());

        mKeyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENDCALL);
        assertTrue(mKeyEvent.isSystem());

        mKeyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP);
        assertTrue(mKeyEvent.isSystem());

        mKeyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN);
        assertTrue(mKeyEvent.isSystem());

        mKeyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_POWER);
        assertTrue(mKeyEvent.isSystem());

        mKeyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SEARCH);
        assertTrue(mKeyEvent.isSystem());

        mKeyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_HEADSETHOOK);
        assertTrue(mKeyEvent.isSystem());

        mKeyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_CAMERA);
        assertTrue(mKeyEvent.isSystem());

        mKeyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_FOCUS);
        assertTrue(mKeyEvent.isSystem());

        mKeyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_0);
        assertFalse(mKeyEvent.isSystem());
    }

    public void testIsPrintingKey() {
        mKeyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, Character.SPACE_SEPARATOR);
        assertTrue(mKeyEvent.isPrintingKey());

        mKeyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, Character.LINE_SEPARATOR);
        assertTrue(mKeyEvent.isPrintingKey());

        mKeyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, Character.PARAGRAPH_SEPARATOR);
        assertTrue(mKeyEvent.isPrintingKey());

        mKeyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, Character.CONTROL);
        assertTrue(mKeyEvent.isPrintingKey());

        mKeyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, Character.FORMAT);
        assertTrue(mKeyEvent.isPrintingKey());

        mKeyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_0);
        assertTrue(mKeyEvent.isPrintingKey());
    }

    public void testGetMatch1() {
        char[] codes1 = new char[] { '0', '1', '2' };
        assertEquals('0', mKeyEvent.getMatch(codes1));

        char[] codes2 = new char[] { 'A', 'B', 'C' };
        assertEquals('\0', mKeyEvent.getMatch(codes2));

        char[] codes3 = { '2', 'S' };
        mKeyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_S);
        assertEquals('S', mKeyEvent.getMatch(codes3));
    }

    public void testGetAction() {
        assertEquals(KeyEvent.ACTION_DOWN, mKeyEvent.getAction());
    }

    public void testGetRepeatCount() {
        int repeatCount = 1;
        mKeyEvent = new KeyEvent(mDownTime, mEventTime, KeyEvent.ACTION_MULTIPLE,
                KeyEvent.KEYCODE_0, repeatCount);
        assertEquals(repeatCount, mKeyEvent.getRepeatCount());
    }

    public void testWriteToParcel() {
        Parcel parcel = Parcel.obtain();
        mKeyEvent.writeToParcel(parcel, Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
        parcel.setDataPosition(0);

        KeyEvent keyEvent = KeyEvent.CREATOR.createFromParcel(parcel);
        parcel.recycle();

        assertEquals(mKeyEvent.getAction(), keyEvent.getAction());
        assertEquals(mKeyEvent.getKeyCode(), keyEvent.getKeyCode());
        assertEquals(mKeyEvent.getRepeatCount(), keyEvent.getRepeatCount());
        assertEquals(mKeyEvent.getMetaState(), keyEvent.getMetaState());
        assertEquals(mKeyEvent.getDeviceId(), keyEvent.getDeviceId());
        assertEquals(mKeyEvent.getScanCode(), keyEvent.getScanCode());
        assertEquals(mKeyEvent.getFlags(), keyEvent.getFlags());
        assertEquals(mKeyEvent.getDownTime(), keyEvent.getDownTime());
        assertEquals(mKeyEvent.getEventTime(), keyEvent.getEventTime());
    }

    public void testDescribeContents() {
        // make sure it never shrow any exception.
        mKeyEvent.describeContents();
    }

    public void testGetKeyCode() {
        assertEquals(KeyEvent.KEYCODE_0, mKeyEvent.getKeyCode());
    }

    public void testGetFlags() {
        mKeyEvent = new KeyEvent(mDownTime, mEventTime, KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_0, 5, KeyEvent.META_SHIFT_ON, 1, 1, KeyEvent.FLAG_WOKE_HERE);
        assertEquals(KeyEvent.FLAG_WOKE_HERE, mKeyEvent.getFlags());

        mKeyEvent = new KeyEvent(mDownTime, mEventTime, KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_0, 5, KeyEvent.META_SHIFT_ON, 1, 1, KeyEvent.FLAG_SOFT_KEYBOARD);
        assertEquals(KeyEvent.FLAG_SOFT_KEYBOARD, mKeyEvent.getFlags());
    }

    public void testGetScanCode() {
        int scanCode = 1;
        mKeyEvent = new KeyEvent(mDownTime, mEventTime, KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_0, 5, KeyEvent.META_SHIFT_ON, 1, scanCode);
        assertEquals(scanCode, mKeyEvent.getScanCode());
    }

    public void testChangeAction() {
        mKeyEvent = new KeyEvent(mDownTime, mEventTime, KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_0, 5, KeyEvent.META_SHIFT_ON, 1, 1, KeyEvent.FLAG_WOKE_HERE);

        KeyEvent newEvent = KeyEvent.changeAction(mKeyEvent, KeyEvent.ACTION_UP);
        assertEquals(KeyEvent.ACTION_UP, newEvent.getAction());
        assertEquals(mKeyEvent.getFlags(), newEvent.getFlags());
        assertEquals(mKeyEvent.getCharacters(), newEvent.getCharacters());
        assertEquals(mKeyEvent.getDisplayLabel(), newEvent.getDisplayLabel());
        assertEquals(mKeyEvent.getDeviceId(), newEvent.getDeviceId());
        assertEquals(mKeyEvent.getDownTime(), newEvent.getDownTime());
        assertEquals(mKeyEvent.getEventTime(), newEvent.getEventTime());
        assertEquals(mKeyEvent.getKeyCode(), newEvent.getKeyCode());
        assertEquals(mKeyEvent.getRepeatCount(), newEvent.getRepeatCount());
    }

    public void testChangeFlags() {
        mKeyEvent = new KeyEvent(mDownTime, mEventTime, KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_0, 5, KeyEvent.META_SHIFT_ON, 1, 1, KeyEvent.FLAG_WOKE_HERE);

        KeyEvent newEvent = KeyEvent.changeFlags(mKeyEvent, KeyEvent.FLAG_FROM_SYSTEM);
        assertEquals(KeyEvent.FLAG_FROM_SYSTEM, newEvent.getFlags());
        assertEquals(mKeyEvent.getAction(), newEvent.getAction());
        assertEquals(mKeyEvent.getCharacters(), newEvent.getCharacters());
        assertEquals(mKeyEvent.getDisplayLabel(), newEvent.getDisplayLabel());
        assertEquals(mKeyEvent.getDeviceId(), newEvent.getDeviceId());
        assertEquals(mKeyEvent.getDownTime(), newEvent.getDownTime());
        assertEquals(mKeyEvent.getEventTime(), newEvent.getEventTime());
        assertEquals(mKeyEvent.getKeyCode(), newEvent.getKeyCode());
        assertEquals(mKeyEvent.getRepeatCount(), newEvent.getRepeatCount());
    }

    public void testChangeTimeRepeat() {
        mKeyEvent = new KeyEvent(mDownTime, mEventTime, KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_0, 5, KeyEvent.META_SHIFT_ON, 1, 1, KeyEvent.FLAG_WOKE_HERE);

        long newEventTime = SystemClock.uptimeMillis();
        int newRepeat = mKeyEvent.getRepeatCount() + 1;
        KeyEvent newEvent = KeyEvent.changeTimeRepeat(mKeyEvent, newEventTime, newRepeat);
        assertEquals(newEventTime, newEvent.getEventTime());
        assertEquals(newRepeat, newEvent.getRepeatCount());
        assertEquals(mKeyEvent.getFlags(), newEvent.getFlags());
        assertEquals(mKeyEvent.getAction(), newEvent.getAction());
        assertEquals(mKeyEvent.getCharacters(), newEvent.getCharacters());
        assertEquals(mKeyEvent.getDisplayLabel(), newEvent.getDisplayLabel());
        assertEquals(mKeyEvent.getDeviceId(), newEvent.getDeviceId());
        assertEquals(mKeyEvent.getDownTime(), newEvent.getDownTime());
        assertEquals(mKeyEvent.getKeyCode(), newEvent.getKeyCode());
    }

    private class MockCallback implements Callback {
        private boolean mIsKeyDown;
        private boolean mIsKeyUp;
        private boolean mIsMultiple;
        private int mKeyCode;
        private KeyEvent mKeyEvent;
        private int mCount;

        public boolean isKeyDown() {
            return mIsKeyDown;
        }

        public boolean isKeyUp() {
            return mIsKeyUp;
        }

        public boolean isKeyMultiple() {
            return mIsMultiple;
        }

        public int getKeyCode() {
            return mKeyCode;
        }

        public KeyEvent getKeyEvent() {
            return mKeyEvent;
        }

        public int getCount() {
            return mCount;
        }

        public void reset() {
            mIsKeyDown = false;
            mIsKeyUp = false;
            mIsMultiple = false;
        }

        public boolean onKeyDown(int keyCode, KeyEvent event) {
            mIsKeyDown = true;
            mKeyCode = keyCode;
            mKeyEvent = event;
            return true;
        }

        public boolean onKeyLongPress(int keyCode, KeyEvent event) {
            return false;
        }

        public boolean onKeyMultiple(int keyCode, int count, KeyEvent event) {
            mIsMultiple = true;
            mKeyCode = keyCode;
            mKeyEvent = event;
            mCount = count;
            if (count < 1) {
                return false; // this key event never repeat.
            }
            return true;
        }

        public boolean onKeyUp(int keyCode, KeyEvent event) {
            mIsKeyUp = true;
            mKeyCode = keyCode;
            mKeyEvent = event;
            return true;
        }
    }
}
