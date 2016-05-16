/*
 * Copyright (C) 2011 The Android Open Source Project
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

/**
*************************************************************************
* @file   VideoEditorUtils.cpp
* @brief  StageFright shell Utilities
*************************************************************************
*/
#define LOG_NDEBUG 0
#define LOG_TAG "SF_utils"
#include "utils/Log.h"

#include "VideoEditorUtils.h"

#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/MediaErrors.h>
#include <media/stagefright/MediaExtractor.h>
#include <media/stagefright/MediaBuffer.h>
#include <media/stagefright/MetaData.h>
#include <media/stagefright/OMXCodec.h>

/* Android includes*/
#include <utils/Log.h>
#include <memory.h>

/*---------------------*/
/*  DEBUG LEVEL SETUP  */
/*---------------------*/
#define LOG1 ALOGE    /*ERRORS Logging*/
#define LOG2 ALOGI    /*WARNING Logging*/
#define LOG3 //ALOGV  /*COMMENTS Logging*/

namespace android {

void displayMetaData(const sp<MetaData> meta) {

    const char* charData;
    int32_t int32Data;
    int64_t int64Data;
    uint32_t type;
    const void* data;
    void* ptr;
    size_t size;

    if (meta->findCString(kKeyMIMEType, &charData)) {
        LOG1("displayMetaData kKeyMIMEType %s", charData);
    }
    if (meta->findInt32(kKeyWidth, &int32Data)) {
        LOG1("displayMetaData kKeyWidth %d", int32Data);
    }
    if (meta->findInt32(kKeyHeight, &int32Data)) {
        LOG1("displayMetaData kKeyHeight %d", int32Data);
    }
    if (meta->findInt32(kKeyIFramesInterval, &int32Data)) {
        LOG1("displayMetaData kKeyIFramesInterval %d", int32Data);
    }
    if (meta->findInt32(kKeyStride, &int32Data)) {
        LOG1("displayMetaData kKeyStride %d", int32Data);
    }
    if (meta->findInt32(kKeySliceHeight, &int32Data)) {
        LOG1("displayMetaData kKeySliceHeight %d", int32Data);
    }
    if (meta->findInt32(kKeyChannelCount, &int32Data)) {
        LOG1("displayMetaData kKeyChannelCount %d", int32Data);
    }
    if (meta->findInt32(kKeySampleRate, &int32Data)) {
        LOG1("displayMetaData kKeySampleRate %d", int32Data);
    }
    if (meta->findInt32(kKeyBitRate, &int32Data)) {
        LOG1("displayMetaData kKeyBitRate %d", int32Data);
    }
    if (meta->findData(kKeyESDS, &type, &data, &size)) {
        LOG1("displayMetaData kKeyESDS type=%d size=%d", type, size);
    }
    if (meta->findData(kKeyAVCC, &type, &data, &size)) {
        LOG1("displayMetaData kKeyAVCC data=0x%X type=%d size=%d",
            *((unsigned int*)data), type, size);
    }
    if (meta->findData(kKeyVorbisInfo, &type, &data, &size)) {
        LOG1("displayMetaData kKeyVorbisInfo type=%d size=%d", type, size);
    }
    if (meta->findData(kKeyVorbisBooks, &type, &data, &size)) {
        LOG1("displayMetaData kKeyVorbisBooks type=%d size=%d", type, size);
    }
    if (meta->findInt32(kKeyWantsNALFragments, &int32Data)) {
        LOG1("displayMetaData kKeyWantsNALFragments %d", int32Data);
    }
    if (meta->findInt32(kKeyIsSyncFrame, &int32Data)) {
        LOG1("displayMetaData kKeyIsSyncFrame %d", int32Data);
    }
    if (meta->findInt32(kKeyIsCodecConfig, &int32Data)) {
        LOG1("displayMetaData kKeyIsCodecConfig %d", int32Data);
    }
    if (meta->findInt64(kKeyTime, &int64Data)) {
        LOG1("displayMetaData kKeyTime %lld", int64Data);
    }
    if (meta->findInt32(kKeyDuration, &int32Data)) {
        LOG1("displayMetaData kKeyDuration %d", int32Data);
    }
    if (meta->findInt32(kKeyColorFormat, &int32Data)) {
        LOG1("displayMetaData kKeyColorFormat %d", int32Data);
    }
    if (meta->findPointer(kKeyPlatformPrivate, &ptr)) {
        LOG1("displayMetaData kKeyPlatformPrivate pointer=0x%x", (int32_t) ptr);
    }
    if (meta->findCString(kKeyDecoderComponent, &charData)) {
        LOG1("displayMetaData kKeyDecoderComponent %s", charData);
    }
    if (meta->findInt32(kKeyBufferID, &int32Data)) {
        LOG1("displayMetaData kKeyBufferID %d", int32Data);
    }
    if (meta->findInt32(kKeyMaxInputSize, &int32Data)) {
        LOG1("displayMetaData kKeyMaxInputSize %d", int32Data);
    }
    if (meta->findInt64(kKeyThumbnailTime, &int64Data)) {
        LOG1("displayMetaData kKeyThumbnailTime %lld", int64Data);
    }
    if (meta->findCString(kKeyAlbum, &charData)) {
        LOG1("displayMetaData kKeyAlbum %s", charData);
    }
    if (meta->findCString(kKeyArtist, &charData)) {
        LOG1("displayMetaData kKeyArtist %s", charData);
    }
    if (meta->findCString(kKeyAlbumArtist, &charData)) {
        LOG1("displayMetaData kKeyAlbumArtist %s", charData);
    }
    if (meta->findCString(kKeyComposer, &charData)) {
        LOG1("displayMetaData kKeyComposer %s", charData);
    }
    if (meta->findCString(kKeyGenre, &charData)) {
        LOG1("displayMetaData kKeyGenre %s", charData);
    }
    if (meta->findCString(kKeyTitle, &charData)) {
        LOG1("displayMetaData kKeyTitle %s", charData);
    }
    if (meta->findCString(kKeyYear, &charData)) {
        LOG1("displayMetaData kKeyYear %s", charData);
    }
    if (meta->findData(kKeyAlbumArt, &type, &data, &size)) {
        LOG1("displayMetaData kKeyAlbumArt type=%d size=%d", type, size);
    }
    if (meta->findCString(kKeyAlbumArtMIME, &charData)) {
        LOG1("displayMetaData kKeyAlbumArtMIME %s", charData);
    }
    if (meta->findCString(kKeyAuthor, &charData)) {
        LOG1("displayMetaData kKeyAuthor %s", charData);
    }
    if (meta->findCString(kKeyCDTrackNumber, &charData)) {
        LOG1("displayMetaData kKeyCDTrackNumber %s", charData);
    }
    if (meta->findCString(kKeyDiscNumber, &charData)) {
        LOG1("displayMetaData kKeyDiscNumber %s", charData);
    }
    if (meta->findCString(kKeyDate, &charData)) {
        LOG1("displayMetaData kKeyDate %s", charData);
    }
    if (meta->findCString(kKeyWriter, &charData)) {
        LOG1("displayMetaData kKeyWriter %s", charData);
    }
    if (meta->findInt32(kKeyTimeScale, &int32Data)) {
        LOG1("displayMetaData kKeyTimeScale %d", int32Data);
    }
    if (meta->findInt32(kKeyVideoProfile, &int32Data)) {
        LOG1("displayMetaData kKeyVideoProfile %d", int32Data);
    }
    if (meta->findInt32(kKeyVideoLevel, &int32Data)) {
        LOG1("displayMetaData kKeyVideoLevel %d", int32Data);
    }
    if (meta->findInt32(kKey64BitFileOffset, &int32Data)) {
        LOG1("displayMetaData kKey64BitFileOffset %d", int32Data);
    }
    if (meta->findInt32(kKeyFileType, &int32Data)) {
        LOG1("displayMetaData kKeyFileType %d", int32Data);
    }
    if (meta->findInt64(kKeyTrackTimeStatus, &int64Data)) {
        LOG1("displayMetaData kKeyTrackTimeStatus %lld", int64Data);
    }
    if (meta->findInt32(kKeyRealTimeRecording, &int32Data)) {
        LOG1("displayMetaData kKeyRealTimeRecording %d", int32Data);
    }
}

/**
 * This code was extracted from StageFright MPEG4 writer
 * Is is used to parse and format the AVC codec specific info received
 * from StageFright encoders
 */
static const uint8_t kNalUnitTypeSeqParamSet = 0x07;
static const uint8_t kNalUnitTypePicParamSet = 0x08;
struct AVCParamSet {
    AVCParamSet(uint16_t length, const uint8_t *data)
        : mLength(length), mData(data) {}

