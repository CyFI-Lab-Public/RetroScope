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
 * @file    M4_VideoEditingCommon.h
 * @brief    Video Editing (VSS3GPP, MCS, PTO3GPP) common definitions
 * @note
 ******************************************************************************
*/

#ifndef __M4_VIDEOEDITINGCOMMON_H__
#define __M4_VIDEOEDITINGCOMMON_H__

#ifdef __cplusplus
extern "C" {
#endif

/**
 *    Version */
/* CHANGE_VERSION_HERE */
#define M4VIDEOEDITING_VERSION_MAJOR    3
#define M4VIDEOEDITING_VERSION_MINOR    1
#define M4VIDEOEDITING_VERSION_REVISION    0

#define M4VIDEOEDITING_VIDEO_UNKNOWN_PROFILE 0x7fffffff
#define M4VIDEOEDITING_VIDEO_UNKNOWN_LEVEL 0x7fffffff

/**
 ******************************************************************************
 * enum        M4VIDEOEDITING_FileType
 * @brief    This enum defines the file format type to be used
 ******************************************************************************
*/
typedef enum {
    M4VIDEOEDITING_kFileType_3GPP        = 0,      /**< 3GPP file media type : input & output */
    M4VIDEOEDITING_kFileType_MP4         = 1,      /**< MP4  file media type : input          */
    M4VIDEOEDITING_kFileType_AMR         = 2,      /**< AMR  file media type : input & output */
    M4VIDEOEDITING_kFileType_MP3         = 3,      /**< MP3  file media type : input          */
    M4VIDEOEDITING_kFileType_PCM         = 4,      /**< PCM RAW file media type : input    RC */
    M4VIDEOEDITING_kFileType_JPG         = 5,      /**< STILL PICTURE FEATURE: JPG file media
                                                        type : input AND OUTPUT */
    M4VIDEOEDITING_kFileType_BMP         = 6,      /**< STILL PICTURE FEATURE: BMP file media
                                                        type : input only */
    M4VIDEOEDITING_kFileType_GIF         = 7,      /**< STILL PICTURE FEATURE: GIF file media
                                                        type : input only */
    M4VIDEOEDITING_kFileType_PNG         = 8,      /**< STILL PICTURE FEATURE: PNG file media
                                                        type : input only */
    M4VIDEOEDITING_kFileType_ARGB8888    = 9,      /**< STILL PICTURE FEATURE: ARGB8888 file
                                                        media type : input only */
    M4VIDEOEDITING_kFileType_M4V         = 10,     /**< M4V  file media type : input only     */
    M4VIDEOEDITING_kFileType_Unsupported = 255     /**< Unsupported file media type           */
} M4VIDEOEDITING_FileType;


/**
 ******************************************************************************
 * enum        M4VIDEOEDITING_VideoFormat
 * @brief    This enum defines the avalaible video compression formats.
 ******************************************************************************
*/
typedef enum
{
    M4VIDEOEDITING_kNoneVideo = 0, /**< Video not present */
    M4VIDEOEDITING_kH263 = 1, /**< H263 video */
    M4VIDEOEDITING_kH264 = 2,    /**< H264 video */
    M4VIDEOEDITING_kMPEG4 = 3, /**< MPEG-4 video */
    M4VIDEOEDITING_kNullVideo = 254,  /**< Do not care video type, use NULL encoder */
    M4VIDEOEDITING_kUnsupportedVideo = 255    /**< Unsupported video stream type */
} M4VIDEOEDITING_VideoFormat;

/**
 ******************************************************************************
 * enum        M4VIDEOEDITING_AudioFormat
 * @brief    This enum defines the avalaible audio format.
 * @note    HE_AAC, HE_AAC_v2 and MP3 can not be used for the output audio format
 ******************************************************************************
*/
typedef enum {
    M4VIDEOEDITING_kNoneAudio            = 0,    /**< Audio not present */
    M4VIDEOEDITING_kAMR_NB              = 1,    /**< AMR Narrow Band audio */
    M4VIDEOEDITING_kAAC                    = 2,    /**< AAC audio */
    M4VIDEOEDITING_kAACplus                = 3,    /**< AAC+ audio */
    M4VIDEOEDITING_keAACplus             = 4,    /**< Enhanced AAC+ audio */
    M4VIDEOEDITING_kMP3                 = 5,    /**< MP3 audio */
    M4VIDEOEDITING_kEVRC                = 6,    /**< EVRC audio */
    M4VIDEOEDITING_kPCM                 = 7,    /**< PCM audio */
    M4VIDEOEDITING_kNullAudio           = 254,  /**< Do not care audio type, use NULL encoder */
    M4VIDEOEDITING_kUnsupportedAudio    = 255    /**< Unsupported audio stream type */
} M4VIDEOEDITING_AudioFormat;

/**
 ******************************************************************************
 * enum        M4VIDEOEDITING_VideoFrameSize
 * @brief    This enum defines the available output frame sizes.
 ******************************************************************************
*/
typedef enum
{
    M4VIDEOEDITING_kSQCIF=0, /**< SQCIF 128x96  */
    M4VIDEOEDITING_kQQVGA,   /**< QQVGA 160x120 */
    M4VIDEOEDITING_kQCIF,    /**< QCIF  176x144 */
    M4VIDEOEDITING_kQVGA,    /**< QVGA  320x240 */
    M4VIDEOEDITING_kCIF,     /**< CIF   352x288 */
    M4VIDEOEDITING_kVGA,     /**< VGA   640x480 */
/* +PR LV5807 */
    M4VIDEOEDITING_kWVGA,    /**< WVGA 800x480 */
    M4VIDEOEDITING_kNTSC,    /**< NTSC 720x480 */
/* -PR LV5807 */

/* +CR Google */
    M4VIDEOEDITING_k640_360,  /**< 640x360 */
    M4VIDEOEDITING_k854_480,  /**< 854x480 */
    M4VIDEOEDITING_k1280_720, /**< 720p 1280x720 */
    M4VIDEOEDITING_k1080_720, /**< 720p 1080x720 */
    M4VIDEOEDITING_k960_720,  /**< 720p 960x720 */
    M4VIDEOEDITING_k1920_1080 /**<1080p 1920x1080*/
/* -CR Google */

} M4VIDEOEDITING_VideoFrameSize;


/**
 ******************************************************************************
 * enum        M4VIDEOEDITING_Videoframerate
 * @brief    This enum defines the available video framerates.
 ******************************************************************************
*/
typedef enum
{
    M4VIDEOEDITING_k5_FPS = 0,
    M4VIDEOEDITING_k7_5_FPS,
    M4VIDEOEDITING_k10_FPS,
    M4VIDEOEDITING_k12_5_FPS,
    M4VIDEOEDITING_k15_FPS,
    M4VIDEOEDITING_k20_FPS,
    M4VIDEOEDITING_k25_FPS,
    M4VIDEOEDITING_k30_FPS
} M4VIDEOEDITING_VideoFramerate;


/**
 ******************************************************************************
 * enum        M4VIDEOEDITING_AudioSamplingFrequency
 * @brief    This enum defines the available output audio sampling frequencies
 * @note    8 kHz is the only supported frequency for AMR-NB output
 * @note    16 kHz is the only supported frequency for AAC output
 * @note    The recommended practice is to use the Default value when setting the encoding parameters
 ******************************************************************************
*/
typedef enum {
    M4VIDEOEDITING_kDefault_ASF    = 0,    /**< Default Audio Sampling Frequency for selected
                                                 Audio output format */
    M4VIDEOEDITING_k8000_ASF    = 8000,    /**< Note: Default audio Sampling Frequency for
                                                    AMR-NB output */
    M4VIDEOEDITING_k11025_ASF    = 11025,
    M4VIDEOEDITING_k12000_ASF    = 12000,
    M4VIDEOEDITING_k16000_ASF    = 16000,    /**< Note: Default audio Sampling Frequency
                                                     for AAC output */
    M4VIDEOEDITING_k22050_ASF    = 22050,
    M4VIDEOEDITING_k24000_ASF    = 24000,
    M4VIDEOEDITING_k32000_ASF    = 32000,
    M4VIDEOEDITING_k44100_ASF    = 44100,
    M4VIDEOEDITING_k48000_ASF    = 48000

} M4VIDEOEDITING_AudioSamplingFrequency;


/**
 ******************************************************************************
 * enum        M4VIDEOEDITING_Bitrate
 * @brief    This enum defines the available audio or video bitrates.
 ******************************************************************************
*/
typedef enum
{
    M4VIDEOEDITING_kVARIABLE_KBPS = -1,     /* no regulation */
    M4VIDEOEDITING_kUndefinedBitrate = 0,   /* undefined */
    M4VIDEOEDITING_k8_KBPS = 8000,
    M4VIDEOEDITING_k9_2_KBPS = 9200,        /* evrc only */
    M4VIDEOEDITING_k12_2_KBPS = 12200,      /* amr only */
    M4VIDEOEDITING_k16_KBPS = 16000,
    M4VIDEOEDITING_k24_KBPS = 24000,
    M4VIDEOEDITING_k32_KBPS = 32000,
    M4VIDEOEDITING_k40_KBPS = 40000,
    M4VIDEOEDITING_k48_KBPS = 48000,
    M4VIDEOEDITING_k56_KBPS = 56000,
    M4VIDEOEDITING_k64_KBPS = 64000,
    M4VIDEOEDITING_k80_KBPS = 80000,
    M4VIDEOEDITING_k96_KBPS = 96000,
    M4VIDEOEDITING_k112_KBPS = 112000,
    M4VIDEOEDITING_k128_KBPS = 128000,
    M4VIDEOEDITING_k160_KBPS = 160000,
    M4VIDEOEDITING_k192_KBPS = 192000,
    M4VIDEOEDITING_k224_KBPS = 224000,
    M4VIDEOEDITING_k256_KBPS = 256000,
    M4VIDEOEDITING_k288_KBPS = 288000,
    M4VIDEOEDITING_k320_KBPS = 320000,
    M4VIDEOEDITING_k384_KBPS = 384000,
    M4VIDEOEDITING_k512_KBPS = 512000,
    M4VIDEOEDITING_k800_KBPS = 800000,
/*+ New Encoder bitrates */
    M4VIDEOEDITING_k2_MBPS = 2000000,
    M4VIDEOEDITING_k5_MBPS = 5000000,
    M4VIDEOEDITING_k8_MBPS = 8000000,
/*- New Encoder bitrates */
} M4VIDEOEDITING_Bitrate;


/**
 ******************************************************************************
 * structure    M4VIDEOEDITING_FtypBox
 * @brief        Information to build the 'ftyp' atom
 ******************************************************************************
*/
#define M4VIDEOEDITING_MAX_COMPATIBLE_BRANDS 10
typedef struct
{
    /* All brand fields are actually char[4] stored in big-endian integer format */

    M4OSA_UInt32    major_brand;           /* generally '3gp4'            */
    M4OSA_UInt32    minor_version;         /* generally '0000' or 'x.x '  */
    M4OSA_UInt32    nbCompatibleBrands;    /* number of compatible brands */
    M4OSA_UInt32    compatible_brands[M4VIDEOEDITING_MAX_COMPATIBLE_BRANDS]; /* array of
                                                                         max compatible brands */

} M4VIDEOEDITING_FtypBox;

/* Some useful brands */
#define M4VIDEOEDITING_BRAND_0000  0x00000000
#define M4VIDEOEDITING_BRAND_3G2A  0x33673261
#define M4VIDEOEDITING_BRAND_3GP4  0x33677034
#define M4VIDEOEDITING_BRAND_3GP5  0x33677035
#define M4VIDEOEDITING_BRAND_3GP6  0x33677036
#define M4VIDEOEDITING_BRAND_AVC1  0x61766331
#define M4VIDEOEDITING_BRAND_EMP   0x656D7020
#define M4VIDEOEDITING_BRAND_ISOM  0x69736F6D
#define M4VIDEOEDITING_BRAND_MP41  0x6D703431
#define M4VIDEOEDITING_BRAND_MP42  0x6D703432
#define M4VIDEOEDITING_BRAND_VFJ1  0x76666A31

/**
 ******************************************************************************
 * enum     M4VIDEOEDITING_ClipProperties
 * @brief   This structure gathers the information related to an input file
 ******************************************************************************
*/
typedef struct {

    /**
     * Common */
    M4OSA_Bool                          bAnalysed;           /**< Flag to know if the file has
                                                                  been already analysed or not */
    M4OSA_UInt8                         Version[3];          /**< Version of the libraries used to
                                                                  perform the clip analysis */
    M4OSA_UInt32                        uiClipDuration;      /**< Clip duration (in ms) */
    M4VIDEOEDITING_FileType             FileType;            /**< .3gp, .amr, .mp3 */
    M4VIDEOEDITING_FtypBox              ftyp;                /**< 3gp 'ftyp' atom, major_brand =
                                                                    0 if not used */

    /**
     * Video */
    M4VIDEOEDITING_VideoFormat          VideoStreamType;     /**< Format of the video stream */
    M4OSA_UInt32                        uiClipVideoDuration; /**< Video track duration (in ms) */
    M4OSA_UInt32                        uiVideoBitrate;      /**< Video average bitrate (in bps)*/
    M4OSA_UInt32                        uiVideoMaxAuSize;    /**< Maximum Access Unit size of the
                                                                  video stream */
    M4OSA_UInt32                        uiVideoWidth;        /**< Video frame width */
    M4OSA_UInt32                        uiVideoHeight;       /**< Video frame height */
    M4OSA_UInt32                        uiVideoTimeScale;    /**< Video time scale */
    M4OSA_Float                         fAverageFrameRate;   /**< Average frame rate of the video
                                                                  stream */
    M4OSA_Int32 uiVideoLevel;   /**< video level*/
    M4OSA_Int32 uiVideoProfile; /**< video profile */

    M4OSA_Bool                          bMPEG4dataPartition; /**< MPEG-4 uses data partitioning */
    M4OSA_Bool                          bMPEG4rvlc;          /**< MPEG-4 uses RVLC tool */
    M4OSA_Bool                          bMPEG4resynchMarker; /**< MPEG-4 stream uses Resynch
                                                                   Marker */

    /**
     * Audio */
    M4VIDEOEDITING_AudioFormat          AudioStreamType;     /**< Format of the audio stream */
    M4OSA_UInt32                        uiClipAudioDuration; /**< Audio track duration (in ms) */
    M4OSA_UInt32                        uiAudioBitrate;      /**< Audio average bitrate (in bps) */
    M4OSA_UInt32                        uiAudioMaxAuSize;    /**< Maximum Access Unit size of the
                                                                    audio stream */
    M4OSA_UInt32                        uiNbChannels;        /**< Number of channels
                                                                    (1=mono, 2=stereo) */
    M4OSA_UInt32                        uiSamplingFrequency; /**< Sampling audio frequency
                                                           (8000 for amr, 16000 or more for aac) */
    M4OSA_UInt32                        uiExtendedSamplingFrequency; /**< Extended frequency for
                                                                         AAC+, eAAC+ streams */
    M4OSA_UInt32                        uiDecodedPcmSize;    /**< Size of the decoded PCM data */

    /**
     * Video editing compatibility chart */
    M4OSA_Bool      bVideoIsEditable;                        /**< Video stream can be decoded and
                                                                 re-encoded */
    M4OSA_Bool      bAudioIsEditable;                        /**< Audio stream can be decoded and
                                                                  re-encoded */
    M4OSA_Bool      bVideoIsCompatibleWithMasterClip;        /**< Video properties match reference
                                                                  clip properties */
    M4OSA_Bool      bAudioIsCompatibleWithMasterClip;        /**< Audio properties match reference
                                                                   clip properties */

    /**
     * Still Picture */
    M4OSA_UInt32                        uiStillPicWidth;        /**< Image width */
    M4OSA_UInt32                        uiStillPicHeight;       /**< Image height */
    M4OSA_UInt32                        uiClipAudioVolumePercentage;
    M4OSA_Bool                          bSetImageData;

    M4OSA_Int32     videoRotationDegrees;        /**< Video rotation degree */

} M4VIDEOEDITING_ClipProperties;


#ifdef __cplusplus
    }
#endif

#endif /* __M4_VIDEOEDITINGCOMMON_H__ */

