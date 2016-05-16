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
 * @file    M4MP4W_Writer.h
 * @brief   Core MP4 writer interface
 * @note    This file declares the MP4 writer interface functions.
 *          The MP4 writer specific types are defined in file M4MP4W_Types.h
 ******************************************************************************
 */
#ifndef M4MP4W_WRITER_H
#define M4MP4W_WRITER_H

#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */

#include "NXPSW_CompilerSwitches.h"

#ifndef _M4MP4W_USE_CST_MEMORY_WRITER

/* includes */
#include "M4OSA_Types.h"
#include "M4OSA_Error.h"
#include "M4OSA_FileWriter.h"
#include "M4OSA_FileReader.h"
#include "M4SYS_AccessUnit.h"
#include "M4MP4W_Types.h"

/**
 ******************************************************************************
 * MP4W Errors & Warnings definition
 ******************************************************************************
 */
#define M4WAR_MP4W_OVERSIZE         M4OSA_ERR_CREATE(M4_WAR, M4MP4_WRITER ,0x000001)
#define M4WAR_MP4W_NOT_EVALUABLE    M4OSA_ERR_CREATE(M4_WAR, M4MP4_WRITER ,0x000002)

/**
 ******************************************************************************
 * @brief    Get MP4W version
 * @param    major            (OUT) Pointer to the 'major' version number.
 * @param    minor            (OUT) Pointer to the 'minor' version number.
 * @param    revision         (OUT) Pointer to the 'revision' number.
 * @return   M4NO_ERROR:         No error
 * @return   M4ERR_PARAMETER:    At least one parameter is null
 ******************************************************************************
 */
M4OSA_ERR M4MP4W_getVersion(M4OSA_UInt8* major,
                            M4OSA_UInt8* minor,
                            M4OSA_UInt8* revision);

/**
 ******************************************************************************
 * @brief    Initiation of the MP4 file creation
 * @param    contextPtr             (OUT) Pointer to the MP4 writer context to create.
 * @param    outputFileDescriptor   (IN)  Descriptor of the output file to open.
 * @param    fileWriterFunction     (IN)  Pointer to structure containing the set of
 *                                          OSAL file write functions.
 * @param    tempFileDescriptor     (IN)  Descriptor of the temporary file to open.
 * @param    fileReaderFunction     (IN)  Pointer to structure containing the set of
 *                                          OSAL file read functions.
 * @return    M4NO_ERROR:         No error
 * @return    M4ERR_PARAMETER:    At least one parameter is null or incorrect
 * @return    M4ERR_ALLOC:        Memory allocation failed
 ******************************************************************************
 */
M4OSA_ERR M4MP4W_openWrite( M4OSA_Context*                  contextPtr,
                            void*                           outputFileDescriptor,
                            M4OSA_FileWriterPointer*        fileWriterFunction,
                            void*                           tempFileDescriptor,
                            M4OSA_FileReadPointer*          fileReaderFunction );

/**
 ******************************************************************************
 * @brief    Add a new track
 * @param    context              (IN/OUT)  MP4 writer context.
 * @param    streamDescPtr        (IN)      Pointer to the structure containing the
                                            parameters for the new track.
 * @return    M4NO_ERROR:         No error
 * @return    M4ERR_PARAMETER:    At least one parameter is null or incorrect
 * @return    M4ERR_ALLOC:        Memory allocation failed
 * @return    M4ERR_STATE:        Invalid state
 * @return    M4ERR_BAD_CONTEXT:  An audio (resp.video) stream has already been added
 *                                to this context while attempting to add another one,
 *                                which is forbidden.
 ******************************************************************************
 */
M4OSA_ERR M4MP4W_addStream( M4OSA_Context                context,
                            M4SYS_StreamDescription*     streamDescPtr);

/**
 ******************************************************************************
 * @brief   Signal to the core MP4 writer that there is no more tracks to add
 * @param   context             (IN/OUT) MP4 writer context.
 * @return  M4NO_ERROR:         No error
 * @return  M4ERR_PARAMETER:    At least one parameter is null or incorrect
 * @return  M4ERR_ALLOC:        Memory allocation failed
 * @return  M4ERR_STATE:        Invalid state
 * @return  M4ERR_BAD_CONTEXT:  Audio size estimation is required but not two streams
 *                              have been added.
 ******************************************************************************
 */
M4OSA_ERR M4MP4W_startWriting( M4OSA_Context context);

/**
 ******************************************************************************
 * @brief   Asks the core MP4 writer to initiate the access unit creation in
 *          the streamID track
 * @param   context             (IN/OUT) MP4 writer context.
 * @param   streamID            (IN) Stream ID of the track.
 * @param   auPtr               (IN/OUT) Access unit.
 * @return    M4NO_ERROR:         No error
 * @return    M4ERR_PARAMETER:    At least one parameter is null or incorrect
 * @return    M4ERR_BAD_STREAM_ID:Unknown stream ID
 * @return    M4ERR_ALLOC:        Memory allocation failed
 * @return    M4ERR_STATE:        Invalid state
 ******************************************************************************
 */
M4OSA_ERR M4MP4W_startAU( M4OSA_Context        context,
                          M4SYS_StreamID       streamID,
                          M4SYS_AccessUnit*    auPtr);

