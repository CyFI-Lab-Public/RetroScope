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

package com.android.cts.videoperf;

import android.media.MediaCodecInfo;
import android.media.MediaCodecInfo.CodecCapabilities;
import android.media.MediaCodecInfo.CodecProfileLevel;
import android.media.MediaCodecList;
import android.util.Log;


/**
 * Utility class for getting codec information like bit rate, fps, and etc.
 * Uses public member variables instead of methods as this code is only for video benchmarking.
 */
public class CodecInfo {
    /** bit rate in bps */
    public int mBitRate = 0;
    /** Frame rate */
    public int mFps = 0;
    /** if codec is supporting YUV semiplanar format */
    public boolean mSupportSemiPlanar = false;
    /** if codec is supporting YUV planar format */
    public boolean mSupportPlanar = false;

    private static final String TAG = "CodecInfo";
    private static final String VIDEO_AVC = "video/avc";
    /**
     * Check if given codec with given (w,h) is supported.
     * @param mimeType codec type in mime format like "video/avc"
     * @param w video width
     * @param h video height
     * @param isEncoder whether the codec is encoder or decoder
     * @return null if the configuration is not supported.
     */
    public static CodecInfo getSupportedFormatInfo(String mimeType, int w, int h,
            boolean isEncoder) {
        CodecCapabilities cap = getCodecCapability(mimeType, isEncoder);
        if (cap == null) { // not supported
            return null;
        }
        CodecInfo info = new CodecInfo();
        for (int color : cap.colorFormats) {
            if (color == CodecCapabilities.COLOR_FormatYUV420SemiPlanar) {
                info.mSupportSemiPlanar = true;
            }
            if (color == CodecCapabilities.COLOR_FormatYUV420Planar) {
                info.mSupportPlanar = true;
            }
        }
        printIntArray("supported colors", cap.colorFormats);
        //  either YUV420 planar or semiplanar should be supported
        if (!info.mSupportPlanar && !info.mSupportSemiPlanar) {
            Log.i(TAG, "no supported color format");
            return null;
        }

        if (mimeType.equals(VIDEO_AVC)) {
            int highestLevel = 0;
            for (CodecProfileLevel lvl : cap.profileLevels) {
                if (lvl.level > highestLevel) {
                    highestLevel = lvl.level;
                }
            }
            Log.i(TAG, "Avc highest level " + Integer.toHexString(highestLevel));
            int maxW = 0;
            int maxH = 0;
            int bitRate = 0;
            int mbW = (w + 15) / 16; // size in macroblocks
            int mbH = (h + 15) / 16;
            int maxMacroblocksPerSecond = 0; // max decoding speed
            switch(highestLevel) {
            // Do not support Level 1 to 2.
            case CodecProfileLevel.AVCLevel1:
            case CodecProfileLevel.AVCLevel11:
            case CodecProfileLevel.AVCLevel12:
            case CodecProfileLevel.AVCLevel13:
            case CodecProfileLevel.AVCLevel1b:
            case CodecProfileLevel.AVCLevel2:
                return null;
            case CodecProfileLevel.AVCLevel21:
                maxW = 352;
                maxH = 576;
                bitRate = 4000000;
                maxMacroblocksPerSecond = 19800;
                break;
            case CodecProfileLevel.AVCLevel22:
                maxW = 720;
                maxH = 480;
                bitRate = 4000000;
                maxMacroblocksPerSecond = 20250;
                break;
            case CodecProfileLevel.AVCLevel3:
                maxW = 720;
                maxH = 480;
                bitRate = 10000000;
                maxMacroblocksPerSecond = 40500;
                break;
            case CodecProfileLevel.AVCLevel31:
                maxW = 1280;
                maxH = 720;
                bitRate = 14000000;
                maxMacroblocksPerSecond = 108000;
                break;
            case CodecProfileLevel.AVCLevel32:
                maxW = 1280;
                maxH = 720;
                bitRate = 20000000;
                maxMacroblocksPerSecond = 216000;
                break;
            case CodecProfileLevel.AVCLevel4:
                maxW = 1920;
                maxH = 1080;
                bitRate = 20000000;
                maxMacroblocksPerSecond = 245760;
                break;
            case CodecProfileLevel.AVCLevel41:
                maxW = 1920;
                maxH = 1080;
                bitRate = 50000000;
                maxMacroblocksPerSecond = 245760;
                break;
            case CodecProfileLevel.AVCLevel42:
                maxW = 2048;
                maxH = 1080;
                bitRate = 50000000;
                maxMacroblocksPerSecond = 522240;
                break;
            case CodecProfileLevel.AVCLevel5:
                maxW = 3672;
                maxH = 1536;
                bitRate = 135000000;
                maxMacroblocksPerSecond = 589824;
                break;
            case CodecProfileLevel.AVCLevel51:
            default:
                maxW = 4096;
                maxH = 2304;
                bitRate = 240000000;
                maxMacroblocksPerSecond = 983040;
                break;
            }
            if ((w > maxW) || (h > maxH)) {
                Log.i(TAG, "Requested resolution (" + w + "," + h + ") exceeds (" +
                        maxW + "," + maxH + ")");
                return null;
            }
            info.mFps = maxMacroblocksPerSecond / mbH / mbW;
            info.mBitRate = bitRate;
            Log.i(TAG, "AVC Level " + Integer.toHexString(highestLevel) + " bit rate " + bitRate +
                    " fps " + info.mFps);
        }
        return info;
    }

    /**
     * Search for given codecName and returns CodecCapabilities if found
     * @param codecName
     * @param isEncoder true for encoder, false for decoder
     * @return null if the codec is not supported
     */
    private static CodecCapabilities getCodecCapability(
            String codecName, boolean isEncoder) {
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
                    Log.i(TAG, "Use codec " + info.getName());
                    return cap;
                }
            }
        }
        return null;
    }

    // for debugging
    private static void printIntArray(String msg, int[] data) {
        StringBuilder builder = new StringBuilder();
        builder.append(msg);
        builder.append(":");
        for (int e : data) {
            builder.append(Integer.toHexString(e));
            builder.append(",");
        }
        builder.deleteCharAt(builder.length() - 1);
        Log.i(TAG, builder.toString());
    }
}
