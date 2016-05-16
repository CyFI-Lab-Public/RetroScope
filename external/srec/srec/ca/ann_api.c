/*---------------------------------------------------------------------------*
 *  ann_api.c  *
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

#include <stdlib.h>
#include <string.h>
#ifndef _RTT
#include <stdio.h>
#endif

#ifdef unix
#include <unistd.h>
#endif
#include <assert.h>

#include "duk_args.h"
#ifndef _RTT
#include "duk_io.h"
#endif

#include "simapi.h"
#include "portable.h"
#include "fpi_tgt.h"
#include "fronttyp.h"
#include "sh_down.h"

#define REMOVE_QUIET_CHUNKS 1

/*****************************************************************************************
 *
 *****************************************************************************************/
CA_Annotation* CA_AllocateAnnotation(void)
{
  CA_Annotation* hAnnotation = NULL;

  TRY_CA_EXCEPT

  hAnnotation = (CA_Annotation*) VAR_ALLOCATE_CLR(1, sizeof(CA_Annotation), "ca.CA_Annotation");
  hAnnotation->data = allocate_annotation();
  hAnnotation->has_segments = False;
  hAnnotation->ca_rtti = CA_ANNOTATION_SIGNATURE;
  hAnnotation->label = NULL;
  hAnnotation->data->snr = 0;
  return hAnnotation;
  BEG_CATCH_CA_EXCEPT;
  END_CATCH_CA_EXCEPT(hAnnotation);
}


/*****************************************************************************************
 *
 *****************************************************************************************/
void CA_FreeAnnotation(CA_Annotation* hAnnotation)
{
  TRY_CA_EXCEPT

  ASSERT(hAnnotation);

  if (hAnnotation->has_segments)
  {
    SERVICE_ERROR(ANNOTATE_SEGMENTS_EXIST);
  }

  free_annotation(hAnnotation->data);
  VAR_FREE(hAnnotation, "hAnnotation");

  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hAnnotation)
}


/*****************************************************************************************
 *
 *****************************************************************************************/
void CA_ClearAnnotation(CA_Annotation* hAnnotation)
{
  TRY_CA_EXCEPT

  /*    if( !hAnnotation->has_segments )
      {
   SERVICE_ERROR ( ANNOTATE_NO_SEGMENTS );
      }
  */
  annotation_delete_segment_info(hAnnotation->data);
  if (hAnnotation->label)
    VAR_FREE(hAnnotation->label, "annotation_label");
  hAnnotation->label = NULL;
  hAnnotation->has_segments = False;

  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hAnnotation)
}


/*****************************************************************************************
 *
 *****************************************************************************************/
int CA_AnnotateFromResults(CA_Annotation* hAnnotation, CA_Recog *hRecog)
{
  TRY_CA_EXCEPT

  int seg_cnt = 0;

  ASSERT(hRecog);
  ASSERT(hAnnotation);

  if (hAnnotation->has_segments)
  {
    SERVICE_ERROR(ANNOTATE_SEGMENTS_EXIST);
  }
  seg_cnt =  annotation_from_results(hAnnotation->data, &hRecog->rec);

  if (hAnnotation->data->numWords > 0)
  {
    annotation_decorate_labels(hAnnotation->data, hAnnotation->label);
    hAnnotation->has_segments = True;
  }

  return (hAnnotation->data->numWords);

  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hAnnotation)
}


/*****************************************************************************************
 *
 *****************************************************************************************/
int CA_AnnotateFromVoicing(CA_Annotation* hAnnotation, CA_Utterance *hUtterance, CA_Pattern *hPattern)
{
  TRY_CA_EXCEPT

  ASSERT(hAnnotation);
  ASSERT(hUtterance);
  ASSERT(hPattern);

  if (hAnnotation->has_segments)
  {
    SERVICE_ERROR(ANNOTATE_SEGMENTS_EXIST);
  }

  annotation_from_voicing(hAnnotation->data, &hUtterance->data, hPattern->data.prep);

  if (hAnnotation->data->numWords > 0)
  {
    hAnnotation->has_segments = True;
  }

  return (hAnnotation->data->numWords);

  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hAnnotation)
}


