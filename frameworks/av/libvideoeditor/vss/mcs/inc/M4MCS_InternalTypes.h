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
 * @brief  MCS internal types and structures definitions
 * @note   This header file is not public
 *************************************************************************
 **/

#ifndef __M4MCS_INTERNALTYPES_H__
#define __M4MCS_INTERNALTYPES_H__

/**
 *    MCS public API and types */
#include "M4MCS_API.h"
#include "M4MCS_ErrorCodes.h"

#include "NXPSW_CompilerSwitches.h"

/** Determine absolute value of a. */
#define M4MCS_ABS(a)               ( ( (a) < (0) ) ? (-(a)) : (a) )


#define Y_PLANE_BORDER_VALUE    0x00
#define U_PLANE_BORDER_VALUE    0x80
#define V_PLANE_BORDER_VALUE    0x80


/**
 *    Internally used modules */
#include "M4READER_3gpCom.h"        /**< Read 3GPP file     */
#include "M4DECODER_Common.h"       /**< Decode video       */
#include "M4VIFI_FiltersAPI.h"      /**< Video resize       */
#include "M4AD_Common.h"            /**< Decoder audio      */
#include "SSRC.h"                   /**< SSRC               */
#include "From2iToMono_16.h"        /**< Stereo to Mono     */
#include "MonoTo2I_16.h"            /**< Mono to Stereo     */
#include "M4ENCODER_AudioCommon.h"  /**< Encode audio       */
#include "M4WRITER_common.h"        /**< Writer common interface */
#include "M4ENCODER_common.h"

/**
 *  Instead of including AAC core properties, it is better to redefine the needed type
 *  AAC_DEC_STREAM_PROPS
 *  In case of external AAC decoder, it will be necessary to put this type as public
 */

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
 * @brief        Codecs registration same as in VPS and VES, so less mapping
 *              is required toward MCS api types
 ******************************************************************************
 */
typedef struct
{
    M4WRITER_GlobalInterface* pGlobalFcts;    /**< open, close, setoption,etc... functions */
    M4WRITER_DataInterface*    pDataFcts;        /**< data manipulation functions */
} M4MCS_WriterInterface;

/**
 ******************************************************************************
 * enum            M4MCS_States
 * @brief        Main state machine of the MCS.
 ******************************************************************************
 */
typedef enum
{
    M4MCS_kState_CREATED,           /**< M4MCS_init has been called                */
    M4MCS_kState_OPENED,            /**< M4MCS_open has been called                */
    M4MCS_kState_SET,               /**< All mandatory parameters have been set    */
    M4MCS_kState_READY,             /**< All optionnal parameters have been set    */
    M4MCS_kState_BEGINVIDEOJUMP,    /**< Must jump to the Iframe before the begin cut */
    M4MCS_kState_BEGINVIDEODECODE,  /**< Must decode up to the begin cut        */
    M4MCS_kState_PROCESSING,        /**< Step can be called                        */
    M4MCS_kState_PAUSED,            /**< Paused, Resume can be called            */
    M4MCS_kState_FINISHED,          /**< Transcoding is finished                */
    M4MCS_kState_CLOSED             /**< Output file has been created            */
} M4MCS_States;

/**
 ******************************************************************************
 * enum            M4MCS_StreamState
 * @brief        State of a media stream encoding (audio or video).
 ******************************************************************************
 */
typedef enum
{
    M4MCS_kStreamState_NOSTREAM  = 0,    /**< No stream present                    */
    M4MCS_kStreamState_STARTED   = 1,    /**< The stream encoding is in progress */
    M4MCS_kStreamState_FINISHED  = 2    /**< The stream has finished encoding    */
} M4MCS_StreamState;


/**
 ******************************************************************************
 * enum            anonymous enum
 * @brief        enum to keep track of the encoder state
 ******************************************************************************
 */
enum
{
    M4MCS_kNoEncoder,
    M4MCS_kEncoderClosed,
    M4MCS_kEncoderStopped,
    M4MCS_kEncoderRunning
};

/**
 ******************************************************************************
 * structure    M4MCS_InternalContext
 * @brief        This structure defines the MCS context (private)
 * @note        This structure is used for all MCS calls to store the context
 ******************************************************************************
 */
