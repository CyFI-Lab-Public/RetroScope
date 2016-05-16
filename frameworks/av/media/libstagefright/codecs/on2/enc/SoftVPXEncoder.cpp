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

// #define LOG_NDEBUG 0
#define LOG_TAG "SoftVPXEncoder"
#include "SoftVPXEncoder.h"

#include <utils/Log.h>

#include <media/hardware/HardwareAPI.h>
#include <media/hardware/MetadataBufferType.h>
#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/MediaDefs.h>

namespace android {


template<class T>
static void InitOMXParams(T *params) {
    params->nSize = sizeof(T);
    // OMX IL 1.1.2
    params->nVersion.s.nVersionMajor = 1;
    params->nVersion.s.nVersionMinor = 1;
    params->nVersion.s.nRevision = 2;
    params->nVersion.s.nStep = 0;
}


static int GetCPUCoreCount() {
    int cpuCoreCount = 1;
#if defined(_SC_NPROCESSORS_ONLN)
    cpuCoreCount = sysconf(_SC_NPROCESSORS_ONLN);
#else
    // _SC_NPROC_ONLN must be defined...
    cpuCoreCount = sysconf(_SC_NPROC_ONLN);
#endif
    CHECK_GE(cpuCoreCount, 1);
    return cpuCoreCount;
}


// This color conversion utility is copied from SoftMPEG4Encoder.cpp
inline static void ConvertSemiPlanarToPlanar(uint8_t *inyuv,
                                             uint8_t* outyuv,
                                             int32_t width,
                                             int32_t height) {
    int32_t outYsize = width * height;
    uint32_t *outy =  (uint32_t *) outyuv;
    uint16_t *outcb = (uint16_t *) (outyuv + outYsize);
    uint16_t *outcr = (uint16_t *) (outyuv + outYsize + (outYsize >> 2));

    /* Y copying */
    memcpy(outy, inyuv, outYsize);

    /* U & V copying */
    uint32_t *inyuv_4 = (uint32_t *) (inyuv + outYsize);
    for (int32_t i = height >> 1; i > 0; --i) {
        for (int32_t j = width >> 2; j > 0; --j) {
            uint32_t temp = *inyuv_4++;
            uint32_t tempU = temp & 0xFF;
            tempU = tempU | ((temp >> 8) & 0xFF00);

            uint32_t tempV = (temp >> 8) & 0xFF;
            tempV = tempV | ((temp >> 16) & 0xFF00);

            // Flip U and V
            *outcb++ = tempV;
            *outcr++ = tempU;
        }
    }
}

static void ConvertRGB32ToPlanar(
        const uint8_t *src, uint8_t *dstY, int32_t width, int32_t height) {
    CHECK((width & 1) == 0);
    CHECK((height & 1) == 0);

    uint8_t *dstU = dstY + width * height;
    uint8_t *dstV = dstU + (width / 2) * (height / 2);

    for (int32_t y = 0; y < height; ++y) {
        for (int32_t x = 0; x < width; ++x) {
#ifdef SURFACE_IS_BGR32
            unsigned blue = src[4 * x];
            unsigned green = src[4 * x + 1];
            unsigned red= src[4 * x + 2];
#else
            unsigned red= src[4 * x];
            unsigned green = src[4 * x + 1];
            unsigned blue = src[4 * x + 2];
#endif

            unsigned luma =
                ((red * 66 + green * 129 + blue * 25) >> 8) + 16;

            dstY[x] = luma;

            if ((x & 1) == 0 && (y & 1) == 0) {
                unsigned U =
                    ((-red * 38 - green * 74 + blue * 112) >> 8) + 128;

                unsigned V =
                    ((red * 112 - green * 94 - blue * 18) >> 8) + 128;

                dstU[x / 2] = U;
                dstV[x / 2] = V;
            }
        }

        if ((y & 1) == 0) {
            dstU += width / 2;
            dstV += width / 2;
        }

        src += 4 * width;
        dstY += width;
    }
}

SoftVPXEncoder::SoftVPXEncoder(const char *name,
                               const OMX_CALLBACKTYPE *callbacks,
                               OMX_PTR appData,
                               OMX_COMPONENTTYPE **component)
    : SimpleSoftOMXComponent(name, callbacks, appData, component),
      mCodecContext(NULL),
      mCodecConfiguration(NULL),
      mCodecInterface(NULL),
      mWidth(176),
      mHeight(144),
      mBitrate(192000),  // in bps
      mBitrateUpdated(false),
      mBitrateControlMode(VPX_VBR),  // variable bitrate
      mFrameDurationUs(33333),  // Defaults to 30 fps
      mDCTPartitions(0),
      mErrorResilience(OMX_FALSE),
      mColorFormat(OMX_COLOR_FormatYUV420Planar),
      mLevel(OMX_VIDEO_VP8Level_Version0),
      mConversionBuffer(NULL),
      mInputDataIsMeta(false),
      mGrallocModule(NULL),
      mKeyFrameRequested(false) {
    initPorts();
}


SoftVPXEncoder::~SoftVPXEncoder() {
    releaseEncoder();
}


void SoftVPXEncoder::initPorts() {
    OMX_PARAM_PORTDEFINITIONTYPE inputPort;
    OMX_PARAM_PORTDEFINITIONTYPE outputPort;

    InitOMXParams(&inputPort);
    InitOMXParams(&outputPort);

    inputPort.nBufferCountMin = kNumBuffers;
    inputPort.nBufferCountActual = inputPort.nBufferCountMin;
    inputPort.bEnabled = OMX_TRUE;
    inputPort.bPopulated = OMX_FALSE;
    inputPort.eDomain = OMX_PortDomainVideo;
    inputPort.bBuffersContiguous = OMX_FALSE;
    inputPort.format.video.pNativeRender = NULL;
    inputPort.format.video.nFrameWidth = mWidth;
    inputPort.format.video.nFrameHeight = mHeight;
    inputPort.format.video.nStride = inputPort.format.video.nFrameWidth;
    inputPort.format.video.nSliceHeight = inputPort.format.video.nFrameHeight;
    inputPort.format.video.nBitrate = 0;
    // frameRate is reciprocal of frameDuration, which is
    // in microseconds. It is also in Q16 format.
    inputPort.format.video.xFramerate = (1000000/mFrameDurationUs) << 16;
    inputPort.format.video.bFlagErrorConcealment = OMX_FALSE;
    inputPort.nPortIndex = kInputPortIndex;
    inputPort.eDir = OMX_DirInput;
    inputPort.nBufferAlignment = kInputBufferAlignment;
    inputPort.format.video.cMIMEType =
        const_cast<char *>(MEDIA_MIMETYPE_VIDEO_RAW);
    inputPort.format.video.eCompressionFormat = OMX_VIDEO_CodingUnused;
    inputPort.format.video.eColorFormat = mColorFormat;
    inputPort.format.video.pNativeWindow = NULL;
    inputPort.nBufferSize =
        (inputPort.format.video.nStride *
        inputPort.format.video.nSliceHeight * 3) / 2;

    addPort(inputPort);

    outputPort.nBufferCountMin = kNumBuffers;
    outputPort.nBufferCountActual = outputPort.nBufferCountMin;
    outputPort.bEnabled = OMX_TRUE;
    outputPort.bPopulated = OMX_FALSE;
    outputPort.eDomain = OMX_PortDomainVideo;
    outputPort.bBuffersContiguous = OMX_FALSE;
    outputPort.format.video.pNativeRender = NULL;
    outputPort.format.video.nFrameWidth = mWidth;
    outputPort.format.video.nFrameHeight = mHeight;
    outputPort.format.video.nStride = outputPort.format.video.nFrameWidth;
    outputPort.format.video.nSliceHeight = outputPort.format.video.nFrameHeight;
    outputPort.format.video.nBitrate = mBitrate;
    outputPort.format.video.xFramerate = 0;
    outputPort.format.video.bFlagErrorConcealment = OMX_FALSE;
    outputPort.nPortIndex = kOutputPortIndex;
    outputPort.eDir = OMX_DirOutput;
    outputPort.nBufferAlignment = kOutputBufferAlignment;
    outputPort.format.video.cMIMEType =
        const_cast<char *>(MEDIA_MIMETYPE_VIDEO_VP8);
    outputPort.format.video.eCompressionFormat = OMX_VIDEO_CodingVP8;
    outputPort.format.video.eColorFormat = OMX_COLOR_FormatUnused;
    outputPort.format.video.pNativeWindow = NULL;
    outputPort.nBufferSize = 256 * 1024;  // arbitrary

    addPort(outputPort);
}


status_t SoftVPXEncoder::initEncoder() {
    vpx_codec_err_t codec_return;

    mCodecContext = new vpx_codec_ctx_t;
    mCodecConfiguration = new vpx_codec_enc_cfg_t;
    mCodecInterface = vpx_codec_vp8_cx();

    if (mCodecInterface == NULL) {
        return UNKNOWN_ERROR;
    }

    codec_return = vpx_codec_enc_config_default(mCodecInterface,
                                                mCodecConfiguration,
                                                0);  // Codec specific flags

    if (codec_return != VPX_CODEC_OK) {
        ALOGE("Error populating default configuration for vpx encoder.");
        return UNKNOWN_ERROR;
    }

    mCodecConfiguration->g_w = mWidth;
    mCodecConfiguration->g_h = mHeight;
    mCodecConfiguration->g_threads = GetCPUCoreCount();
    mCodecConfiguration->g_error_resilient = mErrorResilience;

    switch (mLevel) {
        case OMX_VIDEO_VP8Level_Version0:
            mCodecConfiguration->g_profile = 0;
            break;

        case OMX_VIDEO_VP8Level_Version1:
            mCodecConfiguration->g_profile = 1;
            break;

        case OMX_VIDEO_VP8Level_Version2:
            mCodecConfiguration->g_profile = 2;
            break;

        case OMX_VIDEO_VP8Level_Version3:
            mCodecConfiguration->g_profile = 3;
            break;

        default:
            mCodecConfiguration->g_profile = 0;
    }

    // OMX timebase unit is microsecond
    // g_timebase is in seconds (i.e. 1/1000000 seconds)
    mCodecConfiguration->g_timebase.num = 1;
    mCodecConfiguration->g_timebase.den = 1000000;
    // rc_target_bitrate is in kbps, mBitrate in bps
    mCodecConfiguration->rc_target_bitrate = mBitrate/1000;
    mCodecConfiguration->rc_end_usage = mBitrateControlMode;

    codec_return = vpx_codec_enc_init(mCodecContext,
                                      mCodecInterface,
                                      mCodecConfiguration,
                                      0);  // flags

    if (codec_return != VPX_CODEC_OK) {
        ALOGE("Error initializing vpx encoder");
        return UNKNOWN_ERROR;
    }

    codec_return = vpx_codec_control(mCodecContext,
                                     VP8E_SET_TOKEN_PARTITIONS,
                                     mDCTPartitions);
    if (codec_return != VPX_CODEC_OK) {
        ALOGE("Error setting dct partitions for vpx encoder.");
        return UNKNOWN_ERROR;
    }

    if (mColorFormat == OMX_COLOR_FormatYUV420SemiPlanar || mInputDataIsMeta) {
        if (mConversionBuffer == NULL) {
            mConversionBuffer = (uint8_t *)malloc(mWidth * mHeight * 3 / 2);
            if (mConversionBuffer == NULL) {
                ALOGE("Allocating conversion buffer failed.");
                return UNKNOWN_ERROR;
            }
        }
    }
    return OK;
}


status_t SoftVPXEncoder::releaseEncoder() {
    if (mCodecContext != NULL) {
        vpx_codec_destroy(mCodecContext);
        delete mCodecContext;
        mCodecContext = NULL;
    }

    if (mCodecConfiguration != NULL) {
        delete mCodecConfiguration;
        mCodecConfiguration = NULL;
    }

    if (mConversionBuffer != NULL) {
        delete mConversionBuffer;
        mConversionBuffer = NULL;
    }

    // this one is not allocated by us
    mCodecInterface = NULL;

    return OK;
}


OMX_ERRORTYPE SoftVPXEncoder::internalGetParameter(OMX_INDEXTYPE index,
                                                   OMX_PTR param) {
    // can include extension index OMX_INDEXEXTTYPE
    const int32_t indexFull = index;

    switch (indexFull) {
        case OMX_IndexParamVideoPortFormat: {
            OMX_VIDEO_PARAM_PORTFORMATTYPE *formatParams =
                (OMX_VIDEO_PARAM_PORTFORMATTYPE *)param;

            if (formatParams->nPortIndex == kInputPortIndex) {
                if (formatParams->nIndex >= kNumberOfSupportedColorFormats) {
                    return OMX_ErrorNoMore;
                }

                // Color formats, in order of preference
                if (formatParams->nIndex == 0) {
                    formatParams->eColorFormat = OMX_COLOR_FormatYUV420Planar;
                } else if (formatParams->nIndex == 1) {
                    formatParams->eColorFormat =
                        OMX_COLOR_FormatYUV420SemiPlanar;
                } else {
                    formatParams->eColorFormat = OMX_COLOR_FormatAndroidOpaque;
                }

                formatParams->eCompressionFormat = OMX_VIDEO_CodingUnused;
                // Converting from microseconds
                // Also converting to Q16 format
                formatParams->xFramerate = (1000000/mFrameDurationUs) << 16;
                return OMX_ErrorNone;
            } else if (formatParams->nPortIndex == kOutputPortIndex) {
                formatParams->eCompressionFormat = OMX_VIDEO_CodingVP8;
                formatParams->eColorFormat = OMX_COLOR_FormatUnused;
                formatParams->xFramerate = 0;
                return OMX_ErrorNone;
            } else {
                return OMX_ErrorBadPortIndex;
            }
        }

        case OMX_IndexParamVideoBitrate: {
            OMX_VIDEO_PARAM_BITRATETYPE *bitrate =
                (OMX_VIDEO_PARAM_BITRATETYPE *)param;

                if (bitrate->nPortIndex != kOutputPortIndex) {
                    return OMX_ErrorUnsupportedIndex;
                }

                bitrate->nTargetBitrate = mBitrate;

                if (mBitrateControlMode == VPX_VBR) {
                    bitrate->eControlRate = OMX_Video_ControlRateVariable;
                } else if (mBitrateControlMode == VPX_CBR) {
                    bitrate->eControlRate = OMX_Video_ControlRateConstant;
                } else {
                    return OMX_ErrorUnsupportedSetting;
                }
                return OMX_ErrorNone;
        }

        // VP8 specific parameters that use extension headers
        case OMX_IndexParamVideoVp8: {
            OMX_VIDEO_PARAM_VP8TYPE *vp8Params =
                (OMX_VIDEO_PARAM_VP8TYPE *)param;

                if (vp8Params->nPortIndex != kOutputPortIndex) {
                    return OMX_ErrorUnsupportedIndex;
                }

                vp8Params->eProfile = OMX_VIDEO_VP8ProfileMain;
                vp8Params->eLevel = mLevel;
                vp8Params->nDCTPartitions = mDCTPartitions;
                vp8Params->bErrorResilientMode = mErrorResilience;
                return OMX_ErrorNone;
        }

        case OMX_IndexParamVideoProfileLevelQuerySupported: {
            OMX_VIDEO_PARAM_PROFILELEVELTYPE *profileAndLevel =
                (OMX_VIDEO_PARAM_PROFILELEVELTYPE *)param;

            if (profileAndLevel->nPortIndex != kOutputPortIndex) {
                return OMX_ErrorUnsupportedIndex;
            }

            switch (profileAndLevel->nProfileIndex) {
                case 0:
                    profileAndLevel->eLevel = OMX_VIDEO_VP8Level_Version0;
                    break;

                case 1:
                    profileAndLevel->eLevel = OMX_VIDEO_VP8Level_Version1;
                    break;

                case 2:
                    profileAndLevel->eLevel = OMX_VIDEO_VP8Level_Version2;
                    break;

                case 3:
                    profileAndLevel->eLevel = OMX_VIDEO_VP8Level_Version3;
                    break;

                default:
                    return OMX_ErrorNoMore;
            }

            profileAndLevel->eProfile = OMX_VIDEO_VP8ProfileMain;
            return OMX_ErrorNone;
        }

        case OMX_IndexParamVideoProfileLevelCurrent: {
            OMX_VIDEO_PARAM_PROFILELEVELTYPE *profileAndLevel =
                (OMX_VIDEO_PARAM_PROFILELEVELTYPE *)param;

            if (profileAndLevel->nPortIndex != kOutputPortIndex) {
                return OMX_ErrorUnsupportedIndex;
            }

            profileAndLevel->eLevel = mLevel;
            profileAndLevel->eProfile = OMX_VIDEO_VP8ProfileMain;
            return OMX_ErrorNone;
        }

        default:
            return SimpleSoftOMXComponent::internalGetParameter(index, param);
    }
}


OMX_ERRORTYPE SoftVPXEncoder::internalSetParameter(OMX_INDEXTYPE index,
                                                   const OMX_PTR param) {
    // can include extension index OMX_INDEXEXTTYPE
    const int32_t indexFull = index;

    switch (indexFull) {
        case OMX_IndexParamStandardComponentRole:
            return internalSetRoleParams(
                (const OMX_PARAM_COMPONENTROLETYPE *)param);

        case OMX_IndexParamVideoBitrate:
            return internalSetBitrateParams(
                (const OMX_VIDEO_PARAM_BITRATETYPE *)param);

        case OMX_IndexParamPortDefinition:
        {
            OMX_ERRORTYPE err = internalSetPortParams(
                (const OMX_PARAM_PORTDEFINITIONTYPE *)param);

            if (err != OMX_ErrorNone) {
                return err;
            }

            return SimpleSoftOMXComponent::internalSetParameter(index, param);
        }

        case OMX_IndexParamVideoPortFormat:
            return internalSetFormatParams(
                (const OMX_VIDEO_PARAM_PORTFORMATTYPE *)param);

        case OMX_IndexParamVideoVp8:
            return internalSetVp8Params(
                (const OMX_VIDEO_PARAM_VP8TYPE *)param);

        case OMX_IndexParamVideoProfileLevelCurrent:
            return internalSetProfileLevel(
                (const OMX_VIDEO_PARAM_PROFILELEVELTYPE *)param);

        case OMX_IndexVendorStartUnused:
        {
            // storeMetaDataInBuffers
            const StoreMetaDataInBuffersParams *storeParam =
                (const StoreMetaDataInBuffersParams *)param;

            if (storeParam->nPortIndex != kInputPortIndex) {
                return OMX_ErrorBadPortIndex;
            }

            mInputDataIsMeta = (storeParam->bStoreMetaData == OMX_TRUE);

            return OMX_ErrorNone;
        }

        default:
            return SimpleSoftOMXComponent::internalSetParameter(index, param);
    }
}

OMX_ERRORTYPE SoftVPXEncoder::setConfig(
        OMX_INDEXTYPE index, const OMX_PTR _params) {
    switch (index) {
        case OMX_IndexConfigVideoIntraVOPRefresh:
        {
            OMX_CONFIG_INTRAREFRESHVOPTYPE *params =
                (OMX_CONFIG_INTRAREFRESHVOPTYPE *)_params;

            if (params->nPortIndex != kOutputPortIndex) {
                return OMX_ErrorBadPortIndex;
            }

            mKeyFrameRequested = params->IntraRefreshVOP;
            return OMX_ErrorNone;
        }

        case OMX_IndexConfigVideoBitrate:
        {
            OMX_VIDEO_CONFIG_BITRATETYPE *params =
                (OMX_VIDEO_CONFIG_BITRATETYPE *)_params;

            if (params->nPortIndex != kOutputPortIndex) {
                return OMX_ErrorBadPortIndex;
            }

            if (mBitrate != params->nEncodeBitrate) {
                mBitrate = params->nEncodeBitrate;
                mBitrateUpdated = true;
            }
            return OMX_ErrorNone;
        }

        default:
            return SimpleSoftOMXComponent::setConfig(index, _params);
    }
}

OMX_ERRORTYPE SoftVPXEncoder::internalSetProfileLevel(
        const OMX_VIDEO_PARAM_PROFILELEVELTYPE* profileAndLevel) {
    if (profileAndLevel->nPortIndex != kOutputPortIndex) {
        return OMX_ErrorUnsupportedIndex;
    }

    if (profileAndLevel->eProfile != OMX_VIDEO_VP8ProfileMain) {
        return OMX_ErrorBadParameter;
    }

    if (profileAndLevel->eLevel == OMX_VIDEO_VP8Level_Version0 ||
        profileAndLevel->eLevel == OMX_VIDEO_VP8Level_Version1 ||
        profileAndLevel->eLevel == OMX_VIDEO_VP8Level_Version2 ||
        profileAndLevel->eLevel == OMX_VIDEO_VP8Level_Version3) {
        mLevel = (OMX_VIDEO_VP8LEVELTYPE)profileAndLevel->eLevel;
    } else {
        return OMX_ErrorBadParameter;
    }

    return OMX_ErrorNone;
}


OMX_ERRORTYPE SoftVPXEncoder::internalSetVp8Params(
        const OMX_VIDEO_PARAM_VP8TYPE* vp8Params) {
    if (vp8Params->nPortIndex != kOutputPortIndex) {
        return OMX_ErrorUnsupportedIndex;
    }

    if (vp8Params->eProfile != OMX_VIDEO_VP8ProfileMain) {
        return OMX_ErrorBadParameter;
    }

    if (vp8Params->eLevel == OMX_VIDEO_VP8Level_Version0 ||
        vp8Params->eLevel == OMX_VIDEO_VP8Level_Version1 ||
        vp8Params->eLevel == OMX_VIDEO_VP8Level_Version2 ||
        vp8Params->eLevel == OMX_VIDEO_VP8Level_Version3) {
        mLevel = vp8Params->eLevel;
    } else {
        return OMX_ErrorBadParameter;
    }

    if (vp8Params->nDCTPartitions <= kMaxDCTPartitions) {
        mDCTPartitions = vp8Params->nDCTPartitions;
    } else {
        return OMX_ErrorBadParameter;
    }

    mErrorResilience = vp8Params->bErrorResilientMode;
    return OMX_ErrorNone;
}


OMX_ERRORTYPE SoftVPXEncoder::internalSetFormatParams(
        const OMX_VIDEO_PARAM_PORTFORMATTYPE* format) {
    if (format->nPortIndex == kInputPortIndex) {
        if (format->eColorFormat == OMX_COLOR_FormatYUV420Planar ||
            format->eColorFormat == OMX_COLOR_FormatYUV420SemiPlanar ||
            format->eColorFormat == OMX_COLOR_FormatAndroidOpaque) {
            mColorFormat = format->eColorFormat;

            OMX_PARAM_PORTDEFINITIONTYPE *def = &editPortInfo(kInputPortIndex)->mDef;
            def->format.video.eColorFormat = mColorFormat;

            return OMX_ErrorNone;
        } else {
            ALOGE("Unsupported color format %i", format->eColorFormat);
            return OMX_ErrorUnsupportedSetting;
        }
    } else if (format->nPortIndex == kOutputPortIndex) {
        if (format->eCompressionFormat == OMX_VIDEO_CodingVP8) {
            return OMX_ErrorNone;
        } else {
            return OMX_ErrorUnsupportedSetting;
        }
    } else {
        return OMX_ErrorBadPortIndex;
    }
}


OMX_ERRORTYPE SoftVPXEncoder::internalSetRoleParams(
        const OMX_PARAM_COMPONENTROLETYPE* role) {
    const char* roleText = (const char*)role->cRole;
    const size_t roleTextMaxSize = OMX_MAX_STRINGNAME_SIZE - 1;

    if (strncmp(roleText, "video_encoder.vp8", roleTextMaxSize)) {
        ALOGE("Unsupported component role");
        return OMX_ErrorBadParameter;
    }

    return OMX_ErrorNone;
}


OMX_ERRORTYPE SoftVPXEncoder::internalSetPortParams(
        const OMX_PARAM_PORTDEFINITIONTYPE* port) {
    if (port->nPortIndex == kInputPortIndex) {
        mWidth = port->format.video.nFrameWidth;
        mHeight = port->format.video.nFrameHeight;

        // xFramerate comes in Q16 format, in frames per second unit
        const uint32_t framerate = port->format.video.xFramerate >> 16;
        // frame duration is in microseconds
        mFrameDurationUs = (1000000/framerate);

        if (port->format.video.eColorFormat == OMX_COLOR_FormatYUV420Planar ||
            port->format.video.eColorFormat == OMX_COLOR_FormatYUV420SemiPlanar ||
            port->format.video.eColorFormat == OMX_COLOR_FormatAndroidOpaque) {
            mColorFormat = port->format.video.eColorFormat;
        } else {
            return OMX_ErrorUnsupportedSetting;
        }

        OMX_PARAM_PORTDEFINITIONTYPE *def = &editPortInfo(kInputPortIndex)->mDef;
        def->format.video.nFrameWidth = mWidth;
        def->format.video.nFrameHeight = mHeight;
        def->format.video.xFramerate = port->format.video.xFramerate;
        def->format.video.eColorFormat = mColorFormat;

        return OMX_ErrorNone;
    } else if (port->nPortIndex == kOutputPortIndex) {
        mBitrate = port->format.video.nBitrate;
        return OMX_ErrorNone;
    } else {
        return OMX_ErrorBadPortIndex;
    }
}


OMX_ERRORTYPE SoftVPXEncoder::internalSetBitrateParams(
        const OMX_VIDEO_PARAM_BITRATETYPE* bitrate) {
    if (bitrate->nPortIndex != kOutputPortIndex) {
        return OMX_ErrorUnsupportedIndex;
    }

    mBitrate = bitrate->nTargetBitrate;

    if (bitrate->eControlRate == OMX_Video_ControlRateVariable) {
        mBitrateControlMode = VPX_VBR;
    } else if (bitrate->eControlRate == OMX_Video_ControlRateConstant) {
        mBitrateControlMode = VPX_CBR;
    } else {
        return OMX_ErrorUnsupportedSetting;
    }

    return OMX_ErrorNone;
}


void SoftVPXEncoder::onQueueFilled(OMX_U32 portIndex) {
    // Initialize encoder if not already
    if (mCodecContext == NULL) {
        if (OK != initEncoder()) {
            ALOGE("Failed to initialize encoder");
            notify(OMX_EventError,
                   OMX_ErrorUndefined,
                   0,  // Extra notification data
                   NULL);  // Notification data pointer
            return;
        }
    }

    vpx_codec_err_t codec_return;
    List<BufferInfo *> &inputBufferInfoQueue = getPortQueue(kInputPortIndex);
    List<BufferInfo *> &outputBufferInfoQueue = getPortQueue(kOutputPortIndex);

    while (!inputBufferInfoQueue.empty() && !outputBufferInfoQueue.empty()) {
        BufferInfo *inputBufferInfo = *inputBufferInfoQueue.begin();
        OMX_BUFFERHEADERTYPE *inputBufferHeader = inputBufferInfo->mHeader;

        BufferInfo *outputBufferInfo = *outputBufferInfoQueue.begin();
        OMX_BUFFERHEADERTYPE *outputBufferHeader = outputBufferInfo->mHeader;

        if (inputBufferHeader->nFlags & OMX_BUFFERFLAG_EOS) {
            inputBufferInfoQueue.erase(inputBufferInfoQueue.begin());
            inputBufferInfo->mOwnedByUs = false;
            notifyEmptyBufferDone(inputBufferHeader);

            outputBufferHeader->nFilledLen = 0;
            outputBufferHeader->nFlags = OMX_BUFFERFLAG_EOS;

            outputBufferInfoQueue.erase(outputBufferInfoQueue.begin());
            outputBufferInfo->mOwnedByUs = false;
            notifyFillBufferDone(outputBufferHeader);
            return;
        }

        uint8_t *source =
            inputBufferHeader->pBuffer + inputBufferHeader->nOffset;

        if (mInputDataIsMeta) {
            CHECK_GE(inputBufferHeader->nFilledLen,
                     4 + sizeof(buffer_handle_t));

            uint32_t bufferType = *(uint32_t *)source;
            CHECK_EQ(bufferType, kMetadataBufferTypeGrallocSource);

            if (mGrallocModule == NULL) {
                CHECK_EQ(0, hw_get_module(
                            GRALLOC_HARDWARE_MODULE_ID, &mGrallocModule));
            }

            const gralloc_module_t *grmodule =
                (const gralloc_module_t *)mGrallocModule;

            buffer_handle_t handle = *(buffer_handle_t *)(source + 4);

            void *bits;
            CHECK_EQ(0,
                     grmodule->lock(
                         grmodule, handle,
                         GRALLOC_USAGE_SW_READ_OFTEN
                            | GRALLOC_USAGE_SW_WRITE_NEVER,
                         0, 0, mWidth, mHeight, &bits));

            ConvertRGB32ToPlanar(
                    (const uint8_t *)bits, mConversionBuffer, mWidth, mHeight);

            source = mConversionBuffer;

            CHECK_EQ(0, grmodule->unlock(grmodule, handle));
        } else if (mColorFormat == OMX_COLOR_FormatYUV420SemiPlanar) {
            ConvertSemiPlanarToPlanar(
                    source, mConversionBuffer, mWidth, mHeight);

            source = mConversionBuffer;
        }
        vpx_image_t raw_frame;
        vpx_img_wrap(&raw_frame, VPX_IMG_FMT_I420, mWidth, mHeight,
                     kInputBufferAlignment, source);

        vpx_enc_frame_flags_t flags = 0;
        if (mKeyFrameRequested) {
            flags |= VPX_EFLAG_FORCE_KF;
            mKeyFrameRequested = false;
        }

        if (mBitrateUpdated) {
            mCodecConfiguration->rc_target_bitrate = mBitrate/1000;
            vpx_codec_err_t res = vpx_codec_enc_config_set(mCodecContext,
                                                           mCodecConfiguration);
            if (res != VPX_CODEC_OK) {
                ALOGE("vp8 encoder failed to update bitrate: %s",
                      vpx_codec_err_to_string(res));
                notify(OMX_EventError,
                       OMX_ErrorUndefined,
                       0, // Extra notification data
                       NULL); // Notification data pointer
            }
            mBitrateUpdated = false;
        }

        codec_return = vpx_codec_encode(
                mCodecContext,
                &raw_frame,
                inputBufferHeader->nTimeStamp,  // in timebase units
                mFrameDurationUs,  // frame duration in timebase units
                flags,  // frame flags
                VPX_DL_REALTIME);  // encoding deadline
        if (codec_return != VPX_CODEC_OK) {
            ALOGE("vpx encoder failed to encode frame");
            notify(OMX_EventError,
                   OMX_ErrorUndefined,
                   0,  // Extra notification data
                   NULL);  // Notification data pointer
            return;
        }

        vpx_codec_iter_t encoded_packet_iterator = NULL;
        const vpx_codec_cx_pkt_t* encoded_packet;

        while ((encoded_packet = vpx_codec_get_cx_data(
                        mCodecContext, &encoded_packet_iterator))) {
            if (encoded_packet->kind == VPX_CODEC_CX_FRAME_PKT) {
                outputBufferHeader->nTimeStamp = encoded_packet->data.frame.pts;
                outputBufferHeader->nFlags = 0;
                if (encoded_packet->data.frame.flags & VPX_FRAME_IS_KEY)
                  outputBufferHeader->nFlags |= OMX_BUFFERFLAG_SYNCFRAME;
                outputBufferHeader->nOffset = 0;
                outputBufferHeader->nFilledLen = encoded_packet->data.frame.sz;
                memcpy(outputBufferHeader->pBuffer,
                       encoded_packet->data.frame.buf,
                       encoded_packet->data.frame.sz);
                outputBufferInfo->mOwnedByUs = false;
                outputBufferInfoQueue.erase(outputBufferInfoQueue.begin());
                notifyFillBufferDone(outputBufferHeader);
            }
        }

        inputBufferInfo->mOwnedByUs = false;
        inputBufferInfoQueue.erase(inputBufferInfoQueue.begin());
        notifyEmptyBufferDone(inputBufferHeader);
    }
}

OMX_ERRORTYPE SoftVPXEncoder::getExtensionIndex(
        const char *name, OMX_INDEXTYPE *index) {
    if (!strcmp(name, "OMX.google.android.index.storeMetaDataInBuffers")) {
        *index = OMX_IndexVendorStartUnused;
        return OMX_ErrorNone;
    }

    return SimpleSoftOMXComponent::getExtensionIndex(name, index);
}

}  // namespace android


android::SoftOMXComponent *createSoftOMXComponent(
        const char *name, const OMX_CALLBACKTYPE *callbacks,
        OMX_PTR appData, OMX_COMPONENTTYPE **component) {
    return new android::SoftVPXEncoder(name, callbacks, appData, component);
}
