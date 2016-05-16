/*---------------------------------------------------------------------------*
 *  setting.h  *
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

/*  Header file which describes the settings for the compilation
*/

#ifndef _setting_h_
#define _setting_h_

#ifdef SET_RCSID
static const char setting_h[] = "$Id: setting.h,v 1.5.8.4 2007/08/31 17:44:53 dahan Exp $";
#endif


#include "buildopt.h"

/************************************************************************
 ************************************************************************/

/*  For fixed point code, see settings in hmm_type.h
**  Also do create more abstract types for preferred data types
**  for variables in hmm_type.h
*/


#ifndef _RTT
#define REPORT_USAGE 1
#endif

#define PC_DEMO_NBEST

/************************************************************************
 *  The stuff below should be edited with care
 ************************************************************************/

#if defined(PC_DEMO)

#elif defined(PC_DEMO_NBEST)

/*  Computational and storage features
*/
#define USE_CPP_MEMORY  1

#elif defined(GRAND_UNIX)

#elif defined(PSOS)

#elif defined(VOYAGER)

#elif defined (POSIX)

#elif defined(DEVELOPER)

#else

#endif

/************************************************************************
 ************************************************************************/

/* Sanity checking of defines
*/

#endif
