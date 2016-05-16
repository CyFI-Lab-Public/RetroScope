/*---------------------------------------------------------------------------*
 *  simapi.h  *
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



#ifndef _h_simapi_
#define _h_simapi_

#ifdef SET_RCSID
static const char simapi_h[] = "$Id: simapi.h,v 1.22.4.25 2008/03/07 17:37:14 dahan Exp $";
#endif

#include "creccons.h"   /* CREC Public Constants    */

#include "c42mul.h"
#include "caexcept.h"
#include "setting.h"
#include "mutualob.h"
#include "srec_sizes.h"
#include "search_network.h"
#include "srec.h"
#include "srec_eosd.h"
#include "srec_results.h"
#include "swimodel.h"
#include "ESR_Locale.h"

/**
 ************************************************************************
 * SwigImageBegin       <- DO NOT MOVE THIS LINE !
 *
 * This is a CADOC Keyword section.
 *
 * If CADOC is instructed to create a SWIG I-File and this is one of the
 * files in the input list, everything between the 'SwigImage Begin' and
 * 'SwigImage End' keywords comment blocks will be copied 'as-is' to the
 * SWIG I-File specified on the CADOC command line.
 *
 * The purpose of the following marked section is to identify CA
 * structures that SWIG may need to complete the CREC API interface.
 *
 * Refer to the document "DOC\CADOC.TXT" for details for the CADOC Tool.
 *
 ************************************************************************
 */


