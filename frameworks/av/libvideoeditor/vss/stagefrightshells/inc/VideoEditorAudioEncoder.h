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
* @file   VideoEditorAudioEncoder.cpp
* @brief  StageFright shell Audio Encoder
*************************************************************************
*/
#ifndef VIDEOEDITOR_AUDIOENCODER_H
#define VIDEOEDITOR_AUDIOENCODER_H

#include "M4OSA_CoreID.h"
#include "M4OSA_Memory.h"
#include "M4ENCODER_AudioCommon.h"

M4OSA_ERR VideoEditorAudioEncoder_getInterface_AAC(
        M4ENCODER_AudioFormat* pFormat,
        M4ENCODER_AudioGlobalInterface** pEncoderInterface);

M4OSA_ERR VideoEditorAudioEncoder_getInterface_AMRNB(
        M4ENCODER_AudioFormat* pFormat,
        M4ENCODER_AudioGlobalInterface** pEncoderInterface);

M4OSA_ERR VideoEditorAudioEncoder_getInterface_MP3(
        M4ENCODER_AudioFormat* pFormat,
        M4ENCODER_AudioGlobalInterface** pEncoderInterface);

#endif /* VIDEOEDITOR_AUDIOENCODER_H */
