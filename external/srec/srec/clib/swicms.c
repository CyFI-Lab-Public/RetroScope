/*---------------------------------------------------------------------------*
 *  swicms.c                                                                 *
 *                                                                           *
 *  Copyright 2007, 2008 Nuance Communciations, Inc.                         *
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

#include <string.h>
#include"swicms.h"
#include"srec_sizes.h"
#include"prelib.h"

#include "passert.h"
#include "ESR_Session.h"
#include "ESR_SessionType.h"
#include "IntArrayList.h"
#include "portable.h"

#define printf_vector(HEAD, FMT, PTR, NN) { int i; LCHAR buffer[256]; sprintf(buffer, HEAD); sprintf(buffer + LSTRLEN(buffer), " %x", (int)PTR); for (i=0; i<(NN); ++i) sprintf(buffer + LSTRLEN(buffer), FMT, PTR[i]); PLogMessage(buffer); }

/* Cross-utterance CMN calculation:
   We try to normalize the speech frames before they get to the recognizer.
   The speech frames are LDA-processed mfcc-with-dynamic feature vectors.
   We collect these speech frames during recognition. At the end of
   recognition we exclude the silence frames from the collected data, and
   generate a new channel average based on the previous average and the new
   data, using an exponential decay formula.

   In-utterance CMN calculation:
   A new short-term average mechanism was introduced, with faster update,
   to improve recognition on the very first recognition after init or reset.
   We wait for a minimum number of new data frames to apply this. We also
   disable the fast updater after some frames, because we assume the
   cross-utterance estimator to be more reliable, particularly in its
   ability to exclude silence frames from the calculation.
*/

/* default settings for cross-utterance cms */
#define SWICMS_FORGET_FACTOR_DEFAULT        400 /* effective frms of history */
#define SWICMS_SBINDEX_DEFAULT              100 /* use speech frames only */
/* #define SWICMS_CACHE_RESOLUTION_DEFAULT  see swicms.h */
/* #define SWICMS_CACHE_SIZE_DEFAULT        see swicms.h */

/* default settings for in-utterance cms */
#define SWICMS_INUTT_FORGET_FACTOR2_DISABLE 65535 /* any large number */
#define SWICMS_INUTT_FORGET_FACTOR2_DEFAULT SWICMS_INUTT_FORGET_FACTOR2_DISABLE
/* disable this when cross-utt become more reliable */
#define SWICMS_INUTT_DISABLE_AFTER_FRAMES   200
/* wait while the estimate is poor */
#define SWICMS_INUTT_ENABLE_AFTER_FRAMES    10

/**
 * Logging Stuff
 */
#define LOG_LEVEL 2
#define MODULE_NAME L("swicms.c")
//static const char* MTAG = MODULE_NAME;

static const char *rcsid = 0 ? (const char *) &rcsid :
                           "$Id: swicms.c,v 1.21.6.16 2008/06/05 19:00:55 stever Exp $";

static ESR_BOOL SWICMS_DEBUG = ESR_FALSE;

/* these are good values from cmn/tmn files */
static const imeldata gswicms_cmn1_8 [MAX_CHAN_DIM] =
  {
    158, 141,  99, 125, 101, 162, 113, 138, 128, 143, 123, 141,
    127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127,
    127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127
  };

static const imeldata gswicms_cmn1_11 [MAX_CHAN_DIM] =
  {
    163, 121, 120, 114, 124, 139, 144, 108, 150, 119, 146, 124,
    127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127,
    127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127
  };

static const imeldata gswicms_tmn1_8 [MAX_CHAN_DIM] =
  {
    108, 138, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128,
    127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127,
    127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127
  };

static const imeldata gswicms_tmn1_11 [MAX_CHAN_DIM] =
  {
    108, 138, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128,
    127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127,
    127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127
  };

