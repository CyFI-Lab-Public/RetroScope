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
 * @file    M4VSS3GPP_ErrorCodes.h
 * @brief    Video Studio Service 3GPP error definitions.
 * @note
 ******************************************************************************
 */

#ifndef __M4VSS3GPP_ErrorCodes_H__
#define __M4VSS3GPP_ErrorCodes_H__

/**
 *    OSAL basic types and errors */
#include "M4OSA_Types.h"
#include "M4OSA_Error.h"

/**
 *    OSAL core ID definitions */
#include "M4OSA_CoreID.h"


/************************************************************************/
/* Warning codes                                                        */
/************************************************************************/

/**
 *    End of edition, user should now call M4VSS3GPP_editClose() */
#define M4VSS3GPP_WAR_EDITING_DONE             M4OSA_ERR_CREATE( M4_WAR, M4VSS3GPP, 0x0001)

/**
 *    End of audio mixing, user should now call M4VSS3GPP_audioMixingCleanUp() */
#define M4VSS3GPP_WAR_END_OF_AUDIO_MIXING      M4OSA_ERR_CREATE( M4_WAR, M4VSS3GPP, 0x0010)

/**
 *    End of extract picture, user should now call M4VSS3GPP_extractPictureCleanUp() */
#define M4VSS3GPP_WAR_END_OF_EXTRACT_PICTURE   M4OSA_ERR_CREATE( M4_WAR, M4VSS3GPP, 0x0020)
/* RC: to know when a file has been processed */
#define M4VSS3GPP_WAR_SWITCH_CLIP              M4OSA_ERR_CREATE( M4_WAR, M4VSS3GPP, 0x0030)

/************************************************************************/
/* Error codes                                                          */
/************************************************************************/

/**
 * Invalid file type */
#define M4VSS3GPP_ERR_INVALID_FILE_TYPE               M4OSA_ERR_CREATE( M4_ERR, M4VSS3GPP, 0x0001)
/**
 * Invalid effect kind */
#define M4VSS3GPP_ERR_INVALID_EFFECT_KIND             M4OSA_ERR_CREATE( M4_ERR, M4VSS3GPP, 0x0002)
/**
 * Invalid effect type for video */
#define M4VSS3GPP_ERR_INVALID_VIDEO_EFFECT_TYPE       M4OSA_ERR_CREATE( M4_ERR, M4VSS3GPP, 0x0003)
/**
 * Invalid effect type for audio */
#define M4VSS3GPP_ERR_INVALID_AUDIO_EFFECT_TYPE       M4OSA_ERR_CREATE( M4_ERR, M4VSS3GPP, 0x0004)
/**
 * Invalid transition type for video */
#define M4VSS3GPP_ERR_INVALID_VIDEO_TRANSITION_TYPE   M4OSA_ERR_CREATE( M4_ERR, M4VSS3GPP, 0x0005)
/**
 * Invalid transition type for audio */
#define M4VSS3GPP_ERR_INVALID_AUDIO_TRANSITION_TYPE   M4OSA_ERR_CREATE( M4_ERR, M4VSS3GPP, 0x0006)
/**
 * Invalid video encoding frame rate */
#define M4VSS3GPP_ERR_INVALID_VIDEO_ENCODING_FRAME_RATE        \
                                      M4OSA_ERR_CREATE( M4_ERR, M4VSS3GPP, 0x0007)
 /**
 * External effect function is used without being set */
#define M4VSS3GPP_ERR_EXTERNAL_EFFECT_NULL            M4OSA_ERR_CREATE( M4_ERR, M4VSS3GPP, 0x0008)
/**
 * External transition function is used without being set */
#define M4VSS3GPP_ERR_EXTERNAL_TRANSITION_NULL        M4OSA_ERR_CREATE( M4_ERR, M4VSS3GPP, 0x0009)

/**
 * Begin cut time is larger than the clip duration */
#define M4VSS3GPP_ERR_BEGIN_CUT_LARGER_THAN_DURATION  M4OSA_ERR_CREATE( M4_ERR, M4VSS3GPP, 0x0010)
/**
 * Begin cut time is larger or equal than end cut */
#define M4VSS3GPP_ERR_BEGIN_CUT_LARGER_THAN_END_CUT   M4OSA_ERR_CREATE( M4_ERR, M4VSS3GPP, 0x0011)
/**
 * Two consecutive transitions are overlapping on one clip */
