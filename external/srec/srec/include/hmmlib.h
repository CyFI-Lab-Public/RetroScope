/*---------------------------------------------------------------------------*
 *  hmmlib.h  *
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


#ifndef _h_hmmlib_
#define _h_hmmlib_

#ifdef SET_RCSID
static const char hmmlib_h[] = "$Id: hmmlib.h,v 1.4.6.8 2008/01/21 20:30:05 dahan Exp $";
#endif


#include "hmm_desc.h"
#include "ESR_Locale.h"

int read_word_transcription(const LCHAR* basename, vocab_info* voc, ESR_Locale* locale);
void delete_word_transcription(vocab_info* voc);
int get_prons(const vocab_info* voc, const char* label, char* prons, int prons_len);

int mmap_zip(const char* fname, void** buf, size_t* size);
int munmap_zip(void* buf, size_t size);

#endif /* _h_hmmlib_ */