static ESR_ReturnCode GetSomeIntsIfAny( const LCHAR* parname, imeldata* parvalue, size_t reqSize)
{
  size_t i, size;
  ESR_ReturnCode rc;
  ESR_BOOL exists;
  IntArrayList* intList = 0;

  CHKLOG(rc, ESR_SessionContains(parname, &exists));
  if (exists) {
    rc = ESR_SessionGetProperty(parname, (void**)&intList, TYPES_INTARRAYLIST);
    if (rc != ESR_SUCCESS && rc != ESR_NO_MATCH_ERROR) {
      /* no match will revert to default data already in static array */
      PLogError(L("Error reading %s from session: %s"), parname, ESR_rc2str(rc));
      return ESR_FATAL_ERROR;
    }
    else if (rc == ESR_SUCCESS) {
      CHKLOG(rc, IntArrayListGetSize(intList, &size));
      if(size != reqSize) {
	PLogError(L("Error reading %s from session, expected len %d: %s"), parname, reqSize, ESR_rc2str(rc));
	return ESR_FATAL_ERROR;
      }
      if(reqSize == 1)
	CHKLOG(rc, IntArrayListGet(intList, 0, parvalue));
      else {
	for (i=0; i<size; ++i)
	  CHKLOG(rc, IntArrayListGet(intList, i, &parvalue[i]));
      }
    }
  }
  return ESR_SUCCESS;
 CLEANUP:
  return rc;
}