    uint16_t mLength;
    const uint8_t *mData;
};
struct AVCCodecSpecificContext {
    List<AVCParamSet> mSeqParamSets;
    List<AVCParamSet> mPicParamSets;
    uint8_t mProfileIdc;
    uint8_t mProfileCompatible;
    uint8_t mLevelIdc;
};

const uint8_t *parseParamSet(AVCCodecSpecificContext* pC,
        const uint8_t *data, size_t length, int type, size_t *paramSetLen) {
    CHECK(type == kNalUnitTypeSeqParamSet ||
          type == kNalUnitTypePicParamSet);

    size_t bytesLeft = length;
    while (bytesLeft > 4  &&
            memcmp("\x00\x00\x00\x01", &data[length - bytesLeft], 4)) {
        --bytesLeft;
    }
    if (bytesLeft <= 4) {
        bytesLeft = 0; // Last parameter set
    }
    const uint8_t *nextStartCode = &data[length - bytesLeft];
    *paramSetLen = nextStartCode - data;
    if (*paramSetLen == 0) {
        ALOGE("Param set is malformed, since its length is 0");
        return NULL;
    }

    AVCParamSet paramSet(*paramSetLen, data);
    if (type == kNalUnitTypeSeqParamSet) {
        if (*paramSetLen < 4) {
            ALOGE("Seq parameter set malformed");
            return NULL;
        }
        if (pC->mSeqParamSets.empty()) {
            pC->mProfileIdc = data[1];
            pC->mProfileCompatible = data[2];
            pC->mLevelIdc = data[3];
        } else {
            if (pC->mProfileIdc != data[1] ||
                pC->mProfileCompatible != data[2] ||
                pC->mLevelIdc != data[3]) {
                ALOGV("Inconsistent profile/level found in seq parameter sets");
                return NULL;
            }
        }
        pC->mSeqParamSets.push_back(paramSet);
    } else {
        pC->mPicParamSets.push_back(paramSet);
    }
    return nextStartCode;
}

status_t buildAVCCodecSpecificData(uint8_t **pOutputData, size_t *pOutputSize,
        const uint8_t *data, size_t size, MetaData *param)
{
    //ALOGV("buildAVCCodecSpecificData");

    if ( (pOutputData == NULL) || (pOutputSize == NULL) ) {
        ALOGE("output is invalid");
        return ERROR_MALFORMED;
    }

    if (*pOutputData != NULL) {
        ALOGE("Already have codec specific data");
        return ERROR_MALFORMED;
    }

    if (size < 4) {
        ALOGE("Codec specific data length too short: %d", size);
        return ERROR_MALFORMED;
    }

    // Data is in the form of AVCCodecSpecificData
    if (memcmp("\x00\x00\x00\x01", data, 4)) {
        // 2 bytes for each of the parameter set length field
        // plus the 7 bytes for the header
        if (size < 4 + 7) {
            ALOGE("Codec specific data length too short: %d", size);
            return ERROR_MALFORMED;
        }

        *pOutputSize = size;
        *pOutputData = (uint8_t*)malloc(size);
        memcpy(*pOutputData, data, size);
        return OK;
    }

    AVCCodecSpecificContext ctx;
    uint8_t *outputData = NULL;
    size_t outputSize = 0;

    // Check if the data is valid
    uint8_t type = kNalUnitTypeSeqParamSet;
    bool gotSps = false;
    bool gotPps = false;
    const uint8_t *tmp = data;
    const uint8_t *nextStartCode = data;
    size_t bytesLeft = size;
    size_t paramSetLen = 0;
    outputSize = 0;
    while (bytesLeft > 4 && !memcmp("\x00\x00\x00\x01", tmp, 4)) {
        type = (*(tmp + 4)) & 0x1F;
        if (type == kNalUnitTypeSeqParamSet) {
            if (gotPps) {
                ALOGE("SPS must come before PPS");
                return ERROR_MALFORMED;
            }
            if (!gotSps) {
                gotSps = true;
            }
            nextStartCode = parseParamSet(&ctx, tmp + 4, bytesLeft - 4, type,
                &paramSetLen);
        } else if (type == kNalUnitTypePicParamSet) {
            if (!gotSps) {
                ALOGE("SPS must come before PPS");
                return ERROR_MALFORMED;
            }
            if (!gotPps) {
                gotPps = true;
            }
            nextStartCode = parseParamSet(&ctx, tmp + 4, bytesLeft - 4, type,
                &paramSetLen);
        } else {
            ALOGE("Only SPS and PPS Nal units are expected");
            return ERROR_MALFORMED;
        }

        if (nextStartCode == NULL) {
            return ERROR_MALFORMED;
        }

        // Move on to find the next parameter set
        bytesLeft -= nextStartCode - tmp;
        tmp = nextStartCode;
        outputSize += (2 + paramSetLen);
    }

    {
        // Check on the number of seq parameter sets
        size_t nSeqParamSets = ctx.mSeqParamSets.size();
        if (nSeqParamSets == 0) {
            ALOGE("Cound not find sequence parameter set");
            return ERROR_MALFORMED;
        }

        if (nSeqParamSets > 0x1F) {
            ALOGE("Too many seq parameter sets (%d) found", nSeqParamSets);
            return ERROR_MALFORMED;
        }
    }

    {
        // Check on the number of pic parameter sets
        size_t nPicParamSets = ctx.mPicParamSets.size();
        if (nPicParamSets == 0) {
            ALOGE("Cound not find picture parameter set");
            return ERROR_MALFORMED;
        }
        if (nPicParamSets > 0xFF) {
            ALOGE("Too many pic parameter sets (%d) found", nPicParamSets);
            return ERROR_MALFORMED;
        }
    }

    // ISO 14496-15: AVC file format
    outputSize += 7;  // 7 more bytes in the header
    outputData = (uint8_t *)malloc(outputSize);
    uint8_t *header = outputData;
    header[0] = 1;                     // version
    header[1] = ctx.mProfileIdc;           // profile indication
    header[2] = ctx.mProfileCompatible;    // profile compatibility
    header[3] = ctx.mLevelIdc;

    // 6-bit '111111' followed by 2-bit to lengthSizeMinuusOne
    int32_t use2ByteNalLength = 0;
    if (param &&
        param->findInt32(kKey2ByteNalLength, &use2ByteNalLength) &&
        use2ByteNalLength) {
        header[4] = 0xfc | 1;  // length size == 2 bytes
    } else {
        header[4] = 0xfc | 3;  // length size == 4 bytes
    }

    // 3-bit '111' followed by 5-bit numSequenceParameterSets
    int nSequenceParamSets = ctx.mSeqParamSets.size();
    header[5] = 0xe0 | nSequenceParamSets;
    header += 6;
    for (List<AVCParamSet>::iterator it = ctx.mSeqParamSets.begin();
         it != ctx.mSeqParamSets.end(); ++it) {
        // 16-bit sequence parameter set length
        uint16_t seqParamSetLength = it->mLength;
        header[0] = seqParamSetLength >> 8;
        header[1] = seqParamSetLength & 0xff;
        //ALOGE("### SPS %d %d %d", seqParamSetLength, header[0], header[1]);

        // SPS NAL unit (sequence parameter length bytes)
        memcpy(&header[2], it->mData, seqParamSetLength);
        header += (2 + seqParamSetLength);
    }

    // 8-bit nPictureParameterSets
    int nPictureParamSets = ctx.mPicParamSets.size();
    header[0] = nPictureParamSets;
    header += 1;
    for (List<AVCParamSet>::iterator it = ctx.mPicParamSets.begin();
         it != ctx.mPicParamSets.end(); ++it) {
        // 16-bit picture parameter set length
        uint16_t picParamSetLength = it->mLength;
        header[0] = picParamSetLength >> 8;
        header[1] = picParamSetLength & 0xff;
//ALOGE("### PPS %d %d %d", picParamSetLength, header[0], header[1]);

        // PPS Nal unit (picture parameter set length bytes)
        memcpy(&header[2], it->mData, picParamSetLength);
        header += (2 + picParamSetLength);
    }

    *pOutputSize = outputSize;
    *pOutputData = outputData;
    return OK;
}
}// namespace android
