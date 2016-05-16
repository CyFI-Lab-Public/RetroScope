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

#ifndef __M4VSS3GPP_INTERNALCONFIG_H__
#define __M4VSS3GPP_INTERNALCONFIG_H__

/**
 ******************************************************************************
 * @file    M4VSS3GPP_InternalConfig.h
 * @brief    This file contains some magical and configuration parameters.
 ******************************************************************************
*/

/***********************/
/* VideoEdition config */
/***********************/

#define M4VSS3GPP_MINIMAL_TRANSITION_DURATION            100    /**< 100 milliseconds */
#define M4VSS3GPP_NB_AU_PREFETCH                        4        /**< prefect 4 AUs */
#define M4VSS3GPP_NO_STSS_JUMP_POINT                    40000 /**< If 3gp file does not contain
                                                                   an STSS table (no rap frames),
                                                                   jump backward 40 s maximum */

/*****************/
/* Writer config */
/*****************/

#define M4VSS3GPP_WRITER_AUDIO_STREAM_ID                1
#define M4VSS3GPP_WRITER_VIDEO_STREAM_ID                2

/**< Max AU size will be 0.8 times the YUV4:2:0 frame size */
#define M4VSS3GPP_VIDEO_MIN_COMPRESSION_RATIO            0.9F
/**< Max chunk size will be 1.2 times the max AU size */
#define M4VSS3GPP_VIDEO_AU_SIZE_TO_CHUNCK_SIZE_RATIO    1.2F

/** READ CAREFULLY IN CASE OF REPORTED RUNNING TROUBLES
The max AU size is used to pre-set max size of AU that can be written in the 3GP writer
For audio standard with variable AU size, there could be some encoding settings leading to AU size
exceeding this limit.
For AAC streams for instance the average AU size is given by:
av AU size = (av bitrate * 1024)/(sampling freq)
If VSS returns the message:
>> ERROR: audio AU size (XXXX) to copy larger than allocated one (YYYY) => abort
>> PLEASE CONTACT SUPPORT TO EXTEND MAX AU SIZE IN THE PRODUCT LIBRARY
Error is most likely to happen when mixing with audio full replacement
 */
/**< AAC max AU size - READ EXPLANATION ABOVE */
#define M4VSS3GPP_AUDIO_MAX_AU_SIZE                        2048
/**< set to x4 max AU size per chunk */
#define M4VSS3GPP_AUDIO_MAX_CHUNCK_SIZE                    8192


/***********************/
/* H263 / MPEG4 config */
/***********************/

#define    M4VSS3GPP_EDIT_H263_MODULO_TIME            255

#ifdef BIG_ENDIAN
/**< 0xb3 01 00 00 Little endian / b00 00 00 01 b3 big endian*/
#define    M4VSS3GPP_EDIT_GOV_HEADER            0x000001b3
#else
/**< 0xb3 01 00 00 Little endian / b00 00 00 01 b3 big endian*/
#define    M4VSS3GPP_EDIT_GOV_HEADER            0xb3010000
#endif

/**************/
/* AMR config */
/**************/

#define M4VSS3GPP_WRITTEN_AMR_TRACK_TIME_SCALE            8000
#define M4VSS3GPP_AMR_DECODED_PCM_SAMPLE_NUMBER            160        /**< 20ms at 8000hz -->
                                                                     20x8=160 samples */
#define M4VSS3GPP_AMR_DEFAULT_BITRATE                   12200   /**< 12.2 kbps */

/**************/
/* EVRC config */
/**************/

#define M4VSS3GPP_EVRC_DEFAULT_BITRATE                  9200   /**< 9.2 kbps */

/**************/
/* MP3 config */
/**************/

/** Macro to make a jump on the MP3 track on several steps
    To avoid to block the system with an long MP3 jump, this process
    is divided on several steps.
 */
#define M4VSS3GPP_MP3_JUMPED_AU_NUMBER_MAX 100

/** Macro to define the number of read AU to analyse the bitrate
    So the process will read the first n AU of the MP3 stream to get
    the average bitrate. n is defined by this define.
 */
#define M4VSS3GPP_MP3_AU_NUMBER_MAX 500

/*****************************/
/* define AMR silence frames */
/*****************************/

#define M4VSS3GPP_AMR_AU_SILENCE_FRAME_048_SIZE     13
#define M4VSS3GPP_AMR_AU_SILENCE_FRAME_048_DURATION 160

#ifdef M4VSS3GPP_SILENCE_FRAMES
const M4OSA_UInt8 M4VSS3GPP_AMR_AU_SILENCE_FRAME_048[M4VSS3GPP_AMR_AU_SILENCE_FRAME_048_SIZE] =
{
    0x04, 0xFF, 0x18, 0xC7, 0xF0, 0x0D, 0x04, 0x33,
    0xFF, 0xE0, 0x00, 0x00, 0x00
};
#else
extern const M4OSA_UInt8 \
              M4VSS3GPP_AMR_AU_SILENCE_FRAME_048[M4VSS3GPP_AMR_AU_SILENCE_FRAME_048_SIZE];
#endif

/*****************************/
/* define AAC silence frames */
/*****************************/

#define M4VSS3GPP_AAC_AU_SILENCE_MONO_SIZE      4

#ifdef M4VSS3GPP_SILENCE_FRAMES
const M4OSA_UInt8 M4VSS3GPP_AAC_AU_SILENCE_MONO[M4VSS3GPP_AAC_AU_SILENCE_MONO_SIZE] =
{
    0x00, 0xC8, 0x20, 0x07
};
#else
extern const M4OSA_UInt8 M4VSS3GPP_AAC_AU_SILENCE_MONO[M4VSS3GPP_AAC_AU_SILENCE_MONO_SIZE];
#endif

#define M4VSS3GPP_AAC_AU_SILENCE_STEREO_SIZE        6

#ifdef M4VSS3GPP_SILENCE_FRAMES
const M4OSA_UInt8 M4VSS3GPP_AAC_AU_SILENCE_STEREO[M4VSS3GPP_AAC_AU_SILENCE_STEREO_SIZE] =
{
    0x21, 0x10, 0x03, 0x20, 0x54, 0x1C
};
#else
extern const M4OSA_UInt8 M4VSS3GPP_AAC_AU_SILENCE_STEREO[M4VSS3GPP_AAC_AU_SILENCE_STEREO_SIZE];
#endif

#endif /* __M4VSS3GPP_INTERNALCONFIG_H__ */

