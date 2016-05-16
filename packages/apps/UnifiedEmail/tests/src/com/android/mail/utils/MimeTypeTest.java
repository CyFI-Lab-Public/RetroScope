// Copyright 2011 Google Inc. All Rights Reserved.

package com.android.mail.utils;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

@SmallTest
public class MimeTypeTest extends AndroidTestCase {

    private static final String TEST_MIME_TYPE = "test/mimetype";
    public void testInferMimeType() {
        // eml file
        assertEquals(MimeType.EML_ATTACHMENT_CONTENT_TYPE,
                MimeType.inferMimeType("filename.eml", MimeType.GENERIC_MIMETYPE));

        // mpeg4 video files
        assertEquals("video/mp4", MimeType.inferMimeType("video.mp4", MimeType.GENERIC_MIMETYPE));

        // file with no extension, should return the mimetype that was specified
        assertEquals(TEST_MIME_TYPE, MimeType.inferMimeType("filename", TEST_MIME_TYPE));

        // file with extension, and empty mimetype, where an mimetype can be derived
        // from the extension.
        assertEquals("video/mp4", MimeType.inferMimeType("video.mp4", ""));

        // file with extension, and empty mimetype, where an mimetype can not be derived
        // from the extension.
        assertEquals(MimeType.GENERIC_MIMETYPE, MimeType.inferMimeType("video.foo", ""));

        // rtf files, with a generic mimetype
        assertEquals("text/rtf", MimeType.inferMimeType("filename.rtf", MimeType.GENERIC_MIMETYPE));

        // rtf files, with a specified mimetype
        assertEquals("application/rtf", MimeType.inferMimeType("filename.rtf", "application/rtf"));
    }
}
