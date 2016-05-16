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
* @file   VideoEditorMp3Reader.cpp
* @brief  StageFright shell MP3 Reader
*************************************************************************
*/
#ifndef VIDEOEDITOR_MP3READER_H
#define VIDEOEDITOR_MP3READER_H

#include "M4READER_Common.h"

M4OSA_ERR VideoEditorMp3Reader_getInterface(
        M4READER_MediaType *pMediaType,
        M4READER_GlobalInterface **pRdrGlobalInterface,
        M4READER_DataInterface **pRdrDataInterface);

#endif /* VIDEOEDITOR_MP3READER_H */
