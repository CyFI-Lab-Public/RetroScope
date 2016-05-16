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
 ************************************************************************
 * @file   M4TOOL_VersionInfo.h
 * @brief  defines a common version information structure
 * @note
 *
 ************************************************************************
*/
#ifndef __M4TOOL_VERSIONINFO_H__
#define __M4TOOL_VERSIONINFO_H__

#include "M4OSA_Types.h"

/**
 * structure    M4_VersionInfo
 * @brief        This structure describes version of core component
 * @note        This structure is typically used to retrieve version information
 *                of a component via getOption function
 */
typedef struct _M4_VersionInfo
{
    M4OSA_UInt32 m_major;        /*major version of the component*/
    M4OSA_UInt32 m_minor;        /*minor version of the component*/
    M4OSA_UInt32 m_revision;    /*revision version of the component*/

    /* Structure size */
    M4OSA_UInt32 m_structSize;

} M4_VersionInfo;


#endif /*__M4TOOL_VERSIONINFO_H__*/

