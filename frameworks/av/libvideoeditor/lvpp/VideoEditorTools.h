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

#ifndef ANDROID_VE_TOOLS_H
#define ANDROID_VE_TOOLS_H

#include "M4OSA_Types.h"
#include "M4OSA_Memory.h"
#include "M4OSA_Debug.h"
#include "M4VIFI_FiltersAPI.h"
/* Macro definitions */
#include "M4VIFI_Defines.h"
/* Clip table declaration */
#include "M4VIFI_Clip.h"
#include "M4VFL_transition.h"
#include "M4VSS3GPP_API.h"
#include "M4xVSS_API.h"
#include "M4xVSS_Internal.h"
#include "M4AIR_API.h"
#include "PreviewRenderer.h"

#define MEDIA_RENDERING_INVALID 255
#define TRANSPARENT_COLOR 0x7E0
#define LUM_FACTOR_MAX 10
enum {
    VIDEO_EFFECT_NONE               = 0,
    VIDEO_EFFECT_BLACKANDWHITE      = 1,
    VIDEO_EFFECT_PINK               = 2,
    VIDEO_EFFECT_GREEN              = 4,
    VIDEO_EFFECT_SEPIA              = 8,
    VIDEO_EFFECT_NEGATIVE           = 16,
    VIDEO_EFFECT_FRAMING            = 32,
    VIDEO_EFFECT_FIFTIES            = 64,
    VIDEO_EFFECT_COLOR_RGB16        = 128,
    VIDEO_EFFECT_GRADIENT           = 256,
    VIDEO_EFFECT_FADEFROMBLACK      = 512,
    VIDEO_EFFECT_FADETOBLACK        = 2048,
};

typedef struct {
    M4VIFI_UInt8 *vidBuffer;
    M4OSA_UInt32 videoWidth;
    M4OSA_UInt32 videoHeight;
    M4OSA_UInt32 timeMs;
    M4OSA_UInt32 timeOffset; //has the duration of clips played.
                             //The flag shall be used for Framing.
    M4VSS3GPP_EffectSettings* effectsSettings;
    M4OSA_UInt32 numberEffects;
    M4OSA_UInt32 outVideoWidth;
    M4OSA_UInt32 outVideoHeight;
    M4OSA_UInt32 currentVideoEffect;
    M4OSA_Bool isFiftiesEffectStarted;
    M4xVSS_MediaRendering renderingMode;
    uint8_t *pOutBuffer;
    size_t outBufferStride;
    M4VIFI_UInt8*  overlayFrameRGBBuffer;
    M4VIFI_UInt8*  overlayFrameYUVBuffer;
} vePostProcessParams;

M4VIFI_UInt8 M4VIFI_YUV420PlanarToYUV420Semiplanar(void *user_data, M4VIFI_ImagePlane *PlaneIn, M4VIFI_ImagePlane *PlaneOut );
M4VIFI_UInt8 M4VIFI_SemiplanarYUV420toYUV420(void *user_data, M4VIFI_ImagePlane *PlaneIn, M4VIFI_ImagePlane *PlaneOut );

M4OSA_ERR M4VSS3GPP_externalVideoEffectColor(M4OSA_Void *pFunctionContext, M4VIFI_ImagePlane *PlaneIn,
                                                    M4VIFI_ImagePlane *PlaneOut,M4VSS3GPP_ExternalProgress *pProgress, M4OSA_UInt32 uiEffectKind);

M4OSA_ERR M4VSS3GPP_externalVideoEffectFraming( M4OSA_Void *userData, M4VIFI_ImagePlane PlaneIn[3], M4VIFI_ImagePlane *PlaneOut, M4VSS3GPP_ExternalProgress *pProgress, M4OSA_UInt32 uiEffectKind );

M4OSA_ERR M4VSS3GPP_externalVideoEffectFifties( M4OSA_Void *pUserData, M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut, M4VSS3GPP_ExternalProgress *pProgress, M4OSA_UInt32 uiEffectKind );

unsigned char M4VFL_modifyLumaWithScale(M4ViComImagePlane *plane_in, M4ViComImagePlane *plane_out, unsigned long lum_factor, void *user_data);

M4OSA_ERR M4xVSS_internalConvertRGBtoYUV(M4xVSS_FramingStruct* framingCtx);
M4VIFI_UInt8    M4VIFI_xVSS_RGB565toYUV420(void *pUserData, M4VIFI_ImagePlane *pPlaneIn,
                                                      M4VIFI_ImagePlane *pPlaneOut);