/*****************************************************************************************
 *
 *****************************************************************************************/
void CA_SaveAnnotation(CA_Annotation* hAnnotation, char* FileName)
{
  TRY_CA_EXCEPT
#ifndef _RTT

  ASSERT(hAnnotation);

  if (!hAnnotation->has_segments)
  {
    SERVICE_ERROR(ANNOTATE_NO_SEGMENTS);
  }
  save_annotations(hAnnotation->data, FileName);
  return;
#else
  log_report("RTT not in module\n");
  SERVICE_ERROR(FEATURE_NOT_SUPPORTED);
  return;
#endif
  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hAnnotation)

}


/*****************************************************************************************
 *
 *****************************************************************************************/
void CA_SetAnnotationLabel(CA_Annotation *hAnnotation, char *label)
{
  TRY_CA_EXCEPT

  ASSERT(hAnnotation);
  ASSERT(label);

  if ((strchr(label, '<') || (strchr(label, '>'))))
    SERVICE_ERROR(BAD_LABEL);
  if (hAnnotation->label)
    VAR_FREE(hAnnotation, "annotation_label");
  hAnnotation->label = NULL;
  hAnnotation->label = (char *) VAR_ALLOCATE(strlen(label) + 1, sizeof(char), "ca.annotation_label");
  strcpy(hAnnotation->label, label);

  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hAnnotation)
}


/*****************************************************************************************
 *
 *****************************************************************************************/

int CA_GetAnnotationLabel(CA_Annotation *hAnnotation, char *label, int length)
{
  int required_length;

  TRY_CA_EXCEPT

  ASSERT(hAnnotation);
  ASSERT(label);

  if (length <= 0)
    SERVICE_ERROR(BAD_ARGUMENT);
  if (hAnnotation->label)
  {
    required_length = strlen(hAnnotation->label) + 1;
    if (required_length > length)
    {
      if (length > 1)
      {
        strncpy(label, hAnnotation->label, length - 2);
        label[length - 1] = '\0';
      }
      else
        *label = '\0';
      return (required_length);
    }
    else
      strcpy(label, hAnnotation->label);
    return (0);
  }
  *label = '\0';
  return (0);

  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hAnnotation)
}


/*****************************************************************************************
 *
 *****************************************************************************************/
int CA_CompareAnnotations(CA_Annotation *testAnnotation, CA_Annotation *refAnnotation)
{
  TRY_CA_EXCEPT

  int score;

  ASSERT(testAnnotation);
  ASSERT(refAnnotation);

  if ((!testAnnotation->has_segments) || (!refAnnotation->has_segments))
  {
    SERVICE_ERROR(ANNOTATE_NO_SEGMENTS);
  }

  score = annotation_compare(testAnnotation->data, refAnnotation->data);

  return (score);

  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(testAnnotation)
}


/*****************************************************************************************
 *
 *****************************************************************************************/
int CA_GetAnnotationSegmentCount(CA_Annotation *hAnnotation)
{
  TRY_CA_EXCEPT

  ASSERT(hAnnotation);

  if (!hAnnotation->has_segments)
    return (0);
  ASSERT(hAnnotation->data);
  return (hAnnotation->data->numWords);

  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hAnnotation)
}


/*****************************************************************************************
 *
 *****************************************************************************************/
