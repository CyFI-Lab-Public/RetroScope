/*---------------------------------------------------------------------------*
 *  fsm_dictionary.h  *
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


#ifndef _FSM_DICTIONARY_H__
#define _FSM_DICTIONARY_H__

#include "lts_seq_internal.h"

#define MAX_FSM_WORD_LEN 30
#define MAX_FSM_PRON_LEN 100  

typedef struct FSM_DICTIONARY {
  PM *phone_mapping;
  LM *letter_mapping;
#ifdef TI_DSP
	unsigned short *fsm_array;
#else
	unsigned char *fsm_array;  
#endif

	int fsm_size;
} FSM_DICTIONARY;


/* function declarations */
SWIsltsResult load_fsm_dictionary(const char *dict_file, FSM_DICTIONARY ** pd);
SWIsltsResult free_fsm_dictionary(FSM_DICTIONARY * d);
int get_lts_pron(FSM_DICTIONARY *d, char *word, char **pron, int max_pron_len);

#endif /* _FSM_DICTIONARY_H__ */