int swicms_init(swicms_norm_info* swicms)
{
  ESR_ReturnCode    rc = ESR_SUCCESS;
  size_t            i;
  ESR_BOOL          exists, sessionExists;
  size_t 	    sample_rate;

  /* defaults */
  swicms->sbindex          = SWICMS_SBINDEX_DEFAULT;
  swicms->cached_num_frames = 0;
  swicms->forget_factor    = SWICMS_FORGET_FACTOR_DEFAULT;
  swicms->cache_resolution = SWICMS_CACHE_RESOLUTION_DEFAULT;
  swicms->num_frames_in_cmn = 0;

  CHKLOG(rc, ESR_SessionExists(&sessionExists));

  if (sessionExists)
  {  /* We'll assume this rate is valid or someone else will be complaining.   SteveR */
    rc = ESR_SessionGetSize_t ( L ( "CREC.Frontend.samplerate" ), &sample_rate );

    if ( rc != ESR_SUCCESS )
      return ( rc );
  }
  else
    sample_rate = 11025;

  /* init the data structures by copying the static data so that we can have a copy if we need to reset */
  if ( sample_rate == 8000 )
  {
    for ( i = 0; i < MAX_CHAN_DIM; i++ )
    {
      swicms->cmn [i] = gswicms_cmn1_8 [i];
      swicms->tmn [i] = gswicms_tmn1_8 [i];
// _lda_*mn below are OK, but are recalculated in swicms_lda_process()
      swicms->lda_cmn [i] = 0; /* calculated by swicms_lda_process() */
      swicms->lda_tmn [i] = 0; /* calculated by swicms_lda_process() */
    }
  }
  else
  {
    for ( i = 0; i < MAX_CHAN_DIM; i++ )
    {
      swicms->cmn [i] = gswicms_cmn1_11 [i];
      swicms->tmn [i] = gswicms_tmn1_11 [i];
// _lda_*mn below are OK, but are recalculated in swicms_lda_process()
      swicms->lda_cmn [i] = 0; /* calculated by swicms_lda_process() */
      swicms->lda_tmn [i] = 0; /* calculated by swicms_lda_process() */
    }
  }
  CHKLOG(rc, ESR_SessionExists(&sessionExists));

  if (sessionExists)
  {
    const LCHAR* parname = L("CREC.Frontend.swicms.debug");
    CHKLOG(rc, ESR_SessionContains(parname, &exists));
    if (exists) {
      rc = ESR_SessionGetBool(parname, &SWICMS_DEBUG);
      if (rc != ESR_SUCCESS && rc != ESR_NO_MATCH_ERROR) {
        PLOG_DBG_ERROR((L("Error reading %s from session: %s"), parname, ESR_rc2str(rc)));
        return rc;
      }
    }

    rc = GetSomeIntsIfAny( L("CREC.Frontend.swicms.forget_factor"),
			   &swicms->forget_factor, 1);
    if(rc != ESR_SUCCESS) return rc;

    rc = GetSomeIntsIfAny( L("CREC.Frontend.swicms.sbindex"),
			   &swicms->sbindex, 1);
    if(rc != ESR_SUCCESS) return rc;

    rc = GetSomeIntsIfAny( L("CREC.Frontend.swicms.cmn"),
			   &swicms->cmn[0], MAX_CHAN_DIM);
    if(rc != ESR_SUCCESS) return rc;

    if ( sample_rate == 8000 )
    {
      rc = GetSomeIntsIfAny( L("CREC.Frontend.swicms.cmn8"), &swicms->cmn[0], MAX_CHAN_DIM);

      if(rc != ESR_SUCCESS)
        return rc;
    }
    else
    {
      rc = GetSomeIntsIfAny( L("CREC.Frontend.swicms.cmn11"), &swicms->cmn[0], MAX_CHAN_DIM);

      if(rc != ESR_SUCCESS)
        return rc;
    }

    rc = GetSomeIntsIfAny( L("CREC.Frontend.swicms.tmn"),
			   &swicms->tmn[0], MAX_CHAN_DIM);
    if(rc != ESR_SUCCESS) return rc;
  }

  swicms->is_valid = 0;
  for (i = 0; i < MAX_CHAN_DIM; i++)
    swicms->adjust[i] = 255;

#ifdef SREC_ENGINE_VERBOSE_LOGGING
  PLogMessage("swicms->forget_factor    = %d\n", swicms->forget_factor);
  PLogMessage("swicms->cache_resolution = %d\n", swicms->cache_resolution);
  PLogMessage("swicms->sbindex          = %d\n", swicms->sbindex);
#endif

  /* in-utt cms parameters */
  swicms->inutt.forget_factor2 = SWICMS_INUTT_FORGET_FACTOR2_DEFAULT;
  swicms->inutt.disable_after  = 200;
  swicms->inutt.enable_after   = 10;    /* in-utt is less reliable       */
  swicms->inutt.num_bou_frames_to_skip = 20; /* silence frames! see windback */
  swicms->inutt.num_frames_since_bou = 0;
  swicms->inutt.num_frames_in_accum = 0;
  for(i=0; i<MAX_CHAN_DIM; i++) swicms->inutt.accum[i] = 0;

  if (sessionExists) {
    rc = GetSomeIntsIfAny(L("CREC.Frontend.swicms.inutt.forget_factor2"),
			  &swicms->inutt.forget_factor2, 1);
    if(rc != ESR_SUCCESS) return rc;

    rc = GetSomeIntsIfAny(L("CREC.Frontend.swicms.inutt.disable_after"),
			  &swicms->inutt.disable_after, 1);
    if(rc != ESR_SUCCESS) return rc;

    rc = GetSomeIntsIfAny(L("CREC.Frontend.swicms.inutt.enable_after"),
			  &swicms->inutt.enable_after, 1);
    if(rc != ESR_SUCCESS) return rc;

    /* we need to estimate the in-utt cmn from speech frames only! so let's
       make sure to skip some frames before collecting data, */
    ESR_SessionContains(L("CREC.Frontend.start_windback"), &exists);
    if (exists) {
      ESR_BOOL do_skip_even_frames = ESR_TRUE;
      ESR_SessionGetBool(L("CREC.Frontend.do_skip_even_frames"), &do_skip_even_frames);
      ESR_SessionGetInt(L("CREC.Frontend.start_windback"), &swicms->inutt.num_bou_frames_to_skip);
      if( do_skip_even_frames)
	swicms->inutt.num_bou_frames_to_skip /= 2;
      swicms->inutt.num_bou_frames_to_skip -= 5; /* ensure spch frames only */
    }
  }

  return 0;
 CLEANUP:
  return rc;
}