int CA_SegmentUtterance(CA_Annotation* hAnnotation, CA_Utterance* hUtt,
                        CA_Pattern* hPattern)
{
  TRY_CA_EXCEPT
  int seg_cnt = 0;
  int ii;
  int total_length;
  int num_segments_to_keep = 0;
  int no_beep = False;
  int has_trailing_silence = False;
#if REMOVE_QUIET_CHUNKS
  int jj;
  featdata *peakC0;
  int max_peak;
#endif
  ASSERT(hAnnotation);
  ASSERT(hUtt);
  ASSERT(hPattern);
  ASSERT(hPattern->data.prep);

#if DO_SUBTRACTED_SEGMENTATION
  if (hPattern->data.prep->is_setup_for_noise == False)
  {
    SERVICE_ERROR(PATTERN_NOT_SETUP_FOR_NOISE);
  }
#endif
  if (hUtt->data.utt_type == FILE_OUTPUT)
    SERVICE_ERROR(UTTERANCE_INVALID);
  if (hUtt->data.utt_type != FILE_INPUT &&
      hUtt->data.utt_type != LIVE_INPUT)
  {
    SERVICE_ERROR(UTTERANCE_NOT_INITIALISED);
  }
  if (isFrameBufferActive(hUtt->data.gen_utt.frame))
  {
    SERVICE_ERROR(FB_INVALID_STATE);
  }
  if (!hAnnotation->label)
  {
    SERVICE_ERROR(ANNOTATE_NO_LABEL);
  }
  if (hAnnotation->has_segments)
  {
    SERVICE_ERROR(ANNOTATE_SEGMENTS_EXIST);
  }

  hAnnotation->has_segments = False;
  /* Segment the utterance using DP on c0 */
  seg_cnt =  annotation_segment_utterance(hAnnotation->data,
                                          &hUtt->data,
                                          hPattern->data.prep,
                                          hAnnotation->label, &has_trailing_silence);
  if (seg_cnt <= 0)
  {
    if (seg_cnt < 0)
      return (seg_cnt);
    return (NO_SEGMENTS_FOUND);
  }
  /*  SPECIAL OPERATIONS:
  **  Now manipulate the segmentation to order:
  1. Remove parts of a multi-segment utt that are % below some energy
   measure of the "loudest" segment. NB this may remove a final chunk
   that previously caused a NO_TRAILING_SILENCE
      2. Declare it invalid if the first chunk is within n frames of the start.
      3. Delete unwanted leading segment(s). The criteria are:
          3a. Min/max segment duration.
          3b. Min silence gap to next chunk
      4. Check min/max overall length.
   NB check MAX then look for trailing silence.
  */
#if REMOVE_QUIET_CHUNKS
  /* 1. Remove parts of a multi-segment utt that are % below some energy
  measure of the "loudest" segment. */
  /* TODO :Add par hPattern->data.prep->end.min_segment_rel_c0  */
  if ((hPattern->data.prep->end.min_segment_rel_c0  > 0) && (seg_cnt > 1))
  {
    peakC0 = (featdata*) VAR_ALLOCATE_CLR(seg_cnt, sizeof(featdata), "ca.peakC0");
    max_peak = 0;
    for (ii = 0; ii < seg_cnt; ii++)
    {
      peakC0[ii] = get_c0_peak_over_range(hUtt->data.gen_utt.frame,
                                          hAnnotation->data->tcp[ii].begin,
                                          hAnnotation->data->tcp[ii].begin
                                          + hAnnotation->data->tcp[ii].end);
      if (peakC0[ii] > max_peak)
      {
        max_peak = peakC0[ii];
      }
    }
    max_peak = max_peak - hPattern->data.prep->end.min_segment_rel_c0;
    for (ii = 0; ii < seg_cnt; ii++)
    {
      if (peakC0[ii] < max_peak)
      {
        annotation_delete_segment(hAnnotation->data, ii);
        /* reset flag if last seg is deleted */
        if (!has_trailing_silence && (ii == seg_cnt - 1))
        {
          has_trailing_silence = True;
        }
        for (jj = ii; jj < seg_cnt - 1; jj++)
        {
          peakC0[jj] = peakC0[jj+1];
        }
        seg_cnt--;
        ii--;
      }
    }
    VAR_FREE((char *) peakC0, "peakC0");
    if (seg_cnt < 1)
    {

      return (seg_cnt);
    }
  }
#endif

  /*
  **  2. Declare it invalid if the first chunk is within n frames of the start.
  */
  if (hAnnotation->data->tcp[0].begin
      < hPattern->data.prep->end.min_initial_quiet_frames)
  {
    annotation_delete_segment_info(hAnnotation->data);
    hAnnotation->has_segments = False;
    return (INITIAL_SILENCE_NOT_FOUND) ;
  }

  /*
  **  3. Delete unwanted leading segment(s) - a beep, for example.
  */
  if (hPattern->data.prep->end.delete_leading_segments > 0)
  {
    if (hPattern->data.prep->end.leading_segment_accept_if_not_found)
      num_segments_to_keep = 1;
    else
      num_segments_to_keep = 0;
    if (!annotation_delete_leading_segments(hAnnotation->data,
                                            hPattern->data.prep->end.delete_leading_segments,
                                            num_segments_to_keep,
                                            hPattern->data.prep->end.leading_segment_min_frames,
                                            hPattern->data.prep->end.leading_segment_max_frames,
                                            hPattern->data.prep->end.leading_segment_min_silence_gap_frames))
    {
      if (hPattern->data.prep->end.leading_segment_accept_if_not_found)
        no_beep = True;
      else
      {
        annotation_delete_segment_info(hAnnotation->data);
        hAnnotation->has_segments = False;
        return (INITIAL_SEGMENT_NOT_IDENTIFIED) ;
      }
    }
  }

  /*
  **  4. Check min/max overall length. "Beep" has already been deleted.
  */
  total_length = 0;
  for (ii = 0; ii < hAnnotation->data->numWords; ii++)
    total_length += hAnnotation->data->tcp[ii].end;
  if (total_length > hPattern->data.prep->end.max_annotation_frames)
  {
    annotation_delete_segment_info(hAnnotation->data);
    hAnnotation->has_segments = False;
    return (ANNOTATION_TOO_LONG);
  }
  /* Check for TOO LONG before checking for trailing silence - recognizer gernerally
  terminates on speech when it runs out of hypos. BP */
  if (!has_trailing_silence)
  {
    annotation_delete_segment_info(hAnnotation->data);
    hAnnotation->has_segments = False;
    return (NO_TRAILING_SILENCE);
  }
  if (total_length < hPattern->data.prep->end.min_annotation_frames)
  {
    annotation_delete_segment_info(hAnnotation->data);
    hAnnotation->has_segments = False;
    return (ANNOTATION_TOO_SHORT);
  }

  if (hAnnotation->data->numWords > 0)
  {
    annotation_decorate_labels(hAnnotation->data, hAnnotation->label);
    hAnnotation->has_segments = True;
  }
#if DEBUG
  if (no_beep)
    log_report("W: Initial segment not found\n");
#endif
  ASSERT(hAnnotation->data->numWords > 0);
  return (hAnnotation->data->numWords);
  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hAnnotation)
}

