/*---------------------------------------------------------------------------*
 *  Int8ArrayListImpl.h  *
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

#ifndef __INT8ARRAYLISTIMPL_H
#define __INT8ARRAYLISTIMPL_H



#include "ESR_ReturnCode.h"
#include "ESR_SharedPrefix.h"

/**
 * Int8ArrayList implementation.
 */
typedef struct Int8ArrayListImpl_t
{
  /**
   * Interface functions that must be implemented.
   */
  Int8ArrayList Interface;
  /**
   * Int8ArrayList contents.
   */
  asr_int8_t* contents;
  /**
   * Virtual number of allocated element slots.
   */
  size_t virtualSize;
  /**
   * Actual number of allocated element slots.
   */
  size_t actualSize;
}
Int8ArrayListImpl;


/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode Int8ArrayList_Add(Int8ArrayList* self, const asr_int8_t element);

/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode Int8ArrayList_Remove(Int8ArrayList* self, const asr_int8_t element);

/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode Int8ArrayList_RemoveAll(Int8ArrayList* self);

/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode Int8ArrayList_Contains(Int8ArrayList* self, const asr_int8_t element, ESR_BOOL* exists);

/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode Int8ArrayList_Get(Int8ArrayList* self, size_t index, asr_int8_t* element);

/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode Int8ArrayList_Set(Int8ArrayList* self, size_t index, const asr_int8_t element);

/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode Int8ArrayList_GetSize(Int8ArrayList* self, size_t* size);

/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode Int8ArrayList_ToStaticArray(Int8ArrayList* self, asr_int8_t** newArray);

/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode Int8ArrayList_Clone(Int8ArrayList* self, Int8ArrayList* clone);

/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode Int8ArrayList_Destroy(Int8ArrayList* self);

#endif /* __INT8ARRAYLISTIMPL_H */
