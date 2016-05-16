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
* @file   VideoEditorVideoDecoder.cpp
* @brief  StageFright shell video decoder
*************************************************************************
*/
#ifndef VIDEOEDITOR_VIDEODECODER_H
#define VIDEOEDITOR_VIDEODECODER_H

#include "M4DECODER_Common.h"

M4OSA_ERR VideoEditorVideoDecoder_getInterface_MPEG4(
        M4DECODER_VideoType *pDecoderType,
        M4OSA_Context *pDecoderInterface);

M4OSA_ERR VideoEditorVideoDecoder_getInterface_H264(
        M4DECODER_VideoType *pDecoderType,
        M4OSA_Context *pDecoderInterface);

M4OSA_ERR VideoEditorVideoDecoder_getSoftwareInterface_MPEG4(
        M4DECODER_VideoType *pDecoderType,
        M4OSA_Context *pDecInterface);

M4OSA_ERR VideoEditorVideoDecoder_getSoftwareInterface_H264(
        M4DECODER_VideoType *pDecoderType,
        M4OSA_Context *pDecInterface);

M4OSA_ERR VideoEditorVideoDecoder_getVideoDecodersAndCapabilities(
    M4DECODER_VideoDecoders** decoders);

#endif // VIDEOEDITOR_VIDEODECODER_H