int CA_GetAnnotationSNR(CA_Annotation* hAnnotation)
{
  TRY_CA_EXCEPT

  ASSERT(hAnnotation);
#if DO_SUBTRACTED_SEGMENTATION
  return hAnnotation->data->snr;
#endif
  /* SERVICE_ERROR (FEATURE_NOT_SUPPORTED); */
  log_report("W: CA_GetAnnotationSNR - function not supported.\n");
  return 0;
  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hAnnotation)

}

/*****************************************************************************************
 * TODO : wrap this into a lower level function
 *****************************************************************************************/
void CA_GetAnnotationData(CA_Annotation* hAnnotation, int id,
                          int* begin, int* end, char* buff, int buffLen)
{
  TRY_CA_EXCEPT

  ASSERT(hAnnotation);

  if ((id < 0) || (id >= hAnnotation->data->numWords))
    SERVICE_ERROR(BAD_INDEX);
  if (!hAnnotation->has_segments)
  {
    SERVICE_ERROR(ANNOTATE_NO_SEGMENTS);
  }

  annotation_get_data(hAnnotation->data, id, begin, end, buff, buffLen);

  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hAnnotation)
}


/*****************************************************************************************
 *
 *****************************************************************************************/
int CA_AddUttSegmentsToAcousticWhole(CA_Acoustic  *hAcoust, CA_Pattern *hPattern,
                                     CA_Utterance *hUtt, CA_Annotation *hAnnotation)
{
  int retCode;
  TRY_CA_EXCEPT

  ASSERT(hAcoust);
  ASSERT(hPattern);
  ASSERT(hUtt);
  ASSERT(hAnnotation);
  if (hPattern->setup_whole)
    SERVICE_ERROR(PATTERN_ALREADY_SETUP);

  if (!hAnnotation->has_segments)
  {
    SERVICE_ERROR(ANNOTATE_NO_SEGMENTS);
  }
  if (!hAnnotation->label)
    SERVICE_ERROR(ANNOTATE_NO_LABEL);
  if (CA_IsAcousticWholeModelExtant(hAcoust, hAnnotation->label))
    return (0); /* TODO: ERROR CODE? */

  if (!hAcoust->acc.base_model)
  {
    hAcoust->acc.base_model = allocate_hmm(0, &hAcoust->acc.base_sort);
    hAcoust->acc.base_model->dim = hPattern->data.prep->dim;
    hAcoust->acc.num_pars = hAcoust->acc.base_model->dim;
    hAcoust->acc.mod_type = SEGM;
    hAcoust->acc.mod_style = SEG_STYLE;
#if USE_CONTEXT_MASK
    construct_context_groups_for_wholeword(&hAcoust->acc);
#endif

  }
  else
    if (hAcoust->acc.base_model->dim != hPattern->data.prep->dim)
      SERVICE_ERROR(UTTERANCE_DIMEN_MISMATCH);
  /* Redundant. We now add the whole set of segments.
  if ((id < 0) || (id >= hAnnotation->data->numWords))
  SERVICE_ERROR ( BAD_INDEX );
  */
  /* This fn now returns number of models added */
  retCode = annotation_add_utt_segment_to_acoustic(&hAcoust->acc,
            hPattern->data.prep,
            &hUtt->data,
            hAnnotation->data,
            hAnnotation->label,
            hAcoust->has_backup_means);
  if (retCode > 0)
  {
    hAcoust->is_loaded = True;
  }
  return retCode;
  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hAcoust)
}



