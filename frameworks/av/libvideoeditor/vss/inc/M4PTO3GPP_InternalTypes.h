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
 * @file    M4PTO3GPP_InternalTypes.h
 * @brief    Picture to 3gpp Service internal definitions
 * @note    This file contains all enum and types not visible to the external world.
 ******************************************************************************
 */


#ifndef __M4PTO3GPP_INTERNALTYPES_H__
#define __M4PTO3GPP_INTERNALTYPES_H__

#define M4PTO3GPP_VERSION_MAJOR        3
#define M4PTO3GPP_VERSION_MINOR        0
#define M4PTO3GPP_VERSION_REVISION    6

/**
 *    M4PTO3GPP public API and types */
#include "M4PTO3GPP_API.h"
#include "M4_Utils.h"

/**
 *    Internally used modules */

#include "M4WRITER_common.h"    /* Write 3GPP file    */
#include "M4READER_Common.h"    /* Read AMR file    */
#include "M4ENCODER_common.h"


#ifdef __cplusplus
extern "C" {
#endif

/**
 ******************************************************************************
 * enum            M4PTO3GPP_States
 * @brief        Main state machine of the M4PTO3GPP.
 ******************************************************************************
 */
typedef enum
{
    M4PTO3GPP_kState_CREATED         = 0,    /**< M4PTO3GPP_Init has been called */
    M4PTO3GPP_kState_OPENED          = 1,    /**< M4PTO3GPP_Open has been called */
    M4PTO3GPP_kState_READY           = 2,    /**< Step can be called */
    M4PTO3GPP_kState_FINISHED        = 3,    /**< Transcoding is finished */
    M4PTO3GPP_kState_CLOSED          = 4     /**< Output file has been created */
}
M4PTO3GPP_States;

/**
 ******************************************************************************
 * enum            M4PTO3GPP_StreamState
 * @brief        State of a media stream encoding (audio or video).
 ******************************************************************************
 */
typedef enum
{
    M4PTO3GPP_kStreamState_NOSTREAM  = 0,    /**< No stream present */
    M4PTO3GPP_kStreamState_STARTED   = 1,    /**< The stream encoding is in progress */
    M4PTO3GPP_kStreamState_FINISHED  = 2    /**< The stream has finished encoding */
}
M4PTO3GPP_StreamState;

/*
 * Definition of max AU size */
#define M4PTO3GPP_VIDEO_MIN_COMPRESSION_RATIO     0.8F    /**< Max AU size will be 0.8 times the
                                                               YUV4:2:0 frame size */
#define M4PTO3GPP_VIDEO_AU_SIZE_TO_CHUNCK_SIZE_RATIO    1.2F /**< Max chunk size will be 1.2 times
                                                                  the max AU size */
#define M4PTO3GPP_AUDIO_MAX_AU_SIZE              1000    /**< AAC max AU size seems to be
                                                              about 850 bytes */
#define M4PTO3GPP_AUDIO_MAX_CHUNK_SIZE           5000

/**
 ******************************************************************************
 * enum            anonymous enum
 * @brief        enum to keep track of the encoder state
 ******************************************************************************
 */
enum
{
    M4PTO3GPP_kNoEncoder,
    M4PTO3GPP_kEncoderClosed,
    M4PTO3GPP_kEncoderStopped,
    M4PTO3GPP_kEncoderRunning
};

/**
 ******************************************************************************
 * structure    M4PTO3GPP_InternalContext
 * @brief        This structure defines the M4PTO3GPP context (private)
 * @note        This structure is used for all M4PTO3GPP calls to store the context
 ******************************************************************************
 */
typedef struct
{
    /**
     *    M4PTO3GPP main variables */
    M4PTO3GPP_States             m_State;            /**< M4PTO3GPP internal state */
    M4PTO3GPP_Params             m_Params;           /**< M4PTO3GPP parameters, set by the user */
    M4PTO3GPP_StreamState        m_VideoState;       /**< State of the video encoding */
    M4PTO3GPP_StreamState        m_AudioState;       /**< State of the audio encoding */

    /**
     *    OSAL file read/write functions */
    M4OSA_FileReadPointer*        pOsalFileRead;     /**< OSAL file read functions,
                                                           to be provided by user */
    M4OSA_FileWriterPointer*      pOsalFileWrite;    /**< OSAL file write functions,
                                                          to be provided by user */

    /**
     *    Reader stuff */
    M4_AccessUnit*                m_pReaderAudioAU;    /**< Read audio access unit */
    M4_AudioStreamHandler*        m_pReaderAudioStream;/**< Description of the read audio stream */

    /**
     *    Writer stuff */
    M4SYS_AccessUnit            m_WriterVideoAU;       /**< Written video access unit */
    M4SYS_AccessUnit            m_WriterAudioAU;       /**< Written audio access unit */
    M4ENCODER_Header*           m_pEncoderHeader;      /**< Sequence header returned by the
                                                            encoder at encoder create (if any) */
    M4SYS_StreamDescription*    m_pWriterVideoStream;  /**< Description of the written
                                                             video stream */
    M4SYS_StreamDescription*    m_pWriterAudioStream;  /**< Description of the written
                                                             audio stream */
    M4WRITER_StreamVideoInfos*  m_pWriterVideoStreamInfo;    /**< Video properties of the written
                                                               video stream */
    M4WRITER_StreamAudioInfos*    m_pWriterAudioStreamInfo;   /**< Audio properties of the written
                                                               audio stream */

    /**
     *    Contexts of the used modules  */
    M4OSA_Void*                    m_pAudioReaderContext; /**< Context of the audio reader module*/
    M4OSA_Void*                    m_p3gpWriterContext;   /**< Context of the 3GP writer module */
    M4OSA_Void*                    m_pMp4EncoderContext;  /**< Mp4 encoder context */
    M4OSA_UInt32                   m_eEncoderState;

    /**
     * Reader Interfaces */
    M4READER_GlobalInterface*    m_pReaderGlobInt;    /**< Reader common interface, global part */
    M4READER_DataInterface*      m_pReaderDataInt;     /**< Reader common interface, data part */

    /**
     * Writer Interfaces */
    M4WRITER_GlobalInterface*   m_pWriterGlobInt;     /**< Writer common interface, global part */
    M4WRITER_DataInterface*     m_pWriterDataInt;     /**< Writer common interface, data part */

    /**
     * Encoder Interfaces */
    M4ENCODER_GlobalInterface*  m_pEncoderInt;                /**< Encoder common interface */
    M4OSA_Void*                 m_pEncoderExternalAPI;
    M4OSA_Void*                 m_pEncoderUserData;

    /**
     * */
    M4VIFI_ImagePlane*            pSavedPlane;
    M4OSA_UInt32                  uiSavedDuration;

    /**
     *    Video rate control stuff */
    M4_MediaTime                m_dLastVideoRegulCts; /**< Last time (CTS) the video bitrate
                                                           regulation has been called */
    M4_MediaTime                m_mtCts;         /**< Current video cts */
    M4_MediaTime                m_mtNextCts;     /**< Next video CTS to transcode */
    M4_MediaTime                m_mtAudioCts;    /**< Current audio cts */
    M4_MediaTime                m_AudioOffSet;   /**< Audio Offset to add to the cts in loop mode*/
    M4_MediaTime                m_PrevAudioCts;  /**< Previous audio cts for AAC looping */
    M4_MediaTime                m_DeltaAudioCts; /**< Delta audio cts for AAC looping */
    M4OSA_UInt32                m_CurrentFileSize; /**< Current Output file size  */
    M4OSA_UInt32                m_MaxFileSize;     /**< Max Output file size  */
    M4OSA_Bool                  m_IsLastPicture;   /**< A boolean that signals to the encoder that
                                                       this is the last frame to be encoded*/
    M4OSA_Bool                  m_bLastInternalCallBack;
    M4OSA_UInt32                m_NbCurrentFrame;  /**< Index of the current YUV frame encoded */

    /**
     *    Audio padding mode */
    M4OSA_Bool                    m_bAudioPaddingSilence;  /**< A boolean that signals that audio
                                                                AU will be padded by silence */
} M4PTO3GPP_InternalContext;



/**
 ******************************************************************************
 * M4OSA_ERR M4PTO3GPP_applyVPP(M4VPP_Context pContext, M4VIFI_ImagePlane* pPlaneIn,
 *                                  M4VIFI_ImagePlane* pPlaneOut)
 * @brief    Call an external callback to get the picture to encode
 * @note    It is called by the video encoder
 * @param    pContext    (IN) VPP context, which actually is the M4PTO3GPP
 *                            internal context in our case
 * @param    pPlaneIn    (IN) Contains the image
 * @param    pPlaneOut    (IN/OUT) Pointer to an array of 3 planes that will contain the
 *                        output YUV420 image read with the m_pPictureCallbackFct
 * @return    M4NO_ERROR:    No error
 * @return    Any error returned by an underlaying module
 ******************************************************************************
 */
M4OSA_ERR M4PTO3GPP_applyVPP(M4VPP_Context pContext, M4VIFI_ImagePlane* pPlaneIn,
 M4VIFI_ImagePlane* pPlaneOut);


#ifdef __cplusplus
}
#endif

#endif /* __M4PTO3GPP_INTERNALTYPES_H__ */

