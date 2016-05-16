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

import com.android.cts.media.R;


import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.SoundPool;
import android.test.AndroidTestCase;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.InputStream;

abstract class SoundPoolTest extends AndroidTestCase {

    private static final int SOUNDPOOL_STREAMS = 4;
    private static final int PRIORITY = 1;
    private static final int LOUD = 20;
    private static final int QUIET = LOUD / 2;
    private static final int SILENT = 0;
    private File mFile;
    private SoundPool mSoundPool;

    /**
     * function to return resource ID for A4 sound.
     * should be implemented by child class
     * @return resource ID
     */
    protected abstract int getSoundA();

    protected abstract int getSoundCs();

    protected abstract int getSoundE();

    protected abstract int getSoundB();

    protected abstract int getSoundGs();

    protected abstract String getFileName();

    private int[] getSounds() {
        int[] sounds = { getSoundA(),
                         getSoundCs(),
                         getSoundE(),
                         getSoundB(),
                         getSoundGs() };
        return sounds;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mFile = new File(mContext.getFilesDir(), getFileName());
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if (mFile.exists()) {
            mFile.delete();
        }
        if (mSoundPool != null) {
            mSoundPool.release();
            mSoundPool = null;
            return;
        }
    }

    public void testLoad() throws Exception {
        int srcQuality = 100;
        mSoundPool = new SoundPool(SOUNDPOOL_STREAMS, AudioManager.STREAM_MUSIC, srcQuality);
        int sampleId1 = mSoundPool.load(mContext, getSoundA(), PRIORITY);
        waitUntilLoaded(sampleId1);
        // should return true, but returns false
        mSoundPool.unload(sampleId1);

        AssetFileDescriptor afd = mContext.getResources().openRawResourceFd(getSoundCs());
        int sampleId2;
        sampleId2 = mSoundPool.load(afd, PRIORITY);
        waitUntilLoaded(sampleId2);
        mSoundPool.unload(sampleId2);

        FileDescriptor fd = afd.getFileDescriptor();
        long offset = afd.getStartOffset();
        long length = afd.getLength();
        int sampleId3;
        sampleId3 = mSoundPool.load(fd, offset, length, PRIORITY);
        waitUntilLoaded(sampleId3);
        mSoundPool.unload(sampleId3);

        String path = mFile.getAbsolutePath();
        createSoundFile(mFile);
        int sampleId4;
        sampleId4 = mSoundPool.load(path, PRIORITY);
        waitUntilLoaded(sampleId4);
        mSoundPool.unload(sampleId4);
    }

    private void createSoundFile(File f) throws Exception {
        FileOutputStream fOutput = null;
        try {
            fOutput = new FileOutputStream(f);
            InputStream is = mContext.getResources().openRawResource(getSoundA());
            byte[] buffer = new byte[1024];
            int length = is.read(buffer);
            while (length != -1) {
                fOutput.write(buffer, 0, length);
                length = is.read(buffer);
            }
        } finally {
            if (fOutput != null) {
                fOutput.flush();
                fOutput.close();
            }
        }
    }

    public void testSoundPoolOp() throws Exception {
        int srcQuality = 100;
        mSoundPool = new SoundPool(SOUNDPOOL_STREAMS, AudioManager.STREAM_MUSIC, srcQuality);
        int sampleID = loadSampleSync(getSoundA(), PRIORITY);

        int waitMsec = 1000;
        float leftVolume = SILENT;
        float rightVolume = LOUD;
        int priority = 1;
        int loop = 0;
        float rate = 1f;
        int streamID = mSoundPool.play(sampleID, leftVolume, rightVolume, priority, loop, rate);
        assertTrue(streamID != 0);
        Thread.sleep(waitMsec);
        rate = 1.4f;
        mSoundPool.setRate(streamID, rate);
        Thread.sleep(waitMsec);
        mSoundPool.setRate(streamID, 1f);
        Thread.sleep(waitMsec);
        mSoundPool.pause(streamID);
        Thread.sleep(waitMsec);
        mSoundPool.resume(streamID);
        Thread.sleep(waitMsec);
        mSoundPool.stop(streamID);

        streamID = mSoundPool.play(sampleID, leftVolume, rightVolume, priority, loop, rate);
        assertTrue(streamID != 0);
        loop = -1;// loop forever
        mSoundPool.setLoop(streamID, loop);
        Thread.sleep(waitMsec);
        leftVolume = SILENT;
        rightVolume = SILENT;
        mSoundPool.setVolume(streamID, leftVolume, rightVolume);
        Thread.sleep(waitMsec);
        rightVolume = LOUD;
        mSoundPool.setVolume(streamID, leftVolume, rightVolume);
        priority = 0;
        mSoundPool.setPriority(streamID, priority);
        Thread.sleep(waitMsec * 10);
        mSoundPool.stop(streamID);
        mSoundPool.unload(sampleID);
    }

