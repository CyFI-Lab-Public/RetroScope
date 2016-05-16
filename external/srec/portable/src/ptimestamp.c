/*---------------------------------------------------------------------------*
 *  ptimestamp.c  *
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

#include "ptimestamp.h"
#include "pmutex.h"

#ifdef _WIN32
#include "sys/timeb.h"
#endif

void PTimeStampSet(PTimeStamp *timestamp)
{
	
#ifdef DISABLE_TIMESTAMPS
  timestamp->secs  = 0;
  timestamp->msecs = 0;
#else
	
#ifdef _WIN32
  struct _timeb now;
  
  _ftime(&now);
  timestamp->secs = now.time;
  timestamp->msecs = now.millitm;
#elif defined(POSIX)
  struct timespec now;
  clock_gettime(CLOCK_REALTIME, &now);
  timestamp->secs = now.tv_sec;
  timestamp->msecs = now.tv_nsec / MSECOND2NSECOND;
#else
#error "PTimeStampSet not defined for this platform."
#endif

#endif
}

PINLINE int PTimeStampDiff(const PTimeStamp *a, const PTimeStamp *b)
{
  return (a->secs - b->secs) * 1000 + a->msecs - b->msecs;
}
