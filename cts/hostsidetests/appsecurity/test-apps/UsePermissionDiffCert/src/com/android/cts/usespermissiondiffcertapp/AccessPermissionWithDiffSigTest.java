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

package com.android.cts.usespermissiondiffcertapp;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.UriPermission;
import android.database.Cursor;
import android.net.Uri;
import android.os.SystemClock;
import android.test.AndroidTestCase;
import android.util.Log;

import com.android.cts.permissiondeclareapp.GrantUriPermission;

import java.io.IOException;
import java.util.List;

/**
 * Tests that signature-enforced permissions cannot be accessed by apps signed
 * with different certs than app that declares the permission.
 * 
 * Accesses app cts/tests/appsecurity-tests/test-apps/PermissionDeclareApp/...
 */
public class AccessPermissionWithDiffSigTest extends AndroidTestCase {
    private static final ComponentName GRANT_URI_PERM_COMP
            = new ComponentName("com.android.cts.permissiondeclareapp",
                    "com.android.cts.permissiondeclareapp.GrantUriPermission");
    private static final Uri PERM_URI = Uri.parse("content://ctspermissionwithsignature");
    private static final Uri PERM_URI_GRANTING = Uri.parse("content://ctspermissionwithsignaturegranting");
    private static final Uri PERM_URI_PATH = Uri.parse("content://ctspermissionwithsignaturepath");
    private static final Uri PERM_URI_PATH_RESTRICTING = Uri.parse(
            "content://ctspermissionwithsignaturepathrestricting");
    private static final Uri PRIV_URI = Uri.parse("content://ctsprivateprovider");
    private static final Uri PRIV_URI_GRANTING = Uri.parse("content://ctsprivateprovidergranting");
    private static final String EXPECTED_MIME_TYPE = "got/theMIME";

