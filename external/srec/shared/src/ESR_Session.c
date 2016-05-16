/*---------------------------------------------------------------------------*
 *  ESR_Session.c  *
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


#include "ESR_Session.h"
#include "ESR_SessionType.h"
#include "ESR_SessionTypeImpl.h"
#include <string.h>
#include "HashMap.h"
#include "IntArrayList.h"
#include "LCHAR.h"
#include "lstring.h"
#include "passert.h"
#include "plog.h"
#include "ptrd.h"
#include "pstdio.h"

static ESR_SessionType* ESR_Session = NULL;
#define CHECK_SESSION_OR_RETURN if(!ESR_Session) return ESR_INVALID_ARGUMENT

ESR_ReturnCode ESR_SessionCreate(const LCHAR* filename)
{
  ESR_ReturnCode rc;
  
  CHKLOG(rc, ESR_SessionTypeCreate(&ESR_Session));
  
  /* Initialize default values here */
  CHKLOG(rc, ESR_Session->setLCHAR(ESR_Session, L("cmdline.nametagPath"), L("")));
#ifdef USE_THREAD
  CHKLOG(rc, ESR_Session->setUint16_t(ESR_Session, L("thread.priority"), PtrdThreadNormalPriority));
#endif
  
  /* End of default values */
  CHKLOG(rc, ESR_Session->importParFile(ESR_Session, filename));
  return ESR_SUCCESS;
CLEANUP:
  ESR_SessionDestroy();
  return rc;
}

ESR_ReturnCode ESR_SessionGetProperty(const LCHAR* name, void** value, VariableTypes type)
{
  CHECK_SESSION_OR_RETURN;
  return ESR_Session->getProperty(ESR_Session, name, value, type);
}

ESR_SHARED_API ESR_ReturnCode ESR_SessionGetInt(const LCHAR* name, int* value)
{
  CHECK_SESSION_OR_RETURN;
  return ESR_Session->getInt(ESR_Session, name, value);
}

ESR_SHARED_API ESR_ReturnCode ESR_SessionGetUint16_t(const LCHAR* name, asr_uint16_t* value)
{
  CHECK_SESSION_OR_RETURN;
  return ESR_Session->getUint16_t(ESR_Session, name, value);
}

ESR_SHARED_API ESR_ReturnCode ESR_SessionGetSize_t(const LCHAR* name,
    size_t* value)
{
  CHECK_SESSION_OR_RETURN;
  return ESR_Session->getSize_t(ESR_Session, name, value);
}

ESR_SHARED_API ESR_ReturnCode ESR_SessionGetFloat(const LCHAR* name, float* value)
{
  CHECK_SESSION_OR_RETURN;
  return ESR_Session->getFloat(ESR_Session, name, value);
}

ESR_SHARED_API ESR_ReturnCode ESR_SessionGetBool(const LCHAR* name, ESR_BOOL* value)
{
  CHECK_SESSION_OR_RETURN;
  return ESR_Session->getBool(ESR_Session, name, value);
}

ESR_SHARED_API ESR_ReturnCode ESR_SessionGetLCHAR(const LCHAR* name, LCHAR* value, size_t* len)
{
  CHECK_SESSION_OR_RETURN;
  return ESR_Session->getLCHAR(ESR_Session, name, value, len);
}

ESR_ReturnCode ESR_SessionContains(const LCHAR* name, ESR_BOOL* exists)
{
  CHECK_SESSION_OR_RETURN;
  return ESR_Session->contains(ESR_Session, name, exists);
}

ESR_ReturnCode ESR_SessionSetProperty(const LCHAR* name, void* value, VariableTypes type)
{
  CHECK_SESSION_OR_RETURN;
  return ESR_Session->setProperty(ESR_Session, name, value, type);
}

ESR_ReturnCode ESR_SessionSetInt(const LCHAR* name, int value)
{
  CHECK_SESSION_OR_RETURN;
  return ESR_Session->setInt(ESR_Session, name, value);
}

ESR_ReturnCode ESR_SessionSetUint16_t(const LCHAR* name, asr_uint16_t value)
{
  CHECK_SESSION_OR_RETURN;
  return ESR_Session->setUint16_t(ESR_Session, name, value);
}

ESR_ReturnCode ESR_SessionSetSize_t(const LCHAR* name, size_t value)
{
  CHECK_SESSION_OR_RETURN;
  return ESR_Session->setSize_t(ESR_Session, name, value);
}

ESR_ReturnCode ESR_SessionSetFloat(const LCHAR* name, float value)
{
  CHECK_SESSION_OR_RETURN;
  return ESR_Session->setFloat(ESR_Session, name, value);
}

ESR_ReturnCode ESR_SessionSetBool(const LCHAR* name, ESR_BOOL value)
{
  CHECK_SESSION_OR_RETURN;
  return ESR_Session->setBool(ESR_Session, name, value);
}

ESR_ReturnCode ESR_SessionSetLCHAR(const LCHAR* name, LCHAR* value)
{
  CHECK_SESSION_OR_RETURN;
  return ESR_Session->setLCHAR(ESR_Session, name, value);
}

ESR_ReturnCode ESR_SessionSetIntIfEmpty(const LCHAR* name, int value)
{
  CHECK_SESSION_OR_RETURN;
  return ESR_Session->setIntIfEmpty(ESR_Session, name, value);
}

ESR_ReturnCode ESR_SessionSetUint16_tIfEmpty(const LCHAR* name, asr_uint16_t value)
{
  CHECK_SESSION_OR_RETURN;
  return ESR_Session->setUint16_tIfEmpty(ESR_Session, name, value);
}

