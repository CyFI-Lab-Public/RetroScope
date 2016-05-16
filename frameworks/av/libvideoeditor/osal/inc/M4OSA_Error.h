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
 * @file         M4OSA_Error.h
 * @ingroup      OSAL
 * @brief        Definition of common error types
 * @note         This file contains macros to generate and analyze error codes.
 ************************************************************************
*/


#ifndef M4OSA_ERROR_H
#define M4OSA_ERROR_H

#include "M4OSA_Types.h"

/** M4OSA_ERR is a 32 bits unsigned integer.
 * To sort returned code, a specific naming convention must be followed:
 * - Severity (2 bits): It may br either 0b00 (no error), 0b01 (warning) or
 *                      0b01 (fatal error)
 * - Core ID (14 bits): It is a unique ID for each core component
 * - ErrorID (16 bits): It is the specific error code

 * EACH CORE COMPONENT FUNCTION SHOULD RETURN AN M4OSA_ERR
*/
typedef M4OSA_UInt32   M4OSA_ERR;

#define M4_OK     0
#define M4_WAR    1
#define M4_ERR    2


/* Macro to process M4OSA_ERR */

/** This macro tests if the provided M4OSA_ERR is a warning or not*/
#define M4OSA_ERR_IS_WARNING(error)   ((((error)>>30) == M4_WAR) ? 1:0)

/** This macro tests if the provided M4OSA_ERR is a fatal error or not*/
#define M4OSA_ERR_IS_ERROR(error)   ((((error)>>30) == M4_ERR) ? 1:0)

/** This macro returns an error code accroding to the 3 provided fields:
  * @arg severity: (IN) [M4OSA_UInt32] Severity to put in the error code
  * @arg coreID: (IN) [M4OSA_UInt32] CoreID to put in the error code
  * @arg errorID: (IN) [M4OSA_UInt32] ErrorID to put in the error code*/
#define M4OSA_ERR_CREATE(severity, coreID, errorID)\
   (M4OSA_Int32)((((M4OSA_UInt32)severity)<<30)+((((M4OSA_UInt32)coreID)&0x003FFF)<<16)+(((M4OSA_UInt32)errorID)&0x00FFFF))

/** This macro extracts the 3 fields from the error:
  * @arg error: (IN) [M4OSA_ERR] Error code
  * @arg severity: (OUT) [M4OSA_UInt32] Severity to put in the error code
  * @arg coreID: (OUT) [M4OSA_UInt32] CoreID to put in the error code
  * @arg errorID: (OUT) [M4OSA_UInt32] ErrorID to put in the error code*/
#define M4OSA_ERR_SPLIT(error, severity, coreID, errorID)\
   { severity=(M4OSA_UInt32)((error)>>30);\
     coreID=(M4OSA_UInt32)(((error)>>16)&0x003FFF);\
     (M4OSA_UInt32)(errorID=(error)&0x00FFFF); }


/* "fake" CoreID, is used to report an unknown CoreID. Used by the trace system
when the core ID macro isn't defined. Defined here instead of CoreID.h to avoid
introducing dependencies to common/inc. */

#define M4UNKNOWN_COREID    0x3FFF /* max possible CoreID */

#define M4_COMMON           0x00  /**<Common*/
#define M4MP4_COMMON        0x01  /**<Core MP4 (common)*/
#define M4MP4_WRITER        0x02  /**<Core MP4 writer*/
#define M4MP4_READER        0x03  /**<Core MP4 reader*/
#define M4RTSP_COMMON       0x11  /**<Core RTSP common*/
#define M4RTSP_WRITER       0x12  /**<Core RTSP transmitter*/
#define M4RTSP_READER       0x13  /**<Core RTSP receiver*/
#define M4RTP_WRITER        0x14  /**<Core RTP/RTCP receiver*/
#define M4RTP_READER        0x15  /**<Core RTP/RTCP transmitter*/
#define M4SAP_WRITER        0x16  /**<Core SAP transmitter*/
#define M4SAP_READER        0x17  /**<Core SAP receiver*/
#define M4DVBH_READER        0x18  /**<Core DVBH receiver*/
#define M4SDP_WRITER        0x22  /**<Core SDP writer*/
#define M4SDP_READER        0x31  /**<Core SDP reader*/
#define M4PAK_AMR           0x32  /**<Core packetizer AMR (RFC3267)*/
#define M4DEPAK_AMR         0x33  /**<Core de-packetizer AMR (RFC3267)*/
#define M4PAK_H263          0x34  /**<Core packetizer H263 (RFC2429)*/
#define M4DEPAK_H263        0x35  /**<Core de-packetizer H263(RFC2429)*/
#define M4PAK_SIMPLE        0x36  /**<Core packetizer SimpleDraft (RFC xxxx)*/
#define M4DEPAK_SIMPLE      0x37  /**<Core de-packetizer SimpleDraft (RFC xxxx)*/
#define M4PAK_3016_VIDEO    0x38  /**<Core packetizer RFC3016 video*/
#define M4DEPAK_3016_VIDEO  0x39  /**<Core de-packetizer RFC3016 video*/
#define M4PAK_3016_AUDIO    0x3A  /**<Core packetizer RFC3016 audio (LATM)*/
#define M4DEPAK_3016_AUDIO  0x3B  /**<Core de-packetizer RFC3016 audio (LATM)*/
#define M4DEPAK_H264        0x3C  /**<Core de-packetizer H264*/
#define M4DEPAK_REALV        0x3D  /**<Core de-packetizer Real Video */
#define M4DEPAK_REALA        0x3E  /**<Core de-packetizer Real Audio */
#define M4RDT_READER        0x3F  /**<Core RDT receiver*/
#define M4TCP_DMUX          0x50  /**<Core TCP demux*/
#define M4IOD_PARSER        0x51  /**<Core IOD parser*/
#define M4OSA_FILE_COMMON   0x61  /**<OSAL file common*/
#define M4OSA_FILE_WRITER   0x62  /**<OSAL file writer*/
#define M4OSA_FILE_READER   0x63  /**<OSAL file reader*/
#define M4OSA_FILE_EXTRA    0x64  /**<OSAL file extra*/
#define M4OSA_DIRECTORY     0x65  /**<OSAL directory*/
#define M4OSA_SOCKET        0x71  /**<OSAL socket (both reader and writer)*/
#define M4OSA_THREAD        0x81  /**<OSAL thread*/
#define M4OSA_MUTEX         0x82  /**<OSAL mutex*/
#define M4OSA_SEMAPHORE     0x83  /**<OSAL semaphore*/
#define M4OSA_CLOCK         0x84  /**<OSAL clock*/
#define M4OSA_MEMORY        0x91  /**<OSAL memory*/
#define M4CALL_BACK         0xA1  /**<Call Back error*/
#define M4OSA_URI           0xB1  /**<OSAL URI handler*/
#define M4OSA_STRING        0xB2  /**<OSAL string*/
#define M4SYS_CMAPI         0xB3  /**<SYSTEM Common Medi API*/
#define M4OSA_CHARSTAR      0xB4  /**<OSAL CharStar*/
#define M4REACTOR           0xC1  /**<Core reactor*/
#define M4TEST              0xD1  /**<Test component*/
#define M4STACK                0xE1  /**< Core ID of the integrated stack*/
#define M4STACK_REAL        0xE2  /**<Core ID of the Real integrated stack */
#define M4TOOL_LBVT_PARAM   0xF1  /**<LB_VT config file manager*/
#define M4TOOL_LINK_LIST    0xF2  /**<Tool linked list*/
#define M4TOOL_BASE64       0xF3  /**<Core base64 encoder/decoder*/