    private static final Uri AMBIGUOUS_URI_COMPAT = Uri.parse("content://ctsambiguousprovidercompat");
    private static final String EXPECTED_MIME_TYPE_AMBIGUOUS = "got/theUnspecifiedMIME";
    private static final Uri AMBIGUOUS_URI = Uri.parse("content://ctsambiguousprovider");

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        // Always dispose, usually to clean up from failed tests
        ReceiveUriActivity.finishCurInstanceSync();
    }

    private void assertReadingContentUriNotAllowed(Uri uri, String msg) {
        try {
            getContext().getContentResolver().query(uri, null, null, null, null);
            fail("expected SecurityException reading " + uri + ": " + msg);
        } catch (SecurityException expected) {
            assertNotNull("security exception's error message.", expected.getMessage());
        }
    }

    private void assertReadingContentUriAllowed(Uri uri) {
        try {
            getContext().getContentResolver().query(uri, null, null, null, null);
        } catch (SecurityException e) {
            fail("unexpected SecurityException reading " + uri + ": " + e.getMessage());
        }
    }

    private void assertReadingClipNotAllowed(ClipData clip, String msg) {
        for (int i=0; i<clip.getItemCount(); i++) {
            ClipData.Item item = clip.getItemAt(i);
            Uri uri = item.getUri();
            if (uri != null) {
                assertReadingContentUriNotAllowed(uri, msg);
            } else {
                Intent intent = item.getIntent();
                uri = intent.getData();
                if (uri != null) {
                    assertReadingContentUriNotAllowed(uri, msg);
                }
                ClipData intentClip = intent.getClipData();
                if (intentClip != null) {
                    assertReadingClipNotAllowed(intentClip, msg);
                }
            }
        }
    }

    private void assertOpenFileDescriptorModeNotAllowed(Uri uri, String msg, String mode) {
        try {
            getContext().getContentResolver().openFileDescriptor(uri, mode).close();
            fail("expected SecurityException writing " + uri + ": " + msg);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (SecurityException expected) {
            assertNotNull("security exception's error message.", expected.getMessage());
        }
    }

    private void assertWritingContentUriNotAllowed(Uri uri, String msg) {
        final ContentResolver resolver = getContext().getContentResolver();
        try {
            resolver.insert(uri, new ContentValues());
            fail("expected SecurityException inserting " + uri + ": " + msg);
        } catch (SecurityException expected) {
            assertNotNull("security exception's error message.", expected.getMessage());
        }

        try {
            resolver.update(uri, new ContentValues(), null, null);
            fail("expected SecurityException updating " + uri + ": " + msg);
        } catch (SecurityException expected) {
            assertNotNull("security exception's error message.", expected.getMessage());
        }

        try {
            resolver.delete(uri, null, null);
            fail("expected SecurityException deleting " + uri + ": " + msg);
        } catch (SecurityException expected) {
            assertNotNull("security exception's error message.", expected.getMessage());
        }

        try {
            getContext().getContentResolver().openOutputStream(uri).close();
            fail("expected SecurityException writing " + uri + ": " + msg);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (SecurityException expected) {
            assertNotNull("security exception's error message.", expected.getMessage());
        }

        assertOpenFileDescriptorModeNotAllowed(uri, msg, "w");
        assertOpenFileDescriptorModeNotAllowed(uri, msg, "wt");
        assertOpenFileDescriptorModeNotAllowed(uri, msg, "wa");
        assertOpenFileDescriptorModeNotAllowed(uri, msg, "rw");
        assertOpenFileDescriptorModeNotAllowed(uri, msg, "rwt");
    }

    private void assertWritingContentUriAllowed(Uri uri) {
        final ContentResolver resolver = getContext().getContentResolver();
        try {
            resolver.insert(uri, new ContentValues());
            resolver.update(uri, new ContentValues(), null, null);
            resolver.delete(uri, null, null);

            resolver.openOutputStream(uri).close();
            resolver.openFileDescriptor(uri, "w").close();
            resolver.openFileDescriptor(uri, "wt").close();
            resolver.openFileDescriptor(uri, "wa").close();
            resolver.openFileDescriptor(uri, "rw").close();
            resolver.openFileDescriptor(uri, "rwt").close();
        } catch (IOException e) {
            fail("unexpected IOException writing " + uri + ": " + e.getMessage());
        } catch (SecurityException e) {
            fail("unexpected SecurityException writing " + uri + ": " + e.getMessage());
        }
    }

    private void assertWritingClipNotAllowed(ClipData clip, String msg) {
        for (int i=0; i<clip.getItemCount(); i++) {
            ClipData.Item item = clip.getItemAt(i);
            Uri uri = item.getUri();
            if (uri != null) {
                assertWritingContentUriNotAllowed(uri, msg);
            } else {
                Intent intent = item.getIntent();
                uri = intent.getData();
                if (uri != null) {
                    assertWritingContentUriNotAllowed(uri, msg);
                }
                ClipData intentClip = intent.getClipData();
                if (intentClip != null) {
                    assertWritingClipNotAllowed(intentClip, msg);
                }
            }
        }
    }

    /**
     * Test that the ctspermissionwithsignature content provider cannot be read,
     * since this app lacks the required certs
     */
    public void testReadProviderWithDiff() {
        assertReadingContentUriRequiresPermission(PERM_URI,
                "com.android.cts.permissionWithSignature");
    }

    /**
     * Test that the ctspermissionwithsignature content provider cannot be written,
     * since this app lacks the required certs
     */
    public void testWriteProviderWithDiff() {
        assertWritingContentUriRequiresPermission(PERM_URI,
                "com.android.cts.permissionWithSignature");
    }

    /**
     * Test that the ctsprivateprovider content provider cannot be read,
     * since it is not exported from its app.
     */
    public void testReadProviderWhenPrivate() {
        assertReadingContentUriNotAllowed(PRIV_URI, "shouldn't read private provider");
    }

    /**
     * Test that the ctsambiguousprovider content provider cannot be read,
     * since it doesn't have an "exported=" line.
     */
    public void testReadProviderWhenAmbiguous() {
        assertReadingContentUriNotAllowed(AMBIGUOUS_URI, "shouldn't read ambiguous provider");
    }

    /**
     * Old App Compatibility Test
     *
     * Test that the ctsambiguousprovidercompat content provider can be read for older
     * API versions, because it didn't specify either exported=true or exported=false.
     */
    public void testReadProviderWhenAmbiguousCompat() {
        assertReadingContentUriAllowed(AMBIGUOUS_URI_COMPAT);
    }

    /**
     * Old App Compatibility Test
     *
     * Test that the ctsambiguousprovidercompat content provider can be written for older
     * API versions, because it didn't specify either exported=true or exported=false.
     */
    public void testWriteProviderWhenAmbiguousCompat() {
        assertWritingContentUriAllowed(AMBIGUOUS_URI_COMPAT);
    }

    /**
     * Test that the ctsprivateprovider content provider cannot be written,
     * since it is not exported from its app.
     */
    public void testWriteProviderWhenPrivate() {
        assertWritingContentUriNotAllowed(PRIV_URI, "shouldn't write private provider");
    }

    /**
     * Test that the ctsambiguousprovider content provider cannot be written,
     * since it doesn't have an exported= line.
     */
    public void testWriteProviderWhenAmbiguous() {
        assertWritingContentUriNotAllowed(AMBIGUOUS_URI, "shouldn't write ambiguous provider");
    }

    private static ClipData makeSingleClipData(Uri uri) {
        return new ClipData("foo", new String[] { "foo/bar" },
                new ClipData.Item(uri));
    }

    private static ClipData makeMultiClipData(Uri uri) {
        Uri grantClip1Uri = Uri.withAppendedPath(uri, "clip1");
        Uri grantClip2Uri = Uri.withAppendedPath(uri, "clip2");
        Uri grantClip3Uri = Uri.withAppendedPath(uri, "clip3");
        Uri grantClip4Uri = Uri.withAppendedPath(uri, "clip4");
        Uri grantClip5Uri = Uri.withAppendedPath(uri, "clip5");
        ClipData clip = new ClipData("foo", new String[] { "foo/bar" },
                new ClipData.Item(grantClip1Uri));
        clip.addItem(new ClipData.Item(grantClip2Uri));
        // Intents in the ClipData should allow their data: and clip URIs
        // to be granted, but only respect the grant flags of the top-level
        // Intent.
        clip.addItem(new ClipData.Item(new Intent(Intent.ACTION_VIEW, grantClip3Uri)));
        Intent intent = new Intent(Intent.ACTION_VIEW, grantClip4Uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        clip.addItem(new ClipData.Item(intent));
        intent = new Intent(Intent.ACTION_VIEW);
        intent.setClipData(new ClipData("foo", new String[] { "foo/bar" },
                new ClipData.Item(grantClip5Uri)));
        clip.addItem(new ClipData.Item(intent));
        return clip;
    }

    private static Intent makeClipIntent(ClipData clip, int flags) {
        Intent intent = new Intent();
        intent.setClipData(clip);
        intent.addFlags(flags);
        return intent;
    }

    private static Intent makeClipIntent(Uri uri, int flags) {
        return makeClipIntent(makeMultiClipData(uri), flags);
    }

    private void doTryGrantUriActivityPermissionToSelf(Uri uri, int mode) {
        Uri grantDataUri = Uri.withAppendedPath(uri, "data");
        Intent grantIntent = new Intent();
        grantIntent.setData(grantDataUri);
        grantIntent.addFlags(mode | Intent.FLAG_ACTIVITY_NEW_TASK);
        grantIntent.setClass(getContext(), ReceiveUriActivity.class);
        try {
            ReceiveUriActivity.clearStarted();
            getContext().startActivity(grantIntent);
            ReceiveUriActivity.waitForStart();
            fail("expected SecurityException granting " + grantDataUri + " to activity");
        } catch (SecurityException e) {
            // This is what we want.
        }

        grantIntent = makeClipIntent(uri, mode | Intent.FLAG_ACTIVITY_NEW_TASK);
        grantIntent.setClass(getContext(), ReceiveUriActivity.class);
        try {
            ReceiveUriActivity.clearStarted();
            getContext().startActivity(grantIntent);
            ReceiveUriActivity.waitForStart();
            fail("expected SecurityException granting " + grantIntent.getClipData() + " to activity");
        } catch (SecurityException e) {
            // This is what we want.
        }
    }

    /**
     * Test that we can't grant a permission to ourself.
     */
    public void testGrantReadUriActivityPermissionToSelf() {
        doTryGrantUriActivityPermissionToSelf(
                Uri.withAppendedPath(PERM_URI_GRANTING, "foo"),
                Intent.FLAG_GRANT_READ_URI_PERMISSION);
    }

    /**
     * Test that we can't grant a permission to ourself.
     */
    public void testGrantWriteUriActivityPermissionToSelf() {
        doTryGrantUriActivityPermissionToSelf(
                Uri.withAppendedPath(PERM_URI_GRANTING, "foo"),
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    }

    /**
     * Test that we can't grant a permission to ourself.
     */
    public void testGrantReadUriActivityPrivateToSelf() {
        doTryGrantUriActivityPermissionToSelf(
                Uri.withAppendedPath(PRIV_URI_GRANTING, "foo"),
                Intent.FLAG_GRANT_READ_URI_PERMISSION);
    }

    /**
     * Test that we can't grant a permission to ourself.
     */
    public void testGrantWriteUriActivityPrivateToSelf() {
        doTryGrantUriActivityPermissionToSelf(
                Uri.withAppendedPath(PRIV_URI_GRANTING, "foo"),
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    }

    private void doTryGrantUriServicePermissionToSelf(Uri uri, int mode) {
        Uri grantDataUri = Uri.withAppendedPath(uri, "data");
        Intent grantIntent = new Intent();
        grantIntent.setData(grantDataUri);
        grantIntent.addFlags(mode);
        grantIntent.setClass(getContext(), ReceiveUriService.class);
        try {
            getContext().startService(grantIntent);
            fail("expected SecurityException granting " + grantDataUri + " to service");
        } catch (SecurityException e) {
            // This is what we want.
        }

        grantIntent = makeClipIntent(uri, mode);
        grantIntent.setClass(getContext(), ReceiveUriService.class);
        try {
            getContext().startService(grantIntent);
            fail("expected SecurityException granting " + grantIntent.getClipData() + " to service");
        } catch (SecurityException e) {
            // This is what we want.
        }
    }

    /**
     * Test that we can't grant a permission to ourself.
     */
    public void testGrantReadUriServicePermissionToSelf() {
        doTryGrantUriServicePermissionToSelf(
                Uri.withAppendedPath(PERM_URI_GRANTING, "foo"),
                Intent.FLAG_GRANT_READ_URI_PERMISSION);
    }

    /**
     * Test that we can't grant a permission to ourself.
     */
    public void testGrantWriteUriServicePermissionToSelf() {
        doTryGrantUriServicePermissionToSelf(
                Uri.withAppendedPath(PERM_URI_GRANTING, "foo"),
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    }

    /**
     * Test that we can't grant a permission to ourself.
     */
    public void testGrantReadUriServicePrivateToSelf() {
        doTryGrantUriServicePermissionToSelf(
                Uri.withAppendedPath(PRIV_URI_GRANTING, "foo"),
                Intent.FLAG_GRANT_READ_URI_PERMISSION);
    }

    /**
     * Test that we can't grant a permission to ourself.
     */
    public void testGrantWriteUriServicePrivateToSelf() {
        doTryGrantUriServicePermissionToSelf(
                Uri.withAppendedPath(PRIV_URI_GRANTING, "foo"),
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    }

    private static class GrantResultReceiver extends BroadcastReceiver {
        boolean mHaveResult = false;
        boolean mGoodResult = false;
        boolean mSucceeded = false;
        
        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (this) {
                mHaveResult = true;
                switch (getResultCode()) {
                    case GrantUriPermission.FAILURE:
                        mGoodResult = true;
                        mSucceeded = false;
                        break;
                    case GrantUriPermission.SUCCESS:
                        mGoodResult = true;
                        mSucceeded = true;
                        break;
                    default:
                        mGoodResult = false;
                        break;
                }
                notifyAll();
            }
        }
        
        void assertSuccess(String failureMessage) {
            synchronized (this) {
                final long startTime = SystemClock.uptimeMillis();
                while (!mHaveResult) {
                    try {
                        wait(5000);
                    } catch (InterruptedException e) {
                    }
                    if (SystemClock.uptimeMillis() >= (startTime+5000)) {
                        throw new RuntimeException("Timeout");
                    }
                }
                if (!mGoodResult) {
                    fail("Broadcast receiver did not return good result");
                }
                if (!mSucceeded) {
                    fail(failureMessage);
                }
            }
        }
        
        void assertFailure(String failureMessage) {
            synchronized (this) {
                final long startTime = SystemClock.uptimeMillis();
                while (!mHaveResult) {
                    try {
                        wait(5000);
                    } catch (InterruptedException e) {
                    }
                    if (SystemClock.uptimeMillis() >= (startTime+5000)) {
                        throw new RuntimeException("Timeout");
                    }
                }
                if (!mGoodResult) {
                    fail("Broadcast receiver did not return good result");
                }
                if (mSucceeded) {
                    fail(failureMessage);
                }
            }
        }
    }

    private void grantUriPermissionFail(Uri uri, int mode, boolean service) {
        Uri grantDataUri = Uri.withAppendedPath(uri, "data");
        Intent grantIntent = new Intent();
        grantIntent.setData(grantDataUri);
        grantIntent.addFlags(mode);
        grantIntent.setClass(getContext(),
                service ? ReceiveUriService.class : ReceiveUriActivity.class);
        Intent intent = new Intent();
        intent.setComponent(GRANT_URI_PERM_COMP);
        intent.setAction(service ? GrantUriPermission.ACTION_START_SERVICE
                : GrantUriPermission.ACTION_START_ACTIVITY);
        intent.putExtra(GrantUriPermission.EXTRA_INTENT, grantIntent);
        GrantResultReceiver receiver = new GrantResultReceiver();
        getContext().sendOrderedBroadcast(intent, null, receiver, null, 0, null, null);
        receiver.assertFailure("Able to grant URI permission to " + grantDataUri + " when should not");

        grantIntent = makeClipIntent(uri, mode);
        grantIntent.setClass(getContext(),
                service ? ReceiveUriService.class : ReceiveUriActivity.class);
        intent = new Intent();
        intent.setComponent(GRANT_URI_PERM_COMP);
        intent.setAction(service ? GrantUriPermission.ACTION_START_SERVICE
                : GrantUriPermission.ACTION_START_ACTIVITY);
        intent.putExtra(GrantUriPermission.EXTRA_INTENT, grantIntent);
        receiver = new GrantResultReceiver();
        getContext().sendOrderedBroadcast(intent, null, receiver, null, 0, null, null);
        receiver.assertFailure("Able to grant URI permission to " + grantIntent.getClipData()
                + " when should not");
    }

    private void doTestGrantUriPermissionFail(Uri uri) {
        grantUriPermissionFail(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION, false);
        grantUriPermissionFail(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION, false);
        grantUriPermissionFail(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION, true);
        grantUriPermissionFail(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION, true);
    }
    
    /**
     * Test that the ctspermissionwithsignature content provider can not grant
     * URI permissions to others.
     */
    public void testGrantPermissionNonGrantingFail() {
        doTestGrantUriPermissionFail(PERM_URI);
    }

    /**
     * Test that the ctspermissionwithsignaturegranting content provider can not grant
     * URI permissions to paths outside of the grant tree
     */
    public void testGrantPermissionOutsideGrantingFail() {
        doTestGrantUriPermissionFail(PERM_URI_GRANTING);
        doTestGrantUriPermissionFail(Uri.withAppendedPath(PERM_URI_GRANTING, "invalid"));
    }

    /**
     * Test that the ctsprivateprovider content provider can not grant
     * URI permissions to others.
     */
    public void testGrantPrivateNonGrantingFail() {
        doTestGrantUriPermissionFail(PRIV_URI);
    }

    /**
     * Test that the ctsambiguousprovider content provider can not grant
     * URI permissions to others.
     */
    public void testGrantAmbiguousNonGrantingFail() {
        doTestGrantUriPermissionFail(AMBIGUOUS_URI);
    }

    /**
     * Test that the ctsprivateprovidergranting content provider can not grant
     * URI permissions to paths outside of the grant tree
     */
    public void testGrantPrivateOutsideGrantingFail() {
        doTestGrantUriPermissionFail(PRIV_URI_GRANTING);
        doTestGrantUriPermissionFail(Uri.withAppendedPath(PRIV_URI_GRANTING, "invalid"));
    }

    private void grantClipUriPermission(ClipData clip, int mode, boolean service) {
        Intent grantIntent = new Intent();
        if (clip.getItemCount() == 1) {
            grantIntent.setData(clip.getItemAt(0).getUri());
        } else {
            grantIntent.setClipData(clip);
            // Make this Intent unique from the one that started it.
            for (int i=0; i<clip.getItemCount(); i++) {
                Uri uri = clip.getItemAt(i).getUri();
                if (uri != null) {
                    grantIntent.addCategory(uri.toString());
                }
            }
        }
        grantIntent.addFlags(mode);
        grantIntent.setClass(getContext(),
                service ? ReceiveUriService.class : ReceiveUriActivity.class);
        Intent intent = new Intent();
        intent.setComponent(GRANT_URI_PERM_COMP);
        intent.setAction(service ? GrantUriPermission.ACTION_START_SERVICE
                : GrantUriPermission.ACTION_START_ACTIVITY);
        intent.putExtra(GrantUriPermission.EXTRA_INTENT, grantIntent);
        getContext().sendBroadcast(intent);
    }

    private void assertReadingClipAllowed(ClipData clip) {
        for (int i=0; i<clip.getItemCount(); i++) {
            ClipData.Item item = clip.getItemAt(i);
            Uri uri = item.getUri();
            if (uri != null) {
                Cursor c = getContext().getContentResolver().query(uri,
                        null, null, null, null);
                if (c != null) {
                    c.close();
                }
            } else {
                Intent intent = item.getIntent();
                uri = intent.getData();
                if (uri != null) {
                    Cursor c = getContext().getContentResolver().query(uri,
                            null, null, null, null);
                    if (c != null) {
                        c.close();
                    }
                }
                ClipData intentClip = intent.getClipData();
                if (intentClip != null) {
                    assertReadingClipAllowed(intentClip);
                }
            }
        }
    }

    private void doTestGrantActivityUriReadPermission(Uri uri, boolean useClip) {
        final Uri subUri = Uri.withAppendedPath(uri, "foo");
        final Uri subSubUri = Uri.withAppendedPath(subUri, "bar");
        final Uri sub2Uri = Uri.withAppendedPath(uri, "yes");
        final Uri sub2SubUri = Uri.withAppendedPath(sub2Uri, "no");

        final ClipData subClip = useClip ? makeMultiClipData(subUri) : makeSingleClipData(subUri);
        final ClipData sub2Clip = useClip ? makeMultiClipData(sub2Uri) : makeSingleClipData(sub2Uri);

        // Precondition: no current access.
        assertReadingClipNotAllowed(subClip, "shouldn't read when starting test");
        assertReadingClipNotAllowed(sub2Clip, "shouldn't read when starting test");

        // --------------------------------

        ReceiveUriActivity.clearStarted();
        grantClipUriPermission(subClip, Intent.FLAG_GRANT_READ_URI_PERMISSION, false);
        ReceiveUriActivity.waitForStart();

        // See if we now have access to the provider.
        assertReadingClipAllowed(subClip);

        // But not writing.
        assertWritingClipNotAllowed(subClip, "shouldn't write from granted read");

        // And not to the base path.
        assertReadingContentUriNotAllowed(uri, "shouldn't read non-granted base URI");

        // And not to a sub path.
        assertReadingContentUriNotAllowed(subSubUri, "shouldn't read non-granted sub URI");

        // --------------------------------

        ReceiveUriActivity.clearNewIntent();
        grantClipUriPermission(sub2Clip, Intent.FLAG_GRANT_READ_URI_PERMISSION, false);
        ReceiveUriActivity.waitForNewIntent();

        if (false) {
            synchronized (this) {
                Log.i("**", "******************************* WAITING!!!");
                try {
                    wait(10000);
                } catch (InterruptedException e) {
                }
            }
        }

        // See if we now have access to the provider.
        assertReadingClipAllowed(sub2Clip);

        // And still have access to the original URI.
        assertReadingClipAllowed(subClip);

        // But not writing.
        assertWritingClipNotAllowed(sub2Clip, "shouldn't write from granted read");

        // And not to the base path.
        assertReadingContentUriNotAllowed(uri, "shouldn't read non-granted base URI");

        // And not to a sub path.
        assertReadingContentUriNotAllowed(sub2SubUri, "shouldn't read non-granted sub URI");

        // And make sure we can't generate a permission to a running activity.
        doTryGrantUriActivityPermissionToSelf(
                Uri.withAppendedPath(uri, "hah"),
                Intent.FLAG_GRANT_READ_URI_PERMISSION);
        doTryGrantUriActivityPermissionToSelf(
                Uri.withAppendedPath(uri, "hah"),
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        // --------------------------------

        // Dispose of activity.
        ReceiveUriActivity.finishCurInstanceSync();

        synchronized (this) {
            Log.i("**", "******************************* WAITING!!!");
            try {
                wait(100);
            } catch (InterruptedException e) {
            }
        }

        // Ensure reading no longer allowed.
        assertReadingClipNotAllowed(subClip, "shouldn't read after losing granted URI");
        assertReadingClipNotAllowed(sub2Clip, "shouldn't read after losing granted URI");
    }

    private void assertWritingClipAllowed(ClipData clip) {
        for (int i=0; i<clip.getItemCount(); i++) {
            ClipData.Item item = clip.getItemAt(i);
            Uri uri = item.getUri();
            if (uri != null) {
                getContext().getContentResolver().insert(uri, new ContentValues());
            } else {
                Intent intent = item.getIntent();
                uri = intent.getData();
                if (uri != null) {
                    getContext().getContentResolver().insert(uri, new ContentValues());
                }
                ClipData intentClip = intent.getClipData();
                if (intentClip != null) {
                    assertWritingClipAllowed(intentClip);
                }
            }
        }
    }

    private void doTestGrantActivityUriWritePermission(Uri uri, boolean useClip) {
        final Uri subUri = Uri.withAppendedPath(uri, "foo");
        final Uri subSubUri = Uri.withAppendedPath(subUri, "bar");
        final Uri sub2Uri = Uri.withAppendedPath(uri, "yes");
        final Uri sub2SubUri = Uri.withAppendedPath(sub2Uri, "no");

        final ClipData subClip = useClip ? makeMultiClipData(subUri) : makeSingleClipData(subUri);
        final ClipData sub2Clip = useClip ? makeMultiClipData(sub2Uri) : makeSingleClipData(sub2Uri);

        // Precondition: no current access.
        assertWritingClipNotAllowed(subClip, "shouldn't write when starting test");
        assertWritingClipNotAllowed(sub2Clip, "shouldn't write when starting test");

        // --------------------------------

        ReceiveUriActivity.clearStarted();
        grantClipUriPermission(subClip, Intent.FLAG_GRANT_WRITE_URI_PERMISSION, false);
        ReceiveUriActivity.waitForStart();

        // See if we now have access to the provider.
        assertWritingClipAllowed(subClip);

        // But not reading.
        assertReadingClipNotAllowed(subClip, "shouldn't read from granted write");

        // And not to the base path.
        assertWritingContentUriNotAllowed(uri, "shouldn't write non-granted base URI");

        // And not a sub-path.
        assertWritingContentUriNotAllowed(subSubUri, "shouldn't write non-granted sub URI");

        // --------------------------------

        ReceiveUriActivity.clearNewIntent();
        grantClipUriPermission(sub2Clip, Intent.FLAG_GRANT_WRITE_URI_PERMISSION, false);
        ReceiveUriActivity.waitForNewIntent();

        if (false) {
            synchronized (this) {
                Log.i("**", "******************************* WAITING!!!");
                try {
                    wait(10000);
                } catch (InterruptedException e) {
                }
            }
        }

        // See if we now have access to the provider.
        assertWritingClipAllowed(sub2Clip);

        // And still have access to the original URI.
        assertWritingClipAllowed(subClip);

        // But not reading.
        assertReadingClipNotAllowed(sub2Clip, "shouldn't read from granted write");

        // And not to the base path.
        assertWritingContentUriNotAllowed(uri, "shouldn't write non-granted base URI");

        // And not a sub-path.
        assertWritingContentUriNotAllowed(sub2SubUri, "shouldn't write non-granted sub URI");

        // And make sure we can't generate a permission to a running activity.
        doTryGrantUriActivityPermissionToSelf(
                Uri.withAppendedPath(uri, "hah"),
                Intent.FLAG_GRANT_READ_URI_PERMISSION);
        doTryGrantUriActivityPermissionToSelf(
                Uri.withAppendedPath(uri, "hah"),
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        // --------------------------------

        // Dispose of activity.
        ReceiveUriActivity.finishCurInstanceSync();

        synchronized (this) {
            Log.i("**", "******************************* WAITING!!!");
            try {
                wait(100);
            } catch (InterruptedException e) {
            }
        }

        // Ensure writing no longer allowed.
        assertWritingClipNotAllowed(subClip, "shouldn't write after losing granted URI");
        assertWritingClipNotAllowed(sub2Clip, "shouldn't write after losing granted URI");
    }

    /**
     * Test that the ctspermissionwithsignaturegranting content provider can grant a read
     * permission.
     */
    public void testGrantReadPermissionFromStartActivity() {
        doTestGrantActivityUriReadPermission(PERM_URI_GRANTING, false);
        doTestGrantActivityUriReadPermission(PERM_URI_GRANTING, true);
    }

    /**
     * Test that the ctspermissionwithsignaturegranting content provider can grant a write
     * permission.
     */
    public void testGrantWritePermissionFromStartActivity() {
        doTestGrantActivityUriWritePermission(PERM_URI_GRANTING, true);
        doTestGrantActivityUriWritePermission(PERM_URI_GRANTING, false);
    }

    /**
     * Test that the ctsprivateprovidergranting content provider can grant a read
     * permission.
     */
    public void testGrantReadPrivateFromStartActivity() {
        doTestGrantActivityUriReadPermission(PRIV_URI_GRANTING, false);
        doTestGrantActivityUriReadPermission(PRIV_URI_GRANTING, true);
    }

    /**
     * Test that the ctsprivateprovidergranting content provider can grant a write
     * permission.
     */
    public void testGrantWritePrivateFromStartActivity() {
        doTestGrantActivityUriWritePermission(PRIV_URI_GRANTING, true);
        doTestGrantActivityUriWritePermission(PRIV_URI_GRANTING, false);
    }

    private void doTestGrantServiceUriReadPermission(Uri uri, boolean useClip) {
        final Uri subUri = Uri.withAppendedPath(uri, "foo");
        final Uri subSubUri = Uri.withAppendedPath(subUri, "bar");
        final Uri sub2Uri = Uri.withAppendedPath(uri, "yes");
        final Uri sub2SubUri = Uri.withAppendedPath(sub2Uri, "no");

        ReceiveUriService.stop(getContext());

        final ClipData subClip = useClip ? makeMultiClipData(subUri) : makeSingleClipData(subUri);
        final ClipData sub2Clip = useClip ? makeMultiClipData(sub2Uri) : makeSingleClipData(sub2Uri);

        // Precondition: no current access.
        assertReadingClipNotAllowed(subClip, "shouldn't read when starting test");
        assertReadingClipNotAllowed(sub2Clip, "shouldn't read when starting test");

        // --------------------------------

        ReceiveUriService.clearStarted();
        grantClipUriPermission(subClip, Intent.FLAG_GRANT_READ_URI_PERMISSION, true);
        ReceiveUriService.waitForStart();

        int firstStartId = ReceiveUriService.getCurStartId();

        // See if we now have access to the provider.
        assertReadingClipAllowed(subClip);

        // But not writing.
        assertWritingClipNotAllowed(subClip, "shouldn't write from granted read");

        // And not to the base path.
        assertReadingContentUriNotAllowed(uri, "shouldn't read non-granted base URI");

        // And not to a sub path.
        assertReadingContentUriNotAllowed(subSubUri, "shouldn't read non-granted sub URI");

        // --------------------------------

        // Send another Intent to it.
        ReceiveUriService.clearStarted();
        grantClipUriPermission(sub2Clip, Intent.FLAG_GRANT_READ_URI_PERMISSION, true);
        ReceiveUriService.waitForStart();

        if (false) {
            synchronized (this) {
                Log.i("**", "******************************* WAITING!!!");
                try {
                    wait(10000);
                } catch (InterruptedException e) {
                }
            }
        }

        // See if we now have access to the provider.
        assertReadingClipAllowed(sub2Clip);

        // And still to the previous URI.
        assertReadingClipAllowed(subClip);

        // But not writing.
        assertWritingClipNotAllowed(sub2Clip, "shouldn't write from granted read");

        // And not to the base path.
        assertReadingContentUriNotAllowed(uri, "shouldn't read non-granted base URI");

        // And not to a sub path.
        assertReadingContentUriNotAllowed(sub2SubUri, "shouldn't read non-granted sub URI");

        // --------------------------------

        // Stop the first command.
        ReceiveUriService.stopCurWithId(firstStartId);

        // Ensure reading no longer allowed.
        assertReadingClipNotAllowed(subClip, "shouldn't read after losing granted URI");

        // And make sure we can't generate a permission to a running service.
        doTryGrantUriActivityPermissionToSelf(subUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION);
        doTryGrantUriActivityPermissionToSelf(subUri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        // --------------------------------

        // Dispose of service.
        ReceiveUriService.stopSync(getContext());

        // Ensure reading no longer allowed.
        assertReadingClipNotAllowed(subClip, "shouldn't read after losing granted URI");
        assertReadingClipNotAllowed(sub2Clip, "shouldn't read after losing granted URI");
    }

    private void doTestGrantServiceUriWritePermission(Uri uri, boolean useClip) {
        final Uri subUri = Uri.withAppendedPath(uri, "foo");
        final Uri subSubUri = Uri.withAppendedPath(subUri, "bar");
        final Uri sub2Uri = Uri.withAppendedPath(uri, "yes");
        final Uri sub2SubUri = Uri.withAppendedPath(sub2Uri, "no");

        ReceiveUriService.stop(getContext());

        final ClipData subClip = useClip ? makeMultiClipData(subUri) : makeSingleClipData(subUri);
        final ClipData sub2Clip = useClip ? makeMultiClipData(sub2Uri) : makeSingleClipData(sub2Uri);

        // Precondition: no current access.
        assertReadingClipNotAllowed(subClip, "shouldn't read when starting test");
        assertReadingClipNotAllowed(sub2Clip, "shouldn't read when starting test");

        // --------------------------------

        ReceiveUriService.clearStarted();
        grantClipUriPermission(subClip, Intent.FLAG_GRANT_WRITE_URI_PERMISSION, true);
        ReceiveUriService.waitForStart();

        int firstStartId = ReceiveUriService.getCurStartId();

        // See if we now have access to the provider.
        assertWritingClipAllowed(subClip);

        // But not reading.
        assertReadingClipNotAllowed(subClip, "shouldn't read from granted write");

        // And not to the base path.
        assertWritingContentUriNotAllowed(uri, "shouldn't write non-granted base URI");

        // And not a sub-path.
        assertWritingContentUriNotAllowed(subSubUri, "shouldn't write non-granted sub URI");

        // --------------------------------

        // Send another Intent to it.
        ReceiveUriService.clearStarted();
        grantClipUriPermission(sub2Clip, Intent.FLAG_GRANT_WRITE_URI_PERMISSION, true);
        ReceiveUriService.waitForStart();

        // See if we now have access to the provider.
        assertWritingClipAllowed(sub2Clip);

        // And still to the previous URI.
        assertWritingClipAllowed(subClip);

        // But not reading.
        assertReadingClipNotAllowed(sub2Clip, "shouldn't read from granted write");

        // And not to the base path.
        assertWritingContentUriNotAllowed(uri, "shouldn't write non-granted base URI");

        // And not a sub-path.
        assertWritingContentUriNotAllowed(sub2SubUri, "shouldn't write non-granted sub URI");

        if (false) {
            synchronized (this) {
                Log.i("**", "******************************* WAITING!!!");
                try {
                    wait(10000);
                } catch (InterruptedException e) {
                }
            }
        }

        // --------------------------------

        // Stop the first command.
        ReceiveUriService.stopCurWithId(firstStartId);

        // Ensure writing no longer allowed.
        assertWritingClipNotAllowed(subClip, "shouldn't write after losing granted URI");

        // And make sure we can't generate a permission to a running service.
        doTryGrantUriActivityPermissionToSelf(subUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION);
        doTryGrantUriActivityPermissionToSelf(subUri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        // --------------------------------

        // Dispose of service.
        ReceiveUriService.stopSync(getContext());

        // Ensure writing no longer allowed.
        assertWritingClipNotAllowed(subClip, "shouldn't write after losing granted URI");
        assertWritingClipNotAllowed(sub2Clip, "shouldn't write after losing granted URI");
    }

    public void testGrantReadPermissionFromStartService() {
        doTestGrantServiceUriReadPermission(PERM_URI_GRANTING, false);
        doTestGrantServiceUriReadPermission(PERM_URI_GRANTING, true);
    }

    public void testGrantWritePermissionFromStartService() {
        doTestGrantServiceUriWritePermission(PERM_URI_GRANTING, false);
        doTestGrantServiceUriWritePermission(PERM_URI_GRANTING, true);
    }

    public void testGrantReadPrivateFromStartService() {
        doTestGrantServiceUriReadPermission(PRIV_URI_GRANTING, false);
        doTestGrantServiceUriReadPermission(PRIV_URI_GRANTING, true);
    }

    public void testGrantWritePrivateFromStartService() {
        doTestGrantServiceUriWritePermission(PRIV_URI_GRANTING, false);
        doTestGrantServiceUriWritePermission(PRIV_URI_GRANTING, true);
    }

    /**
     * Test that ctspermissionwithsignaturepath can't grant read permissions
     * on paths it doesn't have permission to.
     */
    public void testGrantReadUriActivityPathPermissionToSelf() {
        doTryGrantUriActivityPermissionToSelf(PERM_URI_PATH,
                Intent.FLAG_GRANT_READ_URI_PERMISSION);
    }

    /**
     * Test that ctspermissionwithsignaturepath can't grant write permissions
     * on paths it doesn't have permission to.
     */
    public void testGrantWriteUriActivityPathPermissionToSelf() {
        doTryGrantUriActivityPermissionToSelf(PERM_URI_PATH,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    }

    /**
     * Test that ctspermissionwithsignaturepath can't grant read permissions
     * on paths it doesn't have permission to.
     */
    public void testGrantReadUriActivitySubPathPermissionToSelf() {
        doTryGrantUriActivityPermissionToSelf(
                Uri.withAppendedPath(PERM_URI_PATH, "foo"),
                Intent.FLAG_GRANT_READ_URI_PERMISSION);
    }

    /**
     * Test that ctspermissionwithsignaturepath can't grant write permissions
     * on paths it doesn't have permission to.
     */
    public void testGrantWriteUriActivitySubPathPermissionToSelf() {
        doTryGrantUriActivityPermissionToSelf(
                Uri.withAppendedPath(PERM_URI_PATH, "foo"),
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    }

    /**
     * Test that the ctspermissionwithsignaturepath content provider can grant a read
     * permission.
     */
    public void testGrantReadPathPermissionFromStartActivity() {
        doTestGrantActivityUriReadPermission(PERM_URI_PATH, false);
        doTestGrantActivityUriReadPermission(PERM_URI_PATH, true);
    }

    /**
     * Test that the ctspermissionwithsignaturepath content provider can grant a write
     * permission.
     */
    public void testGrantWritePathPermissionFromStartActivity() {
        doTestGrantActivityUriWritePermission(PERM_URI_PATH, false);
        doTestGrantActivityUriWritePermission(PERM_URI_PATH, true);
    }

    /**
     * Test that the ctspermissionwithsignaturepath content provider can grant a read
     * permission.
     */
    public void testGrantReadPathPermissionFromStartService() {
        doTestGrantServiceUriReadPermission(PERM_URI_PATH, false);
        doTestGrantServiceUriReadPermission(PERM_URI_PATH, true);
    }

    /**
     * Test that the ctspermissionwithsignaturepath content provider can grant a write
     * permission.
     */
    public void testGrantWritePathPermissionFromStartService() {
        doTestGrantServiceUriWritePermission(PERM_URI_PATH, false);
        doTestGrantServiceUriWritePermission(PERM_URI_PATH, true);
    }

    /**
     * Verify that we can access paths outside the {@code path-permission}
     * protections, which should only rely on {@code provider} permissions.
     */
    public void testRestrictingProviderNoMatchingPath() {
        assertReadingContentUriAllowed(PERM_URI_PATH_RESTRICTING);
        assertWritingContentUriAllowed(PERM_URI_PATH_RESTRICTING);

        // allowed by no top-level permission
        final Uri test = PERM_URI_PATH_RESTRICTING.buildUpon().appendPath("fo").build();
        assertReadingContentUriAllowed(test);
        assertWritingContentUriAllowed(test);
    }

    /**
     * Verify that paths under {@code path-permission} restriction aren't
     * allowed, even though the {@code provider} requires no permissions.
     */
    public void testRestrictingProviderMatchingPathDenied() {
        // rejected by "foo" prefix
        final Uri test1 = PERM_URI_PATH_RESTRICTING.buildUpon().appendPath("foo").build();
        assertReadingContentUriNotAllowed(test1, null);
        assertWritingContentUriNotAllowed(test1, null);

        // rejected by "foo" prefix
        final Uri test2 = PERM_URI_PATH_RESTRICTING.buildUpon()
                .appendPath("foo").appendPath("ba").build();
        assertReadingContentUriNotAllowed(test2, null);
        assertWritingContentUriNotAllowed(test2, null);
    }

    /**
     * Verify that at least one {@code path-permission} rule will grant access,
     * even if the caller doesn't hold another matching {@code path-permission}.
     */
    public void testRestrictingProviderMultipleMatchingPath() {
        // allowed by narrow "foo/bar" prefix
        final Uri test1 = PERM_URI_PATH_RESTRICTING.buildUpon()
                .appendPath("foo").appendPath("bar").build();
        assertReadingContentUriAllowed(test1);
        assertWritingContentUriAllowed(test1);

        // allowed by narrow "foo/bar" prefix
        final Uri test2 = PERM_URI_PATH_RESTRICTING.buildUpon()
                .appendPath("foo").appendPath("bar2").build();
        assertReadingContentUriAllowed(test2);
        assertWritingContentUriAllowed(test2);
    }

    public void testGetMimeTypePermission() {
        // Precondition: no current access.
        assertReadingContentUriNotAllowed(PERM_URI, "shouldn't read when starting test");
        assertWritingContentUriNotAllowed(PERM_URI, "shouldn't write when starting test");
        
        // All apps should be able to get MIME type regardless of permission.
        assertEquals(getContext().getContentResolver().getType(PERM_URI), EXPECTED_MIME_TYPE);
    }

    public void testGetMimeTypePrivate() {
        // Precondition: no current access.
        assertReadingContentUriNotAllowed(PRIV_URI, "shouldn't read when starting test");
        assertWritingContentUriNotAllowed(PRIV_URI, "shouldn't write when starting test");
        
        // All apps should be able to get MIME type even if provider is private.
        assertEquals(getContext().getContentResolver().getType(PRIV_URI), EXPECTED_MIME_TYPE);
    }

    public void testGetMimeTypeAmbiguous() {
        // Precondition: no current access.
        assertReadingContentUriNotAllowed(AMBIGUOUS_URI, "shouldn't read when starting test");
        assertWritingContentUriNotAllowed(AMBIGUOUS_URI, "shouldn't write when starting test");

        // All apps should be able to get MIME type even if provider is private.
        assertEquals(getContext().getContentResolver().getType(AMBIGUOUS_URI), EXPECTED_MIME_TYPE);
    }

    /**
     * Old App Compatibility Test
     *
     * We should be able to access the mime type of a content provider of an older
     * application, even if that application didn't explicitly declare either
     * exported=true or exported=false
     */
    public void testGetMimeTypeAmbiguousCompat() {
        // All apps should be able to get MIME type even if provider is private.
        assertEquals(EXPECTED_MIME_TYPE_AMBIGUOUS,
                getContext().getContentResolver().getType(AMBIGUOUS_URI_COMPAT));
    }

    /**
     * Validate behavior of persistable permission grants.
     */
    public void testGrantPersistableUriPermission() {
        final ContentResolver resolver = getContext().getContentResolver();

        final Uri target = Uri.withAppendedPath(PERM_URI_GRANTING, "foo");
        final ClipData clip = makeSingleClipData(target);

        // Make sure we can't see the target
        assertReadingClipNotAllowed(clip, "reading should have failed");
        assertWritingClipNotAllowed(clip, "writing should have failed");

        // Make sure we can't take a grant we don't have
        try {
            resolver.takePersistableUriPermission(target, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            fail("taking read should have failed");
        } catch (SecurityException expected) {
        }

        // And since we were just installed, no persisted grants yet
        assertNoPersistedUriPermission();

        // Now, let's grant ourselves some access
        ReceiveUriActivity.clearStarted();
        grantClipUriPermission(clip, Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION, false);
        ReceiveUriActivity.waitForStart();

        // We should now have reading access, even before taking the persistable
        // grant. Persisted grants should still be empty.
        assertReadingClipAllowed(clip);
        assertWritingClipNotAllowed(clip, "writing should have failed");
        assertNoPersistedUriPermission();

        // Take the read grant and verify we have it!
        long before = System.currentTimeMillis();
        resolver.takePersistableUriPermission(target, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        long after = System.currentTimeMillis();
        assertPersistedUriPermission(target, Intent.FLAG_GRANT_READ_URI_PERMISSION, before, after);

        // Make sure we can't take a grant we don't have
        try {
            resolver.takePersistableUriPermission(target, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            fail("taking write should have failed");
        } catch (SecurityException expected) {
        }

        // Launch again giving ourselves persistable read and write access
        ReceiveUriActivity.clearNewIntent();
        grantClipUriPermission(clip, Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION, false);
        ReceiveUriActivity.waitForNewIntent();

        // Previous persisted grant should be unchanged
        assertPersistedUriPermission(target, Intent.FLAG_GRANT_READ_URI_PERMISSION, before, after);

        // We should have both read and write; read is persisted, and write
        // isn't persisted yet.
        assertReadingClipAllowed(clip);
        assertWritingClipAllowed(clip);

        // Take again, but still only read; should just update timestamp
        before = System.currentTimeMillis();
        resolver.takePersistableUriPermission(target, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        after = System.currentTimeMillis();
        assertPersistedUriPermission(target, Intent.FLAG_GRANT_READ_URI_PERMISSION, before, after);

        // And take yet again, both read and write
        before = System.currentTimeMillis();
        resolver.takePersistableUriPermission(target,
                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        after = System.currentTimeMillis();
        assertPersistedUriPermission(target,
                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                before, after);

        // Now drop the persisted grant; write first, then read
        resolver.releasePersistableUriPermission(target, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        assertPersistedUriPermission(target, Intent.FLAG_GRANT_WRITE_URI_PERMISSION, before, after);
        resolver.releasePersistableUriPermission(target, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        assertNoPersistedUriPermission();

        // And even though we dropped the persistable grants, our activity is
        // still running with the global grants (until reboot).
        assertReadingClipAllowed(clip);
        assertWritingClipAllowed(clip);

        ReceiveUriActivity.finishCurInstanceSync();
    }

    private void assertNoPersistedUriPermission() {
        assertPersistedUriPermission(null, 0, -1, -1);
    }

    private void assertPersistedUriPermission(Uri uri, int flags, long before, long after) {
        // Assert local
        final List<UriPermission> perms = getContext()
                .getContentResolver().getPersistedUriPermissions();
        if (uri != null) {
            assertEquals("expected exactly one permission", 1, perms.size());

            final UriPermission perm = perms.get(0);
            assertEquals("unexpected uri", uri, perm.getUri());

            final long actual = perm.getPersistedTime();
            if (before != -1) {
                assertTrue("found " + actual + " before " + before, actual >= before);
            }
            if (after != -1) {
                assertTrue("found " + actual + " after " + after, actual <= after);
            }

            final boolean expectedRead = (flags & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0;
            final boolean expectedWrite = (flags & Intent.FLAG_GRANT_WRITE_URI_PERMISSION) != 0;
            assertEquals("unexpected read status", expectedRead, perm.isReadPermission());
            assertEquals("unexpected write status", expectedWrite, perm.isWritePermission());

        } else {
            assertEquals("expected zero permissions", 0, perms.size());
        }

        // And assert remote
        Intent intent = new Intent();
        intent.setComponent(GRANT_URI_PERM_COMP);
        intent.setAction(GrantUriPermission.ACTION_VERIFY_OUTGOING_PERSISTED);
        intent.putExtra(GrantUriPermission.EXTRA_URI, uri);
        GrantResultReceiver receiver = new GrantResultReceiver();
        getContext().sendOrderedBroadcast(intent, null, receiver, null, 0, null, null);
        receiver.assertSuccess("unexpected outgoing persisted Uri status");
    }
}
