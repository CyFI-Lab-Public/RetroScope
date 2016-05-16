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
* @file   VideoEditorAudioDecoder.cpp
* @brief  StageFright shell Audio Decoder
*************************************************************************
*/
#ifndef VIDEOEDITOR_AUDIODECODER_H
#define VIDEOEDITOR_AUDIODECODER_H

#include "M4AD_Common.h"

M4OSA_ERR VideoEditorAudioDecoder_getInterface_AAC(M4AD_Type* pDecoderType,
        M4AD_Interface** pDecoderInterface);

M4OSA_ERR VideoEditorAudioDecoder_getInterface_AMRNB(M4AD_Type* pDecoderType,
        M4AD_Interface** pDecoderInterface);

M4OSA_ERR VideoEditorAudioDecoder_getInterface_AMRWB(M4AD_Type* pDecoderType,
        M4AD_Interface** pDecoderInterface);

M4OSA_ERR VideoEditorAudioDecoder_getInterface_MP3(M4AD_Type* pDecoderType,
        M4AD_Interface** pDecoderInterface);

#endif /* VIDEOEDITOR_AUDIODECODER_H */
