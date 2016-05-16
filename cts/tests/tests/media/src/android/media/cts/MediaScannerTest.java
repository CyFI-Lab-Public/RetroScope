/*
 * Copyright (C) 2012 The Android Open Source Project
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

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.cts.util.PollingCheck;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.mtp.MtpConstants;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.provider.cts.FileCopyHelper;
import android.test.AndroidTestCase;
import android.util.Log;

import java.io.File;
import java.io.IOException;

public class MediaScannerTest extends AndroidTestCase {

    private static final String MEDIA_TYPE = "audio/mpeg";
    private File mMediaFile;
    private static final int TIME_OUT = 10000;
    private MockMediaScannerConnection mMediaScannerConnection;
    private MockMediaScannerConnectionClient mMediaScannerConnectionClient;
    private String mFileDir;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // prepare the media file.

        mFileDir = Environment.getExternalStorageDirectory() + "/" + getClass().getCanonicalName();
        cleanup();
        String fileName = mFileDir + "/test" + System.currentTimeMillis() + ".mp3";
        writeFile(R.raw.testmp3, fileName);

        mMediaFile = new File(fileName);
        assertTrue(mMediaFile.exists());
    }

    private void writeFile(int resid, String path) throws IOException {
        File out = new File(path);
        File dir = out.getParentFile();
        dir.mkdirs();
        FileCopyHelper copier = new FileCopyHelper(mContext);
        copier.copyToExternalStorage(resid, out);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    private void cleanup() {
        if (mMediaFile != null) {
            mMediaFile.delete();
        }
        if (mFileDir != null) {
            new File(mFileDir + "/testmp3.mp3").delete();
            new File(mFileDir + "/testmp3_2.mp3").delete();
            new File(mFileDir + "/ctsmediascanplaylist1.pls").delete();
            new File(mFileDir + "/ctsmediascanplaylist2.m3u").delete();
            new File(mFileDir).delete();
        }

        if (mMediaScannerConnection != null) {
            mMediaScannerConnection.disconnect();
            mMediaScannerConnection = null;
        }

        mContext.getContentResolver().delete(MediaStore.Audio.Media.getContentUri("external"),
                "_data like ?", new String[] { mFileDir + "%"});
    }

    public void testMediaScanner() throws InterruptedException, IOException {
        mMediaScannerConnectionClient = new MockMediaScannerConnectionClient();
        mMediaScannerConnection = new MockMediaScannerConnection(getContext(),
                                    mMediaScannerConnectionClient);

        assertFalse(mMediaScannerConnection.isConnected());

        // start connection and wait until connected
        mMediaScannerConnection.connect();
        checkConnectionState(true);

        // start and wait for scan
        mMediaScannerConnection.scanFile(mMediaFile.getAbsolutePath(), MEDIA_TYPE);
        checkMediaScannerConnection();

        Uri insertUri = mMediaScannerConnectionClient.mediaUri;
        long id = Long.valueOf(insertUri.getLastPathSegment());
        ContentResolver res = mContext.getContentResolver();

        // check that the file ended up in the audio view
        Uri audiouri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
        Cursor c = res.query(audiouri, null, null, null, null);
        assertEquals(1, c.getCount());
        c.close();

        // add nomedia file and insert into database, file should no longer be in audio view
        File nomedia = new File(mMediaFile.getParent() + "/.nomedia");
        nomedia.createNewFile();
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DATA, nomedia.getAbsolutePath());
        values.put(MediaStore.Files.FileColumns.FORMAT, MtpConstants.FORMAT_UNDEFINED);
        Uri nomediauri = res.insert(MediaStore.Files.getContentUri("external"), values);
        // clean up nomedia file
        nomedia.delete();

        // entry should not be in audio view anymore
        c = res.query(audiouri, null, null, null, null);
        assertEquals(0, c.getCount());
        c.close();

        // with nomedia file removed, do media scan and check that entry is in audio table again
        startMediaScanAndWait();

        // Give the 2nd stage scan that makes the unhidden files visible again
        // a little more time
        SystemClock.sleep(10000);
        // entry should be in audio view again
        c = res.query(audiouri, null, null, null, null);
        assertEquals(1, c.getCount());
        c.close();

        // ensure that we don't currently have playlists named ctsmediascanplaylist*
        res.delete(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                MediaStore.Audio.PlaylistsColumns.NAME + "=?",
                new String[] { "ctsmediascanplaylist1"});
        res.delete(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                MediaStore.Audio.PlaylistsColumns.NAME + "=?",
                new String[] { "ctsmediascanplaylist2"});
        // delete the playlist file entries, if they exist
        res.delete(MediaStore.Files.getContentUri("external"),
                MediaStore.Files.FileColumns.DATA + "=?",
                new String[] { mFileDir + "/ctsmediascanplaylist1.pls"});
        res.delete(MediaStore.Files.getContentUri("external"),
                MediaStore.Files.FileColumns.DATA + "=?",
                new String[] { mFileDir + "/ctsmediascanplaylist2.m3u"});

        // write some more files
        writeFile(R.raw.testmp3, mFileDir + "/testmp3.mp3");
        writeFile(R.raw.testmp3_2, mFileDir + "/testmp3_2.mp3");
        writeFile(R.raw.playlist1, mFileDir + "/ctsmediascanplaylist1.pls");
        writeFile(R.raw.playlist2, mFileDir + "/ctsmediascanplaylist2.m3u");

        startMediaScanAndWait();

        // verify that the two playlists were created correctly;
        c = res.query(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, null,
                MediaStore.Audio.PlaylistsColumns.NAME + "=?",
                new String[] { "ctsmediascanplaylist1"}, null);
        assertEquals(1, c.getCount());
        c.moveToFirst();
        long playlistid = c.getLong(c.getColumnIndex(MediaStore.MediaColumns._ID));
        c.close();

        c = res.query(MediaStore.Audio.Playlists.Members.getContentUri("external", playlistid),
                null, null, null, MediaStore.Audio.Playlists.Members.PLAY_ORDER);
        assertEquals(2, c.getCount());
        c.moveToNext();
        long song1a = c.getLong(c.getColumnIndex(MediaStore.Audio.Playlists.Members.AUDIO_ID));
        c.moveToNext();
        long song1b = c.getLong(c.getColumnIndex(MediaStore.Audio.Playlists.Members.AUDIO_ID));
        c.close();
        assertTrue("song id should not be 0", song1a != 0);
        assertTrue("song id should not be 0", song1b != 0);
        assertTrue("song ids should not be same", song1a != song1b);

        // 2nd playlist should have the same songs, in reverse order
        c = res.query(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, null,
                MediaStore.Audio.PlaylistsColumns.NAME + "=?",
                new String[] { "ctsmediascanplaylist2"}, null);
        assertEquals(1, c.getCount());
        c.moveToFirst();
        playlistid = c.getLong(c.getColumnIndex(MediaStore.MediaColumns._ID));
        c.close();

        c = res.query(MediaStore.Audio.Playlists.Members.getContentUri("external", playlistid),
                null, null, null, MediaStore.Audio.Playlists.Members.PLAY_ORDER);
        assertEquals(2, c.getCount());
        c.moveToNext();
        long song2a = c.getLong(c.getColumnIndex(MediaStore.Audio.Playlists.Members.AUDIO_ID));
        c.moveToNext();
        long song2b = c.getLong(c.getColumnIndex(MediaStore.Audio.Playlists.Members.AUDIO_ID));
        c.close();
        assertEquals("mismatched song ids", song1a, song2b);
        assertEquals("mismatched song ids", song2a, song1b);

        mMediaScannerConnection.disconnect();

        checkConnectionState(false);
    }

    public void testWildcardPaths() throws InterruptedException, IOException {
        mMediaScannerConnectionClient = new MockMediaScannerConnectionClient();
        mMediaScannerConnection = new MockMediaScannerConnection(getContext(),
                                    mMediaScannerConnectionClient);

        assertFalse(mMediaScannerConnection.isConnected());

        // start connection and wait until connected
        mMediaScannerConnection.connect();
        checkConnectionState(true);

        long now = System.currentTimeMillis();
        String dir1 = mFileDir + "/test-" + now;
        String file1 = dir1 + "/test.mp3";
        String dir2 = mFileDir + "/test_" + now;
        String file2 = dir2 + "/test.mp3";
        assertTrue(new File(dir1).mkdir());
        writeFile(R.raw.testmp3, file1);
        mMediaScannerConnection.scanFile(file1, MEDIA_TYPE);
        checkMediaScannerConnection();
        Uri file1Uri = mMediaScannerConnectionClient.mediaUri;

        assertTrue(new File(dir2).mkdir());
        writeFile(R.raw.testmp3, file2);
        mMediaScannerConnectionClient.reset();
        mMediaScannerConnection.scanFile(file2, MEDIA_TYPE);
        checkMediaScannerConnection();
        Uri file2Uri = mMediaScannerConnectionClient.mediaUri;

        // if the URIs are the same, then the media scanner likely treated the _ character
        // in the second path as a wildcard, and matched it with the first path
        assertFalse(file1Uri.equals(file2Uri));

        // rewrite Uris to use the file scheme
        long file1id = Long.valueOf(file1Uri.getLastPathSegment());
        long file2id = Long.valueOf(file2Uri.getLastPathSegment());
        file1Uri = MediaStore.Files.getContentUri("external", file1id);
        file2Uri = MediaStore.Files.getContentUri("external", file2id);

        ContentResolver res = mContext.getContentResolver();
        Cursor c = res.query(file1Uri, new String[] { "parent" }, null, null, null);
        c.moveToFirst();
        long parent1id = c.getLong(0);
        c.close();
        c = res.query(file2Uri, new String[] { "parent" }, null, null, null);
        c.moveToFirst();
        long parent2id = c.getLong(0);
        c.close();
        // if the parent ids are the same, then the media provider likely
        // treated the _ character in the second path as a wildcard
        assertTrue("same parent", parent1id != parent2id);

        // check the parent paths are correct
        c = res.query(MediaStore.Files.getContentUri("external", parent1id),
                new String[] { "_data" }, null, null, null);
        c.moveToFirst();
        assertEquals(dir1, c.getString(0));
        c.close();

        c = res.query(MediaStore.Files.getContentUri("external", parent2id),
                new String[] { "_data" }, null, null, null);
        c.moveToFirst();
        assertEquals(dir2, c.getString(0));
        c.close();

        // clean up
        new File(file1).delete();
        new File(dir1).delete();
        new File(file2).delete();
        new File(dir2).delete();
        res.delete(file1Uri, null, null);
        res.delete(file2Uri, null, null);
        res.delete(MediaStore.Files.getContentUri("external", parent1id), null, null);
        res.delete(MediaStore.Files.getContentUri("external", parent2id), null, null);

        mMediaScannerConnection.disconnect();

        checkConnectionState(false);
    }

    public void testCanonicalize() throws Exception {
        mMediaScannerConnectionClient = new MockMediaScannerConnectionClient();
        mMediaScannerConnection = new MockMediaScannerConnection(getContext(),
                                    mMediaScannerConnectionClient);

        assertFalse(mMediaScannerConnection.isConnected());

        // start connection and wait until connected
        mMediaScannerConnection.connect();
        checkConnectionState(true);

        // write file and scan to insert into database
        String fileDir = Environment.getExternalStorageDirectory() + "/"
                + getClass().getCanonicalName() + "/canonicaltest-" + System.currentTimeMillis();
        String fileName = fileDir + "/test.mp3";
        writeFile(R.raw.testmp3, fileName);
        mMediaScannerConnection.scanFile(fileName, MEDIA_TYPE);
        checkMediaScannerConnection();

        // check path and uri
        Uri uri = mMediaScannerConnectionClient.mediaUri;
        String path = mMediaScannerConnectionClient.mediaPath;
        assertEquals(fileName, path);
        assertNotNull(uri);

        // check canonicalization
        ContentResolver res = mContext.getContentResolver();
        Uri canonicalUri = res.canonicalize(uri);
        assertNotNull(canonicalUri);
        assertFalse(uri.equals(canonicalUri));
        Uri uncanonicalizedUri = res.uncanonicalize(canonicalUri);
        assertEquals(uri, uncanonicalizedUri);

        // remove the entry from the database
        assertEquals(1, res.delete(uri, null, null));
        assertTrue(new File(path).delete());

        // write same file again and scan to insert into database
        mMediaScannerConnectionClient.reset();
        String fileName2 = fileDir + "/test2.mp3";
        writeFile(R.raw.testmp3, fileName2);
        mMediaScannerConnection.scanFile(fileName2, MEDIA_TYPE);
        checkMediaScannerConnection();

        // check path and uri
        Uri uri2 = mMediaScannerConnectionClient.mediaUri;
        String path2 = mMediaScannerConnectionClient.mediaPath;
        assertEquals(fileName2, path2);
        assertNotNull(uri2);

        // this should be a different entry in the database and not re-use the same database id
        assertFalse(uri.equals(uri2));

        Uri canonicalUri2 = res.canonicalize(uri2);
        assertNotNull(canonicalUri2);
        assertFalse(uri2.equals(canonicalUri2));
        Uri uncanonicalizedUri2 = res.uncanonicalize(canonicalUri2);
        assertEquals(uri2, uncanonicalizedUri2);

        // uncanonicalize the original canonicalized uri, it should resolve to the new uri
        Uri uncanonicalizedUri3 = res.uncanonicalize(canonicalUri);
        assertEquals(uri2, uncanonicalizedUri3);

        assertEquals(1, res.delete(uri2, null, null));
        assertTrue(new File(path2).delete());
    }

    private void startMediaScanAndWait() throws InterruptedException {
        ScannerNotificationReceiver finishedReceiver = new ScannerNotificationReceiver(
                Intent.ACTION_MEDIA_SCANNER_FINISHED);
        IntentFilter finishedIntentFilter = new IntentFilter(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        finishedIntentFilter.addDataScheme("file");
        mContext.registerReceiver(finishedReceiver, finishedIntentFilter);

        Bundle args = new Bundle();
        args.putString("volume", "external");
        Intent i = new Intent("android.media.IMediaScannerService").putExtras(args);
        i.setClassName("com.android.providers.media",
                "com.android.providers.media.MediaScannerService");
        mContext.startService(i);

        finishedReceiver.waitForBroadcast();
        mContext.unregisterReceiver(finishedReceiver);
    }

    private void checkMediaScannerConnection() {
        new PollingCheck(TIME_OUT) {
            protected boolean check() {
                return mMediaScannerConnectionClient.isOnMediaScannerConnectedCalled;
            }
        }.run();
        new PollingCheck(TIME_OUT) {
            protected boolean check() {
                return mMediaScannerConnectionClient.mediaPath != null;
            }
        }.run();
    }

    private void checkConnectionState(final boolean expected) {
        new PollingCheck(TIME_OUT) {
            protected boolean check() {
                return mMediaScannerConnection.isConnected() == expected;
            }
        }.run();
    }

    class MockMediaScannerConnection extends MediaScannerConnection {

        public boolean mIsOnServiceConnectedCalled;
        public boolean mIsOnServiceDisconnectedCalled;
        public MockMediaScannerConnection(Context context, MediaScannerConnectionClient client) {
            super(context, client);
        }

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            super.onServiceConnected(className, service);
            mIsOnServiceConnectedCalled = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            super.onServiceDisconnected(className);
            mIsOnServiceDisconnectedCalled = true;
            // this is not called.
        }
    }

    class MockMediaScannerConnectionClient implements MediaScannerConnectionClient {

        public boolean isOnMediaScannerConnectedCalled;
        public String mediaPath;
        public Uri mediaUri;
        public void onMediaScannerConnected() {
            isOnMediaScannerConnectedCalled = true;
        }

        public void onScanCompleted(String path, Uri uri) {
            mediaPath = path;
            if (uri != null) {
                mediaUri = uri;
            }
        }

        public void reset() {
            mediaPath = null;
            mediaUri = null;
        }
    }

}
