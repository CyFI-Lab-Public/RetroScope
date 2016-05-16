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
 * @file   M4MCS_API.h
 * @brief  MCS error codes definitions (Media Compressor Service)
 * @note
 *************************************************************************
 **/

#ifndef __M4MCS_ErrorCodes_H__
#define __M4MCS_ErrorCodes_H__

/**
 *    OSAL basic types and errors */
#include "M4OSA_Types.h"
#include "M4OSA_Error.h"

/**
 *    OSAL core ID definitions */
#include "M4OSA_CoreID.h"


/************************************************************************/
/* Warning codes                                                        */
/************************************************************************/

/* End of processing, user should now call M4MCS_close() */
#define M4MCS_WAR_TRANSCODING_DONE            M4OSA_ERR_CREATE( M4_WAR, M4MCS, 0x1)
/* Mediatype is not supported by the MCS */
#define M4MCS_WAR_MEDIATYPE_NOT_SUPPORTED    M4OSA_ERR_CREATE( M4_WAR, M4MCS, 0x2)
/* Indicate that picture will be automatically resized to fit into the required
   parameters (file size) */
#define M4MCS_WAR_PICTURE_AUTO_RESIZE        M4OSA_ERR_CREATE( M4_WAR, M4MCS, 0x3)

/************************************************************************/
/* Error codes                                                          */
/************************************************************************/


/* ----- OPEN ERRORS ----- */

/* The input file contains no supported stream (may be a corrupted file) */
#define M4MCS_ERR_INPUT_FILE_CONTAINS_NO_SUPPORTED_STREAM   M4OSA_ERR_CREATE( M4_ERR, M4MCS, 0x01)
/* The input file is invalid/corrupted */
#define M4MCS_ERR_INVALID_INPUT_FILE                        M4OSA_ERR_CREATE( M4_ERR, M4MCS, 0x02)
/* The input video frame size parameter is undefined */
#define M4MCS_ERR_INVALID_INPUT_VIDEO_FRAME_SIZE            M4OSA_ERR_CREATE( M4_ERR, M4MCS, 0x03)
/* The input video frame size is non multiple of 16 */
#define M4MCS_ERR_INPUT_VIDEO_SIZE_NON_X16                  M4OSA_ERR_CREATE( M4_ERR, M4MCS, 0x04)


/* ----- SET OUTPUT PARAMS ERRORS ----- */

/* The output video format parameter is undefined */
#define M4MCS_ERR_UNDEFINED_OUTPUT_VIDEO_FORMAT             M4OSA_ERR_CREATE( M4_ERR, M4MCS, 0x10)
/* The output video frame size parameter is undefined */
#define M4MCS_ERR_UNDEFINED_OUTPUT_VIDEO_FRAME_SIZE         M4OSA_ERR_CREATE( M4_ERR, M4MCS, 0x11)
/* The output video frame rate parameter is undefined */
#define M4MCS_ERR_UNDEFINED_OUTPUT_VIDEO_FRAME_RATE         M4OSA_ERR_CREATE( M4_ERR, M4MCS, 0x12)
/* The output audio format parameter is undefined */
#define M4MCS_ERR_UNDEFINED_OUTPUT_AUDIO_FORMAT             M4OSA_ERR_CREATE( M4_ERR, M4MCS, 0x13)
/* The output video frame size parameter is incompatible with H263 encoding */
#define M4MCS_ERR_INVALID_VIDEO_FRAME_SIZE_FOR_H263         M4OSA_ERR_CREATE( M4_ERR, M4MCS, 0x14)
/* The output video frame rate parameter is incompatible with H263 encoding
   (It can't happen in current version of MCS!) */
#define M4MCS_ERR_INVALID_VIDEO_FRAME_RATE_FOR_H263         M4OSA_ERR_CREATE( M4_ERR, M4MCS, 0x15)
/* A null clip duration as been computed, which is unvalid (should never happen!) */
#define M4MCS_ERR_DURATION_IS_NULL                          M4OSA_ERR_CREATE( M4_ERR, M4MCS, 0x16)
/* The .mp4 container cannot handle h263 codec */
#define M4MCS_ERR_H263_FORBIDDEN_IN_MP4_FILE                M4OSA_ERR_CREATE( M4_ERR, M4MCS, 0x17)


/* ----- PREPARE DECODERS ERRORS ----- */

/* H263 Profile (other than 0) is not supported */
#define M4MCS_ERR_H263_PROFILE_NOT_SUPPORTED                M4OSA_ERR_CREATE( M4_ERR, M4MCS, 0x20)
/* The input file contains an AAC audio track with an invalid sampling frequency
   (should never happen) */
#define M4MCS_ERR_INVALID_AAC_SAMPLING_FREQUENCY            M4OSA_ERR_CREATE( M4_ERR, M4MCS, 0x21)
/* The audio conversion (AAC to AMR-NB, or MP3) failed */
#define M4MCS_ERR_AUDIO_CONVERSION_FAILED                   M4OSA_ERR_CREATE( M4_ERR, M4MCS, 0x22)


/* ----- SET ENCODING PARAMS ERRORS ----- */

/* Begin cut time is larger than the input clip duration */
#define M4MCS_ERR_BEGIN_CUT_LARGER_THAN_DURATION            M4OSA_ERR_CREATE( M4_ERR, M4MCS, 0x30)
/* Begin cut and End cut are equals */
#define M4MCS_ERR_BEGIN_CUT_EQUALS_END_CUT                  M4OSA_ERR_CREATE( M4_ERR, M4MCS, 0x31)
/* End cut time is smaller than begin cut time */
#define M4MCS_ERR_END_CUT_SMALLER_THAN_BEGIN_CUT            M4OSA_ERR_CREATE( M4_ERR, M4MCS, 0x32)
/* Not enough space to store whole output file at given bitrates */
#define M4MCS_ERR_MAXFILESIZE_TOO_SMALL                     M4OSA_ERR_CREATE( M4_ERR, M4MCS, 0x33)
/* Video bitrate is too low (avoid ugly video) */
#define M4MCS_ERR_VIDEOBITRATE_TOO_LOW                      M4OSA_ERR_CREATE( M4_ERR, M4MCS, 0x34)
/* Audio bitrate is too low (16 kbps min for aac, 12.2 for amr, 8 for mp3) */
#define M4MCS_ERR_AUDIOBITRATE_TOO_LOW                      M4OSA_ERR_CREATE( M4_ERR, M4MCS, 0x35)
/* Video bitrate too high (we limit to 800 kbps) */
#define M4MCS_ERR_VIDEOBITRATE_TOO_HIGH                     M4OSA_ERR_CREATE( M4_ERR, M4MCS, 0x36)
/* Audio bitrate too high (we limit to 96 kbps) */
#define M4MCS_ERR_AUDIOBITRATE_TOO_HIGH                     M4OSA_ERR_CREATE( M4_ERR, M4MCS, 0x37)

/* ----- OTHERS ERRORS ----- */
#define M4MCS_ERR_OUTPUT_FILE_SIZE_TOO_SMALL                M4OSA_ERR_CREATE( M4_ERR, M4MCS, 0x50)
#define M4MCS_ERR_NOMORE_SPACE                              M4OSA_ERR_CREATE(M4_ERR, M4MCS, 0x51)
#define M4MCS_ERR_FILE_DRM_PROTECTED                        M4OSA_ERR_CREATE(M4_ERR, M4MCS, 0x52)
#endif /* __M4MCS_ErrorCodes_H__ */

