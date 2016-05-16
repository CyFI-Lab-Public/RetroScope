/*---------------------------------------------------------------------------*
 *  SessionTypeImpl.c  *
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


#include "ESR_SessionType.h"
#include "ESR_SessionTypeImpl.h"
#include "HashMap.h"
#include "IntArrayList.h"
#include "LCHAR.h"
#include "lstring.h"
#include "passert.h"
#include "pendian.h"
#include "PFile.h"
#include "PFileSystem.h"
#include "plog.h"
#include "pmemory.h"
#include "pstdio.h"
#include "string.h"
#include "ESR_SessionTypeListener.h"

#define MTAG NULL

ESR_ReturnCode ESR_SessionTypeCreate(ESR_SessionType** self)
{
  ESR_SessionType* Interface;
  ESR_SessionTypeData* data;
  ESR_ReturnCode rc;

  if (self == NULL)
  {
    PLogError(L("ESR_OUT_OF_MEMORY"));
    return ESR_OUT_OF_MEMORY;
  }
  Interface = NEW(ESR_SessionType, MTAG);
  if (Interface == NULL)
  {
    PLogError(L("ESR_OUT_OF_MEMORY"));
    return ESR_OUT_OF_MEMORY;
  }
  data = NEW(ESR_SessionTypeData, MTAG);
  if (data == NULL)
  {
    rc = ESR_OUT_OF_MEMORY;
    goto CLEANUP;
  }


  Interface->addListener = &ESR_SessionTypeAddListenerImpl;
  Interface->contains = &ESR_SessionTypeContainsImpl;
  Interface->convertToBool = &ESR_SessionTypeConvertToBoolImpl;
  Interface->convertToFloat = &ESR_SessionTypeConvertToFloatImpl;
  Interface->convertToInt = &ESR_SessionTypeConvertToIntImpl;
  Interface->convertToUint16_t = &ESR_SessionTypeConvertToUint16_tImpl;
  Interface->convertToSize_t = &ESR_SessionTypeConvertToSize_tImpl;
  Interface->destroy = &ESR_SessionTypeDestroyImpl;
  Interface->getBool = &ESR_SessionTypeGetBoolImpl;
  Interface->getFloat = &ESR_SessionTypeGetFloatImpl;
  Interface->getInt = &ESR_SessionTypeGetIntImpl;
  Interface->getUint16_t = &ESR_SessionTypeGetUint16_tImpl;
  Interface->getKeyAtIndex = &ESR_SessionTypeGetKeyAtIndexImpl;
  Interface->getLCHAR = &ESR_SessionTypeGetLCHARImpl;
  Interface->getProperty = &ESR_SessionTypeGetPropertyImpl;
  Interface->getPropertyType = &ESR_SessionTypeGetPropertyTypeImpl;
  Interface->getSize = &ESR_SessionTypeGetSizeImpl;
  Interface->getSize_t = &ESR_SessionTypeGetSize_tImpl;
  Interface->importCommandLine = &ESR_SessionTypeImportCommandLineImpl;
  Interface->importParFile = &ESR_SessionTypeImportParFileImpl;
  Interface->removeProperty = &ESR_SessionTypeRemovePropertyImpl;
  Interface->removeAndFreeProperty = &ESR_SessionTypeRemoveAndFreePropertyImpl;
  Interface->setBool = &ESR_SessionTypeSetBoolImpl;
  Interface->setBoolIfEmpty = &ESR_SessionTypeSetBoolIfEmptyImpl;
  Interface->setFloat = &ESR_SessionTypeSetFloatImpl;
  Interface->setFloatIfEmpty = &ESR_SessionTypeSetFloatIfEmptyImpl;
  Interface->setInt = &ESR_SessionTypeSetIntImpl;
  Interface->setIntIfEmpty = &ESR_SessionTypeSetIntIfEmptyImpl;
  Interface->setUint16_t = &ESR_SessionTypeSetUint16_tImpl;
  Interface->setUint16_tIfEmpty = &ESR_SessionTypeSetUint16_tIfEmptyImpl;
  Interface->setLCHAR = &ESR_SessionTypeSetLCHARImpl;
  Interface->setLCHARIfEmpty = &ESR_SessionTypeSetLCHARIfEmptyImpl;
  Interface->setProperty = &ESR_SessionTypeSetPropertyImpl;
  Interface->setSize_t = &ESR_SessionTypeSetSize_tImpl;
  Interface->setSize_tIfEmpty = &ESR_SessionTypeSetSize_tIfEmptyImpl;
  Interface->removeListener = &ESR_SessionTypeRemoveListenerImpl;

  Interface->data = data;
  data->value = NULL;
  data->listeners = NULL;

  CHK(rc, HashMapCreate(&data->value));
  CHK(rc, ArrayListCreate(&data->listeners));
  *self = Interface;
  return ESR_SUCCESS;
CLEANUP:
  Interface->destroy(Interface);
  return rc;
}

/*
 * Because there are no functions to set and get int size_t parameters and because most if not
 * all int parameters should be size_t anyway, I am adding code to allow size_t and int parameters
 * to be considered equal. Besides, this check is kind of overkill anyway. SteveR
 */

