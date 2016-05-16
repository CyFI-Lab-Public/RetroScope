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
 * @file    M4VSS3GPP_InternalTypes.h
 * @brief    This file contains all enum and types not visible to the external world.
 * @note
 ******************************************************************************
*/


#ifndef __M4VSS3GPP_INTERNALTYPES_H__
#define __M4VSS3GPP_INTERNALTYPES_H__

#define M4VSS_VERSION_MAJOR        3
#define M4VSS_VERSION_MINOR        2
#define M4VSS_VERSION_REVISION    5

#include "NXPSW_CompilerSwitches.h"

/**
 *    VSS public API and types */
#include "M4VSS3GPP_API.h"

/**
 *    Internally used modules */
#include "M4READER_Common.h"        /**< Reader common interface */
#include "M4WRITER_common.h"        /**< Writer common interface */
#include "M4DECODER_Common.h"        /**< Decoder common interface */
#include "M4ENCODER_common.h"        /**< Encoder common interface */
#include "M4VIFI_FiltersAPI.h"        /**< Image planes definition */
#include "M4READER_3gpCom.h"        /**< Read 3GPP file     */
#include "M4AD_Common.h"            /**< Decoder audio   */
#include "M4ENCODER_AudioCommon.h"  /**< Encode audio    */


#include "SSRC.h"                    /**< SSRC             */
#include "From2iToMono_16.h"        /**< Stereo to Mono     */
#include "MonoTo2I_16.h"            /**< Mono to Stereo     */

