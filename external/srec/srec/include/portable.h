/*---------------------------------------------------------------------------*
 *  portable.h  *
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


#ifndef _h_portable_
#define _h_portable_

#ifdef SET_RCSID
static const char portable_h[] = "$Id: portable.h,v 1.4.10.4 2007/10/15 18:06:25 dahan Exp $";
#endif


#include <math.h>

#include "passert.h"
#include "pmemory.h"
#include "PFileSystem.h"
#include "plog.h"
#include "pstdio.h"
#include "ptypes.h"
#include "setting.h"

#define ASSERT passert

/* QNX defines log in math.h */
#ifndef log
#define log(X)          ((X) <= 1e-32 ? -MAX_LOG : log(X))
/* #define exp(X)          ((X) < FLT_MIN_EXP ? 0.0 : exp(X)) */
#endif
/*  Allocation macros
*/

#if defined(__sgi)
#define inline
#elif defined(unix)
#define inline __inline__
#endif

  static PINLINE PFile* file_must_open(PFile* afile, const LCHAR *name, const LCHAR *mode, ESR_BOOL littleEndian)
  {
    PFile* fp;
    ESR_ReturnCode rc;
    
    fp = pfopen ( name, mode );

    if (fp == NULL)
    {
      LCHAR path[P_PATH_MAX];
      LCHAR *tmp = path;
      size_t len;
      
      len = P_PATH_MAX;
      CHKLOG(rc, pf_get_cwd (path, &len));
      PLogError(L("Could not open file %s, mode=%s, cwd=%s\n"), name, mode, tmp);
      /* passert(0); */
      fp = NULL;
    }
    return fp;
CLEANUP:
    return NULL;
  }

#define report_malloc_usage() do { } while(0); /* do nothing */
#define log_report PLogMessage

#define SERVICE_ERROR(X) do \
  { \
    log_report("service error (%d)\n", X); \
    assert(0); \
    exit(1); \
  } while(0)


#endif
