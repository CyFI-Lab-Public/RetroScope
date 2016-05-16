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
 * @brief  MCS internal constant values settings
 * @note   This header file is not public
 *************************************************************************
 **/

#ifndef __M4MCS_INTERNALCONFIG_H__
#define __M4MCS_INTERNALCONFIG_H__


/**
 * Definition of max AU size */
#define M4MCS_AUDIO_MAX_CHUNK_SIZE        7168 /**< add mp3 encoder and writer,
                                                    max bitrate is now 320kbps instead of 128kbps
                                                    so this value has to be increased accordingly
                                                    = ((sizeof(M4OSA_UInt8)*max_channel_number)+3
                                                    to take a margin(after tests, 2 was not enough
                                                    ))*MAX_PCM_GRANULARITY_SAMPLES*/
                                                    /**< Before: 4000*//**< Magical */

/**
 * Video max AU and fragment size */
#define M4MCS_VIDEO_MIN_COMPRESSION_RATIO   0.8 /**< Magical. Used to define the max AU size */
#define M4MCS_VIDEO_CHUNK_AU_SIZE_RATIO     1.2 /**< Magical. Used to define the max chunk size */

/**
 * Various Magicals */
#define M4MCS_WRITER_AUDIO_STREAM_ID        1
#define M4MCS_WRITER_VIDEO_STREAM_ID        2

/**
 * Granularity for audio encoder */
 /**< minimum number of samples to pass in AMR encoding case */
#define M4MCS_PCM_AMR_GRANULARITY_SAMPLES 160
/**< minimum number of samples to pass in AAC encoding case */
#define M4MCS_PCM_AAC_GRANULARITY_SAMPLES 1024
/**< minimum number of samples to pass in MP3 encoding case */
#define M4MCS_PCM_MP3_GRANULARITY_SAMPLES 576

#define M4MCS_AUDIO_MAX_AU_SIZE           1024  /**< add mp3 encoder and writer
                                                This value is not used anymore, now the max AU
                                                size is computed dynamically according to the
                                                number of channels,the max PCM granularity sample
                                                and a margin.*/
                                                /**< Before: 1024*//**< Magical */
/**
 * Writer file and moov size estimation */
#define M4MCS_MOOV_OVER_FILESIZE_RATIO    1.04  /**< magical moov size is less than 4%
                                                     of file size in average */

/**
 * If 3gp file does not contain an STSS table (no rap frames),
   jump backward to a specified limit */
#define M4MCS_NO_STSS_JUMP_POINT          40000 /**< 40 s */

#endif /* __M4MCS_INTERNALCONFIG_H__ */

