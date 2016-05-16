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

//#define LOG_NDEBUG 0
#define LOG_TAG "SoftAVC"
#include <utils/Log.h>

#include "SoftAVC.h"

#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/MediaDefs.h>
#include <media/stagefright/MediaErrors.h>
#include <media/IOMX.h>


namespace android {

static const CodecProfileLevel kProfileLevels[] = {
    { OMX_VIDEO_AVCProfileBaseline, OMX_VIDEO_AVCLevel1  },
    { OMX_VIDEO_AVCProfileBaseline, OMX_VIDEO_AVCLevel1b },
    { OMX_VIDEO_AVCProfileBaseline, OMX_VIDEO_AVCLevel11 },
    { OMX_VIDEO_AVCProfileBaseline, OMX_VIDEO_AVCLevel12 },
    { OMX_VIDEO_AVCProfileBaseline, OMX_VIDEO_AVCLevel13 },
    { OMX_VIDEO_AVCProfileBaseline, OMX_VIDEO_AVCLevel2  },
    { OMX_VIDEO_AVCProfileBaseline, OMX_VIDEO_AVCLevel21 },
    { OMX_VIDEO_AVCProfileBaseline, OMX_VIDEO_AVCLevel22 },
    { OMX_VIDEO_AVCProfileBaseline, OMX_VIDEO_AVCLevel3  },
    { OMX_VIDEO_AVCProfileBaseline, OMX_VIDEO_AVCLevel31 },
    { OMX_VIDEO_AVCProfileBaseline, OMX_VIDEO_AVCLevel32 },
    { OMX_VIDEO_AVCProfileBaseline, OMX_VIDEO_AVCLevel4  },
    { OMX_VIDEO_AVCProfileBaseline, OMX_VIDEO_AVCLevel41 },
    { OMX_VIDEO_AVCProfileBaseline, OMX_VIDEO_AVCLevel42 },
    { OMX_VIDEO_AVCProfileBaseline, OMX_VIDEO_AVCLevel5  },
    { OMX_VIDEO_AVCProfileBaseline, OMX_VIDEO_AVCLevel51 },
};

SoftAVC::SoftAVC(
        const char *name,
        const OMX_CALLBACKTYPE *callbacks,
        OMX_PTR appData,
        OMX_COMPONENTTYPE **component)
    : SoftVideoDecoderOMXComponent(
            name, "video_decoder.avc", OMX_VIDEO_CodingAVC,
            kProfileLevels, ARRAY_SIZE(kProfileLevels),
            320 /* width */, 240 /* height */, callbacks, appData, component),
      mHandle(NULL),
      mInputBufferCount(0),
      mPictureSize(mWidth * mHeight * 3 / 2),
      mFirstPicture(NULL),
      mFirstPictureId(-1),
      mPicId(0),
      mHeadersDecoded(false),
      mEOSStatus(INPUT_DATA_AVAILABLE),
      mSignalledError(false) {
    initPorts(
            kNumInputBuffers, 8192 /* inputBufferSize */,
            kNumOutputBuffers, MEDIA_MIMETYPE_VIDEO_AVC);

    CHECK_EQ(initDecoder(), (status_t)OK);
}

SoftAVC::~SoftAVC() {
    H264SwDecRelease(mHandle);
    mHandle = NULL;

    while (mPicToHeaderMap.size() != 0) {
        OMX_BUFFERHEADERTYPE *header = mPicToHeaderMap.editValueAt(0);
        mPicToHeaderMap.removeItemsAt(0);
        delete header;
        header = NULL;
    }
    List<BufferInfo *> &outQueue = getPortQueue(kOutputPortIndex);
    List<BufferInfo *> &inQueue = getPortQueue(kInputPortIndex);
    CHECK(outQueue.empty());
    CHECK(inQueue.empty());

    delete[] mFirstPicture;
}

status_t SoftAVC::initDecoder() {
    // Force decoder to output buffers in display order.
    if (H264SwDecInit(&mHandle, 0) == H264SWDEC_OK) {
        return OK;
    }
    return UNKNOWN_ERROR;
}

void SoftAVC::onQueueFilled(OMX_U32 portIndex) {
    if (mSignalledError || mOutputPortSettingsChange != NONE) {
        return;
    }

    if (mEOSStatus == OUTPUT_FRAMES_FLUSHED) {
        return;
    }

    List<BufferInfo *> &inQueue = getPortQueue(kInputPortIndex);
    List<BufferInfo *> &outQueue = getPortQueue(kOutputPortIndex);

    if (mHeadersDecoded) {
        // Dequeue any already decoded output frames to free up space
        // in the output queue.

        drainAllOutputBuffers(false /* eos */);
    }

    H264SwDecRet ret = H264SWDEC_PIC_RDY;
    bool portSettingsChanged = false;
    while ((mEOSStatus != INPUT_DATA_AVAILABLE || !inQueue.empty())
            && outQueue.size() == kNumOutputBuffers) {

        if (mEOSStatus == INPUT_EOS_SEEN) {
            drainAllOutputBuffers(true /* eos */);
            return;
        }

        BufferInfo *inInfo = *inQueue.begin();
        OMX_BUFFERHEADERTYPE *inHeader = inInfo->mHeader;
        ++mPicId;

        OMX_BUFFERHEADERTYPE *header = new OMX_BUFFERHEADERTYPE;
        memset(header, 0, sizeof(OMX_BUFFERHEADERTYPE));
        header->nTimeStamp = inHeader->nTimeStamp;
        header->nFlags = inHeader->nFlags;
        if (header->nFlags & OMX_BUFFERFLAG_EOS) {
            mEOSStatus = INPUT_EOS_SEEN;
        }
        mPicToHeaderMap.add(mPicId, header);
        inQueue.erase(inQueue.begin());

        H264SwDecInput inPicture;
        H264SwDecOutput outPicture;
        memset(&inPicture, 0, sizeof(inPicture));
        inPicture.dataLen = inHeader->nFilledLen;
        inPicture.pStream = inHeader->pBuffer + inHeader->nOffset;
        inPicture.picId = mPicId;
        inPicture.intraConcealmentMethod = 1;
        H264SwDecPicture decodedPicture;

        while (inPicture.dataLen > 0) {
            ret = H264SwDecDecode(mHandle, &inPicture, &outPicture);
            if (ret == H264SWDEC_HDRS_RDY_BUFF_NOT_EMPTY ||
                ret == H264SWDEC_PIC_RDY_BUFF_NOT_EMPTY) {
                inPicture.dataLen -= (u32)(outPicture.pStrmCurrPos - inPicture.pStream);
                inPicture.pStream = outPicture.pStrmCurrPos;
                if (ret == H264SWDEC_HDRS_RDY_BUFF_NOT_EMPTY) {
                    mHeadersDecoded = true;
                    H264SwDecInfo decoderInfo;
                    CHECK(H264SwDecGetInfo(mHandle, &decoderInfo) == H264SWDEC_OK);

                    if (handlePortSettingChangeEvent(&decoderInfo)) {
                        portSettingsChanged = true;
                    }

                    if (decoderInfo.croppingFlag &&
                        handleCropRectEvent(&decoderInfo.cropParams)) {
                        portSettingsChanged = true;
                    }
                }
            } else {
                if (portSettingsChanged) {
                    if (H264SwDecNextPicture(mHandle, &decodedPicture, 0)
                        == H264SWDEC_PIC_RDY) {

                        // Save this output buffer; otherwise, it will be
                        // lost during dynamic port reconfiguration because
                        // OpenMAX client will delete _all_ output buffers
                        // in the process.
                        saveFirstOutputBuffer(
                            decodedPicture.picId,
                            (uint8_t *)decodedPicture.pOutputPicture);
                    }
                }
                inPicture.dataLen = 0;
                if (ret < 0) {
                    ALOGE("Decoder failed: %d", ret);

                    notify(OMX_EventError, OMX_ErrorUndefined,
                           ERROR_MALFORMED, NULL);

                    mSignalledError = true;
                    return;
                }
            }
        }
        inInfo->mOwnedByUs = false;
        notifyEmptyBufferDone(inHeader);

        if (portSettingsChanged) {
            portSettingsChanged = false;
            return;
        }

        if (mFirstPicture && !outQueue.empty()) {
            drainOneOutputBuffer(mFirstPictureId, mFirstPicture);
            delete[] mFirstPicture;
            mFirstPicture = NULL;
            mFirstPictureId = -1;
        }

        drainAllOutputBuffers(false /* eos */);
    }
}

bool SoftAVC::handlePortSettingChangeEvent(const H264SwDecInfo *info) {
    if (mWidth != info->picWidth || mHeight != info->picHeight) {
        mWidth  = info->picWidth;
        mHeight = info->picHeight;
        mPictureSize = mWidth * mHeight * 3 / 2;
        updatePortDefinitions();
        notify(OMX_EventPortSettingsChanged, 1, 0, NULL);
        mOutputPortSettingsChange = AWAITING_DISABLED;
        return true;
    }

    return false;
}

bool SoftAVC::handleCropRectEvent(const CropParams *crop) {
    if (mCropLeft != crop->cropLeftOffset ||
        mCropTop != crop->cropTopOffset ||
        mCropWidth != crop->cropOutWidth ||
        mCropHeight != crop->cropOutHeight) {
        mCropLeft = crop->cropLeftOffset;
        mCropTop = crop->cropTopOffset;
        mCropWidth = crop->cropOutWidth;
        mCropHeight = crop->cropOutHeight;

        notify(OMX_EventPortSettingsChanged, 1,
                OMX_IndexConfigCommonOutputCrop, NULL);

        return true;
    }
    return false;
}

void SoftAVC::saveFirstOutputBuffer(int32_t picId, uint8_t *data) {
    CHECK(mFirstPicture == NULL);
    mFirstPictureId = picId;

    mFirstPicture = new uint8_t[mPictureSize];
    memcpy(mFirstPicture, data, mPictureSize);
}

void SoftAVC::drainOneOutputBuffer(int32_t picId, uint8_t* data) {
    List<BufferInfo *> &outQueue = getPortQueue(kOutputPortIndex);
    BufferInfo *outInfo = *outQueue.begin();
    outQueue.erase(outQueue.begin());
    OMX_BUFFERHEADERTYPE *outHeader = outInfo->mHeader;
    OMX_BUFFERHEADERTYPE *header = mPicToHeaderMap.valueFor(picId);
    outHeader->nTimeStamp = header->nTimeStamp;
    outHeader->nFlags = header->nFlags;
    outHeader->nFilledLen = mPictureSize;
    memcpy(outHeader->pBuffer + outHeader->nOffset,
            data, mPictureSize);
    mPicToHeaderMap.removeItem(picId);
    delete header;
    outInfo->mOwnedByUs = false;
    notifyFillBufferDone(outHeader);
}

void SoftAVC::drainAllOutputBuffers(bool eos) {
    List<BufferInfo *> &outQueue = getPortQueue(kOutputPortIndex);
    H264SwDecPicture decodedPicture;

    if (mHeadersDecoded) {
        while (!outQueue.empty()
                && H264SWDEC_PIC_RDY == H264SwDecNextPicture(
                    mHandle, &decodedPicture, eos /* flush */)) {
            int32_t picId = decodedPicture.picId;
            uint8_t *data = (uint8_t *) decodedPicture.pOutputPicture;
            drainOneOutputBuffer(picId, data);
        }
    }

    if (!eos) {
        return;
    }

    while (!outQueue.empty()) {
        BufferInfo *outInfo = *outQueue.begin();
        outQueue.erase(outQueue.begin());
        OMX_BUFFERHEADERTYPE *outHeader = outInfo->mHeader;

        outHeader->nTimeStamp = 0;
        outHeader->nFilledLen = 0;
        outHeader->nFlags = OMX_BUFFERFLAG_EOS;

        outInfo->mOwnedByUs = false;
        notifyFillBufferDone(outHeader);

        mEOSStatus = OUTPUT_FRAMES_FLUSHED;
    }
}

void SoftAVC::onPortFlushCompleted(OMX_U32 portIndex) {
    if (portIndex == kInputPortIndex) {
        mEOSStatus = INPUT_DATA_AVAILABLE;
    }
}

void SoftAVC::onReset() {
    SoftVideoDecoderOMXComponent::onReset();
    mSignalledError = false;
}

}  // namespace android

android::SoftOMXComponent *createSoftOMXComponent(
        const char *name, const OMX_CALLBACKTYPE *callbacks,
        OMX_PTR appData, OMX_COMPONENTTYPE **component) {
    return new android::SoftAVC(name, callbacks, appData, component);
}
