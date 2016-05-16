/*---------------------------------------------------------------------------*
 *  ESR_SessionImpl.h  *
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

#ifndef __ESR_SESSIONIMPL_H
#define __ESR_SESSIONIMPL_H



#include "ESR_ReturnCode.h"
#include "ESR_Session.h"
#include "ESR_SharedPrefix.h"
#include "HashMap.h"
#include "pstdio.h"

/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionGetPropertyImpl(ESR_SessionType* self,
    const LCHAR* name,
    void** value,
    VariableTypes type);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionGetIntImpl(ESR_SessionType* self,
    const LCHAR* name,
    int* value);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionGetSize_tImpl(ESR_SessionType* self,
    const LCHAR* name,
    size_t* value);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionGetFloatImpl(ESR_SessionType* self,
    const LCHAR* name,
    float* value);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionGetBoolImpl(ESR_SessionType* self,
    const LCHAR* name,
    ESR_BOOL* value);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionGetLCHARImpl(ESR_SessionType* self,
    const LCHAR* name,
    LCHAR* value, size_t* len);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionContainsImpl(ESR_SessionType* self,
    const LCHAR* name,
    ESR_BOOL* exists);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionSetPropertyImpl(ESR_SessionType* self,
    const LCHAR* name,
    void* value,
    VariableTypes type);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionSetIntImpl(ESR_SessionType* self,
    const LCHAR* name,
    int value);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionSetSize_tImpl(ESR_SessionType* self,
    const LCHAR* name,
    size_t value);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionSetFloatImpl(ESR_SessionType* self,
    const LCHAR* name,
    float value);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionSetBoolImpl(ESR_SessionType* self,
    const LCHAR* name,
    ESR_BOOL value);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionSetLCHARImpl(ESR_SessionType* self,
    const LCHAR* name,
    LCHAR* value);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionSetIntIfEmptyImpl(ESR_SessionType* self,
    const LCHAR* name,
    int value);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionSetSize_tIfEmptyImpl(ESR_SessionType* self,
    const LCHAR* name,
    size_t value);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionSetFloatIfEmptyImpl(ESR_SessionType* self,
    const LCHAR* name,
    float value);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionSetBoolIfEmptyImpl(ESR_SessionType* self,
    const LCHAR* name,
    ESR_BOOL value);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionSetLCHARIfEmptyImpl(ESR_SessionType* self,
    const LCHAR* name,
    LCHAR* value);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionRemovePropertyImpl(ESR_SessionType* self,
    const LCHAR* name);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionImportCommandLineImpl(ESR_SessionType* self,
    int argc,
    LCHAR* argv[]);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionImportSessionImpl(ESR_SessionType* self,
    ESR_SessionType* source);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionGetSizeImpl(ESR_SessionType* self,
    size_t* size);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionGetKeyAtIndexImpl(ESR_SessionType* self,
    size_t index,
    LCHAR** key);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionDestroyImpl(ESR_SessionType* self);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionConvertToIntImpl(ESR_SessionType* self,
    const LCHAR* key);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionConvertToSize_tImpl(ESR_SessionType* self,
    const LCHAR* key);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionConvertToFloatImpl(ESR_SessionType* self,
    const LCHAR* key);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionConvertToBoolImpl(ESR_SessionType* self,
    const LCHAR* key);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionGetPropertyTypeImpl(ESR_SessionType* self,
    const LCHAR* name,
    VariableTypes* type);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionImportParFileImpl(ESR_SessionType* self,
    const LCHAR* filename);
    
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionExists(ESR_BOOL* val);


#endif /* __ESR_SESSIONIMPL_H */