/**
 ******************************************************************************
 * @brief   Ask the core MP4 writer to write the access unit in the streamID track
 * @note    If M4MP4W_WAR_OVERSIZE is returned, M4MP4W_startAU must not be called anymore,
 *          but directly M4MP4W_closeWrite().
 * @param   context             (IN/OUT)   MP4 writer context.
 * @param   streamID            (IN)       Stream ID of the track.
 * @param   auPtr               (IN/OUT)   Access unit.
 * @return    M4NO_ERROR:                 No error
 * @return    M4ERR_PARAMETER:            At least one parameter is null or incorrect
 * @return    M4ERR_BAD_STREAM_ID:        Unknown stream ID
 * @return    M4ERR_ALLOC:                Memory allocation failed
 * @return    M4ERR_STATE:                Invalid state
 * @return    M4WAR_MP4W_NOT_EVALUABLE:   It is not possible to evaluate audio size if audio
 *                                        samples don't have a constant size.
 * @return    M4WAR_MP4W_OVERSIZE:        Max file size was reached
 ******************************************************************************
 */
M4OSA_ERR M4MP4W_processAU( M4OSA_Context        context,
                            M4SYS_StreamID       streamID,
                            M4SYS_AccessUnit*    auPtr);

/**
 ******************************************************************************
 * @brief     Close the MP4 file
 * @note      In previous versions of the MP4 writer, the M4MP4W_freeContext method
 *            was in the interface, which is not the case anymore.
 *            The context is now always deallocated in the M4MP4W_closeWrite function.
 * @param     context             (IN/OUT) MP4 writer context.
 * @return    M4NO_ERROR:         No error
 * @return    M4ERR_PARAMETER:    At least one parameter is null or incorrect
 ******************************************************************************
 */
M4OSA_ERR M4MP4W_closeWrite( M4OSA_Context context);

/**
 ******************************************************************************
 * @brief    Ask the core MP4 writer to return the value associated with the optionID
 * @param    context                (IN)    MP4 writer context.
 * @param    option                 (IN)    Option ID.
 * @param    valuePtr               (OUT)   Pointer to the option value.
 * @return    M4NO_ERROR:             No error
 * @return    M4ERR_PARAMETER:        At least one parameter is null or incorrect
 * @return    M4ERR_NOT_IMPLEMENTED:  Not implemented in the current version
 * @return    M4ERR_BAD_OPTION_ID:    Unknown optionID
 * @return    M4ERR_BAD_STREAM_ID:    Bad stream ID in the option value
 ******************************************************************************
 */
M4OSA_ERR M4MP4W_getOption( M4OSA_Context        context,
                            M4OSA_OptionID        option,
                            M4OSA_DataOption    *valuePtr);

/**
 ******************************************************************************
 * @brief    Ask the core MP4 writer to set the value associated with the optionID.
 * @param    context              (IN/OUT)  MP4 writer context.
 * @param    option               (IN)      Option ID.
 * @param    value                (IN)      Option value.
 * @return    M4NO_ERROR:             No error
 * @return    M4ERR_PARAMETER:        At least one parameter is null or incorrect
 * @return    M4ERR_NOT_IMPLEMENTED:  Not implemented in the current version
 * @return    M4ERR_BAD_OPTION_ID:    Unknown optionID
 * @return    M4ERR_BAD_STREAM_ID:    Bad stream ID in the option value
 * @return    M4ERR_ALLOC:            A memory allocation failed
 ******************************************************************************
 */
M4OSA_ERR M4MP4W_setOption( M4OSA_Context       context,
                            M4OSA_OptionID      option,
                            M4OSA_DataOption    value);

/**
 ******************************************************************************
 * @brief    Ask the core MP4 writer to return its state.
 * @note     By selecting a specific streamID (not null), the caller can obtain
 *           the state of a specific stream. By using 0 as streamID the returned
 *           state is not stream specific.
 * @param    context                (IN/OUT) MP4 writer context.
 * @param    context                (IN)     Pointer to the state enumeration.
 * @param    context                (IN/OUT) streamID of the stream to retrieve the
 *                                           micro-state (0 for global state).
 * @return    M4NO_ERROR:             No error
 * @return    M4ERR_BAD_STREAM_ID:    Unknown stream ID
 * @return    M4ERR_PARAMETER:        At least one parameter is null or incorrect
 ******************************************************************************
 */
M4OSA_ERR M4MP4W_getState( M4OSA_Context    context,
                           M4MP4W_State*    statePtr,
                           M4SYS_StreamID   streamID);

/**
 ******************************************************************************
 * @brief    Get the currently expected file size
 * @param    context             (IN/OUT) MP4 writer context.
 * @return   M4NO_ERROR:         No error
 * @return   M4ERR_PARAMETER:    At least one parameter is null
 ******************************************************************************
 */
M4OSA_ERR M4MP4W_getCurrentFileSize( M4OSA_Context        context,
                                     M4OSA_UInt32*        currentFileSize);

#endif /* _M4MP4W_USE_CST_MEMORY_WRITER */

#ifdef __cplusplus
}
#endif /* __cplusplus */


#endif /*M4MP4W_WRITER_H*/

