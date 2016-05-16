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


import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecInfo.CodecProfileLevel;
import android.media.MediaCodecInfo.CodecCapabilities;
import android.media.MediaCodecList;
import android.test.AndroidTestCase;
import android.util.Log;

import java.io.File;
import java.util.List;
import java.util.ArrayList;

public class MediaCodecListTest extends AndroidTestCase {

    private static final String TAG = "MediaCodecListTest";
    private static final String MEDIA_CODEC_XML_FILE = "/etc/media_codecs.xml";

    class CodecType {
        CodecType(String type, boolean isEncoder) {
            mMimeTypeName = type;
            mIsEncoder = isEncoder;
        }

        boolean equals(CodecType codecType) {
            return (mMimeTypeName.compareTo(codecType.mMimeTypeName) == 0) &&
                    mIsEncoder == codecType.mIsEncoder;
        }

        String mMimeTypeName;
        boolean mIsEncoder;
    };

    public static void testMediaCodecXmlFileExist() {
        File file = new File(MEDIA_CODEC_XML_FILE);
        assertTrue("/etc/media_codecs.xml does not exist", file.exists());
    }

    // Each component advertised by MediaCodecList should at least be
    // instantiate-able.
    public void testComponentInstantiation() {
        Log.d(TAG, "testComponentInstantiation");

        int codecCount = MediaCodecList.getCodecCount();
        for (int i = 0; i < codecCount; ++i) {
            MediaCodecInfo info = MediaCodecList.getCodecInfoAt(i);

            Log.d(TAG, (i + 1) + ": " + info.getName());
            Log.d(TAG, "  isEncoder = " + info.isEncoder());

            if (!info.getName().startsWith("OMX.")) {
                // Unfortunately for legacy reasons, "AACEncoder", a
                // non OMX component had to be in this list for the video
                // editor code to work... but it cannot actually be instantiated
                // using MediaCodec.
                Log.d(TAG, "  skipping...");
                continue;
            }

            MediaCodec codec = MediaCodec.createByCodecName(info.getName());

            codec.release();
            codec = null;
        }
    }

    // For each type advertised by any of the components we should be able
    // to get capabilities.
    public void testGetCapabilities() {
        Log.d(TAG, "testGetCapabilities");

        int codecCount = MediaCodecList.getCodecCount();
        for (int i = 0; i < codecCount; ++i) {
            MediaCodecInfo info = MediaCodecList.getCodecInfoAt(i);

            Log.d(TAG, (i + 1) + ": " + info.getName());
            Log.d(TAG, "  isEncoder = " + info.isEncoder());

            if (!info.getName().startsWith("OMX.")) {
                // Unfortunately for legacy reasons, "AACEncoder", a
                // non OMX component had to be in this list for the video
                // editor code to work... but it cannot actually be instantiated
                // using MediaCodec.
                Log.d(TAG, "  skipping...");
                continue;
            }

            String[] types = info.getSupportedTypes();
            for (int j = 0; j < types.length; ++j) {
                Log.d(TAG, "calling getCapabilitiesForType " + types[j]);
                CodecCapabilities cap = info.getCapabilitiesForType(types[j]);
            }
        }
    }

    public void testRequiredMediaCodecList() {
        List<CodecType> requiredList = getRequiredCodecTypes();
        List<CodecType> supportedList = getSupportedCodecTypes();
        assertTrue(areRequiredCodecTypesSupported(requiredList, supportedList));
    }

    // H263 baseline profile must be supported
    public void testIsH263BaselineProfileSupported() {
        int profile = CodecProfileLevel.H263ProfileBaseline;
        assertTrue(checkProfileSupported("video/3gpp", false, profile));
        assertTrue(checkProfileSupported("video/3gpp", true, profile));
    }

    // AVC baseline profile must be supported
    public void testIsAVCBaselineProfileSupported() {
        int profile = CodecProfileLevel.AVCProfileBaseline;
        assertTrue(checkProfileSupported("video/avc", false, profile));
        assertTrue(checkProfileSupported("video/avc", true, profile));
    }

