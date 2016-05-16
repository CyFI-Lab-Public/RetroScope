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
 * @file        M4AMRR_CoreReader.h
 * @brief        Implementation of AMR parser
 * @note        This file contains the API def. for AMR Parser.
 ******************************************************************************
*/
#ifndef __M4AMR_COREREADER_H__
#define __M4AMR_COREREADER_H__

#ifdef __cplusplus
extern "C" {
#endif

#include "M4OSA_Types.h"
#include "M4OSA_FileReader.h"
#include "M4SYS_Stream.h"
#include "M4SYS_AccessUnit.h"
#include "M4OSA_Time.h"
#include "M4TOOL_VersionInfo.h"

/**
 ******************************************************************************
 * AMR reader Errors & Warnings definition
 ******************************************************************************
*/
#define M4ERR_AMR_INVALID_FRAME_TYPE    M4OSA_ERR_CREATE(M4_ERR,M4AMR_READER, 0x000001)
#define M4ERR_AMR_NOT_COMPLIANT    M4OSA_ERR_CREATE(M4_ERR,M4AMR_READER, 0x000002)

/**
 ******************************************************************************
 * enumeration    M4AMRR_State
 * @brief        This enum defines the AMR reader states
 * @note        These states are used internaly, but can be retrieved from outside the reader.
 ******************************************************************************
*/
typedef enum{
    M4AMRR_kOpening    = 0x0100,
    M4AMRR_kOpened    = 0x0101,
    M4AMRR_kReading = 0x0200,
    M4AMRR_kReading_nextAU = 0x0201,
    M4AMRR_kClosed = 0x300
}M4AMRR_State;

/**
*******************************************************************************
* M4OSA_ERR M4AMRR_openRead (M4OSA_Context* pContext, M4OSA_Void* pFileDescriptor,
*                               M4OSA_FileReaderPointer* pFileFunction);
* @brief    M4AMRR_OpenRead parses the meta data of the AMR and allocates data structure
* @note        This function opens the file and creates a context for AMR  Parser.
*            - sets context to null if error occured.
* @param    pContext(OUT)        : AMR Reader context allocated in the function
* @param    pFileDesscriptor(IN): File descriptor of the input file
* @param    pFileFunction(IN)    : pointer to file function for file access
*
* @returns    M4NO_ERROR        : There is no error
* @returns    M4ERR_PARAMETER    : pContext and/or pFileDescriptor is NULL
* @returns    M4ERR_ALLOC        : Memory allocation failed
* @returns    M4ERR_FILE_NOT_FOUND : file cannot be found
* @returns    M4AMRR_ERR_AMR_NOT_COMPLIANT : Tthe input is not a AMR file
* @returns    M4OSA_FILE        : See OSAL file Spec. for details.
*******************************************************************************
*/
M4OSA_ERR M4AMRR_openRead (M4OSA_Context* pContext, M4OSA_Void* pFileDescriptor,
                            M4OSA_FileReadPointer* pFileFunction);

/**
******************************************************************************
* M4OSA_ERR M4AMRR_getNextStream(M4OSA_Context Context, M4SYS_StreamDescription* pStreamDesc );
* @brief    Reads the next available stream in the file
* @note        Get the stream description of the stream.
*            - This function assumes that there is only one stream in AMR file.
* @param    Context(IN/OUT)    : AMR Reader context
* @param    pStreamDesc(OUT): Description of the next read stream
*
* @returns     M4NO_ERROR        : There is no error
* @returns     M4ERR_PARAMETER    : atleast one parament is NULL
* @returns     M4ERR_BAD_CONTEXT    :    The provided context is not valid
* @returns     M4ERR_ALLOC        : Memory allocation failed
* @returns     M4ERR_STATE        : this function cannot be called in this state.
* @returns     M4AMRR_WAR_NO_MORE_STREAM : There are no more streams in the file.
******************************************************************************
*/

M4OSA_ERR M4AMRR_getNextStream(M4OSA_Context Context, M4SYS_StreamDescription* pStreamDesc );

/**
******************************************************************************
* M4OSA_ERR M4AMRR_startReading(M4OSA_Context Context, M4SYS_StreamID* pStreamIDs );
* @brief    Prepares the AMR reading of the specified stream Ids
* @note        This function changes the state of the reader reading.
* @param    Context(IN/OUT)    : AMR Reader context
* @param    pStreamIDs(IN)    : Array of stream Ids to be prepared.
*
* @returns     M4NO_ERROR        : There is no error
* @returns     M4ERR_PARAMETER    : atleast one parament is NULL
* @returns     M4ERR_BAD_CONTEXT    :    The provided context is not valid
* @returns     M4ERR_ALLOC        : Memory allocation failed
* @returns     M4ERR_STATE        : this function cannot be called in this state.
* @returns     M4ERR_BAD_STREAM_ID    : Atleast one of the stream Id. does not exist.
******************************************************************************
*/
M4OSA_ERR M4AMRR_startReading(M4OSA_Context Context, M4SYS_StreamID* pStreamIDs );

/**
******************************************************************************
* M4OSA_ERR M4AMRR_nextAU(M4OSA_Context Context, M4SYS_StreamID StreamID, M4SYS_AccessUnit* pAu);
* @brief    Reads the access unit into the providing stream
* @note        This function allocates the memory to dataAddress filed and copied the data.
*            -The Application should not free the dataAddress pointer.
* @param    Context(IN/OUT)    : AMR Reader context
* @param    StreamID(IN)    : Selects the stream
* @param    pAu(IN/OUT)        : Access Unit
*
* @returns    M4NO_ERROR        : There is no error
* @returns     M4ERR_PARAMETER    : atleast one parament is NULL
* @returns     M4ERR_BAD_CONTEXT    :    The provided context is not valid
* @returns     M4ERR_ALLOC        : Memory allocation failed
* @returns     M4ERR_STATE        : this function cannot be called in this state.
* @returns     M4ERR_BAD_STREAM_ID    : Atleast one of the stream Id. does not exist.
* @returns     M4WAR_NO_DATA_YET    : there    is no enough data on the stream for new access unit
* @returns     M4WAR_END_OF_STREAM    : There are no more access unit in the stream
* @returns     M4AMRR_ERR_INVALID_FRAME_TYPE : current frame has no valid frame type.
******************************************************************************
*/
M4OSA_ERR M4AMRR_nextAU(M4OSA_Context Context, M4SYS_StreamID StreamID, M4SYS_AccessUnit* pAu);

/**
******************************************************************************
* M4OSA_ERR M4AMRR_freeAU(M4OSA_Context Context, M4SYS_StreamID StreamID, M4SYS_AccessUnit* pAu);
* @brief    Notify the ARM Reader that application will no longer use "AU"
* @note        This function frees the memory pointed by pAu->dataAddress pointer
*            -Changes the state of the reader back to reading.
* @param    Context(IN/OUT)    : AMR Reader context
* @param    StreamID(IN)    : Selects the stream
* @param    pAu(IN)            : Access Unit
*
* @returns     M4NO_ERROR        : There is no error
* @returns     M4ERR_PARAMETER    : atleast one parament is NULL
* @returns     M4ERR_BAD_CONTEXT    :    The provided context is not valid
* @returns     M4ERR_ALLOC        : Memory allocation failed
* @returns     M4ERR_STATE        : this function cannot be called in this state.
* @returns     M4ERR_BAD_STREAM_ID    : Atleast one of the stream Id. does not exist.
******************************************************************************
*/
M4OSA_ERR M4AMRR_freeAU(M4OSA_Context Context, M4SYS_StreamID StreamID, M4SYS_AccessUnit* pAu);

/**
******************************************************************************
* M4OSA_ERR M4AMRR_seek(M4OSA_Context Context, M4SYS_StreamID* pStreamID, M4OSA_Time time,
*                        M4SYS_seekAccessMode    seekMode, M4OSA_Time* pObtainCTS);
* @brief    The function seeks the targeted time in the give stream by streamId.
* @note        Each frame is of 20 ms duration,, builds the seek table and points
*            the file pointer to starting for the required AU.
* @param    Context(IN/OUT)    : AMR Reader context
* @param    StreamID(IN)    : Array of stream IDs.
* @param    time(IN)        : targeted time
* @param    seekMode(IN)    : Selects the seek mode
* @param    pObtainCTS(OUT)    : Returned time nearest to target.
*
* @returns     M4NO_ERROR        : There is no error
* @returns     M4ERR_PARAMETER    : atleast one parament is NULL
* @returns     M4ERR_BAD_CONTEXT    :    The provided context is not valid
* @returns     M4ERR_ALLOC        : Memory allocation failed
* @returns     M4ERR_STATE        : this function cannot be called in this state.
* @returns     M4ERR_BAD_STREAM_ID    : Atleast one of the stream Id. does not exist.
* @returns     M4WAR_INVALID_TIME    : time cannot be reached.
******************************************************************************
*/
M4OSA_ERR M4AMRR_seek(M4OSA_Context Context, M4SYS_StreamID* pStreamID, M4OSA_Time time,
                         M4SYS_SeekAccessMode    seekMode, M4OSA_Time* pObtainCTS);

/**
******************************************************************************
* M4OSA_ERR M4AMRR_closeRead(M4OSA_Context Context);
* @brief    AMR reader closes the file
* @param    Context(IN?OUT)    : AMR Reader context
* @returns     M4NO_ERROR        : There is no error
* @returns     M4ERR_PARAMETER    : atleast one parament is NULL
* @returns     M4ERR_BAD_CONTEXT    :    The provided context is not valid
* @returns     M4ERR_ALLOC        : Memory allocation failed
* @returns     M4ERR_STATE        : this function cannot be called in this state.
******************************************************************************
*/
M4OSA_ERR M4AMRR_closeRead(M4OSA_Context Context);

/**
******************************************************************************
* M4OSA_ERR M4AMRR_getState(M4OSA_Context Context, M4AMRR_State* pState, M4SYS_StreamID streamId);
* @brief    Gets the current state of the AMR reader
* @param    Context(IN/OUT)    : AMR Reader context
* @param    pState(OUT)        : Core AMR reader state
* @param    streamId(IN)    : Selects the stream 0 for all
*
* @returns     M4NO_ERROR            :    There is no error
* @returns     M4ERR_PARAMETER        :    atleast one parament is NULL
* @returns     M4ERR_BAD_CONTEXT    :    The provided context is not valid
* @returns     M4ERR_BAD_STREAM_ID    :    Atleast one of the stream Id. does not exist.
******************************************************************************
*/
M4OSA_ERR M4AMRR_getState(M4OSA_Context Context, M4AMRR_State* pState, M4SYS_StreamID streamId);


/**
 ******************************************************************************
 * M4OSA_ERR M4AMRR_getVersion    (M4_VersionInfo *pVersion)
 * @brief    Gets the current version of the AMR reader
 * @param    version(OUT)    : the structure that stores the version numbers
 *
 * @returns     M4NO_ERROR            :    There is no error
 * @returns     M4ERR_PARAMETER        :    version is NULL
 ******************************************************************************
*/
M4OSA_ERR M4AMRR_getVersion    (M4_VersionInfo *pVersion);

/**
 ******************************************************************************
 * M4OSA_ERR M4AMRR_getmaxAUsize    (M4OSA_Context Context, M4OSA_UInt32 *pMaxAuSize)
 * @brief    Computes the maximum access unit size of a stream
 *
 * @param    Context        (IN)  Context of the reader
 * @param    pMaxAuSize    (OUT) Maximum Access Unit size in the stream
 *
 * @return    M4NO_ERROR: No error
 * @return    M4ERR_PARAMETER: One of the input pointer is M4OSA_NULL (Debug only)
 ******************************************************************************
*/
M4OSA_ERR M4AMRR_getmaxAUsize(M4OSA_Context Context, M4OSA_UInt32 *pMaxAuSize);


#ifdef __cplusplus
}
#endif /* __cplusplus*/
#endif /*__M4AMR_COREREADER_H__*/

