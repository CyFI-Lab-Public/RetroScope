/*---------------------------------------------------------------------------*
 *  duk_err.h  *
 *                                                                           *
 *  Copyright 2007, 2008 Nuance Communciations, Inc.                               *
 *                                                                           *
 *  Licensed under the Apache License, Version 2.0 (the 'License');          *
 *  you may not use this file except in compliance with the License.         *
 *                                                                           *
 *  You may obtain a copy of the License at                                  *
 *      http://www.apache.org/licenses/LICENSE-2.0                           *
 *                                                                           *
 *  Unless required by applicable law or agreed to in writing, software      *
 *  distributed under the License is distributed on an 'AS IS' BASIS,        *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. * 
 *  See the License for the specific language governing permissions and      *
 *  limitations under the License.                                           *
 *                                                                           *
 *---------------------------------------------------------------------------*/


#ifndef _h_duk_err_
#define _h_duk_err_

#ifdef SET_RCSID
static const char duk_err_h[] = "$Id: duk_err.h,v 1.5.6.5 2007/08/31 17:44:52 dahan Exp $";
#endif

/** Still need this enum */
enum CrecException
{
  TIME_OUT_ERROR,  /* general operational errors */
  FEATURE_NOT_SUPPORTED,
  BAD_PARAMETER,
  BAD_ARGUMENT,
  BAD_INDEX,
  UNEXPECTED_STATE_ERROR,
  UNEXPECTED_DATA_ERROR,
  PREMATURE_EXIT,
  BAD_TUNNEL,
  FFT_TOO_SMALL,
  BAD_COSINE_TRANSFORM,
  ZERO_SPACE_ALLOC,  /* memory allocation errors */
  NO_SPACE_FOR_MALLOC,
  NO_SPACE_FOR_REALLOC,
  NO_SPACE_IN_OSHEAP,
  FREE_INVALID_POINTER,
  UNALLOCATED_VARIABLE,
  UNFREED_VARIABLE,
  STATE_LINK_ERROR,
  NO_ACTIVE_PATHS,
  SELF_LOOP_NODE,
  POLLUTED_TRACEBACK,
  NO_SPACE_FOR_LINKS,
  NO_SPACE_FOR_HIST,
  NO_SPACE_FOR_BACKPTR,
  BAD_SYNTAX_NODE,
  SYNTAX_UNSPECIFIED,
  BAD_SYNTAX,
  BAD_MODEL,
  INCORRECT_MODEL_TYPE,
  BAD_WW_MODEL_NAME,
  NO_MODEL_FOR_SYNTAX,
  MISMATCHED_MODEL_FOR_SYNTAX,
  BAD_SILENCE_MODEL,
  BAD_CONTEXT,
  BAD_GRAMMAR,
  BAD_MULTABLE,
  BAD_PEL_DATA,
  BAD_CHANNEL,
  BAD_PICTYPE_IN_ARB,
  STREAM_OPEN_FAILED,         /* data transmission errors */
  STREAM_CLOSE_FAILED,
  STREAM_READ_FAILED,
  STREAM_WRITE_FAILED,
  STREAM_ALREADY_OPEN,
  FILE_OPEN_FAILED,  /* file handling errors */
  FILE_SEEK_FAILED,
  BAD_DATA_IN_FILE,
  UNSUPPORTED_DATA_IN_FILE,
  FILE_WRITE_ERROR,
  FILE_READ_ERROR,
  INCORRECT_FILE_VERSION,
  MISSING_FILE_VERSION,
  RECOGNITION_RESULT,  /* diagnostic messages */
  DIAG_MESSAGE,
  BAD_RESULT,
  BAD_IMELDA,
  BAD_MLLR_TRANSFORM,
  BAD_COVARIANCE,
  BAD_OPERATION,
  SINGULAR_MATRIX,
  BAD_WAV_DEVICE,
  BAD_LATTICE,
  NO_SPACE_FOR_LATTICE,
  DFILE_EXCEPTION,
  WINSOUND_EXCEPTION,
  INTERNAL_ERROR,
  RECOGNIZER_NOT_LOADED,
  RECOGNIZER_ALREADY_LOADED,
  RECOGNIZER_NOT_SETUP,
  RECOGNIZER_ALREADY_SETUP,
  RECOGNIZER_NOT_STARTED,
  RECOGNIZER_ALREADY_STARTED,
  RECOGNIZER_NOT_CONFIGURED,
  RECOGNIZER_ALREADY_CONFIGURED,
  RECOGNIZER_HAS_RESULTS,
  RECOGNIZER_NO_RESULTS,
  ACOUSTIC_ALREADY_LOADED,
  ACOUSTIC_NOT_LOADED,
  ACOUSTIC_HAS_PATTERN,
  ACOUSTIC_HAS_NO_PATTERN,
  ACOUSTIC_PATTERN_MISMATCH,
  VOCAB_ALREADY_LOADED,
  VOCAB_NOT_LOADED,
  SYNTAX_GROUP_INVALID,
  SYNTAX_GROUPS_NOT_COMMON,
  SYNTAX_GROUP_MISMATCH,
  SYNTAX_GROUP_NOT_EMPTY,
  SYNTAX_RULE_INVALID,
  SYNTAX_RULE_NOT_EMPTY,
  SYNTAX_NOT_SETUP,
  SYNTAX_ALREADY_SETUP,
  UTTERANCE_UNKNOWN,
  UTTERANCE_INVALID,
  UTTERANCE_ALREADY_INITIALISED,
  UTTERANCE_NOT_INITIALISED,
  UTTERANCE_DIMEN_MISMATCH,
  MODEL_DIMEN_MISMATCH,
  PATTERN_NOT_LOADED,
  PATTERN_ALREADY_LOADED,
  PATTERN_NOT_SETUP,
  PATTERN_ALREADY_SETUP,
  PATTERN_NOT_SETUP_FOR_NOISE,
  PATTERN_ALREADY_SETUP_FOR_NOISE,
  ENDIAN_MISMATCH_ERROR,
  IMAGE_BAD_FILETYPE,
  IMAGE_BAD_ENDIAN,
  RECOGNIZER_INPUT_NOT_LOADED,
  PATTERN_INPUT_NOT_LOADED,
  ACOUSTIC_INPUT_NOT_LOADED,
  FRONTEND_INPUT_NOT_LOADED,
  UNCONFIGURED_WAVE,
  CONFIGURED_WAVE,
  UNCONFIGURED_CMS_AND_AGC,
  CONFIGURED_CMS_AND_AGC,
  UNATTACHED_CMS_AND_AGC,
  ATTACHED_CMS_AND_AGC,
  BAD_CMS_AND_AGC_CONFIGURATION,
  UNCONFIGURED_FRONTEND,
  CONFIGURED_FRONTEND,
  SPEC_FILTER_NOT_CONFIGURED,
  SPEC_FILTER_CONFIGURED,
  NONLINEAR_FILTER_NOT_CONFIGURED,
  NONLINEAR_FILTER_CONFIGURED,
  MAX_FILTER_POINTS_EXCEEDED,
  MISMATCHED_BUFF_SIZES,
  INCORRECT_SAMPLERATE,
  OCCUPANCY_MISMATCH,
  UNKNOWN_ARGUMENT,
  CACHE_NOT_SETUP,
  CACHE_ALREADY_SETUP,
  BAD_PHONEME,
  ANNOTATE_NO_SEGMENTS,
  ANNOTATE_SEGMENTS_EXIST,
  ANNOTATE_NO_LABEL,
  BAD_LABEL,
  FB_INVALID_STATE,
  FB_FRAME_INVALID,
  PIECEWISE_START,
  WARP_SCALE,
  NO_WARP,
  SEGMENTATION_NOT_INIT,
  SEGMENTATION_INIT,
  ASSERT_FAILED
};

/**
 * Service error.
 */
typedef struct
{
  char *msg;
  enum CrecException code;
  int is_fatal;
}
crecExceptionInfo;

#ifdef USE_EXCEPTION_HANDLING

typedef struct
{
  int  ca_rtti;
  enum  CrecException   code;
  char  *file_where_thrown;
  int  line_where_thrown;
  void  *err_object;
}
crecXceptn;
#endif

#endif /* _h_duk_err_ */
