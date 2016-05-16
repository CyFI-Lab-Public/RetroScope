/*---------------------------------------------------------------------------*
 *  sizes.h  *
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

#ifndef _SIZES_H_
#define _SIZES_H_

#include "ptypes.h"

#if 1

#define BYTES_PER_ATOM 1

#define FOUR_BYTE_PTR(PTR,NAME,DUMMY) union { PTR NAME; asr_int32_t DUMMY; }

/**
 * @todo document
 */
typedef struct
{
  FOUR_BYTE_PTR(char*, ptr, dummy) tmp;
}
ptr32;

#else
#error Unknown build processor!

#endif
#endif