/* Definition of common error codes */
/** there is no error*/
#define M4NO_ERROR            0x00000000

/** At least one parameter is NULL*/
#define M4ERR_PARAMETER            M4OSA_ERR_CREATE(M4_ERR,M4_COMMON,0x000001)
/** This function cannot be called now*/
#define M4ERR_STATE                M4OSA_ERR_CREATE(M4_ERR,M4_COMMON,0x000002)
/** There is no more memory available*/
#define M4ERR_ALLOC                M4OSA_ERR_CREATE(M4_ERR,M4_COMMON,0x000003)
/** Provided context is not a valid one*/
#define M4ERR_BAD_CONTEXT          M4OSA_ERR_CREATE(M4_ERR,M4_COMMON,0x000004)
#define M4ERR_CONTEXT_FAILED       M4OSA_ERR_CREATE(M4_ERR,M4_COMMON,0x000005)
#define M4ERR_BAD_STREAM_ID        M4OSA_ERR_CREATE(M4_ERR,M4_COMMON,0x000006)
/** The optionID is not a valid one*/
#define M4ERR_BAD_OPTION_ID        M4OSA_ERR_CREATE(M4_ERR,M4_COMMON,0x000007)
/** This option is a write only one*/
#define M4ERR_WRITE_ONLY           M4OSA_ERR_CREATE(M4_ERR,M4_COMMON,0x000008)
/** This option is a read only one*/
#define M4ERR_READ_ONLY            M4OSA_ERR_CREATE(M4_ERR,M4_COMMON,0x000009)
/** This function is not supported yet*/
#define M4ERR_NOT_IMPLEMENTED      M4OSA_ERR_CREATE(M4_ERR,M4_COMMON,0x00000A)

#define    M4ERR_UNSUPPORTED_MEDIA_TYPE  M4OSA_ERR_CREATE(M4_ERR, M4_COMMON, 0x00000B)

#define M4WAR_NO_DATA_YET          M4OSA_ERR_CREATE(M4_WAR,M4_COMMON,0x000001)
#define M4WAR_NO_MORE_STREAM       M4OSA_ERR_CREATE(M4_WAR,M4_COMMON,0x000002)
#define M4WAR_INVALID_TIME         M4OSA_ERR_CREATE(M4_WAR,M4_COMMON,0x000003)
#define M4WAR_NO_MORE_AU           M4OSA_ERR_CREATE(M4_WAR,M4_COMMON,0x000004)
#define M4WAR_TIME_OUT             M4OSA_ERR_CREATE(M4_WAR,M4_COMMON,0x000005)
/** The buffer is full*/
#define M4WAR_BUFFER_FULL          M4OSA_ERR_CREATE(M4_WAR,M4_COMMON,0x000006)
/* The server asks for a redirection */
#define M4WAR_REDIRECT               M4OSA_ERR_CREATE(M4_WAR,M4_COMMON,0x000007)
#define M4WAR_TOO_MUCH_STREAMS     M4OSA_ERR_CREATE(M4_WAR,M4_COMMON,0x000008)
/* SF Codec detected INFO_FORMAT_CHANGE during decode */
#define M4WAR_INFO_FORMAT_CHANGE M4OSA_ERR_CREATE(M4_WAR, M4_COMMON, 0x000009)

#endif /*M4OSA_ERROR_H*/

