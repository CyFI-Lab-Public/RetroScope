/*---------------------------------------------------------------------------*
 *  creccons.h  *
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

#ifndef __creccons_h
#define __creccons_h

/************************************************************************
 * Constants
 ************************************************************************/

/************************************************************************
 * CREC Wave Constants
 ************************************************************************/

#define DEVICE_RAW_PCM      1
#define DEVICE_MULAW        2
#define WAVE_DEVICE_RAW     1

/************************************************************************
 * CREC Utterance Constants
 ************************************************************************/

#define LIVE_INPUT                  2

/************************************************************************
 * CREC Recognizer Constants
 ************************************************************************/

#define FULL_RESULT      3
#define REJECT_RESULT      4
#define INVALID_REQUEST             5

#define FB_DEAD                     0
#define FB_IDLE                     1
#define FB_ACTIVE                   2

/************************************************************************
 * CREC end of utterance constants
 ************************************************************************/

#define RESULTS_UTTERANCE_END 1
#define SYNTAX_UTTERANCE_END  2

#endif
