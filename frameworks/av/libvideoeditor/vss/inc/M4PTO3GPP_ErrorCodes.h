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
 * @file    M4PTO3GPP_ErrorCodes.h
 * @brief    Picture to 3gpp Service error definitions.
 * @note
 ******************************************************************************
 */

#ifndef __M4PTO3GPP_ErrorCodes_H__
#define __M4PTO3GPP_ErrorCodes_H__

/**
 *    OSAL basic types and errors */
#include "M4OSA_Types.h"
#include "M4OSA_Error.h"

/**
 *    OSAL core ID definitions */
#include "M4OSA_CoreID.h"


/**
 *    The output video format parameter is undefined */
#define ERR_PTO3GPP_UNDEFINED_OUTPUT_VIDEO_FORMAT    M4OSA_ERR_CREATE( M4_ERR, M4PTO3GPP, 0x0001 )
/**
 *    The output video frame size parameter is undefined */
#define ERR_PTO3GPP_UNDEFINED_OUTPUT_VIDEO_FRAME_SIZE        \
    M4OSA_ERR_CREATE( M4_ERR, M4PTO3GPP, 0x0002 )
/**
 *    The output video bit-rate parameter is undefined */
#define ERR_PTO3GPP_UNDEFINED_OUTPUT_VIDEO_BITRATE           \
    M4OSA_ERR_CREATE( M4_ERR, M4PTO3GPP, 0x0003 )
/**
 *    The output video frame size parameter is incompatible with H263 encoding */
#define ERR_PTO3GPP_INVALID_VIDEO_FRAME_SIZE_FOR_H263        \
    M4OSA_ERR_CREATE( M4_ERR, M4PTO3GPP, 0x0004 )
/**
 *    The file size is undefined */
#define ERR_PTO3GPP_INVALID_FILE_SIZE                M4OSA_ERR_CREATE( M4_ERR, M4PTO3GPP, 0x0005 )
/**
 * The input audio file contains a track format not handled by PTO3GPP */
#define ERR_PTO3GPP_UNHANDLED_AUDIO_TRACK_INPUT_FILE         \
    M4OSA_ERR_CREATE( M4_ERR, M4PTO3GPP, 0x0006 )
/**
 *    The output video format parameter is undefined */
#define ERR_PTO3GPP_UNDEFINED_OUTPUT_AUDIO_FORMAT    M4OSA_ERR_CREATE( M4_ERR, M4PTO3GPP, 0x0007 )

/**
 *    The AMR decoder initialization failed */
#define ERR_PTO3GPP_AMR_DECODER_INIT_ERROR           M4OSA_ERR_CREATE( M4_ERR, M4PTO3GPP, 0x0020 )
/**
 *    The AMR decoder failed */
#define ERR_PTO3GPP_AMR_DECODE_ERROR                 M4OSA_ERR_CREATE( M4_ERR, M4PTO3GPP, 0x0021 )
/**
 *    The AMR decoder cleanup failed */
#define ERR_PTO3GPP_AMR_DECODER_DESTROY_ERROR        M4OSA_ERR_CREATE( M4_ERR, M4PTO3GPP, 0x0022 )

/**
 *    The video encoder initialization failed */
#define ERR_PTO3GPP_VIDEO_ENCODER_INIT_ERROR         M4OSA_ERR_CREATE( M4_ERR, M4PTO3GPP, 0x0023 )
/**
 *    The video encoder decoding failed */
#define ERR_PTO3GPP_VIDEO_ENCODE_ERROR               M4OSA_ERR_CREATE( M4_ERR, M4PTO3GPP, 0x0024 )
/**
 *    The video encoder cleanup failed */
#define ERR_PTO3GPP_VIDEO_ENCODER_DESTROY_ERROR      M4OSA_ERR_CREATE( M4_ERR, M4PTO3GPP, 0x0025 )

/**
 *    The output file size parameter is undefined */
#define ERR_PTO3GPP_UNDEFINED_OUTPUT_FILE_SIZE       M4OSA_ERR_CREATE( M4_ERR, M4PTO3GPP, 0x0026 )

/**
 *    The Encoding is completed */
#define M4PTO3GPP_WAR_END_OF_PROCESSING              M4OSA_ERR_CREATE( M4_ERR, M4PTO3GPP, 0x0027 )

/**
 *    The Encoding is completed */
#define M4PTO3GPP_WAR_LAST_PICTURE                   M4OSA_ERR_CREATE( M4_ERR, M4PTO3GPP, 0x0028 )

/**
 *    The output audio padding parameter is undefined */
#define ERR_PTO3GPP_UNDEFINED_AUDIO_PADDING          M4OSA_ERR_CREATE( M4_ERR, M4PTO3GPP, 0x0029 )

/**
 * The video encoder encountered an Acces Unit error: very probably a file write error */
#define ERR_PTO3GPP_ENCODER_ACCES_UNIT_ERROR         M4OSA_ERR_CREATE( M4_ERR, M4PTO3GPP, 0x002A )

#endif /* __M4PTO3GPP_ErrorCodes_H__ */

