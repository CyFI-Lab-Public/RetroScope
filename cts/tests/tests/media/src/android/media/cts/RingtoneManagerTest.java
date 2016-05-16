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
package android.media.cts;

import com.android.cts.media.R;


import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.Settings;
import android.test.ActivityInstrumentationTestCase2;

public class RingtoneManagerTest
        extends ActivityInstrumentationTestCase2<RingtonePickerActivity> {

    private static final String PKG = "com.android.cts.media";

    private RingtonePickerActivity mActivity;
    private Instrumentation mInstrumentation;
    private Context mContext;
    private RingtoneManager mRingtoneManager;
    private AudioManager mAudioManager;
    private int mOriginalVolume;
    private Uri mDefaultUri;

    public RingtoneManagerTest() {
        super(PKG, RingtonePickerActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
        mInstrumentation = getInstrumentation();
        mContext = mInstrumentation.getContext();
        mRingtoneManager = new RingtoneManager(mActivity);
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        // backup ringer settings
        mOriginalVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_RING);
        mDefaultUri = RingtoneManager.getActualDefaultRingtoneUri(mContext,
                RingtoneManager.TYPE_RINGTONE);
        mAudioManager.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_RAISE,
                AudioManager.FLAG_ALLOW_RINGER_MODES);
    }

    @Override
    protected void tearDown() throws Exception {
        // restore original ringer settings
        if (mAudioManager != null) {
            mAudioManager.setStreamVolume(AudioManager.STREAM_RING, mOriginalVolume,
                    AudioManager.FLAG_ALLOW_RINGER_MODES);
        }
        RingtoneManager.setActualDefaultRingtoneUri(mContext, RingtoneManager.TYPE_RINGTONE,
                mDefaultUri);
        super.tearDown();
    }

    public void testConstructors() {
        new RingtoneManager(mActivity);
        new RingtoneManager(mContext);
    }

    public void testAccessMethods() {
        Cursor c = mRingtoneManager.getCursor();
        assertTrue("Must have at least one ring tone available", c.getCount() > 0);

        assertNotNull(mRingtoneManager.getRingtone(0));
        assertNotNull(RingtoneManager.getRingtone(mContext, Settings.System.DEFAULT_RINGTONE_URI));
        int expectedPosition = 0;
        Uri uri = mRingtoneManager.getRingtoneUri(expectedPosition);
        assertEquals(expectedPosition, mRingtoneManager.getRingtonePosition(uri));
        assertNotNull(RingtoneManager.getValidRingtoneUri(mContext));

        RingtoneManager.setActualDefaultRingtoneUri(mContext, RingtoneManager.TYPE_RINGTONE, uri);
        assertEquals(uri, RingtoneManager.getActualDefaultRingtoneUri(mContext,
                RingtoneManager.TYPE_RINGTONE));

        assertEquals(Settings.System.DEFAULT_RINGTONE_URI,
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE));
        assertEquals(Settings.System.DEFAULT_NOTIFICATION_URI,
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        assertEquals(RingtoneManager.TYPE_RINGTONE,
                RingtoneManager.getDefaultType(Settings.System.DEFAULT_RINGTONE_URI));
        assertEquals(RingtoneManager.TYPE_NOTIFICATION,
                RingtoneManager.getDefaultType(Settings.System.DEFAULT_NOTIFICATION_URI));
        assertTrue(RingtoneManager.isDefault(Settings.System.DEFAULT_RINGTONE_URI));
    }

    public void testSetType() {
        mRingtoneManager.setType(RingtoneManager.TYPE_ALARM);
        assertEquals(AudioManager.STREAM_ALARM, mRingtoneManager.inferStreamType());
        Cursor c = mRingtoneManager.getCursor();
        assertTrue("Must have at least one alarm tone available", c.getCount() > 0);
        Ringtone r = mRingtoneManager.getRingtone(0);
        assertEquals(RingtoneManager.TYPE_ALARM, r.getStreamType());
    }

    public void testStopPreviousRingtone() {
        Cursor c = mRingtoneManager.getCursor();
        assertTrue("Must have at least one ring tone available", c.getCount() > 0);

        mRingtoneManager.setStopPreviousRingtone(true);
        assertTrue(mRingtoneManager.getStopPreviousRingtone());
        Uri uri = Uri.parse("android.resource://" + PKG + "/" + R.raw.john_cage);
        Ringtone ringtone = RingtoneManager.getRingtone(mContext, uri);
        ringtone.play();
        assertTrue(ringtone.isPlaying());
        ringtone.stop();
        assertFalse(ringtone.isPlaying());
        Ringtone newRingtone = mRingtoneManager.getRingtone(0);
        assertFalse(ringtone.isPlaying());
        newRingtone.play();
        assertTrue(newRingtone.isPlaying());
        mRingtoneManager.stopPreviousRingtone();
        assertFalse(newRingtone.isPlaying());
    }
}
