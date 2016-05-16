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
 * @file    M4WAV_WavReader.h
 * @brief   WAV Reader declarations
 * @note    This file implements functions of the WAV reader
 ************************************************************************
*/

#include "M4OSA_CoreID.h"
#include "M4OSA_Types.h"
#include "M4OSA_Memory.h"
#include "M4OSA_FileReader.h"
#include "M4SYS_AccessUnit.h"
#include "M4TOOL_VersionInfo.h"


#define M4PCMC_ERR_PCM_NOT_COMPLIANT    M4OSA_ERR_CREATE(M4_ERR, M4WAV_COMMON,0x000001)
#define M4PCMC_ERR_PCM_NO_SPACE_AVAIL   M4OSA_ERR_CREATE(M4_ERR, M4WAV_COMMON,0x000002)
#define M4PCMC_ERR_PCM_NOT_SUPPORTED    M4OSA_ERR_CREATE(M4_ERR, M4WAV_COMMON,0x000003)

#define M4PCMC_WAR_END_OF_STREAM        M4OSA_ERR_CREATE(M4_WAR, M4WAV_COMMON ,0x000001)

/**
 ************************************************************************
 * structure    M4WAVC_DecoderSpecificInfo
 * @brief       This structure defines the decoder Specific informations
 * @note        This structure is used by the WAV reader to store all
 *              decoder specific informations:
 *              - Sample Frequency
 *              - Average Bytes per second
 *              - Number of channels (1 or 2)
 *              - Number of bits per sample (8 or 16)
 ************************************************************************
*/
typedef struct {
    M4OSA_UInt32    SampleFrequency;
    M4OSA_UInt32    AvgBytesPerSec;
    M4OSA_UInt32    DataLength;
    M4OSA_UInt16    nbChannels;
    M4OSA_UInt16    BitsPerSample;
} M4PCMC_DecoderSpecificInfo;

/**
 ************************************************************************
 * enum     M4WAVR_State
 * @brief   This enum defines the WAV Reader States
 * @note    The state automaton is documented separately
 *          consult the design specification for details
 ************************************************************************
*/
typedef enum {
    M4PCMR_kInit    = 0x0000,
    M4PCMR_kOpening = 0x0100,
    M4PCMR_kOpening_streamRetrieved = 0x0101,
    M4PCMR_kReading = 0x0200,
    M4PCMR_kReading_nextAU  = 0x0201,
    M4PCMR_kClosed  = 0x0300
} M4PCMR_State;

/**
 ************************************************************************
 * enum     M4WAVR_OptionID
 * @brief   This enum defines the WAV Reader options
 * @note    Only one option is available:
 *          - M4WAVR_kPCMblockSize: sets the size of the PCM block to read
 *            from WAV file
 ************************************************************************
*/
typedef enum {
    M4PCMR_kPCMblockSize    = M4OSA_OPTION_ID_CREATE(M4_READ, M4WAV_READER, 0x01)
} M4PCMR_OptionID;

/**
 ************************************************************************
 * structure    M4WAVR_Context
 * @brief       This structure defines the WAV Reader context
 * @note        This structure is used for all WAV Reader calls to store
 *              the context
 ************************************************************************
*/
typedef struct {
    M4OSA_MemAddr32             m_pDecoderSpecInfo;/**< Pointer to the decoder specific info
                                                        structure contained in pStreamDesc
                                                        (only used to free...) */
    M4OSA_FileReadPointer*      m_pFileReadFunc;/**< The OSAL set of pointer to function for
                                                         file management */
    M4OSA_Context               m_fileContext;  /**< The context needed by OSAL to manage File */
    M4PCMC_DecoderSpecificInfo  m_decoderConfig;/**< Specific configuration for decoder */
    M4PCMR_State                m_state;        /**< state of the wav reader */
    M4PCMR_State                m_microState;   /**< state of the read wav stream */
    M4OSA_UInt32                m_blockSize;    /**< Size of the read block */
    M4OSA_UInt32                m_offset;       /**< Offset of the PCM read (i.e m_offset of the
                                                        file without wav header) */
    M4OSA_MemAddr32             m_pAuBuffer;    /**< Re-used buffer for AU content storage */
    M4OSA_FilePosition          m_dataStartOffset;/**< offset of the pcm data beginning into
                                                         the file */
} M4PCMR_Context;

/*************************************************************************
 *
 *  Prototypes of all WAV reader functions
 *
 ************************************************************************/
M4OSA_ERR M4PCMR_openRead(M4OSA_Context* pContext, M4OSA_Void* pUrl,
                             M4OSA_FileReadPointer* pFileFunction);
M4OSA_ERR M4PCMR_getNextStream(M4OSA_Context context, M4SYS_StreamDescription* pStreamDesc);
M4OSA_ERR M4PCMR_startReading(M4OSA_Context context, M4SYS_StreamID* pStreamIDs);
M4OSA_ERR M4PCMR_nextAU(M4OSA_Context context, M4SYS_StreamID streamID, M4SYS_AccessUnit* pAU);
M4OSA_ERR M4PCMR_freeAU(M4OSA_Context context, M4SYS_StreamID streamID, M4SYS_AccessUnit* pAU);
M4OSA_ERR M4PCMR_seek(M4OSA_Context context, M4SYS_StreamID* pStreamID, M4OSA_Time time,
                         M4SYS_SeekAccessMode seekAccessMode, M4OSA_Time* pObtainCTS);
M4OSA_ERR M4PCMR_closeRead(M4OSA_Context context);
M4OSA_ERR M4PCMR_getOption(M4OSA_Context context, M4PCMR_OptionID optionID,
                              M4OSA_DataOption* pValue);
M4OSA_ERR M4PCMR_setOption(M4OSA_Context context, M4PCMR_OptionID optionID,
                              M4OSA_DataOption Value);
M4OSA_ERR M4PCMR_getVersion(M4_VersionInfo *pVersion);