ESR_ReturnCode swicms_get_cmn ( swicms_norm_info* swicms, LCHAR *cmn_params, size_t* len )
{
  int dim_count;
  int i;
  imeldata temp[MAX_CHAN_DIM];
  const size_t INT_LENGTH = 12;

  if (  swicms->_prep != NULL )	/* lda exists give them transformed lda. */
  {
    for ( dim_count = 0; dim_count < MAX_CHAN_DIM; dim_count++ )
      temp [dim_count] = swicms->lda_cmn [dim_count];
    inverse_transform_frame( swicms->_prep, temp, 1 /*do_shift*/);
  }
  else	/* lda does not exist give them raw cmn values */
  {
    for ( dim_count = 0; dim_count < MAX_CHAN_DIM; dim_count++ )
      temp [dim_count] = swicms->cmn [dim_count];
  }

  for ( dim_count = 0, i = 0; dim_count < MAX_CHAN_DIM; dim_count++ )
  {
    i += sprintf( cmn_params + i, dim_count==0 ? "%d" : ",%d", temp [dim_count] );
    if (i + INT_LENGTH >= *len) {
        *len = MAX_CHAN_DIM * (INT_LENGTH + 2) * sizeof(LCHAR);
        return ESR_BUFFER_OVERFLOW;
    }
  }

  return ESR_SUCCESS;
}


ESR_ReturnCode swicms_set_cmn ( swicms_norm_info* swicms, const char *cmn_params )
{
  ESR_ReturnCode    set_status;
  int               length_of_params;
  int               dim_count;
  int               got_word;
  int               current_position;
  char              *copy_of_params;
  char              *parsed_strings [MAX_CHAN_DIM];
  int               temp_cmn [MAX_CHAN_DIM];

  length_of_params = strlen ( cmn_params ) + 1;
  copy_of_params = (char*)MALLOC ( length_of_params, NULL );

  if ( copy_of_params != NULL )
  {
    set_status = ESR_SUCCESS;
    memcpy ( copy_of_params, cmn_params, length_of_params );
    dim_count = 0;
    current_position = 0;
    got_word = 0;
    parsed_strings [dim_count] = copy_of_params + current_position;

    while ( ( dim_count < MAX_CHAN_DIM ) && ( set_status == ESR_SUCCESS ) )
    {
      switch ( *( copy_of_params + current_position ) )
      {
        case '\0':
          if ( got_word == 1 )
          {
            if ( dim_count == ( MAX_CHAN_DIM - 1 ) )
              dim_count++;
            else
            {
              PLogError ( "Channel Normalization : Missing Params Must Contain %d Params\n", MAX_CHAN_DIM );
              set_status = ESR_INVALID_ARGUMENT;
            }
          }
          else
          {
            PLogError ( "Channel Normalization : Missing Params Mus Contain %d Params\n", MAX_CHAN_DIM );
            set_status = ESR_INVALID_ARGUMENT;
          }
          break;

        case ',':
          if ( got_word == 1 )
          {
            if ( dim_count < ( MAX_CHAN_DIM - 1 ) )
            {
              dim_count++;
              *( copy_of_params + current_position) = '\0';
              current_position++;

              if ( current_position == length_of_params )
              {
                PLogError ( "Channel Normalization : Delimiter At End Of Param String\n" );
                set_status = ESR_INVALID_ARGUMENT;
              }
              parsed_strings [dim_count] = copy_of_params + current_position;
              got_word = 0;
            }
            else
            {
              PLogError ( "Channel Normalization : Too Many Params Must Contain %d Params\n", MAX_CHAN_DIM );
              set_status = ESR_INVALID_ARGUMENT;
            }
          }
          else
          {
            PLogError ( "Channel Normalization : Too Many Params Must Contain %d Params\n", MAX_CHAN_DIM );
            set_status = ESR_INVALID_ARGUMENT;
          }
          break;

        case '0':
        case '1':
        case '2':
        case '3':
        case '4':
        case '5':
        case '6':
        case '7':
        case '8':
        case '9':
          got_word = 1;
          current_position++;

          if ( current_position == length_of_params )
          {
            PLogError ( "Channel Normalization : Too Many Params Must Contain %d Params\n", MAX_CHAN_DIM );
            set_status = ESR_INVALID_ARGUMENT;
          }
          break;

        default:
          PLogError ( "Channel Normalization : Invalid Param : %c : Params Must Contain Only Digits\n" );
          set_status = ESR_INVALID_ARGUMENT;
          break;
      }
    }
    if ( set_status == ESR_SUCCESS )
    {
      dim_count = 0;

      while ( ( dim_count < MAX_CHAN_DIM ) && (  set_status == ESR_SUCCESS ) )
      {
        temp_cmn [dim_count] = atoi ( parsed_strings [dim_count] );

        if ( ( temp_cmn [dim_count] < 0 ) || ( temp_cmn [dim_count] > 255 ) )
        {
          set_status = ESR_INVALID_ARGUMENT;
        }
      }
      if ( set_status == ESR_SUCCESS )
      {
        for ( dim_count = 0; dim_count < MAX_CHAN_DIM; dim_count++ )
          swicms->cmn [dim_count] = temp_cmn [dim_count];
        if ( swicms->_prep != NULL )	/* Set now if NULL it will automatically be set on first utterance */
          linear_transform_frame(swicms->_prep, swicms->lda_cmn, 1 /*do_shift*/);
      }
    }
    FREE ( copy_of_params );
  }
  else
  {
    PLogError ( "Channel Normalization Out Of Memory Error\n" );
    set_status = ESR_OUT_OF_MEMORY;
  }
  swicms->num_frames_in_cmn = 0;
  return ( set_status );
}


