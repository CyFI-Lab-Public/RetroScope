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

#ifndef __M4VD_EXTERNAL_INTERFACE_H__
#define __M4VD_EXTERNAL_INTERFACE_H__

#include "M4DECODER_Common.h"

#ifdef __cplusplus
extern "C" {
#endif

/* ----- DSI bitstream parser ----- */

/* This function is available to clients of the shell to allow them to analyse clips
(useful for video editing) without having to instanciate a decoder, which can be useful precisely
if HW decoders are a possibility. */

M4OSA_ERR M4DECODER_EXTERNAL_ParseVideoDSI(
                      M4OSA_UInt8* pVol, M4OSA_Int32 aVolSize,
                      M4DECODER_MPEG4_DecoderConfigInfo* pDci,
                      M4DECODER_VideoSize* pVideoSize);

M4OSA_ERR getAVCProfileAndLevel(M4OSA_UInt8* pDSI, M4OSA_Int32 DSISize,
                      M4OSA_Int32 *pProfile, M4OSA_Int32 *pLevel);

M4OSA_ERR getH263ProfileAndLevel(M4OSA_UInt8* pDSI, M4OSA_Int32 DSISize,
                      M4OSA_Int32 *pProfile, M4OSA_Int32 *pLevel);

M4OSA_ERR getMPEG4ProfileAndLevel(M4OSA_UInt8 profileAndLevel,
                      M4OSA_Int32 *pProfile, M4OSA_Int32 *pLevel);

#ifdef __cplusplus
}
#endif /* __cplusplus */

#endif /* __M4VD_EXTERNAL_INTERFACE_H__ */