int CA_LocateBeepInUtterance(CA_Annotation* hAnnotation,
                             CA_Utterance* hUtterance,
                             CA_Utterance* hBeepUtterance,
                             CA_Pattern *hPattern,
                             int beep_start_point)
{
  int start, end;
  int success;
  TRY_CA_EXCEPT

  ASSERT(hUtterance);
  ASSERT(hBeepUtterance);
  ASSERT(hAnnotation);
  ASSERT(hAnnotation->data);
  ASSERT(hPattern->data.prep);

  start = end = 0;
  annotation_create_tcp_entry(hAnnotation->data, NULL); /* null label avoids alloc */
  success = detect_beep_by_shape(hPattern->data.prep, &hBeepUtterance->data,  &hUtterance->data,
                                 &start, &end);
  if (success >= 0)
  {
    hAnnotation->data->tcp[0].begin = start;
    hAnnotation->data->tcp[0].end = end - start;
  }
  /* If beep detection fails, back off to a fixed speech start point */
  else
  {
    hAnnotation->data->tcp[0].begin = beep_start_point;
    hAnnotation->data->tcp[0].end = hPattern->data.prep->end.beep_size;
  }
  annotation_decorate_labels(hAnnotation->data, hAnnotation->label);
  hAnnotation->has_segments = True;
  /*
  ** Call fn to diff the utts' c0 profiles here.
  ** Stuff the annotation object with the beep start and end points.
  */
  if (success >= 0)
    return (True);
  else
    return (False);

  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hAcoust)
}


