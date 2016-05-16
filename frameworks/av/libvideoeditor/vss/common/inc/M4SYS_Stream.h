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
 * @file         M4SYS_Stream.h
 * @brief        Stream manipulation
 * @note         This file defines the stream structure.
 ************************************************************************
*/

#ifndef M4SYS_STREAM_H
#define M4SYS_STREAM_H

#include "M4OSA_Types.h"
#include "M4OSA_Memory.h"
#include "M4OSA_Time.h"

typedef M4OSA_UInt32 M4SYS_StreamID;

/** The streamType type provides a way to distinguish all streams (AAC, AMR, YUV420, MPEG-4 Video,
     H263). Stream types can be sorted in 2 ways:
@arg   Some of them are raw data, others are encoded
@arg   Some of them are related to an audio media, a video media...
@n So a specific naming convention has been designed to allow a quick parsing of the streamType
    value to return the above categories. StreamType is an un-signed integer on 16 bits.
@arg   The first byte (MSB) defines the codec type. It can be either Audio,Video, Picture,
         Text or Scene.
@arg   The second byte (LSB) defines the sub-codecs type (ie YUV420, PCM_16 bits, AMR...).
        Moreover if this value is greater than 0x80 the stream is a raw stream, else the stream
        is an encoded one
@n   0x0000 is a forbidden value, it describes an unknown stream */

typedef enum {
   M4SYS_kUnknown       = 0x0000,
   /* Stream type definition
       0xYYZZ   : YY is the codec type (Audio, Video, Picture, Scene ...)
                  ZZ is the sub-codec type (AAC, AMR , ...)
                     if ZZ is greater than 0x80 it is a raw format*/

   /* Audio ones   : Range from [0x0100-0x01FF]*/
   M4SYS_kAudioUnknown  = 0x0100,
   M4SYS_kAAC           = 0x0101,
   M4SYS_kCELP          = 0x0102,
   M4SYS_kAMR           = 0x0103,
   M4SYS_kAMR_WB        = 0x0104,
   M4SYS_kMP3           = 0x0105,
   M4SYS_kMIDI          = 0x0106,
   M4SYS_kWMA           = 0x0107,
   M4SYS_kREALAUDIO     = 0x0108,
   M4SYS_kEVRC            = 0x0109,
   M4SYS_kPCM_16bitsS   = 0x0181, /* PCM 16 bits Signed */
   M4SYS_kPCM_16bitsU   = 0x0182, /* PCM 16 bits Un-signed */
   M4SYS_kPCM_8bitsU    = 0x0183, /* PCM  8 bits Un-signed */
/* FixAA 2008/03/03 types: M4SYS_kPCM_16bitsS, M4SYS_kPCM_16bitsU and M4SYS_kPCM_8bitsU
   are now only used by AudioMixer and ReaderAVI => An update is necessary in the future for use
   type M4SYS_kPCM */
   M4SYS_kXMF            = 0x0184,
   M4SYS_kSMAF          = 0x0185,
   M4SYS_kIMEL          = 0x0186,
   M4SYS_kBBA            = 0x0187,
   M4SYS_kBPC            = 0x0188,
   M4SYS_kADPCM         = 0x0189,  /* ADPCM added */
   M4SYS_kPCM           = 0x0190,  /* stream type added: PCM;  PR2569 fixAA */
   M4SYS_kAudioAll        = 0x01FF,  /* all audio streams */

   /* Video ones   : Range [0x0200-0x02FF]*/
   M4SYS_kVideoUnknown  = 0x0200,
   M4SYS_kMPEG_4        = 0x0201,
   M4SYS_kH263          = 0x0202,
   M4SYS_kH263pp        = 0x0203,
   M4SYS_kH264          = 0x0204,
   M4SYS_kREALVIDEO     = 0x0205,
   M4SYS_kYUV420        = 0x0281,
   M4SYS_kRGB32         = 0x0282,
   M4SYS_kBGR32         = 0x0283,
   M4SYS_kRGB24         = 0x0284,
   M4SYS_kBGR24         = 0x0285,
   M4SYS_kVideoAll        = 0x02FF,  /* all video streams */

  /* Picture ones : Range [0x0300-0x03FF]*/
   M4SYS_kPictureUnknown = 0x0300,
   M4SYS_kJPEG           = 0x0301,
   M4SYS_kGIF            = 0x0302,
   M4SYS_kBMP            = 0x0383,
   M4SYS_kStillAll         = 0x03FF,  /* all still picture streams */

   /* Text ones    : Range [0x0400-0x04FF]*/
   M4SYS_kTextUnknown  = 0x0400,
   M4SYS_kTimedText    = 0x0401,
   M4SYS_kUTF8         = 0x0481,
   M4SYS_kUTF16        = 0x0482,
   M4SYS_kUCS2         = 0x0483,
   M4SYS_kTextAll       = 0x04FF,  /* all text streams */

   /* Scene & Graphics ones   : Range [0x0500-0x05FF]*/
   M4SYS_kSceneUnknown  = 0x0500,
   M4SYS_kSMIL          = 0x0501,
   M4SYS_kBIFS          = 0x0502,
   M4SYS_kSceneAll        = 0x05FF,  /* all scene streams */

   /* hinted ones   : Range [0x0600-0x06FF]*/
   M4SYS_kHintedUnknown = 0x0600,
   M4SYS_kRTP           = 0x0601,
   M4SYS_kMPEG2_TS      = 0x0602,
   M4SYS_kHintedAll        = 0x06FF,  /* all packetized streams */

   /* MPEG-4 system ones : Range [0x0700-0x07FF]*/
   M4SYS_kSysUnknown    = 0x0700,
   M4SYS_kODS           = 0x0701,
   M4SYS_kIPMP          = 0x0702,
   M4SYS_kOCI           = 0x0703,
   M4SYS_kSysAll        = 0x07FF /* all system streams*/
} M4SYS_StreamType ;

