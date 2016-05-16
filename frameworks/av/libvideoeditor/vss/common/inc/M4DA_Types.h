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
 * @file    M4DA_Types.h
 * @brief    Data access type definition
 * @note    This file implements media specific types
 ************************************************************************
*/

#ifndef __M4DA_TYPES_H__
#define __M4DA_TYPES_H__

#include "NXPSW_CompilerSwitches.h"

#include "M4OSA_Types.h"
#include "M4OSA_Memory.h"

#ifdef __cplusplus
extern "C" {
#endif /*__cplusplus*/

/**
 ************************************************************************
 * enumeration    M4_StreamType
 * @brief        Type used to describe a stream (audio or video data flow).
 ************************************************************************
*/
typedef enum
{
    M4DA_StreamTypeUnknown                = -1,    /**< Unknow type */
    M4DA_StreamTypeVideoMpeg4            = 0,    /**< MPEG-4 video */
    M4DA_StreamTypeVideoH263            = 1,    /**< H263 video */
    M4DA_StreamTypeAudioAmrNarrowBand    = 2,    /**< Amr narrow band audio */
    M4DA_StreamTypeAudioAmrWideBand        = 3,    /**< Amr wide band audio */
    M4DA_StreamTypeAudioAac                = 4,    /**< AAC audio */
    M4DA_StreamTypeAudioMp3                = 5,    /**< MP3 audio */
    M4DA_StreamTypeVideoMJpeg            = 6,    /**< MJPEG video */
    M4DA_StreamTypeAudioPcm                = 7,    /**< Wav audio */
    M4DA_StreamTypeAudioMidi            = 8,    /**< Midi audio */
    M4DA_StreamTypeVideoMpeg4Avc        = 9,    /**< MPEG-4 AVC video (h264) */
    M4DA_StreamTypeAudioAacADTS            = 10,    /**< AAC ADTS audio */
    M4DA_StreamTypeAudioAacADIF            = 11,    /**< AAC ADIF audio */
    M4DA_StreamTypeAudioWma                = 12,    /**< WMA audio */
    M4DA_StreamTypeVideoWmv                = 13,    /**< WMV video */
    M4DA_StreamTypeAudioReal            = 14,   /**< REAL audio */
    M4DA_StreamTypeVideoReal            = 15,   /**< REAL video */
    M4DA_StreamTypeAudioEvrc            = 16,   /**< Evrc audio */
    M4DA_StreamTypeTimedText            = 20,    /**< Timed Text */
    M4DA_StreamTypeAudioBba                = 21,    /**< Beat Brew audio fomat */
    M4DA_StreamTypeAudioSmaf            = 22,    /**< SMAF audio */
    M4DA_StreamTypeAudioImelody            = 23,    /**< IMELODY audio*/
    M4DA_StreamTypeAudioXmf                = 24,    /**< XMF audio */
    M4DA_StreamTypeAudioBpc                = 25,    /**< BPC audio */

    /* ADPCM */
    M4DA_StreamTypeAudioADPcm            = 26,    /**< ADPCM */

    M4DA_StreamTypeVideoARGB8888        = 27
} M4_StreamType;

/**
 ************************************************************************
 * structure    M4_StreamHandler
 * @brief        Base structure to describe a stream.
 ************************************************************************
*/
typedef struct
{
    M4_StreamType    m_streamType;                /**< Stream type */
    M4OSA_UInt32    m_streamId;                    /**< Stream Id (unique number definning
                                                        the stream) */
    M4OSA_Int32        m_duration;                    /**< Duration of the stream in milli
                                                            seconds */
    M4OSA_UInt32    m_averageBitRate;            /**< Average bitrate in kb/s */
    M4OSA_UInt32    m_maxAUSize;                /**< Maximum size of an Access Unit */
    M4OSA_UInt8*    m_pDecoderSpecificInfo;        /**< Pointer on specific information required
                                                        to create a decoder */
    M4OSA_UInt32    m_decoderSpecificInfoSize;    /**< Size of the specific information
                                                         pointer above */
    void*            m_pUserData;                /**< Pointer on User Data
                                                    (initialized by the user) */
    M4OSA_UInt32    m_structSize;                /**< Size of the structure in bytes */
    M4OSA_Bool      m_bStreamIsOK;              /**< Flag to know if stream has no errors after
                                                        parsing is finished */
    M4OSA_UInt8*    m_pH264DecoderSpecificInfo;        /**< Pointer on specific information
                                                            required to create a decoder */
    M4OSA_UInt32    m_H264decoderSpecificInfoSize;    /**< Size of the specific
                                                            information pointer above */
    // MPEG4 & AAC decoders require ESDS info
    M4OSA_UInt8*    m_pESDSInfo;                /**< Pointer on MPEG4 or AAC ESDS box */
    M4OSA_UInt32    m_ESDSInfoSize;             /**< Size of the MPEG4 or AAC ESDS box */
} M4_StreamHandler;

/**
 ************************************************************************
 * structure    M4_VideoStreamHandler
 * @brief        Extended structure to describe a video stream.
 ************************************************************************
*/
typedef struct
{
    M4_StreamHandler    m_basicProperties;        /**< Audio-Video stream common parameters */
    M4OSA_UInt32        m_videoWidth;            /**< Width of the video in the stream */
    M4OSA_UInt32        m_videoHeight;            /**< Height of the video in the stream */
    M4OSA_Float            m_averageFrameRate;        /**< Average frame rate of the video
                                                            in the stream */
    M4OSA_Int32         videoRotationDegrees;        /**< Video rotation degree */
    M4OSA_UInt32        m_structSize;            /**< Size of the structure in bytes */
} M4_VideoStreamHandler;

/**
 ************************************************************************
 * structure    M4_AudioStreamHandler
 * @brief        Extended structure to describe an audio stream.
 ************************************************************************
*/
typedef struct
{
    M4_StreamHandler    m_basicProperties;        /**< Audio-Video stream common parameters */
    M4OSA_UInt32        m_nbChannels;            /**< Number of channels in the audio stream
                                                        (1-mono, 2-stereo) */
    M4OSA_UInt32        m_byteFrameLength;        /**< Size of frame samples in bytes */
    M4OSA_UInt32        m_byteSampleSize;        /**< Number of bytes per sample */
    M4OSA_UInt32        m_samplingFrequency;    /**< Sample frequency in kHz */
    M4OSA_UInt32        m_structSize;            /**< Size of the structure in bytes */
} M4_AudioStreamHandler;

#ifdef M4VPS_SUPPORT_TTEXT

/**
 ************************************************************************
 * structure    M4_TextStreamHandler
 * @brief        Extended structure to describe a text stream.
 ************************************************************************
*/
typedef struct
{
    M4_StreamHandler    m_basicProperties;    /**< Audio-Video stream common parameters */
    M4OSA_UInt32        m_trackWidth;        /**< Width of the video in the stream */
    M4OSA_UInt32        m_trackHeight;        /**< Height of the video in the stream */
    M4OSA_UInt32        m_trackXpos;        /**< X position of the text track in video area */
    M4OSA_UInt32        m_trackYpos;        /**< Y position of the text track in video area */
    M4OSA_UInt8            back_col_rgba[4];    /**< the background color in RGBA */
    M4OSA_UInt16        uiLenght;            /**< the string lenght in bytes */
    M4OSA_UInt32        disp_flag;            /**< the way text will be displayed */
    M4OSA_UInt8            horiz_justif;        /**< the horizontal justification of the text */
    M4OSA_UInt8            verti_justif;        /**< the vertical justification of the text */
    /* style */
    M4OSA_UInt16        styl_start_char;    /**< the first character impacted by style */
    M4OSA_UInt16        styl_end_char;        /**< the last character impacted by style */
    M4OSA_UInt16        fontID;                /**< ID of the font */
    M4OSA_UInt8            face_style;            /**< the text face-style: bold, italic,
                                                         underlined, plain(default) */
    M4OSA_UInt8            font_size;            /**< size in pixel of font */
    M4OSA_UInt8            text_col_rgba[4];    /**< the text color in RGBA */
    /* box */
    M4OSA_UInt16        box_top;         /**< the top position of text box in the track area */
    M4OSA_UInt16        box_left;        /**< the left position of text box in the track area */
    M4OSA_UInt16        box_bottom;      /**< the bottom position of text box in the track area */
    M4OSA_UInt16        box_right;       /**< the right position of text box in the track area */
    M4OSA_UInt32        m_structSize;    /**< Size of the structure in bytes */
} M4_TextStreamHandler;

#endif /*M4VPS_SUPPORT_TTEXT*/

/**
 ************************************************************************
 * structure    M4_AccessUnit
 * @brief        Structure to describe an access unit.
 ************************************************************************
*/
typedef struct
{
  M4OSA_UInt32            m_streamID;       /**< Id of the stream to get an AU from */
  M4OSA_MemAddr8        m_dataAddress;      /**< Pointer to a memory area with the encoded data */
  M4OSA_UInt32            m_size;           /**< Size of the dataAdress area */
  M4OSA_Double            m_CTS;            /**< Composition Time Stamp for the Access Unit */
  M4OSA_Double            m_DTS ;           /**< Decoded Time Stamp for the Access Unit */
  M4OSA_UInt8            m_attribute;       /**< RAP information & AU corrupted */
  M4OSA_UInt32            m_maxsize;        /**< Maximum size of the AU */
  M4OSA_UInt32            m_structSize;     /**< Structure size */
} M4_AccessUnit;

#ifdef __cplusplus
}
#endif /*__cplusplus*/

#endif /* __M4DA_TYPES_H__ */