typedef struct
{
    M4OSA_UInt32    bitPos;
                 /* bit count of number of bits used so far */

    M4OSA_UInt8   *streamBuffer;
                /* Bitstream Buffer */

    M4OSA_UInt32    byteCnt;
                /* Number of Bytes written in Bitstream buffer*/

    M4OSA_UInt32    currBuff;
                /* Current buffer holds, 4bytes of bitstream*/

    M4OSA_UInt8   prevByte;
                /* Previous byte written in the buffer */

    M4OSA_UInt8   prevPrevByte;
                /* Previous to previous byte written in the buffer */

}NSWAVC_bitStream_t_MCS;

#define _MAXnum_slice_groups  8
#define _MAXnum_ref_frames_in_pic_order_cnt_cycle  256

typedef struct
{
  M4OSA_UInt32  level_idc_index;
  M4OSA_UInt32  MaxFrameNum;
  M4OSA_UInt32  expectedDeltaPerPicOrderCntCycle;
  M4OSA_Int32   MaxPicOrderCntLsb;
  M4OSA_Int32   max_dec_frame_buffering;

  /* (pic_order_cnt_type == 1) */
  M4OSA_Int32   offset_for_non_ref_pic;
  M4OSA_Int32   offset_for_top_to_bottom_field;
  M4OSA_Int32   frame_crop_left_offset;
  M4OSA_Int32   frame_crop_right_offset;
  M4OSA_Int32   frame_crop_top_offset;
  M4OSA_Int32   frame_crop_bottom_offset;
  M4OSA_Int32   offset_for_ref_frame[_MAXnum_ref_frames_in_pic_order_cnt_cycle];

  M4OSA_UInt16 PicWidthInMbs;
  M4OSA_UInt16 FrameHeightInMbs;
  M4OSA_UInt16  pic_width_in_mbs_minus1;
  M4OSA_UInt16  pic_height_in_map_units_minus1;

#ifdef _CAP_FMO_
  M4OSA_UInt16 NumSliceGroupMapUnits;
  M4OSA_UInt16 MaxPicSizeInMbs;
#endif /*_CAP_FMO_*/

  M4OSA_UInt8   profile_idc;
  M4OSA_UInt8   reserved_zero_4bits;
  M4OSA_UInt8   level_idc;
  M4OSA_UInt8   seq_parameter_set_id;
  M4OSA_UInt8   log2_max_frame_num_minus4;
  M4OSA_UInt8   pic_order_cnt_type;
  /* if(pic_order_cnt_type == 0) */
  M4OSA_UInt8   log2_max_pic_order_cnt_lsb_minus4;

  M4OSA_UInt8   num_ref_frames_in_pic_order_cnt_cycle;
  /* for( i = 0; i < num_ref_frames_in_pic_order_cnt_cycle; i++ ) */
  M4OSA_UInt8   num_ref_frames;

  M4OSA_Bool    constraint_set0_flag;
  M4OSA_Bool    constraint_set1_flag;
  M4OSA_Bool    constraint_set2_flag;
  M4OSA_Bool    constraint_set3_flag;
  M4OSA_Bool    delta_pic_order_always_zero_flag;
  M4OSA_Bool    gaps_in_frame_num_value_allowed_flag;
  M4OSA_Bool    frame_mbs_only_flag;
  M4OSA_Bool    mb_adaptive_frame_field_flag;
  M4OSA_Bool    direct_8x8_inference_flag;
  M4OSA_Bool    frame_cropping_flag;
  M4OSA_Bool    vui_parameters_present_flag;
  M4OSA_Bool    Active;

  /* vui_seq_parameters_t vui_seq_parameters; */
} ComSequenceParameterSet_t_MCS;

