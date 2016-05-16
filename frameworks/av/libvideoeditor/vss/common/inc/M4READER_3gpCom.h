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
 * @file    M4READER_3gpCom.h
 * @brief    Generic encapsulation of the core 3gp reader
 * @note    This file declares the generic shell interface retrieving function
 *            of the 3GP reader
 ************************************************************************
*/

#ifndef __M4READER_3GPCOM_H__
#define __M4READER_3GPCOM_H__

#include "NXPSW_CompilerSwitches.h"

#include "M4OSA_Types.h"
#include "M4OSA_Error.h"
#include "M4READER_Common.h"

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Error: Function M4READER_Com3GP_getNextStreamHandler must be called before.
 */
#define M4ERR_NO_VIDEO_STREAM_RETRIEVED_YET        M4OSA_ERR_CREATE(M4_ERR, M4READER_3GP, 0x000001)

/**
 * Error: No video stream H263 in file.
 */
#define M4ERR_VIDEO_NOT_H263                    M4OSA_ERR_CREATE(M4_ERR, M4READER_3GP, 0x000002)

/**
 * There has been a problem with the decoder configuration information, seems to be invalid */
#define M4ERR_READER3GP_DECODER_CONFIG_ERROR    M4OSA_ERR_CREATE(M4_ERR, M4READER_3GP, 0x000003)

#define M4READER_COM3GP_MAXVIDEOSTREAM  5
#define M4READER_COM3GP_MAXAUDIOSTREAM  5
#define M4READER_COM3GP_MAXTEXTSTREAM   5

typedef struct
{
    M4OSA_Context                m_pFFContext;    /**< core file format context */

    M4_StreamHandler*            m_AudioStreams[M4READER_COM3GP_MAXAUDIOSTREAM];
    M4_StreamHandler*            m_pAudioStream;    /**< pointer to the current allocated audio
                                                            stream handler */

    M4_StreamHandler*            m_VideoStreams[M4READER_COM3GP_MAXVIDEOSTREAM];
    M4_StreamHandler*            m_pVideoStream;    /**< pointer to the current allocated video
                                                            stream handler */

#ifdef M4VPS_SUPPORT_TTEXT
    M4_StreamHandler*            m_TextStreams[M4READER_COM3GP_MAXTEXTSTREAM];
    M4_StreamHandler*            m_pTextStream;    /**< pointer to the current allocated text
                                                            stream handler */
#endif /*M4VPS_SUPPORT_TTEXT*/

} M4READER_Com3GP_Context;

/**
 ************************************************************************
 * structure M4READER_3GP_Buffer (but nothing specific to 3GP, nor to a reader !)
 * @brief     This structure defines a buffer that can be used to exchange data (should be in OSAL)
 ************************************************************************
*/
typedef struct
{
    M4OSA_UInt32    size;            /**< the size in bytes of the buffer */
    M4OSA_MemAddr8    dataAddress;    /**< the pointer to the buffer */
} M4READER_3GP_Buffer;

/**
 ************************************************************************
 * enum     M4READER_3GP_OptionID
 * @brief    This enum defines the reader options specific to the 3GP format.
 * @note    These options can be read from or written to a 3GP reader via M4READER_3GP_getOption.
 ************************************************************************
*/
typedef enum
{
    /**
     * Get the DecoderConfigInfo for H263,
     * option value must be a pointer to M4READER_3GP_H263Properties allocated by caller */
    M4READER_3GP_kOptionID_H263Properties = M4OSA_OPTION_ID_CREATE(M4_READ, M4READER_3GP, 0x01),

    /**
     * Get the Purple Labs drm information */
    M4READER_3GP_kOptionID_PurpleLabsDrm = M4OSA_OPTION_ID_CREATE(M4_READ, M4READER_3GP, 0x02),

    /**
     * Set the Fast open mode (Only the first AU of each stream will be parsed -> less CPU,
                                 less RAM). */
    M4READER_3GP_kOptionID_FastOpenMode = M4OSA_OPTION_ID_CREATE(M4_WRITE, M4READER_3GP, 0x03),

    /**
     * Set the Audio only mode (the video stream won't be opened) */
    M4READER_3GP_kOptionID_AudioOnly = M4OSA_OPTION_ID_CREATE(M4_WRITE, M4READER_3GP, 0x04),

    /**
     * Set the Video only mode (the audio stream won't be opened) */
    M4READER_3GP_kOptionID_VideoOnly = M4OSA_OPTION_ID_CREATE(M4_WRITE, M4READER_3GP, 0x05),

    /**
     * Get the next video CTS */
    M4READER_3GP_kOptionID_getNextVideoCTS = M4OSA_OPTION_ID_CREATE(M4_READ, M4READER_3GP, 0x06)

} M4READER_3GP_OptionID;


/**
 ************************************************************************
 * struct    M4READER_3GP_H263Properties
 * @brief    Contains info about H263 stream read from the 3GP file.
 ************************************************************************
*/
typedef struct
{
    /**< the profile as defined in the Visual Object Sequence header, if present */
    M4OSA_UInt8        uiProfile;
    /**< the level as defined in the Visual Object Sequence header, if present */
    M4OSA_UInt8        uiLevel;

} M4READER_3GP_H263Properties;

/**
 ************************************************************************
 * @brief    Get the next stream found in the 3gp file
 * @note
 * @param    pContext:        (IN)    Context of the reader
 * @param    pMediaFamily:    (OUT)    Pointer to a user allocated M4READER_MediaFamily that will
 *                                      be filled with the media family of the found stream
 * @param    pStreamHandler:    (OUT)    Pointer to a stream handler that will be allocated and
 *                                          filled with the found stream description
 * @return    M4NO_ERROR                 There is no error
 * @return    M4ERR_PARAMETER            At least one parameter is not properly set
 * @return    M4WAR_NO_MORE_STREAM    No more available stream in the media (all streams found)
 ************************************************************************
*/
M4OSA_ERR M4READER_Com3GP_getNextStreamHandler(M4OSA_Context context,
                                                 M4READER_MediaFamily *pMediaFamily,
                                                 M4_StreamHandler **pStreamHandler);

/**
 ************************************************************************
 * @brief    Prepare the  access unit (AU)
 * @note    An AU is the smallest possible amount of data to be decoded by a decoder.
 * @param    pContext:        (IN)        Context of the reader
 * @param    pStreamHandler    (IN)        The stream handler of the stream to make jump
 * @param    pAccessUnit        (IN/OUT)    Pointer to an access unit to fill with read data
 *                                          (the au structure is allocated by the user, and must
 *                                          be initialized by calling M4READER_fillAuStruct_fct
 *                                          after creation)
 * @return    M4NO_ERROR                     There is no error
 * @return    M4ERR_PARAMETER                At least one parameter is not properly set
 * @returns    M4ERR_ALLOC                    Memory allocation failed
 ************************************************************************
*/
M4OSA_ERR M4READER_Com3GP_fillAuStruct(M4OSA_Context context, M4_StreamHandler *pStreamHandler,
                                         M4_AccessUnit *pAccessUnit);

/**
 ************************************************************************
 * @brief    Cleans up the stream handler
 * @param    pContext: (IN/OUT) Context of the reader shell
 * @param    pStreamHandler: (IN/OUT) Stream handler
 * @return    M4ERR_PARAMETER:    The context is null
 * @return    M4NO_ERROR:            No error
 ************************************************************************
*/
M4OSA_ERR M4READER_Com3GP_cleanUpHandler(M4_StreamHandler* pStreamHandler);

#ifdef __cplusplus
}
#endif /* __cplusplus */

#endif /* __M4READER_3GPCOM_H__ */

