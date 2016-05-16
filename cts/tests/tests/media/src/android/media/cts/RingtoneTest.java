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

package android.media.cts;


import android.content.Context;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.Settings;
import android.test.AndroidTestCase;

public class RingtoneTest extends AndroidTestCase {

    private Context mContext;
    private Ringtone mRingtone;
    private AudioManager mAudioManager;
    private int mOriginalVolume;
    private int mOriginalRingerMode;
    private int mOriginalStreamType;
    private Uri mDefaultRingUri;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getContext();
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mRingtone = RingtoneManager.getRingtone(mContext, Settings.System.DEFAULT_RINGTONE_URI);
        // backup ringer settings
        mOriginalRingerMode = mAudioManager.getRingerMode();
        mOriginalVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_RING);
        mOriginalStreamType = mRingtone.getStreamType();
        // set ringer to a reasonable volume
        int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
        mAudioManager.setStreamVolume(AudioManager.STREAM_RING, maxVolume / 2,
                AudioManager.FLAG_ALLOW_RINGER_MODES);
        // make sure that we are not in silent mode
        mAudioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);

        mDefaultRingUri = RingtoneManager.getActualDefaultRingtoneUri(mContext,
                RingtoneManager.TYPE_RINGTONE);
    }

    @Override
    protected void tearDown() throws Exception {
        // restore original settings
        if (mRingtone != null) {
            if (mRingtone.isPlaying()) mRingtone.stop();
            mRingtone.setStreamType(mOriginalStreamType);
        }
        if (mAudioManager != null) {
            mAudioManager.setRingerMode(mOriginalRingerMode);
            mAudioManager.setStreamVolume(AudioManager.STREAM_RING, mOriginalVolume,
                    AudioManager.FLAG_ALLOW_RINGER_MODES);
        }
        RingtoneManager.setActualDefaultRingtoneUri(mContext, RingtoneManager.TYPE_RINGTONE,
                mDefaultRingUri);
        super.tearDown();
    }

    public void testRingtone() {

        assertNotNull(mRingtone.getTitle(mContext));
        assertTrue(mOriginalStreamType >= 0);

        mRingtone.setStreamType(AudioManager.STREAM_MUSIC);
        assertEquals(AudioManager.STREAM_MUSIC, mRingtone.getStreamType());
        mRingtone.setStreamType(AudioManager.STREAM_ALARM);
        assertEquals(AudioManager.STREAM_ALARM, mRingtone.getStreamType());
        // make sure we play on STREAM_RING because we the volume on this stream is not 0
        mRingtone.setStreamType(AudioManager.STREAM_RING);
        assertEquals(AudioManager.STREAM_RING, mRingtone.getStreamType());

        // test both the "None" ringtone and an actual ringtone
        RingtoneManager.setActualDefaultRingtoneUri(mContext, RingtoneManager.TYPE_RINGTONE, null);
        mRingtone = RingtoneManager.getRingtone(mContext, Settings.System.DEFAULT_RINGTONE_URI);
        assertTrue(mRingtone.getStreamType() == AudioManager.STREAM_RING);
        mRingtone.play();
        assertFalse(mRingtone.isPlaying());

        Uri uri = RingtoneManager.getValidRingtoneUri(mContext);
        assertNotNull("ringtone was unexpectedly null", uri);
        RingtoneManager.setActualDefaultRingtoneUri(mContext, RingtoneManager.TYPE_RINGTONE, uri);
        mRingtone = RingtoneManager.getRingtone(mContext, Settings.System.DEFAULT_RINGTONE_URI);
        assertTrue(mRingtone.getStreamType() == AudioManager.STREAM_RING);
        mRingtone.play();
        assertTrue("couldn't play ringtone " + uri, mRingtone.isPlaying());
        mRingtone.stop();
        assertFalse(mRingtone.isPlaying());
    }
}