typedef struct
{
  M4OSA_Int16       pic_init_qp_minus26;
  M4OSA_Int16       pic_init_qs_minus26;
  M4OSA_Int16       chroma_qp_index_offset;

//#ifdef _CAP_FMO_
  /* if( slice_group_map_type = = 0 ) */
  M4OSA_UInt16      run_length_minus1[_MAXnum_slice_groups];
  /* else if( slice_group_map_type = = 2 ) */
  M4OSA_UInt16      top_left[_MAXnum_slice_groups];
  M4OSA_UInt16      bottom_right[_MAXnum_slice_groups];
  /* else if( slice_group_map_type = = 6 ) */
  M4OSA_UInt16      pic_size_in_map_units_minus1;
  M4OSA_UInt16      slice_group_change_rate_minus1;

  M4OSA_UInt16 FirstMbInSliceGroup[_MAXnum_slice_groups];
  M4OSA_UInt16 LastMbInSliceGroup[_MAXnum_slice_groups];


  M4OSA_UInt8  *slice_group_id;
  M4OSA_UInt8  *MapUnitToSliceGroupMap;
  M4OSA_UInt8  *MbToSliceGroupMap;
  M4OSA_UInt16  NumSliceGroupMapUnits;

  M4OSA_UInt8       slice_group_map_type;
  /* else if( slice_group_map_type = = 3 || 4 || 5 */
  M4OSA_Bool        slice_group_change_direction_flag;
  M4OSA_Bool   map_initialized;
// #endif /*_CAP_FMO_*/

  M4OSA_UInt8       pic_parameter_set_id;
  M4OSA_UInt8       seq_parameter_set_id;
  M4OSA_UInt8      num_ref_idx_l0_active_minus1;
  M4OSA_UInt8      num_ref_idx_l1_active_minus1;
  M4OSA_UInt8       weighted_bipred_idc;
  M4OSA_UInt8       num_slice_groups_minus1;

  M4OSA_Bool        entropy_coding_mode_flag;
  /* if( pic_order_cnt_type < 2 )  in the sequence parameter set */
  M4OSA_Bool        pic_order_present_flag;
  M4OSA_Bool        weighted_pred_flag;
  M4OSA_Bool        deblocking_filter_control_present_flag;
  M4OSA_Bool        constrained_intra_pred_flag;
  M4OSA_Bool        redundant_pic_cnt_present_flag;
  M4OSA_Bool    Active;

  ComSequenceParameterSet_t_MCS *p_active_sps;
} ComPictureParameterSet_t_MCS;

typedef struct
{
      M4OSA_UInt32 bitPos;                /*!< bit position in buffer */
      M4OSA_UInt32 totalBits;             /*!< bit position in file (total bits read so far) */

      M4OSA_UInt32 lastTotalBits;         /*!< bit position in file of the last VOP */
      M4OSA_UInt32 numBitsInBuffer;       /*!< number of bits in buffer */
      M4OSA_UInt32 readableBytesInBuffer; /*!< number of bytes that can be read in decoder buffer*/
      M4OSA_UInt32 maxBufferSize;         /*!< max buffer size in bit units */
      M4OSA_UInt8  *Buffer;               /*!< char buffer at reading from file */
      M4OSA_Int32     i8BitCnt;
      M4OSA_UInt32     ui32TempBuff;
      M4OSA_Int8*pui8BfrPtr;
      M4OSA_UInt32    ui32LastTwoBytes;  /*!< stores the last read two bytes */
} ComBitStreamMCS_t;


