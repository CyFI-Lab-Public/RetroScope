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
#ifndef NXPSW_COMPILERSWITCHES_MCS_H
#define NXPSW_COMPILERSWITCHES_MCS_H

                            /***********/
                            /* READERS */
                            /***********/

/* -----  AMR reader support ----- */
#define M4VSS_SUPPORT_READER_AMR        /**< [default] Support .amr files */

/* ----- 3GPP  reader support ----- */
#define M4VSS_SUPPORT_READER_3GP        /**< [default] Support .mp4, .3gp files */


/* ----- MP3 reader support ----- */
#define M4VSS_SUPPORT_READER_MP3        /**< [default] Support .mp3 files */

/* ----- RAW reader support ----- */
#define M4VSS_SUPPORT_READER_PCM        /**< [default] Support .pcm files */


                            /************/
                            /* DECODERS */
                            /************/

/* -----  AMR NB decoder support ----- */
#define M4VSS_SUPPORT_AUDEC_AMRNB       /**< [default] Support AMR NB streams */

/* ----- AAC decoder support ----- */
#define M4VSS_SUPPORT_AUDEC_AAC            /**< [default] Support AAC, AAC+ and eAAC+ streams */
#define M4VSS_SUPPORT_VIDEC_NULL

/* ----- MP4/H263 video decoder support ----- */
#define M4VSS_SUPPORT_VIDEC_3GP         /**< [default] Support mpeg4 and H263 decoders */

#ifdef M4VSS_SUPPORT_VIDEC_3GP
#define GET_DECODER_CONFIG_INFO
#endif

#define M4VSS_SUPPORT_VIDEO_AVC            /**< [default] Support H264 decoders */

/* ----- MP3 decoder support----- */
#define M4VSS_SUPPORT_AUDEC_MP3         /**< [default] Support MP3 decoders */


/* ----- NULL decoder support----- */
#define M4VSS_SUPPORT_AUDEC_NULL        /** [default] Support PCM reading */


                            /***********/
                            /* WRITERS */
                            /***********/

/* ----- 3gp writer ----- */
#define M4VSS_SUPPORT_WRITER_3GPP       /**< [default] support encapsulating in 3gp format
                                             {amr,aac} x {mpeg4,h263} */





                            /************/
                            /* ENCODERS */
                            /************/

/* ----- mpeg4 & h263 encoder ----- */
#define M4VSS_SUPPORT_ENCODER_MPEG4     /**< [default] support encoding in mpeg4 and
                                             h263 format {yuv,rgb} */

/* ----- h264 encoder ----- */
#define M4VSS_SUPPORT_ENCODER_AVC

/* ----- amr encoder ----- */
#define M4VSS_SUPPORT_ENCODER_AMR  /**< [default] support encoding in amr 12.2 format {amr,wav} */

/* ----- aac encoder ----- */
#define M4VSS_SUPPORT_ENCODER_AAC       /**< [default] support encoding in aac format {amr,wav} */


/* ----- mp3 encoder ----- */
#define M4VSS_SUPPORT_ENCODER_MP3       /**< [default] support encoding in mp3 format {mp3} */

                            /************/
                            /* FEATURES */
                            /************/

/* ----- VSS3GPP & xVSS ----- */
#define M4VSS_SUPPORT_EXTENDED_FEATURES /**< [default] if defined, implementation is xVSS else
                                            it is VSS3GPP */

/* ----- SPS ----- */
#ifdef M4VSS_SUPPORT_EXTENDED_FEATURES

//#define M4SPS_GIF_NOT_SUPPORTED  /**< [option] do not support GIF format in still picture api */
//#define M4SPS_JPEG_NOT_SUPPORTED /**< [option] do not support JPEG format in still picture api */
//#define M4SPS_PNG_NOT_SUPPORTED  /**< [option] do not support PNG format in still picture api */
#define M4SPS_WBMP_NOT_SUPPORTED   /**< [option] do not support WBMP format in still picture api */
#define M4SPS_BGR565_COLOR_OUTPUT  /**< [option] output in still picture api is BGR565
                                        (default = BGR24) */

#else

#define M4SPS_GIF_NOT_SUPPORTED    /**< [option] do not support GIF format in still picture api */
//#define M4SPS_JPEG_NOT_SUPPORTED /**< [option] do not support JPEG format in still picture api */
#define M4SPS_PNG_NOT_SUPPORTED    /**< [option] do not support PNG format in still picture api */
#define M4SPS_WBMP_NOT_SUPPORTED   /**< [option] do not support WBMP format in still picture api */
//#define M4SPS_BGR565_COLOR_OUTPUT /**< [option] output in still picture api is BGR565
//                                          (default = BGR24) */

#endif

#define M4VSS_ENABLE_EXTERNAL_DECODERS

#define M4VSS_SUPPORT_OMX_CODECS

#endif /* NXPSW_COMPILERSWITCHES_MCS_H */

