/*---------------------------------------------------------------------------*
 *  mutualob.h  *
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


#ifndef _h_mutualob_
#define _h_mutualob_

#include "frontpar.h"

#ifndef SWIGBUILD

typedef front_parameters CA_FrontendInputParams;

typedef struct
{
  int    ca_rtti;
  /* IMPORTANT NOTE
   *
   * This structure is used by 'CA_WritePatternImage()'
   * in the file "ca/cod_imag.c". Certain assumptions
   * about the member types and order are made.
   * DON'T change this structure without making
   * appropriate changes to the above function.
   */
  
  booldata    is_setup;
  booldata    is_attached;
  utterance_info data;
}
CA_Utterance;
#endif

#define CA_SIGNATURE_PATTERN       0xca00
#define CA_VERIFY_SIGNATURE(X)       ((unsigned long)(X) & 0xca00)

#define CA_ACOUSTIC_SIGNATURE                       0xca01
#define CA_ACOUSTIC_PARAMETERS_SIGNATURE            0xca02
#define CA_ARRAY_SIGNATURE                          0xca03
#define CA_ANNOTATION_SIGNATURE                     0xca04
#define CA_EXCEPTION_SIGNATURE                      0xca05
#define CA_FRONTEND_SIGNATURE                       0xca06
#define CA_FRONTEND_PARAMETERS_SIGNATURE            0xca07
#define CA_NBEST_LIST_SIGNATURE                     0xca08
#define CA_PATTERN_SIGNATURE                        0xca09
#define CA_PATTERN_PARAMETERS_SIGNATURE             0xca0a
#define CA_RECOGNIZER_SIGNATURE                     0xca0b
#define CA_RECOGNIZER_PARAMETERS_SIGNATURE          0xca0c
#define CA_SYNTAX_SIGNATURE                         0xca0d
#define CA_UTTERANCE_SIGNATURE                      0xca0e
#define CA_VOCABULARY_SIGNATURE                     0xca0f
#define CA_WAVE_SIGNATURE                           0xca10
#define CA_RESEARCH_SIGNATURE                       0xca11

#endif