typedef struct {
   M4SYS_StreamID     streamID ;
   M4OSA_UInt32      value ;
} M4SYS_StreamIDValue ;

typedef struct {
   M4SYS_StreamID    streamID ;
   M4OSA_UInt32      size ;
   M4OSA_MemAddr32   addr ;
} M4SYS_StreamIDmemAddr ;

/** This strucure defines a set of properties associated to a stream*/
typedef struct {
  M4SYS_StreamID   streamID;    /**< The ID of the stream. It must be unique for a media
                                (ie in a MP4 file, two tracks can not have two times the same ID).
                                 0 is forbidden.*/
  M4SYS_StreamType streamType;    /**< The stream type of the stream*/
  M4OSA_UInt8      profileLevel;  /**< The profile & level of a stream. It is related to the
                                       stream type & the definition comes from the standard bodies
                                       (i.e. MPEG-4 Video & MPEG-4 Audio). Some values are
                                       pre-defined: 0xFE=userPrivate 0xFF=no Profile &
                                       Level specified*/
  M4OSA_UInt32     decoderSpecificInfoSize;  /**< The decoder configuration. These bytes are
                                                   needed to initialise a decoder.*/
  M4OSA_MemAddr32  decoderSpecificInfo; /**< The size (in bytes) of the decoder specific info.*/
  M4OSA_UInt32     timeScale;     /**< The time scale of the stream. It means that all timing
                                        duration of this stream are computed in this timescale
                                        (ie timeScale = 8000, means there are 8000 ticks in
                                        one second)*/
  M4OSA_Time       duration;        /**< The stream duration of this stream. The time unit is the
                                        time scale. The value can be set to M4SYS_UnknownTime if
                                        the duration is not known.*/
  M4OSA_Int32      averageBitrate;  /**< The average bitrate (in bit per second) of this stream.
                                         The average bitrate is computed on the stream duration.
                                         -1 value means either there is no average bitrate or no
                                         average bitrate is provided.*/
  M4OSA_Int32      maxBitrate;      /**< The maximum bitrate (in bit per second) of this stream.
                                         The maximum bitrate is computed on a sliding window of 1
                                         second. -1 value means either there is no max. bitrate or
                                         no max. bitrate is provided.*/
} M4SYS_StreamDescription;

typedef enum {
   M4SYS_kPreviousRAP      = 0x01 ,
   M4SYS_kNextRAP          = 0x02 ,
   M4SYS_kClosestRAP       = 0x03 ,
   M4SYS_kNoRAPprevious    = 0x11 ,
   M4SYS_kNoRAPnext        = 0x12 ,
   M4SYS_kNoRAPclosest     = 0x13 ,
   M4SYS_kBeginning        = 0x20
} M4SYS_SeekAccessMode ;

#endif /*M4SYS_STREAM_H*/