    // MPEG4 simple profile must be supported
    public void testIsM4VSimpleProfileSupported() {
        int profile = CodecProfileLevel.MPEG4ProfileSimple;
        assertTrue(checkProfileSupported("video/mp4v-es", false, profile));

        // FIXME: no support for M4v simple profile video encoder
        // assertTrue(checkProfileSupported("video/mp4v-es", true, profile));
    }

    /*
     * Find whether the given codec is supported
     */
    private boolean checkProfileSupported(
        String codecName, boolean isEncoder, int profile) {

        boolean isSupported = false;

        int codecCount = MediaCodecList.getCodecCount();
        for (int i = 0; i < codecCount; ++i) {
            MediaCodecInfo info = MediaCodecList.getCodecInfoAt(i);
            String[] types = info.getSupportedTypes();

            if (isEncoder != info.isEncoder()) {
                continue;
            }

            for (int j = 0; j < types.length; ++j) {
                if (types[j].compareTo(codecName) == 0) {
                    CodecCapabilities cap = info.getCapabilitiesForType(types[j]);
                    CodecProfileLevel[] profileLevels = cap.profileLevels;
                    for (int k = 0; k < profileLevels.length; ++k) {
                        if (profileLevels[k].profile == profile) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /*
     * Find whether all required media codec types are supported
     */
    private boolean areRequiredCodecTypesSupported(
        List<CodecType> requiredList, List<CodecType> supportedList) {
        for (CodecType requiredCodec: requiredList) {
            boolean isSupported = false;
            for (CodecType supportedCodec: supportedList) {
                if (requiredCodec.equals(supportedCodec)) {
                    isSupported = true;
                }
            }
            if (!isSupported) {
                String codec = requiredCodec.mMimeTypeName
                                + ", " + (requiredCodec.mIsEncoder? "encoder": "decoder");
                Log.e(TAG, "Media codec (" + codec + ") is not supported");
                return false;
            }
        }
        return true;
    }

    /*
     * Find all the media codec types are supported.
     */
    private List<CodecType> getSupportedCodecTypes() {
        int codecCount = MediaCodecList.getCodecCount();
        assertTrue("Unexpected media codec count", codecCount > 0);
        List<CodecType> supportedList = new ArrayList<CodecType>(codecCount);
        for (int i = 0; i < codecCount; ++i) {
            MediaCodecInfo info = MediaCodecList.getCodecInfoAt(i);
            String[] types = info.getSupportedTypes();
            assertTrue("Unexpected number of supported types", types.length > 0);
            boolean isEncoder = info.isEncoder();
            for (int j = 0; j < types.length; ++j) {
                supportedList.add(new CodecType(types[j], isEncoder));
            }
        }
        return supportedList;
    }

    /*
     * This list should be kept in sync with the CCD document
     * See http://developer.android.com/guide/appendix/media-formats.html
     */
    private List<CodecType> getRequiredCodecTypes() {
        List<CodecType> list = new ArrayList<CodecType>(16);

        // Mandatory audio codecs
        list.add(new CodecType("audio/amr-wb", false));         // amrwb decoder
        list.add(new CodecType("audio/amr-wb", true));          // amrwb encoder

        // flac decoder is not omx-based yet
        // list.add(new CodecType("audio/flac", false));        // flac decoder
        list.add(new CodecType("audio/flac", true));            // flac encoder
        list.add(new CodecType("audio/mpeg", false));           // mp3 decoder
        list.add(new CodecType("audio/mp4a-latm", false));      // aac decoder
        list.add(new CodecType("audio/mp4a-latm", true));       // aac encoder
        list.add(new CodecType("audio/vorbis", false));         // vorbis decoder
        list.add(new CodecType("audio/3gpp", false));           // amrnb decoder
        list.add(new CodecType("audio/3gpp", true));            // amrnb encoder

        // Mandatory video codecs
        list.add(new CodecType("video/avc", false));            // avc decoder
        list.add(new CodecType("video/avc", true));             // avc encoder
        list.add(new CodecType("video/3gpp", false));           // h263 decoder
        list.add(new CodecType("video/3gpp", true));            // h263 encoder
        list.add(new CodecType("video/mp4v-es", false));        // m4v decoder
        list.add(new CodecType("video/x-vnd.on2.vp8", false));  // vp8 decoder
        list.add(new CodecType("video/x-vnd.on2.vp9", false));  // vp9 decoder

        return list;
    }
}
