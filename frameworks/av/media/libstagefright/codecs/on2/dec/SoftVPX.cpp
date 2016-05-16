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
#define LOG_TAG "SoftVPX"
#include <utils/Log.h>

#include "SoftVPX.h"

#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/MediaDefs.h>

#include "vpx/vpx_decoder.h"
#include "vpx/vpx_codec.h"
#include "vpx/vp8dx.h"

namespace android {

SoftVPX::SoftVPX(
        const char *name,
        const char *componentRole,
        OMX_VIDEO_CODINGTYPE codingType,
        const OMX_CALLBACKTYPE *callbacks,
        OMX_PTR appData,
        OMX_COMPONENTTYPE **component)
    : SoftVideoDecoderOMXComponent(
            name, componentRole, codingType,
            NULL /* profileLevels */, 0 /* numProfileLevels */,
            320 /* width */, 240 /* height */, callbacks, appData, component),
      mMode(codingType == OMX_VIDEO_CodingVP8 ? MODE_VP8 : MODE_VP9),
      mCtx(NULL) {
    initPorts(kNumBuffers, 768 * 1024 /* inputBufferSize */,
            kNumBuffers,
            codingType == OMX_VIDEO_CodingVP8 ? MEDIA_MIMETYPE_VIDEO_VP8 : MEDIA_MIMETYPE_VIDEO_VP9);

    CHECK_EQ(initDecoder(), (status_t)OK);
}

SoftVPX::~SoftVPX() {
    vpx_codec_destroy((vpx_codec_ctx_t *)mCtx);
    delete (vpx_codec_ctx_t *)mCtx;
    mCtx = NULL;
}

static int GetCPUCoreCount() {
    int cpuCoreCount = 1;
#if defined(_SC_NPROCESSORS_ONLN)
    cpuCoreCount = sysconf(_SC_NPROCESSORS_ONLN);
#else
    // _SC_NPROC_ONLN must be defined...
    cpuCoreCount = sysconf(_SC_NPROC_ONLN);
#endif
    CHECK(cpuCoreCount >= 1);
    ALOGV("Number of CPU cores: %d", cpuCoreCount);
    return cpuCoreCount;
}

status_t SoftVPX::initDecoder() {
    mCtx = new vpx_codec_ctx_t;
    vpx_codec_err_t vpx_err;
    vpx_codec_dec_cfg_t cfg;
    memset(&cfg, 0, sizeof(vpx_codec_dec_cfg_t));
    cfg.threads = GetCPUCoreCount();
    if ((vpx_err = vpx_codec_dec_init(
                (vpx_codec_ctx_t *)mCtx,
                 mMode == MODE_VP8 ? &vpx_codec_vp8_dx_algo : &vpx_codec_vp9_dx_algo,
                 &cfg, 0))) {
        ALOGE("on2 decoder failed to initialize. (%d)", vpx_err);
        return UNKNOWN_ERROR;
    }

    return OK;
}

void SoftVPX::onQueueFilled(OMX_U32 portIndex) {
    if (mOutputPortSettingsChange != NONE) {
        return;
    }

    List<BufferInfo *> &inQueue = getPortQueue(0);
    List<BufferInfo *> &outQueue = getPortQueue(1);
    bool EOSseen = false;

    while (!inQueue.empty() && !outQueue.empty()) {
        BufferInfo *inInfo = *inQueue.begin();
        OMX_BUFFERHEADERTYPE *inHeader = inInfo->mHeader;

        BufferInfo *outInfo = *outQueue.begin();
        OMX_BUFFERHEADERTYPE *outHeader = outInfo->mHeader;

        if (inHeader->nFlags & OMX_BUFFERFLAG_EOS) {
            EOSseen = true;
            if (inHeader->nFilledLen == 0) {
                inQueue.erase(inQueue.begin());
                inInfo->mOwnedByUs = false;
                notifyEmptyBufferDone(inHeader);

                outHeader->nFilledLen = 0;
                outHeader->nFlags = OMX_BUFFERFLAG_EOS;

                outQueue.erase(outQueue.begin());
                outInfo->mOwnedByUs = false;
                notifyFillBufferDone(outHeader);
                return;
            }
        }

        if (vpx_codec_decode(
                    (vpx_codec_ctx_t *)mCtx,
                    inHeader->pBuffer + inHeader->nOffset,
                    inHeader->nFilledLen,
                    NULL,
                    0)) {
            ALOGE("on2 decoder failed to decode frame.");

            notify(OMX_EventError, OMX_ErrorUndefined, 0, NULL);
            return;
        }

        vpx_codec_iter_t iter = NULL;
        vpx_image_t *img = vpx_codec_get_frame((vpx_codec_ctx_t *)mCtx, &iter);

        if (img != NULL) {
            CHECK_EQ(img->fmt, IMG_FMT_I420);

            uint32_t width = img->d_w;
            uint32_t height = img->d_h;

            if (width != mWidth || height != mHeight) {
                mWidth = width;
                mHeight = height;

                updatePortDefinitions();

                notify(OMX_EventPortSettingsChanged, 1, 0, NULL);
                mOutputPortSettingsChange = AWAITING_DISABLED;
                return;
            }

            outHeader->nOffset = 0;
            outHeader->nFilledLen = (width * height * 3) / 2;
            outHeader->nFlags = EOSseen ? OMX_BUFFERFLAG_EOS : 0;
            outHeader->nTimeStamp = inHeader->nTimeStamp;

            const uint8_t *srcLine = (const uint8_t *)img->planes[PLANE_Y];
            uint8_t *dst = outHeader->pBuffer;
            for (size_t i = 0; i < img->d_h; ++i) {
                memcpy(dst, srcLine, img->d_w);

                srcLine += img->stride[PLANE_Y];
                dst += img->d_w;
            }

            srcLine = (const uint8_t *)img->planes[PLANE_U];
            for (size_t i = 0; i < img->d_h / 2; ++i) {
                memcpy(dst, srcLine, img->d_w / 2);

                srcLine += img->stride[PLANE_U];
                dst += img->d_w / 2;
            }

            srcLine = (const uint8_t *)img->planes[PLANE_V];
            for (size_t i = 0; i < img->d_h / 2; ++i) {
                memcpy(dst, srcLine, img->d_w / 2);

                srcLine += img->stride[PLANE_V];
                dst += img->d_w / 2;
            }

            outInfo->mOwnedByUs = false;
            outQueue.erase(outQueue.begin());
            outInfo = NULL;
            notifyFillBufferDone(outHeader);
            outHeader = NULL;
        }

        inInfo->mOwnedByUs = false;
        inQueue.erase(inQueue.begin());
        inInfo = NULL;
        notifyEmptyBufferDone(inHeader);
        inHeader = NULL;
    }
}

}  // namespace android

android::SoftOMXComponent *createSoftOMXComponent(
        const char *name, const OMX_CALLBACKTYPE *callbacks,
        OMX_PTR appData, OMX_COMPONENTTYPE **component) {
    if (!strcmp(name, "OMX.google.vp8.decoder")) {
        return new android::SoftVPX(
                name, "video_decoder.vp8", OMX_VIDEO_CodingVP8,
                callbacks, appData, component);
    } else if (!strcmp(name, "OMX.google.vp9.decoder")) {
        return new android::SoftVPX(
                name, "video_decoder.vp9", OMX_VIDEO_CodingVP9,
                callbacks, appData, component);
    } else {
        CHECK(!"Unknown component");
    }
}
