/*---------------------------------------------------------------------------*
 *  frontpar.c  *
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

#include <stdlib.h>

#if defined(__cplusplus) && defined(_MSC_VER)
extern "C"
{
#include <string.h>
}
#else
#include <string.h>
#endif

#ifndef _RTT
#include <stdio.h>
#endif

#ifdef unix
#include <unistd.h>
#endif
#include <assert.h>

#include "duk_args.h"
#include "frontapi.h"
#include "portable.h"


#ifdef SET_RCSID
static const char *rcsid = 0 ? (const char *) &rcsid :
                           "$Id: frontpar.c,v 1.4.10.6 2007/10/15 18:06:24 dahan Exp $";
#endif
                           
int load_up_parameter_list(arg_info *arglist, CA_FrontendInputParams *frontArgs);


CA_FrontendInputParams *CA_AllocateFrontendParameters(void)
{
  CA_FrontendInputParams *frontArgs = NULL;
  TRY_CA_EXCEPT
  frontArgs = (CA_FrontendInputParams *) CALLOC_CLR(1,
              sizeof(CA_FrontendInputParams), "cfront.hFrontArgs");
              
  frontArgs->is_loaded = False;
  frontArgs->ca_rtti = CA_FRONTEND_PARAMETERS_SIGNATURE;
  return (frontArgs);
  BEG_CATCH_CA_EXCEPT;
  END_CATCH_CA_EXCEPT(frontArgs);
}

void  CA_FreeFrontendParameters(CA_FrontendInputParams *frontArgs)
{
  TRY_CA_EXCEPT
  ASSERT(frontArgs);
  
  FREE((char *)frontArgs);
  
  BEG_CATCH_CA_EXCEPT;
  END_CATCH_CA_EXCEPT(frontArgs);
}