M4OSA_ERR M4xVSS_internalConvertRGB888toYUV(M4xVSS_FramingStruct* framingCtx);
M4VIFI_UInt8 M4VIFI_RGB888toYUV420(void *pUserData, M4VIFI_ImagePlane *PlaneIn, M4VIFI_ImagePlane PlaneOut[3]);

/*+ Handle the image files here */
M4OSA_ERR LvGetImageThumbNail(const char *fileName, M4OSA_UInt32 height, M4OSA_UInt32 width, M4OSA_Void **pBuffer);
/*- Handle the image files here */

M4OSA_ERR applyRenderingMode(M4VIFI_ImagePlane* pPlaneIn, M4VIFI_ImagePlane* pPlaneOut, M4xVSS_MediaRendering mediaRendering);


M4VIFI_UInt8 M4VIFI_YUV420toYUV420(void *user_data, M4VIFI_ImagePlane PlaneIn[3], M4VIFI_ImagePlane *PlaneOut );
M4VIFI_UInt8    M4VIFI_ResizeBilinearYUV420toYUV420(void *pUserData,
                                                                M4VIFI_ImagePlane *pPlaneIn,
                                                                M4VIFI_ImagePlane *pPlaneOut);

M4OSA_Void prepareYUV420ImagePlane(M4VIFI_ImagePlane *plane,
    M4OSA_UInt32 width, M4OSA_UInt32 height, M4VIFI_UInt8 *buffer,
    M4OSA_UInt32 reportedWidth, M4OSA_UInt32 reportedHeight);

M4OSA_Void prepareYV12ImagePlane(M4VIFI_ImagePlane *plane,
    M4OSA_UInt32 width, M4OSA_UInt32 height, M4OSA_UInt32 stride, M4VIFI_UInt8 *buffer);

M4OSA_Void swapImagePlanes(
    M4VIFI_ImagePlane *planeIn, M4VIFI_ImagePlane *planeOut,
    M4VIFI_UInt8 *buffer1, M4VIFI_UInt8 *buffer2);

M4OSA_Void computePercentageDone(
     M4OSA_UInt32 ctsMs, M4OSA_UInt32 effectStartTimeMs,
     M4OSA_UInt32 effectDuration, M4OSA_Double *percentageDone);

M4OSA_Void computeProgressForVideoEffect(
     M4OSA_UInt32 ctsMs, M4OSA_UInt32 effectStartTimeMs,
     M4OSA_UInt32 effectDuration, M4VSS3GPP_ExternalProgress* extProgress);

M4OSA_ERR prepareFramingStructure(
    M4xVSS_FramingStruct* framingCtx,
    M4VSS3GPP_EffectSettings* effectsSettings, M4OSA_UInt32 index,
    M4VIFI_UInt8* overlayRGB, M4VIFI_UInt8* overlayYUV);

M4OSA_ERR applyColorEffect(M4xVSS_VideoEffectType colorEffect,
    M4VIFI_ImagePlane *planeIn, M4VIFI_ImagePlane *planeOut,
    M4VIFI_UInt8 *buffer1, M4VIFI_UInt8 *buffer2, M4OSA_UInt16 rgbColorData);

M4OSA_ERR applyLumaEffect(M4VSS3GPP_VideoEffectType videoEffect,
    M4VIFI_ImagePlane *planeIn, M4VIFI_ImagePlane *planeOut,
    M4VIFI_UInt8 *buffer1, M4VIFI_UInt8 *buffer2, M4OSA_Int32 lum_factor);

M4OSA_ERR applyEffectsAndRenderingMode(vePostProcessParams *params,
    M4OSA_UInt32 reportedWidth, M4OSA_UInt32 reportedHeight);

android::status_t getVideoSizeByResolution(M4VIDEOEDITING_VideoFrameSize resolution,
    uint32_t *pWidth, uint32_t *pHeight);

M4VIFI_UInt8 M4VIFI_Rotate90LeftYUV420toYUV420(void* pUserData,
    M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);

M4VIFI_UInt8 M4VIFI_Rotate90RightYUV420toYUV420(void* pUserData,
    M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);

M4VIFI_UInt8 M4VIFI_Rotate180YUV420toYUV420(void* pUserData,
    M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);

M4OSA_ERR applyVideoRotation(M4OSA_Void* pBuffer,
    M4OSA_UInt32 width, M4OSA_UInt32 height, M4OSA_UInt32 rotation);
#endif // ANDROID_VE_TOOLS_H
