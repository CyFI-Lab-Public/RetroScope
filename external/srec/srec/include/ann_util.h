/*---------------------------------------------------------------------------*
 *  ann_util.h  *
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

#ifndef _ANNOTATE_UTITITIES_INCLUDED_
#define _ANNOTATE_UTITITIES_INCLUDED_


#include "utteranc.h"
#include "c42mul.h"

#ifdef SET_RCSID
static const char col_util_h[] = "$Id: ann_util.h,v 1.1.10.4 2007/08/31 17:44:52 dahan Exp $";
#endif
#define TCP_BLOCK_SIZE 10

/**
 * @todo document
 */
typedef struct
{
  annotate  *tcp;
  int   numWords;
  int   NumFrames;
  int                 snr;
}
annotate_info;


annotate_info* allocate_annotation(void);
void annotation_create_tcp_entry(annotate_info* hAnnotation ,
                                 char* label);
                                 
void free_annotation(annotate_info* hAnnotation);



int annotation_delete_segment(annotate_info* hAnnotation, int id);
int annotation_delete_leading_segments(annotate_info* hAnnotation,
                                       int num_to_delete, int num_to_keep, int min_length, int max_length,
                                       int min_sil_dur);
void annotation_delete_segment_info(annotate_info* hAnnotation);
void annotation_decorate_labels(annotate_info* hAnnotation, char *label);
int annotation_segment_utterance(annotate_info* hAnnotation,
                                 utterance_info* utt,
                                 preprocessed *prep,
                                 char *label,
                                 int *has_trailing_silence);
                                 
int annotation_add_utt_segment_to_acoustic(model_info *acc,
    preprocessed *prep,
    utterance_info* utt,
    annotate_info* hAnnotation,
    char* base_label,
    int do_backup);
int  annotation_from_voicing(annotate_info * hAnnotation,
                             utterance_info * utt,
                             preprocessed * prep);
int annotation_compare(annotate_info *test_annotation,
                       annotate_info *ref_annotation);
void save_annotations(annotate_info* hAnnotation, char* FileName);
int annotation_from_results(annotate_info *hAnnotation, srec *rec);
void annotation_get_data(annotate_info* hAnnotation, int id,
                         int* begin, int* end,  char* buff, int buffLen);
int construct_syntax_for_annotation(syntax_info *rule, annotate_info* hAnnotation);

int find_beep_in_utterance(annotate_info *hAnnotation, utterance_info *utt,
                           utterance_info *beep_utt, preprocessed *prep, char *label);
int detect_beep_by_shape(preprocessed  *prep, utterance_info *utt1,
                         utterance_info *utt2, int *start, int *end);
#endif 
