/*
 * Copyright (C) 2013 The Android Open Source Project
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

package android.provider.cts;

import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.provider.MediaStore;
import android.test.AndroidTestCase;

import java.util.List;

/**
 * Tests to verify that common actions on {@link MediaStore} content are
 * available.
 */
public class MediaStoreIntentsTest extends AndroidTestCase {
    public void assertCanBeHandled(Intent intent) {
        List<ResolveInfo> resolveInfoList = getContext()
                .getPackageManager().queryIntentActivities(intent, 0);
        assertNotNull("Missing ResolveInfo", resolveInfoList);
        assertTrue("No ResolveInfo found for " + intent.toInsecureString(),
                resolveInfoList.size() > 0);
    }

    public void testPickImageDir() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        assertCanBeHandled(intent);
    }

    public void testPickVideoDir() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setData(MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        assertCanBeHandled(intent);
    }

    public void testPickAudioDir() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setData(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
        assertCanBeHandled(intent);
    }

    public void testViewImageDir() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        assertCanBeHandled(intent);
    }

    public void testViewVideoDir() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        assertCanBeHandled(intent);
    }

    public void testViewImageFile() {
        final String[] schemes = new String[] {
                "file", "http", "https", "content" };
        final String[] mimes = new String[] {
                "image/bmp", "image/jpeg", "image/png", "image/gif", "image/webp" };

        for (String scheme : schemes) {
            for (String mime : mimes) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                final Uri uri = new Uri.Builder().scheme(scheme)
                        .authority("example.com").path("image").build();
                intent.setDataAndType(uri, mime);
                assertCanBeHandled(intent);
            }
        }
    }

    public void testViewVideoFile() {
        final String[] schemes = new String[] {
                "file", "http", "https", "content" };
        final String[] mimes = new String[] {
                "video/mpeg4", "video/mp4", "video/3gp", "video/3gpp", "video/3gpp2",
                "video/webm" };

        for (String scheme : schemes) {
            for (String mime : mimes) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                final Uri uri = new Uri.Builder().scheme(scheme)
                        .authority("example.com").path("video").build();
                intent.setDataAndType(uri, mime);
                assertCanBeHandled(intent);
            }
        }
    }

    public void testViewAudioFile() {
        final String[] schemes = new String[] {
                "file", "http", "content" };
        final String[] mimes = new String[] {
                "audio/mpeg", "audio/mp4", "audio/ogg", "audio/webm", "application/ogg",
                "application/x-ogg" };

        for (String scheme : schemes) {
            for (String mime : mimes) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                final Uri uri = new Uri.Builder().scheme(scheme)
                        .authority("example.com").path("audio").build();
                intent.setDataAndType(uri, mime);
                assertCanBeHandled(intent);
            }
        }
    }
}