#define M4VSS3GPP_ERR_OVERLAPPING_TRANSITIONS         M4OSA_ERR_CREATE( M4_ERR, M4VSS3GPP, 0x0012)

/**
 * An input 3GPP file is invalid/corrupted */
#define M4VSS3GPP_ERR_INVALID_3GPP_FILE               M4OSA_ERR_CREATE( M4_ERR, M4VSS3GPP, 0x0016)
/**
 * A file contains an unsupported video format */
#define M4VSS3GPP_ERR_UNSUPPORTED_INPUT_VIDEO_FORMAT  M4OSA_ERR_CREATE( M4_ERR, M4VSS3GPP, 0x0017)
/**
 * A file contains an unsupported audio format */
#define M4VSS3GPP_ERR_UNSUPPORTED_INPUT_AUDIO_FORMAT  M4OSA_ERR_CREATE( M4_ERR, M4VSS3GPP, 0x0018)

/**
 * A file format is not supported by the VSS */
#define M4VSS3GPP_ERR_AMR_EDITING_UNSUPPORTED         M4OSA_ERR_CREATE( M4_ERR, M4VSS3GPP, 0x0019)
 /**
 *    An input clip has an unexpectedly large Video AU */
#define M4VSS3GPP_ERR_INPUT_VIDEO_AU_TOO_LARGE        M4OSA_ERR_CREATE( M4_ERR, M4VSS3GPP, 0x001A)
/**
 *    An input clip has an unexpectedly large Audio AU */
#define M4VSS3GPP_ERR_INPUT_AUDIO_AU_TOO_LARGE        M4OSA_ERR_CREATE( M4_ERR, M4VSS3GPP, 0x001B)
/**
 *    An input clip has a corrupted Audio AMR AU */
#define M4VSS3GPP_ERR_INPUT_AUDIO_CORRUPTED_AU       M4OSA_ERR_CREATE( M4_ERR, M4VSS3GPP, 0x001C)
/**
 * The video encoder encountered an Acces Unit error: very probably a file write error */
#define M4VSS3GPP_ERR_ENCODER_ACCES_UNIT_ERROR       M4OSA_ERR_CREATE( M4_ERR, M4VSS3GPP, 0x001D)


/************************************************************************/
/* Errors returned by M4VSS3GPP_editAnalyseClip()                       */
/************************************************************************/

/**
 * Unsupported video format for Video Editing */
#define M4VSS3GPP_ERR_EDITING_UNSUPPORTED_VIDEO_FORMAT M4OSA_ERR_CREATE( M4_ERR, M4VSS3GPP, 0x0020)
/**
 * Unsupported H263 profile for Video Editing */
#define M4VSS3GPP_ERR_EDITING_UNSUPPORTED_H263_PROFILE M4OSA_ERR_CREATE( M4_ERR, M4VSS3GPP, 0x0021)
/**
 * Unsupported MPEG-4 profile for Video Editing */
#define M4VSS3GPP_ERR_EDITING_UNSUPPORTED_MPEG4_PROFILE    \
                                             M4OSA_ERR_CREATE( M4_ERR, M4VSS3GPP, 0x0022)
/**
 * Unsupported MPEG-4 RVLC tool for Video Editing */
#define M4VSS3GPP_ERR_EDITING_UNSUPPORTED_MPEG4_RVLC   M4OSA_ERR_CREATE( M4_ERR, M4VSS3GPP, 0x0023)
/**
 * Unsupported audio format for Video Editing */
#define M4VSS3GPP_ERR_EDITING_UNSUPPORTED_AUDIO_FORMAT M4OSA_ERR_CREATE( M4_ERR, M4VSS3GPP, 0x0024)
 /**
 * File contains no supported stream */
#define M4VSS3GPP_ERR_EDITING_NO_SUPPORTED_STREAM_IN_FILE    M4OSA_ERR_CREATE( M4_ERR,\
                                                                            M4VSS3GPP, 0x0025)
/**
 * File contains no video stream or an unsupported video stream */
#define M4VSS3GPP_ERR_EDITING_NO_SUPPORTED_VIDEO_STREAM_IN_FILE    M4OSA_ERR_CREATE( M4_ERR,\
                                                                                M4VSS3GPP, 0x0026)