int swicms_cache_frame(swicms_norm_info* swicms, imeldata* frame, int dimen)
{
  int i;
  imeldata *pcache, *pframe;

  ASSERT(dimen == MAX_CHAN_DIM);
  i = swicms->cached_num_frames / swicms->cache_resolution;
  if (i < SWICMS_CACHE_SIZE_DEFAULT)
  {
    pcache = swicms->cached_sections[ i];
    if (swicms->cached_num_frames % swicms->cache_resolution == 0)
    {
      for (i = 0; i < MAX_CHAN_DIM; i++) *pcache++ = 0;
      pcache -= MAX_CHAN_DIM;
    }
    pframe = frame;
    for (i = 0; i < MAX_CHAN_DIM; i++) *pcache++ += *pframe++;
    swicms->cached_num_frames++;
  }

  return 0;
}

int apply_channel_normalization_in_swicms(swicms_norm_info *swicms,
    imeldata* oframe,
    imeldata* iframe, int dimen)
{
  int ii;
  ASSERT(dimen == MAX_CHAN_DIM);

  /* IF inutt is activated at all */
  if(swicms->inutt.forget_factor2 != SWICMS_INUTT_FORGET_FACTOR2_DISABLE) {
    /* AND IF we have not disabled it (due to x-utt more reliable) */
    if(swicms->inutt.num_frames_in_accum < swicms->inutt.disable_after) {
      /* AND IF we have skipped past the silence frames */
      if( swicms->inutt.num_frames_since_bou >= swicms->inutt.num_bou_frames_to_skip){
	swicms->inutt.num_frames_in_accum++;
	for(ii=0;ii<dimen;ii++) swicms->inutt.accum[ii] += iframe[ii];
	/* AND IF we've already seen at least 10 frames (presumably) of speech */
	if(swicms->inutt.num_frames_in_accum>swicms->inutt.enable_after) {
	  /* THEN we update the adjustment in-line with the current utterance! */
	  for(ii=0;ii<dimen;ii++) {
	    imeldata denom = ( swicms->inutt.forget_factor2
			       + swicms->inutt.num_frames_in_accum );
	    /* tmp: weighted average of the old lda_cmn and the new accum */
	    imeldata tmp=(swicms->lda_cmn[ii]*swicms->inutt.forget_factor2
			  + swicms->inutt.accum[ii] + denom/2) / denom;
	    swicms->adjust[ii] = swicms->lda_tmn[ii] - tmp;
	  }
	  //printf_vector("swicms->adjust2 "," %d",swicms->adjust, dimen);
	}
      }
    }
    swicms->inutt.num_frames_since_bou++;
  }

  for (ii = 0; ii < dimen; ii++)
    oframe[ii] = MAKEBYTE(iframe[ii] + swicms->adjust[ii]);
  return 0;
}

