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
 * @file   M4OSA_CoreID.h
 * @brief  defines the uniques component identifiers used for memory management
 *         and optionID mechanism
 * @note
 ************************************************************************
*/
#ifndef __M4OSA_COREID_H__
#define __M4OSA_COREID_H__

/* CoreId are defined on 14 bits */
/* we start from 0x0100, lower values are reserved for osal core components */

/* reader shells*/
#define M4READER_COMMON     0x0100
#define M4READER_AVI        0x0101
#define M4READER_AMR        0x0102
#define M4READER_3GP        0x0103
#define M4READER_NET        0x0104
#define M4READER_3GP_HTTP   0x0105
#define M4READER_MP3        0x0106
#define M4READER_WAV        0x0107
#define M4READER_MIDI       0x0108
#define M4READER_ASF        0x0109
#define M4READER_REAL        0x010A
#define M4READER_AAC        0x010B
#define M4READER_FLEX        0x010C
#define M4READER_BBA        0x010D
#define M4READER_SYNTHESIS_AUDIO    0x010E
#define M4READER_JPEG        0x010F


/* writer shells*/
#define M4WRITER_COMMON     0x0110
#define M4WRITER_AVI        0x0111
#define M4WRITER_AMR        0x0112
#define M4WRITER_3GP        0x0113
#define M4WRITER_JPEG        0x0116
#define M4WRITER_MP3        0x0117

/* decoder shells */
#define M4DECODER_COMMON    0x0120
#define M4DECODER_JPEG      0x0121
#define M4DECODER_MPEG4     0x0122
#define M4DECODER_AUDIO     0x0123
#define M4DECODER_AVC       0x0124
#define M4DECODER_MIDI      0x0125
#define M4DECODER_WMA        0x0126
#define M4DECODER_WMV        0x0127
#define M4DECODER_RMV        0x0128
#define M4DECODER_RMA        0x0129
#define M4DECODER_AAC       0x012A
#define M4DECODER_BEATBREW  0x012B
#define M4DECODER_EXTERNAL  0x012C

/* encoder shells */
#define M4ENCODER_COMMON    0x0130
#define M4ENCODER_JPEG      0x0131
#define M4ENCODER_MPEG4     0x0132
#define M4ENCODER_AUDIO     0x0133
#define M4ENCODER_VID_NULL  0x0134
#define M4ENCODER_MJPEG        0x0135
#define M4ENCODER_MP3        0x0136
#define M4ENCODER_H264        0x0137
#define M4ENCODER_AAC        0x0138
#define M4ENCODER_AMRNB        0x0139
#define M4ENCODER_AUD_NULL  0x013A
#define M4ENCODER_EXTERNAL  0x013B

/* cores */
#define M4JPG_DECODER       0x0140
#define M4JPG_ENCODER       0x0141

#define M4MP4_DECODER       0x0142
#define M4MP4_ENCODER       0x0143

#define M4AVI_COMMON        0x0144
#define M4AVI_READER        0x0145
#define M4AVI_WRITER        0x0146

#define M4HTTP_ENGINE       0x0147

#define M4OSA_TMPFILE       0x0148
#define M4TOOL_TIMER        0x0149

#define M4AMR_READER        0x014A

#define M4MP3_READER        0x014B

#define M4WAV_READER        0x014C
#define M4WAV_WRITER        0x014D
#define M4WAV_COMMON        0x014E

#define M4ADTS_READER        0x014F
#define M4ADIF_READER        0x016A

#define M4SPS               0x0150
#define M4EXIF_DECODER      0x0151
#define M4EXIF_ENCODER      0x0152
#define M4GIF_DECODER       0x0153
#define M4GIF_ENCODER       0x0154
#define M4PNG_DECODER       0x0155
#define M4PNG_ENCODER       0x0156
#define M4WBMP_DECODER      0x0157
#define M4WBMP_ENCODER      0x0158

#define M4AMR_WRITER        0x0159    /**< no room to put it along M4AMR_READER */


#define M4AVC_DECODER       0x015A
#define M4AVC_ENCODER       0x015B

#define M4ASF_READER        0x015C
#define M4WMDRM_AGENT        0x015D
#define M4MIDI_READER        0x0162    /**< no room before the presenters */
#define M4RM_READER         0x163
#define M4RMV_DECODER        0x164
#define M4RMA_DECODER        0x165

#define M4TOOL_XML            0x0166
#define M4TOOL_EFR            0x0167    /**< Decryption module for Video Artist */
#define M4IAL_FTN            0x0168    /* FTN implementation of the IAL */
#define M4FTN                0x0169    /* FTN library */

/* presenter */
#define M4PRESENTER_AUDIO   0x0160
#define M4PRESENTER_VIDEO   0x0161

/* high level interfaces (vps, etc..)*/
#define M4VPS               0x0170
#define M4VTS               0x0171
#define M4VXS               0x0172
#define M4CALLBACK          0x0173
#define M4VES               0x0174
#define M4PREPROCESS_VIDEO  0x0175
#define M4GRAB_AUDIO        0x0176
#define M4GRAB_VIDEO        0x0177
#define M4VSSAVI            0x0178
#define M4VSS3GPP           0x0179
#define M4PTO3GPP           0x017A
#define M4PVX_PARSER        0x017B
#define M4VCS                0x017C
#define M4MCS                0x017D
#define M4MNMC                0x0180    /**< mnm controller */
#define M4TTEXT_PARSER      0x0181    /**< timed text */
#define M4MM                0x0182    /**< Music manager */
#define M4MDP                0x0183    /**< Metadata parser */
#define M4MMSQLCORE            0x0184
#define M4VPSIL                0x0185
#define M4FILEIL            0x0186 /* IL file Interface */
#define M4MU                0x0187
#define M4VEE                0x0188  /**< Video effect engine */
#define M4VA                0x0189 /* VideoArtist */
#define M4JTS                0x018A
#define M4JTSIL                0x018B
#define M4AIR                0x018C  /**< AIR */
#define M4SPE                0x018D  /**< Still picture editor */
#define M4VS                0x018E    /**< Video Studio (xVSS) */
#define M4VESIL                0x018F    /**< VES il */
#define M4ID3                0x0190    /**< ID3 Tag Module */
#define M4SC                0x0191    /**< Media Scanner */
#define M4TG                0x0192  /**< Thumbnail Generator*/
#define M4TS                0x0193    /**< Thumbnail storage */
#define M4MB                0x0194    /**< Media browser */

/* high level application (test or client app) */
#define M4APPLI             0x0200
#define M4VA_APPLI            0x0201    /**< Video Artist test application */

/* external components (HW video codecs, etc.) */
#define M4VD_EXTERNAL        0x0300
#define M4VE_EXTERNAL        0x0301


/* priority to combine with module ids */
#define M4HIGH_PRIORITY     0xC000
#define M4MEDIUM_PRIORITY   0x8000
#define M4LOW_PRIORITY      0x4000
#define M4DEFAULT_PRIORITY  0x0000


#endif /*__M4OSA_COREID_H__*/

