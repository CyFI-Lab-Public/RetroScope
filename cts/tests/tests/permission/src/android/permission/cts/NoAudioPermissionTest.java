/*
 * Copyright (C) 2009 The Android Open Source Project
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

package android.permission.cts;

import android.content.Context;
import android.media.AudioManager;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

/**
 * Verify the audio related operations require specific permissions.
 */
public class NoAudioPermissionTest extends AndroidTestCase {
    private AudioManager mAudioManager;
    private static final int MODE_COUNT = 3;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        assertNotNull(mAudioManager);
    }

    /**
     * Verify that AudioManager.setMicrophoneMute, AudioManager.setMode requires permissions.
     * <p>Requires Permission:
     *   {@link android.Manifest.permission#MODIFY_AUDIO_SETTINGS}.
     */
    @SmallTest
    public void testSetMicrophoneMute() {
        boolean muteState = mAudioManager.isMicrophoneMute();
        int originalMode = mAudioManager.getMode();
        // If there is no permission of MODIFY_AUDIO_SETTINGS, setMicrophoneMute does nothing.
        mAudioManager.setMicrophoneMute(!muteState);
        assertEquals(muteState, mAudioManager.isMicrophoneMute());

        // If there is no permission of MODIFY_AUDIO_SETTINGS, setMode does nothing.
        assertTrue(AudioManager.MODE_NORMAL != AudioManager.MODE_RINGTONE);

        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        assertEquals(originalMode, mAudioManager.getMode());

        mAudioManager.setMode(AudioManager.MODE_RINGTONE);
        assertEquals(originalMode, mAudioManager.getMode());
    }

    /**
     * Verify that AudioManager routing methods require permissions.
     * <p>Requires Permission:
     *   {@link android.Manifest.permission#MODIFY_AUDIO_SETTINGS}.
     */
    @SuppressWarnings("deprecation")
    @SmallTest
    public void testRouting() {

        // If there is no permission of MODIFY_AUDIO_SETTINGS, setSpeakerphoneOn does nothing.
        boolean prevState = mAudioManager.isSpeakerphoneOn();
        mAudioManager.setSpeakerphoneOn(!prevState);
        assertEquals(prevState, mAudioManager.isSpeakerphoneOn());

        // If there is no permission of MODIFY_AUDIO_SETTINGS, setBluetoothScoOn does nothing.
        prevState = mAudioManager.isBluetoothScoOn();
        mAudioManager.setBluetoothScoOn(!prevState);
        assertEquals(prevState, mAudioManager.isBluetoothScoOn());
    }
}