/**
 * Unsupported video profile for Video Editing */
#define M4VSS3GPP_ERR_EDITING_UNSUPPORTED_VIDEO_PROFILE M4OSA_ERR_CREATE( M4_ERR, M4VSS3GPP, 0x0027)

/**
 * Unsupported video profile for Video Editing */
#define M4VSS3GPP_ERR_EDITING_UNSUPPORTED_VIDEO_LEVEL M4OSA_ERR_CREATE( M4_ERR, M4VSS3GPP, 0x0028)

/************************************************************************/
/* Errors returned by M4VSS3GPP_editCheckClipCompatibility()            */
/************************************************************************/

/**
 * At least one of the clip analysis has been generated by another version of the VSS 3GPP */
#define M4VSS3GPP_ERR_INVALID_CLIP_ANALYSIS_VERSION   M4OSA_ERR_CREATE( M4_ERR, M4VSS3GPP, 0x0030)
/**
 * Clips don't have the same video format (H263 or MPEG4) */
#define M4VSS3GPP_ERR_INCOMPATIBLE_VIDEO_FORMAT       M4OSA_ERR_CREATE( M4_ERR, M4VSS3GPP, 0x0031)
/**
 *    Clips don't have the same frame size */
#define M4VSS3GPP_ERR_INCOMPATIBLE_VIDEO_FRAME_SIZE   M4OSA_ERR_CREATE( M4_ERR, M4VSS3GPP, 0x0032)
/**
 *    Clips don't have the same MPEG-4 time scale */
#define M4VSS3GPP_ERR_INCOMPATIBLE_VIDEO_TIME_SCALE   M4OSA_ERR_CREATE( M4_ERR, M4VSS3GPP, 0x0033)
/**
 *    Clips don't have the same use of MPEG-4 data partitioning */
#define M4VSS3GPP_ERR_INCOMPATIBLE_VIDEO_DATA_PARTITIONING    M4OSA_ERR_CREATE( M4_ERR,\
                                                                              M4VSS3GPP, 0x0034)
/**
 *    MP3 clips can't be assembled */
#define M4VSS3GPP_ERR_UNSUPPORTED_MP3_ASSEMBLY        M4OSA_ERR_CREATE( M4_ERR, M4VSS3GPP, 0x0035)
/**
 *  Clips don't have the same audio stream type (ex: AMR != AAC) */
#define M4VSS3GPP_WAR_INCOMPATIBLE_AUDIO_STREAM_TYPE  M4OSA_ERR_CREATE( M4_WAR, M4VSS3GPP, 0x0036)
/**
 *  Clips don't have the same audio number of channels (ex: stereo != mono) */
#define M4VSS3GPP_WAR_INCOMPATIBLE_AUDIO_NB_OF_CHANNELS        M4OSA_ERR_CREATE( M4_WAR,\
                                                                            M4VSS3GPP, 0x0037)
/**
 *  Clips don't have the same sampling frequency (ex: 44100Hz != 16000Hz) */
#define M4VSS3GPP_WAR_INCOMPATIBLE_AUDIO_SAMPLING_FREQUENCY    M4OSA_ERR_CREATE( M4_WAR,\
                                                                              M4VSS3GPP, 0x0038)

/************************************************************************/
/* Audio mixing error codes                                            */
/************************************************************************/

/**
 * The input 3GPP file does not contain any supported audio or video track */
#define M4VSS3GPP_ERR_NO_SUPPORTED_STREAM_IN_FILE     M4OSA_ERR_CREATE( M4_ERR, M4VSS3GPP, 0x0050)
/**
 * The Volume of the added audio track (AddVolume) must be strictly superior than zero */
#define M4VSS3GPP_ERR_ADDVOLUME_EQUALS_ZERO           M4OSA_ERR_CREATE( M4_ERR, M4VSS3GPP, 0x0051)
/**
 * The time at which the audio track is added (AddCts) can't be superior than the
   input video track duration */
#define M4VSS3GPP_ERR_ADDCTS_HIGHER_THAN_VIDEO_DURATION        M4OSA_ERR_CREATE( M4_ERR,\
                                                                            M4VSS3GPP, 0x0052)
/**
 * The audio track file format setting is undefined */
