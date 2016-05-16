/*---------------------------------------------------------------------------*
 *  lts.h  *
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



#ifndef _LTS_H__
#define _LTS_H__

#if defined (GEN_STATIC_SLTS) && defined (WIN32)
#include <stdio.h>
#endif

#define MTAG NULL

typedef void* FSM_DICT_HANDLE;

typedef void* LTS_HANDLE;

typedef void* PHONEMAP_TABLE_HANDLE;

/*
  creates an instance of the LTS and loads its data from file provided.
  Returns NULL if error.
*/
SWIsltsResult create_lts(char *data_filename, LTS_HANDLE *phLts);

/*
  deallocates an instance of LTS
*/
SWIsltsResult free_lts(LTS_HANDLE hLts);

/*
  runs letter_to_sound rules.

  Fills up phones in phone_string.  This needs to be allocated by calling 
  function to max_length.  Each elements of phone string needs to be 4 
  characters long (output can be 3 characters plus need room for
  EOS)

  return length of phone string.  
  If max_phone_length is exceeded, truncates output and returns max_phone_length.
  
  Returns -1 if error
*/
SWIsltsResult run_lts(LTS_HANDLE h, FSM_DICT_HANDLE hdict, char *input_sentence, char **output_phone_string, int *phone_length);

/* static code generator for LTS structure */
#if defined (GEN_STATIC_SLTS) && defined (WIN32)
void gen_static_lts(LTS_HANDLE h, const char *name, FILE *fp_out);
#endif

#if defined (GEN_STATIC_FSMD) && defined (WIN32)
void gen_static_fsmd(FSM_DICT_HANDLE h, const char *name, FILE *fp_out);
#endif

typedef struct SWIsltsEngine {
  LTS_HANDLE m_hLts;

  FSM_DICT_HANDLE m_hDict;

} SLTS_Engine;


#endif /* _LTS_H__ */