    public void testMultiSound() throws Exception {
        int srcQuality = 100;
        mSoundPool = new SoundPool(SOUNDPOOL_STREAMS, AudioManager.STREAM_MUSIC, srcQuality);
        int sampleID1 = loadSampleSync(getSoundA(), PRIORITY);
        int sampleID2 = loadSampleSync(getSoundCs(), PRIORITY);
        long waitMsec = 1000;
        Thread.sleep(waitMsec);

        // play sounds one at a time
        int streamID1 = mSoundPool.play(sampleID1, LOUD, QUIET, PRIORITY, -1, 1);
        assertTrue(streamID1 != 0);
        Thread.sleep(waitMsec * 4);
        mSoundPool.stop(streamID1);
        int streamID2 = mSoundPool.play(sampleID2, QUIET, LOUD, PRIORITY, -1, 1);
        assertTrue(streamID2 != 0);
        Thread.sleep(waitMsec * 4);
        mSoundPool.stop(streamID2);

        // play both at once repeating the first, but not the second
        streamID1 = mSoundPool.play(sampleID1, LOUD, QUIET, PRIORITY, 1, 1);
        streamID2 = mSoundPool.play(sampleID2, QUIET, LOUD, PRIORITY, 0, 1);
        assertTrue(streamID1 != 0);
        assertTrue(streamID2 != 0);
        Thread.sleep(4000);
        // both streams should have stopped by themselves; no way to check

        mSoundPool.release();
        mSoundPool = null;
    }

    public void testLoadMore() throws Exception {
        mSoundPool = new SoundPool(SOUNDPOOL_STREAMS, AudioManager.STREAM_MUSIC, 0);
        int[] sounds = getSounds();
        int[] soundIds = new int[sounds.length];
        int[] streamIds = new int[sounds.length];
        for (int i = 0; i < sounds.length; i++) {
            soundIds[i] = loadSampleSync(sounds[i], PRIORITY);
            System.out.println("load: " + soundIds[i]);
        }
        for (int i = 0; i < soundIds.length; i++) {
            streamIds[i] = mSoundPool.play(soundIds[i], LOUD, LOUD, PRIORITY, -1, 1);
        }
        Thread.sleep(3000);
        for (int stream : streamIds) {
            assertTrue(stream != 0);
            mSoundPool.stop(stream);
        }
        for (int sound : soundIds) {
            mSoundPool.unload(sound);
        }
        mSoundPool.release();
    }

    /**
     * Load a sample and wait until it is ready to be played.
     * @return The sample ID.
     * @throws InterruptedException
     */
    private int loadSampleSync(int sampleId, int prio) throws InterruptedException {
        int sample = mSoundPool.load(mContext, sampleId, prio);
        waitUntilLoaded(sample);
        return sample;
    }

    /**
     * Wait until the specified sample is loaded.
     * @param sampleId The sample ID.
     * @throws InterruptedException
     */
    private void waitUntilLoaded(int sampleId) throws InterruptedException {
        int stream = 0;
        while (stream == 0) {
            Thread.sleep(500);
            stream = mSoundPool.play(sampleId, SILENT, SILENT, 1, 0, 1);
        }
        mSoundPool.stop(stream);
    }
}