#ifdef __cplusplus
extern "C" {
#endif

#define WINDOW_SIZE 10
/**
 ******************************************************************************
 * enum            M4VSS3GPP_EditState
 * @brief        Main state machine of the VSS 3GPP edit operation.
 ******************************************************************************
*/

typedef enum
{
    M4VSS3GPP_kEditState_CREATED    = 0,    /**< M4VSS3GPP_editInit has been called */
    M4VSS3GPP_kEditState_VIDEO        = 1,    /**< Processing video track */
    M4VSS3GPP_kEditState_AUDIO        = 2,    /**< Processing audio track */
    M4VSS3GPP_kEditState_MP3        = 3,    /**< Processing MP3 audio track */
    M4VSS3GPP_kEditState_MP3_JUMP   = 4,        /**< Processing a jump in a MP3 audio track */
    M4VSS3GPP_kEditState_FINISHED    = 5,    /**< Processing done, VSS 3GPP can be closed */
    M4VSS3GPP_kEditState_CLOSED        = 6        /**< Output file has been closed,
                                                     VSS 3GPP can be destroyed */
}
M4VSS3GPP_EditState;

typedef enum
{
    /**< Doing Read/Write operation. This operation will have no processing
     * on input frames. Only time stamp manipulations in output file. */
    M4VSS3GPP_kEditVideoState_READ_WRITE    = 10,
    /**< Decode encode to create an I frame. This is done for a single frame
     * to create a new reference frame. */
    M4VSS3GPP_kEditVideoState_BEGIN_CUT     = 11,
    /**< Doing Read->Decode->Filter->Encode->Write operation on the input file
     * to create the output file. */
    M4VSS3GPP_kEditVideoState_DECODE_ENCODE = 12,
    /**< Applied when Transition is active and blending of two videos is
     * required. */
    M4VSS3GPP_kEditVideoState_TRANSITION    = 13,
    /**< Special Read/Write mode used after BEGIN_CUT state. The frame
     * is already coded as I frame in BEGIN_CUT state; so skip it. */
    M4VSS3GPP_kEditVideoState_AFTER_CUT     = 14
}
M4VSS3GPP_EditVideoState;

typedef enum
{
    M4VSS3GPP_kEditAudioState_READ_WRITE    = 20,    /**< Doing Read/Write operation
                                                        (no decoding/encoding) */
    M4VSS3GPP_kEditAudioState_DECODE_ENCODE = 21,    /**< Doing Read-Decode/Filter/
                                                            Encode-Write operation */
    M4VSS3GPP_kEditAudioState_TRANSITION    = 22    /**< Transition; blending of two audio */
}
M4VSS3GPP_EditAudioState;


/**
 ******************************************************************************
 * enum            M4VSS3GPP_ClipStatus
 * @brief        Status of the clip.
 ******************************************************************************
*/
typedef enum
{
    M4VSS3GPP_kClipStatus_READ            = 0,    /**< The clip is currently ready for reading */
    M4VSS3GPP_kClipStatus_DECODE        = 1,    /**< The clip is currently ready for decoding */
    M4VSS3GPP_kClipStatus_DECODE_UP_TO    = 2        /**< The clip is currently in splitted
                                                         decodeUpTo() processing */
}
M4VSS3GPP_ClipStatus;


/**
 ******************************************************************************
 * enum            M4VSS3GPP_ClipCurrentEffect
 * @brief        Current effect applied to the clip.
 ******************************************************************************
*/
typedef enum
{
    M4VSS3GPP_kClipCurrentEffect_NONE    = 0,    /**< None */
    M4VSS3GPP_kClipCurrentEffect_BEGIN    = 1,    /**< Begin effect currently applied */
    M4VSS3GPP_kClipCurrentEffect_END    = 2        /**< End effect currently applied */
}
M4VSS3GPP_ClipCurrentEffect;


/**
 ******************************************************************************
 * enum            M4VSS3GPP_AudioMixingState
 * @brief        Main state machine of the VSS audio mixing operation.
 ******************************************************************************
*/
typedef enum
{
    M4VSS3GPP_kAudioMixingState_VIDEO = 0,            /**< Video is being processed */
    M4VSS3GPP_kAudioMixingState_AUDIO_FIRST_SEGMENT,  /**< Audio is being processed */
    M4VSS3GPP_kAudioMixingState_AUDIO_SECOND_SEGMENT, /**< Audio is being processed */
    M4VSS3GPP_kAudioMixingState_AUDIO_THIRD_SEGMENT,  /**< Audio is being processed */
    M4VSS3GPP_kAudioMixingState_FINISHED              /**< Processing finished, user must now
                                                            call M4VSS3GPP_audioMixingCleanUp*/
}
M4VSS3GPP_AudioMixingState;


/**
 ******************************************************************************
 * enum            M4VSS3GPP_ExtractPictureState
 * @brief        Main state machine of the VSS picture extraction.
 ******************************************************************************
*/
typedef enum
{
    M4VSS3GPP_kExtractPictureState_OPENED   = 0,  /**< Video clip is opened and ready to be read
                                                     until the RAP before the picture to extract */
    M4VSS3GPP_kExtractPictureState_PROCESS    = 1,  /**< Video is decoded from the previous RAP
                                                        to the picture to extract */
    M4VSS3GPP_kExtractPictureState_EXTRACTED= 2   /**< Video AU has been  decoded, user must now
                                                        call M4VSS3GPP_extractPictureCleanUp */
}
M4VSS3GPP_ExtractPictureState;


/**
 ******************************************************************************
 * @brief        Codecs registration same as in VPS and VES, so less mapping
 *              is required toward VSS api types
 ******************************************************************************
*/
typedef struct
{
    M4WRITER_GlobalInterface*    pGlobalFcts;    /**< open, close, setoption,etc... functions */
    M4WRITER_DataInterface*        pDataFcts;        /**< data manipulation functions */
} M4VSS3GPP_WriterInterface;
/**
 ******************************************************************************
 * struct AAC_DEC_STREAM_PROPS
 * @brief AAC Stream properties
 * @Note aNoChan and aSampFreq are used for parsing even the user parameters
 *        are different.  User parameters will be input for the output behaviour
 *        of the decoder whereas for parsing bitstream properties are used.
 ******************************************************************************
 */
typedef struct {
  M4OSA_Int32 aAudioObjectType;     /**< Audio object type of the stream - in fact
                                         the type found in the Access Unit parsed */
  M4OSA_Int32 aNumChan;             /**< number of channels (=1(mono) or =2(stereo))
                                         as indicated by input bitstream*/
  M4OSA_Int32 aSampFreq;            /**< sampling frequency in Hz */
  M4OSA_Int32 aExtensionSampFreq;   /**< extended sampling frequency in Hz, = 0 is
                                         no extended frequency */
  M4OSA_Int32 aSBRPresent;          /**< presence=1/absence=0 of SBR */
  M4OSA_Int32 aPSPresent;           /**< presence=1/absence=0 of PS */
  M4OSA_Int32 aMaxPCMSamplesPerCh;  /**< max number of PCM samples per channel */
} AAC_DEC_STREAM_PROPS;


/**
 ******************************************************************************
 * enum            M4VSS3GPP_MediaAndCodecCtxt
 * @brief        Filesystem and codec registration function pointers
 ******************************************************************************
*/
typedef struct {
    /**
      * Media and Codec registration */
    /**< Table of M4VES_WriterInterface structures for avalaible Writers list */
    M4VSS3GPP_WriterInterface    WriterInterface[M4WRITER_kType_NB];
    /**< open, close, setoption,etc... functions of the used writer*/
    M4WRITER_GlobalInterface*    pWriterGlobalFcts;
    /**< data manipulation functions of the used writer */
    M4WRITER_DataInterface*        pWriterDataFcts;

    /**< Table of M4ENCODER_GlobalInterface structures for avalaible encoders list */
    M4ENCODER_GlobalInterface*    pVideoEncoderInterface[M4ENCODER_kVideo_NB];
    /**< Functions of the used encoder */
    M4ENCODER_GlobalInterface*    pVideoEncoderGlobalFcts;

    M4OSA_Void*                    pVideoEncoderExternalAPITable[M4ENCODER_kVideo_NB];
    M4OSA_Void*                    pCurrentVideoEncoderExternalAPI;
    M4OSA_Void*                    pVideoEncoderUserDataTable[M4ENCODER_kVideo_NB];
    M4OSA_Void*                    pCurrentVideoEncoderUserData;

    /**< Table of M4ENCODER_AudioGlobalInterface structures for avalaible encoders list */
    M4ENCODER_AudioGlobalInterface*    pAudioEncoderInterface[M4ENCODER_kAudio_NB];
    /**< Table of internal/external flags for avalaible encoders list */
    M4OSA_Bool                      pAudioEncoderFlag[M4ENCODER_kAudio_NB];
    /**< Functions of the used encoder */
    M4ENCODER_AudioGlobalInterface*    pAudioEncoderGlobalFcts;

    M4READER_GlobalInterface*   m_pReaderGlobalItTable[M4READER_kMediaType_NB];
    M4READER_DataInterface*     m_pReaderDataItTable[M4READER_kMediaType_NB];
    M4READER_GlobalInterface*   m_pReader;
    M4READER_DataInterface*     m_pReaderDataIt;
    M4OSA_UInt8                 m_uiNbRegisteredReaders;

    M4DECODER_VideoInterface*   m_pVideoDecoder;
    M4DECODER_VideoInterface*   m_pVideoDecoderItTable[M4DECODER_kVideoType_NB];
    M4OSA_UInt8                 m_uiNbRegisteredVideoDec;
#ifdef M4VSS_ENABLE_EXTERNAL_DECODERS
    M4OSA_Void*                    m_pCurrentVideoDecoderUserData;
    M4OSA_Void*                    m_pVideoDecoderUserDataTable[M4DECODER_kVideoType_NB];
#endif

    M4AD_Interface*             m_pAudioDecoder;
    M4AD_Interface*                m_pAudioDecoderItTable[M4AD_kType_NB];
    /**< store indices of external decoders */
    M4OSA_Bool                    m_pAudioDecoderFlagTable[M4AD_kType_NB];

    M4OSA_Void*                pAudioEncoderUserDataTable[M4ENCODER_kAudio_NB];
    M4OSA_Void*                pCurrentAudioEncoderUserData;

    M4OSA_Void*                pAudioDecoderUserDataTable[M4AD_kType_NB];
    M4OSA_Void*                pCurrentAudioDecoderUserData;

#ifdef M4VSS_SUPPORT_OMX_CODECS
    /* boolean to tell whether registered external OMX codecs should be freed during cleanup
     or new codec registration*/
    M4OSA_Bool    bAllowFreeingOMXCodecInterface;
#endif


} M4VSS3GPP_MediaAndCodecCtxt;


/**
 ******************************************************************************
 * structure    M4VSS3GPP_ClipContext
 * @brief        This structure contains informations related to one 3GPP clip (private)
 * @note        This structure is used to store the context related to one clip
 ******************************************************************************
*/
typedef struct
{
    M4VSS3GPP_ClipSettings*        pSettings;            /**< Pointer to the clip settings
                                                            (not possessed) */

    M4VSS3GPP_ClipStatus        Vstatus;            /**< Video status of the clip reading */
    M4VSS3GPP_ClipStatus        Astatus;            /**< Audio status of the clip reading */

    M4OSA_Int32                    iVoffset;            /**< [Milliseconds] Offset between the
                                                            clip and the output video stream
                                                            (begin cut taken into account) */
    M4OSA_Int32                    iAoffset;           /**< [Timescale] Offset between the clip
                                                            and the output audio stream (begin
                                                            cut taken into account) */

    /**
     * 3GPP reader Stuff */
    M4OSA_FileReadPointer*        pFileReadPtrFct;
    M4OSA_Context                pReaderContext;         /**< Context of the 3GPP reader module */
    M4_VideoStreamHandler*        pVideoStream;        /**< Description of the read video stream */
    M4_AudioStreamHandler*        pAudioStream;        /**< Description of the read audio stream */
    M4_AccessUnit                VideoAU;            /**< Read video access unit (we do not use a
                                                            pointer to allocate later, because
                                                            most of the time we will need it) */
    M4_AccessUnit                AudioAU;            /**< Read audio access unit (we do not use a
                                                         pointer to allocate later, because most
                                                         of the time we will need it) */
    M4OSA_Bool                    bVideoAuAvailable;    /**< Tell if a video AU is available
                                                            (previously read) */
    /**< Boolean only used to fix the BZZ bug... */
    M4OSA_Bool                    bFirstAuWritten;

    /**
     * Video decoder stuff */
    M4OSA_Context                pViDecCtxt;            /**< Video decoder context */
    M4OSA_Int32                 iVideoDecCts;       /**< [Milliseconds] For video decodeUpTo(),
                                                             the actual reached cts */
    M4OSA_Int32                    iVideoRenderCts;    /**< [Milliseconds] For video render(),
                                                             the actual reached cts */
    M4OSA_Bool                    isRenderDup;        /**< To handle duplicate frame rendering in
                                                             case of external decoding */
    M4VIFI_ImagePlane*            lastDecodedPlane;    /**< Last decoded plane */

    /**
     * MPEG4 time info stuff at clip level */
    M4OSA_Bool             bMpeg4GovState;            /**< Namely, update or initialization */
    M4OSA_UInt32           uiMpeg4PrevGovValueGet;    /**< Previous Gov value read (in second) */
    M4OSA_UInt32           uiMpeg4PrevGovValueSet;    /**< Previous Gov value write (in second) */

    /**
     * Time-line stuff */
     /**< [Milliseconds] CTS at which the video clip actually starts */
    M4OSA_Int32                    iActualVideoBeginCut;
    /**< [Milliseconds] CTS at which the audio clip actually starts */
    M4OSA_Int32                    iActualAudioBeginCut;
    /**< [Milliseconds] Time at which the clip must end */
    M4OSA_Int32                    iEndTime;

    /**
     * Audio decoder stuff */
    M4OSA_Context                pAudioDecCtxt;        /**< Context of the AMR decoder */
    M4AD_Buffer                 AudioDecBufferIn;    /**< Input structure for the audio decoder */
    M4AD_Buffer                    AudioDecBufferOut;    /**< Buffer for the decoded PCM data */
    AAC_DEC_STREAM_PROPS        AacProperties;      /**< Structure for new api to get AAC
                                                            properties */

    /**
     * Audio AU to Frame split stuff */
    M4OSA_Bool                bAudioFrameAvailable;  /**< True if an audio frame is available */
    M4OSA_MemAddr8            pAudioFramePtr;        /**< Pointer to the Audio frame */
    M4OSA_UInt32              uiAudioFrameSize;        /**< Size of the audio frame available */
    M4OSA_Int32               iAudioFrameCts;       /**< [Timescale] CTS of the audio frame
                                                            available */

    /**
     * Silence frame stuff */
     /**< Size to reserve to store a pcm full of zeros compatible with master clip stream type */
    M4OSA_UInt32                uiSilencePcmSize;
    /**< Pointer to silence frame data compatible with master clip stream type */
    M4OSA_UInt8*                pSilenceFrameData;
    /**< Size of silence frame data compatible with master clip stream type */
    M4OSA_UInt32                uiSilenceFrameSize;
    /**< [Timescale] Duration of silence frame data compatible with master clip stream type */
    M4OSA_Int32                 iSilenceFrameDuration;
    M4OSA_Double                scale_audio;            /**< frequency / 1000.0 */

    /**
     * Interfaces of the used modules */
     /**< Filesystem and shell reader, decoder functions */
    M4VSS3GPP_MediaAndCodecCtxt ShellAPI;
    M4VIFI_ImagePlane           *pPlaneYuv;  /* YUV420 image plane, converted from ARGB888 */
    M4VIFI_ImagePlane*          m_pPreResizeFrame;  /* The decoded image before resize
                                                   (allocated only if resize needed)*/
    M4VIFI_ImagePlane           *pPlaneYuvWithEffect; /* YUV420 image plane, with color effect */
    M4OSA_Bool                  bGetYuvDataFromDecoder;  /* Boolean used to get YUV data from dummy video decoder only for first time */
} M4VSS3GPP_ClipContext;


/**
 ******************************************************************************
 * enum            anonymous enum
 * @brief        enum to keep track of the encoder state
 ******************************************************************************
*/
enum
{
    M4VSS3GPP_kNoEncoder,
    M4VSS3GPP_kEncoderClosed,
    M4VSS3GPP_kEncoderStopped,
    M4VSS3GPP_kEncoderRunning
};

/**
 ******************************************************************************
 * structure    M4VSS3GPP_AudioVideoContext
 * @brief        This structure defines the audio video context (private)
 * @note        This structure is used for all audio/video, encoding/writing operations.
 ******************************************************************************
*/
typedef struct
{
    /**
     * Timing Stuff */
    // Decorrelate input and output encoding timestamp to handle encoder prefetch
    /**< [Milliseconds] Duration of the output file, used for progress computation */
    M4OSA_Double                dInputVidCts;
    /**< [Milliseconds] Current CTS of the video output stream */
    M4OSA_Double                dOutputVidCts;
/**< [Milliseconds] Current CTS of the audio output stream */
    M4OSA_Double                dATo;
     /**< [Milliseconds] Duration of the output file, used for progress computation */
    M4OSA_Int32                    iOutputDuration;

    /**
     * Output Video Stream Stuff */
    M4SYS_StreamType            VideoStreamType;        /**< Output video codec */
    M4OSA_Int32                 outputVideoProfile;  /**< Output video profile */
    M4OSA_Int32                 outputVideoLevel;   /**< Output video level */
    M4OSA_UInt32                uiVideoBitrate;     /**< Average video bitrate of the output file,
                                                         computed from input bitrates, durations,
                                                          transitions and cuts */
    M4OSA_UInt32                uiVideoWidth;            /**< Output image width */
    M4OSA_UInt32                uiVideoHeight;            /**< Output image height */
    M4OSA_UInt32                uiVideoTimeScale;        /**< Time scale to use for the encoding
                                                            of the transition (if MPEG-4) */
    M4OSA_Bool                    bVideoDataPartitioning;    /**< Data partitioning to use for the
                                                                 encoding of the transition
                                                                 (if MPEG-4) */
    M4OSA_MemAddr8                pVideoOutputDsi;        /**< Decoder Specific Info of the output
                                                                 MPEG-4 track */
    M4OSA_UInt16                uiVideoOutputDsiSize;    /**< Size of the Decoder Specific Info
                                                                of the output MPEG-4 track */
    /**
     * Output Audio Stream Stuff */
    M4SYS_StreamType            AudioStreamType;        /**< Type of the output audio stream */
    M4OSA_UInt32                uiNbChannels;           /**< Number of channels in the output
                                                            stream (1=mono, 2=stereo) */
    M4OSA_UInt32                uiAudioBitrate;         /**< Audio average bitrate (in bps) */
    M4OSA_UInt32                uiSamplingFrequency;    /**< Sampling audio frequency (8000 for
                                                                amr, 16000 or more for aac) */
    M4OSA_MemAddr8                pAudioOutputDsi;        /**< Decoder Specific Info of the
                                                                output audio track */
    M4OSA_UInt16                uiAudioOutputDsiSize;    /**< Size of the Decoder Specific Info
                                                                of the output audio track */

    /**
     * Audio Encoder stuff */
    M4OSA_Context                   pAudioEncCtxt;        /**< Context of the audio encoder */
    M4ENCODER_AudioDecSpecificInfo  pAudioEncDSI;       /**< Decoder specific info built by the
                                                                encoder */
    M4ENCODER_AudioParams           AudioEncParams;     /**< Config of the audio encoder */

    /**
     * Silence frame stuff */
    M4OSA_UInt32                uiSilencePcmSize;       /**< Size to reserve to store a pcm full
                                                             of zeros compatible with master clip
                                                             stream type */
    M4OSA_UInt8*                pSilenceFrameData;      /**< Pointer to silence frame data
                                                                compatible with master clip
                                                                stream type */
    M4OSA_UInt32                uiSilenceFrameSize;     /**< Size of silence frame data compatible
                                                             with master clip stream type */
    M4OSA_Int32                 iSilenceFrameDuration;  /**< [Timescale] Duration of silence frame
                                                                 data compatible with master clip
                                                                 stream type */
    M4OSA_Double                scale_audio;            /**< frequency / 1000.0 */

    /**
     * Video Encoder stuff */
    M4ENCODER_Context            pEncContext;            /**< Context of the encoder */
    M4WRITER_DataInterface        OurWriterDataInterface;    /**< Our own implementation of the
                                                                    writer interface, to give to
                                                                    the encoder shell */
    M4OSA_MemAddr32                pDummyAuBuffer;            /**< Buffer given to the encoder for
                                                                   it to write AUs we don't want
                                                                    in the output */
    M4OSA_Int32                    iMpeg4GovOffset;        /**< Clip GOV offset in ms between
                                                                 video and system time */
    M4OSA_ERR                    VppError;                /**< Error for VPP are masked by Video
                                                               Encoder, so we must remember it */
    M4OSA_UInt32                encoderState;

    /**
     * Writer stuff */
    M4WRITER_Context            p3gpWriterContext;        /**< Context of the 3GPP writer module */
    M4SYS_StreamDescription        WriterVideoStream;        /**< Description of the written
                                                                    video stream */
    M4SYS_StreamDescription        WriterAudioStream;        /**< Description of the written
                                                                    audio stream */
    M4WRITER_StreamVideoInfos    WriterVideoStreamInfo;    /**< Video properties of the written
                                                                     video stream */
    M4WRITER_StreamAudioInfos    WriterAudioStreamInfo;    /**< Audio properties of the written
                                                                    audio stream */
    M4SYS_AccessUnit            WriterVideoAU;            /**< Written video access unit */
    M4SYS_AccessUnit            WriterAudioAU;            /**< Written audio access unit */
    M4OSA_UInt32                uiVideoMaxAuSize;        /**< Max AU size set to the writer
                                                                for the video */
    M4OSA_UInt32                uiAudioMaxAuSize;        /**< Max AU size set to the writer
                                                                for the audio */
    M4OSA_UInt32                uiOutputAverageVideoBitrate; /**< Average video bitrate of the
                                                                    output file, computed from
                                                                    input bitrates, durations,
                                                                    transitions and cuts */

} M4VSS3GPP_EncodeWriteContext;


/**
 ******************************************************************************
 * structure    M4VSS3GPP_InternalEditContext
 * @brief        This structure defines the edit VSS context (private)
 * @note        This structure is used for all VSS edit operations to store the context
 ******************************************************************************
*/
typedef struct
{
    /**
     * VSS 3GPP main variables */
    M4VSS3GPP_EditState         State;                    /**< VSS internal state */
    M4VSS3GPP_EditVideoState    Vstate;
    M4VSS3GPP_EditAudioState    Astate;

    /**
     * User Settings (copied, thus owned by VSS3GPP) */
    M4OSA_UInt8                        uiClipNumber;        /**< Number of element of the clip
                                                                 list pClipList. */
    M4VSS3GPP_ClipSettings           *pClipList;            /**< List of the input clips settings
                                                            Array of uiClipNumber clip settings */
    M4VSS3GPP_TransitionSettings   *pTransitionList;    /**< List of the transition settings.
                                                    Array of uiClipNumber-1 transition settings */
    M4VSS3GPP_EffectSettings       *pEffectsList;        /**< List of the effects settings.
                                                             Array of nbEffects RC */
    M4OSA_UInt8                       *pActiveEffectsList;    /**< List of the active effects
                                                                settings. Array of nbEffects RC */
    M4OSA_UInt8                        nbEffects;            /**< Numbers of effects RC */
    M4OSA_UInt8                        nbActiveEffects;    /**< Numbers of active effects RC */

    /**
     * Input Stuff */
    M4OSA_UInt8                        uiCurrentClip;        /**< Index of the current clip 1 in
                                                                    the input clip list */
    M4VSS3GPP_ClipContext*            pC1;                /**< Context of the current clip 1 */
    M4VSS3GPP_ClipContext*            pC2;                /**< Context of the current clip 2 */

    /**
     * Decoder stuff */
    M4OSA_Double                dOutputFrameDuration;    /**< [Milliseconds] directly related to
                                                                 output frame rate */
    M4VIFI_ImagePlane            yuv1[3];            /**< First temporary YUV420 image plane */
    M4VIFI_ImagePlane            yuv2[3];            /**< Second temporary YUV420 image plane */
    M4VIFI_ImagePlane            yuv3[3];            /**< Third temporary YUV420 image plane RC */
    M4VIFI_ImagePlane            yuv4[3];            /**< Fourth temporary YUV420 image plane RC */

    /**
     * Effect stuff */
    M4OSA_Bool                    bClip1AtBeginCut;        /**< [Milliseconds] The clip1 is at
                                                                its begin cut */
    M4OSA_Int8                    iClip1ActiveEffect;        /**< The index of the active effect
                                                                    on Clip1 (<0 means none)
                                                                    (used for video and audio but
                                                                     not simultaneously) */
    M4OSA_Int8                    iClip2ActiveEffect;        /**< The index of the active effect
                                                                 on Clip2 (<0 means none)
                                                                 (used for video and audio but
                                                                 not simultaneously) */
    M4OSA_Bool                    bTransitionEffect;        /**< True if the transition effect
                                                                 must be applied at the current
                                                                 time */

    /**
     * Encoding and Writing operations */
    M4OSA_Bool                      bSupportSilence;    /**< Flag to know if the output stream can
                                                             support silence (even if not editable,
                                                              for example AAC+, but not EVRC) */
    M4VSS3GPP_EncodeWriteContext    ewc;                /**< Audio and video encode/write stuff */
    M4OSA_Bool                        bIsMMS;                /**< Boolean used to know if we are
                                                                processing a file with an output
                                                                size constraint */
    M4OSA_UInt32                    uiMMSVideoBitrate;    /**< If in MMS mode,
                                                                 targeted video bitrate */
    M4VIDEOEDITING_VideoFramerate    MMSvideoFramerate;    /**< If in MMS mode,
                                                                 targeted video framerate */

    /**
     * Filesystem functions */
    M4OSA_FileReadPointer*        pOsaFileReadPtr;     /**< OSAL file read functions,
                                                             to be provided by user */
    M4OSA_FileWriterPointer*    pOsaFileWritPtr;     /**< OSAL file write functions,
                                                             to be provided by user */

    /**
     * Interfaces of the used modules */
    M4VSS3GPP_MediaAndCodecCtxt         ShellAPI;           /**< Filesystem and shell reader,
                                                                 decoder functions */
    M4OSA_Bool               bIssecondClip;
    M4OSA_UInt8              *pActiveEffectsList1;  /**< List of the active effects settings. Array of nbEffects RC */
    M4OSA_UInt8              nbActiveEffects1;  /**< Numbers of active effects RC */
    M4OSA_Bool               m_bClipExternalHasStarted;  /**< Flag to indicate that an
                                                              external effect is active */
    M4OSA_Int32              iInOutTimeOffset;
    M4OSA_Bool               bEncodeTillEoF;
    M4xVSS_EditSettings      xVSS;
    M4OSA_Context            m_air_context;

    M4OSA_Bool bClip1ActiveFramingEffect; /**< Overlay flag for clip1 */
    M4OSA_Bool bClip2ActiveFramingEffect; /**< Overlay flag for clip2, used in transition */
} M4VSS3GPP_InternalEditContext;


/**
 ******************************************************************************
 * structure    M4VSS3GPP_InternalAudioMixingContext
 * @brief        This structure defines the audio mixing VSS 3GPP context (private)
 * @note        This structure is used for all VSS 3GPP audio mixing operations to store
 *                the context
 ******************************************************************************
*/
typedef struct
{
    /**
     *    VSS main variables */
    M4VSS3GPP_AudioMixingState State;                    /**< VSS audio mixing internal state */

    /**
     * Internal copy of the input settings */
    M4OSA_Int32                iAddCts;                 /**< [Milliseconds] Time, in milliseconds,
                                                             at which the added audio track is
                                                              inserted */
    M4OSA_UInt32               uiBeginLoop;                /**< Describes in milli-second the
                                                                start time of the loop */
    M4OSA_UInt32               uiEndLoop;                /**< Describes in milli-second the end
                                                            time of the loop (0 means no loop) */
    M4OSA_Bool                 bRemoveOriginal;            /**< If true, the original audio track
                                                                is not taken into account */

    /**
     * Input audio/video file */
    M4VSS3GPP_ClipSettings        InputClipSettings;        /**< Structure internally used to
                                                                 manage the input 3GPP settings */
    M4VSS3GPP_ClipContext*        pInputClipCtxt;           /**< Context of the input 3GPP clip */

    /**
     * Added audio file stuff */
    M4VSS3GPP_ClipSettings        AddedClipSettings;        /**< Structure internally used to
                                                                    manage the added settings */
    M4VSS3GPP_ClipContext*        pAddedClipCtxt;           /**< Context of the added 3GPP clip */

    /**
     * Audio stuff */
    M4OSA_Float                    fOrigFactor;            /**< Factor to apply to the original
                                                                audio track for the mixing */
    M4OSA_Float                    fAddedFactor;            /**< Factor to apply to the added
                                                                    audio track for the mixing */
    M4OSA_Bool                  bSupportSilence;        /**< Flag to know if the output stream can
                                                             support silence (even if not editable,
                                                              for example AAC+, but not EVRC) */
    M4OSA_Bool                  bHasAudio;              /**< Flag to know if we have to delete
                                                            audio track */
    M4OSA_Bool                  bAudioMixingIsNeeded;  /**< Flag to know if we have to do mixing */

    /**
     * Encoding and Writing operations */
    M4VSS3GPP_EncodeWriteContext    ewc;                /**< Audio and video encode/write stuff */

    /**
     * Filesystem functions */
    M4OSA_FileReadPointer*        pOsaFileReadPtr;     /**< OSAL file read functions,
                                                             to be provided by user */
    M4OSA_FileWriterPointer*    pOsaFileWritPtr;     /**< OSAL file write functions,
                                                            to be provided by user */

    /**
     * Interfaces of the used modules */
    M4VSS3GPP_MediaAndCodecCtxt ShellAPI;               /**< Filesystem and shell reader,
                                                                 decoder functions */

    /**
     * Sample Rate Convertor (SSRC) stuff (needed in case of mixing with != ASF/nb of channels) */
    M4OSA_Bool                  b_SSRCneeded;        /**< If true, SSRC is needed
                                                            (!= ASF or nb of channels) */
    M4OSA_UInt8                 ChannelConversion;    /**< 1=Conversion from Mono to Stereo
                                                             2=Stereo to Mono, 0=no conversion */
    SSRC_Instance_t             SsrcInstance;        /**< Context of the Ssrc */
    SSRC_Scratch_t*             SsrcScratch;        /**< Working memory of the Ssrc */
    short                       iSsrcNbSamplIn;    /**< Number of sample the Ssrc needs as input */
    short                       iSsrcNbSamplOut;    /**< Number of sample the Ssrc outputs */
    M4OSA_MemAddr8              pSsrcBufferIn;        /**< Input of the SSRC */
    M4OSA_MemAddr8              pSsrcBufferOut;        /**< Output of the SSRC */
    M4OSA_MemAddr8              pPosInSsrcBufferIn;    /**< Position into the SSRC in buffer */
    M4OSA_MemAddr8              pPosInSsrcBufferOut;/**< Position into the SSRC out buffer */
    M4OSA_MemAddr8              pTempBuffer;        /**< Temporary buffer */
    M4OSA_MemAddr8              pPosInTempBuffer;    /**< Position in temporary buffer */
    M4OSA_UInt32                minimumBufferIn;    /**< Minimum amount of decoded data to be
                                                            processed by SSRC and channel
                                                             convertor */
    M4OSA_Bool                  b_DuckingNeedeed;
    M4OSA_Int32                 InDucking_threshold;  /**< Threshold value at which background
                                                                 music shall duck */
    M4OSA_Float                 InDucking_lowVolume;  /**< lower the background track to this
                                                                factor and increase the primary
                                                                track to inverse of this factor */
    M4OSA_Float                 lowVolume;
    M4OSA_Int32                 audioVolumeArray[WINDOW_SIZE]; // store peak audio vol. level
                                                                  // for duration for WINDOW_SIZE
    M4OSA_Int32                 audVolArrIndex;
    M4OSA_Float                 duckingFactor ;     /**< multiply by this factor to bring
                                                             FADE IN/FADE OUT effect */
    M4OSA_Float                 fBTVolLevel;
    M4OSA_Float                 fPTVolLevel;
    M4OSA_Bool                  bDoDucking;
    M4OSA_Bool                  bLoop;
    M4OSA_Bool                  bNoLooping;
    M4OSA_Context              pLVAudioResampler;
    M4OSA_Bool                  bjumpflag;

} M4VSS3GPP_InternalAudioMixingContext;


/**
 ******************************************************************************
 * structure    M4VSS3GPP_InternalExtractPictureContext
 * @brief        This structure defines the extract picture VSS context (private)
 * @note        This structure is used for all VSS picture extractions to store the context
 ******************************************************************************
*/
typedef struct
{
    /**
     *    VSS main variables */
    M4VSS3GPP_ExtractPictureState State;                /**< VSS extract pictureinternal state */

    /**
     * Input files */
    M4VSS3GPP_ClipSettings        ClipSettings;            /**< Structure internally used to
                                                                manage the input 3FPP settings */
    M4VSS3GPP_ClipContext*        pInputClipCtxt;           /**< Context of the input 3GPP clip */

    /**
     * Settings */
    M4OSA_Int32                    iExtractCts;            /**< [Milliseconds] Cts of the AU
                                                                to be extracted */

    /**
     * Video stuff */
    M4VIFI_ImagePlane            decPlanes[3];            /**< Decoded YUV420 picture plane */
    M4OSA_UInt32                uiVideoWidth;            /**< Decoded image width */
    M4OSA_UInt32                uiVideoHeight;            /**< Decoded image height */

    /*
     * Decoder info */
    M4OSA_Int32                iDecCts;      /**< [Milliseconds] Decoded AU Cts */
    M4OSA_Bool                 bJumpFlag;     /**< 1 if a jump has been made */
    M4OSA_Int32                iDeltaTime;   /**< [Milliseconds] Time between previous RAP and
                                                     picture to extract */
    M4OSA_Int32                iGap;         /**< [Milliseconds] Time between jump AU and
                                                    extraction time */
    M4OSA_UInt32               uiStep;          /**< [Milliseconds] Progress bar time increment */

    /**
     * Filesystem functions */
     /**< OSAL file read functions, to be provided by user */
    M4OSA_FileReadPointer*        pOsaFileReadPtr;
    /**< OSAL file write functions, to be provided by user */
    M4OSA_FileWriterPointer*    pOsaFileWritPtr;

    M4OSA_Bool                    bClipOpened;
} M4VSS3GPP_InternalExtractPictureContext;


#ifdef __cplusplus
}
#endif

#endif /* __M4VSS3GPP_INTERNALTYPES_H__ */

