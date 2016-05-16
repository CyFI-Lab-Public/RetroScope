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
 ******************************************************************************
 * @file    M4EXIFC_CommonAPI.h
 * @brief    EXIF common data header
 * @note    The types, structures and macros defined in this file allow reading
 *            and writing EXIF JPEG images compliant spec EXIF 2.2
 ******************************************************************************
*/


#ifndef __M4_EXIF_COMMON_API_H__
#define __M4_EXIF_COMMON_API_H__

#include "M4TOOL_VersionInfo.h"
#include "M4Common_types.h"
#include "M4OSA_Debug.h"
#include "M4OSA_Error.h"
#include "M4OSA_Types.h"
#include "M4OSA_Memory.h"
#include "M4OSA_CoreID.h"

#ifdef __cplusplus
extern "C" {
#endif

/**
 ************************************************************************
 * type M4EXIFC_Context
 ************************************************************************
*/
typedef M4OSA_Void*    M4EXIFC_Context;

/**
 ******************************************************************************
 * Errors & Warnings
 ******************************************************************************
*/

#define M4EXIFC_NO_ERR              0x00000000    /**< invalid parameter */
#define M4EXIFC_ERR_PARAMETER       0x00000001    /**< invalid parameter */
#define M4EXIFC_ERR_ALLOC           0x00000002    /**< allocation error */
#define M4EXIFC_ERR_BAD_CONTEXT     0x00000003    /**< invalid context */
#define M4EXIFC_ERR_NOT_COMPLIANT   0x00000004    /**< the image in buffer is not
                                                       JPEG compliant */
#define M4EXIFC_ERR_NO_APP_FOUND    0x00000005    /**< the JPEG image does not contain any APP1
                                                        Exif 2.2 compliant */
#define M4EXIFC_WAR_NO_THUMBNAIL    0x00000006    /**< the Exif part does not contain any
                                                        thumbnail */
#define M4EXIFC_ERR_APP_TRUNCATED   0x00000007    /**< The APP1 section in input buffer is
                                                        not complete */


/**
 ******************************************************************************
 * structure    M4EXIFC_BasicTags
 * @brief        This structure stores the basic tags values.
 * @note        This Exif reader focuses on a set of "Entry Tags".
 *                This structure contains the corresponding "Entry Values" of these tags.
 *                M4EXIFC_Char* fields of structure are Null terminated Strings.
 ******************************************************************************
*/
typedef struct
{
    M4OSA_Int32        width;                /**< image width in pixels */
    M4OSA_Int32        height;               /**< image height in pixels */
    M4OSA_Char        *creationDateTime;     /**< date and time original image was generated */
    M4OSA_Char        *lastChangeDateTime;   /**< file change date and time */
    M4OSA_Char        *description;          /**< image title */
    M4OSA_Char        *make;                 /**< manufacturer of image input equipment */
    M4OSA_Char        *model;                /**< model of image input equipment */
    M4OSA_Char        *software;             /**< software used */
    M4OSA_Char        *artist;               /**< person who created the image */
    M4OSA_Char        *copyright;            /**< copyright holder */
    M4COMMON_Orientation orientation;        /**< orientation of image */
    M4OSA_Int32        thumbnailSize;        /**< size of the thumbnail */
    M4OSA_UInt8        *thumbnailImg;        /**< pointer to the thumbnail in main image buffer*/
    M4OSA_Char        *latitudeRef;          /**< latitude reference */
    M4COMMON_Location latitude;              /**< latitude */
    M4OSA_Char        *longitudeRef;         /**< longitude reference */
    M4COMMON_Location longitude;             /**< longitude */

} M4EXIFC_BasicTags;


/**
 ******************************************************************************
 * M4OSA_ERR M4EXIFC_getVersion    (M4_VersionInfo *pVersion)
 * @brief    get the version numbers of the exif library.
 * @note    This function retrieves the version numbers in a structure.
 * @param    pVersion:    (OUT)        the structure containing version numbers
 * @return    M4NO_ERROR:                there is no error
 * @return    M4EXIFC_ERR_PARAMETER:        (Debug only) the parameter is M4EXIFC_NULL.
 ******************************************************************************
*/
M4OSA_ERR M4EXIFC_getVersion (M4_VersionInfo *pVersion);



#ifdef __cplusplus
}
#endif /* __cplusplus*/
#endif /* __M4_EXIF_COMMON_API_H__ */

