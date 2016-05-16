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

#ifndef VIDEOEDITORRESAMPLER_H
#define VIDEOEDITORRESAMPLER_H
#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */

#include "M4OSA_Types.h"

M4OSA_Context LVAudioResamplerCreate(M4OSA_Int32 bitDepth, M4OSA_Int32 inChannelCount,
                                     M4OSA_Int32 sampleRate, M4OSA_Int32 quality);
void LVAudiosetSampleRate(M4OSA_Context resamplerContext,M4OSA_Int32 inSampleRate);
void LVAudiosetVolume(M4OSA_Context resamplerContext, M4OSA_Int16 left, M4OSA_Int16 right) ;
void LVAudioresample_LowQuality(M4OSA_Int16* out, M4OSA_Int16* input,
                                     M4OSA_Int32 outFrameCount, M4OSA_Context resamplerContext);
void LVDestroy(M4OSA_Context resamplerContext);

void MonoTo2I_16( const M4OSA_Int16 *src,
                        M4OSA_Int16 *dst,
                        M4OSA_Int16 n);

void From2iToMono_16( const M4OSA_Int16 *src,
                            M4OSA_Int16 *dst,
                            M4OSA_Int16 n);
#ifdef __cplusplus
}
#endif /* __cplusplus */
#endif /* VIDEOEDITORRESAMPLER_H */