ESR_ReturnCode ESR_SessionSetSize_tIfEmpty(const LCHAR* name, size_t value)
{
  CHECK_SESSION_OR_RETURN;
  return ESR_Session->setSize_tIfEmpty(ESR_Session, name, value);
}

ESR_ReturnCode ESR_SessionSetFloatIfEmpty(const LCHAR* name, float value)
{
  CHECK_SESSION_OR_RETURN;
  return ESR_Session->setFloatIfEmpty(ESR_Session, name, value);
}

ESR_ReturnCode ESR_SessionSetBoolIfEmpty(const LCHAR* name, ESR_BOOL value)
{
  CHECK_SESSION_OR_RETURN;
  return ESR_Session->setBoolIfEmpty(ESR_Session, name, value);
}

ESR_ReturnCode ESR_SessionSetLCHARIfEmpty(const LCHAR* name, LCHAR* value)
{
  CHECK_SESSION_OR_RETURN;
  return ESR_Session->setLCHARIfEmpty(ESR_Session, name, value);
}

ESR_ReturnCode ESR_SessionRemoveProperty(const LCHAR* name)
{
  CHECK_SESSION_OR_RETURN;
  return ESR_Session->removeProperty(ESR_Session, name);
}

ESR_ReturnCode ESR_SessionRemoveAndFreeProperty(const LCHAR* name)
{
  CHECK_SESSION_OR_RETURN;
  return ESR_Session->removeAndFreeProperty(ESR_Session, name);
}

ESR_ReturnCode ESR_SessionImportCommandLine(int argc, LCHAR* argv[])
{
  CHECK_SESSION_OR_RETURN;
  return ESR_Session->importCommandLine(ESR_Session, argc, argv);
}

ESR_ReturnCode ESR_SessionGetSize(size_t* size)
{
  CHECK_SESSION_OR_RETURN;
  return ESR_Session->getSize(ESR_Session, size);
}

ESR_ReturnCode ESR_SessionGetKeyAtIndex(size_t index, LCHAR** key)
{
  CHECK_SESSION_OR_RETURN;
  return ESR_Session->getKeyAtIndex(ESR_Session, index, key);
}

ESR_ReturnCode ESR_SessionConvertToInt(const LCHAR* key)
{
  CHECK_SESSION_OR_RETURN;
  return ESR_Session->convertToInt(ESR_Session, key);
}

ESR_ReturnCode ESR_SessionConvertToUint16_t(const LCHAR* key)
{
  CHECK_SESSION_OR_RETURN;
  return ESR_Session->convertToUint16_t(ESR_Session, key);
}

ESR_ReturnCode ESR_SessionConvertToSize_t(const LCHAR* key)
{
  CHECK_SESSION_OR_RETURN;
  return ESR_Session->convertToSize_t(ESR_Session, key);
}

ESR_ReturnCode ESR_SessionConvertToFloat(const LCHAR* key)
{
  CHECK_SESSION_OR_RETURN;
  return ESR_Session->convertToFloat(ESR_Session, key);
}

ESR_ReturnCode ESR_SessionConvertToBool(const LCHAR* key)
{
  CHECK_SESSION_OR_RETURN;
  return ESR_Session->convertToBool(ESR_Session, key);
}

ESR_ReturnCode ESR_SessionGetPropertyType(const LCHAR* name, VariableTypes* type)
{
  CHECK_SESSION_OR_RETURN;
  return ESR_Session->getPropertyType(ESR_Session, name, type);
}

ESR_ReturnCode ESR_SessionImportParFile(const LCHAR* filename)
{
  CHECK_SESSION_OR_RETURN;
  return ESR_Session->importParFile(ESR_Session, filename);
}

ESR_ReturnCode ESR_SessionDestroy()
{
  ESR_ReturnCode rc;
  
  if (ESR_Session != NULL)
  {
    CHKLOG(rc, ESR_Session->destroy(ESR_Session));
    ESR_Session = NULL;
  }
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode ESR_SessionExists(ESR_BOOL* val)
{
  *val = (ESR_Session != NULL);
  return ESR_SUCCESS;
}

ESR_ReturnCode ESR_SessionPrefixWithBaseDirectory(LCHAR* path, size_t* len)
{
  ESR_ReturnCode rc;
  LCHAR baseDirectory[P_PATH_MAX];
  ESR_BOOL isAbsolute;
  size_t len2 = P_PATH_MAX;
  
  /* Skip absolute paths. */
  CHKLOG(rc, pf_convert_backslashes_to_forwardslashes (path));
  CHKLOG(rc, pf_is_path_absolute (path, &isAbsolute));
  if (isAbsolute)
    return ESR_SUCCESS;
    
  CHKLOG(rc, ESR_SessionGetLCHAR(L("parFile.baseDirectory"), baseDirectory, &len2));
  CHKLOG(rc, lstrinsert(baseDirectory, path, 0, len));
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_SHARED_API ESR_ReturnCode ESR_SessionAddListener(ESR_SessionType* self, ESR_SessionTypeListenerPair* listener)
{
  CHECK_SESSION_OR_RETURN;
  return ESR_Session->addListener(ESR_Session, listener);
}

ESR_SHARED_API ESR_ReturnCode ESR_SessionRemoveListener(ESR_SessionType* self, ESR_SessionTypeListenerPair* listener)
{
  CHECK_SESSION_OR_RETURN;
  return ESR_Session->removeListener(ESR_Session, listener);
}
