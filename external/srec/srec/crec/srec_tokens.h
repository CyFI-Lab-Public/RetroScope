/*---------------------------------------------------------------------------*
 *  srec_tokens.h  *
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

#ifndef _SREC_TOKENS_H_
#define _SREC_TOKENS_H_

#include"srec.h"
#include"srec_sizes.h"

/* miscdata what_to_do_if_fails */
#define EXIT_IF_NO_TOKENS 1  /*for handling allocation failures*/
#define NULL_IF_NO_TOKENS 2  /*for handling allocation failures*/

/*
 * fsmarc_token management
 */

int count_fsmarc_token_list(srec* rec, stokenID token_index);
void initialize_free_fsmarc_tokens(srec *rec);
stokenID setup_free_fsmarc_token(srec *rec, FSMarc* arc, arcID fsm_arc_index, miscdata what_to_do_if_fails);
void free_fsmarc_token(srec *rec, stokenID old_token_index);
void sort_fsmarc_token_list(srec* rec, stokenID* ptoken_index);

/*
 * word_token management
 */

void initialize_free_word_tokens(srec *rec);
wtokenID get_free_word_token(srec *rec, miscdata what_to_do_if_fails);

/*
 * fsmnode_token management
 */

int count_fsmnode_token_list(srec* rec, ftokenID token_index);
void initialize_free_fsmnode_tokens(srec *rec);
ftokenID get_free_fsmnode_token(srec *rec, miscdata what_to_do_if_fails);
void free_fsmnode_token(srec *rec, ftokenID old_token_index);

/*
 *  altword token management
 */

void initialize_free_altword_tokens(srec *rec);
int count_altword_token(srec* rec, altword_token* b);
altword_token* get_free_altword_token(srec* rec, miscdata what_to_do_if_fails);
int free_altword_token(srec* rec, altword_token* old_token);
altword_token* free_altword_token_batch(srec* rec, altword_token* old_token);
#define print_altword_token_counts(a,b)

#endif