typedef struct
{

    M4OSA_Int32 prev_frame_num;
    M4OSA_Int32 cur_frame_num;
    M4OSA_Int32 prev_new_frame_num;
    M4OSA_Int32 log2_max_frame_num_minus4;
    M4OSA_Int32 is_done;
    M4OSA_Int32 is_first;
    M4OSA_Int32 frame_count;
    M4OSA_Int32 frame_mod_count;
    M4OSA_Int32 POC_lsb;
    M4OSA_Int32 POC_lsb_mod;


    M4OSA_UInt32    m_Num_Bytes_NALUnitLength;

    M4OSA_UInt8*    m_pDecoderSpecificInfo;   /**< Pointer on specific information required
                                                   to create a decoder */
    M4OSA_UInt32    m_decoderSpecificInfoSize;/**< Size of the specific information pointer above*/

    M4OSA_UInt8*    m_pEncoderSPS;
    M4OSA_UInt32    m_encoderSPSSize;

    M4OSA_UInt8*    m_pEncoderPPS;
    M4OSA_UInt32    m_encoderPPSSize;

    M4OSA_UInt8*    m_pFinalDSI;
    M4OSA_UInt32    m_pFinalDSISize;

    M4OSA_UInt32    m_encoder_SPS_Cnt;
    ComSequenceParameterSet_t_MCS *p_clip_sps;
    M4OSA_UInt32    m_encoder_PPS_Cnt;
    ComPictureParameterSet_t_MCS  *p_clip_pps;

    ComSequenceParameterSet_t_MCS *p_encoder_sps;
    ComPictureParameterSet_t_MCS  *p_encoder_pps;


    ComSequenceParameterSet_t_MCS  encoder_sps;
    ComPictureParameterSet_t_MCS   encoder_pps;
    ComSequenceParameterSet_t_MCS  clip_sps;

    /* Encoder SPS parameters */
    M4OSA_UInt32 enc_seq_parameter_set_id;
    M4OSA_UInt32 enc_log2_max_frame_num_minus4;
    M4OSA_UInt32 enc_pic_order_cnt_type;
    M4OSA_UInt32 enc_log2_max_pic_order_cnt_lsb_minus4; /* applicable when POC type = 0 */
    M4OSA_UInt32 enc_delta_pic_order_always_zero_flag;
    M4OSA_Int32 enc_offset_for_non_ref_pic;
    M4OSA_Int32 enc_offset_for_top_to_bottom_field;
    M4OSA_UInt32 enc_num_ref_frames_in_pic_order_cnt_cycle; /* range 0 to 255 */
    /* array of size num_ref_frames_in_pic_order_cnt_cycle */
    M4OSA_Int32   enc_offset_for_ref_frame[256];
    M4OSA_UInt32 enc_num_ref_frames;
    M4OSA_UInt32 enc_gaps_in_frame_num_value_allowed_flag;


    /* Input clip SPS parameters */
    M4OSA_UInt32 clip_seq_parameter_set_id;
    M4OSA_UInt32 clip_log2_max_frame_num_minus4;
    M4OSA_UInt32 clip_pic_order_cnt_type;
    M4OSA_UInt32 clip_log2_max_pic_order_cnt_lsb_minus4; /* applicable when POC type = 0 */
    M4OSA_UInt32 clip_delta_pic_order_always_zero_flag;
    M4OSA_Int32  clip_offset_for_non_ref_pic;
    M4OSA_Int32  clip_offset_for_top_to_bottom_field;
    M4OSA_UInt32 clip_num_ref_frames_in_pic_order_cnt_cycle; /* range 0 to 255 */
    /* array of size num_ref_frames_in_pic_order_cnt_cycle */
    M4OSA_Int32  clip_offset_for_ref_frame[256];
    M4OSA_UInt32 clip_num_ref_frames;
    M4OSA_UInt32 clip_gaps_in_frame_num_value_allowed_flag;

    M4OSA_UInt32 final_PPS_ID;
    M4OSA_UInt32 final_SPS_ID;
    NSWAVC_bitStream_t_MCS  encbs;

} NSWAVC_MCS_t;



/**
 ******************************************************************************
 * structure    M4MCS_InternalContext
 * @brief       This structure defines the MCS context (private)
 * @note        This structure is used for all MCS calls to store the context
 ******************************************************************************
 */
