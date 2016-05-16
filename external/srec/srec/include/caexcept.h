/*---------------------------------------------------------------------------*
 *  caexcept.h  *
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

#ifndef _h_caexcept_
#define _h_caexcept_

#ifdef SET_RCSID
static const char caexcept_h[] = "$Id: caexcept.h,v 1.1.10.2 2007/08/31 17:44:52 dahan Exp $";
#endif


#include "duk_err.h"


#ifdef USE_EXCEPTION_HANDLING

typedef crecXceptn CA_Exception;

#define TRY_CA_EXCEPT try{

#define BEG_CATCH_CA_EXCEPT }catch( CA_Exception* e ){

#define END_CATCH_CA_EXCEPT( Obj )      \
  rethrow_crec_xception(e, (void*) Obj );     \
  }

#else

#define TRY_CA_EXCEPT

#define BEG_CATCH_CA_EXCEPT if(0){

#define END_CATCH_CA_EXCEPT( Obj ) }

#endif


#endif /* _h_caexcept_ */
