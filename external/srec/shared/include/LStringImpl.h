/*---------------------------------------------------------------------------*
 *  LStringImpl.h  *
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

#ifndef __LSTRINGIMPL_H
#define __LSTRINGIMPL_H



#include "ESR_ReturnCode.h"
#include "ESR_SharedPrefix.h"
#include "ptypes.h"

/**
 * LString implementation.
 */
typedef struct LStringImpl_t
{
  /**
   * Interface functions that must be implemented.
   */
  LString Interface;
  /**
   * Underlying string value.
   */
  LCHAR* value;
  /**
   * Underlying string size.
   */
  size_t size;
}
LStringImpl;


/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode LString_Append(LString* self, const LCHAR* value);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode LString_Reset(LString* self);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode LString_ToLCHAR(LString* self, LCHAR** result);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode LString_Destroy(LString* self);


#endif /* __LSTRINGIMPL_H */