int swicms_update(swicms_norm_info* swicms, int speech_start, int speech_end)
{
  int i, j;
  asr_int32_t speech_avg[MAX_CHAN_DIM], backgr_avg[MAX_CHAN_DIM], avg[MAX_CHAN_DIM];
  int ff;
  int nn, speech_nn, backgr_nn;
  int num_frames = swicms->cached_num_frames;
  int cache_start, cache_end, backgr_cache_end;
  int sbindex = swicms->sbindex;

  /* init for utterance */
  swicms->inutt.num_frames_since_bou = 0;

  swicms->cached_num_frames = 0;
  cache_start = speech_start;
  cache_start -= (cache_start % swicms->cache_resolution);
  cache_start /= swicms->cache_resolution;

  if (speech_end == MAXframeID)
  {
    cache_end = SWICMS_CACHE_SIZE_DEFAULT;
  }
  else
  {
    if (speech_end < num_frames)
      cache_end = speech_end;
    else
      cache_end = num_frames;
    cache_end -= (cache_end % swicms->cache_resolution);
    cache_end /= swicms->cache_resolution;
  }

  if (num_frames == 0 || speech_end == 0 || speech_start == speech_end || speech_end == MAXframeID)
  {
    if (speech_end != 0 || speech_start != 0)
      PLogError("Warning: speech_bounds (%d,%d) swicms->cached_num_frames (%d)\n",
                speech_start, speech_end, num_frames);
	if (SWICMS_DEBUG) {
      //printf_vector("swicms->adjust.rep", " %d", swicms->adjust, MAX_CHAN_DIM);
    }
    return 1;
  }

  backgr_cache_end = (num_frames - num_frames % swicms->cache_resolution) / swicms->cache_resolution;

  speech_nn = (cache_end - cache_start) * swicms->cache_resolution;
  backgr_nn = backgr_cache_end * swicms->cache_resolution - speech_nn;

  for (i = 0; i < MAX_CHAN_DIM; i++)
  {
    speech_avg[i] = 0;
    backgr_avg[i] = 0;
    for (j = cache_start; j < cache_end; j++)
      speech_avg[i] += swicms->cached_sections[j][i];
    for (j = 0; j < cache_start; j++)
      backgr_avg[i] += swicms->cached_sections[j][i];
    for (j = cache_end; j < backgr_cache_end; j++)
      backgr_avg[i] += swicms->cached_sections[j][i];
    if (speech_nn == 0 && backgr_nn > 0)
    {
      backgr_avg[i] /= backgr_nn;
      speech_avg[i] = backgr_avg[i];
      speech_nn = backgr_nn;
    }
    else if (speech_nn > 0 && backgr_nn == 0)
    {
      speech_avg[i] /= speech_nn;
      backgr_avg[i] = speech_avg[i];
      backgr_nn = speech_nn;
    }
    else if (speech_nn > 0 && backgr_nn > 0)
    {
      speech_avg[i] /= speech_nn;
      backgr_avg[i] /= backgr_nn;
    }
    else
    {
      return 0;
    }

    avg[i] = (sbindex * speech_avg[i] + (100 - sbindex) * backgr_avg[i] + 50) / 100;
  }
  nn = (sbindex * speech_nn + (100 - sbindex) * backgr_nn + 50) / 100;

  for (i = 0, ff = 0; i < MAX_CHAN_DIM; i++)
  {
    ff += (swicms->lda_tmn[i] - avg[i]);
  }
  ff /= MAX_CHAN_DIM; /* sum is now the average offset from TMN */
  if (ff > 5)
  {
    PLogError("Warning: bad utt mean during swicms_update() (moffs=%d)\n", ff);
    //printf_vector("swicms->adjust.rep", " %d", swicms->adjust, MAX_CHAN_DIM);
    return 1;
  }
  ff = swicms->forget_factor;
  if (ff < 9999)
  {
    for (i = 0; i < MAX_CHAN_DIM; i++)
    {
      swicms->lda_cmn[i] = (swicms->lda_cmn[i] * ff + avg[i] * nn + (ff + nn) / 2)  / (ff + nn);
      swicms->adjust[i] = swicms->lda_tmn[i] - swicms->lda_cmn[i];
    }
  }

  if (SWICMS_DEBUG)
    {
      imeldata temp[MAX_CHAN_DIM];
      PLogMessage("swicms_update() used %d frames (%d-%d)", nn, speech_start, speech_end);

      for(i=0;i<MAX_CHAN_DIM;i++) temp[i]=swicms->lda_cmn[i];
      inverse_transform_frame( swicms->_prep, temp, 1 /*do_shift*/);
      /* use this dump, to put back into CREC.Frontend.swicms.cmn */
      printf_vector("swicms.cmn(r)  ", " %d", temp, MAX_CHAN_DIM);

      //printf_vector("swicms.lda_cmn   ", " %d", &swicms.lda_cmn [0], MAX_CHAN_DIM);
      //printf_vector("swicms.lda_tmn   ", " %d", &swicms.lda_tmn [0], MAX_CHAN_DIM);
      //printf_vector("swicms->adjust", " %d", swicms->adjust, MAX_CHAN_DIM);
      //printf_vector("avg.speech    ", " %d", avg, MAX_CHAN_DIM);
    }
  else
    {
#ifndef NDEBUG
      //printf_vector("swicms->adjust", " %d", swicms->adjust, MAX_CHAN_DIM);
#endif
    }
  swicms->num_frames_in_cmn += nn;
  return 0;
}

