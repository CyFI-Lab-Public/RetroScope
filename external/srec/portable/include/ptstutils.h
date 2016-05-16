/*---------------------------------------------------------------------------*
 *  ptstutils.h  *
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

#ifndef PTSTUTILS_H
#define PTSTUTILS_H



#include "pstdio.h"

#ifdef ASSERT
#undef ASSERT
#endif

/**
 * Macros defined to facilitate the writing of test programs.
 * That's why they are not dependent on any compile-time flag.
 */
#define ASSERT(x) \
  do { \
    if (!(x)) \
    { \
      pfprintf(PSTDERR, L(__FILE__ "(%d): " #x " failed: aborting.\n"), __LINE__); \
      exit(-1); \
    } \
  } \
  while(0)

#define ESR_ASSERT(x) \
  do { \
    if ((x) != ESR_SUCCESS) \
    { \
      pfprintf(PSTDERR, L(__FILE__ "(%d): " #x " failed: aborting.\n"), __LINE__); \
      exit(-1); \
    } \
  } \
  while(0)

#define ASSERT2(x, count) \
  do { \
    if (!(x)) \
    { \
      pfprintf(PSTDERR, L(__FILE__ "(%d): " #x " failed.\n"), __LINE__); \
      ++count; \
    } \
  } \
  while(0)

#endif