ESR_ReturnCode ESR_SessionTypeGetPropertyImpl(ESR_SessionType* self,
    const LCHAR* name, void** value,
    VariableTypes type)
{
  ESR_SessionTypeData* data = self->data;
  ESR_SessionPair* pair;
  ESR_ReturnCode rc;

  CHK(rc, HashMapGet(data->value, name, (void **)&pair));
  if ( ( pair->type != type) && ( ( ( pair->type != TYPES_INT ) && ( type != TYPES_SIZE_T ) ) ||
      ( ( type != TYPES_INT ) && ( pair->type != TYPES_SIZE_T ) ) ) )
  {
    PLogError(L("ESR_INVALID_RESULT_TYPE: [got=%d, expected=%d]"), type, pair->type);
    return ESR_INVALID_RESULT_TYPE;
  }
  *value = pair->value;
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode ESR_SessionTypeGetIntImpl(ESR_SessionType* self,
    const LCHAR* name, int* value)
{
  ESR_ReturnCode rc;
  ESR_SessionTypeData* data;
  ESR_SessionPair* pair;

  data = self->data;
  CHK(rc, HashMapGet(data->value, name, (void **)&pair));
  if ( ( pair->type != TYPES_INT ) && ( pair->type != TYPES_SIZE_T ) )
  {
    PLogError(L("ESR_INVALID_RESULT_TYPE: [got=%d, expected=%d]"), TYPES_INT, pair->type);
    return ESR_INVALID_RESULT_TYPE;
  }
  *value = *((int*) pair->value);
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode ESR_SessionTypeGetUint16_tImpl(ESR_SessionType* self,
    const LCHAR* name, asr_uint16_t* value)
{
  ESR_ReturnCode rc;
  ESR_SessionTypeData* data;
  ESR_SessionPair* pair;

  data = self->data;
  CHK(rc, HashMapGet(data->value, name, (void **)&pair));
  if (pair->type != TYPES_UINT16_T)
  {
    PLogError(L("ESR_INVALID_RESULT_TYPE: [got=%d, expected=%d]"), TYPES_UINT16_T, pair->type);
    return ESR_INVALID_RESULT_TYPE;
  }
  *value = *((asr_uint16_t*) pair->value);
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode ESR_SessionTypeGetSize_tImpl(ESR_SessionType* self,
    const LCHAR* name,
    size_t* value)
{
  ESR_ReturnCode rc;
  ESR_SessionTypeData* data;
  ESR_SessionPair* pair;

  data = self->data;
  CHKLOG(rc, HashMapGet(data->value, name, (void **)&pair));
  if ( ( pair->type != TYPES_INT ) && ( pair->type != TYPES_SIZE_T ) )
  {
    PLogError(L("ESR_INVALID_RESULT_TYPE: [got=%d, expected=%d]"), TYPES_SIZE_T, pair->type);
    return ESR_INVALID_RESULT_TYPE;
  }
  *value = *((size_t*) pair->value);
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode ESR_SessionTypeGetFloatImpl(ESR_SessionType* self,
    const LCHAR* name, float* value)
{
  ESR_ReturnCode rc;
  ESR_SessionTypeData* data;
  ESR_SessionPair* pair;

  data = self->data;
  CHK(rc, HashMapGet(data->value, name, (void **)&pair));
  if (pair->type != TYPES_FLOAT)
  {
    PLogError(L("ESR_INVALID_RESULT_TYPE: [got=%d, expected=%d]"), TYPES_FLOAT, pair->type);
    return ESR_INVALID_RESULT_TYPE;
  }
  *value = *((float*) pair->value);
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode ESR_SessionTypeGetBoolImpl(ESR_SessionType* self,
    const LCHAR* name, ESR_BOOL* value)
{
  ESR_ReturnCode rc;
  ESR_SessionTypeData* data;
  ESR_SessionPair* pair;

  data = self->data;
  CHK(rc, HashMapGet(data->value, name, (void **)&pair));
  if (pair->type != TYPES_BOOL)
  {
    PLogError(L("ESR_INVALID_RESULT_TYPE: [got=%d, expected=%d]"), TYPES_BOOL, pair->type);
    return ESR_INVALID_RESULT_TYPE;
  }
  *value = *((ESR_BOOL*) pair->value);
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode ESR_SessionTypeGetLCHARImpl(ESR_SessionType* self,
    const LCHAR* name,
    LCHAR* value, size_t* len)
{
  LCHAR* lValue;
  ESR_ReturnCode rc;
  ESR_SessionTypeData* data;
  ESR_SessionPair* pair;

  if (name == NULL || value == NULL || len == NULL)
    return ESR_INVALID_ARGUMENT;
  data = self->data;

  CHK(rc, HashMapGet(data->value, name, (void **)&pair));
  if (pair->type != TYPES_PLCHAR)
  {
    PLogError(L("ESR_INVALID_RESULT_TYPE: [got=%d, expected=%d]"), TYPES_PLCHAR, pair->type);
    return ESR_INVALID_RESULT_TYPE;
  }
  lValue = (LCHAR*) pair->value;
  if (LSTRLEN(pair->value) + 1 > *len)
  {
    *len = LSTRLEN(lValue) + 1;
    return ESR_BUFFER_OVERFLOW;
  }
  LSTRCPY(value, lValue);
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode ESR_SessionTypeContainsImpl(ESR_SessionType* self,
    const LCHAR* name, ESR_BOOL* exists)
{
  ESR_SessionTypeData* data = self->data;

  return HashMapContainsKey(data->value, name, exists);
}

static ESR_ReturnCode firePropertyChanged(ESR_SessionType* self, const LCHAR* name,
    const void* oldValue, const void* newValue,
    enum VariableTypes_t type)
{
  ESR_SessionTypeData* data = self->data;
  ArrayList* list = data->listeners;
  size_t size, i;
  ESR_SessionTypeListenerPair* listener;
  ESR_ReturnCode rc;

  CHKLOG(rc, list->getSize(list, &size));
  for (i = 0; i < size; ++i)
  {
    CHKLOG(rc, list->get(list, i, (void **)&listener));
    CHKLOG(rc, listener->listener->propertyChanged(listener->listener, name, oldValue, newValue, type, listener->data));
  }
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode ESR_SessionTypeSetPropertyImpl(ESR_SessionType* self,
    const LCHAR* name, void* value,
    VariableTypes type)
{
  ESR_SessionTypeData* data = self->data;
  ESR_SessionPair* pair = NULL;
  ESR_BOOL exists;
  ESR_ReturnCode rc;

  CHKLOG(rc, HashMapContainsKey(data->value, name, &exists));
  if ( exists )
  {
/* We allow change of parameters through the recognizer and we rely on the recognizer to do
 * all of the needed validation. SteveR
 */
/* Deleting the old entry seems stupid, but it's the only way to prevent a memory leak,
 * since the old data is not returned when you add the new data. SteveR
 */
    CHKLOG ( rc, ESR_SessionTypeRemoveAndFreePropertyImpl ( self, name ) );
  }
  pair = NEW(ESR_SessionPair, MTAG);
  if (pair == NULL)
  {
    PLogError(L("ESR_OUT_OF_MEMORY"));
    return ESR_OUT_OF_MEMORY;
  }
  pair->value = value;
  pair->type = type;

  CHKLOG(rc, firePropertyChanged(self, name, NULL, value, type));
  CHKLOG(rc, HashMapPut(data->value, name, pair));
  return ESR_SUCCESS;
CLEANUP:
/* The cleanup potentially leaks memory which could be cleard up with  FREE ( pair->value );
 * but you can't guarantee that the value was allocated. A leak is better than a crash. SteveR
 */
  FREE(pair);
  return rc;
}

ESR_ReturnCode ESR_SessionTypeSetIntImpl(ESR_SessionType* self,
    const LCHAR* name, int value)
{
  ESR_SessionTypeData* data;
  int* clone;

  data = self->data;
  clone = MALLOC(sizeof(int), MTAG);
  if (clone == NULL)
  {
    PLogError(L("ESR_OUT_OF_MEMORY"));
    return ESR_OUT_OF_MEMORY;
  }

  *clone = value;
  return self->setProperty(self, name, clone, TYPES_INT);
}

ESR_ReturnCode ESR_SessionTypeSetUint16_tImpl(ESR_SessionType* self,
    const LCHAR* name, asr_uint16_t value)
{
  ESR_SessionTypeData* data;
  asr_uint16_t* clone;

  data = self->data;
  clone = MALLOC(sizeof(asr_uint16_t), MTAG);
  if (clone == NULL)
  {
    PLogError(L("ESR_OUT_OF_MEMORY"));
    return ESR_OUT_OF_MEMORY;
  }

  *clone = value;
  return self->setProperty(self, name, clone, TYPES_UINT16_T);
}

ESR_ReturnCode ESR_SessionTypeSetSize_tImpl(ESR_SessionType* self,
    const LCHAR* name, size_t value)
{
  ESR_SessionTypeData* data;
  int* clone;

  data = self->data;
  clone = MALLOC(sizeof(size_t), MTAG);
  if (clone == NULL)
  {
    PLogError(L("ESR_OUT_OF_MEMORY"));
    return ESR_OUT_OF_MEMORY;
  }

  *clone = value;
  return self->setProperty(self, name, clone, TYPES_SIZE_T);
}

ESR_ReturnCode ESR_SessionTypeSetFloatImpl(ESR_SessionType* self,
    const LCHAR* name, float value)
{
  ESR_SessionTypeData* data;
  float* clone;

  data = self->data;
  clone = MALLOC(sizeof(float), MTAG);
  if (clone == NULL)
  {
    PLogError(L("ESR_OUT_OF_MEMORY"));
    return ESR_OUT_OF_MEMORY;
  }

  *clone = value;
  return self->setProperty(self, name, clone, TYPES_FLOAT);
}

ESR_ReturnCode ESR_SessionTypeSetBoolImpl(ESR_SessionType* self,
    const LCHAR* name, ESR_BOOL value)
{
  ESR_SessionTypeData* data;
  ESR_BOOL* clone;

  data = self->data;
  clone = MALLOC(sizeof(ESR_BOOL), MTAG);
  if (clone == NULL)
  {
    PLogError(L("ESR_OUT_OF_MEMORY"));
    return ESR_OUT_OF_MEMORY;
  }

  *clone = value;
  return self->setProperty(self, name, clone, TYPES_BOOL);
}

ESR_ReturnCode ESR_SessionTypeSetLCHARImpl(ESR_SessionType* self,
    const LCHAR* name, LCHAR* value)
{
  ESR_SessionTypeData* data;
  LCHAR* clone;

  data = self->data;
  clone = MALLOC(sizeof(LCHAR) * (LSTRLEN(value) + 1), MTAG);
  if (clone == NULL)
  {
    PLogError(L("ESR_OUT_OF_MEMORY"));
    return ESR_OUT_OF_MEMORY;
  }

  LSTRCPY(clone, value);
  return self->setProperty(self, name, clone, TYPES_PLCHAR);
}

ESR_ReturnCode ESR_SessionTypeSetIntIfEmptyImpl(ESR_SessionType* self,
    const LCHAR* name, int value)
{
  ESR_BOOL exists;
  ESR_ReturnCode rc;

  CHK(rc, self->contains(self, name, &exists));
  if (exists)
    return ESR_SUCCESS;
  return self->setInt(self, name, value);
CLEANUP:
  return rc;
}

ESR_ReturnCode ESR_SessionTypeSetUint16_tIfEmptyImpl(ESR_SessionType* self,
    const LCHAR* name, asr_uint16_t value)
{
  ESR_BOOL exists;
  ESR_ReturnCode rc;

  CHK(rc, self->contains(self, name, &exists));
  if (exists)
    return ESR_SUCCESS;
  return self->setInt(self, name, value);
CLEANUP:
  return rc;
}

ESR_ReturnCode ESR_SessionTypeSetSize_tIfEmptyImpl(ESR_SessionType* self,
    const LCHAR* name, size_t value)
{
  ESR_BOOL exists;
  ESR_ReturnCode rc;

  CHK(rc, self->contains(self, name, &exists));
  if (exists)
    return ESR_SUCCESS;
  return self->setSize_t(self, name, value);
CLEANUP:
  return rc;
}

ESR_ReturnCode ESR_SessionTypeSetFloatIfEmptyImpl(ESR_SessionType* self,
    const LCHAR* name, float value)
{
  ESR_BOOL exists;
  ESR_ReturnCode rc;

  CHK(rc, self->contains(self, name, &exists));
  if (exists)
    return ESR_SUCCESS;
  return self->setFloat(self, name, value);
CLEANUP:
  return rc;
}

ESR_ReturnCode ESR_SessionTypeSetBoolIfEmptyImpl(ESR_SessionType* self,
    const LCHAR* name, ESR_BOOL value)
{
  ESR_BOOL exists;
  ESR_ReturnCode rc;

  CHK(rc, self->contains(self, name, &exists));
  if (exists)
    return ESR_SUCCESS;
  return self->setBool(self, name, value);
CLEANUP:
  return rc;
}

ESR_ReturnCode ESR_SessionTypeSetLCHARIfEmptyImpl(ESR_SessionType* self,
    const LCHAR* name, LCHAR* value)
{
  ESR_BOOL exists;
  ESR_ReturnCode rc;

  CHK(rc, self->contains(self, name, &exists));
  if (exists)
    return ESR_SUCCESS;
  return self->setLCHAR(self, name, value);
CLEANUP:
  return rc;
}

ESR_ReturnCode ESR_SessionTypeRemovePropertyImpl(ESR_SessionType* self,
    const LCHAR* name)
{
  ESR_ReturnCode rc;
  ESR_SessionTypeData* data = self->data;
  ESR_SessionPair* pair;

  CHK(rc, HashMapGet(data->value, name, (void **)&pair));
  CHKLOG(rc, firePropertyChanged(self, name, pair->value, NULL, pair->type));
  CHK(rc, HashMapRemove(data->value, name));
  FREE(pair);
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode ESR_SessionTypeRemoveAndFreePropertyImpl(ESR_SessionType* self,
    const LCHAR* name)
{
  ESR_ReturnCode rc;
  ESR_SessionTypeData* data = self->data;
  ESR_SessionPair* pair;
  ESR_SessionPair temp;
  IntArrayList* intList;

  CHK(rc, data->value->get(data->value, name, (void **)&pair));
  temp = *pair;
  CHK(rc, self->removeProperty(self, name));
  if (temp.value)
  {
    if (temp.type == TYPES_INTARRAYLIST)
    {
      intList = temp.value;
      intList->destroy(intList);
    }
    else
      FREE(temp.value);
  }
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode ESR_SessionTypeImportCommandLineImpl(ESR_SessionType* self,
    int argc, LCHAR* argv[])
{
  char* key = NULL;
  char* value = NULL;
  VariableTypes type;
  ESR_ReturnCode rc;

  while (--argc > 0 && **++argv)
  {
    if (**argv != '-')
    {
      /* got value */
      if (key == NULL)
      {
        /* but we don't have any key to associate it with */
        pfprintf(PSTDERR, "Options must be prefixed by '-'%s\n", *argv);
      }
      else
      {
        rc = self->getPropertyType(self, key, &type);
        if (rc == ESR_SUCCESS)
        {
          CHKLOG(rc, self->getProperty(self, key, (void **)&value, type));
          CHKLOG(rc, self->removeProperty(self, key));
          FREE(value);
          value = NULL;
        }
        else if (rc != ESR_NO_MATCH_ERROR)
        {
          PLogError(ESR_rc2str(rc));
          goto CLEANUP;
        }
        value = MALLOC(sizeof(LCHAR) * (strlen(*argv) + 1), MTAG);
        if (value == NULL)
        {
          rc = ESR_OUT_OF_MEMORY;
          PLogError(L("ESR_OUT_OF_MEMORY"));
          goto CLEANUP;
        }
        LSTRCPY(value, *argv);
        CHKLOG(rc, self->setProperty(self, key, value, TYPES_PLCHAR));
        FREE(key);
        key = NULL;
        value = NULL;
      }
    }
    else
    {
      /* got key */
      if (key != NULL)
      {
        /* But we already have a key without a value, so set the old key's value to "" */
        rc = self->getPropertyType(self, key, &type);
        if (rc == ESR_SUCCESS)
        {
          CHKLOG(rc, self->getProperty(self, key, (void **)&value, type));
          CHKLOG(rc, self->removeProperty(self, key));
          FREE(value);
          value = NULL;
        }
        else if (rc != ESR_NO_MATCH_ERROR)
        {
          PLogError(ESR_rc2str(rc));
          goto CLEANUP;
        }
        value = MALLOC(sizeof(LCHAR) + 1, MTAG);
        strcpy(value, "");
        CHKLOG(rc, self->setProperty(self, key, value, TYPES_PLCHAR));
        FREE(key);
        value = NULL;
      }
      key = MALLOC(sizeof(LCHAR) * (LSTRLEN("cmdline.") + LSTRLEN(*argv) + 1), MTAG);
      if (key == NULL)
      {
        rc = ESR_OUT_OF_MEMORY;
        PLogError(L("ESR_OUT_OF_MEMORY"));
        goto CLEANUP;
      }
      LSTRCPY(key, "cmdline.");
      LSTRCAT(key, *argv + 1);
    }
  }
  return ESR_SUCCESS;
CLEANUP:
  if (key != NULL)
    FREE(key);
  if (value != NULL)
    FREE(value);
  return rc;
}

ESR_ReturnCode ESR_SessionTypeGetSizeImpl(ESR_SessionType* self, size_t* size)
{
  ESR_SessionTypeData* data = self->data;
  ESR_ReturnCode rc;

  CHK(rc, HashMapGetSize(data->value, size));
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode ESR_SessionTypeGetKeyAtIndexImpl(ESR_SessionType* self,
    size_t index, LCHAR** key)
{
  ESR_SessionTypeData* data = self->data;
  ESR_ReturnCode rc;

  CHK(rc, HashMapGetKeyAtIndex(data->value, index, key));
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode ESR_SessionTypeConvertToIntImpl(ESR_SessionType* self,
    const LCHAR* key)
{
  LCHAR* value;
  int *newValue = NULL;
  ESR_ReturnCode rc;

  CHK(rc, self->getProperty(self, key, (void **)&value, TYPES_PLCHAR));
  if (value == NULL)
    return ESR_SUCCESS;
  newValue = MALLOC(sizeof(long), MTAG);
  if (newValue == NULL)
  {
    rc = ESR_OUT_OF_MEMORY;
    goto CLEANUP;
  }
  CHKLOG(rc, lstrtoi(value, newValue, 10));
  CHKLOG(rc, self->setProperty(self, key, newValue, TYPES_INT));
  FREE(value);
  return ESR_SUCCESS;
CLEANUP:
  if (newValue != NULL)
    FREE(newValue);
  return rc;
}

ESR_ReturnCode ESR_SessionTypeConvertToUint16_tImpl(ESR_SessionType* self,
    const LCHAR* key)
{
  LCHAR* value;
  int *newValue = NULL;
  ESR_ReturnCode rc;

  CHK(rc, self->getProperty(self, key, (void **)&value, TYPES_PLCHAR));
  if (value == NULL)
    return ESR_SUCCESS;
  newValue = MALLOC(sizeof(long), MTAG);
  if (newValue == NULL)
  {
    rc = ESR_OUT_OF_MEMORY;
    goto CLEANUP;
  }
  CHKLOG(rc, lstrtoi(value, newValue, 10));
  CHKLOG(rc, self->setProperty(self, key, newValue, TYPES_UINT16_T));
  FREE(value);
  return ESR_SUCCESS;
CLEANUP:
  if (newValue != NULL)
    FREE(newValue);
  return rc;
}

ESR_ReturnCode ESR_SessionTypeConvertToSize_tImpl(ESR_SessionType* self,
    const LCHAR* key)
{
  LCHAR* value;
  size_t* newValue = NULL;
  ESR_ReturnCode rc;

  CHK(rc, self->getProperty(self, key, (void **)&value, TYPES_PLCHAR));
  if (value == NULL)
    return ESR_SUCCESS;
  newValue = MALLOC(sizeof(size_t), MTAG);
  if (newValue == NULL)
  {
    rc = ESR_OUT_OF_MEMORY;
    goto CLEANUP;
  }
  CHKLOG(rc, lstrtoui(value, (unsigned int *)newValue, 10));
  CHKLOG(rc, self->setProperty(self, key, newValue, TYPES_SIZE_T));
  FREE(value);
  return ESR_SUCCESS;
CLEANUP:
  if (newValue != NULL)
    FREE(newValue);
  return rc;
}

ESR_ReturnCode ESR_SessionTypeConvertToFloatImpl(ESR_SessionType* self,
    const LCHAR* key)
{
  LCHAR* value;
  float *newValue = NULL;
  ESR_ReturnCode rc;

  CHK(rc, self->getProperty(self, key, (void **)&value, TYPES_PLCHAR));
  if (value == NULL)
    return ESR_SUCCESS;
  newValue = MALLOC(sizeof(double), MTAG);
  if (newValue == NULL)
  {
    rc = ESR_OUT_OF_MEMORY;
    goto CLEANUP;
  }
  CHKLOG(rc, lstrtof(value, newValue));
  CHKLOG(rc, self->setProperty(self, key, newValue, TYPES_FLOAT));
  FREE(value);
  return ESR_SUCCESS;
CLEANUP:
  if (newValue != NULL)
    FREE(newValue);
  return rc;
}

ESR_ReturnCode ESR_SessionTypeConvertToBoolImpl(ESR_SessionType* self,
    const LCHAR* key)
{
  LCHAR* value;
  ESR_BOOL *newValue = NULL;
  ESR_ReturnCode rc;

  CHK(rc, self->getProperty(self, key, (void **)&value, TYPES_PLCHAR));
  if (value == NULL)
    return ESR_SUCCESS;
  newValue = MALLOC(sizeof(ESR_BOOL), MTAG);
  if (newValue == NULL)
  {
    rc = ESR_OUT_OF_MEMORY;
    goto CLEANUP;
  }
  rc = lstrtob(value, newValue);
  if (rc != ESR_SUCCESS)
  {
    FREE(newValue);
    return rc;
  }
  rc = self->setProperty(self, key, newValue, TYPES_BOOL);
  if (rc != ESR_SUCCESS)
  {
    FREE(newValue);
    return rc;
  }
  FREE(value);
  return ESR_SUCCESS;
CLEANUP:
  if (newValue != NULL)
    FREE(newValue);
  return rc;
}

/**
 * Imports file containing [key, value] pairs into session.
 *
 * @param self ESR_SessionType handle
 * @param filename File to read session from
 * @param addMapping Function used to map keys to their type and add them to the session
 * @param data Data used by the mapping function
 */
static ESR_ReturnCode importKeyValueFile(ESR_SessionType* self,
    const LCHAR* filename,
    ESR_ReturnCode(*addMapping)(ESR_SessionType* self, const LCHAR* key, LCHAR* value, void* data),
    void* data)
{
  const size_t LINE_SIZE = 512;
  LCHAR key[512];
  LCHAR buffer[512];
  LCHAR* value;
  LCHAR* ending;
  LCHAR* line;
  PFile* file = NULL;
  ESR_BOOL lineSpan = ESR_FALSE;
  LString* valueBuffer = NULL;
  ESR_ReturnCode rc = ESR_SUCCESS;

  if (filename == NULL)
    return ESR_INVALID_ARGUMENT;

  file = pfopen ( filename, L("r") );
/*  CHKLOG(rc, PFileSystemCreatePFile(filename, ESR_TRUE, &file));
  CHKLOG(rc, PFileOpen(file, L("r")));*/

  if (file == NULL)
  {
    LCHAR msg[P_PATH_MAX + 30];
    LCHAR cwd[P_PATH_MAX];
    size_t len;

    len = P_PATH_MAX;
    CHKLOG(rc, pf_get_cwd (cwd, &len));
    psprintf(msg, L("ESR_OPEN_FILE_ERROR(filename=%s, cwd=%s)"), filename, cwd);
    rc = ESR_OPEN_ERROR;
    PLogError(msg);
    goto CLEANUP;
  }

  rc = LStringCreate(&valueBuffer);
  if (rc != ESR_SUCCESS)
    goto CLEANUP;

  line = buffer;
  while (ESR_TRUE)
  {
    line = pfgets(line, LINE_SIZE, file);
    if (line == NULL)
    {
      if (pfeof(file))
        break;
      rc = ESR_READ_ERROR;
      PLogError(ESR_rc2str(rc));
      goto CLEANUP;
    }
    if (LSTRLEN(line) == LINE_SIZE)
    {
      PLogError(L("%s: line surpasses %d character limit: '%s'"), filename, line);
      continue;
    }
    lstrtrim(line);
    if (LSTRLEN(line) == 0 || line[0] == '#')
      continue;

    if (!lineSpan)
    {
      if (rc != ESR_SUCCESS) goto CLEANUP;

      /* locate the key and value pair */
      ending = LSTRCHR(line, '=');
      if (ending == NULL)
      {
        fprintf(stderr, "Missing equal sign on line '%s'\n", line);
        continue;
      }
      *ending = L('\0');
      value = ending + 1;
    }
    else
      value = line;

    /* Optionally use ';' to denote end of value */
    ending = LSTRCHR(value, L(';'));
    if (ending != NULL)
      *ending = L('\0');
    else
    {
      ending = LSTRCHR(value, L('\n'));
      if (ending != NULL)
        *ending = L('\0');
    }
    if (!lineSpan)
    {
      LSTRCPY(key, line);
      lstrtrim(key);
    }
    if (LSTRLEN(value) == 0)
    {
      pfprintf(PSTDERR, L("Missing value for '%s'\n"), key);
      continue;
    }
    lstrtrim(value);
    if ((ending = LSTRCHR(value, '\\')) == (value + LSTRLEN(value) - 1))
    {
      /* found '\\' at end of line which means data will span to the next line */
      lineSpan = ESR_TRUE;
      *ending = L('\0');
    }
    else
      lineSpan = ESR_FALSE;
    rc = LStringAppend(valueBuffer, value);
    if (rc != ESR_SUCCESS)
    {
      PLogError(ESR_rc2str(rc));
      goto CLEANUP;
    }
    if (!lineSpan)
    {
      rc = LStringToLCHAR(valueBuffer, &value);
      valueBuffer = NULL;
      if (rc != ESR_SUCCESS)
      {
        PLogError(ESR_rc2str(rc));
        goto CLEANUP;
      }
      rc = addMapping(self, key, value, data);
      if (value != NULL)
      {
        FREE(value);
        value = NULL;
      }
      if (rc != ESR_SUCCESS)
      {
        PLogError(ESR_rc2str(rc));
        goto CLEANUP;
      }
      rc = LStringCreate(&valueBuffer);
      if (rc != ESR_SUCCESS)
      {
        PLogError(ESR_rc2str(rc));
        goto CLEANUP;
      }
    }
  }

  if (pferror(file))
  {
    rc = ESR_READ_ERROR;
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }
  if (pfclose(file) != 0)
  {
    rc = ESR_CLOSE_ERROR;
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }
  if (valueBuffer != NULL)
    LStringDestroy(valueBuffer);
  return ESR_SUCCESS;
CLEANUP:
  if (file != NULL)
    pfclose(file);
  if (valueBuffer != NULL)
    LStringDestroy(valueBuffer);
  return rc;
}

/**
 * Appends a collection of integers stored in string format to an IntArrayList.
 *
 * @param self ESR_SessionType handle
 * @param text Text containing integers
 * @param list List to be populated
 */
static ESR_ReturnCode parseIntList(ESR_SessionType* self,
                                   LCHAR* text, IntArrayList* list)
{
  size_t size, pos, beginning;
  int value;
  ESR_ReturnCode rc;

  size = LSTRLEN(text);
  pos = 0;
  while (ESR_TRUE)
  {
    /* Scan for beginning of next token */
    for (; pos < size && LISSPACE(text[pos]); ++pos);

    if (pos >= size)
    {
      /* Reached end of string while looking for beginning of next token */
      break;
    }
    beginning = pos;

    /* Scan for ending of current token */
    for (; pos < size && !LISSPACE(text[pos]); ++pos);
    text[pos] = L('\0');
    CHKLOG(rc, lstrtoi(text + beginning, &value, 10));
    CHKLOG(rc, IntArrayListAdd(list, value));
    ++pos;
  }
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode ESR_SessionTypeGetPropertyTypeImpl(ESR_SessionType* self,
    const LCHAR* name, VariableTypes* type)
{
  ESR_SessionTypeData* data = self->data;
  ESR_SessionPair* pair;
  ESR_ReturnCode rc;

  CHK(rc, HashMapGet(data->value, name, (void **)&pair));
  *type = pair->type;
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

/**
 * Maps key -> type for PAR files.
 *
 * @param self ESR_Session handle
 * @param key Key name
 * @param value The value
 */
static ESR_ReturnCode addParMapping(ESR_SessionType* self,
                                    const LCHAR* key, LCHAR* value, void* data)
{
  IntArrayList* iList;
  ESR_ReturnCode rc;
  HashMap* map;
  VariableTypes* type;
  int iValue;
  size_t size_tValue;
  float fValue;
  ESR_BOOL bValue;
  ESR_BOOL exists;

  map = (HashMap*) data;
  rc = HashMapGet(data, key, (void **)&type);
  if (rc == ESR_NO_MATCH_ERROR)
  {
    /* If type is unknown, assume LCHAR* */
    PLogMessage(L("Unknown parfile key '%s'"), key);
    CHKLOG(rc, self->setLCHAR(self, key, value));
  }
  else if (rc == ESR_SUCCESS)
  {
    switch (*type)
    {
      case TYPES_INT:
        CHKLOG(rc, lstrtoi(value, &iValue, 10));
        CHKLOG(rc, self->contains(self, key, &exists));
        if (exists)
          CHKLOG(rc, self->removeAndFreeProperty(self, key));
        CHKLOG(rc, self->setInt(self, key, iValue));
        break;
      case TYPES_UINT16_T:
        CHKLOG(rc, lstrtoui(value, (unsigned int *)&size_tValue, 10));
        passert(size_tValue >= UINT16_TMIN && size_tValue <= UINT16_TMAX);
        CHKLOG(rc, self->contains(self, key, &exists));
        if (exists)
          CHKLOG(rc, self->removeAndFreeProperty(self, key));
        CHKLOG(rc, self->setUint16_t(self, key, (asr_uint16_t) size_tValue));
        break;
      case TYPES_SIZE_T:
        CHKLOG(rc, lstrtoui(value, (unsigned int *)&size_tValue, 10));
        CHKLOG(rc, self->contains(self, key, &exists));
        if (exists)
          CHKLOG(rc, self->removeAndFreeProperty(self, key));
        CHKLOG(rc, self->setSize_t(self, key, size_tValue));
        break;
      case TYPES_FLOAT:
        CHKLOG(rc, lstrtof(value, &fValue));
        CHKLOG(rc, self->contains(self, key, &exists));
        if (exists)
          CHKLOG(rc, self->removeAndFreeProperty(self, key));
        CHKLOG(rc, self->setFloat(self, key, fValue));
        break;
      case TYPES_BOOL:
        CHKLOG(rc, lstrtob(value, &bValue));
        CHKLOG(rc, self->contains(self, key, &exists));
        if (exists)
          CHKLOG(rc, self->removeAndFreeProperty(self, key));
        CHKLOG(rc, self->setBool(self, key, bValue));
        break;
      case TYPES_INTARRAYLIST:
        CHKLOG(rc, self->contains(self, key, &exists));
        if (exists)
        {
          CHKLOG(rc, self->getProperty(self, key, (void **)&iList, TYPES_INTARRAYLIST));
          CHKLOG(rc, self->removeProperty(self, key));
          CHKLOG(rc, iList->destroy(iList));
        }
        CHKLOG(rc, IntArrayListCreate(&iList));
        CHKLOG(rc, parseIntList(self, value, iList));
        CHKLOG(rc, self->setProperty(self, key, iList, TYPES_INTARRAYLIST));
        break;
      default:
        passert(0); /* Unknown variable type. Assuming LCHAR* */
      case TYPES_PLCHAR:
        CHKLOG(rc, self->contains(self, key, &exists));
        if (exists)
          CHKLOG(rc, self->removeAndFreeProperty(self, key));
        CHKLOG(rc, self->setLCHAR(self, key, value));
        break;
    }
  }
  return rc;
CLEANUP:
  return rc;
}

ESR_ReturnCode ESR_SessionTypeImportParFileImpl(ESR_SessionType* self,
    const LCHAR* filename)
{
  ESR_ReturnCode rc;
  HashMap* parameterList;
  VariableTypes Int = TYPES_INT;
  VariableTypes UInt16_t = TYPES_UINT16_T;
  VariableTypes Float = TYPES_FLOAT;
  VariableTypes Bool = TYPES_BOOL;
  VariableTypes IntArrayList = TYPES_INTARRAYLIST;
  VariableTypes PLChar = TYPES_PLCHAR;
  VariableTypes Size_t = TYPES_SIZE_T;

  /* Create [key, type] lookup table */
  CHKLOG(rc, HashMapCreate(&parameterList));

  CHKLOG(rc, parameterList->put(parameterList, "cmdline.arbfile", &PLChar));
  CHKLOG(rc, parameterList->put(parameterList, "cmdline.bgsniff", &Size_t));
  CHKLOG(rc, parameterList->put(parameterList, "cmdline.channel", &PLChar));
  CHKLOG(rc, parameterList->put(parameterList, "cmdline.datapath", &PLChar));
  CHKLOG(rc, parameterList->put(parameterList, "cmdline.DataCaptureDirectory", &PLChar));
  CHKLOG(rc, parameterList->put(parameterList, "cmdline.detail_res", &PLChar));
  CHKLOG(rc, parameterList->put(parameterList, "cmdline.lda", &PLChar));
  CHKLOG(rc, parameterList->put(parameterList, "cmdline.modelfiles", &PLChar));
  CHKLOG(rc, parameterList->put(parameterList, "cmdline.modelfiles11", &PLChar));
  CHKLOG(rc, parameterList->put(parameterList, "cmdline.modelfiles8", &PLChar));
  CHKLOG(rc, parameterList->put(parameterList, "cmdline.lda11", &PLChar));
  CHKLOG(rc, parameterList->put(parameterList, "cmdline.lda8", &PLChar));
  CHKLOG(rc, parameterList->put(parameterList, "cmdline.results", &PLChar));
  CHKLOG(rc, parameterList->put(parameterList, "cmdline.rules", &PLChar));
  CHKLOG(rc, parameterList->put(parameterList, "cmdline.tcp", &PLChar));
  CHKLOG(rc, parameterList->put(parameterList, "cmdline.multable", &PLChar));
  CHKLOG(rc, parameterList->put(parameterList, "cmdline.parfile", &PLChar));
  CHKLOG(rc, parameterList->put(parameterList, "cmdline.vocabulary", &PLChar));
  CHKLOG(rc, parameterList->put(parameterList, "cmdline.use_image", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "cmdline.semproc_verbose", &Bool));
  CHKLOG(rc, parameterList->put(parameterList, "cmdline.nametagPath", &PLChar));

  /* Beginning of speech detection stuff */
  CHKLOG(rc, parameterList->put(parameterList, "cmdline.bgsniff_min", &Size_t));
  CHKLOG(rc, parameterList->put(parameterList, "cmdline.silence_duration_in_frames", &Size_t));
  CHKLOG(rc, parameterList->put(parameterList, "cmdline.end_of_utterance_hold_off_in_frames", &Size_t));
  CHKLOG(rc, parameterList->put(parameterList, "cmdline.gatedmode", &Bool));

  /* new param from SREC that did not exist in CREC */
  CHKLOG(rc, parameterList->put(parameterList, "SREC.Recognizer.utterance_timeout", &Size_t));
  CHKLOG(rc, parameterList->put(parameterList, "SREC.Recognizer.osi_log_level", &Size_t));
  CHKLOG(rc, parameterList->put(parameterList, "SREC.voice_enroll.bufsz_kB", &Size_t));
  CHKLOG(rc, parameterList->put(parameterList, "SREC.voice_enroll.eos_comfort_frames", &Size_t));
  CHKLOG(rc, parameterList->put(parameterList, "SREC.voice_enroll.bos_comfort_frames", &Size_t));

  CHKLOG(rc, parameterList->put(parameterList, "SREC.Confidence.sigmoid_param.gdiff.one_nbest", &PLChar));
  CHKLOG(rc, parameterList->put(parameterList, "SREC.Confidence.sigmoid_param.gdiff.many_nbest", &PLChar));
  CHKLOG(rc, parameterList->put(parameterList, "SREC.Confidence.sigmoid_param.sd.one_nbest", &PLChar));
  CHKLOG(rc, parameterList->put(parameterList, "SREC.Confidence.sigmoid_param.sd.many_nbest", &PLChar));
  CHKLOG(rc, parameterList->put(parameterList, "SREC.Confidence.sigmoid_param.sd13.one_nbest", &PLChar));
  CHKLOG(rc, parameterList->put(parameterList, "SREC.Confidence.sigmoid_param.sd13.many_nbest", &PLChar));
  CHKLOG(rc, parameterList->put(parameterList, "SREC.Confidence.sigmoid_param.spf.one_nbest", &PLChar));
  CHKLOG(rc, parameterList->put(parameterList, "SREC.Confidence.sigmoid_param.spf.many_nbest", &PLChar));
  CHKLOG(rc, parameterList->put(parameterList, "SREC.Confidence.sigmoid_param.abs.one_nbest", &PLChar));
  CHKLOG(rc, parameterList->put(parameterList, "SREC.Confidence.sigmoid_param.abs.many_nbest", &PLChar));
  CHKLOG(rc, parameterList->put(parameterList, "SREC.Confidence.sigmoid_param.gdiffpf.one_nbest", &PLChar));
  CHKLOG(rc, parameterList->put(parameterList, "SREC.Confidence.sigmoid_param.gdiffpf.many_nbest", &PLChar));

  CHKLOG(rc, parameterList->put(parameterList, "CREC.ParVersion", &Float));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.useCREClogger", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Acoustic.dimen", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Acoustic.skip", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Acoustic.stay", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Acoustic.durscale", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Acoustic.minvar", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Acoustic.maxvar", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Acoustic.frame_period", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Acoustic.load_models", &PLChar));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Frontend.mel_dim", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Frontend.samplerate", &Size_t));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Frontend.premel", &Float));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Frontend.lowcut", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Frontend.highcut", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Frontend.window_factor", &Float));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Frontend.offset", &Float));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Frontend.ddmel", &Bool));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Frontend.peakdecayup", &Float));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Frontend.peakdecaydown", &Float));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Frontend.do_skip_even_frames", &Bool));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Frontend.do_smooth_c0", &Bool));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Frontend.melA", &IntArrayList));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Frontend.melB", &IntArrayList));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Frontend.dmelA", &IntArrayList));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Frontend.dmelB", &IntArrayList));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Frontend.ddmelA", &IntArrayList));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Frontend.ddmelB", &IntArrayList));

  /* new for S2G 3 (read from parfile instead of hardcoding) */
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Frontend.swicms.cmn", &IntArrayList));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Frontend.swicms.cmn8", &IntArrayList));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Frontend.swicms.cmn11", &IntArrayList));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Frontend.swicms.tmn", &IntArrayList));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Frontend.swicms.adjust", &IntArrayList));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Frontend.swicms.debug", &Bool));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Frontend.swicms.sbindex", &IntArrayList));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Frontend.swicms.forget_factor", &IntArrayList));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Frontend.swicms.cache_resolution", &IntArrayList));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Frontend.swicms.inutt.forget_factor2", &IntArrayList));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Frontend.swicms.inutt.disable_after", &IntArrayList));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Frontend.swicms.inutt.enable_after", &IntArrayList));


  /* zwz */
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Frontend.swicms.do_VN", &Bool));

  CHKLOG(rc, parameterList->put(parameterList, "CREC.Frontend.speech_detect", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Frontend.ambient_within", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Frontend.speech_above", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Frontend.start_windback", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Frontend.utterance_allowance", &Int));


  CHKLOG(rc, parameterList->put(parameterList, "CREC.Pattern.dimen", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Pattern.whole_dimen", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Pattern.mix_score_scale", &Float));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Pattern.start", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Pattern.chelt_imelda", &Bool));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Pattern.vfrlimit", &Bool));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Pattern.vfrthresh", &Bool));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Pattern.mix_score_scale", &Float));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Pattern.imelda_scale", &Float));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Pattern.uni_score_scale", &Float));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Pattern.uni_score_offset", &Float));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Pattern.forget_speech", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Pattern.forget_background", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Pattern.rel_low", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Pattern.rel_high", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Pattern.gap_period", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Pattern.click_period", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Pattern.breath_period", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Pattern.extend_annotation", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Pattern.min_initial_quiet_frames", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Pattern.min_annotation_frames", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Pattern.min_initial_quiet_frames", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Pattern.max_annotation_frames", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Pattern.delete_leading_segments", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Pattern.leading_segment_min_frames", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Pattern.leading_segment_max_frames", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Pattern.leading_segment_min_silence_gap_frames", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Pattern.leading_segment_accept_if_not_found", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Pattern.snr_holdoff", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Pattern.min_acceptable_snr", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Pattern.param", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Pattern.beep_size", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Pattern.beep_threshold", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Pattern.partial_distance_dim", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Pattern.partial_distance_threshold", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Pattern.partial_distance_offset", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Pattern.global_model_means", &IntArrayList));

  CHKLOG(rc, parameterList->put(parameterList, "CREC.Recognizer.NBest", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Recognizer.reject", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Recognizer.often", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Recognizer.partial_results", &Bool));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Recognizer.wordpen", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Recognizer.viterbi_prune_thresh", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Recognizer.max_hmm_tokens", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Recognizer.max_fsmnode_tokens", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Recognizer.max_word_tokens", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Recognizer.max_altword_tokens", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Recognizer.num_wordends_per_frame", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Recognizer.max_fsm_nodes", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Recognizer.max_fsm_arcs", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Recognizer.max_searches", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Recognizer.terminal_timeout", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Recognizer.optional_terminal_timeout", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Recognizer.non_terminal_timeout", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Recognizer.eou_threshold", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Recognizer.max_frames", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "CREC.Recognizer.max_model_states", &Int));
  CHKLOG(rc, parameterList->put(parameterList, "thread.priority", &UInt16_t));
  /* for G2P */
  CHKLOG(rc, parameterList->put(parameterList, "G2P.Available", &Bool));
  CHKLOG(rc, parameterList->put(parameterList, "G2P.Data", &PLChar));
  CHKLOG(rc, parameterList->put(parameterList, "G2P.Dictionary", &PLChar));
  /* Enable Get Waveform */
  CHKLOG(rc, parameterList->put(parameterList, "enableGetWaveform", &Bool));

  rc = importKeyValueFile(self, filename, addParMapping, parameterList);
  if (rc != ESR_SUCCESS)
    goto CLEANUP;
  CHKLOG(rc, HashMapDestroy(parameterList));
  return ESR_SUCCESS;