typedef struct
{
    /**
     * MCS State and settings stuff */
    M4MCS_States            State;     /**< MCS internal state */
    M4MCS_StreamState       VideoState;/**< State of the video encoding */
    M4MCS_StreamState       AudioState;/**< State of the audio encoding */
    M4OSA_Bool              noaudio;/**< Flag to know if we have to deal with audio transcoding */
    M4OSA_Bool              novideo;/**< Flag to know if we have to deal with video transcoding */

    M4VIDEOEDITING_ClipProperties  InputFileProperties;/**< Input audio/video stream properties */
    M4OSA_Void*             pInputFile;             /**< Remember input file pointer between fast
                                                         open and normal open */
    M4VIDEOEDITING_FileType InputFileType;          /**< Remember input file type between fast
                                                         open and normal open */
    M4OSA_Bool              bFileOpenedInFastMode;  /**< Flag to know if a particular reader
                                                         supports fast open */
    M4OSA_UInt32            uiMaxMetadataSize;      /**< Limitation on the max acceptable moov
                                                         size of a 3gpp file */

    M4ENCODER_Format        EncodingVideoFormat;    /**< Output video format, set by the user */
    M4ENCODER_FrameWidth    EncodingWidth;          /**< Output video width, set by the user */
    M4ENCODER_FrameHeight   EncodingHeight;         /**< Output video height, set by the user */
    M4ENCODER_FrameRate     EncodingVideoFramerate; /**< Output video framerate, set by the user*/

    M4OSA_UInt32            uiBeginCutTime;     /**< Begin cut time, in milliseconds */
    M4OSA_UInt32            uiEndCutTime;       /**< Begin cut time, in milliseconds */
    M4OSA_UInt32            uiMaxFileSize;      /**< Maximum output file size, in bytes */
    M4OSA_UInt32            uiAudioBitrate;     /**< Targeted audio bitrate in bps */
    M4OSA_UInt32            uiVideoBitrate;     /**< Targeted video bitrate in bps */

    M4OSA_UInt8     uiProgress;  /**< Progress information saved at each step to be able to
                                      return it in case of pause */

    /**
     * Reader stuff */
    M4OSA_Context           pReaderContext;           /**< Context of the reader module */
    M4_VideoStreamHandler*  pReaderVideoStream;       /**< Description of the read video stream */
    M4_AudioStreamHandler*  pReaderAudioStream;       /**< Description of the read audio stream */
    M4OSA_Bool              bUnsupportedVideoFound;   /**< True if an unsupported video stream
                                                            type has been found */
    M4OSA_Bool              bUnsupportedAudioFound;   /**< True if an unsupported audio stream
                                                            type has been found */
    M4_AccessUnit           ReaderVideoAU;            /**< Read video access unit */
    M4_AccessUnit           ReaderVideoAU1;           /**< Read video access unit */
    M4_AccessUnit           ReaderVideoAU2;           /**< Read video access unit */
    M4_AccessUnit           ReaderAudioAU;            /**< Read audio access unit */
    M4_AccessUnit           ReaderAudioAU1;           /**< Read audio access unit */
    M4_AccessUnit           ReaderAudioAU2;           /**< Read audio access unit */
    M4OSA_MemAddr8          m_pDataAddress1;          /**< Temporary buffer for Access Unit */
    M4OSA_MemAddr8          m_pDataAddress2;          /**< Temporary buffer for Access Unit */
    M4OSA_MemAddr8          m_pDataVideoAddress1;     /**< Temporary buffer for Access Unit */
    M4OSA_MemAddr8          m_pDataVideoAddress2;     /**< Temporary buffer for Access Unit */
    M4OSA_UInt32            m_audioAUDuration;        /**< Audio AU duration */
    M4OSA_Int32             iAudioCtsOffset;          /**< Audio AU CTS offset due to begin cut */

    /**
     * Video decoder stuff */
    M4OSA_Context         pViDecCtxt;         /**< Video decoder context */
    M4OSA_Double          dViDecStartingCts;  /**< Video CTS at which the decode/encode will start
                                                   (used for begin cut and pause/resume) */
    M4OSA_Double          dViDecCurrentCts;   /**< Video CTS to decode */
    M4OSA_Int32           iVideoBeginDecIncr; /**< CTS step for the begin cut decode (doesn't
                                                    need floating point precision) */
    M4OSA_Double          dCtsIncrement;      /**< Cts increment from one video frame to another*/
    M4OSA_Bool            isRenderDup;        /**< To handle duplicate frame rendering in case of
                                                    external decoding */
    M4VIFI_ImagePlane*    lastDecodedPlane;   /**< Last decoded plane */

    /**
     * Video encoder stuff */
    M4OSA_Context         pViEncCtxt;         /**< Video encoder context */
    M4VIFI_ImagePlane*    pPreResizeFrame;    /**< The decoded image before resize
                                                  (allocated if resize needed only)*/
    M4OSA_UInt32          uiEncVideoBitrate;  /**< Actual video bitrate for the video encoder */
    M4OSA_UInt32          outputVideoTimescale;
    M4OSA_UInt32          encoderState;

    /**
     * Audio decoder stuff */
    M4OSA_Context         pAudioDecCtxt;        /**< Audio (AAC) decoder context */
    M4AD_Buffer           AudioDecBufferIn;     /**< Input structure for the audio decoder */
    M4AD_Buffer           AudioDecBufferOut;    /**< Output structure for the audio decoder */
    M4OSA_MemAddr8        pPosInDecBufferOut;   /**< Position into the decoder buffer */
    AAC_DEC_STREAM_PROPS  AacProperties;   /**< Structure for new api to get AAC properties */

    /**
     * Sample Rate Convertor (SSRC) stuff */
    SSRC_Instance_t        SsrcInstance;       /**< Context of the Ssrc */
    SSRC_Scratch_t*        SsrcScratch;        /**< Working memory of the Ssrc */
    short                  iSsrcNbSamplIn;     /**< Number of sample the Ssrc needs as input */
    short                  iSsrcNbSamplOut;    /**< Number of sample the Ssrc outputs */
    M4OSA_MemAddr8         pSsrcBufferIn;      /**< Input of the SSRC */
    M4OSA_MemAddr8         pSsrcBufferOut;     /**< Output of the SSRC */
    M4OSA_MemAddr8         pPosInSsrcBufferIn; /**< Position into the SSRC in buffer */
    M4OSA_MemAddr8         pPosInSsrcBufferOut;/**< Position into the SSRC out buffer */

    M4OSA_Context          pLVAudioResampler;


    /**
     * audio encoder stuff */
    M4OSA_Context                   pAudioEncCtxt; /**< Context of the audio encoder */
    M4ENCODER_AudioDecSpecificInfo  pAudioEncDSI; /**< Decoder specific info built by the encoder*/
    M4ENCODER_AudioParams           AudioEncParams;/**< Config of the audio encoder */
    M4OSA_MemAddr8            pAudioEncoderBuffer;      /**< Input of the encoder */
    M4OSA_MemAddr8            pPosInAudioEncoderBuffer; /**< Position into the encoder buffer */
    M4OSA_UInt32              audioEncoderGranularity;  /**< Minimum number of pcm samples needed
                                                             to feed audio encoder */

    /**
     * Writer stuff */
    M4OSA_Context             pWriterContext;     /**< Context of the writer module */
    M4OSA_Void*               pOutputFile;        /**< Output file to be created */
    M4OSA_Void*               pTemporaryFile;     /**< Temporary file to be created to store
                                                        metadata ("moov.bin") */
    M4SYS_StreamDescription   WriterVideoStream;  /**< Description of the written video stream */
    M4SYS_StreamDescription   WriterAudioStream;  /**< Description of the written audio stream */
    M4WRITER_StreamVideoInfos WriterVideoStreamInfo;/**< Video properties of the written video
                                                          stream */
    M4SYS_AccessUnit          WriterVideoAU;        /**< Written video access unit */
    M4SYS_AccessUnit          WriterAudioAU;        /**< Written audio access unit */
    M4OSA_UInt32              uiVideoAUCount;       /**< Number of video AU written in output
                                                          file */
    M4OSA_UInt32              uiVideoMaxAuSize;     /**< Max access unit size for the output
                                                          video stream */
    M4OSA_UInt32              uiVideoMaxChunckSize; /**< Max chunck size for the output video
                                                          stream */
    M4OSA_UInt32              uiAudioAUCount;   /**< Number of audio AU written in output file */
    M4OSA_UInt32              uiAudioMaxAuSize; /**< Max access unit size for the output
                                                       audio stream */
    M4OSA_UInt32              uiAudioCts;       /**< Audio AU cts (when audio is transcoded) */
    M4OSA_Bool                b_isRawWriter;    /**< Boolean to know if the raw writer is
                                                      registered or not */
    M4OSA_Context             pOutputPCMfile;   /**< Output PCM file if not NULL */

    /**
     * Filesystem functions */
    M4OSA_FileReadPointer*    pOsaFileReadPtr; /**< OSAL file read functions,
                                                    to be provided by user */
    M4OSA_FileWriterPointer*  pOsaFileWritPtr; /**< OSAL file write functions,
                                                    to be provided by user */

    /**
      * Media and Codec registration */
    /**< Table of M4VES_WriterInterface structures for avalaible Writers list */
    M4MCS_WriterInterface               WriterInterface[M4WRITER_kType_NB];
    /**< open, close, setoption,etc... functions of the used writer*/
    M4WRITER_GlobalInterface*           pWriterGlobalFcts;
    /**< data manipulation functions of the used writer */
    M4WRITER_DataInterface*             pWriterDataFcts;
    /**< Table of M4ENCODER_GlobalInterface structures for avalaible encoders list */
    M4ENCODER_GlobalInterface*          pVideoEncoderInterface[M4ENCODER_kVideo_NB];
    /**< Functions of the used encoder */
    M4ENCODER_GlobalInterface*          pVideoEncoderGlobalFcts;

    M4OSA_Void*                         pVideoEncoderExternalAPITable[M4ENCODER_kVideo_NB];
    M4OSA_Void*                         pCurrentVideoEncoderExternalAPI;
    M4OSA_Void*                         pVideoEncoderUserDataTable[M4ENCODER_kVideo_NB];
    M4OSA_Void*                         pCurrentVideoEncoderUserData;

    /**< Table of M4ENCODER_AudioGlobalInterface structures for avalaible encoders list */
    M4ENCODER_AudioGlobalInterface*     pAudioEncoderInterface[M4ENCODER_kAudio_NB];
    /**< Table of internal/external flags for avalaible encoders list */
    M4OSA_Bool                          pAudioEncoderFlag[M4ENCODER_kAudio_NB];
    /**< Functions of the used encoder */
    M4ENCODER_AudioGlobalInterface*     pAudioEncoderGlobalFcts;
    M4OSA_Void*                         pAudioEncoderUserDataTable[M4ENCODER_kAudio_NB];
    M4OSA_Void*                         pCurrentAudioEncoderUserData;

    M4READER_GlobalInterface*           m_pReaderGlobalItTable[M4READER_kMediaType_NB];
    M4READER_DataInterface*             m_pReaderDataItTable[M4READER_kMediaType_NB];
    M4READER_GlobalInterface*           m_pReader;
    M4READER_DataInterface*             m_pReaderDataIt;
    M4OSA_UInt8                         m_uiNbRegisteredReaders;

    M4DECODER_VideoInterface*           m_pVideoDecoder;
    M4DECODER_VideoInterface*           m_pVideoDecoderItTable[M4DECODER_kVideoType_NB];
#ifdef M4VSS_ENABLE_EXTERNAL_DECODERS
    M4OSA_Void*                         m_pCurrentVideoDecoderUserData;
    M4OSA_Void*                         m_pVideoDecoderUserDataTable[M4DECODER_kVideoType_NB];
#endif /* M4VSS_ENABLE_EXTERNAL_DECODERS */
    M4OSA_UInt8                         m_uiNbRegisteredVideoDec;

    M4AD_Interface*         m_pAudioDecoder;
    M4AD_Interface*         m_pAudioDecoderItTable[M4AD_kType_NB];
    M4OSA_Bool              m_pAudioDecoderFlagTable[M4AD_kType_NB]; /**< store indices of external
                                                                      decoders */
    M4OSA_Void*             m_pAudioDecoderUserDataTable[M4AD_kType_NB];
    M4OSA_Void*             m_pCurrentAudioDecoderUserData;

    M4MCS_MediaRendering    MediaRendering;     /**< FB: to crop, resize, or render black borders*/
    M4OSA_Context           m_air_context;
    M4OSA_Bool              bExtOMXAudDecoder;  /* External OMX Audio decoder */

    /**< FlB 2009.03.04: Audio effects*/
    M4MCS_EffectSettings    *pEffects;              /**< List of effects */
    M4OSA_UInt8             nbEffects;              /**< Number of effects in the above list */
    M4OSA_Int8              pActiveEffectNumber;    /**< Effect ID to be applied, if -1,
                                                       no effect has to be applied currently*/

#ifdef M4MCS_SUPPORT_STILL_PICTURE
    M4OSA_Bool              m_bIsStillPicture;       /**< =TRUE if input file is a still picture
                                                        (JPEG, PNG, BMP, GIF)*/
    M4MCS_Context           m_pStillPictureContext; /**< Context of the still picture part of MCS*/
#endif /*M4MCS_SUPPORT_STILL_PICTURE*/
    NSWAVC_MCS_t            *m_pInstance;
    M4OSA_UInt8             *H264MCSTempBuffer;
    M4OSA_UInt32            H264MCSTempBufferSize;
    M4OSA_UInt32            H264MCSTempBufferDataSize;
    M4OSA_Bool              bH264Trim;
    /* Flag when to get  lastdecodedframeCTS */
    M4OSA_Bool              bLastDecodedFrameCTS;
    M4OSA_Int32             encodingVideoProfile;
    M4OSA_Int32             encodingVideoLevel;

} M4MCS_InternalContext;


#endif /* __M4MCS_INTERNALTYPES_H__ */

