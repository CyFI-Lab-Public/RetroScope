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
* @file   VideoEditorVideoEncoder.cpp
* @brief  StageFright shell video encoder
*************************************************************************
*/
#ifndef VIDEOEDITOR_VIDEOENCODER_H
#define VIDEOEDITOR_VIDEOENCODER_H

#include "M4ENCODER_common.h"

M4OSA_ERR VideoEditorVideoEncoder_getInterface_H263(M4ENCODER_Format* pFormat,
        M4ENCODER_GlobalInterface** pEncoderInterface, M4ENCODER_OpenMode mode);

M4OSA_ERR VideoEditorVideoEncoder_getInterface_MPEG4(M4ENCODER_Format* pFormat,
        M4ENCODER_GlobalInterface** pEncoderInterface, M4ENCODER_OpenMode mode);

M4OSA_ERR VideoEditorVideoEncoder_getInterface_H264(M4ENCODER_Format* pFormat,
        M4ENCODER_GlobalInterface** pEncoderInterface, M4ENCODER_OpenMode mode);

#endif //VIDEOEDITOR_VIDEOENCODER_H
