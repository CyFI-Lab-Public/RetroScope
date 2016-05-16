/*---------------------------------------------------------------------------*
 *  ESR_SessionTypeImpl.h  *
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

#ifndef __ESR_SESSIONTYPEIMPL_H
#define __ESR_SESSIONTYPEIMPL_H



#include "ArrayList.h"
#include "ESR_ReturnCode.h"
#include "ESR_SessionType.h"
#include "ESR_SharedPrefix.h"
#include "HashMap.h"
#include "pstdio.h"


/**
 * ESR_SessionType implementation data.
 */
typedef struct ESR_SessionTypeData_t
{
  /**
   * [key, value] pairs.
   */
  HashMap* value;
  
  /**
   * Event listeners.
   */
  ArrayList* listeners;
}
ESR_SessionTypeData;

typedef struct ESR_SessionTypePair_t
{
  /**
   * Pointer to value.
   */
  void* value;
  /**
   * Value type.
   */
  VariableTypes type;
}
ESR_SessionPair;

/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionTypeGetPropertyImpl(ESR_SessionType* self,
    const LCHAR* name,
    void** value,
    VariableTypes type);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionTypeGetIntImpl(ESR_SessionType* self,
    const LCHAR* name,
    int* value);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionTypeGetUint16_tImpl(ESR_SessionType* self,
    const LCHAR* name,
    asr_uint16_t* value);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionTypeGetSize_tImpl(ESR_SessionType* self,
    const LCHAR* name,
    size_t* value);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionTypeGetFloatImpl(ESR_SessionType* self,
    const LCHAR* name,
    float* value);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionTypeGetBoolImpl(ESR_SessionType* self,
    const LCHAR* name,
    ESR_BOOL* value);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionTypeGetLCHARImpl(ESR_SessionType* self,
    const LCHAR* name,
    LCHAR* value, size_t* len);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionTypeContainsImpl(ESR_SessionType* self,
    const LCHAR* name,
    ESR_BOOL* exists);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionTypeSetPropertyImpl(ESR_SessionType* self,
    const LCHAR* name,
    void* value, VariableTypes type);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionTypeSetIntImpl(ESR_SessionType* self,
    const LCHAR* name,
    int value);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionTypeSetUint16_tImpl(ESR_SessionType* self,
    const LCHAR* name,
    asr_uint16_t value);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionTypeSetSize_tImpl(ESR_SessionType* self,
    const LCHAR* name,
    size_t value);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionTypeSetFloatImpl(ESR_SessionType* self,
    const LCHAR* name,
    float value);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionTypeSetBoolImpl(ESR_SessionType* self,
    const LCHAR* name,
    ESR_BOOL value);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionTypeSetLCHARImpl(ESR_SessionType* self,
    const LCHAR* name,
    LCHAR* value);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionTypeSetIntIfEmptyImpl(ESR_SessionType* self,
    const LCHAR* name,
    int value);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionTypeSetUint16_tIfEmptyImpl(ESR_SessionType* self,
    const LCHAR* name,
    asr_uint16_t value);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionTypeSetSize_tIfEmptyImpl(ESR_SessionType* self,
    const LCHAR* name,
    size_t value);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionTypeSetFloatIfEmptyImpl(ESR_SessionType* self,
    const LCHAR* name,
    float value);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionTypeSetBoolIfEmptyImpl(ESR_SessionType* self,
    const LCHAR* name,
    ESR_BOOL value);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionTypeSetLCHARIfEmptyImpl(ESR_SessionType* self,
    const LCHAR* name,
    LCHAR* value);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionTypeRemovePropertyImpl(ESR_SessionType* self,
    const LCHAR* name);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionTypeRemoveAndFreePropertyImpl(ESR_SessionType* self,
    const LCHAR* name);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionTypeImportCommandLineImpl(ESR_SessionType* self,
    int argc,
    LCHAR* argv[]);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionTypeImportSessionImpl(ESR_SessionType* self,
    ESR_SessionType* source);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionTypeGetSizeImpl(ESR_SessionType* self,
    size_t* size);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionTypeGetKeyAtIndexImpl(ESR_SessionType* self,
    size_t index,
    LCHAR** key);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionTypeDestroyImpl(ESR_SessionType* self);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionTypeConvertToIntImpl(ESR_SessionType* self,
    const LCHAR* key);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionTypeConvertToUint16_tImpl(ESR_SessionType* self,
    const LCHAR* key);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionTypeConvertToSize_tImpl(ESR_SessionType* self,
    const LCHAR* key);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionTypeConvertToFloatImpl(ESR_SessionType* self,
    const LCHAR* key);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionTypeConvertToBoolImpl(ESR_SessionType* self,
    const LCHAR* key);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionTypeGetPropertyTypeImpl(ESR_SessionType* self,
    const LCHAR* name,
    VariableTypes* type);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionTypeImportParFileImpl(ESR_SessionType* self,
    const LCHAR* filename);
    
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionTypeAddListenerImpl(ESR_SessionType* self,
    ESR_SessionTypeListenerPair* listener);
    
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionTypeRemoveListenerImpl(ESR_SessionType* self,
    ESR_SessionTypeListenerPair* listener);
    
#endif /* __ESR_SESSIONTYPEIMPL_H */