int swicms_lda_process(swicms_norm_info* swicms, preprocessed* prep)
{
  int i;

  for (i = 0; i < MAX_CHAN_DIM; i++) swicms->lda_tmn[i] = swicms->tmn[i];
  for (i = 0; i < MAX_CHAN_DIM; i++) swicms->lda_cmn[i] = swicms->cmn[i];
  linear_transform_frame(prep, swicms->lda_tmn, 1 /*do_shift*/);
  linear_transform_frame(prep, swicms->lda_cmn, 1 /*do_shift*/);

  for (i = 0; i < MAX_CHAN_DIM; i++)
  {
    swicms->adjust[i] = swicms->lda_tmn[i] - swicms->lda_cmn[i];
  }

#ifndef NDEBUG
  //printf_vector("swicms->adjust", " %d", swicms->adjust, MAX_CHAN_DIM);
#endif
  swicms->is_valid = 1;
  swicms->_prep = prep;

  if(SWICMS_DEBUG) {
    imeldata temp[MAX_CHAN_DIM];
    printf_vector("swicms->cmn     ", " %d", swicms->cmn,     MAX_CHAN_DIM);
    printf_vector("swicms->lda_cmn ", " %d", swicms->lda_cmn, MAX_CHAN_DIM);
    //printf_vector("swicms->tmn     ", " %d", swicms->tmn,     MAX_CHAN_DIM);
    //printf_vector("swicms->lda_tmn ", " %d", swicms->lda_tmn, MAX_CHAN_DIM);
    //printf_vector("swicms->adjust  ", " %d", swicms->adjust,  MAX_CHAN_DIM);

    //for(i=0;i<MAX_CHAN_DIM;i++) temp[i]=swicms->lda_tmn[i];
    //inverse_transform_frame( swicms->_prep, temp, 1 /*do_shift*/);
    //printf_vector("swicms->tmn(r)  ", " %d", temp, MAX_CHAN_DIM);

    for(i=0;i<MAX_CHAN_DIM;i++) temp[i]=swicms->lda_cmn[i];
    inverse_transform_frame( swicms->_prep, temp, 1 /*do_shift*/);
    printf_vector("swicms->cmn(r)  ", " %d", temp, MAX_CHAN_DIM);
  }
  return 0;
}



