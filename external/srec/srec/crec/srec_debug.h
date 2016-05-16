/*---------------------------------------------------------------------------*
 *  srec_debug.h  *
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

#ifndef _SREC_DEBUG_H_
#define _SREC_DEBUG_H_

#include "srec.h"
#include "word_lattice.h"

void print_altword_token(srec* rec, altword_token* b, char* msg);
#define sanity_check_altwords(ReC,aWtOkEn)


void print_fsmnode_token(srec* rec, ftokenID token_index, char* msg);
void print_fsmnode_token_list(srec* rec, stokenID token_index, char* msg);
void print_search_status(srec* rec);
void print_fsmarc_token(srec* rec, stokenID token_index, char* msg);
void print_fsmarc_token_list(srec* rec, stokenID token_index, char* msg);

#endif