#define M4VSS3GPP_ERR_UNDEFINED_AUDIO_TRACK_FILE_FORMAT        M4OSA_ERR_CREATE( M4_ERR,\
                                                                            M4VSS3GPP, 0x0053)
/**
 * The added audio track stream has an unsupported format */
#define M4VSS3GPP_ERR_UNSUPPORTED_ADDED_AUDIO_STREAM   M4OSA_ERR_CREATE( M4_ERR, M4VSS3GPP, 0x0054)
/**
 * The audio mixing feature doesn't support EVRC, MP3 audio tracks */
#define M4VSS3GPP_ERR_AUDIO_MIXING_UNSUPPORTED         M4OSA_ERR_CREATE( M4_ERR, M4VSS3GPP, 0x0055)
/**
 * An added audio track limit the available features: uiAddCts must be 0
   and bRemoveOriginal must be M4OSA_TRUE */
#define M4VSS3GPP_ERR_FEATURE_UNSUPPORTED_WITH_AUDIO_TRACK  M4OSA_ERR_CREATE( M4_ERR,\
                                                                              M4VSS3GPP, 0x0056)
/**
 * Input audio track is not AMR-NB nor AAC so it can't be mixed with output */
#define M4VSS3GPP_ERR_AUDIO_CANNOT_BE_MIXED                    M4OSA_ERR_CREATE( M4_ERR,\
                                                                              M4VSS3GPP, 0x0057)
/**
 * Input clip must be a 3gpp file */
#define M4VSS3GPP_ERR_INPUT_CLIP_IS_NOT_A_3GPP              M4OSA_ERR_CREATE( M4_ERR,\
                                                                              M4VSS3GPP, 0x0058)
/**
 * Begin loop time is higher than end loop time or higher than added clip duration */
#define M4VSS3GPP_ERR_BEGINLOOP_HIGHER_ENDLOOP              M4OSA_ERR_CREATE( M4_ERR,\
                                                                              M4VSS3GPP, 0x0059)


/************************************************************************/
/* Audio mixing and extract picture error code                          */
/************************************************************************/

/**
 * H263 Profile 3 level 10 is not supported */
#define M4VSS3GPP_ERR_H263_PROFILE_NOT_SUPPORTED            M4OSA_ERR_CREATE( M4_ERR,\
                                                                            M4VSS3GPP, 0x0060)
/**
 * File contains no video stream or an unsupported video stream */
#define M4VSS3GPP_ERR_NO_SUPPORTED_VIDEO_STREAM_IN_FILE        M4OSA_ERR_CREATE( M4_ERR,\
                                                                            M4VSS3GPP, 0x0061)


/************************************************************************/
/* Internal error and warning codes                                     */
/************************************************************************/

/**
 * Internal state error */
#define M4VSS3GPP_ERR_INTERNAL_STATE                 M4OSA_ERR_CREATE( M4_ERR, M4VSS3GPP, 0x0100)
/**
 * Luminance filter effect error */
#define M4VSS3GPP_ERR_LUMA_FILTER_ERROR              M4OSA_ERR_CREATE( M4_ERR, M4VSS3GPP, 0x0104)
/**
 * Transition filter effect error */
#define M4VSS3GPP_ERR_TRANSITION_FILTER_ERROR        M4OSA_ERR_CREATE( M4_ERR, M4VSS3GPP, 0x0106)
/**
 * The audio decoder initialization failed */
#define M4VSS3GPP_ERR_AUDIO_DECODER_INIT_FAILED      M4OSA_ERR_CREATE( M4_ERR, M4VSS3GPP, 0x0110)
/**
 * The decoder produced an unattended amount of PCM */
#define M4VSS3GPP_ERR_AUDIO_DECODED_PCM_SIZE_ISSUE   M4OSA_ERR_CREATE( M4_ERR, M4VSS3GPP, 0x0115)
/**
 * Output file must be 3GPP or MP3 */
#define M4VSS3GPP_ERR_OUTPUT_FILE_TYPE_ERROR         M4OSA_ERR_CREATE( M4_ERR, M4VSS3GPP, 0x0117)
/**
 * Can not find a valid video frame */
#define M4VSS3GPP_ERR_NO_VALID_VID_FRAME         M4OSA_ERR_CREATE( M4_ERR, M4VSS3GPP, 0x0118)

#endif /* __M4VSS3GPP_ErrorCodes_H__ */