CLEANUP:
  HashMapDestroy(parameterList);
  return rc;
}

ESR_ReturnCode ESR_SessionTypeAddListenerImpl(ESR_SessionType* self, ESR_SessionTypeListenerPair* listener)
{
  ESR_SessionTypeData* data = self->data;
  ArrayList* listeners = data->listeners;
  ESR_ReturnCode rc;

  CHKLOG(rc, listeners->add(listeners, listener));
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode ESR_SessionTypeRemoveListenerImpl(ESR_SessionType* self, ESR_SessionTypeListenerPair* listener)
{
  ESR_SessionTypeData* data = self->data;
  ArrayList* listeners = data->listeners;
  ESR_ReturnCode rc;

  CHKLOG(rc, listeners->remove(listeners, listener));
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode ESR_SessionTypeDestroyImpl(ESR_SessionType* self)
{
  ESR_SessionTypeData* data = self->data;
  size_t hashSize;
  ESR_SessionPair* pair;
  ESR_ReturnCode rc;

  if (data != NULL)
  {
    if (data->value != NULL)
    {
      CHKLOG(rc, HashMapGetSize(data->value, &hashSize));
      while (hashSize > 0)
      {
        CHKLOG(rc, HashMapGetValueAtIndex(data->value, 0, (void **)&pair));

        if (pair->value)
        {
          if (pair->type == TYPES_INTARRAYLIST)
            CHKLOG(rc, IntArrayListDestroy((IntArrayList*) pair->value));
          else
            FREE(pair->value);
        }
        CHKLOG(rc, HashMapRemoveAtIndex(data->value, 0));
        --hashSize;
        FREE(pair);
      }

      CHKLOG(rc, HashMapDestroy(data->value));
      data->value = NULL;
    }
    if (data->listeners != NULL)
    {
      CHKLOG(rc, data->listeners->destroy(data->listeners));
      data->listeners = NULL;
    }
    FREE(data);
  }

  FREE(self);
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}