#ifdef __cplusplus
extern "C"
{
#endif


#ifndef SWIGBUILD

  typedef featdata CA_Feature;

	/**
	 * @todo document
	 */
#define NCONFPARS 6

typedef struct
{
    double scale[NCONFPARS];
    double offset[NCONFPARS];
    double weight[NCONFPARS];
} Confidence_model_parameters;

  typedef struct
  {
	Confidence_model_parameters one_nbest;
	Confidence_model_parameters many_nbest;
  }
  CA_ConfidenceScorer;

	/**
	 * @todo document
	 */
  typedef struct
  {
    int         ca_rtti;
    booldata    is_resultBlocked;
    booldata    is_configured;
    int         setup_count;
    booldata    is_running;
    multi_srec  *recm;                  /* contains recognition structure */
    srec_eos_detector_parms* eosd_parms;/* contains ep parameters */
  }
  CA_Recog;

	/**
	 * @todo document
	 */
  typedef struct
  {
    int         ca_rtti;
    booldata    is_loaded;
    int         pattern_setup_count;
    int         use_dim;
    int         partial_distance_calc_dim;
    prdata      imelda_scale;
    const SWIModel    *swimodel; /* owning pointer to compact acoustic models */
  }
  CA_Acoustic;

	/**
	 * @todo document
	 */
  typedef struct
  {
    int         ca_rtti;
    /* IMPORTANT NOTE
     *
     * This structure is used by CA_WriteSyntaxImage
     * in the file "ca/cod_imag.c". Certain assumptions
     * about the member types and order are made.
     * DON'T change this structure without making
     * appropriate changes to the above function.
     */

    booldata    has_groups;
    booldata    has_rules;
    int         setup_count;
    /* syntax_info* synx; */
    srec_context* synx;
  }
  CA_Syntax;

	/**
	 * @todo document
	 */
  typedef struct
  {
    int dummy;
  }
  CA_Arbdata;

	/**
	 * @todo document
	 */
  typedef struct
  {
    int         ca_rtti;
    /* IMPORTANT NOTE
     *
     * This structure is used by CA_WritePatternImage
     * in the file "ca/cod_imag.c". Certain assumptions
     * about the member types and order are made.
     * DON'T change this structure without making
     * appropriate changes to the above function.
     */

    booldata      is_loaded;
    //model_info    *setup_whole;
    //model_info    *setup_sub;
    booldata      has_cache;
    booldata      true_accumulates;
    booldata      recog_terminated;
    pattern_info  data;
  }
  CA_Pattern;


	/**
	 * @todo document
	 */
  typedef struct
  {
    int           dim;
    transform_info  imelda_acc;
    transform_info  mllr_acc;
    booldata      do_mllr;
    booldata      do_imelda;
    booldata      is_loaded;
    booldata      is_setup;
  }
  CA_Transform;


	/**
	 * @todo document
	 */
  typedef struct
  {
    int             ca_rtti;
    booldata        is_loaded;    /* generates VOCAB_NOT_CONFIGURED */
    vocab_info      voc;
  }
  CA_Vocab;

	/**
	 * @todo document
	 */
  typedef struct
  {
    int           ca_rtti;
    short         type;
    unsigned long startTime;
    unsigned long endTime;
    short         wordID;
    long          score;
    char          *label;
  }
  CA_Result;

  /* typedef nbest_list CA_NBestList; */

	/**
	 * @todo document
	 */
  typedef struct
  {
    asr_int32_t dummy[2];
  }
  CA_NBestList;

	/**
	 * @todo document
	 */
  typedef struct
  {
    int         ca_rtti;
    booldata    is_loaded;
    int         dimen;
    int         stay_penalty;
    int         skip_penalty;
    int         whole_stay_penalty;
    int         whole_skip_penalty;
    int         dur_scale;
    int         frame_period;
    int         min_var;
    int         max_var;
    booldata    load_all;
    char        load_models[MAX_LABEL];
  }
  CA_AcoustInputParams;


	/**
	 * @todo document
	 */
	typedef struct
  {
    int         ca_rtti;
    booldata    is_loaded;
    ESR_BOOL        do_early;
    ESR_BOOL        do_partial;
    int         word_penalty;
    int         top_choices;
    int         reject_score;
    int         traceback_freq;
    int         viterbi_prune_thresh;  /*score-based pruning threshold - only keep paths within this delta of best cost*/
    int         max_hmm_tokens;       /*controls the maximum number of HMM's alive in any frame.  If number
         exceeded, pruning gets tightened.  So, this threshold can be used
         to tradeoff accuracy for computation an memory*/
    int         max_fsmnode_tokens;   /*controls the maximum number of FSMs alive in any frame.  If number,
         exceeded, pruning gets tightened.  So, this threshold can be used
         to tradeoff accuracy for computation an memory*/
    int         max_word_tokens;      /*controls the maximum number of word tokens kept in the word lattice.
         if number exceeded, the word lattice is pruned more tightly (less word
         ends per frame*/
    int         max_altword_tokens;      /*controls the maximum number of alternative paths to propagate, for nbest */
    int         num_wordends_per_frame; /*controls the size of the word lattice - the number of word ends to
           keep at each time frame*/
    int         max_fsm_nodes;        /*allocation size of a few arrays in the search - needs to be big enough
         to handle any grammar that the search needs to run.  Initialization fails
         if num exceeded*/
    int         max_fsm_arcs;         /*allocation size of a few arrays in the search - needs to be big enough
         to handle any grammar that the search needs to run.  Initialization fails
         if num exceeded*/
    int           max_searches;         /* now: 2 searches for 2 genders */
    int         terminal_timeout;           /* end of utterance timeout at terminal node */
    int         optional_terminal_timeout;  /* end of utterance timeout at optional terminal node */
    int         non_terminal_timeout;       /* end of utterance timeout at non terminal node */
    int         eou_threshold;              /* select hypotheses for end-of-utterance tests */
    int         stats_enabled;              /* enable frame-by-frame recognizer stats */
    int         max_frames;             /* max number of frames in for searching */
    int         max_model_states;       /* indicates largest acoustic model this search can use */
  }
  CA_RecInputParams;

	/**
	 * @todo document
	 */
  typedef struct
  {
    int         ca_rtti;
    booldata    is_loaded;
    int         dimen;
    int         whole_dimen;
    int         feat_start;
    float       mix_score_scale;
    float       imelda_scale;
    float       uni_score_scale;
    float       uni_score_offset;
    int         forget_speech;
    int         forget_background;
    int         rel_low;
    int         rel_high;
    int         gap_period;
    int         click_period;
    int         breath_period;
    int         extend_annotation;
    int         param;
#if PARTIAL_DISTANCE_APPROX
    int         partial_distance_dim;
    int         partial_distance_threshold;
    int         partial_distance_offset;
    int         global_model_means[MAX_DIMEN];
#endif
    int         min_initial_quiet_frames;      /* num silence frames needed before input */
    int         min_annotation_frames;          /* minimum overall length */
    int         max_annotation_frames;          /* maximum overall length */
    int         delete_leading_segments;        /* num segments to delete. 0=no action */
    int         leading_segment_accept_if_not_found; /* Do not reject segmentation if not found */
    int         leading_segment_min_frames;     /* remove unless shorter */
    int         leading_segment_max_frames;     /* remove unless exceeded */
    int         leading_segment_min_silence_gap_frames;/* remove if good silence gap to next segment */
    int         beep_size;  /*X201 beep filter */
    int         beep_threshold;  /*X201 beep filter */
    int         min_segment_rel_c0; /* Any segment gets deleted whose peak c0 is < max - min_segment_rel_c0 */

#if DO_SUBTRACTED_SEGMENTATION
    int         snr_holdoff; /* Ignore first n frames when estimating speech level for SNR measure */
    int         min_acceptable_snr; /* for successful segmentation */
#endif
    int         stats_enabled;                  /* enable frame-by-frame pattern stats */
  }
  CA_PatInputParams;



#endif

#define IMELDA_ACC_BETWEEN      1
#define IMELDA_ACC_WITHIN       2
#define IMELDA_ACC_BMEAN        3
#define IMELDA_ACC_WMEAN        4
#define MLLR_ACC_BETWEEN        5
#define MLLR_ACC_WITHIN         6
#define MLLR_ACC_BMEAN          7
#define MLLR_ACC_WMEAN          8
#define IMELDA_MATRIX           9
#define INVERSE_IMELDA_MATRIX   10
#define MLLR_MATRIX             11
#define TRANSFORM_MATRIX        12
#define TRANSFORM_OFFSET        13
#define CHAN_OFFSET             14

  /* for fast match debugging
  ** 5/1/99 SL
  */
#define FRAME_VOICED            1
#define FRAME_QUIET             2
#define FRAME_UNCERTAIN         3

  int CA_GetVoicingStatus(CA_Pattern *hPattern);
  /**
   *
   * Params       hPattern    Handle to valid object
   * Returns      int         See below
   *
   ************************************************************************
   * Returns the voicing status of the current frame contained in the
   * Pattern object.
   * The function will return one of the following values :-
   *
   * FRAME_VOICED         1
   * FRAME_QUIET          2
   * FRAME_UNCERTAIN              3
   *
   * TODO: Integrate these defines into CR_type.h
   ************************************************************************
   */



  /*
  ** File: simapi.c
  */
  void CA_GetVersion(char *vertab,
                     int len);
  /**
   * Params   *vertab     Version info
   *          len         length of vertab
   *
   * Returns  nothing
   *
   ************************************************************************
   * There doesn't seem to be anything using this function.  Has it been
   * replaced by CrecGetVersion??  If so, there should be a CA interface
   * to it.
   ************************************************************************
   */


  void CA_Log(const char *string);
  /**
   *
   * Params       string  Single string for output log
   *
   * Returns      nothing
   *
   *
   ************************************************************************
   * NOTE swig dosen't allow <varargs> for function arguments hence, you
   * will have to format the string yourself prior to calling this function
   ************************************************************************
   */


  void CA_Init(void);
  /**
   *
   * Params       void
   *
   * Returns      Nothing, the error callback routine is registered
   *
   * See          CA_Term
   *
   ************************************************************************
   * Initializes the CREC (CR) interface.  A callback routine is supplied
   * by the user.  This is used as the registered callback function.
   * Any errors will vector through this method.  The prototype for the
   * callback funtion is defined in CR_TYPE.H
   *
   * This method must be the first CR call.  It can only be called again
   * during the lifetime of a process after a call to CA_Term().
   *
   * The user can specify NULL as the handler funtion, this will cause
   * the default error handler, crDefaultHandler(), to run when errors
   * are encountered.  This allows the easy use of PERL scripts and the CR
   * interface.  Generally a handler should be supplied.
   ************************************************************************
   */

  void CA_Term(void);
  /**
   *
   * Params       void
   *
   * Returns      Nothing, CR is terminated.  No CR method can be called
   *              until another call to CA_Init()
   *
   * See          CA_Init
   *
   ************************************************************************
   * Signals that the CREC (CR) interface is no longer needed.  This should
   * be the last method called in a CR application.
   *
   * The callback routine registered with CA_Init() is released.
   *
   * A test is made to ensure that all the handles associated with
   * CR objects have been de-allocated.  If an object handle still
   * exists when CA_Term() is called this will generate an error.
   ************************************************************************
   */




#ifdef USE_EXCEPTION_HANDLING
  /*
  **  CA_Exception methods
  */
  int  CA_ExceptionGetCode(CA_Exception* e);
  int  CA_ExceptionGetMessage(CA_Exception* e , char* buff, int len,
                              char *filebuff, int filebufflen, int* line);

  int  CA_ExceptionGetHandleType(CA_Exception* e);
  int  CA_ExceptionGetHandle(CA_Exception* e, void **handle);
  int  CA_ExceptionDelete(CA_Exception* e);
#endif


  /*
  **  CA_Acoustic methods
  */
  /*
  **  File: acc_basi.c
  */
  CA_Acoustic *CA_AllocateAcoustic(void);
  /**
   *
   * Params       void
   *
   * Returns      Handle to new acoustic structure, or NULL on error.
   *
   * See          CA_FreeAcoustic
   *
   ************************************************************************
   * Allocates a new Acoustic structure (object)
   ************************************************************************
   */


  void CA_FreeAcoustic(CA_Acoustic *hAcoust);
  /**
   *
   * Params       hAcoust   Handle to previously allocated acoustic structure
   *
   * Returns      void
   *
   * See          CA_AllocateAcoustic
   *
   ************************************************************************
   * Frees a previously allocated acoustic object.
   ************************************************************************
   */


  void CA_UnloadAcoustic(CA_Acoustic *hAcoust);


  /*
  **  File: acc_sub.c
  */
  int  CA_LoadAcousticSub(CA_Acoustic *hAcoust,
                          char *subname,
                          CA_AcoustInputParams *hAcoustInp);
  /**
   *
   * Params       hAcoust   point to a valid acoustic object
   *              subname     subword acoustic model file
   *              do_arb      0 for pcx, 1 for arb
   *              do_uni      1 for unimodal models
   *              hAcoustInp  Handle to valid acoustic input object
   *
   * Returns      True/False
   *
   * See          CA_UnloadAcoustic
   *              CA_LoadAcousticWhole
   *              CA_LoadDictionary
   *
   ************************************************************************
   * The acoustic sub-word models, whether speaker dependent or independent
   * must be loaded with this method.
   *
   * The model files must be in the 'Tracy' format (.PDF, .MIX etc.).
   * It is possible to load the ARB files, PCX files and CLV files.
   *
   * Note:    The phoneme table located with the ARB file is checked with
   *          other phoneme tables in the system (such as the one specified
   *          in the OK file).  This is why the Vocabulary class is also
   *          required.
   ************************************************************************
   */




  /*
  **  CA_Vocab methods
  */
  /*
  **  File: voc_basi.c
  */
  CA_Vocab *CA_AllocateVocabulary(void);
  /**
   *
   * Params       void
   *
   * Returns      Handle to allocated vocabulary structure, or NULL on error.
   *
   * See          CA_FreeVocabulary
   *
   ************************************************************************
   * This method creates a new vocabulary structure (object).  The new
   * structure is supplied to the caller by the method's return value.
   ************************************************************************
   */


  void CA_FreeVocabulary(CA_Vocab *hVocab);
  /**
   *
   * Params       hVocab  Handle to a previously allocated vocabulary
   *
   * Returns      void
   *
   * See          CA_AllocateVocabulary
   *
   ************************************************************************
   * Frees a previously allocated vocabular structure (object).
   ************************************************************************
   */
  void CA_LoadDictionary(CA_Vocab *hVocab,
                         const LCHAR* vocname,
                         char *phtname,
                         ESR_Locale* locale);
  /**
   *
   * Params       hVocab  Handle to vocabulary object
   *              vocFile Source vocabulary file
   *              phtname Source of phoneme table file
   *              locale Locale associated with dictionary
   *
   * Returns      void
   *
   * See          CA_UnloadDictionary
   *
   ************************************************************************
   * Loads vocabulary 'words' from an OK file.
   ************************************************************************
   */


  void CA_UnloadDictionary(CA_Vocab *hVocab);
  /**
   *
   * Params       hVocab  Handle to vocabulary object
   *
   * Returns      void
   *
   * See          CA_LoadDictionary
   *
   ************************************************************************
   * Unloads a previously loaded dictionary
   ************************************************************************
   */


  int  CA_CheckEntryInDictionary(CA_Vocab *hVocab,
                                 const char *label);
  /**
   *
   * Params       hVocab  Handle to vocabulary object
   *              label   textual trasncription of requested word
   *
   * Returns      ?
   *
   ************************************************************************
   * Checks the existance of a pronunciation
   ************************************************************************
   */


  int CA_GetEntryInDictionary(CA_Vocab *hVocab,
                              const char *label,
                              char *pron,
                              int *pronSize,
                              int pronMaxSize);
  /**
   *
   * Params       hVocab      Handle to vocabulary object
   *              label       textual trasncription of requested word
   *              pron        buffer to put pronunciation in.  The format
   *                          of this buffer is a series of prons terminated
   *                          by a null all terminated by a double null
   *              pronSize    the number of bytes returned in the pron buffer
   *              pronMaxSiz  this size of "pron"
   *
   * Returns      true if the word exists in the dictionary
   *
   ************************************************************************
   * Get the pronunciation of a word
   ************************************************************************
   */


  /*
  **  CA_NGram methods
  */
  /*
  **  File: lmf_basi.c
  */
  /*
  ** Gethin 25Aug00
  ** Functions excluded from compilation in ca.mak,
  ** therefore commented here
  **
  ** CA_NGram *CA_AllocateNGram (void);
  ** void CA_FreeNGram (CA_NGram *hNGram);
  ** void CA_LoadNGram (CA_NGram *hNGram, char *lmfname, char *trnname);
  ** void CA_SaveNGram (CA_NGram *hNGram, char *lmfname);
  ** void CA_UnloadNGram (CA_NGram *hNGram);
  ** int CA_ScoreSentence (CA_NGram *hNGram, char *sentence);
  ** int CA_SetupNGram (CA_NGram *hNGram, CA_Vocab *hVocab, int type);
  */

  /*
  **  CA_Pattern methods
  */
  /*
  **  File: pat_basi.c
  */

  CA_Pattern *CA_AllocatePattern(void);
  /**
   *
   * Params       void
   *
   * Returns      Handle to new pattern object
   *
   * See          CA_FreePattern
   *
   ************************************************************************
   * Creates a new pattern structure (object)
   ************************************************************************
   */


  void CA_FreePattern(CA_Pattern *hPattern);
  /**
   *
   * Params       hPattern    valid pattern handle
   *
   * Returns      Nothing, the pattern handle is not longer valid.
   *
   * See          CA_AllocatePattern
   *
   ************************************************************************
   * Deletes a previously allocated pattern structure
   ************************************************************************
   */


  int  CA_LoadPattern(CA_Pattern *hPattern,
                      CA_PatInputParams *hPatInput,
                      int dimen,
                      char *multable,
                      char *imelda);
  /**
   *
   * Params       hPattern    valid pattern handle
   *              hPatInput   valid pattern input paramater handle
   *              dimen
   *              multable
   *              imelda
   *
   * Returns      true/false
   *
   * See          CA_UnloadPattern
   *              CA_SetupPatternForAcoustic
   *              CA_LoadPatternParameters
   *
   ************************************************************************
   * Loads a pattern object.  Once loaded it need to be setup
   * with an acoustic model before any further setups involving
   * the recognizer.  The imelda parameters and optionally
   * the multables are loaded into the object.  Setup operations
   * based on parameters are also done.
   *
   * It is an error to call this function without first loading
   * pattern input parameters.
   ************************************************************************
   */


  void CA_UnloadPattern(CA_Pattern *hPattern);
  /**
   *
   * Params       hPattern    valid pattern handle
   *
   * Returns      Nothing, loaded object is unloaded
   *
   * See          CA_LoadPattern
   *
   ************************************************************************
   * Unloads a previously loaded pattern object
   ************************************************************************
   */


  void CA_SetupPatternForAcoustic(CA_Pattern *hPattern,
                                  CA_Acoustic *hAcoustic);
  /**
   *
   * Params       hPattern    valid pattern handle
   *              hAcoustic   valid acoustic handle
   *
   * Returns      Nothing, the pattern object is setup and ready for use
   *
   * See          CA_ClearPatternForAcoustic
   *              CA_LoadPattern
   *
   ************************************************************************
   * This sets up a previously allocated pattern object with acoustic
   * model information from the appropriate acoustic object.
   * It is necessary to call this function before the acoustic
   * object can be used any recognition or adaptation.
   *
   * If you have not called CA_LoadPattern() an error will be generated.
   ************************************************************************
   */


  void CA_ClearPatternForAcoustic(CA_Pattern *hPattern,
                                  CA_Acoustic *hAcoustic);
  /**
   *
   * Params       hPattern    valid pattern handle
   *              hAcoustic   valid acoustic handle
   *
   * Returns      Nothing
   *
   * See          CA_SetupPatternForAcoustic
   *
   ************************************************************************
   * un-sets a previously prepared pattern object
   ************************************************************************
   */


  int  CA_NextPatternFrame(CA_Pattern *hPattern,
                           CA_Utterance *hUtt);
  /**
   *
   * Params       hPattern valid pattern handle
   *              hUtt     valid utterance handle
   *
   * Returns      status code
   *
   * See          CA_NextUtteranceFrame
   *              CA_MakePatternFrame
   *              CA_AdvanceRecognitionByFrame
   *
   ************************************************************************
   * Frames can be accessed, on-by-one, from an utterance object into
   * a pattern object with this method.  If this routine returns a non-zero
   * value then an utterance frame is available for processing and a call
   * to the recognition method CA_AdvanceRecognitionByFrame as shown below:
   *
   * while ( CA_NextPatternFrame( hPattern, hUtt ) )
   * {
   *     CA_AdvanceRecognitionByFrame( hRecog, hPattern, hUtt );
   * }
   *
   * Frames copied in with this method are transformed (post-IMELDA)
   *
   * It is now advised that instead of this function,
   * one uses CA_GetUtteranceFrame and CA_AdvanceUtteranceFrame
   * to access frames in sequence.
   ************************************************************************
   */

  int  CA_MakePatternFrame(CA_Pattern *hPattern,
                           CA_Utterance *hUtt);
  /**
   *
   * Params       hPattern valid pattern handle
   *              hUtt     valid utterance object handle
   *
   * Returns      ?
   *
   * See          CA_NextUtteranceFrame
   *              CA_NextPatternFrame
   *
   ************************************************************************
   * The current frames is pulled in from an utterance object into
   * a pattern object with this method.  The frame position is not
   * moved on.
   *
   * Frames copied in with this method are IMELDA transformed
   ************************************************************************
   */


  /*
  **  File: pat_tran.c
  */
  void CA_SetCepstrumOffsetInPattern(CA_Pattern *hPattern,
                                     int index,
                                     int value);
   /**
   *
   * Params       hPattern    valid pattern handle
   *              hAcoust   valid acoustic handle
   *              relevance   relevance coefficient
   *
   * Returns      Nothing
   *
   ************************************************************************
   *  Accumulates the relevance of hAcoustic into hPattern
   ************************************************************************
   */


  void CA_SetupNoiseConditioning(CA_Pattern *hPattern,
                                 CA_FrontendInputParams *hFrontpar,
                                 int do_wholeword,
                                 int do_subword);
  /**
   *
   * Params       hPattern valid pattern handle
   *              hFrontpar valid frontend parameter object handle
   *              do_wholeword    true or false
   *              do_subword      true or false
   *
   * Returns      ?
   *
   * See          CA_ClearNoiseConditioning
   *              CA_ConditionAcousticToNoise
   *
   ************************************************************************
   * The initial setup for Extended Noise Conditioning (ENC).
   *
   ************************************************************************
   */


  void CA_ClearNoiseConditioning(CA_Pattern *hPattern);
  /**
   *
   * Params       hPattern valid pattern handle
   *
   * Returns      ?
   *
   * See          CA_SetupNoiseConditioning
   *              CA_ConditinAcousticToNoise
   *
   ************************************************************************
   * To free data structures constructed for Extended Noise Conditioning.
   *
   ************************************************************************
   */


  void CA_WriteMLLRCovariates(CA_Pattern *hPattern,
                              CA_Acoustic *hAcoust,
                              char *basename);
  /**
      No CR equivalent function
   */


  void CA_ReadMLLRTransforms(CA_Pattern *hPattern,
                             CA_Acoustic *hAcoust,
                             char *basename,
                             int ntrans);
  /**
      No CR equivalent function
   */


  /*
  **  File: pat_dump.c
  */
  void CA_SetupTransformDump(CA_Pattern *hPattern,
                             char* basename);
  /**
   *
   * Params       hPattern    valid pattern handle
   *              basename    file base name
   *
   * Returns      Nothing
   *
   * See          CA_ClearTransformDump
   *              CA_CalculateTransforms
   *              CA_ClearTransformAccumulates
   *
   ************************************************************************
   *  Sets up the facility to save the transform accumulates to a file.
   ************************************************************************
   */


  void CA_ClearTransformDump(CA_Pattern *hPattern);
  /**
   *
   * Params       hPattern    valid pattern handle
   *
   * Returns      Nothing
   *
   * See          CA_SetupTransformDump
   *              CA_CalculateTransforms
   *              CA_ClearTransformAccumulates
   *
   ************************************************************************
   *  Clears the facility to save the transform accumulates to a file.
   ************************************************************************
   */


  /*
   * Pattern Array access methods
   */
  /*
  **  File: pat_swig.c
  */

	/*
   * Transform methods
   */
  /*
  **  File: catrans.c
  */
  /*
   * dave 08-Oct-99: These functions are now out of sync with CA.
   *
   * CA_Transform *CA_AllocateTransform (void);

   * void CA_FreeTransform (CA_Transform *hTransform);
   * int CA_LoadTransform (CA_Transform *hTransform, int dimen);
   * void CA_UnloadTransform (CA_Transform *hTransform);
   * void CA_ConfigureTransform (CA_Transform *hTransform,int do_mllr,int do_imelda);
   * void CA_ClearTransform (CA_Transform *hTransform);
   * void CA_InheritAccumulates (CA_Transform *hTransform, CA_Pattern *hPattern);
   * void CA_LoadTransformAccumulates( CA_Pattern *hPattern,
   *      CA_Transform *hTransform);
   */

  /*
  **  CA_Utterance methods
  */
  /*
  **  File: utt_basi.c
  */
  CA_Utterance *CA_AllocateUtterance(void);
  /**
   *
   * Params       void
   *
   * Returns      Handle to a new utterance object
   *
   * See          CA_FreeUtterance
   *
   ************************************************************************
   * Creates a new utterance object
   ************************************************************************
   */


  void CA_FreeUtterance(CA_Utterance *hUtt);
  /**
   *
   * Params       hUtt    valid utterance handle
   *
   * Returns      void    the object is no longer valid
   *
   * See          CA_AllocateUtterance
   *
   ************************************************************************
   * Destroys a previously allocated utterance object
   ************************************************************************
   */


  int  CA_InitUtteranceForFrontend(CA_Utterance *hUtt,
                                   CA_FrontendInputParams *hFrontInp);
  /**
   *
   * Params       hUtt    valid utterance handle
   *              hFrontPar valid frontend input parameter handle
   *
   * Returns      non-zero on error
   *
   * See          CA_ClearUtterance
   *              CA_GetFrontendUtteranceDimension
   *              CA_LoadUtteranceFrame
   *
   ************************************************************************
   * Creates an utterance object ready for the frontend to insert frames.
   * This method should also be used to create utterances which will be
   * used for raw utterance (frame-by-frame) recognition - See
   * CA_LoadUtteranceFrame().
   *
   * Use the result from CA_GetFrontendUtteranceDimension to specify
   * the dimension.
   ************************************************************************
   */


  void CA_ClearUtterance(CA_Utterance *hUtt);
  /**
   *
   * Params       hUtt    valid utterance handle
   *
   * Returns      void
   *
   * See          CA_InitUtteranceForFrontend
   *
   ************************************************************************
   * Closes a previously opened utterance and frame buffer.
   ************************************************************************
   */


  int  CA_NextUtteranceFrame(CA_Pattern *hPattern,
                             CA_Utterance *hUtt);
  /**
   *
   * Params       hPattern    valid pattern handle
   *              hUtt        valid utterance handle
   *
   * Returns      status code
   *
   * See          CA_NextPatternFrame
   *              CA_MakePatternFrame
   *              CA_AdvanceUtteranceFrame
   *
   ************************************************************************
   * Frames can be accessed, on-by-one, from an utterance object into
   * a pattern object with this method.  It is advised that instead of
   * this function, one use CA_MakePatternFrame and CA_AdvanceUtteranceFrame
   * to access frames in sequence.
   *
   * Frames copied in with this method are un-transformed (pre-IMELDA)
   ************************************************************************
   */


  int  CA_AdvanceUtteranceFrame(CA_Utterance *hUtt);
  /**
   *
   * Params       hUtt    valid utterance handle
   *
   * Returns      True if it successfully advances the frame positon by one.
   *
   * See          CA_AdvanceRecognitionByFrame
   *
   ************************************************************************
   ************************************************************************
   */

  int  CA_UtteranceHasVoicing(CA_Utterance *hUtt);
  /**
   *
   * Params       hUtt    valid utterance handle
   *
   * Returns      non-zero if utterance has voicing data
   *
   ************************************************************************
   * This method can be polled to interrogate the state of the utterance.
   ************************************************************************
   */

  ESR_BOOL CA_IsUtteranceLockedForInput(CA_Utterance *hUtt);
  /**
   *
   * Params       hUtt    valid utterance handle
   *
   * Returns      TRUE if utterance is locked
   *
   * See          CA_LockUtteranceFromInput
   *
   ************************************************************************
   * Indicates if utterance is locked.
   ************************************************************************
   */

  void CA_UnlockUtteranceForInput(CA_Utterance *hUtt);
  /**
   *
   * Params       hUtt    valid utterance handle
   *
   * Returns      void
   *
   * See          CA_LockUtteranceFromInput
   *              CA_ResetVoicing
   *
   ************************************************************************
   * This primes the utterance object by setting it's status to ACTIVE
   ************************************************************************
   */


  void CA_LockUtteranceFromInput(CA_Utterance *hUtt);
  /**
   *
   * Params       hUtt    valid utterance handle
   *
   * Returns      void
   *
   * See          CA_UnlockUtteranceForInput
   *
   ************************************************************************
   * This signals that the utterance is complete by setting it's status to IDLE
   ************************************************************************
   */


  void CA_ResetVoicing(CA_Utterance *hUtt);
  /**
   *
   * Params       hUtt    valid utterance handle
   *
   * Returns      void
   *
   * See          CA_UnlockUtteranceForInput
   *              CA_LockUtteranceFromInput
   *
   ************************************************************************
   * This prepares the utterance object for re-use.  The 'utterance has
   * started' and 'utterance has ended' flags are cleared.
   * Unlike CA_UnlockUtteranceForInput, this does not fully clear the utterance.
   ************************************************************************
   */


  void CA_FlushUtteranceFrames(CA_Utterance *hUtt);
  /**
   *
   * Params       hUtt    valid utterance handle
   *
   * Returns      void
   *
   * See          CA_InitUtterance
   *
   ************************************************************************
   * Clears the utterance object's frame buffer
   ************************************************************************
   */


  void CA_SetEndOfUtteranceByLevelTimeout(CA_Utterance *hUtt,
                                          long timeout,
                                          long holdOff);
  /**
   *
   * Params       hUtt    valid utterance handle
   *              timeout timeout before EOU decision
   *              holdOff Period which must pass before EOU can be signalled
   *
   * Returns      void
   *
   * See          CA_UtteranceHasEnded
   *              CA_SeekStartOfUtterance
   *
   ************************************************************************
   * Sets the duration (of silence) which should pass before making
   * and end-of-utterance (EOU) decision.
   *
   * 'holdOff' is the period which must elapse before an EOU decision can
   * be made.  An EOU will not occure before 'holdOff'.
   *
   * 'timeout' anf 'holdOff' are measured in frames.
   *
   * Caution: if an utterance can be spoken within 'holdOff' frames
   *          it may be missed by the recognizer.
   ************************************************************************
   */


  int  CA_UtteranceHasEnded(CA_Utterance *hUtt);
  /**
   *
   * Params       hUtt    valid utterance handle
   *
   * Returns      non-zero if utterance has finished
   *              (we've processed the last frame and we're locked from input)
   *
   * See          CA_SeekStartOfUtterance
   *              CA_SetEndOfUtteranceByLevelTimeout
   *
   ************************************************************************
   * This method can be polled to interrogate the state of the utterance.
   ************************************************************************
   */

  /*
  **  File: utt_file.c
  */
  int  CA_SeekUtteranceFrame(CA_Utterance *hUtt,
                             int frame_no);
  /**
   *
   * Params       hUtt        valid utterance handle
   *              frame_no    new frame number (should this be unsigned long?)
   *
   * Returns      -1 on error
   *
   * See          CA_NextPatternFrame
   *
   ************************************************************************
   * The 'file pointer' is set to an explicit location
   ************************************************************************
   */


  int CA_SaveUtterance(CA_Utterance *hUtt);
  /**
   *
   * Params       hUtt    valid utterance handle
   *
   * Returns      int
   *
   ************************************************************************
   * Saves an utterance
   ************************************************************************
   */


  void CA_SaveUtteranceHeader(CA_Utterance *hUtt, int frames,
                              int samplerate, int framerate);
  /**
   *
   * Params       hUtt        valid utterance handle
   *              frames      frames which have been saved in this utt
   *              samplerate  sampling rate (typically 22050Hz)
   *              framerate   frame rate    (typically 100 frames per second)
   * Returns      void
   *
   ************************************************************************
   * Saves an utterance
   ************************************************************************
   */


  /*
  **  File: utt_data.c
  */
  int  CA_SeekStartOfUtterance(CA_Utterance *hUtt);
  /**
   *
   * Params       hUtt    valid utterance handle
   *
   * Returns      non-zero if utterance has begun
   *
   * See          CA_UtterancehasEnded
   *
   ************************************************************************
   * This method can be polled to interrogate the state of the utterance.
   ************************************************************************
   */


  int  CA_GetUnprocessedFramesInUtterance(CA_Utterance *hUtt);
  /**
   *
   * Params       hUtt    valid utterance handle
   *
   * Returns      number of un-read frames in the utterance's frame buffer
   *
   * See          CA_AdvanceRecognitionByFrame
   *
   ************************************************************************
   * Returns the number of un-read frames in the utterance's frame buffer,
   * i.e. those that have been inserted but have not yet been processed
   * by the recognizer object.
   ************************************************************************
   */


  /*
  **  File: utt_proc.c
  */
  int  CA_LoadUtteranceFrame(CA_Utterance *hUtt,
                             unsigned char* pUttFrame,
                             int uttFrameLen);
  /**
   *
   * Params       hUtt        valid utterance handle
   *              pUttFrame   Pointer utterance data
   *              uttFrameLen Number of items (features) in the frame
   *
   * Returns      zero on error
   *
   * See          CA_InitUtteranceForFrontend
   *              CA_GetFrontendUtteranceDimension
   *
   ************************************************************************
   * Inserts the frame data pointed to by 'pUttFrame' into the utterance
   * object 'hUtt'.
   *
   * The size of the frame must agree with the value used to initialise
   * the utterance object.  Utterances suitable for injection are
   * ones created for the Frontend.
   ************************************************************************
   */


  int  CA_CopyUtteranceFrame(CA_Utterance *hUtt1,
                             CA_Utterance *hUtt2);
  /**
   *
   * Params       hUtt1   Destination valid utterance handle
   *              hUtt2   Source valid utterance handle
   *
   * Returns      zero on error
   *
   ************************************************************************
   * Copies utterance frames from one utterance object to another
   ************************************************************************
   */


  void CA_CopyPatternFrame(CA_Utterance *hUtt,
                           CA_Pattern *hPattern);
  /**
      No CR equivalent function
   */


  int  CA_CalculateUtteranceStatistics(CA_Utterance *hUtt,
                                       int start,
                                       int end);
  /**
   *
   * Params       hUtt    valid utterance handle
   *              start   starting frame ID (timecode)
   *              end     final frame ID (timecode)
   *
   * Returns      Number of frames used in analysis
   *
   *
   ************************************************************************
   * Signals the collection of statistics over a pre-defined range of
   * utterance frames in the utterance's frame buffer.  do_filter flag
   * selects the frames that are likely to have just the ambient noise
   * only.
   * returns the number of frames used in the analysis.
   ************************************************************************
   */


  /*
  **  CA_Syntax methods
  */

  CA_Syntax *CA_AllocateSyntax(void);
  /**
   *
   * Params       void
   *
   * Returns      Handle to new syntax structure (object)
   *
   * See          CA_FreeSyntax
   *
   ************************************************************************
   * creates a new syntax object
   ************************************************************************
   */


  void CA_FreeSyntax(CA_Syntax *hSyntax);
  /**
   *
   * Params       hSyntax valid syntax handle
   *
   * Returns      void
   *
   * See          CA_AllocateSyntax
   *
   ************************************************************************
   * Frees an allocated syntax object
   ************************************************************************
   */

  int CA_IsEnrollmentSyntax(CA_Syntax *hSyntax);
  /**
   *
   * Params       hSyntax valid syntax handle
   *
   * Returns      int
   *
   ************************************************************************
   * tells whether a syntax is to be used for voice-enrollment
   ************************************************************************
   */

  int  CA_SetupSyntaxForRecognizer(CA_Syntax *hSyntax,
                                   CA_Recog *hRecog);
  /**
   *
   * Params       hSyntax valid syntax handle
   *              hRecog  valid recog handle
   *
   * Returns      The number of Syntax groups located
   *
   * See          CA_CeilingSyntaxForRecognizer
   *
   ************************************************************************
   * This is an essential, pre-recognition, setup method on the syntax object.
   * A syntax can be set up using more than one recognizer, provided the
   * active models have matching word indices.
   * now deprecated
   ************************************************************************
   */
    int  CA_CeilingSyntaxForRecognizer(CA_Syntax *hSyntax,
                                   CA_Recog *hRecog);
  /**
   *
   * Params       hSyntax valid syntax handle
   *              hRecog  valid recog handle
   *
   * Returns      The number of Syntax groups located
   *
   * See          CA_ClearSyntaxForRecognizer
   *
   ************************************************************************
   * This is an essential, pre-recognition, setup method on the syntax object.
   * A syntax can be set up using more than one recognizer, provided the
   * active models have matching word indices.
   * now deprecated
   ************************************************************************
   */



  void CA_ClearSyntaxForRecognizer(CA_Syntax *hSyntax,
                                   CA_Recog *hRecog);
  /**
   *
   * Params       hSyntax valid syntax handle
   *              hRecog  valid recog handle
   *
   * Returns      void
   *
   * See          CA_SetupSyntaxForRecognizer
   *              CA_UnloadRecognitionModels
   *
   ************************************************************************
   * Clears a previous syntax setup.
   ************************************************************************
   */


  int  CA_CompileSyntax(CA_Syntax *hSyntax);
  /**
   *
   * Params       hSyntax     valid syntax handle
   *
   * Returns      0 if successful, otherwise 1 to indicate faliure
   *
   * See          CA_AddWordToSyntax
   *
   ************************************************************************
   * Compiles a syntax after words have been added dynamically
   *
   ************************************************************************
   */

  /*
  **  File: syn_grou.c
  */


  void CA_GetGroupName(CA_Syntax *hSyntax,
                       int group_id,
                       char *buff,
                       int buff_len);
  /**
      No CR equivalent function.  Not called by any function
   */

  void CA_DeleteSyntaxGroups(CA_Syntax *hSyntax);
  /**
      CR function has no prototype
   */

  /*
  **  File: syn_rule.c
  */

int CA_CheckTranscription(CA_Syntax *hSyntax,
                            const char *trans,
                            int allow_group_labels);
  /**
   *
   * Params       hSyntax     valid syntax handle
   *              trans        character string phrase
    allow_group_labels  If 1, permits either words or group labels
   *
   * Returns      -1 if successful, 1 if failed, 0 if all words parsed,
    but not on a valid terminal node.
   *
   ************************************************************************
   * Use CA_CheckTranscription to check whether a phrase is valid for the
   * given syntax. Set allow_group_labels to 0 if you use single-word per group.
   *
   * NB When allow_group_labels is set to 1, there is a danger that a group
   * label might match an invalid word.
   ************************************************************************
   */

  /* It's nice to keep the Arbdata separate from the models and from the graphs
     because we may have two or more acoustic models that use the same arbdata,
     which is also justification for having the model durations in the
     acoustic model file.  Different grammars should also be able to
     share an Arbdata */
  /* new CA_Syntax functions */
int CA_AttachArbdataToSyntax(CA_Syntax* hSyntax, CA_Arbdata* arbdata);
CA_Arbdata* CA_LoadArbdata(const char* filename);
unsigned int CA_ArbdataGetModelVersionID(CA_Arbdata* ca_arbdata);
int CA_ArbdataGetModelIdsForPron(CA_Arbdata* ca_arbdata,
								 const char* pronunciation,  /* WB assumed at the edges */
								 int pronunciation_len,
								 modelID* pmodelIds);
int CA_ArbdataGetModelIdsForPIC(CA_Arbdata* ca_arbdata, const char lphon, const char cphon, const char rphon);

  /**
   * Destroys arbdata.
   *
   * @param arbdata CA_Arbdata handle
   */
void CA_FreeArbdata(CA_Arbdata* arbdata);
int CA_AddWordToSyntax(CA_Syntax* syntax, const char* slot,
                         const char *phrase, const char* pronunciation, const int weight);
int CA_ResetSyntax(CA_Syntax* syntax);
int CA_LoadSyntaxAsExtensible(CA_Syntax *hSyntax, char *synbase,
                                int num_words_to_add);
int CA_DumpSyntaxAsImage(CA_Syntax *hSyntax, const char *imagename, int version_number);
int CA_DumpSyntax(CA_Syntax *hSyntax, const char *basename);
int CA_LoadSyntaxFromImage(CA_Syntax *hSyntax, const LCHAR* filename);


  /*
  **  CA_Recog methods
  */
  /*
  **  File: rec_basi.c
  */
  CA_Recog *CA_AllocateRecognition(void);
  /**
   *
   * Params       void
   *
   * Returns      Handle to valid recognizer object, NULL on error.
   *
   * See          CA_FreeRecognition
   *
   ************************************************************************
   * Creates a reconition object.
   * This only creates the object and internal storage structures.
   * The pointer can be re-used but must be deleted when finished with.
   ************************************************************************
   */


  int CA_ConfigureRecognition(CA_Recog *hRecoc,
                              CA_RecInputParams *hRecInput);



  void CA_UnconfigureRecognition(CA_Recog *hRecog);
  /**
   *
   * Params       hRecog              valid recog handle
   *
   * Returns      void
   *
   * See          CA_ConfigureRecognition
   *
   ************************************************************************
   * Unconfigures recognizer.
   ************************************************************************
   */


  void CA_FreeRecognition(CA_Recog *hRecog);
  /**
   *
   * Params       hRecog  valid recog handle
   *
   * Returns      void
   *
   * See          CA_AllocateRecognition
   *
   ************************************************************************
   * Deletes a previously allocated recognizer
   ************************************************************************
   */


  void CA_BeginRecognition(CA_Recog *hRecog,
                           CA_Syntax *hSyntax,
                           int first_syntax_node,
                           CA_RecInputParams *hRecInput);
  /**
   *
   * Params       hRecog              valid recog handle
   *              hSyntax             valid syntax handle
   *              first_syntax_node   starting syntax node
   *              hRecInput           valid recog input parameter handle
   *
   * Returns      void
   *
   * See          CA_EndRecognition
   *              CA_LoadRecognitionParameters
   *
   ************************************************************************
   * Before a recognizer object can be used it must be initialized.
   * This method performs this task and is given an appropriate
   * syntax (and starting node) along with the required recognition
   * input parameters.
   *
   * 'first_syntax_node' should be set to '1' (the root of the syntax object).
   * Only experienced users should set this to any other value.  It is
   * a start node ID returned by CA_AddNewNode().
   *
   * It is an error to call this function without first loading
   * recognizer input parameters.
   ************************************************************************
   */


  void CA_AdvanceRecognitionByFrame(CA_Recog *hRecog,
                                    CA_Pattern *hPattern,
                                    CA_Utterance *hUtterance);
  /**
   *
   * Params       hRecog      valid recog handle
   *              hPattern    valid pattern handle
   *              hUtterance  valid utterance handle
   *
   * Returns      void
   *
   * See          CA_BeginRecognition
   *              CA_EndRecognition
   *              CA_AllocatePattern
   *              CA_AllocateUtterance
   *              CA_NextPatternFrame
   *
   ************************************************************************
   * The vocabulary and acoustic object are required by the recognition
   * object to process input frames should already have been loaded with
   * a call to one of the CA_LoadModels methods.
   *
   * This method advances the recognizer by a single frame from the
   * input stream.  This should be called when time permits and frames
   * are available in the input stream (utterance).  Typically one would
   * use the CA_Pattern object to detect the presence of unused frames,
   * This is shown below:
   *
   * while ( CA_NextPatternFrame( hPattern, hUtt ) )
   * {
   *     CA_AdvanceRecognitionByFrame( hRecog, hPattern, hUtt );
   * }
   *
   * This frame-by-frame approach provides the developer with fine control
   * of the recognition task.  In a multi-process real-time enviornment
   * where a sophisticated OS may not be present, one might want to
   * schedule other tasks between individual frames.  This was certainly
   * the case on early DSP boards chosen to host the CA recognizer.
   * The developer may, of course, decide that such fine control is not
   * required.  In such cases a higher level convenience function can easily
   * be constructed and 'wrapped' around this method and
   * which prepares the recognizer, processes all frames and then returns to
   * the caller.
   *
   * Recognition results may be issued at any time within this method,
   * It is partial traceback controls the time when results can be issued.
   * Currently results are placed in a file or to the screen - the
   * CA_Result methods provide additional flexibility.
   ************************************************************************
   */


  int  CA_EndRecognition(CA_Recog *hRecog,
                         CA_Pattern *hPattern,
                         CA_Utterance *hUtterance);
  /**
   *
   * Params       hRecog      valid recog handle
   *              hPattern    valid pattern handle
   *              hUtterance  valid utterance handle
   *
   * Returns      non-ZERO if correct termination
   *
   * See          CA_BeginRecognition
   *              CA_AdvanceRecognitionByFrame
   *  CA_DiscardRecognition
   *
   ************************************************************************
   * When input processing is deemed to be complete, this mthod must
   * be called.  It should be called after all the required input frames
   * have been advanced through.  Not more than one recognizer may be
   * terminated in this fashion when multiple recognizers are employed on
   * given utterance.  All others should be discarded.
   ************************************************************************
   */


  int CA_DiscardRecognition(CA_Recog *hRecog,
                            CA_Pattern *hPattern,
                            CA_Utterance *hUtterance);
  /**
   *
   * Params       hRecog      valid recog handle
   *              hPattern    valid pattern handle
   *              hUtterance  valid utterance handle
   *
   *
   * Returns      non-ZERO if successful
   *
   * See          CA_BeginRecognition
   *              CA_AdvanceRecognitionByFrame
   *  CA_EnddRecognition
   *
   ************************************************************************
   * To discard the current recognition process.  Not more than one
   * recognizer may be terminated in this fashion when multiple recognizers
   * are employed on given utterance.  All others should be discarded.
   ************************************************************************
   */


  void CA_UpdateCMSAccumulates(CA_Utterance *hUtt,
                               CA_Recog *hRecog);
  /**
   *
   * Params       hUtt       valid utterance handle
   *              hRecog     valid recog handle
   *
   * Returns      Nothing
   *
   * See          CA_AttachCMStoUtterane
   *              CA_CalculateCMSParameters
   *
   ************************************************************************
   * Copies channel normalization accumulates from recog to utterance.
   * These are then used when a call is made to CA_CalculateCMSParameters.
   ************************************************************************
   */

  /*
  **  File: rec_load.c
  */
  void CA_LoadModelsInAcoustic(CA_Recog *hRecog,
                               CA_Acoustic *hAcoust,
                               CA_AcoustInputParams *hAcoustInp);
  /**
   *
   * Params       hRecog      valid recog handle
   *              hAcoust     valid acoutic (model) handle
   *              hAcoustInp  valid acoustic inpit parameter handle
   *
   * Returns      void
   *
   * See          CA_LoadModelsInDictionary
   *              CA_UnloadRecognitionModels
   *              CA_LoadAcousticParameters
   *
   ************************************************************************
   * This method constructs whole or sub-word models from
   * the entire model set in preparation for recognition.
   *
   * It is an error to call this function without first loading
   * acoustic input parameters.
   ************************************************************************
   */


  void CA_LoadModelsInDictionary(CA_Recog *hRecog,
                                 CA_Vocab *hVocab,
                                 CA_Acoustic *hAcoust,
                                 CA_AcoustInputParams *hAcoustInp);



  void CA_LoadCrosswordTriphonesInSyntax(CA_Recog *hRecog,
                                         CA_Syntax *hSyntax,
                                         CA_Acoustic *hAcoust,
                                         CA_AcoustInputParams *hAcoustInp,
                                         int do_generics);
  /**
   *
   * Params       hRecog      valid recog handle
   *              hSyntax     valid syntax handle
   *              hVocab      valid vocabulary handle
   *              hAcoust     valid acoustic (model) handle
   *              hAcoustInp  valid acoustic input parameter handle
   *              do-generics
   *
   * Returns      void
   *
   * See          CA_LoadModelsInAcoustic
   *              CA_LoadModelsInDictionary
   *              CA_UnloadRecognitionModels
   *              CA_LoadAcousticParameters
   *
   ************************************************************************
   * It is an error to call this function without first loading
   * acoustic input parameters.
   ************************************************************************
   */


void CA_UnloadRecognitionModels (CA_Recog *hRecog);
/**
 *
 * Params       hRecog  valid recog handle
 *
 * Returns      void
 *
 * See          CA_LoadModelsInAcoustic
 *              CA_LoadModelsInDictionary
 *
 ************************************************************************
 * Destorys a previously loaded set of models.
 * The models could have been constructed from acoustic information,
 * dictionary (vocabulary) information or syntax (grammar) information
 ************************************************************************
 */


  void CA_LoadMonophonesInAcoustic(CA_Recog *hRecog,
                                   CA_Acoustic *hAcoust,
                                   CA_AcoustInputParams *hAcoustInp);
  /**
   *
   * Params       hRecog      valid recog handle
   *              hAcoust     valid acoustic (model) handle
   *              hAcoustInp  valid acoustic input parameter handle
   *
   * Returns      void
   *
   * See          CA_LoadModelsInAcoustic
   *              CA_LoadCrosswordTriphonesInSyntax
   *              CA_UnloadRecognitionModels
   *              CA_LoadAcousticParameters
   *
   ************************************************************************
   * This method constructs models for phoneme recognition. When using it,
   * replace the calls to CA_LoadModelsInAcoustic and
   * CA_LoadCrosswordTriphonesInSyntax with a call to
   * CA_LoadMonophonesInAcoustic.
   ************************************************************************
   */


  int CA_CompareModelIndices(CA_Recog *hRecog1,
                             CA_Recog *hRecog2);
  /**
   *
   * Params       hRecog1 valid recog handle
   *              hRecog2 valid recog handle
   *
   * Returns      0 if indices match.
   *
   * See          CA_SetupSyntaxForRecognizer
   *
   ************************************************************************
   * Checks that the model indices for two recognizers that have been set
   * up are compatible. This check should be made if the recognizers are
   * to use a shared syntax object. The check should be made prior to the
	 * calls to CA_BeginRecognition.
   ************************************************************************
   */


  int CA_FixModelIndices(CA_Recog *hRecogTgt,
                         CA_Acoustic *hAcousticSub,
                         CA_Acoustic *hAcousticWhole,
                         CA_Recog *hRecogSrc);
  /**
   *
   * Params       hRecogTgt       valid recog handle
   *              hAcousticSub    valid sub-word acoustic handle
   *              hAcousticWhole  valid whole-word acoustic handle
   *              hRecogSrc       valid recog handle
   *
   * Returns      0 if successful.
   *
   * See          CA_CompareModelIndices
   *              CA_LoadCrossWordTriphonesInSyntax
   *
   ************************************************************************
   * Us this function if the model indices for two recognizers that have
   * been set up are not compatible. This function modifies the target
   * recognizer's model indices to make them compatible with one another.
   * This means that they can safely share a syntax object without the
   * need for calling CA_LoadCrosswordTriphonesInSyntax.
   ************************************************************************
   */




  /*
  **  File: rec_addi.c
  */
  int  CA_GetRecognitionTopScore(CA_Recog *hRecog);
  /**
   *
   * Params       hRecog  valid recog handle
   *
   * Returns      top score
   *
   * See          CA_SetRecognitionTopScore
   *
   ************************************************************************
   * Reads top scoring result (used in multi-model configurations)
   ************************************************************************
   */


  void CA_SetRecognitionTopScore(CA_Recog *hRecog,
                                 int score);
  /**
   *
   * Params       hRecog  valid recog handle
   *              score   score
   *
   * Returns      void
   *
   * See          CA_GetRecognitionTopScore
   *
   ************************************************************************
   * Sets top scoring result (used in multi-model configurations)
   ************************************************************************
   */


  int  CA_GetRecognitionTerminationScore(CA_Recog *hRecog);
  /**
   *
   * Params       hRecog  valid recog handle
   *
   * Returns      top score
   *
   ************************************************************************
   * Reads top scoring final result (used in multi-model configurations)
   ************************************************************************
   */


  int  CA_TracebackPerformed(CA_Recog* hRecog);
  /**
   *
   * Params       hRecog  valid recog handle
   *
   * Returns      non-ZERO if the previous recognition frame processing
   *              resulted in the execution of a partial-traceback
   *
   * See          CA_SetupRecognizerForSyntax
   *
   ************************************************************************
   * Used to test for the need to get some results
   ************************************************************************
   */


  void CA_BlockRecognitionResults(CA_Recog *hRecog);
  /**
   *
   * Params       hRecog  valid recog handle
   *
   * Returns      void
   *
   * See          CA_UnBlockRecognitionResults
   *
   ************************************************************************
   * Disables result generation (used in multi-model configurations)
   ************************************************************************
   */


  void CA_UnBlockRecognitionResults(CA_Recog *hRecog);
  /**
   *
   * Params       hRecog  valid recog handle
   *
   * Returns      void
   *
   * See          CA_BlockRecognitionResults
   *
   ************************************************************************
   * Re-enables result generation (used in multi-model configurations)
   ************************************************************************
   */


  /*
  **  File: rec_resu.c
  */
  int  CA_FullResultScore(CA_Recog *hRecog,
                          int *score,
                          int do_incsil);



  int  CA_FullResultDuration(CA_Recog *hRecog,
                             int *dur,
                             int do_incsil);



#ifdef SWIGBUILD
  int  CA_FullResultLabel(CA_Recog *hRecog,
                          void *label,
                          int len);
  /**
   *
   * Params       hRecog  valid recog handle
   *              label   ASCII storage for returned result label
   *              len     Number of charcaters available for label.
   *                      Up to 'len'-1 characters will be returned.
   *
   * Returns      PARTIAL_RESULTS_EMPTY if no result is available for release.
   *              Otherwize the result FULL_RESULT if a valid result is available,
   *              or REJECT_RESULT if the reject score is > reject threshold.
   *
   * See          CA_FullResultScore
   *              CA_PurgeResults
   *
   ************************************************************************
   * This method returns all the Main ROOT results that have occured
   * (and have been unread) up to now.  Results reported by this method
   * will only be removed from subsequent calls after a 'purge'.
   *
   * If no results are available a return code of PARTIAL_RESULTS_EMPTY
   * is returned.  In this situation the label will be set to "<>".
   *
   * All results between 'head' and 'tail' are reported.
   ************************************************************************
   */


#else
  int  CA_FullResultLabel(CA_Recog *hRecog,
                          char *label,
                          int len);
  /**
  *
  * Params       hRecog  valid recog handle
  *              label   ASCII storage for returned result label
  *              len     Number of charcaters available for label.
  *                      Up to 'len'-1 characters will be returned.
  *
  * Returns      PARTIAL_RESULTS_EMPTY if no result is available for release.
  *              Otherwize the result FULL_RESULT if a valid result is available,
  *              or REJECT_RESULT if the reject score is > reject threshold.
  *
  * See          CA_FullResultScore
  *              CA_PurgeResults
  *
  ************************************************************************
  * This method returns all the Main ROOT results that have occured
  * (and have been unread) up to now.  Results reported by this method
  * will only be removed from subsequent calls after a 'purge'.
  *
  * If no results are available a return code of PARTIAL_RESULTS_EMPTY
  * is returned.  In this situation the label will be set to "<>".
  *
  * All results between 'head' and 'tail' are reported.
  ************************************************************************
  */
#endif

  /**
   * Strips the slot market characters from the utterance string.
   *
   * @param text String to filter
   */
  ESR_ReturnCode CA_ResultStripSlotMarkers(char *text);
  ESR_ReturnCode CA_FullResultWordIDs(CA_Recog *hRecog, wordID *wordIDs, size_t* len);

  /**
   * Gets the recognizer model id that was succesful (id is index in swimdllist file)
   *
   * @param text String to filter
   */
  ESR_ReturnCode CA_GetRecogID(CA_Recog *hRecog, int *id);


  int  CA_GetRejectMargin(CA_Recog *hRecog);
  /**
   *
   * Params               hRecog  valid recog handle
   *
   * Returns              reject margin, the differnce between the utterance score
   *                      and the reject model score.
   *
   * See          CA_FullResultLabel
   *              CA_FullResultScore
   *
   ************************************************************************
   * This method returns the reject margin, the difference between the
   * utterance score and the reject model score.
   ************************************************************************
   */

  void CA_PurgeResults(CA_Recog *hRecog);
  /**
   *
   * Params       hRecog  valid recog handle
   *
   * Returns      Nothing, internal result list will be deleted
   *
   * See          CA_FullResultLabel
   *              CA_FullResultScore
   *
   ************************************************************************
   ************************************************************************
   */


  void CA_ClearResults(CA_Recog *hRecog);
  /**
   *
   * Params       hRecog  valid recog handle
   *
   * Returns      void
   *
   * See          CA_RecognitionHasResults
   ************************************************************************
   * Clears results in a recognizer.  Should do CA_RecognitionHasResults
   * first, to see if there are results to clear.
   ************************************************************************
   */

  int CA_RecognitionHasResults(CA_Recog *hRecog);
  /**
   *
   * Params       hRecog      valid recog handle
   *              num_frames  number of silence frames before end of
   *                          utterance is considered
   *
   * Returns      void
   *
   ************************************************************************
   ************************************************************************
   */


  int  CA_IsEndOfUtteranceByResults(CA_Recog *hRecog);
  /**
   *
   * Params       hRecog  valid recog handle
   *
   * Returns
   *
   * See
   *
   ************************************************************************
   ************************************************************************
   */


  int  CA_FullResultWordCount(CA_Recog *hRecog,
                              int *count);
  /**
   *
   * Params       hRecog      valid recog handle
   *              count       pointer to storage for results word count
   *
   * Returns      PARTIAL_RESULTS_EMPTY if no result is available for release.
   *              Otherwize the result FULL_RESULT if a valid result is available,
   *              or REJECT_RESULT if the reject score is > reject threshold.
   *
   * See          CA_FullResultLabel
   *              CA_PurgeResults
   *
   ************************************************************************
   * This method returns the word count of the Main ROOT results that have
   * occured (and have been unread) up to now (silence isn't a word).
   * Results reported by this method will only be removed from subsequent
   * calls after a 'purge'.
   *
   * If no results are available a return code of PARTIAL_RESULTS_EMPTY
   * is returned.  In this situation the the count will be set to 0.
   *
   * All results between 'head' and 'tail' are reported.
   ************************************************************************
   */


  int  CA_CheckWordHyphenation(CA_Recog *hRecog,
                               int *hyphen,
                               int position);
  /**
   *
   * Params       hRecog      valid recog handle
   *              hyphen      pointer to storage for hyphenation check result
   *              position    first word position to check (zero based)
   *
   * Returns      PARTIAL_RESULTS_EMPTY if no result is available.
   *              INVALID_REQUEST if position is illegal (negative or too big).
   *              Otherwize the result FULL_RESULT.
   *
   * See          CA_FullResultLabel
   *              CA_FullResultWordCount
   *              CA_PurgeResults
   *
   ************************************************************************
   * This method checks whether two words should be hyphenated or not. This
   * check is done on the Main ROOT results that have occured (and have been
   * unread) up to now, excluding silence labels. The words are pointed out
   * by giving the position of the first word to check inside the sequence
   * (zero based, not counting silence labels).
   *
   * If no results are available, a return code of PARTIAL_RESULTS_EMPTY is
   * returned. If the given word position is illegal (either negative or too
   * big), a return code of INVALID_REQUEST is returned.
   * In these situations the hyphenation will be set to 0.
   ************************************************************************
   */


  /*
  **  File: rec_nbes.c
  */
  CA_NBestList *CA_PrepareNBestList(CA_Recog *hRecog,
                                    int n,
                                    asr_int32_t *bestScore);
  /**
   *
   * Params       hRecog      valid recog object handle
   *              n           number of items in list
   *              bestScore   The score associated with the root result
   *                          is returned in this argument.
   *
   * Returns      Handle to new N-Best object
   *
   * See          CA_DeleteNBestList
   *
   ************************************************************************
   * Creates an N-Best structure for iterating through as results arrive.
   ************************************************************************
   */


  void CA_DeleteNBestList(CA_NBestList *hNbest);
  /**
   *
   * Params       hNbest   valid nbest object handle
   *
   * Returns      Nothing, the object is no longer valid
   *
   * See          CA_PrepareNBestList
   *
   ************************************************************************
   * Deletes a previously allocated NBest list object
   ************************************************************************
   */

  LCHAR* CA_NBestListGetResultWord(CA_NBestList *hNbest, size_t iChoice);
  int  CA_NBestListGetResultConfidenceValue(CA_NBestList *hNbest, size_t iChoice, int* value);
  int  CA_NBestListRemoveResult(CA_NBestList *hNbest, int index);

   int  CA_NBestListCount(CA_NBestList *hNbest);
  /**
   *
   * Params       hNbest       valid N-Best object handle
   *
   * Returns      Number of entries found in the list.  The maximum number
   *              of entries in the list is determined when the list is created.
   *
   * See          CA_PrepareNBestList
   *
   ************************************************************************
   * Once an N-Best list has been created this method allows the caller
   * to check the number of entries in the list.
   ************************************************************************
   */

  /**
   * Returns the n'th nbest-list entry.
   *
   * @param nbest N-best list structure
   * @param index N-best list index
   * @param wordIDs Array of wordIDs making up the literal
   * @param len [in/out] Length of wordIDs argument. If the return code is ESR_BUFFER_OVERFLOW,
   *            the required length is returned in this variable.
   * @param cost Cost associated with literal
   */
  ESR_ReturnCode CA_NBestListGetResultWordIDs(CA_NBestList* nbest, size_t iChoice, wordID* wordIDs, size_t* len, asr_int32_t* cost);


  /*
  **  CA_Params methods
  */
  /*
  **  File: par_basi.c
  */
  CA_AcoustInputParams *CA_AllocateAcousticParameters(void);
  /**
   *
   * Params       void
   *
   * Returns      Handle to valid acoustic input object, NULL on error
   *
   * See          CA_FreeAcousticParameters
   *              CA_LoadAcousticWhole
   *              CA_LoadAcousticSub
   *              CA_LoadModelsInAcoustic
   *              CA_SaveAcousticSub
   *              CA_LoadModelsInDictionary
   *
   ************************************************************************
   * Creates a new acoustic input paramater onject
   ************************************************************************
   */


  void CA_FreeAcousticParameters(CA_AcoustInputParams *hAcoustInp);
  /**
   *
   * Params       hAcoustInp  valid acoustic input handle
   *
   * Returns      Nothing, the acoustic input handle is no longer valid
   *
   * See          CA_AllocateAcousticParameters
   *
   ************************************************************************
   * Removes a previously allocated parameter object
   ************************************************************************
   */


  CA_PatInputParams *CA_AllocatePatternParameters(void);
  /**
   *
   * Params       void
   *
   * Returns      Handle to a new pattern object, NULL on error
   *
   * See          CA_FreePatternParameters
   *              CA_LoadPattern
   *
   ************************************************************************
   * Creates a new pattern input paramater onject
   ************************************************************************
   */


  void CA_FreePatternParameters(CA_PatInputParams *hPatInp);
  /**
   *
   * Params       hPatInp valid pattern input handle
   *
   * Returns      void    The input handle is no longer valid.
   *
   * See          CA_AllocatePatternParameters
   *
   ************************************************************************
   * Removes a previously allocated parameter object
   ************************************************************************
   */


  CA_RecInputParams *CA_AllocateRecognitionParameters(void);
  /**
   *
   * Params       void
   *
   * Returns      Handle to new recog input object
   *
   * See          CA_FreeRecognitionParameters
   *              CA_BeginRecognition
   *
   ************************************************************************
   * Creates a new recog input paramater onject
   ************************************************************************
   */


  void CA_FreeRecognitionParameters(CA_RecInputParams *hRecInp);
  /**
   *
   * Params       hRecInp valid recog input handle
   *
   * Returns      void    the recognition input object is no longer valid
   *
   * See          CA_AllocateRecognitionParameters
   *
   ************************************************************************
   * Removes a previously allocated parameter object
   ************************************************************************
   */



  /*
  **  File: par_load.c
  */
  void CA_LoadAcousticParameters(CA_AcoustInputParams *hAcoustInp,
                                 const char *parfile);
  /**
   *
   * Params       hAcoustInp  valid acoustic input handle
   *              parfile     parameter (.INI) file to read
   *
   * Returns      void
   *
   * See          CA_SaveAcousticParameters
   *              CA_LoadAcousticWhole
   *              CA_LoadAcousticSub
   *              CA_LoadModelsInAcoustic
   *              CA_SaveAcousticSub
   *              CA_LoadModelsInDictionary
   *
   ************************************************************************
   * Loads known acoustic parameters from the given parameter file.
   * The file is an ASCII text file usually of the type .PAR or .INI
   ************************************************************************
   */


  void CA_SaveAcousticParameters(CA_AcoustInputParams *hAcoustInp,
                                 const char *parfile);
  /**
   *
   * Params       hAcoustInp  valid acoustic input handle
   *              parfile     parameter (.INI) file to write
   *
   * Returns      void
   *
   * See          CA_LoadAcousticParameters
   *
   ************************************************************************
   * Saves a previously loaded (modified) parameter file
   *
   * It is an error to call this function without first loading
   * acoustic input parameters.
   ************************************************************************
   */


  void CA_LoadPatternParameters(CA_PatInputParams *hPatInp,
                                const char *parfile);
  /**
   *
   * Params       hPatInp valid pattern handle
   *              parfile parameter (.INI) file to read
   *
   * Returns      void
   *
   * See          CA_SavePatternParameters
   *              CA_LoadPattern
   *
   ************************************************************************
   * Loads known pattern parameters from the given parameter file.
   * The file is an ASCII text file usually of the type .PAR or .INI
   ************************************************************************
   */


  void CA_SavePatternParameters(CA_PatInputParams *hPatInp,
                                const char *parfile);
  /**
   *
   * Params       hPatInp valid pattern input handle
   *              parfile parameter (.INI) file to write
   *
   * Returns      void
   *
   * See          CA_LoadPatternParameters
   *              CA_LoadPattern
   *
   ************************************************************************
   * Saves a previously loaded (modified) parameter file
   *
   * It is an error to call this function without first loading
   * pattern input parameters.
   ************************************************************************
   */


  void CA_LoadRecognitionParameters(CA_RecInputParams *hRecInp,
                                    const char *parfile);
  /**
   *
   * Params       hRecInp valid recog input handle
   *              parfile parameter (.INI) file to read
   *
   * Returns      void
   *
   * See          CA_AllocateRecognitionParameters
   *              CA_SaveRecognitionParameters
   *              CA_BeginRecognition
   *
   ************************************************************************
   * Loads known recognition parameters from the given parameter file.
   * The file is an ASCII text file usually of the type .PAR or .INI
   ************************************************************************
   */


  void CA_SaveRecognitionParameters(CA_RecInputParams *hRecInp,
                                    const char *parfile);
  /**
   *
   * Params       hRecInp valid recog input handle
   *              parfile parameter (.INI) file to write
   *
   * Returns      void
   *
   * See          CA_LoadRecognitionParameters
   *              CA_BeginRecognition
   *
   ************************************************************************
   * Saves a previously loaded (modified) parameter file
   ************************************************************************
   */


  /*
  **  File: par_set.c
  */

  int CA_GetAcousticParameter(CA_AcoustInputParams *hAcoustInp,
                              char *key,
                              void *value);
  /**
   *
   * Params       hAcoustInp  valid acoustic input handle
   *              key         parameter key (text label)
   *              value       pointer to store parameter value (text)
   *              valueLen    size of value buffer
   *
   * Returns      False on error
   *
   * See          CA_GetAcousticIntParameter
   *              CA_GetAcousticFloatParameter
   *              CA_LoadAcousticParameters
   *
   ************************************************************************
   * Gets a known acoustic parameter.
   *
   * It is an error to call this function without first loading
   * acoustic input parameters.
   ************************************************************************
   */


  int CA_GetAcousticStringParameter(CA_AcoustInputParams *hAcoustInp,
                                    char *key,
                                    char *value,
                                    int valueLen,
                                    int *bytes_required);
  /**
   *
   * Params       hAcoustInp  valid acoustic input handle
   *              key         parameter key (text label)
   *              value       pointer to store parameter value (text)
   *              valueLen    size of value buffer
   *
   * Returns      False on error
   *
   * See          CA_GetAcousticIntParameter
   *              CA_GetAcousticFloatParameter
   *              CA_LoadAcousticParameters
   *
   ************************************************************************
   * Gets a known acoustic parameter.
   *
   * It is an error to call this function without first loading
   * acoustic input parameters.
   ************************************************************************
   */

  int CA_SetPatternParameter(CA_PatInputParams *hPatInp,
                             char *key,
                             char *value);
  /**
   *
   * Params       hPatInp valid Pattern Input handle
   *              key     parameter key (text label)
   *              value   new parameter value (text)
   *
   * Returns      Zero on error
   *
   * See          CA_GetPatternStringParameter
   *              CA_GetPatternIntParameter
   *              CA_GetPatternFloatParameter
   *              CA_LoadPatternParameters
   *
   ************************************************************************
   * Sets/Modifies a known Pattern Input parameter.
   *
   * It is an error to call this function without first loading
   * pattern input parameters.
   ************************************************************************
   */


  int CA_GetPatternParameter(CA_PatInputParams *hPatInp,
                             char *key,
                             void *value);
  /**
   *
   * Params       hPatInp valid Pattern Input handle
   *              key     parameter key (text label)
   *              value   pointer to store parameter value (int)
   *
   * Returns      False on error
   *
   * See          CA_SetPatternParameter
   *              CA_LoadPatternParameters
   *
   ************************************************************************
   * Reads a known Pattern Input Parameter.
   *
   * It is an error to call this function without first loading
   * pattern input parameters.
   ************************************************************************
   */


  int CA_GetPatternStringParameter(CA_PatInputParams *hPatInp,
                                   char *key,
                                   char *value,
                                   int valueLen,
                                   int *bytes_required);
  /**
   *
   * Params       hPatInp     valid Pattern Input handle
   *              key         parameter key (text label)
   *              value       pointer to store parameter value (text)
   *              valueLen    size of value buffer
   *
   * Returns      False on error
   *
   * See          CA_SetPatternParameter
   *              CA_LoadPatternParameters
   *
   ************************************************************************
   * Reads a known Pattern Input Parameter.
   *
   * It is an error to call this function without first loading
   * pattern input parameters.
   ************************************************************************
   */


  int CA_SetRecognitionParameter(CA_RecInputParams *hRecInp,
                                 char *key,
                                 char *value);
  /**
   *
   * Params       hRecInp valid recog input handle
   *              key     parameter key (text label)
   *              value   new parameter value (text)
   *
   * Returns      Zero on error
   *
   * See          CA_GetRecognitionStringParameter
   *              CA_GetRecognitionIntParameter
   *              CA_GetRecognitionFloatParameter
   *              CA_LoadRecognitionParameters
   *
   ************************************************************************
   * Sets/Modifies a known recognition parameter.
   *
   * It is an error to call this function without first loading
   * recognizer input parameters.
   ************************************************************************
   */

  int CA_GetRecognitionParameter(CA_RecInputParams *hRecInp,
                                 char *key,
                                 void *value);
  /**
   *
   * Params       hRecInp valid recog input handle
   *              key     parameter key (text label)
   *              value   pointer to store parameter value (int)
   *
   * Returns      False on error
   *
   * See          CA_SetRecognitionParameter
   *              CA_BeginRecognition
   *              CA_LoadRecognitionParameters
   *
   ************************************************************************
   * Reads a known recognition parameter.
   *
   * It is an error to call this function without first loading
   * recognizer input parameters.
   ************************************************************************
   */


  int CA_GetRecognitionStringParameter(CA_RecInputParams *hRecInp,
                                       char *key,
                                       char *value,
                                       int valueLen,
                                       int *bytes_required);
#if USE_CONFIDENCE_SCORER

  CA_ConfidenceScorer* CA_AllocateConfidenceScorer(void);

  /**
   *
   * Params       void
   *
   * Returns      Handle to new ConfidenceScorer structure, or NULL on error.
   *
   * See          CR_FreeConfidenceScorer
   *
   ************************************************************************
   * Allocates a new SymbolMatch structure (object)
   ************************************************************************
   */

  void CA_FreeConfidenceScorer(CA_ConfidenceScorer *hConfidenceScorer);


  int CA_LoadConfidenceScorer(CA_ConfidenceScorer* hConfidenceScorer);
  /**
   *
   * Params       hConfidenceScorer   Handle to previously allocated ConfidenceScorer object
   *  nnet_file     Name of the confidence scoring file
   *
   * Returns      True if successful, False in the case of an error
   *
   * See
   *
   ************************************************************************
   ************************************************************************
   */

  void CA_UnloadConfidenceScorer(CA_ConfidenceScorer *hConfidenceScorer);
  /**
   *
   * Params       hConfidenceScorer   Handle to previously allocated ConfidenceScorer object
   *
   * Returns
   *
   * See
   *
   ************************************************************************
   ************************************************************************
   */

  int CA_ComputeConfidenceValues(CA_ConfidenceScorer* hConfidenceScorer,
                                CA_Recog* recog, CA_NBestList *nbestlist);
   /**
   *
   * Params       hConfidenceScorer   Handle to previously allocated ConfidenceScorer object
   *              recog               Handle to recognizer that won
   *              nbestlist           Handle to nbest list where confidence values will be inserted
   *
   * Returns      0 if successful, 1 if not.
   *
   *
   ************************************************************************
   ************************************************************************
   */

#endif /* #if USE_CONFIDENCE_SCORER */

#ifdef __cplusplus
}
#endif

#endif