int CA_CorrectAnnotationForBeep(CA_Annotation* hAnnotation,
                                CA_Annotation* hBeepAnnotation)
{
  int beep_start;
  int beep_end;
  int chopped_frames;

  TRY_CA_EXCEPT

  ASSERT(hAnnotation);
  ASSERT(hBeepAnnotation);
  ASSERT(hAnnotation->data);
  ASSERT(hBeepAnnotation->data);
  ASSERT(hAnnotation->label);
  /*
  ** Correct annotation's start point if necessary by moving it
  ** forward of the end of the beep.
  */
  if ((hAnnotation->data->numWords <= 0) || (hBeepAnnotation->data->numWords <= 0))
    return (hAnnotation->data->numWords);
  ASSERT(hBeepAnnotation->data->numWords == 1); /* valid? */

  /* NB speech end is length, not endpoint! */
  beep_start = hBeepAnnotation->data->tcp[0].begin;
  beep_end = hBeepAnnotation->data->tcp[hBeepAnnotation->data->numWords-1].begin
             + hBeepAnnotation->data->tcp[hBeepAnnotation->data->numWords-1].end;

#if DEBUG && 0
  printf("Speech1 begin %d, Speech1 end %d\nBeep begin %d\tBeep end\n",
         hAnnotation->data->tcp[0].begin,
         hAnnotation->data->tcp[0].begin + hAnnotation->data->tcp[0].end,
         beep_start, beep_end);


#endif


  /* Delete all speech segments that finish before or at the end of beep.
  WARNING: You must re-label the annotation segments so that they begin
  at 0! i.e. {Name[0] ~Name[1] }Name[2]
  */
  while ((hAnnotation->data->numWords > 0)
         && ((hAnnotation->data->tcp[0].begin + hAnnotation->data->tcp[0].end) <= beep_end))
  {
    annotation_delete_segment(hAnnotation->data, 0);
  }
  if (hAnnotation->data->numWords == 0)
    return (hAnnotation->data->numWords);

  /* Speech before end of beep? - chop it out.
  */
  if (beep_end > hAnnotation->data->tcp[0].begin)
  {
    ASSERT(beep_end <= (hAnnotation->data->tcp[hAnnotation->data->numWords-1].begin
                        + hAnnotation->data->tcp[hAnnotation->data->numWords-1].end)); /* valid? */
    chopped_frames = beep_end - hAnnotation->data->tcp[0].begin;
    hAnnotation->data->tcp[0].begin += chopped_frames;
    hAnnotation->data->tcp[0].end -= chopped_frames;
    ASSERT(hAnnotation->data->tcp[0].end >= 0);
  }
  annotation_decorate_labels(hAnnotation->data, hAnnotation->label);
  return (hAnnotation->data->numWords);
  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hAcoust)
}

int CA_TruncateAnnotation(CA_Annotation* hAnnotation,
                          int start)
{
  int speech_end;
  TRY_CA_EXCEPT

  ASSERT(hAnnotation);
  ASSERT(hAnnotation->data);
  /*
  ** Correct annotation's start point if necessary by moving it
  ** forward of "start".
  */
  if (hAnnotation->data->numWords <= 0)
    return (hAnnotation->data->numWords);
  /* NB speech end is length, not endpoint! */
  speech_end = hAnnotation->data->tcp[hAnnotation->data->numWords-1].begin
               + hAnnotation->data->tcp[hAnnotation->data->numWords-1].end;
  /* Delete all speech segments that finish before end of beep
  */
  while ((hAnnotation->data->numWords > 0)
         && (hAnnotation->data->tcp[0].begin + hAnnotation->data->tcp[0].end < start))
  {
    annotation_delete_segment(hAnnotation->data, 0);
  }
  if (hAnnotation->data->numWords == 0)
    return (hAnnotation->data->numWords);
  /* Speech before end of beep? - chop it out.
  */
  if (start > hAnnotation->data->tcp[0].begin)
  {
    ASSERT(start <= hAnnotation->data->tcp[hAnnotation->data->numWords-1].begin
           + hAnnotation->data->tcp[hAnnotation->data->numWords-1].end); /* valid? */
    hAnnotation->data->tcp[0].begin = start;
  }
  return (hAnnotation->data->numWords);
  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hAcoust)
}
